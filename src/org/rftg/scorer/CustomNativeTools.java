package org.rftg.scorer;

/**
 * @author gc
 */
public class CustomNativeTools {

    public CustomNativeTools() {
        System.loadLibrary("rftg_scorer");
    }

    public native double testNativeCall(double value);

}
