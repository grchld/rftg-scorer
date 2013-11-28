package org.rftg.scorer;

import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * @author gc
 */
class CardPatterns {

    final static int ORIGINAL_SAMPLE_HEIGHT = 520;
    final static int ORIGINAL_SAMPLE_WIDTH = 372;
    final static int ORIGINAL_SAMPLE_BORDER = 23;

    final static float CARD_HORIZONTAL_BORDER = ((float)ORIGINAL_SAMPLE_BORDER)/ORIGINAL_SAMPLE_HEIGHT;
    final static float CARD_VERTICAL_BORDER = ((float)ORIGINAL_SAMPLE_BORDER)/ORIGINAL_SAMPLE_WIDTH;

    final static int SAMPLE_WIDTH = 64;
    final static int SAMPLE_HEIGHT = 64;

    final static int SAMPLE_COUNT = Card.GameType.EXP3.totalCardNum;

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
}
