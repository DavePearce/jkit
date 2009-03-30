package jkit.java;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import jkit.jil.tree.Clazz;
import jkit.jil.io.*;

public class JilCompiler extends JavaCompiler {
	
	/**
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 */
	public JilCompiler(List<String> classpath) {
		super(classpath);		
	}
	
	/**
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 * @param logout
	 *            A stream where log messages are sent
	 */
	public JilCompiler(List<String> classpath, OutputStream logout) {
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
	public JilCompiler(List<String> sourcepath, List<String> classpath,
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
	public void writeOutputFile(File srcfile, Clazz clazz, File rootdir)
			throws IOException {
		long start = System.currentTimeMillis();
		
		String inf = srcfile.getPath();
		inf = inf.substring(0, inf.length() - 5); // strip off .java
		File outputFile = new File(rootdir, inf + ".jil");		
		
		// now, ensure output directory and package directories exist.
		if(outputFile.getParentFile() != null) {
			outputFile.getParentFile().mkdirs();
		}

		OutputStream out = new FileOutputStream(outputFile);		
		new JilFileWriter(out).write(clazz);	
		
		logTimedMessage("[" + srcfile.getPath() + "] Wrote " + outputFile.getPath(),
				(System.currentTimeMillis() - start));	
	}
}
