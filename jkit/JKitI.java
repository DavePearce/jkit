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

import jkit.util.*;
import jkit.compiler.ClassLoader;
import jkit.compiler.SyntaxError;
import jkit.java.*;
import jkit.jil.stages.*;
import jkit.jil.tree.*;


/**
 * The main class provides the entry point for the JKit compiler. It is
 * responsible for parsing command-line parameters, configuring and executing
 * the pipeline and determining the name of the output file.
 * 
 * @author djp
 * 
 */
public class JKitI {

	public static final int MAJOR_VERSION = 0;
	public static final int MINOR_VERSION = 5;
	public static final int MINOR_REVISION = 0;
			
	/**
	 * Main method provides command-line processing capability.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {		
		if(!new JKitI().compile(args)) {	
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
				} else {
					throw new RuntimeException("Unknown option: " + args[i]);
				}

				fileArgsBegin = i + 1;
			}
		}

		OutputStream verbOutput = null;
		
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
			final HashMap<String,List<Insert>> inserts = new HashMap();
			final StaticCallGraphBuilder cgBuilder = new StaticCallGraphBuilder();
			
			JavaCompiler compiler = new JavaCompiler(sourcePath, classPath, verbOutput) {
				public void variableDefinitions(File srcfile, JilClass jfile, ClassLoader loader) {
					super.variableDefinitions(srcfile,jfile,loader);
					cgBuilder.apply(jfile);
					new NonNullInference().apply(jfile);		
					
					// At this point, we want to compute the new inserts
					try {
						computeInserts(srcfile.getCanonicalPath(),jfile,inserts);
					} catch(IOException e) {					
					}
				}
				
				/**
				 * This is the final stage in the compilation pipeline --- we must write the
				 * output file somewhere.
				 * 
				 * @param jfile
				 * @param loader
				 */
				public void writeOutputFile(String baseName, JilClass clazz, File rootdir)
						throws IOException {
					// don't do anything here
				}
			};

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
			
			writeOutputFiles(srcfiles,inserts);
		} catch (SyntaxError e) {
			jkit.JKitC.outputSourceError(e.fileName(), e.line(), e.column(), e.width(), e
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

	public static class Insert implements Comparable<Insert> {
		public SourceLocation loc;
		public String text;
		
		public Insert(String text, SourceLocation loc) {
			this.text = text;
			this.loc = loc;
		}
		
		public int compareTo(Insert i) {
			if(loc.line() < i.loc.line()) {
				return -1;
			} else if(loc.line() > i.loc.line()) {
				return 1;
			} else if(loc.column() < i.loc.column()){
				return -1;
			} else if(loc.column() > i.loc.column()){
				return 1;
			}
			return 0;
		}
	}
	
	public static void computeInserts(String filename, JilClass jclass, HashMap<String,List<Insert>> insertmap) {
		List<Insert> inserts = insertmap.get(filename);
		if(inserts == null) {
			inserts = new ArrayList<Insert>();
			insertmap.put(filename,inserts);
		}
		
		for (JilMethod m : jclass.methods()) {			
			NonNullInference.Attr attr = (NonNullInference.Attr) m
					.attribute(NonNullInference.Attr.class);

			for (JilMethod.Parameter p : m.parameters()) {
				if (attr.nonnulls().contains(p.name())) {
					SourceLocation loc = (SourceLocation) p.attribute(SourceLocation.class);
					inserts.add(new Insert("@NonNull ",  loc));
				}
			}			
		}		
	}
	
	public static void writeOutputFiles(List<File> srcfiles,
			HashMap<String,List<Insert>> insertmap) throws IOException {
		for(File f : srcfiles) {
			String filename = f.getCanonicalPath();
			List<Insert> inserts = insertmap.get(filename);
			writeOutputFile(filename,inserts);
		}
	}
	
	public static void writeOutputFile(String filename, List<Insert> inserts) throws FileNotFoundException,IOException {
		Collections.sort(inserts);
		String outname = filename.substring(0,filename.length() - 5); // strip .java
		outname = outname + ".jout";
		System.out.println("Writing: " + outname);				
		LineNumberReader in = new LineNumberReader(new FileReader(filename));
		Writer out = new FileWriter(outname);
		String line = "";
		while (line != null) {
			line = in.readLine();
			int lineno = in.getLineNumber();	
			int offset = 0;
			while(inserts.size() > 0 && inserts.get(0).loc.line() == lineno) {				
				Insert i = inserts.get(0);		
				int ipos = i.loc.column() + offset;
				String before = line.substring(0,ipos);
				String after = line.substring(ipos);
				line = before + i.text + after;
				offset = offset + i.text.length();
				inserts.remove(0);
			}
			
			if(line != null) {
				out.write(line + "\n");
			}
		}		
		in.close();
		out.close();
	}	
}
