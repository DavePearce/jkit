package whiley.ast.exprs;

import java.util.Map;

import whiley.ast.Function;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;

public class LengthOf extends SyntacticElementImpl implements Expression {
	private Expression expr;
	
	public LengthOf(Expression e, Attribute... attributes) {
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
    	return "|" + expr.toString() + "|";
    }
}
