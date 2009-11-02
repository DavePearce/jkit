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

package jkit.jil.stages;

import java.util.*;

import jkit.java.stages.TypeSystem;
import jkit.jil.tree.*;
import jkit.jil.util.*;
import jkit.compiler.Clazz;
import jkit.compiler.ClassLoader;
import jkit.compiler.MethodNotFoundException;
import jkit.util.*;

/**
 * This stage adds "bypass" methods to a class when they are needed. A typical
 * situation they arise is the following:
 * 
 * <pre>
 * class StringIterator implements Iterator&lt;String&gt; {
 *   public boolean hasNext() { ... }
 *   public String next() { ... }
 *   public void remove() { ... }	
 * }
 * </pre>
 * 
 * When compiling this class, a method called "next" with type ()->String is
 * generated. However, when accessing <code>StringIterator</code> objects via
 * the <code>Iterator</code> interface, the expected method is
 * next:()->Object. This causes a problem as the JVM dynamic dispatch will not
 * be able to find the appropriate method. Therefore, in such situations, we
 * insert an additional method of type next:()->Object. This simply redirects
 * the original method (hence the term "bypass method").
 * 
 * @author djp
 * 
 */
public final class BypassMethods {
	private ClassLoader loader = null;
	private TypeSystem types = null;
	

	public BypassMethods(ClassLoader loader, TypeSystem types) {
		this.loader = loader;
		this.types = types;
	}
	
	/**
	 * Apply this pass to a class.
	 * 
	 * @param owner
	 *            class to manipulate
	 */
	public void apply(JilClass owner) {
		HashSet<Triple<Clazz, Clazz.Method, Type.Function>> matches = new HashSet();

		// First, we identify all the problem cases.
		for (JilMethod m : owner.methods()) {			
			if(!m.isStatic()) {
				// Look through interfaces
				for (Type.Clazz i : owner.interfaces()) {
					checkForProblem(m, i, matches);
				}
				// Now, look through the super class (if there is one)
				if (owner.superClass() != null) {
					checkForProblem(m, owner.superClass(), matches);
				}
			}
		}

		// Second, we add appropriate bypass methods.
		for (Triple<Clazz,Clazz.Method, Type.Function> p : matches) {
			JilMethod m = generateBypass(owner, p.second(), p.third());
			owner.methods().add(m);
		}
	}

	/**
	 * This method looks for matching methods in the owner, and its
	 * super-classes / interfaces. If a matching method is found, then it checks
	 * whether any of its types are generic or not. If they are generic, then
	 * this method is identified as a problem case and added to the set of
	 * problem cases being built.
	 * 
	 * @param method
	 *            The method which we a looking to find a match for.
	 * @param owner
	 *            The class in which to search for matching methods.
	 * @param problems
	 *            The set of problem cases being built up
	 */
	protected void checkForProblem(JilMethod method, Type.Reference owner,
			Set<Triple<Clazz, Clazz.Method, Type.Function>> problems) {
		try {
			// traverse the heirarchy looking for a class or interface which
			// implements this method.
			Triple<Clazz, Clazz.Method, Type.Function> minfo = types
					.resolveMethod(owner, method.name(), method.type()
							.parameterTypes(), loader);
			
			Type.Function ft = minfo.second().type();
			Type.Function mt = method.type();
			List<Type> ftParamTypes = ft.parameterTypes();
			List<Type> mtParamTypes = mt.parameterTypes();
			Type ftReturnType = stripGenerics(ft.returnType());
			Type mtReturnType = stripGenerics(mt.returnType());
			
			boolean isMatch = !ftReturnType.equals(mtReturnType);
			
			for (int i = 0; i != ftParamTypes.size(); ++i) {
				Type fp = ftParamTypes.get(i);
				Type mp = mtParamTypes.get(i);
				// Basically, if the parameter type we found is generic, but the
				// actual type is not, then we found a problem case.
				isMatch = isMatch
						| (fp instanceof Type.Variable && !(mp instanceof Type.Variable));
			}

			if (isMatch) {				
				problems.add(new Triple<Clazz, Clazz.Method, Type.Function>(minfo
						.first(), minfo.second(), method.type()));
			}
		} catch (ClassNotFoundException ce) {
		} catch (MethodNotFoundException me) {
		}
	}

	/**
	 * Generate the bypass method with the same type as the originating method
	 * (parameter 2) which redirects to a method of type "to". Type variables
	 * found in the original methods type are replaced with java.lang.Object or
	 * their lower bounds (if they have them).
	 * 
	 * @param owner
	 *            the class into which the bypass is being placed.
	 * @param method
	 *            the original generic method
	 * @param to
	 *            the (concrete) instantiation of the generic method
	 * @return
	 */
	protected JilMethod generateBypass(JilClass owner, Clazz.Method method,
			Type.Function to) {
		// First, we substitute each type variable with java.lang.object
			
		Type.Function from = method.type();
		Type.Function ftype = (Type.Function) stripGenerics(from);
		String name = method.name();

		// Second, create the local variable list for the new method
		ArrayList<JilMethod.Parameter> params = new ArrayList();
		ArrayList<JilExpr> funParams = new ArrayList<JilExpr>();
		List<Type> ftypeParamTypes = ftype.parameterTypes();

		for (int i = 0; i != ftypeParamTypes.size(); ++i) {
			Type t = ftypeParamTypes.get(i);
			String n = "param$" + i;
			
			ArrayList<Modifier> mods = new ArrayList<Modifier>();
			mods.add(Modifier.ACC_FINAL);
			params.add(new JilMethod.Parameter(n, mods));
			
			if (from.parameterTypes().get(i) instanceof Type.Variable) {
				// the following cast is required because generic types are
				// enforced at compile time, but not strongly enforced in the
				// bytecode. Assuming the class passed compilation without any
				// warnings with respect to generic types, then this cast should
				// always pass.
				funParams.add(new JilExpr.Cast(new JilExpr.Variable(n, t), to
						.parameterTypes().get(i)));
			} else {
				funParams.add(new JilExpr.Variable(n, t));
			}
		}

		ArrayList<JilStmt> body = new ArrayList<JilStmt>();
		
		if (ftype.returnType() instanceof Type.Void) {
			// no return type
			JilStmt ivk = new JilExpr.Invoke(new JilExpr.Variable("this", owner.type()),
					name, funParams, to, ftype.returnType());
			body.add(ivk);			
			body.add(new JilStmt.Return(null));
		} else {
			JilExpr ivk = new JilExpr.Invoke(new JilExpr.Variable("this", owner.type()),
					name, funParams, to, ftype.returnType());
			JilStmt ret = new JilStmt.Return(ivk);
			body.add(ret);
		}

		ArrayList<Modifier> modifiers = new ArrayList<Modifier>();
		modifiers.add(Modifier.ACC_PUBLIC);
		modifiers.add(Modifier.ACC_SYNTHETIC);
		
		JilMethod r = new JilMethod(name, ftype, params, modifiers,
				new ArrayList<Type.Clazz>());
		
		r.body().addAll(body);
		return r;
	}

	/**
	 * This method simply strips off any generic variables and replaces them
	 * with java.lang.Object. For example, the following types:
	 * 
	 * <pre>
	 * T next()
	 * void add(List&lt;T&gt; x, T y)
	 * &lt;T extends Number&gt; void add(List&lt;T&gt; x, T y)
	 * </pre>
	 * 
	 * would become:
	 * 
	 * <pre>
	 * java.lang.Object next()
	 * void add(List x, java.lang.Object y)
	 * void add(List x, java.lang.Number y)
	 * </pre>
	 * 
	 * Observe here that, in the second add, the second parameter goes to Number
	 * because of the type bound.
	 * 
	 * @param type -
	 *            the type to be stripped
	 * @param method -
	 *            the enclosing method of the type
	 * @param owner -
	 *            the enclosing class of the type
	 * @return
	 */
	public static Type stripGenerics(Type type) {
		if (type instanceof Type.Primitive || type instanceof Type.Null
				|| type instanceof Type.Void) {
			return type;
		} else if (type instanceof Type.Variable) {
			Type.Variable tv = (Type.Variable) type;
			Type.Reference lb = tv.lowerBound();
			if (lb != null) {
				return stripGenerics(lb);
			} else {
				return Types.JAVA_LANG_OBJECT;
			}
		} else if (type instanceof Type.Wildcard) {
			Type.Wildcard tv = (Type.Wildcard) type;
			Type.Reference lb = tv.lowerBound();
			if (lb != null) {
				return stripGenerics(lb);
			} else {
				return Types.JAVA_LANG_OBJECT;
			}
		} else if (type instanceof Type.Array) {
			Type.Array at = (Type.Array) type;
			return new Type.Array(stripGenerics(at.element()));
		} else if (type instanceof Type.Clazz) {
			Type.Clazz tr = (Type.Clazz) type;
			List<Pair<String, List<Type.Reference>>> trClasses = tr
					.components();
			ArrayList<Pair<String, List<Type.Reference>>> classes = new ArrayList();
			for (int i = 0; i != trClasses.size(); ++i) {
				classes.add(new Pair(trClasses.get(i).first(), new ArrayList()));
			}
			return new Type.Clazz(tr.pkg(), classes);
		} else if (type instanceof Type.Function) {
			Type.Function tf = (Type.Function) type;
			Type retType = stripGenerics(tf.returnType());
			List<Type> tfParamTypes = tf.parameterTypes();
			List<Type> paramTypes = new ArrayList<Type>();
			for (int i = 0; i != tfParamTypes.size(); ++i) {
				paramTypes.add(stripGenerics(tfParamTypes.get(i)));
			}
			return new Type.Function(retType, paramTypes);
		} else if(type instanceof Type.Intersection) {
			Type.Intersection i = (Type.Intersection) type;
			if(i.bounds().size() == 1) {
			//	return stripGenerics(i.bounds().get(0));
			}
		} 

		throw new RuntimeException("Unknown type encountered: " + type
				+ "(" + type.getClass().getName() + ")");	
	}
}
