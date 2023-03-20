package jszuru.resources;

import jszuru.SzurubooruAPI;
import jszuru.exceptions.SzurubooruException;
import jszuru.exceptions.SzurubooruHTTPException;
import jszuru.exceptions.SzurubooruResourceNotSynchronizedException;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

@SuppressWarnings("unused")
public class SzurubooruTag extends SzurubooruResource{
    @Override
    public List<String> getInstanceUrlParts() {
        return List.of("tag", this.getPrimaryName());
    }
    @Override
    public List<String> getClassUrlParts() {
        return List.of("tags");
    }

    @Override
    public List<String> lazyLoadComponents() {
        return List.of("names", "category", "usages");
    }

    @Override
    protected Map<String, Function<Object, Object>> getterTransforms() {
        Map<String, Function<Object, Object>> map = new HashMap<>();

        map.put("implications", (x) -> new SzurubooruTag(api, (Map<String, Object>)x));
        map.put("suggestions", (x) -> new SzurubooruTag(api, (Map<String, Object>)x));

        return map;
    }
    @Override
    protected Map<String, Function<Object, Object>> setterTransforms() {
        Map<String, Function<Object, Object>> map = new HashMap<>();

        Function<Object, Object> lambda = (x) -> {
            Map<String, Object> itemMap = new HashMap<>();
            SzurubooruTag tag = (SzurubooruTag)x;

            itemMap.put("names", tag.getNames());
            itemMap.put("category", tag.getCategory());

            return itemMap;
        };

        map.put("implications", lambda);
        map.put("suggestions", lambda);

        return map;
    }

    @Override
    protected Map<String, Object> serialized() {
        Map<String, Object> ret = this.copyNewJson(List.of("names", "category", "description", "implications", "suggestions"));

        if(ret.containsKey("implications")){
            List<Map<String, Object>> implications = (List<Map<String, Object>>) ret.get("implications");
            ret.put("implications", getPrimaryNames(implications));
        }
        if(ret.containsKey("suggestions")){
            List<Map<String, Object>> suggestions = (List<Map<String, Object>>) ret.get("suggestions");
            ret.put("suggestions", getPrimaryNames(suggestions));
        }

        return ret;
    }

    public void mergeFrom(SzurubooruTag source, boolean addAsAlias) throws SzurubooruResourceNotSynchronizedException, IOException, SzurubooruHTTPException {
        if(!source.json.containsKey("version") || !source.newJson.isEmpty()){
            throw new SzurubooruResourceNotSynchronizedException("Target tag is not synchronized");
        }
        if(!json.containsKey("version") || !newJson.isEmpty()){
            throw new SzurubooruResourceNotSynchronizedException("This tag is not synchronized");
        }

        Map<String, Object> body = Map.of(
                "removeVersion", source.json.get("version"),
                "remove", source.getPrimaryName(),
                "mergeToVersion", json.get("version"),
                "mergeTo", this.getPrimaryName()
        );

        Map<String, Object> data = api.call("POST", List.of("tag-merge"), null, body);
        this.updateJson(data, true);

        if(addAsAlias){
            List<String> names = this.getNames();
            names.addAll(source.getNames());
            this.setNames(names
                .stream()
                .distinct()
                .toList());
        }

        source.json = new HashMap<>();
    }

    public SzurubooruTag(SzurubooruAPI api, Map<String, Object> initialJson){
        super(api, initialJson);
    }

    public List<String> getNames() {
        try{
            return new ArrayList<>((List<String>) this.genericGetter("names"));
        } catch(IOException | SzurubooruException e){
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruTag setNames(List<String> names) {
        try{
            this.genericSetter("names", names);
        } catch(IOException | SzurubooruException e){
            e.printStackTrace();
        }

        return this;
    }

    public String getCategory() {
        try{
            return (String) this.genericGetter("category");
        } catch(IOException | SzurubooruException e){
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruTag setCategory(String category) {
        try{
            this.genericSetter("category", category);
        } catch(IOException | SzurubooruException e){
            e.printStackTrace();
        }

        return this;
    }

    public List<SzurubooruTag> getImplications() {
        try{
            return new ArrayList<>((List<SzurubooruTag>) this.genericGetter("implications"));
        } catch(IOException | SzurubooruException e){
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruTag setImplications(List<SzurubooruTag> implications) {
        try {
            this.genericSetter("implications", implications);
        } catch(IOException | SzurubooruException e){
            e.printStackTrace();
        }

        return this;
    }

    public List<SzurubooruTag> getSuggestions() {
        try{
            return new ArrayList<>((List<SzurubooruTag>) this.genericGetter("suggestions"));
        } catch(IOException | SzurubooruException e){
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruTag setSuggestions(List<SzurubooruTag> suggestions) {
        try{
            this.genericSetter("suggestions", suggestions);
        } catch(IOException | SzurubooruException e){
            e.printStackTrace();
        }

        return this;
    }

    public String getDescription() {
        try{
            return (String) this.genericGetter("description");
        } catch(IOException | SzurubooruException e){
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruTag setDescription(String description) {
        try{
            this.genericSetter("description", description);
        } catch (IOException | SzurubooruException e){
            e.printStackTrace();
        }

        return this;
    }

    public String getPrimaryName() {
        return this.getNames().get(0);
    }
    public SzurubooruTag setPrimaryName(String primaryName) {
        List<String> existingNames = this.getNames();

        existingNames.remove(primaryName);
        existingNames.add(0, primaryName);

        return this.setNames(existingNames);
    }

    public String toString(){
        return this.getPrimaryName();
    }
}
