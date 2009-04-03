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
     * Access the fields contained in this object. The returned list may be
     * modified by adding, or removing fields. The returned list is always
     * non-null.
     * 
     * @return
     */
	public List<Field> fields();
	
	/**
     * Access the methods contained in this object. The returned list may be
     * modified by adding, or removing methods. The returned list is always
     * non-null.
     * 
     * @return
     */
	public List<Method> methods();
	
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
	public boolean hasModifier(int modifier);
	
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
		public boolean hasModifier(int modifier);
		
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
		public boolean hasModifier(int modifier);
		
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
