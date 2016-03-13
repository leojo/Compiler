import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by leo on 17.2.2016.
 *
 * Parser for the nanoMorpho language as defined by the syntax chart
 */
public class Parser {

    private static Lexer lexer;
    private static Yytoken next_token;
    private static boolean verbose;
    private static HashMap<String, Integer> variables;
    private static int nextVarNum;
    
    static public final int NAME     = 0;
    static public final int VAR      = 1;
    static public final int RETURN   = 2;
    static public final int OPERATOR = 3;
    static public final int LITERAL  = 4;
    static public final int IF       = 5;
    static public final int ELIF     = 6;
    static public final int ELSE     = 7;
    static public final int WHILE    = 8;

    static public final int ERR      = -1;
    static public final int EOF      = -2;
    
    public Parser(Lexer l, boolean v){
        lexer = l;
        verbose = v;
        advance();
    }
    
    public static void advance(){    
        try{
            next_token = lexer.yylex();
        }
        catch (IOException e){
            throw new Error(e);
        }
        if( next_token == null ){
            next_token = new Yytoken(EOF,"EOF");
        }
        if( next_token.number == ERR ){
            System.err.println("Unexpected symbol : "+next_token+" (line: "+(lexer.getLine()+1)+", column: "+lexer.getColumn()+")");
            System.exit(1);
        }
    }

    private static String expectedType(int i){
        String expectedType;
        switch (i){
            case VAR: expectedType = "'var'";
                break;
            case NAME: expectedType = "<NAME>";
                break;
            case RETURN: expectedType = "'return'";
                break;
            case LITERAL: expectedType = "<LITERAL>";
                break;
            case WHILE: expectedType = "'while'";
                break;
            case OPERATOR: expectedType = "<OPERATOR>";
                break;
            case IF: expectedType = "'if'";
                break;
            case ELIF: expectedType = "'elsif'";
                break;
            case ELSE: expectedType = "'else'";
                break;
            case EOF: expectedType = "<EOF>";
                break;
            case ERR: expectedType = "<ERR>";
                break;
            default: expectedType = "'"+((char)i)+"'";
                break;
        }
        return expectedType;
    }
    
    private static void expected( String exp ){
        System.err.println("Expected "+exp+", found "+next_token.string+" (line: "+(lexer.getLine()+1)+", column: "+lexer.getColumn()+")");
        System.exit(1);
    }

    private static String expect(int i) {
        String lexeme = next_token.toString();
        if(!look(i)) {
            String expectedType = expectedType(i);
            expected(expectedType);
        }
        advance();
        return lexeme;
    }

    private static void expect(char c){
        expect((int)c);
    }

    private static void expect(String s){
        for(char c : s.toCharArray()){
            expect(c);
        }
    }
    
    private static boolean look(int i){
        return next_token.number == i;
    }
    
    private static boolean look(char c){
        return look((int)c);
    }

    private static boolean matches(int a, int... b){
        for(int i : b){
            if(a==i){
                return true;
            }
        }
        return false;
    }

    private static void verbose(int indent, String s){
        if(!verbose) return;
        String spaces = "";
        for (int i = 0; i < indent; i++) {
            spaces += ". ";
        }
        System.out.println(spaces+s);
    }
    
    // VARIABLE STORE FUNCTIONS:
    
    private static void resetVariableStore(){
        variables = new HashMap<>();
        nextVarNum = 0;
    }
    
    private static void registerVariable(String varName){
        if(variables.containsKey(varName)){
            System.err.println("The variable name "+varName+" is already being used. (line: "+(lexer.getLine()+1)+", column: "+lexer.getColumn()+")");
            System.exit(1);
        }
        variables.put(varName,nextVarNum++);
    }
    
    private static int getVarNum(String varName){
        return variables.get(varName);
    }

    // PARSER STARTS HERE:

    public static Object[] program(){return program(0);}

    private static Object[] program(int level){
        ArrayList<Object> list = new ArrayList<>();
        verbose(level, "<program>");
        do{
            list.add(function(level+1));
        }while(!look(EOF));
        return list.toArray();
    }

    private static Object[] function(int level){
        ArrayList<Object> list = new ArrayList<>();
        resetVariableStore();
        
        verbose(level,"<function>");
        list.add(expect(NAME));
        expect('(');
        int argsCount = 0;
        if(!look(')')){
            registerVariable(expect(NAME));
            argsCount++;
            while(look(',')){
                advance();
                registerVariable(expect(NAME));
                argsCount++;
            }
        }
        list.add(argsCount);
        expect("){");
        int varCount = 0;
        while(look(VAR)){
            varCount += decl(level+1);
            expect(';');
        }
        list.add(varCount);
        do{
            list.add(expr(level+1));
            expect(';');
        }while(!look('}'));
        expect('}');
        return list.toArray();
    }

    private static int decl(int level){
        int varCount = 0;
        verbose(level,"<decl>");
        expect(VAR);
        registerVariable(expect(NAME));
        varCount++;
        while(look(',')){
            advance();
            registerVariable(expect(NAME));
            varCount++;
        }
        return varCount;
    }
    
    private static Object[] expr(int level){
        ArrayList<Object> list = new ArrayList<>();
        verbose(level,"<expr>");
        list.add(smallExpr(level+1));
        while(look(OPERATOR)){
            list.add(expect(OPERATOR));
            list.add(smallExpr(level+1));
        }
        return list.toArray();
    }

    private static Object[] smallExpr(int level){
        ArrayList<Object> list = new ArrayList<>();
        verbose(level,"<smallExpr>");
        level++;
        if( look(NAME) ){
            String name = expect(NAME);
            if( look('=') ){
                verbose(level,"<NAME> = <expr>");
                advance();
                list.add(Compiler.CodeType.ASSIGN);
                list.add(getVarNum(name));
                list.add(expr(level+1));
                return list.toArray();
            }
            if( !look('(') ){
                verbose(level,"<NAME>");
                list.add(Compiler.CodeType.NAME);
                list.add(getVarNum(name));
                return list.toArray();
            }
            verbose(level,"<NAME>(<expr>...)");
            list.add(Compiler.CodeType.CALL);
            list.add(name);
            advance();
            if( look(')') ){
                advance();
                return list.toArray();
            }
            list.add(expr(level+1));
            while( look(',') ){
                advance();
                list.add(expr(level+1));
            }
            expect(')');
            return list.toArray();
        }
        if( look(RETURN) ){
            verbose(level,"return <expr>");
            list.add(Compiler.CodeType.RETURN);
            advance();
            list.add(expr(level+1));
            return list.toArray();
        }
        if( look(OPERATOR) ){
            verbose(level,"<OPERATOR> <smallExpr>");
            list.add(Compiler.CodeType.UNARY);
            list.add(expect(OPERATOR));
            list.add(smallExpr(level+1));
            return list.toArray();
        }
        if( look(LITERAL) ){
            verbose(level,"<LITERAL>");
            list.add(Compiler.CodeType.LITERAL);
            list.add(next_token.toString());
            advance();
            return list.toArray();
        }
        if( look('(') ){
            verbose(level,"(<expr>)");
            list.add(Compiler.CodeType.PRIORITY);
            advance();
            list.add(expr(level+1));
            expect(')');
            return list.toArray();
        }
        if( look(IF) ){
            list.add(Compiler.CodeType.IF);
            verbose(level,"<ifexpr>");
            expect(IF);
            expect('(');
            list.add(expr(level+1));
            expect(')');
            list.add(body(level+1));
            while( look(ELIF) ){
                advance();
                expect('(');
                list.add(expr(level+1));
                expect(')');
                list.add(body(level+1));
            }
            if( look(ELSE) ){
                advance();
                list.add(body(level+1));
            }
            return list.toArray();
        }
        if( look(WHILE) ){
            list.add(Compiler.CodeType.WHILE);
            verbose(level,"<while>");
            advance();
            expect('(');
            list.add(expr(level+1));
            expect(')');
            list.add(body(level+1));
            return list.toArray();
        }
        expected("expression");
        return null;
    }

    private static Object[] body(int level){
        ArrayList<Object> list = new ArrayList<>();
        
        verbose(level, "<body>");
        expect('{');
        do{
            list.add(expr(level+1));
            expect(';');
        } while(!look('}'));
        advance();
        
        return list.toArray();
    }

    public static void main(String[] args) {
        try {
            lexer = new Lexer(new FileReader(args[0]));
        } catch (FileNotFoundException e) {
            System.err.println("Could not read file "+args[0]+" , check if path is correct!");
            System.exit(1);
        }
        
        verbose = (args.length>1 && args[1].equalsIgnoreCase("-v"));
        if(args.length>1 && !verbose){
            System.out.println("Invalid option: "+args[1]+"\nUse '-v' for verbose mode.");
        }
        advance();
        program();
        if( next_token.number != EOF ){
            System.err.println("Expected EOF, found " + next_token+" (line: "+(lexer.getLine()+1)+")");
            System.exit(1);
        }
        System.out.println("Program parsed successfully");
    }
}
