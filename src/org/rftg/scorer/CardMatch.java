package org.rftg.scorer;

import java.util.Comparator;

/**
* @author gc
*/
class CardMatch {

    final int cardNumber;
    final int score;
    final int secondCardNumber;
    final int secondScore;
    final Point[] rect;
    final int minx, maxx, miny, maxy;

    CardMatch(int cardNumber, int score, int secondCardNumber, int secondScore, Point[] rect) {
        this.cardNumber = cardNumber;
        this.score = score;
        this.secondCardNumber = secondCardNumber;
        this.secondScore = secondScore;
        this.rect = rect;
        minx = Math.max(rect[0].x, rect[3].x);
        maxx = Math.min(rect[1].x, rect[2].x);
        miny = Math.max(rect[0].y, rect[1].y);
        maxy = Math.min(rect[2].y, rect[3].y);
    }

    final static Comparator<CardMatch> MATCH_SCORE_COMPARATOR = new Comparator<CardMatch>() {
        @Override
        public int compare(CardMatch match1, CardMatch match2) {
            int r = match2.score - match1.score;
            if (r > 0) {
                return 1;
            } else if (r < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    };

    boolean isIntersects(CardMatch match) {
        return minx <= match.maxx && match.minx <= maxx && miny <= match.maxy && match.miny <= maxy;
    }

}
