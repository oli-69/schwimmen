package schwimmen.messages;

import java.util.List;

public class CardStack {

    public Card[] cards;

    public CardStack(List<cardgame.Card> gameCard) {
        this.cards = new Card[gameCard.size()];
        for (int i = 0; i < cards.length; i++) {
            cards[i] = new Card(gameCard.get(i));
        }
    }

}
