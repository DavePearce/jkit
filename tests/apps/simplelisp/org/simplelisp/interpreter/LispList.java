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

public class LispList  implements LispSequence, Iterable<LispExpr> {
	private LinkedList<LispExpr> elements = new LinkedList<LispExpr>();
	
	public LispList() { elements = new LinkedList<LispExpr>(); }
	public LispList(LinkedList<LispExpr> es) { elements = es; }
	
	public String toString() {
		StringBuffer r = new StringBuffer("(");
		boolean firstTime=true;

		// go through each element
    	// and add it to the string!
		for(LispExpr e : elements) {
			if(!firstTime) { r.append(" "); }
			firstTime=false;
			r.append(e.toString());
		}
		r.append(")");
		return r.toString();
	}
	
	public boolean equals(Object o) {
		// do the easy checks first
		if(!(o instanceof LispList)) { return false; }
		LispList l = (LispList) o;
		if(l.elements.size() != elements.size()) { return false; }
		
		// recursively check that elements
		// are equal
		for(int i=0;i!=elements.size();++i) {
			if(!elements.get(i).equals(l.elements.get(i))) {
				return false;
			}
		}
		// if we get this far, it must be true!
		return true;
	}
	
	public int size() { return elements.size(); }
	public Iterator<LispExpr> iterator() { return elements.iterator(); }
	public LispExpr get(int i) { return elements.get(i); }
	public void add(LispExpr e) { elements.add(e); }
	
	// --- SEQUENCE METHODS --- 
	// (these are required by the LispSequence
    // interface)
	
	public LispInteger length() { return new LispInteger(elements.size()); }
	
	public LispList subseq(int l, int u) { 
		LispList r = new LispList();
		for(int i=l;i!=u;++i) {
			r.elements.add(elements.get(i));
		}
		return r;
	}
	
	public LispExpr elt(int i) { 
		return elements.get(i); 
	}
	
	public LispSequence reverse() { 
		LinkedList<LispExpr> l = (LinkedList<LispExpr>) elements.clone();
		Collections.reverse(l);
		return new LispList(l);
	}
	
	// --- EVALUATE METHOD ---
	
	public LispExpr evaluate(HashMap<String, LispExpr> locals, 
			HashMap<String, LispExpr> globals) {
		if(elements.size() == 0) { return new LispNil(); }
		
		// must evaluate head to determine target function
		LispExpr head = elements.get(0).evaluate(locals,globals);
		
		// sanity check!
		if(head instanceof LispFunction) {
			LispFunction fn = (LispFunction) head;
			return fn.invoke(elements,locals,globals);
		} else {
			throw new Error("unable to dispatch on \"" + head + "\" - it's not a function!");
		} 
	}
}

