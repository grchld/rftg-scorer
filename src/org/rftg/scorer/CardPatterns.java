package org.rftg.scorer;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author gc
 */
class CardPatterns {

    final static int ORIGINAL_SAMPLE_HEIGHT = 520;
    final static int ORIGINAL_SAMPLE_WIDTH = 372;
    final static int ORIGINAL_SAMPLE_BORDER = 23;

    final static float CARD_HORIZONTAL_BORDER = ((float)ORIGINAL_SAMPLE_BORDER)/ORIGINAL_SAMPLE_HEIGHT;
    final static float CARD_VERTICAL_BORDER = ((float)ORIGINAL_SAMPLE_BORDER)/ORIGINAL_SAMPLE_WIDTH;

    final static int SAMPLE_HEIGHT = 64;
    final static int SAMPLE_WIDTH = 64;
    final static int MATCHER_MINIMAL_BOUND = 5000;
    final static int MATCHER_MINIMAL_GAP = 1000;
    final static Size SAMPLE_SIZE = new Size(SAMPLE_WIDTH, SAMPLE_HEIGHT);

    final static int SAMPLE_COUNT = Card.GameType.EXP3.maxCardNum + 1;

    final ByteBuffer samples = ByteBuffer.allocateDirect(SAMPLE_WIDTH * SAMPLE_HEIGHT * SAMPLE_COUNT);

    private final MainContext mainContext;

    CardPatterns(final MainContext mainContext) {
        this.mainContext = mainContext;

        try {
            InputStream inputStream = mainContext.resourceContext.getAssets().open("samples.dat");
            try {
                byte[] tmp = new byte[samples.capacity()];
                if (inputStream.read(tmp) != tmp.length) {
                    throw new RuntimeException("Can't load samples");
                }
                samples.put(tmp);
            } finally {
                inputStream.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
    public void invokeAnalyse(final Mat selection, final CardMatch[] cardMatches, final Point[] rect, final int maxCardNum) {
        recognizerResources.executor.submit(new Runnable() {
            @Override
            public void run() {

                long matchResult = recognizerResources.customNativeTools.match(selection, samplesFused, SAMPLE_HEIGHT*SAMPLE_WIDTH, maxCardNum + 1);

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

            }

        });
    }
    */

}
