package org.rftg.scorer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.rftg.scorer.Card.Extra;
import static org.rftg.scorer.Card.GoodType;
import static org.rftg.scorer.Card.Power;

/**
 * @author gc
 */
class Scoring {

    final List<Card> cards;
    int chips;
    int military;
    int goodTypes;

    int score;
    List<CardScore> cardScores;

    static class CardScore {

        final Card card;
        final int score;

        CardScore(Card card, int score) {
            this.card = card;
            this.score = score;
        }
    }

    Scoring(Player player) {
        cards = player.cards;
        chips = player.chips;

        calcGoodTypes();
        calcMilitary();

        score = chips;
        cardScores = new ArrayList<CardScore>(cards.size());
        for (Card card : cards) {
            int cardScore = calcScore(card);
            score += cardScore;
            cardScores.add(new CardScore(card, cardScore));
        }

    }

    private int calcScore(Card card) {
        int score = card.vp;

        if (!card.extras.isEmpty()) {
            for (Extra extra : card.extras) {
                switch (extra.extraType) {
                    case KIND_GOOD:
                        switch (goodTypes) {
                            case 1:
                                score += 1;
                                break;
                            case 2:
                                score += 3;
                                break;
                            case 3:
                                score += 6;
                                break;
                            case 4:
                                score += 10;
                                break;
                        }
                        break;
                    case NEGATIVE_MILITARY:
                        score -= military;
                        break;
                    case THREE_VP:
                        score += chips/3;
                        break;
                    case TOTAL_MILITARY:
                        score += military;
                        break;
                }
            }

            nextCard:
            for (Card c : cards) {
                for (Extra extra : card.extras) {
                    switch (extra.extraType) {
                        case KIND_GOOD:
                        case NEGATIVE_MILITARY:
                        case THREE_VP:
                        case TOTAL_MILITARY:
                            break;
                        case NAME:
                            if (extra.namedCard == c) {
                                score += extra.vp;
                                continue nextCard;
                            }
                            break;
                        default:
                            if (extra.extraType.match(c)) {
                                score += extra.vp;
                                continue nextCard;
                            }
                    }
                }
            }
        }

        return score;
    }

    private void calcGoodTypes() {
        Set<GoodType> goodTypes = EnumSet.noneOf(GoodType.class);
        for (Card c : cards) {
            if (c.cardType == Card.CardType.WORLD && c.goodType != null) {
                goodTypes.add(c.goodType);
            }
        }
        this.goodTypes = goodTypes.size();
        if (this.goodTypes > 4) {
            this.goodTypes = 4;
        }
    }

    private void calcMilitary() {
        for (Card card : cards) {
            for (Power power : card.powers) {
                if (power.powers.contains(Card.PowerType.EXTRA_MILITARY)) {
                    if (power.powers.size() == 1) {
                        military += power.value;
                    } else if (power.powers.contains(Card.PowerType.PER_MILITARY)) {
                        throw new UnsupportedOperationException("todo");
                    } else if (power.powers.contains(Card.PowerType.PER_CHROMO)) {
                        throw new UnsupportedOperationException("todo");
                    } else if (power.powers.contains(Card.PowerType.IF_IMPERIUM)) {
                        throw new UnsupportedOperationException("todo");
                    }
                }
            }
        }
    }
}