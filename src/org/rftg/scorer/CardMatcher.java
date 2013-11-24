package org.rftg.scorer;

import java.nio.Buffer;

/**
* @author gc
*/
abstract class CardMatcher extends RecognizerTask {

    final static int MATCHER_MINIMAL_BOUND = 5000;
    final static int MATCHER_MINIMAL_GAP = 1000;

    private final Buffer frame;
    private final Size frameSize;
    private final Buffer samples;
    private final Point[] rect;
    private final CardMatch[] cardMatches;

    CardMatcher(Buffer frame, Size frameSize, Buffer samples, Point[] rect, CardMatch[] cardMatches) {
        this.frame = frame;
        this.frameSize = frameSize;
        this.samples = samples;
        this.rect = rect;
        this.cardMatches = cardMatches;
    }

    abstract protected Buffer createBuffer();

    abstract protected void releaseBuffer(Buffer buffer);

    @Override
    void execute() throws Exception {
        Buffer buffer = createBuffer();
        try {
            NativeTools.warp(frame, frameSize.width, frameSize.height, buffer, rect[0].x, rect[0].y, rect[1].x, rect[1].y, rect[2].x, rect[2].y, rect[3].x, rect[3].y);

            NativeTools.normalize(buffer, CardPatterns.SAMPLE_WIDTH * CardPatterns.SAMPLE_HEIGHT);

            long matchResult = NativeTools.match(buffer, samples, CardPatterns.SAMPLE_WIDTH * CardPatterns.SAMPLE_HEIGHT, CardPatterns.SAMPLE_COUNT);



            int bestCardNumber = (int)matchResult & 0xffff;
            matchResult >>= 16;
            int bestScore = (int)matchResult & 0xffff;
            matchResult >>= 16;
            int secondBestCardNumber = (int)matchResult & 0xffff;
            matchResult >>= 16;
            int secondBestScore = (int)matchResult;

            if (bestScore > MATCHER_MINIMAL_BOUND && bestScore - secondBestScore > MATCHER_MINIMAL_GAP) {
                synchronized (cardMatches) {
                    CardMatch match = cardMatches[bestCardNumber];
                    if (match == null || match.score < bestScore) {
                        cardMatches[bestCardNumber] = new CardMatch(bestCardNumber, bestScore, secondBestCardNumber, secondBestScore, rect);
                    }
                }
            }
        } finally {
            releaseBuffer(buffer);
        }
    }
}
