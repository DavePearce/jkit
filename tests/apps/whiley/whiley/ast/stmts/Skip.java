package whiley.ast.stmts;

import java.util.*;

import whiley.ast.Function;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;

/**
 * This represents the "skip" statement, which is a "no-operation" or "nop" for
 * short. This statement doesn't actually do anything!
 */
public final class Skip extends SyntacticElementImpl implements Stmt {

	public Skip(Attribute... attributes) {
		super(attributes);
	}	
	
	public void bind(Map<String,Function> fmap) {}	
}
