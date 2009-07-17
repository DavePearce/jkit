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
import jkit.jil.stages.DeadCodeElimination;

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
	protected ArrayList<Triple<File,JavaFile,List<JilClass>>> compilationQueue = new ArrayList();

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

	protected final ClassFileBuilder builder;
	
	protected final BytecodeOptimiser optimiser;
	
	/**
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 */
	public JavaCompiler(List<String> classpath) {
		this.loader = new ClassLoader(classpath, this);
		this.builder = new ClassFileBuilder(loader,49);
		this.optimiser = new BytecodeOptimiser();
	}

	/**
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 * @param logout
	 *            A stream where log messages are sent
	 */
	public JavaCompiler(List<String> classpath, OutputStream logout) {
		this.loader = new ClassLoader(classpath, this);
		this.builder = new ClassFileBuilder(loader,49);
		this.optimiser = new BytecodeOptimiser();
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
		this.builder = new ClassFileBuilder(loader,49);
		this.optimiser = new BytecodeOptimiser();
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
     * Compile a class in the file system, using the appropriate pipeline(s).
     * 
     * @param filenames
     *            a list of the full names of the files to be compiled,
     *            including their path expressed in the local host format (e.g.
     *            using File.separatorChar's to indicate directories).
     * @return
     */
	public List<JilClass> compile(File filename) throws IOException,
			SyntaxError {		
		
		parse(filename);					

		ArrayList<JilClass> classes = new ArrayList<JilClass>();

		while (!compilationQueue.isEmpty()) {
			Triple<File,JavaFile,List<JilClass>> e = compilationQueue.get(0);
			compilationQueue.remove(0);
			classes.addAll(finishcompiling(e.first(),e.second(),e.third()));
		}

		return classes;
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
			parse(f);			
		}

		ArrayList<JilClass> classes = new ArrayList<JilClass>();

		while (!compilationQueue.isEmpty()) {
			Triple<File,JavaFile,List<JilClass>> e = compilationQueue.get(0);
			compilationQueue.remove(0);
			classes.addAll(finishcompiling(e.first(),e.second(),e.third()));
		}

		return classes;
	}
	

	public List<JilClass> parse(File filename) throws IOException {
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
			
			compilationQueue.add(new Triple(filename,jfile,skeletons));
			
			return skeletons;
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
     * Finish compiling a class on the compilation queue.
     * 
     * @param filename
     *            the full name of the file to be compiled, including it's path
     *            expressed in the local host format (e.g. using
     *            File.separatorChar's to indicate directories).
     * @return
     */
	public List<JilClass> finishcompiling(File filename, JavaFile jfile,
			List<JilClass> skeletons) throws IOException, SyntaxError {		
		compiling.add(filename.getCanonicalPath());

		try {			
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
				eliminateDeadCode(filename,clazz,loader);
				addBypassMethods(filename,clazz,loader);				
			}
			
			// Ok, at this point, we need to determine the root component of the
			// original filename.			
			String root = determinePackageRoot(filename,jfile);			
			File outdir = root == null ? outputDirectory : new File(outputDirectory,root);		
					
			// Ninth, write out the compiled class file(s).			
			for(JilClass clazz : skeletons) {				
				String baseName = createBasename(clazz.type());
				writeOutputFile(baseName, clazz, outdir);				
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
     * This method attempts to determine the root of the package hierarchy. This
     * is necessary because we may not be compiling a source file from the root.
     * For example, suppose we have class Test in package tmp, which is rooted
     * in the src directory:
     * 
     * <pre>
     * src / tmp / Test.java
     * </pre>
     * 
     * Now, suppose we are in the outermost directory, and compile the src file
     * "src/tmp/Test.java". In order for the compiler to correctly locate other
     * source files that may need to be computed, it needs to know that the root
     * of the hierarchy is in src/. Therefore, this method would return "src".
     * 
     * @param filename
     * @param srcFile
     * @return
     */
	protected String determinePackageRoot(File filename, JavaFile srcFile) {
		String[] paths = File.separatorChar == '\\' ? filename.getPath()
				.split("\\\\") : filename.getPath().split(
				"" + File.separatorChar);
		String[] comps = srcFile.pkg().split("\\.");
		
		int i = paths.length - 2;
		int j = comps.length - 1;
		
		while(i >= 0 && j >= 0 && paths[i].equals(comps[j])) {				
			i=i-1;
			j=j-1;
		}
		
		String root = "";
		for(int k=0;k<=i;++k) {
			root = root + paths[k] + File.separatorChar;
		}
		
		
		if(i >= 0) {				
			return root;
		} else {
			return null;
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
	protected void eliminateDeadCode(File srcfile, JilClass jfile, ClassLoader loader) {
		long start = System.currentTimeMillis();
		new DeadCodeElimination().apply(jfile);
		logTimedMessage("[" + srcfile.getPath() + "] Eliminated Dead code",
				(System.currentTimeMillis() - start));
	}
	
	/**
	 * This is the twelth stage in the compilation pipeline --- we are now
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
		ClassFile cfile = builder.build(clazz);
		
		logTimedMessage("[" + outputFile.getPath() + "] Bytecode generation completed",
				(System.currentTimeMillis() - start));	
		
		start = System.currentTimeMillis();
		
		// this is where the bytecode optimisation would occur.
		int numRewrites = optimiser.optimise(cfile);
		
		logTimedMessage("[" + outputFile.getPath() + "] Bytecode optimisation completed (" + numRewrites + " rewrites)",
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
