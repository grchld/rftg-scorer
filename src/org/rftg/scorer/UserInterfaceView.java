package org.rftg.scorer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * @author gc
 */
public class UserInterfaceView extends View {

    final static Paint PAINT_LOADING_PERCENT = new Paint();
    {
        PAINT_LOADING_PERCENT.setARGB(255, 255, 255, 255);
        PAINT_LOADING_PERCENT.setTextSize(50);
        PAINT_LOADING_PERCENT.setTextAlign(Paint.Align.CENTER);
    }

    private MainContext mainContext;

    public UserInterfaceView(Context context) {
        super(context);
    }

    public UserInterfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UserInterfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setMainContext(MainContext mainContext) {
        this.mainContext = mainContext;
    }

    private Bitmap bitmap;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mainContext == null) {
            return;
        }
       /*
        int loadingPercent = mainContext.cardPatterns.getLoadingPercent();

        if (loadingPercent >= 0) {
            canvas.drawText("Loading: " + loadingPercent + "%", canvas.getWidth()/2, canvas.getHeight()/2, PAINT_LOADING_PERCENT);
            return;
        }
         */




        Paint paint = new Paint();
        paint.setTextSize(50);
        paint.setStrokeWidth(3);

/*
        ByteBuffer buffer = fastCamera.getBuffer();
        if (buffer == null) {
            return;
        }
*/
        paint.setARGB(255, 0, 0, 0);
//        canvas.drawRect(0, 0, fastCamera.getActualSize().width, fastCamera.getActualSize().height, paint);

        if (bitmap == null || bitmap.getWidth() != 64 || bitmap.getHeight() != 640) {
            bitmap = Bitmap.createBitmap(64, 640, Bitmap.Config.ALPHA_8);
        }
        paint.setARGB(255, 255, 255, 255);
        mainContext.cardPatterns.samples.position(0);
        bitmap.copyPixelsFromBuffer(mainContext.cardPatterns.samples);

        canvas.drawBitmap(bitmap, 0, 0, paint);
        /*
        paint.setARGB(255, 255, 0, 0);
        canvas.drawLines(new float[]{
                fastCamera.getLeft() + 10, fastCamera.getTop() + 10, fastCamera.getRight() - 10, fastCamera.getTop() + 10,
                fastCamera.getRight() - 10, fastCamera.getTop() + 10, fastCamera.getRight() - 10, fastCamera.getBottom() - 10,
                fastCamera.getRight() - 10, fastCamera.getBottom() - 10, fastCamera.getLeft() + 10, fastCamera.getBottom() - 10,
                fastCamera.getLeft() + 10, fastCamera.getBottom() - 10, fastCamera.getLeft() + 10, fastCamera.getTop() + 10
        }, paint);
        canvas.drawText("" + frame++, 0, 0, paint);
        canvas.drawText("" + fastCamera.getActualSize(), 100, 400, paint);
        */
    }
}
