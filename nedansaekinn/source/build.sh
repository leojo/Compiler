#!/bin/bash
mkdir -p ../bin
java -jar jflex-1.6.0.jar nanomorpho.jflex
./yacc.linux -J -Jclass=Compiler nanoMorpho.byaccj
javac -d ../bin Lexer.java Compiler.java
rm *.java
cp morpho.jar ../bin
