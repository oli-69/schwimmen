package schwimmen.messages;

public class LoginError {

    public final String action = "loginError";
    public String text;

    public LoginError() {
        this("");
    }

    public LoginError(String text) {
        this.text = text;
    }

}
