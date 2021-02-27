package schwimmen.messages;

import java.util.List;
import schwimmen.SchwimmenPlayer;

public class Finish31OnDealMessage {

    public String finisher;
    public CardStack finishStack;

    public Finish31OnDealMessage(SchwimmenPlayer finisher, List<cardgame.Card> finishStack) {
        this.finisher = finisher.getName();
        this.finishStack = new CardStack(finishStack);
    }

}
