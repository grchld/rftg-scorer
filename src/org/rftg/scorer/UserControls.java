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

import static org.rftg.scorer.ScreenProperties.Dimensions;
import static org.rftg.scorer.ScreenProperties.Position;

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
    public Sprite prestigeBackground;
    public Sprite tokensScienceBackground;
    public Sprite tokensUpliftBackground;
    public Sprite tokensAlienBackground;

    UserControls(final RecognizerResources recognizerResources) {
        this.recognizerResources = recognizerResources;
        final ScreenProperties screen = recognizerResources.screenProperties;

        cardNames = new Sprite[Card.GameType.EXP4.maxCardNum + 1];
        recognizerResources.executor.submit(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i <= Card.GameType.EXP4.maxCardNum; i++) {
                    cardNames[i] = Sprite.textSpriteWithDilate(recognizerResources.cardInfo.cards[i].name,
                            CARD_TEXT_COLOR, CARD_TEXT_SHADOW, CARD_TEXT_FONT_FACE, screen.cardTextFontScale, screen.cardTextThickness, screen.cardTextBorder);
                }
            }
        });
        recognizerResources.executor.submit(new Runnable() {
            @Override
            public void run() {
                cardCountBackground = load("cards", screen.cardsIconPosition.dimensions);
                chipsBackground = load("chip", screen.chipsIconPosition.dimensions);
                prestigeBackground = load("prestige", screen.prestigeIconPosition.dimensions);
                tokensScienceBackground = load("token_science", screen.tokensScienceIconPosition.dimensions);
                tokensUpliftBackground = load("token_uplift", screen.tokensUpliftIconPosition.dimensions);
                tokensAlienBackground = load("token_alien", screen.tokensAlienIconPosition.dimensions);
                militaryBackground = load("military", screen.militaryIconPosition.dimensions);
                resetBackground = load("reset", screen.resetIconPosition.dimensions);
                totalBackground = load("total", screen.totalIconPosition.dimensions);
            }
        });
    }

    void release() {
        for (Sprite sprite : cardNames) {
            sprite.release();
        }
        cardCountBackground.release();
        chipsBackground.release();
        prestigeBackground.release();
        militaryBackground.release();
        resetBackground.release();
        totalBackground.release();
    }

    boolean onTouch(View view, MotionEvent motionEvent, Recognizer recognizer, State state) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

            ScreenProperties screen = recognizerResources.screenProperties;
            int x = (int)motionEvent.getX() - (view.getWidth() - recognizer.width)/2;
            int y = (int)motionEvent.getY() - (view.getHeight() - recognizer.height)/2;

            if (inside(recognizer, screen.resetIconPosition, x, y)) {
                state.player.chips = 0;
                state.player.prestige = 0;
                state.player.tokensScience = 0;
                state.player.tokensUplift = 0;
                state.player.tokensAlien = 0;
                state.player.cards.clear();
            } else if (inside(recognizer, screen.chipsIconPosition, x, y)) {
                state.player.chips++;
            } else if (state.settings.usePrestige && inside(recognizer, screen.prestigeIconPosition, x, y)) {
                state.player.prestige++;
            } else if (state.settings.useTokens && inside(recognizer, screen.tokensScienceIconPosition, x, y)) {
                state.player.tokensScience++;
            } else if (state.settings.useTokens && inside(recognizer, screen.tokensUpliftIconPosition, x, y)) {
                state.player.tokensUplift++;
            } else if (state.settings.useTokens && inside(recognizer, screen.tokensAlienIconPosition, x, y)) {
                state.player.tokensAlien++;
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean inside(Recognizer recognizer, Position position, int x, int y) {
        int positionX = position.x;
        if (positionX < 0) {
            positionX += recognizer.width;
        }
        int positionY = position.y;
        if (positionY < 0) {
            positionY += recognizer.height;
        }
        return x >= positionX && x < positionX + position.dimensions.width && y >= positionY && y < positionY + position.dimensions.height;
    }

    private Sprite load(String imageName, Dimensions size) {
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
