package org.rftg.scorer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.MotionEvent;
import android.view.View;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author gc
 */
class UserControls {

    private RecognizerResources recognizerResources;

    private final static Scalar CARD_TEXT_COLOR = new Scalar(255, 255, 255);
    private final static Scalar CARD_TEXT_SHADOW = new Scalar(0, 0, 0);

    private final static int CARD_TEXT_FONT_FACE = 1;

    public final Sprite[] cardNames;
    public Sprite cardCountBackground;
    public Sprite chipsBackground;
    public Sprite militaryBackground;
    public Sprite resetBackground;
    public Sprite totalBackground;

    UserControls(final RecognizerResources recognizerResources) {
        this.recognizerResources = recognizerResources;
        final ScreenProperties screen = recognizerResources.screenProperties;

        cardNames = new Sprite[recognizerResources.maxCardNum + 1];
        recognizerResources.executor.submit(new Runnable() {
            @Override
            public void run() {
                for (int i = 0 ; i <= recognizerResources.maxCardNum ; i++) {
                    cardNames[i] = Sprite.textSpriteWithDilate(recognizerResources.cardInfo.cards[i].name,
                            CARD_TEXT_COLOR, CARD_TEXT_SHADOW, CARD_TEXT_FONT_FACE, screen.cardTextFontScale, screen.cardTextThickness, screen.cardTextBorder);
                }
            }
        });
        recognizerResources.executor.submit(new Runnable() {
            @Override
            public void run() {
                cardCountBackground = load("cards", screen.cardsIconSize);
                chipsBackground = load("chip", screen.chipsIconSize);
                militaryBackground = load("military", screen.militaryIconSize);
                resetBackground = load("reset", screen.resetIconSize);
                totalBackground = load("total", screen.totalIconSize);
            }
        });
    }

    void release() {
        for (Sprite sprite : cardNames) {
            sprite.release();
        }
        cardCountBackground.release();
        chipsBackground.release();
        militaryBackground.release();
        resetBackground.release();
        totalBackground.release();
    }

    boolean onTouch(View view, MotionEvent motionEvent, Recognizer recognizer, State state) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

            float x = motionEvent.getX() - (view.getWidth() - recognizer.width)/2;
            float y = motionEvent.getY() - (view.getHeight() - recognizer.height)/2;
            if (x < 155 && y < 155) {
                state.player.chips = 0;
                state.player.cards.clear();
            } else if (x > recognizer.width - 155 && y < 155) {
                state.player.chips++;
            }

            return true;
        } else {
            return false;
        }
    }

    private Sprite load(String imageName, ScreenProperties.Dimensions size) {
        int id = recognizerResources.resourceContext.getResources().getIdentifier(imageName, "drawable", "org.rftg.scorer");

        Bitmap bitmap = BitmapFactory.decodeResource(recognizerResources.resourceContext.getResources(), id, new BitmapFactory.Options());

        Mat tempRGBA = new Mat();
        Utils.bitmapToMat(bitmap, tempRGBA);
        bitmap.recycle();

        Mat scaledRGBA = new Mat();
        Imgproc.resize(tempRGBA, scaledRGBA, new Size(size.width, size.height));
        tempRGBA.release();

        Mat image = new Mat();
        Imgproc.cvtColor(scaledRGBA, image, Imgproc.COLOR_RGBA2RGB);

        Mat mask = new Mat();
        Core.extractChannel(scaledRGBA, mask, 3);

        scaledRGBA.release();

        return new Sprite(image, mask);
    }
}
