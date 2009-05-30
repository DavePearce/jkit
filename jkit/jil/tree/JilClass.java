package jkit.jil.tree;

import java.util.*;

public class JilClass extends SyntacticElementImpl implements jkit.compiler.Clazz {
	private List<Modifier> modifiers;
	private Type.Clazz type;
	private Type.Clazz superClass; // maybe null if no supertype (i.e. this is java.lang.Object)
	private List<Type.Clazz> interfaces;
	private List<Type.Clazz> inners;
	private List<JilField> fields;
	private List<JilMethod> methods;
	
	/**
     * Create an object representing a Class in the Java Virtual Machine.
     * 
     * @param type -
     *            The type of the class, including any generic parameters.
     * @param modifiers -
     *            Any modifiers on the class (e.g. final, static, public,
     *            \@NonNull, etc). This list should be non-null regardless.
     * 
     * @param superClass -
     *            The superclass for this class. This maybe null only if this
     *            object represents java.lang.Object.
     * 
     * @param interfaces -
     *            Any interfaces for this object. This list should be non-null
     *            regardless.
     * 
     * @param fields -
     *            Any fields contained in this object. This list should be
     *            non-null regardless.
     * 
     * @param methods -
     *            Any methods contained in this object. This list should be
     *            non-null regardless.
     */
	public JilClass(Type.Clazz type, List<Modifier> modifiers,
			Type.Clazz superClass, List<Type.Clazz> interfaces,
			List<Type.Clazz> inners, List<JilField> fields,
			List<JilMethod> methods, Attribute... attributes) {
		super(attributes);
		this.type = type;
		this.modifiers = modifiers;
		this.superClass = superClass;
		this.interfaces = interfaces;
		this.inners = inners;
		this.fields = fields;
		this.methods = methods;
	}
	
	/**
     * Create an object representing a Class in the Java Virtual Machine.
     * 
     * @param type -
     *            The type of the class, including any generic parameters.
     * @param modifiers -
     *            Any modifiers on the class (e.g. final, static, public,
     *            \@NonNull, etc). This list should be non-null regardless.
     * 
     * @param superClass -
     *            The superclass for this class. This maybe null only if this
     *            object represents java.lang.Object.
     * 
     * @param interfaces -
     *            Any interfaces for this object. This list should be non-null
     *            regardless.
     * 
     * @param fields -
     *            Any fields contained in this object. This list should be
     *            non-null regardless.
     * 
     * @param methods -
     *            Any methods contained in this object. This list should be
     *            non-null regardless.
     */
	public JilClass(Type.Clazz type, List<Modifier> modifiers,
			Type.Clazz superClass, List<Type.Clazz> interfaces, List<Type.Clazz> inners,
			List<JilField> fields, List<JilMethod> methods, List<Attribute> attributes) {		
		super(attributes);
		this.type = type;
		this.modifiers = modifiers;
		this.superClass = superClass;
		this.interfaces = interfaces;
		this.inners = inners;
		this.fields = fields;
		this.methods = methods;
	}
	/**
     * Access the type of this class. This is useful for determining it's
     * package, and/or any generic parameters it declares.
     * 
     * @return
     */
	public Type.Clazz type() {
		return type;
	}
	
	public void setType(Type.Clazz type) {
		this.type = type;
	}
	
	/**
     * Name of class. This is just a convenience method. 
     * 
     * @return
     */
	public String name() {
		return type.components().get(type.components().size() - 1).first();
	}	
	
	/**
     * Access the superclass of this class. This is useful (amongst other
     * things) for determining it's package, and/or any generic parameters it
     * declares. This may be null iff this class represents java.lang.Object.
     * 
     * @return
     */
	public Type.Clazz superClass() {
		return superClass;
	}
	
	public void setSuperClass(Type.Clazz superClass) {
		this.superClass = superClass;
	}	
	
	/**
     * Access the interfaces implemented by this object. The returned list may
     * be modified by adding, or removing interfaces. The returned list is
     * always non-null.
     * 
     * @return
     */
	public List<Type.Clazz> interfaces() { return interfaces; }
	
	public void setInterfaces(List<Type.Clazz> interfaces) {
		this.interfaces = interfaces;
	}
	
	/**
	 * Access the inner classes contained in this Clazz. The returned list may
	 * be modified by adding, or removing interfaces. The returned list is
	 * always non-null.
	 * 
	 * @return
	 */
	public List<Type.Clazz> inners() { return inners; }
	
	
	/**
     * Access the fields contained in this object. The returned list may be
     * modified by adding, or removing fields. The returned list is always
     * non-null.
     * 
     * @return
     */
	public List<JilField> fields() { return fields; }
	
    /**
	 * Attempt to find a field declared in this class with the given name;
	 * if no such field exists, return null.
	 * 
	 * @param name
	 * @return
	 */
	public Field field(String name) {
		for(Field f : fields) {							
			if (f.name().equals(name)) {
				return f;
			}
		}
		return null;
	}
	
	/**
     * Access the methods contained in this object. The returned list may be
     * modified by adding, or removing methods. The returned list is always
     * non-null.
     * 
     * @return
     */
	public List<JilMethod> methods() { return methods; }

	/**
	 * Access the methods contained in this object with a given name. The
	 * returned list may not be modified by adding, or removing methods. The
	 * returned list is always non-null.
	 * 
	 * @return
	 */
	public List<JilMethod> methods(String name) {
		ArrayList<JilMethod> r = new ArrayList();
		for(JilMethod m : methods) {
			if(m.name().equals(name)) {				
				r.add(m);
			}
		}
		return r; 
	}	

	/**
     * Access the modifiers contained in this class object. The returned list
     * may be modified by adding, or removing modifiers. The returned list is
     * always non-null.
     * 
     * @return
     */
	public List<Modifier> modifiers() { return modifiers; }
	
	public void setModifiers(List<Modifier> modifiers) {
		this.modifiers = modifiers;
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
	 * Check whether or not this is an inner class.
	 * @return
	 */
	public boolean isInnerClass() {
		return type.components().size() > 1;
	}

}
