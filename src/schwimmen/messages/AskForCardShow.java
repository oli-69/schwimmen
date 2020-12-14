package schwimmen.messages;

import schwimmen.SchwimmenPlayer;

public class AskForCardShow {

    public final String action = "askForCardShow";

    public String source;
    public int hashCode;

    public AskForCardShow(SchwimmenPlayer source) {
        this.source = source.getName();
        this.hashCode = super.hashCode();
    }
}
