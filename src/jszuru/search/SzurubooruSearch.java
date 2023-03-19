package jszuru.search;

import jszuru.SzurubooruAPI;
import jszuru.exceptions.SzurubooruHTTPException;
import jszuru.resources.SzurubooruResource;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class SzurubooruSearch {
    public static <T extends SzurubooruResource> List<T> searchGeneric(SzurubooruAPI api,
                                                     String searchQuery,
                                                     Class<T> resourceClass,
                                                     int pageSize) throws IOException, SzurubooruHTTPException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return searchGeneric(api, searchQuery, resourceClass, pageSize, false);
    }
    public static <T extends SzurubooruResource> List<T> searchGeneric(SzurubooruAPI api,
                                                     String searchQuery,
                                                     Class<T> resourceClass,
                                                     int pageSize,
                                                     boolean eagerLoad) throws IOException, SzurubooruHTTPException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        int offset = 0;
        int total = Integer.MAX_VALUE;

        List<T> results = new ArrayList<>();

        Constructor<T> resourceConstructor = resourceClass.getDeclaredConstructor(SzurubooruAPI.class, Map.class);
        T defaultResource = resourceConstructor.newInstance(api, new HashMap<>());

        while(offset < total){
            Map<String, String> urlQuery = new HashMap<>(Map.of("offset", offset + "", "limit", pageSize + "", "query", searchQuery));

            if(!eagerLoad){
                urlQuery.put("fields", String.join(",", defaultResource.lazyLoadComponents()));
            }

            Map<String, Object> page = api.call("GET", defaultResource.getClassUrlParts(), urlQuery, null);
            offset += ((List<?>)page.get("results")).size();

            if(!page.get("total").equals(total)){
                total = ((Double)page.get("total")).intValue();
            }

            results.addAll(((List<Map<String, Object>>)page.get("results"))
                    .stream()
                    .map(x -> {
                        try{
                            return resourceConstructor.newInstance(api, x);
                        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                            return null;
                        }
                    })
                    .toList());
        }

        return results;
    }
}
