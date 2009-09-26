package jkit.java.tree;

import java.util.List;

import jkit.compiler.SyntacticAttribute;
import jkit.compiler.SyntacticElementImpl;
import jkit.jil.tree.Modifier;

/**
 * An annotation represents a user-defined modifier. For example,
 * "@deprecated" is a user-defined modifier, or annotation in Java
 * terminolgy.
 * 
 * @author djp
 * 
 */
public final class Annotation extends SyntacticElementImpl implements Modifier {
	private String name;
	private List<Expr> arguments;

	public Annotation(String name, List<Expr> arguments,
			SyntacticAttribute... attributes) {
		super(attributes);
		this.name = name;
		this.arguments = arguments;
	}

	public String name() {
		return name;
	}

	public List<Expr> arguments() {
		return arguments;
	}
}
