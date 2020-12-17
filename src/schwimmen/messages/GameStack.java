package schwimmen.messages;

import cardgame.Card;
import java.awt.Point;
import java.util.List;

public class GameStack extends CardStack {

    public Point[] offset;
    public float[] rotation;
    public int[] cardFlips;

    public GameStack(List<Card> gameStack, Point[] offset, float[] rotation, int[] cardFlips) {
        super(gameStack);
        this.offset = offset;
        this.rotation = rotation;
        this.cardFlips = cardFlips;
    }

}
