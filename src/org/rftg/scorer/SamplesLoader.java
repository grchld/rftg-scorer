package org.rftg.scorer;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

/**
 * @author gc
 */
public class SamplesLoader {

    private Sample[] samples;

    public SamplesLoader(Context context, int maxCardNum) {

        samples = new Sample[maxCardNum+1];
        Size size = new Size(Sample.SAMPLE_WIDTH, Sample.SAMPLE_HEIGHT);

        Mat scaleDown = Imgproc.getAffineTransform(
                new MatOfPoint2f(new Point(0, 0), new Point(Sample.ORIGINAL_SAMPLE_WIDTH, 0), new Point(0, Sample.ORIGINAL_SAMPLE_HEIGHT)),
                new MatOfPoint2f(new Point(0, 0), new Point(Sample.SAMPLE_WIDTH, 0), new Point(0, Sample.SAMPLE_HEIGHT)));

        Mat scaled = new Mat(Sample.SAMPLE_WIDTH, Sample.SAMPLE_HEIGHT, CvType.CV_8UC3);

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

    }
}
