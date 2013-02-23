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

/*#if HAVE_NEON == 1

        int* h1 = &(hist[0]);
        int* h2 = &(hist[256]);
        int* h3 = &(hist[256*2]);
        int* h4 = &(hist[256*3]);
        for (int i = 0; i < image.rows ; i++) {
            uint8x16_t* row = image.ptr<uint8x16_t>(i);
            for (int j = 0; j < image.cols/4 ; j++ ) {
                unsigned int v = row[j];
                h1[v & 0xff]++;
                h2[(v >> 8) & 0xff]++;
                h3[(v >> 16) & 0xff]++;
                h4[v >> 24]++;
            }
        }
        

#else*/

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
//#endif
        
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
}
