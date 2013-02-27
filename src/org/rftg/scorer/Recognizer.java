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

    private static final int MIN_SLOPE = -11;
    private static final int MAX_SLOPE = 12;
    private static final int MAX_GAP = 5;
    private static final int MIN_LENGTH = 70;

    private static final int MAX_BASE_GAP = 2;
    private static final int MAX_BASE_DISTANCE = 5;

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

    private List<Segment> segmentsLeft = new ArrayList<Segment>(MAX_LINES);
    private List<Segment> segmentsRight = new ArrayList<Segment>(MAX_LINES);
    private List<Segment> segmentsTop = new ArrayList<Segment>(MAX_LINES);
    private List<Segment> segmentsBottom = new ArrayList<Segment>(MAX_LINES);

    private Hough houghLeft;
    private Hough houghRight;
    private Hough houghTop;
    private Hough houghBottom;

//    private Mat result;

    private int counter;

    private double minX;
    private double minY;
    private double maxX;
    private double maxY;

    private int xOrigin;
    private int yOrigin;

    private TreeMap<Double, MatOfPoint> tempRects = new TreeMap<Double, MatOfPoint>();

    List<Line> horizontal = new ArrayList<Line>(MAX_LINES);
    List<Line> vertical = new ArrayList<Line>(MAX_LINES);

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

        houghLeft = new Hough(sobel, MASK_LEFT, yOrigin, segmentsStackLeft, segmentsLeft);
        houghRight = new Hough(sobel, MASK_RIGHT, yOrigin, segmentsStackRight, segmentsRight);
        houghTop = new Hough(sobelTransposed, MASK_TOP, xOrigin, segmentsStackTop, segmentsTop);
        houghBottom = new Hough(sobelTransposed, MASK_BOTTOM, xOrigin, segmentsStackBottom, segmentsBottom);

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
        final int origin;

        Segment(int origin, short[] v) {
            this(origin, v[0], v[1], v[2], v[3]);
        }

        Segment(int origin, int y1, int y2, int xbase, int slope) {
            this.y1 = y1;
            this.y2 = y2;
            this.xbase = xbase;
            this.slope = slope;
            this.origin = origin;

            x1 = xbase + slope * (y1 - origin)/64;
            x2 = xbase + slope * (y2 - origin)/64;
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
//        Imgproc.Sobel(gray, sobelX, CvType.CV_8U, 1, 0, 3, 0.25, 128);
        main.customNativeTools.sobel(gray, sobel, 100);

        Log.e("rftg", "Sobel: " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();

        Core.transpose(sobel, sobelTransposed);

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

        Log.e("rftg", "HoughVertical: " + (System.currentTimeMillis() - time));

        Imgproc.cvtColor(sobel, frame, Imgproc.COLOR_GRAY2RGB);


        Scalar green = new Scalar(0, 255, 0);
        Scalar red = new Scalar(255, 0, 0);
        Scalar blue = new Scalar(0, 0, 255);
        Scalar yellow = new Scalar(255, 255, 0);

        for (Segment segment : segmentsLeft) {
            Core.line(frame,
                    new Point(segment.x1, segment.y1),
                    new Point(segment.x2, segment.y2),
                    red);
        }

        for (Segment segment : segmentsRight) {
            Core.line(frame,
                    new Point(segment.x1, segment.y1),
                    new Point(segment.x2, segment.y2),
                    green);
        }

        for (Segment segment : segmentsTop) {
            Core.line(frame,
                    new Point(segment.y1, segment.x1),
                    new Point(segment.y2, segment.x2),
                    yellow);
        }

        for (Segment segment : segmentsBottom) {
            Core.line(frame,
                    new Point(segment.y1, segment.x1),
                    new Point(segment.y2, segment.x2),
                    blue);
        }

        return frame;
    }

    class Hough implements Runnable {
        private Mat image;
        private int mask;
        private int origin;
        private Mat segmentsStack;
        private List<Segment> segments;

        Hough(Mat image, int mask, int origin, Mat segmentsStack, List<Segment> segments) {
            this.image = image;
            this.mask = mask;
            this.origin = origin;
            this.segmentsStack = segmentsStack;
            this.segments = segments;
        }

        @Override
        public void run() {
            segments.clear();

            int segmentCount = main.customNativeTools.houghVertical(image, mask, origin, MIN_SLOPE, MAX_SLOPE, MAX_GAP, MIN_LENGTH, segmentsStack);

            short[] segmentData = new short[4];
            for (int i = 0 ; i < segmentCount ; i++) {
                segmentsStack.get(0, i, segmentData);
                segments.add(new Segment(origin, segmentData));
            }
        }
    }

    private List<Segment> group(int origin, Segment[] segments) {
        List<Segment> result = new ArrayList<Segment>(segments.length);
        for (int i = 0 ; i < segments.length ; i++) {
            Segment base = segments[i];
            if (base != null) {
                int y1 = base.y1;
                int y2 = base.y2;
                int slope = base.slope;
                int xbaseMin = base.xbase;
                int xbaseMax = base.xbase;

                for (int j = i+1 ; j < segments.length ; j++) {
                    Segment s = segments[j];
                    if (s == null) {
                        continue;
                    }
                    if (s.xbase > xbaseMax + MAX_BASE_GAP) {
                        break;
                    }
                    if (s.slope != slope) {
                        continue;
                    }

                    if (s.y1 > y2 || s.y2 < y1) {
                        continue;
                    }
                    segments[j] = null;
                    xbaseMax = s.xbase;
                    if (y1 > s.y1) {
                        y1 = s.y1;
                    }
                    if (y2 < s.y2) {
                        y2 = s.y2;
                    }
                }
                result.add(new Segment(origin, y1, y2, (xbaseMin + xbaseMax)/2, slope));
            }
        }
        return result;
    }


    private Point intersect(Line h, Line v) {

        double divisor = v.dx * h.dy - h.dx * v.dy;

        double x = (v.cross * h.dx - h.cross * v.dx) / divisor;
        double y = (v.cross * h.dy - h.cross * v.dy) / divisor;

        return new Point(x,y);
    }

    static class Line{

        final static Comparator<Line> MX_COMPARATOR = new Comparator<Line>() {
            @Override
            public int compare(Line line1, Line line2) {
                double r = line1.mx - line2.mx;
                if (r > 0) {
                    return 1;
                } else if (r < 0) {
                    return -1;
                } else {
                    return 0;
                }
            }
        };

        double x1;
        double y1;
        double x2;
        double y2;

        double dx;
        double dy;

        double mx;
        double my;

        double cross;

        double tan;
        boolean horizontal;

        Line(double[] points) {
            x1 = points[0];
            y1 = points[1];
            x2 = points[2];
            y2 = points[3];

            dx = x1 - x2;
            dy = y1 - y2;

            mx = (x1 + x2) / 2;
            my = (y1 + y2) / 2;

            cross = x1 * y2 - x2 * y1;

            horizontal = Math.abs(dx) > Math.abs(dy);
            if (horizontal) {
                tan = dy / dx;
            } else {
                tan = dx / dy;
            }
        }

    }

}
