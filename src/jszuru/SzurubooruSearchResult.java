package jszuru;

@SuppressWarnings("unused")
public class SzurubooruSearchResult {
    protected SzurubooruPost post;
    protected float distance;
    protected boolean exact;

    public SzurubooruSearchResult(SzurubooruPost post, float distance, boolean exact){
        this.post = post;
        this.distance = distance;
        this.exact = exact;
    }

    public SzurubooruPost getPost() {
        return post;
    }
    public float getDistance() {
        return distance;
    }
    public boolean isExact() {
        return exact;
    }
}
