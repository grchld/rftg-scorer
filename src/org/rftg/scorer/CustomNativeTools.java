package org.rftg.scorer;

import org.opencv.core.Mat;

/**
 * @author gc
 */
class CustomNativeTools {

    public CustomNativeTools() {
        System.loadLibrary("rftg_scorer");
    }

    public int normalize(Mat mat, double lowerPercent, double upperPercent) {
        return normalize(mat.getNativeObjAddr(), lowerPercent, upperPercent);
    }

    private native int normalize(long mat, double lowerPercent, double upperPercent);

}
