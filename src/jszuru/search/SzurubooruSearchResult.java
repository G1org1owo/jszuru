package jszuru.search;

import jszuru.resources.SzurubooruPost;

@SuppressWarnings("unused")
public class SzurubooruSearchResult {
    protected SzurubooruPost post;
    protected double distance;
    protected boolean exact;

    public SzurubooruSearchResult(SzurubooruPost post, double distance, boolean exact){
        this.post = post;
        this.distance = distance;
        this.exact = exact;
    }

    public SzurubooruPost getPost() {
        return post;
    }
    public double getDistance() {
        return distance;
    }
    public boolean isExact() {
        return exact;
    }
}
