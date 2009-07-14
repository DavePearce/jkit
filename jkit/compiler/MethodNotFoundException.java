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

/**
 * This exception is thrown when trying to resolve a method using
 * ClassTable.resolveMethod(). It indicates that no appropriate method could be
 * found,
 * 
 * @author djp
 * 
 */
public final class MethodNotFoundException extends Exception {
	public MethodNotFoundException(String method, String owner) {
		super("Method \"" + owner + "."  + method + "\" not found.");
	}
	
	public static final long serialVersionUID = 1l;
}
