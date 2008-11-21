package jkit.java;

import java.io.*;
import java.lang.reflect.Modifier;

import jkit.bytecode.ClassFileReader;

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
		
		output.println("");
		
		for(JavaFile.Clazz decl : jf.classes()) {
			writeClass(decl,0);
		}
		
		output.flush();
	}
	
	public void writeClass(JavaFile.Clazz decl, int depth) {
		writeModifiers(decl.modifiers());
		output.print(decl.name());
		output.println(" {");
		output.println("}");
	}
	
	protected void writeModifiers(int modifiers) {
		if((modifiers & Modifier.STATIC)!=0) { output.print("static "); }
		if((modifiers & Modifier.ABSTRACT)!=0) { output.print("abstract "); }
		if((modifiers & Modifier.FINAL)!=0) { output.print("final "); }
		if((modifiers & Modifier.NATIVE)!=0) { output.print("native "); }
		if((modifiers & Modifier.PRIVATE)!=0) { output.print("private "); }
		if((modifiers & Modifier.PROTECTED)!=0) { output.print("protected "); }
		if((modifiers & Modifier.PUBLIC)!=0) { output.print("public "); }
		if((modifiers & Modifier.STRICT)!=0) { output.print("strict "); }
		if((modifiers & Modifier.SYNCHRONIZED)!=0) { output.print("synchronized "); }
		if((modifiers & Modifier.TRANSIENT)!=0) { output.print("transient "); }
		if((modifiers & Modifier.VOLATILE)!=0) { output.print("volatile "); }
		if((modifiers & ClassFileReader.ACC_VARARGS)!=0) { output.print("varargs "); }
	}
	
	protected void indent(int level) {
		for(int i=0;i!=level;++i) {
			output.print("  ");
		}
	}
}
