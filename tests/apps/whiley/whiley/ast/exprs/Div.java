package whiley.ast.exprs;

import java.util.*;
import whiley.ast.*;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;

/**
 * This class represents an "div(ide)" of two expressions.
 */
public class Div extends SyntacticElementImpl implements Expression, BinOp {
	private final Expression lhs;
	private final Expression rhs;

	public Div(Expression lhs, Expression rhs, Attribute... attributes) {
		super(attributes);
		this.lhs = lhs; 
		this.rhs = rhs; 
	}

	/**
	 * Get the subexpression on the left-hand side of this expression.
	 * For example, in '1/2', '1' is on the left-hand side.
	 */
	public Expression getLeftExpr() { return lhs; } 

	/**
	 * Get the subexpression on the right-hand side of this
	 * expression.  For example, in '1/2', '2' is on the right-hand
	 * side.
	 */
	public Expression getRightExpr() { return rhs; } 
	
	public void bind(Map<String,Function> fmap) {
		lhs.bind(fmap);
		rhs.bind(fmap);
	}
	
	public String toString() {
		String l = lhs.toString();
		String r = rhs.toString();
		return l + "/" + r;
	}
}
