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

//(C) David James Pearce, 2007. 

package jkit;

import java.io.*;
import java.util.*;

import jkit.bytecode.ClassFileWriter;
import jkit.core.*;
import jkit.core.ClassCompiler.Pipeline;
import jkit.io.*;
import jkit.stages.*;

/**
 * The main class provides the entry point for the JKit compiler. It is
 * responsible for parsing command-line parameters, configuring and executing
 * the pipeline and determining the name of the output file.
 * 
 * @author djp
 * 
 */
public class Main {

	public static final int MAJOR_VERSION = 0;
	public static final int MINOR_VERSION = 4;
	public static final int MINOR_REVISION = 0;
		
	/**
	 * This provides global control over the target set
	 */
	public static final HashMap<String, Class<?>> targets = new HashMap<String, Class<?>>();
	static{
		// default targets
		targets.put("class", ClassFileWriter.class);
		targets.put("jkil", JKilWriter.class);
		targets.put("java", JavaFileWriter.class);
	}
	
	/**
	 * This provides global control over the pipelines
	 */
	public static final HashMap<String, ClassCompiler.Pipeline> pipelines = new HashMap<String, ClassCompiler.Pipeline>();	
	static{
		// default pipelines
		pipelines.put("java", new Pipeline(JavaFileReader.class,
				ClassFileWriter.class,
				"class",
				new jkit.stages.codegen.FieldInitialisation(),
				new jkit.stages.codegen.AnonClass(),
				new jkit.stages.codegen.BypassMethods(),
				new jkit.stages.codegen.Typing(),
				new jkit.stages.codegen.SwitchConstants(),
				new jkit.stages.codegen.ForEachLoop(),
				new jkit.stages.codegen.Exceptions(),
				new jkit.stages.checks.Subtyping(),
				new jkit.stages.checks.VariableDefinitions()));
	}

	/**
	 * Main method provides command-line processing capability.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if(!new Main().compile(args)) {	
			System.exit(1);
		} else {
			System.exit(0);
		}
	}
	
	public boolean compile(String[] args) {
		ArrayList<String> classPath = null;
		ArrayList<String> bootClassPath = null;

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
					System.out.println("JKit, version " + MAJOR_VERSION + "."
							+ MINOR_VERSION + "." + MINOR_REVISION);
				} else if (arg.equals("-verbose")) {
					verbose = true;
				} else if (arg.equals("-cp") || arg.equals("-claspath")) {
					classPath = new ArrayList<String>();
					// split classpath along appropriate separator
					Collections.addAll(classPath, args[++i]
					                                   .split(File.pathSeparator));
				} else if (arg.equals("-bootclaspath")) {
					bootClassPath = new ArrayList<String>();
					// split classpath along appropriate separator
					Collections.addAll(bootClassPath, args[++i]
					                                       .split(File.pathSeparator));
				} else if (arg.equals("-ls") || arg.equals("-liststages")) {
					listStages(pipelines);
				} else if (arg.equals("-is") || arg.equals("-ignorestage")) {
					String stage = args[++i];
					ignoreStage(stage, pipelines);
				} else if (arg.equals("-ss") || arg.equals("-stopstage")) {
					String stage = args[++i];
					stopStage(stage, pipelines);
				} else if (arg.equals("-t") || arg.equals("-target")) {
					String target = args[++i];
					if (!targets.containsKey(target)) {
						System.err.println("Unregistered target filetype \""
								+ target + "\"");
						return false;
					}
					retargetPipelines(targets.get(target), target, pipelines);
				} else if (arg.equals("-lt") || arg.equals("-listtargets")) {
					listTargets(targets);
				} else {
					throw new RuntimeException("Unknown option: " + args[i]);
				}

				fileArgsBegin = i + 1;
			}
		}

		// ======================================================
		// =========== Second, setup classpath properly ==========
		// ======================================================

		if (classPath == null) {
			classPath = buildClassPath();
		}
		if (bootClassPath == null) {
			bootClassPath = buildBootClassPath();
		}

		classPath.addAll(bootClassPath);
		ClassCompiler compiler;
		
		if(verbose) {
			compiler = new ClassCompiler(pipelines,classPath,System.err);	
		} else {
			compiler = new ClassCompiler(pipelines,classPath);
		}

		ClassTable.setLoader(compiler.getClassLoader());
		
		// ======================================================
		// ============== Third, load skeletons ================
		// ======================================================		
		
		try {
			compiler.compile(Arrays.asList(args).subList(fileArgsBegin,
					args.length));
		} catch (SyntaxError e) {
			outputSourceError(e.fileName(), e.line(), e.column(), e.width(), e
					.getMessage());
			if (verbose) {
				e.printStackTrace(System.err);
			}
			return false;
		} catch (InternalException e) {
			FlowGraph.Point point = e.point();
			outputSourceError(point.source(), point.line(), point.column(), 3, e.getMessage());
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
				{"ls [pipeline]",
				"List stages of pipeline.  If none given, list for all pipelines."},
				{"liststages [pipeline]",
				"List stages of pipeline.  If none given, list for all pipelines."},
				{"is <stage name> [pipeline]",
				"Ignore pipeline stage.  If no pipeline supplied, then apply to all pipelines]"},
				{"ignorestage <stage name> [pipeline]",
				"Ignore pipeline stage.  If no pipeline supplied, applies to all pipelines]"},
				{
					"ss <stage name> [pipeline]",
				"Stop after pipeline stage.  If no pipeline supplied, applies to all pipelines]"},
				{
					"stopstage <stage name> [pipeline]",
				"Stop after pipeline stage.  If no pipeline supplied, applies to all pipelines]"},
				{"t <filetype>",
				"Output to file of given filetype, using list of registered filetypes"},
				{"-target <filetype>",
				"Output to file of given filetype, using list of registered filetypes"},
				{"-lt", "Print list of registered filetypes."},
				{"-listtargets <filetype>",
				"Print list of registered filetypes."},};
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

	/**
	 * List pipeline stages for each pipeline.
	 * 
	 * @param pipelines
	 */
	public void listStages(Map<String, ClassCompiler.Pipeline> pipelines) {
		for (Map.Entry<String, ClassCompiler.Pipeline> e : pipelines.entrySet()) {
			System.out.println("*." + e.getKey() + ":");
			ClassCompiler.Pipeline p = e.getValue();
			System.out.println(" >> " + p.fileReader.getName());
			for (Stage s : p.stages) {
				System.out.println("    " + s.getClass().getName() + "\t[" + s.description() + "]");
			}
			System.out.println(" << " + p.fileWriter.getName());
		}
	}

	/**
	 * Remove all stages after given stage in each pipeline.
	 * 
	 * @param stage
	 * @param pipelines
	 */
	public void stopStage(String stage, Map<String, ClassCompiler.Pipeline> pipelines) {
		// cut every pipeline short
		for (Map.Entry<String, ClassCompiler.Pipeline> e : pipelines.entrySet()) {
			ClassCompiler.Pipeline p = e.getValue();
			for (int i = 0; i != p.stages.length; ++i) {
				String stageName = p.stages[i].getClass().getName();
				if (stageName.contains(stage)) {
					Stage[] nStages = new Stage[i + 1];
					System.arraycopy(p.stages, 0, nStages, 0, i + 1);
					p.stages = nStages;
					break;
				}
			}
		}
	}

	/**
	 * Remove single stage from each pipeline.
	 * 
	 * @param stage
	 * @param pipelines
	 */
	public void ignoreStage(String stage, Map<String, ClassCompiler.Pipeline> pipelines) {
		// cut every pipeline short
		for (Map.Entry<String, ClassCompiler.Pipeline> e : pipelines.entrySet()) {
			ClassCompiler.Pipeline p = e.getValue();
			for (int i = 0; i != p.stages.length; ++i) {
				String stageName = p.stages[i].getClass().getName();
				if (stageName.contains(stage)) {
					Stage[] nStages = new Stage[p.stages.length - 1];
					System.arraycopy(p.stages, 0, nStages, 0, i);
					if (i < nStages.length) {
						System.arraycopy(p.stages, i + 1, nStages, i,
								nStages.length - i);
					}
					p.stages = nStages;
					break;
				}
			}
		}
	}

	/**
	 * Print list of registered filetype targets
	 * 
	 * @param targets
	 *            List of registered filetype targets
	 */
	public void listTargets(HashMap<String, Class<?>> targets) {
		System.out.println("Registered target filetypes:");
		for (Map.Entry<String, Class<?>> e : targets.entrySet()) {
			System.out.println("  " + e.getKey() + " => "
					+ e.getValue().getName());
		}
	}

	/**
	 * Retarget all pipelines to use alternative ClassWriter
	 * 
	 * @param writer
	 *            Class object representing class writer in question.
	 * @param pipelines
	 *            Set of pipelines
	 */
	public void retargetPipelines(Class<?> writer, String target,
			Map<String, ClassCompiler.Pipeline> pipelines) {
		// cut every pipeline short
		for (Map.Entry<String, ClassCompiler.Pipeline> e : pipelines.entrySet()) {
			Pipeline p = e.getValue();
			p.fileWriter = writer;
			p.target = target;
		}
	}

	public ArrayList<String> buildClassPath() {
		// Classpath hasn't been overriden by user, so import
		// from the environment.
		ArrayList<String> classPath = new ArrayList<String>();
		String cp = System.getenv("CLASSPATH");
		if (cp == null) {
			System.err
			.println("Warning: CLASSPATH environment variable not set");
		} else {
			// split classpath along appropriate separator
			Collections.addAll(classPath, cp.split(File.pathSeparator));
		}
		return classPath;
	}

	public ArrayList<String> buildBootClassPath() {
		// Boot class path hasn't been overriden by user, so employ the
		// default option.
		ArrayList<String> bootClassPath = new ArrayList<String>();
		String bcp = System.getProperty("sun.boot.class.path");
		// split classpath along appropriate separator
		Collections.addAll(bootClassPath, bcp.split(File.pathSeparator));
		return bootClassPath;
	}

	public void outputSourceError(String fileArg, int line, int col,
			int width, String message) {
		System.err.println(fileArg + ":" + line + ": " + message);
		String l = readLine(fileArg, line);
		System.err.println(l);
		for (int j = 0; j < col; ++j) {
			if (l.charAt(j) == '\t') {
				System.err.print("\t");
			} else {
				System.err.print(" ");
			}
		}
		for (int j = 0; j < width; ++j)
			System.err.print("^");
		System.err.println("");
	}

	public String readLine(String f, int l) {
		try {
			return readLine(new FileReader(f), l);
		} catch (IOException e) {
			// shouldn't get here.
			return "";
		}
	}

	public String readLine(Reader in, int l) {
		try {
			LineNumberReader lin = new LineNumberReader(in);
			String line = "";
			while (lin.getLineNumber() < l) {
				line = lin.readLine();
			}
			return line;
		} catch (IOException e) {
			return "";
		}
	}
}
