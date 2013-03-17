package org.rftg.scorer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gc
 */
class Scoring {

    int score;
    int military;
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
        score = player.chips;
        cardScores = new ArrayList<CardScore>(player.cards.size());
        for (Card card : player.cards) {
            cardScores.add(new CardScore(card, score(card, player.cards)));
        }
    }

    private int score(Card card, List<Card> cards) {
        int score = card.vp;

        this.score += score;
//        this.military += card.
        return score;
    }
}
