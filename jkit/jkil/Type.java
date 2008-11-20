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
// (C) David James Pearce, 2007. 

package jkit.jkil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jkit.core.ClassTable;
import jkit.util.Pair;

/**
 * Represents types used in Java programs (e.g. int, String, Object[], etc).
 * <p>
 * JKit provides classes and methods for representing and manipulating Java
 * types (such <code>int</code>, <code>String[]</code> etc). The majority
 * of these can be found here. For example, the <code>Type.Int</code> class is
 * used to represent <code>int</code> types, whilst
 * <code>Type.Reference</code> represents general reference types, such as
 * <code>java.lang.String</code>. Methods are also provided for determining
 * whether one type is a subtype of another, finding the least upper bound of
 * two types.
 * </p>
 * <p>
 * For efficiency, the <code>Type</code> class provides a static interface for
 * creating <code>Type</code> instances (doing this enables the <a
 * href="http://en.wikipedia.org/wiki/Flyweight_pattern"><i>flyweight pattern</i></a>).
 * For example, to create a <code>Type</code> object representing the Java
 * type <code>int</code>, you simply call <code>Type.intType()</code>. Or,
 * to create an array of <code>boolean</code>s you would call
 * <code>Type.arrayType(Type.booleanType())</code>. We now outline the main
 * <code>Type</code> classes available:
 * </p>
 *  
 * <p>
 * The following code is provided as an example to illustrate the usage:
 * </p>
 * 
 * <pre>
 * import jkit.core.Type;
 * 
 * class Test {
 * 	public static void main(String[] args) {
 * 		Type.Int it = Type.intType();
 * 		Type.Array at = Type.arrayType(Type.booleanType());
 * 		Type.Reference rt = Type.referenceType(&quot;java.lang&quot;, &quot;String&quot;);
 * 
 * 		System.out.println(&quot;Types: &quot; + it + &quot;, &quot; + at + &quot;, &quot; + rt);
 * 	}
 * }
 * </pre>
 * 
 * <b>NOTE:</b> instances of Type are immutable and any extensions must respect
 * this requirement.
 * 
 * <p>
 * See the <a href="http://www.mcs.vuw.ac.nz/~djp/jkit/manual.html#types">JKit
 * Reference Manual</a> for more information.
 * </p>
 * 
 * @author djp
 * 
 */
public abstract class Type {
	/**
	 * Holds the type elements (e.g. @NonNull) attached to this type.
	 */
	final protected TypeElement[] elements;

	public Type(TypeElement[] es) {
		assert es != null;
		// the following line was added for debugging purposes
		assert es.length == 2 || this instanceof Void
				|| this instanceof Function;

		elements = es;
	}

	/**
     * This is the Least Upper Bound operator. For types, this corresponds to
     * the set union operation (hence the name). This operation MUST have the
     * following properties:
     * 
     * 1) If result.equals(this) then result == this. 2) else if
     * result.equals(t) then result == t
     * 
     * @return
     */
	public abstract Type union(Type t);

	/**
     * This method determines whether this type is a superset or is equal to the
     * parameter t.
     * 
     * @param t
     * @return
     */
	public abstract boolean supsetEqOf(Type t);

	/**
	 * <p>
	 * This operation checks whether two types which are in the "element"
	 * position are subtypes. In most cases, the answer is no unless they are
	 * actually equal. For example, when evaluating supsetEqOf(<code>Number []</code>,<code>Integer
	 * []</code>)
	 * the call supsetEqOfElem(<code>Number</code>,<code>Integer</code>)
	 * will be recursively called (and must return false in this case).
	 * </p>
	 * 
	 * <p>
	 * But, wildcards complicate the issue and motivate the need for this
	 * method. For example, supsetEqOf(<code>Vector&lt;?&gt;</code>,<code>Vector&lt;String&gt;</code>)
	 * must hold, therefore <code>supsetEqOfElem(?,String)</code> must also.
	 * </p>
	 */
	public boolean supsetEqOfElem(Type t) {
		return equals(t);
	}

	/**
     * This operation simply substitute all occurances of type variables with
     * their matches in the environment. Type variables not present in the
     * environment are left as is.
     * 
     * @return
     */
	public abstract Type substitute(Map<String, Type> environment);

	/**
     * This operation builds a substitution map for use with substitute(). It
     * accepts a type which must have the same shape and should have concrete
     * types at the places where this type has variables.
     * 
     * @return indicates whether operation was successful or not
     */
	public abstract void bind(Type t, Map<String, Type> environment);

	/**
	 * Return the list of unbound generic type variables in this type. For
	 * example, in <code>java.util.ArrayList&lt;T&gt;</code>, the type
	 * variable <code>T</code> is unbound.
	 * 
	 * @return
	 */
	public Set<Variable> freeVariables() { return new HashSet<Variable>(); }
	
	/**
     * This method returns a more convenient string representation, which
     * excludes full package information. It's handy for user output, since it's
     * less verbose.
     * 
     * @return
     */
	public String toShortString() {
		return toString();
	}
	
	public int hashCode() {
		return Arrays.hashCode(elements);		
	}
	
	/**
     * Get the type elements (e.g.
     * 
     * @NonNull) attached to this type.
     * @return
     */
	public TypeElement[] elements() { return elements; }

	// ====================================================
	// ================ Type Implementations ==============
	// ====================================================

	private static Void voidT = new Void();
	private static Null nullT = null;

	/**
     * The void type is used to represent void, or empty types.
     */
	public static class Void extends Type {
		Void() {
			super(new TypeElement[0]);
		}
		public Type union(Type _t) {
			return _t;
		}		
		public boolean supsetEqOf(Type t) {
			return t instanceof Void;
		}
		public String toString() {
			return "void";
		}
		public boolean equals(Object o) {
			return o instanceof Void;
		}
		
		public int hashCode() { return 0; }
		
		public Type substitute(Map<String, Type> environment) {
			return this;
		}
		public void bind(Type t, Map<String, Type> environment) {
		}
	}

	/**
     * The Null type is a special type given to the null value. We require that
     * Null is a subtype of any Reference and any Array.
     */
	public static class Null extends Type {
		Null(TypeElement... es) {
			super(es);
		}
		public Type union(Type t) {
			if (t instanceof Void) {
				return this;
			}
			if (t instanceof Primitive) {
				return new Any(union(elements, t.elements));
			}
			if (t instanceof Null) {
				return this;
			} else
				return t.union(this);
		}				
		public boolean supsetEqOf(Type t) {
			return t instanceof Null || t instanceof Void;
		}
		public String toString() {
			return "Null";
		}		
		public boolean equals(Object o) {
			return o instanceof Null;
		}		
		
		public Type substitute(Map<String, Type> environment) {
			return this;
		}
		public void bind(Type t, Map<String, Type> environment) {
		}
	}

	/**
     * The Any type is a special type that subsumes all types. We require that
     * Any is a supertype of all other types.
     * 
     * @author djp
     */
	public static class Any extends Type {
		Any(TypeElement[] es) {
			super(es);
		}

		public Type union(Type _t) {
			if (_t instanceof Void) {
				return this;
			}
			TypeElement es[] = union(this.elements, _t.elements);
			if (es == elements) {
				return this;
			} else {
				return new Any(es);
			}
		}		

		public boolean supsetEqOf(Type t) {
			if (t instanceof Void) {
				return true;
			} // Void has small elements[]
			return supsetEqOf(elements, t.elements);
		}

		public String toString() {
			String r = toString(elements);
			if (r.length() > 0) {
				return r + " *";
			}
			return "*";
		}		
		public boolean equals(Object o) {
			if (o instanceof Any) {
				return Arrays.equals(elements, ((Any) o).elements);
			} else {
				return false;
			}
		}
		
		public Type substitute(Map<String, Type> environment) {
			return this;
		}
		public void bind(Type t, Map<String, Type> environment) {
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
		final private Type elementType;

		public Array(Type et, TypeElement... es) {
			super(es);
			elementType = et;
		}

		public Type elementType() {
			return elementType;
		}

		public Type union(Type t) {
			if (t instanceof Void) {
				return this;
			}
			if (t instanceof Any) {
				return t.union(this);
			}
			TypeElement es[] = union(this.elements, t.elements);
			if (t instanceof Primitive || t instanceof Function) {
				return new Any(es);
			}
			if (t instanceof Reference) {
				return referenceType("java.lang","Object",es);
			}
			if (t instanceof Null) {
				if (es == elements) {
					return this;
				}
				return new Array(elementType, es);
			}
			Array a = (Array) t;
			if (!elementType.equals(a.elementType)) {
				return referenceType("java.lang","Object",es);
			} else {
				// now, check if anything has changed!
				if (es == elements) {
					return this;
				}
				return new Array(elementType, es);
			}
		}
		
		public boolean equals(Object o) {
			if (o instanceof Array) {
				Array a = (Array) o;
				return elementType.equals(a.elementType)
						&& Arrays.equals(elements, a.elements);
			}
			return false;
		}

		public int hashCode() {
			return Arrays.hashCode(elements) ^ elementType.hashCode();
		}
		
		public boolean supsetEqOf(Type t) {
			// The following check follows Java's broken sub-typing for Arrays.
			if (t instanceof Array
					&& elementType.supsetEqOf(((Array) t).elementType)) {
				return supsetEqOf(elements, t.elements);
			} else if (t instanceof Void || t instanceof Null) {
				return true;
			}
			return false;
		}

		public Type substitute(Map<String, Type> environment) {
			Type et = elementType.substitute(environment);
			if (et == elementType) {
				return this;
			} else {
				return new Array(et, elements);
			}
		}

		public void bind(Type t, Map<String, Type> environment) {
			if (t instanceof Array) {
				elementType.bind(((Array) t).elementType, environment);
			}
		}
		
		public Set<Variable> freeVariables() { 
			return elementType.freeVariables(); 
		}
		
		public String toString() {
			String r = toString(elements);
			if (r.length() == 0) {
				return elementType + "[]";
			} else {
				return elementType + " " + r + " []";
			}
		}

		public String toShortString() {
			String r = toString(elements);
			if (r.length() == 0) {
				return elementType.toShortString() + "[]";
			} else {
				return elementType.toShortString() + " " + r + " []";
			}
		}
	}

	static final byte booleanT = 0;
	static final byte byteT = 1;
	static final byte shortT = 2;
	static final byte charT = 3;
	static final byte intT = 4;
	static final byte longT = 5;
	static final byte floatT = 6;
	static final byte doubleT = 7;

	/**
     * The Primitive type abstracts all the primitive types.
     */
	public static class Primitive extends Type {
		private final byte type;

		/**
		 * Computes join of two primitive types, t1 and t2.
		 * 
		 * JLS states that subtyping between primitives looks like this:
		 * double > float 
		 * float > long
		 * long > int
		 * int > char 
		 * int > short 
		 * short > byte
		 * 
		 */
		public static Type join(Primitive t1, Primitive t2) {
			if (t1.type > t2.type) {
				return join(t2, t1);
			}
			
			if (t1.type == t2.type) return t1;
			
			switch (t1.type) {
			case byteT: return join(Type.shortType(), t2);
			case shortT: return join(Type.intType(), t2);
			case charT: return join(Type.intType(), t2);
			case intT: return join(Type.longType(), t2);
			case longT: return join(Type.floatType(), t2);
			case floatT: return join(Type.doubleType(), t2);
			default: return Type.voidT;
			}
		}
		
		public Primitive(byte t, TypeElement[] es) {
			super(es);
			type = t;
		}

		public final boolean equals(Object o) {
			if (o instanceof Primitive) {
				Primitive p = (Primitive) o;
				return p.type == type && Arrays.equals(elements, p.elements);
			}
			return false;
		}

		public int hashCode() {
			return Arrays.hashCode(elements) ^ type;
		}
		
		public final Type union(Type t) {
			if (t instanceof Void) {
				return this;
			}
			if (t instanceof Any) {
				return t.union(this);
			}
			TypeElement[] es = union(this.elements, t.elements);
			if (!(t instanceof Primitive)) {
				return new Any(es);
			}
			if(t instanceof Primitive){
				Primitive p = (Primitive) t;
				if (compareAccuracy(p) < 0) {
					return this;
				}
				else {
					return p;
				}
			}
			if (es == elements) {
				return this;
			}
			return new Primitive(type, es);
		}
		
		private int compareAccuracy(Primitive t) {
			return t.type - type;
		}
				
		public final int comparePrecidence(Primitive o) {
			return o.type - this.type;
		}

		private static Type.Reference[] autoBoxingTypes = {
			Type.referenceType("java.lang","Boolean"),
			Type.referenceType("java.lang","Byte"),
			Type.referenceType("java.lang","Short"),
			Type.referenceType("java.lang","Character"),
			Type.referenceType("java.lang","Integer"),
			Type.referenceType("java.lang","Long"),
			Type.referenceType("java.lang","Float"),
			Type.referenceType("java.lang","Double")
		};
		
		public final boolean supsetEqOf(Type t) {
			if (t instanceof Void) {
				return true;
			}
			
			if (t instanceof Reference) {
				// need to check for autoboxing here ...
				Type.Reference r = (Type.Reference) t;
				if (r.pkg().equals("java.lang") && r.classes().length == 1) {
					if(type == 0) {
						return autoBoxingTypes[type].supsetEqOf(r); 
					} else {
						for(int i=type;i>=0;--i) {				
							if(autoBoxingTypes[i].supsetEqOf(r)) {
								return supsetEqOf(elements, t.elements);
							}
						}
						return false;
					}
				}
				return false;
			}
						
			if (t instanceof Any || !(t instanceof Primitive)) {
				return false;
			}
			
			Primitive pt = (Primitive) t;
			
			//TODO: verify that elements do not need to be checked for primitive types
			return join(this, pt).equals(this);
		}

		public Type substitute(Map<String, Type> environment) {
			return this;
		}
		public void bind(Type t, Map<String, Type> environment) {
		}		

		private static String[] toStringStrs = { 
			"boolean", "byte", "short", "char", "int", "long", "float", "double" 
		};
		
		public String toString() {
			String r = toString(elements);
			if(r.length() == 0) {
				return toStringStrs[type];
			} else {
				return r + " " + toStringStrs[type];
			}
		}
	}

	/**
	 * Represents the Java type "boolean"
	 * @author djp
	 *
	 */
	public final static class Boolean extends Primitive {
		public Boolean(TypeElement... es) {
			super(booleanT, es);
		}		
	}
	
	/**
	 * Represents the Java type "byte"
	 * @author djp
	 *
	 */
	public final static class Byte extends Primitive {
		public Byte(TypeElement... es) {
			super(byteT, es);
		}
	}
	
	/**
	 * Represents the Java type "char"
	 * @author djp
	 *
	 */
	public final static class Char extends Primitive {
		public Char(TypeElement... es) {
			super(charT, es);
		}
	}
	
	/**
	 * Represents the Java type "short"
	 * @author djp
	 *
	 */
	public final static class Short extends Primitive {
		public Short(TypeElement... es) {
			super(shortT, es);
		}
	}
	
	/**
	 * Represents the Java type "int"
	 * @author djp
	 *
	 */
	public static class Int extends Primitive {
		public Int(TypeElement... es) {
			super(intT, es);
		}
	}
	
	/**
	 * Represents the Java type "long"
	 * @author djp
	 *
	 */
	public final static class Long extends Primitive {
		public Long(TypeElement... es) {
			super(longT, es);
		}			
	}
	
	/**
	 * Represents the Java type "float"
	 * @author djp
	 *
	 */
	public final static class Float extends Primitive {
		public Float(TypeElement... es) {
			super(floatT, es);
		}
	}
	
	/**
	 * Represents the Java type "double"
	 * @author djp
	 *
	 */
	public final static class Double extends Primitive {
		public Double(TypeElement... es) {
			super(doubleT, es);
		}	
	}

	/**
     * Represents all kinds of reference type (e.g. java.lang.String), excluding
     * Arrays.
     * 
     * @author djp
     * 
     */
	public static class Reference extends Type {
		final private String pkg;
		final private Pair<String, Type[]>[] classes;

		public Reference(String p, Pair<String, Type[]>[] cs, TypeElement[] es) {
			super(es);
			pkg = p;
			classes = cs;
		}

		@SuppressWarnings("unchecked")
		public Reference(String p, String... cs) {
			super(new TypeElement[0]);
			pkg = p;
			classes = new Pair[cs.length];
			for (int i = 0; i != classes.length; ++i) {
				classes[i] = new Pair<String, Type[]>(cs[i], new Type[0]);
			}
		}

		public String pkg() {
			return pkg;
		}
		public String name() {
			return classes[classes.length-1].first();
		}

		public Pair<String, Type[]>[] classes() {
			return classes;
		}
		
		public boolean equals(Object o) {
			if (o instanceof Reference) {
				Reference p = (Reference) o;		
				
				if (classes.length != p.classes.length) {
					return false;
				}
				for (int i = 0; i != p.classes.length; ++i) {			
					
					if (!p.classes[i].first().equals(classes[i].first())
							|| !Arrays.equals(p.classes[i].second(), classes[i]
									.second())) {
						return false;
					}
				}
				return pkg.equals(pkg) && Arrays.equals(elements, p.elements);
			}
			return false;
		}
		
		public int hashCode() {
			return Arrays.hashCode(elements) ^ pkg.hashCode() ^ Arrays.hashCode(classes);
		}

		public String unqualifiedName() {
			StringBuilder s = new StringBuilder(pkg);
			if (pkg.length() > 0) {
				s.append('.');
			}
			boolean firstTime = true;
			for (Pair<String, Type[]> c : classes) {
				if (!firstTime) {
					s.append('$');
				}
				firstTime = false;
				s.append(c.first());
			}
			return s.toString();
		}

		public Type union(Type t) {
			// THIS METHOD IS STILL BROKEN BECAUSE IT UNIONS
			// TYPE ARGUMENTS WHEN IT SHOULDN'T
			if (t instanceof Void) {
				return this;
			}
			if (t instanceof Primitive || t instanceof Any) {
				return t.union(this);
			}
			TypeElement[] es = union(this.elements, t.elements);
			if (t instanceof Array || t instanceof Variable) {
				return referenceType("java.lang","Object",es);
			}
			if (t instanceof Null) {
				if (es == elements) {
					return this;
				}
				return new Reference(pkg, classes, es);
			}

			try {
				return ClassTable.commonSuperType(this, (Reference) t);
			} catch(ClassNotFoundException e) {
				throw new RuntimeException("Unable to find class " + e.getMessage());
			}
		}
		
		public boolean supsetEqOfElem(Type t) {
			if (!(t instanceof Reference)) {				
				return false;
			}

			Reference r = (Reference) t;
			if (!r.pkg().equals(pkg) || r.classes.length != classes.length) {
				return false;
			}

			for (int i = 0; i != classes.length; ++i) {
				// if names don't match this aint gonna work
				if (!classes[i].first().equals(r.classes[i].first())) {
					return false;
				}
				// type arguments must match, except for special
				// case to do with wildcards
				Type[] t1s = classes[i].second();
				Type[] t2s = r.classes[i].second();
				if (t1s.length != t2s.length) {
					return false;
				}

				for (int j = 0; j != t1s.length; ++j) {
					if (t1s[j] instanceof Variable) {
						Variable v = (Variable) t1s[j];
						if (v.name.charAt(0) == '?') {
							// this is a wildcard, so OK
							continue;
						}
					}
					if (!t1s[j].supsetEqOfElem(t2s[j])) {
						return false;
					}
				}
			}
			// et voila!
			return Arrays.equals(this.elements, t.elements);
		}

		public boolean supsetEqOf(Type t) {
			if (t instanceof Void) { return true; }
			if (t instanceof Null
					|| (t instanceof Array && unqualifiedName().equals(
							"java.lang.Object"))
					|| (t instanceof Array && unqualifiedName().equals(
							"java.io.Serializable"))
					|| (t instanceof Array && unqualifiedName().equals(
							"java.lang.Cloneable"))
					||
					// could make following line more precise by considering
                    // bounds
					(t instanceof Variable && unqualifiedName().equals(
							"java.lang.Object"))) {
				return supsetEqOf(elements, t.elements);
			} else if (!(t instanceof Reference)) {				
				if (pkg.equals("java.lang") && classes.length == 1
						&& t instanceof Primitive) {												
					// check for autoboxing
					Primitive p = (Primitive) t;					
					return supsetEqOf(Primitive.autoBoxingTypes[p.type]);					
				}
				return false;
			}
			Reference g = (Reference) t;

			Type.Reference r;
			try {
				r = ClassTable.commonSuperType(this,g);
			} catch(ClassNotFoundException e) {	
				// ug.
				throw new RuntimeException("Unable to find class " + e.getMessage());
			}

			if(r.classes.length != classes.length) { return false; }
			
			for (int i = 0; i != classes.length; ++i) {
				if(!classes[i].first().equals(r.classes[i].first())) { 
					return false;
				}
				Type[] t1s = classes[i].second();
				Type[] t2s = r.classes[i].second();
				if (t1s.length == t2s.length) {
					for (int j = 0; j != t1s.length; ++j) {
						if (t1s[j] instanceof Variable) {
							Variable v = (Variable) t1s[j];
							if (v.name.charAt(0) == '?') {
								// this is an unknown type
								continue;
							}
						}
						if (!t1s[j].supsetEqOfElem(t2s[j])) {
							return false;
						}
					}
				} else if(t1s.length != 0 && t2s.length != 0) {
					return false;
				} 
			}			
			// and, finally ...
			return supsetEqOf(this.elements, t.elements);
		}

		public Type substitute(Map<String, Type> environment) {
			@SuppressWarnings("unchecked")
			Pair<String, Type[]>[] cs = new Pair[classes.length];
			boolean changed = false;
			for (int i = 0; i != cs.length; ++i) {
				Type[] args = classes[i].second();
				Type[] as = new Type[args.length];
				boolean localChange = false;
				for (int j = 0; j != as.length; ++j) {
					as[j] = args[j].substitute(environment);
					localChange |= (as[j] != args[j]);
				}
				if (localChange) {
					cs[i] = new Pair<String, Type[]>(classes[i].first(), as);
				} else {
					cs[i] = classes[i];
				}
				changed |= localChange;
			}
			if (changed) {
				return new Reference(pkg, cs, elements);
			} else {
				return this;
			}
		}

		public void bind(Type t, Map<String, Type> environment) {
			if (t instanceof Reference) {
				Reference r = (Reference) t;

				for (int i = 0; i != Math.min(classes.length, r.classes.length); ++i) {
					Type[] t1args = classes[i].second();
					Type[] t2args = r.classes[i].second();
					for (int j = 0; j != Math.min(t1args.length, t2args.length); ++j) {
						t1args[j].bind(t2args[j], environment);
					}
				}
			}
		}
		
		public Set<Variable> freeVariables() {
			HashSet<Variable> vs = new HashSet<Variable>();
			for (int i = 0; i != classes.length; ++i) {
				for (Type t : classes[i].second()) {
					vs.addAll(t.freeVariables());
				}
			}
			return vs;
		}
		
		public String toString() {
			String r = toString(elements);
			if (r.length() > 0) {
				r += " ";
			}
			r += pkg;
			boolean firstTime = pkg.length() == 0;
			for (Pair<String, Type[]> n : classes) {
				if (!firstTime) {
					r += ".";
				}
				firstTime = false;
				r += n.first();
				Type[] typeArgs = n.second();
				if (typeArgs != null && typeArgs.length > 0) {
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

		public String toShortString() {
			String r = toString(elements);
			if (r.length() > 0) {
				r += " ";
			}
			boolean firstTime = true;
			for (Pair<String, Type[]> n : classes) {
				if (!firstTime) {
					r += ".";
				}
				firstTime = false;
				r += n.first();
				Type[] typeArgs = n.second();
				if (typeArgs != null && typeArgs.length > 0) {
					r += "<";
					boolean innerFirstTime = true;
					for (Type t : typeArgs) {
						if (!innerFirstTime) {
							r += ", ";
						}
						innerFirstTime = false;
						r += t.toShortString();
					}
					r += ">";
				}
			}
			return r;
		}
	}

	/**
	 * Represents the type of a method.  For example, the following method
	 * 
	 * void m(int x) { ... } has type "void(int)" 
	 * @author djp
	 *
	 */
	public final static class Function extends Type {
		private final Type[] paramTypes;
		private final Type returnType;
		private final Type.Variable[] typeArgs;

		public Function(Type returnT, Type[] ps, Type.Variable[] ts,
				TypeElement[] e) {
			super(e);
			paramTypes = ps;
			returnType = returnT;
			typeArgs = ts;
		}

		public Function(Type returnT, Type[] ps, Type.Variable[] ts) {
			super(new TypeElement[0]);
			paramTypes = ps;
			returnType = returnT;
			typeArgs = ts;
		}

		public boolean equals(Object o) {
			if (o instanceof Function) {
				Function f = (Function) o;
				return returnType.equals(f.returnType)
						&& Arrays.equals(paramTypes, f.paramTypes);
			}
			return false;
		}

		public boolean supsetEqOf(Type t) {
			if (t instanceof Function) {
				Function f = (Function) t;

				// sanity check
				if (paramTypes.length != f.paramTypes.length) {
					return false;
				}

				// There is a slight kludge here, since
				// in the special case of an instance method
				// we don't require that the receiver type
				// be a contravariant parameter position.
				//
				// Also, i'm assuming that supsetEqOf() is
				// only ever called on instance methods,
				// rather than static methods (since this
				// has no meaninig). If that is required then
				// an extra bit of information is required to
				// distinguish static and instance methods

				for (int i = 0; i != paramTypes.length; ++i) {
					if (i == 0 && paramTypes[i] instanceof Reference
							&& f.paramTypes[i] instanceof Reference) {
						// this is the receiver type
						if (!supsetEqOf(f.paramTypes[0].elements,
								paramTypes[0].elements)) {
							return false;
						}
					} else {

						// The "swapping" round of positions for the
						// parameter types corresponds with the
						// notion of "contravariance".

						if (!f.paramTypes[i].supsetEqOf(paramTypes[i])) {
							return false;
						}
					}
				}

				return returnType.supsetEqOf(f.returnType);
			}
			return false;
		}

		public int hashCode() {
			return Arrays.hashCode(elements) ^ Arrays.hashCode(paramTypes)
					^ returnType.hashCode();
		}
		
		public Type union(Type _t) {
			throw new InternalError(
					"Union operation not supported on Type.Function");
		}
		
		public Type[] parameterTypes() {
			return paramTypes;
		}
		public Type returnType() {
			return returnType;
		}
		public Type.Variable[] typeArgs() {
			return typeArgs;
		}

		public Type substitute(Map<String, Type> environment) {
			Type ret = returnType.substitute(environment);
			Type[] ts = new Type[paramTypes.length];
			boolean changed = false;
			for (int i = 0; i != ts.length; ++i) {
				ts[i] = paramTypes[i].substitute(environment);
				changed |= ts[i] != paramTypes[i];
			}
			if (!changed) {
				ts = paramTypes;
			}
			
			Type.Variable[] args = new Type.Variable[typeArgs.length];
			changed = false;
			int eliminatedCount = 0;
			for (int i = 0; i != args.length; ++i) {
				// there must be a bug here.
				Type t = typeArgs[i].substitute(environment);
				if(t instanceof Variable) {
					args[i] = (Variable) t;
				} else {
					args[i] = null;
					eliminatedCount++;
				}
				changed |= args[i] != typeArgs[i];
			}
			if(eliminatedCount > 0) {
				// here, some type variables have been eliminated
				Type.Variable[] nargs = new Type.Variable[typeArgs.length
						- eliminatedCount];
				for(int i=0,j=0; i != args.length; ++i) { 
					if(args[i] != null) {
						nargs[j++]=args[i];
					}
				}
				args=nargs;
			} else if (!changed) {
				args = typeArgs;
			}
			if (ret != returnType || ts != paramTypes || args != typeArgs) {
				return new Function(ret, ts, args);
			} else {
				return this;
			}
		}

		public void bind(Type t, Map<String, Type> environment) {
			if (t instanceof Function) {
				Function f = (Function) t;
				HashMap<String, Type> nenv = new HashMap<String, Type>();
				returnType.bind(f.returnType, nenv);
				for (int i = 0; i != Math.min(paramTypes.length,
						f.paramTypes.length); ++i) {
					paramTypes[i].bind(f.paramTypes[i], nenv);
				}
				// now, put in only those which are actually declared!
				for (Type vt : typeArgs) {
					if (vt instanceof Variable) {
						Variable v = (Variable) vt;
						environment.put(v.name, nenv.get(v.name));
					}
				}
			}
		}
		
		public Set<Variable> freeVariables() {
			HashSet<Variable> vs = new HashSet<Variable>();
			for (Type t : typeArgs) {
				vs.addAll(t.freeVariables());
			}
			for(Type p : paramTypes) {
				vs.addAll(p.freeVariables());
			}
			vs.addAll(returnType.freeVariables());
			return vs;
		}

		public String toString() {
			String r = "";

			if (typeArgs.length > 0) {
				r += "<";
				boolean firstTime = true;
				for (Type t : typeArgs) {
					if (!firstTime) {
						r += ", ";
					}
					firstTime = false;
					r += t;
				}
				r += "> ";
			}

			r += returnType + " (";

			boolean firstTime = true;
			for (int i = 0; i != paramTypes.length; ++i) {
				if (!firstTime) {
					r += ", ";
				}
				firstTime = false;
				r += paramTypes[i];
			}

			return r + ")";
		}

		public String toShortString() {
			String r = "";

			if (typeArgs.length > 0) {
				r += "<";
				boolean firstTime = true;
				for (Type t : typeArgs) {
					if (!firstTime) {
						r += ", ";
					}
					firstTime = false;
					r += t.toShortString();
				}
				r += "> ";
			}

			r += returnType.toShortString() + " (";

			boolean firstTime = true;
			for (int i = 0; i != paramTypes.length; ++i) {
				if (!firstTime) {
					r += ", ";
				}
				firstTime = false;
				// System.out.println(i);
				r += paramTypes[i].toShortString();
			}

			return r + ")";
		}
	}

	/**
     * Represents a Generic type variable. For example, the T in class ArrayList<T> {
     * ... }
     * 
     * @author djp
     * 
     */
	public static class Variable extends Type {
		protected String name;
		protected Type[] lowerBounds;

		Variable(String n,
		Type[] ls,
		TypeElement[] es) {
			super(es);
			name = n;
			lowerBounds = ls;
		}

		public Type union(Type t) {
			if (t instanceof Variable) {
				// more should be done here with
				// regard to lower and upper bounds.
				Variable v = (Variable) t;
				if (name.equals(v.name)) {
					return this;
				}
			}
			// Could improve this in the case of unioning
			// an unknown type with another. This would
			// produce a new unknown type.
			return referenceType("java.lang","Object",elements);			
		}
		
		public boolean supsetEqOf(Type t) {
			if (t instanceof Null) {
				return true;
			}
			if (t instanceof Variable) {
				Variable v = (Variable) t;
				return name.equals(v.name);
			}
			return false;
		}

		public Type substitute(Map<String, Type> environment) {
			// I SHOULD DO STUFF WITH THE VARIABLE
			// BOUNDS HERE ...
			Type r = environment.get(name);
			if (r == null) {
				return this;
			} else
				return r;
		}

		public void bind(Type t, Map<String, Type> environment) {
			if (t instanceof Variable) {
				// in the special case where we're
				// binding with the same variable
				// we actually do nothing.
				Variable v = (Variable) t;
				if (name.equals(v.name)) {
					return;
				}
			}
			Type r = environment.get(name);
			if (r != null) {
				// we already have a binding, so need
				// to find the most general unifier.
				Type nr = r.union(t);
				if (nr == r) {
					return;
				}
				t = r;
			}
			environment.put(name, t);
		}

		public boolean equals(Object o) {
			if (o instanceof Variable) {
				Variable v = (Variable) o;
				return name.equals(v.name)
						&& Arrays.equals(lowerBounds, v.lowerBounds);
			}
			return false;
		}

		public int hashCode() {
			return name.hashCode();
		}

		public Set<Variable> freeVariables() {
			HashSet<Variable> vs = new HashSet<Variable>();
			vs.add(this);
			return vs;
		}
		
		public String toString() {
			String r = toString(elements);
			if (r.length() > 0) {
				r += " ";
			}
			r += name;
			if (lowerBounds.length > 0) {
				boolean firstTime=true;
				r += " extends ";
				for (Type lb : lowerBounds) {
					if(!firstTime) { r += " & "; }
					firstTime=false;
					r += lb.toString();
				}
			}
			return r;
		}

		public String toShortString() {
			String r = toString(elements);
			if (r.length() > 0) {
				r += " ";
			}
			r += name;
			if (lowerBounds.length > 0) {
				r += " extends ";
				boolean firstTime=true;
				for (Type lb : lowerBounds) {
					if(!firstTime) { r += " & "; }
					firstTime=false;
					r += lb.toShortString();
				}
			}
			return r;
		}

		public String name() {
			return name;
		}	
		
		public Type[] lowerBounds() {
			return lowerBounds;
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
	// NOTE: I think it's wrong to view the wildcard as a variable. They
	// actually behave quite differently
	public static class Wildcard extends Type.Variable {
		private Type[] upperBounds;

		public Wildcard(Type[] lowerbounds,
				Type[] upperbounds,
				TypeElement[] es) {
			super("?", lowerbounds, es);
			this.upperBounds = upperbounds;
		}		
		
		// I need to override many of the methods from Type.Variable
		public String toString() {
			String r = toString(elements);
			if (r.length() > 0) {
				r += " ";
			}
			r += "?";			
			if (lowerBounds.length > 0) {				
				r += " extends ";
				boolean firstTime=true;
				for (Type lb : lowerBounds) {
					if(!firstTime) { r += " & "; }
					firstTime=false;
					r += lb.toString();
				}
			}
			if (upperBounds.length > 0) {
				r += " super ";
				boolean firstTime=true;
				for (Type ub : upperBounds) {
					if(!firstTime) { r += " & "; }
					firstTime=false;
					r += ub.toString();
				}
			}
			return r;
		}

		public Set<Variable> freeVariables() {
			return new HashSet<Variable>();
		}
		
		public String toShortString() {
			String r = toString(elements);
			if (r.length() > 0) {
				r += " ";
			}
			r += "?";			
			if (lowerBounds.length > 0) {				
				r += " extends ";
				boolean firstTime=true;
				for (Type lb : lowerBounds) {
					if(!firstTime) { r += " & "; }
					firstTime=false;
					r += lb.toShortString();
				}
			}
			if (upperBounds.length > 0) {
				r += " super ";
				boolean firstTime=true;
				for (Type ub : upperBounds) {
					if(!firstTime) { r += " & "; }
					firstTime=false;
					r += ub.toShortString();
				}
			}
			return r;
		}				
	}

	// ====================================================
	// =============== Type Factory Interface =============
	// ====================================================

	/**
	 * Use for creating a "Void" type
	 */
	public static Type.Void voidType() {
		return voidT;
	}

	/**
	 * Use for creating a "Null" type
	 * @param es
	 * @return
	 */
	public static Type.Null nullType(TypeElement... es) {
		if (nullT == null) {
			nullT = new Null(es);
		}
		return nullT;
	}

	/**
	 * Use for creating an "Any" type.
	 * @param es
	 * @return
	 */
	public static Type.Any anyType(TypeElement... es) {
		return new Any(es);
	}

	/**
	 * This is for creating "?" types, such as for use in "?". This method never
	 * returns the same type, so beware!
	 * 
	 * @param es
	 * @return
	 */
	public static Type.Variable wildcardType(Type[] lowerBounds,
			Type[] upperBounds, TypeElement... es) {		
		return new Wildcard(lowerBounds, upperBounds, es);
	}
	
	/**
	 * Construct an instance of Type.Boolean
	 * 
	 * @param elements
	 * @return
	 */
	public static Type.Boolean booleanType(TypeElement... es) {
		return new Boolean(es);
	}

	/**
	 * Construct an instance of Type.Byte.
	 * 
	 * @param elements
	 * @return
	 */
	public static Type.Byte byteType(TypeElement... elements) {
		return new Byte(elements);
	}
	
	/**
	 * Construct an instance of Type.Char.
	 * 
	 * @param elements
	 * @return
	 */
	public static Type.Char charType(TypeElement... elements) {
		return new Char(elements);
	}

	/**
	 * Construct an instance of Type.Short.
	 * 
	 * @param elements
	 * @return
	 */
	public static Type.Short shortType(TypeElement... elements) {
		return new Short(elements);
	}

	/**
	 * Construct an instance of Type.Int.
	 * 
	 * @param elements
	 * @return
	 */
	public static Type.Int intType(TypeElement... elements) {
		return new Int(elements);
	}

	/**
	 * Construct an instance of Type.Long.
	 * 
	 * @param elements
	 * @return
	 */
	public static Type.Long longType(TypeElement... elements) {
		return new Long(elements);
	}

	/**
	 * Construct an instance of Type.Float.
	 * 
	 * @param elements
	 * @return
	 */
	public static Type.Float floatType(TypeElement... elements) {
		return new Float(elements);
	}

	/**
	 * Construct an instance of Type.Double.
	 * 
	 * @param elements
	 * @return
	 */
	public static Type.Double doubleType(TypeElement... elements) {
		return new Double(elements);
	}

	/**
	 * Construct a reference from a package, and a single class name. This is
	 * really a shortcut method for the special case of an outer class with no
	 * generic type parameters.
	 * 
	 * @param pkg
	 * @param name
	 * @param es
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Type.Reference referenceType(String pkg,
			String name, TypeElement... es) {
		Pair<String,Type[]>[] classes = new Pair[1];
		classes[0] = new Pair<String, Type[]>(name,new Type[0]);
		return new Reference(pkg, classes, es);
	}

	/**
	 * Construct a reference from a package, and appropriate pairs of name and
	 * type parameters. Multiple names will be needed for creating references to
	 * inner classes. Also, the array of type parameters should always be
	 * non-null.
	 * 
	 * @param pkg
	 * @param classes
	 * @param es
	 * @return
	 */
	public static Type.Reference referenceType(String pkg,
			Pair<String, Type[]>[] classes, TypeElement... es) {
		return new Reference(pkg, classes, es);
	}

	/**
	 * Create a uni-dimensional array.
	 * 
	 * @param dims >
	 *            0
	 * @param e
	 * @param es
	 * @return
	 */
	public static Type.Array arrayType(Type e, TypeElement... es) {
		return new Array(e, es);
	}

	/**
     * Create a multi-dimensional array with dims dimensions.
     * 
     * @param dims >
     *            0
     * @param e
     * @param es
     * @return
     */
	public static Type.Array arrayType(int dims, Type e, TypeElement... es) {
		if (dims == 0) {
			throw new IllegalArgumentException("arrayType requires dims > 0");
		} else if (dims == 1) {
			return new Array(e, es);
		} else {
			return new Array(arrayType(dims - 1, e, es));
		}
	}

	/**
	 * Construct an instanceof of Type.Variable.
	 * 
	 * @param name
	 * @param lowerBounds
	 * @param es
	 * @return
	 */
	public static Type.Variable variableType(String name, Type[] lowerBounds, TypeElement... es) {
		return new Variable(name, lowerBounds, es);
	}

	/**
	 * Construct an instance of Type.Function.
	 * 
	 * @param returnT
	 * @param paramsT
	 * @param typeArgs
	 * @return
	 */
	public static Type.Function functionType(Type returnT, Type[] paramsT,
			Type.Variable... typeArgs) {
		return new Function(returnT, paramsT, typeArgs);
	}

	/**
	 * Construct an instance of Type.Function for a generic method.
	 * 
	 * @param returnT
	 * @param paramsT
	 * @param typeArgs
	 * @return
	 */
	public static Type.Function functionType(Type returnT, Type[] paramsT,
			Type.Variable[] typeArgs, TypeElement... es) {
		return new Function(returnT, paramsT, typeArgs, es);
	}

	// ====================================================
	// ================= Type Vector Helpers ==============
	// ====================================================

	static TypeElement[] union(TypeElement[] A, TypeElement[] B) {
		TypeElement[] es = new TypeElement[A.length];
		boolean changed = false;
		for (int i = 0; i != A.length; ++i) {
			TypeElement oldVal = A[i];
			TypeElement newVal = oldVal.union(B[i]);
			es[i] = newVal;
			changed |= !newVal.equals(oldVal);
		}

		if (changed) {
			return es;
		} else {
			return A;
		}
	}
	
	static boolean supsetEqOf(TypeElement[] A, TypeElement[] B) {
		for (int i = 0; i != A.length; ++i) {
			if (!A[i].supsetEqOf(B[i])) {
				return false;
			}
		}
		return true;
	}

	static String toString(TypeElement[] v) {
		String r = "";
		int x = r.length();
		for (TypeElement e : v) {
			if (x != r.length()) {
				r += " ";
			}
			x = r.length();
			r += e.toString();
		}
		return r;
	}	
}
