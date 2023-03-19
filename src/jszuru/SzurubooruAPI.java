package jszuru;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import jszuru.exceptions.SzurubooruHTTPException;
import jszuru.exceptions.SzurubooruResourceNotSynchronizedException;
import jszuru.resources.FileToken;
import jszuru.resources.SzurubooruPost;
import jszuru.resources.SzurubooruTag;
import jszuru.search.SzurubooruSearch;
import jszuru.search.SzurubooruSearchResult;
import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class SzurubooruAPI {
    private final String urlScheme;
    private final String urlNetLocation;
    private String urlPathPrefix;
    private String apiScheme;
    private String apiNetLocation;
    private String apiPathPrefix;
    private final HashMap<String, String> apiHeaders;
    private String username = null;

    public static class APIBuilder{
        private String baseUrl;
        private String username = null;
        private String password = null;
        private String token = null;
        private String apiUri = "/api";

        public APIBuilder(){}

        public APIBuilder setApiUri(String apiUri) {
            this.apiUri = apiUri;
            return this;
        }
        public APIBuilder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }
        public APIBuilder setUsername(String username) {
            this.username = username;
            return this;
        }
        public APIBuilder setPassword(String password) {
            this.password = password;
            return this;
        }
        public APIBuilder setToken(String token) {
            this.token = token;
            return this;
        }

        public SzurubooruAPI build() throws MalformedURLException, URISyntaxException{
            return new SzurubooruAPI(baseUrl, username, password, token, apiUri);
        }
    }

    protected static Pattern tokenChecker = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    protected static String encodeAuthHeaders(String user, String password){
        String fullAuth = user + ":" + password;
        return Base64.getEncoder().encodeToString(fullAuth.getBytes());
    }
    protected static void checkApiResponse(HttpResponse response) throws SzurubooruHTTPException, IOException {
        if(response.getStatusLine().getStatusCode() != 200/*HTTP OK*/){
            Gson gson = new Gson();
            String content = new String(response.getEntity().getContent().readAllBytes());
            Map<String, Object> responseMap = gson.fromJson(content, new TypeToken<Map<String, Object>>(){}.getType());

            Object errorName = responseMap.get("name");
            Object errorDescription = responseMap.get("description");

            if(errorName == null || errorDescription == null){
                throw new SzurubooruHTTPException(content);
            }

            throw new SzurubooruHTTPException(errorName + ": " + errorDescription);
        }
    }

    public SzurubooruAPI(String baseUrl, String username, String password, String token, String apiUri) throws MalformedURLException, URISyntaxException {
        URL parsedBaseUrl = new URL(baseUrl);

        // Extract Base URL parts
        urlScheme = parsedBaseUrl.getProtocol();
        urlNetLocation = parsedBaseUrl.getAuthority();

        if(!urlScheme.equals("https") && !urlScheme.equals("http")){
            throw new IllegalArgumentException("Base URL must be of HTTP or HTTPS scheme");
        }

        urlPathPrefix = stripTrailing(parsedBaseUrl.getPath(), "/");
        if(!urlPathPrefix.startsWith("/")) urlPathPrefix = "/" + urlPathPrefix;

        // Extract API URI parts
        URI parsedApiUri = new URI(apiUri);
        apiScheme = parsedApiUri.getScheme();
        if(apiScheme == null) apiScheme = urlScheme;

        apiNetLocation = parsedApiUri.getAuthority();
        if(apiNetLocation == null) apiNetLocation = urlNetLocation;

        if(!apiScheme.equals("https") && !urlScheme.equals("http")){
            throw new IllegalArgumentException("API URI must be of HTTP or HTTPS scheme");
        }

        apiPathPrefix = parsedApiUri.getPath();
        if(!apiPathPrefix.startsWith("/") && Objects.equals(apiNetLocation, urlNetLocation)){
            apiPathPrefix = urlPathPrefix + "/" + apiPathPrefix;
        }

        apiPathPrefix = stripTrailing(apiPathPrefix, "/");

        // Extract Auth Info
        apiHeaders = new HashMap<>();
        apiHeaders.put("Accept", "application/json");

        String apiUserInfo = parsedApiUri.getUserInfo();
        String baseUserInfo = parsedBaseUrl.getUserInfo();

        String[] usernames = {
                username,
                apiUserInfo != null? apiUserInfo.split(":")[0] : null,
                baseUserInfo != null? baseUserInfo.split(":")[0] : null
        };

        for(String usr:usernames){
            if(usr != null && !usr.isEmpty()){
                this.username = usr;
                break;
            }
        }

        if(token != null){
            if(this.username == null) {
                throw new IllegalArgumentException("Token authentication specified without username");
            }
            if(!tokenChecker.matcher(token).matches()) {
                throw new IllegalArgumentException("Malformed token string");
            }

            apiHeaders.put("Authorization", "Token " + encodeAuthHeaders(this.username, token));
            return;
        }

        String[] passwords = {
                password,
                apiUserInfo != null? apiUserInfo.split(":")[1] : null,
                baseUserInfo != null? baseUserInfo.split(":")[1] : null
        };

        for(String pwd:passwords){
            if(pwd != null && !pwd.isEmpty()){
                password = pwd;
                break;
            }
        }

        if(password != null){
            if(this.username == null) {
                throw new IllegalArgumentException("Password authentication specified without username");
            }

            apiHeaders.put("Authorization", "Basic " + encodeAuthHeaders(this.username, password));
            return;
        }

        if(this.username != null) {
            throw new IllegalArgumentException("Username specified without authentication method");
        }
    }

    protected String createApiUrl(List<String> parts){
        return createApiUrl(parts, null);
    }
    protected String createApiUrl(List<String> parts, Map<String, String> query){
        ArrayList<String> path = new ArrayList<>();
        path.add(apiPathPrefix);
        path.addAll(parts);

        String url = apiScheme + "://" + apiNetLocation + String.join("/", path);
        if(query == null || query.isEmpty()) return url;

        return url + "?" + URLEncodedUtils.format(query.entrySet()
                .stream()
                .map((x) -> new BasicNameValuePair(x.getKey(), x.getValue()))
                .toList(), "UTF-8");
    }

    public Map<String, Object> call(String method, List<String> urlParts) throws IOException, SzurubooruHTTPException {
        return call(method, urlParts, null, null);
    }
    public Map<String, Object> call(String method,
                                       List<String> urlParts,
                                       Map<String, String> urlQuery,
                                       Map<String, Object> body) throws IOException, SzurubooruHTTPException {
        Gson gson = new Gson();

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){
            HttpRequest httpRequest = createHttpRequest(method, createApiUrl(urlParts, urlQuery));
            apiHeaders.forEach(httpRequest::setHeader);
            httpRequest.setHeader("Content", "application/json");

            if(body!=null && httpRequest instanceof HttpEntityEnclosingRequest sendRequest){
                sendRequest.setEntity(new StringEntity(gson.toJson(body)));
            }

            HttpResponse response = httpClient.execute((HttpUriRequest) httpRequest);

            checkApiResponse(response);
            String content = new String(response.getEntity().getContent().readAllBytes());
            return gson.fromJson(content, new TypeToken<Map<String, Object>>(){}.getType());
        }
    }

    public FileToken uploadFile(String file) throws IOException {
        return uploadFile(new File(file));
    }

    public FileToken uploadFile(File file) throws IOException {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("uploads");

        try(CloseableHttpClient httpClient = HttpClients.createDefault()){
            HttpPost uploadFile = new HttpPost(createApiUrl(parts));
            apiHeaders.forEach(uploadFile::setHeader);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("content", file, ContentType.APPLICATION_OCTET_STREAM, file.getName());

            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);
            CloseableHttpResponse response = httpClient.execute(uploadFile);
            String content = new String(response.getEntity().getContent().readAllBytes());

            Gson gson = new Gson();
            HashMap<String, String> responseMap = gson.fromJson(content, new TypeToken<HashMap<String, String>>(){}.getType());

            return new FileToken(responseMap.get("token"), file.getName());
        }
    }

    public String createDataUrl(String relativeUrl) throws URISyntaxException, MalformedURLException {
        return createDataUrl(relativeUrl, true);
    }
    public String createDataUrl(String relativeUrl, boolean overrideBase) throws URISyntaxException, MalformedURLException {
        if(overrideBase){
            String basePath = new File("/", urlPathPrefix).toString();
            String relativePath = new URI(relativeUrl).getPath();

            return urlScheme + "://" + urlNetLocation + new File(basePath, relativePath);
        }

        return new URL(new URL(urlScheme + "://" + urlNetLocation + urlPathPrefix), relativeUrl).toString();
    }

    public void saveToConfig(String filename) throws IOException {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        gson.toJson(this, new FileWriter(filename));
    }
    public static SzurubooruAPI loadFromConfig(String filename) throws FileNotFoundException {
        Gson gson = new Gson();

        return gson.fromJson(new FileReader(filename), SzurubooruAPI.class);
    }

    public SzurubooruPost getPost(int id) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruPost post = new SzurubooruPost(this, Map.of("id", id));
        post.pull();
        return post;
    }
    public SzurubooruPost createPost(FileToken content, String safety) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        if(!SzurubooruPost.isValidSafety(safety)){
            throw new IllegalArgumentException("Safety must be of value safe, sketchy or unsafe");
        }

        SzurubooruPost post = new SzurubooruPost(this, new HashMap<>());
        post.setNewJson(Map.of(
                "tags", new ArrayList<>(),
                "safety", safety,
                "contentToken", content.getToken()
        ));

        post.push();
        return post;
    }
    public List<SzurubooruPost> searchPost(String searchQuery) throws IOException, SzurubooruHTTPException {
        return searchPost(searchQuery, 20, false);
    }
    public List<SzurubooruPost> searchPost(String searchQuery, int pageSize, boolean eagerLoad) throws IOException, SzurubooruHTTPException {
        try{
            return SzurubooruSearch.searchGeneric(this, searchQuery, SzurubooruPost.class, pageSize, eagerLoad);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            return new ArrayList<>();
        }
    }

    public SzurubooruTag getTag(String id) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruTag tag = new SzurubooruTag(this, Map.of("names", List.of(id)));
        tag.pull();
        return tag;
    }
    public SzurubooruTag createTag(String name) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        String defaultCategory = "default";

        if(this.call("GET", List.of("tag-categories")).get("results") instanceof List<?> results){
            defaultCategory = results
                    .stream()
                    .filter((x) -> x instanceof Map<?, ?> map && ((boolean)map.get("default")))
                    .map((x) -> (String)((Map<String, Object>) x).get("name"))
                    .findFirst()
                    .orElse("default");
        }

        SzurubooruTag tag = new SzurubooruTag(this, new HashMap<>());
        tag.setNewJson(Map.of("names", List.of(name), "category", defaultCategory));

        tag.push();
        return tag;
    }
    public List<SzurubooruTag> searchTag(String searchQuery) throws IOException, SzurubooruHTTPException {
        return searchTag(searchQuery, 20, false);
    }
    public List<SzurubooruTag> searchTag(String searchQuery, int pageSize, boolean eagerLoad) throws IOException, SzurubooruHTTPException {
        try{
            return SzurubooruSearch.searchGeneric(this, searchQuery, SzurubooruTag.class, pageSize, eagerLoad);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            return new ArrayList<>();
        }
    }

    public List<SzurubooruSearchResult> searchByImage(FileToken image)  throws IOException, SzurubooruHTTPException{
        return searchByImage(image, false);
    }
    public List<SzurubooruSearchResult> searchByImage(FileToken image, boolean eagerLoad) throws IOException, SzurubooruHTTPException {
        Map<String, String> urlQuery;

        try {
            urlQuery = Map.of("fields", String.join(",", SzurubooruPost.class
                    .getDeclaredConstructor()
                    .newInstance()
                    .lazyLoadComponents()));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            urlQuery = null;
        }

        Map<String, Object> result = this.call("POST",
                List.of("posts", "reverse-search"),
                eagerLoad? null : urlQuery,
                Map.of("contentToken", image.getToken()));

        List<SzurubooruSearchResult> ret = new ArrayList<>(((List<?>) result.get("similarPosts")).stream()
                .map(x -> {
                    Map<String, Object> searchResult = (Map<String, Object>) x;
                    return new SzurubooruSearchResult(
                        new SzurubooruPost(this, (Map<String, Object>) searchResult.get("post")),
                        (float)searchResult.get("distance"),
                        false);
                })
                .toList());

        if(result.containsKey("exactPost")){
            ret.add(0, new SzurubooruSearchResult(
                    new SzurubooruPost(this, (Map<String, Object>) result.get("exactPost")),
                    0.0f,
                    true));
        }

        return ret;
    }

    protected static String stripTrailing(String str, String trailing){
        if(str.equals(trailing)) return "";
        if(str.endsWith(trailing)){
            return str.substring(0, str.length()-trailing.length()-1);
        }
        return str;
    }
    protected HttpRequest createHttpRequest(String method, String url) {
        if(method.equalsIgnoreCase("get")){
            return new HttpGet(url);
        }
        if(method.equalsIgnoreCase("put")){
            return new HttpPut(url);
        }
        if(method.equalsIgnoreCase("delete")){
            return new HttpDelete(url);
        }
        if(method.equalsIgnoreCase("post")){
            return new HttpPost(url);
        }

        return null;
    }

    public String toString(){
        return "Szurubooru API for " + this.username + " at " + this.apiNetLocation;
    }
}
