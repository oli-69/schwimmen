
package cardgame;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *
 */
public class CardGame {

    public static final int CARDS_52 = 52;
    public static final int CARDS_32 = 32;
    public static final int CARDS_24 = 24;

    private final PropertyChangeSupport propChangeSupport;
    private final int cardsCount;
    private final List<Card> allCards; // Alle Karten
    private final List<Card> bigStack;

    public CardGame(int cardsCount) {
        if (cardsCount != CARDS_24
                && cardsCount != CARDS_32
                && cardsCount != CARDS_52) {
            throw new IllegalArgumentException("Unkown count of cards.");
        }

        propChangeSupport = new PropertyChangeSupport(this);
        this.cardsCount = cardsCount;
        allCards = Collections.synchronizedList(new ArrayList<>(cardsCount));
        bigStack = Collections.synchronizedList(new ArrayList<>(cardsCount));
        initializeCards();
    }

    public void addPropertyChangeListener(PropertyChangeListener pl) {
        propChangeSupport.addPropertyChangeListener(pl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pl) {
        propChangeSupport.removePropertyChangeListener(pl);
    }

    protected void firePropertyChange(String popertyName, Object oldValue, Object newValue) {
        propChangeSupport.firePropertyChange(popertyName, oldValue, newValue);
    }

    /**
     * Mischt den Kartenhaufen und setzt den Stapel zurueck Erstellungsdatum:
     * (15.06.2003 11:53:30)
     */
    protected void shuffleStack() {
        reshuffleStack(allCards);
    }

    protected void reshuffleStack(List<Card> sourceStack) {
        Random random = new Random(System.currentTimeMillis());
        List<Card> tempStack = new ArrayList<>(sourceStack.size());
        tempStack.addAll(sourceStack);
        bigStack.clear();
        while (tempStack.size() > 0) {
            bigStack.add(tempStack.remove(random.nextInt(tempStack.size())));
        }
    }

    /**
     * Liefert die naechste Karte vom Stapel Erstellungsdatum: (15.06.2003
     * 13:56:20)
     *
     * @return de.ofh.cardgame.Card
     */
    protected Card getFromStack() {
        return bigStack.isEmpty() ? null : bigStack.remove(bigStack.size() - 1);
    }
    
    protected int stackSize() {
        return bigStack.size();
    }

    /**
     * Initialisisert das Kartenspiel Erstellungsdatum: (15.06.2003 12:10:45)
     */
    private void initializeCards() {
        // Anzahl der Karten festlegen
        int start;
        switch (cardsCount) {
            case CARDS_24:
                start = Card.NEUN;
                break;
            case CARDS_32:
                start = Card.SIEBEN;
                break;
            default:
                start = Card.ZWEI;
                break;
        }

        // Kartenspiel erstellen
        for (int value = start; value <= Card.AS; value++) {
            for (int color = Card.CARO; color <= Card.KREUZ; color++) {
                allCards.add(new Card(color, value));
            }
        }
    }
}
