package jkit.jil;

import java.util.*;

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
public interface Type extends Attribute {
	
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
	public static class Null implements Primitive {		
		public String toString() {
			return "null";
		}
		
		public boolean equals(Object o) {
			return o instanceof Type.Null;
		}
	}
	 
	
	/**
     * The Void type is used to represent "void" types, found in method
     * declarations.
     * 
     * @author djp
     * 
     */
	public static class Void implements Primitive {		
		public String toString() {
			return "void";
		}
		
		public boolean equals(Object o) {
			return o instanceof Type.Void;
		}
	}
	/**
	 * Represents the Java type "boolean"
	 * @author djp
	 *
	 */
	public static class Bool implements Primitive {		
		public String toString() {
			return "boolean";
		}
		
		public boolean equals(Object o) {
			return o instanceof Type.Bool;
		}
	}
	

	/**
	 * Represents the Java type "byte"
	 * @author djp
	 *
	 */
	public static class Byte implements Primitive {
		public String toString() {
			return "byte";
		}
		
		public boolean equals(Object o) {
			return o instanceof Type.Byte;
		}
	}
	
	/**
	 * Represents the Java type "char"
	 * @author djp
	 *
	 */
	public static class Char implements Primitive {
		public String toString() {
			return "char";
		}
		
		public boolean equals(Object o) {
			return o instanceof Type.Char;
		}
	}
	
	/**
	 * Represents the Java type "short"
	 * @author djp
	 *
	 */
	public static class Short implements Primitive {
		public String toString() {
			return "short";
		}
		
		public boolean equals(Object o) {
			return o instanceof Type.Short;
		}
	}

	/**
	 * Represents the Java type "int"
	 * @author djp
	 *
	 */
	public static class Int implements Primitive {
		public String toString() {
			return "int";
		}
		
		public boolean equals(Object o) {
			return o instanceof Type.Int;
		}
	}
	
	/**
	 * Represents the Java type "long"
	 * @author djp
	 *
	 */
	public static class Long implements Primitive {
		public String toString() {
			return "long";
		}
		
		public boolean equals(Object o) {
			return o instanceof Type.Long;
		}
	}
	
	/**
	 * Represents the Java type "float"
	 * @author djp
	 *
	 */
	public static class Float implements Primitive {
		public String toString() {
			return "float";
		}
		
		public boolean equals(Object o) {
			return o instanceof Type.Float;
		}
	}
	
	/**
	 * Represents the Java type "double"
	 * @author djp
	 *
	 */
	public static class Double implements Primitive {
		public String toString() {
			return "double";
		}
		
		public boolean equals(Object o) {
			return o instanceof Type.Double;
		}
	}
	
	/**
     * The Array type captures array types! The elementType gives the types of
     * the elements held in the array. For example, in "int[]", the element type
     * is int.
     * 
     * @author djp
     */
	public static class Array implements Reference {		
		private Type element;
		
		public Array(Type element) {
			this.element = element;			
		}
		
		public Type element() {
			return element;
		}
		public String toString() {
			return element + "[]";
		}
		
		public boolean equals(Object o) {
			if(o instanceof Type.Array) {
				Type.Array a = (Type.Array) o;
				return element.equals(a.element);
			}
			return false;
		}
	}
	
	/**
     * This represents a reference to a class. E.g. java.lang.String
     * 
     * @author djp
     * 
     */
	public static class Clazz implements Reference {		
		private String pkg;
		private List<Pair<String, List<Type>>> components;
		
		public Clazz(List<Pair<String, List<Type>>> components) {
			this.pkg = "";
			this.components = components;
		}		
		
		public Clazz(String pkg, String clazz) {
			this.pkg = pkg;
			components = new ArrayList<Pair<String,List<Type>>>();
			components.add(new Pair(clazz,new ArrayList<Type>()));
		}
		
		public List<Pair<String, List<Type>>> components() {
			return components;
		}
		
		public void setComponents(
				List<Pair<String, List<Type>>> components) {
			this.components = components;
		}
		
		/**
         * Return the package. If no package, then the value is simply "",
         * rather than null.
         * 
         * @return
         */
		public String pkg() {
			return pkg;
		}
		
		public String toString() {
			String r = pkg;			
			boolean firstTime = pkg.length() == 0;
			for (Pair<String, List<Type>> n : components) {
				if (!firstTime) {
					r += ".";
				}
				firstTime = false;
				r += n.first();
				List<Type> typeArgs = n.second();
				if (typeArgs != null && typeArgs.size() > 0) {
					r += "<";
					boolean innerFirstTime = true;
					for (Type t : typeArgs) {
						if (!innerFirstTime) {
							r += ", ";
						}
						innerFirstTime = false;
						r += t;
					}
					r += ">";
				}
			}
			return r;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Type.Clazz) {
				Type.Clazz c = (Type.Clazz) o;
				
				return pkg.equals(c.pkg) &&
					components.equals(c.components);
			}
			return false;
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
	public static class Wildcard implements Reference {
		private Type lowerBound;
		private Type upperBound;

		public Wildcard(Type lowerBound, Type upperBound) {			
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}

		public Type upperBound() {
			return upperBound;
		}

		public Type lowerBound() {
			return lowerBound;
		}
		
		public boolean equals(Object o) {
			if (o instanceof Wildcard) {
				Wildcard w = (Wildcard) o;
				return lowerBound.equals(w.lowerBound)
						&& upperBound.equals(w.upperBound);
			}
			return false;
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
		
		public boolean equals(Object o) {
			if (o instanceof Variable) {
				Variable v = (Variable) o;
				return variable.equals(v.variable)
						&& lowerBounds.equals(v.lowerBounds);
			}
			return false;
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
		
		public boolean equals(Object o) {
			if (o instanceof Function) {
				Function f = (Function) o;
				return returnType.equals(f.returnType)
						&& parameters.equals(f.parameters)
						&& typeArgs.equals(f.typeArgs);
			}
			return false;
		}
	}	
}
