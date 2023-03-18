package jszuru;

import jszuru.exceptions.SzurubooruHTTPException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SzurubooruSearch {
    public static SzurubooruResource[] searchGeneric(SzurubooruAPI api,
                                                     String searchQuery,
                                                     SzurubooruResource transformingClass,
                                                     int pageSize) throws IOException, SzurubooruHTTPException {
        return searchGeneric(api, searchQuery, transformingClass, pageSize, false);
    }
    public static SzurubooruResource[] searchGeneric(SzurubooruAPI api,
                                                     String searchQuery,
                                                     SzurubooruResource transformingClass,
                                                     int pageSize,
                                                     boolean eagerLoad) throws IOException, SzurubooruHTTPException {
        int offset = 0;
        int total = 0;

        List<SzurubooruResource> results = new ArrayList<>();

        while(true){
            Map<String, String> urlQuery = Map.of("offset", offset + "", "limit", pageSize + "", "query", searchQuery);

            if(!eagerLoad){
                urlQuery.put("fields", String.join(",", transformingClass.lazyLoadComponents()));
            }

            Map<String, Object> page = api.call("GET", transformingClass.getClassUrlParts(), urlQuery, null);
            offset += ((List<?>)page.get("results")).size();

            if(!page.get("total").equals(total)){
                total = (int)page.get("total");
            }

            results.addAll((List<? extends SzurubooruResource>)page.get("results"));

            if(offset >= total) break;
        }

        return results.toArray(new SzurubooruResource[0]);
    }
}
