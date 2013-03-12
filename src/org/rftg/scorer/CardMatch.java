package org.rftg.scorer;

import org.opencv.core.Point;

import java.util.Comparator;

/**
* @author gc
*/
public class CardMatch {

    public final int cardNumber;
    public final int score;
    public final Point[] rect;

    public CardMatch(int cardNumber, int score, Point[] rect) {
        this.cardNumber = cardNumber;
        this.score = score;
        this.rect = rect;
    }

    final static Comparator<CardMatch> MATCH_SCORE_COMPARATOR = new Comparator<CardMatch>() {
        @Override
        public int compare(CardMatch match1, CardMatch match2) {
            double r = match2.score - match1.score;
            if (r > 0) {
                return 1;
            } else if (r < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    };

}
