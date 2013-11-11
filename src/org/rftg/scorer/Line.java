package org.rftg.scorer;

import java.util.Comparator;

/**
* @author gc
*/
class Line {

    final static Comparator<Line> MX_COMPARATOR = new Comparator<Line>() {
        @Override
        public int compare(Line line1, Line line2) {
            int r = line1.mx - line2.mx;
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

    Point intersect(Line line) {

        int divisor = line.dx * dy - dx * line.dy;
        int skew = divisor / 2;

        int x = (line.cross * dx - cross * line.dx + skew) / divisor;
        int y = (line.cross * dy - cross * line.dy + skew) / divisor;

        return new Point(x,y);
    }


    @Override
    public String toString() {
        return "" + mx + ":" + my + ":" + slope;
    }
}
