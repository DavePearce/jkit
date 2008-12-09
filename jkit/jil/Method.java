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
     * Access the modifiers contained in this field object. The returned list
     * may be modified by adding, or removing modifiers. The returned list is
     * always non-null.
     * 
     * @return
     */
	public List<Type.Clazz> exceptions() { return exceptions; }
}
