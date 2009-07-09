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
public interface Type extends Attribute, Comparable<Type> {
	
	/**
	 * <p>
	 * This method returns the list of generic variables used in this type. So,
	 * for example, suppose we have:
	 * </p>
	 * 
	 * <pre>
	 * T = java.lang.ArrayList&lt;? extends S&gt;
	 * </pre>
	 * 
	 * <p>
	 * Then, <code>T.usedVariables()=[S]</code>.
	 * </p>
	 * 
	 * @return
	 */
	public List<Type.Variable> usedVariables();
	
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
		
		public int compareTo(Type t) {
			if(t instanceof Type.Null) {
				return 0;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			return new ArrayList<Type.Variable>();
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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Null) {
				return 1;
			} else if (t instanceof Type.Void) {
				return 0;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			return new ArrayList<Type.Variable>();
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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Null || t instanceof Type.Void) {
				return 1;
			} else if (t instanceof Type.Bool) {
				return 0;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			return new ArrayList<Type.Variable>();
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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Null || t instanceof Type.Void
					|| t instanceof Type.Bool) {
				return 1;
			} else if (t instanceof Type.Byte) {
				return 0;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			return new ArrayList<Type.Variable>();
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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Char) {
				return 0;
			} else if (t instanceof Type.Null || t instanceof Type.Void
					|| t instanceof Type.Bool || t instanceof Type.Byte) {
				return 1;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			return new ArrayList<Type.Variable>();
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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Short) {
				return 0;
			} else if (t instanceof Type.Null || t instanceof Type.Void
					|| t instanceof Type.Bool || t instanceof Type.Byte
					|| t instanceof Type.Char) {
				return 1;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			return new ArrayList<Type.Variable>();
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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Int) {
				return 0;
			} else if (t instanceof Type.Null || t instanceof Type.Void
					|| t instanceof Type.Bool || t instanceof Type.Byte
					|| t instanceof Type.Char || t instanceof Type.Short) {
				return 1;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			return new ArrayList<Type.Variable>();
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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Long) {
				return 0;
			} else if (t instanceof Type.Null || t instanceof Type.Void
					|| t instanceof Type.Bool || t instanceof Type.Byte
					|| t instanceof Type.Char || t instanceof Type.Int) {
				return 1;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			return new ArrayList<Type.Variable>();
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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Float) {
				return 0;
			} else if (t instanceof Type.Null || t instanceof Type.Void
					|| t instanceof Type.Bool || t instanceof Type.Byte
					|| t instanceof Type.Char || t instanceof Type.Int
					|| t instanceof Type.Long) {
				return 1;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			return new ArrayList<Type.Variable>();
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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Double) {
				return 0;
			} else if (t instanceof Type.Null || t instanceof Type.Void
					|| t instanceof Type.Bool || t instanceof Type.Byte
					|| t instanceof Type.Char || t instanceof Type.Int
					|| t instanceof Type.Long || t instanceof Type.Float) {
				return 1;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			return new ArrayList<Type.Variable>();
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
		private final Type element;
		
		public Array(Type element) {
			if (element == null) {
				throw new IllegalArgumentException(
						"Supplied element type cannot be null.");
			}
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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Array) {
				return element.compareTo(((Type.Array) t).element());
			} else if (t instanceof Type.Primitive) {
				return 1;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			return element.usedVariables();
		}
	}
	
	/**
     * This represents a reference to a class. E.g. java.lang.String
     * 
     * @author djp
     * 
     */
	public static class Clazz implements Reference {		
		private final String pkg;
		private final List<Pair<String, List<Type.Reference>>> components;
		
		public Clazz(String pkg, List<Pair<String, List<Type.Reference>>> components) {
			if (components == null) {
				throw new IllegalArgumentException(
						"Supplied class components type cannot be null.");
			}
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
		
		public Pair<String, List<Type.Reference>> lastComponent() {
			return components.get(components.size()-1);
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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Clazz) {
				Type.Clazz tc = (Type.Clazz) t;
				int pct = pkg.compareTo(tc.pkg);
				if(pct != 0) { return pct; }
				if(components.size() < tc.components.size()) {
					return -1;
				} else if(components.size() == tc.components.size()) {
					return 1;
				}
				for(int i=0;i!=components.size();++i) {
					Pair<String,List<Type.Reference>> t1 = components.get(i);
					Pair<String,List<Type.Reference>> t2 = tc.components.get(i);
					int fct = t1.first().compareTo(t2.first());
					if(fct != 0) { return fct; }
					if(t1.second().size() < t2.second().size()) {
						return -1;
					} else if(t1.second().size() > t2.second().size()) {
						return 1;
					}
					for(int j=0;j!=t1.second().size();++j) {
						Type.Reference r1 = t1.second().get(j);
						Type.Reference r2 = t2.second().get(j);
						int rct = r1.compareTo(r2);
						if(rct != 0) { return rct; }
					}
				}
				return 0;
			} else if (t instanceof Type.Primitive || t instanceof Type.Array) {
				return 1;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			ArrayList<Type.Variable> ls = new ArrayList();
			for(Pair<String,List<Type.Reference>> p : components) {
				for(Type.Reference r : p.second()) {
					ls.addAll(r.usedVariables());
				}
			}
			return ls;
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
		private final Type.Reference lowerBound;
		private final Type.Reference upperBound;

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
				boolean lb;
				if(lowerBound == null) {
					lb = w.lowerBound == null;
				} else {
					lb = lowerBound.equals(w.lowerBound);
				}
				boolean ub;
				if(upperBound == null) {
					ub = w.upperBound == null;
				} else {
					ub = upperBound.equals(w.upperBound);
				}
				
				return lb && ub;
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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Wildcard) {
				Type.Wildcard tw = (Type.Wildcard) t;
				if(lowerBound == null && tw.lowerBound != null) {
					return -1;
				} else if(lowerBound != null && tw.lowerBound == null) {
					return 1;
				}
				if(upperBound == null && tw.upperBound != null) {
					return -1;
				} else if(upperBound != null && tw.upperBound == null) {
					return 1;
				} 
				if(lowerBound != null) {
					int lbct = lowerBound.compareTo(tw.lowerBound);
					if(lbct != 0) { return lbct; }
				}
				if(upperBound != null) {
					int lbct = upperBound.compareTo(tw.upperBound);
					if(lbct != 0) { return lbct; }
				}
				return 0;
			} else if (t instanceof Type.Primitive || t instanceof Type.Array
					|| t instanceof Type.Clazz) {
				return 1;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			ArrayList<Type.Variable> ls = new ArrayList();
			if(lowerBound != null) {
				ls.addAll(lowerBound.usedVariables());
			}
			if(upperBound != null) {
				ls.addAll(lowerBound.usedVariables());
			}
			return ls;
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
		private final String variable;
		private final Type.Reference lowerBound;

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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Variable) {
				Type.Variable tv = (Type.Variable) t;
				int vct = variable.compareTo(tv.variable);
				if(vct != 0) { return vct; }
				if(lowerBound == null && tv.lowerBound != null) {
					return -1;
				} else if(lowerBound != null && tv.lowerBound == null) {
					return 1;
				} else if (lowerBound != null) {
					return lowerBound.compareTo(tv.lowerBound);
				}
				
				return 0;
			} else if (t instanceof Type.Primitive || t instanceof Type.Array
					|| t instanceof Type.Clazz || t instanceof Type.Wildcard) {
				return 1;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			ArrayList<Type.Variable> ls = new ArrayList();
			ls.add(this);
			if(lowerBound != null) {
				ls.addAll(lowerBound.usedVariables());
			}			
			return ls;
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
		private final ArrayList<Type.Reference> bounds;
		
		public Intersection(List<Type.Reference> bounds,
				Attribute... attributes) {
			super(attributes);
			if (bounds == null) {
				throw new IllegalArgumentException(
						"Supplied bounds cannot be null.");
			} else if(bounds.size() <= 1) {
				throw new IllegalArgumentException(
				"Require more than one bound.");
			}
			this.bounds = new ArrayList<Type.Reference>(bounds);
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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Intersection) {
				Type.Intersection tv = (Type.Intersection) t;
				if(bounds.size() < tv.bounds.size()) { 
					return -1;
				} else if(bounds.size() > tv.bounds.size()) {
					return 1;
				} 
				for(int i=0;i!=bounds.size();++i) {
					Type.Reference r1 = bounds.get(i);
					Type.Reference r2 = tv.bounds.get(i);
					int rct = r1.compareTo(r2);
					if(rct != 0) { return rct; }
				}
				return 0;
			} else if (t instanceof Type.Reference) {
				return 1;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			ArrayList<Type.Variable> ls = new ArrayList();
			for(Type.Reference r : bounds) {
				ls.addAll(r.usedVariables());
			}
			return ls;
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
		
		public Function(Type returnType, Type... parameters) {
			if (returnType == null) {
				throw new IllegalArgumentException(
						"Supplied return type cannot be null.");
			}
			this.returnType = returnType;
			this.parameters = new ArrayList<Type>();
			this.typeArgs = new ArrayList();
			for(Type t : parameters) {
				this.parameters.add(t);
			}
		}
		
		public Function(Type returnType, List<Type> parameters) {
			if (returnType == null) {
				throw new IllegalArgumentException(
						"Supplied return type cannot be null.");
			}
			this.returnType = returnType;
			this.parameters = parameters;
			this.typeArgs = new ArrayList();
		}
		
		public Function(Type returnType, List<Type> parameters, List<Type.Variable> typeArgs) {
			if (returnType == null) {
				throw new IllegalArgumentException(
						"Supplied return type cannot be null.");
			}
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
		
		public int compareTo(Type t) {
			if (t instanceof Type.Function) {
				Type.Function tf = (Type.Function) t;
				if(parameters.size() < tf.parameters.size()) {
					return -1; 
				} else if(parameters.size() > tf.parameters.size()) {
					return 1; 
				} 
				for(int i=0;i!=parameters.size();++i) {
					Type p1 = parameters.get(i);
					Type p2 = tf.parameters.get(i);
					int pct = p1.compareTo(p2);
					if(pct != 0) { return pct; }
				}
				return returnType.compareTo(tf.returnType);
			} else if (t instanceof Type.Reference) {
				return 1;
			} else {
				return -1;
			}
		}
		
		public List<Type.Variable> usedVariables() {
			ArrayList<Type.Variable> ls = new ArrayList();
			ls.addAll(returnType.usedVariables());
			for(Type r : parameters) {
				ls.addAll(r.usedVariables());
			}
			ls.addAll(typeArgs);
			return ls;
		}
	}	
}
