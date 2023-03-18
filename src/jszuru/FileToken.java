package jszuru;

@SuppressWarnings("unused")
public class FileToken {
    private final String token;
    private final String filepath;

    FileToken(String token, String filepath){
        this.token = token;
        this.filepath = filepath;
    }

    public String getToken() {
        return token;
    }
    public String getFilepath() {
        return filepath;
    }

    public String toString(){
        return "<Upload token for file at " + filepath + ">";
    }
}
