package whiley.ast.stmts;

import java.util.*;
import java.io.*;

import whiley.ast.Function;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;

/**
 * This represents a "read x" statement.
 */
public final class Read extends SyntacticElementImpl implements Stmt {
	private final String var;

	public Read(String var, Attribute... attributes) { 
		super(attributes);
		this.var = var;
	}

	public String getVariable() { return var; }
	
	public void bind(Map<String,Function> fmap) {}	
}
