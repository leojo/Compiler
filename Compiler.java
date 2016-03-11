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
	
	// Before:	inter is an object array containing elements which meet
	//		the before criteria of the generateFunction method
	// After:	A morpho assembly code version of the program represented
	//		by inter is in lines, with the name <name>.mexe
	private static void generateProgram(String name, Object[] inter){
		emit("\""+name+".mexe\" = main in");
		emit("!{{");
		for(int i=0; i<inter.length; i++){
			generateFunction((Object[])inter[i]);
		}
		emit("}}*BASIS;");
	}


	// Before:	inter is an object array with the following internal structure
	//{String fname, Integer num_args, Integer num_vars, Object[]... exprInterCode}
	// After:	The morpho assembly code for the function fname has been written to
	//		the class variable lines.
	private static void generateFunction(Object[] inter){
		String fName = (String) inter[0];
		int numArgs = (int) inter[1];
		int numVars = (int) inter[2];
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
		for(int i=3; i<inter.length; i++){
			generateExpression((Object[])inter[i]);
		}
		// Make sure every function returns
		if(!lines.get(lines.size()-1).equals("(Return)")) emit("(Return)");
		emit("];");
	}
	

	// Before:	inter is an object array with the following internal structure
	// {Object[] smallExprInter, (String operator, Object[] smallExprInter)...}
	// After:	Morpho assembly code for the expression has been written to the class
	//		variable lines. The assembly code has the following Before/After:
	// Assembly Before:	The accumulator may be safely overwritten
	// Assembly After:	The value of the expression is in the accumulator
	private static void generateExpression(Object[] inter){
		generateSmallExpression((Object[]) inter[0]);
		for(int i=1; i<inter.length-1; i+=2){
			String opName = (String) inter[i];
			emit("(Push)");
			generateSmallExpression((Object[]) inter[i+1]);
			emit("(Call #\""+opName+"[f2]\" 2)");
		}
	}
	
	// Before:	inter is an object array with the following internal structure
	//		{CodeType codeType, Object... codeTypeSpecificObject}
	//		<A more detailed description is inside each switch statement>
	// After:	Morpho assembly code for the small expression has been written to
	//		the class variable lines.
	//		The assembly code has the following Before/After:
	// Assembly Before:	The accumulator may be safely overwritten
	// Assembly After:	The value of the small expression is in the accumulator
	private static void generateSmallExpression(Object[] inter){
		CodeType ct = (CodeType)inter[0];
		switch(ct){
			case NAME:
				//inter = {CodeType codeType, Integer varID}
				emit("(Fetch "+(int)inter[1]+")");
				return;
			case ASSIGN:
				//inter = {CodeType codeType, Integer varID, Object[] exprInter}
				generateExpression((Object[]) inter[2]);
				emit("(Store "+(int)inter[1]+")");
				return;
			case CALL:
				//inter = {CodeType codeType, String fname, Object[]... exprInter}
				int numArgs = inter.length-2; // first two are CodeType and name
				if(numArgs>0) generateExpression((Object[])inter[2]);
				for(int i=3; i<inter.length; i++){
					emit("(Push)");
					generateExpression((Object[])inter[i]);
				}
				String fName = (String)inter[1];
				emit("(Call #\""+fName+"[f"+numArgs+"]\" "+numArgs+")");
				return;
			case RETURN:
				//inter = {CodeType codeType, Object[] exprInter}
				generateExpression((Object[])inter[1]);
				emit("(Return)");
				return;
			case UNARY:
				//inter = {CodeType codeType, String operator, Object[] smallExprInter}
				String opName = (String) inter[1];
				generateSmallExpression((Object[])inter[2]);
				emit("(Call #\""+opName+"[f1]\" 1)");
				return;
			case LITERAL:
				//inter = {CodeType codeType, String literal}
				emit("(MakeVal "+(String)inter[1]+")");
				return;
			case IF:
				//inter = {CodeType codeType, Object[] exprInterIF, Object[] bodyInterIF,
				//			(Object[] exprInterELIF, Object[] bodyInterELIF)... , 
				//			(Object[] bodyInterELSE)? }
				int elseLab = newLab();
				generateExpression((Object[])inter[1]);
				emit("(GoFalse _"+elseLab+")");
				generateBody((Object[])inter[2]);
				emit("_"+elseLab+":");
				// Deal with the elif's:
				int i=3;
				// while there are at least two more pieces of the
				// intermediate code do an elif.
				while(i<inter.length-1){
					elseLab = newLab();
					generateExpression((Object[])inter[i]);
					emit("(GoFalse _"+elseLab+")");
					generateBody((Object[])inter[i+1]);
					emit("_"+elseLab+":");
					i+=2;
				}
				//Deal with the else
				if(i<inter.length){
					generateBody((Object[])inter[i]);
				}
				return;
			case WHILE:
				//inter = {CodeType codeType, Object[] exprInter, Object[] bodyInter}
				int beforeLab = newLab();
				int afterLab = newLab();
				emit("_"+beforeLab+":");
				generateExpression((Object[])inter[1]);
				emit("(GoFalse _"+afterLab+")");
				generateBody((Object[])inter[2]);
				emit("(Go _"+beforeLab+")");
				emit("_"+afterLab+":");
				return;
			case PRIORITY:
				// this only needs to be a code type to adhere to the standard
				// described in the before condition. The priority has been 
				// handled by the parser.

				//inter = {CodeType codeType, Object[] exprInter}
				generateExpression((Object[])inter[1]);
				return;
			default:
				System.err.println("Unexpected error compiling smallExpression of type "+ct);
				System.exit(1);
		}
	}
	

	// Before:	inter is an object array with the following internal structure
	//		{Object[]... exprInter}
	// After:	The morho assembly code for the body has been stored in the class
	//		variable lines. The assembly code has the following Before/After:
	// Assembly Before:	The accumulator may be safely overwritten
	// Assembly After: 	The value of the last expression executed is in the accumulator 
	private static void generateBody(Object[] inter){
		for(int i=0; i<inter.length; i++){
			if(i!=0) emit("(Push)");
			generateExpression((Object[]) inter[i]);
		}
	}
	
	
	
	public static void main(String[] args) throws Exception{
		Path file = null;
		Path outFile = null;

		// Handle arguments in a very crude way
		//(Couldn't be bothered to find and set up a decent args library)
		if(args.length == 0){
			// Too few arguments:
			System.err.println("No file specified for compilation!");
			System.exit(1);
		}
		if(args.length > 2){
			// Too many arguments:
			String unknowns = "";
			boolean filePassed = false;
			// Make some semblance of an error message
			//(May or may not be actually helpful)
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
			// Just the filename (I presume)
			file = Paths.get(args[0]);
		} else {
			// Here we need to check wether the case is "-v filename" or "filename -v":
			verbose = args[0].equalsIgnoreCase("-v");
			if(verbose){
				// "-v" was the first arg, so the second one should be the file.
				file = Paths.get(args[1]);
			} else {
				// "-v" was not the first arg, so the first arg should be the file
				file = Paths.get(args[0]);
				// The second arg should be "-v"
				verbose = args[1].equalsIgnoreCase("-v");
				if(!verbose){
					// If the second arg was not "-v" then something is wrong!
					System.err.println("Unrecognised argument: "+args[1]+"\nUse '-v' for verbose mode.");
					System.exit(1);
				}
			}
		}
		
		// COMPILER CODE HERE /////////////////////////////////////////////////////////////////////////

		// At this point verbose should accurately depict wether or not the user wants a verbose compilation
		// and file should contain the path to the file the user wants to compile.

		Lexer lexer = null;
		try{
			lexer = new Lexer(new FileReader(file.toString()));
		} catch (FileNotFoundException e){
			System.err.println("Could not find file "+file.toString()+"! Make sure the path/spelling is correct.");
			System.exit(1);
		}

		// get the proper name of the file without an extension (if there was any)
		String name = file.getFileName().toString();
		if(name.contains(".")) name = name.substring(0,name.lastIndexOf('.'));
		// Define an output file
		String outName = name+".masm";
		outFile = Paths.get(outName);
		
		Parser parser = new Parser(lexer,verbose);
		
		// Parse the program into intermediate code
		if(verbose) System.out.println("<Parsing program>\n");
		Object[] inter = parser.program();
		if(verbose) System.out.println("<Done parsing program>\n\n<Compiling program>\n");
		
		// Generate the program from the intermediate code
		generateProgram(name,inter);
		
		if(verbose) System.out.println("\n<Done compiling program>");
		
		////////////////////////////////////////////////////////////////////////////////////////////////
		
		// Finally write to the file
		Files.write(outFile, lines, Charset.forName("UTF-8"));
	}
}