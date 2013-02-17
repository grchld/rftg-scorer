package org.rftg.scorer;

import android.content.Context;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author gc
 */
class Recognizer {

    private static int MAX_LINES = 1000;

    private static double MIN_RATIO = (7./5.)/1.2;
    private static double MAX_RATIO = (7./5.)*1.2;

    private static double ANGLE_BOUND = 0.3;

    private Mat real;
    private Mat gray;
    private Mat canny;

    private Mat result;

    private int counter;

    private double minX;
    private double minY;
    private double maxX;
    private double maxY;

    List<Line> horizontal = new ArrayList<Line>(MAX_LINES);
    List<Line> vertical = new ArrayList<Line>(MAX_LINES);

    int tempRectCounter;


    Recognizer(Context context, int width, int height) {
        Mat tempReal;
        try {

            tempReal = Utils.loadResource(context, R.drawable.real);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        real = new Mat(tempReal.rows(), tempReal.cols(), CvType.CV_8UC4);

        Imgproc.cvtColor(tempReal, real, Imgproc.COLOR_BGR2RGBA);

        tempReal.release();

        gray = new Mat(height, width, CvType.CV_8UC1);

        canny = new Mat(height, width, CvType.CV_8UC1);

        result = new Mat(height, width, CvType.CV_8UC4);

        maxX = width / 2;
        minX = maxX / 5;

        maxY = height / 1.5;
        minY = maxY / 5;
    }

    void release() {
        real.release();
        gray.release();
        canny.release();
    }

    Mat onFrame(Mat inputFrame) {
        /**/
        Mat sub = inputFrame.submat(0,real.rows(),0,real.cols());
        real.copyTo(sub);
        sub.release();
        /**/

        Imgproc.cvtColor(inputFrame, gray, Imgproc.COLOR_BGR2GRAY);

        Imgproc.Canny(gray, canny, 80, 100);

        Mat lines = new Mat();

        Imgproc.HoughLinesP(canny, lines, 1, 3.14159 * 2 / 180, 50, 40, 5);

        int lineCount = lines.cols();

        if (lineCount < 4 || lineCount > 1000) {
            return inputFrame;
        }


//        Imgproc.HoughLines(canny, lines, 4, 3.14159 * 4 / 180, 100/*, 2, 200*/);

//        Imgproc.cvtColor(mIntermediateMat, sub, Imgproc.COLOR_GRAY2RGBA, 4);


//        Mat sub = mRgba.submat(0, real.rows(), 0, real.cols());

////        Mat lines = new Mat();

////        Imgproc.HoughLinesP(mIntermediateMat, lines, 1, 3.14159 / 180, 50, 50, 5);
//        Imgproc.HoughLines(mIntermediateMat, lines, 1, 3.14159 / 180, 100/*, 50, 5*/);

//        Imgproc.cvtColor(canny, result, Imgproc.COLOR_GRAY2RGBA, 4);




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

                        for (int horizontal2 = horizontal1+1 ; horizontal2 < horizontalSize ; horizontal2++) {

                            Line horizontalLine2 = horizontal.get(horizontal2);

                            if (horizontalLine2.mx > rightLine.mx) {
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

                            ///////////
                            if (leftLine.mx > rightLine.mx || rightLine.mx - leftLine.mx < minX || rightLine.mx - leftLine.mx > maxX) {
                                throw new RuntimeException();
                            }
                            if (leftLine.mx > upperLine.mx || rightLine.mx < upperLine.mx || leftLine.mx > lowerLine.mx || rightLine.mx < lowerLine.mx) {
                                throw new RuntimeException();
                            }

                            if (upperLine.my > lowerLine.my || lowerLine.my - upperLine.my < minY || lowerLine.my - upperLine.my > maxY) {
                                throw new RuntimeException();
                            }
                            if (upperLine.my > leftLine.my || lowerLine.my < leftLine.my || upperLine.my > rightLine.my || lowerLine.my < rightLine.my) {
                                throw new RuntimeException();
                            }
                            ////////////
                            ////
                            /*
                            if (rectCounter == tempRectCounter) {
                                tempRectCounter++;
                                Core.line(result, new Point(upperLine.x1, upperLine.y1), new Point(upperLine.x2, upperLine.y2), new Scalar(255,255,255), 3);
                                Core.line(result, new Point(lowerLine.x1, lowerLine.y1), new Point(lowerLine.x2, lowerLine.y2), new Scalar(255,255,255), 3);
                                Core.line(result, new Point(rightLine.x1, rightLine.y1), new Point(rightLine.x2, rightLine.y2), new Scalar(255,255,255), 3);
                                Core.line(result, new Point(leftLine.x1, leftLine.y1), new Point(leftLine.x2, leftLine.y2), new Scalar(255,255,255), 3);
                                break leftBoundLoop;
                            }
                                     */
                            rectCounter++;


                        }

                    }

                }

            }
        }



        Core.putText(result, "" + rectCounter, new Point(100, 200), 1, 1, new Scalar(255,255,255));

        return result;
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

            horizontal = Math.abs(dx) > Math.abs(dy);
            if (horizontal) {
                tan = dy / dx;
            } else {
                tan = dx / dy;
            }
        }

    }

}
