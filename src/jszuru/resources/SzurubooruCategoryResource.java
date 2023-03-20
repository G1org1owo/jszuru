package jszuru.resources;

import jszuru.SzurubooruAPI;
import jszuru.exceptions.SzurubooruHTTPException;
import jszuru.exceptions.SzurubooruResourceNotSynchronizedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class SzurubooruCategoryResource extends SzurubooruResource{
    public SzurubooruCategoryResource(SzurubooruAPI api, Map<String, Object> initialJson){
        super(api, initialJson);
    }

    public SzurubooruCategoryResource setDefault() throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        List<String> urlParts = new ArrayList<>(this.getInstanceUrlParts());
        urlParts.add("default");

        Map<String, Object> data = api.call("PUT", urlParts, null, null);
        this.updateJson(data, true);

        return this;
    }
}
