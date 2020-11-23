package schwimmen.messages;

import java.util.List;
import schwimmen.SchwimmenPlayer;

/**
 *
 */
public class PlayerList {

    public final String action = "playerList";
    
    public Player[] players;
    
    public PlayerList() {
        players = new Player[0];
    }

    public PlayerList(List<SchwimmenPlayer> playerList) {
        players = new Player[playerList.size()];
        for( int i=0; i<players.length; i++) {
            players[i] = new Player(playerList.get(i));
        }
    }

}
