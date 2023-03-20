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

public class SzurubooruPoolCategory extends SzurubooruCategoryResource {
    public SzurubooruPoolCategory(SzurubooruAPI api, Map<String, Object> initialJson){
        super(api, initialJson);
    }

    @Override
    public List<String> getInstanceUrlParts() {
        Object name = json.get("name");
        if(name == null) name = newJson.get("name");

        return List.of("pool-category", (String) name);
    }
    @Override
    public List<String> getClassUrlParts() {
        return List.of("pool-categories");
    }

    @Override
    public List<String> lazyLoadComponents() {
        return null;
    }

    @Override
    protected Map<String, Function<Object, Object>> getterTransforms() {
        return new HashMap<>();
    }
    @Override
    protected Map<String, Function<Object, Object>> setterTransforms() {
        return new HashMap<>();
    }

    @Override
    protected Map<String, Object> serialized() {
        return this.copyNewJson(List.of("name", "color"));
    }

    public String getName(){
        try {
            return (String) this.genericGetter("name");
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruPoolCategory setName(String name){
        try {
            this.genericSetter("name", name);
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
        }

        return this;
    }

    public String getColor(){
        try {
            return (String) this.genericGetter("color");
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruPoolCategory setColor(String color){
        try {
            this.genericSetter("color", color);
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
        }

        return this;
    }

    public int getUsages(){
        try {
            return getIntValue(this.genericGetter("usages"));
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean isDefault(){
        try {
            return (boolean) this.genericGetter("default");
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return false;
        }
    }
    public SzurubooruPoolCategory setDefault() throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        return (SzurubooruPoolCategory) super.setDefault();
    }

    public String toString(){
        return this.getName();
    }
}
