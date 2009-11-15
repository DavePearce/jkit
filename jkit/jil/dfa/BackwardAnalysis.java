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
// (C) Constantine Dymnikov, David James Pearce, 2009. 

package jkit.jil.dfa;

import static jkit.compiler.SyntaxError.internal_error;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jkit.jil.tree.*;
import jkit.jil.util.*;
import jkit.util.Pair;

public abstract class BackwardAnalysis<T extends FlowSet> {
		
	protected final HashMap<Integer, T> stores = new HashMap<Integer, T>();	
	protected final HashMap<String,Integer> labels = new HashMap();
	
	/**
	 * Begins the Forward Analysis traversal of a method
	 */	
	public void start(JilMethod method, T finalStore, T emptyStore) {
		stores.clear(); // need to reset for subsequent calls
		labels.clear();
		
		List<JilStmt> body = method.body();
		HashSet<Integer> worklist = new HashSet();
		HashMap<Integer,HashSet<Integer>> preds = new HashMap();
		// First, build the label map
		int pos = 0;
		for(JilStmt s : body) {						
			if(s instanceof JilStmt.Label) {
				JilStmt.Label lab = (JilStmt.Label) s;				
				labels.put(lab.label(),pos);
			} else if(s instanceof JilStmt.Return) {				
				worklist.add(pos);
			} else if(s instanceof JilStmt.Throw) {
				// I think there could be a bug here ...				
				worklist.add(pos);
			}									
			pos++;
		}
		
		pos = 0;
		for(JilStmt s : body) {
			addPredecessors(pos,s,preds,labels);
			pos++;
		}
		
		// third, iterate until a fixed point is reached!
		while(!worklist.isEmpty()) {
			Integer current = select(worklist);
			
			if(current == body.size()) {
				continue;
			}
									
			JilStmt stmt = body.get(current);
			
			try {
				// now, add any exceptional edges
				for(Pair<Type.Clazz,String> ex : stmt.exceptions()) {
					int target = labels.get(ex.second());
					T store = get_store(target,emptyStore);
					merge(current,store,worklist,preds);
				}

				if(stmt instanceof JilStmt.Goto) {
					JilStmt.Goto gto = (JilStmt.Goto) stmt;
					int target = labels.get(gto.label());
					T store = get_store(target,emptyStore);
					merge(current,store,worklist,preds);
				} else if(stmt instanceof JilStmt.IfGoto) {				
					JilStmt.IfGoto gto = (JilStmt.IfGoto) stmt;
					JilExpr condition = Exprs.eliminateNot(gto.condition());				
					JilExpr notCondition = Exprs.eliminateNot(new JilExpr.UnOp(gto.condition(), JilExpr.UnOp.NOT,Types.T_BOOL));				
					int target = labels.get(gto.label());
					T t_store = transfer(condition,get_store(target,emptyStore));
					T f_store = transfer(notCondition,get_store(current+1,emptyStore));				
					T store = join(t_store,f_store);				
					merge(current,store,worklist,preds);				
				} else if(stmt instanceof JilStmt.Switch) {
					JilStmt.Switch swt = (JilStmt.Switch) stmt;
					JilExpr defCase = new JilExpr.Bool(false);
					for(Pair<JilExpr.Number,String> c : swt.cases()) {
						int target = labels.get(c.second());
						JilExpr.BinOp cond = new JilExpr.BinOp(swt.condition(), c
								.first(), JilExpr.BinOp.EQ, Types.T_BOOL);
						defCase = new JilExpr.BinOp(defCase, cond, JilExpr.BinOp.OR, Types.T_BOOL);
						T t_store = transfer(cond, get_store(target,emptyStore));
						merge(current,t_store,worklist,preds);
					}
					// And, don't forget the default label!
					int deftarget = labels.get(swt.defaultLabel());
					defCase = new JilExpr.UnOp(defCase, JilExpr.UnOp.NOT,Types.T_BOOL);	
					T d_store = transfer(defCase, get_store(deftarget,emptyStore));
					merge(current,d_store,worklist,preds);				
				} else if(stmt instanceof JilStmt.Return || stmt instanceof JilStmt.Throw) {
					// collect the final store as the one at the end of the list
					merge(current,transfer(stmt,finalStore),worklist,preds);			
				} else if(!(stmt instanceof JilStmt.Label)){				
					merge(current,transfer(stmt,get_store(current+1,emptyStore)),worklist,preds);
				} else if(stmt instanceof JilStmt.Label) {
					merge(current,get_store(current+1,emptyStore),worklist,preds);
				}
			} catch(Exception e) {										
				internal_error(stmt,e);
			}
		}
	}			
	
	private void addPredecessors(int pos, JilStmt stmt,
			HashMap<Integer, HashSet<Integer>> preds,
			HashMap<String, Integer> labels) {
		for (Pair<Type.Clazz, String> ex : stmt.exceptions()) {
			int target = labels.get(ex.second());
			addPredecessor(pos, target, preds);
		}

		if (stmt instanceof JilStmt.Goto) {
			JilStmt.Goto gto = (JilStmt.Goto) stmt;
			int target = labels.get(gto.label());
			addPredecessor(pos, target, preds);
		} else if (stmt instanceof JilStmt.IfGoto) {
			JilStmt.IfGoto gto = (JilStmt.IfGoto) stmt;
			int target = labels.get(gto.label());
			addPredecessor(pos, target, preds); // true case
			addPredecessor(pos, pos + 1, preds); // false case
		} else if (stmt instanceof JilStmt.Switch) {
			JilStmt.Switch swt = (JilStmt.Switch) stmt;
			for (Pair<JilExpr.Number, String> c : swt.cases()) {
				int target = labels.get(c.second());
				addPredecessor(pos, target, preds);
			}
			// And, don't forget the default label!
			int deftarget = labels.get(swt.defaultLabel());
			addPredecessor(pos, deftarget, preds);
		} else if (!(stmt instanceof JilStmt.Return || stmt instanceof JilStmt.Throw)) {
			addPredecessor(pos, pos + 1, preds);
		}
	}
	
	public T get_store(int index, T emptyStore) {
		T tmp = stores.get(index);
		if(tmp == null) {
			return emptyStore;
		} else {
			return tmp;
		}
	}
	
	private void addPredecessor(int from, int to, HashMap<Integer,HashSet<Integer>> preds) {
		HashSet<Integer> ps = preds.get(to);
		if(ps == null) {
			ps = new HashSet<Integer>();
			preds.put(to, ps);
		}
		ps.add(from);
	}
	
	private T join(T s1, T s2) {
		if(s1 == null) {
			return s2; 
		} else if(s2 == null) {
			return s1;
		}
		return (T) s1.join(s2);
	}
	
	/**
	 * Merges a FlowSet with a certain Point and adds it to the worklist
	 * if something changes
	 * 
	 * @param p Destination Point for merge
	 * @param m FlowSet to merge into point
	 */
	private void merge(Integer p, T m, HashSet<Integer> worklist, HashMap<Integer,HashSet<Integer>> preds) {
		T tmp = stores.get(p);		
		if(tmp != null) {
			T ntmp = (T) tmp.join(m);
			if(ntmp != tmp) {
				stores.put(p, ntmp);								
			} else {
				return; // no change.
			}
		} else {
			stores.put(p, m);			
		}
		
		// now, determine who is before!
		HashSet<Integer> ps = preds.get(p);
		if(ps == null) { return; }
		for(Integer pred : preds.get(p)) {
			worklist.add(pred);
		}
		
	}
	
	/**
	 * Selects the next Point from the worklist
	 * 
	 * @return Next Point from worklist, or null if worklist is empty
	 */
	private Integer select(HashSet<Integer> worklist) {
		if(!worklist.isEmpty()) {
			Integer p = worklist.iterator().next();
			worklist.remove(p);
			return p;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Transfer function for Code Statements
	 * 
	 * @param e Expr to evaluate
	 * @param m FlowSet to use in evaluation
	 */
	
	public abstract T transfer(JilStmt stmt, T m);
	
	/**
	 * Transfer function for Code Expressions.  This is used primarily for
	 * evaluating conditional expressions.
	 * 
	 * @param e Expr to evaluate
	 * @param m FlowSet to use in evaluation
	 */
	public abstract T transfer(JilExpr e, T m);

}
