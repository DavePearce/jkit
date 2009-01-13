package jkit.jil;

import java.util.List;

public class Method extends SyntacticElementImpl {
	private String name;
	private Type.Function type;
	private List<Modifier> modifiers;
	private List<Type.Clazz> exceptions;
	
	/**
     * Construct an object representing a field of a JVM class.
     * 
     * @param name -
     *            The name of the method.
     * @param type -
     *            The (fully generic) function type of this method.
     * @param modifiers -
     *            Any modifiers of the method (e.g. public, static, etc)
     * @param exceptions -
     *            The (non-null) list of exceptions thrown by this method.
     */
	public Method(String name, Type.Function type, List<Modifier> modifiers,
			List<Type.Clazz> exceptions, Attribute... attributes) {
		super(attributes);
		this.name = name;
		this.type = type;
		this.modifiers = modifiers;
		this.exceptions = exceptions;
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
	
	/**
     * Access the modifiers contained in this field object. The returned list
     * may be modified by adding, or removing modifiers. The returned list is
     * always non-null.
     * 
     * @return
     */
	public List<Modifier> modifiers() { return modifiers; }
	
	/**
     * Access the modifiers contained in this field object. The returned list
     * may be modified by adding, or removing modifiers. The returned list is
     * always non-null.
     * 
     * @return
     */
	public List<Type.Clazz> exceptions() { return exceptions; }
	
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
		// note, ACC_TRANSIENT is same mask as ACC_VARARGS in vm spec.
		return hasModifier(java.lang.reflect.Modifier.TRANSIENT);
	}

}
