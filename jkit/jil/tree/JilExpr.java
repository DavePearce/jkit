package jkit.jil.tree;

import java.util.ArrayList;
import java.util.List;

import jkit.java.tree.Decl;
import jkit.java.tree.Expr;
import jkit.jil.tree.Type.Array;
import jkit.jil.tree.Type.Bool;
import jkit.jil.tree.Type.Byte;
import jkit.jil.tree.Type.Char;
import jkit.jil.tree.Type.Clazz;
import jkit.jil.tree.Type.Double;
import jkit.jil.tree.Type.Float;
import jkit.jil.tree.Type.Int;
import jkit.jil.tree.Type.Long;
import jkit.jil.tree.Type.Null;
import jkit.jil.tree.Type.Primitive;
import jkit.jil.tree.Type.Reference;
import jkit.jil.tree.Type.Short;

public interface JilExpr extends SyntacticElement {
	
	public Type type();
	
	/**
	 * A Variable object represents a local variable access. A variable
	 * access is considered local if the variable is declared within the current
	 * method. Non-local variable accesses correspond to variables which are
	 * declared outside the current method.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Variable extends SyntacticElementImpl implements JilExpr {
		private String value;
		private Type type;

		public Variable(String value, Type type, Attribute... attributes) {
			super(attributes);
			this.value = value;
			this.type = type;
		}
		
		public Variable(String value, Type type, List<Attribute> attributes) {
			super(attributes);
			this.value = value;
			this.type = type;
		}

		public String value() {
			return value;
		}
		
		public void setValue(String value) {
			this.value = value;
		}
		
		public Type type() {
			return type;
		}
		
		public void setType(Type type) {
			this.type = type;
		}
	}
	
	/**
	 * An static Class Access
	 * 
	 * @author djp
	 * 
	 */
	public static final class ClassVariable extends SyntacticElementImpl
			implements JilExpr {
		private Type.Clazz type;

		public ClassVariable(Type.Clazz type, Attribute... attributes) {
			super(attributes);
			this.type = type;
		}

		public ClassVariable(Type.Clazz type, List<Attribute> attributes) {
			super(attributes);
			this.type = type;
		}

		public Type.Clazz type() {
			return type;
		}
	}

	
	/**
	 * Represents an explicit cast.
	 * 
	 * @author djp
	 *
	 */
	public static final class Cast extends SyntacticElementImpl implements JilExpr {
		protected JilExpr expr;
		protected Type type;

		public Cast(JilExpr expr, Type type, Attribute... attributes) {
			super(attributes);
			this.expr = expr;
			this.type = type;
		}

		public Cast(JilExpr expr, Type type, List<Attribute> attributes) {
			super(attributes);
			this.expr = expr;
			this.type = type;
		}
		
		public JilExpr expr() {
			return expr;
		}

		public void setExpr(JilExpr e) {
			expr = e;
		}
		
		public Type type() {
			return type;
		}
		
		public void setType(Type t) {
			type = t;
		}
	}

	/**
	 * Represents an implicit type conversion between primitive types.
	 * 
	 * @author djp
	 *
	 */
	public static final class Convert extends SyntacticElementImpl implements JilExpr {
		protected JilExpr expr;
		protected Type.Primitive type;

		public Convert(Type.Primitive type, JilExpr expr, Attribute... attributes) {
			super(attributes);
			this.expr = expr;
			this.type = type;
		}

		public Convert(Type.Primitive type, JilExpr expr, List<Attribute> attributes) {
			super(attributes);
			this.expr = expr;
			this.type = type;
		}
		
		public JilExpr expr() {
			return expr;
		}

		public Type.Primitive type() {
			return type;
		}
		
		public void setType(Type.Primitive type) {
			this.type = type;
		}
	}
	
	/**
	 * Represents an InstanceOf binary operation.
	 * 
	 * @author djp
	 * 
	 */
	public static final class InstanceOf extends SyntacticElementImpl implements JilExpr {
		protected JilExpr lhs;
		protected Type rhs;
		protected Type type;

		public InstanceOf(JilExpr lhs, Type rhs, Type type, Attribute... attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
			this.type = type;
		}

		public InstanceOf(JilExpr lhs, Type rhs, Type type, List<Attribute> attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
			this.type = type;
		}
		
		public JilExpr lhs() {
			return lhs;
		}

		public void setLhs(JilExpr e) {
			lhs = e;
		}
		
		public Type rhs() {
			return rhs;
		}
		
		public void setRhs(Type e) {
			rhs = e;
		}
		
		public Type type() {
			return type;
		}
		
		public void setType(Type e) {
			type = e;
		}
	}

	/**
	 * Represents Unary Arithmetic Operators
	 * 
	 * @author djp
	 * 
	 */
	public static final class UnOp extends SyntacticElementImpl implements JilExpr {
		public static final int NOT = 0;
		public static final int INV = 1;
		public static final int NEG = 2;

		protected JilExpr expr;
		protected int op;
		protected Type.Primitive type;
		
		public UnOp(JilExpr expr, int op, Type.Primitive type, Attribute... attributes) {
			super(attributes);
			this.expr = expr;
			this.op = op;
			this.type = type;
		}

		public UnOp(JilExpr expr, int op, Type.Primitive type, List<Attribute> attributes) {
			super(attributes);
			this.expr = expr;
			this.op = op;
			this.type = type;
		}
		
		public int op() {
			return op;
		}

		public void setOp(int op) {
			this.op = op;
		}
		
		public JilExpr expr() {
			return expr;
		}
		
		public void setExpr(JilExpr expr) {
			this.expr = expr;
		}
		
		public Type.Primitive type() {
			return type;
		}
		
		public void setType(Type.Primitive type) {
			this.type = type;
		}
	}

	/**
	 * A Binary Operator.  E.g. +.-,*,/,<,<=,>,?=,==,!=, etc.
	 * 
	 * @author djp
	 * 
	 */
	public static final class BinOp extends SyntacticElementImpl implements JilExpr {
		// BinOp Constants
		public static final int ADD = 0;
		public static final int SUB = 1;
		public static final int MUL = 2;
		public static final int DIV = 3;
		public static final int MOD = 4;
		public static final int SHL = 5;
		public static final int SHR = 6;
		public static final int USHR = 7;
		public static final int AND = 8;
		public static final int OR = 9;
		public static final int XOR = 10;

		public static final int LT = 11;
		public static final int LTEQ = 12;
		public static final int GT = 13;
		public static final int GTEQ = 14;
		public static final int EQ = 15;
		public static final int NEQ = 16;
		public static final int LAND = 17;
		public static final int LOR = 18;

		protected JilExpr lhs;
		protected JilExpr rhs;
		protected int op;
		protected Type.Primitive type;

		public BinOp(JilExpr lhs, JilExpr rhs, int op, Type.Primitive type, Attribute... attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
			this.op = op;
			this.type = type;
		}

		public BinOp(JilExpr lhs, JilExpr rhs, int op, Type.Primitive type, List<Attribute> attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
			this.op = op;
			this.type = type;
		}
		
		public int op() {
			return op;
		}

		public void setOp(int op) {
			this.op = op;
		}
		
		public JilExpr lhs() {
			return lhs;
		}

		public void setLhs(JilExpr lhs) {
			this.lhs = lhs;
		}
		
		public JilExpr rhs() {
			return rhs;
		}
		
		public void setRhs(JilExpr rhs) {
			this.rhs = rhs;
		}
		
		public Type.Primitive type() {
			return type;
		}
		
		public void setType(Type.Primitive type) {
			this.type = type;
		}
	}

	/**
	 * Represents a method call. The method call be either "polymorphic", or
	 * "non-polymorphic". The former means the method will be called on the
	 * dynamic type of the received, whilst the latter means that the method
	 * will be called directly on the static type of the receiver.
	 * 
	 * @author djp
	 * 
	 */
	public static class Invoke extends SyntacticElementImpl implements JilExpr, JilStmt {
		protected JilExpr target;
		protected String name;
		protected List<JilExpr> parameters;
		protected Type type; 		
		protected Type.Function funType;		
				
		/**
		 * Construct a method which may, or may not be polymorphic.
		 * 
		 * @param target
		 *            The expression from which the receiver is determined
		 * @param name
		 *            The name of the method
		 * @param parameters
		 *            The parameters of the method
		 * @param funType
		 *            The function type of the method being called.
		 * @param type
		 *            the return type from this expression. Note that this can
		 *            differ from the return type of funType, especially in the
		 *            case of generics.
		 */
		public Invoke(JilExpr target, String name, List<JilExpr> parameters,
				Type.Function funType, Type type, Attribute... attributes) {
			super(attributes);
			this.target = target;
			this.name = name;
			this.parameters = parameters;
			this.type = type;
			this.funType = funType;
		}
		
		/**
		 * Construct a method which may, or may not be polymorphic.
		 * 
		 * @param target
		 *            The expression from which the receiver is determined
		 * @param name
		 *            The name of the method
		 * @param parameters
		 *            The parameters of the method
		 * @param funType
		 *            The function type of the method being called.
		 * @param type
		 *            the return type from this expression. Note that this can
		 *            differ from the return type of funType, especially in the
		 *            case of generics.
		 */
		public Invoke(JilExpr target, String name, List<JilExpr> parameters,
				Type.Function funType, Type type, List<Attribute> attributes) {
			super(attributes);
			this.target = target;
			this.name = name;
			this.parameters = parameters;
			this.type = type;
			this.funType = funType;
		}
		
		public JilExpr target() {
			return target;
		}

		public void setTarget(JilExpr target) {
			this.target = target;
		}
		
		public String name() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Type type() {
			return type;
		}
		
		public void setType(Type type) {
			this.type = type;
		}
		
		public Type.Function funType() {
			return funType;
		}
		
		public void setFunType(Type.Function funtype) {
			this.funType = funtype;
		}		
		
		public List<JilExpr> parameters() {
			return parameters;
		}
		
		public void setParameters(List<JilExpr> parameters) {
			this.parameters = parameters;
		}
	}

	/**
	 * A special invoke represents a method call that will be translated into an
	 * invokespecial bytecode. It may seem unnecessary to have this explicit in
	 * the JIL intermediate representation. However, in some cases, the user
	 * needs to be able to state this explicitly. For example, consider explicit
	 * super-class method calls. E.g. "super.add()" from a class extending
	 * ArrayList. In this case, the method call is "non-polymorphic". That is,
	 * it does not adhere to the normal dynamic dispatch rules, and will not
	 * dynamically dispatch based on the actual type of the receiver. This must
	 * be so, since otherwise any such super call would likely end causing an
	 * infinte loop!
	 * 
	 * @author djp
	 * 
	 */
	public static final class SpecialInvoke extends Invoke {
		/**
		 * Construct a method which may, or may not be polymorphic.
		 * 
		 * @param target
		 *            The expression from which the receiver is determined
		 * @param name
		 *            The name of the method
		 * @param parameters
		 *            The parameters of the method
		 * @param funType
		 *            The function type of the method being called.
		 * @param type
		 *            the return type from this expression. Note that this can
		 *            differ from the return type of funType, especially in the
		 *            case of generics.
		 */
		public SpecialInvoke(JilExpr target, String name, List<JilExpr> parameters,
				Type.Function funType, Type type, Attribute... attributes) {
			super(target,name,parameters,funType,type,attributes);
		}
		
		/**
		 * Construct a method which may, or may not be polymorphic.
		 * 
		 * @param target
		 *            The expression from which the receiver is determined
		 * @param name
		 *            The name of the method
		 * @param parameters
		 *            The parameters of the method
		 * @param funType
		 *            The function type of the method being called.
		 * @param type
		 *            the return type from this expression. Note that this can
		 *            differ from the return type of funType, especially in the
		 *            case of generics.
		 */
		public SpecialInvoke(JilExpr target, String name, List<JilExpr> parameters,
				Type.Function funType, Type type, List<Attribute> attributes) {
			super(target,name,parameters,funType,type,attributes);			
		}
	}
		
	
	/**
	 * Represents the new operator. The parameters provided are either passed to
	 * that object's constructor, or are used to determine the necessary array
	 * dimensions (e.g. in new array[x+1]). Observe that, if this new operator
	 * declares an anonymous class, then this can include various declarations.
	 * 
	 * @author djp
	 * 
	 */
	public static final class New extends SyntacticElementImpl implements JilExpr, JilStmt {
		private Type.Reference type;
		private JilExpr context;
		private List<JilExpr> parameters;
		private Type.Function funType;
		
		/**
		 * Create an AST node represent a new statement or expression.
		 * 
		 * @param type -
		 *            the type being constructed e.g. java.lang.String or
		 *            Integer[]
		 * @param parameters -
		 *            The parameters (if any) supplied to the constructor.
		 *            Should be an empty (i.e. non-null) list.
		 * @param funType
		 *            The static type of the constructor to be called. Note, if
		 *            this new call is for an array, then you can pass null
		 *            here.
		 */
		public New(Type.Reference type, List<JilExpr> parameters,
				Type.Function funType, Attribute... attributes) {
			super(attributes);
			this.type = type;
			this.context = context;
			this.parameters = parameters;
			this.funType = funType;
		}

		/**
		 * Create an AST node represent a new statement or expression.
		 * 
		 * @param type -
		 *            the type being constructed e.g. java.lang.String or
		 *            Integer[]
		 * @param parameters -
		 *            The parameters (if any) supplied to the constructor.
		 *            Should be an empty (i.e. non-null) list .
		 */
		public New(Type.Reference type, List<JilExpr> parameters,
				Type.Function funType, List<Attribute> attributes) {
			super(attributes);
			this.type = type;
			this.context = context;
			this.parameters = parameters;			
			this.funType = funType;
		}
		
		public Type.Reference type() {
			return type;
		}

		public void setType(Type.Reference type) {
			this.type = type;
		}

		public Type.Function funType() {
			return funType;
		}

		public void setFunType(Type.Function funType) {
			this.funType = funType;
		}
		
		public List<JilExpr> parameters() {
			return parameters;
		}

		public void setParameters(List<JilExpr> parameters) {
			this.parameters = parameters;
		}

		public JilExpr context() {
			return context;
		}

		public void setContext(JilExpr context) {
			this.context = context;
		}
	}

	/**
	 * Represents the act of derefencing a field.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Deref extends SyntacticElementImpl implements JilExpr {
		protected JilExpr target;
		protected String name;
		protected Type type;
		protected boolean isStatic;
		
		public Deref(JilExpr lhs, String rhs, boolean isStatic, Type type,
				Attribute... attributes) {
			super(attributes);
			this.target = lhs;
			this.name = rhs;
			this.type = type;
			this.isStatic = isStatic;
		}

		public Deref(JilExpr lhs, String rhs, boolean isStatic, Type type, 
				List<Attribute> attributes) {
			super(attributes);
			this.target = lhs;
			this.name = rhs;
			this.type = type;
			this.isStatic = isStatic;
		}
		
		public JilExpr target() {
			return target;
		}

		public void setTarget(JilExpr target) {
			this.target = target;
		}
		
		public String name() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public Type type() {
			return type;
		}

		public void setType(Type type) {
			this.type = type;
		}
		
		public boolean isStatic() {
			return isStatic;
		}
		
		public void setIsStatic(boolean isStatic) {
			this.isStatic = isStatic;
		}
	}

	/**
	 * Represents an index into an array. E.g. A[i] is an index into array A.
	 * 
	 * @author djp
	 * 
	 */
	public static final class ArrayIndex extends SyntacticElementImpl implements JilExpr {
		protected JilExpr array;
		protected JilExpr idx;
		protected Type type;

		public ArrayIndex(JilExpr array, JilExpr idx, Type type, Attribute... attributes) {
			super(attributes);
			this.array = array;
			this.idx = idx;
			this.type = type;
		}

		public ArrayIndex(JilExpr array, JilExpr idx, Type type, List<Attribute> attributes) {
			super(attributes);
			this.array = array;
			this.idx = idx;
			this.type = type;
		}
		
		public JilExpr target() {
			return array;
		}

		public JilExpr index() {
			return idx;
		}
		
		public void setIndex(JilExpr e) {
			idx = e;
		}
		
		public void setTarget(JilExpr e) {
			array = e;
		}
		
		public Type type() {
			return type;
		}

		public void setType(Type type) {
			this.type = type;
		}
	}
	
	/**
	 * Represents a numerical constant
	 * 
	 * @author djp
	 *
	 */
	public static class Number extends SyntacticElementImpl implements JilExpr {
		protected int value;
		protected Type.Primitive type;
		
		public Number(int value, Type.Primitive type, Attribute... attributes) {
			super(attributes);
			this.value = value;
			this.type = type;
		}
		
		public Number(int value, Type.Primitive type, List<Attribute> attributes) {
			super(attributes);
			this.value = value;
			this.type = type;
		}
		
		public int intValue() {
			return value;
		}
		
		public Type.Primitive type() {
			return type;
		}
		
		public void setType(Type.Primitive type) {
			this.type = type;
		}		
	}
	
	/**
	 * A boolean constant.
	 * 
	 * @author djp
	 *
	 */
	public static final class Bool extends Number {
		public Bool(boolean value, Attribute... attributes) {
			super(value?1:0, new Type.Bool(), attributes);
		}
		
		public Bool(boolean value, List<Attribute> attributes) {
			super(value?1:0, new Type.Bool(), attributes);
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
	public static final class Char extends Number {
		public Char(char value, Attribute... attributes) {
			super(value,new Type.Char(),attributes);
		}
		
		public Char(char value, List<Attribute> attributes) {
			super(value,new Type.Char(),attributes);
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
	public static final class Byte extends Number {
		public Byte(byte value, Attribute... attributes) {
			super(value,new Type.Byte(), attributes);
		}
		
		public Byte(byte value, List<Attribute> attributes) {
			super(value,new Type.Byte(), attributes);
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
	public static final class Short extends Number {
		public Short(short value, Attribute... attributes) {
			super(value, new Type.Short(), attributes);
		}
		
		public Short(short value, List<Attribute> attributes) {
			super(value, new Type.Short(), attributes);
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
	public static final class Int extends Number {
		public Int(int value, Attribute... attributes) {
			super(value,new Type.Int(), attributes);
		}
		
		public Int(int value, List<Attribute> attributes) {
			super(value,new Type.Int(), attributes);
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
	public static final class Long extends SyntacticElementImpl implements JilExpr {
		private long value;
		
		public Long(long value, Attribute... attributes) {
			super(attributes);
			this.value=value;
		}
		
		public Long(long value, List<Attribute> attributes) {
			super(attributes);
			this.value=value;
		}
		
		public long value() {
			return value;
		}
		
		public Type.Long type() {
			return new Type.Long();
		}
	}
	
	/**
     * A Float Constant.
     * 
     * @author djp
     * 
     */
	public static final class Float extends SyntacticElementImpl implements JilExpr {
		private float value;
		
		public Float(float value, Attribute... attributes) {
			super(attributes);
			this.value=value;
		}
		
		public Float(float value, List<Attribute> attributes) {
			super(attributes);
			this.value=value;
		}
		
		public float value() {
			return value;
		}
		
		public Type.Float type() {
			return new Type.Float();
		}
	}

	/**
     * A Double Constant.
     * 
     * @author djp
     * 
     */
	public static final class Double extends SyntacticElementImpl implements JilExpr {
		private double value;
		
		public Double(double value, Attribute... attributes) {
			super(attributes);
			this.value=value;
		}
		
		public Double(double value, List<Attribute> attributes) {
			super(attributes);
			this.value=value;
		}
		
		public double value() {
			return value;
		}
		
		public Type.Double type() {
			return new Type.Double();
		}
	}
	
	/**
     * A String Constant.
     * 
     * @author djp
     * 
     */
	public static final class StringVal extends SyntacticElementImpl implements JilExpr {
		private java.lang.String value;
		
		public StringVal(java.lang.String value, Attribute... attributes) {
			super(attributes);
			this.value=value;
		}
		
		public StringVal(java.lang.String value, List<Attribute> attributes) {
			super(attributes);
			this.value = value;
		}
		
		public java.lang.String value() {
			return value;
		}
		
		public Type.Clazz type() {
			return new Type.Clazz("java.lang","String");
		}
	}		
	
	/**
     * The null Constant.
     * 
     * @author djp
     * 
     */
	public static final class Null extends SyntacticElementImpl implements JilExpr {
		public Null(Attribute... attributes) {
			super(attributes);
		}
		
		public Type.Null type() {
			return new Type.Null();
		}
	}
			
	/**
     * An array constant (used for array initialisers only).
     * 
     * @author djp
     * 
     */
	public static final class Array extends SyntacticElementImpl implements JilExpr {
		private List<JilExpr> values;
		private Type.Array type;
		
		public Array(List<JilExpr> values, Type.Array type, Attribute... attributes) {
			super(attributes);
			this.values = values;
			this.type = type;
		}
		
		public Array(List<JilExpr> values, Type.Array type, List<Attribute> attributes) {
			super(attributes);
			this.values = values;
			this.type = type;
		}
		
		public List<JilExpr> values() {
			return values;
		}
		
		public Type.Array type() {
			return type;
		}
		
		public void setType(Type.Array type) {
			this.type = type;
		}
	}	
	
	/**
	 * Represents a Class Constant
	 * 
	 */
	public static final class Class extends SyntacticElementImpl implements JilExpr {
		private Type.Clazz type;

		public Class(Type.Clazz type, Attribute... attributes) {
			super(attributes);
			this.type = type;
		}

		public Class(Type.Clazz type, List<Attribute> attributes) {
			super(attributes);
			this.type = type;
		}

		public Type.Clazz type() {
			return type;
		}
		
		public void setType(Type.Clazz type) {
			this.type = type;
		}
	}
}
