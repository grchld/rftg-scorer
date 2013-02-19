package org.rftg.scorer;

import java.util.Map;
import java.util.Set;

/**
 * @author gc
 */
class Card {

    int id;
    String name;
    CardType cardType;
    int cost;
    int vp;
    GoodType goodType;
    Set<Flags> flags;
    Map<GameType, Integer> count;

    enum GameType {
        BASE("The base game", 94),
        EXP1("The Gathering Storm", 113),
        EXP2("Rebel vs Imperium", 150),
        EXP3("The Brink of War", 190);
        final String name;
        final int maxCardNum;

        GameType(String name, int maxCardNum) {
            this.name = name;
            this.maxCardNum = maxCardNum;
        }
    }

    enum CardType {
        WORLD,
        DEVELOPMENT
    }

    enum GoodType {
        NOVELTY,
        RARE,
        GENE,
        ALIEN,
        ANY
    }

    enum Flags {
        IMPERIUM,
        MILITARY,
        WINDFALL,
        START,
        START_RED,
        START_BLUE,
        REBEL,
        UPLIFT,
        ALIEN,
        TERRAFORMING,
        CHROMO,
        PRESTIGE,
        STARTHAND_3,
        START_SAVE,
        DISCARD_TO_12,
        GAME_END_14,
        TAKE_DISCARDS,
        SELECT_LAST
    }

}
