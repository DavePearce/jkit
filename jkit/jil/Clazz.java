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
     * Access the type of this class. This is useful for determining it's
     * package, and/or any generic parameters it declares.
     * 
     * @return
     */
	public Type type() {
		return type;
	}
	
	/**
     * Access the superclass of this class. This is useful (amongst other
     * things) for determining it's package, and/or any generic parameters it
     * declares. This may be null iff this class represents java.lang.Object.
     * 
     * @return
     */
	public Type superClass() {
		return superClass;
	}
	
	/**
     * Access the modifiers contained in this class object. The returned list
     * may be modified by adding, or removing modifiers. The returned list is
     * always non-null.
     * 
     * @return
     */
	public List<Modifier> modifiers() { return modifiers; }
		
	/**
     * Access the interfaces implemented by this object. The returned list may
     * be modified by adding, or removing interfaces. The returned list is
     * always non-null.
     * 
     * @return
     */
	public List<Type.Clazz> interfaces() { return interfaces; }
	
	/**
     * Access the fields contained in this object. The returned list may be
     * modified by adding, or removing fields. The returned list is always
     * non-null.
     * 
     * @return
     */
	public List<Field> fields() { return fields; }
	
	/**
     * Access the methods contained in this object. The returned list may be
     * modified by adding, or removing methods. The returned list is always
     * non-null.
     * 
     * @return
     */
	public List<Method> methods() { return methods; }
}
