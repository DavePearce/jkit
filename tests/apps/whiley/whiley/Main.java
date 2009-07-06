package whiley;

import java.io.*;
import java.util.*;

import whiley.ast.*;
import whiley.ast.attrs.PostConditionAttr;
import whiley.ast.attrs.PreConditionAttr;
import whiley.ast.exprs.*;
import whiley.ast.stmts.*;
import whiley.ast.types.*;
import whiley.util.*;
import whiley.stages.*;
import wone.lang.Formula;
import wone.solver.*;

public class Main {
	public static final int MAJOR_VERSION = 0;
	public static final int MINOR_VERSION = 1;
	public static final int MINOR_REVISION = 0;
	
	public static boolean run(String[] args) {
		boolean writeProofs = false;
		boolean verbose = false;
		boolean nvc = false;
		
		if(args.length == 0) {
			System.err.println("usage: java Whiley <input file>");
			System.exit(1);
		}

		int fileArgsBegin = 0;
		for (int i = 0; i != args.length; ++i) {
			if (args[i].startsWith("-")) {
				String arg = args[i];
				if(arg.equals("-help")) {
					usage();
					System.exit(0);
				} else if (arg.equals("-version")) {
					System.out.println("Whiley, version " + MAJOR_VERSION + "."
							+ MINOR_VERSION + "." + MINOR_REVISION);
				} else if (arg.equals("-verbose")) {
					verbose = true;
				} else if (arg.equals("-proofs")) {
					writeProofs = true;
				} else if(arg.equals("-nvc")) {					
					nvc = true;
				} else {
					throw new RuntimeException("Unknown option: " + args[i]);
				}

				fileArgsBegin = i + 1;
			}
		}
		
		// Note.  The following pipeline is essentially the same as
		// that used in a typical compiler (e.g. javac).  It's a bit
		// simpler, of course!
	
		HashMap<String,Function> functions = new HashMap<String,Function>();
		ArrayList<WhileyFile> srcFiles = new ArrayList<WhileyFile>();
		
		try {
			try {		

				// =========================================================
				// First, attempt to parse the input file(s)
				// =========================================================

				for(int i=fileArgsBegin;i<args.length;++i) {
					long start = System.currentTimeMillis();			
					String file = args[i];
					WhileyParser wfr = new WhileyParser(file);			
					WhileyFile wf = wfr.read();				
					srcFiles.add(wf);

					for(Function f : wf.declarations()) {
						functions.put(f.getName(),f);
					}

					if (verbose) {
						logTimedMessage("[" + file + "] Parsing complete", System
								.currentTimeMillis()
								- start);
					}
				}

				// =========================================================
				// Second, bind invoke statements
				// =========================================================
				long start = System.currentTimeMillis();

				// Now, bind the functions
				for(Function f : functions.values()) {
					for(Stmt s : f.getStatements()) {
						s.bind(functions);
					}
				}		

				// =========================================================
				// Third, check for other (simple) syntactical errors 
				// =========================================================
				TypeChecker typeChecker = new TypeChecker();
				VarDefChecker varDefChecker = new VarDefChecker();
				
				for(WhileyFile wf : srcFiles) {
					for(Function f : wf.declarations()) {				
						start = System.currentTimeMillis();		
						typeChecker.verify(f);
						varDefChecker.verify(f);
						if(verbose) {
							logTimedMessage("[" + wf.filename() + "] Syntax checked " + f.getName() + "()",
									System.currentTimeMillis() - start);
						}						
					}
				}

				// =========================================================
				// Fourth, check that pre/post conditions are satisfied 
				// =========================================================
				
				if(!nvc) {
					Solver solver = new Solver();
					ConditionChecker verifier = new ConditionChecker(solver);

					for(WhileyFile wf : srcFiles) {
						for(Function f : wf.declarations()) {				
							start = System.currentTimeMillis();					
							verifier.verify(f);
							if (verbose) {
								logTimedMessage("[" + wf.filename()
										+ "] Verified " + f.getName() + "()",
										System.currentTimeMillis() - start);
							}				
						}
					}
				}
					
				// =========================================================
				// Fifth, write proofs (if requested)
				// =========================================================

				if(writeProofs) {
					for(WhileyFile wf : srcFiles) {
						start = System.currentTimeMillis();	
						String outName = stripExtension(wf.filename()) + ".proofs";				 
						PrintWriter wfo = new PrintWriter(new FileWriter(outName));				
						wfo.println("// Generated for " + wf.filename() + " on "
								+ new Date());				
						for(Function f : wf.declarations()) {
							wfo.println();
							writeProof(f,wfo);					
						}
						if(verbose) {
							logTimedMessage("[" + wf.filename() + "] Written " + outName,
									System.currentTimeMillis() - start);
						}
						wfo.close();
					}
				}

				// =========================================================
				// Sixth, evaluate program!
				// =========================================================

				Function mainFun = functions.get("main");
				
				if(mainFun != null) {
					Interpreter interp = new Interpreter();
					interp.evaluate(mainFun, new ArrayList<Object>());			
				} else {
					System.err.println("No main method found.");
				}
			} catch (SyntaxError e) {
				if(e.filename() != null) {
					outputSourceError(e.filename(), e.start(), e.end(), e.getMessage());
				}

				if(verbose) {
					e.printStackTrace(System.err);
				}

				// Now, attempt to write the offending proof file, if possible.
				for(WhileyFile wf : srcFiles) {
					if(wf.filename().equals(e.filename())) {
						String outName = stripExtension(wf.filename()) + ".err";				 
						PrintWriter wfo = new PrintWriter(new FileWriter(outName));				
						wfo.println("// Generated for " + wf.filename() + " on "
								+ new Date());				
						for(Function f : wf.declarations()) {
							wfo.println();
							writeProof(f,wfo);					
						}
						System.err.println("\nSee " + outName + " for more information.");
						wfo.close();
						break;
					}
				}

				return false;
			} catch (StuckError e) {
				if (e.filename() != null) {
					outputSourceError(e.filename(), e.start(), e.end(),
							"runtime error (" + e.getMessage() + ").");
				}

				if (verbose) {
					e.printStackTrace(System.err);
				}
			} 
		} catch(Exception e) {			
			System.err.println("Error: " + e.getMessage());
			if(verbose) {
				e.printStackTrace(System.err);
			}
			return false;
		}
		
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		if(!run(args)) {
			System.exit(1);
		} else {
			System.exit(0);
		}
	}
	
	/**
	 * Print out information regarding command-line arguments
	 * 
	 */
	public static void usage() {
		String[][] info = {
				{ "version", "Print version information" },
				{ "verbose",
						"Print detailed information on what the compiler is doing" },
				{ "proofs", "Write detailed proofs for each input file" },
				{ "nvc",
						"Don't using the condition verifier, so pre/post conditions are ignored" } };
		System.out.println("Usage: whiley <options> <source-files>");
		System.out.println("Options:");

		// first, work out gap information
		int gap = 0;

		for (String[] p : info) {
			gap = Math.max(gap, p[0].length() + 5);
		}

		// now, print the information
		for (String[] p : info) {
			System.out.print("  -" + p[0]);
			int rest = gap - p[0].length();
			for (int i = 0; i != rest; ++i) {
				System.out.print(" ");
			}
			System.out.println(p[1]);
		}
	}	
	
	static public void writeProof(Function f, PrintWriter output) {				
		output.print(f.getReturnType() + " " + f.getName() + "(");
		boolean firstTime=true;
		for(Pair<Type,String> p : f.getParameterTypes()) {
			if(!firstTime) {
				output.print(", ");
			}
			firstTime=false;
			output.print(p.first() + " " + p.second());
		}
		output.println(")");
		if(f.getPrecondition() != null) {
			output.print("requires " + f.getPrecondition());
			if(f.getPostcondition() != null) {
				output.print(", ");
			} 
		}
		if(f.getPostcondition() != null) {			
			output.println("ensures " + f.getPostcondition());
		}
		output.println("{");
		for(Stmt s : f.getStatements()) {							
			printProof(s,1,output);
		}
		output.println("}");
	}	
	
	static public void printProof(List<Stmt> trace, int level,PrintWriter output) {				
		for(Stmt s : trace) {							
			printProof(s,level+1,output);
		}				
	}	
	
	static public void printProof(Stmt s, int level,PrintWriter output) {
		PreConditionAttr attr = (PreConditionAttr)s.attribute(PreConditionAttr.class);
					
		if(attr != null) {
			Formula f = attr.preCondition(); 		
			indent(level,output);output.println("// " + f);
		} else {
			indent(level,output);output.println("...");
			return;
		}
		
		if (s instanceof Assign || s instanceof Return
				|| s instanceof Assertion || s instanceof Print || s instanceof VarDecl) {
			// Simple statements.						
			indent(level,output);output.println(s);
			if(s instanceof Return) {
				PostConditionAttr pattr = (PostConditionAttr) s
						.attribute(PostConditionAttr.class);
				if(pattr != null) {
					Formula f = pattr.postCondition(); 		
					indent(level,output);output.println("// " + f);
				} 	
			}
		} else if(s instanceof IfElse) {
			IfElse is = (IfElse) s;
			indent(level,output);output.println("if(" + is.getCondition() + ") {");
			printProof(is.getTrueBranch(),level,output);
			if(is.getFalseBranch() != null) {
				indent(level,output);output.println("} else {");
				printProof(is.getFalseBranch(),level,output);
			}			
			indent(level,output);output.println("}");
		} else if(s instanceof While) {
			While is = (While) s;
			indent(level,output);output.print("while(" + is.condition() + ")");
			if(is.invariant() != null) {
				output.print(" invariant " + is.invariant());
			}
			output.println(" {");
			printProof(is.body(),level,output);					
			indent(level,output);output.println("}");
		}
	}

	static public void indent(int level,PrintWriter output) {
		for(int i=0;i!=level;++i) {
			output.print("  ");
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
	
	/**
	 * This method is just a helper to format the output
	 */
	private static void logTimedMessage(String msg, long time) {
		System.err.print(msg);
		System.err.print(" ");

		String t = Long.toString(time);

		for (int i = 0; i < (80 - msg.length() - t.length()); ++i) {
			System.err.print(".");
		}
		System.err.print(" [");
		System.err.print(time);
		System.err.println("ms]");
	}
	
	private static String stripExtension(String input) {
		int li = input.lastIndexOf('.');
		if(li == -1) { return input; } // had no extension?
		else {
			return input.substring(0,li);
		}
	}
}
