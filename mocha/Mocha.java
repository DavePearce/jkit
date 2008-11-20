package mocha;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import mocha.core.Inferrer;
import mocha.core.MochaTyping;
import mocha.core.MochaRetype;
import mocha.core.Inferrer.InferForEachTrans;
import mocha.io.MochaFileReader;

import jkit.Main.Pipeline;
import jkit.bytecode.ClassFileWriter;
import jkit.core.ClassTable;
import jkit.core.Clazz;
import jkit.core.SyntaxError;
import jkit.io.JavaFileWriter;
import jkit.java.JavaFileReader;

import jkit.ClassReader;
import jkit.Main;

public class Mocha {
	
	private static Inferrer inf;
	
	public static void main(String[] args) {
		ArrayList<String> classPath = null;
		ArrayList<String> bootClassPath = null;
		boolean verbose = false;
		boolean jkitonly = false;
		boolean writeres = false;
		// ======================================================
		// ======== First, parse command-line arguments =========
		// ======================================================		
		
		int fileArgsBegin = -1;
		for(int i=0;i!=args.length;++i) {
			if(args[i].startsWith("-")) {								
				if(args[i].equals("-cp")) {					
					classPath = new ArrayList<String>();
					// split classpath along appropriate separator
					Collections.addAll(classPath,args[++i].split(File.pathSeparator));
				} else if(args[i].equals("-verbose")) {
					verbose = true;
				} else if(args[i].equals("-jkit")) {
					jkitonly = true;
				} else if(args[i].equals("-write")) {
					writeres = true;
				} else {						
					throw new RuntimeException("Unknown option: " + args[i]);
				} 
			} else {
				fileArgsBegin = i;
				break;
			}
		}
		
		inf = new Inferrer(verbose, writeres);
		
		// ======================================================
		// ========== Second, setup classpath properly ==========
		// ======================================================
		
		if (classPath == null) {
			classPath = Main.buildClassPath();
		}
		bootClassPath = Main.buildBootClassPath();

		classPath.addAll(bootClassPath);
		ClassTable.setClassPath(classPath);
		
		// ======================================================
		// ============ Third, compile source files =============
		// ======================================================
		
		MochaTyping typing = new MochaTyping(inf, verbose);
		MochaRetype retype = new MochaRetype(inf);
		InferForEachTrans trans = new InferForEachTrans(inf);
		
		Pipeline p = null;
		Class readClass = null;
		
		if(jkitonly) {
			p = new Pipeline(JavaFileReader.class,
					ClassFileWriter.class,
					new jkit.stages.codegen.FieldInitialisation(),
					new jkit.stages.codegen.Typing(),
					new jkit.stages.codegen.ForEachLoop(),
					new jkit.stages.codegen.Exceptions(),
					new jkit.stages.checks.Subtyping(),
					new jkit.stages.checks.VariableDefinitions());
			readClass = JavaFileReader.class;
		} else {
			p = new Pipeline(MochaFileReader.class, 
							ClassFileWriter.class, 
							new jkit.stages.codegen.FieldInitialisation(),
							new jkit.stages.codegen.AnonClass(),
							inf,
							trans,
							retype, 
							typing,
							new jkit.stages.codegen.SwitchConstants());
			readClass = MochaFileReader.class;
		}

		
		ClassReader[] readers = new ClassReader[args.length - fileArgsBegin];
		
		for(int i = fileArgsBegin; i < args.length; ++i) {
			String fileArg = args[i];
			
			try {
				long start = System.currentTimeMillis();

				ClassReader reader = Main.constructReader(
						new FileInputStream(fileArg), readClass);

				long last = System.currentTimeMillis();
				if (verbose) {
					System.err.println("Parsed " + fileArg + " ["
							+ (last - start) + "ms]");
				}

				// First, read in all the classes from the source file
				List<Clazz> classes = reader.readSkeletons();

				for(Clazz c : classes) {
					if(verbose) {
						System.err.println("Loaded skeleton of " + c.name());
					}
					ClassTable.add(c);
				}

				readers[i-fileArgsBegin] = reader; // save for later!				
			} catch(InvocationTargetException e) {
				Throwable re = e.getTargetException(); 
				if(re instanceof SyntaxError) {
					SyntaxError se = (SyntaxError) re;
					Main.outputSourceError(fileArg, se.line(), se.column(), se.width(), se.getMessage());
					if(verbose) { se.printStackTrace(System.err); }
				} else {
					// Unsure what to do here.
					System.err.println("Error: problem compiling file \"" + fileArg
						+ "\" (" + e.getMessage() + ")");
					if(verbose) { e.printStackTrace(System.err); }			
					System.exit(1);
				}
			} catch (FileNotFoundException e) {
				System.err.println("Error: unable to read file \"" + fileArg
						+ "\"");
				if(verbose) { e.printStackTrace(System.err); }
				System.exit(1);
			} catch (SyntaxError e) {
				Main.outputSourceError(fileArg, e.line(), e.column(), e.width(), e.getMessage());
				if(verbose) { e.printStackTrace(System.err); }
			} catch (IOException e) {
				System.err.println("Error: I/O error reading file \"" + fileArg
						+ "\"");			
				if(verbose) { e.printStackTrace(System.err); }
				System.exit(1);
			} catch (Exception e) {		
				System.err.println("Error: problem compiling file \"" + fileArg
						+ "\" (" + e.getMessage() + ")");
				if(verbose) { e.printStackTrace(System.err); }			
				System.exit(1);
			}
			
			
		}
		
		for(int i=fileArgsBegin;i!=args.length;++i) {
			String fileArg = args[i];
			
			if(fileArg.endsWith(".java")) {
				File outfile = new File(fileArg);
				Main.processPipeline(fileArg, readers[i - fileArgsBegin], outfile.getParent(), "class", p, verbose);
			} else {
				System.err.println("Error: unknown source file \"" + fileArg + "\".");
				System.exit(1);
			}
		}
	}
}
