package wone;

import java.io.*;

import wone.lang.*;
import wone.util.*;
import wone.solver.*;

/**
 * This class provides a simple text interface to the Solver, allowing simple
 * conditions to be checked for satisfiability.
 * 
 * @author djp
 * 
 */
public class Main {
	
	public static boolean checkUnsat(String input) {
		Parser parser = new Parser(input);
		Formula f = parser.readFormula();
		Solver solver = new Solver();
		return solver.checkUnsatisfiable(f);
	}
	
	public static void main(String[] args) {
		try {
			try {
				if(args.length == 0) {
					System.out.println("usage: java Solve <input-file>");
					System.exit(1);
				} 						
				Parser parser = new Parser(new File(args[0]));
				Formula f = parser.readFormula();
				System.out.println("Parsed: " + f);				
				
				Solver solver = new Solver();
				if(solver.checkUnsatisfiable(f)) {
					System.out.println("Unsatisfiable");
				} else {
					System.out.println("Satisfiable");
				}
							
			} catch(SyntaxError e) {				
				outputSourceError(e.filename(),e.start(),e.end(),e.getMessage());
			}
		} catch(IOException e) {
			System.err.println("i/o error: " + e.getMessage());
		}
	}	
	
	/**
	 * This method simply reads in the input file, and prints out a
	 * given line of text, with little markers (i.e. '^') placed
	 * underneath a portion of it.  
	 *
	 * @param fileArg - the name of the file whose line to print
	 * @param start - the start position of the offending region.
	 * @param end - the end position of the offending region.
	 * @param message - the message to print about the error
	 */
	public static void outputSourceError(String fileArg, int start, int end,
			String message) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(fileArg));
						
		int line = 0;
		String lineText = "";
		while (in.ready() && start >= lineText.length()) {
			start -= lineText.length() + 1;
			end -= lineText.length() + 1;
			lineText = in.readLine();						
			line = line + 1;			
		}		
								
		System.err.println(fileArg + ":" + line + ": " + message);
		System.err.println(lineText);	
		for (int i = 0; i <= start; ++i) {
			if (lineText.charAt(i) == '\t') {
				System.err.print("\t");
			} else {
				System.err.print(" ");
			}
		}				
		for (int i = start; i <= end; ++i) {		
			System.err.print("^");
		}
		System.err.println("");		
	}		
}
