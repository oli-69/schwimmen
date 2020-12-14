package schwimmen.messages;

import java.util.List;
import schwimmen.SchwimmenGame.MOVE;

public class PlayerMove {

    public String move;
    public CardSwap cardSwap;
    public StackSwap stackSwap;
    public CardStack gameStack;
    public int count;
    public String stackAction;
    public ViewerStackList viewerStackList;

    public PlayerMove(MOVE move, List<cardgame.Card> gameStack) {
        this.move = move.name();
        this.gameStack = new CardStack(gameStack);
    }

    public PlayerMove(MOVE move, List<cardgame.Card> gameStack, String stackAction) {
        this(move, gameStack);
        this.gameStack = new CardStack(gameStack);
        this.stackAction = stackAction;
    }

    public PlayerMove(MOVE move, int count, List<cardgame.Card> gameStack) {
        this(move, gameStack);
        this.count = count;
    }

    public PlayerMove(CardSwap cardSwap, List<cardgame.Card> gameStack) {
        this(MOVE.swapCard, gameStack);
        this.cardSwap = cardSwap;
    }

    public PlayerMove(StackSwap stackSwap, List<cardgame.Card> gameStack) {
        this(MOVE.swapAllCards, gameStack);
        this.stackSwap = stackSwap;
    }

    @Override
    public String toString() {
        String detail = "";
        if (cardSwap != null) {
            detail = String.format("nimmt: '%s', gibt: '%s'",
                    cardgame.Card.toString(cardSwap.cardGiven.color, cardSwap.cardGiven.value),
                    cardgame.Card.toString(cardSwap.cardTaken.color, cardSwap.cardTaken.value));
        }
        if (stackSwap != null) {
            detail = String.format("nimmt: '%s'/'%s'/'%s', gibt: '%s'/'%s'/'%s'",
                    cardgame.Card.toString(stackSwap.stackTaken[0].color, stackSwap.stackTaken[0].value),
                    cardgame.Card.toString(stackSwap.stackTaken[1].color, stackSwap.stackTaken[1].value),
                    cardgame.Card.toString(stackSwap.stackTaken[2].color, stackSwap.stackTaken[2].value),
                    cardgame.Card.toString(stackSwap.stackGiven[0].color, stackSwap.stackGiven[0].value),
                    cardgame.Card.toString(stackSwap.stackGiven[1].color, stackSwap.stackGiven[1].value),
                    cardgame.Card.toString(stackSwap.stackGiven[2].color, stackSwap.stackGiven[2].value)
            );
        }
        return move + " " + detail;
    }

}
