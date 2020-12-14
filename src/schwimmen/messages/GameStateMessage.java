package schwimmen.messages;

import java.util.List;
import java.util.Map;
import schwimmen.SchwimmenPlayer;

public class GameStateMessage {

    public final String action = "gameState";

    public String phase;
    public PlayerList playerList;
    public AttendeeList attendeeList;
    public ViewerMap viewerMap;
    public String mover;
    public CardStack gameStack;
    public CardStack playerStack;
    public ViewerStackList viewerStackList;
    public boolean changeStackAllowed;
    public boolean knockAllowed;
    public CardStack[] discoverStacks;
    public boolean webradioPlaying;

    public GameStateMessage(String phase, List<SchwimmenPlayer> players, List<SchwimmenPlayer> attendees, int[] allAttendees,
            Map<SchwimmenPlayer, List<SchwimmenPlayer>> viewerMap, SchwimmenPlayer mover, List<cardgame.Card> gameStack, 
            List<cardgame.Card> playerStack, ViewerStackList viewerStackList, boolean changeStackAllowed, boolean knockAllowed, 
            List<DiscoverStack> discoverStacks, boolean webradioPlaying) {
        this.phase = phase;
        this.playerList = new PlayerList(players);
        this.attendeeList = new AttendeeList(attendees, allAttendees, mover);
        this.viewerMap = new ViewerMap(viewerMap);
        this.mover = mover != null ? mover.getName() : "";
        this.gameStack = new CardStack(gameStack);
        this.playerStack = new CardStack(playerStack);
        this.viewerStackList = viewerStackList;
        this.changeStackAllowed = changeStackAllowed;
        this.knockAllowed = knockAllowed;
        if (discoverStacks != null) {
            this.discoverStacks = discoverStacks.toArray(new DiscoverStack[discoverStacks.size()]);
        }
        this.webradioPlaying = webradioPlaying;
    }
}
