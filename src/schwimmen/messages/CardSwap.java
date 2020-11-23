package schwimmen.messages;

public class CardSwap {

    public Card cardGiven;
    public Card cardTaken;
    public int stackIdGiven;
    public int stackIdTaken;

    public CardSwap(cardgame.Card cardGiven, cardgame.Card cardTaken, int stackIdGiven, int stackIdTaken) {
        this.cardGiven = new Card(cardGiven);
        this.cardTaken = new Card(cardTaken);
        this.stackIdGiven = stackIdGiven;
        this.stackIdTaken = stackIdTaken;
    }

}
