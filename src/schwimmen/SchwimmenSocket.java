package schwimmen;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import schwimmen.messages.LoginError;
import schwimmen.messages.LoginSuccess;

/**
 *
 */
@WebSocket
public class SchwimmenSocket {

    public static final String PROP_CONNECTED = "connected";
    public static final String PROP_LOGOFF = "logoff";
    public static final String PROP_MESSAGE = "message";

    private static final Logger LOGGER = LogManager.getLogger(SchwimmenSocket.class);

    private final PropertyChangeSupport propChangeSupport;
    private final SchwimmenGame game;
    private final String configPath;
    private final JsonParser jsonParser;
    private final Gson gson;

    private Session session;
    private boolean connected = false;

    public SchwimmenSocket(SchwimmenGame game, String configPath) {
        this.game = game;
        this.configPath = configPath;
        jsonParser = new JsonParser();
        gson = new Gson();
        propChangeSupport = new PropertyChangeSupport(this);
    }

    SchwimmenSocket(SchwimmenGame game) {
        this(game, System.getProperty("user.dir"));
    }

    public void addPropertyChangeListener(PropertyChangeListener pl) {
        propChangeSupport.addPropertyChangeListener(pl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pl) {
        propChangeSupport.removePropertyChangeListener(pl);
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) throws IOException {
        try {
            LOGGER.debug("Message received:" + message);
            if (session.isOpen()) {
                JsonObject jsonObj = jsonParser.parse(message).getAsJsonObject();
                String action = jsonObj.get("action").getAsString();
                switch (action) {
                    case "login":
                        onLogin(jsonObj);
                        break;
                    case "logoff":
                        onLogoff(jsonObj);
                        break;
                    default:
                        propChangeSupport.firePropertyChange(PROP_MESSAGE, null, new SocketMessage(action, message, jsonObj));
                }
            }
        } catch (Exception e) {
            LOGGER.error("onText() failed: ", e);
        }
    }

    @OnWebSocketConnect
    public void onConnect(Session session) throws IOException {
        this.session = session;
        connected = true;
        propChangeSupport.firePropertyChange(PROP_CONNECTED, Boolean.FALSE, Boolean.TRUE);
        LOGGER.info(session.getRemoteAddress().getHostString() + " connected!");
    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        connected = false;
        propChangeSupport.firePropertyChange(PROP_CONNECTED, Boolean.TRUE, Boolean.FALSE);
        LOGGER.info(session.getRemoteAddress().getHostString() + " closed!");
    }

    public void close() {
        try {
            closeSession(session);
        } catch (Exception e) {
            onClose(session, 0, ""); // force the event to be thrown.
        }
    }

    public void sendString(String buff) {
        try {
            if (session.isOpen()) {
                session.getRemote().sendString(buff);
            }
        } catch (IOException ex) {
            LOGGER.error("sendString", ex);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    private void onLogoff(JsonObject jsonObj) {
        propChangeSupport.firePropertyChange(PROP_LOGOFF, null, Boolean.TRUE);
    }

    private void closeSession(Session session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    private void onLogin(JsonObject jsonObj) {
        String name = jsonObj.get("name").getAsString();
        byte[] pwd = jsonObj.get("pwd").getAsString().getBytes();
        if (validateLoginData(name, pwd)) {
            SchwimmenPlayer player = game.getPlayer(name);
            if (player == null) {
                player = new SchwimmenPlayer(name, this);
                game.addPlayerToRoom(player);
                sendString(gson.toJson(game.getGameState(player)));
                sendString(gson.toJson(new LoginSuccess()));
            } else {
                closeSession(player.getSocket().session);
                player.setSocket(this);
                sendString(gson.toJson(new LoginSuccess()));
            }
        }
        LOGGER.info("LOGIN " + name);
    }

    private boolean validateLoginData(String name, byte[] pwd) {
        String errorMsg = null;
        Properties usersProps = new Properties();
        try {
            String path = configPath + File.separator + "users.properties";
            usersProps.load(new FileInputStream(path));
            String userPwd = usersProps.getProperty(name);
            if (userPwd != null) {
                if (!userPwd.equals(new String(pwd))) {
                    errorMsg = "badPwd";
                }
            } else {
                errorMsg = "badUser";
            }
        } catch (Exception ex) {
            LOGGER.error("Unable to read user properties", ex);
            errorMsg = "internalServerError";
        }
        if (errorMsg != null) {
            sendString(gson.toJson(new LoginError(errorMsg)));
            return false;
        }
        return true;
    }

}
