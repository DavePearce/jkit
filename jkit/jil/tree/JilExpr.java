package jkit.jil.tree;

import java.util.*;
import java.util.concurrent.*;

public interface JilExpr extends SyntacticElement,Cloneable {
	
	public Type type();
	
	public static class AbstractExpr {
		private final ArrayList<Attribute> attributes;
		
		public AbstractExpr(Attribute... attributes) {
			this.attributes = new ArrayList();
			for(Attribute a : attributes) {
				this.attributes.add(a);
			}
		}
		
		public AbstractExpr(List<Attribute> attributes) {
			this.attributes = new ArrayList(attributes);			
		}
				
		public Attribute attribute(java.lang.Class ac) {
			for(Attribute a : attributes) {
				if(a.getClass().equals(ac)) {
					return a;
				}
			}
			return null;
		}
		
		public List<Attribute> attributes() {
			// this is to prevent any kind of aliasing issues.
			return new CopyOnWriteArrayList<Attribute>(attributes);
		}
	}
	
	/**
	 * A Variable object represents a local variable access. A variable
	 * access is considered local if the variable is declared within the current
	 * method. Non-local variable accesses correspond to variables which are
	 * declared outside the current method.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Variable extends AbstractExpr implements JilExpr {
		private final String value;
		private final Type type;

		public Variable(String value, Type type, Attribute... attributes) {
			super(attributes);
			if(value == null) {
				throw new IllegalArgumentException("supplied variable cannot be null");
			} 
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.value = value;
			this.type = type;
		}
		
		public Variable(String value, Type type, List<Attribute> attributes) {
			super(attributes);
			if(value == null) {
				throw new IllegalArgumentException("supplied variable cannot be null");
			} 
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.value = value;
			this.type = type;
		}

		public String value() {
			return value;
		}		
		
		public Type type() {
			return type;
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
		private final Type.Clazz type;

		public ClassVariable(Type.Clazz type, Attribute... attributes) {
			super(attributes);			
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.type = type;
		}

		public ClassVariable(Type.Clazz type, List<Attribute> attributes) {
			super(attributes);
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
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
		private final JilExpr expr;
		private final Type type;

		public Cast(JilExpr expr, Type type, Attribute... attributes) {
			super(attributes);
			if(expr == null) {
				throw new IllegalArgumentException("supplied expression cannot be null");
			}
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.expr = expr;
			this.type = type;
		}

		public Cast(JilExpr expr, Type type, List<Attribute> attributes) {
			super(attributes);
			if(expr == null) {
				throw new IllegalArgumentException("supplied expression cannot be null");
			}
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.expr = expr;
			this.type = type;
		}
		
		public JilExpr expr() {
			return expr;
		}

		public Type type() {
			return type;
		}
	}

	/**
	 * Represents an implicit type conversion between primitive types.
	 * 
	 * @author djp
	 *
	 */
	public static final class Convert extends SyntacticElementImpl implements JilExpr {
		private final JilExpr expr;
		private final Type.Primitive type;

		public Convert(Type.Primitive type, JilExpr expr, Attribute... attributes) {
			super(attributes);
			if(expr == null) {
				throw new IllegalArgumentException("supplied expression cannot be null");
			}
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.expr = expr;
			this.type = type;
		}

		public Convert(Type.Primitive type, JilExpr expr, List<Attribute> attributes) {
			super(attributes);
			if(expr == null) {
				throw new IllegalArgumentException("supplied expression cannot be null");
			}
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.expr = expr;
			this.type = type;
		}
		
		public JilExpr expr() {
			return expr;
		}

		public Type.Primitive type() {
			return type;
		}		
	}
	
	/**
	 * Represents an InstanceOf binary operation.
	 * 
	 * @author djp
	 * 
	 */
	public static final class InstanceOf extends SyntacticElementImpl implements JilExpr {
		private final JilExpr lhs;
		private final Type.Reference rhs;
		private final Type type;

		public InstanceOf(JilExpr lhs, Type.Reference rhs, Type type, Attribute... attributes) {
			super(attributes);
			if(lhs == null) {
				throw new IllegalArgumentException("supplied expression cannot be null");
			}			
			if(type == null || rhs == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.lhs = lhs;
			this.rhs = rhs;
			this.type = type;
		}

		public InstanceOf(JilExpr lhs, Type.Reference rhs, Type type, List<Attribute> attributes) {
			super(attributes);
			if(lhs == null) {
				throw new IllegalArgumentException("supplied expression cannot be null");
			}			
			if(type == null || rhs == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.lhs = lhs;
			this.rhs = rhs;
			this.type = type;
		}
		
		public JilExpr lhs() {
			return lhs;
		}

		public Type.Reference rhs() {
			return rhs;
		}
		
		public Type type() {
			return type;
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

		private final JilExpr expr;
		private final int op;
		private final Type.Primitive type;
		
		public UnOp(JilExpr expr, int op, Type.Primitive type, Attribute... attributes) {
			super(attributes);
			if(expr == null) {
				throw new IllegalArgumentException("supplied expression cannot be null");
			}			
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.expr = expr;
			this.op = op;
			this.type = type;
		}

		public UnOp(JilExpr expr, int op, Type.Primitive type, List<Attribute> attributes) {
			super(attributes);
			if(expr == null) {
				throw new IllegalArgumentException("supplied expression cannot be null");
			}			
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.expr = expr;
			this.op = op;
			this.type = type;
		}
		
		public int op() {
			return op;
		}

		public JilExpr expr() {
			return expr;
		}
		
		public Type.Primitive type() {
			return type;
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

		private final JilExpr lhs;
		private final JilExpr rhs;
		private final int op;
		private final Type.Primitive type;

		public BinOp(JilExpr lhs, JilExpr rhs, int op, Type.Primitive type, Attribute... attributes) {
			super(attributes);
			if(lhs == null || rhs == null) {
				throw new IllegalArgumentException("supplied expression(s) cannot be null");
			}			
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.lhs = lhs;
			this.rhs = rhs;
			this.op = op;
			this.type = type;
		}

		public BinOp(JilExpr lhs, JilExpr rhs, int op, Type.Primitive type, List<Attribute> attributes) {
			super(attributes);
			if(lhs == null || rhs == null) {
				throw new IllegalArgumentException("supplied expression(s) cannot be null");
			}			
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.lhs = lhs;
			this.rhs = rhs;
			this.op = op;
			this.type = type;
		}
		
		public int op() {
			return op;
		}

		public JilExpr lhs() {
			return lhs;
		}
		
		public JilExpr rhs() {
			return rhs;
		}
		
		public Type.Primitive type() {
			return type;
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
	public static class Invoke extends jkit.jil.tree.JilStmt.AbstractStmt implements JilExpr {
		private final JilExpr target;
		private final String name;
		private final ArrayList<JilExpr> parameters;
		private final Type type; 		
		private final Type.Function funType;		
				
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
			if(target == null) {
				throw new IllegalArgumentException("supplied expression(s) cannot be null");
			}
			if(parameters == null || parameters.contains(null)) {
				throw new IllegalArgumentException("supplied parameter(s) cannot be null");
			}
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.target = target;
			this.name = name;
			this.parameters = new ArrayList(parameters);
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
			if(target == null) {
				throw new IllegalArgumentException("supplied expression(s) cannot be null");
			}
			if(parameters == null || parameters.contains(null)) {
				throw new IllegalArgumentException("supplied parameter(s) cannot be null");
			}
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.target = target;
			this.name = name;
			this.parameters = new ArrayList(parameters);			
			this.type = type;
			this.funType = funType;
		}
		
		public JilExpr target() {
			return target;
		}
		
		public String name() {
			return name;
		}

		public Type type() {
			return type;
		}
		
		public Type.Function funType() {
			return funType;
		}
		
		public List<? extends JilExpr> parameters() {
			return parameters;
		}
		
		public Invoke clone() {
			return new Invoke(target, name, parameters, funType, type,
					attributes());
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
		
		public Invoke clone() {
			// Note, the unsafe cast below is actually safe!
			return new SpecialInvoke(target(), name(),
					(List<JilExpr>) parameters(), funType(), type(),
					attributes());
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
	public static final class New extends JilStmt.AbstractStmt implements JilExpr {
		private final Type.Reference type;		
		private final ArrayList<JilExpr> parameters;
		private final Type.Function funType;
		
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
			if(parameters == null || parameters.contains(null)) {
				throw new IllegalArgumentException("supplied parameter(s) cannot be null");
			}
			if(type == null || funType == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.type = type;			
			this.parameters = new ArrayList(parameters);			
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
			if(parameters == null || parameters.contains(null)) {
				throw new IllegalArgumentException("supplied parameter(s) cannot be null");
			}
			if(type == null || funType == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.type = type;			
			this.parameters = new ArrayList(parameters);			
			this.funType = funType;
		}
		
		public Type.Reference type() {
			return type;
		}

		public Type.Function funType() {
			return funType;
		}
		
		public List<? extends JilExpr> parameters() {
			return parameters;
		}
		
		public New clone() {
			return new New(type, parameters, funType, 
					attributes());
		}
	}

	/**
	 * Represents the act of derefencing a field.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Deref extends AbstractExpr implements JilExpr {
		private final JilExpr target;
		private final String name;
		private final Type type;
		private final boolean isStatic;
		
		public Deref(JilExpr lhs, String rhs, boolean isStatic, Type type,
				Attribute... attributes) {
			super(attributes);
			if(lhs == null) {
				throw new IllegalArgumentException("supplied expression cannot be null");
			}
			if(rhs == null) {
				throw new IllegalArgumentException("supplied string cannot be null");
			}
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.target = lhs;
			this.name = rhs;
			this.type = type;
			this.isStatic = isStatic;
		}

		public Deref(JilExpr lhs, String rhs, boolean isStatic, Type type, 
				List<Attribute> attributes) {
			super(attributes);
			if(lhs == null) {
				throw new IllegalArgumentException("supplied expression cannot be null");
			}
			if(rhs == null) {
				throw new IllegalArgumentException("supplied string cannot be null");
			}
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.target = lhs;
			this.name = rhs;
			this.type = type;
			this.isStatic = isStatic;
		}
		
		public JilExpr target() {
			return target;
		}
		
		public String name() {
			return name;
		}
				
		public Type type() {
			return type;
		}
		
		public boolean isStatic() {
			return isStatic;
		}		
	}

	/**
	 * Represents an index into an array. E.g. A[i] is an index into array A.
	 * 
	 * @author djp
	 * 
	 */
	public static final class ArrayIndex extends AbstractExpr implements JilExpr {
		private final JilExpr array;
		private final JilExpr idx;
		private final Type type;

		public ArrayIndex(JilExpr array, JilExpr idx, Type type, Attribute... attributes) {
			super(attributes);
			if(array == null || idx == null) {
				throw new IllegalArgumentException("supplied expression(s) cannot be null");
			}			
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.array = array;
			this.idx = idx;
			this.type = type;
		}

		public ArrayIndex(JilExpr array, JilExpr idx, Type type, List<Attribute> attributes) {
			super(attributes);
			if(array == null || idx == null) {
				throw new IllegalArgumentException("supplied expression(s) cannot be null");
			}			
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
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
		
		public Type type() {
			return type;
		}
	}
	
	/**
	 * Represents a numerical constant
	 * 
	 * @author djp
	 *
	 */
	public static class Number extends AbstractExpr implements JilExpr {
		protected final int value;
		private final Type.Primitive type;
		
		public Number(int value, Type.Primitive type, Attribute... attributes) {
			super(attributes);					
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.value = value;
			this.type = type;
		}
		
		public Number(int value, Type.Primitive type, List<Attribute> attributes) {
			super(attributes);
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.value = value;
			this.type = type;
		}
		
		public int intValue() {
			return value;
		}
		
		public Type.Primitive type() {
			return type;
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
	public static final class Long extends AbstractExpr implements JilExpr {
		private final long value;
		
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
	public static final class Float extends AbstractExpr implements JilExpr {
		private final float value;
		
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
	public static final class Double extends AbstractExpr implements JilExpr {
		private final double value;
		
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
	public static final class StringVal extends AbstractExpr implements JilExpr {
		private final java.lang.String value;
		
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
			return jkit.jil.util.Types.JAVA_LANG_STRING;			
		}
	}		
	
	/**
     * The null Constant.
     * 
     * @author djp
     * 
     */
	public static final class Null extends AbstractExpr implements JilExpr {
		public Null(Attribute... attributes) {
			super(attributes);
		}
		
		public Type.Null type() {
			return jkit.jil.util.Types.T_NULL;
		}
	}
			
	/**
     * An array constant (used for array initialisers only).
     * 
     * @author djp
     * 
     */
	public static final class Array extends AbstractExpr implements JilExpr {
		private final ArrayList<JilExpr> values;
		private final Type.Array type;
		
		public Array(List<JilExpr> values, Type.Array type, Attribute... attributes) {
			super(attributes);
			if(values == null || values.contains(null)) {
				throw new IllegalArgumentException("supplied expression(s) cannot be null");
			}			
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.values = new ArrayList(values);
			this.type = type;
		}
		
		public Array(List<JilExpr> values, Type.Array type, List<Attribute> attributes) {
			super(attributes);
			if(values == null || values.contains(null)) {
				throw new IllegalArgumentException("supplied expression(s) cannot be null");
			}			
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.values = new ArrayList(values);
			this.type = type;
		}
		
		public List<JilExpr> values() {
			return values;
		}
		
		public Type.Array type() {
			return type;
		}		
	}	
	
	/**
	 * Represents a Class Constant
	 * 
	 */
	public static final class Class extends AbstractExpr implements JilExpr {
		private final Type classType;
		private final Type.Clazz type;

		public Class(Type classType, Type.Clazz type, Attribute... attributes) {
			super(attributes);
			if(classType == null || type == null) {
				throw new IllegalArgumentException("supplied type(s) cannot be null");
			}
			this.type = type;
			this.classType = classType;
		}

		public Class(Type classType, Type.Clazz type, List<Attribute> attributes) {
			super(attributes);
			if(classType == null || type == null) {
				throw new IllegalArgumentException("supplied type(s) cannot be null");
			}
			this.type = type;
			this.classType = classType;
		}

		public Type.Clazz type() {
			return type;
		}
		
		public Type classType() {
			return classType;
		}		
	}
}
