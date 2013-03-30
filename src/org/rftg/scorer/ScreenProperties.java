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

    private Dimensions cardsIconSize = new Dimensions(120, 120);
    private Dimensions chipsIconSize = new Dimensions(125, 125);
    private Dimensions prestigeIconSize = new Dimensions(125, 125);

    private Dimensions militaryIconSize = new Dimensions(100, 100);
    private Dimensions resetIconSize = new Dimensions(125, 125);
    private Dimensions totalIconSize = new Dimensions(125, 125);

    Position cardsIconPosition;
    Position chipsIconPosition;
    Position prestigeIconPosition;
    Position militaryIconPosition;
    Position resetIconPosition;
    Position totalIconPosition;

    int previewHeight = 119;
    int previewWidth = 85;
    Size previewSize = new Size(previewWidth, previewHeight);

    int previewGap = 10;
    int previewStep = previewWidth + previewGap;

    double previewTextScale = 3;
    double chipsTextScale = 4;
    double prestigeTextScale = 4;
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
            prestigeIconSize = new Dimensions(100, 100);

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
            prestigeTextScale = 3;
            militaryTextScale = 2.5;
            cardCountTextScale = 2.8;
            totalTextScale = 3;
            totalTextScaleShrink = 2.5;

            cardNameOffsetX = 8;
            cardNameOffsetY = 40;
        }

        resetIconPosition = new Position(previewGap, previewGap, resetIconSize);
        cardsIconPosition = new Position(previewGap, - cardsIconSize.height - previewHeight - 2*previewGap, cardsIconSize);
        chipsIconPosition = new Position(- chipsIconSize.width - previewGap, previewGap, chipsIconSize);
        prestigeIconPosition = new Position(chipsIconPosition.x - prestigeIconSize.width - previewGap, previewGap, prestigeIconSize);
        militaryIconPosition = new Position(chipsIconPosition.x + (chipsIconSize.width - militaryIconSize.width) / 2,
                prestigeIconPosition.y + prestigeIconSize.height + previewGap, militaryIconSize);
        totalIconPosition = new Position(- totalIconSize.width - previewGap, - totalIconSize.height - previewHeight - 2*previewGap, totalIconSize);

    }

    // Negative values mean offsets from opposite edges
    static class Position {
        final int x;
        final int y;
        final Dimensions dimensions;

        Position(int x, int y, Dimensions dimensions) {
            this.x = x;
            this.y = y;
            this.dimensions = dimensions;
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
