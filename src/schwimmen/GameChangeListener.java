/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

/**
 *
 */
public class GameChangeListener implements PropertyChangeListener {

    private final SchwimmenGame game;
    private final Gson gson;

    public GameChangeListener(SchwimmenGame source) {
        game = source;
        gson = new Gson();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case SchwimmenGame.PROP_WEBRADIO_PLAYING:
                game.sendToPlayers("{\"action\":\"playWebradio\", \"play\": " + ((Boolean) evt.getNewValue()) + "}");
                break;
            case SchwimmenGame.PROP_ATTENDEESLIST:
                game.sendToPlayers(gson.toJson(new AttendeeList((List<SchwimmenPlayer>) evt.getNewValue(), game.getMover())));
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
                GAMEPHASE phase = (GAMEPHASE) evt.getNewValue();
                game.sendToPlayers(gson.toJson(getMessageForPhase(phase)));
                break;
        }

    }

    private GamePhase getMessageForPhase(GAMEPHASE phase) {
        SchwimmenPlayer actor = getActorForGamePhase(phase);
        switch (phase) {
            case discover:
                return new GamePhase(game.getDiscoverMessage(), actor);
            case moveResult:
                return new GamePhase(game.getPlayertMove(), actor);
            case waitForPlayerMove:
                return new GamePhase(phase, actor, game.isChangeStackAllowed(actor), game.isKnockAllowed());
            default:
                return new GamePhase(phase, actor);
        }
    }

    private SchwimmenPlayer getActorForGamePhase(GAMEPHASE phase) {
        return game.getMover();
    }
}
