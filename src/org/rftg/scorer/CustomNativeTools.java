package org.rftg.scorer;

import org.opencv.core.Mat;

/**
 * @author gc
 */
class CustomNativeTools {

    public CustomNativeTools() {
        System.loadLibrary("rftg_scorer");
    }

    public void sobel(Mat src, Mat dst, int bound) {
        sobel(src.getNativeObjAddr(), dst.getNativeObjAddr(), bound);
    }

    private native void sobel(long src, long dst, int bound);

    // segments: short miny, short maxy, short x, short angle
    public int houghVertical(Mat image, int bordermask, int origin,  int maxGap, int minLength, Mat segments) {
        return houghVertical(image.getNativeObjAddr(), bordermask, origin, maxGap, minLength, segments.getNativeObjAddr());
    }

    private native int houghVertical(long imageAddr, int bordermask, int origin, int maxGap, int minLength, long segmentsAddr);

    public void transpose(Mat src, Mat dst) {
        transpose(src.getNativeObjAddr(), dst.getNativeObjAddr());
    }

    private native void transpose(long src, long dst);

    public int compare(Mat selection, Mat pattern) {
        return compare(selection.getNativeObjAddr(), pattern.getNativeObjAddr());
    }

    private native int compare(long selection, long pattern);

    public void normalize(Mat image) {
        normalize(image.getNativeObjAddr());
    }

    private native void normalize(long image);
}
