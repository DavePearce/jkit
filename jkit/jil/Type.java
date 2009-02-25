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
	public static class Null implements Reference {		
		public String toString() {
			return "null";
		}
		
		public boolean equals(Object o) {
			return o instanceof Type.Null;
		}
		
		public int hashCode() {
			return 0;
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
		
		public int hashCode() {
			return 1;
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
		
		public int hashCode() {
			return 2;
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
		
		public int hashCode() {
			return 3;
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
		
		public int hashCode() {
			return 4;
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
		
		public int hashCode() {
			return 5;
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
		
		public int hashCode() {
			return 6;
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
		
		public int hashCode() {
			return 7;
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
		
		public int hashCode() {
			return 8;
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
		
		public int hashCode() {
			return 9;
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
		
		public int hashCode() {
			return 1 + element.hashCode();
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
		private List<Pair<String, List<Type.Reference>>> components;
		
		public Clazz(String pkg, List<Pair<String, List<Type.Reference>>> components) {
			this.pkg = pkg;
			this.components = components;
		}		
		
		public Clazz(String pkg, String clazz) {
			this.pkg = pkg;
			components = new ArrayList<Pair<String,List<Type.Reference>>>();
			components.add(new Pair(clazz,new ArrayList<Type.Reference>()));
		}
		
		public List<Pair<String, List<Type.Reference>>> components() {
			return components;
		}
		
		public void setComponents(
				List<Pair<String, List<Type.Reference>>> components) {
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
			for (Pair<String, List<Type.Reference>> n : components) {
				if (!firstTime) {
					r += ".";
				}
				firstTime = false;
				r += n.first();
				List<Type.Reference> typeArgs = n.second();
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
		
		public int hashCode() {
			int hc = 0;
			for (Pair<String, List<Type.Reference>> n : components) {
				hc ^= n.first().hashCode();
			}
			return hc;
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
		private Type.Reference lowerBound;
		private Type.Reference upperBound;

		public Wildcard(Type.Reference lowerBound, Type.Reference upperBound) {			
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
		
		public boolean equals(Object o) {
			if (o instanceof Wildcard) {
				Wildcard w = (Wildcard) o;
				return lowerBound.equals(w.lowerBound)
						&& upperBound.equals(w.upperBound);
			}
			return false;
		}
		
		public String toString() {
			String r = "?";
			if(lowerBound != null) {
				r += " extends " + lowerBound;							
			}
			if(upperBound != null) {
				r += " super " + upperBound;							
			}
			return r;
		}
		
		public int hashCode() {
			int hc = 0;
			if(lowerBound != null) {
				hc ^= lowerBound.hashCode();
			}
			if(upperBound != null) {
				hc ^= upperBound.hashCode();
			}
			return hc;
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
		private Type.Reference lowerBound;

		public Variable(String variable, Type.Reference lowerBound,
				Attribute... attributes) {
			super(attributes);
			this.variable = variable;
			this.lowerBound = lowerBound;
		}

		public String variable() {
			return variable;
		}

		public Type.Reference lowerBound() {
			return lowerBound;
		}		
		
		public boolean equals(Object o) {			
			if (o instanceof Variable) {
				Variable v = (Variable) o;
				return variable.equals(v.variable)
						&& lowerBound == v.lowerBound
						&& (lowerBound == null || lowerBound
								.equals(v.lowerBound));
			}
			return false;
		}
		
		public String toString() {
			if(lowerBound == null) {
				return variable;
			} else {					
				return variable + " extends " + lowerBound;				
			}
		}
		
		public int hashCode() {
			return variable.hashCode();
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
	public static class Intersection extends SyntacticElementImpl implements Reference {
		private List<Type.Reference> bounds;
		
		public Intersection(List<Type.Reference> bounds,
				Attribute... attributes) {
			super(attributes);
			this.bounds = bounds;
		}
		
		public List<Type.Reference> bounds() {
			return bounds;
		}
		
		public String toString() {
			String r = "";
			if(bounds.size() > 1) { r += "("; }
			boolean firstTime = true;
			for(Type.Reference b : bounds) {
				if(!firstTime) { r += " & "; }
				firstTime = false;
				r += b.toString();
			}
			if(bounds.size() > 1) { r += ")"; }
			return r;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Intersection) {
				Intersection t = (Intersection) o;
				if(t.bounds.size() == bounds.size()) {
					for(int i=0;i!=bounds.size();++i) {
						if(!t.bounds.get(i).equals(bounds.get(i))) {
							return false;
						}
					}
					return true;
				}
			}
			return false;
		}
		
		public int hashCode() {
			int hc = 0;
			for(Type.Reference r : bounds) {
				hc ^= r.hashCode();
			}
			return hc;
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
		private final List<Type.Variable> typeArgs;
		
		public Function(Type returnType, List<Type> parameters, List<Type.Variable> typeArgs) {
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
		
		public List<Type.Variable> typeArguments() {
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
		
		public String toString() {
			String r="";
			boolean firstTime;
			if(typeArgs.size() > 0) {
				r += "<";
				firstTime=true;
				for(Type.Variable v : typeArgs) {
					if(!firstTime) {
						r += ", ";						
					}
					firstTime=false;
					r += v;
				}
				r += "> ";
			}
			r += returnType;
			r += " ("; 
			
			firstTime=true;
			for(Type t : parameters) {
				if(!firstTime) {
					r += ", ";						
				}
				firstTime=false;
				r += t;
			}
			r+= ")";
			return r;
		}
		
		public int hashCode() {
			int hc = 0;
			for(Type t : parameters) {
				hc ^= t.hashCode();
			}
			return hc;
		}
	}	
}
