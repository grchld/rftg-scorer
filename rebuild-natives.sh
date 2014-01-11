#! /bin/sh

rm -r obj/* libs/* out/*
NDK_DEBUG=0 ndk-build
