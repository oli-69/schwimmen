package schwimmen.messages;

import schwimmen.SchwimmenPlayer;

public class DiscoverStack extends CardStack {

    public String player;
    public float score;

    public DiscoverStack(SchwimmenPlayer player, float score) {
        super(player.getStack());
        this.player = player.getName();
        this.score = score;
    }

}
