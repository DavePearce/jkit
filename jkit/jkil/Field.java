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

import jkit.jkil.FlowGraph.Expr;
import jkit.jkil.FlowGraph.Point;

/**
 * This class stores all known information about a class field, including it's
 * full (possibly generic) type, its name and its modifiers (e.g. public/private
 * etc)
 * 
 * @author djp
 * 
 */
public class Field {
	private int modifiers;
	private Type type;
	private String name;
	private FlowGraph.Point point;
	private FlowGraph.Expr initialiser;
	private Object constValue = null;
	
	public Field(int modifiers, Type type, String name) {
		this.modifiers=modifiers;
		this.type=type;
		this.name=name;
	}
	
	public Field(int modifiers, Type type, String name, Object cvalue) {
		this.modifiers=modifiers;
		this.type=type;
		this.name=name;
		this.constValue = cvalue;
	}
	
	public Field(int modifiers, Type type, String name, FlowGraph.Point point) {
		this.modifiers=modifiers;
		this.type=type;
		this.name=name;
		this.point=point;
	}
	
	public Field(int modifiers, Type type, String name, FlowGraph.Point point, FlowGraph.Expr initialiser) {
		this.modifiers=modifiers;
		this.type=type;
		this.name=name;
		this.point=point;
		this.initialiser=initialiser;
	}
	
	/**
	 * Get the name of this field.
	 * 
	 * @return
	 */
	public String name() { return name; }
	
	/**
	 * Set the name of this field.
	 * 
	 * @return
	 */
	public void setName(String name) { this.name = name; }
	
	/**
	 * Get the code point for this field
	 * 
	 * @return
	 */
	public FlowGraph.Point point() { return this.point; }
	
	/**
	 * Get the code point for this field
	 * 
	 * @return
	 */
	public void setPoint(FlowGraph.Point point) { this.point = point; }
	
	/**
	 * Check whether this field is final
	 */
	public boolean isFinal() { return (modifiers&Modifier.FINAL)!=0; }
	
	/**
	 * Check whether this field is static
	 */
	public boolean isStatic() { return (modifiers&Modifier.STATIC)!=0; }
	
	/**
	 * Check whether this field is public
	 */
	public boolean isPublic() { return (modifiers&Modifier.PUBLIC)!=0; }
	
	/**
	 * Check whether this field is protected
	 */
	public boolean isProtected() { return (modifiers&Modifier.PROTECTED)!=0; }
	
	/**
	 * Check whether this field is private
	 */
	public boolean isPrivate() { return (modifiers&Modifier.PRIVATE)!=0; }
	
	/**
	 * Check whether this field is transient
	 */
	public boolean isTransient() { return (modifiers&Modifier.TRANSIENT)!=0; }
	
	/**
	 * Check whether this field is volatile
	 */
	public boolean isVolatile() { return (modifiers&Modifier.VOLATILE)!=0; }
	
	
	/**
	 * Get direct access to modifiers for this field (e.g. public, private etc.)
	 * 
	 * @return
	 */
	public int modifiers() { return modifiers; }
	
	/**
	 * Set the access modifiers for this field (e.g. public, private, etc.)
	 */
	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}
	
	/**
	 * Get the type of the field.
	 * 
	 * @return
	 */
	public Type type() { return type; }

	/**
	 * Set the type of the field.
	 * 
	 * @return
	 */
	public void type(Type type) {
		this.type = type;
	}
	
	/**
	 * Changes the type of the Field
	 * 
	 * @param t New Field type
	 */
	public void setType(Type t) { type = t; }
	
	/**
	 * Get the initialiser for the field.
	 * 
	 * @return
	 */
	public FlowGraph.Expr initialiser() { return initialiser; }
		
	/**
	 * Changes the initialiser for the Field
	 * 
	 * @param i New initialiser
	 */
	public void setInitialiser(FlowGraph.Expr i) { initialiser = i; }
	
	public void setConstantValue(Object o) { constValue = o; }
		
	public Object constantValue() { return constValue; }
}
