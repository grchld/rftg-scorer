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

    static native void warp(Buffer image, int width, int height, Buffer warp, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4);

    static native void normalize(Buffer image, int size);
}
