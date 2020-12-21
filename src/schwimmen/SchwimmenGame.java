package schwimmen;

import cardgame.Card;
import cardgame.CardGame;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import schwimmen.messages.AskForCardShow;
import schwimmen.messages.AskForCardView;
import schwimmen.messages.CardSwap;
import schwimmen.messages.ChatMessage;
import schwimmen.messages.DiscoverMessage;
import schwimmen.messages.DiscoverStack;
import schwimmen.messages.GameStateMessage;
import schwimmen.messages.PlayerMove;
import schwimmen.messages.PlayerStack;
import schwimmen.messages.StackSwap;
import schwimmen.messages.ViewerStack;
import schwimmen.messages.ViewerStackList;

/**
 * This class implements the game rules and evaluates the player's decisions.
 */
public class SchwimmenGame extends CardGame {

    private static final Logger LOGGER = LogManager.getLogger(SchwimmenGame.class);

    /**
     * Enumeration of the game phases.
     */
    public static enum GAMEPHASE {
        /**
         * The game didn's start yet. Player's can choose to attend the next
         * round, or not.
         */
        waitForAttendees,
        /**
         * The card dealer is shuffling. The player ends this phase by starting
         * to deal cards.
         */
        shuffle,
        /**
         * The card dealer has to select which stack is hold and which goes into
         * the stock in the middle.
         */
        dealCards,
        /**
         * Game is waiting for next player move.
         */
        waitForPlayerMove,
        /**
         * The result of a player move (a player's decision) got caclulated and
         * distributed to the clients.
         */
        moveResult,
        /**
         * Ths game round ended. All cards become discoverd.
         */
        discover
    };

    /**
     * Enumeration of the available player moves.
     */
    public static enum MOVE {
        /**
         * Card dealer selects which stack is hold, the other one goes into the
         * stock in the middle.
         */
        selectStack,
        /**
         * Player swaps a card (one in, one out).
         */
        swapCard,
        /**
         * Player swappes all three cards.
         */
        swapAllCards,
        /**
         * Player passes this round.
         */
        pass,
        /**
         * Player knocks.
         */
        knock,
        /**
         * Player changes ths stock cards in the middle. Allowed if all
         * attendees passed in a row.
         */
        changeStack
    };

    public static final String PROP_GAMEPHASE = "gameState";
    public static final String PROP_ATTENDEESLIST = "attendeesList";
    public static final String PROP_PLAYERLIST = "playerList";
    public static final String PROP_PLAYER_ONLINE = "playerOnline";
    public static final String PROP_VIEWER_MAP = "viewerMap";
    public static final String PROP_WEBRADIO_PLAYING = "webradioPlaying";

    private final PlayerIdComparator playerIdComparator;
    private final List<SchwimmenPlayer> players; // List of all players in the room
    private final List<SchwimmenPlayer> attendees; // sub-list of players, which are actually in the game (alive).
    private final List<SchwimmenPlayer> gameLeavers; // sub-list of attendees, which are already out (death)
    private final Map<SchwimmenPlayer, List<SchwimmenPlayer>> viewerMap;
    private final Map<Integer, AskForCardView> askForViewMap;
    private final Map<Integer, AskForCardShow> askForShowMap;
    private final PropertyChangeListener playerListener;
    private final List<Card> gameStack; // Stock in the middle of the game, visible to all players.
    private final List<Card> dealerStack; // 2nd stack while dealing cards.
    private final GameStackProperties gameStackProperties;
    private final Gson gson;
    private final Round round;
    private final List<Integer> finishSoundIds;
    private final String videoRoomName;

    private GAMEPHASE gamePhase = GAMEPHASE.waitForAttendees;
    private int[] allAttendees; // IDs of players at start of the game (alive + death).
    private SchwimmenPlayer gameLooser = null;
    private SchwimmenPlayer mover = null; // this is like the cursor or pointer of the player which has to move. 
    private PlayerMove playerMove = null;
    private DiscoverMessage discoverMessage = null;
    private boolean webradioPlaying = true;
    private int finishSoundIdCursor = 0;

    /**
     * Default Constructor. Creates an instance of this class.
     */
    public SchwimmenGame() {
        this(Collections.synchronizedList(new ArrayList<>()), "");
    }

    /**
     * Constructor. Creates an instance of this class from given Value.
     *
     * @param conferenceName the room name for the jitsi conference
     */
    public SchwimmenGame(String conferenceName) {
        this(Collections.synchronizedList(new ArrayList<>()), conferenceName);
    }

    /**
     * Package protected constructor. Required for unit testing.
     *
     * @param gameStack injected game stack.
     * @param conferenceName the room name for the jitsi conference
     */
    SchwimmenGame(List<Card> gameStack, String conferenceName) {
        super(CARDS_32);
        players = Collections.synchronizedList(new ArrayList<>());
        playerIdComparator = new PlayerIdComparator(players);
        attendees = Collections.synchronizedList(new ArrayList<>());
        allAttendees = new int[0];
        gameLeavers = Collections.synchronizedList(new ArrayList<>());
        viewerMap = new HashMap<>();
        askForViewMap = new HashMap<>();
        askForShowMap = new HashMap<>();
        this.gameStack = gameStack;
        this.dealerStack = Collections.synchronizedList(new ArrayList<>());
        this.gameStackProperties = new GameStackProperties(gameStack);
        playerListener = this::playerPropertyChanged;
        round = new Round(this);
        gson = new Gson();
        finishSoundIds = new ArrayList<>();
        initFinishSoundIds();
        videoRoomName = conferenceName;
        super.addPropertyChangeListener(new GameChangeListener(this));
    }

    /**
     * Lookup for a player by name.
     *
     * @param name the player's name.
     * @return the player specified by name, null if there isn't one.f
     */
    public SchwimmenPlayer getPlayer(String name) {
        return players.stream().filter(player -> player.getName().equalsIgnoreCase(name)).findAny().orElse(null);
    }

    /**
     * The Login function. A player logged in and therefore "entered the room".
     *
     * @param player the player causing the event.
     */
    public void addPlayerToRoom(SchwimmenPlayer player) {
        if (mover == null) {
            mover = player;
        }
        player.addPropertyChangeListener(playerListener);
        players.add(player);
        firePropertyChange(PROP_PLAYERLIST, null, players);
        String msg = player.getName() + " ist gekommen";
        chat(msg);
        LOGGER.info(msg);
        if (gamePhase == GAMEPHASE.waitForAttendees) {
            addAttendee(player);
        }
    }

    /**
     * The logout function. A player logged out and therefore "left the round".
     * Currently disabled in the clients.
     *
     * @param player the player causing the event.
     */
    public void removePlayerFromRoom(SchwimmenPlayer player) {
        if (gamePhase != GAMEPHASE.waitForAttendees) {
            LOGGER.warn("Spieler kann jetzt nicht abgemeldet werden. Spiel laeuft!");
            return;
        }
        if (isAttendee(player)) {
            removeAttendee(player);
        }
        if (player.equals(gameLooser)) {
            gameLooser = null;
        }
        player.removePropertyChangeListener(playerListener);
        players.remove(player);
        firePropertyChange(PROP_PLAYERLIST, null, players);
        String msg = "Spieler " + player.getName() + " ist gegangen";
        chat(msg);
        LOGGER.info(msg);
    }

    /**
     * Getter for property videoRoomName.
     *
     * @return the name for the room in Jitsi meet.
     */
    public String getVideoRoomName() {
        return videoRoomName;
    }

    /**
     * Lookup for property isAttendee.
     *
     * @param player the player for which it is asked for.
     * @return true if the player is currently attendee of the game, false
     * otherwise.
     */
    public boolean isAttendee(SchwimmenPlayer player) {
        return attendees.contains(player);
    }

    /**
     * Getter for property player list.
     *
     * @return the list of players in the room.
     */
    public List<SchwimmenPlayer> getPlayerList() {
        return Collections.unmodifiableList(players);
    }

    /**
     * Sends a ping to all clients. Required to prevent the websocket timeout in
     * case of no action.
     */
    public void sendPing() {
        sendToPlayers("{\"action\":\"ping\"}");
    }

    /**
     * Sends a message to all players.
     *
     * @param message the message in JSON format.
     */
    public void sendToPlayers(String message) {
        players.forEach(p -> {
            p.getSocket().sendString(message);
        });
    }

    /**
     * Sends a chat message to all clients.
     *
     * @param text the text to be send to the chat.
     */
    public void chat(String text) {
        chat(text, null);
    }

    /**
     * Sends a chat message to all clients.
     *
     * @param text the text to be send to the chat.
     * @param sender the sending player.
     */
    public void chat(String text, SchwimmenPlayer sender) {
        if (text != null && !text.trim().isEmpty()) {
            ChatMessage chatMessage = new ChatMessage(text, sender);
            sendToPlayers(gson.toJson(chatMessage));
        }
    }

    /**
     * Getter for property game state.
     *
     * @param player the player for which it is asked for. Will vary e.g. if the
     * player is allowed to knock etc.
     * @return the game state for this player.
     */
    public GameStateMessage getGameState(SchwimmenPlayer player) {
        List<DiscoverStack> discoverStacks = null;
        if (gamePhase == GAMEPHASE.discover) {
            discoverStacks = new ArrayList<>();
            for (SchwimmenPlayer attendee : attendees) {
                if (attendee.equals(round.finisher)) {
                    discoverStacks.add(new DiscoverStack(round.finisher, round.finishScore));
                } else {
                    discoverStacks.add(new DiscoverStack(attendee, getStackScore(attendee.getStack())));
                }
            }
        }
        return new GameStateMessage(gamePhase.name(), players, attendees, allAttendees, viewerMap, mover,
                gameStackProperties.getGameStack(), player.getStack(), getViewerStackList(player), isChangeStackAllowed(player), isKnockAllowed(),
                discoverStacks, webradioPlaying);
    }

    /**
     * Getter for Property allAttendees.
     *
     * @return a list of player ids of the players at the start of a game (dead
     * + alive)
     */
    public int[] getAllAttendees() {
        return allAttendees;
    }

    /**
     * Getter for property Attendees count.
     *
     * @return the number of attendees (still) in the game.
     */
    public int getAttendeesCount() {
        return attendees.size();
    }

    /**
     * Getter for property game phase.
     *
     * @return the current game phase.
     */
    public GAMEPHASE getGamePhase() {
        return gamePhase;
    }

    /**
     * Getter for property viewer map.
     *
     * @return the map of card viewers for all players.
     */
    public Map<SchwimmenPlayer, List<SchwimmenPlayer>> getViewerMap() {
        return viewerMap;
    }

    /**
     * Getter for the viewer stack list for a player.
     *
     * @param player the viewing player.
     * @return a list of card stacks, which the player is viewing.
     */
    public ViewerStackList getViewerStackList(SchwimmenPlayer player) {
        Iterator<SchwimmenPlayer> iterator = viewerMap.keySet().iterator();
        List<SchwimmenPlayer> showerList = new ArrayList<>();
        while (iterator.hasNext()) {
            SchwimmenPlayer shower = iterator.next();
            List<SchwimmenPlayer> viewers = viewerMap.get(shower);
            if (viewers != null && viewers.contains(player)) {
                showerList.add(shower);
            }
        }
        if (!showerList.isEmpty()) {
            ViewerStack[] viewerStacks = new ViewerStack[showerList.size()];
            for (int i = 0; i < viewerStacks.length; i++) {
                viewerStacks[i] = new ViewerStack(showerList.get(i));
            }
            return new ViewerStackList(viewerStacks);
        }
        return new ViewerStackList();
    }

    /**
     * Getter for property mover.
     *
     * @return the player which is allowed to select a move. (The game's
     * 'cursor')
     */
    public SchwimmenPlayer getMover() {
        return mover;
    }

    /**
     * Getter for current player move.
     *
     * @return the current player's decision.
     */
    public PlayerMove getPlayertMove() {
        return playerMove;
    }

    /**
     * Getter for property discover message.
     *
     * @return the current discover message (if any).
     */
    public DiscoverMessage getDiscoverMessage() {
        return discoverMessage;
    }

    /**
     * Getter for property ChangeStackAllowed.
     *
     * @param player the player for which it is asked for.
     * @return true if the player is allowed to change the stock cards (in the
     * middle), false otherwise.
     */
    public boolean isChangeStackAllowed(SchwimmenPlayer player) {
        return round != null && round.isChangeStackAllowed(player);
    }

    /**
     * Getter for property knock allowed.
     *
     * @return true if the player is allowed to knock, false otherwise.
     */
    public boolean isKnockAllowed() {
        return (round != null) && round.isKnockAllowed();
    }

    /**
     * Setter for property WebRadioPlaying.
     *
     * @param play true to turn on the webradio, false to turn off.
     */
    public void setWebRadioPlaying(boolean play) {
        boolean oldValue = webradioPlaying;
        webradioPlaying = play;
        firePropertyChange(PROP_WEBRADIO_PLAYING, oldValue, play);
    }

    /**
     * Getter for property WebradioPlaying.
     *
     * @return true if the webradio is currently playing, false otherwise.
     */
    public boolean isWebradioPlaying() {
        return webradioPlaying;
    }

    /**
     * Starts the game.
     */
    public void startGame() {
        mover = guessNextGameStarter();
        gameLeavers.clear();
        viewerMap.clear();
        askForViewMap.clear();
        askForShowMap.clear();
        gameLooser = null;
        initRound();
        List<SchwimmenPlayer> offlineAttendees = new ArrayList<>();
        attendees.forEach((attendee) -> {
            attendee.reset();
            if (!attendee.isOnline()) {
                offlineAttendees.add(attendee);
            }
        });
        offlineAttendees.forEach(attendee -> removeAttendee(attendee));
        if (offlineAttendees.isEmpty()) { // ensure the event is fired at least once.
            firePropertyChange(PROP_ATTENDEESLIST, null, attendees);
        }
        firePropertyChange(PROP_VIEWER_MAP, null, viewerMap);
        setGamePhase(GAMEPHASE.shuffle);
    }

    /**
     * Stops a game. (Serverside only, not part of the game rules)
     */
    public void stopGame() {
        setGamePhase(GAMEPHASE.waitForAttendees);
    }

    /**
     * Adds a player to the list of attendees.
     *
     * @param attendee player to add to the attendees.
     */
    public void addAttendee(SchwimmenPlayer attendee) {
        if (gamePhase == GAMEPHASE.waitForAttendees) {
            if (!attendees.contains(attendee)) {
                if (players.contains(attendee)) {
                    attendees.add(attendee);
                    Collections.sort(attendees, playerIdComparator);
                    if (gamePhase == GAMEPHASE.waitForAttendees) {
                        allAttendees = new int[attendees.size()];
                        for (int i = 0; i < allAttendees.length; i++) {
                            allAttendees[i] = players.indexOf(attendees.get(i));
                        }
                    }
                    firePropertyChange(PROP_ATTENDEESLIST, null, attendees);
                    LOGGER.debug("Player '" + attendee + "' added to attendees list");
                } else {
                    LOGGER.warn("Can't add attendee '" + attendee + "': isn't part of the game!");
                }
            } else {
                LOGGER.warn("Can't add attendee '" + attendee + "': already in attendees list");
            }
        }
    }

    /**
     * Removes a player from the list of attendees.
     *
     * @param attendee the player to remove from the attendees.
     */
    public void removeAttendee(SchwimmenPlayer attendee) {
        if (gamePhase != GAMEPHASE.waitForAttendees) {
            if (attendees.contains(attendee) && !round.leavers.contains(attendee)) {
                round.leavers.add(attendee);
            }
        }
        if (attendees.contains(attendee)) {
            attendees.remove(attendee);
            if (attendee.equals(mover)) {
                mover = guessNextGameStarter();
            }
            if (gamePhase == GAMEPHASE.waitForAttendees) {
                allAttendees = new int[attendees.size()];
                for (int i = 0; i < allAttendees.length; i++) {
                    allAttendees[i] = players.indexOf(attendees.get(i));
                }
            }
            firePropertyChange(PROP_ATTENDEESLIST, null, attendees);
            LOGGER.debug("Player '" + attendee + "' removed from attendees list");
        } else {
            LOGGER.warn("Can't remove attendee '" + attendee + "': not in attendees list");
        }
    }

    private void playerPropertyChanged(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case SchwimmenPlayer.PROP_LOGOUT:
                SchwimmenPlayer player = (SchwimmenPlayer) evt.getSource();
                removePlayerFromRoom(player);
                break;
            case SchwimmenPlayer.PROP_ONLINE:
                firePropertyChange(PROP_PLAYER_ONLINE, null, evt.getSource());
                break;
            case SchwimmenPlayer.PROP_SOCKETMESSAGE:
                processMessage((SchwimmenPlayer) evt.getSource(), (SocketMessage) evt.getNewValue());
                break;
        }
    }

    /*
    /* Messages from the players 
     */
    private void processMessage(SchwimmenPlayer player, SocketMessage message) {
        switch (message.action) {
            case "addToAttendees":
                addAttendee(player);
                break;
            case "removeFromAttendees":
                removeAttendee(player);
                break;
            case "startGame":
                processStartGame(player);
                break;
            case "nextRound":
                processNextRound(player);
                break;
            case "dealCards":
                processDealCards(player);
                break;
            case "selectStack":
                processSelectStack(player, message.jsonObject.get("stack").getAsString());
                break;
            case "swapCard":
                processSwapCard(player, message);
                break;
            case "swapAllCards":
                processSwapAllCards(player);
                break;
            case "pass":
                processPass(player);
                break;
            case "knock":
                processKnock(player);
                break;
            case "changeStack":
                processChangeStack(player);
                break;
            case "askForCardView":
                processAskForCardView(player, message.jsonObject.get("target").getAsString());
                break;
            case "askForCardViewResponse":
                processAskForCardViewResponse(player, message);
                break;
            case "askForCardShow":
                processAskForCardShow(player, message.jsonObject.get("target").getAsString());
                break;
            case "askForCardShowResponse":
                processAskForCardShowResponse(player, message);
                break;
            case "stopCardViewing":
                processStopCardViewing(player, message);
                break;
            case "stopCardShowing":
                processStopCardShowing(player, message);
                break;
            case "chat":
                chat(message.jsonObject.get("text").getAsString(), player);
                break;
            default:
                LOGGER.warn("Unknown message from player '" + player.getName() + "': '" + message.jsonString);
        }
    }

    private void initRound() {
        round.reset(mover);
        gameStack.clear();
        dealerStack.clear();
        attendees.forEach(attendee -> attendee.clearStack());
    }

    private void setPlayerMove(PlayerMove move) {
        LOGGER.debug(mover.getName() + ": " + move);
        round.setPlayerMove(move);
        playerMove = move;
        setGamePhase(GAMEPHASE.moveResult);
    }

    private void setGamePhase(GAMEPHASE phase) {
        LOGGER.info("GamePhase: '" + phase + "'");
        this.gamePhase = phase;
        if (phase == GAMEPHASE.shuffle) {
            gameStackProperties.shakeAll();
        }
        firePropertyChange(PROP_GAMEPHASE, null, phase);
    }

    private void stepGamePhase() {
        stepGamePhase(true);
    }

    private void stepGamePhase(boolean shiftMover) {
        if (isFinishStackExists()) {
            discover();
        } else {
            if (round.getKnockCount() == 1 && getNextTo(mover).equals(round.knocker1)) {
                round.knock(round.knocker1); // round completed after 1th knock
            }
            if (round.getKnockCount() > 1) {
                discover();
            } else {
                if (shiftMover) {
                    shiftMover();
                }
                setGamePhase(GAMEPHASE.waitForPlayerMove);
            }
        }
    }

    private void discover() {
        mover = getNextTo(round.dealer);
        List<DiscoverStack> playerStacks = new ArrayList<>();
        for (SchwimmenPlayer attendee : attendees) {
            if (attendee.equals(round.finisher)) {
                playerStacks.add(new DiscoverStack(round.finisher, round.finishScore));
            } else {
                playerStacks.add(new DiscoverStack(attendee, getStackScore(attendee.getStack())));
            }
        }
        List<SchwimmenPlayer> payers = findRoundPayers();
        List<SchwimmenPlayer> leavers = findRoundLeavers(payers);
        round.leavers = leavers;
        while (leavers != null && leavers.contains(mover)) { //  regular next mover has left -> step forward
            mover = getNextTo(mover);
        }
        discoverMessage = new DiscoverMessage(round.finisher, round.finishScore,
                round.knocker2, playerStacks, payers, leavers, getNextFinishSoundId());
        setGamePhase(GAMEPHASE.discover);
    }

    private List<SchwimmenPlayer> findRoundPayers() {
        final ArrayList<SchwimmenPlayer> payers = new ArrayList<>();

        if (round.finishScore == 33f) { // fire
            payers.addAll(attendees);
            payers.remove(round.finisher);
            return payers;
        }

        float minScore = Float.MAX_VALUE;
        float playerScore;
        final ArrayList<SchwimmenPlayer> loosers = new ArrayList<>();
        for (SchwimmenPlayer attendee : attendees) {
            if (!attendee.equals(round.finisher)) {
                playerScore = getStackScore(attendee.getStack());
                if (playerScore < minScore) {
                    loosers.clear();
                    minScore = playerScore;
                }
                if (playerScore <= minScore) {
                    loosers.add(attendee);
                }
            }
        }
        int maxPrio = -1;
        for (SchwimmenPlayer looser : loosers) {
            int knockPrio = round.getKnockPriority(looser);
            if (knockPrio > maxPrio) {
                payers.clear();
                maxPrio = knockPrio;
            }
            if (knockPrio >= maxPrio) {
                payers.add(looser);
            }
        }
        return payers;
    }

    private List<SchwimmenPlayer> findRoundLeavers(List<SchwimmenPlayer> payers) {
        ArrayList<SchwimmenPlayer> leavers = new ArrayList<>();
        payers.forEach(payer -> {
            if (payer.decreaseToken() < 0) {
                leavers.add(payer);
            }
        });
        return leavers;
    }

    private void processNextRound(SchwimmenPlayer player) {
        if (player.equals(mover)) {
            if (null != gamePhase) {
                switch (gamePhase) {
                    case discover:
                        round.leavers.forEach(leaver -> {
                            if (gameLooser == null) {
                                gameLooser = leaver; // dealer for the next game
                            }
                            gameLeavers.add(leaver);
                            List<SchwimmenPlayer> viewerList = viewerMap.get(leaver);
                            if (viewerList != null) {
                                viewerList.clear();
                            }
                            attendees.remove(leaver); // swimming & paying -> death
                            firePropertyChange(PROP_ATTENDEESLIST, null, attendees);

                        });
                        if (attendees.size() == 1) { // game over
                            SchwimmenPlayer winner = attendees.get(0);
                            List<SchwimmenPlayer> viewerList = viewerMap.get(winner);
                            if (viewerList != null) {
                                viewerList.clear();
                            }
                            winner.addTotalTokens(3 * gameLeavers.size());
                            gameLeavers.forEach(leaver -> {
                                leaver.removeTotalTokens(3);
                                if (leaver.isOnline()) {
                                    attendees.add(leaver);
                                }
                            });
                            attendees.sort(playerIdComparator);
                            mover = guessNextGameStarter();
                            firePropertyChange(PROP_ATTENDEESLIST, null, attendees);
                            players.forEach(p -> p.reset());
                            players.forEach(p -> p.getSocket().sendString(gson.toJson(
                                    new GameStateMessage(gamePhase.name(), players, attendees, allAttendees, viewerMap, mover,
                                            gameStackProperties.getGameStack(), new ArrayList<>(), new ViewerStackList(), false, false, null, webradioPlaying))));
                            setGamePhase(GAMEPHASE.waitForAttendees);
                        } else {
                            players.forEach(p -> {
                                p.getStack().clear();
                                p.getSocket().sendString(gson.toJson(
                                        new GameStateMessage(gamePhase.name(), players, attendees, allAttendees, viewerMap, mover,
                                                gameStackProperties.getGameStack(), new ArrayList<>(), new ViewerStackList(), false, false, null, webradioPlaying)));
                            }
                            );
                            setGamePhase(GAMEPHASE.shuffle);
                        }
                        break;
                    case waitForAttendees:
                        startGame();
                        break;
                    default:
                        LOGGER.warn("Nächste Runde starten nicht möglich (Reason: GamePhase: " + gamePhase + ")");
                        break;
                }
            }
        } else if (gamePhase == GAMEPHASE.waitForAttendees) {
            setGamePhase(GAMEPHASE.shuffle);
            LOGGER.warn("Spieler '" + player.getName() + "' " + " ist nicht dran!");
        }
    }

    /* Round and Game has finished. Now look for the Player which must start the next Game. */
    private SchwimmenPlayer guessNextGameStarter() {
        SchwimmenPlayer nextMover = (gameLooser != null && attendees.contains(gameLooser)) ? gameLooser : getNextTo(gameLooser);
        if (nextMover == null || !nextMover.isOnline()) {
            // look for the next attendee
            if (!attendees.isEmpty()) {
                for (int i = 0; i < attendees.size(); i++) {
                    nextMover = getNextTo(nextMover);
                    if (nextMover.isOnline()) {
                        LOGGER.debug("guessNextGameStarter from attendees: " + nextMover);
                        return nextMover;
                    }
                }
            }
            // look in the player list
            for (int i = 0; i < players.size(); i++) {
                nextMover = players.get(i);
                if (nextMover.isOnline()) {
                    LOGGER.debug("guessNextGameStarter from players: " + nextMover);
                    return nextMover;
                }
            }
            LOGGER.debug("guessNextGameStarter (last exit): " + nextMover);
            nextMover = players.get(0); // last exit
        }
        LOGGER.debug("guessNextGameStarter: " + nextMover);
        return nextMover;
    }

    private void processStartGame(SchwimmenPlayer player) {
        if (gamePhase == GAMEPHASE.waitForAttendees) {
            if (attendees.contains(player)) {
//                if (player.equals(mover) || mover == null) {
                startGame();
                LOGGER.debug("Player '" + player + "' started the game");
//                }
            } else {
                LOGGER.warn("'" + player + "' is not an attendee!");
            }
        }
    }

    private void processDealCards(SchwimmenPlayer player) {
        if (player.equals(mover)) {
            if (gamePhase == GAMEPHASE.shuffle) {
                initRound();
                shuffleStack();
                for (int i = 0; i < 3; i++) {
                    attendees.forEach((attendee) -> {
                        attendee.addToStack(getFromStack());
                        if (attendee.equals(mover)) {
                            dealerStack.add(getFromStack());
                        }
                    });
                }
                attendees.forEach((attendee) -> {
                    attendee.getSocket().sendString(gson.toJson(new PlayerStack(attendee.getStack())));
                });
                setGamePhase(GAMEPHASE.dealCards);
                if (isFinishStackExists()) {
                    discover();
                }
            } else {
                LOGGER.warn(String.format("Aktion nicht erlaubt (%s != %s)", gamePhase, GAMEPHASE.shuffle));
            }
        } else {
            LOGGER.warn("Spieler '" + player.getName() + "' " + " ist nicht der Kartengeber!");
        }
    }

    private void processSelectStack(SchwimmenPlayer player, String action) {
        if (player.equals(mover)) {
            if (gamePhase == GAMEPHASE.dealCards) {
                switch (action) {
                    case "keep":
                        gameStack.addAll(dealerStack);
                        dealerStack.clear();
                        break;
                    case "change":
                        List<Card> playerStack = player.getStack();
                        gameStack.addAll(playerStack);
                        playerStack.clear();
                        playerStack.addAll(dealerStack);
                        dealerStack.clear();
                        break;
                    default:
                        LOGGER.error("Unkonwn action for selectDealerStack: '" + action + "'");
                }
                player.getSocket().sendString(gson.toJson(new PlayerStack(player.getStack())));
                setPlayerMove(new PlayerMove(MOVE.selectStack, gameStackProperties.getGameStack(), action));
                stepGamePhase();
            } else {
                LOGGER.warn(String.format("Aktion nicht erlaubt (%s != %s)", gamePhase, GAMEPHASE.dealCards));
            }
        } else {
            LOGGER.warn("Spieler '" + player.getName() + "' " + " ist nicht der Kartengeber!");
        }
    }

    private void processSwapCard(SchwimmenPlayer player, SocketMessage message) {
        if (player.equals(mover)) {
            if (gamePhase == GAMEPHASE.waitForPlayerMove) {
                List<Card> playerStack = player.getStack();
                int playerStackId = message.jsonObject.get("playerStack").getAsInt();
                int gameStackId = message.jsonObject.get("gameStack").getAsInt();
                try {
                    Card cardTaken = gameStack.get(gameStackId);
                    Card cardGiven = playerStack.get(playerStackId);
                    playerStack.set(playerStackId, cardTaken);
                    gameStack.set(gameStackId, cardGiven);
                    gameStackProperties.shake(gameStackId);
                    player.getSocket().sendString(gson.toJson(new PlayerStack(playerStack)));
                    setPlayerMove(new PlayerMove(new CardSwap(cardGiven, cardTaken, playerStackId, gameStackId), gameStackProperties.getGameStack()));
                    stepGamePhase();
                } catch (Exception e) {
                    LOGGER.error("Spieler '" + player.getName() + "' " + " Karten IDs ungueltig!");
                }
            } else {
                LOGGER.warn(String.format("Aktion nicht erlaubt (%s != %s)", gamePhase, GAMEPHASE.waitForPlayerMove));
            }
        } else {
            LOGGER.warn("Spieler '" + player.getName() + "' " + " ist nicht dran!");
        }
    }

    private void processSwapAllCards(SchwimmenPlayer player) {
        if (player.equals(mover)) {
            if (gamePhase == GAMEPHASE.waitForPlayerMove) {
                List<Card> playerStack = player.getStack();
                List<Card> stackTaken = new ArrayList<>();
                List<Card> stackGiven = new ArrayList<>();
                stackTaken.addAll(gameStack);
                stackGiven.addAll(playerStack);
                gameStack.clear();
                playerStack.clear();
                gameStack.addAll(stackGiven);
                gameStackProperties.shakeAll();
                playerStack.addAll(stackTaken);
                player.getSocket().sendString(gson.toJson(new PlayerStack(playerStack)));
                setPlayerMove(new PlayerMove(new StackSwap(stackGiven, stackTaken), gameStackProperties.getGameStack()));
                stepGamePhase();
            } else {
                LOGGER.warn(String.format("Aktion nicht erlaubt (%s != %s)", gamePhase, GAMEPHASE.waitForPlayerMove));
            }
        } else {
            LOGGER.warn("Spieler '" + player.getName() + "' " + " ist nicht dran!");
        }
    }

    private void processPass(SchwimmenPlayer player) {
        if (player.equals(mover)) {
            if (gamePhase == GAMEPHASE.waitForPlayerMove) {
                round.pass(player);
                setPlayerMove(new PlayerMove(MOVE.pass, round.getPassCount(), gameStackProperties.getGameStack()));
                stepGamePhase();
            } else {
                LOGGER.warn(String.format("Aktion nicht erlaubt (%s != %s)", gamePhase, GAMEPHASE.waitForPlayerMove));
            }
        } else {
            LOGGER.warn("Spieler '" + player.getName() + "' " + " ist nicht dran!");
        }
    }

    private void processKnock(SchwimmenPlayer player) {
        if (player.equals(mover)) {
            if (gamePhase == GAMEPHASE.waitForPlayerMove) {
                if (isKnockAllowed()) {
                    round.knock(player);
                    setPlayerMove(new PlayerMove(MOVE.knock, round.getKnockCount(), gameStackProperties.getGameStack()));
                    stepGamePhase();
                } else {
                    LOGGER.warn("Spieler '" + player.getName() + "' " + " darf nicht klopfen!");
                }
            } else {
                LOGGER.warn(String.format("Aktion nicht erlaubt (%s != %s)", gamePhase, GAMEPHASE.waitForPlayerMove));
            }
        } else {
            LOGGER.warn("Spieler '" + player.getName() + "' " + " ist nicht dran!");
        }
    }

    private void processChangeStack(SchwimmenPlayer player) {
        if (player.equals(mover)) {
            if (gamePhase == GAMEPHASE.waitForPlayerMove) {
                if (isChangeStackAllowed(player)) {
                    if (!(stackSize() < 3)) {
                        gameStack.clear();
                        for (int i = 0; i < 3; i++) {
                            gameStack.add(getFromStack());
                        }
                        gameStackProperties.shakeAll();
                    } else {
                        LOGGER.info("Es sind zu wenig Karten verfuegbar um zu tauschen.");
                    }
                    setPlayerMove(new PlayerMove(MOVE.changeStack, gameStackProperties.getGameStack()));
                    stepGamePhase(false);
                } else {
                    LOGGER.warn("Spieler '" + player.getName() + "' " + " darf den Stapel nicht austauschen!");
                }
            } else {
                LOGGER.warn(String.format("Aktion nicht erlaubt (%s != %s)", gamePhase, GAMEPHASE.waitForPlayerMove));
            }
        } else {
            LOGGER.warn("Spieler '" + player.getName() + "' " + " ist nicht dran!");
        }
    }

    private void processAskForCardViewResponse(SchwimmenPlayer player, SocketMessage message) {
        AskForCardView question = null;
        JsonElement hashCodeElement = message.jsonObject.get("hashCode");
        if (hashCodeElement != null) {
            question = askForViewMap.get(hashCodeElement.getAsInt());
        }
        if (question == null) {
            LOGGER.warn("Received invalid AskForView-Response: no question available");
            return;
        }
        JsonElement valueElement = message.jsonObject.get("value");
        if (valueElement == null) {
            LOGGER.warn("Received invalid AskForView-Response: no value available");
            return;
        }
        if (valueElement.getAsBoolean() && attendees.contains(player)) {
            List<SchwimmenPlayer> viewerList = viewerMap.get(player);
            if (viewerList == null) {
                viewerList = new ArrayList<>();
                viewerMap.put(player, viewerList);
            }
            SchwimmenPlayer viewer = getPlayer(question.source);
            viewerList.add(viewer);
            firePropertyChange(PROP_VIEWER_MAP, null, viewerMap);
            viewer.getSocket().sendString(gson.toJson(getGameState(viewer)));
            chat(question.source + " schaut bei " + player.getName() + " in die Karten.");
        } else {
            chat(player.getName() + " l&auml;sst " + question.source + " nicht in die Karten schauen.");
        }
        askForViewMap.remove(question.hashCode);
    }

    private void processAskForCardView(SchwimmenPlayer player, String target) {
        if (attendees.contains(player)) {
            LOGGER.warn("Player '" + player.getName() + "' is in game and therefore not allowed to view other cards!");
            return;
        }
        SchwimmenPlayer targetPlayer = getPlayer(target);
        if (targetPlayer == null) {
            LOGGER.warn("Player '" + target + "' doesn't exist!");
            return;
        }
        if (!targetPlayer.isOnline()) {
            LOGGER.warn("Player '" + target + "' is not online!");
            return;
        }
        if (!attendees.contains(targetPlayer)) {
            LOGGER.warn("Player '" + target + "' is not in game!");
            return;
        }
        List<SchwimmenPlayer> viewerList = viewerMap.get(targetPlayer);
        if (viewerList != null && viewerList.contains(player)) {
            LOGGER.warn("Player '" + player.getName() + "' is already viewer of " + target + "!");
            return;
        }

        AskForCardView askForCardView = new AskForCardView(player);
        askForViewMap.put(askForCardView.hashCode, askForCardView);
        targetPlayer.getSocket().sendString(gson.toJson(askForCardView));
    }

    private void processAskForCardShowResponse(SchwimmenPlayer player, SocketMessage message) {
        AskForCardShow question = null;
        JsonElement hashCodeElement = message.jsonObject.get("hashCode");
        if (hashCodeElement != null) {
            question = askForShowMap.get(hashCodeElement.getAsInt());
        }
        if (question == null) {
            LOGGER.warn("Received invalid AskForShow-Response: no question available");
            return;
        }
        JsonElement valueElement = message.jsonObject.get("value");
        if (valueElement == null) {
            LOGGER.warn("Received invalid AskForShow-Response: no value available");
            return;
        }
        if (valueElement.getAsBoolean()) {
            SchwimmenPlayer askingPlayer = getPlayer(question.source);
            if (attendees.contains(askingPlayer)) {
                List<SchwimmenPlayer> viewerList = viewerMap.get(askingPlayer);
                if (viewerList == null) {
                    viewerList = new ArrayList<>();
                    viewerMap.put(askingPlayer, viewerList);
                }
                viewerList.add(player);
                firePropertyChange(PROP_VIEWER_MAP, null, viewerMap);
                player.getSocket().sendString(gson.toJson(getGameState(player)));
                chat(askingPlayer.getName() + " zeigt " + player.getName() + " die Karten.");
            }
        } else {
            chat(player.getName() + " m&ouml;chte die Karten von " + question.source + " nicht sehen.");
        }
        askForShowMap.remove(question.hashCode);
    }

    private void processAskForCardShow(SchwimmenPlayer player, String target) {
        if (!attendees.contains(player)) {
            LOGGER.warn("Player '" + player.getName() + "' is not in game and therefore can't show cards!");
            return;
        }
        SchwimmenPlayer targetPlayer = getPlayer(target);
        if (targetPlayer == null) {
            LOGGER.warn("Player '" + target + "' doesn't exist!");
            return;
        }
        if (!targetPlayer.isOnline()) {
            LOGGER.warn("Player '" + target + "' is not online!");
            return;
        }
        if (attendees.contains(targetPlayer)) {
            LOGGER.warn("Player '" + target + "' is in game!");
            return;
        }
        List<SchwimmenPlayer> viewerList = viewerMap.get(player);
        if (viewerList != null && viewerList.contains(targetPlayer)) {
            LOGGER.warn("Player '" + target + "' is already viewer of " + player.getName() + "!");
            return;
        }

        AskForCardShow askForCardShow = new AskForCardShow(player);
        askForShowMap.put(askForCardShow.hashCode, askForCardShow);
        targetPlayer.getSocket().sendString(gson.toJson(askForCardShow));
    }

    private void processStopCardViewing(SchwimmenPlayer player, SocketMessage message) {
        String targetName = message.jsonObject.get("target").getAsString();
        SchwimmenPlayer targetPlayer = getPlayer(targetName);
        if (targetPlayer == null) {
            LOGGER.warn("Player '" + targetName + "' doesn't exist!");
            return;
        }
        List<SchwimmenPlayer> viewerList = viewerMap.get(targetPlayer);
        if (viewerList == null || !viewerList.contains(player)) {
            LOGGER.warn("Player '" + player.getName() + "' is not viewer of player '" + targetName + "'!");
            return;
        }
        viewerList.remove(player);
        firePropertyChange(PROP_VIEWER_MAP, null, viewerMap);
        player.getSocket().sendString(gson.toJson(getGameState(player)));
        chat(player.getName() + " schaut nicht mehr bei " + targetName + " in die Karten.");

    }

    private void processStopCardShowing(SchwimmenPlayer player, SocketMessage message) {
        String targetName = message.jsonObject.get("target").getAsString();
        SchwimmenPlayer targetPlayer = getPlayer(targetName);
        if (targetPlayer == null) {
            LOGGER.warn("Player '" + targetName + "' doesn't exist!");
            return;
        }
        List<SchwimmenPlayer> viewerList = viewerMap.get(player);
        if (viewerList == null || !viewerList.contains(targetPlayer)) {
            LOGGER.warn("Player '" + targetName + "' is not viewer of player '" + player.getName() + "'!");
            return;
        }
        viewerList.remove(targetPlayer);
        firePropertyChange(PROP_VIEWER_MAP, null, viewerMap);
        targetPlayer.getSocket().sendString(gson.toJson(getGameState(targetPlayer)));
        chat(player.getName() + " zeigt " + targetName + " die Karten nicht mehr.");
    }

    /**
     * Get the scoreValue of a card in a schwimmen game
     */
    private int getSchwimmenValue(int cardValue) {
        switch (cardValue) {
            case Card.AS:
                return 11;
            case Card.BUBE:
            case Card.DAME:
            case Card.KOENIG:
            case Card.ZEHN:
                return 10;
            default:
                return cardValue;
        }
    }

    /**
     * calculate the value sum of the stack if of same type
     */
    private int getValueSum(List<Card> cards) {
        int valueSum = 0;
        if (!cards.isEmpty()) {
            int value = cards.get(0).getValue();
            for (Card card : cards) {
                if (value == card.getValue()) {
                    valueSum += getSchwimmenValue(card.getValue());
                } else {
                    valueSum = 0;
                    break;
                }
            }
        }
        return valueSum;
    }

    /**
     * calculate the score of a stack
     */
    float getStackScore(List<Card> cards) {
        if (cards.isEmpty()) {
            return 0;
        }
        int valueSum = getValueSum(cards);
        if (valueSum == 33) {
            return 33;
        }
        if (valueSum > 0) {
            return 30.5f;
        }
        int[] colorScores = new int[4];
        int maxColorScore = 0;
        for (Card card : cards) {
            int id = card.getColor() - 1;
            colorScores[id] += getSchwimmenValue(card.getValue());
            maxColorScore = Math.max(colorScores[id], maxColorScore);
        }
        return maxColorScore;
    }

    boolean isFinishStack(List<Card> cards, SchwimmenPlayer player) {
        float score = getStackScore(cards);
        if (score > 30.5) {
            round.finisher = player;
            round.finishScore = score;
            LOGGER.debug("Player '" + player + "' finishes with " + score);
        }
        return round.finisher != null;
    }

    // search the stacks for 31 or FIRE 
    boolean isFinishStackExists() {
        for (SchwimmenPlayer attendee : attendees) {
            if (isFinishStack(attendee.getStack(), attendee)) {
                return true;
            }
        }
        return false;
        // disabled this, since the next player must have the chance to make fire, even if there is 31 in the game stack.
        // return  (isFinishStack(gameStack, getNextTo(mover)));
    }

    Round getRound() { // for unit testing
        return round;
    }

    private void initFinishSoundIds() {
        int soundCount = 13;
        List<Integer> soundIDs = new ArrayList<>();
        for (int i = 0; i < soundCount; i++) {
            soundIDs.add(i);
        }
        while (soundIDs.size() > 0) {
            int randomId = (int) Math.round(Math.random() * (soundIDs.size() - 1));
            finishSoundIds.add(soundIDs.get(randomId));
            soundIDs.remove(randomId);
        }
        if (finishSoundIds.size() == soundCount) {
            LOGGER.info("Finish Sound IDs initialized successfully");
        } else {
            LOGGER.error("Finish Sound IDs initialization failed");
        }
    }

    private int getNextFinishSoundId() {
        finishSoundIdCursor++;
        if (finishSoundIdCursor >= finishSoundIds.size()) {
            finishSoundIdCursor = 0;
        }
        return finishSoundIdCursor;
    }

    private void shiftMover() {
        mover = getNextTo(mover);
        LOGGER.debug("New mover: " + mover);
    }

    private SchwimmenPlayer getNextTo(SchwimmenPlayer player) {
        if (attendees.isEmpty()) {
            return null;
        }
        int index = attendees.indexOf(player) + 1;
        return attendees.get(index < attendees.size() ? index : 0);
    }

    static class PlayerIdComparator implements Comparator<SchwimmenPlayer> {

        final List<SchwimmenPlayer> playerList;

        public PlayerIdComparator(List<SchwimmenPlayer> playerList) {
            this.playerList = playerList;
        }

        @Override
        public int compare(SchwimmenPlayer p1, SchwimmenPlayer p2) {
            return playerList.indexOf(p1) < playerList.indexOf(p2) ? -1 : 1;
        }
    }

}
