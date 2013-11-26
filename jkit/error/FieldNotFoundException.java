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

import jkit.compiler.ClassLoader;
import jkit.jil.tree.Type;

/**
 * This exception is thrown when trying to resolve a field using
 * ClassTable.resolveField(). It indicates that no appropriate field could be
 * found.
 *
 * @author djp
 *
 */
public final class FieldNotFoundException extends JKitException {

	private final ClassLoader loader;		//Needed to turn class types into actual classes
	private final String field;				//Name of field not found
	private final Type.Reference owner;		//Name of the owner of the field that wasn't found

	public FieldNotFoundException(String f, Type.Reference o, ClassLoader l) {
		field = f;
		owner = o;
		loader = l;
	}

	public ClassLoader loader() {
		return loader;
	}

	public String field() {
		return field;
	}

	public Type.Reference owner() {
		return owner;
	}

	public static final long serialVersionUID = 1l;
}
