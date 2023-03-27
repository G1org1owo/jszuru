package jszuru;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import jszuru.exceptions.SzurubooruHTTPException;
import jszuru.exceptions.SzurubooruResourceNotSynchronizedException;
import jszuru.resources.*;
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

            String errorName = (String) responseMap.get("name");
            String errorDescription = (String) responseMap.get("description");

            if(errorName == null || errorDescription == null){
                throw new SzurubooruHTTPException(content);
            }

            throw new SzurubooruHTTPException(errorName, errorDescription);
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
    public void deletePost(int id) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        try{
            this.getPost(id)
                .delete();
        } catch( SzurubooruHTTPException e){
            if(!e.getErrorName().equals("PostNotFoundError")){
                throw e;
            }
        }
    }
    public List<SzurubooruPost> getAroundPost(int id) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        return this.getPost(id).getAround();
    }
    public SzurubooruPost mergePosts(int source, int target) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        return mergePosts(source, target, false);
    }
    public SzurubooruPost mergePosts(int source, int target, boolean replaceContent) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruPost sourcePost = this.getPost(source);
        SzurubooruPost targetPost = this.getPost(target);

        targetPost.mergeFrom(sourcePost, replaceContent);

        return targetPost;
    }
    public SzurubooruPost getFeaturedPost() throws IOException, SzurubooruHTTPException {
        Map<String, Object> data = this.call("GET", List.of("featured-post"));

        return new SzurubooruPost(this, data);
    }
    public void setFeaturedPost(int id) throws IOException, SzurubooruHTTPException {
        this.call("POST", List.of("featured-post"), null, Map.of("id", id));
    }

    public SzurubooruTag getTag(String id) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruTag tag = new SzurubooruTag(this, Map.of("names", List.of(id)));
        tag.pull();
        return tag;
    }
    public SzurubooruTag createTag(String name) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruTagCategory defaultCategory = this.getDefaultTagCategory();

        SzurubooruTag tag = new SzurubooruTag(this, new HashMap<>());
        tag.setNewJson(Map.of("names", List.of(name), "category", defaultCategory.getName()));

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
    public void deleteTag(String name) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        try{
            this.getTag(name)
                .delete();
        } catch (SzurubooruHTTPException e){
            if(!e.getErrorName().equals("TagNotFoundError")){
                throw e;
            }
        }
    }
    public SzurubooruTag mergeTags(String source, String target) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        return mergeTags(source, target, false);
    }
    public SzurubooruTag mergeTags(String source, String target, boolean addAsAlias) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruTag sourceTag = this.getTag(source);
        SzurubooruTag targetTag = this.getTag(target);

        targetTag.mergeFrom(sourceTag, addAsAlias);

        return targetTag;
    }
    public List<SzurubooruTag> listTagSiblings(String name) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruTag tag = getTag(name);
        return tag.getSiblings();
    }

    public SzurubooruTagCategory getTagCategory(String name) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruTagCategory tagCategory = new SzurubooruTagCategory(this, Map.of("name", name));
        tagCategory.pull();
        return tagCategory;
    }
    public SzurubooruTagCategory getDefaultTagCategory() throws IOException, SzurubooruHTTPException {
        return this.listTagCategories()
                .stream()
                .filter(x -> x.isDefault())
                .findFirst()
                .orElse(new SzurubooruTagCategory(this, Map.of("name", "default")));
    }
    public SzurubooruTagCategory createTagCategory(String name) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruTagCategory tagCategory = new SzurubooruTagCategory(this, new HashMap<>());
        tagCategory.setNewJson(Map.of("name", name, "color", "default", "order", 1));

        tagCategory.push();
        return tagCategory;
    }
    public List<SzurubooruTagCategory> listTagCategories() throws IOException, SzurubooruHTTPException {
        try{
            return SzurubooruSearch.searchUnpaged(this, SzurubooruTagCategory.class);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            return new ArrayList<>();
        }
    }
    public void setDefaultTagCategory(String name) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        this.getTagCategory(name)
            .setDefault();
    }
    public void deleteTagCategory(String name) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        try{
            this.getTagCategory(name)
                .delete();
        } catch (SzurubooruHTTPException e){
            if(!e.getErrorName().equals("TagCategoryNotFoundError")){
                throw e;
            }
        }
    }

    public SzurubooruPoolCategory getPoolCategory(String name) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruPoolCategory poolCategory = new SzurubooruPoolCategory(this, Map.of("name", name));
        poolCategory.pull();
        return poolCategory;
    }
    public SzurubooruPoolCategory getDefaultPoolCategory() throws IOException, SzurubooruHTTPException {
        return this.listPoolCategories()
                .stream()
                .filter(x -> x.isDefault())
                .findFirst()
                .orElse(new SzurubooruPoolCategory(this, Map.of("name", "default")));
    }
    public SzurubooruPoolCategory createPoolCategory(String name) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruPoolCategory poolCategory = new SzurubooruPoolCategory(this, new HashMap<>());
        poolCategory.setNewJson(Map.of("name", name, "color", "default"));

        poolCategory.push();
        return poolCategory;
    }
    public List<SzurubooruPoolCategory> listPoolCategories() throws IOException, SzurubooruHTTPException {
        try{
            return SzurubooruSearch.searchUnpaged(this, SzurubooruPoolCategory.class);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            return new ArrayList<>();
        }
    }
    public void setDefaultPoolCategory(String name) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        this.getPoolCategory(name)
                .setDefault();
    }
    public void deletePoolCategory(String name) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        try{
            this.getPoolCategory(name)
                    .delete();
        } catch (SzurubooruHTTPException e){
            if(!e.getErrorName().equals("PoolCategoryNotFoundError")){
                throw e;
            }
        }
    }

    public SzurubooruPool getPool(int id) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruPool pool = new SzurubooruPool(this, Map.of("id", id));
        pool.pull();

        return pool;
    }
    public SzurubooruPool createPool(String name) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruPool pool = new SzurubooruPool(this, new HashMap<>());
        pool.setNewJson(Map.of(
                "names", List.of(name),
                "category", this.getDefaultPoolCategory().getName()
        ));

        pool.push();
        return pool;
    }
    public List<SzurubooruPool> searchPool(String searchQuery) throws IOException, SzurubooruHTTPException {
        return searchPool(searchQuery, 20, false);
    }
    public List<SzurubooruPool> searchPool(String searchQuery, int pageSize, boolean eagerLoad) throws IOException, SzurubooruHTTPException {
        try{
            return SzurubooruSearch.searchGeneric(this, searchQuery, SzurubooruPool.class, pageSize, eagerLoad);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public SzurubooruPool mergePools(int source, int target) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        return mergePools(source, target, false);
    }
    public SzurubooruPool mergePools(int source, int target, boolean addAsAlias) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruPool sourcePool = this.getPool(source);
        SzurubooruPool targetPool = this.getPool(target);

        targetPool.mergeFrom(sourcePool, addAsAlias);

        return targetPool;
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

        List<?> similarPosts = (List<?>) result.get("similarPosts");
        List<SzurubooruSearchResult> ret = new ArrayList<>(similarPosts.stream()
                .filter(x -> ((Double)((Map<String, Object>)x).get("distance")) != 0.0d)
                .map(x -> {
                    Map<String, Object> searchResult = (Map<String, Object>) x;
                    return new SzurubooruSearchResult(
                        new SzurubooruPost(this, (Map<String, Object>) searchResult.get("post")),
                        (Double)searchResult.get("distance"),
                        false);
                })
                .toList());

        if(result.get("exactPost") != null){
            ret.add(0, new SzurubooruSearchResult(
                    new SzurubooruPost(this, (Map<String, Object>) result.get("exactPost")),
                    0.0d,
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
            return new HttpPost(url){
                @Override
                public String getMethod() {
                    return "DELETE";
                }
            };
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
