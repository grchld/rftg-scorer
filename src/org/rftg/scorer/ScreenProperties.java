package org.rftg.scorer;

import org.opencv.core.Size;

/**
 * @author gc
 */
class ScreenProperties {

    final int width;
    final int height;

    double cardTextFontScale = 1.2;
    int cardTextThickness = 1;
    int cardTextBorder = 2;

    Dimensions cardsIconSize = new Dimensions(120, 120);
    Dimensions chipsIconSize = new Dimensions(125, 125);

    Dimensions militaryIconSize = new Dimensions(100, 100);
    Dimensions resetIconSize = new Dimensions(125, 125);
    Dimensions totalIconSize = new Dimensions(125, 125);

    int previewHeight = 119;
    int previewWidth = 85;
    Size previewSize = new Size(previewWidth, previewHeight);

    int previewGap = 10;
    int previewStep = previewWidth + previewGap;

    double previewTextScale = 3;
    double chipsTextScale = 4;
    double militaryTextScale = 3;
    double cardCountTextScale = 4;
    double totalTextScale = 4;
    double totalTextScaleShrink = 3;

    int cardNameOffsetX = 10;
    int cardNameOffsetY = 50;

    ScreenProperties(int width, int height) {
        this.width = width;
        this.height = height;

        // Adjustment for small screens
        if (height < 600) {
            cardTextFontScale = 1;

            cardsIconSize = new Dimensions(100, 100);
            chipsIconSize = new Dimensions(100, 100);

            militaryIconSize = new Dimensions(90, 90);
            resetIconSize = new Dimensions(100, 100);
            totalIconSize = new Dimensions(110, 110);

            int previewHeight = 105;
            int previewWidth = 75;
            previewSize = new Size(previewWidth, previewHeight);

            previewGap = 8;
            previewStep = previewWidth + previewGap;

            previewTextScale = 2.5;
            chipsTextScale = 3;
            militaryTextScale = 2.5;
            cardCountTextScale = 2.8;
            totalTextScale = 3;
            totalTextScaleShrink = 2.5;

            cardNameOffsetX = 8;
            cardNameOffsetY = 40;
        }
    }

    static class Dimensions {
        final int width;
        final int height;

        Dimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

}
