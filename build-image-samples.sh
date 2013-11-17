#! /bin/bash

OUT=assets/samples.dat

rm $OUT

TMPDIR=`mktemp --tmpdir -d RFTG.XXXXXX`

for q in '?' '??' '???' ; do
    ls -1 res/drawable/card_${q}.jpg | while read file ; do
	filename=`basename $file`
	echo "Processing: $filename"
        convert -crop '326x474+23+23' -resize 64x64! -flip $file $TMPDIR/$filename.bmp
        ./normalize-sample-image.py $TMPDIR/$filename.bmp $TMPDIR/$filename.strip
        cat $TMPDIR/$filename.strip >> $OUT
    done
done

rm -r $TMPDIR
