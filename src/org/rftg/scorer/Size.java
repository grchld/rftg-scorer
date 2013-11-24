package org.rftg.scorer;

/**
 * @author gc
 */
public class Size implements Comparable<Size> {
    public final int width;
    public final int height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Size scaleIn(Size inner) {
        if (equals(inner)) {
            return inner;
        }
        if (inner.width <= 0 || inner.height <= 0) {
            return this;
        }
        int wih = width * inner.height;
        int hiw = height * inner.width;
        if (wih < hiw) {
            return new Size(width, wih / inner.width);
        } else if (wih > hiw) {
            return new Size(hiw / inner.height, height);
        } else {
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Size size = (Size) o;

        if (height != size.height) return false;
        if (width != size.width) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = width;
        result = 31 * result + height;
        return result;
    }

    @Override
    public int compareTo(Size size) {
        return (size.width == width) ? (size.width - width) : (size.height - height);
    }

    @Override
    public String toString() {
        return "" + width + "x" + height;
    }
}
