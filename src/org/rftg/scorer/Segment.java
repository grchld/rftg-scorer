package org.rftg.scorer;

/**
* @author gc
*/
class Segment {

    private static final int MAX_BASE_DISTANCE = 10;
    private static final int LEAST_BASE_DISTANCE = 20;

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

    boolean closeEnough(Segment segment) {
        int baseDist = xbase - segment.xbase;
        if (baseDist > LEAST_BASE_DISTANCE || baseDist < -LEAST_BASE_DISTANCE || y1 > segment.y2 || segment.y1 > y2) {
            return false;
        }

        int a1;
        int a2;
        if (y1 < segment.y1) {
            a1 = calcX(segment.y1);
            a2 = segment.x1;
        } else if (y1 > segment.y1) {
            a1 = x1;
            a2 = segment.calcX(y1);
        } else {
            a1 = x1;
            a2 = segment.x1;
        }
        int a = a1 - a2;
        if (a > MAX_BASE_DISTANCE || a < -MAX_BASE_DISTANCE) {
            return false;
        }

        int b1;
        int b2;
        if (y2 > segment.y2) {
            b1 = calcX(segment.y2);
            b2 = segment.x2;
        } else if (y2 < segment.y2) {
            b1 = x2;
            b2 = segment.calcX(y2);
        } else {
            b1 = x2;
            b2 = segment.x2;
        }
        int b = b1 - b2;
        if (b > MAX_BASE_DISTANCE || b < -MAX_BASE_DISTANCE) {
            return false;
        }

        return true;
    }

}
