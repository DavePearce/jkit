package jkit.jil;

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
	
	/**
	 * A base modifier represents a standard modifier. See
	 * java.lang.reflect.Modifier for a list of the base modifiers.
	 * 
	 * @author djp
	 */
	public static class Base extends SyntacticElementImpl implements Modifier {
		private int modifier;
		
		public Base(int modifier, Attribute... attributes) {
			super(attributes);
			this.modifier = modifier;
		}
		
		public int modifier() {
			return modifier;
		}
	}	
	
	/**
	 * An annotation represents a user-defined modifier. For example,
	 * "@deprecated" is a user-defined modifier, or annotation in Java terminolgy.
	 * 
	 * @author djp
	 * 
	 */
	public static class Annotation extends SyntacticElementImpl  implements Modifier {
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