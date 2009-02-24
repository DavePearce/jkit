package jkit.java;

import java.io.*;
import java.util.*;

import jkit.compiler.Compiler;
import jkit.compiler.SyntaxError;
import jkit.compiler.ClassLoader;
import jkit.java.io.JavaFile;
import jkit.java.io.JavaFileReader;
import jkit.java.io.JavaFileWriter;
import jkit.java.stages.SkeletonDiscovery;
import jkit.java.stages.SkeletonBuilder;
import jkit.java.stages.TypeChecking;
import jkit.java.stages.TypeResolution;
import jkit.java.stages.ScopeResolution;
import jkit.java.stages.TypePropagation;
import jkit.java.stages.TypeSystem;
import jkit.jil.*;

/**
 * A Java compiler is responsible for compiling Java source files into class
 * files. It's one of the most important classes in the system, and is built
 * upon several other important components. In particular, the ClassLoader is
 * responsible for locating source and class files from the classpath.
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
		this.loader = new ClassLoader(classpath, this);
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
		this.loader = new ClassLoader(sourcepath, classpath, this);
		this.logout = new PrintStream(logout);
	}		
	
	/**
	 * Get the ClassLoader being used by this class.
	 * @return
	 */
	public ClassLoader getClassLoader() {
		return loader;
	}
	
	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}
	
	/**
	 * The purpose of this method is to indicate that a source file is currently
	 * being compiled.
	 */
	public boolean isCompiling(File sfile)  {		
		try {			
			return compiling.contains(sfile.getCanonicalPath());
		} catch (IOException e) {
			return false;
		}
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
	public List<Clazz> compile(List<File> filenames) throws IOException, SyntaxError {
		for(File f : filenames) {
			compilationQueue.add(f.getPath());
		}
		
		ArrayList<Clazz> classes = new ArrayList<Clazz>();
		
		while(!compilationQueue.isEmpty()) {
			classes.addAll(compile(new File(compilationQueue.get(0))));
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
	public List<Clazz> compile(File filename) throws IOException, SyntaxError {				
		compilationQueue.remove(filename.getPath());
		compiling.add(filename.getCanonicalPath());			
						
		try {
			// First, parse the Java source file to yield an abstract syntax
			// tree.
			long start = System.currentTimeMillis();						
			JavaFile jfile = parseSourceFile(filename);
			logTimedMessage("[" + filename.getPath() + "] Parsing completed ",(System.currentTimeMillis() - start));
			
			// Second, we need to resolve types. That is, for each class
			// reference type, determine what package it's in.				
			start = System.currentTimeMillis();
			List<Clazz> skeletons = discoverSkeletons(jfile,loader);			
			logTimedMessage("[" + filename.getPath() + "] Skeleton discovery completed",(System.currentTimeMillis()-start));
			
			// Third, we need to resolve all types found in the src file.
			start = System.currentTimeMillis();
			resolveTypes(jfile,loader);					
			logTimedMessage("[" + filename.getPath() + "] Type resolution completed",(System.currentTimeMillis()-start));
			
			// Fourth, we need to build the skeletons of the classes. This is
			// necessary to resolve the scope of a particular variable.
			// Specifically, during scope resolution, we need to be able to:
			// 1) traverse the class heirarchy
			// 2) determine what fields are declared.			
			start = System.currentTimeMillis();
			buildSkeletons(jfile,loader);			
			logTimedMessage("[" + filename.getPath() + "] Skeleton construction completed",(System.currentTimeMillis()-start));
						
			// Fifth, perform the scope resolution itself. The aim here is, for
			// each variable access, to determine whether it is a local
			// variable access, an inherited field access, an enclosing field
			// access, or an access to a local variable in an enclosing scope
			// (e.g. for anonymous inner classes).
			start = System.currentTimeMillis();
			resolveScopes(jfile,loader);			
			logTimedMessage("[" + filename.getPath() + "] Scope resolution completed",(System.currentTimeMillis()-start));
			
			// Sixth, propagate the type information throughout all expressions
			// in the class file, including those in the method bodies and field
			// initialisers.
			start = System.currentTimeMillis();
			propagateTypes(jfile,loader);			
			logTimedMessage("[" + filename.getPath() + "] Type propagation completed",(System.currentTimeMillis()-start));
			
			// Seventh, check whether the types are being used correctly. If not,
			// report a syntax error.
			start = System.currentTimeMillis();
			checkTypes(jfile,loader);
			logTimedMessage("[" + filename.getPath() + "] Type checking completed",(System.currentTimeMillis()-start));
			
			// Eigth, write out the compiled class file.
			start = System.currentTimeMillis();
			String outFile = writeOutputFile(jfile, filename);			
			logTimedMessage("[" + filename.getPath() + "] Wrote " + outFile,(System.currentTimeMillis()-start));
			
			compiling.remove(filename);
			
			return skeletons; // to be completed			
		} catch(SyntaxError se) {
			if(se.fileName() == null) {
				throw new SyntaxError(se.msg(), filename.getPath(), se.line(), se
					.column(), se.width(), se);
			} else {
				throw se;
			}
		} 
	}
	
	/**
	 * This is the first stage in the compilation pipeline --- given a source
	 * file, we must parse it into an Abstract Syntax Tree.
	 * 
	 * @param srcFile --- the source file to be parsed.
	 * @return
	 */
	protected JavaFile parseSourceFile(File srcFile) throws IOException,SyntaxError {
		// Now, construct the reader!
		JavaFileReader reader = new JavaFileReader(srcFile.getPath());		
		return reader.read();
	}
	
	/**
	 * This is the second stage in the compilation pipeline --- we must visit
	 * all declared classes in the source file and extract their types.
	 * 
	 * @param jfile
	 * @param loader
	 */
	protected List<Clazz> discoverSkeletons(JavaFile jfile, ClassLoader loader) {
		return new SkeletonDiscovery().apply(jfile,loader);
	}
		
	/**
	 * This is the third stage in the compilation pipeline --- we must visit
	 * all declared types in the code and resolve them to fully qualified types. 
	 * 
	 * @param jfile
	 * @param loader
	 */
	protected void resolveTypes(JavaFile jfile, ClassLoader loader) {
		new TypeResolution(loader, new TypeSystem()).apply(jfile);
	}
	
	/**
	 * This is the fourth stage in the compilation pipeline --- we must revisit
	 * all declared and anonymous classes, and flesh out their fields and methods.
	 * 
	 * @param jfile
	 * @param loader
	 */
	protected void buildSkeletons(JavaFile jfile, ClassLoader loader) {
		new SkeletonBuilder(loader).apply(jfile);
	}
	
	/**
	 * This is the fifth stage in the compilation pipeline. The aim here is, for
	 * each variable access, to determine whether it is a local variable access,
	 * an inherited field access, an enclosing field access, or an access to a
	 * local variable in an enclosing scope (e.g. for anonymous inner classes).
	 */
	protected void resolveScopes(JavaFile jfile, ClassLoader loader) {
		new ScopeResolution(loader, new TypeSystem()).apply(jfile);					
	}
	
	/**
	 * This is the sixth stage in the compilation pipeline --- we must
	 * propagate our fully qualified types throughout the expressions of the
	 * source file.
	 * 
	 * @param jfile
	 * @param loader
	 */
	protected void propagateTypes(JavaFile jfile, ClassLoader loader) {
		new TypePropagation(loader, new TypeSystem()).apply(jfile);
	}
	
	/**
	 * This is the seventh stage in the compilation pipeline --- we must check
	 * that types are used correctly throughout the source code.
	 * 
	 * @param jfile
	 * @param loader
	 */
	protected void checkTypes(JavaFile jfile, ClassLoader loader) {
		new TypeChecking(loader, new TypeSystem()).apply(jfile);
	}
	
	/**
	 * This is the final stage in the compilation pipeline --- we must write the
	 * output file somewhere.
	 * 
	 * @param jfile
	 * @param loader
	 */
	public String writeOutputFile(JavaFile jfile, File inputFile)
			throws IOException {
		// This is currently a hack
		File outputFile;

		if (outputDirectory == null) {
			String inf = inputFile.getPath();
			inf = inf.substring(0, inf.length() - 5); // strip off .java
			outputFile = new File(inf + ".jout"); // to avoid overwriting
			// input files.
		} else {
			outputFile = new File(outputDirectory, inputFile.getPath());
			outputFile.getParentFile().mkdirs(); // ensure output directory
			// and package
			// directories exist.
		}

		// write the file!!
		new JavaFileWriter(new FileWriter(outputFile)).write(jfile);
		return outputFile.getPath();
	}
	
	/**
	 * This method is just a helper to format the output
	 */
	public void logMessage(String msg) {
		logout.print(msg);		
	}
	
	/**
	 * This method is just a helper to format the output
	 */
	public void logTimedMessage(String msg, long time) {
		logout.print(msg);
		logout.print(" ");
		
		String t = Long.toString(time);
		
		for(int i=0;i<(80-msg.length()-t.length());++i) {
			logout.print(".");
		}
		logout.print(" [");
		logout.print(time);
		logout.println("ms]");
	}	
	
	/**
     * This method is just to factor out the code for looking up the source
     * location and throwing an exception based on that.
     * 
     * @param msg --- the error message
     * @param e --- the syntactic element causing the error
     */
	protected void syntax_error(String msg, SyntacticElement e) {
		SourceLocation loc = (SourceLocation) e.attribute(SourceLocation.class);
		throw new SyntaxError(msg,loc.line(),loc.column());
	}
}
