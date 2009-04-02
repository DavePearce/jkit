package jkit.bytecode;

import java.io.IOException;
import java.util.*;

import jkit.jil.tree.Type;

public class Exceptions implements Attribute {
	protected List<Type.Clazz> exceptions;
	
	public Exceptions(List<Type.Clazz> exceptions) {
		this.exceptions = exceptions;
	}
	
	public String name() {
		return "Exceptions";
	}
	
	/**
	 * This method requires the attribute to write itself to the binary stream.
	 * 
	 * @param writer
	 * @returns the number of bytes written.
	 * @throws IOException
	 */
	public void write(BinaryWriter writer,
			Map<Constant.Info, Integer> constantPool) throws IOException {
		
		writer.write_u2(constantPool.get(new Constant.Utf8("Exceptions")));
		writer.write_u4(2 + (2 * exceptions.size()));
		writer.write_u2(exceptions.size());
		for (Type.Clazz e : exceptions) {
			writer.write_u2(constantPool.get(Constant.buildClass(e)));
		}
	}
	
	/**
	 * When this method is called, the attribute must add all items that it
	 * needs to the constant pool.
	 * 
	 * @param constantPool
	 */
	public void addPoolItems(Set<Constant.Info> constantPool) {
		Constant.addPoolItem(new Constant.Utf8("Exceptions"), constantPool);
		for (Type.Clazz e : exceptions) {
			Constant.addPoolItem(Constant.buildClass(e), constantPool);
		}
	}
}
