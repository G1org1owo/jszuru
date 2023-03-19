package jszuru.resources;

import jszuru.SzurubooruAPI;
import jszuru.exceptions.SzurubooruException;
import jszuru.exceptions.SzurubooruHTTPException;
import jszuru.exceptions.SzurubooruResourceNotSynchronizedException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public class SzurubooruPost extends SzurubooruResource {
    public SzurubooruPost(SzurubooruAPI api, Map<String, Object> initialJson){
        super(api, initialJson);
    }

    public static boolean isValidSafety(String safety){
        return safety.equalsIgnoreCase("safe") ||
            safety.equalsIgnoreCase("sketchy") ||
            safety.equalsIgnoreCase("unsafe");
    }
    protected SzurubooruTag stringToTag(String value) throws IOException, SzurubooruHTTPException, SzurubooruResourceNotSynchronizedException {
        SzurubooruTag tag = new SzurubooruTag(api, Map.of("names", List.of(value)));
        tag.pull(); // Might cause some trouble when converting the name of a tag that is yet to be created
        return tag;
    }

    @Override
    public List<String> getInstanceUrlParts() {
        return List.of("post", ((Double)json.get("id")).intValue() + "");
    }
    @Override
    public List<String> getClassUrlParts() {
        return List.of("posts");
    }

    @Override
    public List<String> lazyLoadComponents() {
        return List.of("id", "safety", "type", "contentUrl", "flags", "tags", "relations");
    }

    @Override
    protected Map<String, Function<Object, Object>> getterTransforms() {
        Function<Object, Object> lambdaTags = (x) -> new SzurubooruTag(api, (Map<String, Object>) x);
        Function<Object, Object> lambdaRelations = (x) -> new SzurubooruPost(api, (Map<String, Object>) x);

        return Map.of("tags", lambdaTags, "relations", lambdaRelations);
    }
    @Override
    protected Map<String, Function<Object, Object>> setterTransforms() {
        Function<Object, Object> lambdaTags = (x) -> {
            SzurubooruTag tag = (SzurubooruTag) x;
            return Map.of("names", ((SzurubooruTag) x).getNames(), "category", tag.getCategory());
        };
        Function<Object, Object> lambdaRelations = (x) -> {
            SzurubooruPost post = (SzurubooruPost) x;
            return Map.of("id", post.getId());
        };

        return Map.of("tags", lambdaTags, "relations", lambdaRelations);
    }

    @Override
    protected Map<String, Object> serialized() {
        Map<String, Object> ret = this.copyNewJson(List.of(
                "tags",
                "safety",
                "source",
                "relations",
                "flags",
                "contentToken",
                "thumbnailToken",
                "notes"
        ));

        Function<List<Map<String, Object>>, List<Integer>> lambdaRelations = x ->
                x.stream()
                    .map(y -> ((Double)y.get("id")).intValue())
                    .toList();

        if(ret.containsKey("tags")){
            List<Map<String, Object>> tags = (List<Map<String, Object>>) ret.get("tags");
            ret.put("tags", getPrimaryNames(tags));
        }

        if(ret.containsKey("relations")){
            List<Map<String, Object>> relations = (List<Map<String, Object>>) ret.get("relations");
            ret.put("relations", lambdaRelations.apply(relations));
        }

        return ret;
    }

    public int getId(){
        try {
            return ((Double)this.genericGetter("id")).intValue();
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return -1;
        }
    }
    public SzurubooruPost setId(int id){
        try{
            this.genericSetter("id", id);
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
        }

        return this;
    }

    public String getSafety(){
        try{
            return (String) this.genericGetter("safety");
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruPost setSafety(String safety){
        try{
            this.genericSetter("safety", safety);
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
        }

        return this;
    }

    public String[] getSource(){
        try{
            return ((String) this.genericGetter("source")).split("\n");
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruPost setSource(String[] source){
        try{
            this.genericSetter("source", String.join("\n", source));
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
        }

        return this;
    }
    public SzurubooruPost setSource(List<String> source){
        try{
            this.genericSetter("source", String.join("\n", source));
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
        }

        return this;
    }
    public SzurubooruPost setSource(String source){
        try{
            this.genericSetter("source", source);
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
        }

        return this;
    }

    public List<SzurubooruTag> getTags(){
        try{
            return new ArrayList<>((List<SzurubooruTag>) this.genericGetter("tags"));
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruPost setTags(List<SzurubooruTag> tags){
        try{
            this.genericSetter("tags", tags);
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
        }

        return this;
    }

    public List<SzurubooruPost> getRelations(){
        try{
            return new ArrayList<>((List<SzurubooruPost>) this.genericGetter("relations"));
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruPost setRelations(List<SzurubooruPost> relations){
        try{
            this.genericSetter("relations", relations);
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
        }

        return this;
    }

    public String getContent(){
        try{
            return this.fileGetter("content");
        } catch (IOException | SzurubooruException | URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruPost setContent(FileToken content){
        this.fileSetter("content", content);

        return this;
    }

    public String getThumbnail(){
        try{
            return this.fileGetter("thumbnail");
        } catch (IOException | SzurubooruException | URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruPost setThumbnail(FileToken thumbnail){
        this.fileSetter("thumbnail", thumbnail);

        return this;
    }

    public String getType(){
        try{
            return (String) this.genericGetter("type");
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getMime(){
        try{
            return (String) this.genericGetter("mime");
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getChecksum(){
        try{
            return (String) this.genericGetter("checksum");
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getWidth(){
        try{
            return ((Double)this.genericGetter("width")).intValue();
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return -1;
        }
    }
    public int getHeight(){
        try{
            return ((Double)this.genericGetter("height")).intValue();
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return -1;
        }
    }

    protected boolean flagGetter(String flagName){
        try{
            List<String> flagList = (List<String>) this.genericGetter("flags");
            return flagList.contains(flagName);
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return false;
        }

    }
    protected boolean flagSetter(String flagName, boolean value) {
        try{
            List<String> flagList = (List<String>) this.genericGetter("flags");

            if(value && !flagList.contains(flagName)){
                flagList.add(flagName);
            }
            else if(!value){
                flagList.remove(flagName);
            }

            this.genericSetter("flags", flagList, false);
            return false;
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return true;
        }
    }

    public boolean loop() {
        return this.flagGetter("loop");
    }
    public SzurubooruPost loop(boolean value){
        if(this.flagSetter("loop", value)) return null;
        return this;
    }

    public List<SzurubooruPostNote> getNotes(){
        try{
            return new ArrayList<>((List<SzurubooruPostNote>) this.genericGetter("notes"));
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
            return null;
        }
    }
    public SzurubooruPost setNotes(List<SzurubooruPostNote> notes){
        try{
            this.genericSetter("notes", notes.stream().map(SzurubooruPostNote::json).toList());
        } catch (IOException | SzurubooruException e) {
            e.printStackTrace();
        }

        return this;
    }

    public boolean sound() {
        return this.flagGetter("sound");
    }
    public SzurubooruPost sound(boolean value){
        if(this.flagSetter("sound", value)) return null;
        return this;
    }

    public String toString(){
        return "Post " + this.getId();
    }
}
