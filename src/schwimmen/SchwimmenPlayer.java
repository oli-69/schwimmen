package schwimmen;

import cardgame.Player;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This class represents a game player.
 */
public class SchwimmenPlayer extends Player {

    public static final String PROP_ONLINE = "online";
    public static final String PROP_LOGOUT = "logout";
    public static final String PROP_SOCKETMESSAGE = "socketMessage";

    private final PropertyChangeListener socketListener;
    private SchwimmenSocket socket;
    private int gameTokens = 3;
    private int totalTokens = 0;

    /**
     * Constructor. Creates a new player from given value.
     *
     * @param name name of the player.
     */
    public SchwimmenPlayer(String name) {
        super(name);
        socketListener = this::socketPropertyChanged;
    }

    /**
     * Constructor. Creates a new player from given values.
     *
     * @param name the name of the player.
     * @param socket the websockt of the player.
     */
    public SchwimmenPlayer(String name, SchwimmenSocket socket) {
        this(name);
        this.socket = socket;
        socket.addPropertyChangeListener(socketListener);
    }

    /**
     * Resets the state. Usually called to begin of each round.
     */
    public void reset() {
        clearStack();
        gameTokens = 3;
    }

    /**
     * Getter for property total tokens.
     *
     * @return the number of the tokens over all games.
     */
    public int getTotalTokens() {
        return totalTokens;
    }

    /**
     * Adds an amount of tokens.
     *
     * @param tokens the amount of tokens to add.
     */
    public void addTotalTokens(int tokens) {
        totalTokens += tokens;
    }

    /**
     * Remove an amount of game tokens.
     *
     * @param tokens the amount of tokens to remove.
     */
    public void removeTotalTokens(int tokens) {
        totalTokens -= tokens;
    }

    /**
     * Get the amount of tokens in the current game.
     *
     * @return -1=death, 0=swimming, 1, 2 or 3 otherwise.
     */
    public int getGameTokens() {
        return gameTokens;
    }

    /**
     * Decrease the number of game tokens. (If the player is a/the payer of a
     * round).
     *
     * @return the new number of game tokens after decreasing.
     */
    public int decreaseToken() {
        --gameTokens;
        return gameTokens;
    }

    /**
     * Getter for property socket.
     *
     * @return the player's webSocket.
     */
    public SchwimmenSocket getSocket() {
        return socket;
    }

    /**
     * Getter for property isOnline.
     *
     * @return true if the player's connection is ok, false otherwise.
     */
    public boolean isOnline() {
        return socket != null && socket.isConnected();
    }

    /**
     * Setter for property socket.
     *
     * @param socket the new webSocket.
     */
    public void setSocket(SchwimmenSocket socket) {
        if (socket != null) {
            socket.removePropertyChangeListener(socketListener);
        }
        this.socket = socket;
        if (socket != null) {
            socket.addPropertyChangeListener(socketListener);
            firePropertyChange(PROP_ONLINE, Boolean.FALSE, Boolean.TRUE);
        }
    }

    private void socketPropertyChanged(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case SchwimmenSocket.PROP_CONNECTED:
                firePropertyChange(PROP_ONLINE, evt.getOldValue(), evt.getNewValue());
                break;
            case SchwimmenSocket.PROP_LOGOFF:
                firePropertyChange(PROP_LOGOUT, null, this);
                break;
            case SchwimmenSocket.PROP_MESSAGE:
                firePropertyChange(PROP_SOCKETMESSAGE, null, evt.getNewValue());
                break;
        }
    }
}
