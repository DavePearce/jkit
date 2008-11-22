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
		
		output.println("");
		
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
		indent(depth);
		writeModifiers(decl.modifiers());
		
		if(decl.isInterface()) {
			output.print("interface ");
		} else {
			output.print("class ");
		}
		
		output.print(decl.name());
		
		if(decl.isInterface()) {
			if(decl.interfaces().size() > 0) {
				output.print(" extends ");
				boolean firstTime = true;
				for(JavaFile.Type i : decl.interfaces()) {
					if(!firstTime) {
						output.print(", ");
					} else { firstTime = false; }
					writeType(i);
				}
			}
		} else {
			if(decl.superclass() != null) {
				output.print(" extends ");
				writeType(decl.superclass());
			}
			if(decl.interfaces().size() > 0) {
				output.print(" implements ");
				boolean firstTime = true;
				for(JavaFile.Type i : decl.interfaces()) {
					if(!firstTime) {
						output.print(", ");
					} else { firstTime = false; }
					writeType(i);					
				}
			}
		}
		
		output.println(" {");
		
		for(JavaFile.Declaration d : decl.declarations()) {
			if(d instanceof JavaFile.Clazz) {
				writeClass((JavaFile.Clazz) d, depth + 1);
			} else if(d instanceof JavaFile.Field) {
				writeField((JavaFile.Field) d, depth + 1);
			}
		}
		
		indent(depth);output.println("}");
	}
	
	protected void writeField(JavaFile.Field f, int depth) {
		indent(depth);
		writeModifiers(f.modifiers());		
		writeType(f.type());		
		output.print(f.name());
		if(f.initialiser() != null) {
			output.print(" = ");
			writeExpression(f.initialiser());
		}
		output.println(";\n");
	}
	
	protected void writeExpression(JavaFile.Expression e) {
		if(e instanceof JavaFile.BoolVal) {
			writeBoolVal((JavaFile.BoolVal)e);
		} else if(e instanceof JavaFile.CharVal) {
			writeCharVal((JavaFile.CharVal)e);
		} else if(e instanceof JavaFile.IntVal) {
			writeIntVal((JavaFile.IntVal)e);
		} else if(e instanceof JavaFile.LongVal) {
			writeLongVal((JavaFile.LongVal)e);
		} else if(e instanceof JavaFile.FloatVal) {
			writeFloatVal((JavaFile.FloatVal)e);
		} else if(e instanceof JavaFile.DoubleVal) {
			writeDoubleVal((JavaFile.DoubleVal)e);
		} else if(e instanceof JavaFile.StringVal) {
			writeStringVal((JavaFile.StringVal)e);
		} else if(e instanceof JavaFile.NullVal) {
			writeNullVal((JavaFile.NullVal)e);
		} else if(e instanceof JavaFile.ArrayVal) {
			writeArrayVal((JavaFile.ArrayVal)e);
		}
		
		else {
			throw new RuntimeException("Invalid expression encountered: "
					+ e.getClass());
		}
	}
	
	protected void writeBoolVal(JavaFile.BoolVal e) {
		if(e.value()) {
			output.write("true");
		} else {
			output.write("false");
		}
	}
	
	protected void writeCharVal(JavaFile.CharVal e) {
		output.write("'");
		output.write(e.value()); // this will fail for non-ASCII chars
		output.write("'");
	}
	
	protected void writeIntVal(JavaFile.IntVal e) {		
		output.write(Integer.toString(e.value()));
	}
	
	protected void writeLongVal(JavaFile.LongVal e) {		
		output.write(Long.toString(e.value()) + "L");
	}
	
	protected void writeFloatVal(JavaFile.FloatVal e) {		
		output.write(Float.toString(e.value()) + "F");
	}
	
	protected void writeDoubleVal(JavaFile.DoubleVal e) {		
		output.write(Double.toString(e.value()));
	}
	
	protected void writeStringVal(JavaFile.StringVal e) {		
		output.write("\"");
		output.write(e.value());
		output.write("\"");
	}
	
	protected void writeNullVal(JavaFile.NullVal e) {		
		output.write("null");
	}
	
	protected void writeArrayVal(JavaFile.ArrayVal e) {		
		boolean firstTime = true;
		output.write("{");
		for(JavaFile.Expression i : e.values()) {
			if(!firstTime) {
				output.write(", ");
			} else {
				firstTime = false;
			}
			writeExpression(i);
		}
		output.write("}");
	}
	
	protected void writeType(JavaFile.Type t) {
		boolean firstTime=true;
		for(String c : t.components()) {
			if(!firstTime) {
				output.write(".");
			} else {
				firstTime=false;
			}
			output.write(c);			
		}
		for(int i=0;i!=t.dims();++i) {
			output.write("[]");
		}
		output.write(" ");
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
