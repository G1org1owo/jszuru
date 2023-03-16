import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) {
        try{
            new jszuru.API("https://testbooru.com/", "g1org1o", "owo", "owo", "/api");
        } catch (MalformedURLException | URISyntaxException e){
            e.printStackTrace();
        }
    }
}