package jkit.java;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import jkit.compiler.Compiler;
import jkit.compiler.SyntaxError;
import jkit.compiler.ClassLoader;
import jkit.java.io.JavaFile;
import jkit.java.io.JavaFileReader;
import jkit.java.io.JavaFileWriter;
import jkit.java.stages.TypeChecking;
import jkit.java.stages.TypeResolution;
import jkit.java.stages.ScopeResolution;
import jkit.java.stages.TypePropagation;
import jkit.java.stages.TypeSystem;
import jkit.java.tree.Decl;
import jkit.jil.*;
import jkit.util.Pair;

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
	 * The purpose of this method is to indicate that a source file is currently
	 * being compiled.
	 */
	public boolean isCompiling(File sfile)  {		
		try {
			return compilationQueue.contains(sfile.getCanonicalPath())
					|| compiling.contains(sfile.getCanonicalPath());
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
			compilationQueue.add(f.getCanonicalPath());
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
		compilationQueue.remove(filename.getCanonicalPath());
		compiling.add(filename.getCanonicalPath());			
						
		try {
			long start = System.currentTimeMillis();
			
			// Now, construct the reader!
			JavaFileReader reader = new JavaFileReader(filename.getPath());

			logout.println("Parsed " + filename.getPath() + " [" + (System.currentTimeMillis() - start) + "ms]");
			
			JavaFile jfile = reader.read();
			
			// First, we need to resolve types. That is, for each class
			// reference type, determine what package it's in.	
			List<Clazz> skeletons = buildSkeletons(jfile,true);
			loader.compilingClasses(skeletons);
			
			start = System.currentTimeMillis();
			new TypeResolution(loader, new TypeSystem()).apply(jfile);			
			logout.println("Type resolution completed [" + (System.currentTimeMillis()-start) + "ms]");
			
			// Second, we need to build the skeletons of the classes. This is
			// necessary to resolve the scope of a particular variable.
			// Specifically, during scope resolution, we need to be able to:
			// 1) traverse the class heirarchy
			// 2) determine what fields are declared.			
			skeletons = buildSkeletons(jfile,false);
			loader.compilingClasses(skeletons);			
			
			// Third, perform the scope resolution itself. The aim here is, for
			// each variable access, to determine whether it is a local
			// variable access, an inherited field access, an enclosing field
			// access, or an access to a local variable in an enclosing scope
			// (e.g. for anonymous inner classes).
			start = System.currentTimeMillis();
			new ScopeResolution(loader, new TypeSystem()).apply(jfile);
			logout.println("Scope resolution completed [" + (System.currentTimeMillis()-start) + "ms]");
			
			// Fourth, propagate the type information throughout all expressions
			// in the class file, including those in the method bodies and field
			// initialisers.
			start = System.currentTimeMillis();
			new TypePropagation(loader, new TypeSystem()).apply(jfile);
			logout.println("Type propagation completed [" + (System.currentTimeMillis()-start) + "ms]");
			
			// Fifth, check whether the types are being used correctly. If not,
			// report a syntax error.
			start = System.currentTimeMillis();
			new TypeChecking(loader, new TypeSystem()).apply(jfile);
			logout.println("Type checking completed [" + (System.currentTimeMillis()-start) + "ms]");
			
			// This stage is temporary. Just write out the java file again to
			// indicate success thus far.
			new JavaFileWriter(System.out).write(jfile);
						
			compiling.remove(filename);
			
			return skeletons; // to be completed			
		} catch(SyntaxError se) {
			throw new SyntaxError(se.msg(), filename.getPath(), se.line(), se
					.column(), se.width(), se);			
		} 
	}
	
	/**
	 * The purpose of this method is to go through the JavaFiles and extract as
	 * much basic information as possible. This method is used at to points in
	 * the build process; firstly, before type resolution has been run on all
	 * input java files; and, secondly after type resolution has been run. It
	 * needs to be run before type resolution, in order that we can extract all
	 * the classes being compiled, including those inner classes. Thus, we can
	 * resolve the package of any given type during type resolution. Once type
	 * resolution is complete, then we want to put into place the proper
	 * skeleton for every file being compiled, which includes fully resolve
	 * types for all methods, fields and classes. This is necessary in order to
	 * resolve a particular method or field at a dereference point in some code.
	 * 
	 * @param file
	 *            --- input Java file to process.
	 * @param typeOnly
	 *            --- indicate whether only classes should be included (true),
	 *            or include fields and methods as well (false).
	 * @return
	 */
	protected List<Clazz> buildSkeletons(JavaFile file, boolean typeOnly) {
		ArrayList<Clazz> skeletons = new ArrayList();
		for(Decl d : file.declarations()) {
			if(d instanceof Decl.Clazz) {
				skeletons.addAll(buildSkeletons((Decl.Clazz) d, file.pkg(),
						null, typeOnly));
			}
		}
		return skeletons;
	}
	
	/**
	 * Helper method for buildSkeletons.
	 * 
	 * @param c
	 *            --- Class being traversed
	 * @param pkg
	 *            --- package of enclosing file
	 * @param parent
	 *            --- type of parent class, or null if there is none.
	 * @param typeOnly
	 *            --- true if we don't want field or method information (because
	 *            type resolution has not yet been performed and thus we can't
	 *            generate their types yet.)
	 * @return
	 */
	protected List<Clazz> buildSkeletons(Decl.Clazz c, String pkg,
			Type.Clazz parent, boolean typeOnly) {
		ArrayList<Clazz> skeletons = new ArrayList();
		
		Type.Clazz type;
		
		if(!typeOnly) {
			// In this case, type resolution has already occurred and, hence, we
			// have more detailed type information which can be extracted here.
			type = (Type.Clazz) c.attribute(Type.class);
		} else {
			// At this stage, type resolution has not already occurred and,
			// hence, we have only basic (i.e. non-generic) type information
			// available.
			List<Pair<String,List<Type.Reference>>> components = new ArrayList();
			if(parent != null) {
				for (Pair<String, List<Type.Reference>> i : parent.components()) {
					components.add(i);
				}
			}
			components.add(new Pair(c.name(), new ArrayList()));
			type = new Type.Clazz(pkg,components);
		}
		
		Type.Clazz superClass = null;
		ArrayList<Type.Clazz> interfaces = new ArrayList();
		ArrayList<Field> fields = new ArrayList();
		ArrayList<Method> methods = new ArrayList();
		
		for(Decl d : c.declarations()) {
			if(d instanceof Decl.Clazz) {
				skeletons.addAll(buildSkeletons((Decl.Clazz) d, pkg,type,typeOnly));
			} else if(d instanceof Decl.Field && !typeOnly) {				
				Decl.Field f = (Decl.Field) d;
				Type t = (Type) f.attribute(Type.class);
				fields.add(new Field(f.name(),t,f.modifiers()));
			} else if(d instanceof Decl.Method && !typeOnly) {
				
			}
		}
		
		/** 
		 * Now, construct the skeleton for this class!
		 */
		skeletons.add(new Clazz(type,c.modifiers(),superClass,interfaces,fields,methods));
		
		return skeletons;
	}
}
