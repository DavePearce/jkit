package whiley.ast.exprs;

import java.util.Map;
import whiley.ast.*;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;

public class Negate extends SyntacticElementImpl implements Expression {
	private Expression expr;
	
	public Negate(Expression e, Attribute... attributes) {
		super(attributes);
		this.expr = e;
	}
	
	public Expression getExpr() {
		return expr;
	}
    	
    public void bind(Map<String,Function> fmap) {
    	expr.bind(fmap);
    }
    
    public String toString() {
    	return "-" + expr.toString();
    }
}
