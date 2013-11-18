package org.rftg.scorer;

/**
* @author gc
*/
class Rect {
    final Point origin;
    final Point text;
    final Size size;

    Rect(Point origin, Size size) {
        this.origin = origin;
        this.size = size;
        this.text = new Point(origin.x + size.width / 2, origin.y + 39 * size.height / 56);
    }
}
