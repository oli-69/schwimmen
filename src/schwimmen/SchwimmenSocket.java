package schwimmen;

import cardgame.Player;
import cardgame.PlayerSocket;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * This class implements the player's web socket.
 */
@WebSocket
public class SchwimmenSocket extends PlayerSocket {

    public SchwimmenSocket(SchwimmenGame game, String configPath) {
        super(game, configPath);
    }

    SchwimmenSocket(SchwimmenGame game) {
        super(game, System.getProperty("user.dir"));
    }

    @Override
    protected Player createPlayer(String name) {
        return new SchwimmenPlayer(name, this);
    }

}
