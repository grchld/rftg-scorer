package org.rftg.scorer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * @author gc
 */
class UserInterfaceResources {

    private final Paint PAINT_PREVIEW = paint(255, 255, 255);
    private final Paint PAINT_CARDS = paint(128, 255, 128);
    private final Paint PAINT_CHIPS = paint(255, 255, 0);
    private final Paint PAINT_PRESTIGE = paint(0, 255, 0);
    private final Paint PAINT_MILITARY = paint(255, 0, 0);
    private final Paint PAINT_RESET = paint(0, 0, 0);
    private final Paint PAINT_TOTAL = paint(0, 0, 0);
    private final Paint PAINT_MAGNIFIED = paint(255, 255, 255);

    final Paint PAINT_BORDER_NEW = paint(0, 255, 0);
    final Paint PAINT_BORDER_OLD = paint(255, 0, 0);

    final Paint PAINT_BLACK = paint(0, 0, 0);
    final Paint PAINT_WHITE = paint(255, 255, 255);
    final Paint PAINT_GREEN = paint(0, 255, 0);
    final Paint PAINT_RED = paint(255, 0, 0);
    final Paint PAINT_MAGENTA = paint(255, 0, 255);
    final Paint PAINT_YELLOW = paint(255, 255, 0);

    private static Paint paint(int r, int g, int b) {
        Paint paint = new Paint();
        paint.setARGB(255, r, g, b);
        paint.setTextAlign(Paint.Align.CENTER);
        return paint;
    }


    final MainContext mainContext;

    final ScreenProperties screenProperties;

    private Sprite cardsIcon;
    private Sprite chipsIcon;
    private Sprite prestigeIcon;
    private Sprite militaryIcon;
    private Sprite resetIcon;
    private Sprite totalIcon;
    private Sprite[] cards = new Sprite[Card.GameType.EXP3.maxCardNum + 1];

    private int magnifiedCardId;
    private Bitmap magnifiedCardBitmap;

    UserInterfaceResources(MainContext mainContext, Size screenSize) {
        this.mainContext = mainContext;

        this.screenProperties = new ScreenProperties(screenSize);

        PAINT_PREVIEW.setTextSize(screenProperties.previewTextScale);
        PAINT_MAGNIFIED.setTextSize(screenProperties.magnifiedTextScale);
        PAINT_CARDS.setTextSize(screenProperties.cardCountTextScale);
        PAINT_CHIPS.setTextSize(screenProperties.chipsTextScale);
        PAINT_PRESTIGE.setTextSize(screenProperties.prestigeTextScale);
        PAINT_MILITARY.setTextSize(screenProperties.militaryTextScale);
        PAINT_TOTAL.setTextSize(screenProperties.totalTextScale);

        PAINT_BORDER_NEW.setStrokeWidth(screenProperties.borderNewStrokeWidth);
        PAINT_BORDER_OLD.setStrokeWidth(screenProperties.borderOldStrokeWidth);

        cardsIcon = loadSprite("cards", screenProperties.cardsIconRect, PAINT_CARDS);
        chipsIcon = loadSprite("chip", screenProperties.chipsIconRect, PAINT_CHIPS);
        prestigeIcon = loadSprite("prestige", screenProperties.prestigeIconRect, PAINT_PRESTIGE);
        militaryIcon = loadSprite("military", screenProperties.militaryIconRect, PAINT_MILITARY);
        resetIcon = loadSprite("reset", screenProperties.resetIconRect, PAINT_RESET);
        totalIcon = loadSprite("total", screenProperties.totalIconRect, PAINT_TOTAL);
    }

    Sprite getCardsIcon() {
        return cardsIcon;
    }

    Sprite getChipsIcon() {
        return chipsIcon;
    }

    Sprite getPrestigeIcon() {
        return prestigeIcon;
    }

    Sprite getMilitaryIcon() {
        return militaryIcon;
    }

    Sprite getResetIcon() {
        return resetIcon;
    }

    Sprite getTotalIcon() {
        return totalIcon;
    }

    Sprite getCard(int num) {
        Sprite card = cards[num];
        if (card == null) {
            card = loadSprite("card_" + num, new Rect(null, screenProperties.previewSize), PAINT_PREVIEW);
            cards[num] = card;
        }
        return card;
    }

    private int getDrawableId(String name) {
        return mainContext.resourceContext.getResources().getIdentifier(name, "drawable", "org.rftg.scorer");
    }

    public Sprite getMagnifiedCard(int num) {
        int id = getDrawableId("card_" + num);
        if (magnifiedCardBitmap == null || magnifiedCardId != num) {
            if (magnifiedCardBitmap != null) {
                magnifiedCardBitmap.recycle();
            }
            magnifiedCardBitmap = BitmapFactory.decodeResource(mainContext.resourceContext.getResources(), id, null);
            magnifiedCardId = num;
        }
        return new Sprite(magnifiedCardBitmap, screenProperties.magnifiedRect, PAINT_MAGNIFIED){
            @Override
            void draw(Canvas canvas, Rect rect) {
                canvas.drawBitmap(magnifiedCardBitmap, null, rect.toAndroidRect(), paint);
            }
        };
    }

    private Sprite loadSprite(String name, Rect rect, Paint paint) {
        Size size = rect.size;
        int id = getDrawableId(name);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(mainContext.resourceContext.getResources(), id, options);

        float aspect = options.outWidth * size.height > options.outHeight * size.width
                ? (float)options.outWidth / (float)size.width
                : (float)options.outHeight / (float)size.height;

        options.inJustDecodeBounds = false;
        options.inSampleSize = (int)aspect;

        Bitmap unscaled = BitmapFactory.decodeResource(mainContext.resourceContext.getResources(), getDrawableId(name), options);
        Bitmap scaled = Bitmap.createScaledBitmap(unscaled, size.width, size.height, true);
        unscaled.recycle();
        return new Sprite(scaled, rect, paint);
    }

    private void disposeSprite(Sprite sprite) {
        if (sprite != null) {
            sprite.bitmap.recycle();
        }
    }

    void dispose() {
        disposeSprite(cardsIcon);
        disposeSprite(chipsIcon);
        disposeSprite(prestigeIcon);
        disposeSprite(militaryIcon);
        disposeSprite(resetIcon);
        disposeSprite(totalIcon);
        for (Sprite sprite : cards) {
            disposeSprite(sprite);
        }

        if (magnifiedCardBitmap != null) {
            magnifiedCardBitmap.recycle();
        }
    }
}
