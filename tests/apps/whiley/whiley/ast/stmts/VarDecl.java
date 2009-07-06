package whiley.ast.stmts;

import java.util.Map;

import whiley.ast.Function;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;
import whiley.ast.types.*;

/**
 * This class represents a variable declaration. All variables in whiley must be
 * declared before they are used.
 * 
 * @author djp
 */
public final class VarDecl extends SyntacticElementImpl implements Stmt {
	protected Type type;
	protected String name;
	
	public VarDecl(Type type, String name, Attribute... attributes) {
		super(attributes);
		this.type = type;
		this.name = name;
	}
	
	public Type type() { return type; }
	public String name() { return name; }	
	
	public void bind(Map<String,Function> fmap) {}	
	
	public String toString() {
		return type + " " + name + ";";
	}
}
