package jszuru.resources;

import jszuru.SzurubooruAPI;
import jszuru.exceptions.SzurubooruException;
import jszuru.exceptions.SzurubooruHTTPException;
import jszuru.exceptions.SzurubooruResourceNotSynchronizedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SzurubooruPool extends SzurubooruResource {
    public SzurubooruPool(SzurubooruAPI api, Map<String, Object> initialJson){
        super(api, initialJson);
    }

    @Override
    public List<String> getInstanceUrlParts() {
        return List.of("pool", this.getId() + "");
    }
    @Override
    public List<String> getClassUrlParts() {
        return List.of("pools");
    }

    @Override
    public List<String> lazyLoadComponents() {
        return List.of("id", "names", "category", "description", "postCount", "posts");
    }

    @Override
    protected Map<String, Function<Object, Object>> getterTransforms() {
        Function<Object, Object> lambda = x -> new SzurubooruPool(api, ((Map<String, Object>) x));

        return Map.of("posts", lambda);
    }
    @Override
    protected Map<String, Function<Object, Object>> setterTransforms() {
        Function<Object, Object> lambdaPosts = (x) -> {
            SzurubooruPost post = (SzurubooruPost) x;
            return Map.of("id", post.getId());
        };

        return Map.of("posts", lambdaPosts);
    }

    @Override
    protected Map<String, Object> serialized() {
        Map<String, Object> ret = this.copyNewJson(List.of(
                "names",
                "category",
                "description",
                "posts"
        ));

        if(ret.containsKey("posts")){
            List<Map<String, Object>> posts = (List<Map<String, Object>>) ret.get("posts");
            ret.put("posts", getPostIds(posts));
        }

        return ret;
    }

    @Override
    public void push() throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        // API inconsistency :shrug:
        List<String> postUrlParts = List.of("pool");

        this.pushGeneric(SzurubooruResource::getInstanceUrlParts, x -> postUrlParts);
    }
    @Override
    public void delete() throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        this.deleteGeneric(x -> List.of("pool", this.getNames().get(0)));
    }

    public void mergeFrom(SzurubooruPool source, boolean addAsAlias) throws SzurubooruResourceNotSynchronizedException, IOException, SzurubooruHTTPException {
        if(!source.json.containsKey("version") || !source.newJson.isEmpty()){
            throw new SzurubooruResourceNotSynchronizedException("Target pool is not synchronized");
        }
        if(!json.containsKey("version") || !newJson.isEmpty()){
            throw new SzurubooruResourceNotSynchronizedException("This pool is not synchronized");
        }

        Map<String, Object> body = Map.of(
                "removeVersion", source.json.get("version"),
                "remove", source.getId(),
                "mergeToVersion", json.get("version"),
                "mergeTo", this.getId()
        );

        Map<String, Object> data = api.call("POST", List.of("pool-merge"), null, body);
        this.updateJson(data, true);

        if(addAsAlias){
            List<String> names = this.getNames();
            names.addAll(source.getNames());
            this.setNames(names
                    .stream()
                    .distinct()
                    .toList());
        }
    }

    public int getId(){
        try {
            return getIntValue(this.genericGetter("id"));
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public List<String> getNames(){
        try {
            return (List<String>)this.genericGetter("names");
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruPool setNames(List<String> names){
        try{
            this.genericSetter("names", names);
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
        }

        return this;
    }

    public String getCategory(){
        try {
            return (String)this.genericGetter("category");
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruPool setCategory(String category){
        try{
            this.genericSetter("category", category);
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
        }

        return this;
    }

    public String getDescription(){
        try {
            return (String)this.genericGetter("description");
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruPool setDescription(String description){
        try{
            this.genericSetter("description", description);
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
        }

        return this;
    }

    public List<SzurubooruPost> getPosts(){
        try {
            return (List<SzurubooruPost>)this.genericGetter("posts");
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruPool setPosts(List<SzurubooruPost> posts){
        try{
            this.genericSetter("posts", posts);
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
        }

        return this;
    }

    @Override
    public String toString() {
        return this.getNames().get(0);
    }
}
