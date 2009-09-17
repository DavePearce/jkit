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

/**
 * This implements a directed graph as a bidirectional adjacency list using HashMap.  
 * The class provides efficient to adjacent nodes in both directions. 
 */

package jkit.util.graph;

import java.util.*;
import jkit.util.Pair;

public class DirectedAdjacencyList<T,P extends Pair<T,T> > extends HashSet<P> implements Graph<T,P> {
	private HashMap<T,HashSet<P>> froms = new HashMap<T,HashSet<P>>();
	private HashMap<T,HashSet<P>> tos = new HashMap<T,HashSet<P>>();
	
	public DirectedAdjacencyList() { super(); }
	
	public boolean add(P p) {
		if(super.add(p)) {
			HashSet<P> fp = froms.get(p.first());
			if(fp == null) { 
				fp = new HashSet<P>();
				froms.put(p.first(),fp);
			}
			HashSet<P> tp = tos.get(p.second());
			if(tp == null) { 
				tp = new HashSet<P>();
				tos.put(p.second(),tp);
			}
			fp.add(p);
			tp.add(p);
			return true;
		}
		return false;
	}
		
	public void clear() {
		super.clear();
		froms.clear();
		tos.clear();
	}
	
	public boolean remove(P p) {
		if(super.remove(p)) {
			froms.get(p.first()).remove(p);
			tos.get(p.second()).remove(p);
			return true;
		}
		return false;
	}
	
	/**
	 * Returns shallow copy of this graph.
	 */
	public Object clone() {
		// could maybe make this more efficient ...
		DirectedAdjacencyList<T,P> c = new DirectedAdjacencyList<T,P>();
		c.addAll(this);
		return c;
	}
	
	public Set<P> from(T x) {
		HashSet<P> fx = froms.get(x);
		return fx == null ? new HashSet<P>() : fx; 
	}
	
	public Set<P> to(T x) {
		HashSet<P> tx = tos.get(x);
		return tx == null ? new HashSet<P>() : tx;		
	}
	
	public Set<T> domain() {
		HashSet<T> dom = new HashSet<T>();
		
		for(P p : this) {
			dom.add(p.first());
			dom.add(p.second());
		}
		
		return dom;
	}
	
	public static final long serialVersionUID = 1l;
}
