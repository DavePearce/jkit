package jkit.bytecode;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jkit.jil.tree.*;

/**
 * This is for method and/or field signatures
 * @author djp 
 */
public class Signature implements Attribute {
	protected Type type;
	
	public Signature(Type type) {
		this.type = type;
	}
	
	public String name() {
		return "Signature";
	}
	
	public void write(BinaryOutputStream writer,
			Map<Constant.Info, Integer> constantPool) throws IOException {
		writer.write_u2(constantPool.get(new Constant.Utf8("Signature")));
		writer.write_u4(2);
		writer.write_u2(constantPool.get(new Constant.Utf8(ClassFile
				.descriptor(type, true))));
	}
	
	public void addPoolItems(Set<Constant.Info> constantPool) {
		Constant.addPoolItem(
				new Constant.Utf8(ClassFile.descriptor(type, true)),
				constantPool);
	}
}
