
package schwimmen.messages;

public class Card {

    public int color;
    public int value;
    
    public Card(cardgame.Card gameCard) {
        color = gameCard.getColor();
        value = gameCard.getValue();
    }
    
}
