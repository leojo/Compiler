#!/bin/bash
HELP1="Incorrect usage:"
HELP2="To compile a program run the command"
HELP3=">NanoMorpho.sh -c [full name of source file]"
HELP4="To run a compiled program run the command"
HELP5=">NanoMorpho.sh [name of source file without extension]"
if [[ $# == 0 ]] || [[ $# > 2 ]]; then
	echo $HELP1
	echo $HELP2
	echo $HELP3
	echo $HELP4
	echo $HELP5
	exit
fi
if [ $1 == '-c' ]
then
SOURCE_NAME=$2
BASE_NAME=$(basename $SOURCE_NAME)
RAW_NAME=${BASE_NAME%.*}
java -cp bin Compiler $SOURCE_NAME
java -cp bin/morpho.jar -jar bin/morpho.jar -c $RAW_NAME.masm
rm $RAW_NAME.masm
else
java -cp bin/morpho.jar -jar bin/morpho.jar $1
fi
