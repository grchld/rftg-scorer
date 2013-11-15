package org.rftg.scorer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.nio.ByteBuffer;

/**
 * @author gc
 */
public class UserInterfaceView extends View {

    private State state;

    private RecognizerResources recognizerResources;

    public UserInterfaceView(Context context) {
        super(context);
    }

    public UserInterfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UserInterfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setRecognizerResources(RecognizerResources recognizerResources) {
        this.recognizerResources = recognizerResources;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint paint = new Paint();
        paint.setTextSize(50);
        paint.setStrokeWidth(3);
/*
        ByteBuffer buffer = fastCamera.getBuffer();
        if (buffer == null) {
            return;
        }

        paint.setARGB(255, 0, 0, 0);
        canvas.drawRect(0, 0, fastCamera.getActualSize().width, fastCamera.getActualSize().height, paint);

        if (bitmap == null || bitmap.getWidth() != fastCamera.getActualSize().width || bitmap.getHeight() != fastCamera.getActualSize().height) {
            bitmap = Bitmap.createBitmap(fastCamera.getActualSize().width, fastCamera.getActualSize().height, Bitmap.Config.ALPHA_8);
        }
        paint.setARGB(255, 255, 255, 255);
        buffer.position(0);
        bitmap.copyPixelsFromBuffer(buffer);

        canvas.drawBitmap(bitmap, 0, 0, paint);

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
