package whiley.ast.stmts;

import java.util.*;

import whiley.ast.Function;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;
import whiley.ast.exprs.Expression;

/**
 * This represents a "print x" statement.
 */
public final class Return extends SyntacticElementImpl implements Stmt {
	private final Expression expr;

	public Return(Expression expr, Attribute... attributes) { 
		super(attributes);
		this.expr = expr;
	}

	/**
	 * Get the expression that this output statement refers to.
	 */
	public Expression getExpression() { return expr; }
	
	public void bind(Map<String,Function> fmap) {
		expr.bind(fmap);
	}	
	
	public String toString() {
		return "return " + expr + ";";
	}
}
