/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package schwimmen;

import cardgame.Player;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 *
 */
public class SchwimmenPlayer extends Player {

    public static final String PROP_ONLINE = "online";
    public static final String PROP_LOGOUT = "logout";
    public static final String PROP_SOCKETMESSAGE = "socketMessage";

    private final PropertyChangeListener socketListener;
    private SchwimmenSocket socket;
    private int gameTokens = 3;
    private int totalTokens = 0;

    public SchwimmenPlayer(String name) {
        super(name);
        socketListener = this::socketPropertyChanged;
    }

    public SchwimmenPlayer(String name, SchwimmenSocket socket) {
        this(name);
        this.socket = socket;
        socket.addPropertyChangeListener(socketListener);
    }

    public void reset() {
        clearStack();
        gameTokens = 3;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void addTotalTokens(int tokens) {
        totalTokens += tokens;
    }

    public void removeTotalTokens(int tokens) {
        totalTokens -= tokens;
    }

    public int getGameTokens() {
        return gameTokens;
    }

    public int decreaseToken() {
        --gameTokens;
        return gameTokens;
    }

    public SchwimmenSocket getSocket() {
        return socket;
    }

    public boolean isOnline() {
        return socket != null && socket.isConnected();
    }

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
