package jkit.java.stages;

import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.jil.Type;
import jkit.jil.Clazz;
import jkit.util.Pair;

/**
 * This method contains a variety of useful algorithms for deal with Java's type
 * system.
 * 
 * @author djp
 */
public class TypeSystem {
	
	/**
     * This method determines whether t1 :> t2; that is, whether t2 is a subtype
     * of t1 or not, following the class heirarchy. Observe that this relation
     * is reflexive, transitive and anti-symmetric:
     * 
     * 1) t1 :> t1 always holds
     * 2) if t1 :> t2 and t2 :> t3, then t1 :> t3
     * 3) if t1 :> t2 then not t2 :> t1 (unless t1 == t2)
     * 
     * @param t1
     * @param t2
     * @return
     * @throws ClassNotFoundException
     */
	public boolean subtype(Type t1, Type t2, ClassLoader loader)
			throws ClassNotFoundException {
		
		// First, do the easy cases where things are symmetrical.
		if(t1 instanceof Type.Clazz && t2 instanceof Type.Clazz) {
			return subtype((Type.Clazz) t1, (Type.Clazz) t2, loader);
		} else if(t1 instanceof Type.Primitive && t2 instanceof Type.Primitive) {
			return subtype((Type.Primitive) t1, (Type.Primitive) t2);
		} else if(t1 instanceof Type.Array && t2 instanceof Type.Array) {
			return subtype((Type.Array) t1, (Type.Array) t2, loader);
		} 
		
		// Now, we have to do the harder cases.
		
		
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
		if(t1 instanceof Type.Double && subtype(new Type.Float(),t2)) { 
			return true;
		} else if(t1 instanceof Type.Float && subtype(new Type.Long(),t2)) {
			return true;
		} else if(t1 instanceof Type.Long && subtype(new Type.Int(),t2)) {
			return true;
		} else if(t1 instanceof Type.Int && subtype(new Type.Short(),t2)) {
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
		return subtype(t1.element(), t2.element(), loader);
	}

	/**
     * This method determines whether two Clazz types are subtypes or not.
     * 
     * @param t1
     * @param t2
     * @param loader
     * @return
     * @throws ClassNotFoundException
     */
	public boolean subtype(Type.Clazz t1, Type.Clazz t2, ClassLoader loader)
			throws ClassNotFoundException {
		ArrayList<Type.Clazz> worklist = new ArrayList<Type.Clazz>();
		
		worklist.add(t2);
		
		// Ok, so the idea behind the worklist is to start from type t2, and
        // proceed up the class heirarchy visiting all supertypes (i.e. classes
        // + interfaces) of t2 until either we reach t1, or java.lang.Object.
		while(!worklist.isEmpty()) {
			Type.Clazz type = worklist.remove(worklist.size() - 1);

			if(type.equals(t1)) {
				return true;
			} else if(baseEquivalent(type, t1)) {
				// at this point, we have reached a type with the same base
                // components as t1, but they are not identical. We now have to
                // check wether or not any types in the generic parameter
                // position are compatible or not.
				return true; // TEMPORARY
			}
			
			Clazz c = loader.loadClass(type);
			System.out.println("GOT HERE");
			
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
			Map<String,Type.Reference> binding = bind(type, c.type());
			
			if (c.superClass() != null) {
				worklist.add((Type.Clazz) substitute(c.superClass(), binding));
			}
			for (Type.Clazz t : c.interfaces()) {
				worklist.add((Type.Clazz) substitute(t, binding));
			}			
		}
		
		return false;
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
	public Map<String, Type.Reference> bind(Type.Reference concrete,
			Type.Reference template) {
		if(template instanceof Type.Clazz && concrete instanceof Type.Clazz && !baseEquivalent((Type.Clazz)concrete,(Type.Clazz)template)) {
			throw new IllegalArgumentException(
					"Parameters to TypeSystem.bind() are not base equivalent ("
							+ concrete + ", " + template + ")");
		}
		
		HashMap<String, Type.Reference> binding = new HashMap<String, Type.Reference>();
		innerBind(concrete, template, binding);
		return binding;
	}
	
	public class BindError extends RuntimeException {
		public BindError(String m) {
			super(m);
		}
	}
	
	/**
	 * A helper method for bind()
	 * 
	 * @param concrete
	 * @param template
	 * @return
	 */
	private void innerBind(Type.Reference concrete, Type.Reference template,
			HashMap<String, Type.Reference> binding) {
		
		if (template instanceof Type.Variable) {
			// Ok, we've reached a type variable, so we can now bind this with
			// what we already have.
			Type.Variable v = (Type.Variable) template;
			Type.Reference r = binding.get(v.variable());
			if (r != null && !r.equals(concrete)) {
				// in this case, a sound binding cannot be constructed, so we
				// must throw an error.
				throw new BindError("attempt to bind variable " + v
						+ " to both " + r + " and " + concrete);
			} else if(r == null) {
				binding.put(v.variable(), concrete);
			}
		} else if(template instanceof Type.Wildcard) {
			// NEED TO HANDLE LOWER AND UPPER BOUNDS.
			
		} else if(template instanceof Type.Clazz) {
			if(!(concrete instanceof Type.Clazz)) {
				throw new BindError("Cannot bind " + concrete + " to " + template);
			}
			
			Type.Clazz cc = (Type.Clazz) concrete;
			Type.Clazz tc = (Type.Clazz) template;			
			
			if(cc.components().size() != tc.components().size()) {
				throw new BindError("Cannot bind " + concrete + " to " + template);
			}
			
			for(int i=0;i!=cc.components().size();++i) {
				Pair<String,List<Type.Reference>> c = cc.components().get(i);
				Pair<String,List<Type.Reference>> t = tc.components().get(i);
				List<Type.Reference> cs = c.second();
				List<Type.Reference> ts = t.second();
				
				// this maybe too strict for erased types.
				if(!c.first().equals(t.first()) || cs.size() != ts.size()) {
					throw new BindError("Cannot bind " + concrete + " to " + template);
				}
				
				for(int j=0;j!=cs.size();++j) {
					innerBind(cs.get(j),ts.get(j),binding);	
				}							
			}
			
		} else if(template instanceof Type.Array) {
			if(!(concrete instanceof Type.Array)) {
				throw new BindError("Cannot bind " + concrete + " to " + template);
			}
			
			Type.Array ca = (Type.Array) concrete;
			Type.Array ta = (Type.Array) template;
			
			innerBind(ca,ta, binding);
		} 
	}
	
	/**
     * This method accepts a binding from type variables to concrete types, and
     * then substitutes each such variable occuring in the target type with its
     * corresponding instantation. For example, suppose we have this binding:
     * 
     * <pre>
     *  K -&gt; String
     *  V -&gt; Integer
     * </pre>
     * 
     * Then, substituting against <code>HashMap<K,V></code> yields
     * <code>HashMap<String,Integer></code>.
     * 
     * @param type
     * @param binding
     * @return
     */
	protected Type.Reference substitute(Type.Reference type, Map<String,Type.Reference> binding) {
		if (type instanceof Type.Variable) {
			// Ok, we've reached a type variable, so we can now bind this with
			// what we already have.
			Type.Variable v = (Type.Variable) type;
			Type.Reference r = binding.get(v.variable());
			if(r == null) {
				// if the variable is not part of the binding, then we simply do
                // not do anything with it.
				return v;
			} else {
				return r;
			}
		} else if(type instanceof Type.Wildcard) {
			Type.Wildcard wc = (Type.Wildcard) type;
			Type.Reference lb = wc.lowerBound();
			Type.Reference ub = wc.upperBound();
			if(lb != null) { lb = substitute(lb,binding); }
			if(ub != null) { ub = substitute(ub,binding); }
			return new Type.Wildcard(lb,ub);
		} else if(type instanceof Type.Array) {
			Type.Array at = (Type.Array) type;
			if(at.element() instanceof Type.Reference) {
				return new Type.Array(substitute((Type.Reference) at.element(),binding));
			} else {
				return type;
			}
		} else if(type instanceof Type.Clazz) {
			Type.Clazz ct = (Type.Clazz) type;
			ArrayList<Pair<String,List<Type.Reference>>> ncomponents = new ArrayList();
			List<Pair<String,List<Type.Reference>>> components = ct.components();
			
			for(Pair<String,List<Type.Reference>> c : components) {
				ArrayList<Type.Reference> nc = new ArrayList<Type.Reference>();
				for(Type.Reference r : c.second()) {
					nc.add(substitute(r,binding));
				}
				ncomponents.add(new Pair(c.first(),nc));
			}
			
			return new Type.Clazz(ct.pkg(),ncomponents);
		}
		
		throw new BindError("Cannot substitute against type " + type);
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
}
