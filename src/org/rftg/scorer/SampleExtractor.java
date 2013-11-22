package org.rftg.scorer;

/**
* @author gc
*/
class SampleExtractor /*implements Runnable*/ {
    /*
    private Mat image;
    private int maxCardNum;
    private Point[] rect;
    private Mat destination;
    private RecognizerResources recognizerResources;
    private CardMatch[] cardMatches;

    SampleExtractor(RecognizerResources recognizerResources, Mat image, Point[] rect, Mat destination, CardMatch[] cardMatches, int maxCardNum) {
        this.recognizerResources = recognizerResources;
        this.image = image;
        this.rect = rect;
        this.destination = destination;
        this.cardMatches = cardMatches;
        this.maxCardNum = maxCardNum;
    }

    @Override
    public void run() {

        final Mat scaleDown = Imgproc.getPerspectiveTransform(new MatOfPoint2f(rect), CardPatterns.SAMPLE_RECT);
        Imgproc.warpPerspective(image, destination, scaleDown, CardPatterns.SAMPLE_SIZE, CardPatterns.SAMPLE_INTER);
        recognizerResources.customNativeTools.normalize(destination);
        scaleDown.release();

        recognizerResources.cardPatterns.invokeAnalyse(destination, cardMatches, rect, maxCardNum);

    }
      */
}
