package jkit.bytecode;

import java.io.*;
import java.util.*;

import jkit.jil.tree.Modifier;
import jkit.jil.tree.Type;

public class BytecodeFileWriter {	
	protected final PrintWriter output;
	
	public BytecodeFileWriter(OutputStream o) {
		output = new PrintWriter(o);
	}	

	protected void write(ClassFile cfile) throws IOException {
		ArrayList<Constant.Info> constantPool = cfile.constantPool();
		HashMap<Constant.Info,Integer> poolMap = new HashMap<Constant.Info,Integer>();
		
		int index = 0;
		for(Constant.Info ci : constantPool) {
			poolMap.put(ci, index++);
		}

		for (Constant.Info c : constantPool) {
			if (c != null) { // item at index 0 is always null
				output.println(c);
			}
		}
		
		output.print("\nclass " + cfile.type() + " ");
		if(cfile.superClazz() != null) {
			output.println("");
			output.print("\t extends " + cfile.superClazz());
		}
		if(cfile.interfaces().size() > 0) {
			output.println("");
			output.print("\t implements ");
			boolean firstTime=true;
			for(Type.Clazz i : cfile.interfaces()) {
				if(!firstTime) {
					output.print(", ");
				}
				firstTime=false;
				output.print(i);
			}			
		}				
		
		output.println(" {\n");
		
		for(ClassFile.Field f : cfile.fields()) {
			writeField(f);
		}
		
		output.println("");
		
		for(ClassFile.Method m : cfile.methods()) {
			writeMethod(cfile,m,poolMap);
		}
	
		output.println("}");
		
		output.flush();
	}
	
	protected void writeField(ClassFile.Field f) {
		output.print("\t");
		writeModifiers(f.modifiers());
		output.print(f.type());
		output.println(" " + f.name() + ";");		
	
	}

	protected void writeMethod(ClassFile clazz, ClassFile.Method method,
			HashMap<Constant.Info, Integer> poolMap) throws IOException {
		output.print("\t");
		writeModifiers(method.modifiers());
		Type.Function type = method.type(); 
		output.print(type.returnType() + " " + method.name());
		output.print("(");
		boolean firstTime=true;
		
		List<Type> paramTypes = type.parameterTypes();
		List<List<Modifier>> params = method.parameterModifiers();
		
		for(int i = 0; i != params.size();++i) {
			if(!firstTime) {
				output.print(", ");
			}
			firstTime=false;
			writeModifiers(params.get(i));			
			output.print(paramTypes.get(i));
		}
		
		output.println(") {");
		
		ClassFile.Code codeAttr = (ClassFile.Code) method.attribute(ClassFile.Code.class);
		if(codeAttr != null) {
			writeCodeAttribute(codeAttr,poolMap);
		}		
		
		output.println("\t}");
	}
	
	protected void writeCodeAttribute(ClassFile.Code code,
			HashMap<Constant.Info, Integer> poolMap) throws IOException {

		output.println("\t\tstack = " + code.maxStack() + ", locals = "
				+ code.maxLocals());

		for (Bytecode b : code.bytecodes) {
			output.println("\t\t" + b);
		}

		// need to dump out exception handlers here.
	}

	protected void writeModifiers(List<Modifier> modifiers) {
		for (Modifier x : modifiers) {
			if (x instanceof Modifier.Base) {
				int mod = ((Modifier.Base) x).modifier();
				if ((mod & java.lang.reflect.Modifier.PRIVATE) != 0) {
					output.print("private ");
				}
				if ((mod & java.lang.reflect.Modifier.PROTECTED) != 0) {
					output.print("protected ");
				}
				if ((mod & java.lang.reflect.Modifier.PUBLIC) != 0) {
					output.print("public ");
				}
				if ((mod & java.lang.reflect.Modifier.STATIC) != 0) {
					output.print("static ");
				}
				if ((mod & java.lang.reflect.Modifier.ABSTRACT) != 0) {
					output.print("abstract ");
				}
				if ((mod & java.lang.reflect.Modifier.FINAL) != 0) {
					output.print("final ");
				}
				if ((mod & java.lang.reflect.Modifier.NATIVE) != 0) {
					output.print("native ");
				}
				if ((mod & java.lang.reflect.Modifier.STRICT) != 0) {
					output.print("strictfp ");
				}
				if ((mod & java.lang.reflect.Modifier.SYNCHRONIZED) != 0) {
					output.print("synchronized ");
				}
				if ((mod & java.lang.reflect.Modifier.TRANSIENT) != 0) {
					output.print("transient ");
				}
				if ((mod & java.lang.reflect.Modifier.VOLATILE) != 0) {
					output.print("volatile ");
				}
			} else if (x instanceof Modifier.Annotation) {
				Modifier.Annotation a = (Modifier.Annotation) x;
				output.print("@");
				output.print(a.name());
			} else {
				// do nothing
			}
		}
	}
}
