/**
	JFlex lesgreinir fyrir NanoMorpho.
	Höfundur: Leó Jóhannsson  Apríl 2016
				
	Smíðað eftir beinagrind fyrir NanoLisp eftir Snorra Agnarsson

	Þennan lesgreini má þýða með skipununum
		java -jar JFlex.jar nanomorpho.jflex
		javac Lexer.java
 */
%%

%public
%class Lexer
%unicode
%line
%column
%byaccj

%{

public Compiler yyparser;

public Lexer( java.io.Reader r, Compiler yyparser )
{
	this(r);
	this.yyparser = yyparser;
}

public int getLine(){
	return yyline;
}

public int getColumn(){
	return yycolumn;
}
%}

  /* Reglulegar skilgreiningar */

  /* Regular definitions */

_DIGIT=[0-9]
_FLOAT={_DIGIT}+\.{_DIGIT}+([eE][+-]?{_DIGIT}+)?
_INT={_DIGIT}+
_STRING=\"([^\"\\]|\\b|\\t|\\n|\\f|\\r|\\\"|\\\'|\\\\|(\\[0-3][0-7][0-7])|\\[0-7][0-7]|\\[0-7])*\"
_CHAR=\'([^\'\\]|\\b|\\t|\\n|\\f|\\r|\\\"|\\\'|\\\\|(\\[0-3][0-7][0-7])|(\\[0-7][0-7])|(\\[0-7]))\'
_DELIM=[()}{,;=]
_OPERATOR=[\+\-*/!%&><\:\^\~&|?=]+
_NAME=[:letter:]([:letter:]|[_]|{_DIGIT})*

%%

{_DELIM} {
	yyparser.yylval = new CompilerVal(yytext());
	return yycharat(0);
}

{_STRING} | {_FLOAT} | {_CHAR} | {_INT} | null | true | false {
	yyparser.yylval = new CompilerVal(yytext());
	return Compiler.LITERAL;
}

{_OPERATOR} {
	yyparser.yylval = new CompilerVal(yytext());
	return Compiler.OP;
}

"if" {
	return Compiler.IF;
}

"elsif" {
	return Compiler.ELSIF;
}

"else" {
	return Compiler.ELSE;
}

"while" {
	return Compiler.WHILE;
}

"var" {
	return Compiler.VAR;
}

"return" {
	return Compiler.RETURN;
}

{_NAME} {
	yyparser.yylval = new CompilerVal(yytext());
	return Compiler.NAME;
}

"#".*$ {
}

[ \t\r\n\f] {
}

. {
	return Compiler.YYERRCODE;
}