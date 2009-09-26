package jkit.java.tree;

import java.util.List;

import jkit.compiler.SyntacticAttribute;
import jkit.compiler.SyntacticElementImpl;
import jkit.jil.tree.Modifier;
import jkit.java.tree.*;

/**
 * An annotation represents a user-defined modifier. For example,
 * "@deprecated" is a user-defined modifier, or annotation in Java
 * terminolgy.
 * 
 * @author djp
 * 
 */
public final class Annotation extends SyntacticElementImpl implements Modifier {
	private Type.Clazz type;
	private List<Expr> arguments;

	public Annotation(Type.Clazz type, List<Expr> arguments,
			SyntacticAttribute... attributes) {
		super(attributes);
		this.type = type;
		this.arguments = arguments;
	}

	public Type.Clazz type() {
		return type;
	}

	public List<Expr> arguments() {
		return arguments;
	}
}
