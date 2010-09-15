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

package jkit.jil.dfa;

import java.util.*;

/**
 * A union flow set implements the flow set as a simple set of elements, using
 * set union to join them.
 * 
 * This implementation extends java.util.HashSet and, hence, element types must
 * provide appropriate hashcode() methods.
 * 
 * @author djp
 * 
 */
public class UnionFlowSet<T> implements FlowSet, Cloneable, Iterable<T> {
	private HashSet<T> data = new HashSet<T>();
	
	public UnionFlowSet() {}
	public UnionFlowSet(Collection<? extends T> src) { 
		data.addAll(src);
	}
	
	public UnionFlowSet<T> clone() {
		UnionFlowSet<T> r = new UnionFlowSet<T>();
		r.data.addAll(this.data);
		return r;
	}
	
	public Iterator<T> iterator() {
		return data.iterator();
	}
	
	public UnionFlowSet<T> join(FlowSet _fs) {		
		if(_fs instanceof UnionFlowSet) {
			UnionFlowSet<T> fs = (UnionFlowSet<T>) _fs;
			return union(fs);			
		}
		return null;
	}
	
	public UnionFlowSet<T> union(UnionFlowSet<T> fs) {							
		UnionFlowSet<T> tmp = (UnionFlowSet<T>) clone();

		if(tmp.data.addAll(fs.data)) {				
			return tmp;
		} else {				
			return this;
		}					
	}
	
	public UnionFlowSet<T> intersect(UnionFlowSet _fs) {				
		UnionFlowSet<T> fs = (UnionFlowSet<T>) _fs;
		UnionFlowSet<T> tmp = new UnionFlowSet<T>();
		for(T i : data) {
			if(fs.contains(i)) {
				tmp.data.add(i);
			}
		}
		return tmp;		
	}
	
	public UnionFlowSet<T> add(T s) {
		if(!data.contains(s)) {
			UnionFlowSet r = (UnionFlowSet) this.clone();			
			if(r.data.add(s)) {
				return r;
			}
		}
		return this;		
	}
	
	public UnionFlowSet<T> addAll(Collection<T> s) {
		if(!data.contains(s)) {
			UnionFlowSet r = (UnionFlowSet) this.clone();			
			if(r.data.addAll(s)) {
				return r;	
			}			
		} 
		return this;		
	}	
	
	public UnionFlowSet<T> remove(T s) {
		if(data.contains(s)) {
			UnionFlowSet r = (UnionFlowSet) this.clone();			
			if(r.data.remove(s)) {
				return r;	
			}			
		} 
		
		return this;		
	}
	
	public boolean contains(T s) {
		return data.contains(s);
	}
	
	public int hashCode() {
		return data.hashCode();
	}
	
	public boolean equals(Object o) {
		if(o instanceof UnionFlowSet) {
			UnionFlowSet ufs = (UnionFlowSet) o;
			return data.equals(ufs.data);
		}
		return false;
	}
	
	public int size() {
		return data.size();
	}
	
	public Set<T> toSet() {
		return new HashSet<T>(data);
	}
	
	public String toString() {
		String r = "{";
		boolean firstTime=true;
		for(T x : data) {
			if(!firstTime) {
				r = r + ", ";				
			}
			firstTime=false;
			r = r + x;
		}
		return r + "}";
	}
}
