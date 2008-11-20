package jkit.util.dfa;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import jkit.bytecode.ClassFileWriter;
import jkit.jkil.FlowGraph;
import jkit.jkil.FlowGraph.Expr;
import jkit.jkil.FlowGraph.Point;
import jkit.util.Triple;
import jkit.util.graph.Graph;

public abstract class ForwardAnalysis<T extends FlowSet> {
	
	private final TreeSet<Point> worklist;
	private final HashMap<Point, T> stores = new HashMap<Point, T>();
	private HashMap<Point, Integer> order = new HashMap<Point, Integer>();
	//private Graph<Point, Triple<Point, Point, Expr>> cfg;
	
	public ForwardAnalysis() {
		worklist = new TreeSet<Point>(new Comparator<Point>() {
			public int compare(Point a, Point b) {
				int i = order.get(a);
				int j = order.get(b);
				
				if(i < j)
					return -1;
				else if(i == j)
					return 0;
				return 1;
			}
		});
	}
	
	/**
	 * Begins the Forward Analysis traversal of a CFG stating a defined entry point.
	 * 
	 * @param cfg CFG graph to traverse
	 * @param entry Entry Point of the CFG traversal.  This can be any Point in the CFG, 
	 * 				it does not necessarily need to be that first point in the CFG
	 * @param store Initial FlowSet
	 */
	@SuppressWarnings("unchecked")
	public void start(Graph<Point, Triple<Point, Point, Expr>> cfg, Point entry, T initStore) {
		reset();
		
		ArrayList<Point> order = ClassFileWriter.cfgOrder(entry, cfg);

		int i = 0;
		for(Point p : order) {
			this.order.put(p, i++);
		}
		
		stores.put(entry, initStore);
		worklist.add(entry);
		
		while(!worklist.isEmpty()) {
			Point current = select();
			// prestore represents store going into this point
			T preStore = stores.get(current);
			// poststore represents store after effect of statement at this
			// point is applied.
			T postStore = (T) preStore.clone();
						
			// Get all edges emanating from the current point, and split out the
			// exceptional edges. The latter is important since the semantics of
			// exceptional edges is subtly different others --- they occur
			// before the statements effect occurs. For example, this is really
			// important in dealing with definite assignment.  Consider:
			//
			//  int x; 
			//  try { x = 1 / 0; } 
			//  catch(Exception e) {} 
			//  x = x + 1;
			//
			// Here, the exceptional edge going from the statement "x=1/0"
			// represents a flow that occurs before the assignment to x takes
			// effect. Thus, the above has a problem because x may not be
			// assigned before reaching the last statement.
			
			Set<Triple<Point, Point, Expr>> flows = cfg.from(current);
			Set<Triple<Point, Point, Expr>> normalEdges = new HashSet<Triple<Point, Point, Expr>>();
			Set<Triple<Point, Point, Expr>> exceptionalEdges = new HashSet<Triple<Point, Point, Expr>>();
			
			for(Triple<Point, Point, Expr> t : flows) {
				if(!(t.third() instanceof FlowGraph.Exception)) {
					normalEdges.add(t);
				} else {
					exceptionalEdges.add(t);
				}
			}
			
			// At this point, we propagate our along all outgoing exceptional
			// edges.
			for(Triple<Point, Point, Expr>  e : exceptionalEdges) {							
				merge(e.second(), preStore);
			}
			
			// Now, we apply the effect of the current statement.
			transfer(current, postStore);
			
			// Finally, we propagate along all outgoing edges
			for(Triple<Point, Point, Expr>  e : normalEdges) {							
				Expr condition = e.third();
				if(condition == null) {
					// this is an optimised case to avoid the clone as we know
					// it's not necessary (since the condition is null and,
					// hence, this is a unconditional edge).
					merge(e.second(), postStore);
				} else {
					// Ok, apply the clone then.
					T postPostStore = (T) postStore.clone();
					transfer(current, condition, postPostStore);
					merge(e.second(), postPostStore);
				}
			}
		}
	}
	
	private void reset() {
		worklist.clear();
		stores.clear();
		order.clear();
	}
	
	/**
	 * Merges a FlowSet with a certain Point and adds it to the worklist
	 * if something changes
	 * 
	 * @param p Destination Point for merge
	 * @param m FlowSet to merge into point
	 */
	private void merge(Point p, T m) {
		T tmp = stores.get(p);
		if(tmp != null) {
			if(tmp.join(m)) { worklist.add(p); }
		} else {
			stores.put(p, m);
			worklist.add(p);
		}
	}
	
	/**
	 * Selects the next Point from the worklist
	 * 
	 * @return Next Point from worklist, or null if worklist is empty
	 */
	private Point select() {
		if(!worklist.isEmpty()) {
			Point p = worklist.iterator().next();
			worklist.remove(p);
			return p;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Transfer function for Code Points
	 * 
	 * @param p Point containing a statement that needs to be evaluated
	 * @param m FlowSet to use in evaluation
	 */
	public abstract void transfer(Point p, T m);
	
	/**
	 * Transfer function for Code Expressions.  This is used primarily for
	 * evaluating conditional expressions.
	 * 
	 * @param e Expr to evaluate
	 * @param m FlowSet to use in evaluation
	 */
	public abstract void transfer(Point p, Expr e, T m);

}
