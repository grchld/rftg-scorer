package org.rftg.scorer;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

/**
 * @author gc
 */
class CardPatterns {

    public final static int ORIGINAL_SAMPLE_HEIGHT = 520;
    public final static int ORIGINAL_SAMPLE_WIDTH = 372;
    public final static int ORIGINAL_SAMPLE_BORDER = 23;
/*
    public final static double CARD_HORIZONTAL_BORDER = ((double)ORIGINAL_SAMPLE_BORDER)/ORIGINAL_SAMPLE_HEIGHT;
    public final static double CARD_VERTICAL_BORDER = ((double)ORIGINAL_SAMPLE_BORDER)/ORIGINAL_SAMPLE_WIDTH;
  */
    public final static int SAMPLE_HEIGHT = 64;
    public final static int SAMPLE_WIDTH = 64;
    public final static int MATCHER_MINIMAL_BOUND = 5000;
    public final static int MATCHER_MINIMAL_GAP = 1000;
    public final static Size SAMPLE_SIZE = new Size(SAMPLE_WIDTH, SAMPLE_HEIGHT);

    public final static int SAMPLE_COUNT = Card.GameType.EXP3.maxCardNum + 1;

//    public final static MatOfPoint2f SAMPLE_RECT = new MatOfPoint2f(new Point(0, 0), new Point(SAMPLE_WIDTH, 0), new Point(SAMPLE_WIDTH, SAMPLE_HEIGHT), new Point(0, SAMPLE_HEIGHT));

//    public final static int SAMPLE_INTER = Imgproc.INTER_LINEAR;

    // number of samples to be loaded
    private volatile int sampleLoaderLeft = SAMPLE_COUNT;
    private final boolean[] samplesReady = new boolean[SAMPLE_COUNT];
    private final Executor.CheckedTask[] sampleLoaderTasks = new Executor.CheckedTask[SAMPLE_COUNT];
    private final ByteBuffer samples = ByteBuffer.allocateDirect(SAMPLE_WIDTH * SAMPLE_HEIGHT * SAMPLE_COUNT);

    private final MainContext mainContext;

    CardPatterns(final MainContext mainContext) {
        this.mainContext = mainContext;

        synchronized (samplesReady) {
            for (int num = 0; num < SAMPLE_COUNT; num++) {
                sampleLoaderTasks[num] = mainContext.executor.submitChecked(new LoadingTask(num));
            }
        }

        // Loading is not complete yet!!! check loaded flag before use!
    }
  /*
    public void release() {
        sampleScaleDown.release();
        for (Mat sample : samples) {
            if (sample != null) {
                sample.release();
            }
        }
        samplesFused.release();
        for (Sprite preview : previews) {
            if (preview != null) {
                preview.release();
            }
        }
    }

    public void invokeAnalyse(final Mat selection, final CardMatch[] cardMatches, final Point[] rect, final int maxCardNum) {
        recognizerResources.executor.submit(new Runnable() {
            @Override
            public void run() {

                long matchResult = recognizerResources.customNativeTools.match(selection, samplesFused, SAMPLE_HEIGHT*SAMPLE_WIDTH, maxCardNum + 1);

                int bestCardNumber = (int)matchResult & 0xffff;
                matchResult >>= 16;
                int bestScore = (int)matchResult & 0xffff;
                matchResult >>= 16;
                int secondBestCardNumber = (int)matchResult & 0xffff;
                matchResult >>= 16;
                int secondBestScore = (int)matchResult;

                if (bestScore > MATCHER_MINIMAL_BOUND && bestScore - secondBestScore > MATCHER_MINIMAL_GAP) {
                    synchronized (cardMatches) {
                        CardMatch match = cardMatches[bestCardNumber];
                        if (match == null || match.score < bestScore) {
                            cardMatches[bestCardNumber] = new CardMatch(bestCardNumber, bestScore, secondBestCardNumber, secondBestScore, rect);
                        }
                    }
                }

            }

        });
    }
    */

    /**
     * Check if loading Futures are ok
     */
    private void checkLoading() {
        synchronized (samplesReady) {
            for (int i = 0 ; i < SAMPLE_COUNT ; i++) {
                Executor.CheckedTask t = sampleLoaderTasks[i];
                if (t == null || !t.isTrusty()) {
                    // Reschedule loading
                    sampleLoaderTasks[i] = mainContext.executor.submitChecked(new LoadingTask(i));
                }
            }
        }
    }

    /**
     * @return DirectByteBuffer with samples or null if it is not loaded yet
     */
    public ByteBuffer getSamples() {
        if (sampleLoaderLeft == 0) {
            return samples;
        } else {
            synchronized (samplesReady) {
                checkLoading();
                return null;
            }
        }
    }

    /**
     * @return value between 0 and 100 if in loading phase, or -1 if resources are ready
     */
    public int getLoadingPercent() {
        return sampleLoaderLeft == 0 ? -1 : (SAMPLE_COUNT - sampleLoaderLeft) * 100 / SAMPLE_COUNT;
    }

    class LoadingTask implements Callable<Void> {

        int num;

        LoadingTask(int num) {
            this.num = num;
        }

        @Override
        public Void call() throws Exception {

            int id = mainContext.resourceContext.getResources().getIdentifier("card_" + num, "drawable", "org.rftg.scorer");
                /*
                Mat origBGR = Utils.loadResource(recognizerResources.resourceContext, id);

                Mat orig = new Mat();
                Mat origGray = new Mat();

                Imgproc.cvtColor(origBGR, orig, Imgproc.COLOR_BGR2RGB);
                Imgproc.cvtColor(origBGR, origGray, Imgproc.COLOR_BGR2GRAY);

                origBGR.release();

                Mat scaledSample = samplesFused.submat(num*SAMPLE_HEIGHT, (num+1)*SAMPLE_HEIGHT, 0, SAMPLE_WIDTH);
                Imgproc.warpAffine(origGray, scaledSample, sampleScaleDown, SAMPLE_SIZE, SAMPLE_INTER);

                recognizerResources.customNativeTools.normalize(scaledSample);

                samples[num] = scaledSample;

                ScreenProperties screen = recognizerResources.screenProperties;

                Mat scaledPreview = new Mat(screen.previewHeight, screen.previewWidth, CvType.CV_8UC3);
                Imgproc.resize(orig, scaledPreview, screen.previewSize);

                previews[num] = new Sprite(scaledPreview);

                orig.release();
                origGray.release();
                  */

            Thread.sleep(1000);

            synchronized (samplesReady) {
                if (!samplesReady[num]) {
                    samplesReady[num] = true;
                    sampleLoaderLeft--;
                    mainContext.userInterface.postInvalidate();
                } else {
                    Rftg.w("Superfluous sample loading: " + num);
                }
            }

            return null;
        }

    }
}
