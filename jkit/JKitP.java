package jkit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import jkit.bytecode.BytecodeFileWriter;
import jkit.bytecode.ClassFileReader;
import jkit.compiler.ClassLoader;

public class JKitP {
	public static final int MAJOR_VERSION = 0;
	public static final int MINOR_VERSION = 1;
	public static final int MINOR_REVISION = 0;
	
	public static void main(String[] args) {
		ArrayList<String> classPath = null;
		ArrayList<String> bootClassPath = null;
		boolean verbose = false;
		int fileArgsBegin = 0;
		for (int i = 0; i != args.length; ++i) {
			if (args[i].startsWith("-")) {
				String arg = args[i];
				if(arg.equals("-help")) {
					usage();
					System.exit(0);
				} else if (arg.equals("-version")) {
					System.out.println("JKitp, version " + MAJOR_VERSION + "."
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
				} 

				fileArgsBegin = i + 1;
			}
		}
		

		if (classPath == null) {
			classPath = buildClassPath();
		}
		if (bootClassPath == null) {
			bootClassPath = buildBootClassPath();
		}

		classPath.addAll(bootClassPath);
		
		try {
			BytecodeFileWriter bfw = new BytecodeFileWriter(System.out,
					new ClassLoader(classPath, null));

			for(int i=fileArgsBegin;i!=args.length;++i) {
				ClassFileReader cfr = new ClassFileReader(args[i]);
				bfw.write(cfr.readClass());
			}
		} catch(Exception e) {
			System.err.println("Error: " + e.getMessage());
			if (verbose) {
				e.printStackTrace(System.err);
			}
			System.exit(1);
		}
		
		System.exit(0);
	}

	/**
	 * Print out information regarding command-line arguments
	 * 
	 */
	public static void usage() {
		String[][] info = {
				{"version", "Print version information"},
				{"verbose","Print full information about classfile"}				
		};
		
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
	
	public static ArrayList<String> buildClassPath() {
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

	public static ArrayList<String> buildBootClassPath() {
		// Boot class path hasn't been overriden by user, so employ the
		// default option.
		ArrayList<String> bootClassPath = new ArrayList<String>();
		String bcp = System.getProperty("sun.boot.class.path");
		// split classpath along appropriate separator
		Collections.addAll(bootClassPath, bcp.split(File.pathSeparator));
		return bootClassPath;
	}
}
