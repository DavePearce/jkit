package jkit;

import jkit.java.*;
import jkit.java.stages.*;
import jkit.compiler.*;

import java.io.*;

public class Test {

	public static void main(String[] args) {
		String outputdir = null;

		int i;

		for (i = 0; i != args.length; ++i) {
			if (args[i].equals("-o")) {
				outputdir = args[i + 1];
				i = i + 1;
			} else {
				break;
			}
		}

		for (; i != args.length; ++i) {
			try {
				JavaFile f = new JavaFileReader2(args[i]).read();
				new TypingChecking(null).apply(f);
				
				if(outputdir != null) {
					File path = new File(outputdir + File.separatorChar + args[i])
					.getParentFile();
					if (!path.exists()) {
						path.mkdirs();
					}
					FileWriter writer = new FileWriter(outputdir
							+ File.separatorChar + args[i]);			
					
					new JavaFileWriter(writer).write(f);
				} else {
					new JavaFileWriter(System.out).write(f);
				}
			} catch (IOException e) {
				System.out.println("ERROR: " + e);
			} catch (SyntaxError e) {
				System.err.println(args[i] + ":" + e.line() + ": "
						+ e.getMessage());
				e.printStackTrace(System.err);
			} catch(Exception e) {
				System.err.println("Error: " + args[i]);
				e.printStackTrace(System.err);
			}
		}
	}

	public static void outputSourceError(String fileArg, int line, int col,
			int width, String message) {
		System.err.println(fileArg + ":" + line + ": " + message);
		String l = readLine(fileArg, line);
		System.err.println(l);
		for (int j = 0; j < col; ++j) {
			if (l.charAt(j) == '\t') {
				System.err.print("\t");
			} else {
				System.err.print(" ");
			}
		}
		for (int j = 0; j < width; ++j)
			System.err.print("^");
		System.err.println("");
	}

	public static String readLine(String f, int l) {
		try {
			return readLine(new FileReader(f), l);
		} catch (IOException e) {
			// shouldn't get here.
			return "";
		}
	}

	public static String readLine(Reader in, int l) {
		try {
			LineNumberReader lin = new LineNumberReader(in);
			String line = "";
			while (lin.getLineNumber() < l) {
				line = lin.readLine();
			}
			return line;
		} catch (IOException e) {
			return "";
		}
	}
}
