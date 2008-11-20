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

package jkit.core;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import jkit.util.*;
import jkit.util.bytecode.Types;

/**
 * The ClassTable stores the hierarchy of Java Classes. Through this class you
 * can find/load classes, resolve packages (given an import list), resolve
 * methods based on parameter types and more.
 * 
 * @author djp
 * 
 */
public class ClassTable {
	/**
	 * The class loader is used to load classes appropriately
	 */
	private static ClassLoader loader;
	
	public static void setLoader(ClassLoader l) {
		loader = l;
	}		

	/**
	 * Find the Clazz object for the given class. This may require searching the
	 * CLASSPATH and loading the class.
	 * 
	 * @param className
	 * @param pkg
	 * @return
	 * @throws ClassNotFoundException
	 *             If it needs to access a Class which cannot be found.
	 */
	public static Clazz findClass(Type.Reference ref)
			throws ClassNotFoundException {
		// THIS METHOD IS CURRENTLY KEPT ONLY FOR BACKWARDS COMPATIBILITY. IT IS
		// GOING TO BE REMOVED.
		return loader.loadClass(ref);
	}

	/**
	 * Identify the field with the given name in the given clazz.
	 * 
	 * @param owner
	 *            enclosing class
	 * @param name
	 *            Field name
	 * @return (C,F,T) where C is the enclosing class, F is the field being
	 *         accessed, and T is type of that field with appropriate type
	 *         subsititions based on the owner reference given.
	 * @throws ClassNotFoundException
	 *             If it needs to access a Class which cannot be found.
	 * @throws FieldNotFoundException
	 *             If it cannot find the field in question
	 */
	public static Triple<Clazz, Field, Type> resolveField(Type.Reference owner,
			String name) throws ClassNotFoundException, FieldNotFoundException {
		// traverse class hierarchy looking for field
		ArrayList<Type.Reference> worklist = new ArrayList<Type.Reference>();
		worklist.add(owner);
		while (!worklist.isEmpty()) {
			Type.Reference type = worklist.remove(worklist.size() - 1);
			Clazz c = findClass(type);
			Field f = c.getField(name);
			if (f != null) {
				// found it!
				Type fieldT = substituteTypes(type, c.type(), f.type());
				return new Triple<Clazz, Field, Type>(c, f, fieldT);
			}
			// no match yet, so traverse super class and interfaces
			if (c.superClass() != null) {
				worklist.add((Type.Reference) substituteTypes(type, c.type(), c
						.superClass()));
			}
			for (Type.Reference t : c.interfaces()) {
				worklist
						.add((Type.Reference) substituteTypes(type, c.type(), t));
			}
		}

		throw new FieldNotFoundException(name, owner.toString());
	}

	/**
	 * Identify the method with the given name in the given clazz that matches
	 * the given method signature.
	 * 
	 * @param owner
	 *            enclosing class
	 * @param name
	 *            Method name
	 * @param concreteParameterTypes
	 *            the actual parameter types to match against
	 * @return A triple (C,M,T), where M is the method being invoked, C it's
	 *         enclosing class, and T is the actual type of the method. Note
	 *         that T can vary from M.type, since it may contain appropriate
	 *         substitutions for any generic type variables.
	 * @throws ClassNotFoundException
	 *             If it needs to access a class which cannot be found.
	 * @throws MethodNotFoundException
	 *             If it cannot find a matching method.
	 */
	public static Triple<Clazz, Method, Type.Function> resolveMethod(
			Type.Reference owner, String name, List<Type> concreteParameterTypes)
			throws ClassNotFoundException, MethodNotFoundException {	

		// Phase 1: traverse heirarchy whilst ignoring autoboxing and varargs
		Triple<Clazz, Method, Type.Function> methodInfo = resolveMethod(owner,
				name, concreteParameterTypes, false, false);

		if (methodInfo == null) {
			// Phase 2: Ok, phase 1 failed, so now consider autoboxing.
			methodInfo = resolveMethod(owner, name, concreteParameterTypes,
					true, false);

			if (methodInfo == null) {
				// Phase 3: Ok, phase 2 failed, so now consider var args as well.
				methodInfo = resolveMethod(owner, name, concreteParameterTypes,
						true, true);
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
					throw new MethodNotFoundException(method + ")", owner
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
	 * @param owner
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
	private static Triple<Clazz, Method, Type.Function> resolveMethod(
			Type.Reference owner, String name,
			List<Type> concreteParameterTypes, boolean autoboxing,
			boolean varargs) throws ClassNotFoundException {
		// Construct conrete function type
		Type.Function concreteFunctionType = Type.functionType(Type.anyType(),
				concreteParameterTypes.toArray(new Type[concreteParameterTypes
						.size()]));
		// traverse class hierarchy looking for field
		ArrayList<Type.Reference> worklist = new ArrayList<Type.Reference>();
		worklist.add(owner);
		
		ArrayList<Triple<Clazz, Method, Type.Function>> mts = new ArrayList<Triple<Clazz, Method, Type.Function>>();

		// Traverse type hierarchy building a list of potential methods
		while (!worklist.isEmpty()) {
			Type.Reference type = worklist.remove(0);
			Clazz c = findClass(type);
			List<Method> methods = c.methods(name);

			for (Method m : methods) {
				// try to rule out as many impossible candidates as possible
				Type.Function m_type = m.type();
				if (m_type.parameterTypes().length == concreteParameterTypes
						.size()
						|| (varargs && m.isVariableArity() && m_type
								.parameterTypes().length <= (concreteParameterTypes
								.size()+1))) {
					// First, substitute class type parameters
					Type.Function mt = (Type.Function) substituteTypes(type, c
							.type(), m.type());
					// Second, substitute method type parameters
					mt = (Type.Function) substituteTypes(concreteFunctionType, mt, mt);
					// Third, identify and substitute any remaining generic variables
					// for java.lang.Object. This corresponds to unsafe
                    // operations that will compile in e.g. javac
					Set<Type.Variable> freeVars = mt.freeVariables();					
					HashMap<String,Type> freeVarMap = new HashMap<String,Type>();
					for(Type.Variable fv : freeVars) {
						freeVarMap.put(fv.name,Type.referenceType("java.lang","Object"));
					}
					mt = (Type.Function) mt.substitute(freeVarMap);
					mts.add(new Triple<Clazz, Method, Type.Function>(c, m, mt));					
				}
			}

			if (c.superClass() != null) {
				worklist.add((Type.Reference) substituteTypes(type, c.type(), c
						.superClass()));
			}
			for (Type.Reference t : c.interfaces()) {
				worklist
						.add((Type.Reference) substituteTypes(type, c.type(), t));
			}
		}

		// Find target method
		return matchMethod(concreteParameterTypes, mts, autoboxing);
	}

	/**
	 * Give two references, find the most specific super type (i.e. least upper
	 * bound) of them. For exampe, both java.lang.Number and java.lang.Object
	 * are supertypes of both java.lang.Integer and java.lang.Float; however,
	 * java.lang.Integer is more specific that java.lang.Object.
	 * 
	 * @param refT1
	 * @param refT2
	 * @return
	 */
	public static Type.Reference commonSuperType(Type.Reference refT1,
			Type.Reference refT2) throws ClassNotFoundException {
		Type.Reference lubT = findCommonSuperType(refT2, refT1);
		TypeElement[] es = union(refT1.elements, refT2.elements);
		return new Type.Reference(lubT.pkg(), lubT.classes(), es);
	}

	// Fundamentally this method is in the wrong place.
	private static Type.Reference findCommonSuperType(Type.Reference refT1,
			Type.Reference refT2) throws ClassNotFoundException {
		//System.out.println("Finding common super type of " + refT1 + " and " + refT2);
		// this performs quite a complicated search,
		// which could be made more efficient
		// through caching and by recording which have
		// already been visited.

		ArrayList<Type.Reference> worklist = new ArrayList<Type.Reference>();
		HashSet<Type.Reference> closure = new HashSet<Type.Reference>();
		worklist.add(refT1);

		while (!worklist.isEmpty()) {
			// first, remove and check
			Type.Reference type = worklist.remove(worklist.size() - 1);
			if (type.equals(refT2)) {
				return type;
			} else if (Types.descriptor(type, false).equals(
					Types.descriptor(refT2, false))) {
				// There are several cases here:
				//
				// 1) computing common supertype of erased and non-erased types.
                //    e.g. ArrayList<String> and ArrayList. In this case, we employ
                //    the unchecked assumption that they go together (as Java does)
				//
				// 2) computing common supertype of two non-erased types.
                //    e.g. ArrayList<String> and ArrayList<Integer>. In this case,
                //    we can generate ArrayList<?>
				Pair<String,Type[]>[] tClasses = type.classes();
				Pair<String,Type[]>[] rClasses = refT2.classes();
				@SuppressWarnings("unchecked")
				Pair<String,Type[]>[] nClasses = new Pair[tClasses.length];
				boolean failed = false;
				for(int i=0;i!=tClasses.length;++i) {
					Type[] ts = tClasses[i].second();
					Type[] rs = rClasses[i].second();					
					if(ts.length != rs.length) {
						if(ts.length == 0) {
							nClasses[i] = rClasses[i];							
						} else if(rs.length == 0) {
							nClasses[i] = tClasses[i];
						} else {
							// fall through, since this on doesn't work
							failed = true;
							break;
						}
					} else {
						Type[] ns = new Type[rs.length];
						for (int j = 0; j != ts.length; ++j) {
							if(!ts[j].supsetEqOfElem(rs[j]) && !rs[j].supsetEqOfElem(ts[j])) {
								// FIXME: need to deal with bounds here
								ns[j] = Type.wildcardType(new Type[0], new Type[0]);
							} else if(rs[j].supsetEqOfElem(ts[j])){
								ns[j] = rs[j];
							} else {
								ns[j] = ts[j];
							}							
						}
						nClasses[i] = new Pair<String, Type[]>(tClasses[i].first(),ns);
					}		
				}
				if(!failed) {
					return Type.referenceType(type.pkg(),nClasses);
				}				
			}
			closure.add(type);
			Clazz c = findClass(type);

			// no match yet, so traverse super class and interfaces
			if (c.superClass() != null) {
				worklist.add((Type.Reference) substituteTypes(type, c.type(), c
						.superClass()));
			}
			for (Type.Reference t : c.interfaces()) {
				worklist
						.add((Type.Reference) substituteTypes(type, c.type(), t));
			}
		}
		// Now, do the second half of the search
		worklist.add(refT2);

		while (!worklist.isEmpty()) {
			// first, remove and check
			Type.Reference type = worklist.remove(worklist.size() - 1);
			for(Type.Reference t : closure) {
				if(t.equals(type)) {
					return t;
				}
			}
			
			if (closure.contains(type)) {
				return type;
			}
			Clazz c = findClass(type);
			// no match yet, so traverse super class and interfaces
			if (c.superClass() != null) {
				worklist.add((Type.Reference) substituteTypes(type, c.type(), c
						.superClass()));
			}
			for (Type.Reference t : c.interfaces()) {
				worklist
						.add((Type.Reference) substituteTypes(type, c.type(), t));
			}
		}

		// this is always true!
		return Type.referenceType("java.lang","Object");
	}

	/**
	 * The problem here is, given a list of similar functions, to select the
	 * most appropriate match for the given parameter types. If there is no
	 * appropriate match, simply return null.
	 * 
	 * cm - This problem is not even this simple. Not only do we want to find a
	 * match but we want it to be the most specific. Method has been changed to
	 * incorporate this.
	 */
	private static Triple<Clazz, Method, Type.Function> matchMethod(
			List<Type> parameterTypes,
			List<Triple<Clazz, Method, Type.Function>> methods,
			boolean autoboxing) {
		int matchIndex = -1;
		// params contains the original parameter types we're looking for.
		Type[] params = parameterTypes.toArray(new Type[parameterTypes.size()]);
		// nparams contains the best match we have so far.
		Type[] nparams = null;

		outer: for (int i = methods.size() - 1; i >= 0; --i) {
			Triple<Clazz, Method, Type.Function> methInfo = methods.get(i);
			Method m = methInfo.second();
			Type.Function f = methInfo.third();			
			Type[] mps = f.parameterTypes();
			if (mps.length == params.length
					|| (m.isVariableArity() && mps.length <= (params.length + 1))) {
				// check each parameter type.
				int numToCheck = m.isVariableArity() ? mps.length - 1
						: mps.length;
				for (int j = 0; j != numToCheck; ++j) {
					Type p1 = mps[j];
					Type p2 = params[j];

					if (!p1.supsetEqOf(p2)) {
						continue outer;
					}
					if (!autoboxing
							&& ((p1 instanceof Type.Primitive && !(p2 instanceof Type.Primitive)) || (p2 instanceof Type.Primitive && !(p1 instanceof Type.Primitive)))) {
						continue outer;
					}
					if (nparams != null && !nparams[j].supsetEqOf(p1)) {
						continue outer;
					}
				}
				// At this point, if the method is a variable arity method we
				// need to also check that the varargs portion make sense.
				if(m.isVariableArity()) {
					Type.Array arrayType = (Type.Array) mps[numToCheck];
					Type elementType = arrayType.elementType();
					if(numToCheck == (params.length-1)) {
						// In the special case that just one parameter is
						// provided in a variable arity position, we need to
						// check whether or not it is an array of the
						// appropriate type.
						Type p2 = params[numToCheck];
						if(!elementType.supsetEqOf(p2) && !arrayType.supsetEqOf(p2)) {
							continue outer;
						}
					} else {
						// This is the normal situation. We need to check
						// whether or not the arguments provided in the variable
						// arity positions are subtypes of the variable arity
						// list element type.
						for(int j=numToCheck;j<params.length;++j) {
							Type p2 = params[j];						
							if(!elementType.supsetEqOf(p2)) {
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
	 * The problem here is, given a concrete type and an parameterised type,
	 * determine the appropriate concrete super type. For example, suppose we
	 * have:
	 * 
	 * concreteT = Test<Float> templateT = Test<T> destationT = class Test2<Integer,T>
	 * 
	 * Then, the resulting type we want is:
	 * 
	 * Test2<Integer,Float>
	 * 
	 * @param concreteT
	 *            a (not necessarily complete) concrete instance of the type
	 * @param templateT
	 *            a generic (i.e. parameterised) version of the type.
	 * @param destinationT
	 *            an uninstantiated type in to which the type variables from
	 *            concreteT are substituted.
	 * @return
	 */
	private static Type substituteTypes(Type concreteT,
			Type templateT, Type destinationT) {
		// Compute the binding from type variables to their instantiated
		// types.
		HashMap<String, Type> binding = new HashMap<String, Type>();
		templateT.bind(concreteT, binding);
		// Finally, substitute the binding into the super type to get the
		// result.
		Type r = destinationT.substitute(binding);

		if (r instanceof Type.Reference) {
			// The following line looks a bit wierd. You need the concreteT
			// elements otherwise you reduce @NonNull Vector to Object and
			// loose the @NonNull.
			Type.Reference tr = (Type.Reference) r;
			return Type.referenceType(tr.pkg(), tr.classes(),
					concreteT.elements);
		} else {
			return r;
		}
	}

	/**
	 * Build a class name from a Type Reference.
	 * 
	 */
	@SuppressWarnings("unused")
	private static String className(Type.Reference ref) {
		String r = "";
		Pair<String, Type[]>[] cs = ref.classes();
		for (int i = 0; i != cs.length; ++i) {
			if (i != 0) {
				r += "$";
			}
			r += cs[i].first();
		}
		return r;
	}

	/**
	 * Union two lists of type elements.
	 * 
	 * @param A
	 * @param B
	 * @return
	 */
	private static TypeElement[] union(TypeElement[] A, TypeElement[] B) {
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

	
}
