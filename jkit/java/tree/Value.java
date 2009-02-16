package jkit.java.tree;

import java.util.List;

import jkit.jil.*;

public interface Value extends Expr {
	
	/**
	 * Represents a numerical constant
	 * 
	 * @author djp
	 *
	 */
	public static class Number extends SyntacticElementImpl implements Value {
		protected int value;
		
		public Number(int value, Attribute... attributes) {
			super(attributes);
			this.value = value;
		}
	}
	
	/**
	 * A boolean constant.
	 * 
	 * @author djp
	 *
	 */
	public static class Bool extends Number {
		public Bool(boolean value, Attribute... attributes) {
			super(value?1:0, attributes);
		}
		
		public boolean value() {
			return value==1;
		}
	}
	
	/**
	 * Represents a character constant.
	 * 
	 * @author djp
	 *
	 */
	public static class Char extends Number {
		public Char(char value, Attribute... attributes) {
			super(value,attributes);
		}
		
		public char value() {
			return (char)value;
		}
	}
	
	/**
	 * Represents a byte constant.
	 * 
	 * @author djp
	 *
	 */
	public static class Byte extends Number {
		public Byte(byte value, Attribute... attributes) {
			super(value,attributes);
		}
		
		public byte value() {
			return (byte)value;
		}
	}
	
	/**
	 * Represents a short constant.
	 * @author djp
	 *
	 */
	public static class Short extends Number {
		public Short(short value, Attribute... attributes) {
			super(value,attributes);
		}
		
		public short value() {
			return (short)value;
		}
	}

	/**
     * Represents an int constant.
     * 
     * @author djp
     * 
     */	
	public static class Int extends Number {
		public Int(int value, Attribute... attributes) {
			super(value,attributes);
		}
		
		public int value() {
			return value;
		}
	}

	/**
     * Represents a long Constant.
     * 
     * @author djp
     * 
     */
	public static class Long extends SyntacticElementImpl implements Value {
		private long value;
		
		public Long(long value, Attribute... attributes) {
			super(attributes);
			this.value=value;
		}
		
		public long value() {
			return value;
		}
	}
	
	/**
     * A Float Constant.
     * 
     * @author djp
     * 
     */
	public static class Float extends SyntacticElementImpl implements Value {
		private float value;
		
		public Float(float value, Attribute... attributes) {
			super(attributes);
			this.value=value;
		}
		
		public float value() {
			return value;
		}
	}

	/**
     * A Double Constant.
     * 
     * @author djp
     * 
     */
	public static class Double extends SyntacticElementImpl implements Value {
		private double value;
		
		public Double(double value, Attribute... attributes) {
			super(attributes);
			this.value=value;
		}
		
		public double value() {
			return value;
		}
	}
	
	/**
     * A String Constant.
     * 
     * @author djp
     * 
     */
	public static class String extends SyntacticElementImpl implements Value {
		private java.lang.String value;
		
		public String(java.lang.String value, Attribute... attributes) {
			super(attributes);
			this.value=value;
		}
		
		public java.lang.String value() {
			return value;
		}
	}		
	
	/**
     * The null Constant.
     * 
     * @author djp
     * 
     */
	public static class Null extends SyntacticElementImpl implements Value {
		public Null(Attribute... attributes) {
			super(attributes);
		}
	}
			
	/**
     * An array constant (used for array initialisers only).
     * 
     * @author djp
     * 
     */
	public static class Array extends SyntacticElementImpl implements Value {
		private List<Expr> values;
		
		public Array(List<Expr> values, Attribute... attributes) {
			super(attributes);
			this.values = values;
		}
		
		public List<Expr> values() {
			return values;
		}
	}
	
	/**
	 * A typed array constant (used for array initialisers only). This is
	 * similar to a normal array constant, except that the target type is also
	 * specified. For example:
	 * 
	 * <pre>
     * Object[] test = new Object[]{&quot;abc&quot;, new Integer(2)};
     * </pre>
     * 
	 * @author djp
	 */
	public static class TypedArray extends Array {
		private Type type;

		public TypedArray(Type type, List<Expr> values,
				Attribute... attributes) {
			super(values,attributes);
			this.type = type;
		}

		public Type type() {
			return type;
		}
	}	
	
	/**
	 * Represents a Class Constant
	 * 
	 */
	public static class Class extends SyntacticElementImpl implements Value {
		private Type.Clazz type;

		public Class(Type.Clazz type, Attribute... attributes) {
			super(attributes);
			this.type = type;
		}

		public Type.Clazz value() {
			return type;
		}
	}

}
