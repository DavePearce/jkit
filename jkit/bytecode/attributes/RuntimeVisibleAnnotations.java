package jkit.bytecode.attributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import jkit.jil.tree.Modifier;
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
		writer.write_u4(2 + (annotations.size() * 4));
		writer.write_u2(annotations.size());
		for(Modifier.Annotation a : annotations) {
			writer.write_u2(constantPool.get(new Constant.Utf8(ClassFile.descriptor(a.type(),false))));
			writer.write_u2(0); // ignore arguments for now
		}
	}
	
	public void addPoolItems(Set<Constant.Info> constantPool, ClassLoader loader) {		
		Constant.addPoolItem(new Constant.Utf8("RuntimeVisibleAnnotations"),constantPool);
				
		for(Modifier.Annotation a : annotations) {
			Constant.addPoolItem(new Constant.Utf8(ClassFile.descriptor(a.type(),false)),constantPool);		
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
