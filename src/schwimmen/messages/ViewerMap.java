package schwimmen.messages;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import schwimmen.SchwimmenPlayer;

public class ViewerMap {

    public final String action = "viewerMap";

    public String[][] table;

    public ViewerMap(Map<SchwimmenPlayer, Collection<SchwimmenPlayer>> map) {
        Set<SchwimmenPlayer> keys = map.keySet();
        table = new String[keys.size()][];
        int i = 0;
        Iterator<SchwimmenPlayer> keyIterator = keys.iterator();
        while (keyIterator.hasNext()) {
            int y = 0;
            SchwimmenPlayer player = keyIterator.next();
            Collection<SchwimmenPlayer> viewers = map.get(player);
            table[i] = new String[viewers.size() + 1];
            table[i][y++] = player.getName();
            Iterator<SchwimmenPlayer> viewerIterator = viewers.iterator();
            while (viewerIterator.hasNext()) {
                table[i][y++] = viewerIterator.next().getName();
            }
            i++;
        }
    }
}
