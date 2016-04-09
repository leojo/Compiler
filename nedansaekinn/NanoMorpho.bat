@echo off
setlocal EnableDelayedExpansion
set LF=^

set argC=0
for %%x in (%*) do Set /A argC+=1

set Help=Incorrect usage:^

To compile a program run the command^

"NanoMorpho.bat -c [full name of source file]"^

To run a compiled program run the command^

"NanoMorpho.bat [name of source file without extension]

if %argC% == 0 (
	echo !Help!
	goto :eof
)
if %argC% gtr 2 (
	echo !Help!
	goto :eof
)

if "%1%" == "-c" (
	java -cp bin Compiler %2%
	java -cp bin\morpho.jar -jar bin\morpho.jar -c %~n2.masm
	del %~n2.masm
) else (
	java -cp bin\morpho.jar -jar bin\morpho.jar %1%
)