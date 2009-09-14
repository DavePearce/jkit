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

package jkit.jil.stages;

import java.util.*;

import static jkit.compiler.SyntaxError.*;
import jkit.compiler.SyntacticElement;
import jkit.jil.tree.*;
import jkit.jil.dfa.*;
import jkit.util.*;
/**
 * This stage performs a definite assignment analysis to check that every
 * variable is always defined before being used. For example, consider the
 * following code snippets:
 * 
 *  1)
 *	   int x,y;
 *	   x = y + 1;
 *	   System.out.println("GOT: " + x);
 *  
 *  2)
 *     int x;
 *     if(args == null) { x = 1; }
 *     System.out.println("GOT: " + x);
 *  
 *  3)
 *     int x;
 *     try { x = 1/0; } 
 *     catch(Exception e) {}
 *     System.out.println("GOT: " + x);
 *
 *
 * In each of the above cases, variable "x" is not definitely assigned
 * before being used. @author djp
 * 
 */
public class VariableDefinitions extends ForwardAnalysis<UnionFlowSet<String>> {
	
	private static final boolean debugging = false;
	
	public void apply(JilClass owner) {
		for(JilMethod m : owner.methods()) {			
			if(m.body() != null) {
				check(m,owner);
			} 
		}
	}
	
	public void check(JilMethod method,JilClass owner) {			
		// In the following dataflow analysis, a variable is in the flow set if
		// it is undefined.
		
		if(debugging) {
			System.err.println("******** " + method.name() + " " + method.type() + " ********");
		}
		
		HashSet<String> initStore = new HashSet<String>();
		for(Pair<String,Boolean> v : method.localVariables()) {
			if(!v.second()) {				
				initStore.add(v.first());
			} 
		}
		
		if(debugging) {
			System.out.println("INITIAL UNDEFS: " + initStore);
		}
		
		start(method,new UnionFlowSet<String>(initStore),new UnionFlowSet<String>());
	}
	
	public UnionFlowSet<String> transfer(JilStmt stmt, UnionFlowSet<String> in) {		
		if(debugging) {
			System.out.println("***    " + in);
			System.out.println("*** " + stmt);
		}
		if(stmt instanceof JilStmt.Assign) {
			return transfer((JilStmt.Assign)stmt,in);					
		} else if(stmt instanceof JilExpr.Invoke) {
			return transfer((JilExpr.Invoke)stmt,in);										
		} else if(stmt instanceof JilExpr.New) {
			return transfer((JilExpr.New) stmt,in);						
		} else if(stmt instanceof JilStmt.Return) {
			return transfer((JilStmt.Return) stmt,in);
		} else if(stmt instanceof JilStmt.Throw) {
			return transfer((JilStmt.Throw) stmt,in);
		} else if(stmt instanceof JilStmt.Nop) {		
			return in;
		} else if(stmt instanceof JilStmt.Lock) {		
			return transfer((JilStmt.Lock) stmt,in);
		} else if(stmt instanceof JilStmt.Unlock) {		
			return transfer((JilStmt.Unlock) stmt,in);
		} else {
			syntax_error("unknown statement encountered (" + stmt.getClass().getName() + ")",stmt);
			return null;
		}				
	}
	
	public UnionFlowSet<String> transfer(JilStmt.Assign stmt, UnionFlowSet<String> undefs) {
		Set<String> uses = uses(stmt.rhs(),stmt);
		if(!(stmt.lhs() instanceof JilExpr.Variable)) {
			uses.addAll(uses(stmt.lhs(),stmt));
		}
		checkUses(uses,undefs,stmt);
		if(stmt.lhs() instanceof JilExpr.Variable) {	
			JilExpr.Variable lv = (JilExpr.Variable) stmt.lhs();
			undefs = undefs.remove(lv.value());
		}
		return undefs;
	}
	
	public UnionFlowSet<String> transfer(JilExpr.Invoke stmt, UnionFlowSet<String> undefs) {		
		Set<String> uses = uses(stmt.target(),stmt);
		for(JilExpr e : stmt.parameters()) {
			uses.addAll(uses(e,stmt));
		}
		checkUses(uses,undefs,stmt);
		return undefs;
	}

	public UnionFlowSet<String> transfer(JilExpr.New stmt, UnionFlowSet<String> undefs) {
		Set<String> uses = new HashSet<String>();
		for(JilExpr e : stmt.parameters()) {
			uses.addAll(uses(e,stmt));
		}
		checkUses(uses,undefs,stmt);
		return undefs;
	}
	
	public UnionFlowSet<String> transfer(JilStmt.Return stmt, UnionFlowSet<String> undefs) {
		if(stmt.expr() != null) {
			Set<String> uses = uses(stmt.expr(),stmt);		
			checkUses(uses,undefs,stmt);
		}
		return undefs;
	}
	
	public UnionFlowSet<String> transfer(JilStmt.Throw stmt, UnionFlowSet<String> undefs) {
		Set<String> uses = uses(stmt.expr(),stmt);		
		checkUses(uses,undefs,stmt);
		return undefs;
	}
	
	public UnionFlowSet<String> transfer(JilStmt.Lock stmt, UnionFlowSet<String> undefs) {
		Set<String> uses = uses(stmt.expr(),stmt);		
		checkUses(uses,undefs,stmt);
		return undefs;
	}
	
	public UnionFlowSet<String> transfer(JilStmt.Unlock stmt, UnionFlowSet<String> undefs) {
		Set<String> uses = uses(stmt.expr(),stmt);		
		checkUses(uses,undefs,stmt);
		return undefs;
	}
	
	public UnionFlowSet<String> transfer(JilExpr e, UnionFlowSet<String> undefs) {				
		checkUses(uses(e,e), undefs,e);
		return undefs;
	}
	
	public Set<String> uses(JilExpr expr, SyntacticElement s) {
		if(expr instanceof JilExpr.ArrayIndex) {
			return uses((JilExpr.ArrayIndex) expr,  s);
		} else if(expr instanceof JilExpr.BinOp) {		
			return uses((JilExpr.BinOp) expr, s);
		} else if(expr instanceof JilExpr.UnOp) {		
			return uses((JilExpr.UnOp) expr, s);								
		} else if(expr instanceof JilExpr.Cast) {
			return uses((JilExpr.Cast) expr, s);			 			
		}  else if(expr instanceof JilExpr.Convert) {
			return uses((JilExpr.Convert) expr, s);			 			
		} else if(expr instanceof JilExpr.ClassVariable) {
			return uses((JilExpr.ClassVariable) expr, s);			 			
		} else if(expr instanceof JilExpr.Deref) {
			return uses((JilExpr.Deref) expr, s);			 							
		} else if(expr instanceof JilExpr.Variable) {
			return uses((JilExpr.Variable) expr, s);
		} else if(expr instanceof JilExpr.InstanceOf) {
			return uses((JilExpr.InstanceOf) expr, s);
		} else if(expr instanceof JilExpr.Invoke) {
			return uses((JilExpr.Invoke) expr, s);
		} else if(expr instanceof JilExpr.New) {
			return uses((JilExpr.New) expr, s);
		} else if(expr instanceof JilExpr.Value) {
			return uses((JilExpr.Value) expr, s);
		} else {
			syntax_error("Unknown expression \"" + expr + "\" encoutered",s);
			return null;
		}		
	}
	
	public Set<String> uses(JilExpr.ArrayIndex expr, SyntacticElement s) { 
		Set<String> r = uses(expr.target(),s);
		r.addAll(uses(expr.index(),s));
		return r;
	}	
	public Set<String> uses(JilExpr.BinOp expr, SyntacticElement s) {
		Set<String> r = uses(expr.lhs(),s);
		r.addAll(uses(expr.rhs(),s));
		return r; 
	}
	public Set<String> uses(JilExpr.UnOp expr, SyntacticElement s) { 		
		return uses(expr.expr(),s); 
	}
	public Set<String> uses(JilExpr.Cast expr, SyntacticElement s) { 
		return uses(expr.expr(),s);		
	}
	public Set<String> uses(JilExpr.Convert expr, SyntacticElement s) { 
		return uses(expr.expr(),s);		
	}
	public Set<String> uses(JilExpr.ClassVariable expr, SyntacticElement s) { 		
		return new HashSet<String>();
	}
	public Set<String> uses(JilExpr.Deref expr, SyntacticElement s) { 		
		return uses(expr.target(),s);
	}	
	public Set<String> uses(JilExpr.Variable expr, SyntacticElement s) { 
		HashSet<String> r = new HashSet<String>();
		r.add(expr.value());
		return r;
	}
	public Set<String> uses(JilExpr.InstanceOf expr, SyntacticElement s) { 		
		return uses(expr.lhs(),s);
	}
	public Set<String> uses(JilExpr.Invoke expr, SyntacticElement s) { 
		Set<String> r = uses(expr.target(),s);
		for(JilExpr e : expr.parameters()) {
			r.addAll(uses(e,s));
		}
		return r; 		
	}
	public Set<String> uses(JilExpr.New expr, SyntacticElement s) { 
		Set<String> r = new HashSet<String>();
		for(JilExpr e : expr.parameters()) {
			r.addAll(uses(e,s));
		}
		return r; 			
	}
	public Set<String> uses(JilExpr.Value expr, SyntacticElement s) { 
		HashSet<String> r = new HashSet<String>();
		if(expr instanceof JilExpr.Array) {
			JilExpr.Array ae = (JilExpr.Array) expr;
			for(JilExpr v : ae.values()) {
				r.addAll(uses(v,s));
			}
		}
		return r; 
	}

	/**
	 * This method simply checks whether there is any variable being used that
	 * has not yet been defined.  And, if so, throws an InternalException
	 * 
	 * @param uses the set of variables used at this point
	 * @param undefs the set of variables not defined at this point
	 * @param point the program point in question
	 * @param method enclosing method
	 * @param owner enclosing class
	 */
	private void checkUses(Set<String> uses, UnionFlowSet<String> undefs, SyntacticElement s) {		
		for(String v : uses) {			
			if(undefs.contains(v)) {
				syntax_error("variable " + v + " might not have been initialised",s);
			}
		}
	}
}