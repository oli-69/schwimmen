package schwimmen.messages;

import schwimmen.SchwimmenGame.GAMEPHASE;
import schwimmen.SchwimmenPlayer;

public class GamePhase {

    public final String action = "gamePhase";
    public String phase;
    public String actor;
    public PlayerMove moveResult;
    public DiscoverMessage discoverMessage;
    public Finish31OnDealMessage finish31OnDealMessage;
    public boolean changeStackAllowed;
    public boolean knockAllowed;
    public boolean passAllowed;
    public int[] allAttendees;
    public ViewerStackList viewerStackList;

    public GamePhase(GAMEPHASE phase) {
        this(phase, null);
    }

    public GamePhase(GAMEPHASE phase, SchwimmenPlayer actor) {
        this(phase, actor, false, false, true);
    }

    public GamePhase(GAMEPHASE phase, SchwimmenPlayer actor, int[] allAttendees) {
        this(phase, actor);
        this.allAttendees = allAttendees;
    }

    public GamePhase(GAMEPHASE phase, SchwimmenPlayer actor, boolean changeStackAllowed, boolean knockAllowed, boolean passAllowed) {
        this.phase = phase.name();
        this.actor = actor != null ? actor.getName() : "";
        this.changeStackAllowed = changeStackAllowed;
        this.knockAllowed = knockAllowed;
        this.passAllowed = passAllowed;
    }

    public GamePhase(PlayerMove move, SchwimmenPlayer actor) {
        this(GAMEPHASE.moveResult, actor);
        this.moveResult = move;
    }

    public GamePhase(DiscoverMessage discoverMessage, SchwimmenPlayer actor) {
        this(GAMEPHASE.discover, actor);
        this.discoverMessage = discoverMessage;
    }

    public GamePhase(Finish31OnDealMessage finish31OnDealMessage, SchwimmenPlayer actor) {
        this(GAMEPHASE.finish31OnDeal, actor);
        this.finish31OnDealMessage = finish31OnDealMessage;
    }
}
