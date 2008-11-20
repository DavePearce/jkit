package jkit.core;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import jkit.ClassReader;
import jkit.ClassWriter;
import jkit.jkil.Clazz;
import jkit.stages.Stage;
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
public class ClassCompiler {
	/**
	 * Represents a pipeline configuration, for a given input file type.
	 * 
	 * @author djp
	 * 
	 */
	public static class Pipeline {
		public Class<?> fileReader;
		public Stage[] stages;
		public Class<?> fileWriter;
		public String target;

		public Pipeline(Class<?> fileReader, Class<?> fileWriter, String target, Stage... stages) {
			this.fileReader = fileReader;
			this.fileWriter = fileWriter;
			this.stages = stages;
			this.target = target;
		}
	}
	
	/**
	 * The pipelines map maps source file extensions (e.g. "java", "mocha") to
	 * the pipeline configurations needed for compiling them.
	 */
	private HashMap<String, Pipeline> pipelines;
	
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
	 * forces the compilation of "C2.java"; then, "C2.java" is automatically
	 * removed from the queue, thus preventing it from being compiled again.
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
	 * @param pipelines
	 *            A map from extensions (e.g. "java", "mocha") to pipelines for
	 *            compiling source files with those extensions.
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 */	
	public ClassCompiler(HashMap<String, Pipeline> pipelines, List<String> classpath) {
		this.pipelines = pipelines;		
		this.loader = new ClassLoader(classpath,this);
	}
	
	/**
	 * @param pipelines
	 *            A map from extensions (e.g. "java", "mocha") to pipelines for
	 *            compiling source files with those extensions.
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 * @param logout
	 *            A stream where log messages are sent
	 */	
	public ClassCompiler(HashMap<String, Pipeline> pipelines,
			List<String> classpath, OutputStream logout) {
		this.pipelines = pipelines;
		this.loader = new ClassLoader(classpath, this, logout);
		this.logout = new PrintStream(logout);
	}
	
	/**
	 * @param pipelines
	 *            A map from extensions (e.g. "java", "mocha") to pipelines for
	 *            compiling source files with those extensions.
	 * @param sourcepath
	 *            a list of directory and/or jar file locations.
	 * @param classpath
	 *            A list of directory and/or jar file locations.
	 * @param logout
	 *            A stream where log messages are sent
	 */	
	public ClassCompiler(HashMap<String, Pipeline> pipelines,
			List<String> sourcepath, List<String> classpath, OutputStream logout) {
		this.pipelines = pipelines;
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
		
		String fileExtension = filename.substring(filename.lastIndexOf(".") + 1);	
		
		ClassCompiler.Pipeline pipeline = pipelines.get(fileExtension);
		
		if (pipeline == null) {
			// problem, no pipeline for this file type!
			throw new IllegalArgumentException(
					"Error: Unable to process files with extension \"."
							+ fileExtension + "\" (" + filename + ")");
		}
						
		try {
			long start = System.currentTimeMillis();
			
			// Now, construct the reader!
			File srcFile = new File(filename); 
			ClassReader reader = constructReader(
					new FileInputStream(srcFile), loader, pipeline.fileReader);

			long last = System.currentTimeMillis();
			logout.println("Parsed " + filename + " [" + (last - start) + "ms]");
			
			// First we must read the skeletons
			List<Clazz> classes = reader.readSkeletons();

			for(Clazz c : classes) {
				logout.println("Parsed skeleton " + c.name());
				loader.updateInfo(c); 
			}

			// Second, read the full source of each class
			classes = reader.readClasses();
			for(Clazz c : classes) {
				logout.println("Parsed class " + c.name());
				loader.updateInfo(c); 
			}

			// Third, apply the appropriate pipeline stages
			for (Clazz c : classes) {				
				// apply each stage in the pipeline ...
				for (Stage s : pipeline.stages) {

					s.apply(c);

					long next = System.currentTimeMillis();					
					logout.println("Applied stage "
							+ s.getClass().getName() + " [" + (next - start)
							+ "ms]");					
					start = next;
				}
								
				String outfile;
				if(outputDirectory == null) {
					String parent = srcFile.getParent();
					outfile = parent == null ? "" : parent;
				} else { 
					outfile =  outputDirectory.getPath() + File.separatorChar;
					outfile += c.type().pkg().replace('.',File.separatorChar);
				}
									
				if(!outfile.equals("")) { outfile += File.separatorChar; }
				boolean firstTime = true;
				for (Pair<String, jkit.jkil.Type[]> p : c.type().classes()) {
					if (!firstTime) {
						outfile += "$";
					}
					firstTime = false;
					outfile = outfile + p.first();
				}
				outfile += "." + pipeline.target;
				OutputStream fos = new FileOutputStream(outfile);
				ClassWriter writer = constructWriter(fos, pipeline.fileWriter);

				writer.writeClass(c);

				long next = System.currentTimeMillis();				
				logout.println("Written " + outfile + " ["
						+ (next - start) + "ms]");
			}
			
			compilationQueue.remove(filename);
			compiling.remove(filename);
			
			return classes;			
		} catch(IllegalAccessException e) {
			throw new RuntimeException("Compilation failure",e);
		} catch(InvocationTargetException e) {
			Throwable re = e.getTargetException(); 
			if(re instanceof SyntaxError) {
				SyntaxError se = (SyntaxError) re;	
				se.filename = filename;
				throw se;
			} 
			throw new RuntimeException("Compilation failure",e);
		} catch(InstantiationException e) {
			throw new RuntimeException("Compilation failure",e);
		} catch(SyntaxError e) {
			if(e.filename.equals("unknown")) {
				e.filename = filename;
			}
			throw e;
		} catch(InternalException e) {
			if(e.point().source().equals("unknown")) {
				e.point().setSource(filename);
			}
			throw e;
		} 
	}
	
	public static ClassReader constructReader(InputStream fis,
			jkit.core.ClassLoader loader, Class<?> readerClass)
			throws IllegalAccessException, InvocationTargetException,
			InstantiationException {

		for (java.lang.reflect.Constructor<?> c : readerClass.getConstructors()) {
			Class<?>[] params = c.getParameterTypes();
			if (params != null && params.length == 2
					&& params[0].equals(InputStream.class)
					&& params[1].equals(jkit.core.ClassLoader.class)) {				
				return (ClassReader) c.newInstance(fis,loader);
			}
		}
		throw new RuntimeException("Unable to construct ClassReader");
	}

	public static ClassWriter constructWriter(OutputStream fos,
			Class<?> readerClass) throws IllegalAccessException,
			InvocationTargetException, InstantiationException {

		for (java.lang.reflect.Constructor<?> c : readerClass.getConstructors()) {
			Class<?>[] params = c.getParameterTypes();
			if (params != null && params.length == 1
					&& params[0].equals(OutputStream.class)) {
				return (ClassWriter) c.newInstance(fos);
			}
		}
		throw new RuntimeException("Unable to construct ClassWriter");
	}
}
