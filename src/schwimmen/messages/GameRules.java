package schwimmen.messages;

import schwimmen.SchwimmenGame;

public class GameRules {

    public final String action = "gameRuless";

    private final boolean knocking;
    private final boolean new789;
    private final boolean passOnce;

    public GameRules(SchwimmenGame game) {
        this.knocking = game.isGameRuleEnabled(SchwimmenGame.GAMERULE.Knocking);
        this.new789 = game.isGameRuleEnabled(SchwimmenGame.GAMERULE.newCardsOn789);
        this.passOnce = game.isGameRuleEnabled(SchwimmenGame.GAMERULE.passOnlyOncePerRound);
    }

}
