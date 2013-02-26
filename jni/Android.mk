LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include /home/gc/OpenCV-2.4.3.2-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := rftg_scorer
LOCAL_SRC_FILES := jni_part.cpp
LOCAL_LDLIBS +=  -llog -ldl
LOCAL_CFLAGS += -O2


ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_ARM_NEON  := true
LOCAL_CFLAGS += -ffast-math -mfpu=neon -DHAVE_NEON=1
endif

include $(BUILD_SHARED_LIBRARY)
