package org.rftg.scorer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * @author gc
 */
class Sprite {

    final Bitmap bitmap;
    final Rect rect;
    final Paint paint;

    Sprite(Bitmap bitmap, Rect rect, Paint paint) {
        this.bitmap = bitmap;
        this.rect = rect;
        this.paint = paint;
    }

    void draw(Canvas canvas, Rect rect) {
        canvas.drawBitmap(bitmap, rect.origin.x, rect.origin.y, paint);
    }
}
