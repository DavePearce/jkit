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
import jkit.compiler.Clazz;
import jkit.compiler.SyntacticAttribute;
import jkit.compiler.SyntacticElementImpl;
import jkit.jil.util.*;

public final class JilMethod extends SyntacticElementImpl implements jkit.compiler.Clazz.Method {
	
	public static final class JilParameter extends SyntacticElementImpl implements Clazz.Parameter {
		private String name;
		private final List<Modifier> modifiers;
		
		public JilParameter(String name, List<Modifier> modifiers, SyntacticAttribute... attributes) {
			super(attributes);
			this.name = name;
			this.modifiers = new ArrayList<Modifier>(modifiers);
		}
		
		public JilParameter(String name, List<Modifier> modifiers, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.name = name;
			this.modifiers = new ArrayList<Modifier>(modifiers);
		}
		
		public String name() {
			return name;
		}
		
		public List<Modifier> modifiers() {
			return modifiers;
		}
		

		/**
		 * Check whether this method is synthetic
		 */
		public boolean isSynthetic() {
			for (Modifier m : modifiers) {
				if (m instanceof Modifier.Synthetic) {
					return true;
				}
			}
			return false;
		}
	}	
	
	private String name;
	private Type.Function type;
	private List<Modifier> modifiers;
	private List<Type.Clazz> exceptions;
	private List<JilParameter> parameters; 	
	private List<JilStmt> body = new ArrayList<JilStmt>();
	
	/**
	 * Construct an object representing a field of a JVM class.
	 * 
	 * @param name -
	 *            The name of the method.
	 * @param type -
	 *            The (fully generic) function type of this method.
	 * @param parameters -
	 *            The names of the parameter variables, in order of their
	 *            appearance. It must hold that parameters.size() ==
	 *            type.parameterTypes().size().
	 * @param modifiers -
	 *            Any modifiers of the method (e.g. public, static, etc)
	 * @param exceptions -
	 *            The (non-null) list of exceptions thrown by this method.
	 */
	public JilMethod(String name, Type.Function type,
			List<JilParameter> parameters, List<Modifier> modifiers,
			List<Type.Clazz> exceptions, SyntacticAttribute... attributes) {
		super(attributes);
		this.name = name;
		this.type = type;
		this.parameters = new ArrayList<JilParameter>(parameters);
		this.modifiers = new ArrayList<Modifier>(modifiers);
		this.exceptions = new ArrayList<Type.Clazz>(exceptions);
	}
	
	/**
	 * Construct an object representing a field of a JVM class.
	 * 
	 * @param name -
	 *            The name of the method.
	 * @param type -
	 *            The (fully generic) function type of this method.
	 * @param parameters -
	 *            The names of the parameter variables, in order of their
	 *            appearance. It must hold that parameters.size() ==
	 *            type.parameterTypes().size().
	 * @param modifiers -
	 *            Any modifiers of the method (e.g. public, static, etc)
	 * @param exceptions -
	 *            The (non-null) list of exceptions thrown by this method.
	 */
	public JilMethod(String name, Type.Function type,
			List<JilParameter> parameters,			
			List<Modifier> modifiers, List<Type.Clazz> exceptions,
			List<SyntacticAttribute> attributes) {
		super(attributes);
		this.name = name;
		this.type = type;		
		this.parameters = new ArrayList<JilParameter>(parameters);
		this.modifiers = new ArrayList<Modifier>(modifiers);
		this.exceptions = new ArrayList<Type.Clazz>(exceptions);		
	}
	
	/**
     * Access the name of this field.  
     * 
     * @return
     */
	public String name() {
		return name;
	}
	
	/**
     * Access the type of this field. This is useful for determining it's
     * package, and/or any generic parameters it declares.
     * 
     * @return
     */
	public Type.Function type() {
		return type;
	}
	
	public void setType(Type.Function type) {
		this.type = type;
	}
	
	/**
     * Access the modifiers contained in this method object. The returned list
     * may be modified by adding, or removing modifiers. The returned list is
     * always non-null.
     * 
     * @return
     */
	public List<Modifier> modifiers() { return modifiers; }
	
	/**
	 * Access the names of the parameter variables to this method object. These
	 * are needed to distinguish the other local variables from those which are
	 * parameters.
	 * 
	 * @return
	 */
	public List<JilParameter> parameters() { return parameters; }	
	
	/**
     * Access the modifiers contained in this field object. The returned list
     * may be modified by adding, or removing modifiers. The returned list is
     * always non-null.
     * 
     * @return
     */
	public List<Type.Clazz> exceptions() { return exceptions; }
	
	/**
	 * Access the statements that make up the body of this method.
	 * @return
	 */
	public List<JilStmt> body() { return body; }
	
	/**
     * Check whether this method has one of the "base" modifiers (e.g. static,
     * public, private, etc). These are found in Modifier.ACC_
     * 
     * @param modifier
     * @return true if it does!
     */
	public boolean hasModifier(Class modClass) {
		for(Modifier m : modifiers) {
			if(m.getClass().equals(modClass)) {
				return true;
			}			
		}
		return false;
	}
	
	/**
	 * Check whether this method is abstract
	 */
	public boolean isAbstract() {
		for(Modifier m : modifiers) { 
			if(m instanceof Modifier.Abstract) {
				return true;
			}
		}
		return false;		
	}

	/**
	 * Check whether this method is final
	 */
	public boolean isFinal() {
		for (Modifier m : modifiers) {
			if (m instanceof Modifier.Final) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether this method is static
	 */
	public boolean isStatic() {
		for (Modifier m : modifiers) {
			if (m instanceof Modifier.Static) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether this method is public
	 */
	public boolean isPublic() {
		for (Modifier m : modifiers) {
			if (m instanceof Modifier.Public) {
				return true;
			}
		}		
		return false;
	}

	/**
	 * Check whether this method is protected
	 */
	public boolean isProtected() {
		for(Modifier m : modifiers) { if(m instanceof Modifier.Protected) { return true; }}
		return false;
	}

	/**
	 * Check whether this method is private
	 */
	public boolean isPrivate() {
		for (Modifier m : modifiers) {
			if (m instanceof Modifier.Private) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether this method is pure
	 */
	public boolean isPure() {
		for (Modifier m : modifiers) {
			if (m instanceof Modifier.Annotation) {
				Modifier.Annotation a = (Modifier.Annotation) m;
				Type.Clazz t = a.type();
				if (t.pkg().equals("jkit.java.annotations")
						&& t.lastComponent().first().equals("Pure")) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Check whether this method is pure
	 */
	public boolean isLocal() {
		for (Modifier m : modifiers) {
			if (m instanceof Modifier.Annotation) {
				Modifier.Annotation a = (Modifier.Annotation) m;
				Type.Clazz t = a.type();
				if (t.pkg().equals("jkit.java.annotations")
						&& t.lastComponent().first().equals("Local")) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Check whether this method is native
	 */
	public boolean isNative() {
		for (Modifier m : modifiers) {
			if (m instanceof Modifier.Native) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether this method is synchronized
	 */
	public boolean isSynchronized() {
		for (Modifier m : modifiers) {
			if (m instanceof Modifier.Synchronized) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether this method is synthetic
	 */
	public boolean isSynthetic() {
		for (Modifier m : modifiers) {
			if (m instanceof Modifier.Synthetic) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Check whether this method has varargs
	 */
	public boolean isVariableArity() {
		for (Modifier m : modifiers) {
			if (m instanceof Modifier.VarArgs) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * This method determines the set of local variables used within this
	 * method.  Note, this does not included parameters.
	 * 
	 * @return
	 */
	public List<Pair<String,Boolean>> localVariables() {
		HashSet<String> vars = new HashSet<String>();
		HashSet<String> biguns = new HashSet(); 
		
		for(JilStmt s : body) {
			if(s instanceof JilStmt.Assign) {
				JilStmt.Assign a = (JilStmt.Assign) s;
				Map<String,Type> env1 = Exprs.localVariables(a.lhs());
				Map<String,Type> env2 = Exprs.localVariables(a.rhs());
				vars.addAll(env1.keySet());
				vars.addAll(env2.keySet());
				for(Map.Entry<String,Type> e : env1.entrySet()) {
					if (e.getValue() instanceof Type.Double
							|| e.getValue() instanceof Type.Long) {
						biguns.add(e.getKey());
					}
				}				
				for(Map.Entry<String,Type> e : env2.entrySet()) {
					if (e.getValue() instanceof Type.Double
							|| e.getValue() instanceof Type.Long) {
						biguns.add(e.getKey());
					}
				}
				
			} else if(s instanceof JilStmt.Return) {
				JilStmt.Return a = (JilStmt.Return) s;
				if(a.expr() != null) {
					Map<String,Type> env = Exprs.localVariables(a.expr());
					vars.addAll(env.keySet());
					for(Map.Entry<String,Type> e : env.entrySet()) {
						if (e.getValue() instanceof Type.Double
								|| e.getValue() instanceof Type.Long) {
							biguns.add(e.getKey());
						}
					}	
				}
			} else if(s instanceof JilStmt.Throw) {
				JilStmt.Throw a = (JilStmt.Throw) s;				
				Map<String,Type> env = Exprs.localVariables(a.expr());
				vars.addAll(env.keySet());
				for(Map.Entry<String,Type> e : env.entrySet()) {
					if (e.getValue() instanceof Type.Double
							|| e.getValue() instanceof Type.Long) {
						biguns.add(e.getKey());
					}
				}
			} else if(s instanceof JilStmt.Lock) {
				JilStmt.Lock a = (JilStmt.Lock) s;
				Map<String,Type> env = Exprs.localVariables(a.expr());
				vars.addAll(env.keySet());
				for(Map.Entry<String,Type> e : env.entrySet()) {
					if (e.getValue() instanceof Type.Double
							|| e.getValue() instanceof Type.Long) {
						biguns.add(e.getKey());
					}
				}
			} else if(s instanceof JilStmt.Unlock) {
				JilStmt.Unlock a = (JilStmt.Unlock) s;
				Map<String,Type> env = Exprs.localVariables(a.expr());
				vars.addAll(env.keySet());
				for(Map.Entry<String,Type> e : env.entrySet()) {
					if (e.getValue() instanceof Type.Double
							|| e.getValue() instanceof Type.Long) {
						biguns.add(e.getKey());
					}
				}
			} else if(s instanceof JilStmt.IfGoto) {
				JilStmt.IfGoto a = (JilStmt.IfGoto) s;
				Map<String,Type> env = Exprs.localVariables(a.condition());
				vars.addAll(env.keySet());
				for(Map.Entry<String,Type> e : env.entrySet()) {
					if (e.getValue() instanceof Type.Double
							|| e.getValue() instanceof Type.Long) {
						biguns.add(e.getKey());
					}
				}
			} else if(s instanceof JilExpr.Invoke) {
				JilExpr.Invoke a = (JilExpr.Invoke) s;
				Map<String,Type> env = Exprs.localVariables(a);
				vars.addAll(env.keySet());
				for(Map.Entry<String,Type> e : env.entrySet()) {
					if (e.getValue() instanceof Type.Double
							|| e.getValue() instanceof Type.Long) {
						biguns.add(e.getKey());
					}
				}
			}
		}
		
		for(JilParameter p : parameters) {
			vars.remove(p.name());
		}
		
		vars.remove("this"); // these are implicit
		vars.remove("super"); // these are implicit 
		vars.remove("$"); // these are implicit
		
		ArrayList<Pair<String,Boolean>> r = new ArrayList();
		
		for(String var : vars) {
			if(biguns.contains(var)) {
				r.add(new Pair(var,true));
			} else {
				r.add(new Pair(var,false));
			}
		}
		
		return r;
	}

}
