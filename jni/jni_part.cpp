#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>
#include <math.h>
#include <android/log.h>

#if HAVE_NEON == 1
#include <arm_neon.h>
#endif

using namespace std;
using namespace cv;

extern "C" {

#define SOBEL_V_LIGHT_BOUND 65
#define SOBEL_V_DARK_BOUND 100
#define SOBEL_H_LIGHT_BOUND 150
#define SOBEL_H_DARK_BOUND 100

JNIEXPORT void JNICALL Java_org_rftg_scorer_CustomNativeTools_sobel(JNIEnv*, jobject, jlong srcAddr, jlong dstAddr)
{
    Mat& src = *(Mat*)srcAddr;
    Mat& dst = *(Mat*)dstAddr;

    CV_DbgAssert(src.depth() == CV_8U);
    CV_DbgAssert(dst.depth() == CV_8U);
    CV_DbgAssert(src.rows == dst.rows);
    CV_DbgAssert(src.cols == dst.cols);
    CV_DbgAssert(src.cols == dst.cols);
    CV_DbgAssert(src.channels() == dst.channels());
    CV_DbgAssert(src.channels() == 1);

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

        int16x8_t hlower = vdupq_n_s16(-SOBEL_H_DARK_BOUND);
        int16x8_t hupper = vdupq_n_s16(SOBEL_H_LIGHT_BOUND);

        int16x8_t vlower = vdupq_n_s16(-SOBEL_V_DARK_BOUND);
        int16x8_t vupper = vdupq_n_s16(SOBEL_V_LIGHT_BOUND);

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

            uint8x8_t hdark = vqmovn_u16(vcleq_s16(horizontal, hlower));
            uint8x8_t hlight = vqmovn_u16(vcgeq_s16(horizontal, hupper));

            uint8x8_t vdark = vqmovn_u16(vcleq_s16(vertical, vlower));
            uint8x8_t vlight = vqmovn_u16(vcgeq_s16(vertical, vupper));

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
            if (horizontal >= SOBEL_H_DARK_BOUND) {
                result = 0x40;
            } else if (horizontal <= -SOBEL_H_LIGHT_BOUND) {
                result = 0x80;
            } else {
                result = 0;
            }

            if (vertical >= SOBEL_V_DARK_BOUND) {
                result |= 0x10;
            } else if (vertical <= -SOBEL_V_LIGHT_BOUND) {
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
    union {
        uint state;
        struct {
            short last;
            short count;
        };
    };
};

#define DIVISOR 64
#define MIN_SLOPE (-15)
#define MAX_SLOPE 15
#define SLOPE_COUNT (MAX_SLOPE - MIN_SLOPE + 1)


int segmentCompare(void const *a1, void const* a2) {
    Segment& s1 = *(Segment*)a1;
    Segment& s2 = *(Segment*)a2;
    if (s1.slope < s2.slope || (s1.slope == s2.slope && s1.x < s2.x)) {
        return -1;
    } else {
        return 1;
    }
}

jint houghVerticalUnsorted(jlong imageAddr, jint bordermask, jint origin, jint maxGap, jint minLength, jlong segmentsAddr);

JNIEXPORT jint JNICALL Java_org_rftg_scorer_CustomNativeTools_houghVertical(JNIEnv*, jobject, jlong imageAddr, jint bordermask, jint origin, jint maxGap, jint minLength, jlong segmentsAddr) {
    jint segmentNumber = houghVerticalUnsorted(imageAddr, bordermask, origin, maxGap, minLength, segmentsAddr);
    if (segmentNumber > 0) {
        Mat& segmentsMat = *(Mat*)segmentsAddr;
        qsort(segmentsMat.ptr<Segment>(0), segmentNumber, sizeof(Segment), segmentCompare);
    }
    return segmentNumber;
}

jint houghVerticalUnsorted(jlong imageAddr, jint bordermask, jint origin, jint maxGap, jint minLength, jlong segmentsAddr) {

//    __android_log_print(ANDROID_LOG_ERROR, "rftg", "UINT sizeof is: %d", sizeof(uint));

    Mat& image = *(Mat*)imageAddr;
    Mat& segmentsMat = *(Mat*)segmentsAddr;

    CV_DbgAssert(image.channels() == 1);
    CV_DbgAssert(image.depth() == CV_8U);

    CV_DbgAssert(segmentsMat.channels() == 4);
    CV_DbgAssert(segmentsMat.depth() == CV_16S);
    CV_DbgAssert(segmentsMat.rows == 1);

    int segmentNumber = 0;
    const int maxSegments = segmentsMat.cols;

    Segment* segments = segmentsMat.ptr<Segment>(0);

    const int cols = image.cols;
    const int rows = image.rows;
    const uchar mask = bordermask;
    const uint longmask = bordermask | (bordermask << 8) | (bordermask << 16) | (bordermask << 24);
//    __android_log_print(ANDROID_LOG_ERROR, "rftg", "Long mask is: %x", bordermask);

    int totalStates = cols * SLOPE_COUNT;

    cv::AutoBuffer<SegmentState, 32> states(totalStates);
    memset(states, 0, sizeof(SegmentState) * totalStates);

    for (int y = 0; y < rows; y++) {
        uint* row = image.ptr<uint>(y);
        int x = 0;
//        for (int x = 0 ; x < cols ; x++) {
        for (int i = cols/sizeof(uint); i > 0 ; i--) {
            uint value = *(row++);

            if ((value & longmask) == 0) {
                x+=sizeof(uint);
                continue;
            }

            for (int j = sizeof(uint) ; j > 0 ; j--) {
                if ((value & mask) != 0) {
                    for (int slope = MIN_SLOPE ; slope <= MAX_SLOPE ; slope++) {
                        int xbase = x + slope * (origin - y) / DIVISOR;

                        if (xbase < 0 || xbase >= cols) {
                            continue;
                        }

                        SegmentState& state = states[SLOPE_COUNT * xbase + (slope - MIN_SLOPE)];

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
                value >>= 8;
                x++;
            }
        }
    }

    // force line endings
    SegmentState* state = states;
    for (int xbase = 0 ; xbase < cols; xbase++) {
        for (int slope = MIN_SLOPE ; slope <= MAX_SLOPE ; slope++) {
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

    CV_DbgAssert(src.depth() == CV_8U);
    CV_DbgAssert(dst.depth() == CV_8U);
    CV_DbgAssert(src.rows == dst.cols);
    CV_DbgAssert(src.cols == dst.rows);
    CV_DbgAssert(src.channels() == 1);
    CV_DbgAssert(dst.channels() == 1);

    int cols = src.cols;
    int rows = src.rows;

    #if HAVE_NEON == 1

    const uchar* su = &src.ptr<uchar>(0)[0];
    const uchar* du = &dst.ptr<uchar>(0)[0];

    const int sa = src.ptr<uchar>(1) - src.ptr<uchar>(0);
    const int da = dst.ptr<uchar>(1) - dst.ptr<uchar>(0);

    for (int y = 0 ; y < rows; y+=16) {
        for (int x = 0 ; x < cols; x+=16) {

            const uchar* s = su + sa * y + x;
            const uchar* d = du + da * x + y;

            asm (
                "mov r0, %[SA]\n\t"
                "mov r1, %[DA]\n\t"
                "mov r2, %[S]\n\t"
                "mov r3, %[D]\n\t"

                "vldmia r2, {q0}\n\t"
                "add r2, r2, r0\n\t"
                "vldmia r2, {q1}\n\t"
                "add r2, r2, r0\n\t"
                "pld [r2]\n\t"
                "vtrn.8 q0, q1\n\t"

                "vldmia r2, {q2}\n\t"
                "add r2, r2, r0\n\t"
                "vldmia r2, {q3}\n\t"
                "add r2, r2, r0\n\t"
                "pld [r2]\n\t"
                "vtrn.8 q2, q3\n\t"

                "vldmia r2, {q4}\n\t"
                "add r2, r2, r0\n\t"
                "vldmia r2, {q5}\n\t"
                "add r2, r2, r0\n\t"
                "pld [r2]\n\t"
                "vtrn.8 q4, q5\n\t"

                "vldmia r2, {q6}\n\t"
                "add r2, r2, r0\n\t"
                "vldmia r2, {q7}\n\t"
                "add r2, r2, r0\n\t"
                "pld [r2]\n\t"
                "vtrn.8 q6, q7\n\t"

                "vldmia r2, {q8}\n\t"
                "add r2, r2, r0\n\t"
                "vldmia r2, {q9}\n\t"
                "add r2, r2, r0\n\t"
                "pld [r2]\n\t"
                "vtrn.8 q8, q9\n\t"

                "vldmia r2, {q10}\n\t"
                "add r2, r2, r0\n\t"
                "vldmia r2, {q11}\n\t"
                "add r2, r2, r0\n\t"
                "pld [r2]\n\t"
                "vtrn.8 q10, q11\n\t"

                "vldmia r2, {q12}\n\t"
                "add r2, r2, r0\n\t"
                "vldmia r2, {q13}\n\t"
                "add r2, r2, r0\n\t"
                "pld [r2]\n\t"
                "vtrn.8 q12, q13\n\t"

                "vldmia r2, {q14}\n\t"
                "add r2, r2, r0\n\t"
                "vldmia r2, {q15}\n\t"
                "vtrn.8 q14, q15\n\t"

                "pld [r3]\n\t"

                "vtrn.32 q0, q4\n\t"
                "vtrn.32 q1, q5\n\t"
                "vtrn.32 q2, q6\n\t"
                "vtrn.32 q3, q7\n\t"

                "vtrn.32 q8, q12\n\t"
                "vtrn.32 q9, q13\n\t"
                "vtrn.32 q10, q14\n\t"
                "vtrn.32 q11, q15\n\t"

                "vtrn.16 q0, q2\n\t"
                "vtrn.16 q1, q3\n\t"
                "vtrn.16 q4, q6\n\t"
                "vtrn.16 q5, q7\n\t"

                "vtrn.16 q8, q10\n\t"
                "vtrn.16 q9, q11\n\t"
                "vtrn.16 q12, q14\n\t"
                "vtrn.16 q13, q15\n\t"

                "vswp d1, d16\n\t"
                "vstmia r3, {q0}\n\t"
                "add r3, r3, r1\n\t"

                "vswp d3, d18\n\t"
                "vstmia r3, {q1}\n\t"
                "add r3, r3, r1\n\t"

                "vswp d5, d20\n\t"
                "vstmia r3, {q2}\n\t"
                "add r3, r3, r1\n\t"

                "vswp d7, d22\n\t"
                "vstmia r3, {q3}\n\t"
                "add r3, r3, r1\n\t"

                "vswp d9, d24\n\t"
                "vstmia r3, {q4}\n\t"
                "add r3, r3, r1\n\t"

                "vswp d11, d26\n\t"
                "vstmia r3, {q5}\n\t"
                "add r3, r3, r1\n\t"

                "vswp d13, d28\n\t"
                "vstmia r3, {q6}\n\t"
                "add r3, r3, r1\n\t"

                "vswp d15, d30\n\t"
                "vstmia r3, {q7}\n\t"
                "add r3, r3, r1\n\t"

                "vstmia r3, {q8}\n\t"
                "add r3, r3, r1\n\t"
                "vstmia r3, {q9}\n\t"
                "add r3, r3, r1\n\t"
                "vstmia r3, {q10}\n\t"
                "add r3, r3, r1\n\t"
                "vstmia r3, {q11}\n\t"
                "add r3, r3, r1\n\t"
                "vstmia r3, {q12}\n\t"
                "add r3, r3, r1\n\t"
                "vstmia r3, {q13}\n\t"
                "add r3, r3, r1\n\t"
                "vstmia r3, {q14}\n\t"
                "add r3, r3, r1\n\t"
                "vstmia r3, {q15}\n\t"


                :
                : [S]"r" (s), [D]"r" (d), [SA]"r" (sa), [DA]"r" (da)
                : "memory", "r0", "r1", "r2", "r3", "q0", "q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10", "q11", "q12", "q13", "q14", "q15"
            );

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

#define COMPARE_BOUND_1 40
#define COMPARE_BOUND_2 80

#define COMPARE_SCORE_1 3
#define COMPARE_SCORE_2 2

JNIEXPORT jint JNICALL Java_org_rftg_scorer_CustomNativeTools_compare(JNIEnv*, jobject, jlong selectionAddr, jlong patternAddr)
{
    Mat& selection = *(Mat*)selectionAddr;
    Mat& pattern = *(Mat*)patternAddr;

    int cols = selection.cols;
    int rows = selection.rows;

    CV_DbgAssert(selection.depth() == CV_8U);
    CV_DbgAssert(pattern.depth() == CV_8U);
    CV_DbgAssert(pattern.rows == rows);
    CV_DbgAssert(pattern.cols == cols);
    CV_DbgAssert(selection.channels() == 3);
    CV_DbgAssert(pattern.channels() == 3);

    CV_DbgAssert(selection.ptr<uchar>(1) - selection.ptr<uchar>(0) == 3*cols);

    jint score = 0;

    uchar* s = selection.ptr<uchar>(0);
    uchar* p = pattern.ptr<uchar>(0);

    #if HAVE_NEON == 1

    asm (
        "mov r0, %[COMPARE_BOUND_B]\n\t"
        "vdup.8 q14, r0\n\t" /* q14: COMPARE_BOUND_2 */
        "mov r0, %[COMPARE_BOUND_A]\n\t"
        "vdup.8 q15, r0\n\t" /* q15: COMPARE_BOUND_1 */
        "mov r0, %[COMPARE_SCORE_B]\n\t"
        "vdup.8 q12, r0\n\t" /* q12: COMPARE_SCORE_2 */
        "mov r0, %[COMPARE_SCORE_DIFF_A]\n\t"
        "vdup.8 q13, r0\n\t" /* q13: COMPARE_SCORE_DIFF_1 */
        "mov r0, 0\n\t"
        "vdup.8 q10, r0\n\t" /* q10: scorer */

        "mov r3, %[SIZE]\n\t"
        "mov r0, %[S]\n\t"
        "mov r1, %[P]\n\t"
        "mov r2, %[S]\n\t"
        "CustomNativeTools_compare_loop:\n\t"
        "vld3.8 {d0,d2,d4}, [r0]!\n\t"
        "vld3.8 {d1,d3,d5}, [r0]!\n\t"
        "pld [r0, #40]\n\t"
        "vld3.8 {d6,d8,d10}, [r1]!\n\t"
        "vld3.8 {d7,d9,d11}, [r1]!\n\t"
        "pld [r1, #40]\n\t"
        "vabd.u8 q6, q0, q3\n\t"
        "vabd.u8 q7, q1, q4\n\t"
        "vabd.u8 q8, q2, q5\n\t"
        "vqadd.u8 q6, q6, q7\n\t"
        "vqadd.u8 q6, q6, q8\n\t"
        "vcle.u8 q7, q6, q14\n\t" /* q7: < COMPARE_BOUND_2 */
        "vcle.u8 q6, q6, q15\n\t" /* q6: < COMPARE_BOUND_1 */
        "vand q7, q7, q12\n\t"
        "vand q6, q6, q13\n\t"
        "vadd.i8 q7, q7, q6\n\t"
        "vpadal.u8 q10, q7\n\t"
        "subs r3, r3, 1\n\t"
        "bgt CustomNativeTools_compare_loop\n\t"
        "vpaddl.u16 q10, q10\n\t"
        "vpaddl.u32 q10, q10\n\t"
        "vadd.i64 d20, d20, d21\n\t"
        "vmov.u32 r0, d20[0]\n\t"
        "mov %[SCORE], r0\n\t"
        : [SCORE]"=r" (score)
        : [S]"r" (s), [P]"r" (p), [SIZE]"r" (cols*rows/16),
          [COMPARE_BOUND_A]"i" (COMPARE_BOUND_1), [COMPARE_BOUND_B]"i" (COMPARE_BOUND_2),
          [COMPARE_SCORE_DIFF_A]"i" (COMPARE_SCORE_1 - COMPARE_SCORE_2), [COMPARE_SCORE_B]"i" (COMPARE_SCORE_2)
        : "cc", "memory", "r0", "r1", "r3", "q0", "q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10", "q11", "q12", "q13", "q14", "q15"
    );

    #else

    for (int i = cols*rows ; i > 0 ; i--) {
        int a1 = (int)(*(s++)) - (int)(*(p++));
        if (a1 < 0) {
            a1 = -a1;
        }

        int a2 = (int)(*(s++)) - (int)(*(p++));
        if (a2 < 0) {
            a2 = -a2;
        }

        int a3 = (int)(*(s++)) - (int)(*(p++));
        if (a3 < 0) {
            a3 = -a3;
        }

        int a = a1 + a2 + a3;
        if (a <= COMPARE_BOUND_1) {
            score += COMPARE_SCORE_1;
        } else if (a <= COMPARE_BOUND_2) {
            score += COMPARE_SCORE_2;
        }
    }

    #endif

    return score;
}

#define NORMAL_DISPERSION 70.*70.
#define NORMAL_MEDIAN 127.5

JNIEXPORT void JNICALL Java_org_rftg_scorer_CustomNativeTools_normalize(JNIEnv*, jobject, jlong imageAddr)
{
    Mat& image = *(Mat*)imageAddr;

    const int cols = image.cols;
    const int rows = image.rows;
    const int total = cols*rows;

    CV_DbgAssert(image.depth() == CV_8U);
    CV_DbgAssert(image.channels() == 3);

    CV_DbgAssert(image.ptr<uchar>(1) - image.ptr<uchar>(0) == 3*cols);

    uchar* a = image.ptr<uchar>(0);

    int sum0, sum1, sum2;
    int sq0, sq1, sq2;

    #if HAVE_NEON == 1

    asm (

        "mov r3, %[SIZE]\n\t"
        "mov r0, %[SRC]\n\t"
        "vmov.i8 q7, 0\n\t" // sum0, sum1
        "vmov.i8 d16, 0\n\t" // sum2
        "vmov.i8 q13, 0\n\t" // sq0
        "vmov.i8 q14, 0\n\t" // sq1
        "vmov.i8 q15, 0\n\t" // sq2

        "CustomNativeTools_normalize_loop_1:\n\t"

        "vld3.8 {d0,d1,d2}, [r0]!\n\t"
        "pld [r0, #16]\n\t"

        "vmull.u8 q3, d0, d0\n\t"
        "vmull.u8 q4, d1, d1\n\t"
        "vmull.u8 q5, d2, d2\n\t"

        "vpadal.u16 q13, q3\n\t"
        "vpadal.u16 q14, q4\n\t"
        "vpadal.u16 q15, q5\n\t"

        "vpaddl.u8 q0, q0\n\t"
        "vpaddl.u8 d2, d2\n\t"

        "vpadal.u16 q7, q0\n\t"
        "vpadal.u16 d16, d2\n\t"

        "subs r3, r3, 1\n\t"
        "bgt CustomNativeTools_normalize_loop_1\n\t"

        "vpaddl.u32 q7, q7\n\t"
        "vpaddl.u32 d16, d16\n\t"

        "vmov.u32 %[SUM0], d14[0]\n\t"
        "vmov.u32 %[SUM1], d15[0]\n\t"
        "vmov.u32 %[SUM2], d16[0]\n\t"

        "vadd.i32 d26, d26, d27\n\t"
        "vpaddl.u32 d26, d26\n\t"
        "vmov.u32 %[SQ0], d26[0]\n\t"

        "vadd.i32 d28, d28, d29\n\t"
        "vpaddl.u32 d28, d28\n\t"
        "vmov.u32 %[SQ1], d28[0]\n\t"

        "vadd.i32 d30, d30, d31\n\t"
        "vpaddl.u32 d30, d30\n\t"
        "vmov.u32 %[SQ2], d30[0]\n\t"

        : [SUM0]"=r" (sum0), [SUM1]"=r" (sum1), [SUM2]"=r" (sum2), [SQ0]"=r" (sq0), [SQ1]"=r" (sq1), [SQ2]"=r" (sq2)
        : [SRC]"r" (a), [SIZE]"r" (total/8)
        : "cc", "r0", "r3", "q0", "q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10", "q11", "q12", "q13", "q14", "q15"
    );

    #else

    sum0 = 0; sum1 = 0; sum2 = 0;
    sq0 = 0; sq1 = 0; sq2 = 0;

    for (int i = total ; i > 0 ; i--) {
        uchar v0 = *(a++);
        sum0 += v0;
        sq0 += v0*v0;

        uchar v1 = *(a++);
        sum1 += v1;
        sq1 += v1*v1;

        uchar v2 = *(a++);
        sum2 += v2;
        sq2 += v2*v2;
    }

    #endif

    float sum0norm = sum0 / total;
    float sq0norm = sq0 / total;
    float disp0 = sq0norm - sum0norm*sum0norm;
    float alpha0, beta0;
    if (disp0 < 1) {
        alpha0 = 1;
        beta0 = 0;
    } else {
        alpha0 = sqrt(NORMAL_DISPERSION/disp0);
        beta0 = NORMAL_MEDIAN - alpha0 * sum0norm;
    }

    float sum1norm = sum1 / total;
    float sq1norm = sq1 / total;
    float disp1 = sq1norm - sum1norm*sum1norm;
    float alpha1, beta1;
    if (disp1 < 1) {
        alpha1 = 1;
        beta1 = 0;
    } else {
        alpha1 = sqrt(NORMAL_DISPERSION/disp1);
        beta1 = NORMAL_MEDIAN - alpha1 * sum1norm;
    }

    float sum2norm = sum2 / total;
    float sq2norm = sq2 / total;
    float disp2 = sq2norm - sum2norm*sum2norm;
    float alpha2, beta2;
    if (disp2 < 1) {
        alpha2 = 1;
        beta2 = 0;
    } else {
        alpha2 = sqrt(NORMAL_DISPERSION/disp2);
        beta2 = NORMAL_MEDIAN - alpha2 * sum2norm;
    }

    a = image.ptr<uchar>(0);

    #if HAVE_NEON == 1

    asm (
        "mov r3, %[SIZE]\n\t"
        "mov r0, %[SRC]\n\t"

        "vdup.32 q10, %[ALPHA0]\n\t"
        "vdup.32 q11, %[ALPHA1]\n\t"
        "vdup.32 q12, %[ALPHA2]\n\t"
        "vdup.32 q13, %[BETA0]\n\t"
        "vdup.32 q14, %[BETA1]\n\t"
        "vdup.32 q15, %[BETA2]\n\t"

        "CustomNativeTools_normalize_loop_2:\n\t"
        "vld3.8 {d0,d2,d4}, [r0]\n\t"
        "pld [r0, #40]\n\t"

        // red
        "vmovl.u8 q4, d0\n\t"

        "vmovl.u16 q5, d8\n\t"
        "vcvt.f32.u32 q5, q5\n\t"
        "vmov q6, q13\n\t"
        "vmla.f32 q6, q5, q10\n\t"
        "vcvt.u32.f32 q6, q6\n\t"
        "vqmovn.u32 d8, q6\n\t"

        "vmovl.u16 q5, d9\n\t"
        "vcvt.f32.u32 q5, q5\n\t"
        "vmov q6, q13\n\t"
        "vmla.f32 q6, q5, q10\n\t"
        "vcvt.u32.f32 q6, q6\n\t"
        "vqmovn.u32 d9, q6\n\t"

        "vqmovn.u16 d0, q4\n\t"

        // green
        "vmovl.u8 q4, d2\n\t"

        "vmovl.u16 q5, d8\n\t"
        "vcvt.f32.u32 q5, q5\n\t"
        "vmov q6, q14\n\t"
        "vmla.f32 q6, q5, q11\n\t"
        "vcvt.u32.f32 q6, q6\n\t"
        "vqmovn.u32 d8, q6\n\t"

        "vmovl.u16 q5, d9\n\t"
        "vcvt.f32.u32 q5, q5\n\t"
        "vmov q6, q14\n\t"
        "vmla.f32 q6, q5, q11\n\t"
        "vcvt.u32.f32 q6, q6\n\t"
        "vqmovn.u32 d9, q6\n\t"

        "vqmovn.u16 d2, q4\n\t"

        // blue
        "vmovl.u8 q4, d4\n\t"

        "vmovl.u16 q5, d8\n\t"
        "vcvt.f32.u32 q5, q5\n\t"
        "vmov q6, q15\n\t"
        "vmla.f32 q6, q5, q12\n\t"
        "vcvt.u32.f32 q6, q6\n\t"
        "vqmovn.u32 d8, q6\n\t"

        "vmovl.u16 q5, d9\n\t"
        "vcvt.f32.u32 q5, q5\n\t"
        "vmov q6, q15\n\t"
        "vmla.f32 q6, q5, q12\n\t"
        "vcvt.u32.f32 q6, q6\n\t"
        "vqmovn.u32 d9, q6\n\t"

        "vqmovn.u16 d4, q4\n\t"


        "vst3.8 {d0,d2,d4}, [r0]!\n\t"
        "subs r3, r3, 1\n\t"
        "bgt CustomNativeTools_normalize_loop_2\n\t"

        : 
        : [SRC]"r" (a), [SIZE]"r" (total/8),
          [ALPHA0]"r" (alpha0), [ALPHA1]"r" (alpha1), [ALPHA2]"r" (alpha2),
          [BETA0]"r" (beta0), [BETA1]"r" (beta1), [BETA2]"r" (beta2)
        : "cc", "memory", "r0", "r3", "q0", "q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10", "q11", "q12", "q13", "q14", "q15"
    );

    #else
    
    for (int i = total ; i > 0 ; i--) {
        uchar v0 = *a;
        float f0 = alpha0 * v0 + beta0;
        if (f0 <= 0) {
            *a = 0;
        } else if (f0 >= 255) {
            *a = 255;
        } else {
            *a = f0;
        }
        a++;

        uchar v1 = *a;
        float f1 = alpha1 * v1 + beta1;
        if (f1 <= 0) {
            *a = 0;
        } else if (f1 >= 255) {
            *a = 255;
        } else {
            *a = f1;
        }
        a++;

        uchar v2 = *a;
        float f2 = alpha2 * v2 + beta2;
        if (f1 <= 0) {
            *a = 0;
        } else if (f2 >= 255) {
            *a = 255;
        } else {
            *a = f2;
        }
        a++;

    }

    #endif

}

}
