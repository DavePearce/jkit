// This file is part of the Java Compiler Kit (JKit)
//
// The Java Compiler Kit is free software; you can 
// redistribute it and/or modify it under the terms of the 
// GNU General Public License as published by the Free Software 
// Foundation; either version 2 of the License, or (at your 
// option) any later version.
//
// The Java Compiler Kit is distributed in the hope
// that it will be useful, but WITHOUT ANY WARRANTY; without 
// even the implied warranty of MERCHANTABILITY or FITNESS FOR 
// A PARTICULAR PURPOSE.  See the GNU General Public License 
// for more details.
//
// You should have received a copy of the GNU General Public 
// License along with the Java Compiler Kit; if not, 
// write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA
//
// (C) David James Pearce, 2009. 

package jkit.jil.tree;

import java.util.*;

import jkit.compiler.SyntacticAttribute;
import jkit.compiler.SyntacticElement;
import jkit.compiler.SyntacticElementImpl;

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
	public static final Modifier ACC_PURE = new Pure();
	public static final Modifier ACC_VOLATILE = new Volatile();
	public static final Modifier ACC_STRICT = new StrictFP();
	public static final Modifier ACC_FINAL = new Final();
	public static final Modifier ACC_BRIDGE = new Bridge();

	public final static class Public extends SyntacticElementImpl implements Modifier {
		public Public(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}

	public final static class Protected extends SyntacticElementImpl implements
			Modifier {
		public Protected(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}

	public final static class Private extends SyntacticElementImpl implements
			Modifier {
		public Private(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}

	public final static class StrictFP extends SyntacticElementImpl implements
			Modifier {
		public StrictFP(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}

	public final static class Static extends SyntacticElementImpl implements Modifier {
		public Static(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}

	public final static class Abstract extends SyntacticElementImpl implements
			Modifier {
		public Abstract(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}

	public final static class Final extends SyntacticElementImpl implements Modifier {
		public Final(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}

	public final static class Native extends SyntacticElementImpl implements Modifier {
		public Native(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}

	public final static class Synchronized extends SyntacticElementImpl implements
			Modifier {
		public Synchronized(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}

	public final static class Transient extends SyntacticElementImpl implements
			Modifier {
		public Transient(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}

	public final static class Volatile extends SyntacticElementImpl implements
			Modifier {
		public Volatile(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}

	public final static class Interface extends SyntacticElementImpl implements
			Modifier {
		public Interface(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}

	public final static class Synthetic extends SyntacticElementImpl implements
			Modifier {
		public Synthetic(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}
	
	public final static class AnnotationT extends SyntacticElementImpl implements
			Modifier {
		public AnnotationT(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}
	
	public final static class Enum extends SyntacticElementImpl implements Modifier {
		public Enum(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}
	
	public final static class Super extends SyntacticElementImpl implements Modifier {
		public Super(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}
	
	public final static class Bridge extends SyntacticElementImpl implements Modifier {
		public Bridge(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}
	
	public final static class Pure extends SyntacticElementImpl implements Modifier {
		public Pure(SyntacticAttribute... attributes) {
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
		public VarArgs(SyntacticAttribute... attributes) {
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
		private Type.Clazz type;
		private List<JilExpr> arguments;

		public Annotation(Type.Clazz type, Collection<JilExpr> arguments,
				SyntacticAttribute... attributes) {
			super(attributes);
			this.type = type;
			this.arguments = new ArrayList<JilExpr>(arguments);
		}

		public Type.Clazz type() {
			return type;
		}

		public List<JilExpr> arguments() {
			return arguments;
		}
	}
}