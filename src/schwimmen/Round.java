package schwimmen;

import cardgame.Card;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import schwimmen.SchwimmenGame.GAMERULE;
import schwimmen.SchwimmenGame.MOVE;
import schwimmen.messages.PlayerMove;

/**
 * State object representing a game round. A game round is the part of the game
 * which ends with a score of 31, fire or 2nd knocking.
 */
public class Round {

    public SchwimmenPlayer dealer; // the card dealer of this round.
    public SchwimmenPlayer finisher; // filled if the round ended by knocking
    public SchwimmenPlayer knocker1;
    public SchwimmenPlayer knocker2;
    public float finishScore; // filled, if the game ended by 31 or fire.
    public int moveCount; // counter used by game rules.
    public List<SchwimmenPlayer> leavers = new ArrayList<>(); // leavers after this round, meaning payed and death

    private final List<SchwimmenPlayer> passSequence = new ArrayList<>(); // list of players passing in a row (used by game rules)
    private final Set<SchwimmenPlayer> passers = new HashSet<>(); // list of players having passed once in this round (used by game rules)
    private final SchwimmenGame game; // reference to the game.

    /**
     * Constructor. Creates an instance of this class.
     *
     * @param game reference to the game.
     */
    public Round(SchwimmenGame game) {
        this.game = game;
    }

    /**
     * Resets the round. Usually called at begin of a new round.
     *
     * @param dealer card dealer of this round.
     */
    public void reset(SchwimmenPlayer dealer) {
        this.dealer = dealer;
        finisher = null;
        knocker1 = null;
        knocker2 = null;
        passSequence.clear();
        passers.clear();
        finishScore = 0f;
        moveCount = 0;
        leavers.clear();
    }

    /**
     * Set the curren player move (then player's decision).
     *
     * @param move the player move.
     */
    public void setPlayerMove(PlayerMove move) {
        if (!MOVE.pass.toString().equals(move.move)) {
            passSequence.clear(); // the row was interrupted
        }
        if (!MOVE.changeStack.toString().equals(move.move)) {
            moveCount++;
        }
    }

    /**
     * The current player passed.
     *
     * @param player current player.
     */
    public void pass(SchwimmenPlayer player) {
        passSequence.add(player);
        passers.add(player);
    }

    /**
     * The count of passers in a row. Used by game rules to find the point when
     * selecting new cards is allowed.
     *
     * @return the number of passers in a row.
     */
    public int getPassCount() {
        return passSequence.size();
    }

    /**
     * Getter for property knockAllowed
     *
     * @return true if the current player is allowed to knock, false otherwise.
     */
    public boolean isKnockAllowed() {
        return !(moveCount < game.getAttendeesCount());
    }

    /**
     * Getter for property changeStackAllowed.
     *
     * @param player the player for which it is asked for.
     * @param gameStack the cards in the middle
     * @return true it the specified player is allowed to change the stack (get
     * new cards if all players passed in a row, or the 7-8-9 rule), false
     * otherwise.
     */
    public boolean isChangeStackAllowed(SchwimmenPlayer player, List<Card> gameStack) {
        boolean allPlayersPassed = !passSequence.isEmpty() && !(passSequence.size() < game.getAttendeesCount());
        boolean is_7_8_9 = game.is7_8_9(gameStack) && game.isGameRuleEnabled(GAMERULE.newCardsOn789);
        return player.equals(game.getMover()) && (allPlayersPassed || is_7_8_9);
    }

    /**
     * Getter for property passAllowed
     *
     * @param player the player for which it is asked for.
     * @return true it the specified player is allowed to pass. Can be false, if
     * the game rule pass-once is enabled.
     */
    public boolean isPassAllowed(SchwimmenPlayer player) {
        boolean isAllowed = (!game.isGameRuleEnabled(GAMERULE.passOnlyOncePerRound)) || !passers.contains(player);
        return player.equals(game.getMover()) && isAllowed;
    }

    /**
     * The player knocks.
     *
     * @param player the knocking player.
     */
    public void knock(SchwimmenPlayer player) {
        if (knocker1 != null) {
            knocker2 = player;
        } else {
            knocker1 = player;
        }
    }

    /**
     * Getter for property knock count.
     *
     * @return 0, 1 or 2.
     */
    public int getKnockCount() {
        if (knocker2 != null) {
            return 2;
        }
        if (knocker1 != null) {
            return 1;
        }
        return 0;
    }

    /**
     * Getter for property knockPriority for a player. Used by the gamerules if
     * there are multiple player with the lowest score.
     *
     * @param player the player for which it is asked for.
     * @return 2 (highest) if the player did first knock, 1 if the player did
     * the 2nd knock, 0 otherwise.
     */
    public int getKnockPriority(SchwimmenPlayer player) {
        if (player.equals(knocker1)) {
            return 2;
        }
        if (player.equals(knocker2)) {
            return 1;
        }
        return 0;
    }
}
