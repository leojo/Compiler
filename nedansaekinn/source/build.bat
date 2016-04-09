if not exist "..\bin" mkdir ..\bin
java -jar jflex-1.6.0.jar nanomorpho.jflex
byacc.exe -J -Jclass=Compiler nanoMorpho.byaccj
javac -d ..\bin Lexer.java Compiler.java
del *.java
copy morpho.jar ..\bin