package jkit.error;

import jkit.java.stages.TypeSystem;
import jkit.java.tree.Expr;
import jkit.jil.tree.Type;
import jkit.compiler.ClassLoader;

public class TypeMismatchException extends JKitException {

	private final Expr found;
	private final Type expected;
	private final ClassLoader loader;
	private final TypeSystem types;

	public Expr found() {
		return found;
	}

	public Type expected() {
		return expected;
	}

	public ClassLoader loader() {
		return loader;
	}

	public TypeSystem types() {
		return types;
	}

	public TypeMismatchException(Expr f, Type e, ClassLoader l, TypeSystem t) {
		found = f;
		expected = e;
		loader = l;
		types = t;
	}

	private static final long serialVersionUID = 1L;

}
