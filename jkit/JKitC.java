//This file is part of the Java Compiler Kit (JKit)

//The Java Compiler Kit is free software; you can 
//redistribute it and/or modify it under the terms of the 
//GNU General Public License as published by the Free Software 
//Foundation; either version 2 of the License, or (at your 
//option) any later version.

//The Java Compiler Kit is distributed in the hope
//that it will be useful, but WITHOUT ANY WARRANTY; without 
//even the implied warranty of MERCHANTABILITY or FITNESS FOR 
//A PARTICULAR PURPOSE.  See the GNU General Public License 
//for more details.

//You should have received a copy of the GNU General Public 
//License along with the Java Compiler Kit; if not, 
//write to the Free Software Foundation, Inc., 59 Temple Place, 
//Suite 330, Boston, MA  02111-1307  USA

//(C) David James Pearce, 2009. 

package jkit;

import java.io.*;
import java.util.*;

import jkit.bytecode.*;
import jkit.compiler.ClassLoader;
import jkit.compiler.SyntaxError;
import jkit.java.*;

/**
 * The main class provides the entry point for the JKit compiler. It is
 * responsible for parsing command-line parameters, configuring and executing
 * the pipeline and determining the name of the output file.
 * 
 * @author djp
 * 
 */
public class JKitC {

	public static final int MAJOR_VERSION = 0;
	public static final int MINOR_VERSION = 5;
	public static final int MINOR_REVISION = 1;
			
	/**
	 * Main method provides command-line processing capability.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {		
		if(!new JKitC().compile(args)) {	
			System.exit(1);
		} else {
			System.exit(0);
		}
	}
	
	public boolean compile(String[] args) {
		ArrayList<String> classPath = null;
		ArrayList<String> bootClassPath = null;
		ArrayList<String> sourcePath = null;
		String outputDirectory = null;		
		boolean verbose = false;
		boolean bytecodeOutput = false;
		boolean jilOutput = false;		
		boolean bytecodeOptimisation = true;
		boolean fieldLoadOptimisation = false;
		
		if (args.length == 0) {
			// no command-line arguments provided
			usage();
			System.exit(0);
		}
				
		// ======================================================
		// ======== First, parse command-line arguments ========
		// ======================================================

		int fileArgsBegin = 0;
		for (int i = 0; i != args.length; ++i) {
			if (args[i].startsWith("-")) {
				String arg = args[i];
				if(arg.equals("-help")) {
					usage();
					System.exit(0);
				} else if (arg.equals("-version")) {
					System.out.println("JKit Compiler, version " + MAJOR_VERSION + "."
							+ MINOR_VERSION + "." + MINOR_REVISION);
				} else if (arg.equals("-verbose")) {
					verbose = true;
				} else if (arg.equals("-cp") || arg.equals("-claspath")) {
					classPath = new ArrayList<String>();
					// split classpath along appropriate separator
					Collections.addAll(classPath, args[++i]
					                                   .split(File.pathSeparator));
				} else if (arg.equals("-sourcepath")) {
					sourcePath = new ArrayList<String>();
					// split classpath along appropriate separator
					Collections.addAll(sourcePath, args[++i]
					                                   .split(File.pathSeparator));
				} else if (arg.equals("-bootclaspath")) {
					bootClassPath = new ArrayList<String>();
					// split classpath along appropriate separator
					Collections.addAll(bootClassPath, args[++i]
					                                       .split(File.pathSeparator));
				} else if (arg.equals("-d")) {
					outputDirectory = args[++i];
				} else if (arg.equals("-bytecode")) {
					bytecodeOutput = true;
				} else if (arg.equals("-jil")) {
					jilOutput = true;
				} else if (arg.equals("-noopt")) {
					bytecodeOptimisation = false;
				} else if (arg.equals("-flo")) {
					fieldLoadOptimisation = true;
				} else {
					throw new RuntimeException("Unknown option: " + args[i]);
				}

				fileArgsBegin = i + 1;
			}
		}

		PrintStream verbOutput = null;
		
		if(verbose) {
			verbOutput = System.err;
		}
		
		// ======================================================
		// =========== Second, setup classpath properly ==========
		// ======================================================

		if (classPath == null) {
			classPath = ClassLoader.buildClassPath();
		}
		if (bootClassPath == null) {
			bootClassPath = ClassLoader.buildBootClassPath();
		}
		if(sourcePath == null) {
			sourcePath = new ArrayList<String>();
		}

		classPath.addAll(bootClassPath);
		
		try {
			JavaCompiler compiler;

			if(bytecodeOutput) {
				compiler = new BytecodeCompiler(sourcePath, classPath, verbOutput);	
			} else if(jilOutput) {
				compiler = new JilCompiler(sourcePath, classPath, verbOutput);
			} else {
				compiler = new JavaCompiler(sourcePath, classPath, verbOutput);
			}

			compiler.setBytecodeOptimisation(bytecodeOptimisation);
			compiler.setFieldLoadOptimisation(fieldLoadOptimisation);
			
			if (outputDirectory != null) {
				compiler.setOutputDirectory(new File(outputDirectory));
			}

			// ======================================================
			// ============== Third, load skeletons ================
			// ======================================================		


			List<File> srcfiles = new ArrayList<File>();
			for(int i=fileArgsBegin;i!=args.length;++i) {
				srcfiles.add(new File(args[i]));
			}
			compiler.compile(srcfiles);		
			compiler.flushCompilationQueue();
		} catch (SyntaxError e) {
			outputSourceError(e.fileName(), e.line(), e.column(), e.width(), e
					.getMessage());
			if (verbose) {
				e.printStackTrace(System.err);
			}
			return false;
		} catch(Exception e) {
			System.err.println("Error: " + e.getMessage());
			if (verbose) {
				e.printStackTrace(System.err);
			}
			return false;
		}
		
		return true;
	}
	
	/**
	 * Print out information regarding command-line arguments
	 * 
	 */
	public void usage() {
		String[][] info = {
				{"version", "Print version information"},
				{"verbose",
				"Print information about what the compiler is doing"},
				{"classpath <path>", "Specific where to find user class files"},
				{"cp <path>", "Specific where to find user class files"},
				{"bootclasspath <path>",
				"Specific where to find system class files"},
				{"jil","output jil intermediate representation"},
				{"bytecode","output bytecode in textual format"}};
		System.out.println("Usage: jkit <options> <source-files>");
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

	public static void outputSourceError(String fileArg, int line, int col,
			int width, String message) {
		System.err.println(fileArg + ":" + line + ": " + message);
		String l = readLine(fileArg, line);		
		if(l != null) {
			System.err.println(l);
			for (int j = 0; j < Math.min(col,l.length()); ++j) {
				if (l.charAt(j) == '\t') {
					System.err.print("\t");
				} else {
					System.err.print(" ");
				}
			}		
			for (int j = 0; j < width; ++j)
				System.err.print("^");
			System.err.println("");
		} else {
			// We shouldn't be able to get here. But, if there is a bug in jkit
			// itself, such that it attributes the wrong filename to the file
			// containing the error, then we can. So, it's helpful not to throw
			// an exception at this point ...
		}
	}

	public static String readLine(String f, int l) {
		try {
			return readLine(new FileReader(f), l);
		} catch (IOException e) {
			// shouldn't get here.
			return "";
		}
	}

	public static String readLine(Reader in, int l) {
		try {
			LineNumberReader lin = new LineNumberReader(in);
			String line = "";
			while (lin.getLineNumber() < l && line != null) {
				line = lin.readLine();
			}
			return line;
		} catch (IOException e) {
			return "";
		}
	}
}
