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

JNIEXPORT jlong JNICALL Java_org_rftg_scorer_CustomNativeTools_sobel(JNIEnv*, jobject, jlong srcAddr, jlong dstAddr);

JNIEXPORT jlong JNICALL Java_org_rftg_scorer_CustomNativeTools_sobel(JNIEnv*, jobject, jlong srcAddr, jlong dstAddr)
{
    Mat& src = *(Mat*)srcAddr;
    Mat& dst = *(Mat*)dstAddr;

    CV_Assert(src.depth() == CV_8U);
    CV_Assert(dst.depth() == CV_8U);
    CV_Assert(src.rows == dst.rows);
    CV_Assert(src.cols == dst.cols);
    CV_Assert(src.cols == dst.cols);
    CV_Assert(src.channels() == dst.channels());

    const int rows = src.rows;
    const int cols = src.cols;
    for (int i = 1 ; i < rows-1 ; i++) {

#if HAVE_NEON == 1

        uint8x8_t* prow = src.ptr<uint8x8_t>(i-1);
        uint8x8_t* nrow = src.ptr<uint8x8_t>(i+1);

        uint8x8_t* drow = dst.ptr<uint8x8_t>(i);

        int16x8_t p0 = (int16x8_t)vmovl_u8(prow[0]);
        int16x8_t n0 = (int16x8_t)vmovl_u8(nrow[0]);

        int16x8_t p1 = (int16x8_t)vmovl_u8(prow[1]);
        int16x8_t n1 = (int16x8_t)vmovl_u8(nrow[1]);

//        uint8x8_t delta = vdup_n_u8(128);

        int16x8_t lower = vdupq_n_s16(-100);
        int16x8_t upper = vdupq_n_s16(100);

        for (int j = 2 ; j < cols/8; j++) {

            int16x8_t p2 = (int16x8_t)vmovl_u8(prow[j]);
            int16x8_t n2 = (int16x8_t)vmovl_u8(nrow[j]);

            int16x8_t px = vextq_s16(p0, p1, 7);
            int16x8_t py = vextq_s16(p1, p2, 1);

            int16x8_t nx = vextq_s16(n0, n1, 7);
            int16x8_t ny = vextq_s16(n1, n2, 1);

            // nx+2*n1+ny - (px+2*p1+py)
            int16x8_t a = vsubq_s16(n1, p1);
            int16x8_t c = vaddq_s16(nx, ny);
            a = vshlq_n_s16(a, 1);
            int16x8_t b = vaddq_s16(px, py);
            a = vaddq_s16(a, c);
            a = vsubq_s16(a, b);

            uint16x8_t dark = vcgeq_s16(a, lower);
            uint16x8_t light = vcgeq_s16(a, upper);

            drow[j-1] = vorr_u8(vqmovn_u16(light), vshr_n_u8(vqmovn_u16(dark),1));
            //vadd_u8((uint8x8_t)vqmovn_s16(a), delta);

            p0 = p1;
            n0 = n1;
            p1 = p2;
            n1 = n2;

           //drow[j] = (uchar)((1020 + (int)prow[j-1] + 2*(int)prow[j] + (int)prow[j+1] - (int)nrow[j-1] - 2*(int)nrow[j] - (int)nrow[j+1]) >> 3);
        }


#else


        uchar* prow = src.ptr<uchar>(i-1);
//        uchar* crow = src.ptr<uchar>(i);
        uchar* nrow = src.ptr<uchar>(i+1);

        uchar* drow = dst.ptr<uchar>(i);

        for (int j = 1 ; j < cols-1; j++) {
            drow[j] = (uchar)((1020 + (int)prow[j-1] + 2*(int)prow[j] + (int)prow[j+1] - (int)nrow[j-1] - 2*(int)nrow[j] - (int)nrow[j+1]) >> 3);
        }


#endif

    }

    return 0;

}
}
