package whiley.ast.attrs;

import whiley.ast.types.*;

public class TypeAttr implements Attribute {
	private Type type;
	
	public TypeAttr(Type t) {
		type = t;
	}
	
	public Type type() {
		return type;
	}
}
