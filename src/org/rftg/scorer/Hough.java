package org.rftg.scorer;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Collections;
import java.util.List;

/**
* @author gc
*/
class Hough extends RecognizerTask {

    static final int MAX_LINES = 1000;
    private static final int MAX_BASE_GAP = 2;

    private static final int SEGMENT_STRUCT_SIZE = 8;
    private static final int SEGMENT_STATUS_STRUCT_SIZE = 4;
    // #define SLOPE_COUNT (MAX_SLOPE - MIN_SLOPE + 1)
    private static final int SEGMENT_STATUSES_PER_COLUMN = 32;

    /*
    private RecognizerResources recognizerResources;
    */
    private final ByteBuffer sobel;
    private final int width;
    private final int height;
    private final Buffer segmentStates;
    private final ByteBuffer segments = ByteBuffer.allocateDirect(MAX_LINES * SEGMENT_STRUCT_SIZE);
    private final byte[] segmentsBytes = new byte[MAX_LINES * SEGMENT_STRUCT_SIZE];
/*    private final ByteBuffer segmentsBytes = ByteBuffer.allocate(MAX_LINES * SEGMENT_STRUCT_SIZE);
    private final ShortBuffer segments = segmentsBytes.order(ByteOrder.nativeOrder()).asShortBuffer();*/
    private final boolean transposed;
    private final int mask;
    private final int origin;
    private final int maxGap;
    private final int minLength;
    private final List<Line> lines;

    long timingHoughTime;
    long timingGroupTime;
    long timingSegmentTime;

//    private Mat segmentsStack;
    private final Segment[] segmentsBuffer = new Segment[MAX_LINES];

    Hough(ByteBuffer sobel, int width, int height, boolean transposed, int mask, int origin, int maxGap, int minLength, List<Line> lines) {
        this.sobel = sobel;
        this.width = width;
        this.height = height;
        this.segmentStates = ByteBuffer.allocateDirect(SEGMENT_STATUS_STRUCT_SIZE * SEGMENT_STATUSES_PER_COLUMN * width);
        this.transposed = transposed;
        this.mask = mask;
        this.origin = origin;
        this.maxGap = maxGap;
        this.minLength = minLength;
        this.lines = lines;
    }
/*    Hough(ByteBuffer sobel, ByteBuffer segmentStates, boolean transposed, int mask, int origin, int maxGap, int minLength) {

    }

    /*RecognizerResources recognizerResources, Mat sobel, boolean transposed, int mask, int origin, int maxGap, int minLength, List<Line> lines) {
        this.recognizerResources = recognizerResources;
        this.sobel = sobel;
        this.transposed = transposed;
        this.mask = mask;
        this.origin = origin;
        this.maxGap = maxGap;
        this.minLength = minLength;
        this.lines = lines;
        this.segmentsStack = new Mat(1, MAX_LINES, CvType.CV_16SC4);
    }
*/

    /*
    // segments: short miny, short maxy, short x, short angle
    // returns actual segment count
    static native int houghVertical(ByteBuffer image, int width, int height, int bordermask, int origin, int maxGap, int minLength, ByteBuffer segments, int maxSegments, ByteBuffer segmentStates);
    */

    @Override
    void execute() throws Exception {
        long time = System.nanoTime();
        int segmentCount = NativeTools.houghVertical(sobel, width, height, mask, origin, maxGap, minLength, segments, MAX_LINES, segmentStates);

        timingHoughTime = System.nanoTime() - time;

        segments.position(0);
        segments.get(segmentsBytes);
        time = System.nanoTime();
        int j = 0;
        for (int i = 0 ; i < segmentCount ; i++) {
            segmentsBuffer[i] = new Segment(origin,
                    ((int)segmentsBytes[j++]) & 0xff | ((int)segmentsBytes[j++]) << 8,
                    ((int)segmentsBytes[j++]) & 0xff | ((int)segmentsBytes[j++]) << 8,
                    ((int)segmentsBytes[j++]) & 0xff | ((int)segmentsBytes[j++]) << 8,
                    ((int)segmentsBytes[j++]) & 0xff | ((int)segmentsBytes[j++]) << 8, 0);
        }
        timingSegmentTime = System.nanoTime() - time;

        time = System.nanoTime();
        group(segmentCount);
        timingGroupTime = System.nanoTime() - time;
    }

    private void group(int size) {

        int groups = 0;

        for (int i = 0 ; i < size ; i++) {
            Segment base = segmentsBuffer[i];
            if (base != null) {
                int y1 = base.y1;
                int y2 = base.y2;
                int slope = base.slope;
                int xbaseMin = base.xbase;
                int xbaseMax = base.xbase;
                int length = y2 - y1;

                for (int j = i+1 ; j < size ; j++) {
                    Segment s = segmentsBuffer[j];
                    if (s == null) {
                        continue;
                    }
                    if (s.slope != slope || s.xbase > xbaseMax + MAX_BASE_GAP) {
                        break;
                    }

                    if (s.y1 > y2 || s.y2 < y1) {
                        continue;
                    }
                    segmentsBuffer[j] = null;
                    xbaseMax = s.xbase;
                    if (y1 > s.y1) {
                        y1 = s.y1;
                    }
                    if (y2 < s.y2) {
                        y2 = s.y2;
                    }
                    int l = s.y2 - s.y1;
                    if (l > length) {
                        length = l;
                    }
                }
                segmentsBuffer[groups++] = new Segment(origin, y1, y2, (xbaseMin + xbaseMax)/2, slope, length);
            }
        }

        lines.clear();

        int selections = 0;
        nextbase:
        for (int i = 0 ; i < groups ; i++) {
            Segment segment = segmentsBuffer[i];
            for (int j = 0 ; j < selections ; j++) {
                Segment base = segmentsBuffer[j];
                if (base.closeEnough(segment)) {
                    if (segment.length > base.length) {
                        segmentsBuffer[j] = segment;
                    }
                    continue nextbase;
                }
            }
            segmentsBuffer[selections++] = segment;
        }

        for (int i = 0 ; i < selections ; i++) {
            Segment segment = segmentsBuffer[i];
            lines.add(transposed
                    ? new Line(segment.y1, segment.x1, segment.y2, segment.x2, segment.slope)
                    : new Line(segment.x1, segment.y1, segment.x2, segment.y2, segment.slope)
            );
        }

        Collections.sort(lines, Line.MX_COMPARATOR);
    }

}
