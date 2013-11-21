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
        this.text = origin == null ? null : new Point(origin.x + size.width / 2, origin.y + 39 * size.height / 56);
    }

    boolean contains(Point point) {
        return point.x >= origin.x && point.y >= origin.y &&
                point.x < origin.x + size.width && point.y < origin.y + size.height;
    }

    android.graphics.Rect toAndroidRect() {
        return new android.graphics.Rect(origin.x, origin.y, origin.x + size.width, origin.y + size.height);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rect rect = (Rect) o;

        if (origin != null ? !origin.equals(rect.origin) : rect.origin != null) return false;
        if (size != null ? !size.equals(rect.size) : rect.size != null) return false;
        if (text != null ? !text.equals(rect.text) : rect.text != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = origin != null ? origin.hashCode() : 0;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (size != null ? size.hashCode() : 0);
        return result;
    }
}
