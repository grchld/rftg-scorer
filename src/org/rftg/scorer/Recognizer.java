package org.rftg.scorer;

import android.content.Context;
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

    private static int MAX_LINES = 500;

    private static double MIN_RATIO = (7./5.)/1.2;
    private static double MAX_RATIO = (7./5.)*1.2;
    private static double PARALLEL_ANGLE_BOUND = 0.1;

    private static double ANGLE_BOUND = 0.2;

    final MainActivity main;

    private Mat real;
    private Mat gray;
    private Mat canny;

//    private Mat result;

    private int counter;

    private double minX;
    private double minY;
    private double maxX;
    private double maxY;

    private TreeMap<Double, MatOfPoint> tempRects = new TreeMap<Double, MatOfPoint>();

    List<Line> horizontal = new ArrayList<Line>(MAX_LINES);
    List<Line> vertical = new ArrayList<Line>(MAX_LINES);


    Recognizer(MainActivity main, int width, int height) {
        this.main = main;
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
    }

    Mat onFrame(Mat frame) {
        /**/
        Mat sub = frame.submat(0,real.rows(),0,real.cols());
//        real.copyTo(sub);
        sub.release();
        /**/
        tempRects.clear();
        /**/

        long time = System.currentTimeMillis();
        //new Normalizer().normalize(frame);
        long x = main.customNativeTools.normalize(frame, Normalizer.NORMALIZE_THRESHOLD, Normalizer.NORMALIZE_THRESHOLD);
        Log.i("rftg", "Normalize4: " + x + " " + (System.currentTimeMillis() - time));
/*
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

        Imgproc.Canny(gray, canny, 80, 100);

        Imgproc.cvtColor(gray, frame, Imgproc.COLOR_GRAY2RGBA);
  */
/*
        Mat lines = new Mat();

        Imgproc.HoughLinesP(canny, lines, 1, 3.14159 * 2 / 180, 40, 40, 5);

        int lineCount = lines.cols();

        if (lineCount < 4 || lineCount > MAX_LINES) {
            return inputFrame;
        }

        inputFrame.copyTo(result);

        Scalar red = new Scalar(255, 0, 0);

        Core.putText(result, ""+(counter++), new Point(100, 100), 1, 1, red);

        horizontal.clear();
        vertical.clear();

        for (int i = 0 ; i < lineCount ; i++) {
            Line line = new Line(lines.get(0, i));

            if (-ANGLE_BOUND < line.tan && line.tan < ANGLE_BOUND) {
                if (line.horizontal) {
                    horizontal.add(line);
                } else {
                    vertical.add(line);
                }
            }
        }
        lines.release();

        if (horizontal.size() < 2 || vertical.size() < 2) {
            return inputFrame;
        }

        Scalar green = new Scalar(0, 255, 0);
        for (Line line : horizontal) {
            Core.line(result, new Point(line.x1, line.y1), new Point(line.x2, line.y2), green, 3);
        }

        Scalar blue = new Scalar(0, 0, 255);
        for (Line line : vertical) {
            Core.line(result, new Point(line.x1, line.y1), new Point(line.x2, line.y2), blue, 3);
        }

        int rectCounter = 0;
        int sameRectCounter = 0;

        Collections.sort(vertical, Line.MX_COMPARATOR);
        Collections.sort(horizontal, Line.MX_COMPARATOR);

        int leftHorizontalBound = 0;
        int horizontalSize = horizontal.size();
        int horizontalSizeMinusOne = horizontal.size() - 1;

        int leftUpperBound = vertical.size() - 1;
        int rightUpperBound = vertical.size();
        int rightLowerBound = 1;
        leftBoundLoop:
        for (int left = 0 ; left < leftUpperBound ; left++) {
            Line leftLine = vertical.get(left);
            double lowerBound = leftLine.mx + minX;
            while (vertical.get(rightLowerBound).mx < lowerBound) {
                rightLowerBound++;
                if (rightLowerBound >= rightUpperBound) {
                    break leftBoundLoop;
                }
            }

            while (horizontal.get(leftHorizontalBound).mx < leftLine.mx) {
                leftHorizontalBound++;
                if (leftHorizontalBound >= horizontalSizeMinusOne) {
                    break leftBoundLoop;
                }
            }

            double upperBound = leftLine.mx + maxX;
            for (int right = rightLowerBound; right < rightUpperBound ; right++) {
                Line rightLine = vertical.get(right);

                if (rightLine.mx > upperBound) {
                    break;
                }

                if (Math.abs(leftLine.tan - rightLine.tan) > PARALLEL_ANGLE_BOUND) {
                    break;
                }

                if (Math.abs(leftLine.my-rightLine.my) <= maxY ) {

                    for (int horizontal1 = leftHorizontalBound ; horizontal1 < horizontalSizeMinusOne ; horizontal1++) {

                        Line horizontalLine1 = horizontal.get(horizontal1);
                        if (horizontalLine1.mx > rightLine.mx) {
                            break;
                        }

                        boolean firstIsUpper;
                        if (horizontalLine1.my < leftLine.my) {
                             if (horizontalLine1.my < rightLine.my) {
                                firstIsUpper = true;
                             } else {
                                 continue;
                             }
                        } else {
                            if (horizontalLine1.my > rightLine.my) {
                                firstIsUpper = false;
                            } else {
                                continue;
                            }
                        }

                        nextHorizontal2:
                        for (int horizontal2 = horizontal1+1 ; horizontal2 < horizontalSize ; horizontal2++) {

                            Line horizontalLine2 = horizontal.get(horizontal2);

                            if (horizontalLine2.mx > rightLine.mx) {
                                break;
                            }

                            if (Math.abs(horizontalLine1.tan - horizontalLine2.tan) > PARALLEL_ANGLE_BOUND) {
                                break;
                            }

                            Line upperLine;
                            Line lowerLine;
                            if (firstIsUpper) {
                                if (horizontalLine2.my < leftLine.my || horizontalLine2.my < rightLine.my) {
                                    continue;
                                }
                                upperLine = horizontalLine1;
                                lowerLine = horizontalLine2;
                            } else {
                                if (horizontalLine2.my > leftLine.my || horizontalLine2.my > rightLine.my) {
                                    continue;
                                }
                                upperLine = horizontalLine2;
                                lowerLine = horizontalLine1;
                            }

                            double dy = lowerLine.my - upperLine.my;
                            if (dy < minY || dy > maxY) {
                                continue;
                            }

                            double ratio = dy / (rightLine.mx - leftLine.mx);

                            if (ratio < MIN_RATIO || ratio > MAX_RATIO) {
                                continue;
                            }

                            Point p1 = intersect(leftLine, upperLine);
                            Point p2 = intersect(rightLine, upperLine);
                            Point p3 = intersect(rightLine, lowerLine);
                            Point p4 = intersect(leftLine, lowerLine);


                            MatOfPoint rect = new MatOfPoint(p1, p2, p3, p4);

                            Point[] rectPoints = rect.toArray();

                            double delta = (p2.x - p1.x) / 10;

                            nextRect: for (MatOfPoint r : tempRects.subMap(p1.x - delta, true, p1.x + delta, true).values()) {
                                Point[] p = r.toArray();
                                for (int i = 0 ; i < 4 ; i++) {
                                    if (Math.abs(rectPoints[i].x - p[i].x) > 5) {
                                        continue nextRect;
                                    }
                                    if (Math.abs(rectPoints[i].y - p[i].y) > 5) {
                                        continue nextRect;
                                    }
                                }
                                sameRectCounter++;


                                continue nextHorizontal2;
                            }

                            tempRects.put(p1.x, rect);

                            rectCounter++;


                        }

                    }

                }

            }
        }


        Core.polylines(result, new ArrayList<MatOfPoint>(tempRects.values()), true, new Scalar(255,255,255));

        Core.putText(result, "" + rectCounter, new Point(100, 200), 1, 1, new Scalar(255,255,255));
        Core.putText(result, "" + sameRectCounter, new Point(100, 250), 1, 1, new Scalar(0,255,255));
  */
        return frame;
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
