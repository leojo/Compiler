To run this code, first simply compile all the java files.
Then write your own NanoMorpho code and store it in a file (say "name.extension")
Next run these commands: (replace name.extensions with your file name and extension respectively)
>java Compiler name.extension
>java -jar morpho.jar -c name.masm
>java -jar morpho.jar name

and Voilá! your code should have run.

Hint: To see the "inner workings" of the compiler and parser, run the first command with a "-v" argument
like so:
>java Compiler name.extension -v

(The order of the file name and the "-v" argument doesn't matter)