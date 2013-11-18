package org.rftg.scorer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;

/**
 * @author gc
 */
class UserInterfaceResources {

    private final Paint PAINT_PREVIEW = new Paint();
    private final Paint PAINT_CARDS = new Paint();
    private final Paint PAINT_CHIPS = new Paint();
    private final Paint PAINT_PRESTIGE = new Paint();
    private final Paint PAINT_MILITARY = new Paint();
    private final Paint PAINT_RESET = new Paint();
    private final Paint PAINT_TOTAL = new Paint();

    final MainContext mainContext;
    final Size screenSize;

    final ScreenProperties screenProperties;

    private Sprite cardsIcon;
    private Sprite chipsIcon;
    private Sprite prestigeIcon;
    private Sprite militaryIcon;
    private Sprite resetIcon;
    private Sprite totalIcon;
    private Sprite[] cards = new Sprite[Card.GameType.EXP3.maxCardNum + 1];

    UserInterfaceResources(MainContext mainContext, Size screenSize) {
        this.mainContext = mainContext;
        this.screenSize = screenSize;

        this.screenProperties = new ScreenProperties(screenSize);

        PAINT_PREVIEW.setARGB(255, 255, 255, 255);
        PAINT_PREVIEW.setTextAlign(Paint.Align.CENTER);
        PAINT_PREVIEW.setTextSize(screenProperties.previewTextScale);
//        PAINT_PREVIEW.setStrokeWidth(screenProperties.previewTextScale/3);

        PAINT_CARDS.setARGB(255, 128, 255, 128);
        PAINT_CARDS.setTextAlign(Paint.Align.CENTER);
        PAINT_CARDS.setTextSize(screenProperties.cardCountTextScale);
//        PAINT_CARDS.setStrokeWidth(screenProperties.cardCountTextScale/3);

        PAINT_CHIPS.setARGB(255, 255, 255, 0);
        PAINT_CHIPS.setTextAlign(Paint.Align.CENTER);
        PAINT_CHIPS.setTextSize(screenProperties.chipsTextScale);
//        PAINT_CHIPS.setStrokeWidth(screenProperties.chipsTextScale/3);

        PAINT_PRESTIGE.setARGB(255, 0, 255, 0);
        PAINT_PRESTIGE.setTextAlign(Paint.Align.CENTER);
        PAINT_PRESTIGE.setTextSize(screenProperties.prestigeTextScale);
//        PAINT_PRESTIGE.setStrokeWidth(screenProperties.prestigeTextScale/3);

        PAINT_MILITARY.setARGB(255, 255, 0, 0);
        PAINT_MILITARY.setTextAlign(Paint.Align.CENTER);
        PAINT_MILITARY.setTextSize(screenProperties.militaryTextScale);
//        PAINT_MILITARY.setStrokeWidth(screenProperties.militaryTextScale/3);

        PAINT_RESET.setTextAlign(Paint.Align.CENTER);

        PAINT_TOTAL.setARGB(255, 0, 0, 0);
        PAINT_TOTAL.setTextAlign(Paint.Align.CENTER);
        PAINT_TOTAL.setTextSize(screenProperties.totalTextScale);
//        PAINT_TOTAL.setStrokeWidth(screenProperties.totalTextScale/3);

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
    }
}
