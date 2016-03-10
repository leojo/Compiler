import java.io.*;
import java.util.ArrayList;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;

public class Compiler{
	public static enum CodeType{
		NAME, ASSIGN, CALL, RETURN, UNARY, LITERAL, IF, WHILE, PRIORITY
	};
	private static boolean verbose = false;
	private static ArrayList<String> lines = new ArrayList<>();
	private static int nextLab = 1;
	
	private static int newLab()
	{
		return nextLab++;
	}
	
	private static void emit(String s){
		lines.add(s);
		if(verbose) System.out.println(s);
	}
	
	// CODE GENERATING FUNCTIONS
	
	private static void generateProgram(String name, Object[] intermediate){
		emit("\""+name+".mexe\" = main in");
		emit("!{{");
		for(int i=0; i<intermediate.length; i++){
			generateFunction((Object[])intermediate[i]);
		}
		emit("}}*BASIS;");
	}
	
	private static void generateFunction(Object[] intermediate){
		String fName = (String) intermediate[0];
		int numArgs = (int) intermediate[1];
		int numVars = (int) intermediate[2];
		emit("#\""+fName+"[f"+numArgs+"]\" =");
		emit("[");
		// Assign memory for the variables, leave the last on in accumulator
		if(numVars>0){
			emit("(MakeVal null)");
			for(int i=1; i<numVars; i++){
				emit("(MakeValP null)");
			}
			emit("(Push)");
		}
		for(int i=3; i<intermediate.length; i++){
			generateExpression((Object[])intermediate[i]);
		}
		// Make sure every function returns
		if(!lines.get(lines.size()-1).equals("(Return)")) emit("(Return)");
		emit("];");
	}
	
	// Before: The accumulator may be safely overwritten
	// After: the value of the expression is in the accumulator
	private static void generateExpression(Object[] intermediate){
		generateSmallExpression((Object[]) intermediate[0]);
		for(int i=1; i<intermediate.length-1; i+=2){
			String opName = (String) intermediate[i];
			emit("(Push)");
			generateSmallExpression((Object[]) intermediate[i+1]);
			emit("(Call #\""+opName+"[f2]\" 2)");
		}
	}
	
	// Before: The accumulator may be safely overwritten
	// After: the value of the expression is in the accumulator
	private static void generateSmallExpression(Object[] intermediate){
		CodeType ct = (CodeType)intermediate[0];
		switch(ct){
			case NAME:
				emit("(Fetch "+(int)intermediate[1]+")");
				return;
			case ASSIGN:
				generateExpression((Object[]) intermediate[2]);
				emit("(Store "+(int)intermediate[1]+")");
				return;
			case CALL:
				int numArgs = intermediate.length-2; // first two are CodeType and name
				if(numArgs>0) generateExpression((Object[])intermediate[2]);
				for(int i=3; i<intermediate.length; i++){
					emit("(Push)");
					generateExpression((Object[])intermediate[i]);
				}
				String fName = (String)intermediate[1];
				emit("(Call #\""+fName+"[f"+numArgs+"]\" "+numArgs+")");
				return;
			case RETURN:
				generateExpression((Object[])intermediate[1]);
				emit("(Return)");
				return;
			case UNARY:
				String opName = (String) intermediate[1];
				generateSmallExpression((Object[])intermediate[2]);
				emit("(Call #\""+opName+"[f1]\" 1)");
				return;
			case LITERAL:
				emit("(MakeVal "+(String)intermediate[1]+")");
				return;
			case IF:
				int elseLab = newLab();
				generateExpression((Object[])intermediate[1]);
				emit("(GoFalse _"+elseLab+")");
				generateBody((Object[])intermediate[2]);
				emit("_"+elseLab+":");
				// Deal with the elif's:
				int i=3;
				// while there are at least two more pieces of the
				// intermediate code do an elif.
				while(i<intermediate.length-1){
					elseLab = newLab();
					generateExpression((Object[])intermediate[i]);
					emit("(GoFalse _"+elseLab+")");
					generateBody((Object[])intermediate[i+1]);
					emit("_"+elseLab+":");
					i+=2;
				}
				//Deal with the else
				if(i<intermediate.length){
					generateBody((Object[])intermediate[i]);
				}
				return;
			case WHILE:
				int beforeLab = newLab();
				int afterLab = newLab();
				emit("_"+beforeLab+":");
				generateExpression((Object[])intermediate[1]);
				emit("(GoFalse _"+afterLab+")");
				generateBody((Object[])intermediate[2]);
				emit("(Go _"+beforeLab+")");
				emit("_"+afterLab+":");
				return;
			case PRIORITY:
				generateExpression((Object[])intermediate[1]);
				return;
			default:
				System.err.println("Unexpected error compiling smallExpression of type "+ct);
				System.exit(1);
		}
	}
	
	// Before: The accumulator may be safely overwritten
	// After: the value of the last expression is in the accumulator 
	private static void generateBody(Object[] intermediate){
		for(int i=0; i<intermediate.length; i++){
			if(i!=0) emit("(Push)");
			generateExpression((Object[]) intermediate[i]);
		}
	}
	
	
	
	public static void main(String[] args) throws Exception{
		Path file = null;
		Path outFile = null;
		if(args.length == 0){
			System.err.println("No file specified for compilation!");
			System.exit(1);
		}
		if(args.length > 2){
			String unknowns = "";
			boolean filePassed = false;
			for(String arg : args){
				if(!arg.equalsIgnoreCase("-v")){
					if(filePassed) unknowns += arg+", ";
					else filePassed = true;
				}
			}
			unknowns = unknowns.substring(0,unknowns.length()-2);
			System.err.println("Unrecognised arguments: "+unknowns+"\nUse '-v' for verbose mode.");
			System.exit(1);
		}
		if(args.length == 1){
			file = Paths.get(args[0]);
		} else {
			verbose = args[0].equalsIgnoreCase("-v");
			if(verbose){
				file = Paths.get(args[1]);
			} else {
				file = Paths.get(args[0]);
				verbose = (args.length>1 && args[1].equalsIgnoreCase("-v"));
				if(args.length>1 && !verbose){
					System.err.println("Unrecognised argument: "+args[1]+"\nUse '-v' for verbose mode.");
				}
			}
		}
		
		// COMPILER CODE HERE /////////////////////////////////////////////////////////////////////////
		Lexer lexer = null;
		try{
			lexer = new Lexer(new FileReader(file.toString()));
		} catch (FileNotFoundException e){
			System.err.println("Could not find file "+file.toString()+"! Make sure the path/spelling is correct.");
			System.exit(1);
		}
		String name = file.getFileName().toString();
		if(name.contains(".")) name = name.substring(0,name.lastIndexOf('.'));
		String outName = name+".masm";
		outFile = Paths.get(outName);
		
		Parser parser = new Parser(lexer,verbose);
		
		// Parse the program into intermediate code
		if(verbose) System.out.println("<Parsing program>\n");
		Object[] intermediate = parser.program();
		if(verbose) System.out.println("<Done parsing program>\n\n<Compiling program>\n");
		
		// Generate the program from the intermediate code
		generateProgram(name,intermediate);
		
		if(verbose) System.out.println("\n<Done compiling program>");
		
		////////////////////////////////////////////////////////////////////////////////////////////////
		
		if(outFile != null){
			// Finally write to the file
			Files.write(outFile, lines, Charset.forName("UTF-8"));
		}
	}
}