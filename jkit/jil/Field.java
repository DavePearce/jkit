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
}
