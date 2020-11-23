package schwimmen.messages;

import java.util.List;
import schwimmen.SchwimmenPlayer;

public class GameStateMessage {

    public final String action = "gameState";

    public String phase;
    public PlayerList playerList;
    public AttendeeList attendeeList;
    public String mover;
    public CardStack gameStack;
    public CardStack playerStack;
    public boolean changeStackAllowed;
    public boolean knockAllowed;
    public CardStack[] discoverStacks;
    public boolean webradioPlaying;

    public GameStateMessage(String phase, List<SchwimmenPlayer> players, List<SchwimmenPlayer> attendees,
            SchwimmenPlayer mover, List<cardgame.Card> gameStack, List<cardgame.Card> playerStack,
            boolean changeStackAllowed, boolean knockAllowed, List<DiscoverStack> discoverStacks, boolean webradioPlaying) {
        this.phase = phase;
        this.playerList = new PlayerList(players);
        this.attendeeList = new AttendeeList(attendees, mover);
        this.mover = mover != null ? mover.getName() : "";
        this.gameStack = new CardStack(gameStack);
        this.playerStack = new CardStack(playerStack);
        this.changeStackAllowed = changeStackAllowed;
        this.knockAllowed = knockAllowed;
        if (discoverStacks != null) {
            this.discoverStacks = discoverStacks.toArray(new DiscoverStack[discoverStacks.size()]);
        }
        this.webradioPlaying = webradioPlaying;
    }
}
