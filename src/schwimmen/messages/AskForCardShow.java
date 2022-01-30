package schwimmen.messages;

import cardgame.Player;

public class AskForCardShow {

    public final String action = "askForCardShow";

    public String source;
    public int hashCode;

    public AskForCardShow(Player source) {
        this.source = source.getName();
        this.hashCode = super.hashCode();
    }
}
