package org.rftg.scorer;

import org.opencv.core.Mat;

/**
 * @author gc
 */
class CustomNativeTools {

    public CustomNativeTools() {
        System.loadLibrary("rftg_scorer");
    }

    public long normalize(Mat mat, double lowerPercent, double upperPercent) {
        return normalize(mat.getNativeObjAddr(), lowerPercent, upperPercent);
    }

    private native long normalize(long mat, double lowerPercent, double upperPercent);

    public void sobel(Mat src, Mat dst, int bound) {
        sobel(src.getNativeObjAddr(), dst.getNativeObjAddr(), bound);
    }

    private native void sobel(long src, long dst, int bound);

}
