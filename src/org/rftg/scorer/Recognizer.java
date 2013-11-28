package org.rftg.scorer;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author gc
 */
class Recognizer {

    private static final int MAX_GAP_LEFT = 10;
    private static final int MAX_GAP = 2;
    private static final int MIN_LENGTH_LEFT = 120;
    private static final int MIN_LENGTH = 80;

    private static final int MASK_LEFT = 0x20;
    private static final int MASK_RIGHT = 0x10;
    private static final int MASK_TOP = 0x80;
    private static final int MASK_BOTTOM = 0x40;

    private final List<Line> linesLeft = new ArrayList<Line>();
    private final List<Line> linesRight = new ArrayList<Line>();
    private final List<Line> linesTop = new ArrayList<Line>();
    private final List<Line> linesBottom = new ArrayList<Line>();

    private final List<Point[]> rectangles = new ArrayList<Point[]>();

    private final CardMatch[] cardMatches = new CardMatch[CardPatterns.SAMPLE_COUNT];
    private final List<CardMatch> sortedCardMatches = new ArrayList<CardMatch>();
    final List<CardMatch> collectedCardMatches = new ArrayList<CardMatch>();

    private final List<Buffer> cardMatchesBuffers = new ArrayList<Buffer>();

    private final MainContext mainContext;

    Size frameSize;
    private ByteBuffer frame;
    private ByteBuffer sobel;
    private ByteBuffer sobelTransposed;

    volatile ByteBuffer debugPicture;
    volatile List<Point[]> debugRectangles;

    private AtomicInteger houghSync = new AtomicInteger(0);
    private AtomicInteger matchSync = new AtomicInteger(0);


    private long timingStartFrame;
    private long timingCopyTime;
    private long timingSobelTime;
    private long timingTransposeTime;
    private long timingTotalHoughTime;
    private long timingTotalSegmentTime;
    private long timingTotalGroupTime;
    private long timingExtractRectTime;
    private int timingRectCount;
    private long timingTotalWarpTime;
    private long timingTotalNormalizeTime;
    private long timingTotalMatchTime;

    Recognizer(MainContext mainContext) {
        this.mainContext = mainContext;
    }

    private RecognizerTask copyFrame = new RecognizerTask() {
        @Override
        void execute() throws Exception {
            /*
            if (timingStartFrame != 0) {
                Rftg.i("Frame: "+((System.nanoTime() - timingStartFrame)/1000000) +
                        ", copy: " + (timingCopyTime / 1000000) +
                        ", sobel: " + (timingSobelTime / 1000000) +
                        ", trans: " + (timingTransposeTime / 1000000) +
                        ", hough: " + (timingTotalHoughTime / 1000000) + "=4*" + (timingTotalHoughTime / 1000000 / 4) +
                        ", segment: " + (timingTotalSegmentTime / 1000000) + "=4*" + (timingTotalSegmentTime / 1000000 / 4) +
                        ", group: " + (timingTotalGroupTime / 1000000) + "=4*" + (timingTotalGroupTime / 1000000 / 4) +
                        ", rect: " + (timingExtractRectTime / 1000000) +
                        ", warp: " + (timingTotalWarpTime / 1000000) + "=" + timingRectCount + "*" + (timingRectCount == 0 ? "?" : "" + (timingTotalWarpTime / 1000 / timingRectCount) + "/1000") +
                        ", norm: " + (timingTotalNormalizeTime / 1000000) + "=" + timingRectCount + "*" + (timingRectCount == 0 ? "?" : "" + (timingTotalNormalizeTime / 1000 / timingRectCount) + "/1000") +
                        ", match: " + (timingTotalMatchTime / 1000000) + "=" + timingRectCount + "*" + (timingRectCount == 0 ? "?" : "" + (timingTotalMatchTime / 1000 / timingRectCount) + "/1000")
                );
            } */

            timingStartFrame = System.nanoTime();
            if (mainContext.fastCamera.copyFrame(frame, frameSize)) {
                frame.position(0);
                timingCopyTime = System.nanoTime() - timingStartFrame;
                calcSobel.execute();
            } else {
                Size cameraActualSize = mainContext.fastCamera.getActualSize();
                if (cameraActualSize == null || cameraActualSize.equals(frameSize)) {
                    // Camera is not ready to give us a frame
                    // Wait a little and retry
                    Rftg.w("Camera frame is not ready yet");
                    Thread.sleep(100);
                } else {
                    // Our frame buffer has the wrong size, recreate frame buffer and retry
                    frameSize = cameraActualSize;
                    int frameBufferSize = frameSize.width * frameSize.height;
                    frame = ByteBuffer.allocateDirect(frameBufferSize);
                    sobel = ByteBuffer.allocateDirect(frameBufferSize);
                    sobelTransposed = ByteBuffer.allocateDirect(frameBufferSize);
                    debugPicture = ByteBuffer.allocateDirect(64*64);
                    initTasks();
                }
                startFrameRecognition();
            }
        }
    };

    private RecognizerTask calcSobel = new RecognizerTask() {
        @Override
        void execute() throws Exception {
            long time = System.nanoTime();
            NativeTools.sobel(frame, sobel, frameSize.width, frameSize.height);
            timingSobelTime = System.nanoTime() - time;
            timingTotalHoughTime = 0;
            timingTotalSegmentTime = 0;
            timingTotalGroupTime = 0;

            if (!houghSync.compareAndSet(0, 4)) {
                Rftg.e("Hough sync counter has bad value");
                Thread.sleep(100);
                houghSync.set(0);
                startFrameRecognition();
            } else {
                mainContext.executor.submit(calcTranspose);
                mainContext.executor.submit(calcHoughLeft);
                mainContext.executor.submit(calcHoughRight);
            }
        }
    };

    private RecognizerTask calcTranspose = new RecognizerTask() {
        @Override
        void execute() throws Exception {
            long time = System.nanoTime();
            NativeTools.transpose(sobel, sobelTransposed, frameSize.width, frameSize.height);
            timingTransposeTime = System.nanoTime() - time;

            mainContext.executor.submit(calcHoughTop);
            mainContext.executor.submit(calcHoughBottom);
        }
    };

    private RecognizerTask calcHoughLeft;
    private RecognizerTask calcHoughRight;
    private RecognizerTask calcHoughTop;
    private RecognizerTask calcHoughBottom;

    class HoughTask extends Hough {
        private HoughTask(ByteBuffer sobel, int width, int height, boolean transposed, int mask, int origin, int maxGap, int minLength, List<Line> lines) {
            super(sobel, width, height, transposed, mask, origin, maxGap, minLength, lines);
        }

        @Override
        void execute() throws Exception {
            super.execute();
            timingTotalHoughTime += timingHoughTime;
            timingTotalSegmentTime += timingSegmentTime;
            timingTotalGroupTime += timingGroupTime;
            if (houghSync.decrementAndGet() == 0) {
                extractRect.execute();
            }
        }
    }

    private RecognizerTask extractRect;

    class RectExtractorTask extends RectExtractor {

        RectExtractorTask() {
            super(rectangles, linesLeft, linesRight, linesTop, linesBottom, frameSize);
        }

        @Override
        void execute() throws Exception {
            long time = System.nanoTime();
            super.execute();
            timingExtractRectTime = System.nanoTime() - time;
/*
            for (Point[] p : rectangles) {
                debugRectangles = Collections.singletonList(p);
                NativeTools.warp(frame, frameSize.width, frameSize.height, debugPicture, p[0].x, p[0].y, p[1].x, p[1].y, p[2].x, p[2].y, p[3].x, p[3].y);
                break;
            }
*/
            timingRectCount = rectangles.size();
            timingTotalWarpTime = 0;
            timingTotalNormalizeTime = 0;
            timingTotalMatchTime = 0;

            if (rectangles.isEmpty()) {
                synchronized (collectedCardMatches) {
                    collectedCardMatches.clear();
                }
                mainContext.userInterface.postInvalidate();
                startFrameRecognition();
            } else {
                Arrays.fill(cardMatches, null);
                matchSync.set(rectangles.size());
                for (Point[] rect : rectangles) {
                    mainContext.executor.submit(new CardMatcherTask(rect));
                }
            }
        }
    }

    class CardMatcherTask extends CardMatcher {
        CardMatcherTask(Point[] rect) {
            super(frame, frameSize, mainContext.cardPatterns.samples, rect, cardMatches);
        }

        @Override
        protected Buffer createBuffer() {
            synchronized (cardMatchesBuffers) {
                if (cardMatchesBuffers.isEmpty()) {
                    return ByteBuffer.allocateDirect(CardPatterns.SAMPLE_WIDTH * CardPatterns.SAMPLE_HEIGHT);
                } else {
                    return cardMatchesBuffers.remove(cardMatchesBuffers.size() - 1);
                }
            }
        }

        @Override
        protected void releaseBuffer(Buffer buffer) {
            synchronized (cardMatchesBuffers) {
                cardMatchesBuffers.add(buffer);
            }
        }

        @Override
        void execute() throws Exception {
            super.execute();
            timingTotalWarpTime += timingWarpTime;
            timingTotalNormalizeTime += timingNormalizeTime;
            timingTotalMatchTime += timingMatchTime;
            if (matchSync.decrementAndGet() == 0) {

                startFrameRecognition();

                sortedCardMatches.clear();
                for (CardMatch match : cardMatches) {
                    if (match != null) {
                        sortedCardMatches.add(match);
                    }
                }

                Collections.sort(sortedCardMatches, CardMatch.MATCH_SCORE_COMPARATOR);

                synchronized (collectedCardMatches) {
                    collectedCardMatches.clear();
                    matchIntersect:
                    for (CardMatch match : sortedCardMatches) {
                        for (CardMatch goodMatch : collectedCardMatches) {
                            if (goodMatch.isIntersects(match)) {
                                continue matchIntersect;
                            }
                        }
                        collectedCardMatches.add(match);
                    }

                    List<Card> cards = new ArrayList<Card>(mainContext.state.player.cards);

                    for (CardMatch match : collectedCardMatches) {

                        Card card = mainContext.cardInfo.cards[match.cardNumber];
                        if (!cards.contains(card)) {
                            if (card.gamblingWorld) {
                                // need to remove another "Gambling World" card
                                for (int i = 0 ; i < cards.size() ; i++) {
                                    if (cards.get(i).gamblingWorld) {
                                        cards.remove(i);
                                        break;
                                    }
                                }
                            }
                            cards.add(card);
                        }
                    }

                    mainContext.state.player.cards = cards;
                    mainContext.state.player.resetScoring();
                }
                mainContext.userInterface.postInvalidate();
            }
        }
    }


    private void initTasks() {
        int xOrigin = frameSize.width / 2;
        int yOrigin = frameSize.height / 2;

        calcHoughLeft = new HoughTask(sobel, frameSize.width, frameSize.height, false, MASK_LEFT, yOrigin, MAX_GAP_LEFT, MIN_LENGTH_LEFT, linesLeft);
        calcHoughRight = new HoughTask(sobel, frameSize.width, frameSize.height, false, MASK_RIGHT, yOrigin, MAX_GAP, MIN_LENGTH, linesRight);
        calcHoughTop = new HoughTask(sobelTransposed, frameSize.height, frameSize.width, true, MASK_TOP, xOrigin, MAX_GAP, MIN_LENGTH, linesTop);
        calcHoughBottom = new HoughTask(sobelTransposed, frameSize.height, frameSize.width, true, MASK_BOTTOM, xOrigin, MAX_GAP, MIN_LENGTH, linesBottom);

        extractRect = new RectExtractorTask();
    }


    void startFrameRecognition() {
        mainContext.executor.submit(copyFrame);
    }

/*
    void release() {
        rgb.release();
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
*/
}
