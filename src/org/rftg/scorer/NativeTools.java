package org.rftg.scorer;

import java.nio.ByteBuffer;

/**
 * @author gc
 */
class NativeTools {

    static {
        System.loadLibrary("rftg_scorer");
    }

    public static native void sobel(ByteBuffer src, ByteBuffer dst, int width, int height);

    // segments: short miny, short maxy, short x, short angle
    /*
    public int houghVertical(Mat image, int bordermask, int origin,  int maxGap, int minLength, Mat segments) {
        return houghVertical(image.getNativeObjAddr(), bordermask, origin, maxGap, minLength, segments.getNativeObjAddr());
    }

    private native int houghVertical(long imageAddr, int bordermask, int origin, int maxGap, int minLength, long segmentsAddr);
     */
    public static native void transpose(ByteBuffer src, ByteBuffer dst, int width, int height);

    public static native long match(ByteBuffer selection, ByteBuffer patterns, int patternSize, int patternsCount);

    public static native void normalize(ByteBuffer image, int size);
    /*
    public void drawSobel(Mat sobel, Mat flame) {
        drawSobel(sobel.getNativeObjAddr(), flame.getNativeObjAddr());
    }

    private native void drawSobel(long sobel, long flame);*/
}
