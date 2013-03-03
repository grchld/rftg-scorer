package org.rftg.scorer;

import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author gc
 */
class Recognizer {

    private static final int MAX_LINES = 1000;

    private static final int MIN_SLOPE = -12;
    private static final int MAX_SLOPE = 12;
    private static final int MAX_GAP = 4;
    private static final int MIN_LENGTH = 70;

    private static final int MAX_BASE_GAP = 2;
    private static final int MAX_BASE_DISTANCE = 10;
    private static final int LEAST_BASE_DISTANCE = 20;

    private static double MIN_RATIO = (7./5.)/1.2;
    private static double MAX_RATIO = (7./5.)*1.2;
    private static double PARALLEL_ANGLE_BOUND = 0.1;

    private static double ANGLE_BOUND = 0.2;

    private static int MASK_LEFT = 0x10;
    private static int MASK_RIGHT = 0x20;
    private static int MASK_TOP = 0x40;
    private static int MASK_BOTTOM = 0x80;

    final MainActivity main;

    private Mat real;
    private Mat gray;
    private Mat canny;
    private Mat sobel;
    private Mat sobelTransposed;

    private Mat segmentsStackLeft;
    private Mat segmentsStackRight;
    private Mat segmentsStackTop;
    private Mat segmentsStackBottom;

    private List<Line> segmentsLeft = new ArrayList<Line>(MAX_LINES);
    private List<Line> segmentsRight = new ArrayList<Line>(MAX_LINES);
    private List<Line> segmentsTop = new ArrayList<Line>(MAX_LINES);
    private List<Line> segmentsBottom = new ArrayList<Line>(MAX_LINES);

    private Hough houghLeft;
    private Hough houghRight;
    private Hough houghTop;
    private Hough houghBottom;

    private double minX;
    private double minY;
    private double maxX;
    private double maxY;

    private int xOrigin;
    private int yOrigin;

    private TreeMap<Double, MatOfPoint> tempRects = new TreeMap<Double, MatOfPoint>();

    private long frameTimer;


    Recognizer(MainActivity main, int width, int height) {
        this.main = main;

        xOrigin = width/2;
        yOrigin = height/2;

        Mat tempReal;
        try {

            tempReal = Utils.loadResource(main, R.drawable.real);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        real = new Mat(tempReal.rows(), tempReal.cols(), CvType.CV_8UC4);

        Imgproc.cvtColor(tempReal, real, Imgproc.COLOR_BGR2RGBA);

        tempReal.release();

        gray = new Mat(height, width, CvType.CV_8UC1);

        canny = new Mat(height, width, CvType.CV_8UC1);

        sobel = new Mat(height, width, CvType.CV_8U);
        sobelTransposed = new Mat(width, height, CvType.CV_8U);

        segmentsStackLeft = new Mat(1, MAX_LINES, CvType.CV_16SC4);
        segmentsStackRight = new Mat(1, MAX_LINES, CvType.CV_16SC4);
        segmentsStackTop = new Mat(1, MAX_LINES, CvType.CV_16SC4);
        segmentsStackBottom = new Mat(1, MAX_LINES, CvType.CV_16SC4);

        houghLeft = new Hough(false, MASK_LEFT, yOrigin, segmentsStackLeft, segmentsLeft);
        houghRight = new Hough(false, MASK_RIGHT, yOrigin, segmentsStackRight, segmentsRight);
        houghTop = new Hough(true, MASK_TOP, xOrigin, segmentsStackTop, segmentsTop);
        houghBottom = new Hough(true, MASK_BOTTOM, xOrigin, segmentsStackBottom, segmentsBottom);

//        result = new Mat(height, width, CvType.CV_8UC4);

        maxX = width / 3;
        minX = maxX / 3;

        maxY = height / 1.8;
        minY = maxY / 3;
    }

    void release() {
        real.release();
        gray.release();
        canny.release();
        sobel.release();
        sobelTransposed.release();
        segmentsStackLeft.release();
        segmentsStackRight.release();
        segmentsStackTop.release();
        segmentsStackBottom.release();
    }

    class Segment {
        final int x1;
        final int x2;
        final int y1;
        final int y2;
        final int xbase;
        final int slope;
        final int length;
        final int original;

        Segment(int origin, int y1, int y2, int xbase, int slope, int length) {
            this.y1 = y1;
            this.y2 = y2;
            this.xbase = xbase;
            this.slope = slope;

            this.original = origin;
            this.length = length;

            x1 = calcX(y1);
            x2 = calcX(y2);
        }

        int calcX(int y) {
            return xbase + slope * (y - original)/64;
        }
    }

    Mat onFrame(Mat frame) {

        if (frameTimer != 0) {
            Log.e("rftg", "Total frame time: " + (System.currentTimeMillis() - frameTimer));
        }
        frameTimer = System.currentTimeMillis();

        /**/
        Mat sub = frame.submat(0,real.rows(),0,real.cols());
        real.copyTo(sub);
        sub.release();
        /**/
//        tempRects.clear();
        /**/

        long time;
        time = System.currentTimeMillis();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Log.e("rftg", "Convert color: " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        main.customNativeTools.sobel(gray, sobel, 100);

        Log.e("rftg", "Sobel: " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();

        main.customNativeTools.transpose(sobel, sobelTransposed);

        Log.e("rftg", "Transpose: " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();

        Future futureLeft = main.executorService.submit(houghLeft);
        Future futureRight = main.executorService.submit(houghRight);
        Future futureTop = main.executorService.submit(houghTop);
        Future futureBottom = main.executorService.submit(houghBottom);

        try {
            futureLeft.get();
            futureRight.get();
            futureTop.get();
            futureBottom.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        Log.e("rftg", "Hough: " + (System.currentTimeMillis() - time));


        Imgproc.cvtColor(sobel, frame, Imgproc.COLOR_GRAY2RGB);

        Scalar green = new Scalar(0, 255, 0);
        Scalar red = new Scalar(255, 0, 0);
        Scalar blue = new Scalar(0, 0, 255);
        Scalar yellow = new Scalar(255, 255, 0);

        for (Line line : segmentsLeft) {
            Core.line(frame,
                    new Point(line.x1, line.y1),
                    new Point(line.x2, line.y2),
                    red);
        }

        for (Line line : segmentsRight) {
            Core.line(frame,
                    new Point(line.x1, line.y1),
                    new Point(line.x2, line.y2),
                    green);
        }

        for (Line line : segmentsTop) {
            Core.line(frame,
                    new Point(line.x1, line.y1),
                    new Point(line.x2, line.y2),
                    yellow);
        }

        for (Line line : segmentsBottom) {
            Core.line(frame,
                    new Point(line.x1, line.y1),
                    new Point(line.x2, line.y2),
                    blue);
        }

        return frame;
    }

    class Hough implements Runnable {
        private boolean transposed;
        private int mask;
        private int origin;
        private Mat segmentsStack;
        private List<Line> segments;
        private short[] segmentData = new short[4];
        private Segment[] segmentsBuffer = new Segment[MAX_LINES];

        Hough(boolean transposed, int mask, int origin, Mat segmentsStack, List<Line> segments) {
            this.transposed = transposed;
            this.mask = mask;
            this.origin = origin;
            this.segmentsStack = segmentsStack;
            this.segments = segments;
        }

        @Override
        public void run() {

            int segmentCount = main.customNativeTools.houghVertical(transposed?sobelTransposed:sobel, mask, origin, MIN_SLOPE, MAX_SLOPE, MAX_GAP, MIN_LENGTH, segmentsStack);

            for (int i = 0 ; i < segmentCount ; i++) {
                segmentsStack.get(0, i, segmentData);
                segmentsBuffer[i] = new Segment(origin, segmentData[0], segmentData[1], segmentData[2], segmentData[3], 0);
            }

            group(segmentCount);
        }

        private void group(int size) {

            int groups = 0;

            for (int i = 0 ; i < size ; i++) {
                Segment base = segmentsBuffer[i];
                if (base != null) {
                    int y1 = base.y1;
                    int y2 = base.y2;
                    int slope = base.slope;
                    int xbaseMin = base.xbase;
                    int xbaseMax = base.xbase;
                    int length = y2 - y1;

                    for (int j = i+1 ; j < size ; j++) {
                        Segment s = segmentsBuffer[j];
                        if (s == null) {
                            continue;
                        }
                        if (s.slope != slope || s.xbase > xbaseMax + MAX_BASE_GAP) {
                            break;
                        }

                        if (s.y1 > y2 || s.y2 < y1) {
                            continue;
                        }
                        segmentsBuffer[j] = null;
                        xbaseMax = s.xbase;
                        if (y1 > s.y1) {
                            y1 = s.y1;
                        }
                        if (y2 < s.y2) {
                            y2 = s.y2;
                        }
                        int l = s.y2 - s.y1;
                        if (l > length) {
                            length = l;
                        }
                    }
                    segmentsBuffer[groups++] = new Segment(origin, y1, y2, (xbaseMin + xbaseMax)/2, slope, length);
                }
            }

            segments.clear();

            int selections = 0;
            nextbase:
            for (int i = 0 ; i < groups ; i++) {
                Segment segment = segmentsBuffer[i];
                for (int j = 0 ; j < selections ; j++) {
                    Segment base = segmentsBuffer[j];
                    if (closeEnough(base, segment)) {
                        if (segment.length > base.length) {
                            segmentsBuffer[j] = segment;
                        }
                        continue nextbase;
                    }
                }
                segmentsBuffer[selections++] = segment;
            }

            for (int i = 0 ; i < selections ; i++) {
                Segment segment = segmentsBuffer[i];
                segments.add(transposed
                        ?new Line(segment.y1, segment.x1, segment.y2, segment.x2, (segment.y2 + segment.y1)/2)
                        :new Line(segment.x1, segment.y1, segment.x2, segment.y2, (segment.x2 + segment.x1)/2)
                );
            }

            Collections.sort(segments, Line.M_COMPARATOR);
        }

        private boolean closeEnough(Segment s1, Segment s2) {
            int baseDist = s1.xbase - s2.xbase;
            if (baseDist > LEAST_BASE_DISTANCE || baseDist < -LEAST_BASE_DISTANCE || s1.y1 > s2.y2 || s2.y1 > s1.y2) {
                return false;
            }

            int a1;
            int a2;
            if (s1.y1 < s2.y1) {
                a1 = s1.calcX(s2.y1);
                a2 = s2.x1;
            } else if (s1.y1 > s2.y1) {
                a1 = s1.x1;
                a2 = s2.calcX(s1.y1);
            } else {
                a1 = s1.x1;
                a2 = s2.x1;
            }
            int a = a1 - a2;
            if (a > MAX_BASE_DISTANCE || a < -MAX_BASE_DISTANCE) {
                return false;
            }

            int b1;
            int b2;
            if (s1.y2 > s2.y2) {
                b1 = s1.calcX(s2.y2);
                b2 = s2.x2;
            } else if (s1.y2 < s2.y2) {
                b1 = s1.x2;
                b2 = s2.calcX(s1.y2);
            } else {
                b1 = s1.x2;
                b2 = s2.x2;
            }
            int b = b1 - b2;
            if (b > MAX_BASE_DISTANCE || b < -MAX_BASE_DISTANCE) {
                return false;
            }

            return true;
        }

    }

    private Point intersect(Line h, Line v) {

        double divisor = v.dx * h.dy - h.dx * v.dy;

        double x = (v.cross * h.dx - h.cross * v.dx) / divisor;
        double y = (v.cross * h.dy - h.cross * v.dy) / divisor;

        return new Point(x,y);
    }

    static class Line{

        final static Comparator<Line> M_COMPARATOR = new Comparator<Line>() {
            @Override
            public int compare(Line line1, Line line2) {
                double r = line1.m - line2.m;
                if (r > 0) {
                    return 1;
                } else if (r < 0) {
                    return -1;
                } else {
                    return 0;
                }
            }
        };

        final int x1;
        final int y1;
        final int x2;
        final int y2;

        final int dx;
        final int dy;

        final int m;

        final int cross;

//        double tan;
//        boolean horizontal;

        Line(int x1, int y1, int x2, int y2, int m) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;

            this.m = m;

            dx = x1 - x2;
            dy = y1 - y2;
/*
            mx = (x1 + x2) / 2;
            my = (y1 + y2) / 2;
        */
            cross = x1 * y2 - x2 * y1;
          /*
            horizontal = Math.abs(dx) > Math.abs(dy);
            if (horizontal) {
                tan = dy / dx;
            } else {
                tan = dx / dy;
            }
            */
        }

    }

}
