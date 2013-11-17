#! /usr/bin/python

import sys
import math

with open(sys.argv[1], "rb") as f:
    f.seek(138) # skip bmp header
    imagebytes = f.read()

PIXELCOUNT = len(imagebytes)/3

NORMAL_DISPERSION = 70.*70.
NORMAL_MEDIAN = 127.5

sum = 0.
sq = 0.

gray = []

for i in xrange(PIXELCOUNT):
    #print "Pixel %d has R:%d G:%d B:%d" % (i,ord(imagebytes[3*i+2]), ord(imagebytes[3*i+1]), ord(imagebytes[3*i]))
    g = 0.298839 * ord(imagebytes[3*i+2]) + 0.586811 * ord(imagebytes[3*i+1]) + 0.114350 * ord(imagebytes[3*i])
    sum += g;
    sq += g*g;
    gray.append(g)
    
sumnorm = sum / PIXELCOUNT
sqnorm = sq / PIXELCOUNT
disp = sqnorm - sumnorm*sumnorm

if disp < 1:
    alpha = 1
    beta = 0
else:
    alpha = math.sqrt(NORMAL_DISPERSION/disp)
    beta = NORMAL_MEDIAN - alpha * sumnorm

with open(sys.argv[2], "wb") as f:
    for g in gray:
        v = int(alpha * g + beta)
        if v < 0: v = 0
        elif v > 255: v = 255
        f.write(chr(v))