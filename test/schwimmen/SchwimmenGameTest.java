package schwimmen;

import cardgame.Card;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
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
import schwimmen.SchwimmenGame.MOVE;
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
    private SchwimmenGame game;
    private SchwimmenPlayer player1;
    private SchwimmenPlayer player2;
    private SchwimmenPlayer player3;
    private Session session1;
    private Session session2;
    private Session session3;
    private TestSocket socket1;
    private TestSocket socket2;
    private TestSocket socket3;
    private final String name1 = "Player 1";
    private final String name2 = "Player 2";
    private final String name3 = "Player 3";
    private final Gson gson = new Gson();

    @Before
    public void setUp() {
        gameStack = Collections.synchronizedList(new ArrayList<>());
        game = new SchwimmenGame(gameStack, "");
        session1 = Mockito.mock(Session.class);
        session2 = Mockito.mock(Session.class);
        session3 = Mockito.mock(Session.class);
        socket1 = new TestSocket(game, name1, session1);
        socket2 = new TestSocket(game, name2, session2);
        socket3 = new TestSocket(game, name3, session3);
        player1 = new SchwimmenPlayer(name1, socket1);
        player2 = new SchwimmenPlayer(name2, socket2);
        player3 = new SchwimmenPlayer(name3, socket3);
        when(session1.isOpen()).thenReturn(Boolean.TRUE);
        when(session2.isOpen()).thenReturn(Boolean.TRUE);
        when(session3.isOpen()).thenReturn(Boolean.TRUE);
    }

    @Test
    public void testPlayerComparator() {
        List <SchwimmenPlayer> playerList = new ArrayList<>();
        List <SchwimmenPlayer> attendeeList = new ArrayList<>();
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
        assertTrue(game.isFinishStackExists());
        assertEquals(player2, game.getRound().finisher);
        assertEquals(31f, game.getRound().finishScore, 0f);
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
    public void testIsKnockAllowed() {
        startWith2Players();
        socket1.onText("{\"action\": \"dealCards\"}");
        socket1.onText("{\"action\": \"selectStack\", \"stack\": \"keep\"}");
        assertFalse(game.isKnockAllowed());
        swapCard(socket2);
        assertTrue(game.isKnockAllowed());
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

}
