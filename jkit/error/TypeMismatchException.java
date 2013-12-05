package jkit.error;

import jkit.java.stages.TypeSystem;
import jkit.java.tree.Expr;
import jkit.jil.tree.Type;
import jkit.compiler.ClassLoader;

public class TypeMismatchException extends Exception {

	private final Expr found;			//The found expression
	private final Type expected;		//The required/expected expression type
	private final ClassLoader loader;	//The classloader

	public Expr found() {
		return found;
	}

	public Type expected() {
		return expected;
	}

	public ClassLoader loader() {
		return loader;
	}

	public TypeMismatchException(Expr f, Type e, ClassLoader l, TypeSystem t) {
		found = f;
		expected = e;
		loader = l;
	}

	private static final long serialVersionUID = 1L;

}
