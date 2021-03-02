package schwimmen.messages;

import java.util.ArrayList;
import java.util.Collection;
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
    public GameStack gameStack;
    public CardStack playerStack;
    public ViewerStackList viewerStackList;
    public boolean changeStackAllowed;
    public boolean knockAllowed;
    public boolean passAllowed;
    public CardStack[] discoverStacks;
    public Finish31OnDealMessage finish31OnDealMessage;
    public boolean webradioPlaying;

    public GameStateMessage(String phase, List<SchwimmenPlayer> players, List<SchwimmenPlayer> attendees, int[] allAttendees,
            Map<SchwimmenPlayer, Collection<SchwimmenPlayer>> viewerMap, SchwimmenPlayer mover, GameStack gameStack, boolean webradioPlaying) {
        this(phase, players, attendees, allAttendees, viewerMap, mover, gameStack,
                new ArrayList<>(), new ViewerStackList(), false, false, false, null, null, webradioPlaying);
    }

    public GameStateMessage(String phase, List<SchwimmenPlayer> players, List<SchwimmenPlayer> attendees, int[] allAttendees,
            Map<SchwimmenPlayer, Collection<SchwimmenPlayer>> viewerMap, SchwimmenPlayer mover, GameStack gameStack,
            List<cardgame.Card> playerStack, ViewerStackList viewerStackList, boolean changeStackAllowed, boolean knockAllowed,
            boolean passAllowed, List<DiscoverStack> discoverStacks, Finish31OnDealMessage finish31OnDealMessage, boolean webradioPlaying) {
        this.phase = phase;
        this.playerList = new PlayerList(players);
        this.attendeeList = new AttendeeList(attendees, allAttendees, mover);
        this.viewerMap = new ViewerMap(viewerMap);
        this.mover = mover != null ? mover.getName() : "";
        this.gameStack = gameStack;
        this.playerStack = new CardStack(playerStack);
        this.viewerStackList = viewerStackList;
        this.changeStackAllowed = changeStackAllowed;
        this.knockAllowed = knockAllowed;
        this.passAllowed = passAllowed;
        if (discoverStacks != null) {
            this.discoverStacks = discoverStacks.toArray(new DiscoverStack[discoverStacks.size()]);
        }
        this.finish31OnDealMessage = finish31OnDealMessage;
        this.webradioPlaying = webradioPlaying;
    }
}
