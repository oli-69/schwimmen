package schwimmen;

import java.util.ArrayList;
import java.util.List;
import schwimmen.SchwimmenGame.MOVE;
import schwimmen.messages.PlayerMove;

public class Round {

    public SchwimmenPlayer dealer;
    public SchwimmenPlayer finisher;
    public SchwimmenPlayer knocker1;
    public SchwimmenPlayer knocker2;
    public float finishScore;
    public int moveCount;
    public List<SchwimmenPlayer> leavers = new ArrayList<>(); // in dieser Runde ausgeschiedene spieler (bezahlt & geschwommen)

    private final List<SchwimmenPlayer> passers = new ArrayList<>(); // liste der spieler, die schieben
    private final SchwimmenGame game;
  

    public Round(SchwimmenGame game) {
        this.game = game;
    }

    public void reset(SchwimmenPlayer dealer) {
        this.dealer = dealer;
        finisher = null;
        knocker1 = null;
        knocker2 = null;
        passers.clear();
        finishScore = 0f;
        moveCount = 0;
        leavers.clear();
    }

    public void setPlayerMove(PlayerMove move) {
        if (!MOVE.pass.toString().equals(move.move)) {
            passers.clear();
        }
        if (!MOVE.changeStack.toString().equals(move.move)) {
            moveCount++;
        }
    }

    public void pass(SchwimmenPlayer player) {
        passers.add(player);
    }
    
    public int getPassCount() {
        return passers.size();
    }

    public boolean isKnockAllowed() {
        return !(moveCount < game.getAttendeesCount());
    }

    public boolean isChangeStackAllowed(SchwimmenPlayer player) {
        return !passers.isEmpty()
                && !(passers.size() < game.getAttendeesCount())
                && player.equals(game.getMover()
                );
    }

    public void knock(SchwimmenPlayer player) {
        if (knocker1 != null) {
            knocker2 = player;
        } else {
            knocker1 = player;
        }
    }

    public int getKnockCount() {
        if (knocker2 != null) {
            return 2;
        }
        if (knocker1 != null) {
            return 1;
        }
        return 0;
    }

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
