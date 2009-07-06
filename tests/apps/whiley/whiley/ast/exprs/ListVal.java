package whiley.ast.exprs;

import java.util.*;

import whiley.ast.Function;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;

public class ListVal extends SyntacticElementImpl implements Expression {
	private final ArrayList<Expression> values;

	public ListVal(List<Expression> value, Attribute... attributes) {
		super(attributes);
		this.values = new ArrayList<Expression>(value);
	}

	public List<Expression> getValues() {
		return values;
	}

	public void bind(Map<String,Function> fmap) {
		for(Expression e : values) {
			e.bind(fmap);
		}
	}
	
	
	public String toString() { 
		String r = "[";
		boolean firstTime=true;
		for(Expression e : values) {
			if(!firstTime) {
				r += ", ";
			}
			firstTime=false;
			r += e.toString();
		}
		return r + "]";
	}
}
