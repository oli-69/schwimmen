package schwimmen.messages;

import cardgame.Player;
import schwimmen.SchwimmenGame.GAMEPHASE;

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

    public GamePhase(GAMEPHASE phase, Player actor) {
        this(phase, actor, false, false, true);
    }

    public GamePhase(GAMEPHASE phase, Player actor, int[] allAttendees) {
        this(phase, actor);
        this.allAttendees = allAttendees;
    }

    public GamePhase(GAMEPHASE phase, Player actor, boolean changeStackAllowed, boolean knockAllowed, boolean passAllowed) {
        this.phase = phase.name();
        this.actor = actor != null ? actor.getName() : "";
        this.changeStackAllowed = changeStackAllowed;
        this.knockAllowed = knockAllowed;
        this.passAllowed = passAllowed;
    }

    public GamePhase(PlayerMove move, Player actor) {
        this(GAMEPHASE.moveResult, actor);
        this.moveResult = move;
    }

    public GamePhase(DiscoverMessage discoverMessage, Player actor) {
        this(GAMEPHASE.discover, actor);
        this.discoverMessage = discoverMessage;
    }

    public GamePhase(Finish31OnDealMessage finish31OnDealMessage, Player actor) {
        this(GAMEPHASE.finish31OnDeal, actor);
        this.finish31OnDealMessage = finish31OnDealMessage;
    }
}
