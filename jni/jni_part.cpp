#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>

#if HAVE_NEON == 1
#include <arm_neon.h>
#endif

using namespace std;
using namespace cv;

extern "C" {
JNIEXPORT jlong JNICALL Java_org_rftg_scorer_CustomNativeTools_normalize(JNIEnv*, jobject, jlong addrImage, jdouble lowerPercent, jdouble upperPercent);

JNIEXPORT jlong JNICALL Java_org_rftg_scorer_CustomNativeTools_normalize(JNIEnv*, jobject, jlong addrImage, jdouble lowerPercent, jdouble upperPercent)
{
    Mat& image = *(Mat*)addrImage;
    const int channels = image.channels();

    int hist[channels*256];
    memset(hist, 0, channels*256*sizeof(int));

    if (channels == 4) {

        int* h1 = &(hist[0]);
        int* h2 = &(hist[256]);
        int* h3 = &(hist[256*2]);
        int* h4 = &(hist[256*3]);

        for (int i = 0; i < image.rows ; i++) {
            unsigned int* row = image.ptr<unsigned int>(i);
            for (int j = 0; j < image.cols ; j++ ) {
                unsigned int v = row[j];
                h1[v & 0xff]++;
                h2[(v >> 8) & 0xff]++;
                h3[(v >> 16) & 0xff]++;
                h4[v >> 24]++;
            }
        }

    } else {
        int* s = &(hist[0]);
        int* h = s;
        int* e = s + channels*256;

        for (int i = 0; i < image.rows ; i++) {
            unsigned char* row = image.ptr<unsigned char>(i);
            for (int j = 0; j < image.cols*channels ; j++ ) {
                h[*(row++)]++;
                h += 256;
                if (h == e) {
                    h = s;
                }
            }
        }
    }

    return hist[channels*256-1];
}

JNIEXPORT void JNICALL Java_org_rftg_scorer_CustomNativeTools_sobel(JNIEnv*, jobject, jlong srcAddr, jlong dstAddr, jint bound);

JNIEXPORT void JNICALL Java_org_rftg_scorer_CustomNativeTools_sobel(JNIEnv*, jobject, jlong srcAddr, jlong dstAddr, jint bound)
{
    Mat& src = *(Mat*)srcAddr;
    Mat& dst = *(Mat*)dstAddr;

    CV_Assert(src.depth() == CV_8U);
    CV_Assert(dst.depth() == CV_8U);
    CV_Assert(src.rows == dst.rows);
    CV_Assert(src.cols == dst.cols);
    CV_Assert(src.cols == dst.cols);
    CV_Assert(src.channels() == dst.channels());
    CV_Assert(src.channels() == 1);

    const int rows = src.rows;
    const int cols = src.cols;

    memset(dst.ptr<uchar>(0), 0, cols);
    memset(dst.ptr<uchar>(rows-1), 0, cols);


    for (int i = 1 ; i < rows-1 ; i++) {

#if HAVE_NEON == 1

        uint8x8_t* drow = dst.ptr<uint8x8_t>(i);

        int* idrow = (int*)drow;
        idrow[0] = 0;
        idrow[1] = 0;
        idrow[cols-1] = 0;
        idrow[cols-2] = 0;


        uint8x8_t* prow = src.ptr<uint8x8_t>(i-1);
        uint8x8_t* crow = src.ptr<uint8x8_t>(i);
        uint8x8_t* nrow = src.ptr<uint8x8_t>(i+1);


        uint8x8_t p0 = prow[0];
        uint8x8_t c0 = crow[0];
        uint8x8_t n0 = nrow[0];

        uint8x8_t p1 = prow[1];
        uint8x8_t c1 = crow[1];
        uint8x8_t n1 = nrow[1];

        int16x8_t lower = vdupq_n_s16(-bound);
        int16x8_t upper = vdupq_n_s16(bound);

        uint8x8_t hlightmask = vdup_n_u8(0x80);
        uint8x8_t hdarkmask = vdup_n_u8(0x40);
        uint8x8_t vlightmask = vdup_n_u8(0x20);
        uint8x8_t vdarkmask = vdup_n_u8(0x10);

        for (int j = 2 ; j < cols/8; j++) {

            uint8x8_t p2 = prow[j];
            uint8x8_t c2 = crow[j];
            uint8x8_t n2 = nrow[j];

            int16x8_t px = (int16x8_t)vmovl_u8(vext_u8(p0, p1, 7));
            int16x8_t py = (int16x8_t)vmovl_u8(vext_u8(p1, p2, 1));

            int16x8_t cx = (int16x8_t)vmovl_u8(vext_u8(c0, c1, 7));
            int16x8_t cy = (int16x8_t)vmovl_u8(vext_u8(c1, c2, 1));

            int16x8_t nx = (int16x8_t)vmovl_u8(vext_u8(n0, n1, 7));
            int16x8_t ny = (int16x8_t)vmovl_u8(vext_u8(n1, n2, 1));

            int16x8_t pz = (int16x8_t)vmovl_u8(p1);
            int16x8_t nz = (int16x8_t)vmovl_u8(n1);

            int16x8_t d1 = vsubq_s16(ny, px);
            int16x8_t d2 = vsubq_s16(nx, py);

            // nx+2*n1+ny - (px+2*p1+py)
            int16x8_t horizontal = vaddq_s16(vshlq_n_s16(vsubq_s16(nz, pz), 1), vaddq_s16(d1,d2));

            // ny + 2*cy + py - (nx + 2*cx + px)
            int16x8_t vertical = vaddq_s16(vshlq_n_s16(vsubq_s16(cy, cx), 1), vsubq_s16(d1,d2));

            uint8x8_t hdark = vqmovn_u16(vcleq_s16(horizontal, lower));
            uint8x8_t hlight = vqmovn_u16(vcgeq_s16(horizontal, upper));

            uint8x8_t vdark = vqmovn_u16(vcleq_s16(vertical, lower));
            uint8x8_t vlight = vqmovn_u16(vcgeq_s16(vertical, upper));

            hdark = vand_u8(hdark, hdarkmask);
            hlight = vand_u8(hlight, hlightmask);

            vdark = vand_u8(vdark, vdarkmask);
            vlight = vand_u8(vlight, vlightmask);

            drow[j-1] = vorr_u8(vorr_u8(hdark,hlight), vorr_u8(vdark,vlight));

            p0 = p1;
            n0 = n1;
            c0 = c1;

            p1 = p2;
            n1 = n2;
            c1 = c2;

        }


#else


        uchar* prow = src.ptr<uchar>(i-1);
        uchar* crow = src.ptr<uchar>(i);
        uchar* nrow = src.ptr<uchar>(i+1);

        uchar* drow = dst.ptr<uchar>(i);

        drow[0] = 0;
        drow[cols-1] = 0;

        for (int j = 1 ; j < cols-1; j++) {
            int horizontal = (int)prow[j-1] + 2*(int)prow[j] + (int)prow[j+1] - (int)nrow[j-1] - 2*(int)nrow[j] - (int)nrow[j+1];
            int vertical = (int)prow[j-1] + 2*(int)crow[j-1] + (int)nrow[j-1] - (int)prow[j+1] - 2*(int)crow[j+1] - (int)nrow[j+1];

            uchar result;
            if (horizontal >= bound) {
                result = 0x80;
            } else if (horizontal <= -bound) {
                result = 0x40;
            } else {
                result = 0;
            }

            if (vertical >= bound) {
                result |= 0x10;
            } else if (vertical <= -bound) {
                result |= 0x20;
            }
            drow[j] = result;
        }


#endif

    }

}

struct Segment {
    short ymin;
    short ymax;
    short x;
    short slope;
};

struct SegmentState {
    short count;
    short last;
};

#define DIVISOR 64

int segmentCompare(void const *a1, void const* a2) {
    Segment& s1 = *(Segment*)a1;
    Segment& s2 = *(Segment*)a2;
    if (s1.slope < s2.slope || (s1.slope == s2.slope && s1.x < s2.x)) {
        return -1;
    } else {
        return 1;
    }
}

jint houghVerticalUnsorted(jlong imageAddr, jint bordermask, jint origin, jint minSlope, jint maxSlope, jint maxGap, jint minLength, jlong segmentsAddr);

JNIEXPORT jint JNICALL Java_org_rftg_scorer_CustomNativeTools_houghVertical(JNIEnv*, jobject, jlong imageAddr, jint bordermask, jint origin, jint minSlope, jint maxSlope, jint maxGap, jint minLength, jlong segmentsAddr);

JNIEXPORT jint JNICALL Java_org_rftg_scorer_CustomNativeTools_houghVertical(JNIEnv*, jobject, jlong imageAddr, jint bordermask, jint origin, jint minSlope, jint maxSlope, jint maxGap, jint minLength, jlong segmentsAddr) {
    jint segmentNumber = houghVerticalUnsorted(imageAddr, bordermask, origin, minSlope, maxSlope, maxGap, minLength, segmentsAddr);
    if (segmentNumber > 0) {
        Mat& segmentsMat = *(Mat*)segmentsAddr;
        qsort(segmentsMat.ptr<Segment>(0), segmentNumber, sizeof(Segment), segmentCompare);
    }
    return segmentNumber;
}

jint houghVerticalUnsorted(jlong imageAddr, jint bordermask, jint origin, jint minSlope, jint maxSlope, jint maxGap, jint minLength, jlong segmentsAddr) {

    Mat& image = *(Mat*)imageAddr;
    Mat& segmentsMat = *(Mat*)segmentsAddr;

    CV_Assert(image.channels() == 1);
    CV_Assert(image.depth() == CV_8U);

    CV_Assert(segmentsMat.channels() == 4);
    CV_Assert(segmentsMat.depth() == CV_16S);
    CV_Assert(segmentsMat.rows == 1);

    int segmentNumber = 0;
    int maxSegments = segmentsMat.cols;

    Segment* segments = segmentsMat.ptr<Segment>(0);

    int cols = image.cols;
    int rows = image.rows;
    uchar mask = bordermask;
    int slopeCount = maxSlope - minSlope + 1;

    int totalStates = cols * slopeCount;

    cv::AutoBuffer<SegmentState, 32> states(totalStates);
    memset(states, 0, sizeof(SegmentState) * totalStates);

    for (int y = 0; y < rows; y++) {
        uchar* row = image.ptr<uchar>(y);
        for (int x = 0 ; x < cols; x++) {
            uchar value = row[x];
            if (value & mask) {
                for (int slope = minSlope ; slope <= maxSlope ; slope++) {
                    int xbase = x + slope * (origin - y) / DIVISOR;

                    if (xbase < 0 || xbase >= cols) {
                        continue;
                    }

                    SegmentState& state = states[slopeCount * xbase + (slope - minSlope)];

                    if (state.count) {
                        if (y - state.last <= maxGap) {
                            // line continues
                            state.count += y-state.last;
                            state.last = y;
                        } else {
                            // previous line stops
                            if (state.count > minLength) {
                                // save line
                                Segment& segment = segments[segmentNumber];

                                segment.ymin = state.last - state.count + 1;
                                segment.ymax = state.last;
                                segment.x = xbase;
                                segment.slope = slope;

                                if (++segmentNumber == maxSegments) {
                                    // segment stack is full
                                    return maxSegments;
                                }
                            }
                            // staring new line
                            state.count = 1;
                            state.last = y;
                        }
                    } else {
                        // starting new line
                        state.count = 1;
                        state.last = y;
                    }

                }
            }
        }
    }

    // force line endings
    SegmentState* state = states;
    for (int xbase = 0 ; xbase < cols; xbase++) {
        for (int slope = minSlope ; slope <= maxSlope ; slope++) {
            if (state->count > minLength) {
                // save line
                Segment& segment = segments[segmentNumber];

                segment.ymin = state->last - state->count + 1;
                segment.ymax = state->last;
                segment.x = xbase;
                segment.slope = slope;
                if (++segmentNumber == maxSegments) {
                    // segment stack is full
                    return maxSegments;
                }
            }
            state++;
        }
    }

    return segmentNumber;
}

JNIEXPORT void JNICALL Java_org_rftg_scorer_CustomNativeTools_transpose(JNIEnv*, jobject, jlong srcAddr, jlong dstAddr)
{
    Mat& src = *(Mat*)srcAddr;
    Mat& dst = *(Mat*)dstAddr;

    CV_Assert(src.depth() == CV_8U);
    CV_Assert(dst.depth() == CV_8U);
    CV_Assert(src.rows == dst.cols);
    CV_Assert(src.cols == dst.rows);
    CV_Assert(src.channels() == 1);
    CV_Assert(dst.channels() == 1);

    int cols = src.cols;
    int rows = src.rows;

    #if HAVE_NEON == 1

    for (int y = 0 ; y < rows/16; y++) {
        for (int x = 0 ; x < cols/16; x++) {
            uint32x4_t a0 = src.ptr<uint32x4_t>(16*y+ 0)[x];
            uint32x4_t a1 = src.ptr<uint32x4_t>(16*y+ 1)[x];
            uint32x4_t a2 = src.ptr<uint32x4_t>(16*y+ 2)[x];
            uint32x4_t a3 = src.ptr<uint32x4_t>(16*y+ 3)[x];
            uint32x4_t a4 = src.ptr<uint32x4_t>(16*y+ 4)[x];
            uint32x4_t a5 = src.ptr<uint32x4_t>(16*y+ 5)[x];
            uint32x4_t a6 = src.ptr<uint32x4_t>(16*y+ 6)[x];
            uint32x4_t a7 = src.ptr<uint32x4_t>(16*y+ 7)[x];

            uint32x4_t a8 = src.ptr<uint32x4_t>(16*y+ 8)[x];
            uint32x4_t a9 = src.ptr<uint32x4_t>(16*y+ 9)[x];
            uint32x4_t aA = src.ptr<uint32x4_t>(16*y+10)[x];
            uint32x4_t aB = src.ptr<uint32x4_t>(16*y+11)[x];
            uint32x4_t aC = src.ptr<uint32x4_t>(16*y+12)[x];
            uint32x4_t aD = src.ptr<uint32x4_t>(16*y+13)[x];
            uint32x4_t aE = src.ptr<uint32x4_t>(16*y+14)[x];
            uint32x4_t aF = src.ptr<uint32x4_t>(16*y+15)[x];

            uint32x4x2_t b0 = vtrnq_u32(a4, a0);
            uint32x4x2_t b1 = vtrnq_u32(a5, a1);
            uint32x4x2_t b2 = vtrnq_u32(a6, a2);
            uint32x4x2_t b3 = vtrnq_u32(a7, a3);

            uint32x4x2_t b4 = vtrnq_u32(aC, a8);
            uint32x4x2_t b5 = vtrnq_u32(aD, a9);
            uint32x4x2_t b6 = vtrnq_u32(aE, aA);
            uint32x4x2_t b7 = vtrnq_u32(aF, aB);

            
            uint16x8x2_t c0 = vtrnq_u16(vreinterpretq_u16_u32(b2.val[1]/*2*/), vreinterpretq_u16_u32(b0.val[1]/*0*/));
            uint16x8x2_t c1 = vtrnq_u16(vreinterpretq_u16_u32(b3.val[1]/*3*/), vreinterpretq_u16_u32(b1.val[1]/*1*/));

            uint16x8x2_t c2 = vtrnq_u16(vreinterpretq_u16_u32(b2.val[0]/*6*/), vreinterpretq_u16_u32(b0.val[0]/*4*/));
            uint16x8x2_t c3 = vtrnq_u16(vreinterpretq_u16_u32(b3.val[0]/*7*/), vreinterpretq_u16_u32(b1.val[0]/*5*/));

            uint16x8x2_t c4 = vtrnq_u16(vreinterpretq_u16_u32(b6.val[1]/*A*/), vreinterpretq_u16_u32(b4.val[1]/*8*/));
            uint16x8x2_t c5 = vtrnq_u16(vreinterpretq_u16_u32(b7.val[1]/*B*/), vreinterpretq_u16_u32(b5.val[1]/*9*/));

            uint16x8x2_t c6 = vtrnq_u16(vreinterpretq_u16_u32(b6.val[0]/*E*/), vreinterpretq_u16_u32(b4.val[0]/*C*/));
            uint16x8x2_t c7 = vtrnq_u16(vreinterpretq_u16_u32(b7.val[0]/*F*/), vreinterpretq_u16_u32(b5.val[0]/*D*/));

            
            uint8x16x2_t d0 = vtrnq_u8(vreinterpretq_u8_u16(c1.val[1]/*1*/), vreinterpretq_u8_u16(c0.val[1]/*0*/));
            uint8x16x2_t d1 = vtrnq_u8(vreinterpretq_u8_u16(c1.val[0]/*3*/), vreinterpretq_u8_u16(c0.val[0]/*2*/));

            uint8x16x2_t d2 = vtrnq_u8(vreinterpretq_u8_u16(c3.val[1]/*5*/), vreinterpretq_u8_u16(c2.val[1]/*4*/));
            uint8x16x2_t d3 = vtrnq_u8(vreinterpretq_u8_u16(c3.val[0]/*7*/), vreinterpretq_u8_u16(c2.val[0]/*6*/));

            uint8x16x2_t d4 = vtrnq_u8(vreinterpretq_u8_u16(c5.val[1]/*9*/), vreinterpretq_u8_u16(c4.val[1]/*8*/));
            uint8x16x2_t d5 = vtrnq_u8(vreinterpretq_u8_u16(c5.val[0]/*11*/), vreinterpretq_u8_u16(c4.val[0]/*10*/));

            uint8x16x2_t d6 = vtrnq_u8(vreinterpretq_u8_u16(c7.val[1]/*13*/), vreinterpretq_u8_u16(c6.val[1]/*12*/));
            uint8x16x2_t d7 = vtrnq_u8(vreinterpretq_u8_u16(c7.val[0]/*15*/), vreinterpretq_u8_u16(c6.val[0]/*14*/));


            dst.ptr<uint8x8_t>(16*x+ 0)[2*y  ] = vget_low_u8(d0.val[1]);
            dst.ptr<uint8x8_t>(16*x+ 0)[2*y+1] = vget_low_u8(d4.val[1]);
            dst.ptr<uint8x8_t>(16*x+ 1)[2*y  ] = vget_low_u8(d0.val[0]);
            dst.ptr<uint8x8_t>(16*x+ 1)[2*y+1] = vget_low_u8(d4.val[0]);
            dst.ptr<uint8x8_t>(16*x+ 2)[2*y  ] = vget_low_u8(d1.val[1]);
            dst.ptr<uint8x8_t>(16*x+ 2)[2*y+1] = vget_low_u8(d5.val[1]);
            dst.ptr<uint8x8_t>(16*x+ 3)[2*y  ] = vget_low_u8(d1.val[0]);
            dst.ptr<uint8x8_t>(16*x+ 3)[2*y+1] = vget_low_u8(d5.val[0]);
            dst.ptr<uint8x8_t>(16*x+ 4)[2*y  ] = vget_low_u8(d2.val[1]);
            dst.ptr<uint8x8_t>(16*x+ 4)[2*y+1] = vget_low_u8(d6.val[1]);
            dst.ptr<uint8x8_t>(16*x+ 5)[2*y  ] = vget_low_u8(d2.val[0]);
            dst.ptr<uint8x8_t>(16*x+ 5)[2*y+1] = vget_low_u8(d6.val[0]);
            dst.ptr<uint8x8_t>(16*x+ 6)[2*y  ] = vget_low_u8(d3.val[1]);
            dst.ptr<uint8x8_t>(16*x+ 6)[2*y+1] = vget_low_u8(d7.val[1]);
            dst.ptr<uint8x8_t>(16*x+ 7)[2*y  ] = vget_low_u8(d3.val[0]);
            dst.ptr<uint8x8_t>(16*x+ 7)[2*y+1] = vget_low_u8(d7.val[0]);

            dst.ptr<uint8x8_t>(16*x+ 8)[2*y  ] = vget_high_u8(d0.val[1]);
            dst.ptr<uint8x8_t>(16*x+ 8)[2*y+1] = vget_high_u8(d4.val[1]);
            dst.ptr<uint8x8_t>(16*x+ 9)[2*y  ] = vget_high_u8(d0.val[0]);
            dst.ptr<uint8x8_t>(16*x+ 9)[2*y+1] = vget_high_u8(d4.val[0]);
            dst.ptr<uint8x8_t>(16*x+10)[2*y  ] = vget_high_u8(d1.val[1]);
            dst.ptr<uint8x8_t>(16*x+10)[2*y+1] = vget_high_u8(d5.val[1]);
            dst.ptr<uint8x8_t>(16*x+11)[2*y  ] = vget_high_u8(d1.val[0]);
            dst.ptr<uint8x8_t>(16*x+11)[2*y+1] = vget_high_u8(d5.val[0]);
            dst.ptr<uint8x8_t>(16*x+12)[2*y  ] = vget_high_u8(d2.val[1]);
            dst.ptr<uint8x8_t>(16*x+12)[2*y+1] = vget_high_u8(d6.val[1]);
            dst.ptr<uint8x8_t>(16*x+13)[2*y  ] = vget_high_u8(d2.val[0]);
            dst.ptr<uint8x8_t>(16*x+13)[2*y+1] = vget_high_u8(d6.val[0]);
            dst.ptr<uint8x8_t>(16*x+14)[2*y  ] = vget_high_u8(d3.val[1]);
            dst.ptr<uint8x8_t>(16*x+14)[2*y+1] = vget_high_u8(d7.val[1]);
            dst.ptr<uint8x8_t>(16*x+15)[2*y  ] = vget_high_u8(d3.val[0]);
            dst.ptr<uint8x8_t>(16*x+15)[2*y+1] = vget_high_u8(d7.val[0]);



            
        }
    }

    #else

    for (int y = 0 ; y < rows; y++) {
        for (int x = 0 ; x < cols; x++) {
            dst.ptr<uchar>(x)[y] = src.ptr<uchar>(y)[x];
        }
    }

    #endif

}

}
