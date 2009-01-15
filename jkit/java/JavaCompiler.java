package jkit.java;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import jkit.compiler.Compiler;
import jkit.compiler.SyntaxError;
import jkit.compiler.ClassLoader;
import jkit.java.stages.TypeChecking;
import jkit.java.stages.TypeResolution;
import jkit.java.stages.ScopeResolution;
import jkit.java.stages.TypePropagation;
import jkit.java.stages.TypeSystem;
import jkit.jil.*;
import jkit.util.Pair;

/**
 * A class compiler is responsible for compiling class files. It's one of the
 * most important classes in the system, and is built upon several other
 * important components. In particular, the ClassLoader is responsible for
 * locating source and class files from the classpath.
 * 
 * @author djp
 * 
 */
public class JavaCompiler implements Compiler {
	
	/**
	 * The class loader is needed to locate required types and classes in the
	 * file system.
	 */
	private ClassLoader loader;
		
	/**
     * The compilation queue is a list of files which are scheduled for
     * compilation. The purpose of the queue is simply to ensure that files to
     * not get compiled twice. For example, suppose we have files "C1.java" and
     * "C2.java" on the queue; we begin compiling "C1.java" and this in turn
     * forces the compilation of "C2.java"; then, when this is complete,
     * "C2.java" is automatically removed from the queue, thus preventing it
     * from being compiled again.
     */
	private ArrayList<String> compilationQueue = new ArrayList<String>(); 
	
	/**
     * The compiling set gives a full list of files which are currently being
     * compiled. This may be more than one if there are multiple compiles going
     * on in parallel. The reason for this is to prevent an infinite loop of
     * recursive compiles. [ALTHOUGH I THINK THERE MAYBE A BETTER WAY OF DOING
     * THIS]
     */
	private Set<String> compiling = new HashSet<String>();
	
	/**
	 * The output directory for class files.
	 */
	private File outputDirectory = null;
	
	/**
	 * The logout output stream is used to write log information about the
	 * status of compilation.  The default stream just discards everything.
	 */
	private PrintStream logout = new PrintStream(new OutputStream() {
		public void write(byte[] b) { /* don't do anything! */}
		public void write(byte[] b,int x, int y) { /* don't do anything! */}
		public void write(int x) { /* don't do anything! */}
	});
		
	/**
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 */	
	public JavaCompiler(List<String> classpath) {			
		this.loader = new ClassLoader(classpath,this);
	}
	
	/**
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 * @param logout
	 *            A stream where log messages are sent
	 */	
	public JavaCompiler(List<String> classpath, OutputStream logout) {
		this.loader = new ClassLoader(classpath, this, logout);
		this.logout = new PrintStream(logout);
	}
	
	/**
	 * @param sourcepath
	 *            a list of directory and/or jar file locations.
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 * @param logout
	 *            A stream where log messages are sent
	 */	
	public JavaCompiler(List<String> sourcepath, List<String> classpath, OutputStream logout) {
		this.loader = new ClassLoader(sourcepath, classpath, this, logout);
		this.logout = new PrintStream(logout);
	}		
	
	/**
	 * Get the ClassLoader being used by this class.
	 * @return
	 */
	public ClassLoader getClassLoader() {
		return loader;
	}
	
	/**
	 * Compile a list of classes in the file system, using the appropriate
	 * pipeline(s).
	 * 
	 * @param filenames
	 *            a list of the full names of the files to be compiled,
	 *            including their path expressed in the local host format (e.g.
	 *            using File.separatorChar's to indicate directories).
	 * @return
	 */
	public List<Clazz> compile(List<String> filenames) throws IOException, SyntaxError {
		compilationQueue.addAll(filenames);
		
		ArrayList<Clazz> classes = new ArrayList<Clazz>();
		
		while(!compilationQueue.isEmpty()) {
			classes.addAll(compile(compilationQueue.get(0)));
		}
		
		return classes;
	}
	
	/**
	 * Compile a class in the file system, using the appropriate pipeline.
	 * 
	 * @param filename
	 *            the full name of the file to be compiled, including it's path
	 *            expressed in the local host format (e.g. using
	 *            File.separatorChar's to indicate directories).
	 * @return
	 */
	public List<Clazz> compile(String filename) throws IOException, SyntaxError {		
		// first, check whether we are already compiling this file. If so,
        // return nothing [CAN WE DO BETTER THAN THIS?]
		if(compiling.contains(filename)) {			
			return new ArrayList<Clazz>();
		}
		compiling.add(filename);			
						
		try {
			long start = System.currentTimeMillis();
			
			// Now, construct the reader!
			JavaFileReader2 reader = new JavaFileReader2(filename);

			long last = System.currentTimeMillis();
			logout.println("Parsed " + filename + " [" + (last - start) + "ms]");
			
			// First we must read the skeletons
			JavaFile jfile = reader.read();
			new TypeResolution(loader, new TypeSystem()).apply(jfile);
			new ScopeResolution(loader, new TypeSystem()).apply(jfile);
			new TypePropagation(loader, new TypeSystem()).apply(jfile);
			new TypeChecking(loader, new TypeSystem()).apply(jfile);
			new JavaFileWriter(System.out).write(jfile);
			
			compilationQueue.remove(filename);
			compiling.remove(filename);
			
			return new ArrayList<Clazz>(); // to be completed			
		} catch(SyntaxError se) {
			throw new SyntaxError(se.msg(), filename, se.line(), se
					.column(), se.width(), se);			
		} 
	}	
}
