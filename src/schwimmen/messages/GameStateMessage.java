package schwimmen.messages;

import cardgame.Player;
import cardgame.messages.AttendeeList;
import cardgame.messages.CardStack;
import cardgame.messages.GameStack;
import cardgame.messages.PlayerList;
import cardgame.messages.WebradioUrl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GameStateMessage {

    public final String action = "gameState";

    public String phase;
    public PlayerList playerList;
    public AttendeeList attendeeList;
    public ViewerMap viewerMap;
    public GameStack gameStack;
    public CardStack playerStack;
    public ViewerStackList viewerStackList;
    public boolean changeStackAllowed;
    public boolean knockAllowed;
    public boolean passAllowed;
    public CardStack[] discoverStacks;
    public Finish31OnDealMessage finish31OnDealMessage;
    public boolean webradioPlaying;
    public WebradioUrl radioUrl;
    public GameRules gameRules;

    public GameStateMessage(String phase, List<Player> players, List<Player> attendees, int[] allAttendees,
            Map<Player, Collection<Player>> viewerMap, Player mover, Player activeAdmin, GameStack gameStack, 
            boolean webradioPlaying, WebradioUrl radioUrl, GameRules gameRules) {
        this(phase, players, attendees, allAttendees, viewerMap, mover, activeAdmin, gameStack,
                new ArrayList<>(), new ViewerStackList(), false, false, false, null, null, 
                webradioPlaying, radioUrl, gameRules);
    }

    public GameStateMessage(String phase, List<Player> players, List<Player> attendees, int[] allAttendees,
            Map<Player, Collection<Player>> viewerMap, Player mover, Player activeAdmin, GameStack gameStack,
            List<cardgame.Card> playerStack, ViewerStackList viewerStackList, boolean changeStackAllowed, boolean knockAllowed,
            boolean passAllowed, List<DiscoverStack> discoverStacks, Finish31OnDealMessage finish31OnDealMessage, 
            boolean webradioPlaying, WebradioUrl radioUrl, GameRules gameRules) {
        this.phase = phase;
        this.playerList = new PlayerList(players, activeAdmin);
        this.attendeeList = new AttendeeList(attendees, allAttendees, mover);
        this.viewerMap = new ViewerMap(viewerMap);
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
        this.radioUrl = radioUrl;
        this.gameRules = gameRules;
    }
}
