package org.rftg.scorer;

import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author gc
 */
class SamplesLoader {

    public final static int ORIGINAL_SAMPLE_HEIGHT = 520;
    public final static int ORIGINAL_SAMPLE_WIDTH = 372;
    private final static int SIZE_MULTIPLIER = 2;

    public final static int SAMPLE_HEIGHT = 7 * SIZE_MULTIPLIER;
    public final static int SAMPLE_WIDTH = 5 * SIZE_MULTIPLIER;
    private Mat[] samples;
    private Normalizer normalizer = new Normalizer();

    public SamplesLoader(final MainActivity main, int maxCardNum) {

        samples = new Mat[maxCardNum + 1];
        final Size size = new Size(SAMPLE_WIDTH, SAMPLE_HEIGHT);

        final Mat scaleDown = Imgproc.getAffineTransform(
                new MatOfPoint2f(new Point(0, 0), new Point(ORIGINAL_SAMPLE_WIDTH, 0), new Point(0, ORIGINAL_SAMPLE_HEIGHT)),
                new MatOfPoint2f(new Point(0, 0), new Point(SAMPLE_WIDTH, 0), new Point(0, SAMPLE_HEIGHT)));

        long time = System.currentTimeMillis();

        class Task implements Callable<Void> {

            int num;

            Task(int num) {
                this.num = num;
            }

            @Override
            public Void call() throws Exception {
                Mat tempSample;

                int id = main.getResources().getIdentifier("card_" + num, "drawable", "org.rftg.scorer");

                tempSample = Utils.loadResource(main, id);

                Mat scaled = new Mat(SAMPLE_WIDTH, SAMPLE_HEIGHT, CvType.CV_8UC3);
                Imgproc.warpAffine(tempSample, scaled, scaleDown, size, Imgproc.INTER_LINEAR);

                normalizer.normalize(scaled);

                samples[num] = scaled;
                tempSample.release();

                return null;
            }

        }

        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>(maxCardNum + 1);

        for (int num = 0; num <= maxCardNum; num++) {
            tasks.add(new Task(num));
        }

        try {
            main.executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Log.i("rftg", "Parallel loading time: " + (System.currentTimeMillis() - time));

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
