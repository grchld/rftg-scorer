package org.rftg.scorer;

import android.util.Log;

/**
 * @author gc
 */
public class Rftg {

    public final static String TAG = "rftg";

    public static void e(String message) {
        Log.e(TAG, message);
    }

    public static void e(String message, Throwable t) {
        Log.e(TAG, message, t);
    }

    public static void e(Throwable t) {
        Log.e(TAG, t.getMessage(), t);
    }

    public static void w(String message) {
        Log.w(TAG, message);
    }

    public static void d(String message) {
        Log.d(TAG, message);
    }

    public static void i(String message) {
        Log.i(TAG, message);
    }
}
