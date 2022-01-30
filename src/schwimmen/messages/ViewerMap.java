package schwimmen.messages;

import cardgame.Player;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ViewerMap {

    public final String action = "viewerMap";

    public String[][] table;

    public ViewerMap(Map<Player, Collection<Player>> map) {
        Set<Player> keys = map.keySet();
        table = new String[keys.size()][];
        int i = 0;
        Iterator<Player> keyIterator = keys.iterator();
        while (keyIterator.hasNext()) {
            int y = 0;
            Player player = keyIterator.next();
            Collection<Player> viewers = map.get(player);
            table[i] = new String[viewers.size() + 1];
            table[i][y++] = player.getName();
            Iterator<Player> viewerIterator = viewers.iterator();
            while (viewerIterator.hasNext()) {
                table[i][y++] = viewerIterator.next().getName();
            }
            i++;
        }
    }
}
