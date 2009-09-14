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

import jkit.compiler.SyntacticAttribute;

/**
 * A Source Location identifies a position within an originating source file.
 * This is useful for producing good error messages.
 * 
 * @author djp
 * 
 */
public final class SourceLocation implements SyntacticAttribute {
	private int line;
	private int column;	

	public SourceLocation(int line, int column) {
		this.line = line;
		this.column = column;		
	}

	/**
	 * Get line number in originating source file for this source location.
	 * 
	 * @return 
	 */		
	public int line() { return line; }

	/**
	 * Set line number in originating source file for this source location.
	 * 
	 * @return 
	 */
	public void setLine(int line) { this.line = line; }

	/**
	 * Get column number in originating source file for this source location.
	 * 
	 * @return 
	 */
	public int column() { return column; }

	/**
	 * Set column number in originating source file for this source location.
	 * 
	 * @return 
	 */
	public void setColumn(int column) { this.column = column; }

	public String toString() {
		return Integer.toString(line) + ":"
				+ Integer.toString(column);					
	}				
}
