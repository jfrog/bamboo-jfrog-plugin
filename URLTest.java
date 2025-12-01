import java.net.URL;
import java.net.MalformedURLException;

public class URLTest {
    public static void main(String[] args) {
        try {
            new URL("https://");
            System.out.println("https:// is VALID");
        } catch (MalformedURLException e) {
            System.out.println("https:// is INVALID: " + e.getMessage());
        }
    }
}
