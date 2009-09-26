package jkit.bytecode.attributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import jkit.bytecode.BinaryOutputStream;
import jkit.bytecode.BytecodeAttribute;
import jkit.bytecode.Constant;
import jkit.compiler.ClassLoader;

/**
 * A PureMethod attribute is used to indicate a pure method.
 */
public class PureMethod implements BytecodeAttribute {
	public String name() {
		return "PureMethod";
	}
	public void write(BinaryOutputStream writer,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader)
			throws IOException {		
		writer.write_u2(constantPool.get(new Constant.Utf8("PureMethod")));
		writer.write_u4(0);
	}
	
	public void addPoolItems(Set<Constant.Info> constantPool, ClassLoader loader) {		
		Constant.addPoolItem(new Constant.Utf8("PureMethod"), constantPool);		
	}
	
	public void print(PrintWriter output,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader)
			throws IOException {
		output.println("  PureMethod");
	}
}
