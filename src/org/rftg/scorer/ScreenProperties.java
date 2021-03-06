package org.rftg.scorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author gc
 */
class ScreenProperties {

    final Size screenSize;

    final Size cardsIconSize;
    final Size chipsIconSize;
    final Size prestigeIconSize;

    final Size militaryIconSize;
    final Size resetIconSize;
    final Size totalIconSize;

    final Rect cardsIconRect;
    final Rect chipsIconRect;
    final Rect prestigeIconRect;
    final Rect militaryIconRect;
    final Rect resetIconRect;
    final Rect totalIconRect;
    final Rect magnifiedRect;

    final Size previewSize;

    final int previewGap;
    final int previewStep;

    final float cardTextScale;
    final float previewTextScale;
    final float magnifiedTextScale;
    final float chipsTextScale;
    final float prestigeTextScale;
    final float militaryTextScale;
    final float cardCountTextScale;
    final float totalTextScale;
    final float textShadow;
//    final float totalTextScaleShrink;

    final float borderNewStrokeWidth;
    final float borderOldStrokeWidth;

    final int cardNameOffsetX;
    final int cardNameOffsetY;

    private float scaleFloat(double length) {
        return (float)(length*screenSize.height/720.);
    }

    private int scale(int length) {
        return (int)scaleFloat(length);
    }

    private Size scale(int width, int height) {
        return new Size(scale(width), scale(height));
    }

    ScreenProperties(Size screenSize) {

        this.screenSize = screenSize;

        cardsIconSize = scale(120, 120);
        chipsIconSize = scale(125, 125);
        prestigeIconSize = scale(125, 125);

        militaryIconSize = scale(100, 100);
        resetIconSize = scale(125, 125);
        totalIconSize = scale(125, 125);

        previewSize = scale(85, 119);

        previewGap = scale(10);
        previewStep = previewSize.width + previewGap;

        cardTextScale = scaleFloat(18);
        previewTextScale = scaleFloat(60);
        magnifiedTextScale = scaleFloat(200);
        chipsTextScale = scaleFloat(70);
        prestigeTextScale = scaleFloat(70);
        militaryTextScale = scaleFloat(60);
        cardCountTextScale = scaleFloat(70);
        totalTextScale = scaleFloat(70);

        borderNewStrokeWidth = scaleFloat(6);
        borderOldStrokeWidth = scaleFloat(4);

        textShadow = scaleFloat(4);

        cardNameOffsetX = scale(10);
        cardNameOffsetY = scale(50);

        resetIconRect = new Rect(new Point(previewGap, previewGap), resetIconSize);
        cardsIconRect = new Rect(new Point(previewGap, screenSize.height - previewSize.height - cardsIconSize.height - 2*previewGap), cardsIconSize);
        chipsIconRect = new Rect(new Point(screenSize.width - chipsIconSize.width - previewGap, previewGap), chipsIconSize);
        prestigeIconRect = new Rect(new Point(chipsIconRect.origin.x - prestigeIconSize.width - previewGap, previewGap), prestigeIconSize);
        militaryIconRect = new Rect(new Point(chipsIconRect.origin.x + (chipsIconSize.width - militaryIconSize.width) / 2,
                prestigeIconRect.origin.y + prestigeIconSize.height + previewGap), militaryIconSize);
        totalIconRect = new Rect(new Point(screenSize.width - totalIconSize.width - previewGap, screenSize.height - totalIconSize.height - previewSize.height - 2*previewGap), totalIconSize);

        int magnifiedHeight = screenSize.height - previewSize.height - 3 * previewGap;
        int magnifiedWidth = magnifiedHeight * 5 / 7;
        magnifiedRect = new Rect(new Point((screenSize.width - magnifiedWidth) / 2, previewGap), new Size(magnifiedWidth, magnifiedHeight));
    }

    List<Point> getPreviewPositions(int n) {
        if (n == 0) {
            return Collections.emptyList();
        }
        int y = screenSize.height - previewSize.height - previewGap;
        if (n == 1) {
            return Collections.singletonList(new Point(previewGap, y));
        }
        List<Point> result = new ArrayList<Point>(n);

        float step = ((float)(screenSize.width - previewGap - previewStep)) / (n - 1);
            if (step > previewStep) {
                step = previewStep;
            }

        float x = previewGap;
        for (int i = 0 ; i < n ; i++, x+=step) {
            result.add(new Point((int)x, y));
        }

        return result;
    }

}
