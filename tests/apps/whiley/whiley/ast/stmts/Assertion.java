package whiley.ast.stmts;

import java.util.*;

import whiley.ast.Function;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;
import whiley.ast.exprs.Condition;

/**
 * This represents a "print x" statement.
 */
public final class Assertion extends SyntacticElementImpl implements Stmt {
	private final Condition expr;

	public Assertion(Condition expr, Attribute... attributes) {
		super(attributes);
		this.expr = expr;
	}

	/**
	 * Get the expression that this output statement refers to.
	 */
	public Condition getExpression() { return expr; }
	
	public void bind(Map<String,Function> fmap) {
		expr.bind(fmap);
	}	
	
	public String toString() {
		return "assert " + expr + ";";
	}
}
