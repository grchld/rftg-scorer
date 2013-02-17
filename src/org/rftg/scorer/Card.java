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
        BASE("The base game"),
        EXP1("The Gathering Storm"),
        EXP2("Rebel vs Imperium"),
        EXP3("The Brink of War");
        final String name;

        GameType(String name) {
            this.name = name;
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
