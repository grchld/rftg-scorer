package org.rftg.scorer;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

/**
 * @author gc
 */
class Sprite {

    private Mat image;
    private Mat mask;
    private int width;
    private int height;

    public Sprite(Mat image) {
        this(image, null);
    }

    public Sprite(Mat image, Mat mask) {
        this.image = image;
        this.width = image.width();
        this.height = image.height();
        this.mask = mask;
        if (mask != null && (mask.width() != width || mask.height() != height)) {
            throw new IllegalArgumentException("Bad mask dimensions");
        }
    }

    public void draw(Mat frame, int x, int y) {
        int colStart = x;
        int colEnd = x + width;
        int rowStart = y;
        int rowEnd = y + height;

        int xStart = 0;
        int xEnd = width;
        int yStart = 0;
        int yEnd = height;

        if (colStart < 0) {
            xStart -= colStart;
            colStart = 0;
        }
        if (colEnd > frame.cols()) {
            xEnd -= colEnd - frame.cols();
            colEnd = frame.cols();
        }
        if (xStart >= xEnd) {
            return;
        }

        if (rowStart < 0) {
            yStart -= rowStart;
            rowStart = 0;
        }
        if (rowEnd > frame.rows()) {
            yEnd -= rowEnd - frame.rows();
            rowEnd = frame.rows();
        }
        if (yStart >= yEnd) {
            return;
        }


        Mat clippedImage = null;
        Mat clippedMask = null;

        if (xStart != 0 || xEnd != width || yStart != 0 || yEnd != height) {
            clippedImage = image.submat(yStart, yEnd, xStart, xEnd);
            if (mask != null) {
                clippedMask = mask.submat(yStart, yEnd, xStart, xEnd);
            }
        }

        Mat destination = frame.submat(rowStart, rowEnd, colStart, colEnd);

        Mat actualImage = clippedImage == null ? image : clippedImage;
        if (mask == null) {
            actualImage.copyTo(destination);
        } else {
            actualImage.copyTo(destination, clippedMask == null ? mask : clippedMask);
        }

        if (clippedImage != null) {
            clippedImage.release();
        }
        if (clippedMask != null) {
            clippedMask.release();
        }

        destination.release();
    }

    public void release() {
        image.release();
        if (mask != null) {
            mask.release();
        }
    }

    private final static Scalar MASK_TRANSPARENT = new Scalar(0);
    private final static Scalar MASK_OPAQUE = new Scalar(255);

    private final static Mat DILATE_KERNEL = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
    private final static Point DILATE_ANCHOR = new Point(-1,-1);

    public static Sprite textSpriteWithDilate(String text, Scalar textColor, Scalar textShadow, int fontFace, double fontScale, int thickness, int dilateSize) {
        int[] baseLine = new int[1];

        Size textSize = Core.getTextSize(text, fontFace, fontScale, thickness, baseLine);

        Mat image = new Mat((int)textSize.height + baseLine[0] + 2 * dilateSize, (int)textSize.width + 2 * dilateSize, CvType.CV_8UC3, textShadow);
        Point textOrigin = new Point(dilateSize, image.height() - baseLine[0] - dilateSize);

        Core.putText(image, text, textOrigin, fontFace, fontScale, textColor, thickness);

        Mat mask = new Mat(image.rows(), image.cols(), CvType.CV_8U, MASK_TRANSPARENT);
        Core.putText(mask, text, textOrigin, fontFace, fontScale, MASK_OPAQUE, thickness);

        Imgproc.dilate(mask, mask, DILATE_KERNEL, DILATE_ANCHOR, dilateSize);

        return new Sprite(image, mask);
    }
}
