package schwimmen.messages;

import schwimmen.SchwimmenPlayer;

public class AskForCardView {

    public final String action = "askForCardView";

    public String source;
    public int hashCode;

    public AskForCardView(SchwimmenPlayer source) {
        this.source = source.getName();
        this.hashCode = super.hashCode();
    }
}
