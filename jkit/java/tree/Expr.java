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

package jkit.java.tree;

import java.util.List;

import jkit.compiler.SyntacticAttribute;
import jkit.compiler.SyntacticElement;
import jkit.compiler.SyntacticElementImpl;
import jkit.error.OperatorTypeMismatchException.AllowedType;

public interface Expr extends SyntacticElement {
	/**
	 * An unresolved variable access. The parser will turn anything that looks
	 * like a variable access into a Variable object. However, in many cases,
	 * these do not correspond to variable accesses. Instead, they can be field
	 * accesses, static class references, non-local variable accesses, etc.
	 * Thus, all variable accesses are resolved during scope resolution to
	 * determine in more detail what they are. Once scope resolution is
	 * complete, there should be no UnresolvedVariable instances remaining.
	 *
	 * @author djp
	 *
	 */
	public static class UnresolvedVariable extends SyntacticElementImpl implements Expr {
		private String value;

		public UnresolvedVariable(String value, SyntacticAttribute... attributes) {
			super(attributes);
			this.value = value;
		}

		public UnresolvedVariable(String value, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.value = value;
		}

		public String value() {
			return value;
		}

		public String toString() {
			return value;
		}
	}

	/**
	 * A LocalVariable object represents a local variable access. A variable
	 * access is considered local if the variable is declared within the current
	 * method. Non-local variable accesses correspond to variables which are
	 * declared outside the current method.
	 *
	 * @author djp
	 *
	 */
	public static class LocalVariable extends SyntacticElementImpl implements Expr {
		private String value;

		public LocalVariable(String value, SyntacticAttribute... attributes) {
			super(attributes);
			this.value = value;
		}

		public LocalVariable(String value, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.value = value;
		}

		public String value() {
			return value;
		}

		public String toString() {
			return value;
		}
	}

	/**
	 * A NonLocalVariable object represents a non-local variable access. This
	 * typically occurs in the definition of anonymous inner classes. For
	 * example:
	 *
	 * <pre>
	 * public abstract class Test {
	 * 	public abstract void f();
	 *
	 * 	public static void main(final String[] args) {
	 *    Test x = new Test() {
	 * 	    public void f() {
	 *        System.out.println(args[0]);
	 *      }
	 *    };
	 *    x.f();
	 * }}
	 * </pre>
	 *
	 * Here, the access "args[0]" is a non-local variable access.
	 *
	 * @author djp
	 */
	public static class NonLocalVariable extends SyntacticElementImpl implements Expr {
		private String value;

		public NonLocalVariable(String value, SyntacticAttribute... attributes) {
			super(attributes);
			this.value = value;
		}

		public NonLocalVariable(String value, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.value = value;
		}

		public String value() {
			return value;
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
	public static class ClassVariable extends SyntacticElementImpl implements Expr {
		private String type;

		public ClassVariable(String type, SyntacticAttribute... attributes) {
			super(attributes);
			this.type = type;
		}

		public ClassVariable(String type, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.type = type;
		}

		public String type() {
			return type;
		}

		public String toString() {
			return type;
		}
	}


	/**
	 * Represents an explicit cast.
	 *
	 * @author djp
	 *
	 */
	public static class Cast extends SyntacticElementImpl implements Expr {
		protected Expr expr;
		protected Type type;

		public Cast(Type type, Expr expr, SyntacticAttribute... attributes) {
			super(attributes);
			this.expr = expr;
			this.type = type;
		}

		public Cast(Type type, Expr expr, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.expr = expr;
			this.type = type;
		}

		public Expr expr() {
			return expr;
		}

		public void setExpr(Expr e) {
			expr = e;
		}

		public Type type() {
			return type;
		}

		public void setType(Type t) {
			type = t;
		}

		public String toString() {
			return String.format("(%s) %s", type, expr);
		}
	}

	/**
	 * Represents an implicit type conversion between primitive types.
	 *
	 * @author djp
	 *
	 */
	public static class Convert extends SyntacticElementImpl implements Expr {
		protected Expr expr;
		protected Type.Primitive type;

		public Convert(Type.Primitive type, Expr expr, SyntacticAttribute... attributes) {
			super(attributes);
			this.expr = expr;
			this.type = type;
		}

		public Expr expr() {
			return expr;
		}

		public void setExpr(Expr expr) {
			this.expr = expr;
		}

		public Type type() {
			return type;
		}

		public void setType(Type.Primitive type) {
			this.type = type;
		}

		public String toString() {
			//This is just a guess as to what this is meant to look like
			return expr.toString();
		}

	}

	/**
	 * Represents an InstanceOf binary operation.
	 *
	 * @author djp
	 *
	 */
	public static class InstanceOf extends SyntacticElementImpl implements Expr {
		protected Expr lhs;
		protected Type rhs;

		public InstanceOf(Expr lhs, Type rhs, SyntacticAttribute... attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public InstanceOf(Expr lhs, Type rhs, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public Expr lhs() {
			return lhs;
		}

		public void setLhs(Expr e) {
			lhs = e;
		}

		public Type rhs() {
			return rhs;
		}

		public void setRhs(Type e) {
			rhs = e;
		}

		public String toString() {
			return String.format("%s instanceof %s", lhs, rhs);
		}
	}

	/**
	 * Represents Unary Arithmetic Operators
	 *
	 * @author djp
	 *
	 */
	public static class UnOp extends SyntacticElementImpl implements Expr {
		public static final int NOT = 0;
		public static final int INV = 1;
		public static final int NEG = 2;
		public static final int PREINC = 3;
		public static final int PREDEC = 4;
		public static final int POSTINC = 5;
		public static final int POSTDEC = 6;

		protected Expr expr;
		protected int op;

		public UnOp(int op, Expr expr, SyntacticAttribute... attributes) {
			super(attributes);
			this.expr = expr;
			this.op = op;
		}

		public UnOp(int op, Expr expr, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.expr = expr;
			this.op = op;
		}

		public int op() {
			return op;
		}

		public void setOp(int op) {
			this.op = op;
		}

		public Expr expr() {
			return expr;
		}

		public void setExpr(Expr expr) {
			this.expr = expr;
		}

		public String toString() {
			return (op == POSTINC || op == POSTDEC) ?
					expr.toString() + operator() : operator() + expr.toString();
		}

		public String operator() {
			switch(op) {

			case NOT:
				return "!";

			case INV:
				return "~";

			case NEG:
				return "-";

			case PREINC:
				return "++";

			case PREDEC:
				return "--";

			case POSTINC:
				return "++";

			case POSTDEC:
				return "--";

			default:
				return String.valueOf(op);
			}
		}
	}

	/**
	 * A Binary Operator.  E.g. +.-,*,/,<,<=,>,?=,==,!=, etc.
	 *
	 * @author djp
	 *
	 */
	public static class BinOp extends SyntacticElementImpl implements Expr {
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

		public static final int CONCAT = 19; // string concatenation

		protected Expr lhs;
		protected Expr rhs;
		protected int op;

		public BinOp(int op, Expr lhs, Expr rhs, SyntacticAttribute... attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
			this.op = op;
		}

		public BinOp(int op, Expr lhs, Expr rhs, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
			this.op = op;
		}

		public int op() {
			return op;
		}

		public void setOp(int op) {
			this.op = op;
		}

		public Expr lhs() {
			return lhs;
		}

		public void setLhs(Expr lhs) {
			this.lhs = lhs;
		}

		public Expr rhs() {
			return rhs;
		}

		public void setRhs(Expr rhs) {
			this.rhs = rhs;
		}

		public String toString() {
			return String.format("%s %s %s", lhs.toString(), operator(), rhs.toString());
		}

		public String operator() {
			switch (op) {

			case (ADD):
				return "+";

			case (SUB):
				return "-";

			case (MUL):
				return "*";

			case (DIV):
				return "/";

			case (MOD):
				return "%";

			case (SHL):
				return "<<";

			case (SHR):
				return ">>";

			case (USHR):
				return ">>>";

			case (AND):
				return "&";

			case (OR):
				return "|";

			case (XOR):
				return "^";

			case (LT):
				return "<";

			case (LTEQ):
				return "<=";

			case (GT):
				return ">";

			case (GTEQ):
				return ">=";

			case (EQ):
				return "==";

			case (NEQ):
				return "!=";

			case (LAND):
				return "&&";

			case (LOR):
				return "||";

			case (CONCAT):
				return "+";

			default:
				return String.valueOf(op);
			}
		}

		/**
		 * Utility method - tells what types are allowed for a given operator
		 *
		 * @param left	- Whether we are considering the right or left side of the operator
		 * @return
		 */
		public AllowedType getAllowed(boolean left) {
			AllowedType type = null;
			switch (op) {

			case (ADD):
			case (SUB):
			case (MUL):
			case (DIV):
			case (MOD):
				type = AllowedType.PRIMITIVE;
				break;

			case (SHL):
			case (SHR):
			case (USHR):
				type = (left) ? AllowedType.INTEGER : AllowedType.INT;
				break;

			case (AND):
			case (OR):
			case (XOR):
			case (LT):
			case (LTEQ):
			case (GT):
			case (GTEQ):
				type = AllowedType.INTEGER;
				break;

			case (EQ):
			case (NEQ):
				type = AllowedType.ANY;
				break;

			case (LAND):
			case (LOR):
				type = AllowedType.BOOL;
				break;

			case (CONCAT):
				type = AllowedType.ANY;
				break;

			}
			return type;
		}
	}

	public static class TernOp extends SyntacticElementImpl implements Expr {

		protected Expr cond;
		protected Expr toption;
		protected Expr foption;

		public TernOp(Expr con, Expr tOption, Expr fOption,
				SyntacticAttribute... attributes) {
			super(attributes);
			cond = con;
			toption = tOption;
			foption = fOption;
		}

		public TernOp(Expr con, Expr tOption, Expr fOption,
				List<SyntacticAttribute> attributes) {
			super(attributes);
			cond = con;
			toption = tOption;
			foption = fOption;
		}

		public Expr trueBranch() {
			return toption;
		}

		public void setTrueBranch(Expr toption) {
			this.toption = toption;
		}

		public Expr falseBranch() {
			return foption;
		}

		public void setFalseBranch(Expr foption) {
			this.foption = foption;
		}

		public Expr condition() {
			return cond;
		}

		public void setCondition(Expr condition) {
			this.cond = condition;
		}

		public String toString() {
			return String.format("(%s) ? %s : %s", cond, toption, foption);
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
	public static class Invoke extends SyntacticElementImpl implements Expr,
			Stmt.Simple {
		private Expr target;
		private String name;
		private List<Expr> parameters;
		private List<Type> typeParameters;

		/**
		 * Construct a method which may, or may not be polymorphic.
		 *
		 * @param target
		 *            The expression from which the receiver is determined
		 * @param name
		 *            The name of the method
		 * @param parameters
		 *            The parameters of the method
		 */
		public Invoke(Expr target, String name, List<Expr> parameters,
				List<Type> typeParameters, SyntacticAttribute... attributes) {
			super(attributes);
			this.target = target;
			this.name = name;
			this.parameters = parameters;
			this.typeParameters = typeParameters;
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
		 */
		public Invoke(Expr target, String name, List<Expr> parameters,
				List<Type> typeParameters, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.target = target;
			this.name = name;
			this.parameters = parameters;
			this.typeParameters = typeParameters;
		}

		public Expr target() {
			return target;
		}

		public void setTarget(Expr target) {
			this.target = target;
		}

		public String name() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Expr> parameters() {
			return parameters;
		}

		public List<Type> typeParameters() {
			return typeParameters;
		}

		public String toString() {
			String params = "(";
			boolean first = true;

			for (Expr e : parameters) {
				if (!first)
					params += ", ";
				params += e.toString();
				first = false;
			}
			params += ")";

			return String.format("%s.%s%s", target, name, params);
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
	public static class New extends SyntacticElementImpl implements Expr,
			Stmt.Simple {
		private Type type;
		private Expr context;
		private List<Expr> parameters;
		private List<Decl> declarations;

		/**
		 * Create an AST node represent a new statement or expression.
		 *
		 * @param type -
		 *            the type being constructed e.g. java.lang.String or
		 *            Integer[]
		 * @param Context -
		 *            the context in which the type is constructed. This is only
		 *            required for non-static classes which are created outside
		 *            of their enclosing class's scope. For example, i = o.new
		 *            Inner(); Should be null if no context.
		 * @param parameters -
		 *            The parameters (if any) supplied to the constructor.
		 *            Should be an empty (i.e. non-null) list .
		 * @param declarations -
		 *            the list of field/method/class declarations contained in
		 *            this statement. These arise only when constructing a new
		 *            anonymous class. Again, should be an empty (i.e. non-null)
		 *            list.
		 */
		public New(Type type, Expr context, List<Expr> parameters,
				List<Decl> declarations, SyntacticAttribute... attributes) {
			super(attributes);
			this.type = type;
			this.context = context;
			this.parameters = parameters;
			this.declarations = declarations;
		}

		/**
		 * Create an AST node represent a new statement or expression.
		 *
		 * @param type -
		 *            the type being constructed e.g. java.lang.String or
		 *            Integer[]
		 * @param Context -
		 *            the context in which the type is constructed. This is only
		 *            required for non-static classes which are created outside
		 *            of their enclosing class's scope. For example, i = o.new
		 *            Inner(); Should be null if no context.
		 * @param parameters -
		 *            The parameters (if any) supplied to the constructor.
		 *            Should be an empty (i.e. non-null) list .
		 * @param declarations -
		 *            the list of field/method/class declarations contained in
		 *            this statement. These arise only when constructing a new
		 *            anonymous class. Again, should be an empty (i.e. non-null)
		 *            list.
		 */
		public New(Type type, Expr context, List<Expr> parameters,
				List<Decl> declarations, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.type = type;
			this.context = context;
			this.parameters = parameters;
			this.declarations = declarations;
		}

		public Type type() {
			return type;
		}

		public void setType(Type type) {
			this.type = type;
		}

		public List<Expr> parameters() {
			return parameters;
		}

		public void setParameters(List<Expr> parameters) {
			this.parameters = parameters;
		}

		public List<Decl> declarations() {
			return declarations;
		}

		public void setDeclarations(List<Decl> declarations) {
			this.declarations = declarations;
		}

		public Expr context() {
			return context;
		}

		public void setContext(Expr context) {
			this.context = context;
		}

		public String toString() {
			String ctxt = "";
				if (context != null)
					ctxt += context.toString() + ".";

			String params = "(";
			boolean first = true;

			for (Expr e : parameters) {
				if (!first)
					params += ", ";
				params += e.toString();
				first = false;
			}
			params += ")";

			String decl = "";
			if (!declarations.isEmpty()) {
				//If it comes up, I'll write toString for declarations as well
				decl = " {\n\t...\n}";
			}
			String t = (type instanceof Type.Clazz) ?
					jkit.compiler.ClassLoader.pathChild(type.toString()) :
						type.toString();
			return String.format("new %s%s%s%s", ctxt, t, params, decl);
		}
	}

	/**
	 * Represents the act of derefencing a field.
	 *
	 * @author djp
	 *
	 */
	public static class Deref extends SyntacticElementImpl implements Expr {
		private Expr target;
		private String name;

		public Deref(Expr lhs, String rhs, SyntacticAttribute... attributes) {
			super(attributes);
			this.target = lhs;
			this.name = rhs;
		}

		public Deref(Expr lhs, String rhs, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.target = lhs;
			this.name = rhs;
		}

		public Expr target() {
			return target;
		}

		public void setTarget(Expr target) {
			this.target = target;
		}

		public String name() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String toString() {
			return String.format("%s.%s", target, name);
		}
	}

	/**
	 * Represents an index into an array. E.g. A[i] is an index into array A.
	 *
	 * @author djp
	 *
	 */
	public static class ArrayIndex extends SyntacticElementImpl implements Expr {
		private Expr array;
		private Expr idx;

		public ArrayIndex(Expr array, Expr idx, SyntacticAttribute... attributes) {
			super(attributes);
			this.array = array;
			this.idx = idx;
		}

		public ArrayIndex(Expr array, Expr idx, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.array = array;
			this.idx = idx;
		}

		public Expr target() {
			return array;
		}

		public Expr index() {
			return idx;
		}

		public void setIndex(Expr e) {
			idx = e;
		}

		public void setTarget(Expr e) {
			array = e;
		}

		public String toString() {
			return String.format("%s[%s]", array, idx);
		}
	}
}
