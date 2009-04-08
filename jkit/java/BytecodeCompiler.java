package jkit.java;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
	public BytecodeCompiler(List<String> classpath, OutputStream logout) {
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
			OutputStream logout) {
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
		ClassFile cfile = new ClassFileBuilder(loader,49).build(clazz);
		
		logTimedMessage("[" + outputFile.getPath() + "] Bytecode generation completed",
				(System.currentTimeMillis() - start));	
		
		start = System.currentTimeMillis();
		
		// this is where the bytecode optimisation would occur.
		
		logTimedMessage("[" + outputFile.getPath() + "] Bytecode optimisation completed",
				(System.currentTimeMillis() - start));	
		
		start = System.currentTimeMillis();
		
		new BytecodeFileWriter(out).write(cfile);		
		
		logTimedMessage("[" + outputFile.getPath() + "] Wrote " + outputFile.getPath(),
				(System.currentTimeMillis() - start));	
	}
}
