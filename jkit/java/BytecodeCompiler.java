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

package jkit.java;

import java.io.*;
import java.util.List;

import jkit.bytecode.BytecodeFileWriter;
import jkit.bytecode.ClassFile;
import jkit.jil.stages.ClassFileBuilder;
import jkit.jil.tree.JilClass;

public class BytecodeCompiler extends JavaCompiler {
	
	/**
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 */
	public BytecodeCompiler(List<String> classpath) {
		super(classpath);		
	}
	
	/**
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 * @param logout
	 *            A stream where log messages are sent
	 */
	public BytecodeCompiler(List<String> classpath, PrintStream logout) {
		super(classpath,logout);		
	}
	
	/**
	 * @param sourcepath
	 *            a list of directory and/or jar file locations.
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 * @param logout
	 *            A stream where log messages are sent
	 */
	public BytecodeCompiler(List<String> sourcepath, List<String> classpath,
			PrintStream logout) {
		super(sourcepath,classpath,logout);
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
		long start = System.currentTimeMillis();
		
		File outputFile = new File(rootdir, baseName + ".bytecode");		
		
		// now, ensure output directory and package directories exist.
		if(outputFile.getParentFile() != null) {
			outputFile.getParentFile().mkdirs();
		}

		OutputStream out = new FileOutputStream(outputFile);		
		ClassFile cfile = builder.build(clazz);
		
		logTimedMessage("[" + outputFile.getPath() + "] Bytecode generation completed",
				(System.currentTimeMillis() - start));	
		
		if(bytecodeOptimisationFlag) {
			start = System.currentTimeMillis();

			// this is where the bytecode optimisation would occur.
			int numRewrites = optimiser.optimise(cfile);		
			logTimedMessage("[" + outputFile.getPath() + "] Bytecode optimisation completed (" + numRewrites + " rewrites)",
					(System.currentTimeMillis() - start));	
		}
		
		start = System.currentTimeMillis();
				
		new BytecodeFileWriter(out,loader).write(cfile);		
		
		logTimedMessage("[" + outputFile.getPath() + "] Wrote " + outputFile.getPath(),
				(System.currentTimeMillis() - start));	
	}
}
