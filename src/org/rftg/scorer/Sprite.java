package org.rftg.scorer;

import org.opencv.core.Mat;

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
}
