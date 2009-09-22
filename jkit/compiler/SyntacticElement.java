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

import java.util.List;


/**
 * A Syntactic Element represents any part of the file for which is relevant
 * to the syntactic structure of the file, and in particular parts we may
 * wish to add information too (e.g. line numbers, types, etc).
 * 
 * @author djp
 * 
 */
public interface SyntacticElement {
	/**
     * Get the list of attributes associated with this syntactice element.
     * 
     * @return
     */
	public List<SyntacticAttribute> attributes();
	
	/**
     * Get the first attribute of the given class type. This is useful
     * short-hand.
     * 
     * @param c
     * @return
     */
	public <T extends SyntacticAttribute> T attribute(Class<T> c);
	
	/**
     * Get all attributes of the given class type. 
     * 
     * @param c
     * @return
     */
	public <T extends SyntacticAttribute> List<T> attributes(Class<T> c);
}
