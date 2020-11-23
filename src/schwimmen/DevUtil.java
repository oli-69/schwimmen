package schwimmen;

import cardgame.Card;
import java.util.List;

public class DevUtil {

    public static void make31(List<Card> stack) {
        stack.clear();
        stack.add(new cardgame.Card(1, 14));
        stack.add(new cardgame.Card(1, 13));
        stack.add(new cardgame.Card(1, 12));
    }

    public static void make30_5(List<Card> stack) {
        stack.clear();
        stack.add(new cardgame.Card(1, 7));
        stack.add(new cardgame.Card(2, 7));
        stack.add(new cardgame.Card(3, 7));
    }

    public static void makeFire(List<Card> stack) {
        stack.clear();
        stack.add(new cardgame.Card(1, 14));
        stack.add(new cardgame.Card(2, 14));
        stack.add(new cardgame.Card(3, 14));
    }
}
