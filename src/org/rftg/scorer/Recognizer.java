package org.rftg.scorer;

import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.*;

/**
 * @author gc
 */
class Recognizer {

    private static final boolean DEBUG_SHOW_ALL_RECTS = true;
    private static final boolean DEBUG_SHOW_SEGMENTS = true;

    private static final int MAX_LINES = 1000;
    private static final int MAX_RECTANGLES = 100;

    private static final int MAX_GAP_LEFT = 10;
    private static final int MAX_GAP = 2;
    private static final int MIN_LENGTH_LEFT = 120;
    private static final int MIN_LENGTH = 80;

    private static final int MAX_BASE_GAP = 2;

    private static final double RECT_MIN_ASPECT = CardPatterns.RECT_ASPECT/1.2;
    private static final double RECT_MAX_ASPECT = CardPatterns.RECT_ASPECT*1.2;

    private static final int RECT_SLOPE_BOUND = 5;

    private static final double RECT_MIN_LINE_LENGTH_PERCENT = 35;

    private static final int MASK_LEFT = 0x20;
    private static final int MASK_RIGHT = 0x10;
    private static final int MASK_TOP = 0x80;
    private static final int MASK_BOTTOM = 0x40;

    final RecognizerResources recognizerResources;

    private Mat real;
    private Mat rgb;
    private Mat gray;
    private Mat canny;
    private Mat sobel;
    private Mat sobelTransposed;

    private Mat segmentsStackLeft;
    private Mat segmentsStackRight;
    private Mat segmentsStackTop;
    private Mat segmentsStackBottom;

    private List<Line> linesLeft = new ArrayList<Line>();
    private List<Line> linesRight = new ArrayList<Line>();
    private List<Line> linesTop = new ArrayList<Line>();
    private List<Line> linesBottom = new ArrayList<Line>();

    private Mat[] selection = new Mat[MAX_RECTANGLES];

    private CardMatch[] cardMatches;

    private Hough houghLeft;
    private Hough houghRight;
    private Hough houghTop;
    private Hough houghBottom;

    private double minX;
    private double minY;
    private double maxX;
    private double maxY;

    private long frameTimer;


    Recognizer(RecognizerResources recognizerResources, int width, int height) {
        this.recognizerResources = recognizerResources;

        cardMatches = new CardMatch[recognizerResources.maxCardNum + 1];

        int xOrigin = width/2;
        int yOrigin = height/2;

        Mat tempReal;
        try {

            tempReal = Utils.loadResource(recognizerResources.resourceContext, R.drawable.real);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        real = new Mat(tempReal.rows(), tempReal.cols(), CvType.CV_8UC3);

        Imgproc.cvtColor(tempReal, real, Imgproc.COLOR_BGR2RGB);

        tempReal.release();

        rgb = new Mat(height, width, CvType.CV_8UC3);

        gray = new Mat(height, width, CvType.CV_8UC1);

        canny = new Mat(height, width, CvType.CV_8UC1);

        sobel = new Mat(height, width, CvType.CV_8U);
        sobelTransposed = new Mat(width, height, CvType.CV_8U);

        segmentsStackLeft = new Mat(1, MAX_LINES, CvType.CV_16SC4);
        segmentsStackRight = new Mat(1, MAX_LINES, CvType.CV_16SC4);
        segmentsStackTop = new Mat(1, MAX_LINES, CvType.CV_16SC4);
        segmentsStackBottom = new Mat(1, MAX_LINES, CvType.CV_16SC4);

        houghLeft = new Hough(false, MASK_LEFT, yOrigin, MAX_GAP_LEFT, MIN_LENGTH_LEFT, segmentsStackLeft, linesLeft);
        houghRight = new Hough(false, MASK_RIGHT, yOrigin, MAX_GAP, MIN_LENGTH, segmentsStackRight, linesRight);
        houghTop = new Hough(true, MASK_TOP, xOrigin, MAX_GAP, MIN_LENGTH, segmentsStackTop, linesTop);
        houghBottom = new Hough(true, MASK_BOTTOM, xOrigin, MAX_GAP, MIN_LENGTH, segmentsStackBottom, linesBottom);

        for (int i = 0 ; i < MAX_RECTANGLES ; i++) {
            selection[i] = new Mat(CardPatterns.SAMPLE_HEIGHT, CardPatterns.SAMPLE_WIDTH, CvType.CV_8UC3);
        }

        maxX = width / 2;
        minX = maxX / 5;

        maxY = height / 1.3;
        minY = maxY / 5;
    }

    void release() {
        rgb.release();
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

    Mat onFrame(Mat frame) {

        if (frameTimer != 0) {
            Log.e("rftg", "Inter frame time: " + (System.currentTimeMillis() - frameTimer));
        }

        long totalTimer = System.currentTimeMillis();

        long time;

        time = System.currentTimeMillis();


        Imgproc.cvtColor(frame, rgb, Imgproc.COLOR_RGBA2RGB);
        frame = rgb;

        /**/
        Mat sub = frame.submat(0,real.rows(),0,real.cols());
        real.copyTo(sub);
        sub.release();
        /**/

        Log.e("rftg", "Prepare image: " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGB2GRAY);
        Log.e("rftg", "Convert color: " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        recognizerResources.customNativeTools.sobel(gray, sobel);

        Log.e("rftg", "Sobel: " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();

        recognizerResources.customNativeTools.transpose(sobel, sobelTransposed);

        Log.e("rftg", "Transpose: " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();

        recognizerResources.executor.submit(houghLeft);
        recognizerResources.executor.submit(houghRight);
        recognizerResources.executor.submit(houghTop);
        recognizerResources.executor.submit(houghBottom);

        recognizerResources.executor.sync();

        Log.e("rftg", "Hough: " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        List<Point[]> rectangles = extractRectangles();
        Log.e("rftg", "Extraction: " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        int selectionCounter = 0;
        for (Point[] rect : rectangles) {
            recognizerResources.executor.submit(new SampleExtractor(frame, rect, selection[selectionCounter++]));
        }
        recognizerResources.executor.sync();
        Log.e("rftg", "Scaling " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        for (int i = 0 ; i < rectangles.size() ; i++) {
            final Mat image = selection[i++];
            recognizerResources.executor.submit(new Runnable() {
                @Override
                public void run() {
                    recognizerResources.customNativeTools.normalize(image);
                }
            });
        }
        recognizerResources.executor.sync();
        Log.e("rftg", "Normalize " + (System.currentTimeMillis() - time));


        Arrays.fill(cardMatches, null);

        time = System.currentTimeMillis();
        for (int i = 0 ; i < selectionCounter ; i++) {
            recognizerResources.cardPatterns.invokeAnalyse(selection[i], cardMatches, rectangles.get(i));
        }
        recognizerResources.executor.sync();
        Log.e("rftg", "Matching " + (System.currentTimeMillis() - time));


        time = System.currentTimeMillis();

        List<CardMatch> allMatches = new ArrayList<CardMatch>(64);
        for (int cardNumber = 0 ; cardNumber <= recognizerResources.maxCardNum ; cardNumber ++ ) {
            CardMatch match = cardMatches[cardNumber];
            if (match != null) {
                allMatches.add(match);
            }
        }

        Collections.sort(allMatches, CardMatch.MATCH_SCORE_COMPARATOR);
        List<CardMatch> matches = new ArrayList<CardMatch>(32);
        matchIntersect:
        for (CardMatch match : allMatches) {
            for (CardMatch goodMatch : matches) {
                if (goodMatch.isIntersects(match)) {
                    continue matchIntersect;
                }
            }
            matches.add(match);
        }

        Log.e("rftg", "Match filtering: " + (System.currentTimeMillis() - time));


        time = System.currentTimeMillis();

        Scalar rectColor = new Scalar(255, 255, 255);

        if (DEBUG_SHOW_ALL_RECTS) {
            List<MatOfPoint> allRectanglesToDraw = new ArrayList<MatOfPoint>(rectangles.size());
            for (Point[] rect : rectangles) {
                allRectanglesToDraw.add(new MatOfPoint(rect));
            }
            Core.polylines(frame, allRectanglesToDraw, true, new Scalar(255, 255, 92), 3);
        }


        List<MatOfPoint> rectanglesToDraw = new ArrayList<MatOfPoint>(matches.size());

        for (CardMatch match : matches) {
            Point[] points = match.rect;
            rectanglesToDraw.add(new MatOfPoint(points));
                             /*
            Mat warpMatrix = Imgproc.getPerspectiveTransform(new MatOfPoint2f(points), CardPatterns.SAMPLE_RECT);
            Imgproc.remap(recognizerResources.cardPatterns.samples[match.cardNumber], frame, Mat map1, Mat map2, Imgproc.INTER_LINEAR, Imgproc.BORDER_TRANSPARENT, new Scalar(0,0,0));

            scaleMatrix.release();

            */
            String name = recognizerResources.cardInfo.cards.get(match.cardNumber).name;
            int length = Math.max(100, name.length() * 10);
            Core.fillConvexPoly(frame, new MatOfPoint(
                    new Point(points[0].x + 20, points[0].y + 100),
                    new Point(points[0].x + 20, points[0].y + 60),
                    new Point(points[0].x + 20 + length, points[0].y + 60),
                    new Point(points[0].x + 20 + length, points[0].y + 100)
            ), new Scalar(0,0,0));
            Core.putText(frame, "" + match.cardNumber + " - " + match.score, new Point(points[0].x + 23, points[0].y + 76), 1, 1, new Scalar(255,255,255));
            Core.putText(frame, name, new Point(points[0].x + 23, points[0].y + 96), 1, 1, new Scalar(255,255,255));


            /*
            Mat bestSample = recognizerResources.cardPatterns.samples[match.cardNumber];
            draw(frame, bestSample, 400, 500);

            draw(frame, selection[0], 250, 500);

            Mat report = new Mat(bestSample.rows(), bestSample.cols(), CvType.CV_8UC3);
            recognizerResources.customNativeTools.compare(selection[0], bestSample, report);
            draw(frame, report, 400, 650);
            report.release();
                             */
        }

        /*
        for (MatOfPoint2f rect : rectangles) {
            rectanglesToDraw.add(new MatOfPoint(rect.toArray()));
        }
        */

        Core.polylines(frame, rectanglesToDraw, true, rectColor, 3);

        Core.putText(frame, ""+rectangles.size(), new Point(50,50), 1 ,1, rectColor);

        if (DEBUG_SHOW_SEGMENTS) {
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
        }

        Log.e("rftg", "Drawing: " + (System.currentTimeMillis() - time));

        Log.e("rftg", "Total calc time: " + (System.currentTimeMillis() - totalTimer));

        frameTimer = System.currentTimeMillis();

        return frame;
    }

    private void draw(Mat frame, Mat image, int x, int y) {
        Mat d = new Mat();
        Imgproc.resize(image, d, new Size(128, 128));
        d.copyTo(frame.submat(x, d.rows() + x, y, d.cols()+ y));
        d.release();
    }

    class Hough implements Runnable {
        private boolean transposed;
        private int mask;
        private int origin;
        private int maxGap;
        private int minLength;
        private Mat segmentsStack;
        private List<Line> lines;
        private short[] segmentData = new short[4];
        private Segment[] segmentsBuffer = new Segment[MAX_LINES];

        Hough(boolean transposed, int mask, int origin, int maxGap, int minLength, Mat segmentsStack, List<Line> lines) {
            this.transposed = transposed;
            this.mask = mask;
            this.origin = origin;
            this.maxGap = maxGap;
            this.minLength = minLength;
            this.segmentsStack = segmentsStack;
            this.lines = lines;
        }

        @Override
        public void run() {

            long time = System.currentTimeMillis();
            int segmentCount = recognizerResources.customNativeTools.houghVertical(transposed?sobelTransposed:sobel, mask, origin, maxGap, minLength, segmentsStack);

            Log.e("rftg", "Hough-native: " + (System.currentTimeMillis() - time));

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
                    if (base.closeEnough(segment)) {
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


    }

    private List<Point[]> extractRectangles() {
        List<Point[]> rectangles = new ArrayList<Point[]>(MAX_RECTANGLES);

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

                        Point p1 = left.intersect(top);
                        Point p2 = right.intersect(top);
                        Point p3 = right.intersect(bottom);
                        Point p4 = left.intersect(bottom);

                        rectangles.add(new Point[]{p1, p2, p3, p4});
                        if (rectangles.size() >= MAX_RECTANGLES) {
                            return rectangles;
                        }
                    }
                }
            }
        }
        return rectangles;
    }

}
