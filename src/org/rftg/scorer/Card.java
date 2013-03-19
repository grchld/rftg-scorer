package org.rftg.scorer;

import java.util.*;

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
    Set<Flag> flags =  EnumSet.noneOf(Flag.class);
    Map<GameType, Integer> count = new EnumMap<GameType, Integer>(GameType.class);
    List<Power> powers = new ArrayList<Power>(4);
    List<Extra> extras = new ArrayList<Extra>(4);
    Set<Phase> phasePowers = EnumSet.noneOf(Phase.class);

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

    enum Phase {
        EXPLORE,
        DEVELOP,
        SETTLE,
        TRADE,
        CONSUME,
        PRODUCE
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

    public static final Set<PowerType> TRADE_POWERS = EnumSet.of(
            PowerType.TRADE_ACTION,
            PowerType.TRADE_ANY,
            PowerType.TRADE_BONUS_CHROMO,
            PowerType.TRADE_GENE,
            PowerType.TRADE_NO_BONUS,
            PowerType.TRADE_NOVELTY,
            PowerType.TRADE_RARE,
            PowerType.TRADE_THIS
    );

    enum ExtraType {
        ALIEN_FLAG {
            @Override
            public boolean match(Card card) {
                return card.flags.contains(Flag.ALIEN);
            }
        },
        ALIEN_PRODUCTION {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.WORLD && card.goodType == GoodType.ALIEN && !card.flags.contains(Flag.WINDFALL);
            }
        },
        ALIEN_WINDFALL {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.WORLD && card.goodType == GoodType.ALIEN && card.flags.contains(Flag.WINDFALL);
            }
        },
        CHROMO_FLAG {
            @Override
            public boolean match(Card card) {
                return card.flags.contains(Flag.CHROMO);
            }
        },
        DEVEL {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.DEVELOPMENT;
            }
        },
        DEVEL_CONSUME {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.DEVELOPMENT && card.phasePowers.contains(Phase.CONSUME);
            }
        },
        DEVEL_EXPLORE {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.DEVELOPMENT && card.phasePowers.contains(Phase.EXPLORE);
            }
        },
        DEVEL_TRADE {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.DEVELOPMENT && card.phasePowers.contains(Phase.TRADE);
            }
        },
        GENE_PRODUCTION {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.WORLD && card.goodType == GoodType.GENE && !card.flags.contains(Flag.WINDFALL);
            }
        },
        GENE_WINDFALL {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.WORLD && card.goodType == GoodType.GENE && card.flags.contains(Flag.WINDFALL);
            }
        },
        IMPERIUM_FLAG {
            @Override
            public boolean match(Card card) {
                return card.flags.contains(Flag.IMPERIUM);
            }
        },
        KIND_GOOD, // 1,3,6,10 for different kinds
        MILITARY {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.WORLD && card.flags.contains(Flag.MILITARY);
            }
        },
        NAME, // find by name
        NEGATIVE_MILITARY, // -military
        NOVELTY_PRODUCTION {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.WORLD && card.goodType == GoodType.NOVELTY && !card.flags.contains(Flag.WINDFALL);
            }
        },
        NOVELTY_WINDFALL {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.WORLD && card.goodType == GoodType.NOVELTY && card.flags.contains(Flag.WINDFALL);
            }
        },
        PRESTIGE,
        RARE_PRODUCTION {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.WORLD && card.goodType == GoodType.RARE && !card.flags.contains(Flag.WINDFALL);
            }
        },
        RARE_WINDFALL {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.WORLD && card.goodType == GoodType.RARE && card.flags.contains(Flag.WINDFALL);
            }
        },
        REBEL_FLAG {
            @Override
            public boolean match(Card card) {
                return card.flags.contains(Flag.REBEL);
            }
        },
        REBEL_MILITARY {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.WORLD && card.flags.contains(Flag.MILITARY) && card.flags.contains(Flag.REBEL);
            }
        },
        SIX_DEVEL {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.DEVELOPMENT && card.cost == 6;
            }
        },
        TERRAFORMING_FLAG {
            @Override
            public boolean match(Card card) {
                return card.flags.contains(Flag.TERRAFORMING);
            }
        },
        THREE_VP, // extra vp for every three chips
        TOTAL_MILITARY, // +military
        UPLIFT_FLAG {
            @Override
            public boolean match(Card card) {
                return card.flags.contains(Flag.UPLIFT);
            }
        },
        WORLD {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.WORLD;
            }
        },
        WORLD_CONSUME {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.WORLD && card.phasePowers.contains(Phase.CONSUME);
            }
        },
        WORLD_EXPLORE {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.WORLD && card.phasePowers.contains(Phase.EXPLORE);
            }
        },
        WORLD_TRADE {
            @Override
            public boolean match(Card card) {
                return card.cardType == CardType.WORLD && card.phasePowers.contains(Phase.TRADE);
            }
        };

        public boolean match(Card card) {
            throw new IllegalStateException("This extra can't be calculated with this method");
        }

    }

    public static class Power {
        Phase phase;
        Set<PowerType> powers = EnumSet.noneOf(PowerType.class);
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
