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
public final class JilConstant extends JilField {	
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
			SyntacticAttribute... attributes) {
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
			List<SyntacticAttribute> attributes) {
		super(name,type,modifiers,attributes);
		this.initialiser = initialiser;
	}
	
	public Object constant() {		
		return initialiser;
	}
	
	public boolean isConstant() {
		return true;
	}
}
