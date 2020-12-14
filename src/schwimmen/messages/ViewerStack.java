package schwimmen.messages;

import schwimmen.SchwimmenPlayer;

public class ViewerStack extends CardStack {

    public String name;

    public ViewerStack(SchwimmenPlayer player) {
        super(player.getStack());
        this.name = player.getName();
    }

}
