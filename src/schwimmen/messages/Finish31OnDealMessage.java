package schwimmen.messages;

import cardgame.messages.CardStack;
import cardgame.Player;
import java.util.List;

public class Finish31OnDealMessage {

    public String finisher;
    public CardStack finishStack;

    public Finish31OnDealMessage(Player finisher, List<cardgame.Card> finishStack) {
        this.finisher = finisher.getName();
        this.finishStack = new CardStack(finishStack);
    }

}
