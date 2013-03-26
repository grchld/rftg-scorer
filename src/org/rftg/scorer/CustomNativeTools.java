package org.rftg.scorer;

import org.opencv.core.Mat;

/**
 * @author gc
 */
class CustomNativeTools {

    public CustomNativeTools() {
        System.loadLibrary("rftg_scorer");
    }

    public void sobel(Mat src, Mat dst) {
        sobel(src.getNativeObjAddr(), dst.getNativeObjAddr());
    }

    private native void sobel(long src, long dst);

    // segments: short miny, short maxy, short x, short angle
    public int houghVertical(Mat image, int bordermask, int origin,  int maxGap, int minLength, Mat segments) {
        return houghVertical(image.getNativeObjAddr(), bordermask, origin, maxGap, minLength, segments.getNativeObjAddr());
    }

    private native int houghVertical(long imageAddr, int bordermask, int origin, int maxGap, int minLength, long segmentsAddr);

    public void transpose(Mat src, Mat dst) {
        transpose(src.getNativeObjAddr(), dst.getNativeObjAddr());
    }

    private native void transpose(long src, long dst);

    public long match(Mat selection, Mat patterns, int patternSize, int patternsCount) {
        return match(selection.getNativeObjAddr(), patterns.getNativeObjAddr(), patternSize, patternsCount);
    }

    private native long match(long selection, long patterns, int patternSize, int patternsCount);

    public void normalize(Mat image) {
        normalize(image.getNativeObjAddr());
    }

    private native void normalize(long image);
}
