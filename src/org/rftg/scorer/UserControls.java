package org.rftg.scorer;

import android.view.MotionEvent;
import android.view.View;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

/**
 * @author gc
 */
class UserControls {

    private final static Scalar CARD_TEXT_COLOR = new Scalar(255, 255, 255);
    private final static Scalar CARD_TEXT_SHADOW = new Scalar(0, 0, 0);

    private final static int CARD_TEXT_FONT_FACE = 1;
    private final static double CARD_TEXT_FONT_SCALE = 1.2;
    private final static int CARD_TEXT_THICKNESS = 1;

    private final static int CARD_TEXT_BORDER = 2;

    public final Sprite[] cardNames;

    UserControls(RecognizerResources recognizerResources) {
        cardNames = new Sprite[recognizerResources.maxCardNum + 1];
        for (int i = 0 ; i <= recognizerResources.maxCardNum ; i++) {
            cardNames[i] = Sprite.textSpriteWithDilate(recognizerResources.cardInfo.cards[i].name,
                    CARD_TEXT_COLOR, CARD_TEXT_SHADOW, CARD_TEXT_FONT_FACE, CARD_TEXT_FONT_SCALE, CARD_TEXT_THICKNESS, CARD_TEXT_BORDER);
        }
    }

    void release() {
        for (Sprite sprite : cardNames) {
            sprite.release();
        }
    }

    boolean onTouch(View view, MotionEvent motionEvent, State state) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

            float x = motionEvent.getX();
            float y = motionEvent.getY();
            if (x < 200 && y < 200) {
                state.player.chips = 0;
                state.player.cards.clear();
            }

            return true;
        } else {
            return false;
        }
    }
}
