package schwimmen.messages;

import java.util.List;

public class PlayerStack extends CardStack{

    public final String action = "playerStack";

    public PlayerStack(List<cardgame.Card> cards) {
        super(cards);
    }

}
