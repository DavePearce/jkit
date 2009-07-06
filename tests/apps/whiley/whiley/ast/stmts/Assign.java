package whiley.ast.stmts;

import java.util.*;

import whiley.ast.Function;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;
import whiley.ast.exprs.*;

public final class Assign extends SyntacticElementImpl implements Stmt {
	private final LVal lhs;
	private final Expression rhs;

	public Assign(LVal lhs, Expression rhs, Attribute... attributes) {
		super(attributes);
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public LVal lhs() { return lhs; }
	public Expression rhs() { return rhs; }	
	
	public void bind(Map<String,Function> fmap) {
		lhs.bind(fmap);
		rhs.bind(fmap);
	}	
	
	public String toString() {
		return lhs + " = " + rhs + ";";
	}
}
