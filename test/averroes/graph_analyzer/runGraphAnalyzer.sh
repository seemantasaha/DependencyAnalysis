#!/bin/bash

JAVA=/home/zzk/projects/isstac/jre1.7.0_80/lib
ENG_DIR=/home/zzk/projects/isstac/Challenge_Programs

REG=Acme.**:brs.**:data.**:graph.**:graphed.**:graphviz.**:org.graph.**:org.tigris.**:user.**:EDIFParser:listnode
MAIN=user.commands.CommandProcessor
APP=$ENG_DIR/graph_analyzer/challenge_program/GraphDisplay.jar 
OUT=output_graphanalyzer

java -jar averroes.jar -r $REG -m $MAIN -a $APP -o $OUT -j $JAVA

