package jkit.jil.tree;

import java.util.List;

import jkit.java.tree.Expr;

/**
 * A modifier represents a flag (e.g. public/final/static) which can be used in
 * a variety of places, including on classes, methods and variable definitions.
 * 
 * @author djp
 * 
 */
public interface Modifier extends SyntacticElement {

	// Constants are provided to same memory in the common case.
	public static final Modifier ACC_PUBLIC = new Public();
	public static final Modifier ACC_PRIVATE = new Private();
	public static final Modifier ACC_PROTECTED = new Protected();
	public static final Modifier ACC_ABSTRACT = new Abstract();
	public static final Modifier ACC_NATIVE = new Native();
	public static final Modifier ACC_SYNCHRONIZED = new Synchronized();
	public static final Modifier ACC_SUPER = new Super();
	public static final Modifier ACC_INTERFACE = new Interface();
	public static final Modifier ACC_SYNTHETIC = new Synthetic();
	public static final Modifier ACC_ANNOTATION = new AnnotationT();
	public static final Modifier ACC_ENUM = new Enum();
	public static final Modifier ACC_TRANSIENT = new Transient();
	public static final Modifier ACC_STATIC = new Static();
	public static final Modifier ACC_VARARGS = new VarArgs();
	public static final Modifier ACC_VOLATILE = new Volatile();
	public static final Modifier ACC_STRICT = new StrictFP();
	public static final Modifier ACC_FINAL = new Final();
	public static final Modifier ACC_BRIDGE = new Bridge();

	public final static class Public extends SyntacticElementImpl implements Modifier {
		public Public(Attribute... attributes) {
			super(attributes);
		}
	}

	public final static class Protected extends SyntacticElementImpl implements
			Modifier {
		public Protected(Attribute... attributes) {
			super(attributes);
		}
	}

	public final static class Private extends SyntacticElementImpl implements
			Modifier {
		public Private(Attribute... attributes) {
			super(attributes);
		}
	}

	public final static class StrictFP extends SyntacticElementImpl implements
			Modifier {
		public StrictFP(Attribute... attributes) {
			super(attributes);
		}
	}

	public final static class Static extends SyntacticElementImpl implements Modifier {
		public Static(Attribute... attributes) {
			super(attributes);
		}
	}

	public final static class Abstract extends SyntacticElementImpl implements
			Modifier {
		public Abstract(Attribute... attributes) {
			super(attributes);
		}
	}

	public final static class Final extends SyntacticElementImpl implements Modifier {
		public Final(Attribute... attributes) {
			super(attributes);
		}
	}

	public final static class Native extends SyntacticElementImpl implements Modifier {
		public Native(Attribute... attributes) {
			super(attributes);
		}
	}

	public final static class Synchronized extends SyntacticElementImpl implements
			Modifier {
		public Synchronized(Attribute... attributes) {
			super(attributes);
		}
	}

	public final static class Transient extends SyntacticElementImpl implements
			Modifier {
		public Transient(Attribute... attributes) {
			super(attributes);
		}
	}

	public final static class Volatile extends SyntacticElementImpl implements
			Modifier {
		public Volatile(Attribute... attributes) {
			super(attributes);
		}
	}

	public final static class Interface extends SyntacticElementImpl implements
			Modifier {
		public Interface(Attribute... attributes) {
			super(attributes);
		}
	}

	public final static class Synthetic extends SyntacticElementImpl implements
			Modifier {
		public Synthetic(Attribute... attributes) {
			super(attributes);
		}
	}
	
	public final static class AnnotationT extends SyntacticElementImpl implements
			Modifier {
		public AnnotationT(Attribute... attributes) {
			super(attributes);
		}
	}
	
	public final static class Enum extends SyntacticElementImpl implements Modifier {
		public Enum(Attribute... attributes) {
			super(attributes);
		}
	}
	
	public final static class Super extends SyntacticElementImpl implements Modifier {
		public Super(Attribute... attributes) {
			super(attributes);
		}
	}
	
	public final static class Bridge extends SyntacticElementImpl implements Modifier {
		public Bridge(Attribute... attributes) {
			super(attributes);
		}
	}
	
	/**
	 * A varargs modifier is used to indicate that a method has variable-length
	 * arity. In the Java ClassFile format, this is written as ACC_TRANSIENT,
	 * although it's simpler for us to distinguish these things properly.
	 */
	public final static class VarArgs extends SyntacticElementImpl implements
			Modifier {
		public VarArgs(Attribute... attributes) {
			super(attributes);
		}
	}

	/**
	 * An annotation represents a user-defined modifier. For example,
	 * "@deprecated" is a user-defined modifier, or annotation in Java
	 * terminolgy.
	 * 
	 * @author djp
	 * 
	 */
	public final static class Annotation extends SyntacticElementImpl implements
			Modifier {
		private String name;
		private List<Expr> arguments;

		public Annotation(String name, List<Expr> arguments,
				Attribute... attributes) {
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
}