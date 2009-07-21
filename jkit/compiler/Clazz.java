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

package jkit.compiler;

import java.util.List;
import jkit.jil.tree.Modifier;
import jkit.jil.tree.Type;

/**
 * The represents a class which can be represented in a variety of different
 * ways. For example, as a ClassFile or as a JilClass.
 * 
 * @author djp
 */
public interface Clazz {
	/**
     * Access the type of this class. This is useful for determining it's
     * package, and/or any generic parameters it declares.
     * 
     * @return
     */
	public Type.Clazz type();
	
	/**
     * Name of class. This is just a convenience method. 
     * 
     * @return
     */
	public String name();
	
	/**
     * Access the superclass of this class. This is useful (amongst other
     * things) for determining it's package, and/or any generic parameters it
     * declares. This may be null iff this class represents java.lang.Object.
     * 
     * @return
     */
	public Type.Clazz superClass();
	
	/**
     * Access the interfaces implemented by this object. The returned list may
     * be modified by adding, or removing interfaces. The returned list is
     * always non-null.
     * 
     * @return
     */
	public List<Type.Clazz> interfaces();
	
	/**
     * Access the inner classes of this clazz. The returned list may
     * be modified by adding, or removing interfaces. The returned list is
     * always non-null.
     * 
     * @return
     */
	public List<Type.Clazz> inners();
	
	/**
     * Access the fields contained in this object. The returned list may be
     * modified by adding, or removing fields. The returned list is always
     * non-null.
     * 
     * @return
     */
	public List<? extends Field> fields();
	
	/**
	 * Access the field contained in this object with the given name. If no such
	 * field exists, return null.
	 * 
	 * @return
	 */
	public Field field(String name);
	
	/**
     * Access the methods contained in this object. The returned list may be
     * modified by adding, or removing methods. The returned list is always
     * non-null.
     * 
     * @return
     */
	public List<? extends Method> methods();
	
	/**
	 * Access the methods contained in this object with a given name. The
	 * returned list may be modified by adding, or removing methods. The
	 * returned list is always non-null.
	 * 
	 * @return
	 */
	public List<? extends Method> methods(String name);
	
	/**
     * Access the modifiers contained in this class object. The returned list
     * may be modified by adding, or removing modifiers. The returned list is
     * always non-null.
     * 
     * @return
     */
	public List<Modifier> modifiers();
	
	/**
     * Check whether this method has one of the "base" modifiers (e.g. static,
     * public, private, etc). These are found in java.lang.reflect.Modifier.
     * 
     * @param modifier
     * @return true if it does!
     */
	public boolean hasModifier(Class modifier);
	
	/**
	 * Check whether this method is abstract
	 */
	public boolean isInterface();
	
	/**
	 * Check whether this method is abstract
	 */
	public boolean isAbstract();

	/**
	 * Check whether this method is final
	 */
	public boolean isFinal();

	/**
	 * Check whether this method is static
	 */
	public boolean isStatic();

	/**
	 * Check whether this method is public
	 */
	public boolean isPublic();

	/**
	 * Check whether this method is protected
	 */
	public boolean isProtected();

	/**
	 * Check whether this method is private
	 */
	public boolean isPrivate();

	/**
	 * Check whether this method is native
	 */
	public boolean isNative();

	/**
	 * Check whether this method is synchronized
	 */
	public boolean isSynchronized();
	
	/**
	 * Check whether or not this is an inner class.
	 * @return
	 */
	public boolean isInnerClass();
	
	public static interface Field {
		/**
	     * Access the name of this field.  
	     * 
	     * @return
	     */
		public String name();
		
		/**
	     * Access the type of this field. This is useful for determining it's
	     * package, and/or any generic parameters it declares.
	     * 
	     * @return
	     */
		public Type type();
		
		/**
	     * Access the modifiers contained in this field object. The returned list
	     * may be modified by adding, or removing modifiers. The returned list is
	     * always non-null.
	     * 
	     * @return
	     */
		public List<Modifier> modifiers();
		
		/**
	     * Check whether this field has one of the "base" modifiers (e.g. static,
	     * public, private, etc). These are found in java.lang.reflect.Modifier.
	     * 
	     * @param modifier
	     * @return true if it does!
	     */
		public boolean hasModifier(Class modifier);
		
		/**
		 * Check whether this field is abstract
		 */
		public boolean isAbstract();

		/**
		 * Check whether this field is final
		 */
		public boolean isFinal();

		/**
		 * Check whether this field is static
		 */
		public boolean isStatic();

		/**
		 * Check whether this field is public
		 */
		public boolean isPublic();

		/**
		 * Check whether this field is protected
		 */
		public boolean isProtected();

		/**
		 * Check whether this field is private
		 */
		public boolean isPrivate();
		
		/**
		 * Check whether this field is a constant or not.
		 */
		public boolean isConstant();
		
		/**
		 * Get the constant value for this field.
		 */
		public Object constant();
	}
	
	public static interface Method {
		/**
	     * Access the name of this method.  
	     * 
	     * @return
	     */
		public String name();
		
		/**
	     * Access the type of this method. This is useful for determining it's
	     * package, and/or any generic parameters it declares.
	     * 
	     * @return
	     */
		public Type.Function type();
		
		/**
	     * Access the modifiers contained in this method object. The returned list
	     * may be modified by adding, or removing modifiers. The returned list is
	     * always non-null.
	     * 
	     * @return
	     */
		public List<Modifier> modifiers();
				
		/**
	     * Access the modifiers contained in this method object. The returned list
	     * may be modified by adding, or removing modifiers. The returned list is
	     * always non-null.
	     * 
	     * @return
	     */
		public List<Type.Clazz> exceptions();
				
		/**
	     * Check whether this method has one of the "base" modifiers (e.g. static,
	     * public, private, etc). These are found in java.lang.reflect.Modifier.
	     * 
	     * @param modifier
	     * @return true if it does!
	     */
		public boolean hasModifier(Class modifier);
		
		/**
		 * Check whether this method is abstract
		 */
		public boolean isAbstract();
		
		/**
		 * Check whether this method is final
		 */
		public boolean isFinal();

		/**
		 * Check whether this method is static
		 */
		public boolean isStatic();

		/**
		 * Check whether this method is public
		 */
		public boolean isPublic();

		/**
		 * Check whether this method is protected
		 */
		public boolean isProtected();

		/**
		 * Check whether this method is private
		 */
		public boolean isPrivate();

		/**
		 * Check whether this method is native
		 */
		public boolean isNative();

		/**
		 * Check whether this method is synchronized
		 */
		public boolean isSynchronized();

		/**
		 * Check whether this method has varargs
		 */
		public boolean isVariableArity();
	}
}
