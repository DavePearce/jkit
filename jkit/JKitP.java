// This file is part of the Java Compiler Kit (JKit)
//
// The Java Compiler Kit is free software; you can 
// redistribute it and/or modify it under the terms of the 
// GNU General Public License as published by the Free Software 
// Foundation; either version 2 of the License, or (at your 
// option) any later version.
//
// The Java Compiler Kit is distributed in the hope
// that it will be useful, but WITHOUT ANY WARRANTY; without 
// even the implied warranty of MERCHANTABILITY or FITNESS FOR 
// A PARTICULAR PURPOSE.  See the GNU General Public License 
// for more details.
//
// You should have received a copy of the GNU General Public 
// License along with the Java Compiler Kit; if not, 
// write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA
//
// (C) David James Pearce, 2009. 

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
			classPath = ClassLoader.buildClassPath();
		}
		if (bootClassPath == null) {
			bootClassPath = ClassLoader.buildBootClassPath();
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
	
}
