package jkit.bytecode.attributes;

import java.io.*;
import java.util.*;

import jkit.jil.tree.Modifier;
import jkit.jil.tree.JilExpr;
import jkit.bytecode.*;
import jkit.compiler.ClassLoader;

public class RuntimeVisibleAnnotations implements BytecodeAttribute {
	private ArrayList<Modifier.Annotation> annotations;
	
	public RuntimeVisibleAnnotations(Collection<Modifier.Annotation> annotations) {
		this.annotations = new ArrayList(annotations);
	}
	
	public String name() {
		return "RuntimeVisibleAnnotations";
	}
	
	public List<Modifier.Annotation> annotations() {
		return annotations;
	}
	
	public void write(BinaryOutputStream writer,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader)
			throws IOException {		
		writer.write_u2(constantPool.get(new Constant.Utf8("RuntimeVisibleAnnotations")));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BinaryOutputStream outstream = new BinaryOutputStream(out);
		for(Modifier.Annotation a : annotations) {
			outstream.write_u2(constantPool.get(new Constant.Utf8(ClassFile.descriptor(a.type(),false))));
			Map<String,JilExpr.Value> args = a.arguments();
			outstream.write_u2(args.size());
			for(Map.Entry<String,JilExpr.Value> me : args.entrySet()) {
				outstream.write_u2(constantPool.get(new Constant.Utf8(me.getKey())));
				// here we have to write the value
				writeElementValue(me.getValue(),outstream,constantPool);
			}			
		}
		byte[] bytes = out.toByteArray();
		writer.write_u4(2 + bytes.length);
		writer.write_u2(annotations.size());
		writer.write(bytes);
	}
	
	protected void writeElementValue(JilExpr.Value val, BinaryOutputStream writer,
			Map<Constant.Info, Integer> constantPool) throws IOException {
		if(val instanceof JilExpr.Int){
			int v = ((JilExpr.Int)val).value(); 
			writer.write_u1('I');
			writer.write_u2(constantPool.get(new Constant.Integer(v)));
		} else {
			throw new RuntimeException("No support for annotation element (yet): " + val);
		}
	}
	
	public void addPoolItems(Set<Constant.Info> constantPool, ClassLoader loader) {		
		Constant.addPoolItem(new Constant.Utf8("RuntimeVisibleAnnotations"),constantPool);
				
		for(Modifier.Annotation a : annotations) {
			Constant.addPoolItem(new Constant.Utf8(ClassFile.descriptor(a.type(),false)),constantPool);
			for(Map.Entry<String,JilExpr.Value> me : a.arguments().entrySet()) {
				Constant.addPoolItem(new Constant.Utf8(me.getKey()),constantPool);
				addElementValue(me.getValue(),constantPool);
			}
		}
	}
		
	protected void addElementValue(JilExpr.Value val, Set<Constant.Info> constantPool) {
		if(val instanceof JilExpr.Int){
			int v = ((JilExpr.Int)val).value(); 			
			Constant.addPoolItem(new Constant.Integer(v),constantPool);			
		} else {
			throw new RuntimeException("No support for annotation element (yet): " + val);
		}
	}
	
	public void print(PrintWriter output,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader)
			throws IOException {
		output.println("  RuntimeVisibleAnnotations:");
		for(Modifier.Annotation a : annotations) {
			output.println("    "  + a.type());
		}
	}
}
