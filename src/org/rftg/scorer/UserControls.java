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

    private final static Scalar MASK_TRANSPARENT = new Scalar(0);
    private final static Scalar MASK_OPAQUE = new Scalar(255);

    private final static int CARD_TEXT_FONT_FACE = 1;
    private final static double CARD_TEXT_FONT_SCALE = 1;
    private final static int CARD_TEXT_THICKNESS = 1;

    private final static int CARD_TEXT_BORDER = 2;

    private final static Mat DILATE_KERNEL = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3));
    private final static Point DILATE_ANCHOR = new Point(-1,-1);

    public final Sprite[] cardNames;

    UserControls(RecognizerResources recognizerResources) {
        int[] baseLine = new int[1];
        cardNames = new Sprite[recognizerResources.maxCardNum + 1];
        for (int i = 0 ; i <= recognizerResources.maxCardNum ; i++) {
            String name = recognizerResources.cardInfo.cards[i].name;

            Size textSize = Core.getTextSize(name, CARD_TEXT_FONT_FACE, CARD_TEXT_FONT_SCALE, CARD_TEXT_THICKNESS, baseLine);

            Mat text = new Mat((int)textSize.height + baseLine[0] + 2 * CARD_TEXT_BORDER, (int)textSize.width + 2 * CARD_TEXT_BORDER, CvType.CV_8UC3, CARD_TEXT_SHADOW);
            Point textOrigin = new Point(CARD_TEXT_BORDER, text.height() - baseLine[0] - CARD_TEXT_BORDER);

            Core.putText(text, name, textOrigin, CARD_TEXT_FONT_FACE, CARD_TEXT_FONT_SCALE, CARD_TEXT_COLOR, CARD_TEXT_THICKNESS);

            Mat mask = new Mat(text.rows(), text.cols(), CvType.CV_8U, MASK_TRANSPARENT);
            Core.putText(mask, name, textOrigin, CARD_TEXT_FONT_FACE, CARD_TEXT_FONT_SCALE, MASK_OPAQUE, CARD_TEXT_THICKNESS);

//            Mat erodedMask = new Mat(mask.rows(), mask.cols(), mask.type());
            Imgproc.dilate(mask, mask, DILATE_KERNEL, DILATE_ANCHOR, 2);

//            mask.release();

            cardNames[i] = new Sprite(text, mask);
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
