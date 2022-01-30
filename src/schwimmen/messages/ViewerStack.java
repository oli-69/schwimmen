package schwimmen.messages;

import cardgame.messages.CardStack;
import cardgame.Player;

public class ViewerStack extends CardStack {

    public String name;

    public ViewerStack(Player player) {
        super(player.getStack());
        this.name = player.getName();
    }

}
