package jkit.util.bytecode;

import jkit.core.*;
import jkit.util.Pair;

/**
 * This class provides various functions relating to Types in the JVM.
 * 
 * @author djp
 * 
 */
public class Types {
	
	/**
	 * Determine the slot size for the corresponding Java type.
	 * 
	 * @param type
	 *            The type to determine the slot size for.
	 * @return the slot size in slots.
	 */
	public static int slotSize(Type type) {
		if(type instanceof Type.Double || type instanceof Type.Long) {
			return 2;
		} else {
			return 1;
		}
	}
	
	/**
	 * This method returns a JVM descriptor string for the type in question. The
	 * format of the string is defined in "The JavaTM Virtual Machine
	 * Specification, 2nd ed", Section 4.3. Example descriptor strings include:
	 * 
	 * <table>
	 * <tr>
	 * <td><b>Type</b></td>
	 * <td><b>Descriptor</b></td>
	 * </tr>
	 * <tr>
	 * <td>int
	 * <tr>
	 * <td>I</td>
	 * <tr>
	 * <tr>
	 * <td>boolean
	 * <tr>
	 * <td>Z</td>
	 * <tr>
	 * <tr>
	 * <td>float[]
	 * <tr>
	 * <td>F[</td>
	 * <tr>
	 * <tr>
	 * <td>java.lang.Integer
	 * <tr>
	 * <td>Ljava/lang/Integer;</td>
	 * <tr>
	 * <tr>
	 * <td>int(Double,Float)
	 * <tr>
	 * <td>(DF)I</td>
	 * <tr> </table>
	 * <p>
	 * The descriptor string is used, amongst other things, to uniquely identify
	 * a class in the ClassTable.
	 * </p>
	 * 
	 * See the <a
	 * href="http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#1169">JVM
	 * Specification</a> for more information.
	 * 
	 * @param t
	 *            The type to generate the descriptor for
	 * @param generic
	 *            True indicates generic information should be included.
	 * @return
	 */
	public static String descriptor(Type t, boolean generic) {
		if(t instanceof Type.Boolean) {
			return "Z";
		} if(t instanceof Type.Byte) {
			return "B";
		} else if(t instanceof Type.Char) {
			return "C";
		} else if(t instanceof Type.Short) {
			return "S";
		} else if(t instanceof Type.Int) {
			return "I";
		} else if(t instanceof Type.Long) {
			return "J";
		} else if(t instanceof Type.Float) {
			return "F";
		} else if(t instanceof Type.Double) {
			return "D";
		} else if(t instanceof Type.Void) {
			return "V";
		} else if(t instanceof Type.Array) {
			Type.Array at = (Type.Array) t;
			return "[" + descriptor(at.elementType(),generic);
		} else if(t instanceof Type.Reference) {
			Type.Reference ref = (Type.Reference) t;
			String r = "L" + ref.pkg().replace(".","/");
			Pair<String, Type[]>[] classes = ref.classes();
			for (int i = 0; i != classes.length; ++i) {
				if (i == 0 && r.length() > 1) {
					r += "/";
				} else if(i > 0) {
					r += "$";
				}
				r += classes[i].first();
				if(generic) {
					Type[] gparams = classes[i].second();
					if(gparams != null && gparams.length > 0) {
						r += "<";
						for(Type gt : gparams) {
							r += descriptor(gt,generic);
						}
						r += ">";
					}
				}
			}
			return r + ";";
		} else if(t instanceof Type.Function) {
			Type.Function ft = (Type.Function) t;
			String r = "(";

			for (Type pt : ft.parameterTypes()) {				
				r += descriptor(pt,generic);
			}

			return r + ")" + descriptor(ft.returnType(),generic);
		} else if(t instanceof Type.Variable) {
			if(generic) {
				Type.Variable tv = (Type.Variable) t;
				return "T" + tv.name() + ";";
			} else {
				Type.Variable tv = (Type.Variable) t;
				Type[] lb = tv.lowerBounds();
				if(lb.length > 0) {
					return descriptor(lb[0],generic);
				} else {
					return "Ljava/lang/Object;";
				}
			}
		}
		 
		throw new RuntimeException("Invalid type passed to Types.descriptor(): " + t);
	}
	

	/**
	 * This method constructors a class signature string from a clazz.
	 * 
	 * @param clazz
	 * @return
	 */
	public static String classSignature(Clazz clazz) {
		String desc = "";
		
		Pair<String,Type[]>[] classes = clazz.type().classes();
		if(classes[classes.length-1].second().length > 0) { 
			desc += "<"; 
			for(Type t : classes[classes.length-1].second()) {
				if(t instanceof Type.Variable) {
					Type.Variable tv = (Type.Variable) t;
					desc += tv.name() + ":";
					// NOTE: lowerBounds() should *never* be null.
					if(tv.lowerBounds() == null || tv.lowerBounds().length == 0) {
						desc += "Ljava/lang/Object;";
					} else {
						for(Type lb : tv.lowerBounds()) {
							// The following check is needed to deal with the case
							// where the type bounds are only interfaces. In this
							// case, there must be an extra colon indicating the
							// absence of super class. It's actually really annoying
							// since it couples this code with ClassTable ... grrr.
							try {
								Clazz tmp = ClassTable.findClass((Type.Reference) lb);
								if(tmp.isInterface()) { desc += ":"; }
								desc += Types.descriptor(lb, true);
							} catch(ClassNotFoundException ce) {
								throw new RuntimeException("Type bound " + lb + " not found");
							}
						}
					}
				} else {
					throw new RuntimeException("Type Variable required in Class Signature!");
				}
			}
			desc += ">";
		}
		if (clazz.superClass() != null) {
			desc += descriptor(clazz.superClass(), true);
		}
		for (Type t : clazz.interfaces()) {
			desc += descriptor(t, true);
		}
		return desc;
	}
}
