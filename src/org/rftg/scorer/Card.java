package org.rftg.scorer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
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
    Set<Flag> flags = new HashSet<Flag>();
    Map<GameType, Integer> count = new EnumMap<GameType, Integer>(GameType.class);
    Set<Power> powers = new HashSet<Power>();
    Set<Extra> extras = new HashSet<Extra>();

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

    enum Flag {
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

    enum PowerType {
        AGAINST_CHROMO,
        AGAINST_REBEL,
        ALIEN,
        ANTE_CARD,
        AUTO_PRODUCE,
        CONQUER_SETTLE,
        CONSUME_3_DIFF,
        CONSUME_ALIEN,
        CONSUME_ALL,
        CONSUME_ANY,
        CONSUME_GENE,
        CONSUME_N_DIFF,
        CONSUME_NOVELTY,
        CONSUME_PRESTIGE,
        CONSUME_RARE,
        CONSUME_THIS,
        CONSUME_TWO,
        DESTROY,
        DISCARD,
        DISCARD_ANY,
        DISCARD_HAND,
        DISCARD_PRESTIGE,
        DISCARD_REDUCE,
        DRAW,
        DRAW_AFTER,
        DRAW_CHROMO,
        DRAW_DIFFERENT,
        DRAW_EACH_ALIEN,
        DRAW_EACH_NOVELTY,
        DRAW_IF,
        DRAW_LUCKY,
        DRAW_MILITARY,
        DRAW_MOST_PRODUCED,
        DRAW_MOST_RARE,
        DRAW_REBEL,
        DRAW_WORLD_GENE,
        EXPLORE,
        EXPLORE_AFTER,
        EXTRA_MILITARY,
        GENE,
        GET_2_CARD,
        GET_CARD,
        GET_PRESTIGE,
        GET_VP,
        IF_IMPERIUM,
        KEEP,
        MILITARY_HAND,
        NO_TAKEOVER,
        NO_TRADE,
        NOT_THIS,
        NOVELTY,
        PAY_DISCOUNT,
        PAY_MILITARY,
        PAY_PRESTIGE,
        PER_CHROMO,
        PER_MILITARY,
        PLACE_MILITARY,
        PLACE_TWO,
        PRESTIGE,
        PRESTIGE_IF,
        PRESTIGE_MOST_CHROMO,
        PRESTIGE_REBEL,
        PRESTIGE_SIX,
        PREVENT_TAKEOVER,
        PRODUCE,
        PRODUCE_PRESTIGE,
        RARE,
        REDUCE,
        REDUCE_ZERO,
        SAVE_COST,
        TAKEOVER_DEFENSE,
        TAKEOVER_IMPERIUM,
        TAKEOVER_MILITARY,
        TAKEOVER_PRESTIGE,
        TAKEOVER_REBEL,
        TAKE_SAVED,
        TRADE_ACTION,
        TRADE_ANY,
        TRADE_BONUS_CHROMO,
        TRADE_GENE,
        TRADE_NO_BONUS,
        TRADE_NOVELTY,
        TRADE_RARE,
        TRADE_THIS,
        UPGRADE_WORLD,
        VP,
        WINDFALL_ALIEN,
        WINDFALL_ANY,
        WINDFALL_GENE,
        WINDFALL_NOVELTY,
        WINDFALL_RARE
    }

    enum ExtraType {
        ALIEN_FLAG,
        ALIEN_PRODUCTION,
        ALIEN_WINDFALL,
        CHROMO_FLAG,
        DEVEL,
        DEVEL_CONSUME,
        DEVEL_EXPLORE,
        DEVEL_TRADE,
        GENE_PRODUCTION,
        GENE_WINDFALL,
        IMPERIUM_FLAG,
        KIND_GOOD,
        MILITARY,
        NAME,
        NEGATIVE_MILITARY,
        NOVELTY_PRODUCTION,
        NOVELTY_WINDFALL,
        PRESTIGE,
        RARE_PRODUCTION,
        RARE_WINDFALL,
        REBEL_FLAG,
        REBEL_MILITARY,
        SIX_DEVEL,
        TERRAFORMING_FLAG,
        THREE_VP,
        TOTAL_MILITARY,
        UPLIFT_FLAG,
        WORLD,
        WORLD_CONSUME,
        WORLD_EXPLORE,
        WORLD_TRADE
    }

    public static class Power {
        int phase;
        Set<PowerType> powers = new HashSet<PowerType>();
        int value;
        int times;
    }

    public static class Extra {
        int vp;
        ExtraType extraType;
        String name;
        Card namedCard;
    }
}
