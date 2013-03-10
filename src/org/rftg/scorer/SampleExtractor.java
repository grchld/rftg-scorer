package org.rftg.scorer;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;

/**
* @author gc
*/
class SampleExtractor implements Runnable {
    private Mat image;
    private MatOfPoint2f rect;
    private Mat destination;

    SampleExtractor(Mat image, MatOfPoint2f rect, Mat destination) {
        this.image = image;
        this.rect = rect;
        this.destination = destination;
    }

    @Override
    public void run() {

        final Mat scaleDown = Imgproc.getPerspectiveTransform(rect, CardPatterns.SAMPLE_RECT);
        Imgproc.warpPerspective(image, destination, scaleDown, CardPatterns.SAMPLE_SIZE, Imgproc.INTER_LINEAR);
        Normalizer.normalize(destination);

    }

}
