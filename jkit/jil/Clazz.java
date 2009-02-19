package jkit.jil;

import java.util.*;

public class Clazz extends SyntacticElementImpl {
	private List<Modifier> modifiers;
	private Type.Clazz type;
	private Type.Clazz superClass; // maybe null if no supertype (i.e. this is java.lang.Object)
	private List<Type.Clazz> interfaces;
	private List<Field> fields;
	private List<Method> methods;
	
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
	public Clazz(Type.Clazz type, List<Modifier> modifiers,
			Type.Clazz superClass, List<Type.Clazz> interfaces,
			List<Field> fields, List<Method> methods, Attribute... attributes) {		
		super(attributes);
		this.type = type;
		this.modifiers = modifiers;
		this.superClass = superClass;
		this.interfaces = interfaces;
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
	public Clazz(Type.Clazz type, List<Modifier> modifiers,
			Type.Clazz superClass, List<Type.Clazz> interfaces,
			List<Field> fields, List<Method> methods, List<Attribute> attributes) {		
		super(attributes);
		this.type = type;
		this.modifiers = modifiers;
		this.superClass = superClass;
		this.interfaces = interfaces;
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
     * Access the fields contained in this object. The returned list may be
     * modified by adding, or removing fields. The returned list is always
     * non-null.
     * 
     * @return
     */
	public List<Field> fields() { return fields; }
	
    /**
	 * Attempt to find a field declared in this class with the given name;
	 * if no such field exists, return null.
	 * 
	 * @param name
	 * @return
	 */
	public Field getField(String name) {
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
	public List<Method> methods() { return methods; }

	/**
	 * Access the methods contained in this object with a given name. The
	 * returned list may not be modified by adding, or removing methods. The
	 * returned list is always non-null.
	 * 
	 * @return
	 */
	public List<Method> methods(String name) {
		ArrayList<Method> r = new ArrayList();
		for(Method m : methods) {
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

}
