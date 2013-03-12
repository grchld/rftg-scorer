package org.rftg.scorer;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

/**
* @author gc
*/
class SampleExtractor implements Runnable {
    private Mat image;
    private Point[] rect;
    private Mat destination;

    SampleExtractor(Mat image, Point[] rect, Mat destination) {
        this.image = image;
        this.rect = rect;
        this.destination = destination;
    }

    @Override
    public void run() {

        final Mat scaleDown = Imgproc.getPerspectiveTransform(new MatOfPoint2f(rect), CardPatterns.SAMPLE_RECT);
        Imgproc.warpPerspective(image, destination, scaleDown, CardPatterns.SAMPLE_SIZE, Imgproc.INTER_LINEAR);
        Normalizer.normalize(destination);

    }

}
