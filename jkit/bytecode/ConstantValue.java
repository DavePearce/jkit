package jkit.bytecode;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import jkit.compiler.ClassLoader;

public class ConstantValue implements Attribute {	
	private Object constant;
	
	/**
	 * A ConstantValue attribute. Here, the constant must be either an
	 * instanceof of Boolean, Byte, Character, Short, Integer, Float, Double or
	 * String.
	 * 
	 * @param constant
	 */
	public ConstantValue(Object constant) {		
		this.constant = constant;
		if (getConstantInfo() == null) {
			throw new IllegalArgumentException(
					"constant must be instance of Boolean, Byte, Character, Short, Integer, Float, Double or String");
		}
	}
	
	public String name() {
		return "ConstantValue";
	}
	
	public void addPoolItems(Set<Constant.Info> constantPool, ClassLoader loader) {
		Constant.addPoolItem(new Constant.Utf8("ConstantValue"), constantPool);
		Constant.addPoolItem(getConstantInfo(), constantPool);
	}
	
	public void write(BinaryOutputStream writer,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader) throws IOException {
		writer.write_u2(constantPool.get(new Constant.Utf8("ConstantValue")));
		writer.write_u4(2);
		writer.write_u2(constantPool.get(getConstantInfo()));
	}
	
	public void print(PrintWriter output,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader)
			throws IOException {
		String type;
		if (constant instanceof Byte || constant instanceof Character
				|| constant instanceof Boolean || constant instanceof Short
				|| constant instanceof Integer) {
			type = "int";
		} else if (constant instanceof Long) {
			type = "long";
		} else if (constant instanceof Float) {
			type = "float";
		} else {
			type = "double";
		}
		output.println("  Constant value: " + type + " " + constant.toString());
	}
	
	private Constant.Info getConstantInfo() {
		if(constant instanceof Boolean) {
			boolean b = (Boolean) constant;
			if(b) {
				return new Constant.Integer(1);
			} else {
				return new Constant.Integer(0);
			}
		} else if(constant instanceof Byte) {
			return new Constant.Integer((Byte)constant);
		} else if(constant instanceof Character) {
			return new Constant.Integer((Character)constant);
		} else if(constant instanceof Short) {
			return new Constant.Integer((Short)constant);
		} else if(constant instanceof Integer) {
			return new Constant.Integer((Integer)constant);
		} else if(constant instanceof Long) {
			return new Constant.Long((Long)constant);
		} else if(constant instanceof Float) {
			return new Constant.Float((Float)constant);
		} else if(constant instanceof Double) {
			return new Constant.Double((Double)constant);
		} else {
			return null;
		}
	}
}
