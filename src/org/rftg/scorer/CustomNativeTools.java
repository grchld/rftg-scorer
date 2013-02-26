package org.rftg.scorer;

import org.opencv.core.Mat;

/**
 * @author gc
 */
class CustomNativeTools {

    public CustomNativeTools() {
        System.loadLibrary("rftg_scorer");
    }

    public long normalize(Mat mat, double lowerPercent, double upperPercent) {
        return normalize(mat.getNativeObjAddr(), lowerPercent, upperPercent);
    }

    private native long normalize(long mat, double lowerPercent, double upperPercent);

    public void sobel(Mat src, Mat dst, int bound) {
        sobel(src.getNativeObjAddr(), dst.getNativeObjAddr(), bound);
    }

    private native void sobel(long src, long dst, int bound);

    // segments: short miny, short maxy, short x, short angle
    public int houghVertical(Mat image, int bordermask, int origin, int minSlope, int maxSlope, int maxGap, int minLength, Mat segments) {
  /*
        int DIVISOR = 64;

        int segmentNumber = 0;
        int maxSegments = segments.cols();

        int cols = image.cols();
        int rows = image.rows();
        byte mask = (byte)bordermask;

        for (int xbase = 0 ; xbase < cols; xbase++) {
            for (int slope = minSlope ; slope <= maxSlope ; slope++) {
                int xbig = xbase * DIVISOR - slope * origin;
                int count = 0;
                int length = Integer.MAX_VALUE/2;
                int last = Integer.MAX_VALUE/2;
                for (int y = 0; y < rows; y++) {
                    int x = xbig / DIVISOR;
                    xbig += slope;
                    if (x >= 0 && x < cols) {
                        byte[] b = new byte[1];
                        image.get(y,x,b);
                        if ((b[0] & mask) != 0) {
                            if (count != 0) {
                                // Line continues
                                count++;
                                length += y-last;
                                last = y;
                            } else {
                                // Beginning of the line
                                count = 1;
                                length = 1;
                                last = y;
                            }
                        } else if (count != 0) {
                            if (y - last > maxGap) {
                                // line stops
                                if (count > minLength) {
                                    // save line
                                    segments.put(0, segmentNumber, new short[]{(short)(last - length + 1), (short)last, (short)xbase, (short)slope});

                                    if (++segmentNumber == maxSegments) {
                                        // segment stack is full
                                        return maxSegments;
                                    }
                                }
                                // clear line
                                count = 0;
                            }

                        }
                    }
                }
                // force to end the line
                if (count != 0 && count > minLength) {
                    segments.put(0, segmentNumber, new short[]{(short)(last - length + 1), (short)last, (short)xbase, (short)slope});
                }
            }
        }

        return segmentNumber;
*/
        return houghVertical(image.getNativeObjAddr(), bordermask, origin, minSlope, maxSlope, maxGap, minLength, segments.getNativeObjAddr());
    }

    private native int houghVertical(long imageAddr, int bordermask, int origin, int minSlope, int maxSlope, int maxGap, int minLength, long segmentsAddr);
}
