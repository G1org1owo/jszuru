package jszuru.resources;

import jszuru.SzurubooruAPI;
import jszuru.exceptions.SzurubooruHTTPException;
import jszuru.exceptions.SzurubooruResourceNotSynchronizedException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public abstract class SzurubooruResource {
    protected SzurubooruAPI api;
    protected Map<String, Object> json;
    protected Map<String, Object> newJson;

    public SzurubooruResource(SzurubooruAPI api, Map<String, Object> initialJson){
        this.api = api;
        this.json = initialJson;
        this.newJson = new HashMap<>();
    }

    public abstract List<String> getInstanceUrlParts();
    public abstract List<String> getClassUrlParts();

    public abstract List<String> lazyLoadComponents();
    protected abstract Map<String, Function<Object, Object>> getterTransforms();
    protected abstract Map<String, Function<Object, Object>> setterTransforms();

    protected abstract Map<String, Object> serialized();

    protected Map<String, Object> copyNewJson(List<String> keysToCopy){
        Map<String, Object> ret = new HashMap<>();

        for(String key:keysToCopy){
            if(newJson.containsKey(key)){
                ret.put(key, newJson.get(key));
            }
        }

        return ret;
    }

    protected void updateJson(Map<String, Object> data) throws SzurubooruResourceNotSynchronizedException {
        updateJson(data, false);
    }
    protected void updateJson(Map<String, Object> data, boolean force) throws SzurubooruResourceNotSynchronizedException{
        if(!force){
            for(Map.Entry<String, Object> entry:data.entrySet()){
                String key = entry.getKey();
                Object value = entry.getValue();

                if(!newJson.containsKey(key)) continue;
                if(newJson.get(key).equals(json.get(key))) continue;
                if(newJson.get(key).equals(value)) continue;

                throw new SzurubooruResourceNotSynchronizedException(key);
            }
        }

        newJson = new HashMap<>();
        json = data;
    }

    public void pull() throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        Map<String, Object> data = api.call("GET", this.getInstanceUrlParts());
        this.updateJson(data);
    }
    public void push() throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        this.pushGeneric(SzurubooruResource::getInstanceUrlParts,
                         SzurubooruResource::getClassUrlParts);
    }
    public void delete() throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        deleteGeneric(SzurubooruResource::getInstanceUrlParts);
    }

    protected void pushGeneric(Function<SzurubooruResource, List<String>> putUrlParts,
                               Function<SzurubooruResource, List<String>> postUrlParts) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        Map<String, Object> body = this.serialized();
        Map<String, Object> data;

        if(json.containsKey("version") && json.get("version") != null){
            body.put("version", json.get("version"));
            data = api.call("PUT", putUrlParts.apply(this), null, body);
        }
        else{
            data = api.call("POST", postUrlParts.apply(this), null, body);
        }

        this.updateJson(data, true);
    }
    protected void deleteGeneric(Function<SzurubooruResource, List<String>> deleteUrlParts) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        if(json.containsKey("version") && json.get("version") != null){
            Map<String, Object> data = api.call("DELETE", deleteUrlParts.apply(this), null, Map.of("version", json.get("version")));
            this.updateJson(data, true);
        }
        else{
            throw new SzurubooruResourceNotSynchronizedException("Missing version in resource " + this);
        }
    }

    public boolean isSynchronized(){
        return !newJson.isEmpty();
    }

    public static Object applyTransforms(Map<String, Function<Object, Object>> transforms, String propertyName, Object propertyValue){
        if(propertyValue == null) return null;
        if(propertyValue instanceof List<?> list){
            return list.stream().map(x -> applyTransforms(transforms, propertyName, x)).toList();
        }
        if(transforms.containsKey(propertyName)){
            return transforms.get(propertyName).apply(propertyValue);//????
        }

        return propertyValue;
    }

    protected Object genericGetter(String propertyName) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        return genericGetter(propertyName, true);
    }
    protected Object genericGetter(String propertyName, boolean dynamicRefresh) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        if(newJson.containsKey(propertyName)){
            return applyTransforms(this.getterTransforms(), propertyName, newJson.get(propertyName));
        }
        if(json.containsKey(propertyName)){
            return applyTransforms(this.getterTransforms(), propertyName, json.get(propertyName));
        }
        if(dynamicRefresh){
            this.pull();
            return this.genericGetter(propertyName, false);
        }

        throw new IllegalStateException(propertyName + " is not present in the JSON response");
    }

    protected void genericSetter(String propertyName, Object propertyValue) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        genericSetter(propertyName, propertyValue, true);
    }
    protected void genericSetter(String propertyName, Object propertyValue, boolean dynamicRefresh) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        if(json.containsKey(propertyName)){
            if(json.get(propertyName) instanceof List && !(propertyValue instanceof Iterable<?>)){
                throw new IllegalArgumentException(propertyName + " must be an iterable");
            }

            newJson.put(propertyName, applyTransforms(setterTransforms(), propertyName, propertyValue));
            return;
        }

        if(dynamicRefresh){
            this.pull();
            this.genericSetter(propertyName, propertyValue, false);
            return;
        }

        throw new IllegalStateException(propertyName + " is not present in the JSON response");
    }

    protected String fileGetter(String propertyName) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException, URISyntaxException {
        return fileGetter(propertyName, true);
    }
    protected String fileGetter(String propertyName, boolean dynamicRefresh) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException, URISyntaxException {
        if(json.containsKey(propertyName + "Url")){
            return api.createDataUrl(json.get(propertyName + "Url").toString());
        }

        if(dynamicRefresh){
            this.pull();
            return this.fileGetter(propertyName, false);
        }

        throw new IllegalStateException(propertyName + " is not a URL resource in the JSON response");
    }

    protected void fileSetter(String propertyName, FileToken propertyValue){
        fileSetter(propertyName,propertyValue, true);
    }
    protected void fileSetter(String propertyName, FileToken propertyValue, boolean dynamicRefresh){
        newJson.put(propertyName + "Token", propertyValue.getToken());
    }

    protected static List<String> getPrimaryNames(List<Map<String, Object>> list){
        return list.stream()
                .map(y -> ((List<String>)y.get("names")).get(0))
                .toList();
    }

    public SzurubooruAPI getApi(){
        return api;
    }
    public SzurubooruResource setApi(SzurubooruAPI api) {
        this.api = api;
        return this;
    }

    public Map<String, Object> getJson() {
        return json;
    }
    public SzurubooruResource setJson(Map<String, Object> json) {
        this.json = json;
        return this;
    }

    public Map<String, Object> getNewJson() {
        return newJson;
    }
    public SzurubooruResource setNewJson(Map<String, Object> newJson) {
        this.newJson = newJson;
        return this;
    }
}
