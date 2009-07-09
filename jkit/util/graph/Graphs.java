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
 * This class provides several algorithms for manipulating graphs.  
 * Essentially, it is to Graph what java.util.Collections is to java.util.Collection.  
 */

package jkit.util.graph;

import java.util.HashSet;
import java.util.Set;

import jkit.util.Pair;

public class Graphs {

	public static <T, P extends Pair<T,T>> Set<T> sources(Graph<T, P> graph) {
		
		Set<T> sources = new HashSet<T>();
		for (P e: graph) {
			if (graph.to(e.first()).isEmpty()) {
				sources.add(e.first());
			}
		}
		return sources;
	}

	public static <T, P extends Pair<T,T>> Set<T> sinks(Graph<T, P> graph) {
		
		Set<T> sinks = new HashSet<T>();
		for (P e: graph) {
			if (graph.from(e.second()).isEmpty()) {
				sinks.add(e.second());
			}
		}
		return sinks;
	}
}
