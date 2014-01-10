#! /bin/sh

rm -r obj/* libs/* out/*
NDK_DEBUG=1 ndk-build
