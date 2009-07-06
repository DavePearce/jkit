package whiley.ast.exprs;

import java.util.Map;

import whiley.ast.*;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;

public final class ListAccess extends SyntacticElementImpl implements Expression, LVal {
	private Expression source;
	private Expression index;
	
	public ListAccess(Expression source, Expression index, Attribute... attributes)  {
		super(attributes);
		this.source = source;
		this.index = index;
	}
	
	public Expression source() { return source; }
	public Expression index() { return index; }
	
    public void bind(Map<String,Function> fmap) {
    	source.bind(fmap);
    	index.bind(fmap);
    }
    
    public String toString() {
    	return source + "[" + index + "]";
    }
}
