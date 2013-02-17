package org.rftg.scorer;

import android.content.Context;
import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

/**
 * @author gc
 */
class Recognizer {

    private Mat real;

    Recognizer(Context context, int width, int height) {
        Mat tempReal;
        try {

            tempReal = Utils.loadResource(context, R.drawable.real);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        real = new Mat(tempReal.rows(), tempReal.cols(), CvType.CV_8UC4);

        Imgproc.cvtColor(tempReal, real, Imgproc.COLOR_BGR2RGBA);

        tempReal.release();
    }

    void release() {
        real.release();
    }

    Mat onFrame(Mat inputFrame) {
        Mat sub = inputFrame.submat(0,real.rows(),0,real.cols());
        real.copyTo(sub);
        sub.release();

        return inputFrame;
    }

}
