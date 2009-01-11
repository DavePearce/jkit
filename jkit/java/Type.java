package jkit.java;

import java.util.List;
import jkit.jil.Attribute;
import jkit.jil.SyntacticElementImpl;
import jkit.util.Pair;

/**
 * A an instance of java.Type is a (potentially incomplete) type declaration.
 * Consider the following:
 * 
 * <pre>
 * public void f(String x, java.util.Vector y) {
 * 
 * }
 * </pre>
 * 
 * We have two type declarations in the parameter list of this method. The first
 * is unqualified, whilst the second is not. A type declaration does not
 * represent a fully qualified type; instead, the type resolution process is
 * responsible for annoting a type declaration with the fully qualified type it
 * represents.
 * 
 * The primary aim of these classes are to capture the information extracted
 * from the source file by the parser. The key is that further processing is
 * required to turn these types into proper, fully-qualified types.
 * 
 * @author djp
 * 
 */
public class Type extends SyntacticElementImpl {		
	
	public Type(Attribute... attributes) {
		super(attributes);
	}
	
	/**
     * The Primitive type abstracts all the primitive types.
     */
	public static class Primitive extends Type {
		private String type;
		
		public Primitive(String type, Attribute... attributes) {
			super(attributes);
			this.type = type;
		}
		
		public String type() { return type; }
	}
	
	/**
     * The Reference type abstracts all the reference types, including class
     * types, array types, variable and wildcard types.
     */
	public static class Reference extends Type {
		public Reference(Attribute... attributes) {
			super(attributes);
		}
	}
	
	/**
     * The Array type captures array types! The elementType gives the types of
     * the elements held in the array. For example, in "int[]", the element type
     * is int.
     * 
     * @author djp
     */
	public static class Array extends Type {
		private Type element;
		public Array(Type element, Attribute... attributes) {
			super(attributes);
			this.element = element;
		}
		public Type element() { return element; }
	}
	
	/**
     * This represents a reference to a class. E.g. java.lang.String
     * 
     * @author djp
     * 
     */
	public static class Clazz extends Reference {
		private List<Pair<String, List<Type.Reference>>> components;

		public Clazz(List<Pair<String, List<Type.Reference>>> components,
				Attribute... attributes) {
			super(attributes);
			this.components = components;
		}

		public List<Pair<String, List<Type.Reference>>> components() {
			return components;
		}

		public void setComponents(
				List<Pair<String, List<Type.Reference>>> components) {
			this.components = components;
		}
	}
	
	/**
     * This represents the special "?" type. As used, for example, in the
     * following method declaration:
     * 
     * void printAll(Collection<? extends MyClass> { ... }
     * 
     * @author djp
     * 
     */
	public static class Wildcard extends Reference {
		private Reference lowerBound;
		private Reference upperBound;

		public Wildcard(Reference lowerBound, Reference upperBound,Attribute... attributes) {
			super(attributes);
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}
		
		/**
         * Return the upper bound of this wildcard. This will be null if there
         * is none.
         * 
         * @return
         */
		public Type.Reference upperBound() {
			return upperBound;
		}

		/**
         * Return the lower bound of this wildcard. This will be null if there
         * is none.
         * 
         * @return
         */
		public Type.Reference lowerBound() {
			return lowerBound;
		}
	}
	
	/**
     * Represents a Generic type variable. For example, the T in class ArrayList<T> {
     * ... }
     * 
     * @author djp
     * 
     */
	public static class Variable extends Reference {
		private String variable;
		private List<Type> lowerBounds;

		public Variable(String variable, List<Type> lowerBounds,
				Attribute... attributes) {
			super(attributes);
			this.variable = variable;
			this.lowerBounds = lowerBounds;
		}

		public String variable() {
			return variable;
		}

		public List<Type> lowerBounds() {
			return lowerBounds;
		}		
		
		public boolean equals(Object o) {
			if (o instanceof Variable) {
				Variable v = (Variable) o;
				return variable.equals(v.variable)
						&& lowerBounds.equals(v.lowerBounds);
			}
			return false;
		}		
	}
}
