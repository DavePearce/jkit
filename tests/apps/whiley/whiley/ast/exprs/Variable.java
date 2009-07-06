package whiley.ast.exprs;

import java.util.*;
import whiley.ast.*;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;


/**
 * This class represents a variable used in an expression.
 */
public class Variable extends SyntacticElementImpl implements Condition, LVal {
	private final String var;

	public Variable(String var, Attribute... attributes) {
		super(attributes);
		this.var = var;
	}

	/**
	 * Get the variable name this object refers to
	 */
	public String getVariable() {
		return var;
	}
	
	public void bind(Map<String,Function> fmap) {}
	
	public String toString() { return var; }
}
