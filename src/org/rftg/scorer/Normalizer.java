package org.rftg.scorer;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author gc
 */
class Normalizer {

    public static final int HISTOGRAM_SIZE = 256;
//    public static final double NORMALIZE_THRESHOLD = .05;

    public static void normalize(Mat image) {

        List<Mat> splits = new ArrayList<Mat>(3);
        Core.split(image, splits);

        Mat[] channels = new Mat[3];
        for (int channelNumber = 0 ; channelNumber < 3 ; channelNumber++) {

            Mat hist = new Mat();

            Imgproc.calcHist(Collections.singletonList(splits.get(channelNumber)), new MatOfInt(0), new Mat(), hist, new MatOfInt(HISTOGRAM_SIZE), new MatOfFloat(0, HISTOGRAM_SIZE));

            double sum = 0;
            double sq = 0;
            for (int i = 0 ; i < HISTOGRAM_SIZE ; i++) {
                double v = hist.get(i, 0)[0];
                sum += v*(double)i;
                sq += v*(double)i*(double)i;
            }

            sq /= image.cols()*image.rows();
            sum /= image.cols()*image.rows();

            double dispersion = (sq - sum*sum);
            if (dispersion < 1) {
                continue;
            }
            double alpha = Math.sqrt(70*70/dispersion);

            double beta = 128. - alpha*sum;

            /*
            double sum = 0;
            for (int i = 0 ; i < HISTOGRAM_SIZE ; i++) {
                sum += hist.get(i, 0)[0];
            }

            double threshold = sum*NORMALIZE_THRESHOLD;

            sum = threshold;
            int min = -1;
            do {
                min++;
                sum -= hist.get(min, 0)[0];
            } while (sum > 0);

            sum = threshold;
            int max = HISTOGRAM_SIZE;
            do {
                max--;
                sum -= hist.get(max, 0)[0];
            } while (sum > 0);
            */

            hist.release();

            Mat m = new Mat();
            splits.get(channelNumber).convertTo(m, -1, alpha, beta);
            channels[channelNumber] = m;
        }

        Core.merge(Arrays.asList(channels[0], channels[1], channels[2]), image);

        for (Mat mat : splits) {
            mat.release();
        }
        for (Mat mat : channels) {
            mat.release();
        }

    }

}
