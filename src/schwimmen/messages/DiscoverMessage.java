package schwimmen.messages;

import java.util.List;
import schwimmen.SchwimmenPlayer;

public class DiscoverMessage {

    public String finisher;
    public float finisherScore;
    public String finishKnocker;
    public DiscoverStack[] playerStacks;
    public String[] payers;
    public String[] leavers;
    public int remainingAttendeesCount;
    public int finishSoundId;

    public DiscoverMessage(SchwimmenPlayer finisher, float finisherScore, SchwimmenPlayer finishKnocker, List<DiscoverStack> playerStacks,
            List<SchwimmenPlayer> payers, List<SchwimmenPlayer> leavers, int finishSoundId) {
        this.finisherScore = finisherScore;
        this.playerStacks = playerStacks.toArray(new DiscoverStack[playerStacks.size()]);
        this.payers = getNames(payers);
        this.leavers = getNames(leavers);

        if (finisher != null) {
            this.finisher = finisher.getName();
        }
        if (finishKnocker != null) {
            this.finishKnocker = finishKnocker.getName();
        }
        remainingAttendeesCount = this.playerStacks.length - leavers.size();
        this.finishSoundId = finishSoundId;
    }

    private String[] getNames(List<SchwimmenPlayer> players) {
        if (players != null && !players.isEmpty()) {
            String[] names = new String[players.size()];
            for (int i = 0; i < names.length; i++) {
                names[i] = players.get(i).getName();
            }
            return names;
        }
        return null;
    }
}
