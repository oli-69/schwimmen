package schwimmen.messages;

import java.util.List;

public class StackSwap {

    public Card[] stackGiven;
    public Card[] stackTaken;

    public StackSwap(List<cardgame.Card> given, List<cardgame.Card> taken) {
        this.stackGiven = new Card[given.size()];
        this.stackTaken = new Card[taken.size()];
        for (int i = 0; i < stackTaken.length; i++) {
            stackGiven[i] = new Card(given.get(i));
            stackTaken[i] = new Card(taken.get(i));
        }
    }
}
