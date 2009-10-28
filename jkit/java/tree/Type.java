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

package jkit.java.tree;

import java.util.*;

import static jkit.compiler.SyntaxError.*;
import jkit.compiler.SyntacticAttribute;
import jkit.compiler.SyntacticElementImpl;
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
	
	public Type(SyntacticAttribute... attributes) {
		super(attributes);
	}
	
	/**
     * The Primitive type abstracts all the primitive types.
     */
	public static class Primitive extends Type {
		public Primitive(SyntacticAttribute... attributes) {
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
		public Void(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}
	
	/**
	 * Represents the Java type "boolean"
	 * @author djp
	 *
	 */
	public static class Bool extends Primitive {		
		public Bool(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}
	

	/**
	 * Represents the Java type "byte"
	 * @author djp
	 *
	 */
	public static class Byte extends Primitive {
		public Byte(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}
	
	/**
	 * Represents the Java type "char"
	 * @author djp
	 *
	 */
	public static class Char extends Primitive {
		public Char(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}
	
	/**
	 * Represents the Java type "short"
	 * @author djp
	 *
	 */
	public static class Short extends Primitive {
		public Short(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}

	/**
	 * Represents the Java type "int"
	 * @author djp
	 *
	 */
	public static class Int extends Primitive {
		public Int(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}
	
	/**
	 * Represents the Java type "long"
	 * @author djp
	 *
	 */
	public static class Long extends Primitive {
		public Long(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}
	
	/**
	 * Represents the Java type "float"
	 * @author djp
	 *
	 */
	public static class Float extends Primitive {
		public Float(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}
	
	/**
	 * Represents the Java type "double"
	 * @author djp
	 *
	 */
	public static class Double extends Primitive {
		public Double(SyntacticAttribute... attributes) {
			super(attributes);
		}
	}
	
	
	/**
     * The Reference type abstracts all the reference types, including class
     * types, array types, variable and wildcard types.
     */
	public static class Reference extends Type {
		public Reference(SyntacticAttribute... attributes) {
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
		public Array(Type element, SyntacticAttribute... attributes) {
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
				SyntacticAttribute... attributes) {
			super(attributes);
			this.components = components;
		}

		public Clazz(String str, SyntacticAttribute... attributes) {
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
		
		public String toString() {
			String r = "";
			boolean firstTime = true;
			for(Pair<String,List<Type.Reference>> c : components) {
				if(!firstTime) {
					r = r + ".";
				}
				firstTime = false;
				r = r + c.first();
			}
			return r;
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

		public Wildcard(Reference lowerBound, Reference upperBound, SyntacticAttribute... attributes) {
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
				SyntacticAttribute... attributes) {
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
				SyntacticAttribute... attributes) {
			super(attributes);
			this.bounds = bounds;
		}
		
		public List<Type.Reference> bounds() {
			return bounds;
		}					
	}
	
	/**
     * Convert a type in jil to a type in java. This method is annoying, since
     * it seems to be converting to the same thing. However, there is a subtle
     * difference, in that a Java type represents a type as written in the
     * source code, rather than the abstract notion of a type.
     * 
     * @param jt
     * @return
     */
	public static jkit.java.tree.Type fromJilType(jkit.jil.tree.Type t) {		
		if(t instanceof jkit.jil.tree.Type.Primitive) {
			return fromJilType((jkit.jil.tree.Type.Primitive)t);
		} else if(t instanceof jkit.jil.tree.Type.Array) {
			return fromJilType((jkit.jil.tree.Type.Array)t);
		} else if(t instanceof jkit.jil.tree.Type.Clazz) {
			return fromJilType((jkit.jil.tree.Type.Clazz)t);
		} else if(t instanceof jkit.jil.tree.Type.Variable) {
			return fromJilType((jkit.jil.tree.Type.Variable)t);
		} else if(t instanceof jkit.jil.tree.Type.Wildcard) {
			return fromJilType((jkit.jil.tree.Type.Wildcard)t);
		}
		
		return null;
	}
	
	public static jkit.java.tree.Type.Primitive fromJilType(
			jkit.jil.tree.Type.Primitive pt) {
		if (pt instanceof jkit.jil.tree.Type.Void) {
			return new jkit.java.tree.Type.Void(pt);
		} else if (pt instanceof jkit.jil.tree.Type.Bool) {
			return new jkit.java.tree.Type.Bool(pt);
		} else if (pt instanceof jkit.jil.tree.Type.Byte) {
			return new jkit.java.tree.Type.Byte(pt);
		} else if (pt instanceof jkit.jil.tree.Type.Char) {
			return new jkit.java.tree.Type.Char(pt);
		} else if (pt instanceof jkit.jil.tree.Type.Short) {
			return new jkit.java.tree.Type.Short(pt);
		} else if (pt instanceof jkit.jil.tree.Type.Int) {
			return new jkit.java.tree.Type.Int(pt);
		} else if (pt instanceof jkit.jil.tree.Type.Long) {
			return new jkit.java.tree.Type.Long(pt);
		} else if (pt instanceof jkit.jil.tree.Type.Float) {
			return new jkit.java.tree.Type.Float(pt);
		} else {
			return new jkit.java.tree.Type.Double(pt);
		}
	}
	
	public static jkit.java.tree.Type.Array fromJilType(
			jkit.jil.tree.Type.Array at) {
		return new jkit.java.tree.Type.Array(fromJilType(at.element()), at);
	}
	
	public static jkit.java.tree.Type.Variable fromJilType(
			jkit.jil.tree.Type.Variable v) {
		return new jkit.java.tree.Type.Variable(v.variable(), (jkit.java.tree.Type.Reference) fromJilType(v.lowerBound()));
	}
	
	public static jkit.java.tree.Type.Wildcard fromJilType(
			jkit.jil.tree.Type.Wildcard v) {
		return new jkit.java.tree.Type.Wildcard(
				(jkit.java.tree.Type.Reference) fromJilType(v.lowerBound()),
				(jkit.java.tree.Type.Reference) fromJilType(v.upperBound()));
	}
	
	public static jkit.java.tree.Type.Clazz fromJilType(
			jkit.jil.tree.Type.Clazz jt) {			
		// I will make it fully qualified for simplicity.
		ArrayList<Pair<String,List<jkit.java.tree.Type.Reference>>> ncomponents = new ArrayList();
		// So, we need to split out the package into the component parts
		String pkg = jt.pkg();
		int idx = 0;
		int start = 0;		
		while((idx = pkg.indexOf('.',start)) != -1) {
			ncomponents.add(new Pair(pkg.substring(start,idx),new ArrayList()));			
			start = idx+1;
		}		
		
		// Now, complete the components list
		for(Pair<String,List<jkit.jil.tree.Type.Reference>> c : jt.components()) {
			ArrayList<jkit.java.tree.Type.Reference> l = new ArrayList();
			for(jkit.jil.tree.Type.Reference r : c.second()) {
				l.add((jkit.java.tree.Type.Reference)fromJilType(r));
			}
			ncomponents.add(new Pair(c.first(),l));
		}
		
		return new jkit.java.tree.Type.Clazz(ncomponents,jt);
	}
}
