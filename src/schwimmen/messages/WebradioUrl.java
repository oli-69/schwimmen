package schwimmen.messages;

public class WebradioUrl {

    public final String action = "radioUrl";
    public String name;
    public String url;

    public WebradioUrl(String name, String url) {
        this.name = name;
        this.url = url;
    }
}
