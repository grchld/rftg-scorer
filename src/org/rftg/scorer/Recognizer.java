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
    private static final int MAX_RECTANGLES = 100;

    private static final int MIN_SLOPE = -15;
    private static final int MAX_SLOPE = 15;
    private static final int MAX_GAP = 4;
    private static final int MIN_LENGTH = 70;

    private static final int MAX_BASE_GAP = 2;
    private static final int MAX_BASE_DISTANCE = 10;
    private static final int LEAST_BASE_DISTANCE = 20;

    private static final double RECT_MIN_ASPECT = (7./5.)/1.2;
    private static final double RECT_MAX_ASPECT = (7./5.)*1.2;

    private static final int RECT_SLOPE_BOUND = 5;
    /*
    private static final int RECT_MIN_WIDTH = 70;
    private static final int RECT_MAX_WIDTH = 400;
    private static final int RECT_MIN_HEIGHT = 100;
    private static final int RECT_MAX_HEIGHT = 700;
    */
    private static final double RECT_MIN_LINE_LENGTH_PERCENT = 35;

    private static final int MASK_LEFT = 0x10;
    private static final int MASK_RIGHT = 0x20;
    private static final int MASK_TOP = 0x40;
    private static final int MASK_BOTTOM = 0x80;

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

    private List<Line> linesLeft = new ArrayList<Line>(MAX_LINES);
    private List<Line> linesRight = new ArrayList<Line>(MAX_LINES);
    private List<Line> linesTop = new ArrayList<Line>(MAX_LINES);
    private List<Line> linesBottom = new ArrayList<Line>(MAX_LINES);

    private List<MatOfPoint2f> rectangles = new ArrayList<MatOfPoint2f>(MAX_RECTANGLES);

    private Mat[] selection = new Mat[MAX_RECTANGLES];

    private Hough houghLeft;
    private Hough houghRight;
    private Hough houghTop;
    private Hough houghBottom;

    private double minX;
    private double minY;
    private double maxX;
    private double maxY;

    private TreeMap<Double, MatOfPoint> tempRects = new TreeMap<Double, MatOfPoint>();

    private long frameTimer;


    Recognizer(MainActivity main, int width, int height) {
        this.main = main;

        int xOrigin = width/2;
        int yOrigin = height/2;

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

        houghLeft = new Hough(false, MASK_LEFT, yOrigin, segmentsStackLeft, linesLeft);
        houghRight = new Hough(false, MASK_RIGHT, yOrigin, segmentsStackRight, linesRight);
        houghTop = new Hough(true, MASK_TOP, xOrigin, segmentsStackTop, linesTop);
        houghBottom = new Hough(true, MASK_BOTTOM, xOrigin, segmentsStackBottom, linesBottom);

//        result = new Mat(height, width, CvType.CV_8UC4);

        for (int i = 0 ; i < MAX_RECTANGLES ; i++) {
            selection[i] = new Mat(SamplesMatcher.SAMPLE_HEIGHT, SamplesMatcher.SAMPLE_WIDTH, CvType.CV_8UC3);
        }

        maxX = width / 2;
        minX = maxX / 5;

        maxY = height / 1.3;
        minY = maxY / 5;
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
        for (Mat mat : selection) {
            mat.release();
        }
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

        time = System.currentTimeMillis();
        extractRectangles();
        Log.e("rftg", "Extraction: " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        int selectionCounter = 0;
        List<Future> selectionFutures = new ArrayList<Future>(rectangles.size());
        for (MatOfPoint2f rect : rectangles) {
            selectionFutures.add(main.executorService.submit(new SamplesMatcher.SampleExtractor(frame, rect, selection[selectionCounter++])));
        }
        for (Future future : selectionFutures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        Log.e("rftg", "Scaling " + (System.currentTimeMillis() - time));
        //////////

        Scalar rectColor = new Scalar(255, 255, 255);

        List<MatOfPoint> rectanglesToDraw = new ArrayList<MatOfPoint>(rectangles.size());
        for (MatOfPoint2f rect : rectangles) {
            rectanglesToDraw.add(new MatOfPoint(rect.toArray()));
        }
        Core.polylines(frame, rectanglesToDraw, true, rectColor, 3);

        Core.putText(frame, ""+rectangles.size(), new Point(50,50), 1 ,1, rectColor);

//        Imgproc.cvtColor(sobel, frame, Imgproc.COLOR_GRAY2RGB);

        Scalar green = new Scalar(0, 255, 0);
        Scalar red = new Scalar(255, 0, 0);
        Scalar blue = new Scalar(255, 0, 255);
        Scalar yellow = new Scalar(255, 255, 0);

        for (Line line : linesLeft) {
            Core.line(frame,
                    new Point(line.x1, line.y1),
                    new Point(line.x2, line.y2),
                    red);
            Core.putText(frame, line.toString(), new Point(line.mx - 10, line.my), 1, 1, red);
        }

        for (Line line : linesRight) {
            Core.line(frame,
                    new Point(line.x1, line.y1),
                    new Point(line.x2, line.y2),
                    green);
            Core.putText(frame, line.toString(), new Point(line.mx + 10, line.my), 1, 1, green);
        }

        for (Line line : linesTop) {
            Core.line(frame,
                    new Point(line.x1, line.y1),
                    new Point(line.x2, line.y2),
                    yellow);
            Core.putText(frame, line.toString(), new Point(line.mx, line.my - 20), 1, 1, yellow);
        }

        for (Line line : linesBottom) {
            Core.line(frame,
                    new Point(line.x1, line.y1),
                    new Point(line.x2, line.y2),
                    blue);
            Core.putText(frame, line.toString(), new Point(line.mx, line.my + 20), 1, 1, blue);
        }


        return frame;
    }

    class Hough implements Runnable {
        private boolean transposed;
        private int mask;
        private int origin;
        private Mat segmentsStack;
        private List<Line> lines;
        private short[] segmentData = new short[4];
        private Segment[] segmentsBuffer = new Segment[MAX_LINES];

        Hough(boolean transposed, int mask, int origin, Mat segmentsStack, List<Line> lines) {
            this.transposed = transposed;
            this.mask = mask;
            this.origin = origin;
            this.segmentsStack = segmentsStack;
            this.lines = lines;
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

            lines.clear();

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
                lines.add(transposed
                        ? new Line(segment.y1, segment.x1, segment.y2, segment.x2, segment.slope)
                        : new Line(segment.x1, segment.y1, segment.x2, segment.y2, segment.slope)
                );
            }

            Collections.sort(lines, Line.MX_COMPARATOR);
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

    static class Line {

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

        final int x1;
        final int y1;
        final int x2;
        final int y2;

        final int dx;
        final int dy;

        final int mx;
        final int my;

        final int cross;

        final int slope;

        Line(int x1, int y1, int x2, int y2, int slope) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;

            this.mx = (x1 + x2)/2;
            this.my = (y1 + y2)/2;
            this.slope = slope;

            dx = x2 - x1;
            dy = y2 - y1;
            cross = x2 * y1 - x1 * y2;
        }

        @Override
        public String toString() {
            return "" + mx + ":" + my + ":" + slope;
        }
    }

    class Rectangle {
        public final Point r1;
        public final Point r2;
        public final Point r3;
        public final Point r4;

        Rectangle(Point r1, Point r2, Point r3, Point r4) {
            this.r1 = r1;
            this.r2 = r2;
            this.r3 = r3;
            this.r4 = r4;
        }
    }

    private void extractRectangles() {
        rectangles.clear();

        int minRight = 0;
        int minTop = 0;
        int minBottom = 0;


        rectanglesDone:
        for (int leftIndex = 0 ; leftIndex < linesLeft.size() ; leftIndex++ ) {
            Line left = linesLeft.get(leftIndex);
            for (int rightIndex = minRight ; rightIndex < linesRight.size() ; rightIndex++ ) {
                Line right = linesRight.get(rightIndex);
                if (left.mx + minX > right.mx) {
                    minRight++;
                    if (minRight == linesRight.size()) {
                        break rectanglesDone;
                    }
                    continue;
                }
                if (left.mx + maxX < right.mx) {
                    break;
                }
                int slopeDiffX = left.slope - right.slope;
                if (slopeDiffX > RECT_SLOPE_BOUND || slopeDiffX < -RECT_SLOPE_BOUND) {
                    continue;
                }

                for (int topIndex = minTop ; topIndex < linesTop.size() ; topIndex++ ) {
                    Line top = linesTop.get(topIndex);
                    if (left.mx > top.mx) {
                        minTop++;
                        if (minTop == linesTop.size()) {
                            break rectanglesDone;
                        }
                        continue;
                    }
                    if (right.mx < top.mx || top.my > right.my || top.my > left.my) {
                        continue;
                    }

                    for (int bottomIndex = minBottom ; bottomIndex < linesBottom.size() ; bottomIndex++ ) {
                        Line bottom = linesBottom.get(bottomIndex);
                        if (left.mx > bottom.mx) {
                            minBottom++;
                            if (minBottom == linesBottom.size()) {
                                break rectanglesDone;
                            }
                            continue;
                        }
                        if (right.mx < bottom.mx || bottom.my < right.my || bottom.my < left.my) {
                            continue;
                        }
                        int slopeDiffY = top.slope - bottom.slope;
                        if (slopeDiffY > RECT_SLOPE_BOUND || slopeDiffY < -RECT_SLOPE_BOUND) {
                            continue;
                        }

                        int height = bottom.my - top.my;
                        if (height < minY || height > maxY) {
                            continue;
                        }
                        int width = right.mx - left.mx;
                        double aspect = ((double)height)/width;
                        if (aspect < RECT_MIN_ASPECT || aspect > RECT_MAX_ASPECT) {
                            continue;
                        }

                        if (height * RECT_MIN_LINE_LENGTH_PERCENT > left.dy * 100 || height * RECT_MIN_LINE_LENGTH_PERCENT > right.dy * 100
                                || width * RECT_MIN_LINE_LENGTH_PERCENT > top.dx * 100 || width * RECT_MIN_LINE_LENGTH_PERCENT > bottom.dx * 100) {
                            continue;
                        }


                        Point p1 = intersect(left, top);
                        Point p2 = intersect(right, top);
                        Point p3 = intersect(right, bottom);
                        Point p4 = intersect(left, bottom);

                        MatOfPoint2f rect = new MatOfPoint2f(p1, p2, p3, p4);
                        rectangles.add(rect);
                        if (rectangles.size() >= MAX_RECTANGLES) {
                            return;
                        }
                    }
                }
            }
        }

    }

}
