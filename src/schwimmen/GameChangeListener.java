package schwimmen;

import com.google.gson.Gson;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import schwimmen.SchwimmenGame.GAMEPHASE;
import schwimmen.messages.AttendeeList;
import schwimmen.messages.GamePhase;
import schwimmen.messages.PlayerList;
import schwimmen.messages.PlayerOnline;
import schwimmen.messages.ViewerMap;

/**
 * This class is a listener to class Game. It listens to change events and
 * synchronizes the clients by sending appropriate messages. This part of code
 * was placed here just to take some code out from the game class. (even this
 * isn't a good design).
 */
public class GameChangeListener implements PropertyChangeListener {

    private final SchwimmenGame game;
    private final Gson gson;

    /**
     * Constructor. Creates an instance of this class.
     *
     * @param source the game source to which it belongs.
     */
    public GameChangeListener(SchwimmenGame source) {
        game = source;
        gson = new Gson();
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case SchwimmenGame.PROP_WEBRADIO_PLAYING:
                game.sendToPlayers("{\"action\":\"playWebradio\", \"play\": " + ((Boolean) evt.getNewValue()) + "}");
                break;
            case SchwimmenGame.PROP_ATTENDEESLIST:
                game.sendToPlayers(gson.toJson(new AttendeeList((List<SchwimmenPlayer>) evt.getNewValue(), game.getAllAttendees(), game.getMover())));
                break;
            case SchwimmenGame.PROP_PLAYERLIST:
                game.sendToPlayers(gson.toJson(new PlayerList((List<SchwimmenPlayer>) evt.getNewValue())));
                break;
            case SchwimmenGame.PROP_PLAYER_ONLINE:
                SchwimmenPlayer player = (SchwimmenPlayer) evt.getNewValue();
                if (player.isOnline()) {
                    player.getSocket().sendString(gson.toJson(game.getGameState(player)));
                }
                game.sendToPlayers(gson.toJson(new PlayerOnline(player)));
                break;
            case SchwimmenGame.PROP_GAMEPHASE:
                processGamePhase(evt);
                break;
            case SchwimmenGame.PROP_VIEWER_MAP:
                game.sendToPlayers(gson.toJson(new ViewerMap(game.getViewerMap())));
        }
    }

    private GamePhase getMessageForPhase(GAMEPHASE phase) {
        SchwimmenPlayer actor = game.getMover();
        switch (phase) {
            case discover:
                return new GamePhase(game.getDiscoverMessage(), actor);
            case finish31OnDeal:
                return new GamePhase(game.getFinish31OnDealMessage(), actor);
            case moveResult:
                return new GamePhase(game.getPlayertMove(), actor);
            case waitForPlayerMove:
                return new GamePhase(phase, actor, game.isChangeStackAllowed(actor), game.isKnockAllowed(), game.isPassAllowed(actor));
            case shuffle:
                return new GamePhase(phase, actor, game.getAllAttendees());
            default:
                return new GamePhase(phase, actor);
        }
    }

    private void processGamePhase(PropertyChangeEvent evt) {
        GAMEPHASE phase = (GAMEPHASE) evt.getNewValue();
        if (phase == GAMEPHASE.moveResult || phase == GAMEPHASE.dealCards) {
            // send an individual message to the clients which include the viewer stacks
            game.getPlayerList().forEach(p -> {
                GamePhase message = getMessageForPhase(phase);
                message.viewerStackList = game.getViewerStackList(p);
                p.getSocket().sendString(gson.toJson(message));
            });

        } else {
            game.sendToPlayers(gson.toJson(getMessageForPhase(phase)));
        }
    }

}
