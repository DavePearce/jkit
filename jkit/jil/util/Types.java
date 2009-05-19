package jkit.jil.util;

import java.util.*;

import static jkit.compiler.SyntaxError.*;
import jkit.jil.tree.SyntacticElement;
import jkit.jil.tree.Type;
import jkit.util.Pair;

public class Types {
	
	/**
     * Given a primitive type, determine the equivalent boxed type. For example,
     * the primitive type int yields the type java.lang.Integer. 
     * 
     * @param p
     * @return
     */
	public static Type.Clazz boxedType(Type.Primitive p) {
		if(p instanceof Type.Bool) {
			return new Type.Clazz("java.lang","Boolean");
		} else if(p instanceof Type.Byte) {
			return new Type.Clazz("java.lang","Byte");
		} else if(p instanceof Type.Char) {
			return new Type.Clazz("java.lang","Character");
		} else if(p instanceof Type.Short) {
			return new Type.Clazz("java.lang","Short");
		} else if(p instanceof Type.Int) {
			return new Type.Clazz("java.lang","Integer");
		} else if(p instanceof Type.Long) {
			return new Type.Clazz("java.lang","Long");
		} else if(p instanceof Type.Float) {
			return new Type.Clazz("java.lang","Float");
		} else {
			return new Type.Clazz("java.lang","Double");
		}
	}
	
	/**
	 * Given a primitive wrapper class (i.e. a boxed type), return the unboxed
	 * equivalent. For example, java.lang.Integer yields Type.Int, whilst
	 * java.lang.Boolean yields Type.Bool.
	 * 
	 * @param p
	 * @return
	 */
	public static Type.Primitive unboxedType(Type.Clazz p, SyntacticElement e) {
		String type = p.components().get(p.components().size()-1).first();
		
		if(type.equals("Boolean")) {
			return new Type.Bool();
		} else if(type.equals("Byte")) {
			return new Type.Byte();
		} else if(type.equals("Character")) {
			return new Type.Char();
		} else if(type.equals("Short")) {
			return new Type.Short();
		} else if(type.equals("Integer")) {
			return new Type.Int();
		} else if(type.equals("Long")) {
			return new Type.Long();
		} else if(type.equals("Float")) {
			return new Type.Float();
		} else if(type.equals("Double")) {
			return new Type.Double();
		} else {
			syntax_error("unknown boxed type \"" + p.toString()
					+ "\" encountered.",e);
			return null; // very dead!
		}
	}
	
	/**
	 * Determine whether or not the given type is a wrapper for a primitive
	 * type.  E.g. java.lang.Integer is a wrapper for int.
	 * 
	 * @param t
	 * @return
	 */
	public static boolean isBoxedType(Type t) {
		if(!(t instanceof Type.Clazz)) {
			return false;
		}
		Type.Clazz ref = (Type.Clazz) t;
		if(ref.pkg().equals("java.lang") && ref.components().size() == 1) {
			String s = ref.components().get(0).first();
			if(s.equals("Byte") || s.equals("Character") || s.equals("Short") ||
				s.equals("Integer") || s.equals("Long")
					|| s.equals("Float") || s.equals("Double")
					|| s.equals("Boolean")) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Determine whether or not the given type is a wrapper for a primitive
	 * type. E.g. java.lang.Integer is a wrapper for int.
	 * 
	 * @param t
	 *            --- type to test
	 * @param wrapper
	 *            --- specific wrapper class to look for (i.e. Integer, Boolean,
	 *            Character).
	 * @return
	 */
	public static boolean isBoxedTypeOf(Type t, String wrapper) {
		if(!(t instanceof Type.Clazz)) {
			return false;
		}
		Type.Clazz ref = (Type.Clazz) t;
		if(ref.pkg().equals("java.lang") && ref.components().size() == 1) {
			String s = ref.components().get(0).first();
			if(s.equals(wrapper)) {
				return true;
			}
		}
		return false;
	}
	
	/**
     * Check wither a given type is a reference to java.lang.Object or not.
     * 
     * @param t
     * @return
     */
	public static boolean isJavaLangObject(Type t) {
		if(t instanceof Type.Clazz) {
			Type.Clazz c = (Type.Clazz) t;
			 return c.pkg().equals("java.lang") && c.components().size() == 1
					&& c.components().get(0).first().equals("Object");			
		}
		return false;
	}
	
	/**
     * Check wither a given type is a reference to java.lang.String or not.
     * 
     * @param t
     * @return
     */
	public static boolean isJavaLangString(Type t) {
		if(t instanceof Type.Clazz) {
			Type.Clazz c = (Type.Clazz) t;
			 return c.pkg().equals("java.lang") && c.components().size() == 1
					&& c.components().get(0).first().equals("String");			
		}
		return false;
	}
	
	public static boolean isClass(String pkg, String clazz, Type t) {
		if(t instanceof Type.Clazz) {
			Type.Clazz c = (Type.Clazz) t;
			 return c.pkg().equals(pkg) && c.components().size() == 1
					&& c.components().get(0).first().equals(clazz);
		}
		return false;
	}
	
	/**
	 * Return the depth of array nesting. E.g. "int" has 0 depth, "int[]" has
	 * depth 1, "int[][]" has depth 2, etc.
	 * 
	 * @param t
	 * @return
	 */
	public static int arrayDepth(Type t) {
		if(t instanceof Type.Array) {
			Type.Array _t = (Type.Array) t;
			return 1 + arrayDepth(_t.element());
		} else {
			return 0;
		}
	}	
	
	/**
	 * Return type representing the enclosing class for the given type, or null
	 * if the given type is already outermost. For example, given
	 * <code>pkg.Test$inner</code> return <code>pkg.Test</code>.
	 * 
	 * @param t
	 * @return
	 */
	public static Type.Clazz parentType(Type.Clazz t) {
		List<Pair<String,List<Type.Reference>>> components = t.components(); 
		if(components.size() == 0) {
			return null;
		}
		ArrayList<Pair<String,List<Type.Reference>>> ncomponents = new ArrayList();
		for(int i=0;i!=components.size()-1;++i) {
			ncomponents.add(components.get(i));
		}
		return new Type.Clazz(t.pkg(),ncomponents);
	}	
	
	/**
	 * The following are provided for performance reasons, particularly to help
	 * reduce the memory footprint during compilation.
	 */
	public static final Type.Void T_VOID = new Type.Void();
	public static final Type.Null T_NULL = new Type.Null();
	public static  final Type.Bool T_BOOL = new Type.Bool();
	public static  final Type.Byte T_BYTE = new Type.Byte();
	public static  final Type.Char T_CHAR = new Type.Char();
	public static  final Type.Short T_SHORT = new Type.Short();
	public static  final Type.Int T_INT = new Type.Int();
	public static  final Type.Long T_LONG = new Type.Long();
	public static  final Type.Float T_FLOAT = new Type.Float();
	public static  final Type.Double T_DOUBLE = new Type.Double();
	
	public static final Type.Clazz JAVA_LANG_OBJECT = new Type.Clazz("java.lang","Object");
	public static final Type.Clazz JAVA_LANG_CLONEABLE = new Type.Clazz("java.lang","Cloneable");
	public static final Type.Clazz JAVA_LANG_STRING = new Type.Clazz("java.lang","String");
	
	public static final Type.Clazz JAVA_LANG_BOOLEAN = new Type.Clazz("java.lang","Boolean");
	public static final Type.Clazz JAVA_LANG_CHARACTER = new Type.Clazz("java.lang","Character");
	public static final Type.Clazz JAVA_LANG_BYTE = new Type.Clazz("java.lang","Byte");
	public static final Type.Clazz JAVA_LANG_SHORT = new Type.Clazz("java.lang","Short");
	public static final Type.Clazz JAVA_LANG_INTEGER = new Type.Clazz("java.lang","Integer");
	public static final Type.Clazz JAVA_LANG_LONG = new Type.Clazz("java.lang","Long");
	public static final Type.Clazz JAVA_LANG_FLOAT = new Type.Clazz("java.lang","Float");
	public static final Type.Clazz JAVA_LANG_DOUBLE = new Type.Clazz("java.lang","Double");
	
	// io
	public static final Type.Clazz JAVA_IO_SERIALIZABLE = new Type.Clazz("java.io","Serializable");
	
	// util
	public static final Type.Clazz JAVA_UTIL_ITERATOR = new Type.Clazz("java.util","Iterator");
	
	// exceptions related types
	public static  final Type.Clazz JAVA_LANG_THROWABLE = new Type.Clazz("java.lang","Throwable");
	public static  final Type.Clazz JAVA_LANG_RUNTIMEEXCEPTION = new Type.Clazz("java.lang","RuntimeException");
	public static  final Type.Clazz JAVA_LANG_VIRTUALMACHINEERROR = new Type.Clazz("java.lang","VirtualMachineError");
	public static  final Type.Clazz JAVA_LANG_NULLPOINTEREXCEPTION = new Type.Clazz("java.lang","NullPointerException");
	public static  final Type.Clazz JAVA_LANG_ARITHMETICEXCEPTION = new Type.Clazz("java.lang","ArithmeticException");
	
}