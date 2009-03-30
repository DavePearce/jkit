package jkit.bytecode;

import java.util.*;

import jkit.jil.tree.Clazz;
import jkit.jil.tree.Type;
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
}
