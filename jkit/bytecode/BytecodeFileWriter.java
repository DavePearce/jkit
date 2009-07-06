package jkit.bytecode;

import java.io.*;
import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.jil.tree.Modifier;
import jkit.jil.tree.Type;

public class BytecodeFileWriter {	
	protected final PrintWriter output;
	protected final ClassLoader loader;
	
	public BytecodeFileWriter(OutputStream o, ClassLoader loader) {
		output = new PrintWriter(o);
		this.loader = loader;
	}	

	public void write(ClassFile cfile) throws IOException {
		ArrayList<Constant.Info> constantPool = cfile.constantPool(loader);
		HashMap<Constant.Info,Integer> poolMap = new HashMap<Constant.Info,Integer>();
		
		int index = 0;
		for(Constant.Info ci : constantPool) {
			poolMap.put(ci, index++);
		}

		index = 0;
		for (Constant.Info c : constantPool) {
			if (c != null) { // item at index 0 is always null
				output.print("#" + ++index + "\t");
				output.println(c);
			}
		}
		output.println();

		writeModifiers(cfile.modifiers());
		output.print("class " + cfile.type() + " ");
		if(cfile.superClass() != null) {
			output.print(" extends " + cfile.superClass());
		}
		if(cfile.interfaces().size() > 0) {
			output.print(" implements ");
			boolean firstTime=true;
			for(Type.Clazz i : cfile.interfaces()) {
				if(!firstTime) {
					output.print(", ");
				}
				firstTime=false;
				output.print(i);
			}			
		}				
		
		output.println();
		
		for(Attribute a : cfile.attributes()) {
			a.print(output,poolMap,loader);
		}
		
		output.println(" {");
		
		for(ClassFile.Field f : cfile.fields()) {
			writeField(f,poolMap);
		}
		
		if(!cfile.fields().isEmpty()) {
			output.println();
		}
		
		for(ClassFile.Method m : cfile.methods()) {
			writeMethod(cfile,m,poolMap);
			output.println();
		}
	
		output.println("}");
		
		output.flush();
	}
	
	protected void writeField(ClassFile.Field f,
			HashMap<Constant.Info, Integer> poolMap) throws IOException {
		output.print("  ");
		writeModifiers(f.modifiers());
		output.print(f.type());
		output.println(" " + f.name() + ";");
		for(Attribute a : f.attributes()) {
			a.print(output,poolMap,loader);
		}
	}

	protected void writeMethod(ClassFile clazz, ClassFile.Method method,
			HashMap<Constant.Info, Integer> poolMap) throws IOException {
		output.print("  ");
		writeModifiers(method.modifiers());
		Type.Function type = method.type(); 
		output.print(type.returnType() + " " + method.name());
		output.print("(");
		boolean firstTime=true;
		
		List<Type> paramTypes = type.parameterTypes();				
		
		for(int i = 0; i != paramTypes.size();++i) {
			if(!firstTime) {
				output.print(", ");
			}
			firstTime=false;					
			output.print(paramTypes.get(i));
		}
		
		output.println(");");
		
		for(Attribute a : method.attributes()) {
			a.print(output,poolMap,loader);
		}					
	}	
	protected void writeModifiers(List<Modifier> modifiers) {	
		writeModifiers(modifiers,output);
	}
	
	static void writeModifiers(List<Modifier> modifiers, PrintWriter output) {
		for (Modifier x : modifiers) {			
			if (x instanceof Modifier.Private) {
				output.write("private ");
			} else if (x instanceof Modifier.Protected) {
				output.write("protected ");
			} else if (x instanceof Modifier.Public) {
				output.write("public ");
			} else if (x instanceof Modifier.Static) {
				output.write("static ");
			} else if (x instanceof Modifier.Abstract) {
				output.write("abstract ");
			} else if (x instanceof Modifier.Final) {
				output.write("final ");
			} else if (x instanceof Modifier.Super) {
				output.write("super ");
			} else if (x instanceof Modifier.Bridge) {
				output.write("bridge ");
			} else if (x instanceof Modifier.Enum) {
				output.write("enum ");
			} else if (x instanceof Modifier.Synthetic) {
				output.write("synthetic ");
			} else if (x instanceof Modifier.Native) {
				output.write("native ");
			} else if (x instanceof Modifier.StrictFP) {
				output.write("strictfp ");
			} else if (x instanceof Modifier.Synchronized) {
				output.write("synchronized ");
			} else if (x instanceof Modifier.Transient) {
				output.write("transient ");
			} else if (x instanceof Modifier.Volatile) {
				output.write("volatile ");
			} else if (x instanceof Modifier.Annotation) {
				Modifier.Annotation a = (Modifier.Annotation) x;
				output.write("@");
				output.write(a.name());
			} else {
				output.write("unknown ");
			}
		}
	}	
}
