package whiley.testing;

import static org.junit.Assert.fail;

import java.io.*;
import whiley.Main;

public class TestHarness {
	private String srcPath;    // path to source files	
	private String outputPath; // path to output files
	private String outputExtension; // the extension of output files
	private boolean verification;
	
	/**
	 * Construct a test harness object.
	 * 
	 * @param srcPath
	 *            The path to the source files to be tested
	 * @param outputPath
	 *            The path to the sample output files to compare against.
	 * @param outputExtension
	 *            The extension of output files
	 * @param verification
	 *            if true, the verifier is used.
	 */
	public TestHarness(String srcPath, String outputPath,
			String outputExtension, boolean verification) {
		this.srcPath = srcPath.replace('/', File.separatorChar);
		this.outputPath = outputPath.replace('/', File.separatorChar);
		this.outputExtension = outputExtension;
		this.verification = verification;
	}
	
	/**
	 * Compile and execute a test case, whilst comparing its output against the
	 * sample output.
	 * 
	 * @param name
	 *            Name of the test to run. This must correspond to an executable
	 *            Java file in the srcPath of the same name.
	 */
	protected void runTest(String name) {				
		final String[] args = new String[verification ? 2 : 3];
		args[0] = "-proofs";		
		if(verification) {
			args[1] = srcPath + File.separatorChar + name + ".whiley";
		} else {			
			args[1] = "-nvc";
			args[2] = srcPath + File.separatorChar + name + ".whiley";
		}
		
		if(!Main.run(args)) { 
			fail("couldn't compile test!");
		} else {
			String output = run(args);				
			compare(output, outputPath + File.separatorChar + name
					+ "." + outputExtension);
		}
	}
	
	/**
	 * Attempt to compile a test case, and check that it fails as it should.
	 * 
	 * @param name
	 *            Name of the test to run. This must correspond to an executable
	 *            Java file in the srcPath of the same name.
	 */
	protected void compileFailTest(String name) {				
		final String[] args = new String[verification ? 2 : 3];
		args[0] = "-proofs";
		if(verification) {
			args[1] = srcPath + File.separatorChar + name + ".whiley";
		} else {		
			args[1] = "-nvc";
			args[2] = srcPath + File.separatorChar + name + ".whiley";
		}
		
		if(Main.run(args)) { 
			fail("Test compiled when it shouldn't have!");
		}
	}

	private static String run(String[] args) {
		try {
			String tmp = "java whiley/Main";
			for(String s : args) {
				tmp += " " + s;
			}
			
			Process p = Runtime.getRuntime().exec(tmp, null,
					new File("."));			
				
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
