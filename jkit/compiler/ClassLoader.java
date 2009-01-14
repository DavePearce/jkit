package jkit.compiler;

import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.*;

import jkit.bytecode.ClassFileReader;
import jkit.jil.Clazz;
import jkit.jil.Type;
import jkit.util.Pair;

/**
 * A ClassLoader is responsible for loading classes from the filesystem. Classes
 * are located in either jar files or directories; furthermore, classes may be
 * in source or binary (i.e. compiled) form. The ClassLoader will search the
 * classpath and/or sourcepath to find classes and (if necessary) compile them. 
 * 
 * @author djp
 */
public class ClassLoader {
	
	/**
	 * The class path is a list of directories and Jar files which must be
	 * searched in ascending order for *class files*.
	 */
	private ArrayList<String> classpath = new ArrayList<String>();

	/**
	 * The source path is a list of directories and Jar files which must be
	 * search in ascending order for *source files*. By default, it is the same
	 * as the classpath, although this can be overriden (e.g. by using the
	 * "-sourcepath <path>" command-line option in jkit or javac).
	 */
	private ArrayList<String> sourcepath;
	
	/**
	 * A map from class names in the form "xxx.yyy$zzz to Clazz objects. This is
	 * the master cache of classes which have been loaded during the compilation
	 * process. Once a class has been entered into the classtable, it will not
	 * be loaded again.
	 */
	private HashMap<String,Clazz> classtable = new HashMap<String,Clazz>();
	
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
	private class PackageInfo {
		/**
		 * The classes field contains those classes contained in this package.
		 * Class names are represented as strings of the form "xxx$yyy".
		 */
		public HashSet<String> classes = new HashSet<String>();
		
		/**
		 * The compiledClasses indicates which classes are definitely compiled.
		 * This is useful for detecting classes that need to be compiled in
		 * order to correctly resolve types.
		 */
		public HashSet<String> compiledClasses = new HashSet<String>();
		
		/**
		 * The locations list contains the list of locations that have been
		 * identified for a particular package. Each location identifies either
		 * a jar file, or a directory. The order of locations found is
		 * important --- those which come first have higher priority.
		 */
		public ArrayList<File> locations = new ArrayList<File>();
	}
	
	/**
	 * The packages map maps each package to the classes they contain. For
	 * example, "java.lang" will map to "String", "Integer" etc. Inner classes
	 * are appended to their enclosing class using "$" and located in their
	 * package accordingly. Therefore, for the class "java.util.Map.Entry" there
	 * will be an entry "Map$Entry" in the "java.util" package.
	 */
	private HashMap<String, PackageInfo> packages = new HashMap<String, PackageInfo>();
	
	/**
	 * The ClassCompiler is needed for compiling source files found on the
	 * sourcepath which are needed to identify inner classes appropriately.
	 */
	private Compiler compiler = null;			
	
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
		
		buildPackageMap();		
	}
	
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
	 * @param logout
	 *            A stream where log messages are sent
	 */
	public ClassLoader(List<String> classpath, Compiler compiler, OutputStream logout) {
		this.classpath = new ArrayList<String>(classpath);
		this.sourcepath = new ArrayList<String>(classpath);
		this.logout = new PrintStream(logout);
		this.compiler = compiler;
		
		buildPackageMap();
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
	 * @param logout
	 *            an output stream to which logging information is written.
	 */		
	public ClassLoader(List<String> sourcepath, List<String> classpath,
			Compiler compiler, OutputStream logout) {		
		this.sourcepath = new ArrayList<String>(sourcepath);
		this.classpath = new ArrayList<String>(classpath);
		this.compiler = compiler;
		this.logout = new PrintStream(logout);
				
		buildPackageMap();
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
		// I'm a little suspect about this method. I think it at least needs to
		// take an import list
		return packages.keySet().contains(pkg);
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
						
		for (String imp : imports) {
			Type.Clazz ref = null;
			if (imp.endsWith(".*")) {
				// try and resolve the class
				ref = resolveClassName(imp.substring(0, imp.length() - 2),className);												
			} else if (imp.endsWith("." + className)) {				
				// strip off class name itself				
				String pkg = imp.substring(0, imp.length()
						- (1 + className.length()));
				// now try and resolve it.
				ref = resolveClassName(pkg,className);				
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
		ArrayList<Pair<String,List<Type.Reference>>> classes = new ArrayList<Pair<String,List<Type.Reference>>>();
		classes.add(new Pair<String, List<Type.Reference>>(className,new ArrayList<Type.Reference>()));
		String fullClassName = className;
		String outerClassName = className;
		while(pkg != null) {			
			PackageInfo pkgInfo = packages.get(pkg);
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
					String ocn = pkg == "" ? outerClassName : pkg + "." + outerClassName;
					loadClass(ocn,pkgInfo); // this will force a compile					
					continue; // try again for the same class/pkg combination
				} else {
					break;
				}
			} else {
				// This import does not correspond to a valid package.
				// Therefore, it may be specifying an inner class and we need to check.
				outerClassName = pathChild(pkg);
				fullClassName = outerClassName +"$" + fullClassName;
				classes.add(0,new Pair<String, List<Type.Reference>>(pathChild(pkg),new ArrayList<Type.Reference>()));				
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
		PackageInfo pkgInfo = packages.get(ref.pkg());
		
		if (pkgInfo == null) { throw new ClassNotFoundException("Unable to load class " + name); }
						
		c = loadClass(name,pkgInfo);

		if(c == null) { throw new ClassNotFoundException("Unable to load class " + name); }
		
		if(ref.components().size() > 1) {			
			// Now, if this is an inner class, we need to load it's parent, so
			// that we can finalise this classes modifiers.
			List<Pair<String,List<Type.Reference>>> nclasses = new ArrayList<Pair<String,List<Type.Reference>>>(ref.components());			
			Clazz parent = loadClass(new Type.Clazz(ref.pkg(), nclasses));

			/**
			 * INNER CLASSES ARE BEING IGNORED FOR NOW --- djp
			 * 
			// now, iterate parent's inner classes to find this one
			for (Triple<Type.Reference, Integer, Boolean> i : parent.inners()) {
				if (i.first().supsetEqOf(ref)) {
					// found it
					c.setModifiers(c.modifiers() | i.second());
					c.setAnonymous(i.third());
					return c;
				}
			}	
			*/								
		}
				
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
						return null; 
					}  
					ClassFileReader r = new ClassFileReader(jf.getInputStream(je));
					time = System.currentTimeMillis() - time;
					logout.println("Loaded skeletons " + location + ":"
							+ jarname + " [" + time + "ms]");
					Clazz clazz = r.readClass();
					// Update our knowledge base of classes.					
					classtable.put(refName(clazz.type()), clazz);					
					return clazz;
				} else {
					File classFile = new File(filename + ".class");					
					File srcFile = new File(srcFilename + ".java");
					
					if (srcFile.exists()
							&& (!classFile.exists() || classFile.lastModified() < srcFile
									.lastModified())) {
						// Here, there is a source file, and either there is no class
						// file, or the class file is older than the source file.
						// Therefore, we need to (re)compile the source file.
						time = System.currentTimeMillis() - time;						
						List<Clazz> cs = compiler.compile(srcFile.getPath());

						// ADDED TO RESOLVE INFINITE COMPILATION LOOP. 
						if (cs.size() == 0) { continue; }
						
						logout.println("Compiled " + srcFile + " [" + time + "ms]");						
						// Add all classes coming out of the src file into the
						// classtable, and register that they have been compiled.
						for(Clazz c : cs) {
							String n = pathChild(refName(c.type()));
							pkgInfo.classes.add(n);
							pkgInfo.compiledClasses.add(n);
							classtable.put(n,c);
						}
						
						return cs.get(0);
					} else if(classFile.exists()) {
						// Here, there is no sourcefile, but there is a classfile.
						// So, no need to compile --- just load the class file!
						ClassFileReader r = new ClassFileReader(new FileInputStream(classFile));
						time = System.currentTimeMillis() - time;
						logout.println("Loaded skeletons " + classFile + " [" + time + "ms]");
						Clazz clazz = r.readClass();
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
	 * Given a path string of the form "xxx.yyy.zzz" this returns the parent
	 * component (i.e. "xxx.yyy")
	 * 
	 * @param pkg
	 * @return
	 */
	private String pathParent(String pkg) {
		int idx = pkg.lastIndexOf('.');
		if(idx == -1) {
			return null;
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
	private String pathChild(String pkg) {
		int idx = pkg.lastIndexOf('.');
		if(idx == -1) {
			return pkg;
		} else {
			return pkg.substring(idx+1);
		}
	}
	
	/**
	 * This builds a list of all the known packages and the classes they
	 * contain.
	 */
	private void buildPackageMap() {
		// BUG: there's a bug here, since the sourcepath is not considered.
		for (String dir : classpath) {
			// check if classpath entry is a jarfile or a directory
			if (dir.endsWith(".jar")) {
				try {
					JarFile jf = new JarFile(dir);
					for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements();) {
						JarEntry je = e.nextElement();
						String tmp = je.getName().replace("/", ".");
						// i'm not sure what should happen if a jarfile contains
						// a source file which is older than the classfile!
						addPackageItem(pathParent(tmp),new File(dir), true);
					}
				} catch (IOException e) {
					// jarfile listed on classpath doesn't exist!
					// So, silently ignore it (this is what javac does).
				}
			} else {
				// dir is not a Jar file, so I assume it's a directory.
				recurseDirectoryForClasses(dir, "");
			}
		}
	}
	
	/**
	 * This traverses the directory tree, starting from dir, looking for class
	 * or java files. There's probably a bug if the directory tree is cyclic!
	 */
	private void recurseDirectoryForClasses(String root, String dir) {
		File f = new File(root + File.separatorChar + dir);
		if (f.isDirectory()) {
			for (String file : f.list()) {
				if(file.endsWith(".class") || file.endsWith(".java")) {	
					if (dir.length() == 0) {
						addPackageItem(pathParent(file), new File(root),
								isCompiled(new File(f.getPath()
										+ File.separatorChar + file)));
					} else {
						addPackageItem(dir.replace(File.separatorChar, '.')
								+ "." + pathParent(file), new File(root),
								isCompiled(new File(f.getPath()
										+ File.separatorChar + file)));
					}
				} else {
					if (dir.length() == 0) {
						recurseDirectoryForClasses(root,
								file);
					} else {
						recurseDirectoryForClasses(root,
								dir + File.separatorChar + file);
					}
				}
			}
		}
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
		for(Pair<String,List<Type.Reference>> c : ref.components()) {
			if(!descriptor.equals("")) {
				descriptor += ".";
			}
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
}
