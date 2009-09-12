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

import java.util.ArrayList;
import java.util.List;

import jkit.java.tree.Type.Clazz;
import jkit.java.tree.Type.Variable;
import jkit.jil.tree.SyntacticAttribute;
import jkit.jil.tree.Modifier;
import jkit.jil.tree.SyntacticElement;
import jkit.jil.tree.SyntacticElementImpl;
import jkit.util.Triple;

public interface Decl extends SyntacticElement {

	public static class JavaClass extends SyntacticElementImpl implements Decl, Stmt {
		private List<Modifier> modifiers;
		private String name;
		private List<Type.Variable> typeParameters;
		private Type.Clazz superclass;
		private List<Type.Clazz> interfaces;
		private List<Decl> declarations;		

		public JavaClass(List<Modifier> modifiers, String name,
				List<Type.Variable> typeParameters, Type.Clazz superclass,
				List<Type.Clazz> interfaces, List<Decl> declarations,
				SyntacticAttribute... attributes) {
			super(attributes);
			this.modifiers = modifiers;
			this.name = name;
			this.typeParameters = typeParameters;
			this.superclass = superclass;
			this.interfaces = interfaces;
			this.declarations = declarations;			
		}

		public List<Modifier> modifiers() {
			return modifiers;
		}

		public String name() {
			return name;
		}

		public List<Type.Variable> typeParameters() {
			return typeParameters;
		}

		public Type.Clazz superclass() {
			return superclass;
		}

		public List<Type.Clazz> interfaces() {
			return interfaces;
		}

		public List<Decl> declarations() { 
			return declarations;
		}
		
		/**
	     * Check whether this method has one of the "base" modifiers (e.g. static,
	     * public, private, etc). These are found in java.lang.reflect.Modifier.
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
		public boolean isInterface() {
			for (Modifier m : modifiers) {
				if (m instanceof Modifier.Interface) {
					return true;
				}
			}
			return false;
		}
		
		/**
		 * Check whether this method is abstract
		 */
		public boolean isAbstract() {
			for (Modifier m : modifiers) {
				if (m instanceof Modifier.Abstract) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Check whether this method is final
		 */
		public boolean isFinal() {
			for(Modifier m : modifiers) { if (m instanceof Modifier.Final) { return true; }}
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
			for (Modifier m : modifiers) {
				if (m instanceof Modifier.Protected) {
					return true;
				}
			}
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
	}

	public static class JavaInterface extends JavaClass {
		public JavaInterface(List<Modifier> modifiers, String name,
				List<Type.Variable> typeParameters, Type.Clazz superclass,
				List<Type.Clazz> interfaces, List<Decl> declarations,
				SyntacticAttribute... attributes) {
			super(modifiers, name, typeParameters, superclass, interfaces,
					declarations, attributes);
		}
	}

	public static class JavaEnum extends JavaClass {
		private List<EnumConstant> constants;

		public JavaEnum(List<Modifier> modifiers, String name,
				List<Type.Clazz> interfaces, List<EnumConstant> constants,
				List<Decl> declarations, SyntacticAttribute... attributes) {
			super(modifiers, name, new ArrayList<Type.Variable>(), null,
					interfaces, declarations, attributes);
			this.constants = constants;
		}

		public List<EnumConstant> constants() {
			return constants;
		}
	}

	public static class EnumConstant extends SyntacticElementImpl{
		private String name;
		private List<Expr> arguments;
		private List<Decl> declarations;

		public EnumConstant(String name, List<Expr> arguments,
				List<Decl> declarations, SyntacticAttribute... attributes) {
			super(attributes);
			this.name = name;
			this.arguments = arguments;
			this.declarations = declarations;
		}

		public String name() {
			return name;
		}

		public List<Expr> arguments() {
			return arguments;
		}

		public List<Decl> declarations() {
			return declarations;
		}
	}

	public static class AnnotationInterface extends SyntacticElementImpl  implements Decl {
		private List<Modifier> modifiers;
		private String name;
		private List<Triple<Type, String, Value>> methods; 

		public AnnotationInterface(List<Modifier> modifiers, String name,
				List<Triple<Type, String, Value>> methods,
				SyntacticAttribute... attributes) {
			super(attributes);
			this.modifiers = modifiers;
			this.name = name;
			this.methods = methods;
		}

		public List<Modifier> modifiers() {
			return modifiers;
		}
		public String name() {
			return name;
		}
		public List<Triple<Type, String, Value>> methods() {
			return methods;
		}
	}

	public static class JavaParameter extends SyntacticElementImpl {
		private String name;
		private List<Modifier> modifiers;
		private Type type;
		
		public JavaParameter(String name, List<Modifier> modifiers, Type type, SyntacticAttribute... attributes) {
			super(attributes);
			this.name = name;
			this.modifiers = modifiers;
			this.type = type;
		}
		
		public JavaParameter(String name, List<Modifier> modifiers, Type type, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.name = name;
			this.modifiers = modifiers;
			this.type = type;
		}
		
		public String name() {
			return name;
		}
		
		public List<Modifier> modifiers() {
			return modifiers;
		}
		
		public Type type() {
			return type;
		}
	}
	
	/**
	 * This class stores all known information about a method, including it's
	 * full (possibly generic) type, its name, its modifiers (e.g. public/private
	 * etc), as well as the methods code.
	 * 
	 * @author djp
	 * 
	 */
	public static class JavaMethod extends SyntacticElementImpl  implements Decl {
		private List<Modifier> modifiers;
		private String name;
		private Type returnType;
		private List<JavaParameter> parameters;
		private List<Type.Variable> typeParameters;
		private List<Type.Clazz> exceptions;
		private Stmt.Block block;

		public JavaMethod(List<Modifier> modifiers, String name, Type returnType,
				List<JavaParameter> parameters,
				boolean varargs, List<Type.Variable> typeParameters,
				List<Type.Clazz> exceptions, Stmt.Block block,
				SyntacticAttribute... attributes) {
			super(attributes);
			this.modifiers = modifiers;
			this.returnType = returnType;
			this.name = name;
			this.parameters = parameters;
			if(varargs) {
				modifiers.add(new Modifier.VarArgs());
			}			
			this.typeParameters = typeParameters;
			this.exceptions = exceptions;
			this.block = block;
		}

		public List<Modifier> modifiers() {
			return modifiers;
		}

		public String name() {
			return name;
		}

		public Type returnType() {
			return returnType;
		}

		/**
		 * List of triples (n,m,t), where n is the parameter name, m are the
		 * modifiers and t is the type.
		 * 
		 * @return
		 */
		public List<JavaParameter> parameters() {
			return parameters;
		}

		public List<Type.Variable> typeParameters() {
			return typeParameters;
		}

		public List<Type.Clazz> exceptions() {
			return exceptions;
		}

		public Stmt.Block body() {
			return block;
		}
		
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
	}

	/**
	 * A constructor is a special kind of method.
	 * 
	 * @author djp
	 * 
	 */
	public static class JavaConstructor extends JavaMethod {
		public JavaConstructor(List<Modifier> modifiers, String name,
				List<JavaParameter> parameters, boolean varargs,
				List<Type.Variable> typeParameters,
				List<Type.Clazz> exceptions,
				Stmt.Block block, SyntacticAttribute... attributes) {			
			super(modifiers, name, null, parameters, varargs, typeParameters,
					exceptions, block,attributes);
		}
	}

	public static class JavaField extends SyntacticElementImpl implements Decl {
		private List<Modifier> modifiers;
		private String name;
		private Type type;
		private Expr initialiser;

		public JavaField(List<Modifier> modifiers, String name, Type type,
				Expr initialiser, SyntacticAttribute... attributes) {
			super(attributes);
			this.modifiers = modifiers;
			this.name = name;
			this.type = type;
			this.initialiser = initialiser;
		}

		public List<Modifier> modifiers() {
			return modifiers;
		}

		public String name() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}

		public Type type() {
			return type;
		}

		public void setType(Type t) {
			this.type = t;			
		}

		public Expr initialiser() {
			return initialiser;
		}		
		
		public void setInitialiser(Expr init) {
			this.initialiser = init;
		}
		
		/**
		 * Check whether this field represents a constant or not.
		 * 
		 * @return
		 */
		public boolean isConstant() {			
			return initialiser != null && initialiser instanceof Value
					&& !(initialiser instanceof Value.Array);
		}
		
		public Object constant() {
			if(initialiser instanceof Value.Bool) {
				Value.Bool i = (Value.Bool) initialiser;
				return i.value();
			} else if(initialiser instanceof Value.Byte) {
				Value.Byte i = (Value.Byte) initialiser;
				return i.value();
			} else if(initialiser instanceof Value.Char) {
				Value.Char i = (Value.Char) initialiser;
				return i.value();
			} else if(initialiser instanceof Value.Short) {
				Value.Short i = (Value.Short) initialiser;
				return i.value();
			} else if(initialiser instanceof Value.Int) {
				Value.Int i = (Value.Int) initialiser;
				return i.value();
			} else if(initialiser instanceof Value.Long) {
				Value.Long i = (Value.Long) initialiser;
				return i.value();
			} else if(initialiser instanceof Value.Float) {
				Value.Float i = (Value.Float) initialiser;
				return i.value();
			} else if(initialiser instanceof Value.Double) {
				Value.Double i = (Value.Double) initialiser;
				return i.value();
			} else {
				Value.String i = (Value.String) initialiser;
				return i.value();
			}
		}
		
		/**
	     * Check whether this field has one of the "base" modifiers (e.g. static,
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
		 * Check whether this field is abstract
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
		 * Check whether this field is final
		 */
		public boolean isFinal() {		
			for(Modifier m : modifiers) {
				if(m instanceof Modifier.Final) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Check whether this field is static
		 */
		public boolean isStatic() {		
			for(Modifier m : modifiers) {
				if(m instanceof Modifier.Static) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Check whether this field is public
		 */
		public boolean isPublic() {
			for(Modifier m : modifiers) {
				if(m instanceof Modifier.Public) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Check whether this field is protected
		 */
		public boolean isProtected() {
			for(Modifier m : modifiers) {
				if(m instanceof Modifier.Protected) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Check whether this field is private
		 */
		public boolean isPrivate() {		
			for(Modifier m : modifiers) {
				if(m instanceof Modifier.Private) {
					return true;
				}
			}
			return false;
		}
	}

	public static class InitialiserBlock extends Stmt.Block implements Decl {
		public InitialiserBlock(List<Stmt> statements, SyntacticAttribute... attributes) {
			super(statements,attributes);
		}
	}
	public static class StaticInitialiserBlock extends Stmt.Block implements Decl {
		public StaticInitialiserBlock(List<Stmt> statements, SyntacticAttribute... attributes) {
			super(statements,attributes);
		}
	}

	
	
	
}	