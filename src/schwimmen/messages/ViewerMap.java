package schwimmen.messages;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import schwimmen.SchwimmenPlayer;

public class ViewerMap {

    public final String action = "viewerMap";

    public String[][] table;

    public ViewerMap(Map<SchwimmenPlayer, List<SchwimmenPlayer>> map) {
        Set<SchwimmenPlayer> keys = map.keySet();
        table = new String[keys.size()][];
        int i = 0;
        Iterator<SchwimmenPlayer> iterator = keys.iterator();
        while (iterator.hasNext()) {
            SchwimmenPlayer player = iterator.next();
            List<SchwimmenPlayer> viewers = map.get(player);
            table[i] = new String[viewers.size() + 1];
            table[i][0] = player.getName();
            for (int y = 0; y < viewers.size(); y++) {
                table[i][y+1] = viewers.get(y).getName();
            }
            i++;
        }
    }
}
