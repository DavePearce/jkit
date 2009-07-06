package whiley.ast.exprs;

import java.util.*;

import whiley.ast.*;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;

public class Or extends SyntacticElementImpl implements Condition, BinOp {
	private final Condition lhs;
	private final Condition rhs;

	public Or(Condition lhs, Condition rhs, Attribute... attributes) {
		super(attributes);
		this.lhs = lhs;
		this.rhs = rhs;
	}
		
	public Condition getLeftExpr() {
		return lhs;
	}
	
	public Condition getRightExpr() {
		return rhs;
	}
	
	public void bind(Map<String,Function> fmap) {
		lhs.bind(fmap);
		rhs.bind(fmap);		
	}
	
	public String toString() {
		return "(" + lhs + ") || (" + rhs + ")";		
	}
}
