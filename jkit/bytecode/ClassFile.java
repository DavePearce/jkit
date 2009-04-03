package jkit.bytecode;

import java.util.*;

import jkit.compiler.Clazz;
import jkit.jil.tree.*;
import jkit.util.Pair;

public class ClassFile implements Clazz {
	protected int version;
	protected Type.Clazz type;
	protected Type.Clazz superClazz;
	protected List<Type.Clazz> interfaces;	
	protected List<Modifier> modifiers;
	protected ArrayList<Attribute> attributes;
	protected ArrayList<Field> fields;
	protected ArrayList<Method> methods;	
	
	public ClassFile(int version, Type.Clazz type, Type.Clazz superClazz,
			List<Type.Clazz> interfaces, List<Modifier> modifiers) {
		this.version = version;
		this.type = type;
		this.superClazz = superClazz;
		this.interfaces = interfaces;		
		this.modifiers = modifiers;
		this.fields = new ArrayList<Field>();
		this.methods = new ArrayList<Method>();
		this.attributes = new ArrayList<Attribute>();
	}
		
	public Type.Clazz type() {
		return type;
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
	
	public List<Attribute> attributes() {
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
	public boolean hasModifier(int modifier) {
		for(Modifier m : modifiers) {
			if(m instanceof Modifier.Base) {
				Modifier.Base b = (Modifier.Base) m;
				if(b.modifier() == modifier) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Check whether this method is abstract
	 */
	public boolean isInterface() {
		return hasModifier(java.lang.reflect.Modifier.INTERFACE);
	}
	
	/**
	 * Check whether this method is abstract
	 */
	public boolean isAbstract() {
		return hasModifier(java.lang.reflect.Modifier.ABSTRACT);
	}

	/**
	 * Check whether this method is final
	 */
	public boolean isFinal() {
		return hasModifier(java.lang.reflect.Modifier.FINAL);
	}

	/**
	 * Check whether this method is static
	 */
	public boolean isStatic() {
		return hasModifier(java.lang.reflect.Modifier.STATIC);
	}

	/**
	 * Check whether this method is public
	 */
	public boolean isPublic() {
		return hasModifier(java.lang.reflect.Modifier.PUBLIC);
	}

	/**
	 * Check whether this method is protected
	 */
	public boolean isProtected() {
		return hasModifier(java.lang.reflect.Modifier.PROTECTED);
	}

	/**
	 * Check whether this method is private
	 */
	public boolean isPrivate() {
		return hasModifier(java.lang.reflect.Modifier.PRIVATE);
	}

	/**
	 * Check whether this method is native
	 */
	public boolean isNative() {
		return hasModifier(java.lang.reflect.Modifier.NATIVE);
	}

	/**
	 * Check whether this method is synchronized
	 */
	public boolean isSynchronized() {
		return hasModifier(java.lang.reflect.Modifier.SYNCHRONIZED);
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
		protected ArrayList<Attribute> attributes;
		
		public Field(String name, Type type, List<Modifier> modifiers) {
			this.name = name;
			this.type = type;
			this.modifiers = modifiers;
			this.attributes = new ArrayList<Attribute>();
		}
		
		public String name() {
			return name;
		}

		public Type type() {
			return type;
		}

		public List<Modifier> modifiers() {
			return modifiers;
		}
		
		public List<Attribute> attributes() {
			return attributes;
		}
		
		/**
	     * Check whether this field has one of the "base" modifiers (e.g. static,
	     * public, private, etc). These are found in java.lang.reflect.Modifier.
	     * 
	     * @param modifier
	     * @return true if it does!
	     */
		public boolean hasModifier(int modifier) {
			for(Modifier m : modifiers) {
				if(m instanceof Modifier.Base) {
					Modifier.Base b = (Modifier.Base) m;
					if(b.modifier() == modifier) {
						return true;
					}
				}
			}
			return false;
		}
		
		/**
		 * Check whether this field is abstract
		 */
		public boolean isAbstract() {
			return hasModifier(java.lang.reflect.Modifier.ABSTRACT);
		}

		/**
		 * Check whether this field is final
		 */
		public boolean isFinal() {
			return hasModifier(java.lang.reflect.Modifier.FINAL);
		}

		/**
		 * Check whether this field is static
		 */
		public boolean isStatic() {
			return hasModifier(java.lang.reflect.Modifier.STATIC);
		}

		/**
		 * Check whether this field is public
		 */
		public boolean isPublic() {
			return hasModifier(java.lang.reflect.Modifier.PUBLIC);
		}

		/**
		 * Check whether this field is protected
		 */
		public boolean isProtected() {
			return hasModifier(java.lang.reflect.Modifier.PROTECTED);
		}

		/**
		 * Check whether this field is private
		 */
		public boolean isPrivate() {
			return hasModifier(java.lang.reflect.Modifier.PRIVATE);
		}
	}
	
	public static class Method implements Clazz.Method {
		protected String name;
		protected Type.Function type;
		protected List<Modifier> modifiers;
		protected List<Type.Clazz> exceptions;
		protected ArrayList<Attribute> attributes;		

		public Method(String name, Type.Function type,
				List<Modifier> modifiers, List<Type.Clazz> exceptions) {
			this.name = name;
			this.type = type;
			this.modifiers = modifiers;
			this.exceptions = exceptions;
			attributes = new ArrayList<Attribute>();
		}

		public String name() {
			return name;
		}

		public Type.Function type() {
			return type;
		}

		public List<Modifier> modifiers() {
			return modifiers;
		}
		
		public List<List<Modifier>> parameterModifiers() {
			return new ArrayList();
		}
		
		public List<Type.Clazz> exceptions() {
			return exceptions;
		}
		
		public Attribute attribute(Class c) {
			for(Attribute a : attributes) {
				if(c.isInstance(a)) {
					return a;
				}
			}
			return null;
		}

		public List<Attribute> attributes() {
			return attributes;
		}
		
		/**
	     * Check whether this method has one of the "base" modifiers (e.g. static,
	     * public, private, etc). These are found in java.lang.reflect.Modifier.
	     * 
	     * @param modifier
	     * @return true if it does!
	     */
		public boolean hasModifier(int modifier) {
			for(Modifier m : modifiers) {
				if(m instanceof Modifier.Base) {
					Modifier.Base b = (Modifier.Base) m;
					if(b.modifier() == modifier) {
						return true;
					}
				}
			}
			return false;
		}
		
		/**
		 * Check whether this method is abstract
		 */
		public boolean isAbstract() {
			return hasModifier(java.lang.reflect.Modifier.ABSTRACT);
		}

		/**
		 * Check whether this method is final
		 */
		public boolean isFinal() {
			return hasModifier(java.lang.reflect.Modifier.FINAL);
		}

		/**
		 * Check whether this method is static
		 */
		public boolean isStatic() {
			return hasModifier(java.lang.reflect.Modifier.STATIC);
		}

		/**
		 * Check whether this method is public
		 */
		public boolean isPublic() {
			return hasModifier(java.lang.reflect.Modifier.PUBLIC);
		}

		/**
		 * Check whether this method is protected
		 */
		public boolean isProtected() {
			return hasModifier(java.lang.reflect.Modifier.PROTECTED);
		}

		/**
		 * Check whether this method is private
		 */
		public boolean isPrivate() {
			return hasModifier(java.lang.reflect.Modifier.PRIVATE);
		}

		/**
		 * Check whether this method is native
		 */
		public boolean isNative() {
			return hasModifier(java.lang.reflect.Modifier.NATIVE);
		}

		/**
		 * Check whether this method is synchronized
		 */
		public boolean isSynchronized() {
			return hasModifier(java.lang.reflect.Modifier.SYNCHRONIZED);
		}

		/**
		 * Check whether this method has varargs
		 */
		public boolean isVariableArity() {
			for(Modifier m : modifiers) {
				if(m instanceof Modifier.VarArgs) {				
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
	public ArrayList<Constant.Info> constantPool() {
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
		
		// FIXME: support for inner classes
//		if (clazz.inners().size() > 0 || clazz.isInnerClass()) {
//			Constant.addPoolItem(new Constant.Utf8("InnerClasses"),constantPool);
//			for(Triple<Type.Reference,Integer,Boolean> i : clazz.inners()) {
//				Constant.addPoolItem(Constant.buildClass(i.first()), constantPool);
//				Constant.addPoolItem(new Constant.Utf8(i.first().name()), constantPool);
//			}
//			if(clazz.isInnerClass()) {
//				Type.Reference inner = clazz.type();
//				Pair<String,Type[]>[] classes = clazz.type().classes();
//				for(int i=classes.length-1;i>0;--i) {
//					// First, we need to construct the outer reference type.
//					Pair<String,Type[]>[] nclasses = new Pair[i];
//					System.arraycopy(classes,0,nclasses,0,nclasses.length);				
//					Type.Reference outer = Type.referenceType(inner.pkg(),nclasses);
//					// Now, we can actually write the information.									
//					Constant.addPoolItem(Constant.buildClass(outer), constantPool);
//					Constant.addPoolItem(Constant.buildClass(inner), constantPool);
//					Constant.addPoolItem(new Constant.Utf8(inner.name()), constantPool);									
//					inner = outer;				
//				}
//			}	
//		}
		
		// Now, add all constant pool information for fields
		for (Field f : fields) {
			// Now, add pool items
			Constant.addPoolItem(new Constant.Utf8(f.name()), constantPool);
			Constant.addPoolItem(
					new Constant.Utf8(descriptor(f.type(), false)),
					constantPool);
			for(Attribute a : f.attributes()) {
				a.addPoolItems(constantPool);
			}
		}
		
		for(Method m : methods) {
			// Now, add all constant pool information for methods
			Constant.addPoolItem(new Constant.Utf8(m.name()), constantPool);						
			Constant.addPoolItem(new Constant.Utf8(descriptor(m.type(),
					false)), constantPool);

			for(Attribute a : m.attributes()) {
				a.addPoolItems(constantPool);
			}			
		}
		
		for(Attribute a : attributes) {
			a.addPoolItems(constantPool);
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
			Type.Function ft = (Type.Function) t;
			String r = "(";

			for (Type pt : ft.parameterTypes()) {				
				r += descriptor(pt,generic);
			}

			return r + ")" + descriptor(ft.returnType(),generic);
		} else if(t instanceof Type.Variable) {
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
		}
		 
		throw new RuntimeException("Invalid type passed to Types.descriptor(): " + t);
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
