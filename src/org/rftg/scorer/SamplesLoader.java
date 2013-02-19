package org.rftg.scorer;

import android.content.Context;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

/**
 * @author gc
 */
public class SamplesLoader {

    public final static int ORIGINAL_SAMPLE_HEIGHT = 520;
    public final static int ORIGINAL_SAMPLE_WIDTH = 372;

    private final static int SIZE_MULTIPLIER = 2;
    public final static int SAMPLE_HEIGHT = 7 * SIZE_MULTIPLIER;
    public final static int SAMPLE_WIDTH = 5 * SIZE_MULTIPLIER;

    private Mat[] samples;

    public SamplesLoader(Context context, int maxCardNum) {

        samples = new Mat[maxCardNum+1];
        Size size = new Size(SAMPLE_WIDTH, SAMPLE_HEIGHT);

        Mat scaleDown = Imgproc.getAffineTransform(
                new MatOfPoint2f(new Point(0, 0), new Point(ORIGINAL_SAMPLE_WIDTH, 0), new Point(0, ORIGINAL_SAMPLE_HEIGHT)),
                new MatOfPoint2f(new Point(0, 0), new Point(SAMPLE_WIDTH, 0), new Point(0, SAMPLE_HEIGHT)));

        Mat scaled = new Mat(SAMPLE_WIDTH, SAMPLE_HEIGHT, CvType.CV_8UC3);

        for (int num = 0 ; num <= maxCardNum ; num++ ) {
            Mat tempSample;
            try {

                int id = context.getResources().getIdentifier("card_"+num, "drawable", "org.rftg.scorer");

                tempSample = Utils.loadResource(context, id);

                Imgproc.warpAffine(tempSample, scaled, scaleDown, size, Imgproc.INTER_LINEAR);

                tempSample.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        scaled.release();
        scaleDown.release();

    }

    public void release() {
        for (Mat sample : samples) {
            if (sample != null) {
                sample.release();
            }
        }
    }
}
