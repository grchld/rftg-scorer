package org.rftg.scorer;

import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.Callable;

/**
 * @author gc
 */
class CardPatterns {

    public final static int ORIGINAL_SAMPLE_HEIGHT = 520;
    public final static int ORIGINAL_SAMPLE_WIDTH = 372;
    private final static int SIZE_MULTIPLIER = 2;

    public final static int SAMPLE_HEIGHT = 7 * SIZE_MULTIPLIER;
    public final static int SAMPLE_WIDTH = 5 * SIZE_MULTIPLIER;

    public final static Size SAMPLE_SIZE = new Size(SAMPLE_WIDTH, SAMPLE_HEIGHT);
    public final static MatOfPoint2f SAMPLE_RECT = new MatOfPoint2f(new Point(0, 0), new Point(SAMPLE_WIDTH, 0), new Point(SAMPLE_WIDTH, SAMPLE_HEIGHT), new Point(0, SAMPLE_HEIGHT));

    private final Mat[] samples;

    public CardPatterns(final RecognizerResources recognizerResources) {

        samples = new Mat[recognizerResources.maxCardNum + 1];
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

                int id = recognizerResources.resourceContext.getResources().getIdentifier("card_" + num, "drawable", "org.rftg.scorer");

                tempSample = Utils.loadResource(recognizerResources.resourceContext, id);

                Mat scaled = new Mat(SAMPLE_WIDTH, SAMPLE_HEIGHT, CvType.CV_8UC3);
                Imgproc.warpAffine(tempSample, scaled, scaleDown, size, Imgproc.INTER_LINEAR);

                Normalizer.normalize(scaled);

                samples[num] = scaled;
                tempSample.release();

                return null;
            }

        }

        for (int num = 0; num <= recognizerResources.maxCardNum; num++) {
            recognizerResources.executor.submit(new Task(num));
        }
        recognizerResources.executor.sync();

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

    public void invokeAnalyse(Mat selection, int selectionNumber, CardMatch[] cardMatches) {



    }

    public static class CardMatch {

        public final int selectionNumber;
        public final int cardNumber;
        public final int score;

        public CardMatch(int selectionNumber, int cardNumber, int score) {
            this.selectionNumber = selectionNumber;
            this.cardNumber = cardNumber;
            this.score = score;
        }
    }

}
