package jkit.jil.tree;

import java.util.*;

/**
 * A JilConstant represents a constant field. It requires an initialiser field
 * which will be converted into a ConstantValue attribute (see JVM Spec,
 * $4.7.2).
 * 
 * @author djp
 * 
 */
public class JilConstant extends JilField {	
	private Object initialiser;
	
	/**
	 * Construct an object representing a field of a JVM class.
	 * 
	 * @param name -
	 *            The name of the field.
	 * @param type -
	 *            The (fully generic) type of the field.
	 * @param initialiser -
	 *            an instance of String, Boolean, Character, Short, Integer,
	 *            Long, Float or Double.
	 * @param modifiers -
	 *            Any modifiers of the field (e.g. public, static, etc)
	 */
	public JilConstant(String name, Type type, Object initialiser,
			List<Modifier> modifiers,
			Attribute... attributes) {
		super(name,type,modifiers,attributes);
		this.initialiser = initialiser;
	}
	
	/**
	 * Construct an object representing a field of a JVM class.
	 * 
	 * @param name -
	 *            The name of the field.
	 * @param type -
	 *            The (fully generic) type of the field.
	 * @param initialiser -
	 *            an instance of String, Boolean, Character, Short, Integer,
	 *            Long, Float or Double.
	 * @param modifiers -
	 *            Any modifiers of the field (e.g. public, static, etc)
	 */
	public JilConstant(String name, Type type, Object initialiser,
			List<Modifier> modifiers,
			List<Attribute> attributes) {
		super(name,type,modifiers,attributes);
		this.initialiser = initialiser;
	}
	
	public Object initialiser() {
		return initialiser;
	}
}
