package org.rftg.scorer;

import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.*;

import static org.rftg.scorer.Scoring.CardScore;
import static org.rftg.scorer.ScreenProperties.Position;

/**
 * @author gc
 */
class Recognizer {

    private static final boolean DEBUG_SHOW_SOBEL = false;
    private static final boolean DEBUG_SHOW_ALL_RECTANGLES = false;
    private static final boolean DEBUG_SHOW_SEGMENTS = false;
    private static final boolean DEBUG_SHOW_RECTANGLE_COUNTER = false;
    private static final boolean DEBUG_SHOW_CARD_SCORES = false;

    private static final int MAX_RECTANGLES = 400;
    private static final int MAX_RECTANGLES_TO_USE_OUTERS = 100;

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

    private static final Scalar COLOR_SHADOW = new Scalar(0, 0, 0);
    private static final Scalar COLOR_SCORE = new Scalar(255, 255, 255);
    private static final Scalar COLOR_TOTAL = new Scalar(0, 0, 0);
    private static final Scalar COLOR_MATCH_OLD = new Scalar(255, 0, 0);
    private static final Scalar COLOR_MATCH_NEW = new Scalar(0, 255, 0);
    private static final Scalar COLOR_CHIPS = new Scalar(255, 255, 0);
    private static final Scalar COLOR_PRESTIGE = new Scalar(0, 255, 0);
    private static final Scalar COLOR_CARDS = new Scalar(128, 255, 128);
    private static final Scalar COLOR_MILITARY = new Scalar(255, 0, 0);

    private final RecognizerResources recognizerResources;
    private final ScreenProperties screen;
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

    private Mat selectionFused;
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

    final int width;
    final int height;


    Recognizer(RecognizerResources recognizerResources, State state, int width, int height) {
        this.width = width;
        this.height = height;
        this.state = state;
        this.recognizerResources = recognizerResources;
        this.screen = recognizerResources.screenProperties;

        cardMatches = new CardMatch[Card.GameType.EXP3.maxCardNum + 1];

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

        selectionFused = new Mat(CardPatterns.SAMPLE_HEIGHT*MAX_RECTANGLES, CardPatterns.SAMPLE_WIDTH, CvType.CV_8UC1);
        for (int i = 0 ; i < MAX_RECTANGLES ; i++) {
            selection[i] = selectionFused.submat(i*CardPatterns.SAMPLE_HEIGHT , (i+1)*CardPatterns.SAMPLE_HEIGHT, 0, CardPatterns.SAMPLE_WIDTH);
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
        selectionFused.release();
    }

    Mat onFrame(Mat frame) {

        if (!recognizerResources.isLoaded()) {

            Core.putText(frame, "Loading: " + recognizerResources.getLoadingPercent() + "%", new Point(frame.width() / 2 - 200, frame.height()/2 - 20), 1, 3, COLOR_SCORE, 3);
            try {
                // Yield some more resources loading thread
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Log.e("rftg", e.getMessage(), e);
            }
            return frame;
        }

        Imgproc.cvtColor(frame, rgb, Imgproc.COLOR_RGBA2RGB);
        frame = rgb;

        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGB2GRAY);
        recognizerResources.customNativeTools.sobel(gray, sobel);

        recognizerResources.customNativeTools.transpose(sobel, sobelTransposed);

        recognizerResources.executor.submit(houghLeft);
        recognizerResources.executor.submit(houghRight);
        recognizerResources.executor.submit(houghTop);
        recognizerResources.executor.submit(houghBottom);

        recognizerResources.executor.sync();

        List<Point[]> rectangles = new ArrayList<Point[]>(MAX_RECTANGLES);
        extractRectangles(rectangles, false);
        int innerRectangles = rectangles.size();
        if (rectangles.size() <= MAX_RECTANGLES_TO_USE_OUTERS) {
            extractRectangles(rectangles, true);
        }

        Arrays.fill(cardMatches, null);

        int selectionCounter = 0;
        for (Point[] rect : rectangles) {
            recognizerResources.executor.submit(new SampleExtractor(recognizerResources, gray, rect, selection[selectionCounter++], cardMatches, state.settings.gameType.maxCardNum));
        }

        recognizerResources.executor.sync();

        List<CardMatch> allMatches = new ArrayList<CardMatch>(64);
        for (int cardNumber = 0 ; cardNumber <= state.settings.gameType.maxCardNum ; cardNumber ++ ) {
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

        // Drawing results

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
                Card card = recognizerResources.cardInfo.cards[match.cardNumber];
                if (card.gamblingWorld) {
                    // need to remove another "Gambling World" card
                    for (int i = 0 ; i < state.player.cards.size() ; i++) {
                        if (state.player.cards.get(i).gamblingWorld) {
                            state.player.cards.remove(i);
                            break;
                        }
                    }
                }
                state.player.cards.add(card);
            }

        }

        // Red rectangles for already recognized cards
        if (!rectanglesOld.isEmpty()) {
            Core.polylines(frame, rectanglesOld, true, COLOR_MATCH_OLD, 2);
        }
        // Green rectangles for newly recognized cards
        if (!rectanglesNew.isEmpty()) {
            Core.polylines(frame, rectanglesNew, true, COLOR_MATCH_NEW, 3);
        }

        // Show card names
        for (CardMatch match : matches) {
            Point[] points = match.rect;
            recognizerResources.userControls.cardNames[match.cardNumber].draw(frame, (int)points[0].x + screen.cardNameOffsetX, (int)points[0].y + screen.cardNameOffsetY);
            if (DEBUG_SHOW_CARD_SCORES) {
                Sprite score = Sprite.textSpriteWithDilate(""+match.score + " >> " + match.secondScore, COLOR_SCORE, COLOR_SHADOW, 1, 1.3, 1, 1);
                score.draw(frame, (int)points[0].x + 10, (int)points[0].y + 70);
                score.release();
                recognizerResources.userControls.cardNames[match.secondCardNumber].draw(frame, (int)points[0].x + 10, (int)points[0].y + 100);
            }
        }

        Scoring scoring = new Scoring(state.player);

        // Draw accepted cards
        if (!scoring.cardScores.isEmpty()) {
            int step;
            if (scoring.cardScores.size() > 1) {
                step = (frame.cols() - screen.previewGap - screen.previewStep) / (scoring.cardScores.size() - 1);
                if (step > screen.previewStep) {
                    step = screen.previewStep;
                }
            } else {
                step = 0;
            }

            int previewX = screen.previewGap;
            int previewY = frame.rows() - screen.previewHeight - screen.previewGap;

            for (CardScore cardScore : scoring.cardScores) {
                draw(frame, recognizerResources.cardPatterns.previews[cardScore.card.id],
                        Sprite.textSpriteWithDilate(""+cardScore.score, COLOR_SCORE, COLOR_SHADOW, 1, screen.previewTextScale, 2, 2),
                        previewX, previewY);

                previewX += step;
            }
        }

        // Draw reset button
        draw(frame,recognizerResources.userControls.resetBackground, null, screen.resetIconPosition);

        // Draw chips buttons
        draw(frame, recognizerResources.userControls.chipsBackground, Sprite.textSpriteWithDilate(""+state.player.chips, COLOR_CHIPS, COLOR_SHADOW, 1, screen.chipsTextScale, 3, 1),
                screen.chipsIconPosition);

        // Draw prestige
        if (state.settings.usePrestige) {
            draw(frame, recognizerResources.userControls.prestigeBackground, Sprite.textSpriteWithDilate(""+state.player.prestige, COLOR_PRESTIGE, COLOR_SHADOW, 1, screen.prestigeTextScale, 3, 1),
                    screen.prestigeIconPosition);
        }

        // Draw military scores
        String militaryValue;
        if (scoring.military > 0) {
            militaryValue = "+" + scoring.military;
        } else {
            militaryValue = "" + scoring.military;
        }

        draw(frame, recognizerResources.userControls.militaryBackground, Sprite.textSpriteWithDilate(militaryValue, COLOR_MILITARY, COLOR_SHADOW, 1, screen.militaryTextScale, 3, 0),
                screen.militaryIconPosition);

        // Draw card counter
        draw(frame, recognizerResources.userControls.cardCountBackground, Sprite.textSpriteWithDilate(""+state.player.cards.size(), COLOR_CARDS, COLOR_SHADOW, 1, screen.cardCountTextScale, 3, 1),
                screen.cardsIconPosition);

        // Draw total
        draw(frame, recognizerResources.userControls.totalBackground, Sprite.textSpriteWithDilate(""+scoring.score, COLOR_TOTAL, COLOR_SHADOW, 1, scoring.score >= 100 ? screen.totalTextScaleShrink : screen.totalTextScale, 3, 1),
                screen.totalIconPosition);

        //
        if (DEBUG_SHOW_RECTANGLE_COUNTER) {
            Core.putText(frame, ""+innerRectangles + "+" + (rectangles.size() - innerRectangles), new Point(50,50), 1, 1, new Scalar(255, 255, 255));
        }

        if (DEBUG_SHOW_SOBEL) {
            recognizerResources.customNativeTools.drawSobel(sobel, frame);
        }

        if (DEBUG_SHOW_SEGMENTS) {
            Scalar green = new Scalar(0, 255, 0);
            Scalar red = new Scalar(255, 0, 0);
            Scalar magenta = new Scalar(255, 0, 255);
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
                        magenta);
                Core.putText(frame, line.toString(), new Point(line.mx, line.my + 20), 1, 1, magenta);
            }
        }

        return frame;
    }

    private void extractRectangles(List<Point[]> rectangles, boolean outer) {

        List<Line> linesLeft;
        List<Line> linesRight;
        List<Line> linesTop;
        List<Line> linesBottom;

        if (outer) {
            linesLeft = this.linesRight;
            linesRight = this.linesLeft;
            linesTop = this.linesBottom;
            linesBottom = this.linesTop;
        } else {
            linesLeft = this.linesLeft;
            linesRight = this.linesRight;
            linesTop = this.linesTop;
            linesBottom = this.linesBottom;
        }


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

                        if (outer) {
                            Point np1 = new Point(p1.x + (p2.x - p1.x) * CardPatterns.CARD_VERTICAL_BORDER, p1.y + (p4.y - p1.y) * CardPatterns.CARD_HORIZONTAL_BORDER);
                            Point np2 = new Point(p2.x - (p2.x - p1.x) * CardPatterns.CARD_VERTICAL_BORDER, p2.y + (p3.y - p2.y) * CardPatterns.CARD_HORIZONTAL_BORDER);
                            Point np3 = new Point(p3.x - (p3.x - p4.x) * CardPatterns.CARD_VERTICAL_BORDER, p3.y - (p3.y - p2.y) * CardPatterns.CARD_HORIZONTAL_BORDER);
                            Point np4 = new Point(p4.x + (p3.x - p4.x) * CardPatterns.CARD_VERTICAL_BORDER, p4.y - (p4.y - p1.y) * CardPatterns.CARD_HORIZONTAL_BORDER);

                            p1 = np1;
                            p2 = np2;
                            p3 = np3;
                            p4 = np4;
                        }

                        rectangles.add(new Point[]{p1, p2, p3, p4});
                        if (rectangles.size() >= MAX_RECTANGLES) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private void draw(Mat frame, Sprite background, Sprite text, Position position) {
        draw(frame, background, text, position.x, position.y);
    }

    private void draw(Mat frame, Sprite background, Sprite text, int x, int y) {
        if (x < 0) {
            x += frame.cols();
        }
        if (y < 0) {
            y += frame.rows();
        }

        background.draw(frame, x, y);

        if (text != null) {
            text.draw(frame,
                    x + background.width / 2 - text.width / 2,
                    y + background.height / 2 - text.height / 2);
            text.release();
        }
    }
}
