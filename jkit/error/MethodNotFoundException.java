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

package jkit.error;

import java.util.List;

import jkit.compiler.ClassLoader;
import jkit.jil.tree.Type;

/**
 * This exception is thrown when trying to resolve a method using
 * ClassTable.resolveMethod(). It indicates that no appropriate method could be
 * found,
 *
 * @author djp
 *
 */
public final class MethodNotFoundException extends Exception {


	private final Type.Reference owner;   //The receiver/owner of the method that was not found
	private final String method;		  //The name of the method that was not found
	private final List<Type> parameters;  //The parameters of the method that wasn't found
	private final ClassLoader loader;	  //The classloader (necessary to turn types into classes)

	/**
	 * This constructor is called when preparing to pass the error to the error handler - in this case
	 * the exception passes along the information necessary to find possible alternatives.
	 *
	 * @param m 	- the name of missing method
	 * @param o 	- the reference (class or intersection) containing that method
	 */
	public MethodNotFoundException(String m, Type.Reference o, List<Type> p, ClassLoader l) {
		method = m;
		owner = o;
		parameters = p;
		loader = l;
	}

	public static final long serialVersionUID = 1l;

	public Type.Reference owner() {
		return owner;
	}

	public String method() {
		return method;
	}

	public List<Type> parameters() {
		return parameters;
	}

	public ClassLoader loader() {
		return loader;
	}
}
