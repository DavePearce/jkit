package jkit.java;

import java.util.*;

import jkit.jkil.SyntacticElement;
import jkit.jkil.SyntacticElementImpl;
import jkit.jkil.Attribute;
import jkit.util.*;

public class JavaFile {
	private String pkg;
	private List<Pair<Boolean,String> > imports;
	private List<Declaration> declarations; 
	
	public JavaFile(String pkg, List<Pair<Boolean, String> > imports, List<Declaration> declarations) {
		this.pkg = pkg;
		this.imports = imports;
		this.declarations = declarations;
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
	public List<Pair<Boolean,String> > imports() { 
		return imports;
	}
	
	/**
	 * Get the list of class declarations in this file.
	 * 
	 * @return
	 */
	public List<Declaration> declarations() { 
		return declarations;
	}
			
	/**
     * Represents the class of imperative statements allowed.
     * 
     * @author djp
     * 
     */		
	public static interface Statement extends SyntacticElement {		
	}

	public static interface SimpleStatement extends Statement {		
	}
	
	
	// ====================================================
	// MODIFIERS
	// ====================================================
	
	public interface Modifier extends SyntacticElement {
		
	}
	
	public static class BaseModifier extends SyntacticElementImpl implements Modifier {
		private int modifier;
		public BaseModifier(int modifier, Attribute... attributes) {
			super(attributes);
			this.modifier = modifier;
		}
		public int modifier() {
			return modifier;
		}
	}
	
	public static class Annotation extends SyntacticElementImpl  implements Modifier {
		private String name;
		private List<Expression> arguments;
		
		public Annotation(String name, List<Expression> arguments,
				Attribute... attributes) {
			super(attributes);
			this.name = name;
			this.arguments = arguments;
		}
		public String name() {
			return name;
		}
		public List<Expression> arguments() {
			return arguments;
		}
	}
	
	// ====================================================
	// DECLARATIONS
	// ====================================================
	
	public static interface Declaration extends SyntacticElement {
		
	}
	
	public static class Clazz extends SyntacticElementImpl  implements Declaration, Statement {
		private List<Modifier> modifiers;
		private String name;
		private List<Type.Variable> typeParameters;
		private Type.Clazz superclass;
		private List<Type.Clazz> interfaces;
		private List<Declaration> declarations;		
				
		public Clazz(List<Modifier> modifiers, String name,
				List<Type.Variable> typeParameters, Type.Clazz superclass,
				List<Type.Clazz> interfaces, List<Declaration> declarations,
				Attribute... attributes) {
			super(attributes);
			this.modifiers = modifiers;
			this.name = name;
			this.typeParameters = typeParameters;
			this.superclass = superclass;
			this.interfaces = interfaces;
			this.declarations = declarations;			
		}
		
		public List<Modifier> modifiers() {
			return modifiers;
		}
		
		public String name() {
			return name;
		}

		public List<Type.Variable> typeParameters() {
			return typeParameters;
		}
		
		public Type.Clazz superclass() {
			return superclass;
		}
		
		public List<Type.Clazz> interfaces() {
			return interfaces;
		}
		
		public List<Declaration> declarations() { 
			return declarations;
		}		
	}
	
	public static class Interface extends Clazz {
		public Interface(List<Modifier> modifiers, String name,
				List<Type.Variable> typeParameters, Type.Clazz superclass,
				List<Type.Clazz> interfaces, List<Declaration> declarations,
				Attribute... attributes) {
			super(modifiers, name, typeParameters, superclass, interfaces,
					declarations, attributes);
		}
	}
	
	public static class Enum extends Clazz {
		private List<EnumConstant> constants;

		public Enum(List<Modifier> modifiers, String name,
				List<Type.Clazz> interfaces, List<EnumConstant> constants,
				List<Declaration> declarations, Attribute... attributes) {
			super(modifiers, name, new ArrayList<Type.Variable>(), null,
					interfaces, declarations, attributes);
			this.constants = constants;
		}

		public List<EnumConstant> constants() {
			return constants;
		}
	}
	
	public static class EnumConstant extends SyntacticElementImpl{
		private String name;
		private List<Expression> arguments;
		private List<Declaration> declarations;
		
		public EnumConstant(String name, List<Expression> arguments,
				List<Declaration> declarations, Attribute... attributes) {
			super(attributes);
			this.name = name;
			this.arguments = arguments;
			this.declarations = declarations;
		}
		
		public String name() {
			return name;
		}
		
		public List<Expression> arguments() {
			return arguments;
		}
		
		public List<Declaration> declarations() {
			return declarations;
		}
	}
	
	public static class AnnotationInterface extends SyntacticElementImpl  implements Declaration {
		private List<Modifier> modifiers;
		private String name;
		private List<Triple<Type, String, JavaFile.Value>> methods; 
						
		public AnnotationInterface(List<Modifier> modifiers, String name,
				List<Triple<Type, String, JavaFile.Value>> methods,
				Attribute... attributes) {
			super(attributes);
			this.modifiers = modifiers;
			this.name = name;
			this.methods = methods;
		}
		
		public List<Modifier> modifiers() {
			return modifiers;
		}
		public String name() {
			return name;
		}
		public List<Triple<Type, String, JavaFile.Value>> methods() {
			return methods;
		}
	}
	
	/**
	 * This class stores all known information about a method, including it's
	 * full (possibly generic) type, its name, its modifiers (e.g. public/private
	 * etc), as well as the methods code.
	 * 
	 * @author djp
	 * 
	 */
	public static class Method extends SyntacticElementImpl  implements Declaration {
		private List<Modifier> modifiers;
		private String name;
		private Type returnType;
		private List<Triple<String,List<Modifier>,Type>> parameters;
		private boolean varargs;
		private List<Type.Variable> typeParameters;
		private List<Type.Clazz> exceptions;
		private JavaFile.Block block;

		public Method(List<Modifier> modifiers, String name, Type returnType,
				List<Triple<String, List<Modifier>, Type>> parameters,
				boolean varargs, List<Type.Variable> typeParameters,
				List<Type.Clazz> exceptions, JavaFile.Block block,
				Attribute... attributes) {
			super(attributes);
			this.modifiers = modifiers;
			this.returnType = returnType;
			this.name = name;
			this.parameters = parameters;
			this.varargs = varargs;
			this.typeParameters = typeParameters;
			this.exceptions = exceptions;
			this.block = block;
		}
		
		public List<Modifier> modifiers() {
			return modifiers;
		}
		
		public String name() {
			return name;
		}

		public Type returnType() {
			return returnType;
		}
		
		/**
         * List of triples (n,m,t), where n is the parameter name, m are the
         * modifiers and t is the type.
         * 
         * @return
         */
		public List<Triple<String,List<Modifier>,Type>> parameters() {
			return parameters;
		}
		
		/**
         * Indicates whether or not this method accept a variable-length
         * argument list.
         * 
         * @return
         */
		public boolean varargs() {
			return varargs;
		}
		
		public List<Type.Variable> typeParameters() {
			return typeParameters;
		}
		
		public List<Type.Clazz> exceptions() {
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
		public Constructor(List<Modifier> modifiers, String name,
				List<Triple<String, List<Modifier>, Type>> parameters, boolean varargs,
				List<Type.Variable> typeParameters,
				List<Type.Clazz> exceptions,
				JavaFile.Block block, Attribute... attributes) {			
			super(modifiers, name, null, parameters, varargs, typeParameters,
					exceptions, block,attributes);
		}
	}
	
	public static class Field extends SyntacticElementImpl  implements Declaration {
		private List<Modifier> modifiers;
		private String name;
		private Type type;
		private Expression initialiser;
		
		public Field(List<Modifier> modifiers, String name, Type type,
				Expression initialiser, Attribute... attributes) {
			super(attributes);
			this.modifiers = modifiers;
			this.name = name;
			this.type = type;
			this.initialiser = initialiser;
		}
		
		public List<Modifier> modifiers() {
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
	}
	
	public static class InitialiserBlock extends Block implements Declaration {
		public InitialiserBlock(List<Statement> statements, Attribute... attributes) {
			super(statements,attributes);
		}
	}
	public static class StaticInitialiserBlock extends Block implements Declaration {
		public StaticInitialiserBlock(List<Statement> statements, Attribute... attributes) {
			super(statements,attributes);
		}
	}
	
	// ====================================================
	// STATEMENTS
	// ====================================================

	public static class Block extends SyntacticElementImpl  implements Statement {
		private List<Statement> statements;
		
		public Block(List<Statement> statements, Attribute... attributes) {
			super(attributes);
			this.statements = statements;
		}
		
		public List<Statement> statements() {
			return statements;
		}
	}
	
	public static class SynchronisedBlock extends Block {
		private Expression expr;
		
		public SynchronisedBlock(Expression expr, List<Statement> statements,
				Attribute... attributes) {
			super(statements, attributes);
			this.expr = expr;
		}
		
		public Expression expr() {
			return expr;
		}
	}
	
	public static class CatchBlock extends Block {
		private Type.Clazz type;
		private String variable;
		
		public CatchBlock(Type.Clazz type, String variable,
				List<Statement> statements, Attribute... attributes) {
			super(statements, attributes);
			this.type = type;
			this.variable = variable;
		}
		
		public Type.Clazz type() {
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
				List<Statement> statements, Attribute... attributes) {
			super(statements, attributes);
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
	
	public static class Assignment extends SyntacticElementImpl  implements SimpleStatement, Expression {
		private Expression lhs,rhs;
		public Assignment(Expression lhs, Expression rhs, Attribute... attributes) {
			super(attributes);
			this.lhs=lhs;
			this.rhs=rhs;
		}
		public Expression lhs() { return lhs; }
		public Expression rhs() { return rhs; }
	}
	
	public static class Return extends SyntacticElementImpl  implements SimpleStatement {
		private Expression expr;
		public Return(Expression expr, Attribute... attributes) {
			super(attributes);
			this.expr = expr;			
		}
		public Expression expr() { return expr; }		
	}
	
	public static class Throw extends SyntacticElementImpl  implements SimpleStatement {
		private Expression expr;
		public Throw(Expression expr, Attribute... attributes) {
			super(attributes);
			this.expr = expr;			
		}
		public Expression expr() { return expr; }		
	}
	
	public static class Assert extends SyntacticElementImpl  implements SimpleStatement {
		private Expression expr;
		public Assert(Expression expr, Attribute... attributes) {
			super(attributes);
			this.expr = expr;			
		}
		public Expression expr() { return expr; }		
	}
	
	public static class Break extends SyntacticElementImpl  implements SimpleStatement {
		private String label;
		public Break(String label, Attribute... attributes) {
			super(attributes);
			this.label = label;	
		}
		public String label() { return label; }		
	}
	
	public static class Label extends SyntacticElementImpl  implements Statement {
		private String label;
		private Statement statement;
		public Label(String label, Statement statement, Attribute... attributes) {
			super(attributes);
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
	
	public static class Continue extends SyntacticElementImpl  implements SimpleStatement {
		private String label;
		public Continue(String label, Attribute... attributes) {
			super(attributes);
			this.label = label;	
		}
		public String label() { return label; }		
	}
	
	public static class If extends SyntacticElementImpl  implements Statement {
		private Expression condition;
		private Statement trueStatement;
		private Statement falseStatement;

		public If(Expression condition, Statement trueStatement,
				Statement falseStatement, Attribute... attributes) {
			super(attributes);			
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
	
	public static class While extends SyntacticElementImpl  implements Statement {
		private Expression condition;
		private Statement body;		

		public While(Expression condition, Statement body, Attribute... attributes) {
			super(attributes);
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
	
	public static class DoWhile extends SyntacticElementImpl  implements Statement {
		private Expression condition;
		private Statement body;		

		public DoWhile(Expression condition, Statement body, Attribute... attributes) {
			super(attributes);
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
	
	public static class For extends SyntacticElementImpl  implements Statement {
		private Statement initialiser;
		private Expression condition;
		private Statement increment;
		private Statement body;		

		public For(Statement initialiser, Expression condition,
				Statement increment, Statement body, Attribute... attributes) {
			super(attributes);
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
	
	public static class ForEach extends SyntacticElementImpl  implements Statement {
		private String var;
		private List<Modifier> modifiers; // for variable
		private Type type; // for variable
		private Expression source; 
		private Statement body;
		
		public ForEach(List<Modifier> modifiers, String var, Type type,
				Expression source, Statement body, Attribute... attributes) {
			super(attributes);
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
		public void setModifiers(List<Modifier> modifiers) { this.modifiers = modifiers; }
		
		/**
		 * Get modifiers of this local variable
		 * 
		 * @return
		 */
		public List<Modifier> modifiers() { return modifiers; }	
		
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
	public static class VarDef extends SyntacticElementImpl  implements SimpleStatement {		
		private List<Modifier> modifiers;
		private Type type;
		private List<Triple<String,Integer,Expression> > definitions;
		
		public VarDef(List<Modifier> modifiers, Type type,
				List<Triple<String, Integer, Expression>> definitions,
				Attribute... attributes) {
			super(attributes);
			this.modifiers = modifiers;
			this.definitions = definitions;
			this.type = type;
		}
						
		/**
         * Set the modifiers of this local variable. Use
         * java.lang.reflect.Modifier for this.
         * 
         * @param type
         */
		public void setModifiers(List<Modifier> modifiers) { this.modifiers = modifiers; }
		
		/**
		 * Get modifiers of this local variable
		 * 
		 * @return
		 */
		public List<Modifier> modifiers() { return modifiers; }		
		
		public List<Triple<String,Integer,Expression> > definitions() {
			return definitions;
		}
		
		public Type type() { return type; }
	}
	
	public static class Case extends SyntacticElementImpl {
		private Expression condition;
		private List<Statement> statements;
		
		public Case(Expression condition, List<Statement> statements,
				Attribute... attributes) {
			super(attributes);
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
		public DefaultCase(List<Statement> statements, Attribute... attributes) {
			super(null, statements, attributes);
		}
	}
	
	public static class Switch extends SyntacticElementImpl
			implements
				Statement {
		private Expression condition;
		private List<Case> cases;

		public Switch(Expression condition, List<Case> cases,
				Attribute... attributes) {
			super(attributes);
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
	public static interface Expression extends SyntacticElement {
		
	}
	
	/**
     * An (unresolved) variable
     * 
     * @author djp
     * 
     */
	public static class Variable extends SyntacticElementImpl  implements Expression {
		private String value;
		
		public Variable(String value, Attribute... attributes) {
			super(attributes);
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
	public static class Cast extends SyntacticElementImpl  implements Expression {		
		protected Expression expr;
		protected Type type;
		
		public Cast(Type type,  Expression expr, Attribute... attributes) {
			super(attributes);
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
	public static class InstanceOf extends SyntacticElementImpl  implements Expression {
		protected Expression lhs;		
		protected Type rhs;		
		
		public InstanceOf(Expression lhs, Type rhs, Attribute... attributes) {
			super(attributes);
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
	public static class UnOp extends SyntacticElementImpl  implements Expression {
		public static final int NOT = 0;
		public static final int INV = 1;
		public static final int NEG = 2;
		public static final int PREINC = 3;
		public static final int PREDEC = 4;
		public static final int POSTINC = 5;
		public static final int POSTDEC = 6;
				
		protected final Expression expr;		
		protected final int op;
		
		public UnOp(int op, Expression expr, Attribute... attributes) {
			super(attributes);
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
	public static class BinOp extends SyntacticElementImpl  implements Expression {
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
		
		public BinOp(int op, Expression lhs, Expression rhs, Attribute... attributes) {
			super(attributes);
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
	
	public static class TernOp extends SyntacticElementImpl  implements Expression {
		
		protected Expression cond;
		protected Expression toption;
		protected Expression foption;
		
		public TernOp(Expression con, Expression tOption, Expression fOption, Attribute... attributes) {		
			super(attributes);
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
	public static class Invoke extends SyntacticElementImpl  implements Expression, SimpleStatement {
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
				List<Type> typeParameters, Attribute... attributes) {			
			super(attributes);
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
	public static class New extends SyntacticElementImpl  implements Expression, SimpleStatement {
		private Type type;
		private Expression context;
		private List<Expression> parameters;
		private List<Declaration> declarations;

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
         *            Inner();
         * @param parameters -
         *            The parameters (if any) supplied to the constructor.
         * @param declarations -
         *            the list of field/method/class declarations contained in
         *            this statement. These arise only when constructing a new
         *            anonymous class.
         */
		public New(Type type, Expression context, List<Expression> parameters,
				List<Declaration> declarations, Attribute... attributes) {
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
		
		public List<Expression> parameters() {
			return parameters;
		}

		public void setParameters(List<Expression> parameters) {
			this.parameters = parameters;
		}
		
		public List<Declaration> declarations() {
			return declarations;
		}
		
		public void setDeclarations(List<Declaration> declarations) {
			this.declarations = declarations;
		}
		
		public Expression context() {
			return context;
		}
		
		public void setContext(Expression context) {
			this.context = context;
		}
	}
	
	/**
     * Represents the act of derefencing a field.
     * 
     * @author djp
     * 
     */
	public static class Deref extends SyntacticElementImpl
			implements
				Expression {
		private Expression target;
		private String name;

		public Deref(Expression lhs, String rhs, Attribute... attributes) {
			super(attributes);
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
	public static class ArrayIndex extends SyntacticElementImpl
			implements
				Expression {
		private Expression array;
		private Expression idx;

		public ArrayIndex(Expression array, Expression idx,
				Attribute... attributes) {
			super(attributes);
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
	
	public static interface Value extends Expression {
		
	}
		
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
	public static class BoolVal extends Number {
		public BoolVal(boolean value, Attribute... attributes) {
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
		public CharVal(char value, Attribute... attributes) {
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
	public static class ByteVal extends Number {
		public ByteVal(byte value, Attribute... attributes) {
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
	public static class ShortVal extends Number {
		public ShortVal(short value, Attribute... attributes) {
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
	public static class IntVal extends Number {
		public IntVal(int value, Attribute... attributes) {
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
	public static class LongVal extends SyntacticElementImpl implements Value {
		private long value;
		
		public LongVal(long value, Attribute... attributes) {
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
	public static class FloatVal extends SyntacticElementImpl implements Value {
		private float value;
		
		public FloatVal(float value, Attribute... attributes) {
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
	public static class DoubleVal extends SyntacticElementImpl implements Value {
		private double value;
		
		public DoubleVal(double value, Attribute... attributes) {
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
	public static class StringVal extends SyntacticElementImpl implements Value {
		private String value;
		
		public StringVal(String value, Attribute... attributes) {
			super(attributes);
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
	public static class NullVal extends SyntacticElementImpl implements Value {
		public NullVal(Attribute... attributes) {
			super(attributes);
		}
	}
			
	/**
     * An array constant (used for array initialisers only).
     * 
     * @author djp
     * 
     */
	public static class ArrayVal extends SyntacticElementImpl implements Value {
		private List<Expression> values;
		
		public ArrayVal(List<Expression> values, Attribute... attributes) {
			super(attributes);
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
	 */
	public static class TypedArrayVal extends ArrayVal {
		private Type type;

		public TypedArrayVal(Type type, List<Expression> values,
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
	public static class ClassVal extends SyntacticElementImpl implements Value {
		private Type.Clazz type;

		public ClassVal(Type.Clazz type, Attribute... attributes) {
			super(attributes);
			this.type = type;
		}

		public Type.Clazz value() {
			return type;
		}
	}
}
