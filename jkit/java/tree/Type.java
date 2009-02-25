package jkit.java.tree;

import java.util.*;

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
		public Primitive(Attribute... attributes) {
			super(attributes);			
		}
	}
	
	/**
     * The Void type is used to represent "void" types, found in method
     * declarations.
     * 
     * @author djp
     * 
     */
	public static class Void extends Primitive {		
		public Void(Attribute... attributes) {
			super(attributes);
		}
	}
	
	/**
	 * Represents the Java type "boolean"
	 * @author djp
	 *
	 */
	public static class Bool extends Primitive {		
		public Bool(Attribute... attributes) {
			super(attributes);
		}
	}
	

	/**
	 * Represents the Java type "byte"
	 * @author djp
	 *
	 */
	public static class Byte extends Primitive {
		public Byte(Attribute... attributes) {
			super(attributes);
		}
	}
	
	/**
	 * Represents the Java type "char"
	 * @author djp
	 *
	 */
	public static class Char extends Primitive {
		public Char(Attribute... attributes) {
			super(attributes);
		}
	}
	
	/**
	 * Represents the Java type "short"
	 * @author djp
	 *
	 */
	public static class Short extends Primitive {
		public Short(Attribute... attributes) {
			super(attributes);
		}
	}

	/**
	 * Represents the Java type "int"
	 * @author djp
	 *
	 */
	public static class Int extends Primitive {
		public Int(Attribute... attributes) {
			super(attributes);
		}
	}
	
	/**
	 * Represents the Java type "long"
	 * @author djp
	 *
	 */
	public static class Long extends Primitive {
		public Long(Attribute... attributes) {
			super(attributes);
		}
	}
	
	/**
	 * Represents the Java type "float"
	 * @author djp
	 *
	 */
	public static class Float extends Primitive {
		public Float(Attribute... attributes) {
			super(attributes);
		}
	}
	
	/**
	 * Represents the Java type "double"
	 * @author djp
	 *
	 */
	public static class Double extends Primitive {
		public Double(Attribute... attributes) {
			super(attributes);
		}
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
	public static class Array extends Reference {
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

		public Clazz(String str, Attribute... attributes) {
			super(attributes);
			this.components = new ArrayList<Pair<String, List<Type.Reference>>>();
			this.components.add(new Pair(str, new ArrayList()));
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

		public Wildcard(Reference lowerBound, Reference upperBound, Attribute... attributes) {
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
		private Reference lowerBound;

		public Variable(String variable, Reference lowerBound,
				Attribute... attributes) {
			super(attributes);
			this.variable = variable;
			this.lowerBound = lowerBound;
		}

		public String variable() {
			return variable;
		}

		public Reference lowerBound() {
			return lowerBound;
		}		
		
		public boolean equals(Object o) {
			if (o instanceof Variable) {
				Variable v = (Variable) o;
				return variable.equals(v.variable)
						&& lowerBound.equals(v.lowerBound);
			}
			return false;
		}		
	}
	
	/**
	 * An intersection type represents a (unknown) type which known to be a
	 * subtype of several types. For example, given types T1 and T2, then their
	 * intersection type is T1 & T2. The intersection type represents an object
	 * which is *both* an instance of T1 and an instance of T2. Thus, we always
	 * have that T1 :> T1 & T2 and T2 :> T1 & T2.
	 * 
	 * @author djp
	 */
	public static class Intersection extends Reference {
		private List<Type.Reference> bounds;
		
		public Intersection(List<Type.Reference> bounds,
				Attribute... attributes) {
			super(attributes);
			this.bounds = bounds;
		}
		
		public List<Type.Reference> bounds() {
			return bounds;
		}					
	}
}
