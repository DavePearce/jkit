// This file is part of the Simple Lisp Interpreter.
//
// The Simple Lisp Interpreter is free software; you can 
// redistribute it and/or modify it under the terms of the 
// GNU General Public License as published by the Free Software 
// Foundation; either version 2 of the License, or (at your 
// option) any later version.
//
// The Simpular Program Interpreter is distributed in the hope
// that it will be useful, but WITHOUT ANY WARRANTY; without 
// even the implied warranty of MERCHANTABILITY or FITNESS FOR 
// A PARTICULAR PURPOSE.  See the GNU General Public License 
// for more details.
//
// You should have received a copy of the GNU General Public 
// License along with the Simpular Program Interpreter; if not, 
// write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA
//
// (C) David James Pearce, 2005. 

package org.simplelisp.interpreter;

import java.util.*;

// this is an incomplete example of the DECORATOR pattern
public final class LispString implements LispSequence {
	private final String value;
	
	public LispString(String v) { value=v; }
	public String toString() { return value; }
	
	public boolean equals(Object o) {
		return (o instanceof LispString) && value.equals(((LispString)o).value);
	}
	
	public LispInteger length() { return new LispInteger(value.length()); }
	
	public LispString subseq(int l, int u) { 
		return new LispString(value.substring(l,u));
	}
	
	public LispExpr elt(int i) { 
		return new LispChar(value.charAt(i)); 
	}
	
	public LispString reverse() { 
		StringBuffer sb = new StringBuffer(value).reverse();
		return new LispString(sb.toString());
	}
	
	public LispExpr evaluate(HashMap<String, LispExpr> locals, 
			HashMap<String, LispExpr> globals) {
		return this;
    } 
}
    
