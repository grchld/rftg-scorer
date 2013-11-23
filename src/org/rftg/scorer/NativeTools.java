package org.rftg.scorer;

import java.nio.Buffer;

/**
 * @author gc
 */
class NativeTools {

    static {
        System.loadLibrary("rftg_scorer");
    }

    static native void sobel(Buffer src, Buffer dst, int width, int height);

    // segments: short miny, short maxy, short x, short angle
    // returns actual segment count
    static native int houghVertical(Buffer image, int width, int height, int bordermask, int origin, int maxGap, int minLength, Buffer segments, int maxSegments, Buffer segmentStates);

    static native void transpose(Buffer src, Buffer dst, int width, int height);

    static native long match(Buffer selection, Buffer patterns, int patternSize, int patternsCount);

    static native void normalize(Buffer image, int size);
    /*
    public void drawSobel(Mat sobel, Mat flame) {
        drawSobel(sobel.getNativeObjAddr(), flame.getNativeObjAddr());
    }

    private native void drawSobel(long sobel, long flame);*/
}
