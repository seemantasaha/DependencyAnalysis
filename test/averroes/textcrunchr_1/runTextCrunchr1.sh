#!/bin/bash

JAVA=/home/zzk/projects/isstac/jre1.7.0_80/lib
ENG_DIR=/home/zzk/projects/isstac/Challenge_Programs

REG=com.ahancock.**:com.cyberpointllc.**:com.nicnilov.**
MAIN=com.cyberpointllc.stac.host.Main 
APP=$ENG_DIR/textcrunchr_1/challenge_program/lib/textcrunchr_1.jar
OUT=output_textcrunchr_1
LIB=$ENG_DIR/textcrunchr_1/challenge_program/lib/commons-cli-1.3.jar:$ENG_DIR/textcrunchr_1/challenge_program/lib/commons-codec-1.9.jar:$ENG_DIR/textcrunchr_1/challenge_program/lib/commons-compress-1.3.jar:$ENG_DIR/textcrunchr_1/challenge_program/lib/commons-fileupload-1.3.1.jar:$ENG_DIR/textcrunchr_1/challenge_program/lib/commons-io-2.2.jar:$ENG_DIR/textcrunchr_1/challenge_program/lib/commons-lang3-3.4.jar:$ENG_DIR/textcrunchr_1/challenge_program/lib/commons-logging-1.2.jar:$ENG_DIR/textcrunchr_1/challenge_program/lib/httpclient-4.5.1.jar:$ENG_DIR/textcrunchr_1/challenge_program/lib/httpcore-4.4.3.jar:$ENG_DIR/textcrunchr_1/challenge_program/lib/jline-2.8.jar:$ENG_DIR/textcrunchr_1/challenge_program/lib/mapdb-2.0-beta8.jar:$ENG_DIR/textcrunchr_1/challenge_program/lib/scrypt-1.4.0.jar

java -jar averroes.jar -r $REG -m $MAIN -a $APP -l $LIB -o $OUT -j $JAVA

