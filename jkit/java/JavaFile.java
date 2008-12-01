package jkit.java;

import java.lang.reflect.Modifier;
import java.util.*;

import jkit.jkil.FlowGraph;
import jkit.jkil.SourceLocation;
import jkit.jkil.Type;
import jkit.util.*;

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
	public static interface Statement {		
	}

	public static interface SimpleStatement extends Statement {		
	}
	
	// ====================================================
	// Type
	// ====================================================

	public static interface Type {}
	
	public static class ReferenceType implements Type {
		private int dims;
		private List<Pair<String, List<ReferenceType>>> components;
		public ReferenceType(
				List<Pair<String, List<ReferenceType>>> components, int dims) {
			this.components = components;
			this.dims = dims;
		}
		public int dims() {
			return dims;
		}
		public void setDims(int dims) {
			this.dims = dims;
		}
		public List<Pair<String, List<ReferenceType>>> components() {
			return components;
		}
		public void setComponents(
				List<Pair<String, List<ReferenceType>>> components) {
			this.components = components;
		}
	}
	
	public static class WildcardType implements Type {
		private ReferenceType lowerBound;
		private ReferenceType upperBound;

		public WildcardType(ReferenceType lowerBound, ReferenceType upperBound) {
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}

		public ReferenceType upperBound() {
			return upperBound;
		}

		public ReferenceType lowerBound() {
			return lowerBound;
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
		private ReferenceType superclass;
		private List<ReferenceType> interfaces;
		private List<Declaration> declarations;		
				
		public Clazz(int modifiers, String name, ReferenceType superclass,
				List<ReferenceType> interfaces, List<Declaration> declarations) {
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

		public ReferenceType superclass() {
			return superclass;
		}
		
		public List<ReferenceType> interfaces() {
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
	
	/**
	 * This class stores all known information about a method, including it's
	 * full (possibly generic) type, its name, its modifiers (e.g. public/private
	 * etc), as well as the methods code.
	 * 
	 * @author djp
	 * 
	 */
	public static class Method extends Declaration {
		private int modifiers;
		private String name;
		private Type returnType;
		private List<Pair<String,Type>> parameters;		
		private List<ReferenceType> exceptions;
		private JavaFile.Block block;

		public Method(int modifiers, String name, Type returnType,
				List<Pair<String,Type>> parameters,List<ReferenceType> exceptions,
				JavaFile.Block block) {
			this.modifiers = modifiers;
			this.returnType = returnType;
			this.name = name;
			this.parameters = parameters;
			this.exceptions = exceptions;
			this.block = block;
		}
		
		public int modifiers() {
			return modifiers;
		}
		
		public String name() {
			return name;
		}

		public Type returnType() {
			return returnType;
		}
		
		public List<Pair<String,Type>> parameters() {
			return parameters;
		}
		
		public List<ReferenceType> exceptions() {
			return exceptions;
		}
		
		public Block block() {
			return block;
		}
	}
	
	/**
	 * A constructor is a special kind of method.
	 * 
	 * @author djp
	 * 
	 */
	public static class Constructor extends Method {
		public Constructor(int modifiers, String name,
				List<Pair<String, Type>> parameters, List<ReferenceType> exceptions,
				JavaFile.Block block) {
			super(modifiers, name, null, parameters, exceptions, block);
		}
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

	public static class Block implements Statement {
		private List<Statement> statements;
		public Block(List<Statement> statements) {
			
			this.statements = statements; 
		}
		
		public List<Statement> statements() {
			return statements;
		}
	}
	
	public static class SynchronisedBlock extends Block {
		private Expression expr;
		public SynchronisedBlock(Expression expr, List<Statement> statements) {
			super(statements);
			this.expr = expr; 
		}
		
		public Expression expr() {
			return expr;
		}
	}
	
	public static class CatchBlock extends Block {
		private ReferenceType type;
		private String variable;
		public CatchBlock(ReferenceType type, String variable, List<Statement> statements) {
			super(statements);
			this.type = type;
			this.variable = variable;
		}
		
		public ReferenceType type() {
			return type;
		}
		
		public String variable() {
			return variable;
		}
	}
	
	public static class TryCatchBlock extends Block {
		private List<CatchBlock> handlers;
		private Block finallyBlk;

		public TryCatchBlock(List<CatchBlock> handlers, Block finallyBlk,
				List<Statement> statements) {
			super(statements);
			this.handlers = handlers;
			this.finallyBlk = finallyBlk;
		}
		
		public List<CatchBlock> handlers() {
			return handlers;
		}
		
		public Block finaly() {
			return finallyBlk;
		}
	}
	public static class Assignment implements SimpleStatement, Expression {
		private Expression lhs,rhs;
		public Assignment(Expression lhs, Expression rhs) {
			
			this.lhs=lhs;
			this.rhs=rhs;
		}
		public Expression lhs() { return lhs; }
		public Expression rhs() { return rhs; }
	}
	
	public static class Return implements SimpleStatement {
		private Expression expr;
		public Return(Expression expr) {
			
			this.expr = expr;			
		}
		public Expression expr() { return expr; }		
	}
	
	public static class Throw implements SimpleStatement {
		private Expression expr;
		public Throw(Expression expr) {
			
			this.expr = expr;			
		}
		public Expression expr() { return expr; }		
	}
	
	public static class Assert implements SimpleStatement {
		private Expression expr;
		public Assert(Expression expr) {
			
			this.expr = expr;			
		}
		public Expression expr() { return expr; }		
	}
	
	public static class Break implements SimpleStatement {
		private String label;
		public Break(String label) {
			
			this.label = label;	
		}
		public String label() { return label; }		
	}
	
	public static class Label implements Statement {
		private String label;
		private Statement statement;
		public Label(String label, Statement statement) {
			
			this.label = label;
			this.statement = statement;
		}
		public String label() {
			return label;
		}
		public Statement statement() {
			return statement;
		}
	}
	
	public static class Continue implements SimpleStatement {
		private String label;
		public Continue(String label) {
			
			this.label = label;	
		}
		public String label() { return label; }		
	}
	
	public static class If implements Statement {
		private Expression condition;
		private Statement trueStatement;
		private Statement falseStatement;

		public If(Expression condition, Statement trueStatement,
				Statement falseStatement) {
			
			this.condition = condition;
			this.trueStatement = trueStatement;
			this.falseStatement = falseStatement;
		}

		public Expression condition() {
			return condition;
		}
		public Statement trueStatement() {
			return trueStatement;
		}
		public Statement falseStatement() {
			return falseStatement;
		}
	}
	
	public static class While implements Statement {
		private Expression condition;
		private Statement body;		

		public While(Expression condition, Statement body) {
			
			this.condition = condition;
			this.body = body;
		}

		public Expression condition() {
			return condition;
		}
		public Statement body() {
			return body;
		}
	}		
	
	public static class DoWhile implements Statement {
		private Expression condition;
		private Statement body;		

		public DoWhile(Expression condition, Statement body) {
			
			this.condition = condition;
			this.body = body;
		}

		public Expression condition() {
			return condition;
		}
		public Statement body() {
			return body;
		}
	}
	
	public static class For implements Statement {
		private Statement initialiser;
		private Expression condition;
		private Statement increment;
		private Statement body;		

		public For(Statement initialiser, Expression condition,
				Statement increment, Statement body) {
			
			this.initialiser = initialiser;
			this.condition = condition;
			this.increment = increment;
			this.body = body;
		}

		public Statement initialiser() {
			return initialiser;
		}
		public Expression condition() {
			return condition;
		}
		public Statement body() {
			return body;
		}
		public Statement increment() {
			return increment;
		}
	}	
	
	public static class ForEach implements Statement {
		private String var;
		private int modifiers; // for variable
		private Type type; // for variable
		private Expression source; 
		private Statement body;
		
		public ForEach(int modifiers, String var, Type type, Expression source, Statement body) {
			this.modifiers = modifiers;
			this.var = var;
			this.type = type;
			this.source = source;
			this.body = body;
		}
		
		/**
		 * Set the modifiers of the variable declared in the for-each statement.
		 * Use java.lang.reflect.Modifier for this.
		 * 
		 * @param type
		 */
		public void setModifiers(int modifiers) { this.modifiers = modifiers; }
		
		/**
		 * Get modifiers of this local variable
		 * 
		 * @return
		 */
		public int modifiers() { return modifiers; }	
		
		/**
		 * Get type of variable declared in for-each statement.
		 * 
		 * @return
		 */
		public Type type() {
			return type;
		}
		
		/**
		 * Get name of variable declared in for-each statement.
		 * 
		 * @return
		 */
		public String var() { 
			return var;
		}
		
		/**
		 * Get the source expression which corresponds to an array or collection
		 * which the for-each statement is going to iterate over.
		 * 
		 * @return
		 */
		public Expression source() {
			return source;
		}
		
		
		/**
		 * Get the body of the for-each statement. Maybe null if there is no
		 * body!
		 * 
		 * @return
		 */
		public Statement body() {
			return body;
		}
	}
	
	/**
     * A VarDef is a symbol table entry for a local variable. It can be thought
     * of as a declaration for that variable, including its type, modifiers,
     * name and whether or not it is a parameter to the method.
     * 
     * @author djp
     */
	public static class VarDef implements SimpleStatement {		
		private int modifiers;
		private List<Triple<String,Type,Expression> > definitions;
		
		public VarDef(int modifiers, List<Triple<String,Type,Expression> > definitions) {
			
			this.modifiers = modifiers;
			this.definitions = definitions;
		}
						
		/**
         * Set the modifiers of this local variable. Use
         * java.lang.reflect.Modifier for this.
         * 
         * @param type
         */
		public void setModifiers(int modifiers) { this.modifiers = modifiers; }
		
		/**
		 * Get modifiers of this local variable
		 * 
		 * @return
		 */
		public int modifiers() { return modifiers; }		
		
		public void setDefinitions(List<Triple<String,Type,Expression>> e) {
			definitions = e;
		}
		
		public List<Triple<String,Type,Expression> > definitions() {
			return definitions;
		}
	}
	
	public static class Case {
		private Expression condition;
		private List<Statement> statements;
		
		public Case(Expression condition, List<Statement> statements) {
			this.condition = condition;
			this.statements = statements;
		}
		
		public Expression condition() {
			return condition;
		}

		public List<Statement> statements() {
			return statements;
		}		
	}
	
	public static class DefaultCase extends Case {
		public DefaultCase(List<Statement> statements) {
			super(null,statements);
		}
	}
	
	public static class Switch implements Statement {
		private Expression condition;
		private List<Case> cases;
		
		public Switch(Expression condition, List<Case> cases) {
			this.condition = condition;
			this.cases = cases;
		}
		
		public Expression condition() {
			return condition;
		}
		
		public List<Case> cases() {
			return cases;
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
	public static interface Expression {
		
	}
	
	/**
     * An (unresolved) variable
     * 
     * @author djp
     * 
     */
	public static class Variable implements Expression {
		private String value;
		
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
	public static class Cast implements Expression {		
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
	public static class InstanceOf implements Expression {
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
	public static class UnOp implements Expression {
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
	public static class BinOp implements Expression {
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
				
		protected Expression lhs;
		protected Expression rhs;
		protected int op;				
		
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
	
	public static class TernOp implements Expression {
		
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
	public static class Invoke implements Expression, SimpleStatement {
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
	public static class New implements Expression, SimpleStatement {
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
	public static class Deref implements Expression {
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
	public static class ArrayIndex implements Expression {
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
	
	public static abstract class Value implements Expression {
		
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
		private String value;
		
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
		private ReferenceType classType;

		public ClassVal(ReferenceType type) {			
			this.classType = type;
		}
		
		public ReferenceType value() {
			return classType;
		}
	}
}
