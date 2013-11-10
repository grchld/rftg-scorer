#! /bin/sh

rm -r obj/* libs/*
NDK_DEBUG=0 ndk-build
