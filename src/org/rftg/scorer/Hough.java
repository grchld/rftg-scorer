package org.rftg.scorer;

/**
* @author gc
*/
class Hough extends RecognizerTask {

    private static final int MAX_LINES = 1000;
    private static final int MAX_BASE_GAP = 2;

    /*
    private RecognizerResources recognizerResources;
    private Mat sobel;
    private boolean transposed;
    private int mask;
    private int origin;
    private int maxGap;
    private int minLength;
    private Mat segmentsStack;
    private List<Line> lines;
    private short[] segmentData = new short[4];
    private Segment[] segmentsBuffer = new Segment[MAX_LINES];

    Hough(RecognizerResources recognizerResources, Mat sobel, boolean transposed, int mask, int origin, int maxGap, int minLength, List<Line> lines) {
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

    void release() {
        segmentsStack.release();
    }

    @Override
    public void run() {

        int segmentCount = recognizerResources.customNativeTools.houghVertical(sobel, mask, origin, maxGap, minLength, segmentsStack);

        for (int i = 0 ; i < segmentCount ; i++) {
            segmentsStack.get(0, i, segmentData);
            segmentsBuffer[i] = new Segment(origin, segmentData[0], segmentData[1], segmentData[2], segmentData[3], 0);
        }

        group(segmentCount);
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

  */
    @Override
    void execute() throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
