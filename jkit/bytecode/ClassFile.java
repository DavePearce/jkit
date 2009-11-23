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

package jkit.bytecode;

import java.util.*;

import jkit.bytecode.attributes.ConstantValue;
import jkit.bytecode.attributes.Exceptions;
import jkit.bytecode.attributes.RuntimeVisibleAnnotations;
import jkit.compiler.Clazz;
import jkit.compiler.ClassLoader;
import jkit.jil.tree.*;
import jkit.util.Pair;

public class ClassFile implements Clazz {
	protected int version;
	protected Type.Clazz type;
	protected Type.Clazz superClazz;
	protected List<Type.Clazz> interfaces;	
	protected List<Modifier> modifiers;
	protected ArrayList<BytecodeAttribute> attributes;
	protected ArrayList<Field> fields;
	protected ArrayList<Method> methods;	
	
	public ClassFile(int version, Type.Clazz type, Type.Clazz superClazz,
			List<Type.Clazz> interfaces, List<Modifier> modifiers, BytecodeAttribute... attributes) {
		this.version = version;
		this.type = type;
		this.superClazz = superClazz;
		this.interfaces = interfaces;		
		this.modifiers = modifiers;
		this.fields = new ArrayList<Field>();
		this.methods = new ArrayList<Method>();
		this.attributes = new ArrayList<BytecodeAttribute>();
		for(BytecodeAttribute a : attributes) {
			this.attributes.add(a);
		}		
	}
	
	public ClassFile(int version, Type.Clazz type, Type.Clazz superClazz,
			List<Type.Clazz> interfaces, List<Modifier> modifiers, Collection<BytecodeAttribute> attributes) {
		this.version = version;
		this.type = type;
		this.superClazz = superClazz;
		this.interfaces = interfaces;		
		this.modifiers = modifiers;
		this.fields = new ArrayList<Field>();
		this.methods = new ArrayList<Method>();
		this.attributes = new ArrayList<BytecodeAttribute>(attributes);
	}
	
	public Type.Clazz type() {
		return type;
	}
	
	public void setType(Type.Clazz t) {
		type = t;
	}	
	
	public String name() {
		return type.lastComponent().first();
	}	
	
	public Type.Clazz superClass() {
		return superClazz;
	}
	
	public List<Type.Clazz> interfaces() {
		return interfaces;
	}
	
	public List<Type.Clazz> inners() {
		// this needs to be fixed. Essentially, by looking for an InnerClasses
		// attribute and then digging stuff out of it.
		return new ArrayList<Type.Clazz>();
	}
	
	public List<BytecodeAttribute> attributes() {
		return attributes;
	}
	
	public List<Modifier> modifiers() {
		return modifiers;
	}
	
	public List<Field> fields() {
		return fields;
	}

	public Field field(String name) {
		for(Field f : fields) {
			if(f.name().equals(name)) {
				return f;
			}
		}
		return null;
	}
	
	public List<Method> methods() {
		return methods;
	}
	
	public List<Method> methods(String name) {
		ArrayList<Method> r = new ArrayList<Method>();
		for(Method m : methods) {
			if(m.name().equals(name)) {
				r.add(m);
			}
		}
		return r;
	}
	
	public int version() {
		return version;
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
	 * Check whether or not this is an inner class.
	 * @return
	 */
	public boolean isInnerClass() {
		return type.components().size() > 1;
	}
	
	public static class Field implements Clazz.Field {
		protected String name;
		protected Type type;
		protected List<Modifier> modifiers;
		protected ArrayList<BytecodeAttribute> attributes;
		
		public Field(String name, Type type, List<Modifier> modifiers) {
			this.name = name;
			this.type = type;
			this.modifiers = modifiers;
			this.attributes = new ArrayList<BytecodeAttribute>();
		}
		
		public String name() {
			return name;
		}

		public void setName(String n) {
			name = n;
		}
		
		public Type type() {
			return type;
		}

		public void setType(Type t) {
			type = t;
		}
		
		public List<Modifier> modifiers() {
			return modifiers;
		}
		
		public List<BytecodeAttribute> attributes() {
			return attributes;
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
		
		public boolean isConstant() {
			for(BytecodeAttribute a : attributes) {
				if(a instanceof ConstantValue) {
					return true;
				}
			}
			return false;
		}
		
		public Object constant() {
			for(BytecodeAttribute a : attributes) {
				if(a instanceof ConstantValue) {
					ConstantValue x = (ConstantValue) a;
					return x.constant();
				}
			}
			return null;
		}
	}
	
	public static class Method implements Clazz.Method {
		protected String name;
		protected Type.Function type;
		protected List<Modifier> modifiers;
		protected ArrayList<BytecodeAttribute> attributes;		

		public Method(String name, Type.Function type,
				List<Modifier> modifiers) {
			this.name = name;
			this.type = type;
			this.modifiers = modifiers;			
			attributes = new ArrayList<BytecodeAttribute>();
		}

		public String name() {
			return name;
		}

		public Type.Function type() {
			return type;
		}

		public void setType(Type.Function t) {
			type = t;
		}
		
		public List<Parameter> parameters() {
			ArrayList<Parameter> r = new ArrayList<Parameter>();
			
			// FIXME: need to do more here.
			
			return r;
		}
		
		public List<Modifier> modifiers() {
			return modifiers;
		}		
		
		public List<Type.Clazz> exceptions() {
			for(BytecodeAttribute a : attributes) {
				if(a instanceof Exceptions) {
					return ((Exceptions)a).exceptions();
				}
			}
			return new ArrayList();
		}
		
		public BytecodeAttribute attribute(Class c) {
			for(BytecodeAttribute a : attributes) {
				if(c.isInstance(a)) {
					return a;
				}
			}
			return null;
		}

		public List<BytecodeAttribute> attributes() {
			return attributes;
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
	}
	
	/**
	 * This method builds a constant pool for this class file.
	 * 
	 * @return
	 */
	public ArrayList<Constant.Info> constantPool(ClassLoader loader) {
		HashSet<Constant.Info> constantPool = new HashSet<Constant.Info>();
		// Now, add constant pool items
		Constant.addPoolItem(Constant.buildClass(type),constantPool);
		Constant.addPoolItem(new Constant.Utf8("Signature"),constantPool);
		Constant.addPoolItem(new Constant.Utf8("ConstantValue"),constantPool);
		
		if (superClazz != null) {
			Constant.addPoolItem(Constant.buildClass(superClazz), constantPool);
		}

		for (Type.Reference i : interfaces) {
			Constant.addPoolItem(Constant.buildClass(i), constantPool);
		}			
		
		// Now, add all constant pool information for fields
		for (Field f : fields) {
			// Now, add pool items
			Constant.addPoolItem(new Constant.Utf8(f.name()), constantPool);
			Constant.addPoolItem(
					new Constant.Utf8(descriptor(f.type(), false)),
					constantPool);
			for(BytecodeAttribute a : f.attributes()) {
				a.addPoolItems(constantPool,loader);
			}
		}
		
		for(Method m : methods) {
			// Now, add all constant pool information for methods
			Constant.addPoolItem(new Constant.Utf8(m.name()), constantPool);						
			Constant.addPoolItem(new Constant.Utf8(descriptor(m.type(),
					false)), constantPool);

			for(BytecodeAttribute a : m.attributes()) {
				a.addPoolItems(constantPool,loader);
			}			
		}
		
		for(BytecodeAttribute a : attributes) {
			a.addPoolItems(constantPool,loader);
		}
		
		// Finally, we need to flatten the constant pool
		ArrayList<Constant.Info> pool = new ArrayList<Constant.Info>();
		pool.add(null); // first entry is not used
		for (Constant.Info ci : constantPool) {
			pool.add(ci);
			// Doubles and Longs require (for some reason) double slots.
			if(ci instanceof Constant.Double || ci instanceof Constant.Long) {
				pool.add(null);
			}
		}
		
		return pool;
	}
	
	/**
	 * This method returns a JVM descriptor string for the type in question. The
	 * format of the string is defined in "The JavaTM Virtual Machine
	 * Specification, 2nd ed", Section 4.3. Example descriptor strings include:
	 * 
	 * <table>
	 * <tr>
	 * <td><b>Type</b></td>
	 * <td><b>Descriptor</b></td>
	 * </tr>
	 * <tr>
	 * <td>int
	 * <tr>
	 * <td>I</td>
	 * <tr>
	 * <tr>
	 * <td>boolean
	 * <tr>
	 * <td>Z</td>
	 * <tr>
	 * <tr>
	 * <td>float[]
	 * <tr>
	 * <td>F[</td>
	 * <tr>
	 * <tr>
	 * <td>java.lang.Integer
	 * <tr>
	 * <td>Ljava/lang/Integer;</td>
	 * <tr>
	 * <tr>
	 * <td>int(Double,Float)
	 * <tr>
	 * <td>(DF)I</td>
	 * <tr> </table>
	 * <p>
	 * The descriptor string is used, amongst other things, to uniquely identify
	 * a class in the ClassTable.
	 * </p>
	 * 
	 * See the <a
	 * href="http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#1169">JVM
	 * Specification</a> for more information.
	 * 
	 * @param t
	 *            The type to generate the descriptor for
	 * @param generic
	 *            True indicates generic information should be included.
	 * @return
	 */
	public static String descriptor(Type t, boolean generic) {
		if(t instanceof Type.Bool) {
			return "Z";
		} if(t instanceof Type.Byte) {
			return "B";
		} else if(t instanceof Type.Char) {
			return "C";
		} else if(t instanceof Type.Short) {
			return "S";
		} else if(t instanceof Type.Int) {
			return "I";
		} else if(t instanceof Type.Long) {
			return "J";
		} else if(t instanceof Type.Float) {
			return "F";
		} else if(t instanceof Type.Double) {
			return "D";
		} else if(t instanceof Type.Void) {
			return "V";
		} else if(t instanceof Type.Array) {
			Type.Array at = (Type.Array) t;
			return "[" + descriptor(at.element(),generic);
		} else if(t instanceof Type.Clazz) {
			Type.Clazz ref = (Type.Clazz) t;
			String r = "L" + ref.pkg().replace(".","/");
			List<Pair<String, List<Type.Reference>>> classes = ref.components();
			for (int i = 0; i != classes.size(); ++i) {
				if (i == 0 && r.length() > 1) {
					r += "/";
				} else if(i > 0) {
					r += "$";
				}
				r += classes.get(i).first();
				if(generic) {
					List<Type.Reference> gparams = classes.get(i).second();
					if(gparams != null && gparams.size() > 0) {
						r += "<";
						for(Type gt : gparams) {
							r += descriptor(gt,generic);
						}
						r += ">";
					}
				}
			}
			return r + ";";
		} else if(t instanceof Type.Function) {
			// For simplicity, this code does not support generic function
            // types. The reason for this is that, to do so, requires access to
            // the ClassLoader. Instead, generic method signatures are supported
            // only by the MethodSignature class.
			
			Type.Function ft = (Type.Function) t;
			String r = "(";

			for (Type pt : ft.parameterTypes()) {				
				r += ClassFile.descriptor(pt,generic);
			}

			r = r + ")" + ClassFile.descriptor(ft.returnType(),generic);
			return r;			
		} if(t instanceof Type.Variable) {		
			if(generic) {
				Type.Variable tv = (Type.Variable) t;
				return "T" + tv.variable() + ";";
			} else {
				Type.Variable tv = (Type.Variable) t;
				Type.Reference lb = tv.lowerBound();
				if(lb != null) {
					return descriptor(lb,generic);
				} else {
					return "Ljava/lang/Object;";
				}
			}
		}  else if(t instanceof Type.Wildcard) {
			if(generic) {
				 Type.Wildcard tw = (Type.Wildcard) t;
				 if(tw.lowerBound() == null) {
					 return "*";
				 } else {
					 return "+" + descriptor(tw.lowerBound(),generic);
				 }
			} else {
				return "Ljava/lang/Object;";
			}
		} 
		 
		throw new RuntimeException("Invalid type passed to descriptor(): " + t);
	}
	
	/**
	 * Determine the slot size for the corresponding Java type.
	 * 
	 * @param type
	 *            The type to determine the slot size for.
	 * @return the slot size in slots.
	 */
	public static int slotSize(Type type) {
		if (type instanceof Type.Double || type instanceof Type.Long) {
			return 2;
		} else {
			return 1;
		}
	}
}
