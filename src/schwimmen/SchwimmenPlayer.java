package schwimmen;

import cardgame.Player;
import cardgame.PlayerSocket;

/**
 * This class represents a game player.
 */
public class SchwimmenPlayer extends Player {

    /**
     * Constructor. Creates a new player from given value.
     *
     * @param name name of the player.
     */
    public SchwimmenPlayer(String name) {
        super(name);
        gameTokens = 3;
    }

    /**
     * Constructor. Creates a new player from given values.
     *
     * @param name the name of the player.
     * @param socket the websockt of the player.
     */
    public SchwimmenPlayer(String name, PlayerSocket socket) {
        super(name, socket);
        gameTokens = 3;
    }

    /**
     * Resets the state. Usually called to begin of each round.
     */
    @Override
    public void reset() {
        clearStack();
        gameTokens = 3;
    }
}
