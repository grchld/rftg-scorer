#include <jni.h>
//#include <vector>
//#include <math.h>
#include <string.h>
#include <stdlib.h>
#include <android/log.h>

#if HAVE_NEON == 1
#include <arm_neon.h>
#endif

using namespace std;

extern "C" {

#define SOBEL_V_LIGHT_BOUND 65
#define SOBEL_V_DARK_BOUND 100
#define SOBEL_H_LIGHT_BOUND 150
#define SOBEL_H_DARK_BOUND 100

typedef unsigned char uchar;
typedef unsigned int uint;

JNIEXPORT void JNICALL Java_org_rftg_scorer_NativeTools_sobel(JNIEnv* env, jclass, jobject srcBuffer, jobject dstBuffer, jint width, jint height)
{
    uchar* src = (uchar*)(*env).GetDirectBufferAddress(srcBuffer);
    uchar* dst = (uchar*)(*env).GetDirectBufferAddress(dstBuffer);

    memset(dst, 0, width);
    memset(&dst[width*(height-1)], 0, width);

    for (int i = 1 ; i < height-1 ; i++) {

#if HAVE_NEON == 1

        uint8x8_t* drow = (uint8x8_t*)&dst[i*width];

        int* idrow = (int*)drow;
        idrow[0] = 0;
        idrow[1] = 0;
        idrow[width-1] = 0;
        idrow[width-2] = 0;


        uint8x8_t* prow = (uint8x8_t*)&src[(i-1)*width];
        uint8x8_t* crow = (uint8x8_t*)&src[i*width];
        uint8x8_t* nrow = (uint8x8_t*)&src[(i+1)*width];


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

        for (int j = 2 ; j < width/8; j++) {

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


        uchar* prow = &src[(i-1)*width];
        uchar* crow = &src[i*width];
        uchar* nrow = &src[(i+1)*width];

        uchar* drow = &dst[i*width];

        drow[0] = 0;
        drow[width-1] = 0;

        for (int j = 1 ; j < width-1; j++) {
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

jint houghVerticalUnsorted(uchar* image, jint width, jint height, jint bordermask, jint origin, jint maxGap, jint minLength, Segment* segments, jint maxSegments, SegmentState* segmentStates);

JNIEXPORT jint JNICALL Java_org_rftg_scorer_NativeTools_houghVertical(JNIEnv* env, jclass, jobject imageBuffer, jint width, jint height, jint bordermask, jint origin, jint maxGap, jint minLength, jobject segmentsBuffer, jint maxSegments, jobject segmentStatesBuffer) {
    uchar* image = (uchar*)(*env).GetDirectBufferAddress(imageBuffer);
    Segment* segments = (Segment*)(*env).GetDirectBufferAddress(segmentsBuffer);
    SegmentState* segmentStates = (SegmentState*)(*env).GetDirectBufferAddress(segmentStatesBuffer);

    jint segmentNumber = houghVerticalUnsorted(image, width, height, bordermask, origin, maxGap, minLength, segments, maxSegments, segmentStates);
    if (segmentNumber > 0) {
        qsort(segments, segmentNumber, sizeof(Segment), segmentCompare);
    }
    return segmentNumber;
}

jint houghVerticalUnsorted(uchar* image, jint width, jint height, jint bordermask, jint origin, jint maxGap, jint minLength, Segment* segments, jint maxSegments, SegmentState* states) {

    int segmentNumber = 0;

    const uchar mask = bordermask;
    const uint longmask = bordermask | (bordermask << 8) | (bordermask << 16) | (bordermask << 24);

    int totalStates = width * SLOPE_COUNT;

    memset(states, 0, sizeof(SegmentState) * totalStates);

    uint* row = (uint*)image;
    
    for (int y = 0; y < height; y++) {
//        uint* row = (uint*)(&image[width * y]);
        int x = 0;
        for (int i = width/sizeof(uint); i > 0 ; i--) {
            uint value = *(row++);

            if ((value & longmask) == 0) {
                x+=sizeof(uint);
                continue;
            }

            for (int j = sizeof(uint) ; j > 0 ; j--) {
                if ((value & mask) != 0) {
                    for (int slope = MIN_SLOPE ; slope <= MAX_SLOPE ; slope++) {
                        int xbase = x + slope * (origin - y) / DIVISOR;

                        if (xbase < 0 || xbase >= width) {
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
    for (int xbase = 0 ; xbase < width; xbase++) {
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


JNIEXPORT void JNICALL Java_org_rftg_scorer_NativeTools_transpose(JNIEnv* env, jclass, jobject srcBuffer, jobject dstBuffer, jint width, jint height)
{

    uchar* src = (uchar*)(*env).GetDirectBufferAddress(srcBuffer);
    uchar* dst = (uchar*)(*env).GetDirectBufferAddress(dstBuffer);

    #if HAVE_NEON == 1

    const uchar* su = src;
    const uchar* du = dst;

    const int sa = width;
    const int da = height;

    for (int y = 0 ; y < height; y+=16) {
        for (int x = 0 ; x < width; x+=16) {

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

    for (int y = 0 ; y < height; y++) {
        for (int x = 0 ; x < width; x++) {
            dst[x * height + y] = src[y * width + x];
        }
    }

    #endif

}

#ifdef FALSE

#define COMPARE_BOUND_1 15
#define COMPARE_BOUND_2 30

#define COMPARE_SCORE_1 3
#define COMPARE_SCORE_2 2

#define FIRST_GAMBLING_WORLD 86
#define SECOND_GAMBLING_WORLD 144

JNIEXPORT jlong JNICALL Java_org_rftg_scorer_NativeTools_match(JNIEnv*, jobject, jlong selectionAddr, jlong patternsAddr, jint patternSize, jint patternsCount)
{
    Mat& selection = *(Mat*)selectionAddr;
    Mat& pattern = *(Mat*)patternsAddr;

    #if HAVE_NEON == 1

    int best;
    int second_best;

    asm (
        "mov r0, %[COMPARE_BOUND_B]\n\t"
        "vdup.8 q14, r0\n\t" /* q14: COMPARE_BOUND_2 */
        "mov r0, %[COMPARE_BOUND_A]\n\t"
        "vdup.8 q15, r0\n\t" /* q15: COMPARE_BOUND_1 */
        "mov r0, %[COMPARE_SCORE_B]\n\t"
        "vdup.8 q12, r0\n\t" /* q12: COMPARE_SCORE_2 */
        "mov r0, %[COMPARE_SCORE_DIFF_A]\n\t"
        "vdup.8 q13, r0\n\t" /* q13: COMPARE_SCORE_DIFF_1 */

        "mov r0, %[SELECTION]\n\t"
        "mov r1, %[PATTERN]\n\t"

        "mov %[BEST], #0\n\t" /* bestScore << 16 + bestCardNumber */
        "mov %[SECOND_BEST], #0\n\t" /* secondBestScore << 16 + secondBestCardNumber */

        "mov r2, #0\n\t"

        "NativeTools_match_loop_1:\n\t"
        "mov r3, %[SIZE]\n\t"

        "vmov.i8 q10, #0\n\t" /* q10: scorer */

        "NativeTools_match_loop_2:\n\t"
        "vldmia r0!, {q0}\n\t"
        "vldmia r1!, {q1}\n\t"
        "vabd.u8 q8, q0, q1\n\t"
        "vcle.u8 q9, q8, q14\n\t" /* q7: < COMPARE_BOUND_2 */
        "vcle.u8 q8, q8, q15\n\t" /* q6: < COMPARE_BOUND_1 */
        "vand q9, q9, q12\n\t"
        "vand q8, q8, q13\n\t"
        "vadd.i8 q9, q9, q8\n\t"
        "vpadal.u8 q10, q9\n\t"
        "subs r3, 1\n\t"
        "bgt NativeTools_match_loop_2\n\t"

        "mov r0, %[SELECTION]\n\t"

        "vadd.i16 d20, d20, d21\n\t"
        "vpaddl.u16 d20, d20\n\t"
        "vpaddl.u32 d20, d20\n\t"

        "vmov.u32 r4, d20[0]\n\t"

        "orr r4, r2, r4, lsl #16\n\t"

        "cmp r2, %[_SECOND_GAMBLING_WORLD]\n\t"
        "beq NativeTools_match_loop_3\n\t"

        "NativeTools_match_loop_5:\n\t"

        "cmp %[SECOND_BEST], r4\n\t"
        "itttt le\n\t"
        "movle %[SECOND_BEST], r4\n\t"
        "cmple %[BEST], r4\n\t"
        "movle %[SECOND_BEST], %[BEST]\n\t"
        "movle %[BEST], r4\n\t"

        "add r2, 1\n\t"
        "cmp %[COUNT], r2\n\t"
        "bgt NativeTools_match_loop_1\n\t"
        "bal NativeTools_match_loop_4\n\t"

        "NativeTools_match_loop_3:\n\t"

        "and r3, %[BEST], #0xff\n\t"
        "cmp r3, %[_FIRST_GAMBLING_WORLD]\n\t"
        "bne NativeTools_match_loop_5\n\t"
        "cmp %[BEST], r4\n\t"
        "it le\n\t"
        "movle %[BEST], r4\n\t"
        "add r2, 1\n\t"
        "bal NativeTools_match_loop_1\n\t"
        "NativeTools_match_loop_4:\n\t"

        : [BEST]"=&r" (best), [SECOND_BEST]"=&r" (second_best)
        : [SELECTION]"r" (selection.ptr<uchar>(0)), [PATTERN]"r" (pattern.ptr<uchar>(0)), [SIZE]"r" (patternSize/16), [COUNT]"r" (patternsCount),
          [COMPARE_BOUND_A]"i" (COMPARE_BOUND_1), [COMPARE_BOUND_B]"i" (COMPARE_BOUND_2),
          [COMPARE_SCORE_DIFF_A]"i" (COMPARE_SCORE_1 - COMPARE_SCORE_2), [COMPARE_SCORE_B]"i" (COMPARE_SCORE_2),
          [_FIRST_GAMBLING_WORLD]"i" (FIRST_GAMBLING_WORLD), [_SECOND_GAMBLING_WORLD]"i" (SECOND_GAMBLING_WORLD)

        : "cc", "r0", "r1", "r2", "r3", "r4", "q0", "q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10", "q11", "q12", "q13", "q14", "q15"
    );

    return ((jlong)second_best) << 32 | ((jlong)best);

    #else

    uchar* p = pattern.ptr<uchar>(0);

    int bestCardNumber = 0;
    int bestScore = 0;
    int secondBestCardNumber = 0;
    int secondBestScore = 0;

    for (int cardNumber = 0 ; cardNumber < patternsCount ; cardNumber++) {

        uchar* s = selection.ptr<uchar>(0);

        int score = 0;

        for (int i = patternSize ; i > 0 ; i--) {
            int a = (int)(*(s++)) - (int)(*(p++));
            if (a < 0) {
                a = -a;
            }

            if (a <= COMPARE_BOUND_1) {
                score += COMPARE_SCORE_1;
            } else if (a <= COMPARE_BOUND_2) {
                score += COMPARE_SCORE_2;
            }
        }

        if (bestScore < score) {
            if (bestCardNumber != FIRST_GAMBLING_WORLD || cardNumber != SECOND_GAMBLING_WORLD) {
                secondBestScore = bestScore;
                secondBestCardNumber = bestCardNumber;
            }
            bestScore = score;
            bestCardNumber = cardNumber;
        } else if (secondBestScore < score) {
            secondBestScore = score;
            secondBestCardNumber = cardNumber;
        }
    }

    return (((jlong)secondBestScore) << 48) | (((jlong)secondBestCardNumber) << 32) | (((jlong)bestScore) << 16) | ((jlong)bestCardNumber);

    #endif

}

#define NORMAL_DISPERSION 70.*70.
#define NORMAL_MEDIAN 127.5

JNIEXPORT void JNICALL Java_org_rftg_scorer_NativeTools_normalize(JNIEnv*, jobject, jlong imageAddr)
{
    Mat& image = *(Mat*)imageAddr;

    const int cols = image.cols;
    const int rows = image.rows;
    const int total = cols*rows;

    CV_DbgAssert(image.depth() == CV_8U);
    CV_DbgAssert(image.channels() == 1);

    CV_DbgAssert(image.ptr<uchar>(1) - image.ptr<uchar>(0) == cols);

    uchar* a = image.ptr<uchar>(0);

    int sum, sq;

    #if HAVE_NEON == 1

    asm (

        "mov r3, %[SIZE]\n\t"
        "mov r0, %[SRC]\n\t"
        "vmov.i8 q7, 0\n\t" // sum
        "vmov.i8 q13, 0\n\t" // sq

        "NativeTools_normalize_loop_1:\n\t"

        "vldmia r0!, {q0} \n\t"

        "vmull.u8 q3, d0, d0\n\t"
        "vmull.u8 q4, d1, d1\n\t"

        "vpadal.u16 q13, q3\n\t"
        "vpadal.u16 q13, q4\n\t"

        "vpaddl.u8 q0, q0\n\t"

        "vpadal.u16 q7, q0\n\t"

        "subs r3, r3, 1\n\t"
        "bgt NativeTools_normalize_loop_1\n\t"

        "vpaddl.u32 q7, q7\n\t"
        "vadd.i32 d14, d14, d15\n\t"

        "vmov.u32 %[SUM], d14[0]\n\t"

        "vadd.i32 d26, d26, d27\n\t"
        "vpaddl.u32 d26, d26\n\t"
        "vmov.u32 %[SQ], d26[0]\n\t"

        : [SUM]"=r" (sum), [SQ]"=r" (sq)
        : [SRC]"r" (a), [SIZE]"r" (total/16)
        : "cc", "r0", "r3", "q0", "q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10", "q11", "q12", "q13", "q14", "q15"
    );

    #else

    sum = 0;
    sq = 0;

    for (int i = total ; i > 0 ; i--) {
        uchar v = *(a++);
        sum += v;
        sq += v*v;
    }

    #endif

    float sumnorm = sum / total;
    float sqnorm = sq / total;
    float disp = sqnorm - sumnorm*sumnorm;
    float alpha, beta;
    if (disp < 1) {
        alpha = 1;
        beta = 0;
    } else {
        alpha = sqrt(NORMAL_DISPERSION/disp);
        beta = NORMAL_MEDIAN - alpha * sumnorm;
    }

    a = image.ptr<uchar>(0);

    #if HAVE_NEON == 1

    asm (
        "mov r3, %[SIZE]\n\t"
        "mov r0, %[SRC]\n\t"

        "vdup.32 q10, %[ALPHA]\n\t"
        "vdup.32 q13, %[BETA]\n\t"

        "NativeTools_normalize_loop_2:\n\t"
        "vldmia r0, {d0}\n\t"

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

        "vstmia r0!, {d0}\n\t"
        "subs r3, r3, 1\n\t"
        "bgt NativeTools_normalize_loop_2\n\t"

        :
        : [SRC]"r" (a), [SIZE]"r" (total/8),
          [ALPHA]"r" (alpha), [BETA]"r" (beta)
        : "cc", "memory", "r0", "r3", "q0", "q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10", "q11", "q12", "q13", "q14", "q15"
    );

    #else

    for (int i = total ; i > 0 ; i--) {
        uchar v = *a;
        float f = alpha * v + beta;
        if (f <= 0) {
            *a = 0;
        } else if (f >= 255) {
            *a = 255;
        } else {
            *a = f;
        }
        a++;

    }

    #endif

}

JNIEXPORT jint JNICALL Java_org_rftg_scorer_NativeTools_drawSobel(JNIEnv*, jobject, jlong sobelAddr, jlong frameAddr)
{
    Mat& sobel = *(Mat*)sobelAddr;
    Mat& frame = *(Mat*)frameAddr;

    int total = sobel.cols * sobel.rows;

    uchar* s = sobel.ptr<uchar>(0);
    uchar* f = frame.ptr<uchar>(0);

    for (int i = total ; i > 0 ; i--) {
        uchar mask = *(s++);

        uchar* r = f++;
        uchar* g = f++;
        uchar* b = f++;


        if (mask & 0x80) {
            if (mask & 0x20) {
                *r = 224;
                *g = 224;
                *b = 255;
            } else if (mask & 0x10) {
                *r = 224;
                *g = 224;
                *b = 255;
            } else {
                *r = 255;
                *g = 255;
                *b = 0;
            }
        } else if (mask & 0x40) {
            if (mask & 0x20) {
                *r = 224;
                *g = 224;
                *b = 255;
            } else if (mask & 0x10) {
                *r = 224;
                *g = 224;
                *b = 255;
            } else {
                *r = 255;
                *g = 0;
                *b = 255;
            }
        } else {
            if (mask & 0x20) {
                *r = 255;
                *g = 0;
                *b = 0;
            } else if (mask & 0x10) {
                *r = 0;
                *g = 255;
                *b = 0;
            }
        }

    }
}
#endif
}
