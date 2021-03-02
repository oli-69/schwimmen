package schwimmen;

import cardgame.Card;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.eclipse.jetty.websocket.api.Session;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import schwimmen.SchwimmenGame.GAMEPHASE;
import schwimmen.SchwimmenGame.GAMERULE;
import schwimmen.SchwimmenGame.MOVE;
import schwimmen.messages.AskForCardShow;
import schwimmen.messages.AskForCardView;
import schwimmen.messages.ChatMessage;
import schwimmen.messages.DiscoverMessage;
import schwimmen.messages.DiscoverStack;
import schwimmen.messages.GamePhase;
import schwimmen.messages.LoginSuccess;

/**
 * Tests for class SchwimmenGame
 */
public class SchwimmenGameTest {

    static {
        Configurator.initialize(new DefaultConfiguration());
        Configurator.setRootLevel(Level.DEBUG);
    }
    private static final Logger LOGGER = LogManager.getLogger(SchwimmenGameTest.class);

    private List<Card> gameStack;
    private List<Card> dealerStack;
    private SchwimmenGame game;
    private SchwimmenPlayer player1;
    private SchwimmenPlayer player2;
    private SchwimmenPlayer player3;
    private SchwimmenPlayer player4;
    private SchwimmenPlayer player5;
    private Session session1;
    private Session session2;
    private Session session3;
    private Session session4;
    private Session session5;
    private TestSocket socket1;
    private TestSocket socket2;
    private TestSocket socket3;
    private TestSocket socket4;
    private TestSocket socket5;
    private List<List<Card>> dealCardStacks;
    private final String name1 = "Player 1";
    private final String name2 = "Player 2";
    private final String name3 = "Player 3";
    private final String name4 = "Player 4";
    private final String name5 = "Player 5";
    private final Gson gson = new Gson();

    @Before
    public void setUp() {
        gameStack = Collections.synchronizedList(new ArrayList<>());
        dealerStack = Collections.synchronizedList(new ArrayList<>());
        dealCardStacks = Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        dealCardStacks.forEach(stack -> make25(stack));
        game = new SchwimmenGame(gameStack, dealerStack, "", new CardDealServiceImpl());
        session1 = Mockito.mock(Session.class);
        session2 = Mockito.mock(Session.class);
        session3 = Mockito.mock(Session.class);
        session4 = Mockito.mock(Session.class);
        session5 = Mockito.mock(Session.class);
        socket1 = new TestSocket(game, name1, session1);
        socket2 = new TestSocket(game, name2, session2);
        socket3 = new TestSocket(game, name3, session3);
        socket4 = new TestSocket(game, name4, session4);
        socket5 = new TestSocket(game, name5, session5);
        player1 = new SchwimmenPlayer(name1, socket1);
        player2 = new SchwimmenPlayer(name2, socket2);
        player3 = new SchwimmenPlayer(name3, socket3);
        player4 = new SchwimmenPlayer(name4, socket4);
        player5 = new SchwimmenPlayer(name5, socket5);
        when(session1.isOpen()).thenReturn(Boolean.TRUE);
        when(session2.isOpen()).thenReturn(Boolean.TRUE);
        when(session3.isOpen()).thenReturn(Boolean.TRUE);
        when(session4.isOpen()).thenReturn(Boolean.TRUE);
        when(session5.isOpen()).thenReturn(Boolean.TRUE);
    }

    @Test
    public void testPlayerComparator() {
        List<SchwimmenPlayer> playerList = new ArrayList<>();
        List<SchwimmenPlayer> attendeeList = new ArrayList<>();
        playerList.add(player1);
        playerList.add(player2);
        playerList.add(player3);
        attendeeList.add(player2);
        attendeeList.add(player1);
        attendeeList.add(player3);

        // when
        attendeeList.sort(new SchwimmenGame.PlayerIdComparator(playerList));

        // then
        assertEquals(player1, attendeeList.get(0));
        assertEquals(player2, attendeeList.get(1));
        assertEquals(player3, attendeeList.get(2));
    }

    @Test
    public void testGetNextFinishSoundId() {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < game.getFinishSoundCount(); i++) {
            int cursor = game.getNextFinishSoundId();
            assertFalse(ids.contains(cursor));
            ids.add(cursor);
            LOGGER.info("FinishSound: " + cursor);
        }
        assertEquals(game.getFinishSoundCount(), ids.size());
    }

    @Test
    public void test31onDeal_player_keep() {
        startWith3Players();
        make30(dealCardStacks.get(1));
        make31(dealCardStacks.get(2));
        make25(dealCardStacks.get(3));
        socket1.onText("{\"action\": \"dealCards\"}");
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        assertEquals(3, player1.getGameTokens());
        assertEquals(3, player2.getGameTokens());
        assertEquals(2, player3.getGameTokens());
    }

    @Test
    public void test31onDeal_player_change() {
        startWith3Players();
        make25(dealCardStacks.get(0));
        make7_8_9(dealCardStacks.get(1));
        make31(dealCardStacks.get(2));
        make30(dealCardStacks.get(3));
        socket1.onText("{\"action\": \"dealCards\"}");
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"change\"}");
        assertEquals(2, player1.getGameTokens());
        assertEquals(3, player2.getGameTokens());
        assertEquals(3, player3.getGameTokens());
    }

    @Test
    public void test31onDeal_dealer() {
        startWith2Players();
        make31(dealCardStacks.get(1));
        socket1.onText("{\"action\": \"dealCards\"}");
        assertEquals(3, player1.getGameTokens());
        assertEquals(2, player2.getGameTokens());
    }

    @Test
    public void test31onDealChangedStack() {
        startWith2Players();
        make31(dealCardStacks.get(0));
        socket1.onText("{\"action\": \"dealCards\"}");
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"change\"}");
        assertEquals(3, player1.getGameTokens());
        assertEquals(2, player2.getGameTokens());
    }

    @Test
    public void testFireonDealChangedStack() {
        startWith2Players();
        makeFire(dealCardStacks.get(0));
        socket1.onText("{\"action\": \"dealCards\"}");
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"change\"}");
        assertEquals(3, player1.getGameTokens());
        assertEquals(2, player2.getGameTokens());
    }

    @Test
    public void testFireOnDeal_dealer() {
        startWith2Players();
        makeFire(dealCardStacks.get(1));
        socket1.onText("{\"action\": \"dealCards\"}");
        assertEquals(3, player1.getGameTokens());
        assertEquals(2, player2.getGameTokens());
    }

    @Test
    public void testFireOnDeal_player() {
        startWith2Players();
        makeFire(dealCardStacks.get(2));
        socket1.onText("{\"action\": \"dealCards\"}");
        assertEquals(2, player1.getGameTokens());
        assertEquals(3, player2.getGameTokens());
    }

    @Test
    public void testDiscover() {
        startWith2Players();
        assertEquals(3, player1.getGameTokens());
        assertEquals(3, player2.getGameTokens());

        // Runde 1
        socket1.onText("{\"action\": \"dealCards\"}");
        make31(player1.getStack());
        make25(player2.getStack());
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        assertEquals(3, player1.getGameTokens());
        assertEquals(2, player2.getGameTokens());

        // Runde 2
        socket2.onText("{\"action\": \"nextRound\"}");
        socket2.onText("{\"action\": \"dealCards\"}");
        make31(player1.getStack());
        make25(player2.getStack());
        socket2.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        assertEquals(3, player1.getGameTokens());
        assertEquals(1, player2.getGameTokens());

        // Runde 3
        socket1.onText("{\"action\": \"nextRound\"}");
        socket1.onText("{\"action\": \"dealCards\"}");
        make30(player1.getStack());
        make31(player2.getStack());
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        assertEquals(2, player1.getGameTokens());
        assertEquals(1, player2.getGameTokens());

        // Runde 4
        socket2.onText("{\"action\": \"nextRound\"}");
        socket2.onText("{\"action\": \"dealCards\"}");
        make31(player1.getStack());
        make25(player2.getStack());
        socket2.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        assertEquals(2, player1.getGameTokens());
        assertEquals(0, player2.getGameTokens()); // -> schwimmt

        // Runde 5
        socket1.onText("{\"action\": \"nextRound\"}");
        assertTrue(game.isAttendee(player1));
        assertTrue(game.isAttendee(player2));
        socket1.onText("{\"action\": \"dealCards\"}");
        make31(player1.getStack());
        make25(player2.getStack());
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        assertEquals(2, player1.getGameTokens());
        assertEquals(-1, player2.getGameTokens());

        // neues Spiel
        socket1.onText("{\"action\": \"nextRound\"}"); // spieler koennen ein/aussteigen
        socket2.onText("{\"action\": \"nextRound\"}"); // shuffle
        assertEquals(3, player1.getGameTokens());
        assertEquals(3, player2.getGameTokens());
        assertEquals(3, player1.getTotalTokens());
        assertEquals(-3, player2.getTotalTokens());
    }

    @Test
    public void testGetDiscoverMessage_DiscoveryStack() {
        startWith2Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        make31(player1.getStack());
        make25(player2.getStack());
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        DiscoverMessage message = game.getDiscoverMessage();
        DiscoverStack player1DiscStack = message.playerStacks[0];
        assertEquals(name1, player1DiscStack.player);
        assertEquals(31f, player1DiscStack.score, 0f);
        player1DiscStack.cards[0].color = player1.getStack().get(0).getColor();
        player1DiscStack.cards[0].value = player1.getStack().get(0).getValue();
        player1DiscStack.cards[1].color = player1.getStack().get(1).getColor();
        player1DiscStack.cards[1].value = player1.getStack().get(1).getValue();
        player1DiscStack.cards[2].color = player1.getStack().get(2).getColor();
        player1DiscStack.cards[2].value = player1.getStack().get(2).getValue();
        DiscoverStack player2DiscStack = message.playerStacks[1];
        assertEquals(name2, player2DiscStack.player);
        assertEquals(25f, player2DiscStack.score, 0f);
        assertNull(message.leavers);
        assertEquals(2, message.remainingAttendeesCount);
    }

    @Test
    public void testGetDiscoverMessage_onSingleKnockerFinish_badKnock() {
        startWith3Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        make25(player1.getStack());
        make25(player2.getStack());
        make25(player3.getStack());
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        swapCard(socket2);
        swapCard(socket3);
        swapCard(socket1);
        socket2.onText("{\"action\": \"knock\"}"); // klopft
        swapCard(socket3);
        assertNull(game.getDiscoverMessage()); // spiel muss noch laufen

        make30_5(player1.getStack());
        make30(player2.getStack());
        make30(player3.getStack());
        make21(gameStack);

        pass(socket1);
        DiscoverMessage message = game.getDiscoverMessage();  // spielende erreicht
        assertNull(message.finisher);
        assertEquals(0f, message.finisherScore, 0f);
        List<String> payers = Arrays.asList(message.payers);
        assertEquals(1, payers.size());
        assertTrue(payers.contains(name2)); // spieler 2 hat geklopft und muss bei punktgleichheit zahlen
    }

    @Test
    public void testGetDiscoverMessage_onSingleKnockerFinish_twoPayers() {
        startWith3Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        make25(player1.getStack());
        make25(player2.getStack());
        make25(player3.getStack());
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        swapCard(socket2);
        swapCard(socket3);
        swapCard(socket1);
        socket2.onText("{\"action\": \"knock\"}"); // klopft
        swapCard(socket3);
        assertNull(game.getDiscoverMessage()); // spiel muss noch laufen

        make25(player1.getStack());
        make30(player2.getStack());
        make25(player3.getStack());
        make21(gameStack);

        pass(socket1);
        DiscoverMessage message = game.getDiscoverMessage();  // spielende erreicht
        assertNull(message.finisher);
        assertEquals(0f, message.finisherScore, 0f);
        List<String> payers = Arrays.asList(message.payers);
        assertEquals(2, payers.size());
        assertTrue(payers.contains(name1));
        assertTrue(payers.contains(name3));
    }

    @Test
    public void testGetDiscoverMessage_onSingleKnockerFinish_equalPoints() {
        startWith3Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        make25(player1.getStack());
        make25(player2.getStack());
        make25(player3.getStack());
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");

        swapCard(socket2);
        swapCard(socket3);
        swapCard(socket1);

        socket2.onText("{\"action\": \"knock\"}"); // klopft
        swapCard(socket3);
        assertNull(game.getDiscoverMessage()); // spiel muss noch laufen

        make25(player1.getStack());
        make25(player2.getStack());
        make25(player3.getStack());
        make21(gameStack);

        pass(socket1);
        DiscoverMessage message = game.getDiscoverMessage();  // spielende erreicht

        // Spieler 2 hat geklopft und muss bei Punktgleichheit zahlen.
        assertNull(message.finisher);
        assertEquals(0f, message.finisherScore, 0f);
        assertEquals(1, message.payers.length);
        assertEquals(name2, message.payers[0]);
    }

    @Test
    public void testGetDiscoverMessage_onDoubleKnockerFinish_equalPoints() {
        startWith3Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        make25(player1.getStack());
        make25(player2.getStack());
        make25(player3.getStack());
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");

        swapCard(socket2);
        swapCard(socket3);

        socket1.onText("{\"action\": \"knock\"}"); // klopft
        swapCard(socket2);
        assertNull(game.getDiscoverMessage()); // spiel muss noch laufen

        make25(player1.getStack());
        make25(player2.getStack());
        make25(player3.getStack());
        make21(gameStack);

        socket3.onText("{\"action\": \"knock\"}"); // klopft
        DiscoverMessage message = game.getDiscoverMessage();  // spielende erreicht

        // Spieler 1 hat zuerst geklopft und muss bei Punktgleichheit zahlen.
        assertNull(message.finisher);
        assertEquals(0f, message.finisherScore, 0f);
        assertEquals(1, message.payers.length);
        assertEquals(name1, message.payers[0]);
    }

    @Test
    public void testGetDiscoverMessage_on31_Player2_twoPayer() {
        startWith3Players();
        socket1.onText("{\"action\": \"dealCards\"}");

        // when
        make25(player1.getStack());
        make31(player2.getStack());
        make25(player3.getStack());
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");

        // then
        DiscoverMessage message = game.getDiscoverMessage();
        assertEquals(name2, message.finisher);
        assertEquals(31f, message.finisherScore, 0f);
        assertNull(message.finishKnocker);
        List<String> payers = Arrays.asList(message.payers);
        assertEquals(2, payers.size());
        assertTrue(payers.contains(name1));
        assertTrue(payers.contains(name3));
    }

    @Test
    public void testGetDiscoverMessage_on31_Player1_onePayer() {
        startWith3Players();
        socket1.onText("{\"action\": \"dealCards\"}");

        // when
        make31(player1.getStack());
        make30_5(player2.getStack());
        make30(player3.getStack());
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");

        // then
        DiscoverMessage message = game.getDiscoverMessage();
        assertEquals(name1, message.finisher);
        assertEquals(31f, message.finisherScore, 0f);
        assertNull(message.finishKnocker);
        List<String> payers = Arrays.asList(message.payers);
        assertEquals(1, payers.size());
        assertTrue(payers.contains(name3));
    }

    @Test
    public void testGetDiscoverMessage_onFire_Player1() {
        startWith3Players();
        socket1.onText("{\"action\": \"dealCards\"}");

        // when
        makeFire(player1.getStack());
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");

        // then
        DiscoverMessage message = game.getDiscoverMessage();
        assertEquals(name1, message.finisher);
        assertEquals(33f, message.finisherScore, 0f);
        assertNull(message.finishKnocker);
        List<String> payers = Arrays.asList(message.payers);
        assertEquals(2, payers.size());
        assertTrue(payers.contains(name2));
        assertTrue(payers.contains(name3));
    }

    @Test
    public void testGetDiscoverMessage_onFire_Player2() {
        startWith3Players();
        socket1.onText("{\"action\": \"dealCards\"}");

        // when
        makeFire(player2.getStack());
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");

        // then
        DiscoverMessage message = game.getDiscoverMessage();
        assertEquals(name2, message.finisher);
        assertEquals(33f, message.finisherScore, 0f);
        assertNull(message.finishKnocker);
        List<String> payers = Arrays.asList(message.payers);
        assertEquals(2, payers.size());
        assertTrue(payers.contains(name1));
        assertTrue(payers.contains(name3));
    }

    @Test
    public void testGetDiscoverMessage_onFire_Player3() {
        startWith3Players();
        socket1.onText("{\"action\": \"dealCards\"}");

        // when
        makeFire(player3.getStack());
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");

        // then
        DiscoverMessage message = game.getDiscoverMessage();
        assertEquals(name3, message.finisher);
        assertEquals(33f, message.finisherScore, 0f);
        assertNull(message.finishKnocker);
        List<String> payers = Arrays.asList(message.payers);
        assertEquals(2, payers.size());
        assertTrue(payers.contains(name1));
        assertTrue(payers.contains(name2));
    }

    @Test
    public void testChat() {
        String message = "Hello World!";
        login(player1);
        login(player2);
        socket1.onText("{\"action\": \"chat\", \"text\":\"" + message + "\"}");
    }

    @Test
    public void testIsFinishStackExists_GameStack() {
        startWith2Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        make31(gameStack);
        assertFalse(game.isFinishStackExists());
    }

    @Test
    public void testIsFinishStackExists_P2() {
        startWith2Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        make31(player2.getStack());
        assertTrue(game.isFinishStackExists());
        assertEquals(player2, game.getRound().finisher);
        assertEquals(31f, game.getRound().finishScore, 0f);
    }

    @Test
    public void testIsFinishStackExists_P1() {
        startWith2Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        make31(player1.getStack());
        assertTrue(game.isFinishStackExists());
        assertEquals(player1, game.getRound().finisher);
        assertEquals(31f, game.getRound().finishScore, 0f);
    }

    @Test
    public void testIsFinishStack_31() {
        List<Card> cards = player1.getStack();
        assertFalse(game.isFinishStack(cards, player1));

        // Test Kreuz 25
        cards.clear();
        cards.add(new Card(4, 7));
        cards.add(new Card(4, 8));
        cards.add(new Card(4, 10));
        assertFalse(game.isFinishStack(cards, player1));

        // Test Karo 31
        make31(cards);
        assertTrue(game.isFinishStack(cards, player1));
    }

    @Test
    public void testIsFinishStack_Fire() {
        List<Card> cards = player1.getStack();
        cards.clear();
        cards.add(new Card(1, 14));
        cards.add(new Card(2, 14));
        cards.add(new Card(3, 14));
        assertTrue(game.isFinishStack(cards, player1));
    }

    @Test
    public void testGetStackScore() {
        List<Card> cards = new ArrayList<>();
        assertEquals(0, game.getStackScore(cards), 0.0);

        // Test Feuer
        makeFire(cards);
        assertEquals(33, game.getStackScore(cards), 0.0);

        // Test 30.5
        make30_5(cards);
        assertEquals(30.5, game.getStackScore(cards), 0.0);

        // Test Karo 31
        make31(cards);
        assertEquals(31, game.getStackScore(cards), 0.0);

        // Test Kreuz 25
        make25(cards);
        assertEquals(25, game.getStackScore(cards), 0.0);

        // Test 21
        make21(cards);
        assertEquals(21, game.getStackScore(cards), 0.0);
    }

    @Test
    public void testIs_7_8_9() {
        // Test Kreuz 25
        List<Card> cards = new ArrayList<>();
        make25(cards);
        assertFalse(game.is7_8_9(cards));

        // Test 7,8,9
        make7_8_9(cards);
        assertTrue(game.is7_8_9(cards));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnableGameRules_fail() {
        // test in wrong game phase
        startWith3Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        game.setGameRuleEnabled(GAMERULE.newCardsOn789, true);
    }

    @Test
    public void testEnableGameRules() {
        assertFalse(game.isGameRuleEnabled(GAMERULE.newCardsOn789));
        assertFalse(game.isGameRuleEnabled(GAMERULE.passOnlyOncePerRound));

        game.setGameRuleEnabled(GAMERULE.newCardsOn789, true);
        assertTrue(game.isGameRuleEnabled(GAMERULE.newCardsOn789));
        game.setGameRuleEnabled(GAMERULE.passOnlyOncePerRound, true);
        assertTrue(game.isGameRuleEnabled(GAMERULE.passOnlyOncePerRound));

        // add a second time, then remove. Game rule must be disabled
        game.setGameRuleEnabled(GAMERULE.newCardsOn789, true);
        game.setGameRuleEnabled(GAMERULE.passOnlyOncePerRound, true);
        game.setGameRuleEnabled(GAMERULE.newCardsOn789, false);
        game.setGameRuleEnabled(GAMERULE.passOnlyOncePerRound, false);
        assertFalse(game.isGameRuleEnabled(GAMERULE.newCardsOn789));
        assertFalse(game.isGameRuleEnabled(GAMERULE.passOnlyOncePerRound));

        // remove a second time. Must not result in an exception
        game.setGameRuleEnabled(GAMERULE.newCardsOn789, false);
        game.setGameRuleEnabled(GAMERULE.passOnlyOncePerRound, false);
        assertFalse(game.isGameRuleEnabled(GAMERULE.newCardsOn789));
        assertFalse(game.isGameRuleEnabled(GAMERULE.passOnlyOncePerRound));
    }

    @Test
    public void testGetAttendeesCount() {
        assertEquals(0, game.getAttendeesCount());
        login(player1);
        assertEquals(1, game.getAttendeesCount());
        login(player2);
        assertEquals(2, game.getAttendeesCount());
        socket1.onText("{\"action\": \"removeFromAttendees\"}");
        assertEquals(1, game.getAttendeesCount());
        socket1.onText("{\"action\": \"addToAttendees\"}");
        assertEquals(2, game.getAttendeesCount());
    }

    @Test
    public void testGetGamePhase() {
        startWith2Players();
        assertEquals(GAMEPHASE.shuffle, game.getGamePhase());
    }

    @Test
    public void testGetMover() {
        startWith2Players();
        assertEquals(player1, game.getMover());

        // wählt Karten
        socket1.onText("{\"action\": \"dealCards\"}");
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        assertEquals(player2, game.getMover());

        // tauscht
        swapCard(socket2);
        assertFalse(game.isChangeStackAllowed(player1));
        assertFalse(game.isChangeStackAllowed(player2));
        assertEquals(player1, game.getMover());

        // Spieler 2 ist nicht dran
        swapCard(socket2);
        assertEquals(player1, game.getMover()); // mover darf nicht weiterspringen

        // schiebt
        pass(socket1);
        assertFalse(game.isChangeStackAllowed(player1));
        assertFalse(game.isChangeStackAllowed(player2));
        assertEquals(player2, game.getMover());

        // schiebt
        pass(socket2);
        assertTrue(game.isChangeStackAllowed(player1)); // -> darf jetzt neue Karten anfordern
        assertFalse(game.isChangeStackAllowed(player2));
        assertEquals(player1, game.getMover());

        // tauscht die karten
        socket1.onText("{\"action\": \"changeStack\"}");
        assertEquals(player1, game.getMover()); // mover darf nicht weiterspringen
    }

    @Test
    public void testGetPlayertMove() {
        startWith2Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        assertEquals(MOVE.selectStack.name(), game.getPlayertMove().move);
    }

    @Test
    public void testIsChangeStackAllowed() {
        startWith2Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        assertFalse(game.isChangeStackAllowed(player1));
        assertFalse(game.isChangeStackAllowed(player2));

        // tauscht
        swapCard(socket2);
        assertFalse(game.isChangeStackAllowed(player1));
        assertFalse(game.isChangeStackAllowed(player2));

        // schiebt
        socket1.onText("{\"action\": \"pass\"}");
        assertFalse(game.isChangeStackAllowed(player1));
        assertFalse(game.isChangeStackAllowed(player2));

        // tauscht
        swapCard(socket2);
        assertFalse(game.isChangeStackAllowed(player1));
        assertFalse(game.isChangeStackAllowed(player2));

        // schiebt
        socket1.onText("{\"action\": \"pass\"}");
        assertFalse(game.isChangeStackAllowed(player1));
        assertFalse(game.isChangeStackAllowed(player2));
        // schiebt
        pass(socket2);
        assertTrue(game.isChangeStackAllowed(player1)); // -> darf jetzt neue Karten anfordern
        assertFalse(game.isChangeStackAllowed(player2));
    }

    @Test
    public void testIsChangeStackAllowedOn789() {
        game.setGameRuleEnabled(GAMERULE.newCardsOn789, true);
        startWith2Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        assertFalse(game.isChangeStackAllowed(player1));
        assertFalse(game.isChangeStackAllowed(player2));

        // tauscht
        swapCard(socket2);
        assertFalse(game.isChangeStackAllowed(player1));
        assertFalse(game.isChangeStackAllowed(player2));

        make7_8_9(gameStack);
        assertTrue(game.isChangeStackAllowed(player1));
        assertFalse(game.isChangeStackAllowed(player2));
    }

    @Test
    public void testIsKnockAllowed() {
        startWith2Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        assertFalse(game.isKnockAllowed());
        swapCard(socket2);
        assertTrue(game.isKnockAllowed());
    }

    @Test
    public void testIsPassAllowed() {
        startWith2Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        assertFalse(game.isPassAllowed(player1));
        assertTrue(game.isPassAllowed(player2));
        pass(socket2);
        assertTrue(game.isPassAllowed(player1));
        assertFalse(game.isPassAllowed(player2));
        swapCard(socket1);
        assertFalse(game.isPassAllowed(player1));
        assertTrue(game.isPassAllowed(player2));
        pass(socket2);
        assertTrue(game.isPassAllowed(player1));
        assertFalse(game.isPassAllowed(player2));
    }

    @Test
    public void testIsPassAllowedOnPassOnce() {
        game.setGameRuleEnabled(GAMERULE.passOnlyOncePerRound, true);
        startWith2Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        assertFalse(game.isPassAllowed(player1));
        assertTrue(game.isPassAllowed(player2));
        pass(socket2);
        assertTrue(game.isPassAllowed(player1));
        assertFalse(game.isPassAllowed(player2));
        swapCard(socket1);
        assertFalse(game.isPassAllowed(player1));
        assertFalse(game.isPassAllowed(player2));
        swapCard(socket2);
        assertTrue(game.isPassAllowed(player1));
        assertFalse(game.isPassAllowed(player2));
        pass(socket1);
        assertFalse(game.isPassAllowed(player1));
        assertFalse(game.isPassAllowed(player2));
        swapCard(socket2);
        assertFalse(game.isPassAllowed(player1));
        assertFalse(game.isPassAllowed(player2));
    }

    @Test
    public void testAskForCardShow_fail_askingPlayerIsNotAttendee() {
        startWith3Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);
        int messageCount = socket2.messageBuff.size();
        socket1.onText("{\"action\": \"askForCardShow\", \"target\": \"Player 2\"}");
        assertEquals(messageCount, socket2.messageBuff.size());
    }

    @Test
    public void testAskForCardShow_fail_targetPlayerDoesntExist() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);
        int messageCount = socket1.messageBuff.size();
        socket2.onText("{\"action\": \"askForCardShow\", \"target\": \"random-name\"}");
        assertEquals(messageCount, socket1.messageBuff.size());
    }

    @Test
    public void testAskForCardShow_fail_targetPlayerIsAttendee() {
        startWith5Players();
        game.getRound().leavers.add(player2);
        game.removeAttendee(player1);
        int messageCount = socket2.messageBuff.size();
        socket3.onText("{\"action\": \"askForCardShow\", \"target\": \"Player 2\"}");
        assertEquals(messageCount, socket2.messageBuff.size());
    }

    @Test
    public void testAskForCardShow_response_doubleclick() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);
        Runnable r = () -> {
            socket2.onText("{\"action\": \"askForCardShow\", \"target\": \"Player 1\"}");
            AskForCardShow question = gson.fromJson(socket1.lastMessage(), AskForCardShow.class);
            socket1.onText("{\"action\": \"askForCardShowResponse\", \"hashCode\": \"" + question.hashCode + "\", \"value\": \"true\"}");
        };
        Thread thread1 = new Thread(r);
        Thread thread2 = new Thread(r);
        thread1.start();
        thread2.start();
        try {
            while (thread2.isAlive() && thread1.isAlive()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException ex) {
            System.out.println(ex);
        }
    }

    @Test
    public void testAskForCardShow_response_yes() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);

        // ask
        int messageCount = socket1.messageBuff.size();
        socket2.onText("{\"action\": \"askForCardShow\", \"target\": \"Player 1\"}");
        assertEquals(messageCount + 1, socket1.messageBuff.size());
        AskForCardShow question = gson.fromJson(socket1.lastMessage(), AskForCardShow.class);
        assertEquals(name2, question.source);

        // response yes
        messageCount = socket2.messageBuff.size();
        socket1.onText("{\"action\": \"askForCardShowResponse\", \"hashCode\": \"" + question.hashCode + "\", \"value\": \"true\"}");
        assertEquals(messageCount + 2, socket2.messageBuff.size());
        ChatMessage chatMessage = gson.fromJson(socket1.lastMessage(), ChatMessage.class);
        assertEquals(name2 + " zeigt " + name1 + " die Karten.", chatMessage.text);
        Collection<SchwimmenPlayer> viewerList = game.getViewerMap().get(player2);
        assertEquals(1, viewerList.size());
        assertEquals(player1, viewerList.iterator().next());
    }

    @Test
    public void testAskForCardShow_response_no() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);

        // ask
        int messageCount = socket1.messageBuff.size();
        socket2.onText("{\"action\": \"askForCardShow\", \"target\": \"Player 1\"}");
        assertEquals(messageCount + 1, socket1.messageBuff.size());
        AskForCardView question = gson.fromJson(socket1.lastMessage(), AskForCardView.class);
        assertEquals(name2, question.source);

        // response no
        messageCount = socket2.messageBuff.size();
        socket1.onText("{\"action\": \"askForCardShowResponse\", \"hashCode\": \"" + question.hashCode + "\", \"value\": \"false\"}");
        assertEquals(messageCount + 1, socket2.messageBuff.size());
        ChatMessage chatMessage = gson.fromJson(socket2.lastMessage(), ChatMessage.class);
        assertEquals(name1 + " m&ouml;chte die Karten von " + name2 + " nicht sehen.", chatMessage.text);
    }

    @Test
    public void testAskForCardShow_fail_targetPlayerIsAlreadyViewer() {
        startWith5Players();
        game.getRound().leavers.add(player2);
        game.removeAttendee(player2);
        socket1.onText("{\"action\": \"askForCardShow\", \"target\": \"Player 2\"}");
        AskForCardView question = gson.fromJson(socket2.lastMessage(), AskForCardView.class);
        socket2.onText("{\"action\": \"askForCardShowResponse\", \"hashCode\": \"" + question.hashCode + "\", \"value\": \"true\"}");

        int messageCount = socket2.messageBuff.size();
        socket1.onText("{\"action\": \"askForCardShow\", \"target\": \"Player 2\"}");
        assertEquals(messageCount, socket2.messageBuff.size());
    }

    @Test
    public void testAskForCardShowResponse_fail_noQuestion() {
        startWith2Players();
        socket2.onText("{\"action\": \"askForCardShowResponse\", \"value\": \"true\"}");
        socket2.onText("{\"action\": \"askForCardShowResponse\", \"hashCode\": \"123456\", \"value\": \"true\"}");
    }

    @Test
    public void testAskForCardShowResponse_fail_noValue() {
        startWith5Players();
        game.getRound().leavers.add(player2);
        game.removeAttendee(player2);
        socket1.onText("{\"action\": \"askForCardShow\", \"target\": \"Player 2\"}");
        AskForCardShow question = gson.fromJson(socket2.lastMessage(), AskForCardShow.class);
        socket2.onText("{\"action\": \"askForCardViewResponse\", \"hashCode\": \"" + question.hashCode + "\"}");
    }

    @Test
    public void testAskForCardView_fail_askingPlayerIsAttendee() {
        startWith2Players();
        int messageCount = socket2.messageBuff.size();
        socket1.onText("{\"action\": \"askForCardView\", \"target\": \"Player 2\"}");
        assertEquals(messageCount, socket2.messageBuff.size());
    }

    @Test
    public void testAskForCardView_fail_targetPlayerDoesntExist() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);
        int messageCount = socket2.messageBuff.size();
        socket1.onText("{\"action\": \"askForCardView\", \"target\": \"random-name\"}");
        assertEquals(messageCount, socket2.messageBuff.size());
    }

    @Test
    public void testAskForCardView_fail_targetPlayerIsNotAttendee() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.getRound().leavers.add(player2);
        game.removeAttendee(player1);
        game.removeAttendee(player2);
        int messageCount = socket2.messageBuff.size();
        socket1.onText("{\"action\": \"askForCardView\", \"target\": \"Player 2\"}");
        assertEquals(messageCount, socket2.messageBuff.size());
    }

    @Test
    public void testAskForCardView_response_yes() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);

        // ask
        int messageCount = socket2.messageBuff.size();
        socket1.onText("{\"action\": \"askForCardView\", \"target\": \"Player 2\"}");
        assertEquals(messageCount + 1, socket2.messageBuff.size());
        AskForCardView question = gson.fromJson(socket2.lastMessage(), AskForCardView.class);
        assertEquals(name1, question.source);

        // response yes
        messageCount = socket1.messageBuff.size();
        socket2.onText("{\"action\": \"askForCardViewResponse\", \"hashCode\": \"" + question.hashCode + "\", \"value\": \"true\"}");
        assertEquals(messageCount + 3, socket1.messageBuff.size());
        ChatMessage chatMessage = gson.fromJson(socket1.lastMessage(), ChatMessage.class);
        assertEquals(name1 + " schaut bei " + name2 + " in die Karten.", chatMessage.text);
        Collection<SchwimmenPlayer> viewerList = game.getViewerMap().get(player2);
        assertEquals(1, viewerList.size());
        assertEquals(player1, viewerList.iterator().next());
    }

    @Test
    public void testAskForCardView_response_no() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);

        // ask
        int messageCount = socket2.messageBuff.size();
        socket1.onText("{\"action\": \"askForCardView\", \"target\": \"Player 2\"}");
        assertEquals(messageCount + 1, socket2.messageBuff.size());
        AskForCardView question = gson.fromJson(socket2.lastMessage(), AskForCardView.class);
        assertEquals(name1, question.source);

        // response no
        messageCount = socket1.messageBuff.size();
        socket2.onText("{\"action\": \"askForCardViewResponse\", \"hashCode\": \"" + question.hashCode + "\", \"value\": \"false\"}");
        assertEquals(messageCount + 1, socket1.messageBuff.size());
        ChatMessage chatMessage = gson.fromJson(socket1.lastMessage(), ChatMessage.class);
        assertEquals(name2 + " l&auml;sst " + name1 + " nicht in die Karten schauen.", chatMessage.text);
    }

    @Test
    public void testAskForCardView_response_doubleclick() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);
        Runnable r = () -> {
            socket1.onText("{\"action\": \"askForCardView\", \"target\": \"Player 2\"}");
            AskForCardShow question = gson.fromJson(socket1.lastMessage(), AskForCardShow.class);
            socket2.onText("{\"action\": \"askForCardViewResponse\", \"hashCode\": \"" + question.hashCode + "\", \"value\": \"true\"}");
        };
        Thread thread1 = new Thread(r);
        Thread thread2 = new Thread(r);

        thread1.start();
        thread2.start();
        try {
            while (thread2.isAlive() && thread1.isAlive()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException ex) {
            System.out.println(ex);
        }
    }

    @Test
    public void testAskForCardView_fail_askingPlayerIsAlreadyViewer() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);
        socket1.onText("{\"action\": \"askForCardView\", \"target\": \"Player 2\"}");
        AskForCardView question = gson.fromJson(socket2.lastMessage(), AskForCardView.class);
        socket2.onText("{\"action\": \"askForCardViewResponse\", \"hashCode\": \"" + question.hashCode + "\", \"value\": \"true\"}");

        int messageCount = socket2.messageBuff.size();
        socket1.onText("{\"action\": \"askForCardView\", \"target\": \"Player 2\"}");
        assertEquals(messageCount, socket2.messageBuff.size());
    }

    @Test
    public void testAskForCardViewResponse_fail_noQuestion() {
        startWith2Players();
        socket2.onText("{\"action\": \"askForCardViewResponse\", \"value\": \"true\"}");
        socket2.onText("{\"action\": \"askForCardViewResponse\", \"hashCode\": \"123456\", \"value\": \"true\"}");
    }

    @Test
    public void testAskForCardViewResponse_fail_noValue() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);
        socket1.onText("{\"action\": \"askForCardView\", \"target\": \"Player 2\"}");
        AskForCardView question = gson.fromJson(socket2.lastMessage(), AskForCardView.class);
        socket2.onText("{\"action\": \"askForCardViewResponse\", \"hashCode\": \"" + question.hashCode + "\"}");
    }

    @Test
    public void testStopCardViewing_fail_invalidPlayer() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);
        socket1.onText("{\"action\": \"askForCardView\", \"target\": \"Player 2\"}");
        AskForCardView question = gson.fromJson(socket2.lastMessage(), AskForCardView.class);
        socket2.onText("{\"action\": \"askForCardViewResponse\", \"hashCode\": \"" + question.hashCode + "\", \"value\": \"true\"}");

        // stop viewing
        int messageCount = socket1.messageBuff.size();
        socket1.onText("{\"action\": \"stopCardViewing\", \"target\": \"nobody\"}");
        assertEquals(messageCount, socket1.messageBuff.size());
    }

    @Test
    public void testStopCardViewing_fail_playerNotViewer() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);
        socket1.onText("{\"action\": \"askForCardView\", \"target\": \"Player 2\"}");
        AskForCardView question = gson.fromJson(socket2.lastMessage(), AskForCardView.class);
        socket2.onText("{\"action\": \"askForCardViewResponse\", \"hashCode\": \"" + question.hashCode + "\", \"value\": \"true\"}");

        // stop viewing
        int messageCount = socket3.messageBuff.size();
        socket3.onText("{\"action\": \"stopCardViewing\", \"target\": \"Player 2\"}");
        assertEquals(messageCount, socket3.messageBuff.size());
    }

    @Test
    public void testStopCardViewing_success() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);
        socket1.onText("{\"action\": \"askForCardView\", \"target\": \"Player 2\"}");
        AskForCardView question = gson.fromJson(socket2.lastMessage(), AskForCardView.class);
        socket2.onText("{\"action\": \"askForCardViewResponse\", \"hashCode\": \"" + question.hashCode + "\", \"value\": \"true\"}");

        // stop viewing
        int messageCount = socket1.messageBuff.size();
        socket1.onText("{\"action\": \"stopCardViewing\", \"target\": \"Player 2\"}");
        assertEquals(messageCount + 3, socket1.messageBuff.size());
        ChatMessage chatMessage = gson.fromJson(socket1.lastMessage(), ChatMessage.class);
        assertEquals(name1 + " schaut nicht mehr bei " + name2 + " in die Karten.", chatMessage.text);
    }

    @Test
    public void testStopCardShowing_fail_invalidPlayer() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);
        socket1.onText("{\"action\": \"askForCardView\", \"target\": \"Player 2\"}");
        AskForCardView question = gson.fromJson(socket2.lastMessage(), AskForCardView.class);
        socket2.onText("{\"action\": \"askForCardViewResponse\", \"hashCode\": \"" + question.hashCode + "\", \"value\": \"true\"}");

        // stop showing
        int messageCount = socket2.messageBuff.size();
        socket2.onText("{\"action\": \"stopCardShowing\", \"target\": \"no one\"}");
        assertEquals(messageCount, socket2.messageBuff.size());
    }

    @Test
    public void testStopCardShowing_fail_targetNotViewer() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);
        socket1.onText("{\"action\": \"askForCardView\", \"target\": \"Player 2\"}");
        AskForCardView question = gson.fromJson(socket2.lastMessage(), AskForCardView.class);
        socket2.onText("{\"action\": \"askForCardViewResponse\", \"hashCode\": \"" + question.hashCode + "\", \"value\": \"true\"}");

        // stop showing
        int messageCount = socket2.messageBuff.size();
        socket2.onText("{\"action\": \"stopCardShowing\", \"target\": \"Player 3\"}");
        assertEquals(messageCount, socket2.messageBuff.size());
    }

    @Test
    public void testStopCardShowing_success() {
        startWith5Players();
        game.getRound().leavers.add(player1);
        game.removeAttendee(player1);
        socket1.onText("{\"action\": \"askForCardView\", \"target\": \"Player 2\"}");
        AskForCardView question = gson.fromJson(socket2.lastMessage(), AskForCardView.class);
        socket2.onText("{\"action\": \"askForCardViewResponse\", \"hashCode\": \"" + question.hashCode + "\", \"value\": \"true\"}");

        // stop showing
        int messageCount = socket2.messageBuff.size();
        socket2.onText("{\"action\": \"stopCardShowing\", \"target\": \"Player 1\"}");
        assertEquals(messageCount + 2, socket2.messageBuff.size());
        ChatMessage chatMessage = gson.fromJson(socket1.lastMessage(), ChatMessage.class);
        assertEquals(name2 + " zeigt " + name1 + " die Karten nicht mehr.", chatMessage.text);
    }

    @Test
    public void testStartStopGame() {
        LOGGER.info("Login the Player");
        login(player1);

        LOGGER.info("Start the Game");
        game.startGame();
        GamePhase startResult = gson.fromJson(socket1.lastMessage(), GamePhase.class);
        assertEquals("gamePhase", startResult.action);
        assertEquals("shuffle", startResult.phase);
        assertEquals(name1, startResult.actor);

        LOGGER.info("Stop the Game");
        game.stopGame();
        GamePhase stopResult = gson.fromJson(socket1.lastMessage(), GamePhase.class);
        assertEquals("gamePhase", stopResult.action);
        assertEquals("waitForAttendees", stopResult.phase);
        assertEquals(player1.getName(), stopResult.actor);
    }

    // Debug the Problem that the game discovered automatically when the last 
    // player in a knocked round changed 789
    @Test
    public void testNewCardOn789_knocked() {
        game.setGameRuleEnabled(GAMERULE.newCardsOn789, true);
        startWith3Players();
        make7_8_9(player1.getStack());
        make7_8_9(player2.getStack());
        make7_8_9(player3.getStack());
        socket1.onText("{\"action\": \"dealCards\"}");
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        swapCard(socket2);
        swapCard(socket3);
        socket1.onText("{\"action\": \"knock\"}"); // klopft
        swapCard(socket2);
        make7_8_9(gameStack);
        assertTrue(game.isChangeStackAllowed(player3));
        socket3.onText("{\"action\": \"changeStack\"}"); // neue Karten bei 7/8/9
        GamePhase gamePhase = gson.fromJson(socket1.lastMessage(), GamePhase.class);
        assertEquals("waitForPlayerMove", gamePhase.phase); // player 3 must still be the mover
        pass(socket3);
        gamePhase = gson.fromJson(socket1.lastMessage(), GamePhase.class);
        assertEquals("discover", gamePhase.phase);
    }

    private void make21(List<Card> cards) {
        cards.clear();
        cards.add(new Card(3, 14));
        cards.add(new Card(4, 11));
        cards.add(new Card(3, 10));
    }

    private void make25(List<Card> cards) {
        cards.clear();
        cards.add(new Card(4, 7));
        cards.add(new Card(4, 8));
        cards.add(new Card(4, 10));
    }

    private void make30(List<Card> cards) {
        cards.clear();
        cards.add(new Card(3, 12));
        cards.add(new Card(3, 11));
        cards.add(new Card(3, 10));
    }

    private void make30_5(List<Card> cards) {
        cards.clear();
        int value = (int) (7 + (Math.random() * 4));
        cards.add(new Card(1, value));
        cards.add(new Card(2, value));
        cards.add(new Card(3, value));
    }

    private void make31(List<Card> cards) {
        cards.clear();
        cards.add(new Card(1, 10));
        cards.add(new Card(1, 11));
        cards.add(new Card(1, 14));
    }

    private void makeFire(List<Card> cards) {
        cards.clear();
        cards.add(new Card(1, 14));
        cards.add(new Card(2, 14));
        cards.add(new Card(3, 14));
    }

    private void make7_8_9(List<Card> cards) {
        cards.clear();
        cards.add(new Card(1, 7));
        cards.add(new Card(2, 8));
        cards.add(new Card(3, 9));
    }

    private void pass(TestSocket socket) {
        socket.onText("{\"action\": \"pass\"}");
    }

    private void swapCard(TestSocket socket) {
        socket.onText("{\"action\": \"swapCard\", \"playerStack\": \"0\", \"gameStack\": \"0\"}");
    }

    private void login(SchwimmenPlayer player) {
        game.addPlayerToRoom(player);
        player.getSocket().sendString(gson.toJson(new LoginSuccess("roomName")));
        player.getSocket().sendString(gson.toJson(game.getGameState(player)));
    }

    private void startWith2Players() {
        login(player1);
        login(player2);
        game.startGame();
    }

    private void startWith3Players() {
        login(player1);
        login(player2);
        login(player3);
        game.startGame();
    }

    private void startWith5Players() {
        login(player1);
        login(player2);
        login(player3);
        login(player4);
        login(player5);
        game.startGame();
    }

    private static class TestSocket extends SchwimmenSocket {

        String name;
        List<String> messageBuff = new ArrayList();
        boolean connected = true;
        Session session;

        public TestSocket(SchwimmenGame game, String name, Session session) {
            super(game);
            this.name = name;
            this.session = session;
        }

        @Override
        public void sendString(String buff) {
            LOGGER.debug(name + " received: " + buff);
            messageBuff.add(buff);
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        public void onText(String message) {
            try {
                super.onText(session, message);
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }

        public String lastMessage() {
            return !messageBuff.isEmpty() ? messageBuff.get(messageBuff.size() - 1) : "";
        }

        public boolean isLastMessage(String message) {
            return !messageBuff.isEmpty() && messageBuff.get(messageBuff.size() - 1).equals(message);
        }

        public boolean receivedMessage(String message) {
            return messageBuff.stream().anyMatch((msg) -> (message.equals(msg)));
        }
    }

    private class CardDealServiceImpl implements SchwimmenGame.CardDealService {

        @Override
        public void dealCards(SchwimmenGame game) {
            dealerStack.clear();
            player1.getStack().clear();
            player2.getStack().clear();
            player3.getStack().clear();
            player4.getStack().clear();
            player5.getStack().clear();
            dealerStack.addAll(dealCardStacks.get(0));
            player1.getStack().addAll(dealCardStacks.get(1));
            player2.getStack().addAll(dealCardStacks.get(2));
            player3.getStack().addAll(dealCardStacks.get(3));
            player4.getStack().addAll(dealCardStacks.get(4));
            player5.getStack().addAll(dealCardStacks.get(5));
        }
    }

}
