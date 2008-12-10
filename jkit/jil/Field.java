package jkit.jil;

import java.util.*;

public class Field extends SyntacticElementImpl {
	private String name;
	private Type type;
	private List<Modifier> modifiers;
	
	/**
     * Construct an object representing a field of a JVM class.
     * 
     * @param name - The name of the field.
     * @param type - The (fully generic) type of the field.
     * @param modifiers - Any modifiers of the field (e.g. public, static, etc)
     */
	public Field(String name, Type type, List<Modifier> modifiers,
			Attribute... attributes) {
		super(attributes);
		this.name = name;
		this.type = type;
		this.modifiers = modifiers;
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
	public Type type() {
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
