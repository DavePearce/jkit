package whiley.ast.exprs;

import java.math.*;
import java.util.*;
import whiley.ast.*;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;
import whiley.util.BigRational;

/**
 * This class represents an integer number used in an expression.
 */
public class Constant extends SyntacticElementImpl implements Expression {
	private final Object constant;

	public Constant(Boolean number, Attribute... attributes) {
		super(attributes);
		this.constant = number;
	}
	
	public Constant(BigRational number, Attribute... attributes) {
		super(attributes);
		this.constant = number;
	}
	
	public Constant(BigInteger number, Attribute... attributes) {
		super(attributes);
		this.constant = number;
	}	
	
	/**
	 * Get the integer this object refers to
	 */
	public Object value() {
		return constant;
	}

	public void bind(Map<String,Function> fmap) {}
	
	public String toString() {		
		return constant.toString();		
	}
}
