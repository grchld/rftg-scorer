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
    public final static int ORIGINAL_SAMPLE_BORDER = 23;

    public final static int SAMPLE_HEIGHT = 64;
    public final static int SAMPLE_WIDTH = 64;
    public final static int MATCHER_MINIMAL_BOUND = 5000;
    public final static int MATCHER_MINIMAL_GAP = 500;
    public final static Size SAMPLE_SIZE = new Size(SAMPLE_WIDTH, SAMPLE_HEIGHT);
    public final static MatOfPoint2f SAMPLE_RECT = new MatOfPoint2f(new Point(0, 0), new Point(SAMPLE_WIDTH, 0), new Point(SAMPLE_WIDTH, SAMPLE_HEIGHT), new Point(0, SAMPLE_HEIGHT));

    public final static int PREVIEW_HEIGHT = 70;
    public final static int PREVIEW_WIDTH = 50;
    public final static Size PREVIEW_SIZE = new Size(PREVIEW_WIDTH, PREVIEW_HEIGHT);

    public final static int SAMPLE_INTER = Imgproc.INTER_LINEAR;

    private final Mat[] samples;
    private final RecognizerResources recognizerResources;
    public final Mat[] previews;


    public CardPatterns(final RecognizerResources recognizerResources) {

        this.recognizerResources = recognizerResources;

        samples = new Mat[recognizerResources.maxCardNum + 1];
        previews = new Mat[recognizerResources.maxCardNum + 1];

        final Mat sampleScaleDown = Imgproc.getAffineTransform(
                new MatOfPoint2f(
                        new Point(ORIGINAL_SAMPLE_BORDER, ORIGINAL_SAMPLE_BORDER),
                        new Point(ORIGINAL_SAMPLE_WIDTH-ORIGINAL_SAMPLE_BORDER, ORIGINAL_SAMPLE_BORDER),
                        new Point(ORIGINAL_SAMPLE_BORDER, ORIGINAL_SAMPLE_HEIGHT-ORIGINAL_SAMPLE_BORDER)),
                new MatOfPoint2f(new Point(0, 0), new Point(SAMPLE_WIDTH, 0), new Point(0, SAMPLE_HEIGHT)));
        long time = System.currentTimeMillis();

        class Task implements Callable<Void> {

            int num;

            Task(int num) {
                this.num = num;
            }

            @Override
            public Void call() throws Exception {
                int id = recognizerResources.resourceContext.getResources().getIdentifier("card_" + num, "drawable", "org.rftg.scorer");

                Mat tempSampleBGR = Utils.loadResource(recognizerResources.resourceContext, id);

                Mat tempSample = new Mat();

                Imgproc.cvtColor(tempSampleBGR, tempSample, Imgproc.COLOR_BGR2RGB);

                tempSampleBGR.release();

                Mat scaledSample = new Mat(SAMPLE_WIDTH, SAMPLE_HEIGHT, CvType.CV_8UC3);
                Imgproc.warpAffine(tempSample, scaledSample, sampleScaleDown, SAMPLE_SIZE, SAMPLE_INTER);

                recognizerResources.customNativeTools.normalize(scaledSample);

                samples[num] = scaledSample;

                Mat scaledPreview = new Mat(PREVIEW_WIDTH, PREVIEW_HEIGHT, CvType.CV_8UC3);
                Imgproc.resize(tempSample, scaledPreview, PREVIEW_SIZE);

                previews[num] = scaledPreview;

                tempSample.release();

                return null;
            }

        }

        for (int num = 0; num <= recognizerResources.maxCardNum; num++) {
            recognizerResources.executor.submit(new Task(num));
        }
        recognizerResources.executor.sync();

        Log.i("rftg", "Parallel loading time: " + (System.currentTimeMillis() - time));

        sampleScaleDown.release();
    }

    public void release() {
        for (Mat sample : samples) {
            if (sample != null) {
                sample.release();
            }
        }
        for (Mat preview : previews) {
            if (preview != null) {
                preview.release();
            }
        }
    }

    public void invokeAnalyse(final Mat selection, final CardMatch[] cardMatches, final Point[] rect) {
        recognizerResources.executor.submit(new Runnable() {
            @Override
            public void run() {

                int bestCardNumber = 0;
                int secondBestScore = 0;
                int bestScore = 0;

                for (int cardNumber = 0; cardNumber <= recognizerResources.maxCardNum; cardNumber++) {

                    Mat sample = samples[cardNumber];

                    int score = recognizerResources.customNativeTools.compare(selection, sample);

                    if (score > MATCHER_MINIMAL_BOUND) {
                        if (bestScore < score) {
                            secondBestScore = bestScore;
                            bestScore = score;
                            bestCardNumber = cardNumber;
                        } else if (secondBestScore < score) {
                            secondBestScore = score;
                        }
                    }

                }

                if (bestScore > MATCHER_MINIMAL_BOUND && bestScore - secondBestScore > MATCHER_MINIMAL_GAP) {
                    synchronized (cardMatches) {
                        CardMatch match = cardMatches[bestCardNumber];
                        if (match == null || match.score < bestScore) {
                            cardMatches[bestCardNumber] = new CardMatch(bestCardNumber, bestScore, rect);
                        }
                    }
                }

            }

        });
    }

}
