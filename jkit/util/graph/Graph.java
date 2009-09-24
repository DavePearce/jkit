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

package jkit.util.graph;

import java.util.Set;
import jkit.util.Pair;

/**
 * This interface describes a graph, which could be either undirected or
 * directed (although in JKit, we generally use directed graphs)
 * 
 * @author djp
 * 
 * @param <T> The node type to use in the graph.
 * @param <P> The pair type to use in the graph.
 */
public interface Graph<T,P extends Pair<T,T> > extends Set<P> {	
	/**
	 * This method provides efficient access to the edges going out of 'x'.
	 *   
	 * @param x
	 * @return
	 */
	public Set<P> from(T x);
	
	/**
     * This method provides efficient access to the edges going in to 'x'.
     * 
     * @param x
     * @return
     */
	public Set<P> to(T x);
	
	/**
	 * Get the domain of the nodes.
	 * 
	 * @return
	 */
	public Set<T> domain();
}
