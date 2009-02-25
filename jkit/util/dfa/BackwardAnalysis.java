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
// (C) Constantine Dymnikov, 2008. 


package jkit.util.dfa;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jkit.bytecode.ClassFileWriter;
import jkit.util.Triple;
import jkit.util.graph.Graph;

public abstract class BackwardAnalysis<T extends FlowSet> {

	private final TreeSet<Point> worklist;

	private HashMap<Point, Integer> order = new HashMap<Point, Integer>();

	private final HashMap<Point, T> stores = new HashMap<Point, T>();

	protected Graph<Point, Triple<Point, Point, Expr>> cfg;

	public BackwardAnalysis() {
		worklist = new TreeSet<Point>(new Comparator<Point>() {
			public int compare(Point a, Point b) {
				int i = order.get(a);
				int j = order.get(b);

				if (i < j)
					return -1;
				else if (i == j)
					return 0;
				return 1;
			}
		});
	}

	/**
	 * Begins the Backward Analysis transversal of a CFG stating at defined exit
	 * points.
	 * 
	 * @param cfg
	 *            CFG graph to transverse
	 */
	// TODO: Initial flow sets
	@SuppressWarnings("unchecked")
	public void start(FlowGraph cfg, T initialStore) {
		this.cfg = cfg;
		if(cfg == null) return;

		// Generate order map
		Point entry = cfg.entry();

		if (entry == null) {
			throw new Error("Cannot find the entry point");
		}

		List<Point> orderList = ClassFileWriter.cfgOrder(entry, cfg);
		if (orderList.isEmpty())
			return;
		Collections.reverse(orderList);

		for (int i = 0; i < orderList.size(); i++) {
			this.order.put(orderList.get(i), i);
		}
	
		// Add all exit points to the work list
		worklist.addAll(this.getExitPoints(cfg));
		if(worklist.isEmpty() && cfg.entry() != null){
			worklist.add(cfg.entry());
		}

		// Assign the initialStore to each exit point
		for (Point curPoint : worklist) {
			this.stores.put(curPoint, (T) initialStore.clone());
		}

		Point curPoint = select();

		while (curPoint != null) {
			T store = stores.get(curPoint);
			store = (T) store.clone();

			// Get all the predecessors to the current Point
			Set<Triple<Point, Point, Expr>> predecessors = cfg.to(curPoint);

			Expr conditional = null;
			if (cfg.to(curPoint).iterator().hasNext()) {
				Triple<Point, Point, Expr> t = cfg.to(curPoint).iterator()
						.next();
				conditional = t.third;
			}
			if (predecessors.size() == 0) {// TODO: Use entry instead
				// Entry point
				transfer(curPoint, store);

			} else {
				// Ordinary statement
				for (Triple<Point, Point, Expr> trip : predecessors) {
					transfer(curPoint, store);
					if (conditional != null) {
						transfer(conditional, store);
					}
					Point predecessor = trip.first();
					merge(predecessor, store);
				}

			} 

			// Get the next point
			curPoint = select();
		}
	}

	/**
	 * Merges a FlowSet with a certain Point and adds it to the worklist if
	 * something changes
	 * 
	 * @param p
	 *            Destination Point for merge
	 * @param m
	 *            FlowSet to merge into point
	 */
	@SuppressWarnings("unchecked")
	private void merge(Point p, T m) {
		T tmp = stores.get(p);

		if (tmp != null) {
			if (tmp.join(m)) {
				// tmp.join(m);
				if (this.order.containsKey(p)) {
					// (Check for undreachable code)
					worklist.add(p);
				}
			}
		} else {
			stores.put(p, (T) m.clone());
			if (this.order.containsKey(p)) {
				// (Check for undreachable code)
				worklist.add(p);
			}
		}
	}

	/**
	 * Selects the next Point from the worklist
	 * 
	 * @return Next Point from worklist, or null if worklist is empty
	 */
	private Point select() {
		if (!worklist.isEmpty()) {
			Point p = worklist.iterator().next();
			worklist.remove(p);
			return p;
		} else {
			return null;
		}
	}

	/**
	 * Transfer function for Code Points
	 * 
	 * @param p
	 *            Point containing a statement that needs to be evaluated
	 * @param m
	 *            FlowSet to use in evaluation
	 */
	public abstract void transfer(Point p, T m);

	/**
	 * Transfer function for Code Expressions. This is used primarily for
	 * evaluating conditional expressions.
	 * 
	 * @param e
	 *            Expr to evaluate
	 * @param m
	 *            FlowSet to use in evaluation
	 */
	public abstract void transfer(Expr e, T m);

	private Collection<Point> getExitPoints(
			Graph<Point, Triple<Point, Point, Expr>> cfg) {
		Set<Point> out = new HashSet<Point>();

		for (Triple<Point, Point, Expr> curTriple : cfg) {
			if (cfg.from(curTriple.second()).isEmpty()) {
				Point p = curTriple.second();
				if(p.statement() != null){
				out.add(p);
				}
			}
		}
		return out;
	}
}

