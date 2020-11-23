package schwimmen.messages;

import schwimmen.SchwimmenPlayer;

public class PlayerOnline extends Player {

    public final String action = "playerOnline";

    public PlayerOnline(SchwimmenPlayer player) {
        super(player);
    }

}
