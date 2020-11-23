package schwimmen.messages;

import schwimmen.SchwimmenPlayer;

public class ChatMessage {

    public final String action = "chatMessage";
    public String text;
    public String sender;

    public ChatMessage() {
        this("");
    }

    public ChatMessage(String text) {
        this(text, null);
    }

    public ChatMessage(String text, SchwimmenPlayer sender) {
        this.text = text;
        if (sender != null) {
            this.sender = sender.getName();
        }
    }

}
