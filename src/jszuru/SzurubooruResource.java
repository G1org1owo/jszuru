package jszuru;

import jszuru.exceptions.SzurubooruHTTPException;
import jszuru.exceptions.SzurubooruResourceNotSynchronizedException;

import java.io.IOException;
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

    protected abstract List<String> getInstanceUrlParts();
    protected abstract List<String> getClassUrlParts();

    protected abstract List<String> lazyLoadComponents();
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

                if(!newJson.containsKey(key)) return;
                if(newJson.get(key).equals(json.get(key))) return;
                if(newJson.get(key).equals(value)) return;

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
        Map<String, Object> body = this.serialized();
        Map<String, Object> data;

        if(json.containsKey("version") && json.get("version") != null){
            body.put("version", json.get("version"));
            data = api.call("PUT", this.getInstanceUrlParts(), null, body);
        }
        else{
            data = api.call("POST", getInstanceUrlParts(), null, body);
        }

        this.updateJson(data, true);
    }

    public boolean isSynchronized(){
        return !newJson.isEmpty();
    }

    public SzurubooruAPI getApi(){
        return api;
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

    protected String fileGetter(String propertyName) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        return fileGetter(propertyName, true);
    }
    protected String fileGetter(String propertyName, boolean dynamicRefresh) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
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
}