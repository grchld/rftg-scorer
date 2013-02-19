package org.rftg.scorer;

/**
 * @author gc
 */
public class CustomNativeTools {

    static {
        System.loadLibrary("rftg_scorer");
    }

    public native double testNativeCall(double value);

}
