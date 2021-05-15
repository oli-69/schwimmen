package schwimmen;

import cardgame.Card;
import cardgame.CardGame;
import cardgame.GameStackProperties;
import cardgame.Player;
import cardgame.SocketMessage;
import cardgame.messages.PlayerStack;
import cardgame.messages.WebradioUrl;
import com.google.gson.JsonElement;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import schwimmen.messages.AskForCardShow;
import schwimmen.messages.AskForCardView;
import schwimmen.messages.CardSwap;
import schwimmen.messages.DiscoverMessage;
import schwimmen.messages.DiscoverStack;
import schwimmen.messages.Finish31OnDealMessage;
import schwimmen.messages.GameStateMessage;
import schwimmen.messages.PlayerMove;
import schwimmen.messages.StackSwap;
import schwimmen.messages.ViewerStack;
import schwimmen.messages.ViewerStackList;

/**
 * This class implements the game rules and evaluates the player's decisions.
 */
public class SchwimmenGame extends CardGame {

    private static final Logger LOGGER = LogManager.getLogger(SchwimmenGame.class);
    private static final String GAME_NAME = "Schwimmen Server";
    private static Image GAME_ICON;

    /**
     * Enumeration of optional game rules
     */
    public static enum GAMERULE {
        /**
         * This game rule enables the active player to select "new cards" if the
         * gamestack contains 7, 8 and 9.
         */
        newCardsOn789,
        /**
         * This game rule enables the playeres are allowed to pass only once per
         * round
         */
        passOnlyOncePerRound,
        /**
         * This game rule enables the round ending after 2nd knock
         */
        Knocking
    }

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
         * A player got 31 on his hand. The dealer has to select which stack is
         * hold and which goes into the stock in the middle.
         */
        finish31OnDeal,
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

    public static final String PROP_VIEWER_MAP = "viewerMap";
    public static final String PROP_GAMERULE = "gameRule";

    private final List<Player> gameLeavers; // sub-list of attendees, which are already out (death)
    private final Map<Player, Collection<Player>> viewerMap;
    private final Map<Integer, AskForCardView> askForViewMap;
    private final Map<Integer, AskForCardShow> askForShowMap;
    private final List<Card> gameStack; // Stock in the middle of the game, visible to all players.
    private final List<Card> dealerStack; // 2nd stack while dealing cards.
    private final GameStackProperties gameStackProperties;
    private final Round round;
    private final List<Integer> finishSoundIds;
    private final Set<GAMERULE> gameRules;
    private final CardDealService cardDealService;

    private GAMEPHASE gamePhase = GAMEPHASE.waitForAttendees;
    private int[] allAttendees; // IDs of players at start of the game (alive + death).
    private Player gameLooser = null;
    private PlayerMove playerMove = null;
    private DiscoverMessage discoverMessage = null;
    private Finish31OnDealMessage finish31OnDealMessage = null;
    private int finishSoundIdCursor = 0;
    private int finishSoundId = 0;
    private int gameCounter = 0;

    /**
     * Default Constructor. Creates an instance of this class.
     */
    public SchwimmenGame() {
        this(Collections.synchronizedList(new ArrayList<>()), Collections.synchronizedList(new ArrayList<>()),
                "", new CardDealServiceImpl(), new ArrayList<>());
    }

    /**
     * Constructor. Creates an instance of this class from given Value.
     *
     * @param conferenceName the room name for the jitsi conference
     * @param webradioList list of known webradios
     */
    public SchwimmenGame(String conferenceName, List<WebradioUrl> webradioList) {
        this(Collections.synchronizedList(new ArrayList<>()), Collections.synchronizedList(new ArrayList<>()),
                conferenceName, new CardDealServiceImpl(), webradioList);
    }

    /**
     * Package protected constructor. Required for unit testing.
     */
    SchwimmenGame(List<Card> gameStack, List<Card> dealerStack, String conferenceName, CardDealService cardDealService, List<WebradioUrl> webradioList) {
        super(CARDS_32, conferenceName, webradioList);
        allAttendees = new int[0];
        gameLeavers = Collections.synchronizedList(new ArrayList<>());
        viewerMap = Collections.synchronizedMap(new HashMap<>());
        askForViewMap = Collections.synchronizedMap(new HashMap<>());
        askForShowMap = Collections.synchronizedMap(new HashMap<>());
        this.gameStack = gameStack;
        this.dealerStack = dealerStack;
        gameStackProperties = new GameStackProperties(gameStack, 3);
        gameRules = new HashSet<>();
        round = new Round(this);
        finishSoundIds = new ArrayList<>();
        initFinishSoundIds();
        this.cardDealService = cardDealService;
        super.addPropertyChangeListener(new GameChangeListener(this));
    }

    /**
     * Enable or disable a game rule.
     *
     * @param rule the rule to enable/disable.
     * @param enabled true to enable the rule, false otherwise.
     */
    public void setGameRuleEnabled(GAMERULE rule, boolean enabled) {
        if (gamePhase == GAMEPHASE.waitForAttendees
                || gamePhase == GAMEPHASE.shuffle
                || gamePhase == GAMEPHASE.discover) {
            boolean oldVal = isGameRuleEnabled(rule);
            if (enabled) {
                gameRules.add(rule);
            } else {
                gameRules.remove(rule);
            }
            firePropertyChange(PROP_GAMERULE, oldVal, enabled);
        } else {
            throw new IllegalArgumentException("Game must be in phase 'waitForAttendees', 'shuffle', or 'discover'");
        }
    }

    /**
     * Getter for property game rule enabled.
     *
     * @param rule the game rule to ask for.
     * @return true if the game rule is enabled, false otherwise.
     */
    public boolean isGameRuleEnabled(GAMERULE rule) {
        return gameRules.contains(rule);
    }

    /**
     * The Login function. A player logged in and therefore "entered the room".
     *
     * @param player the player causing the event.
     */
    @Override
    public void addPlayerToRoom(Player player) {
        super.addPlayerToRoom(player);
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
    @Override
    public void removePlayerFromRoom(Player player) {
        if (gamePhase != GAMEPHASE.waitForAttendees) {
            LOGGER.warn("Spieler kann jetzt nicht abgemeldet werden. Spiel laeuft!");
            return;
        }
        if (player.equals(gameLooser)) {
            gameLooser = null;
        }
        super.removePlayerFromRoom(player);
    }

    /**
     * Getter for property game state.
     *
     * @param player the player for which it is asked for. Will vary e.g. if the
     * player is allowed to knock etc.
     * @return the game state for this player in JSON format
     */
    private GameStateMessage getGameStateMessage(Player player) {
        List<DiscoverStack> discoverStacks = null;
        if (gamePhase == GAMEPHASE.discover) {
            discoverStacks = new ArrayList<>();
            for (Player attendee : attendees) {
                if (attendee.equals(round.finisher)) {
                    discoverStacks.add(new DiscoverStack(round.finisher, round.finishScore));
                } else {
                    discoverStacks.add(new DiscoverStack(attendee, getStackScore(attendee.getStack())));
                }
            }
        }
        Finish31OnDealMessage finish31OnDeal = (gamePhase == GAMEPHASE.finish31OnDeal) ? finish31OnDealMessage : null;
        return new GameStateMessage(gamePhase.name(), players, attendees, allAttendees, viewerMap, mover,
                gameStackProperties.getGameStack(), player.getStack(), getViewerStackList(player), isChangeStackAllowed(player),
                isKnockAllowed(), isPassAllowed(player), discoverStacks, finish31OnDeal, isWebradioPlaying(), getRadioUrl());
    }

    /**
     * Getter for property game state.
     *
     * @param player the player for which it is asked for. Will vary e.g. if the
     * player is allowed to knock etc.
     * @return the game state for this player in JSON format
     */
    @Override
    public String getGameState(Player player) {
        return gson.toJson(getGameStateMessage(player));
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
    public Map<Player, Collection<Player>> getViewerMap() {
        return viewerMap;
    }

    /**
     * Getter for the viewer stack list for a player.
     *
     * @param player the viewing player.
     * @return a list of card stacks, which the player is viewing.
     */
    public ViewerStackList getViewerStackList(Player player) {
        Iterator<Player> iterator = viewerMap.keySet().iterator();
        List<Player> showerList = new ArrayList<>();
        while (iterator.hasNext()) {
            Player shower = iterator.next();
            Collection<Player> viewers = viewerMap.get(shower);
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
     * Getter for property Finish31OnDeal Message.
     *
     * @return the current Finish31OnDeal message, if any.
     */
    public Finish31OnDealMessage getFinish31OnDealMessage() {
        return finish31OnDealMessage;
    }

    /**
     * Getter for property ChangeStackAllowed.
     *
     * @param player the player for which it is asked for.
     * @return true if the player is allowed to change the stock cards (in the
     * middle), false otherwise.
     */
    public boolean isChangeStackAllowed(Player player) {
        return round != null && round.isChangeStackAllowed(player, gameStack);
    }

    /**
     * Getter for property PassAllowed.
     *
     * @param player the player for which it is asked for.
     * @return true if the player is allowed to pass, false otherwise.
     */
    public boolean isPassAllowed(Player player) {
        return round != null && round.isPassAllowed(player);
    }

    /**
     * Getter for property knock allowed.
     *
     * @return true if the player is allowed to knock, false otherwise.
     */
    public boolean isKnockAllowed() {
        return (round != null) && round.isKnockAllowed();
    }

    @Override
    public void shufflePlayers() {
        if (gamePhase == GAMEPHASE.waitForAttendees) {
            super.shufflePlayers();
        } else {
            LOGGER.warn("Das Umsetzen der Spieler ist im Spiel nicht erlaubt.");
        }
    }

    @Override
    public String getName() {
        return GAME_NAME;
    }

    @Override
    public Image getIcon() {
        if (GAME_ICON == null) {
            GAME_ICON = new ImageIcon(SchwimmenGame.class.getResource("favicon-32x32.png")).getImage();
        }
        return GAME_ICON;
    }

    /**
     * Starts the game.
     */
    @Override
    public void startGame() {
        if (gamePhase == GAMEPHASE.waitForAttendees) {
            gameCounter++;
            mover = guessNextGameStarter();
            gameLeavers.clear();
            viewerMap.clear();
            askForViewMap.clear();
            askForShowMap.clear();
            gameLooser = null;
            finishSoundId = getNextFinishSoundId();
            initRound();
            List<Player> offlineAttendees = new ArrayList<>();
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
            chat("Spiel #" + gameCounter + " wird gestartet");
        } else {
            LOGGER.warn("Das Spiel ist bereits gestartet!");
        }
    }

    /**
     * Stops a game. (Serverside only, not part of the game rules)
     */
    @Override
    public void stopGame() {
        if (gamePhase != GAMEPHASE.waitForAttendees) {
            mover = guessNextGameStarter();
            setGamePhase(GAMEPHASE.waitForAttendees);
            chat("Spiel #" + gameCounter + " wurde abgebrochen");
        } else {
            LOGGER.warn("Das Spiel ist bereits gestoppt!");
        }
    }

    /**
     * Adds a player to the list of attendees.
     *
     * @param attendee player to add to the attendees.
     */
    @Override
    public void addAttendee(Player attendee) {
        if (gamePhase == GAMEPHASE.waitForAttendees) {
            if (!attendees.contains(attendee)) {
                if (players.contains(attendee)) {
                    attendees.add(attendee);
                    Collections.sort(attendees, playerIdComparator);
                    allAttendees = new int[attendees.size()];
                    for (int i = 0; i < allAttendees.length; i++) {
                        allAttendees[i] = players.indexOf(attendees.get(i));
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
    @Override
    public void removeAttendee(Player attendee) {
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


    /*
    /* Messages from the players 
     */
    @Override
    protected void processMessage(Player player, SocketMessage message) {
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
            if (shiftMover && round.getKnockCount() == 1 && getNextTo(mover).equals(round.knocker1)) {
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

    private void finishOnDeal() {
        if (round.finishScore == 33 || mover.equals(round.finisher)) {
            discover();
        } else {
            finish31OnDealMessage = new Finish31OnDealMessage(round.finisher, round.finisher.getStack());
            setGamePhase(GAMEPHASE.finish31OnDeal);
        }
    }

    private void discover() {
        mover = getNextTo(round.dealer);
        List<DiscoverStack> playerStacks = new ArrayList<>();
        for (Player attendee : attendees) {
            if (attendee.equals(round.finisher)) {
                playerStacks.add(new DiscoverStack(round.finisher, round.finishScore));
            } else {
                playerStacks.add(new DiscoverStack(attendee, getStackScore(attendee.getStack())));
            }
        }
        List<Player> payers = findRoundPayers();
        List<Player> leavers = findRoundLeavers(payers);
        round.leavers = leavers;
        while (leavers != null && leavers.contains(mover)) { //  regular next mover has left -> step forward
            mover = getNextTo(mover);
        }
        discoverMessage = new DiscoverMessage(round.finisher, round.finishScore,
                round.knocker2, playerStacks, payers, leavers, finishSoundId);
        setGamePhase(GAMEPHASE.discover);
    }

    private List<Player> findRoundPayers() {
        final ArrayList<Player> payers = new ArrayList<>();

        if (round.finishScore == 33f) { // fire
            payers.addAll(attendees);
            payers.remove(round.finisher);
            return payers;
        }

        float minScore = Float.MAX_VALUE;
        float playerScore;
        final ArrayList<Player> loosers = new ArrayList<>();
        for (Player attendee : attendees) {
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

        if (isGameRuleEnabled(GAMERULE.Knocking)) {
            int maxPrio = -1;
            for (Player looser : loosers) {
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

        } else {
            payers.clear();
            payers.addAll(loosers);

            if (loosers.size() == attendees.size()) {
                // all currently playing attendees are loosers
                // if all loosers are already swimming then
                // do not return payers
                int sumLooserGameToken = 0;

                for (Player looser : loosers) {
                    // sum over all looser game token
                    sumLooserGameToken += looser.getGameTokens();
                }
                if (sumLooserGameToken == 0) {
                    payers.clear();
                }
            }
            return payers;
        }
    }

    private List<Player> findRoundLeavers(List<Player> payers) {
        ArrayList<Player> leavers = new ArrayList<>();
        payers.forEach(payer -> {
            if (payer.decreaseGameToken() < 0) {
                leavers.add(payer);
            }
        });
        return leavers;
    }

    private void processNextRound(Player player) {
        if (player.equals(mover)) {
            if (null != gamePhase) {
                switch (gamePhase) {
                    case discover:
                        round.leavers.forEach(leaver -> {
                            if (gameLooser == null) {
                                gameLooser = leaver; // dealer for the next game
                            }
                            gameLeavers.add(leaver);
                            Collection<Player> viewerList = viewerMap.get(leaver);
                            if (viewerList != null) {
                                viewerList.clear();
                            }
                            attendees.remove(leaver); // swimming & paying -> death
                            firePropertyChange(PROP_ATTENDEESLIST, null, attendees);

                        });
                        if (attendees.size() == 1) { // game over
                            Player winner = attendees.get(0);
                            Collection<Player> viewerList = viewerMap.get(winner);
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
                                    new GameStateMessage(gamePhase.name(), players, attendees, allAttendees, viewerMap, mover, gameStackProperties.getGameStack(), isWebradioPlaying(), getRadioUrl()))));
                            setGamePhase(GAMEPHASE.waitForAttendees);
                            chat(winner.getName() + " hat gewonnen");
                        } else {
                            players.forEach(p -> {
                                p.getStack().clear();
                                p.getSocket().sendString(gson.toJson(
                                        new GameStateMessage(gamePhase.name(), players, attendees, allAttendees, viewerMap, mover, gameStackProperties.getGameStack(), isWebradioPlaying(), getRadioUrl())));
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
            LOGGER.warn("Spieler '" + player.getName() + "' " + " ist nicht dran!");
            startGame(); // <- emergency case
        }
    }

    /* Round and Game has finished. Now look for the Player which must start the next Game. */
    private Player guessNextGameStarter() {
        Player nextMover = (gameLooser != null && attendees.contains(gameLooser)) ? gameLooser : getNextTo(gameLooser);
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

    private void processStartGame(Player player) {
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

    private void processDealCards(Player player) {
        if (player.equals(mover)) {
            if (gamePhase == GAMEPHASE.shuffle) {
                initRound();
                cardDealService.dealCards(this);
                attendees.forEach((attendee) -> {
                    attendee.getSocket().sendString(gson.toJson(new PlayerStack(attendee.getStack())));
                });
                setGamePhase(GAMEPHASE.dealCards);
                if (isFinishStackExists()) {
                    finishOnDeal();
                }
            } else {
                LOGGER.warn(String.format("Aktion nicht erlaubt (%s != %s)", gamePhase, GAMEPHASE.shuffle));
            }
        } else {
            LOGGER.warn("Spieler '" + player.getName() + "' " + " ist nicht der Kartengeber!");
        }
    }

    private void processSelectStack(Player player, String action) {
        if (player.equals(mover)) {
            if (gamePhase == GAMEPHASE.dealCards || gamePhase == GAMEPHASE.finish31OnDeal) {
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
                LOGGER.warn(String.format("Aktion nicht erlaubt (%s != %s)", gamePhase, GAMEPHASE.dealCards + "|" + GAMEPHASE.finish31OnDeal));
            }
        } else {
            LOGGER.warn("Spieler '" + player.getName() + "' " + " ist nicht der Kartengeber!");
        }
    }

    private void processSwapCard(Player player, SocketMessage message) {
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

    private void processSwapAllCards(Player player) {
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

    private void processPass(Player player) {
        if (player.equals(mover)) {
            if (gamePhase == GAMEPHASE.waitForPlayerMove) {
                if (isPassAllowed(player)) {
                    round.pass(player);
                    setPlayerMove(new PlayerMove(MOVE.pass, round.getPassCount(), gameStackProperties.getGameStack()));
                    stepGamePhase();
                } else {
                    LOGGER.warn("Spieler '" + player.getName() + "' " + " darf nicht schieben!");
                }
            } else {
                LOGGER.warn(String.format("Aktion nicht erlaubt (%s != %s)", gamePhase, GAMEPHASE.waitForPlayerMove));
            }
        } else {
            LOGGER.warn("Spieler '" + player.getName() + "' " + " ist nicht dran!");
        }
    }

    private void processKnock(Player player) {
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

    private void processChangeStack(Player player) {
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

    private void processAskForCardViewResponse(Player player, SocketMessage message) {
        AskForCardView question = null;
        JsonElement hashCodeElement = message.jsonObject.get("hashCode");
        if (hashCodeElement != null) {
            question = askForViewMap.get(hashCodeElement.getAsInt());
        }
        if (question == null) {
            LOGGER.warn("Received invalid AskForView-Response: no question available");
            return;
        }
        askForViewMap.remove(question.hashCode);
        JsonElement valueElement = message.jsonObject.get("value");
        if (valueElement == null) {
            LOGGER.warn("Received invalid AskForView-Response: no value available");
            return;
        }
        if (valueElement.getAsBoolean() && attendees.contains(player)) {
            Collection<Player> viewerList = viewerMap.get(player);
            if (viewerList == null) {
                viewerList = new HashSet<>();
                viewerMap.put(player, viewerList);
            }
            Player viewer = getPlayer(question.source);
            viewerList.add(viewer);
            firePropertyChange(PROP_VIEWER_MAP, null, viewerMap);
            viewer.getSocket().sendString(getGameState(viewer));
            chat(question.source + " schaut bei " + player.getName() + " in die Karten.", true);
        } else {
            chat(player.getName() + " l&auml;sst " + question.source + " nicht in die Karten schauen.", true);
        }
    }

    private void processAskForCardView(Player player, String target) {
        if (attendees.contains(player)) {
            LOGGER.warn("Player '" + player.getName() + "' is in game and therefore not allowed to view other cards!");
            return;
        }
        Player targetPlayer = getPlayer(target);
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
        Collection<Player> viewerList = viewerMap.get(targetPlayer);
        if (viewerList != null && viewerList.contains(player)) {
            LOGGER.warn("Player '" + player.getName() + "' is already viewer of " + target + "!");
            return;
        }
        AskForCardView askForCardView = new AskForCardView(player);
        askForViewMap.put(askForCardView.hashCode, askForCardView);
        targetPlayer.getSocket().sendString(gson.toJson(askForCardView));
    }

    private void processAskForCardShowResponse(Player player, SocketMessage message) {
        AskForCardShow question = null;
        JsonElement hashCodeElement = message.jsonObject.get("hashCode");
        if (hashCodeElement != null) {
            question = askForShowMap.get(hashCodeElement.getAsInt());
        }
        if (question == null) {
            LOGGER.warn("Received invalid AskForShow-Response: no question available");
            return;
        }
        askForShowMap.remove(question.hashCode);
        JsonElement valueElement = message.jsonObject.get("value");
        if (valueElement == null) {
            LOGGER.warn("Received invalid AskForShow-Response: no value available");
            return;
        }
        if (valueElement.getAsBoolean()) {
            Player askingPlayer = getPlayer(question.source);
            if (attendees.contains(askingPlayer)) {
                Collection<Player> viewerList = viewerMap.get(askingPlayer);
                if (viewerList == null) {
                    viewerList = new HashSet<>();
                    viewerMap.put(askingPlayer, viewerList);
                }
                viewerList.add(player);
                firePropertyChange(PROP_VIEWER_MAP, null, viewerMap);
                player.getSocket().sendString(getGameState(player));
                chat(askingPlayer.getName() + " zeigt " + player.getName() + " die Karten.", true);
            }
        } else {
            chat(player.getName() + " m&ouml;chte die Karten von " + question.source + " nicht sehen.", true);
        }
    }

    private void processAskForCardShow(Player player, String target) {
        if (!attendees.contains(player)) {
            LOGGER.warn("Player '" + player.getName() + "' is not in game and therefore can't show cards!");
            return;
        }
        Player targetPlayer = getPlayer(target);
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
        Collection<Player> viewerList = viewerMap.get(player);
        if (viewerList != null && viewerList.contains(targetPlayer)) {
            LOGGER.warn("Player '" + target + "' is already viewer of " + player.getName() + "!");
            return;
        }
        AskForCardShow askForCardShow = new AskForCardShow(player);
        askForShowMap.put(askForCardShow.hashCode, askForCardShow);
        targetPlayer.getSocket().sendString(gson.toJson(askForCardShow));
    }

    private void processStopCardViewing(Player player, SocketMessage message) {
        String targetName = message.jsonObject.get("target").getAsString();
        Player targetPlayer = getPlayer(targetName);
        if (targetPlayer == null) {
            LOGGER.warn("Player '" + targetName + "' doesn't exist!");
            return;
        }
        Collection<Player> viewerList = viewerMap.get(targetPlayer);
        if (viewerList == null || !viewerList.contains(player)) {
            LOGGER.warn("Player '" + player.getName() + "' is not viewer of player '" + targetName + "'!");
            return;
        }
        viewerList.remove(player);
        firePropertyChange(PROP_VIEWER_MAP, null, viewerMap);
        player.getSocket().sendString(getGameState(player));
        chat(player.getName() + " schaut nicht mehr bei " + targetName + " in die Karten.", true);

    }

    private void processStopCardShowing(Player player, SocketMessage message) {
        String targetName = message.jsonObject.get("target").getAsString();
        Player targetPlayer = getPlayer(targetName);
        if (targetPlayer == null) {
            LOGGER.warn("Player '" + targetName + "' doesn't exist!");
            return;
        }
        Collection<Player> viewerList = viewerMap.get(player);
        if (viewerList == null || !viewerList.contains(targetPlayer)) {
            LOGGER.warn("Player '" + targetName + "' is not viewer of player '" + player.getName() + "'!");
            return;
        }
        viewerList.remove(targetPlayer);
        firePropertyChange(PROP_VIEWER_MAP, null, viewerMap);
        targetPlayer.getSocket().sendString(getGameState(targetPlayer));
        chat(player.getName() + " zeigt " + targetName + " die Karten nicht mehr.", true);
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

    boolean isFinishStack(List<Card> cards, Player player) {
        float score = getStackScore(cards);
        if (score > 30.5) {
            round.finisher = score > round.finishScore ? player : round.finisher;
            round.finishScore = Math.max(score, round.finishScore);
            LOGGER.debug("Player '" + player + "' finishes with " + score);
        }
        return round.finisher != null;
    }

    // search the stacks for 31 or FIRE 
    boolean isFinishStackExists() {
        boolean result = false;
        for (Player attendee : attendees) {
            if (isFinishStack(attendee.getStack(), attendee)) {
                result = true;
            }
        }
        return result;
        // disabled this, since the next player must have the chance to make fire, even if there is 31 in the game stack.
        // return  (isFinishStack(gameStack, getNextTo(mover)));
    }

    // search the stack for 7-8-9
    boolean is7_8_9(List<Card> cards) {
        return containsValue(cards, 7)
                && containsValue(cards, 8)
                && containsValue(cards, 9);
    }

    boolean containsValue(List<Card> cards, int value) {
        if (cards.stream().anyMatch((card) -> (card.getValue() == value))) {
            return true;
        }
        return false;
    }

    Round getRound() { // for unit testing
        return round;
    }

    int getFinishSoundCount() {
        return 13;
    }

    private void initFinishSoundIds() {
        int soundCount = getFinishSoundCount();
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

    int getNextFinishSoundId() {
        finishSoundIdCursor++;
        if (finishSoundIdCursor >= finishSoundIds.size()) {
            finishSoundIdCursor = 0;
        }
        return finishSoundIds.get(finishSoundIdCursor);
    }

    private void shiftMover() {
        mover = getNextTo(mover);
        LOGGER.debug("New mover: " + mover);
    }

    interface CardDealService {

        void dealCards(SchwimmenGame game);
    }

    private static class CardDealServiceImpl implements CardDealService {

        @Override
        public void dealCards(SchwimmenGame game) {
            game.shuffleStack();
            for (int i = 0; i < 3; i++) {
                game.attendees.forEach((attendee) -> {
                    attendee.getStack().add(game.getFromStack());
                    if (attendee.equals(game.mover)) {
                        game.dealerStack.add(game.getFromStack());
                    }
                });
            }
        }
    }

}
