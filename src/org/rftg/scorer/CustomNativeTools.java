package org.rftg.scorer;

/**
 * @author gc
 */
class CustomNativeTools {

    public CustomNativeTools() {
        System.loadLibrary("rftg_scorer");
    }

    public native double testNativeCall(double value);

}
