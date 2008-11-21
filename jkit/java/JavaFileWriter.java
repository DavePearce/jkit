package jkit.java;

import java.io.*;

public class JavaFileWriter {
	private PrintWriter output;
	
	public JavaFileWriter(Writer out) {
		output = new PrintWriter(out);
	}
	
	public JavaFileWriter(OutputStream out) {
		output = new PrintWriter(out);
	}
	
	public void write(JavaFile jf) {
		if(!jf.pkg().equals("")) {
			output.println("package " + jf.pkg() + ";");
		}
		
		for(String imp : jf.imports()) {
			output.println("import " + imp + ";");
		}
		
		output.flush();
	}
}
