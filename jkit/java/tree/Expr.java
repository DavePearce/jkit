package jkit.java.tree;

import java.util.List;

import jkit.jil.Attribute;
import jkit.jil.SyntacticElement;
import jkit.jil.SyntacticElementImpl;

public interface Expr extends SyntacticElement {
	/**
	 * An (unresolved) variable
	 * 
	 * @author djp
	 * 
	 */
	public static class Variable extends SyntacticElementImpl implements Expr {
		private String value;

		public Variable(String value, Attribute... attributes) {
			super(attributes);
			this.value = value;
		}
		
		public Variable(String value, List<Attribute> attributes) {
			super(attributes);
			this.value = value;
		}

		public String value() {
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

		public ClassVariable(String type, Attribute... attributes) {
			super(attributes);
			this.type = type;
		}
		
		public ClassVariable(String type, List<Attribute> attributes) {
			super(attributes);
			this.type = type;
		}

		public String type() {
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

		public Cast(Type type, Expr expr, Attribute... attributes) {
			super(attributes);
			this.expr = expr;
			this.type = type;
		}

		public Cast(Type type, Expr expr, List<Attribute> attributes) {
			super(attributes);
			this.expr = expr;
			this.type = type;
		}
		
		public Expr expr() {
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
	public static class Convert extends SyntacticElementImpl implements Expr {
		protected Expr expr;
		protected Type.Primitive type;

		public Convert(Type.Primitive type, Expr expr, Attribute... attributes) {
			super(attributes);
			this.expr = expr;
			this.type = type;
		}

		public Expr expr() {
			return expr;
		}

		public Type type() {
			return type;
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

		public InstanceOf(Expr lhs, Type rhs, Attribute... attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public InstanceOf(Expr lhs, Type rhs, List<Attribute> attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
		}
		
		public Expr lhs() {
			return lhs;
		}

		public Type rhs() {
			return rhs;
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

		protected final Expr expr;
		protected final int op;

		public UnOp(int op, Expr expr, Attribute... attributes) {
			super(attributes);
			this.expr = expr;
			this.op = op;
		}

		public UnOp(int op, Expr expr, List<Attribute> attributes) {
			super(attributes);
			this.expr = expr;
			this.op = op;
		}
		
		public int op() {
			return op;
		}

		public Expr expr() {
			return expr;
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

		public BinOp(int op, Expr lhs, Expr rhs, Attribute... attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
			this.op = op;
		}

		public BinOp(int op, Expr lhs, Expr rhs, List<Attribute> attributes) {
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
	}

	public static class TernOp extends SyntacticElementImpl implements Expr {

		protected Expr cond;
		protected Expr toption;
		protected Expr foption;

		public TernOp(Expr con, Expr tOption, Expr fOption,
				Attribute... attributes) {
			super(attributes);
			cond = con;
			toption = tOption;
			foption = fOption;
		}

		public TernOp(Expr con, Expr tOption, Expr fOption,
				List<Attribute> attributes) {
			super(attributes);
			cond = con;
			toption = tOption;
			foption = fOption;
		}
		
		public Expr trueBranch() {
			return toption;
		}

		public Expr falseBranch() {
			return foption;
		}

		public Expr condition() {
			return cond;
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
				List<Type> typeParameters, Attribute... attributes) {
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
				List<Type> typeParameters, List<Attribute> attributes) {
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
				List<Decl> declarations, Attribute... attributes) {
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
				List<Decl> declarations, List<Attribute> attributes) {
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

		public Deref(Expr lhs, String rhs, Attribute... attributes) {
			super(attributes);
			this.target = lhs;
			this.name = rhs;
		}

		public Deref(Expr lhs, String rhs, List<Attribute> attributes) {
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

		public ArrayIndex(Expr array, Expr idx, Attribute... attributes) {
			super(attributes);
			this.array = array;
			this.idx = idx;
		}

		public ArrayIndex(Expr array, Expr idx, List<Attribute> attributes) {
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
	}
}
