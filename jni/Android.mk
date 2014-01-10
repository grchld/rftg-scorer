LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := rftg_scorer
LOCAL_SRC_FILES := jni_part.cpp
LOCAL_LDLIBS +=  -llog -ldl
LOCAL_CFLAGS += -O2
#LOCAL_CFLAGS += -O3 -g -S -dA
#LOCAL_CFLAGS += -O2 -g -S -dA
#LOCAL_CFLAGS += -O0 -g


ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_ARM_NEON  := true
LOCAL_CFLAGS += -ffast-math -mfpu=neon -DHAVE_NEON=1
endif

include $(BUILD_SHARED_LIBRARY)
