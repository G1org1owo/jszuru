package jszuru.search;

import jszuru.SzurubooruAPI;
import jszuru.exceptions.SzurubooruHTTPException;
import jszuru.resources.SzurubooruResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class SzurubooruSearch {
    public static List<SzurubooruResource> searchGeneric(SzurubooruAPI api,
                                                     String searchQuery,
                                                     SzurubooruResource transformingClass,
                                                     int pageSize) throws IOException, SzurubooruHTTPException {
        return searchGeneric(api, searchQuery, transformingClass, pageSize, false);
    }
    public static List<SzurubooruResource> searchGeneric(SzurubooruAPI api,
                                                     String searchQuery,
                                                     SzurubooruResource transformingClass,
                                                     int pageSize,
                                                     boolean eagerLoad) throws IOException, SzurubooruHTTPException {
        int offset = 0;
        int total = Integer.MAX_VALUE;

        List<SzurubooruResource> results = new ArrayList<>();

        while(offset < total){
            Map<String, String> urlQuery = new HashMap<>(Map.of("offset", offset + "", "limit", pageSize + "", "query", searchQuery));

            if(!eagerLoad){
                urlQuery.put("fields", String.join(",", transformingClass.lazyLoadComponents()));
            }

            Map<String, Object> page = api.call("GET", transformingClass.getClassUrlParts(), urlQuery, null);
            offset += ((List<?>)page.get("results")).size();

            if(!page.get("total").equals(total)){
                total = (int)page.get("total");
            }

            results.addAll((List<? extends SzurubooruResource>)page.get("results"));
        }

        return results;
    }
}
