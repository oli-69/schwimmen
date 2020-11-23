package schwimmen.messages;

import schwimmen.SchwimmenPlayer;

public class Player {

    public String name;
    public boolean online;
    public int gameTokens;
    public int totalTokens;

    public Player() {
        this("", false, 0, 0);
    }

    public Player(SchwimmenPlayer player) {
        this(player.getName(), player.isOnline(), player.getGameTokens(), player.getTotalTokens());
    }

    public Player(String name, boolean online, int gameTokens, int totalTokens) {
        this.name = name;
        this.online = online;
        this.gameTokens = gameTokens;
        this.totalTokens = totalTokens;
    }

}
