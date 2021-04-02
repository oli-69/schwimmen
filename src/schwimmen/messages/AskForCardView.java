package schwimmen.messages;

import cardgame.Player;

public class AskForCardView {

    public final String action = "askForCardView";

    public String source;
    public int hashCode;

    public AskForCardView(Player source) {
        this.source = source.getName();
        this.hashCode = super.hashCode();
    }
}
