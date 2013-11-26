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

package jkit.compiler;

import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.*;

import jkit.bytecode.ClassFile;
import jkit.bytecode.ClassFileReader;
import jkit.error.ErrorHandler;
import jkit.error.FieldNotFoundException;
import jkit.error.JKitException;
import jkit.error.MethodNotFoundException;
import jkit.error.PackageNotFoundException;
import jkit.jil.tree.Type;
import jkit.jil.tree.JilClass;
import jkit.jil.util.Types;
import jkit.util.Pair;
import jkit.compiler.SyntaxError;

/**
 * A ClassLoader is responsible for loading classes from the filesystem. Classes
 * are located in either jar files or directories; furthermore, classes may be
 * in source or binary (i.e. compiled) form. The ClassLoader will search the
 * classpath and/or sourcepath to find classes and (if necessary) compile them.
 *
 * @author djp
 */
public final class ClassLoader {

	/**
	 * The class path is a list of directories and Jar files which must be
	 * searched in ascending order for *class files*.
	 */
	private final ArrayList<String> classpath;

	/**
	 * The source path is a list of directories and Jar files which must be
	 * search in ascending order for *source files*. By default, it is the same
	 * as the classpath, although this can be overriden (e.g. by using the
	 * "-sourcepath <path>" command-line option in jkit or javac).
	 */
	private final ArrayList<String> sourcepath;

	/**
	 * A map from class names in the form "xxx.yyy$zzz to Clazz objects. This is
	 * the master cache of classes which have been loaded during the compilation
	 * process. Once a class has been entered into the classtable, it will not
	 * be loaded again.
	 */
	private final HashMap<String,Clazz> classtable = new HashMap<String,Clazz>();

	/**
	 * A PackageInfo object contains information about a particular package,
	 * including the following information:
	 *
	 * 1) where it can be found on the file system (either a directory, or a
	 *    jar file).
	 *
	 * 2) what classes it contains.
	 *
	 */
	private final class PackageInfo {
		/**
		 * The classes field contains those classes contained in this package.
		 * Class names are represented as strings of the form "xxx$yyy".
		 */
		public final HashSet<String> classes = new HashSet<String>();

		/**
		 * The compiledClasses indicates which classes are definitely compiled.
		 * This is useful for detecting classes that need to be compiled in
		 * order to correctly resolve types.
		 */
		public final HashSet<String> compiledClasses = new HashSet<String>();

		/**
		 * The locations list contains the list of locations that have been
		 * identified for a particular package. Each location identifies either
		 * a jar file, or a directory. The order of locations found is
		 * important --- those which come first have higher priority.
		 */
		public final ArrayList<File> locations = new ArrayList<File>();

		/**
         * This indicates whether or not the package information is fully
         * resolved. This is useful as it tells us whether or not we can avoid
         * research the classpath and sourcepath looking for packages.
         */
		public boolean fullyResolved = false;
	}

	/**
	 * The packages map maps each package to the classes they contain. For
	 * example, "java.lang" will map to "String", "Integer" etc. Inner classes
	 * are appended to their enclosing class using "$" and located in their
	 * package accordingly. Therefore, for the class "java.util.Map.Entry" there
	 * will be an entry "Map$Entry" in the "java.util" package.
	 */
	private final HashMap<String, PackageInfo> packages = new HashMap<String, PackageInfo>();

	/**
     * The failed packages set is a collection of packages which have been
     * requested, but are known not to exist. The purpose of this cache is
     * simply to speek up package resolution.
     */
	private final HashSet<String> failedPackages = new HashSet<String>();

	/**
	 * The ClassCompiler is needed for compiling source files found on the
	 * sourcepath which are needed to identify inner classes appropriately.
	 */
	private final Compiler compiler;

	/**
	 * Construct a ClassLoader with a given classpath. The classpath is a list
	 * of directory and/or jar file locations (specified according to the local
	 * file system) which is to be searched for class files. The sourcepath
	 * (which is used for finding source files) is set the same as the
	 * classpath.
	 *
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 * @param compiler
	 *            A class compiler
	 */
	public ClassLoader(List<String> classpath, Compiler compiler) {
		this.sourcepath = new ArrayList<String>(classpath);
		this.classpath = new ArrayList<String>(classpath);
		this.compiler = compiler;

		buildInitialPackageMap();
	}

	/**
	 * Construct a ClassLoader with a given classpath and sourcepath. The
	 * classpath is a list of directory and/or jar file locations (specified
	 * according to the local file system) which is to be searched for class
	 * files. The sourcepath is, likewise, a list of directory and/or jar file
	 * locations which is used for finding source files.
	 *
	 * @param sourcepath
	 *            a list of directory and/or jar file locations.
	 * @param classpath
	 *            a list of directory and/or jar file locations.
	 * @param compiler
	 *            A class compiler
	 */
	public ClassLoader(List<String> sourcepath, List<String> classpath,
			Compiler compiler) {
		this.sourcepath = new ArrayList<String>(sourcepath);
		this.classpath = new ArrayList<String>(classpath);
		this.compiler = compiler;

		buildInitialPackageMap();
	}

	/**
	 * This function checks whether the supplied package exists or not.
	 *
	 * @param pkg
	 *            The package whose existence we want to check for.
	 *
	 * @return true if the package exists, false otherwise.
	 */
	public boolean isPackage(String pkg) {
		return resolvePackage(pkg) != null;
	}

	/**
	 * This methods attempts to resolve the correct package for a class, given a
	 * list of imports. Resolving the correct package may require loading
	 * classes as necessary from the classpath and/or compiling classes for
	 * which only source code is currently available.
	 *
	 * @param className
	 *            A class name without package specifier. Inner classes are
	 *            indicated by a "$" separator.
	 * @param imports
	 *            A list of packages to search through. Packages are searched in
	 *            order of appearance. Note, "java.lang.*" must be included in
	 *            imports if it is to be searched.
	 * @return A Type.Reference representing the fully qualified class.
	 * @throws ClassNotFoundException
	 *             if it couldn't resolve the class
	 */
	public Type.Clazz resolve(String className, List<String> imports)
			throws ClassNotFoundException {

		if(className.contains(".")) {
			throw new IllegalArgumentException("className cannot contain \".\"");
		}

		// Observe, the manner in which we go through imports reflects the way
		// javac operates.

		for (String imp : imports) {
			Type.Clazz ref = null;
			if (!imp.endsWith(".*")) {
				// The aim of this piece of code is to replicate the way javac
				// works.
				String impName = imp.substring(imp.lastIndexOf('.') + 1, imp
						.length());
				String tmp = className.replace('$', '.');
				while(tmp.length() > 0) {
					if (impName.equals(tmp)) {
						// strip off class name itself
						String pkg = imp.substring(0, imp.length()
								- (1 + tmp.length()));
						// now try and resolve it.
						ref = resolveClassName(pkg,className);
						break;
					}
					tmp = tmp.substring(0,Math.max(0,tmp.lastIndexOf('.')));
				}
			}
			if(ref != null) { return ref; }
		}

		for (String imp : imports) {
			Type.Clazz ref = null;
			if (imp.endsWith(".*")) {
				// try and resolve the class
				ref = resolveClassName(imp.substring(0, imp.length() - 2),className);
			}
			if(ref != null) { return ref; }
		}
		throw new ClassNotFoundException(className);
	}

	/**
	 * This method attempts to resolve a classname; that is, determine the
	 * proper package and inner class scope for the class in question. An
	 * initial package (e.g. "xxx.yyy") suggestion is provided, along with an
	 * appropriate class name (e.g. "zzz"). If the given package contains "zzz",
	 * then "xxx.yyy.zzz" is returned. Otherwise, we assume that the package is
	 * not, in fact, a proper package; rather, it is a package with an inner
	 * class appended on. Therefore, we transform "xxx.yyy" and "zzz" into "xxx"
	 * and "yyy$zzz" and check to see whether "xxx.yyy$zzz" exists. If not, we
	 * recursively check for class "xxx$yyy$zzz" in the default package.
	 *
	 * @param pkg
	 *            the package suggestion provided, in the form "xxx.yyy.zzz"
	 * @param className
	 *            the class name provided, in the form "xxx$yyy"
	 * @return
	 */
	protected Type.Clazz resolveClassName(String pkg, String className) {
		ArrayList<Pair<String, List<Type.Reference>>> classes = new ArrayList<Pair<String, List<Type.Reference>>>();

		String fullClassName = className;
		String outerClassName = fullClassName;

		boolean firstTime = true;
		for (String c : className.split("\\$")) {
			if(firstTime) {
				outerClassName = c;
			}
			firstTime=false;
			classes.add(new Pair<String, List<Type.Reference>>(c,
					new ArrayList<Type.Reference>()));
		}

		while(pkg != null) {
			PackageInfo pkgInfo = resolvePackage(pkg);
			if (pkgInfo != null) {
				if(pkgInfo.classes.contains(fullClassName)) {
					// Found the class!!
					return new Type.Clazz(pkg,classes);
				} else if (pkgInfo.classes.contains(outerClassName)
						&& !pkgInfo.compiledClasses.contains(outerClassName)) {
					// If we get here, then we may have a source file for the
					// outer class which has not been compiled yet. Therefore,
					// we need to check for this and, if so, compile it to check
					// whether or not the inner class we're after is actually
					// contain therein.
					String ocn = pkg.equals("") ? outerClassName : pkg + "." + outerClassName;
					loadClass(ocn,pkgInfo); // this will force a compile
					continue; // try again for the same class/pkg combination
				} else {
					break;
				}
			} else {
				// This import does not correspond to a valid package.
				// Therefore, it may be specifying an inner class and we need to check.
				outerClassName = pathChild(pkg);
				fullClassName = outerClassName + "$" + fullClassName;
				classes.add(0, new Pair<String, List<Type.Reference>>(
						pathChild(pkg), new ArrayList<Type.Reference>()));
				pkg = pathParent(pkg);
			}
		}

		return null;
	}

	/**
	 * Load a class from the classpath by searching for it in the sourcepath,
	 * then in the classpath. If the class has already been loaded, then this is
	 * returned immediately. Otherwise, the cache of package locations is
	 * consulted in an effort to find a class or source file representing the
	 * class in question.
	 *
	 * @param ref
	 *            details of class to load
	 * @throws ClassNotFoundException
	 *             If it couldn't load the class
	 */
	public Clazz loadClass(Type.Clazz ref) throws ClassNotFoundException {
		String name = refName(ref);
		// First, look in the classtable to see whether we have loaded this
		// class before.
		Clazz c = classtable.get(name);
		if(c != null) { return c; }

		// Second, locate the information we know about the classes package and
		// then attempt to locate either a source or class file.
		PackageInfo pkgInfo = resolvePackage(ref.pkg());

		if (pkgInfo == null) { throw new ClassNotFoundException("Unable to load class " + name); }
		c = loadClass(name,pkgInfo);

		if(c == null) { throw new ClassNotFoundException("Unable to load class " + name); }

		return c;
	}

	/**
	 * This method attempts to read a classfile from a given package.
	 *
	 * @param name
	 *            The name of the class to load, in the format
	 *            "xxx.yyy$zzz"
	 * @param pkgIngo
	 *            Information about the including package, in particular where
	 *            it can be located on the file system.
	 * @return
	 */
	private Clazz loadClass(String name, PackageInfo pkgInfo) {
		long time = System.currentTimeMillis();
		String jarname = name.replace('.','/') + ".class";
		String filename = name.replace('.',File.separatorChar);

		// The srcFilename gives the filename of the source file. So, if the
        // class we're looking for is an inner class, then we need to strip off
        // the inner class name(s) to obtain the source file name.
		int tmpIndex = filename.indexOf('$');
		String srcFilename = tmpIndex >= 0 ? filename.substring(0, tmpIndex) : filename;

		for(File location : pkgInfo.locations) {
			try {
				if (location.getName().endsWith(".jar")) {
					// location is a jar file
					JarFile jf = new JarFile(location);
					JarEntry je = jf.getJarEntry(jarname);
					if (je == null) {
						continue;
					}
					ClassFileReader r = new ClassFileReader(jf.getInputStream(je));
					compiler.logTimedMessage("Loaded " + location + ":"
							+ jarname, System.currentTimeMillis() - time);
					Clazz clazz = r.readClass();
					// Update our knowledge base of classes.
					classtable.put(refName(clazz.type()), clazz);
					return clazz;
				} else {
					File classFile = new File(location.getPath(),filename + ".class");
					File srcFile = new File(location.getPath(),srcFilename + ".java");

					if (srcFile.exists()
							&& (!classFile.exists() || classFile.lastModified() < srcFile
									.lastModified())) {
						// Here, there is a source file, and either there is no class
						// file, or the class file is older than the source file.
						// Therefore, we need to (re)compile the source file.
						List<? extends Clazz> cs = compiler.parse(srcFile);

						for(Clazz c : cs) {
							if(refName(c.type()).equals(name)) {
								return c;
							}
						}
						throw new RuntimeException(
								"unreachable code reached!");
					} else if(classFile.exists()) {
						// Here, there is no sourcefile, but there is a classfile.
						// So, no need to compile --- just load the class file!
						ClassFileReader r = new ClassFileReader(new FileInputStream(classFile));

						Clazz clazz = r.readClass();

						compiler.logTimedMessage("Loaded " + classFile, System
								.currentTimeMillis()
								- time);

						// Update our knowledge base of classes.
						classtable.put(refName(clazz.type()), clazz);
						return clazz;
					}
				}
			} catch(IOException e) {
				// could possibly report stuff back to user here.
			}
		}
		return null;
	}

	/**
	 * This method simply adds a class definition to the classtable. This is
	 * needed for when a class is being compiled, since we cannot simply load
	 * the class details from the bytecode as this doesn't exist!
	 *
	 * @param jilClasses -
	 *            The classes being added.
	 */
	public void register(List<? extends Clazz> jilClasses) {
		for(Clazz f : jilClasses) {
			register(f);
		}
	}


	/**
	 * This method simply adds a class definition to the classtable. This is
	 * needed for when a class is being compiled, since we cannot simply load
	 * the class details from the bytecode as this doesn't exist!
	 *
	 * @param jilClasses -
	 *            The classes being added.
	 */
	public void register(Clazz jilClass) {
		PackageInfo pkgInfo = resolvePackage(jilClass.type().pkg());
		String rn = refName(jilClass.type());
		String pc = pathChild(rn);
		classtable.put(rn, jilClass);
		// Need to do this to indicate that the source file in question has
		// being compiled. Otherwise, we end up with an infinite loop of
		// class loading.

		if (pkgInfo == null) {
			ErrorHandler.handleError(ErrorHandler.ErrorType.PACKAGE_NOT_FOUND,
					new PackageNotFoundException(jilClass,
							Collections.unmodifiableList(classpath), Collections.unmodifiableList(sourcepath)), null);
		}
		pkgInfo.classes.add(pc);
		pkgInfo.compiledClasses.add(pc);
	}

	/**
	 * Given a path string of the form "xxx.yyy.zzz" this returns the parent
	 * component (i.e. "xxx.yyy"). If you supply "xxx", then the path parent is
	 * "". However, the path parent of "" is null.
	 *
	 * @param pkg
	 * @return
	 */
	public static String pathParent(String pkg) {
		if(pkg.equals("")) {
			return null;
		}
		int idx = pkg.lastIndexOf('.');
		if(idx == -1) {
			return "";
		} else {
			return pkg.substring(0,idx);
		}
	}

	/**
	 * Given a path string of the form "xxx.yyy.zzz" this returns the child
	 * component (i.e. "zzz")
	 *
	 * @param pkg
	 * @return
	 */
	public static String pathChild(String pkg) {
		int idx = pkg.lastIndexOf('.');
		if(idx == -1) {
			return pkg;
		} else {
			return pkg.substring(idx+1);
		}
	}

	/**
     * The purpose of this method is to determine which method is actually
     * called for a given invoke instruction.
     */
	public Pair<Clazz, Clazz.Method> determineMethod(Type.Reference receiver,
			String name, Type.Function funType) throws ClassNotFoundException,
			MethodNotFoundException {

		Type.Function ftype = Types.stripGenerics(funType);

		Stack<Type.Clazz> worklist = new Stack<Type.Clazz>();
		Stack<Type.Clazz> interfaceWorklist = new Stack<Type.Clazz>();

		initDetermineMethodWorklist(receiver,worklist);

		if (receiver instanceof Type.Clazz
				&& (name.equals("super") || name.equals("this"))) {
			Type.Clazz r = (Type.Clazz) receiver;
			name = r.lastComponent().first();
		}

		while (!worklist.isEmpty()) {
			Clazz c = loadClass(worklist.pop());
			for (Clazz.Method m : c.methods(name)) {
				Type.Function mtype = Types.stripGenerics(m.type());
				if (ftype.equals(mtype)) {
					return new Pair(c,m);
				}
			}
			if(c.superClass() != null) {
				worklist.push(c.superClass());
			}
			for(Type.Clazz i : c.interfaces()) {
				interfaceWorklist.push(i);
			}
		}

		while (!interfaceWorklist.isEmpty()) {
			Clazz c = loadClass(interfaceWorklist.pop());
			for (Clazz.Method m : c.methods(name)) {
				Type.Function mtype = Types.stripGenerics(m.type());
				if (ftype.equals(mtype)) {
					return new Pair(c,m);
				}
			}
			for(Type.Clazz i : c.interfaces()) {
				interfaceWorklist.push(i);
			}
		}

			throw new MethodNotFoundException(name, receiver, funType.parameterTypes(), this);

	}

	protected void initDetermineMethodWorklist(Type.Reference receiver, Stack<Type.Clazz> worklist) {
		if(receiver instanceof Type.Clazz) {
			worklist.push((Type.Clazz) receiver);
		} else if(receiver instanceof Type.Variable) {
			Type.Variable tv = (Type.Variable) receiver;
			if(tv.lowerBound() != null) {
				initDetermineMethodWorklist(tv.lowerBound(),worklist);
			} else {
				worklist.push(Types.JAVA_LANG_OBJECT); // fall back
			}
		} else if(receiver instanceof Type.Wildcard) {
			Type.Wildcard tv = (Type.Wildcard) receiver;
			if(tv.lowerBound() != null) {
				initDetermineMethodWorklist(tv.lowerBound(),worklist);
			} else {
				worklist.push(Types.JAVA_LANG_OBJECT); // fall back
			}
		} else if(receiver instanceof Type.Intersection) {
			Type.Intersection tv = (Type.Intersection) receiver;
			for(Type.Reference lb : tv.bounds()) {
				initDetermineMethodWorklist(lb,worklist);
			}
		} else {
			worklist.push(Types.JAVA_LANG_OBJECT); // fall back
		}
	}

	/**
	 * This method determines the actual type of a field. This is important,
	 * since the actual type and the bytecode type can differ in the case of
	 * generics. Thus, if we're loading a field of generic type, then we need a
	 * cast in the bytecode accordinly.
	 *
	 * @param t
	 * @return
	 */
	public Pair<Clazz,Clazz.Field> determineField(Type.Reference receiver, String name)
			throws ClassNotFoundException, FieldNotFoundException {

		Stack<Type.Clazz> worklist = new Stack<Type.Clazz>();
		Stack<Type.Clazz> interfaceWorklist = new Stack<Type.Clazz>();

		if(!(receiver instanceof Type.Clazz)) {
			receiver = Types.JAVA_LANG_OBJECT;
		}

		worklist.push((Type.Clazz) receiver);

		while (!worklist.isEmpty()) {
			Clazz c = loadClass(worklist.pop());
			Clazz.Field f = c.field(name);
			if (f != null) {
				return new Pair(c,f);
			}
			if(c.superClass() != null) {
				worklist.push(c.superClass());
			}
			for(Type.Clazz i : c.interfaces()) {
				interfaceWorklist.push(i);
			}
		}

		while (!interfaceWorklist.isEmpty()) {
			Clazz c = loadClass(interfaceWorklist.pop());
			Clazz.Field f = c.field(name);
			if (f != null) {
				return new Pair(c,f);
			}
			for(Type.Clazz i : c.interfaces()) {
				interfaceWorklist.push(i);
			}
		}

		throw new FieldNotFoundException(name,receiver, this);

	}

	/**
	 * This builds a list of all the known packages and the classes they
	 * contain.
	 */
	private final PackageInfo resolvePackage(String pkg) {
		// First, check if we have already resolved this package.
		PackageInfo pkgInfo = packages.get(pkg);

		if(pkgInfo != null && pkgInfo.fullyResolved) {
			return pkgInfo;
		} else if(failedPackages.contains(pkg)) {
			// yes, it's already been resolved but it doesn't exist.
			return null;
		}

		// package has not been previously resolved.
		String filePkg = pkg.replace('.', File.separatorChar);

		// First, consider source path
		for (String dir : sourcepath) {
			pkgInfo = lookForPackage(dir,pkg,filePkg);
			if(pkgInfo != null) {
				pkgInfo.fullyResolved = true;
				return pkgInfo;
			}
		}

		// second, try classpath
		for (String dir : classpath) {
			// check if classpath entry is a jarfile or a directory
			if (!dir.endsWith(".jar")) {
				// dir is not a Jar file, so I assume it's a directory.
				pkgInfo = lookForPackage(dir,pkg,filePkg);
				if(pkgInfo != null) {
					pkgInfo.fullyResolved = true;
					return pkgInfo;
				}
			}
		}

		failedPackages.add(pkg);
		return null;
	}

	protected void buildInitialPackageMap() {
		// This attempts to build an initial package map in order to prevent
        // lots of retraversing the class path.

		for (String dir : classpath) {
			// check if classpath entry is a jarfile or a directory
			if (dir.endsWith(".jar")) {
				try {
					JarFile jf = new JarFile(dir);
					for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements();) {
						JarEntry je = e.nextElement();
						String entryName = je.getName();
						if (entryName.endsWith(".class")) {
							String cname = pathParent(entryName.replace("/", "."));
							addPackageItem(cname,new File(dir), true);
						}
					}
				} catch (IOException e) {
					// jarfile listed on classpath doesn't exist!
					// So, silently ignore it (this is what javac does).
				}
			} else {
				// dir is not a Jar file, so it's a directory. We ignored these
                // here, and resolve them on demand. The reason for this is that
                // otherwise we'd have to traverse the entire subdirectory
                // starting at dir which is very expensive.
			}
		}
	}

	/**
	 * This traverses the directory tree, starting from dir, looking for class
	 * or java files. There's probably a bug if the directory tree is cyclic!
	 */
	private PackageInfo lookForPackage(String root, String pkg, String filepkg) {
		File f = new File(root + File.separatorChar + filepkg);
		if (f.isDirectory()) {
			for (String file : f.list()) {
				if (file.endsWith(".class") || file.endsWith(".java")) {
					if (pkg.equals("")) {
						addPackageItem(pathParent(file), new File(root),
								isCompiled(new File(f.getPath()
										+ File.separatorChar + file)));
					} else {
						addPackageItem(pkg + "." + pathParent(file), new File(
								root), isCompiled(new File(f.getPath()
								+ File.separatorChar + file)));
					}
				}
			}
		}

		return packages.get(pkg);
	}

	/**
	 * This adds a class to the list of known classes. The class name is given
	 * in the form "xxx.yyy$zzz". Here, "xxx" is the package name, "yyy" is the
	 * outerclass name and "zzz" is the inner class name.
	 *
	 * @param name
	 *            The name of the class to be added
	 * @param location
	 *            The location of the enclosing package. This is either a jar
	 *            file, or a directory.
	 */
	private void addPackageItem(String name, File pkgLocation, boolean isCompiled) {
		if(name == null) return;

		// this is a class file.
		String pkg = pathParent(name);
		String clazz = pathChild(name);

		if(pkg == null) { pkg = ""; } // default package

		PackageInfo items = packages.get(pkg);
		if (items == null) {
			items = new PackageInfo();
			packages.put(pkg, items);
		}

		// add the class in question
		if(items.classes.add(clazz)) {
			// The first time that we find this class, we need to check whether
			// or not it is compiled.
			if(isCompiled) {
				items.compiledClasses.add(clazz);
			}
		}

		// now, add the location (if it wasn't already added)
		if(!items.locations.contains(pkgLocation)) {
			items.locations.add(pkgLocation);
		}

		// Finally, add all enclosing packages of this package as
		// well. Otherwise, isPackage("java") can fails even when we know about
		// a particular package.
		pkg = pathParent(pkg);
		while (pkg != null) {
			if (packages.get(pkg) == null) {
				packages.put(pkg, new PackageInfo());
			}
			pkg = pathParent(pkg);
		}
	}

	/**
	 * Convert a class reference type into a proper name.
	 */
	String refName(Type.Clazz ref) {
		String descriptor = ref.pkg();
		if(!descriptor.equals("")) {
			descriptor += ".";
		}
		boolean firstTime=true;
		for(Pair<String,List<Type.Reference>> c : ref.components()) {
			if(!firstTime) {
				descriptor += "$";
			}
			firstTime=false;
			descriptor += c.first();
		}
		return descriptor;
	}

	/**
	 * This method simply checks whether or not a given file is "compiled". The
	 * file is either a source or class file, and this method checks whether the
	 * corresponding class file exists or not; furthermore, if it does exist
	 * then it checks whether or not it is older than the source file (if there
	 * is one).
	 *
	 * @param file
	 * @return
	 */
	private static boolean isCompiled(File file) {
		String filename = file.getPath();
		if(filename.endsWith(".class")) {
			// class file. construct source file by stripping off extension and
			// inner class identifiers
			filename = filename.substring(0,filename.length()-6);
			int idx = filename.indexOf('$');
			if(idx > 0) { filename = filename.substring(0,idx); }
			File srcFile = new File(filename + ".java");
			return !srcFile.exists() || (srcFile.lastModified() < file.lastModified());
		} else if(filename.endsWith(".java")){
			// source file
			File classFile = new File(filename.substring(0,filename.length()-5) + ".class");
			return file.lastModified() < classFile.lastModified();
		} else {
			throw new RuntimeException("Unknown file type encountered: " + file);
		}
	}


	/**
     * This method builds a default classpath, based upon the CLASSPATH
     * environment variable.
     *
     * @return
     */
	public static ArrayList<String> buildClassPath() {
		// Classpath hasn't been overriden by user, so import
		// from the environment.
		ArrayList<String> classPath = new ArrayList<String>();
		String cp = System.getenv("CLASSPATH");
		if (cp == null) {
			System.err
			.println("Warning: CLASSPATH environment variable not set");
		} else {
			// split classpath along appropriate separator
			Collections.addAll(classPath, cp.split(File.pathSeparator));
		}
		return classPath;
	}

	/**
     * This method builds a default bootclasspath, based upon the
     * sun.boot.class.path property.
     *
     * @return
     */
	public static ArrayList<String> buildBootClassPath() {
		// Boot class path hasn't been overriden by user, so employ the
		// default option.
		ArrayList<String> bootClassPath = new ArrayList<String>();
		String bcp = System.getProperty("sun.boot.class.path");
		// split classpath along appropriate separator
		Collections.addAll(bootClassPath, bcp.split(File.pathSeparator));
		return bootClassPath;
	}
}
