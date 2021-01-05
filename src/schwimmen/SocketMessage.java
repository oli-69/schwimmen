
package schwimmen;

import com.google.gson.JsonObject;

/**
 * Message from Socket to Game
 */
public class SocketMessage {
    
    public String action;
    public String jsonString;
    public JsonObject jsonObject;

    public SocketMessage(String action, String jsonString, JsonObject jsonObject) {
        this.action = action;
        this.jsonString = jsonString;
        this.jsonObject = jsonObject;
    }    
}
