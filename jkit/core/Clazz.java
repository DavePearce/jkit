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
// (C) David James Pearce, 2007. 

package jkit.core;

import java.lang.reflect.Modifier;
import java.util.*;

import jkit.util.*;

/**
 * This class stores all known information about a particular class, including
 * it's full (possibly generic) type, the type of its super class and any
 * interfaces it implements; you can iterate the fields and methods of the
 * class, and access the flow graph (if there is one).
 * 
 * Clazz objects are typically constructed from Java source files (via
 * JavaFileReader) or Java class files (via ClassFileReader). However, you can
 * construct your own if you want to create a class from scratch!
 * 
 * @author djp
 * 
 */
public class Clazz {
	private int modifiers;
	private Type.Reference type;
	private Type.Reference superClass;
	private ArrayList<Type.Reference> interfaces = new ArrayList<Type.Reference>();
	/**
     * The list of this class's inner classes, where each is a Triple (r,m,a).
     * here, r identifies inner class, m gives it's modifiers whilst a indicates
     * whether it is anonymous or not.
     * 
     * Observe that the modifier value is required since the class file
     * representing the inner class cannot specify whether or not it is e.g. a
     * static inner class.
     */
	private ArrayList<Triple<Type.Reference, Integer, Boolean>> inners = new ArrayList<Triple<Type.Reference, Integer, Boolean>>();
	private ArrayList<Method> methods = new ArrayList<Method>();
	private ArrayList<Field> fields = new ArrayList<Field>();
	private boolean anonymous = false;

	/**
     * Construct a Clazz object without any methods, fields, interfaces etc. Use
     * java.lang.reflect.Modifier to specific the modifiers.
     * 
     * @param modifiers modifiers for class (e.g. public, protected, static etc)
     * @param type full type of class
     * @param superClass full type of super class (or null if there is none)
     */
	public Clazz(int modifiers, Type.Reference type, Type.Reference superClass) {
		this.modifiers = modifiers;
		this.type = type;
		this.superClass = superClass;		
	}

	/**
     * Construct a Clazz object without any methods, fields, interfaces etc. Use
     * java.lang.reflect.Modifier to specific the modifiers.
     * 
     * @param modifiers modifiers for class (e.g. public, protected, static etc)
     * @param type full type of class
     * @param superClass full type of super class (or null if there is none)
     */
	
	/**
	 * Construct a Clazz object without any methods or fields etc. Use
	 * java.lang.reflect.Modifier to specific the modifiers.
	 * 
	 * @param modifiers
	 *            modifiers for class (e.g. public, protected, static etc)
	 * @param type
	 *            full type of class
	 * @param superClass
	 *            full type of super class (or null if there is none)
	 * @param interfaces
	 *            full types of interfaces implemented by this class
	 */
	public Clazz(int modifiers, Type.Reference type, Type.Reference superClass,
			List<Type.Reference> interfaces) {
		this.modifiers = modifiers;
		this.type = type;
		this.superClass = superClass;
		this.interfaces.addAll(interfaces);
	}
	
	/**
	 * Construct a Clazz object without any methods or fields etc. Use
	 * java.lang.reflect.Modifier to specific the modifiers.
	 * 
	 * @param modifiers
	 *            modifiers for class (e.g. public, protected, static etc)
	 * @param type
	 *            full type of class
	 * @param superClass
	 *            full type of super class (or null if there is none)
	 * @param interfaces
	 *            full types of interfaces implemented by this class
	 * @param anon
	 *            specify whether this is an anonymous class or not
	 *	
	 */
	public Clazz(int modifiers, Type.Reference type, Type.Reference superClass,
			List<Type.Reference> interfaces, boolean anon) {
		this(modifiers, type, superClass, interfaces);
		anonymous = anon;
	}
	
	/**
	 * Construct a Clazz empty object. Use java.lang.reflect.Modifier to
	 * specific the modifiers.
	 * 
	 * 
	 * @param modifiers
	 *            modifiers for class (e.g. public, protected, static etc)
	 * @param type
	 *            full type of class
	 * @param superClass
	 *            full type of super class (or null if there is none)
	 * @param interfaces
	 *            full types of interfaces implemented by this class
	 * @param fields
	 *            Field objects representing fields of this class
	 */	
	public Clazz(int modifiers, Type.Reference type, Type.Reference superClass,
			List<Type.Reference> interfaces, List<Field> fields) {

		this.modifiers = modifiers;
		this.type = type;
		this.superClass = superClass;
		this.interfaces.addAll(interfaces);
		this.fields.addAll(fields);		
	}
	
	/**
	 * Construct a Clazz object without innerclasses. Use
	 * java.lang.reflect.Modifier to specific the modifiers.
	 * 
	 * @param modifiers
	 *            modifiers for class (e.g. public, protected, static etc)
	 * @param type
	 *            full type of class
	 * @param superClass
	 *            full type of super class (or null if there is none)
	 * @param interfaces
	 *            full types of interfaces implemented by this class
	 * @param fields
	 *            Field objects representing fields of this class
	 * @param methods
	 *            Method objects representing methods of this class
	 */	
	public Clazz(int modifiers, Type.Reference type, Type.Reference superClass,
			List<Type.Reference> interfaces, List<Field> fields, List<Method> methods) {

		this.modifiers = modifiers;
		this.type = type;
		this.superClass = superClass;
		this.interfaces.addAll(interfaces);
		this.fields.addAll(fields);
		this.methods.addAll(methods);			
	}
	
	/**
	 * Construct a Clazz object with everything. Use java.lang.reflect.Modifier
	 * to specific the modifiers.
	 * 
	 * 
	 * @param modifiers
	 *            modifiers for class (e.g. public, protected, static etc)
	 * @param type
	 *            full type of class
	 * @param superClass
	 *            full type of super class (or null if there is none)
	 * @param interfaces
	 *            full types of interfaces implemented by this class
	 * @param fields
	 *            Field objects representing fields of this class
	 * @param methods
	 *            Method objects representing methods of this class
	 * @param innerClasses
	 *            Triple (r,m,a), where r identifies inner class, m gives it's
	 *            modifiers whilst a indicates whether it is anonymous or not.
	 */	
	public Clazz(int modifiers, Type.Reference type, Type.Reference superClass,
			List<Type.Reference> interfaces, List<Field> fields, List<Method> methods,
			List<Triple<Type.Reference,Integer,Boolean>> innerClasses) {

		this.modifiers = modifiers;
		this.type = type;
		this.superClass = superClass;
		this.interfaces.addAll(interfaces);
		this.fields.addAll(fields);
		this.methods.addAll(methods);
		this.inners.addAll(innerClasses);		
	}

	/**
	 * Check whether this is an interface
	 */
	public boolean isInterface() { return (modifiers&Modifier.INTERFACE)!=0; } 

	/**
	 * Check whether this class or interface is abstract
	 */
	public boolean isAbstract() { return (modifiers&Modifier.ABSTRACT)!=0; }
	
	/**
	 * Check whether this class or interface is final
	 */
	public boolean isFinal() { return (modifiers&Modifier.FINAL)!=0; }
	
	/**
	 * Check whether this class or interface is static
	 */
	public boolean isStatic() { return (modifiers&Modifier.STATIC)!=0; }
	
	/**
	 * Check whether this class or interface is static
	 */
	public boolean isPublic() { return (modifiers&Modifier.PUBLIC)!=0; }
		
	/**
	 * Check whether this is an inner class or interface
	 */
	public boolean isInnerClass() { return type.classes().length > 1; }
	
	/**
	 * Check whether this is an anonymous class.
	 */
	public boolean isAnonymous() { return anonymous; }
	
	/**
     * Modifiers on class (e.g. static, public etc). Decode them using
     * java.lang.reflect.Modifier.
     * 
     * @return
     */
	public int modifiers() { return modifiers; }	
	
	/**
     * set Modifiers on class (e.g. static, public etc). Decode them using
     * java.lang.reflect.Modifier.
     * 
     * @return
     */
	public void setModifiers(int modifiers) { this.modifiers = modifiers; }
	
	/**
     * Specify whether this is an anonymous class or not.
     * 
     * @return
     */
	public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }
		
	/**
	 * Full type of class, including generic type arguments
	 * @return
	 */
	public Type.Reference type() {
		return type;
	}
	
	/**
     * Name of class. This is just a convenience method, and is equivalent to
     * using: type().classes()[type().classes().length-1].first() 
     * 
     * @return
     */
	public String name() { return type().classes()[type().classes().length-1].first(); }
	
	/**
	 * Super class of this class, including substituted generic type arguments.
	 * @return superClass type, or null (if this is java.lang.Object) 
	 */
	public Type.Reference superClass() {
		return superClass;
	}
	
	/**
	 * Interfaces implemented by this class, including substituted generic type arguments.
	 * @return
	 */
	public List<Type.Reference> interfaces() { return interfaces; }
	
	/**
	 * Fields declared by this class.
	 * @return
	 */
	public List<Field> fields() { return fields; }
	
	/**
	 * Fields with a particular name declared by this class.
	 * @return
	 */
	public List<Field> fields(String name) { 
		ArrayList<Field> matches = new ArrayList<Field>();
		for(Field f: fields) {
			if(f.name().equals(name)) {
				matches.add(f);
			}
		}
		return matches;
	}
	
	/**
	 * Methods declared by this class.
	 * @return
	 */
	public List<Method> methods() { return methods; }
	
	/**
	 * Methods with a particular name declared by this class.
	 * @return
	 */
	public List<Method> methods(String name) { 
		ArrayList<Method> matches = new ArrayList<Method>();
		for(Method m : methods) {
			if(m.name().equals(name)) {
				matches.add(m);
			}
		}
		return matches;
	}
	
	
	/**
	 * Get information about inner classes contained in this class. For each
	 * inner classes, there is a triple (r,m,a) where: "r" identifies the class,
	 * "m" gives the modifiers for the class and "a" specifies whether it is
	 * anonymous or not.
	 * 
	 * @return
	 */
	public List<Triple<Type.Reference,Integer,Boolean>> inners() { return inners; }
	
	/**
     * Return the reference type representing the enclosing class for this inner
     * class. If this class has multiple enclosing classes, only the most
     * immediate is returned.
     */
	public Type.Reference enclosingClass() {
		Pair<String,Type[]>[] classes = type.classes();
		Pair<String,Type[]>[] nclasses = new Pair[classes.length-1];
		System.arraycopy(classes,0,nclasses,0,nclasses.length);
		return Type.referenceType(type.pkg(),nclasses);
	}
	
	
	/**
	 * Return the Field object representing the field with the given name, or
	 * null if no such field exists.
	 */
	public Field getField(String name) {
		for(Field f : fields) {
			if(f.name().equals(name)) {
				return f;
			}
		}
		return null;
	}
}
