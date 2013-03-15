package org.rftg.scorer;

/**
* @author gc
*/
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
