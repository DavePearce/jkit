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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static jkit.compiler.SyntaxError.*;
import jkit.jil.tree.*;
import jkit.jil.util.*;
import jkit.util.Pair;

public abstract class ForwardAnalysis<T extends FlowSet> {
	
	
	protected final HashMap<Integer, T> stores = new HashMap<Integer, T>();	
	protected final HashMap<String,Integer> labels = new HashMap(); 
	
	/**
	 * Begins the Forward Analysis traversal of a method
	 */	
	public void start(JilMethod method, T startStore, T emptyStore) {	
		stores.clear(); // need to reset for subsequent calls
		labels.clear();
		
		List<JilStmt> body = method.body();
		
		// First, build the label map
		int pos = 0;
		for(JilStmt s : body) {
			if(s instanceof JilStmt.Label) {
				JilStmt.Label lab = (JilStmt.Label) s;				
				labels.put(lab.label(),pos);
			}
			pos++;			
		}
		
		// Second, initialise the worklist
		HashSet<Integer> worklist = new HashSet();
		stores.put(0, startStore);
		worklist.add(0);
		
		// third, iterate until a fixed point is reached!
		while(!worklist.isEmpty()) {
			Integer current = select(worklist);
			
			if(current == body.size()) {
				continue;
			}
			

			JilStmt stmt = body.get(current);
			
			try {
				// prestore represents store going into this point
				T store = stores.get(current);

				if(store == null) {
					store = emptyStore;
				}

				// now, add any exceptional edges
				for(Pair<Type.Clazz,String> ex : stmt.exceptions()) {
					int target = labels.get(ex.second());
					merge(target,store,worklist);
				}

				if(stmt instanceof JilStmt.Goto) {
					JilStmt.Goto gto = (JilStmt.Goto) stmt;
					int target = labels.get(gto.label());
					merge(target,store,worklist);
				} else if(stmt instanceof JilStmt.IfGoto) {				
					JilStmt.IfGoto gto = (JilStmt.IfGoto) stmt;
					JilExpr condition = Exprs.eliminateNot(gto.condition());
					JilExpr notCondition = Exprs.eliminateNot(new JilExpr.UnOp(gto.condition(), JilExpr.UnOp.NOT,Types.T_BOOL));
					int target = labels.get(gto.label());
					T t_store = transfer(condition,store);
					T f_store = transfer(notCondition,store);				
					merge(target,t_store,worklist);
					merge(current+1,f_store,worklist);
				} else if(stmt instanceof JilStmt.Switch) {
					JilStmt.Switch swt = (JilStmt.Switch) stmt;
					JilExpr defCase = new JilExpr.Bool(false);
					for(Pair<JilExpr.Number,String> c : swt.cases()) {
						int target = labels.get(c.second());
						JilExpr.BinOp cond = new JilExpr.BinOp(swt.condition(), c
								.first(), JilExpr.BinOp.EQ, Types.T_BOOL);
						defCase = new JilExpr.BinOp(defCase, cond, JilExpr.BinOp.OR, Types.T_BOOL);
						T t_store = transfer(cond, store);
						merge(target,t_store,worklist);
					}
					// And, don't forget the default label!
					int deftarget = labels.get(swt.defaultLabel());
					defCase = new JilExpr.UnOp(defCase, JilExpr.UnOp.NOT,Types.T_BOOL);	
					T d_store = transfer(defCase, store);
					merge(deftarget,d_store,worklist);				
				} else if(stmt instanceof JilStmt.Return || stmt instanceof JilStmt.Throw) {
					// collect the final store as the one at the end of the list
					merge(body.size(),transfer(stmt,store),worklist);			
				} else if(!(stmt instanceof JilStmt.Label)){				
					merge(current+1,transfer(stmt,store),worklist);
				} else if(stmt instanceof JilStmt.Label) {
					merge(current+1,store,worklist);
				}
			} catch(Exception e) {											
				internal_error(stmt,e);
			}
		}
	}	
	
	/**
	 * Merges a FlowSet with a certain Point and adds it to the worklist
	 * if something changes
	 * 
	 * @param p Destination Point for merge
	 * @param m FlowSet to merge into point
	 */
	private void merge(Integer p, T m, HashSet<Integer> worklist) {		
		T tmp = stores.get(p);		
		if(tmp != null) {
			T ntmp = (T) tmp.join(m);
			if(ntmp != tmp) {
				stores.put(p, ntmp);				
				worklist.add(p); 
			}			
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
