package org.rftg.scorer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.MotionEvent;
import android.view.View;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

/**
 * @author gc
 */
class UserControls {

    private RecognizerResources recognizerResources;

    private final static Scalar CARD_TEXT_COLOR = new Scalar(255, 255, 255);
    private final static Scalar CARD_TEXT_SHADOW = new Scalar(0, 0, 0);

    private final static int CARD_TEXT_FONT_FACE = 1;
    private final static double CARD_TEXT_FONT_SCALE = 1.2;
    private final static int CARD_TEXT_THICKNESS = 1;

    private final static int CARD_TEXT_BORDER = 2;

    public final Sprite[] cardNames;
    public final Sprite cardCountBackground;
    public final Sprite chipsBackground;
    public final Sprite militaryBackground;
    public final Sprite resetBackground;


    UserControls(RecognizerResources recognizerResources) {
        this.recognizerResources = recognizerResources;
        cardNames = new Sprite[recognizerResources.maxCardNum + 1];
        for (int i = 0 ; i <= recognizerResources.maxCardNum ; i++) {
            cardNames[i] = Sprite.textSpriteWithDilate(recognizerResources.cardInfo.cards[i].name,
                    CARD_TEXT_COLOR, CARD_TEXT_SHADOW, CARD_TEXT_FONT_FACE, CARD_TEXT_FONT_SCALE, CARD_TEXT_THICKNESS, CARD_TEXT_BORDER);
        }
        cardCountBackground = load("icon_11", 120, 120);
        chipsBackground = load("icon_12", 120, 120);
        militaryBackground = load("icon_13", 120, 120);
        resetBackground = load("icon_15", 120, 120);
    }

    void release() {
        for (Sprite sprite : cardNames) {
            sprite.release();
        }
        cardCountBackground.release();
        chipsBackground.release();
        militaryBackground.release();
        resetBackground.release();
    }

    boolean onTouch(View view, MotionEvent motionEvent, State state) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

            float x = motionEvent.getX();
            float y = motionEvent.getY();
            if (x < 120 && y < 120) {
                state.player.chips = 0;
                state.player.cards.clear();
            }

            return true;
        } else {
            return false;
        }
    }

    private Sprite load(String imageName, int width, int height) {
        int id = recognizerResources.resourceContext.getResources().getIdentifier(imageName, "drawable", "org.rftg.scorer");

        Bitmap bitmap = BitmapFactory.decodeResource(recognizerResources.resourceContext.getResources(), id, new BitmapFactory.Options());

        Mat tempRGBA = new Mat();
        Utils.bitmapToMat(bitmap, tempRGBA);
        bitmap.recycle();

        Mat scaledRGBA = new Mat();
        Imgproc.resize(tempRGBA, scaledRGBA, new Size(width, height));
        tempRGBA.release();

        Mat image = new Mat();
        Imgproc.cvtColor(scaledRGBA, image, Imgproc.COLOR_RGBA2RGB);

        Mat mask = new Mat();
        Core.extractChannel(scaledRGBA, mask, 3);

        scaledRGBA.release();

        return new Sprite(image, mask);
    }
}
