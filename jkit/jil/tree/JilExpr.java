// This file is part of the Java Compiler Kit (JKit)
//
// The Java Compiler Kit is free software; you can 
// redistribute it and/or modify it under the terms of the 
// GNU General Public License as published by the Free Software 
// Foundation; either version 2 of the License, or (at your 
// option) any later version.
//
// The Java Compiler Kit is distributed in the hope
// that it will be useful, but WITHOUT ANY WARRANTY; without 
// even the implied warranty of MERCHANTABILITY or FITNESS FOR 
// A PARTICULAR PURPOSE.  See the GNU General Public License 
// for more details.
//
// You should have received a copy of the GNU General Public 
// License along with the Java Compiler Kit; if not, 
// write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA
//
// (C) David James Pearce, 2009. 

package jkit.jil.tree;

import java.util.*;
import java.util.concurrent.*;
import jkit.util.Pair;

public interface JilExpr extends SyntacticElement,Cloneable {
	
	public Type type();
	
	public static class AbstractExpr {
		private final ArrayList<SyntacticAttribute> attributes;
		
		public AbstractExpr(SyntacticAttribute... attributes) {
			this.attributes = new ArrayList();
			for(SyntacticAttribute a : attributes) {
				this.attributes.add(a);
			}
		}
		
		public AbstractExpr(List<SyntacticAttribute> attributes) {
			this.attributes = new ArrayList(attributes);			
		}
				
		public <T extends SyntacticAttribute> T attribute(java.lang.Class<T> ac) {
			for(SyntacticAttribute a : attributes) {
				if(a.getClass().equals(ac)) {
					return (T) a;
				}
			}
			return null;
		}
		
		public List<SyntacticAttribute> attributes() {
			// this is to prevent any kind of aliasing issues.
			return new CopyOnWriteArrayList<SyntacticAttribute>(attributes);
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

		public Variable(String value, Type type, SyntacticAttribute... attributes) {
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
		
		public Variable(String value, Type type, List<SyntacticAttribute> attributes) {
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
		
		public String toString() {
			return value;
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

		public ClassVariable(Type.Clazz type, SyntacticAttribute... attributes) {
			super(attributes);			
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.type = type;
		}

		public ClassVariable(Type.Clazz type, List<SyntacticAttribute> attributes) {
			super(attributes);
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.type = type;
		}

		public Type.Clazz type() {
			return type;
		}
		
		public String toString() {
			return type.toString();
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

		public Cast(JilExpr expr, Type type, SyntacticAttribute... attributes) {
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

		public Cast(JilExpr expr, Type type, List<SyntacticAttribute> attributes) {
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
		
		public String toString() {
			return "(" + type.toString() + ")" + expr;
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

		public Convert(Type.Primitive type, JilExpr expr, SyntacticAttribute... attributes) {
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

		public Convert(Type.Primitive type, JilExpr expr, List<SyntacticAttribute> attributes) {
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
		
		public String toString() {
			return "[" + type.toString() + "]" + expr;
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

		public InstanceOf(JilExpr lhs, Type.Reference rhs, Type type, SyntacticAttribute... attributes) {
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

		public InstanceOf(JilExpr lhs, Type.Reference rhs, Type type, List<SyntacticAttribute> attributes) {
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
		
		public String toString() {
			return "(" + lhs.toString() + " instanceof " + rhs + ")";
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
		
		public UnOp(JilExpr expr, int op, Type.Primitive type, SyntacticAttribute... attributes) {
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

		public UnOp(JilExpr expr, int op, Type.Primitive type, List<SyntacticAttribute> attributes) {
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
		
		public static final String[] unopstr={"!","~","-","++","--","++","--"};	
		
		public String toString() {
			return unopstr[op] + "(" + expr + ")";
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

		public BinOp(JilExpr lhs, JilExpr rhs, int op, Type.Primitive type, SyntacticAttribute... attributes) {
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

		public BinOp(JilExpr lhs, JilExpr rhs, int op, Type.Primitive type, List<SyntacticAttribute> attributes) {
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
		
		protected static final String[] binopstr = {"+", "-", "*", "/", "%", "<<",
			">>", ">>>", "&", "|", "^", "<", "<=", ">", ">=", "==", "!=", "&&",
			"||", "+"};
		
		public String toString() {
			return "(" + lhs + binopstr[op] + rhs + ")";
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
		protected final JilExpr target;
		protected final String name;
		protected final ArrayList<JilExpr> parameters;
		protected final Type type; 		
		protected final Type.Function funType;		
				
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
				Type.Function funType, Type type, SyntacticAttribute... attributes) {
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
				Type.Function funType, Type type, List<SyntacticAttribute> attributes) {
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
		
		public Invoke(JilExpr target, String name, List<JilExpr> parameters,
				Type.Function funType, Type type,
				List<Pair<Type.Clazz, String>> exceptions,
				List<SyntacticAttribute> attributes) {
			super(exceptions, attributes);
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
					(List<Pair<Type.Clazz, String>>) exceptions(),attributes());
		}
		
		public String toString() {
			String r = target.toString() + "." + name + "(";
			boolean firstTime = true;
			for(JilExpr p : parameters) {
				if(!firstTime) {
					r += ", ";
				}
				firstTime=false;
				r += p.toString();
			}
			return r + ")";
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
				Type.Function funType, Type type, SyntacticAttribute... attributes) {
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
				Type.Function funType, Type type, List<SyntacticAttribute> attributes) {
			super(target,name,parameters,funType,type,attributes);			
		}
		
		public SpecialInvoke(JilExpr target, String name,
				List<JilExpr> parameters, Type.Function funType, Type type,
				List<Pair<Type.Clazz, String>> exceptions,
				List<SyntacticAttribute> attributes) {
			super(target,name,parameters,funType,type,exceptions,attributes);			
		}
		
		public Invoke clone() {
			// Note, the unsafe cast below is actually safe!
			return new SpecialInvoke(target(), name(),
					(List<JilExpr>) parameters(), funType(), type(),
					(List<Pair<Type.Clazz, String>>) exceptions(),attributes());
		}
		
		public String toString() {
			String r = target.toString() + "." + name + "!(";
			boolean firstTime = true;
			for(JilExpr p : parameters) {
				if(!firstTime) {
					r += ", ";
				}
				firstTime=false;
				r += p.toString();
			}
			return r + ")";
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
				Type.Function funType, SyntacticAttribute... attributes) {
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
				Type.Function funType, List<SyntacticAttribute> attributes) {
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
		
		public New(Type.Reference type, List<JilExpr> parameters,
				Type.Function funType,
				List<Pair<Type.Clazz, String>> exceptions,
				List<SyntacticAttribute> attributes) {
			super(exceptions,attributes);
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
					(List<Pair<Type.Clazz, String>>) exceptions(),attributes());
		}
		public String toString() {
			String r = "new " + type + "(";
			boolean firstTime = true;
			for(JilExpr p : parameters) {
				if(!firstTime) {
					r += ", ";
				}
				firstTime=false;
				r += p.toString();
			}
			return r + ")";
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
				SyntacticAttribute... attributes) {
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
				List<SyntacticAttribute> attributes) {
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
		
		public String toString() {
			return target + "." + name;
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

		public ArrayIndex(JilExpr array, JilExpr idx, Type type, SyntacticAttribute... attributes) {
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

		public ArrayIndex(JilExpr array, JilExpr idx, Type type, List<SyntacticAttribute> attributes) {
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
		
		public String toString() {
			return array + "[" + idx + "]";
		}
	}
	
	public static interface Value extends JilExpr {}
	
	/**
	 * Represents a numerical constant
	 * 
	 * @author djp
	 *
	 */
	public static class Number extends AbstractExpr implements JilExpr,Value {
		protected final int value;
		private final Type.Primitive type;
		
		public Number(int value, Type.Primitive type, SyntacticAttribute... attributes) {
			super(attributes);					
			if(type == null) {
				throw new IllegalArgumentException("supplied type cannot be null");
			}
			this.value = value;
			this.type = type;
		}
		
		public Number(int value, Type.Primitive type, List<SyntacticAttribute> attributes) {
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
		public Bool(boolean value, SyntacticAttribute... attributes) {
			super(value?1:0, new Type.Bool(), attributes);
		}
		
		public Bool(boolean value, List<SyntacticAttribute> attributes) {
			super(value?1:0, new Type.Bool(), attributes);
		}
		
		public boolean value() {
			return value==1;
		}
		
		public String toString() {
			if(value==1) { return "true"; }
			else { return "false"; }
		}
	}
	
	/**
	 * Represents a character constant.
	 * 
	 * @author djp
	 *
	 */
	public static final class Char extends Number {
		public Char(char value, SyntacticAttribute... attributes) {
			super(value,new Type.Char(),attributes);
		}
		
		public Char(char value, List<SyntacticAttribute> attributes) {
			super(value,new Type.Char(),attributes);
		}
		
		public char value() {
			return (char)value;
		}
		
		public String toString() {
			return "'" + (char)value + "'"; 
		}
	}
	
	/**
	 * Represents a byte constant.
	 * 
	 * @author djp
	 *
	 */
	public static final class Byte extends Number {
		public Byte(byte value, SyntacticAttribute... attributes) {
			super(value,new Type.Byte(), attributes);
		}
		
		public Byte(byte value, List<SyntacticAttribute> attributes) {
			super(value,new Type.Byte(), attributes);
		}
		
		public byte value() {
			return (byte)value;
		}
		
		public String toString() {
			return value + "b"; 
		}
	}
	
	/**
	 * Represents a short constant.
	 * @author djp
	 *
	 */
	public static final class Short extends Number {
		public Short(short value, SyntacticAttribute... attributes) {
			super(value, new Type.Short(), attributes);
		}
		
		public Short(short value, List<SyntacticAttribute> attributes) {
			super(value, new Type.Short(), attributes);
		}
		
		public short value() {
			return (short)value;
		}
		
		public String toString() {
			return value + "s"; 
		}
	}

	/**
     * Represents an int constant.
     * 
     * @author djp
     * 
     */	
	public static final class Int extends Number {
		public Int(int value, SyntacticAttribute... attributes) {
			super(value,new Type.Int(), attributes);
		}
		
		public Int(int value, List<SyntacticAttribute> attributes) {
			super(value,new Type.Int(), attributes);
		}
		
		public int value() {
			return value;
		}
		public String toString() {
			return java.lang.Integer.toString(value);			
		}
	}

	/**
     * Represents a long Constant.
     * 
     * @author djp
     * 
     */
	public static final class Long extends AbstractExpr implements JilExpr,Value {
		private final long value;
		
		public Long(long value, SyntacticAttribute... attributes) {
			super(attributes);			
			this.value=value;
		}
		
		public Long(long value, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.value=value;
		}
		
		public long value() {
			return value;
		}
		
		public Type.Long type() {
			return new Type.Long();
		}
		public String toString() {
			return value + "l"; 
		}
	}
	
	/**
     * A Float Constant.
     * 
     * @author djp
     * 
     */
	public static final class Float extends AbstractExpr implements JilExpr,Value {
		private final float value;
		
		public Float(float value, SyntacticAttribute... attributes) {
			super(attributes);
			this.value=value;
		}
		
		public Float(float value, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.value=value;
		}
		
		public float value() {
			return value;
		}
		
		public Type.Float type() {
			return new Type.Float();
		}
		public String toString() {
			return value + "f"; 
		}
	}

	/**
     * A Double Constant.
     * 
     * @author djp
     * 
     */
	public static final class Double extends AbstractExpr implements JilExpr,Value {
		private final double value;
		
		public Double(double value, SyntacticAttribute... attributes) {
			super(attributes);
			this.value=value;
		}
		
		public Double(double value, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.value=value;
		}
		
		public double value() {
			return value;
		}
		
		public Type.Double type() {
			return new Type.Double();
		}
		public String toString() {
			return java.lang.Double.toString(value); 
		}
	}
	
	/**
     * A String Constant.
     * 
     * @author djp
     * 
     */
	public static final class StringVal extends AbstractExpr implements JilExpr,Value {
		private final java.lang.String value;
		
		public StringVal(java.lang.String value, SyntacticAttribute... attributes) {
			super(attributes);
			this.value=value;
		}
		
		public StringVal(java.lang.String value, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.value = value;
		}
		
		public java.lang.String value() {
			return value;
		}
		
		public Type.Clazz type() {
			return jkit.jil.util.Types.JAVA_LANG_STRING;			
		}
		public String toString() {
			return "\"" + value + "\""; 
		}
	}		
	
	/**
     * The null Constant.
     * 
     * @author djp
     * 
     */
	public static final class Null extends AbstractExpr implements JilExpr,Value {
		public Null(SyntacticAttribute... attributes) {
			super(attributes);
		}
		
		public Type.Null type() {
			return jkit.jil.util.Types.T_NULL;
		}
		public String toString() {
			return "null"; 
		}
	}
			
	/**
     * An array constant (used for array initialisers only).
     * 
     * @author djp
     * 
     */
	public static final class Array extends AbstractExpr implements JilExpr,Value {
		private final ArrayList<JilExpr> values;
		private final Type.Array type;
		
		public Array(List<JilExpr> values, Type.Array type, SyntacticAttribute... attributes) {
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
		
		public Array(List<JilExpr> values, Type.Array type, List<SyntacticAttribute> attributes) {
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
		
		public String toString() {
			String r = type + "{";
			boolean firstTime=true;
			for(JilExpr v : values) {
				if(!firstTime) {
					r += ", ";
				}
				firstTime = false;
				r += v.toString();
			}
			return r + "}";
		}
	}	
	
	/**
	 * Represents a Class Constant
	 * 
	 */
	public static final class Class extends AbstractExpr implements JilExpr,Value {
		private final Type classType;
		private final Type.Clazz type;

		public Class(Type classType, Type.Clazz type, SyntacticAttribute... attributes) {
			super(attributes);
			if(classType == null || type == null) {
				throw new IllegalArgumentException("supplied type(s) cannot be null");
			}
			this.type = type;
			this.classType = classType;
		}

		public Class(Type classType, Type.Clazz type, List<SyntacticAttribute> attributes) {
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
		
		public String toString() {
			return classType + ".class";
		}
	}
}
