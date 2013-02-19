#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>

using namespace std;
using namespace cv;

extern "C" {
JNIEXPORT jdouble JNICALL Java_org_rftg_scorer_CustomNativeTools_testNativeCall(JNIEnv*, jobject, jdouble value);

JNIEXPORT jdouble JNICALL Java_org_rftg_scorer_CustomNativeTools_testNativeCall(JNIEnv*, jobject, jdouble value)
{
    return 2*value;
}
}
