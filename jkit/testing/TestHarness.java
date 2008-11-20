package jkit.testing;

import static org.junit.Assert.fail;

import jkit.*;
import java.io.*;
import java.lang.reflect.*;

public class TestHarness {
	private String srcPath;    // path to source files
	private String srcExtension; // the extension of source files
	private String outputPath; // path to output files
	private String outputExtension; // the extension of output files
	
	/**
	 * Construct a test harness object.
	 * 
	 * @param srcPath
	 *            The path to the source files to be tested
	 * @param srcExtension
	 *            The extension of source files (e.g. "java").
	 * @param outputPath
	 *            The path to the sample output files to compare against.
	 * @param outputExtension
	 *            The extension of output files
	 */
	public TestHarness(String srcPath, String srcExtension, String outputPath,
			String outputExtension) {
		this.srcPath = srcPath.replace('/', File.separatorChar);
		this.srcExtension = srcExtension;
		this.outputPath = outputPath.replace('/', File.separatorChar);
		this.outputExtension = outputExtension;
	}
	
	/**
	 * Compile and execute a test case, whilst comparing its output against the
	 * sample output.
	 * 
	 * @param name
	 *            Name of the test to run. This must correspond to an executable
	 *            Java file in the srcPath of the same name.
	 */
	protected void runTest(String name, String... args) {		
		if(!compile(srcPath, name)) {
			fail("Unable to compile test!");
		}
			
		String output = run(srcPath, name, args);				
		compare(output, outputPath + File.separatorChar + name
				+ "." + outputExtension);
	}
	
	/**
	 * Attempt to compile a test case, and check that it fails as it should.
	 * 
	 * @param name
	 *            Name of the test to run. This must correspond to an executable
	 *            Java file in the srcPath of the same name.
	 */
	protected void compileFailTest(String name, String... args) {		
		if(compile(srcPath, name)) {
			fail("Test compiled when it shouldn't have!");
		}
	}
	
	/**
	 * Compile a test file in a given path.
	 * 
	 * @param path
	 *            The path to the source file (i.e. the directory it's in). This
	 *            path must use the appropriate file separator for the host
	 *            operating system.
	 * @param name
	 *            The name of the source file including any package specifies
	 *            (but excluding the extension)
	 */
	private boolean compile(String path, String name) {		
		String classpath = System.getenv("CLASSPATH");		
		final String[] args = new String[3];
		args[0] = "-cp";
		args[1] = path + ";" + classpath;
		args[2] = path + File.separatorChar + name + "." + srcExtension;		
		
		return new Main().compile(args);
	}
	
	/**
	 * Execute a given class file
	 * 
	 * @param path
	 *            The path to the class file (i.e. the directory it's in). This
	 *            path must use the appropriate file separator for the host
	 *            operating system.
	 * @param class
	 *            The name of the class file including any package specifies
	 *            (but excluding the ".class" extension)
	 * 
	 * @return The stdout generated from executing this program.
	 */
	private static String run(String path, String classFile, String... args) {
		try {			
			String tmp = "java " + classFile;
			for(String a : args) {
				tmp += " " + a;
			}
			
			Process p = Runtime.getRuntime().exec(tmp, null,
					new File(path));			
				
			StringBuffer syserr = new StringBuffer();
			StringBuffer sysout = new StringBuffer();
			new StreamGrabber(p.getErrorStream(),syserr);
			new StreamGrabber(p.getInputStream(),sysout);
			p.waitFor();
			System.err.println(syserr);
			return sysout.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Problem running compiled test");
		}
		
		return null;
	}
	
	/**
	 * Compare the output of executing java on the test case with a reference
	 * file.
	 * 
	 * @param output
	 *            This provides the output from executing java on the test case.
	 * @param referenceFile
	 *            The full path to the reference file. This should use the
	 *            appropriate separator char for the host operating system.
	 */
	private static void compare(String output, String referenceFile) {
		try {			
			BufferedReader outReader = new BufferedReader(new StringReader(output));
			BufferedReader refReader = new BufferedReader(new FileReader(
					new File(referenceFile)));
			
			while (refReader.ready() && outReader.ready()) {
				String a = refReader.readLine();
				String b = outReader.readLine();
				
				if (a.equals(b)) {
					continue;
				} else {
					System.err.println(" > " + a);
					System.err.println(" < " + b);
					throw new Error("Output doesn't match reference");
				}
			}
						
			String l1 = outReader.readLine();
			String l2 = refReader.readLine();
			if (l1 == null && l2 == null) return;
			do {
				l1 = outReader.readLine();
				l2 = refReader.readLine();
				if (l1 != null) {
					System.err.println(" < " + l1);
				} else if (l2 != null) {
					System.err.println(" > " + l2);
				}
			} while(l1 != null && l2 != null);			
			
			fail("Files do not match");
		} catch (Exception ex) {
			ex.printStackTrace();
			fail();
		}
	}
	
	static public class StreamGrabber extends Thread {
		private InputStream input;
		private StringBuffer buffer;

		StreamGrabber(InputStream input,StringBuffer buffer) {
			this.input = input;
			this.buffer = buffer;
			start();
		}

		public void run() {
			try {
				int nextChar;
				// keep reading!!
				while ((nextChar = input.read()) != -1) {
					buffer.append((char) nextChar);
				}
			} catch (IOException ioe) {
			}
		}
	}
}
