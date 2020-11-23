package schwimmen.messages;

import schwimmen.SchwimmenGame.GAMEPHASE;
import schwimmen.SchwimmenPlayer;

public class GamePhase {

    public final String action = "gamePhase";
    public String phase;
    public String actor;
    public PlayerMove moveResult;
    public DiscoverMessage discoverMessage;
    public boolean changeStackAllowed;
    public boolean knockAllowed;

    public GamePhase(GAMEPHASE phase) {
        this(phase, null);
    }

    public GamePhase(GAMEPHASE phase, SchwimmenPlayer actor) {
        this(phase, actor, false, false);
    }

    public GamePhase(GAMEPHASE phase, SchwimmenPlayer actor, boolean changeStackAllowed, boolean knockAllowed) {
        this.phase = phase.name();
        this.actor = actor != null ? actor.getName() : "";
        this.changeStackAllowed = changeStackAllowed;
        this.knockAllowed = knockAllowed;
    }

    public GamePhase(PlayerMove move, SchwimmenPlayer actor) {
        this(GAMEPHASE.moveResult, actor);
        this.moveResult = move;
    }

    public GamePhase(DiscoverMessage discoverMessage, SchwimmenPlayer actor) {
        this(GAMEPHASE.discover, actor);
        this.discoverMessage = discoverMessage;
    }
}
