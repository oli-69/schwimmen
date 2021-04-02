package schwimmen.messages;

import cardgame.messages.CardStack;
import cardgame.Player;

public class DiscoverStack extends CardStack {

    public String player;
    public float score;

    public DiscoverStack(Player player, float score) {
        super(player.getStack());
        this.player = player.getName();
        this.score = score;
    }

}
