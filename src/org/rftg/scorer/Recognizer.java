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

    private static final boolean DEBUG_SHOW_ALL_RECTANGLES = false;
    private static final boolean DEBUG_SHOW_SEGMENTS = false;

    private static final int MAX_RECTANGLES = 200;

    private static final int MAX_GAP_LEFT = 10;
    private static final int MAX_GAP = 2;
    private static final int MIN_LENGTH_LEFT = 120;
    private static final int MIN_LENGTH = 80;

    private static final double RECT_ASPECT = 1.4;
    private static final double RECT_MIN_ASPECT = RECT_ASPECT/1.25;
    private static final double RECT_MAX_ASPECT = RECT_ASPECT*1.25;

    private static final int RECT_SLOPE_BOUND = 5;

    private static final double RECT_MIN_LINE_LENGTH_PERCENT = 35;

    private static final int MASK_LEFT = 0x20;
    private static final int MASK_RIGHT = 0x10;
    private static final int MASK_TOP = 0x80;
    private static final int MASK_BOTTOM = 0x40;

    private static final Scalar COLOR_MATCH_OLD = new Scalar(255, 0, 0);
    private static final Scalar COLOR_MATCH_NEW = new Scalar(0, 255, 0);

    private static final int PREVIEW_GAP = 10;
    private static final int PREVIEW_STEP = CardPatterns.PREVIEW_WIDTH + PREVIEW_GAP;

    private final RecognizerResources recognizerResources;
    private final State state;

    private Mat real;
    private Mat rgb;
    private Mat gray;
    private Mat canny;
    private Mat sobel;
    private Mat sobelTransposed;

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


    Recognizer(RecognizerResources recognizerResources, State state, int width, int height) {
        this.state = state;
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

        houghLeft = new Hough(recognizerResources, sobel, false, MASK_LEFT, yOrigin, MAX_GAP_LEFT, MIN_LENGTH_LEFT, linesLeft);
        houghRight = new Hough(recognizerResources, sobel, false, MASK_RIGHT, yOrigin, MAX_GAP, MIN_LENGTH, linesRight);
        houghTop = new Hough(recognizerResources, sobelTransposed, true, MASK_TOP, xOrigin, MAX_GAP, MIN_LENGTH, linesTop);
        houghBottom = new Hough(recognizerResources, sobelTransposed, true, MASK_BOTTOM, xOrigin, MAX_GAP, MIN_LENGTH, linesBottom);

        for (int i = 0 ; i < MAX_RECTANGLES ; i++) {
            selection[i] = new Mat(CardPatterns.SAMPLE_HEIGHT, CardPatterns.SAMPLE_WIDTH, CvType.CV_8UC3);
        }

        minX = 50;
        minY = minX * RECT_ASPECT;

        maxY = height / 1.1;
        maxX = maxY / RECT_ASPECT;
    }

    void release() {
        rgb.release();
        real.release();
        gray.release();
        canny.release();
        sobel.release();
        sobelTransposed.release();
        houghLeft.release();
        houghRight.release();
        houghTop.release();
        houghBottom.release();
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

        /*
        Mat realsub = frame.submat(0,real.rows(),0,real.cols());
        real.copyTo(realsub);
        realsub.release();
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

        /*
        Point[] p = rectangles.get(4);
        rectangles.clear();
        rectangles.add(p);
        /**/

        time = System.currentTimeMillis();
        int selectionCounter = 0;
        for (Point[] rect : rectangles) {
            recognizerResources.executor.submit(new SampleExtractor(recognizerResources, frame, rect, selection[selectionCounter++]));
        }
        recognizerResources.executor.sync();
        Log.e("rftg", "Scaling&Normalizing " + (System.currentTimeMillis() - time));
/*
        time = System.currentTimeMillis();
        for (int i = 0 ; i < rectangles.size() ; i++) {
            final Mat image = selection[i];
            recognizerResources.executor.submit(new Runnable() {
                @Override
                public void run() {
                    recognizerResources.customNativeTools.normalize(image);
                }
            });
        }
        recognizerResources.executor.sync();
        Log.e("rftg", "Normalize " + (System.currentTimeMillis() - time));
*/
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

        if (DEBUG_SHOW_ALL_RECTANGLES) {
            List<MatOfPoint> allRectanglesToDraw = new ArrayList<MatOfPoint>(rectangles.size());
            for (Point[] rect : rectangles) {
                allRectanglesToDraw.add(new MatOfPoint(rect));
            }
            Core.polylines(frame, allRectanglesToDraw, true, new Scalar(92, 92, 255), 3);
        }

        List<MatOfPoint> rectanglesOld = new ArrayList<MatOfPoint>(matches.size());
        List<MatOfPoint> rectanglesNew = new ArrayList<MatOfPoint>(matches.size());

        for (CardMatch match : matches) {
            Point[] points = match.rect;
            MatOfPoint rect = new MatOfPoint(points);

            boolean old = false;
            for (Card card : state.player.cards) {
                if (card.id == match.cardNumber) {
                    old = true;
                    break;
                }
            }

            if (old) {
                rectanglesOld.add(rect);
            } else {
                rectanglesNew.add(rect);
                state.player.cards.add(recognizerResources.cardInfo.cards[match.cardNumber]);
            }
                             /*
            Mat warpMatrix = Imgproc.getPerspectiveTransform(new MatOfPoint2f(points), CardPatterns.SAMPLE_RECT);
            Imgproc.remap(recognizerResources.cardPatterns.samples[match.cardNumber], frame, Mat map1, Mat map2, Imgproc.INTER_LINEAR, Imgproc.BORDER_TRANSPARENT, new Scalar(0,0,0));

            scaleMatrix.release();

            */
            String name = recognizerResources.cardInfo.cards[match.cardNumber].name;
            int length = name.length() * 9;
            Core.fillConvexPoly(frame, new MatOfPoint(
                    new Point(points[0].x + 10, points[0].y + 100),
                    new Point(points[0].x + 10, points[0].y + 80),
                    new Point(points[0].x + 10 + length, points[0].y + 80),
                    new Point(points[0].x + 10 + length, points[0].y + 100)
            ), new Scalar(0,0,0));
//            Core.putText(frame, "" + match.cardNumber + " - " + match.score, new Point(points[0].x + 13, points[0].y + 76), 1, 1, new Scalar(255,255,255));
            Core.putText(frame, name, new Point(points[0].x + 13, points[0].y + 96), 1, 1, new Scalar(255,255,255));


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

        if (!state.player.cards.isEmpty()) {
            int step = (frame.cols() - PREVIEW_GAP - PREVIEW_STEP) / state.player.cards.size();
            if (step > PREVIEW_STEP) {
                step = PREVIEW_STEP;
            }

            int previewX = PREVIEW_GAP;
            int previewY = frame.rows() - CardPatterns.PREVIEW_HEIGHT - PREVIEW_GAP;

            for (Card card : state.player.cards) {

                Mat sub = frame.submat(previewY, previewY + CardPatterns.PREVIEW_HEIGHT, previewX, previewX + CardPatterns.PREVIEW_WIDTH);
                recognizerResources.cardPatterns.previews[card.id].copyTo(sub);
                sub.release();
                previewX += step;
            }
        }

        Core.putText(frame, ""+state.player.cards.size(), new Point(frame.cols() - 30, frame.rows() - CardPatterns.PREVIEW_HEIGHT - 2*PREVIEW_GAP), 1, 1, COLOR_MATCH_NEW);

        if (!rectanglesOld.isEmpty()) {
            Core.polylines(frame, rectanglesOld, true, COLOR_MATCH_OLD, 2);
        }
        if (!rectanglesNew.isEmpty()) {
            Core.polylines(frame, rectanglesNew, true, COLOR_MATCH_NEW, 3);
        }

        Core.putText(frame, ""+rectangles.size(), new Point(50,50), 1, 1, new Scalar(255, 255, 255));


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
