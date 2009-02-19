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
			long start = System.currentTimeMillis();						
			JavaFile jfile = parseSourceFile(filename);
			logTimedMessage("Parsed " + filename.getPath(),(System.currentTimeMillis() - start));
			
			// First, we need to resolve types. That is, for each class
			// reference type, determine what package it's in.	
			List<Clazz> skeletons = buildSkeletons(jfile,true);
			loader.compilingClasses(skeletons);
			
			// Second, we need to resolve all types found in the src file.
			start = System.currentTimeMillis();
			resolveTypes(jfile,loader);					
			logTimedMessage("Type resolution completed",(System.currentTimeMillis()-start));
			
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
			resolveScopes(jfile,loader);			
			logTimedMessage("Scope resolution completed",(System.currentTimeMillis()-start));
			
			// Fourth, propagate the type information throughout all expressions
			// in the class file, including those in the method bodies and field
			// initialisers.
			start = System.currentTimeMillis();
			propagateTypes(jfile,loader);			
			logTimedMessage("Type propagation completed",(System.currentTimeMillis()-start));
			
			// Fifth, check whether the types are being used correctly. If not,
			// report a syntax error.
			start = System.currentTimeMillis();
			checkTypes(jfile,loader);
			logTimedMessage("Type checking completed",(System.currentTimeMillis()-start));
			
			// Finally, write out the compiled class file.
			start = System.currentTimeMillis();
			String outFile = writeOutputFile(jfile, filename);			
			logTimedMessage("Wrote " + outFile,(System.currentTimeMillis()-start));
			
			compiling.remove(filename);
			
			return skeletons; // to be completed			
		} catch(SyntaxError se) {
			throw new SyntaxError(se.msg(), filename.getPath(), se.line(), se
					.column(), se.width(), se);			
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
	 * all declared types in the code and resolve them to fully qualified types. 
	 * 
	 * @param jfile
	 * @param loader
	 */
	protected void resolveTypes(JavaFile jfile, ClassLoader loader) {
		new TypeResolution(loader, new TypeSystem()).apply(jfile);
	}
	
	
	/**
	 * Third, perform the scope resolution itself. The aim here is, for each
	 * variable access, to determine whether it is a local variable access, an
	 * inherited field access, an enclosing field access, or an access to a
	 * local variable in an enclosing scope (e.g. for anonymous inner classes).
	 */
	protected void resolveScopes(JavaFile jfile, ClassLoader loader) {
		new ScopeResolution(loader, new TypeSystem()).apply(jfile);					
	}
	
	/**
	 * This is the fourth stage in the compilation pipeline --- we must
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
	 * This is the fifth stage in the compilation pipeline --- we must check
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
	private int anonymousClassCount = 1; // bit of a hack :(
	
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
		Type.Clazz superClass = new Type.Clazz("java.lang","Object");
		if(c.superclass() != null) {
			// Observe, after type resolution, this will give the correct
			// superclass type. However, prior to type resolution it will just
			// return null.
			superClass = (Type.Clazz) c.superclass().attribute(Type.class);
		}
		ArrayList<Type.Clazz> interfaces = new ArrayList();
		for(jkit.java.tree.Type.Clazz i : c.interfaces()) {
			Type.Clazz t = (Type.Clazz) i.attribute(Type.class);
			if(t != null) {
				interfaces.add(t);
			}
		}
		ArrayList<Field> fields = new ArrayList();
		ArrayList<Method> methods = new ArrayList();
		
		for(Decl d : c.declarations()) {
			if(d instanceof Decl.Clazz) {
				skeletons.addAll(buildSkeletons((Decl.Clazz) d, pkg,type,typeOnly));
			} else if(d instanceof Decl.Field && !typeOnly) {				
				Decl.Field f = (Decl.Field) d;
				Type t = (Type) f.attribute(Type.class);				
				fields.add(new Field(f.name(), t, f.modifiers(), new ArrayList(
						f.attributes())));
			} else if(d instanceof Decl.Method && !typeOnly) {
				Decl.Method m = (Decl.Method) d;
				Type.Function t = (Type.Function) m.attribute(Type.class);
				List<Type.Clazz> exceptions = new ArrayList<Type.Clazz>();
				
				for(jkit.java.tree.Type.Clazz tc : m.exceptions()) {
					exceptions.add((Type.Clazz)tc.attribute(Type.class));
				}
				 
				methods.add(new Method(m.name(), t, m.modifiers(), exceptions,
						new ArrayList(m.attributes())));
			}
		}
		
		/**
		 * Now, deal with some special cases when this is not actually a class
		 */
		if(c instanceof Decl.Enum) {
			Decl.Enum ec = (Decl.Enum) c;
			for(Decl.EnumConstant enc : ec.constants()) {
				Type t = (Type) enc.attribute(Type.class);
				if(enc.declarations().size() > 0) {
					syntax_error("No support for ENUMS that have methods",enc);
				} else {
					List<Modifier> modifiers = new ArrayList<Modifier>();
					modifiers.add(new Modifier.Base(java.lang.reflect.Modifier.PUBLIC));
					fields.add(new Field(enc.name(), t, modifiers, new ArrayList(
						enc.attributes())));
				}
			}
		}
		
		/**
		 * Now, construct the skeleton for this class! 
		 */
		skeletons.add(new Clazz(type,c.modifiers(),superClass,interfaces,fields,methods));
		
		return skeletons;
	}
	
	/**
	 * This method is just a helper to format the output
	 */
	protected void logTimedMessage(String msg, long time) {
		logout.print(msg);
		logout.print(" ");
		
		for(int i=0;i<(80-msg.length());++i) {
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
