package schwimmen;

import cardgame.Card;
import java.awt.Point;
import java.util.List;
import schwimmen.messages.GameStack;

public class GameStackProperties {

    public final Point[] offset = new Point[]{new Point(), new Point(), new Point(),};
    public final float[] rotation = new float[]{0f, 0f, 0f};
    public final List<Card> cards;
    public final int[] cardFlips = new int[]{0, 0, 0};

    public GameStackProperties(List<Card> cards) {
        this.cards = cards;
        shakeAll();
    }

    public GameStack getGameStack() {
        return new GameStack(cards, offset, rotation, cardFlips);
    }

    public void shakeAll() {
        boolean shallFlip = false;
        clearFlips();
        shake(0, shallFlip ? getRandomFlips() : 0);
        shake(1, shallFlip ? getRandomFlips() : 0);
        shake(2, shallFlip ? getRandomFlips() : 0);
    }

    public void shake(int id) {
        clearFlips();
        shake(id, shallFlip() ? getRandomFlips() : 0);
    }

    private void shake(int id, int flips) {
        this.cardFlips[id] = flips;
        rotate(id);
        offset(id);
    }

    private int getRandomFlips() {
        int max = 5;
        int numFlips = (int) ((Math.random() * (2 * max)) - max);
        boolean superFlip = Math.random() > 0.9;
        return (superFlip ? 3 : 1) * numFlips;
    }

    private void rotate(int id) {
        rotation[id] = getRandomRotation(3.5f);
    }

    public void offset(int id) {
        offset[id].setLocation(getRandomOffset(10), getRandomOffset(5));
    }

    private float getRandomRotation(float max) {
        return (float) ((Math.random() * (2 * max)) - max);
    }

    private int getRandomOffset(int max) {
        return (int) ((Math.random() * (2 * max)) - max);
    }

    private boolean shallFlip() {
        return Math.random() > 0.9;
    }

    private void clearFlips() {
        for (int i = 0; i < cardFlips.length; i++) {
            cardFlips[i] = 0;
        }
    }
}
