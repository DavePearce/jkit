package jkit.jil.stages;

import static jkit.compiler.SyntaxError.syntax_error;

import java.util.*;

import jkit.jil.dfa.*;
import jkit.jil.tree.*;

public class NonNullInference extends BackwardAnalysis<UnionFlowSet<String>> {
	
	public static class Attr implements Attribute {
		private Set<String> nonnulls; // parameters and fields which must be
                                        // non-null on entry
		public Attr(Set<String> nonnulls) {
			this.nonnulls = nonnulls;
		}
		public Set<String> nonnulls() {
			return nonnulls;
		}
	}
	
	public void apply(JilClass owner) {
		for(JilMethod m : owner.methods()) {			
			if(m.body() != null) {
				infer(m,owner);
			} 
		}
	}
	
	public void infer(JilMethod method,JilClass owner) {		
		HashSet<String> finalStore = new HashSet<String>();				
		start(method,new UnionFlowSet<String>(finalStore));
		
		UnionFlowSet<String> entryStore = stores.get(0);
		
		// System.out.println("METHOD: " + owner.type() + "." + method.name() + " " + method.type() + ": " + entryStore);
		
		method.attributes().add(new Attr(entryStore.toSet()));
	}

	public UnionFlowSet<String> transfer(JilStmt stmt, UnionFlowSet<String> in) {		
		UnionFlowSet<String> r;
		if(stmt instanceof JilStmt.Assign) {
			r = transfer((JilStmt.Assign)stmt,in);					
		} else if(stmt instanceof JilExpr.Invoke) {
			r = transfer((JilExpr.Invoke)stmt,in);										
		} else if(stmt instanceof JilExpr.New) {
			r = transfer((JilExpr.New) stmt,in);						
		} else if(stmt instanceof JilStmt.Return) {
			r = transfer((JilStmt.Return) stmt,in);
		} else if(stmt instanceof JilStmt.Throw) {
			r = transfer((JilStmt.Throw) stmt,in);
		} else if(stmt instanceof JilStmt.Nop) {		
			r = in;
		} else if(stmt instanceof JilStmt.Lock) {		
			r = transfer((JilStmt.Lock) stmt,in);
		} else if(stmt instanceof JilStmt.Unlock) {		
			r = transfer((JilStmt.Unlock) stmt,in);
		} else {
			syntax_error("unknown statement encountered (" + stmt.getClass().getName() + ")",stmt);
			return null;
		}		
		// System.out.println("BEFORE: " + stmt + " " + r);
		return r;
	}
	
	public UnionFlowSet<String> transfer(JilStmt.Assign stmt, UnionFlowSet<String> nonnulls) {				
		
		String lhsName = derefName(stmt.lhs());
		String rhsName = derefName(stmt.rhs());
		if(nonnulls.contains(lhsName)) {
			nonnulls = nonnulls.remove(lhsName);
			nonnulls = nonnulls.add(rhsName);
		}
		
		nonnulls = nonnulls.addAll(derefs(stmt.lhs()));
		nonnulls = nonnulls.addAll(derefs(stmt.rhs()));		
		return nonnulls;
	}
	
	public UnionFlowSet<String> transfer(JilExpr.Invoke stmt, UnionFlowSet<String> nonnulls) {				
		JilExpr target = stmt.target();
		
		nonnulls = nonnulls.addAll(derefs(target));
		for(JilExpr p : stmt.parameters()) {			
			nonnulls = nonnulls.addAll(derefs(p));
		}
		nonnulls = addDeref(target,nonnulls);				
		
		return nonnulls;
	}

	public UnionFlowSet<String> transfer(JilExpr.New stmt, UnionFlowSet<String> nonnulls) {
		return nonnulls;
	}
	
	public UnionFlowSet<String> transfer(JilStmt.Return stmt, UnionFlowSet<String> nonnulls) {
		return nonnulls;
	}
	
	public UnionFlowSet<String> transfer(JilStmt.Throw stmt, UnionFlowSet<String> nonnulls) {
		nonnulls = addDeref(stmt.expr(),nonnulls);						
		return nonnulls;
	}
	
	public UnionFlowSet<String> transfer(JilStmt.Lock stmt, UnionFlowSet<String> nonnulls) {
		return nonnulls;
	}
	
	public UnionFlowSet<String> transfer(JilStmt.Unlock stmt, UnionFlowSet<String> nonnulls) {
		return nonnulls;
	}
	
	public UnionFlowSet<String> transfer(JilExpr e, UnionFlowSet<String> nonnulls) {				
		return nonnulls;
	}
	
	public Set<String> derefs(JilExpr expr) {
		if(expr instanceof JilExpr.ArrayIndex) {
			return derefs((JilExpr.ArrayIndex) expr);
		} else if(expr instanceof JilExpr.BinOp) {		
			return derefs((JilExpr.BinOp) expr);
		} else if(expr instanceof JilExpr.UnOp) {		
			return derefs((JilExpr.UnOp) expr);								
		} else if(expr instanceof JilExpr.Cast) {
			return derefs((JilExpr.Cast) expr);			 			
		}  else if(expr instanceof JilExpr.Convert) {
			return derefs((JilExpr.Convert) expr);			 			
		} else if(expr instanceof JilExpr.ClassVariable) {
			return derefs((JilExpr.ClassVariable) expr);			 			
		} else if(expr instanceof JilExpr.Deref) {
			return derefs((JilExpr.Deref) expr);			 							
		} else if(expr instanceof JilExpr.Variable) {
			return derefs((JilExpr.Variable) expr);
		} else if(expr instanceof JilExpr.InstanceOf) {
			return derefs((JilExpr.InstanceOf) expr);
		} else if(expr instanceof JilExpr.Invoke) {
			return derefs((JilExpr.Invoke) expr);
		} else if(expr instanceof JilExpr.New) {
			return derefs((JilExpr.New) expr);
		} else if(expr instanceof JilExpr.Value) {
			return derefs((JilExpr.Value) expr);
		} else {
			syntax_error("Unknown expression \"" + expr + "\" encoutered",expr);
			return null;
		}		
	}
	
	public Set<String> derefs(JilExpr.ArrayIndex expr) { 
		Set<String> r = derefs(expr.target());
		r.addAll(derefs(expr.index()));
		return r;
	}	
	public Set<String> derefs(JilExpr.BinOp expr) {
		Set<String> r = derefs(expr.lhs());
		r.addAll(derefs(expr.rhs()));
		return r; 
	}
	public Set<String> derefs(JilExpr.UnOp expr) { 		
		return derefs(expr.expr()); 
	}
	public Set<String> derefs(JilExpr.Cast expr) { 
		return derefs(expr.expr());		
	}
	public Set<String> derefs(JilExpr.Convert expr) { 
		return derefs(expr.expr());		
	}
	public Set<String> derefs(JilExpr.ClassVariable expr) { 		
		return new HashSet<String>();
	}
	public Set<String> derefs(JilExpr.Deref expr) { 		
		Set<String> derefs = derefs(expr.target());				
		addDeref(expr.target(),derefs);		
		return derefs; 
	}	
	public Set<String> derefs(JilExpr.Variable expr) { 
		return new HashSet<String>();		
	}
	public Set<String> derefs(JilExpr.InstanceOf expr) { 		
		return derefs(expr.lhs());
	}
	public Set<String> derefs(JilExpr.Invoke expr) { 
		Set<String> r = derefs(expr.target());
		for(JilExpr e : expr.parameters()) {
			r.addAll(derefs(e));
		}
		addDeref(expr.target(),r);	
		return r; 		
	}
	public Set<String> derefs(JilExpr.New expr) { 
		Set<String> r = new HashSet<String>();
		for(JilExpr e : expr.parameters()) {
			r.addAll(derefs(e));
		}
		return r; 			
	}
	public Set<String> derefs(JilExpr.Value expr) { 
		HashSet<String> r = new HashSet<String>();
		if(expr instanceof JilExpr.Array) {
			JilExpr.Array ae = (JilExpr.Array) expr;
			for(JilExpr v : ae.values()) {
				r.addAll(derefs(v));
			}
		}
		return r; 
	}
	
	protected void addDeref(JilExpr deref, Collection<String> nonnulls) {
		String dn = derefName(deref);
		if(dn != null) {
			nonnulls.add(dn);
		}
	}
	
	protected UnionFlowSet<String> addDeref(JilExpr deref,UnionFlowSet<String> nonnulls) {
		String dn = derefName(deref);
		if(dn != null) {
			nonnulls = nonnulls.add(dn);
		}
		return nonnulls;
	}
	
	protected String derefName(JilExpr deref) {
		if(deref instanceof JilExpr.Variable) {
			JilExpr.Variable v = (JilExpr.Variable) deref;
			return v.value();
		} else if(deref instanceof JilExpr.Deref) {
			JilExpr.Deref d = (JilExpr.Deref) deref;
			return d.target().type().toString() + "$" + d.name();
		} else if(deref instanceof JilExpr.ClassVariable) {
			return null; // no deref implied here 
		} else {
			return "?" + deref.getClass().getName();
		}
	}
}
