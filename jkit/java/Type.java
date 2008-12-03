package jkit.java;

import java.util.List;

import jkit.jkil.Attribute;
import jkit.jkil.SyntacticElement;
import jkit.jkil.SyntacticElementImpl;
import jkit.util.Pair;

/**
 * Represents types used in Java programs (e.g. int, String, Object[], etc).
 * <p>
 * JKit provides classes and methods for representing and manipulating Java
 * types (such <code>int</code>, <code>String[]</code> etc). The majority
 * of these can be found here. For example, the <code>Type.Int</code> class is
 * used to represent <code>int</code> types, whilst
 * <code>Type.Reference</code> represents general reference types, such as
 * <code>java.lang.String</code>.
 */
public interface Type {		
	/**
     * The Primitive type abstracts all the primitive types.
     */
	public interface Primitive extends Type {}
	
	/**
     * The Reference type abstracts all the reference types, including class
     * types, array types, variable and wildcard types.
     */
	public interface Reference extends Type {}
	
	/**
     * The Null type is a special type given to the null value. We require that
     * Null is a subtype of any Reference.
     */
	public static class Null extends SyntacticElementImpl implements Primitive {
		public Null(Attribute... attributes) {
			super(attributes);			
		}		
	}
	
	/**
	 * Represents the Java type "boolean"
	 * @author djp
	 *
	 */
	public static class Bool extends SyntacticElementImpl implements Primitive {
		public Bool(Attribute... attributes) {
			super(attributes);			
		}		
	}
	

	/**
	 * Represents the Java type "byte"
	 * @author djp
	 *
	 */
	public static class Byte extends SyntacticElementImpl implements Primitive {
		public Byte(Attribute... attributes) {
			super(attributes);			
		}		
	}
	
	/**
	 * Represents the Java type "char"
	 * @author djp
	 *
	 */
	public static class Char extends SyntacticElementImpl implements Primitive {
		public Char(Attribute... attributes) {
			super(attributes);			
		}		
	}
	
	/**
	 * Represents the Java type "short"
	 * @author djp
	 *
	 */
	public static class Short extends SyntacticElementImpl implements Primitive {
		public Short(Attribute... attributes) {
			super(attributes);			
		}		
	}

	/**
	 * Represents the Java type "int"
	 * @author djp
	 *
	 */
	public static class Int extends SyntacticElementImpl implements Primitive {
		public Int(Attribute... attributes) {
			super(attributes);			
		}		
	}
	
	/**
	 * Represents the Java type "long"
	 * @author djp
	 *
	 */
	public static class Long extends SyntacticElementImpl implements Primitive {
		public Long(Attribute... attributes) {
			super(attributes);			
		}		
	}
	
	/**
	 * Represents the Java type "float"
	 * @author djp
	 *
	 */
	public static class Float extends SyntacticElementImpl implements Primitive {
		public Float(Attribute... attributes) {
			super(attributes);			
		}		
	}
	
	/**
	 * Represents the Java type "double"
	 * @author djp
	 *
	 */
	public static class Double extends SyntacticElementImpl implements Primitive {
		public Double(Attribute... attributes) {
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
	public static class Array extends SyntacticElementImpl implements Reference {		
		private Type element;
		
		public Array(Type element, Attribute... attributes) {
			super(attributes);
			this.element = element;			
		}
		
		public Type element() {
			return element;
		}		
	}
	
	/**
     * This represents a reference to a class. E.g. java.lang.String
     * 
     * @author djp
     * 
     */
	public static class Clazz extends SyntacticElementImpl implements Reference {		
		private List<Pair<String, List<Type>>> components;
		public Clazz(List<Pair<String, List<Type>>> components,
				Attribute... attributes) {
			super(attributes);
			this.components = components;
		}		
		public List<Pair<String, List<Type>>> components() {
			return components;
		}
		public void setComponents(
				List<Pair<String, List<Type>>> components) {
			this.components = components;
		}
	}
	
	/**
     * This represents the special "?" type. As used, for example, in the
     * following method declaration:
     * 
     *  void printAll(Collection<? extends MyClass> { ... }
     * 
     * @author djp
     * 
     */
	public static class Wildcard extends SyntacticElementImpl implements Reference {
		private Type lowerBound;
		private Type upperBound;

		public Wildcard(Type lowerBound, Type upperBound,
				Attribute... attributes) {
			super(attributes);
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}

		public Type upperBound() {
			return upperBound;
		}

		public Type lowerBound() {
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
	public static class Variable extends SyntacticElementImpl implements Reference {
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
	}
	
	/**
	 * Represents the type of a method.  For example, the following method
	 * 
	 * void m(int x) { ... } has type "void(int)" 
	 * @author djp
	 *
	 */
	public static class Function extends SyntacticElementImpl implements Type {
		private final List<Type> parameters;
		private final Type returnType;
		private final List<Type> typeArgs;
		
		public Function(Type returnType, List<Type> parameters, List<Type> typeArgs) {
			this.returnType = returnType;
			this.parameters = parameters;
			this.typeArgs = typeArgs;
		}
		
		public Type returnType() { 
			return returnType;
		}
		
		public List<Type> parameterTypes() {
			return parameters;
		}
		
		public List<Type> typeArguments() {
			return typeArgs;
		}
	}	
}
