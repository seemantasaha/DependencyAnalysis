#!/bin/bash

JAVA=/home/zzk/projects/isstac/jre1.7.0_80/lib
ENG_DIR=/home/zzk/projects/isstac/Challenge_Programs

REG=com.stac.**
MAIN=com.stac.Main 
APP=$ENG_DIR/image_processor/challenge_program/ipchallenge-0.1.jar 
OUT=output_imageprocessor
TAMIFLEX=$OUT/refl.log

java -jar averroes.jar -r $REG -m $MAIN -a $APP -o $OUT -j $JAVA -t $TAMIFLEX

