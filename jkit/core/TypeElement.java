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

package jkit.core;

/**
 * A Type Element represents a point in a lattice. Operations are provided for
 * moving up (union) and down (intersect) the lattice, as well as comparing
 * positions within the lattice (supsetOf, supsetEqOf).
 * 
 * A TypeElement also provides operations that can be performed on that type.
 * For example, a NonNull type might have four positions in the lattice:
 * UNDEFINED, (definitely) NULL, (definitely) NONNULL, NULLABLE. The latter
 * indicating that it maybe either NULL or NONNULL. Thus, all operations on the
 * type should be prevented when it's in the states NULL and NULLABLE, since
 * these may result in NullPointerExceptions. This is effected by moving the
 * type to the UNDEFINED state.
 * 
 * @author djp
 */

public interface TypeElement {
	/**
	 * This is the Least Upper Bound operator. For types, this corresponds to
	 * the set union operation (hence the name).  This operation MUST have 
	 * the following properties:
	 * 
	 * 1) If result.equals(this) then result == this.
	 * 2) else if result.equals(t) then result == t
	 * 
	 * @return
	 */
	public TypeElement union(TypeElement t);

	/**
	 * This is the Greatest Lower Bound operator. For types, this corresponds to
	 * the set intersection operation (hence the name). This operation MUST have 
	 * the following properties:
	 * 
	 * 1) If result.equals(this) then result == this.
	 * 2) else if result.equals(t) then result == t
	 *
	 * @return
	 */
	public TypeElement intersect(TypeElement t);

	/**
	 * This is set difference. This operation MUST have 
	 * the following properties:
	 * 
	 * 1) If result.equals(this) then result == this.
	 * 2) else if result.equals(t) then result == t
	 * 
	 * @return
	 */
	public TypeElement difference(TypeElement t);

	/**
	 * This is the bottom operator. For types, this corresponds to an empty set
	 * operation (hence the name). You can think of the empty set as indicating
	 * no values are possible for this variable.  This operation MUST have 
	 * the following properties:
	 * 
	 * 1) If result.equals(this) then result == this.
	 * 2) else if result.equals(t) then result == t
	 * 
	 * @return
	 */
	public TypeElement emptySet();

	/**
	 * This is the top operator. For types, this corresponds to an "full set"
	 * operation (hence the name). You can think of the full set as having all
	 * possible values in it.
	 * 
	 * @return
	 */
	public TypeElement fullSet();

	/**
	 * This method returns true if this is a subset of the parmeter or is equal
	 * to the parameter.
	 * 
	 * @return
	 */
	public boolean supsetEqOf(TypeElement t);

	/**
	 * This method returns true if this is a (strict) subset of the parmeter
	 * to the parameter.
	 * 
	 * @return
	 */
	public boolean supsetOf(TypeElement t);

	/**
	 * This method returns true if this type has no values
	 * 
	 * @return
	 */
	public boolean isEmpty();
}
