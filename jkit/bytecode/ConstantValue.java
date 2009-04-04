package jkit.bytecode;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

public class ConstantValue implements Attribute {
	private String name;
	private Object constant;
	
	/**
	 * A ConstantValue attribute. Here, the constant must be either an
	 * instanceof of Boolean, Byte, Character, Short, Integer, Float, Double or
	 * String.
	 * 
	 * @param name
	 * @param constant
	 */
	public ConstantValue(String name, Object constant) {
		this.name = name;
		this.constant = constant;
		if(getConstantInfo() == null) {
			throw new IllegalArgumentException(
					"constant must be instance of Boolean, Byte, Character, Short, Integer, Float, Double or String");
		}
	}
	
	public String name() {
		return "ConstantValue";
	}
	
	public void addPoolItems(Set<Constant.Info> constantPool) {
		Constant.addPoolItem(new Constant.Utf8("Exceptions"), constantPool);
		Constant.addPoolItem(getConstantInfo(), constantPool);
	}
	
	public void write(BinaryOutputStream writer,
			Map<Constant.Info, Integer> constantPool) throws IOException {
		writer.write_u2(constantPool.get(new Constant.Utf8("ConstantValue")));
		writer.write_u4(2);
		writer.write_u2(constantPool.get(getConstantInfo()));
	}
	
	public void print(PrintWriter output,
			Map<Constant.Info, Integer> constantPool) throws IOException {
		output.println("  ConstantValue: ");
		output.println("   " + name + ": " + constant.toString());
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
