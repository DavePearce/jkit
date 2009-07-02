package jkit.java;

import java.io.*;
import java.util.*;

import jkit.compiler.Compiler;
import jkit.compiler.SyntaxError;
import jkit.compiler.ClassLoader;
import jkit.java.io.JavaFile;
import jkit.java.io.JavaFileReader;
import jkit.java.io.JavaFileWriter;
import jkit.bytecode.*;
import jkit.java.stages.*;
import jkit.util.*;
import jkit.jil.*;
import jkit.jil.io.*;
import jkit.jil.stages.ClassFileBuilder;
import jkit.jil.tree.JilClass;
import jkit.jil.tree.Type;
import jkit.jil.tree.SourceLocation;
import jkit.jil.tree.SyntacticElement;
import jkit.jil.stages.BypassMethods;

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
	protected ClassLoader loader;

	/**
	 * The compilation queue is a list of files which are scheduled for
	 * compilation. The purpose of the queue is simply to ensure that files to
	 * not get compiled twice. For example, suppose we have files "C1.java" and
	 * "C2.java" on the queue; we begin compiling "C1.java" and this in turn
	 * forces the compilation of "C2.java"; then, when this is complete,
	 * "C2.java" is automatically removed from the queue, thus preventing it
	 * from being compiled again.
	 */
	protected ArrayList<String> compilationQueue = new ArrayList<String>();

	/**
	 * The compiling set gives a full list of files which are currently being
	 * compiled. This may be more than one if there are multiple compiles going
	 * on in parallel. The reason for this is to prevent an infinite loop of
	 * recursive compiles. [ALTHOUGH I THINK THERE MAYBE A BETTER WAY OF DOING
	 * THIS]
	 */
	protected Set<String> compiling = new HashSet<String>();

	/**
	 * The output directory for class files.
	 */
	protected File outputDirectory = null;

	/**
	 * The logout output stream is used to write log information about the
	 * status of compilation. The default stream just discards everything.
	 */
	protected PrintStream logout = new PrintStream(new OutputStream() {
		public void write(byte[] b) { /* don't do anything! */
		}

		public void write(byte[] b, int x, int y) { /* don't do anything! */
		}

		public void write(int x) { /* don't do anything! */
		}
	});

	/**
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 */
	public JavaCompiler(List<String> classpath) {
		this.loader = new ClassLoader(classpath, this);
	}

	/**
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 * @param logout
	 *            A stream where log messages are sent
	 */
	public JavaCompiler(List<String> classpath, OutputStream logout) {
		this.loader = new ClassLoader(classpath, this);
		if(logout != null) {
			this.logout = new PrintStream(logout);
		}
	}

	/**
	 * @param sourcepath
	 *            a list of directory and/or jar file locations.
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 * @param logout
	 *            A stream where log messages are sent
	 */
	public JavaCompiler(List<String> sourcepath, List<String> classpath,
			OutputStream logout) {
		this.loader = new ClassLoader(sourcepath, classpath, this);
		if(logout != null) {
			this.logout = new PrintStream(logout);
		}
	}

	/**
	 * Get the ClassLoader being used by this class.
	 * 
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
	public boolean isCompiling(File sfile) {
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
	public List<JilClass> compile(List<File> filenames) throws IOException,
			SyntaxError {
		for (File f : filenames) {
			compilationQueue.add(f.getPath());
		}

		ArrayList<JilClass> classes = new ArrayList<JilClass>();

		while (!compilationQueue.isEmpty()) {
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
	public List<JilClass> compile(File filename) throws IOException, SyntaxError {
		compilationQueue.remove(filename.getPath());
		compiling.add(filename.getCanonicalPath());

		try {
			// First, parse the Java source file to yield an abstract syntax
			// tree.
			
			JavaFile jfile = parseSourceFile(filename);
			
			// Second, we need to resolve types. That is, for each class
			// reference type, determine what package it's in.			
			List<JilClass> skeletons = discoverSkeletons(filename, jfile, loader);			
			
			// Third, we need to resolve all types found in the src file.			
			resolveTypes(filename, jfile, loader);

			// Fourth, we need to build the skeletons of the classes. This is
			// necessary to resolve the scope of a particular variable.
			// Specifically, during scope resolution, we need to be able to:
			// 1) traverse the class heirarchy
			// 2) determine what fields are declared.			
			skeletons.addAll(buildSkeletons(filename, jfile, loader));			

			// Fifth, perform the scope resolution itself. The aim here is, for
			// each variable access, to determine whether it is a local
			// variable access, an inherited field access, an enclosing field
			// access, or an access to a local variable in an enclosing scope
			// (e.g. for anonymous inner classes).
			resolveScopes(filename, jfile, loader);
			
			// Sixth, propagate the type information throughout all expressions
			// in the class file, including those in the method bodies and field
			// initialisers.			
			propagateTypes(filename, jfile, loader);			

			// Seventh, check whether the types are being used correctly. If
			// not, report a syntax error.
			checkTypes(filename, jfile, loader);
		
			// Eight, break down anonymous inner classes
			breakAnonClasses(filename,jfile,loader);
			
			// Ninth, rewrite inner classes
			rewriteInnerClasses(filename,jfile,loader);
						
			// Tenth, rewriten enumerations
			rewriteEnumerations(filename,jfile,loader);
			
			// Eleventh, eliminate side effects from expressions
			generateJilCode(filename, jfile, loader);
			
			// Twelth, add bypass methods
			for(JilClass clazz : skeletons) {
				addBypassMethods(filename,clazz,loader);
			}
			
			// Ok, at this point, we need to determine the root component of the
			// original filename.
			String path = filename.getPath();
			int i = 0;
			int rootEnd = path.lastIndexOf(File.separatorChar,path.length()-1);
			while(i != -1) {				
				i = jfile.pkg().indexOf('.',i+1);
				rootEnd = path.lastIndexOf(File.separatorChar,rootEnd-1);
			}	
			File outdir = outputDirectory;
			if(rootEnd != -1) {
				String root = path.substring(0, rootEnd);				
				outdir = new File(outputDirectory,root);
			}							
			
			// Ninth, write out the compiled class file(s).			
			for(JilClass clazz : skeletons) {				
				writeOutputFile(createBasename(clazz.type()), clazz, outdir);				
			}
						
			compiling.remove(filename);

			return skeletons; // to be completed
		} catch (SyntaxError se) {
			if (se.fileName() == null) {
				throw new SyntaxError(se.msg(), filename.getPath(), se.line(),
						se.column(), se.width(), se);
			} else {
				throw se;
			}
		}
	}

	/**
	 * This is the first stage in the compilation pipeline --- given a source
	 * file, we must parse it into an Abstract Syntax Tree.
	 * 
	 * @param srcFile
	 *            --- the source file to be parsed.
	 * @return
	 */
	protected JavaFile parseSourceFile(File srcFile) throws IOException,
			SyntaxError {		
		long start = System.currentTimeMillis();
				
		JavaFileReader reader = new JavaFileReader(srcFile.getPath());
		JavaFile jfile = reader.read();
		logTimedMessage("[" + srcFile.getPath() + "] Parsing completed ",
				(System.currentTimeMillis() - start));
		
		return jfile;
	}

	/**
	 * This is the second stage in the compilation pipeline --- we must visit
	 * all declared classes in the source file and extract their types.
	 * 
	 * @param jfile
	 * @param loader
	 */
	protected List<JilClass> discoverSkeletons(File srcfile, JavaFile jfile,
			ClassLoader loader) {
		long start = System.currentTimeMillis();
		List<JilClass> r = new SkeletonDiscovery().apply(jfile, loader);
		logTimedMessage("[" + srcfile.getPath()
				+ "] Skeleton discovery completed", (System
				.currentTimeMillis() - start));
		return r;
	}

	/**
	 * This is the third stage in the compilation pipeline --- we must visit all
	 * declared types in the code and resolve them to fully qualified types.
	 * 
	 * @param jfile
	 * @param loader
	 */
	protected void resolveTypes(File srcfile, JavaFile jfile, ClassLoader loader) {
		long start = System.currentTimeMillis();
		new TypeResolution(loader, new TypeSystem()).apply(jfile);
		logTimedMessage("[" + srcfile.getPath()
				+ "] Type resolution completed", (System
				.currentTimeMillis() - start));
	}

	/**
	 * This is the fourth stage in the compilation pipeline --- we must revisit
	 * all declared and anonymous classes, and flesh out their fields and
	 * methods.
	 * 
	 * @param jfile
	 * @param loader
	 */
	protected List<JilClass> buildSkeletons(File srcfile, JavaFile jfile, ClassLoader loader) {
		long start = System.currentTimeMillis();
		List<JilClass> r = new SkeletonBuilder(loader).apply(jfile);
		logTimedMessage("[" + srcfile.getPath()
				+ "] Skeleton construction completed", (System
				.currentTimeMillis() - start));
		return r;
	}

	/**
	 * This is the fifth stage in the compilation pipeline. The aim here is, for
	 * each variable access, to determine whether it is a local variable access,
	 * an inherited field access, an enclosing field access, or an access to a
	 * local variable in an enclosing scope (e.g. for anonymous inner classes).
	 */
	protected void resolveScopes(File srcfile, JavaFile jfile, ClassLoader loader) {
		long start = System.currentTimeMillis();
		new ScopeResolution(loader, new TypeSystem()).apply(jfile);
		logTimedMessage("[" + srcfile.getPath()
				+ "] Scope resolution completed", (System
				.currentTimeMillis() - start));
	}

	/**
	 * This is the sixth stage in the compilation pipeline --- we must propagate
	 * our fully qualified types throughout the expressions of the source file.
	 * 
	 * @param jfile
	 * @param loader
	 */
	protected void propagateTypes(File srcfile, JavaFile jfile, ClassLoader loader) {
		long start = System.currentTimeMillis();
		new TypePropagation(loader, new TypeSystem()).apply(jfile);
		logTimedMessage("[" + srcfile.getPath()
				+ "] Type propagation completed", (System
				.currentTimeMillis() - start));
	}

	/**
	 * This is the seventh stage in the compilation pipeline --- we must check
	 * that types are used correctly throughout the source code.
	 * 
	 * @param jfile
	 * @param loader
	 */
	protected void checkTypes(File srcfile, JavaFile jfile, ClassLoader loader) {
		long start = System.currentTimeMillis();
		new TypeChecking(loader, new TypeSystem()).apply(jfile);
		logTimedMessage("[" + srcfile.getPath()
				+ "] Type checking completed",
				(System.currentTimeMillis() - start));

	}

	/**
	 * This is the eigth stage in the compilation pipeline --- we must break
	 * down anonymous class declarations.
	 * 
	 * @param srcfile
	 * @param jfile
	 * @param loader
	 */
	protected void breakAnonClasses(File srcfile, JavaFile jfile, ClassLoader loader) {
		long start = System.currentTimeMillis();
		new AnonClassesRewrite(loader, new TypeSystem()).apply(jfile);
		logTimedMessage("[" + srcfile.getPath()
				+ "] Anonymous classes rewritten", (System
				.currentTimeMillis() - start));
	}
	
	/**
	 * This is the ninth stage in the compilation pipeline --- we must rewrite
	 * inner classes to be individual classes, and include accessors and parent
	 * pointers where appropriate.
	 * 
	 * @param srcfile
	 * @param jfile
	 * @param loader
	 */
	protected void rewriteInnerClasses(File srcfile, JavaFile jfile, ClassLoader loader) {
		long start = System.currentTimeMillis();
		new InnerClassRewrite(loader, new TypeSystem()).apply(jfile);
		logTimedMessage("[" + srcfile.getPath()
				+ "] Inner classes rewritten", (System
				.currentTimeMillis() - start));
	}
	
	/**
     * This is the Tenth stage in the compilation pipeline --- we must rewrite
     * enumerations to include the necessary setup information, and include
     * appropriate accessors.
     * 
     * @param srcfile
     * @param jfile
     * @param loader
     */
	protected void rewriteEnumerations(File srcfile, JavaFile jfile, ClassLoader loader) {
		long start = System.currentTimeMillis();
		new EnumRewrite(loader, new TypeSystem()).apply(jfile);
		logTimedMessage("[" + srcfile.getPath()
				+ "] Enumerations rewritten", (System
				.currentTimeMillis() - start));
	}
	
	
	/**
	 * This is the tenth stage in the compilation pipeline --- we are now
	 * beginning the process of code-generation. In this stage, we generate jil
	 * code from the java source file.
	 * 
	 * @param jfile
	 * @param loader
	 */
	protected void generateJilCode(File srcfile, JavaFile jfile, ClassLoader loader) {
		long start = System.currentTimeMillis();
		new JilBuilder(loader, new TypeSystem()).apply(jfile);
		logTimedMessage("[" + srcfile.getPath() + "] Jil generation completed",
				(System.currentTimeMillis() - start));
	}

	/**
	 * This is the eleven	th stage in the compilation pipeline --- we are now
	 * beginning the process of code-generation. In this stage, we generate jil
	 * code from the java source file.
	 * 
	 * @param jfile
	 * @param loader
	 */
	protected void addBypassMethods(File srcfile, JilClass jfile, ClassLoader loader) {
		long start = System.currentTimeMillis();
		new BypassMethods(loader, new TypeSystem()).apply(jfile);
		logTimedMessage("[" + srcfile.getPath() + "] Added bypass methods",
				(System.currentTimeMillis() - start));
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
		
		File outputFile = new File(rootdir, baseName + ".class");		
		
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
		
		new ClassFileWriter(out,loader).write(cfile);		
		
		logTimedMessage("[" + outputFile.getPath() + "] Wrote " + outputFile.getPath(),
				(System.currentTimeMillis() - start));		
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

		for (int i = 0; i < (80 - msg.length() - t.length()); ++i) {
			logout.print(".");
		}
		logout.print(" [");
		logout.print(time);
		logout.println("ms]");
	}
	
	/**
	 * Create a string suitable as a filename for this class.
	 * 
	 * @param c
	 * @return
	 */
	public String createBasename(Type.Clazz tc) {
		String filename = tc.pkg().replace('.', File.separatorChar);
		if(!filename.equals("")) {
			filename = filename + File.separatorChar;
		}
		boolean firstTime=true;
		for(Pair<String,List<Type.Reference>> c : tc.components()) {
			if(!firstTime) {
				filename += "$";
			}
			firstTime=false;
			filename += c.first();
		}
		return filename;
	}

	/**
	 * This method is just to factor out the code for looking up the source
	 * location and throwing an exception based on that.
	 * 
	 * @param msg
	 *            --- the error message
	 * @param e
	 *            --- the syntactic element causing the error
	 */
	protected void syntax_error(String msg, SyntacticElement e) {
		SourceLocation loc = (SourceLocation) e.attribute(SourceLocation.class);
		throw new SyntaxError(msg, loc.line(), loc.column());
	}
}
