package schwimmen.messages;

public class LoginSuccess {

    public final String action = "loginSuccess";
    
    public String videoRoomName;

    public LoginSuccess(String videoRoomName) {
        this.videoRoomName = videoRoomName;
    }
    
    
}
