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

package jkit.jkil;

import java.lang.reflect.Modifier;
import java.util.*;


/**
 * This class stores all known information about a method, including it's
 * full (possibly generic) type, its name, its modifiers (e.g. public/private
 * etc), as well as the methods code.
 * 
 * @author djp
 * 
 */
public class Method {
	private int modifiers;

	private Type.Function type;
	
	private String name;

	private FlowGraph.Point point;
	
	private FlowGraph code;

	private final ArrayList<Type.Reference> exceptions = new ArrayList<Type.Reference>();

	public Method(int modifiers, Type.Function type, String name,
			List<Type.Reference> exceptions) {
		this.modifiers = modifiers;
		this.type = type;
		this.name = name;
		this.exceptions.addAll(exceptions);
	}
	
	public Method(int modifiers, Type.Function type, String name,
			List<Type.Reference> exceptions, FlowGraph.Point point) {
		this.modifiers = modifiers;
		this.type = type;
		this.name = name;
		this.exceptions.addAll(exceptions);
		this.point = point;
	}

	public Method(int modifiers, Type.Function type, String name,
			List<Type.Reference> exceptions, FlowGraph.Point point,
			FlowGraph code) {
		this.modifiers = modifiers;
		this.type = type;
		this.name = name;
		this.exceptions.addAll(exceptions);
		this.point = point;
		this.code = code;
	}

	/**
	 * Get the name of this method.
	 * 
	 * @return
	 */
	public String name() {
		return name;
	}
	
	/**
	 * Set the name of this method.
	 * 
	 * @return
	 */
	public void setName(String name) { this.name = name; }

	/**
	 * Set the type of this method.
	 * 
	 * @return
	 */
	public void setType(Type.Function type) { this.type = type; }
	
	
	/**
	 * Get the code point for this method
	 * 
	 * @return
	 */
	public FlowGraph.Point point() { return this.point; }
	
	/**
	 * Set the code point for this method
	 * 
	 * @return
	 */
	public void setPoint(FlowGraph.Point point) { this.point = point; }
	
	/**
	 * Set the code for this method
	 * 
	 * @return
	 */
	public void setCode(FlowGraph c) {
		code = c;
	}
	
	/**
	 * Get the code for this method
	 * 
	 * @return
	 */
	public FlowGraph code() {
		return code;
	}

	/**
	 * Check whether this method is abstract
	 */
	public boolean isAbstract() {
		return (modifiers & Modifier.ABSTRACT) != 0;
	}

	/**
	 * Check whether this method is final
	 */
	public boolean isFinal() {
		return (modifiers & Modifier.FINAL) != 0;
	}

	/**
	 * Check whether this method is static
	 */
	public boolean isStatic() {
		return (modifiers & Modifier.STATIC) != 0;
	}

	/**
	 * Check whether this method is public
	 */
	public boolean isPublic() {
		return (modifiers & Modifier.PUBLIC) != 0;
	}

	/**
	 * Check whether this method is protected
	 */
	public boolean isProtected() {
		return (modifiers & Modifier.PROTECTED) != 0;
	}

	/**
	 * Check whether this method is private
	 */
	public boolean isPrivate() {
		return (modifiers & Modifier.PRIVATE) != 0;
	}

	/**
	 * Check whether this method is native
	 */
	public boolean isNative() {
		return (modifiers & Modifier.NATIVE) != 0;
	}

	/**
	 * Check whether this method is synchronized
	 */
	public boolean isSynchronized() {
		return (modifiers & Modifier.SYNCHRONIZED) != 0;
	}

	/**
	 * Check whether this method has variable arity or not.
	 */
	public boolean isVariableArity() {
		return (modifiers & 0x80) != 0;
	}
	
	/**
	 * Get direct access to modifiers for this method (e.g. public, private
	 * etc.)
	 * 
	 * @return
	 */
	public int modifiers() {
		return modifiers;
	}

	/**
     * Set access to modifiers for this method (e.g. public, private etc.) Use
     * java.lang.reflect.Modifier for this.
     * 
     * @return
     */
	public void setModifiers(int mods) {
		modifiers = mods;
	}

	/**
	 * Get the type of the method.
	 * 
	 * @return
	 */
	public Type.Function type() {
		return type;
	}

	/**
	 * Set the type of the method.
	 * 
	 * @return
	 */
	public void type(Type.Function type) {
		this.type = type;
	}

	
	/**
	 * Get the exceptions thrown by the method.
	 * 
	 * @return
	 */
	public List<Type.Reference> exceptions() {
		return exceptions;
	}
}
