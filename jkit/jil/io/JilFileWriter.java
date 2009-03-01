package jkit.jil.io;

import java.io.*;
import java.util.List;
import jkit.util.Pair;
import jkit.jil.tree.*;

/**
 * This class is used to output Jil code to file.
 * 
 * @author djp
 * 
 */

public class JilFileWriter {
	private PrintWriter output;
	
	public JilFileWriter(Writer out) {
		output = new PrintWriter(out);
	}
	
	public JilFileWriter(OutputStream out) {
		output = new PrintWriter(out);
	}
	
	public void write(Clazz jc) {
		output.print("class " + jc.type() + " ");
		if(jc.superClass() != null) {
			output.println("");
			output.print("\t extends " + jc.superClass());
		}
		if(jc.interfaces().size() > 0) {
			output.println("");
			output.print("\t implements ");
			boolean firstTime=true;
			for(Type.Clazz i : jc.interfaces()) {
				if(!firstTime) {
					output.print(", ");
				}
				firstTime=false;
				output.print(i);
			}			
		}				
		
		output.println(" {\n");
		
		for(Field f : jc.fields()) {
			write(f);
		}
		
		output.println("");
		
		for(Method m : jc.methods()) {
			write(m);
		}
		
		output.println("}");
		
		output.flush();
	}
	
	public void write(Field f) {
		output.print("\t");
		writeModifiers(f.modifiers());
		output.print(f.type());
		output.println(" " + f.name() + ";");		
	}
	
	public void write(Method m) {
		output.print("\t");
		writeModifiers(m.modifiers());
		Type.Function type = m.type(); 
		output.print(type.returnType() + " " + m.name());
		output.print("(");
		boolean firstTime=true;
		
		List<Type> paramTypes = type.parameterTypes();
		List<Pair<String,List<Modifier>>> params = m.parameters();
		
		for(int i = 0; i != params.size();++i) {
			if(!firstTime) {
				output.print(", ");
			}
			firstTime=false;
			writeModifiers(params.get(i).second());			
			output.print(paramTypes.get(i));
			output.print(" " + params.get(i).first());
		}
		
		output.println(") {");
		
		output.println("\t}");
		
	}
	
	protected void writeModifiers(List<Modifier> modifiers) {
		for (Modifier x : modifiers) {
			if (x instanceof Modifier.Base) {
				int mod = ((Modifier.Base) x).modifier();
				if ((mod & java.lang.reflect.Modifier.PRIVATE) != 0) {
					output.write("private ");
				}
				if ((mod & java.lang.reflect.Modifier.PROTECTED) != 0) {
					output.write("protected ");
				}
				if ((mod & java.lang.reflect.Modifier.PUBLIC) != 0) {
					output.write("public ");
				}
				if ((mod & java.lang.reflect.Modifier.STATIC) != 0) {
					output.write("static ");
				}
				if ((mod & java.lang.reflect.Modifier.ABSTRACT) != 0) {
					output.write("abstract ");
				}
				if ((mod & java.lang.reflect.Modifier.FINAL) != 0) {
					output.write("final ");
				}
				if ((mod & java.lang.reflect.Modifier.NATIVE) != 0) {
					output.write("native ");
				}
				if ((mod & java.lang.reflect.Modifier.STRICT) != 0) {
					output.write("strictfp ");
				}
				if ((mod & java.lang.reflect.Modifier.SYNCHRONIZED) != 0) {
					output.write("synchronized ");
				}
				if ((mod & java.lang.reflect.Modifier.TRANSIENT) != 0) {
					output.write("transient ");
				}
				if ((mod & java.lang.reflect.Modifier.VOLATILE) != 0) {
					output.write("volatile ");
				}
			} else if (x instanceof Modifier.Annotation) {
				Modifier.Annotation a = (Modifier.Annotation) x;
				output.write("@");
				output.write(a.name());
			} else {
				// do nothing
			}
		}
	}	
}
