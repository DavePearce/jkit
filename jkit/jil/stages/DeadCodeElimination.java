package jkit.jil.stages;

import java.util.*;

import jkit.util.Pair;
import jkit.jil.tree.*;

/**
 * The purpose of this pass is to eliminate any statically determinable
 * dead-code. This is required to generate correct java bytecode. For example,
 * consider the following Java code:
 * 
 * <pre>
 * void f(...) {
 *  if(...) {
 *   return ...;
 *  else {
 *   return ...;
 *  }
 * }
 * </pre>
 * 
 * Then, the Java front-end will produce the following JIL code:
 * 
 * <pre>
 * void f(...) {
 *  if(...) goto iftrue0;
 *  return ...;
 *  goto ifexit0;
 * iftrue0:
 *   return ...;
 * ifexit0:
 * }
 * </pre>
 * 
 * Here, we can see quite clearly that the code after the first return
 * statement, as well as the ifexit label, is dead.
 * 
 * @author djp
 * 
 */
public class DeadCodeElimination {

	public void apply(JilClass owner) {
		// First, we identify all the problem cases.
		for (JilMethod m : owner.methods()) {
			eliminateDeadCode(m);
		}
	}
	
	protected void eliminateDeadCode(JilMethod method) {
		HashMap<String,Integer> labels = new HashMap();
		List<JilStmt> body = method.body();
		Stack<Integer> worklist = new Stack();
		HashSet<Integer> visited = new HashSet();
		
		// first, initialiser label map
		int pos = 0;
		for(JilStmt s : body) {
			if(s instanceof JilStmt.Label) {
				JilStmt.Label lab = (JilStmt.Label) s;				
				labels.put(lab.label(),pos);
			}
			pos++;
		}
		
		worklist.push(0);
		visited.add(0);
		
		while(!worklist.isEmpty()) {
			int idx = worklist.pop();
			
			if(idx != body.size()) {			
				JilStmt stmt = body.get(idx);
				addSuccessors(worklist,visited,labels,stmt,idx);
			}
		}
				
		// Ok, now eliminate any dead statements (if there are any)
		if(visited.size() <= body.size()) {
			pos = 0;
			int size = body.size();
			for(int i=0;i!=size;++i) {
				if(!visited.contains(i)) {					
					body.remove(pos);
				} else {
					pos = pos + 1;
				}
			}
		}
	}
	
	protected void addSuccessors(Stack<Integer> worklist,
			HashSet<Integer> visited, HashMap<String, Integer> labels,
			JilStmt stmt, int offset) {
		
		// First, add sequential exit if there is one.
		if(stmt instanceof JilStmt.Goto) {
			JilStmt.Goto gto = (JilStmt.Goto) stmt;
			int target = labels.get(gto.label());
			if(!visited.contains(target)) {
				worklist.add(target);
				visited.add(target);
			}
		} else if(stmt instanceof JilStmt.Switch) {
			JilStmt.Switch swt = (JilStmt.Switch) stmt;
			for(Pair<JilExpr.Number,String> c : swt.cases()) {
				int target = labels.get(c.second());				
				if(!visited.contains(target)) {
					worklist.add(target);
					visited.add(target);
				}	
			}
			// And, don't forget the default label!
			int deftarget = labels.get(swt.defaultLabel());				
			if(!visited.contains(deftarget)) {
				worklist.add(deftarget);
				visited.add(deftarget);
			}
		} else if(!(stmt instanceof JilStmt.Return || stmt instanceof JilStmt.Throw)) {
			// this is a statement with a sequential exit
			if(!visited.contains(offset+1)) {
				worklist.add(offset+1);
				visited.add(offset+1);
			}
		}
		
		// Second, check for conditional branch
		if(stmt instanceof JilStmt.IfGoto) {
			JilStmt.IfGoto gto = (JilStmt.IfGoto) stmt;
			int target = labels.get(gto.label());
			if(!visited.contains(target)) {
				worklist.add(target);
				visited.add(target);
			}
		}
		
		// Third, add any exceptional edges
		for(Pair<Type.Clazz,String> ex : stmt.exceptions()) {
			int target = labels.get(ex.second());
			if(!visited.contains(target)) {
				worklist.add(target);
				visited.add(target);
			}
		}
	}
}
