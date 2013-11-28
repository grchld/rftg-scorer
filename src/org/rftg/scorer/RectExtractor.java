package org.rftg.scorer;

import java.util.List;

/**
 * @author gc
 */
public class RectExtractor extends RecognizerTask {

    private static final int MAX_RECTANGLES = 400;
    private static final int MAX_RECTANGLES_TO_USE_OUTERS = 100;

    private static final double RECT_ASPECT = 1.4;
    private static final double RECT_MIN_ASPECT = RECT_ASPECT/1.25;
    private static final double RECT_MAX_ASPECT = RECT_ASPECT*1.25;

    private static final int RECT_SLOPE_BOUND = 5;

    private static final double RECT_MIN_LINE_LENGTH_PERCENT = 35;

    private final List<Point[]> rectangles;
    private final List<Line> linesLeft;
    private final List<Line> linesRight;
    private final List<Line> linesTop;
    private final List<Line> linesBottom;

    private final Size frameSize;

    private final int minX;
    private final int minY;
    private final int maxX;
    private final int maxY;

    public RectExtractor(List<Point[]> rectangles, List<Line> linesLeft, List<Line> linesRight, List<Line> linesTop, List<Line> linesBottom, Size frameSize) {
        this.rectangles = rectangles;
        this.linesLeft = linesLeft;
        this.linesRight = linesRight;
        this.linesTop = linesTop;
        this.linesBottom = linesBottom;
        this.frameSize = frameSize;

        minX = 50;
        minY = (int)(minX * RECT_ASPECT);

        maxY = (int)(frameSize.height / 1.1);
        maxX = (int)(maxY / RECT_ASPECT);

    }

    @Override
    void execute() throws Exception {
        rectangles.clear();
        extractRectangles(false);
        if (rectangles.size() <= MAX_RECTANGLES_TO_USE_OUTERS) {
            extractRectangles(true);
        }
    }

    private void extractRectangles(boolean outer) {

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
                            Point np1 = new Point((int)(p1.x + (p2.x - p1.x) * CardPatterns.CARD_VERTICAL_BORDER), (int)(p1.y + (p4.y - p1.y) * CardPatterns.CARD_HORIZONTAL_BORDER));
                            Point np2 = new Point((int)(p2.x - (p2.x - p1.x) * CardPatterns.CARD_VERTICAL_BORDER), (int)(p2.y + (p3.y - p2.y) * CardPatterns.CARD_HORIZONTAL_BORDER));
                            Point np3 = new Point((int)(p3.x - (p3.x - p4.x) * CardPatterns.CARD_VERTICAL_BORDER), (int)(p3.y - (p3.y - p2.y) * CardPatterns.CARD_HORIZONTAL_BORDER));
                            Point np4 = new Point((int)(p4.x + (p3.x - p4.x) * CardPatterns.CARD_VERTICAL_BORDER), (int)(p4.y - (p4.y - p1.y) * CardPatterns.CARD_HORIZONTAL_BORDER));

                            p1 = np1;
                            p2 = np2;
                            p3 = np3;
                            p4 = np4;
                        }


                        if (frameSize.contains(p1) && frameSize.contains(p2) && frameSize.contains(p3) && frameSize.contains(p4))
                        {
                            rectangles.add(new Point[]{p1, p2, p3, p4});
                            if (rectangles.size() >= MAX_RECTANGLES) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

}
