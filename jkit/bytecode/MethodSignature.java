package jkit.bytecode;

import java.io.*;
import java.util.Map;
import java.util.Set;

import jkit.compiler.ClassLoader;
import jkit.jil.tree.*;

/**
 * This is for method and/or field signatures
 * @author djp 
 */
public class MethodSignature implements Attribute {
	protected Type.Function type;
	
	public MethodSignature(Type.Function type) {
		this.type = type;
	}
	
	public String name() {
		return "Signature";
	}
	
	public Type.Function type() {
		return type;
	}
	
	public void write(BinaryOutputStream writer,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader)
			throws IOException {
		writer.write_u2(constantPool.get(new Constant.Utf8("Signature")));
		writer.write_u4(2);
		writer.write_u2(constantPool.get(new Constant.Utf8(ClassFile
				.descriptor(type, true))));
	}
	
	public void addPoolItems(Set<Constant.Info> constantPool, ClassLoader loader) {
		Constant.addPoolItem(
				new Constant.Utf8(ClassFile.descriptor(type, true)),
				constantPool);
	}
	
	public void print(PrintWriter output,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader) {
		output.println("Signature: "
				+ constantPool.get(new Constant.Utf8(ClassFile.descriptor(type,
						true))));
	}
}
