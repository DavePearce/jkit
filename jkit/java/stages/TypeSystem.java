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

package jkit.java.stages;

import java.util.*;

import static jkit.jil.util.Types.*;
import jkit.compiler.ClassLoader;
import jkit.compiler.FieldNotFoundException;
import jkit.compiler.MethodNotFoundException;
import jkit.compiler.Clazz;
import jkit.jil.tree.JilMethod;
import jkit.jil.tree.Type;
import jkit.jil.util.*;
import jkit.util.Pair;
import jkit.util.Triple;

/**
 * This method contains a variety of useful algorithms for deal with Java's type
 * system.
 * 
 * @author djp
 */
public class TypeSystem {
	
	/**
	 * <p>
	 * This method determines whether t1 :> t2; that is, whether t2 is a subtype
	 * of t1 or not, following the class hierarchy. Observe that this relation
	 * is reflexive, transitive and anti-symmetric:
	 * </p>
	 * 
	 * <ol>
	 * <li> t1 :> t1 always holds</li>
	 * <li> if t1 :> t2 and t2 :> t3, then t1 :> t3</li>
	 * <li> if t1 :> t2 then not t2 :> t1 (unless t1 == t2)</li>
	 * </ol>
	 * 
	 * <p>
	 * <b>Note</b>, this method does not consider auto-boxing. Thus, it is not the case
	 * that int <: java.lang.Integer. To including autoboxing information, use
	 * boxSubtype().
	 * </p>
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 * @throws ClassNotFoundException
	 */
	public boolean subtype(Type t1, Type t2, ClassLoader loader)
			throws ClassNotFoundException {		
		if(loader == null) {
			throw new IllegalArgumentException("loader cannot be null");
		}
		if(t1 == null) {
			throw new IllegalArgumentException("t1 cannot be null");
		}
		if(t2 == null) {
			throw new IllegalArgumentException("t2 cannot be null");
		}				
		
		// First, do the easy cases ...		
		if(t1 instanceof Type.Reference && t2 instanceof Type.Null) {
			return true; // null is a subtype of all references.
		} else if(t1 instanceof Type.Intersection && t2 instanceof Type.Reference) {			
			return subtype((Type.Intersection) t1, (Type.Reference) t2, loader);
		} else if(t2 instanceof Type.Intersection && t1 instanceof Type.Reference) {			
			return subtype((Type.Reference) t1, (Type.Intersection) t2, loader);
		} else if(t1 instanceof Type.Reference && t2 instanceof Type.Wildcard) {			
			return subtype((Type.Reference) t1, (Type.Wildcard) t2, loader);
		}  else if(t1 instanceof Type.Wildcard && t2 instanceof Type.Reference) {			
			return subtype((Type.Wildcard) t1, (Type.Reference) t2, loader);
		} else if(t1 instanceof Type.Clazz && t2 instanceof Type.Clazz) {			
			return subtype((Type.Clazz) t1, (Type.Clazz) t2, loader);
		} else if(t1 instanceof Type.Primitive && t2 instanceof Type.Primitive) {
			return subtype((Type.Primitive) t1, (Type.Primitive) t2);
		} else if(t1 instanceof Type.Array && t2 instanceof Type.Array) {
			return subtype((Type.Array) t1, (Type.Array) t2, loader);
		} else if(t2 instanceof Type.Array && t1 instanceof Type.Clazz) {
			return JAVA_LANG_OBJECT.equals(t1)
					|| JAVA_LANG_CLONEABLE.equals(t1)
					|| JAVA_IO_SERIALIZABLE.equals(t1);
		} else if (t1 instanceof Type.Variable && t2 instanceof Type.Variable) {
			return t1.equals(t2);
		} else if(t2 instanceof Type.Variable && t1 instanceof Type.Reference) {			
			Type.Variable tv = (Type.Variable) t2;
			if(tv.lowerBound() != null) {				
				return subtype(t1,tv.lowerBound(),loader);
			} else {				
				return JAVA_LANG_OBJECT.equals(t1);
			}
		} else if(t1 instanceof Type.Variable && t2 instanceof Type.Reference) {			
			Type.Variable tv = (Type.Variable) t1;
			if(tv.lowerBound() != null) {				
				return subtype(tv.lowerBound(),t2,loader);
			} else {				
				return subtype(JAVA_LANG_OBJECT,t2,loader);				
			}
		} 
		
		return false;
	}
	
	/**
     * This determines whether two primitive types are subtypes of each other or
     * not. The JLS 4.10.1 states that subtyping between primitives looks like
     * this:
     * 
     * <pre>
     *    double :&gt; float 
     *    float :&gt; long
     *    long :&gt; int
     *    int :&gt; char 
     *    int :&gt; short 
     *    short :&gt; byte
     * </pre>
     * 
     * @param t1
     * @param t2
     * @return
     */
	public boolean subtype(Type.Primitive t1, Type.Primitive t2) {		
		if(t1 == null) {
			throw new IllegalArgumentException("t1 cannot be null");
		}
		if(t2 == null) {
			throw new IllegalArgumentException("t2 cannot be null");
		}	
		if(t1.getClass() == t2.getClass()) {
			return true;
		} else if(t1 instanceof Type.Double && subtype(T_FLOAT,t2)) { 
			return true;
		} else if(t1 instanceof Type.Float && subtype(T_LONG,t2)) {
			return true;
		} else if(t1 instanceof Type.Long && subtype(T_INT,t2)) {
			return true;
		} else if(t1 instanceof Type.Int && subtype(T_SHORT,t2)) {
			return true;
		} else if(t1 instanceof Type.Int && t2 instanceof Type.Char) {
			return true;
		} else if (t1 instanceof Type.Short && t2 instanceof Type.Byte) {
			return true;
		}

		return false;
	} 	
		
	/**
     * This method determines whether two Array types are subtypes or not.
     * Observe that we must follow Java's broken rules on this, depsite the fact
     * that they can lead to runtime type errors.
     * 
     * @param t1
     * @param t2
     * @return
     */
	public boolean subtype(Type.Array t1, Type.Array t2, ClassLoader loader)
			throws ClassNotFoundException {
		if(loader == null) {
			throw new IllegalArgumentException("loader cannot be null");
		}
		if(t1 == null) {
			throw new IllegalArgumentException("t1 cannot be null");
		}
		if(t2 == null) {
			throw new IllegalArgumentException("t2 cannot be null");
		}	
		return subtype(t1.element(), t2.element(), loader);
	}

	/**
	 * This method determines whether t2 is a subtype of t1.
	 * 
	 * @param t1
	 * @param t2
	 * @param loader
	 * @return
	 * @throws ClassNotFoundException
	 */
	public boolean subtype(Type.Clazz t1, Type.Clazz t2, ClassLoader loader)
			throws ClassNotFoundException {
		if(loader == null) {
			throw new IllegalArgumentException("loader cannot be null");
		}
		if(t1 == null) {
			throw new IllegalArgumentException("t1 cannot be null");
		}
		if(t2 == null) {
			throw new IllegalArgumentException("t2 cannot be null");
		}	
		
		// The following is needed to prevent an infinite loop from occuring
		// when checking whether something is a subtype of Object.
		if(isJavaLangObject(t1)) {
			return true;
		}		
		
		Type.Clazz rt = reduce(t1,t2,loader); 				
		
		if(rt != null) {
			return true; // actually, not sufficient;
		}
		
		return false;
	}
	
	public boolean subtype(Type.Intersection t1, Type.Reference t2,
			ClassLoader loader) throws ClassNotFoundException {
		if (loader == null) {
			throw new IllegalArgumentException("loader cannot be null");
		}
		if (t1 == null) {
			throw new IllegalArgumentException("t1 cannot be null");
		}
		if (t2 == null) {
			throw new IllegalArgumentException("t2 cannot be null");
		}

		for (Type.Reference bound : t1.bounds()) {			
			if (!subtype(bound, t2, loader)) {
				return false;
			}
		}

		return true;
	}
	
	public boolean subtype(Type.Reference t1, Type.Intersection t2,
			ClassLoader loader) throws ClassNotFoundException {
		if (loader == null) {
			throw new IllegalArgumentException("loader cannot be null");
		}
		if (t1 == null) {
			throw new IllegalArgumentException("t1 cannot be null");
		}
		if (t2 == null) {
			throw new IllegalArgumentException("t2 cannot be null");
		}

		for (Type.Reference bound : t2.bounds()) {			
			if (!subtype(t1, bound, loader)) {
				return false;
			}
		}

		return true;
	}
	
	public boolean subtype(Type.Reference t1, Type.Wildcard t2,
			ClassLoader loader) throws ClassNotFoundException {
		if (loader == null) {
			throw new IllegalArgumentException("loader cannot be null");
		}
		if (t1 == null) {
			throw new IllegalArgumentException("t1 cannot be null");
		}
		if (t2 == null) {
			throw new IllegalArgumentException("t2 cannot be null");
		}		
		
		if(t1 instanceof Type.Clazz && isJavaLangObject((Type.Clazz)t1)) {
			return true;
		} else if(t1 instanceof Type.Wildcard) {
			Type.Wildcard tw1 = (Type.Wildcard) t1;
			return tw1.lowerBound() == null
					|| subtype(tw1.lowerBound(), t2.lowerBound(), loader);
		} else if(t2.lowerBound() != null){			
			return subtype(t1,t2.lowerBound(),loader);
		}

		return false;
	}
	
	public boolean subtype(Type.Wildcard t1, Type.Reference t2,
			ClassLoader loader) throws ClassNotFoundException {
		if (loader == null) {
			throw new IllegalArgumentException("loader cannot be null");
		}
		if (t1 == null) {
			throw new IllegalArgumentException("t1 cannot be null");
		}
		if (t2 == null) {
			throw new IllegalArgumentException("t2 cannot be null");
		}		
						
		if(t1.upperBound() != null) {			
			return subtype(t1.upperBound(),t2,loader);
		}
		
		return false;
	}
	
	/**
	 * <p>This method determines whether or not type t1 :> t2 under autoboxing.
	 * Thus, it is very similar to the subtype() method above, except that it
	 * also considers autoboxing, whereas subtype() does not.</p>
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	public boolean boxSubtype(Type t1, Type t2, ClassLoader loader)
			throws ClassNotFoundException {
		
		if (t1 instanceof Type.Primitive && isWrapper(t2)) {
			t2 = unboxedType((Type.Clazz) t2);
		} else if (t2 instanceof Type.Primitive && isWrapper(t1)) {
			t1 = unboxedType((Type.Clazz) t1);
		} else if (t1 instanceof Type.Primitive && t2 instanceof Type.Clazz) {
			t1 = boxedType((Type.Primitive)t1);
			return subtype(t1, t2, loader);		
		} else if (t2 instanceof Type.Primitive && t1 instanceof Type.Clazz) {
			t2 = boxedType((Type.Primitive)t2);
			return subtype(t1, t2, loader);		
		} 
		
		return subtype(t1, t2, loader);		
	}
	
	/**
	 * Determine whether or not the given type is a wrapper for a primitive
	 * type.  E.g. java.lang.Integer is a wrapper for int.
	 * 
	 * @param t
	 * @return
	 */
	protected boolean isWrapper(Type t) {
		if(!(t instanceof Type.Clazz)) {
			return false;
		}
		return null != unboxedType((Type.Clazz)t);
	}
	
	/**
	 * Given a primitive wrapper class (i.e. a boxed type), return the unboxed
	 * equivalent. For example, java.lang.Integer yields int, whilst
	 * java.lang.Boolean yields bool.
	 * 
	 * @param p
	 * @return
	 */
	protected Type.Primitive unboxedType(Type.Clazz p) {		
		if (p.pkg().equals("java.lang") && p.components().size() == 1) {
			String type = p.components().get(p.components().size() - 1).first();

			if (type.equals("Boolean")) {
				return T_BOOL;
			} else if (type.equals("Byte")) {
				return T_BYTE;
			} else if (type.equals("Character")) {
				return T_CHAR;
			} else if (type.equals("Short")) {
				return T_SHORT;
			} else if (type.equals("Integer")) {
				return T_INT;
			} else if (type.equals("Long")) {
				return T_LONG;
			} else if (type.equals("Float")) {
				return T_FLOAT;
			} else if (type.equals("Double")) {
				return T_DOUBLE;
			}
		}
		return null;
	}
	
	/**
     * Given a primitive type, determine the equivalent boxed type. For example,
     * the primitive type int yields the type java.lang.Integer. For simplicity
     * in the code using this, it returns in the form a java.Type, rather than a
     * jil.Type.
     * 
     * @param p
     * @return
     */
	public static Type.Reference boxedType(Type.Primitive p) {
		if(p instanceof Type.Bool) {
			return JAVA_LANG_BOOLEAN;
		} else if(p instanceof Type.Byte) {
			return JAVA_LANG_BYTE;
		} else if(p instanceof Type.Char) {
			return JAVA_LANG_CHARACTER;
		} else if(p instanceof Type.Short) {
			return JAVA_LANG_SHORT;
		} else if(p instanceof Type.Int) {
			return JAVA_LANG_INTEGER;
		} else if(p instanceof Type.Long) {
			return JAVA_LANG_LONG;
		} else if(p instanceof Type.Float) {
			return JAVA_LANG_FLOAT;
		} else {
			return JAVA_LANG_DOUBLE;
		}
	}
	
	/**
     * This method builds a binding between a concrete class type, and a
     * "template" type. For example, consider these two types:
     * 
     * <pre>
     *        java.util.ArrayList&lt;String&gt; 
     *        java.util.ArrayList&lt;T&gt;
     * </pre>
     * 
     * Here, the parameterised variant is the "template". The binding produced
     * from these two types would be:
     * 
     * <pre>
     *        T -&gt; String
     * </pre>
     * 
     * Thus, it is a mapping from the generic parameters, to the concrete types
     * that they are instantiated with. This method requires that the concrete
     * and template types are base equivalent.
     * 
     * Finally, a binding is not always constructable. This occurs when an
     * attempt is made to bind one variable to different instantiations. This
     * can occur is some rather strange places.
     * 
     * @param concrete
     *            --- the concrete (i.e. instantiated) type.
     * @param template
     *            --- the template (i.e. having generic parameters) type.
     * @return
     * @throws ---
     *             a BindError if the binding is not constructable.
     */
	public Map<String, Type.Reference> bind(Type concrete, Type template,
			ClassLoader loader) throws ClassNotFoundException {		
		// At this point, we must compute the innerBinding and, from this,
		// determine the final binding
		ArrayList<BindConstraint> constraints = innerBind(concrete,template,loader);		
		HashMap<String,Type.Reference> r = solveBindingConstraints(constraints, loader);					
		return r;
	}
	
	protected static interface BindConstraint {}
	
	protected static class EqualityConstraint implements BindConstraint {
		public Type.Reference type;
		public String var;
		public EqualityConstraint(String rhs, Type.Reference lhs) { 
			this.type = lhs;
			this.var = rhs;
		}
		public String toString() { 
			return var + " = " + type.toString(); 
		}
	}
	/**
	 * A lower bound constraint indicates that the variable in question must be
	 * a subtype of the lower bound given.
	 * 
	 * @author djp
	 * 
	 */
	protected static class LowerBoundConstraint implements BindConstraint {
		public Type.Reference lowerBound;
		public String var;
		public LowerBoundConstraint(String rhs, Type.Reference lhs) { 
			this.lowerBound = lhs;
			this.var = rhs;
		}
		public String toString() { 
			return var + " <: " + lowerBound.toString(); 
		}		
	}
	
	/**
	 * An upper bound constraint indicates that the variable in question must be
	 * a supertype of the upper bound given.
	 * 
	 * @author djp
	 * 
	 */
	protected static class UpperBoundConstraint implements BindConstraint {
		public Type.Reference upperBound;
		public String var;
		public UpperBoundConstraint(String rhs, Type.Reference lhs) { 
			this.upperBound = lhs;
			this.var = rhs;
		}
		public String toString() { 
			return var + " :> " + upperBound.toString() + " <: "; 
		}		
	}
	
	protected ArrayList<BindConstraint> innerBind(Type concrete, Type template,
			ClassLoader loader) throws ClassNotFoundException {		
		if(concrete == null) {
			throw new IllegalArgumentException("concrete cannot be null");
		}
		if(template == null) {
			throw new IllegalArgumentException("template cannot be null");
		}
		if(loader == null) {
			throw new IllegalArgumentException("loader cannot be null");
		}		
		// =====================================================================		
		if (template instanceof Type.Variable
				&& concrete instanceof Type.Reference) {
			// Observe, we can only bind a generic variable to a reference type.						
			return innerBind((Type.Reference) concrete, (Type.Variable) template,
					loader);
		} else if (template instanceof Type.Wildcard) {
			return innerBind(concrete, (Type.Wildcard) template,
					loader);
		} else if (template instanceof Type.Clazz
				&& concrete instanceof Type.Clazz) {
			return innerBind((Type.Clazz) concrete, (Type.Clazz) template, loader);
		} else if (template instanceof Type.Array
				&& concrete instanceof Type.Array) {
			return innerBind((Type.Array) concrete, (Type.Array) template, loader);
		} else {
			return new ArrayList<BindConstraint>();
		}
	}
	
	protected ArrayList<BindConstraint> innerBind(Type.Reference concrete, Type.Variable template,
			ClassLoader loader) throws ClassNotFoundException {
		if(concrete == null) {
			throw new IllegalArgumentException("concrete cannot be null");
		}
		if(template == null) {
			throw new IllegalArgumentException("template cannot be null");
		}
		if(loader == null) {
			throw new IllegalArgumentException("loader cannot be null");
		}
						
		// =====================================================================
		// Ok, we've reached a type variable, so we can now bind this with
		// what we already have.
		ArrayList<BindConstraint> constraints = new ArrayList<BindConstraint>();
						
		constraints.add(new EqualityConstraint(template.variable(),concrete));
	
		if (template.lowerBound() != null
				&& !subtype(template.lowerBound(),concrete, loader)) {				
			throw new BindError("cannot instantiate \"" + template
					+ "\" with \"" + concrete + "\"");
		}				
		
		return constraints;
	}
	
	protected ArrayList<BindConstraint> innerBind(Type.Array concrete, Type.Array template,
			ClassLoader loader) throws ClassNotFoundException {
		return innerBind(concrete.element(),template.element(),loader);
	}
	
	protected ArrayList<BindConstraint> innerBind(Type concrete,
			Type.Wildcard template, ClassLoader loader)
			throws ClassNotFoundException {
		ArrayList<BindConstraint> constraints = new ArrayList();
		
		if (concrete instanceof Type.Wildcard) {
			// Does this case make sense? How can we have a concrete type with a
			// wildcard? Well, a return type being passed straight into a
			// parameter would give rise to one example. Also, a field passed as
			// a parameter.
			Type.Wildcard _concrete = (Type.Wildcard) concrete;
			if (template.upperBound() != null && _concrete.upperBound() != null) {
				constraints.addAll(innerBind(
						_concrete.upperBound(), template.upperBound(), loader));
			}
			if (template.lowerBound() != null && _concrete.lowerBound() != null) {
				constraints.addAll(innerBind(
						_concrete.lowerBound(), template.lowerBound(), loader));
			}
		} else if(concrete instanceof Type.Reference) {
			// This is the complex case. We need to impose bounds on the type.	
			if(template.lowerBound() != null) {
				for (BindConstraint c : innerBind(concrete, template
						.lowerBound(), loader)) {
					
					if(c instanceof EqualityConstraint) {
						EqualityConstraint _c = (EqualityConstraint) c;
						constraints.add(new UpperBoundConstraint(_c.var,_c.type));
					} else {
						// not sure what to do here!
						throw new RuntimeException("internal failure (bind algorithm incomplete!)");
					}
				}
			}
			
			if(template.upperBound() != null) {
				for (BindConstraint c : innerBind(concrete, template
						.upperBound(), loader)) {					
					if(c instanceof EqualityConstraint) {
						EqualityConstraint _c = (EqualityConstraint) c;
						constraints.add(new LowerBoundConstraint(_c.var,_c.type));
					} else {
						// not sure what to do here!
						throw new RuntimeException("internal failure (bind algorithm incomplete!)");
					}
				}
			}
		}
		
		return constraints;
	}
	
	protected ArrayList<BindConstraint> innerBind(Type.Clazz concrete,
			Type.Clazz template, ClassLoader loader) throws ClassNotFoundException {
		if(concrete == null) {
			throw new IllegalArgumentException("concrete cannot be null");
		}
		if(template == null) {
			throw new IllegalArgumentException("template cannot be null");
		}
		if(loader == null) {
			throw new IllegalArgumentException("loader cannot be null");
		}
		// =====================================================================
				
		concrete = reduce(template, concrete, loader);		
		
		ArrayList<BindConstraint> constraints = new ArrayList<BindConstraint>();
		
		if (concrete != null) {			
			for(int i=0;i!=concrete.components().size();++i) {
				Pair<String,List<Type.Reference>> c = concrete.components().get(i);
				Pair<String,List<Type.Reference>> t = template.components().get(i);
				List<Type.Reference> cs = c.second();
				List<Type.Reference> ts = t.second();

				for (int j = 0; j != Math.max(cs.size(), ts.size()); ++j) {					
					if(cs.size() <= j || ts.size() <= j) {
						// This basically means we've got an erased type. So, we
						// must do nothing.
					} else {
						Type.Reference cr = cs.get(j);
						Type.Reference tr = ts.get(j);
						
						if(cr instanceof Type.Clazz && tr instanceof Type.Clazz) {
							// At this point, we need to check the classes
							// actually match.
							Type.Clazz cc = (Type.Clazz) cr;
							Type.Clazz tc = (Type.Clazz) tr;
							List<Pair<String,List<Type.Reference>>> cc_components = cc.components();
							List<Pair<String,List<Type.Reference>>> tc_components = tc.components();
							
							if(cc_components.size() != tc_components.size()) {
								throw new BindError("cannot bind " + concrete
										+ " to " + template);
							}
							
							for(int k=0;k!=cc_components.size();++k) {
								String a = cc_components.get(k).first();
								String b = tc_components.get(k).first();
								if(!a.equals(b)) {
									throw new BindError("cannot bind " + concrete
											+ " to " + template);
								}
							}
							
						}
						
						constraints.addAll(innerBind(cr, tr,
								loader));
					}
				}							
			}
		}
		
		return constraints;
	}
	
	
	/**
	 * This method builds a binding between a concrete function type, and a
	 * "template" type. It works in much the same way as for the bind method on
	 * class types (see above).
	 * 
	 * @param concrete
	 *            --- the concrete (i.e. instantiated) type. Must be non-null
	 * @param template
	 *            --- the template (i.e. having generic parameters) type. Must
	 *            be non-null
	 * @param variableArity
	 *            --- True if the function type has variable arity.
	 * @return
	 * @throws ---
	 *             a BindError if the binding is not constructable.
	 */
	public Map<String, Type.Reference> bind(Type.Function concrete,
			Type.Function template, boolean variableArity, ClassLoader loader)
			throws ClassNotFoundException {
		if (concrete == null) {
			throw new IllegalArgumentException("concrete cannot be null");
		}
		if(template == null) {
			throw new IllegalArgumentException("template cannot be null");
		}
		if(loader == null) {
			throw new IllegalArgumentException("loader cannot be null");
		}					
		
		// Note, I ignore the return type when performing a binding. The reason
		// for this, is that the concrete return type is (always?) taken
		// directly from the template's return type. Thus binding something on
		// itself results in constraints of the form T = T, which can break
		// things.
		ArrayList<BindConstraint> constraints = new ArrayList<BindConstraint>();
		
		// second, do type parameters
		
		List<Type> concreteParams = concrete.parameterTypes();
		List<Type> templateParams = template.parameterTypes();
		
		int paramLength = templateParams.size();
		
		if(variableArity) { paramLength--; }
		
		for(int i=0;i!=Math.min(paramLength,concreteParams.size());++i) {
			Type cp = concreteParams.get(i);
			Type tp = templateParams.get(i);
			constraints.addAll(innerBind(cp,tp,loader));																		
		}
		
		// At this point, we need to consider variable arity methods. 
		if(variableArity && concreteParams.size() >= templateParams.size()) {			
			Type cType = concreteParams.get(paramLength);			
			Type.Array vaType = (Type.Array) templateParams.get(paramLength);		
			
			if(arrayDepth(cType) == arrayDepth(vaType)) {
				// In this situation, we're actually passing the variable arity
				// parameter array in directly, rather than indirectly.  e.g.
				// 
				// <pre>
				// public void test(String... xs) { ... }
				// public void main(String[] args) { test(args) }
				// </pre>
				// Here, args is being passed in directly.
				constraints.addAll(innerBind(cType,vaType,loader));
			} else {
				Type elementType = vaType.element();
				constraints.addAll(innerBind(cType,elementType,loader));							
			}
		}
		
		return solveBindingConstraints(constraints, loader);
	}
	
	/**
	 * This is essentially the heart of the algorithm for solving binding
	 * constraints. Essentially, it employs full unifcation, as well as
	 * computing least upper and greatest lower bounds. It can certainly be
	 * significantly optimised.
	 * 
	 * @param constraints
	 * @param loader
	 * @return
	 * @throws ClassNotFoundException
	 */
	protected HashMap<String, Type.Reference> solveBindingConstraints(
			ArrayList<BindConstraint> constraints, ClassLoader loader)
			throws ClassNotFoundException {
		HashMap<String, Type.Reference> binding = new HashMap();
		HashSet<String> fixed = new HashSet<String>();
		HashSet<String> lower = new HashSet<String>();
		
		// First, go through and eliminate all simple equality constraints
		// To optimise this properly, we need to use a proper fast union find
		// algorithm for performing the unification.
		
		for(int i=0;i!=constraints.size();++i) {
			if(constraints.get(i) instanceof EqualityConstraint) {
				EqualityConstraint c = (EqualityConstraint) constraints.get(i);
				Type.Reference oldVal = binding.get(c.var);
				if(oldVal != null && !oldVal.equals(c.type)) {
					throw new BindError("cannot bind " + c.var
							+ " to " + oldVal + " and " + c.type);
				} else {
					binding.put(c.var,c.type);
					fixed.add(c.var);
				}									 				
			}
		}
		
		// Second, apply all lower bound constraints 
		for(int i=0;i!=constraints.size();++i) {
			if(constraints.get(i) instanceof LowerBoundConstraint) {
				LowerBoundConstraint c = (LowerBoundConstraint) constraints.get(i);				
				Type.Reference oldVal = binding.get(c.var);
				if(oldVal != null) {
					if (!subtype(c.lowerBound, oldVal, loader)) {	
						if(fixed.contains(c.var)) {
							throw new BindError("cannot bind " + c.var
									+ " to " + oldVal + " and " + c.lowerBound);
						} else {
							// Ok, try to improve the lower bound then
							Type.Reference newLowerBound = leastSubtype(c.lowerBound,oldVal,loader);

							if(newLowerBound == null) {
								throw new BindError("cannot bind " + c.var
										+ " to " + oldVal + " and " + c.lowerBound);
							} else {
								lower.add(c.var);
								binding.put(c.var, newLowerBound);
							}
						}
					} 
				} else {
					binding.put(c.var,c.lowerBound);						
				}									 				
			}
		}
		
		fixed.addAll(lower);
		
		// Finally, apply all upper bound constraints 
		for(int i=0;i!=constraints.size();++i) {
			if(constraints.get(i) instanceof UpperBoundConstraint) {
				UpperBoundConstraint c = (UpperBoundConstraint) constraints.get(i);					
				Type.Reference oldVal = binding.get(c.var);
				if(oldVal != null) {
					if (!subtype(oldVal, c.upperBound, loader)) {	
						if(fixed.contains(c.var)) {
							throw new BindError("cannot bind " + c.var
									+ " to " + oldVal + " and " + c.upperBound);
						} else {
							// Ok, try to improve the lower bound then
							Type.Reference newUpperBound = greatestSupertype(c.upperBound,oldVal,loader);

							if(newUpperBound == null) {
								throw new BindError("cannot bind " + c.var
										+ " to " + oldVal + " and " + c.upperBound);
							} else {
								binding.put(c.var, newUpperBound);
							}
						}
					} 
				} else {
					binding.put(c.var,c.upperBound);						
				}									 				
			}
		}
		
		return binding;
	}
	
	public Type.Reference leastSubtype(Type.Reference t1, Type.Reference t2,
			ClassLoader loader) {
		return t1;
	}
	
	/**
	 * Given two types t1 and t2, compute the common supertype t3, such that t3 >=
	 * t2, t3 >= t1 and there is no other common super type t4 where t3 >= t4.
	 * 
	 * @param t1
	 * @param t2
	 * @param loader
	 * @return
	 * @throws ClassNotFoundException
	 */
	public Type.Reference greatestSupertype(Type.Reference t1, Type.Reference t2,
			ClassLoader loader) throws ClassNotFoundException {		
		Set<Type.Reference> t1supertypes = listSupertypes(t1,loader);
		Set<Type.Reference> t2supertypes = listSupertypes(t2,loader);
	
		// An interesting question here, is whether we need to use an
		// intersection type to deal with the possibility of multiple possible
		// subtypes.
		
		
		ArrayList<Type.Reference> candidates = new ArrayList<Type.Reference>();
		
		for(Type.Reference t1s : t1supertypes) {
			outer: for(Type.Reference t2s : t2supertypes) {
				if(t1s.equals(t2s)) {
					candidates.add(t1s);
				} else if(t1s instanceof Type.Clazz && t2s instanceof Type.Clazz) {
					Type.Clazz t1c = (Type.Clazz) t1s;
					Type.Clazz t2c = (Type.Clazz) t2s;
					
					if (t1c.pkg().equals(t2c.pkg())
							&& t1c.components().size() == t2c.components()
									.size()) {
						List<Pair<String,List<Type.Reference>>> t1c_components = t1c.components();
						List<Pair<String,List<Type.Reference>>> t2c_components = t2c.components();
						for(int i=0;i!=t1c_components.size();++i) {
							String n1 = t1c_components.get(i).first();
							String n2 = t2c_components.get(i).first();
							if(!n1.equals(n2)) {
								continue outer;
							}
						}
						candidates.add(t1s);
						candidates.add(t2s);
					}
				}  
			}
		}
		
		Type.Reference st = null;
		for(Type.Reference c : candidates) {
			if(subtype(c,t1,loader) && subtype(c,t2,loader)) {
				if(st == null || subtype(st,c,loader)) {
					st = c;
				} 
			}			
		}		
		
		return st;
	}
		
	public static class BindError extends RuntimeException {
		public BindError(String m) {
			super(m);
		}
	}				
		
	/**
	 * The aim of this method is to identify the super types of t1. For example,
	 * if:
	 * 
	 * <pre>
	 * t1 = java.util.ArrayList&lt;String&gt;
	 * </pre>
	 * 
	 * Then, the resulting list looks like this:
	 * 
	 * <pre>
	 * java.util.ArrayList&lt;String&gt;
	 * java.util.AbstractList&lt;String&gt;
	 * java.io.Serializable
	 * java.lang.Cloneable
	 * java.lang.Iterable&lt;String&gt;
	 * java.util.List&lt;String&gt;
	 * java.util.RandomAccess
	 * java.util.AbstractList&lt;String&gt;
	 * java.util.AbstractCollection&lt;String&gt;
	 * java.lang.Object
	 * </pre>
	 * 
	 * @param t1
	 *            --- type whose supertypes we're interested in.
	 * @param loader
	 *            --- ClassLoader. Needed for loading classes to allow traversal
	 *            of the class heirarchy
	 * @return
	 * @throws ClassNotFoundException
	 */
	public HashSet<Type.Reference> listSupertypes(Type.Reference t1,
			ClassLoader loader) throws ClassNotFoundException {
		HashSet<Type.Reference> types = new HashSet();		
		
		if(t1 instanceof Type.Array) {
			types.add(t1);
			types.add(JAVA_LANG_OBJECT);
			return types;
		} else {

			LinkedList<Type.Clazz> worklist = new LinkedList();
			worklist.add((Type.Clazz) t1);

			while(!worklist.isEmpty()) {
				Type.Clazz type = worklist.removeFirst(); // to ensure BFS traversal
				types.add(type);
				Clazz c = loader.loadClass(type);

				// The current type we're visiting is not a match. Therefore, we
				// need to explore its supertypes as well. A key issue
				// in doing this, is that we must preserve the appropriate types
				// according to the class declaration in question. For example,
				// suppose we're checking:
				// 
				//         subtype(List<String>,ArrayList<String>)
				// 
				// then, we'll start with ArrayList<String> and we'll want to move
				// that here to be List<String>. The key issue is what determines
				// how we decide what the appropriate generic parameters for List
				// should be. To do that, we must look at the declaration for class
				// ArrayList, where we'll notice something like this:
				//
				// <pre> 
				// class ArrayList<T> implements List<T> { ... }
				// </pre>
				// 
				// We need to use this template --- namely that the first generic
				// parameter of ArrayList maps to the first of List --- in order to
				// determine the proper supertype for ArrayList<String>. This is
				// what the binding / substitution stuff is for.			
				Map<String,Type.Reference> binding = bind(type, c.type(),loader);			
				if (c.superClass() != null) {				
					worklist.add((Type.Clazz) Types.substitute(c.superClass(), binding));
				}
				for (Type.Clazz t : c.interfaces()) {
					worklist.add((Type.Clazz) Types.substitute(t, binding));				
				}			
			}

			return types;
		}
	}
	
	/**
	 * The aim of this method is to identify the methods which override the given method
	 * 
	 * @param loader
	 *            --- ClassLoader. Needed for loading classes to allow traversal
	 *            of the class heirarchy
	 * @return
	 * @throws ClassNotFoundException
	 */
	public ArrayList<Triple<Clazz,Clazz.Method,Type.Function>> listOverrides(Type.Clazz owner, String name,
			Type.Function funType, ClassLoader loader)
			throws ClassNotFoundException {
		funType = Types.stripGenerics(funType);		
		ArrayList<Triple<Clazz,Clazz.Method,Type.Function>> methods = new ArrayList();

		LinkedList<Type.Clazz> worklist = new LinkedList();
		worklist.add((Type.Clazz) owner);

		while (!worklist.isEmpty()) {
			Type.Clazz type = worklist.removeFirst(); // to ensure BFS
                                                      // traversal			
			Clazz c = loader.loadClass(type);												

			Map<String,Type.Reference> binding = bind(type,c.type(), loader);
						
			if(!type.equals(owner)) {
				for(Clazz.Method m : c.methods(name)) {																
					Type.Function mtype = Types.stripGenerics(substitute(m.type(),binding));					
					if(mtype.parameterTypes().equals(funType.parameterTypes())) {						
						methods.add(new Triple(c,m,mtype));
					}
				}
			}
			
			if (c.superClass() != null) {
				worklist.add((Type.Clazz) substitute(c.superClass(),binding));
			}
			for (Type.Clazz t : c.interfaces()) {				
				worklist.add((Type.Clazz) substitute(t,binding));
			}			
		}

		return methods;		
	}
	
	/**
     * This method checks whether the two types in question have the same base
     * components. So, for example, ArrayList<String> and ArrayList<Integer>
     * have the same base component --- namely, ArrayList.
     * 
     * @param t
     * @return
     */
	protected boolean baseEquivalent(Type.Clazz t1, Type.Clazz t2) {
		List<Pair<String, List<Type.Reference>>> t1components = t1.components();
		List<Pair<String, List<Type.Reference>>> t2components = t2.components();

		// first, check they have the same number of components.
		if(t1components.size() != t2components.size()) {
			return false;
		}
		
		// second, check each component in turn
		for(int i=0;i!=t1components.size();++i) {
			String t1c = t1components.get(i).first();
			String t2c = t2components.get(i).first();
			if(!t1c.equals(t2c)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * The aim of this method is to reduce type t2 to the level of type t1,
	 * whilst applying any and all substitions as necessary. For example,
	 * suppose we have:
	 * 
	 * <pre>
	 * t1 = java.util.Collection&lt;T&gt;
	 * t2 = java.util.ArrayList&lt;String&gt;
	 * </pre>
	 * 
	 * Here, we want to reduce ArrayList to Collection applying all the implied
	 * subsitutions (which in this case is easy enough).
	 * 
	 * Observe that, if this method cannot find a reduced type (i.e. returns
	 * null), then we know that t2 is not a subtype of t1.
	 * 
	 * @param t1
	 *            --- type to reduce to
	 * @param t2
	 *            --- type to be reduced
	 * @return the reduced type, or null if there is none.
	 */
	protected Type.Clazz reduce(Type.Clazz t1, Type.Clazz t2, ClassLoader loader)
			throws ClassNotFoundException {
		ArrayList<Type.Clazz> worklist = new ArrayList<Type.Clazz>();
		
		worklist.add(t2);
		
		// Ok, so the idea behind the worklist is to start from type t2, and
        // proceed up the class heirarchy visiting all supertypes (i.e. classes
        // + interfaces) of t2 until either we reach t1, or java.lang.Object.		
		while(!worklist.isEmpty()) {
			Type.Clazz type = worklist.remove(worklist.size() - 1);						
			
			if(baseEquivalent(type, t1)) {				
				return type;
			}									
			
			Clazz c = loader.loadClass(type);
						
			// The current type we're visiting is not a match. Therefore, we
            // need to explore its supertypes as well. A key issue
            // in doing this, is that we must preserve the appropriate types
            // according to the class declaration in question. For example,
            // suppose we're checking:
			// 
			//         subtype(List<String>,ArrayList<String>)
			// 
			// then, we'll start with ArrayList<String> and we'll want to move
            // that here to be List<String>. The key issue is what determines
            // how we decide what the appropriate generic parameters for List
            // should be. To do that, we must look at the declaration for class
            // ArrayList, where we'll notice something like this:
			//
			// <pre> 
			// class ArrayList<T> implements List<T> { ... }
			// </pre>
			// 
			// We need to use this template --- namely that the first generic
            // parameter of ArrayList maps to the first of List --- in order to
            // determine the proper supertype for ArrayList<String>. This is
            // what the binding / substitution stuff is for.			
			Map<String,Type.Reference> binding = bind(type, c.type(),loader);			
			if (c.superClass() != null) {
				worklist.add((Type.Clazz) substitute(c.superClass(), binding));
			}
			for (Type.Clazz t : c.interfaces()) {
				worklist.add((Type.Clazz) substitute(t, binding));				
			}			
		}
		
		// this indicates that when traversing the heirarchy from t2, we did not
		// encounter t1
		return null;
	}
	
	/**
	 * Identify whether or not there is a method with the given name in the
	 * receiver class. Traverse the class hierarchy if necessary to answer this.
	 * 
	 * @param receiver
	 * @param name
	 * @return
	 */
	public boolean hasMethod(Type.Clazz receiver, String name,
			ClassLoader loader) throws ClassNotFoundException {
		if(loader == null) {
			throw new IllegalArgumentException("loader cannot be null");
		}
		if(name == null) {
			throw new IllegalArgumentException("name cannot be null");
		}
		if(receiver == null) {
			throw new IllegalArgumentException("receiver cannot be null");
		}		
		
		Stack<Type.Clazz> worklist = new Stack<Type.Clazz>();
		worklist.push(receiver);
		
		while(!worklist.isEmpty()) {
			receiver = worklist.pop();
			Clazz c = loader.loadClass(receiver);
			if(c.methods(name).size() > 0) {
				return true;
			}
			if(c.superClass() != null) {
				worklist.push(c.superClass());
			}
			for (Type.Clazz t : c.interfaces()) {
				worklist.add(t);				
			}
		}
		
		return false;
	}
	
	/**
	 * Identify the method with the given name in the given clazz that matches
	 * the given method signature.
	 * 
	 * @param receiver
	 *            enclosing class
	 * @param name
	 *            Method name
	 * @param concreteParameterTypes 
	 *            The actual parameter types to match against.  Must be non-null  
	 * @return A triple (C,M,T), where M is the method being invoked, C it's
	 *         enclosing class, and T is the actual type of the method. Note
	 *         that T can vary from M.type, since it may contain appropriate
	 *         substitutions for any generic type variables.
	 * @throws ClassNotFoundException
	 *             If it needs to access a class which cannot be found.
	 * @throws MethodNotFoundException
	 *             If it cannot find a matching method.
	 */
	public Triple<Clazz, Clazz.Method, Type.Function> resolveMethod(
			Type.Reference receiver, String name,
			List<Type> concreteParameterTypes, ClassLoader loader)
			throws ClassNotFoundException, MethodNotFoundException {
		
		if(loader == null) {
			throw new IllegalArgumentException("loader cannot be null");
		}
		if(name == null) {
			throw new IllegalArgumentException("name cannot be null");
		}
		if(receiver == null) {
			throw new IllegalArgumentException("receiver cannot be null");
		}
		if(concreteParameterTypes == null) {
			throw new IllegalArgumentException("concreteParameterTypes cannot be null");
		}		
						
		// Phase 1: traverse heirarchy whilst ignoring autoboxing and varargs
		Triple<Clazz, Clazz.Method, Type.Function> methodInfo = resolveMethod(
				receiver, name, concreteParameterTypes, false, false, loader);

		if (methodInfo == null) {
			// Phase 2: Ok, phase 1 failed, so now consider autoboxing.
			methodInfo = resolveMethod(receiver, name, concreteParameterTypes,
					true, false, loader);

			if (methodInfo == null) {
				// Phase 3: Ok, phase 2 failed, so now consider var args as well.
				methodInfo = resolveMethod(receiver, name, concreteParameterTypes,
						true, true, loader);
				if(methodInfo == null) {
					// Ok, phase 3 failed, so give up.
					String method = name + "(";
					boolean firstTime = true;
					for (Type p : concreteParameterTypes) {
						if (!firstTime) {
							method += ", ";
						}
						method += p.toString();
						firstTime = false;
					}
					throw new MethodNotFoundException(method + ")", receiver
							.toString());
				}
			}
		}
						
		return methodInfo;
	}

	/**
	 * <p>
	 * Attempt to determine which method is actually being called. This process
	 * is rather detailed, and you should refer to the <a
	 * href="http://java.sun.com/docs/books/jls/third_edition/html/expressions.html#15.12">Java
	 * Language Spec, Section 15.12</a>.
	 * </p>
	 * 
	 * <p>
	 * This method supports the three phases described in the JLS#15.12 through
	 * the two boolean flags: <code>autoboxing</code> and <code>varargs</code>.
	 * These flags indicate that the concept they represent should be considered
	 * in resolution. In phase 1, following the JLS, neither concepts are
	 * considered; in Phase 2, only autoboxing is considered; finally, in Phase
	 * 3, both autoboxing and variable length argument lists are considered.
	 * </p>
	 * 
	 * @param receiver
	 *            Method Receiver Type
	 * @param name
	 *            Method name
	 * @param concreteParameterTypes
	 *            Parameter types to search for.
	 * @param autoboxing
	 *            Indicates whether autoboxing should be considered or not.
	 * @param varargs
	 *            Indicates whether variable-length arguments should be
	 *            considered or not.
	 * @return
	 * @throws ClassNotFoundException
	 */
	protected Triple<Clazz, Clazz.Method, Type.Function> resolveMethod(
			Type.Reference receiver, String name,
			List<Type> concreteParameterTypes, boolean autoboxing,
			boolean varargs, ClassLoader loader) throws ClassNotFoundException {				
				
		if(receiver instanceof Type.Clazz) {			
			return resolveMethod((Type.Clazz) receiver, name,
					concreteParameterTypes, autoboxing, varargs, loader);
		} else if(receiver instanceof Type.Intersection) {
			Type.Intersection it = (Type.Intersection) receiver;
			for(Type.Reference b : it.bounds()) {
				Triple<Clazz, Clazz.Method, Type.Function> r = resolveMethod(b, name,
						concreteParameterTypes, autoboxing, varargs, loader);
				if(r != null) {
					return r;
				}
			}
		} else if(receiver instanceof Type.Wildcard) {
			
		} 
		
		// failure
		return null;		
	}

	protected Triple<Clazz, Clazz.Method, Type.Function> resolveMethod(
			Type.Clazz receiver, String name,
			List<Type> concreteParameterTypes, boolean autoboxing,
			boolean varargs, ClassLoader loader) throws ClassNotFoundException {				

		// traverse class hierarchy looking for field
		ArrayList<Type.Clazz> worklist = new ArrayList<Type.Clazz>();
		worklist.add(receiver);
		
		ArrayList<Triple<Clazz, Clazz.Method, Type.Function>> mts = new ArrayList();
			
		// Traverse type hierarchy building a list of potential methods
		while (!worklist.isEmpty()) {
			Type.Clazz type = worklist.remove(0);
			Clazz c = loader.loadClass(type);			
			List<? extends Clazz.Method> methods = c.methods(name);						
			
			Map<String,Type.Reference> binding = bind(type, c.type(), loader);		
												
			for (Clazz.Method m : methods) {
				// try to rule out as many impossible candidates as possible
				Type.Function m_type = m.type();												
				
				if (m_type.parameterTypes().size() == concreteParameterTypes
						.size()
						|| (varargs && m.isVariableArity() && m_type
								.parameterTypes().size() <= (concreteParameterTypes
								.size() + 1))) {														
					
					// First, substitute class type parameters							
					Type.Function mt = (Type.Function) substitute(m_type, binding);														
					
					// Second, substitute method type parameters
					Type.Function concreteFunctionType = new Type.Function(mt.returnType(),
							concreteParameterTypes, new ArrayList<Type.Variable>());										
												
					try {
						mt = (Type.Function) substitute(mt, bind(
							concreteFunctionType, mt, m.isVariableArity(),
							loader));				
																
						mts.add(new Triple<Clazz, Clazz.Method, Type.Function>(c, m, mt));
					} catch(BindError e) {
						// don't need to do anything. This just indicates that
						// the current method is not a candidate.
																	
					}										 			
				}
			}

			if (c.superClass() != null) {				
				worklist.add((Type.Clazz) substitute(c.superClass(),binding));				
			}

			for (Type.Reference t : c.interfaces()) {
				worklist.add((Type.Clazz) substitute(t,binding));
			}
		}

		// Find target method
		return matchMethod(concreteParameterTypes, mts, autoboxing, loader);
	}
	
	/**
	 * The problem here is, given a list of similar functions, to select the
	 * most appropriate match for the given parameter types. If there is no
	 * appropriate match, simply return null
	 */
	protected Triple<Clazz, Clazz.Method, Type.Function> matchMethod(
			List<Type> parameterTypes,
			List<Triple<Clazz, Clazz.Method, Type.Function>> methods,
			boolean autoboxing, ClassLoader loader)
			throws ClassNotFoundException {
	
		int matchIndex = -1;
		// params contains the original parameter types we're looking for.
		Type[] params = parameterTypes.toArray(new Type[parameterTypes.size()]);
		// nparams contains the best match we have so far.
		Type[] nparams = null;

		outer: for (int i = methods.size() - 1; i >= 0; --i) {
			Triple<Clazz, Clazz.Method, Type.Function> methInfo = methods.get(i);
			Clazz.Method m = methInfo.second();
									
			Type.Function f = methInfo.third();			
			Type[] mps = f.parameterTypes().toArray(new Type[f.parameterTypes().size()]);
			if (mps.length == params.length
					|| (m.isVariableArity() && mps.length <= (params.length + 1))) {
				// check each parameter type.
				int numToCheck = m.isVariableArity() ? mps.length - 1
						: mps.length;
				
				for (int j = 0; j != numToCheck; ++j) {
					Type p1 = mps[j];
					Type p2 = params[j];
									
					if (!(autoboxing && boxSubtype(p1, p2, loader))
							&& !subtype(p1, p2, loader)) {																	
						continue outer;
					}
					
					if (nparams != null && !subtype(nparams[j], p1, loader)) {
						continue outer;
					}
				}
				
				// At this point, if the method is a variable arity method we
				// need to also check that the varargs portion make sense.
				if(m.isVariableArity()) {					
					Type.Array arrayType = (Type.Array) mps[numToCheck];
					Type elementType = arrayType.element();					
					if(numToCheck == (params.length-1)) {
						// In the special case that just one parameter is
						// provided in a variable arity position, we need to
						// check whether or not it is an array of the
						// appropriate type.
						Type p2 = params[numToCheck];
						if (!(autoboxing && boxSubtype(elementType, p2, loader))
								&& !subtype(elementType, p2, loader)
								&& !(autoboxing && boxSubtype(arrayType, p2,
										loader))
								&& !subtype(arrayType, p2, loader)) {
							continue outer;
						}
					} else {
						// This is the normal situation. We need to check
						// whether or not the arguments provided in the variable
						// arity positions are subtypes of the variable arity
						// list element type.
						for(int j=numToCheck;j<params.length;++j) {
							Type p2 = params[j];						
							if (!(autoboxing && boxSubtype(elementType, p2,
									loader))
									&& !subtype(elementType, p2, loader)) {
								continue outer;
							}
						}
					}
				}
				matchIndex = i;
				nparams = mps;
			}
		}

		if (matchIndex == -1) {
			// No method was found			
			return null;
		} else {
			return methods.get(matchIndex);
		}
	}

	/**
	 * Identify the field with the given name in the given clazz.
	 * 
	 * @param owner
	 *            enclosing class.  Must be non-null
	 * @param name
	 *            Field name.  Must be non-null
	 * @return (C,F,T) where C is the enclosing class, F is the field being
	 *         accessed, and T is type of that field with appropriate type
	 *         subsititions based on the owner reference given.
	 * @throws ClassNotFoundException
	 *             If it needs to access a Class which cannot be found.
	 * @throws FieldNotFoundException
	 *             If it cannot find the field in question
	 */
	public Triple<Clazz, Clazz.Field, Type> resolveField(Type.Clazz owner,
			String name, ClassLoader loader) throws ClassNotFoundException,
			FieldNotFoundException {
		if(loader == null) {
			throw new IllegalArgumentException("loader cannot be null");
		}
		if(name == null) {
			throw new IllegalArgumentException("name cannot be null");
		}
		if(owner == null) {
			throw new IllegalArgumentException("receiver cannot be null");
		}	
		// traverse class hierarchy looking for field
		ArrayList<Type.Clazz> worklist = new ArrayList<Type.Clazz>();
		worklist.add(owner);
		while (!worklist.isEmpty()) {
			Type.Clazz type = worklist.remove(worklist.size() - 1);			
			Clazz c = loader.loadClass(type);			
			Map<String,Type.Reference> binding = bind(type, c.type(), loader);
			Clazz.Field f = c.field(name);
			
			if (f != null) {
				// found it!
				Type fieldT = f.type();
				if(fieldT instanceof Type.Reference) {
					fieldT = substitute((Type.Reference) f.type(), binding);
				}
				return new Triple<Clazz, Clazz.Field, Type>(c, f, fieldT);
			}
			// no match yet, so traverse super class and interfaces
			if (c.superClass() != null) {				
				worklist.add((Type.Clazz) substitute(c.superClass(),binding));
			}
			for (Type.Reference t : c.interfaces()) {				
				worklist.add((Type.Clazz) substitute(t,binding));
			}
		}

		throw new FieldNotFoundException(name, owner.toString());
	}
}
