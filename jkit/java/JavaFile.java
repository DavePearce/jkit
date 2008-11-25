package jkit.java;

import java.lang.reflect.Modifier;
import java.util.*;

import jkit.jkil.FlowGraph;
import jkit.jkil.SourceLocation;
import jkit.jkil.Type;
import jkit.jkil.FlowGraph.ArrayVal;
import jkit.jkil.FlowGraph.BinOp;
import jkit.jkil.FlowGraph.Cast;
import jkit.jkil.FlowGraph.ClassVal;
import jkit.jkil.FlowGraph.Expr;
import jkit.jkil.FlowGraph.FloatVal;
import jkit.jkil.FlowGraph.InstanceOf;
import jkit.jkil.FlowGraph.Invoke;
import jkit.jkil.FlowGraph.LVal;
import jkit.jkil.FlowGraph.LongVal;
import jkit.jkil.FlowGraph.NullVal;
import jkit.jkil.FlowGraph.Number;
import jkit.jkil.FlowGraph.Stmt;
import jkit.jkil.FlowGraph.StringVal;
import jkit.jkil.FlowGraph.TernOp;
import jkit.jkil.FlowGraph.Value;

public class JavaFile {
	private String pkg;
	private List<String> imports;
	private List<Clazz> classes; 
	
	public JavaFile(String pkg, List<String> imports, List<Clazz> classes) {
		this.pkg = pkg;
		this.imports = imports;
		this.classes = classes;
	}
	
	/**
	 * Get the package declared at the beginning of this file (if there is one)
	 */			
	public String pkg() { 
		return pkg;
	}
	
	/**
	 * Get the list of import declarations at the beginning of this file.
	 * 
	 * @return
	 */
	public List<String> imports() { 
		return imports;
	}
	
	/**
	 * Get the list of class declarations in this file.
	 * 
	 * @return
	 */
	public List<Clazz> classes() { 
		return classes;
	}
	
	
	/**
     * Represents the class of imperative statements allowed.
     * 
     * @author djp
     * 
     */		
	public static abstract class Statement {
		public final SourceLocation location;
		public Statement(SourceLocation location) {
			this.location = location;
		}
	}

	// ====================================================
	// Type
	// ====================================================

	public static class Type {
		private int dims;
		private List<String> components;
		public Type(List<String> components, int dims) {
			this.components = components;
			this.dims = dims;
		}
		public int dims() {
			return dims;
		}
		public void setDims(int dims) {
			this.dims = dims;
		}
		public List<String> components() {
			return components;
		}
		public void setComponents(List<String> components) {
			this.components = components;
		}
	}
	
	// ====================================================
	// DECLARATIONS
	// ====================================================
	
	public static abstract class Declaration {
		
	}
	
	public static class Clazz extends Declaration {
		private int modifiers;
		private String name;
		private Type superclass;
		private List<Type> interfaces;
		private List<Declaration> declarations;
				
		public Clazz(int modifiers, String name, Type superclass,
				List<Type> interfaces, List<Declaration> declarations) {
			this.modifiers = modifiers;
			this.name = name;
			this.superclass = superclass;
			this.interfaces = interfaces;
			this.declarations = declarations;
		}
		
		public int modifiers() {
			return modifiers;
		}
		
		public String name() {
			return name;
		}

		public Type superclass() {
			return superclass;
		}
		
		public List<Type> interfaces() {
			return interfaces;
		}
		
		public List<Declaration> declarations() { 
			return declarations;
		}
		
		
		/**
		 * Check whether this is an interface
		 */
		public boolean isInterface() { return (modifiers&Modifier.INTERFACE)!=0; } 

		/**
		 * Check whether this class or interface is abstract
		 */
		public boolean isAbstract() { return (modifiers&Modifier.ABSTRACT)!=0; }
		
		/**
		 * Check whether this class or interface is final
		 */
		public boolean isFinal() { return (modifiers&Modifier.FINAL)!=0; }
		
		/**
		 * Check whether this class or interface is static
		 */
		public boolean isStatic() { return (modifiers&Modifier.STATIC)!=0; }
		
		/**
		 * Check whether this class or interface is static
		 */
		public boolean isPublic() { return (modifiers&Modifier.PUBLIC)!=0; }
	}
	
	public static class Method extends Declaration {
		
	}
	
	public static class Field extends Declaration {
		private int modifiers;
		private String name;
		private Type type;
		private Expression initialiser;
		
		public Field(int modifiers, String name, Type type,
				Expression initialiser) {
			this.modifiers = modifiers;
			this.name = name;
			this.type = type;
			this.initialiser = initialiser;
		}
		
		public int modifiers() {
			return modifiers;
		}
		
		public String name() {
			return name;
		}

		public Type type() {
			return type;
		}
		
		public Expression initialiser() {
			return initialiser;
		}
		
		/**
		 * Check whether this is an interface
		 */
		public boolean isInterface() { return (modifiers&Modifier.INTERFACE)!=0; } 

		/**
		 * Check whether this class or interface is abstract
		 */
		public boolean isAbstract() { return (modifiers&Modifier.ABSTRACT)!=0; }
		
		/**
		 * Check whether this class or interface is final
		 */
		public boolean isFinal() { return (modifiers&Modifier.FINAL)!=0; }
		
		/**
		 * Check whether this class or interface is static
		 */
		public boolean isStatic() { return (modifiers&Modifier.STATIC)!=0; }
		
		/**
		 * Check whether this class or interface is static
		 */
		public boolean isPublic() { return (modifiers&Modifier.PUBLIC)!=0; }
			
	}
	
	// ====================================================
	// STATEMENTS
	// ====================================================

	public static class Block extends Statement {
		public final List<Statement> statements;
		public Block(List<Statement> statements, SourceLocation location) {
			super(location);
			this.statements = statements; 
		}
	}
	
	public static class Assignment extends Statement {
		public Assignment(SourceLocation location) {
			super(location);
		}
	}
	
	public static class WhileLoop extends Statement {
		public final Expression condition;
		public final Statement body;
		public WhileLoop(Expression condition, Statement body,
				SourceLocation location) {
			super(location);
			this.body = body; 
			this.condition = condition;
		}		
	}
	
	public static class ForLoop extends Statement {
		public final Statement initialiser;
		public final Expression condition;
		public final Statement increment;
		public final Statement body;
		
		
		public ForLoop(Statement initialiser, Expression condition,
				Statement increment, Statement body, SourceLocation location) {
			super(location);
			this.initialiser = initialiser;
			this.condition = condition;
			this.increment = increment;
			this.body = body; 						
		}
	}
		
	public static class ForEachLoop extends Statement {
		public ForEachLoop(SourceLocation location) {
			super(location);
		}
	}
	
	
	// ====================================================
	// EXPRESSIONS
	// ====================================================
	/**
     * Represents all expressions in the code
     * 
     * @author djp
     * 
     */	
	public static abstract class Expression {
		
	}
	
	/**
     * An (unresolved) variable
     * 
     * @author djp
     * 
     */
	public static class Variable extends Expression {
		private final String value;
		
		public Variable(String value) {			
			this.value=value;
		}
		
		public String value() {
			return value;
		}
	}		
	
	/**
	 * Represents an explicit cast.
	 * 
	 * @author djp
	 *
	 */
	public static class Cast extends Expression {		
		protected Expression expr;
		protected Type type;
		
		public Cast(Type type,  Expression expr) {			
			this.expr = expr;
			this.type = type;
		}

		public Expression expr() {
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
	public static class InstanceOf extends Expression {
		protected Expression lhs;		
		protected Type rhs;		
		
		public InstanceOf(Expression lhs, Type rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
		}
		

		public Expression lhs() {
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
	public static class UnOp extends Expression {
		public static final int NOT = 0;
		public static final int INV = 1;
		public static final int NEG = 2;
		public static final int PREINC = 3;
		public static final int PREDEC = 4;
		public static final int POSTINC = 5;
		public static final int POSTDEC = 6;
				
		protected final Expression expr;		
		protected final int op;
		
		public UnOp(int op, Expression expr) {
			this.expr = expr;
			this.op=op;
		}		
		
		public int op() {
			return op;
		}
		
		public Expression expr() {
			return expr;
		}
	}
	
	/**
     * A Binary Operator.  E.g. +.-,*,/,<,<=,>,?=,==,!=, etc.
     * 
     * @author djp
     * 
     */
	public static class BinOp extends Expression {
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
				
		protected final Expression lhs;
		protected final Expression rhs;
		protected final int op;				
		
		public BinOp(int op, Expression lhs, Expression rhs) {		
			this.lhs = lhs;
			this.rhs = rhs;
			this.op=op;
		}
		
		public int op() {
			return op;
		}
		
		public Expression lhs() {
			return lhs;
		}
		
		public Expression rhs() {
			return rhs;
		}
	}
	
	public static class TernOp extends Expression {
		
		protected Expression cond;
		protected Expression toption;
		protected Expression foption;
		
		public TernOp(Expression con, Expression tOption, Expression fOption) {		
			cond = con;
			toption = tOption;
			foption = fOption;
		}
		
		public Expression trueBranch() {
			return toption;
		}
		
		public Expression falseBranch() {
			return foption;
		}
		
		public Expression condition() {
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
	public static class Invoke extends Expression {
		private Expression target;
		private String name;		
		private List<Expression> parameters;
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
		public Invoke(Expression target, String name, List<Expression> parameters,
				List<Type> typeParameters) {			
			this.target = target;
			this.name = name;
			this.parameters = parameters;
			this.typeParameters = typeParameters;										
		}
		
		public Expression target() { return target; }
		
		public String name() { return name; }
		
		public List<Expression> parameters() { return parameters; }
		
		public List<Type> typeParameters() { return typeParameters; }
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
	public static final class New extends Expression {
		private Type type;
		private List<Expression> parameters;
		private List<Declaration> declarations;

		public New(Type type, List<Expression> parameters,
				List<Declaration> declarations) {
			this.type = type;
			this.parameters = parameters;
			this.declarations = declarations;
		}

		public Type type() {
			return type;
		}

		public List<Expression> parameters() {
			return parameters;
		}

		public List<Declaration> declarations() {
			return declarations;
		}
	}
	
	/**
     * Represents the act of derefencing a field.
     * 
     * @author djp
     * 
     */
	public static final class Deref extends Expression {
		private Expression target;
		private String name;
		
		
		public Deref(Expression lhs, String rhs) {
			this.target = lhs;
			this.name = rhs;
		}
		
		public Expression target() { 
			return target;
		}
		
		public String name() {
			return name;
		}
	}
	
	/**
     * Represents an index into an array. E.g. A[i] is an index into array A.
     * 
     * @author djp
     * 
     */
	public static class ArrayIndex extends Expression {
		private Expression array;
		private Expression idx;
		
		
		public ArrayIndex(Expression array, Expression idx) {
			this.array = array;
			this.idx = idx;
		}
	
		public Expression target() { 
			return array;			
		}
		
		public Expression index() {
			return idx;
		}
	}
	
	// ====================================================
	// Values
	// ====================================================
	
	public static abstract class Value extends Expression {
		
	}
		
	/**
	 * Represents a numerical constant
	 * 
	 * @author djp
	 *
	 */
	public static class Number extends Value {
		protected int value;
		
		public Number(int value) {	
			this.value = value;
		}
	}
	
	/**
	 * A boolean constant.
	 * 
	 * @author djp
	 *
	 */
	public static class BoolVal extends Number {
		public BoolVal(boolean value) {
			super(value?1:0);
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
	public static class CharVal extends Number {
		public CharVal(char value) {
			super(value);
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
	public static class ByteVal extends Number {
		public ByteVal(byte value) {
			super(value);
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
	public static class ShortVal extends Number {
		public ShortVal(short value) {
			super(value);
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
	public static class IntVal extends Number {
		public IntVal(int value) {
			super(value);
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
	public static class LongVal extends Value {
		private long value;
		
		public LongVal(long value) {
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
	public static class FloatVal extends Value {
		private float value;
		
		public FloatVal(float value) {
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
	public static class DoubleVal extends Value {
		private double value;
		
		public DoubleVal(double value) {			
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
	public static class StringVal extends Value {
		private final String value;
		
		public StringVal(String value) {			
			this.value=value;
		}
		
		public String value() {
			return value;
		}
	}		
	
	/**
     * The null Constant.
     * 
     * @author djp
     * 
     */
	public static class NullVal extends Value {	}
			
	/**
     * An array constant (used for array initialisers only).
     * 
     * @author djp
     * 
     */
	public static class ArrayVal extends Value {
		private List<Expression> values;
		
		public ArrayVal(List<Expression> values) {			
			this.values = values;
		}
		
		public List<Expression> values() {
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
	 * 
	 */
	public static class TypedArrayVal extends ArrayVal {
		private Type type;
		
		public TypedArrayVal(Type type, List<Expression> values) {			
			super(values);
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
	public static class ClassVal extends Value {
		private final Type classType;

		public ClassVal(Type type) {			
			this.classType = type;
		}
		
		public Type value() {
			return classType;
		}
	}
}
