package whiley.ast;

import java.util.*;

import whiley.ast.attrs.*;
import whiley.ast.exprs.Condition;
import whiley.ast.stmts.Stmt;
import whiley.ast.types.IntType;
import whiley.ast.types.Type;
import whiley.ast.types.VoidType;
import whiley.util.Pair;

public class Function extends SyntacticElementImpl {
	private String name;
	private Type returnType;
	private List<Pair<Type, String>> paramTypes;
	private Condition precondition;
	private Condition postcondition;
	private List<Stmt> statements;
	
	/**
	 * Construct an object representing a Whiley function.
	 * 
	 * @param name -
	 *            The name of the function.
	 * @param returnType -
	 *            The return type of this method
	 * @param paramTypes -
	 *            The list of parameter names and their types for this method
	 * @param pre -
	 *            The pre condition which must hold true on entry (maybe null)
	 * @param post -
	 *            The post condition which must hold true on exit (maybe null)
	 * @param statements -
	 *            The Statements making up the function body.
	 */
	public Function(String name, Type returnType,
			List<Pair<Type, String>> paramTypes, Condition pre, Condition post,
			List<Stmt> statements, Attribute... attributes) {
		super(attributes);
		this.name = name;
		this.returnType = returnType;
		this.paramTypes = paramTypes;
		this.precondition = pre;
		this.postcondition = post;
		this.statements = statements;
	}
	
	/**
	 * Return the name of this function.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Return the precondition of this function.
	 * 
	 * @return
	 */
	public Condition getPrecondition() {
		return precondition;
	}
	
	/**
	 * Return the postcondition of this function.
	 * 
	 * @return
	 */
	public Condition getPostcondition() {
		return postcondition;
	}
	
	/**
	 * Return the return Type of this function.
	 * 
	 * @return
	 */
	public Type getReturnType() {
		return returnType;
	}
	
	/**
	 * Return the parameter types for this function.
	 * 
	 * @return
	 */
	public List<Pair<Type,String>> getParameterTypes() {
		return paramTypes;
	}
	
	/**
	 * Return the list of statements which make up the body of this function.
	 */
	public List<Stmt> getStatements() {
		return statements;
	}
}
