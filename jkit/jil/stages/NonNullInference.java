package jkit.jil.stages;

import static jkit.compiler.SyntaxError.syntax_error;

import java.util.HashSet;
import java.util.Set;

import jkit.jil.dfa.*;
import jkit.jil.tree.*;
import jkit.util.Pair;

public class NonNullInference extends BackwardAnalysis<UnionFlowSet<String>> {
	
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
		System.out.println("BEFORE: " + stmt + " " + r);
		return r;
	}
	
	public UnionFlowSet<String> transfer(JilStmt.Assign stmt, UnionFlowSet<String> nonnulls) {				
		return nonnulls;
	}
	
	public UnionFlowSet<String> transfer(JilExpr.Invoke stmt, UnionFlowSet<String> nonnulls) {				
		return nonnulls;
	}

	public UnionFlowSet<String> transfer(JilExpr.New stmt, UnionFlowSet<String> nonnulls) {
		return nonnulls;
	}
	
	public UnionFlowSet<String> transfer(JilStmt.Return stmt, UnionFlowSet<String> nonnulls) {
		return nonnulls;
	}
	
	public UnionFlowSet<String> transfer(JilStmt.Throw stmt, UnionFlowSet<String> nonnulls) {
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
	
	public Set<String> derefs(JilExpr expr, SyntacticElement s) {
		if(expr instanceof JilExpr.ArrayIndex) {
			return derefs((JilExpr.ArrayIndex) expr,  s);
		} else if(expr instanceof JilExpr.BinOp) {		
			return derefs((JilExpr.BinOp) expr, s);
		} else if(expr instanceof JilExpr.UnOp) {		
			return derefs((JilExpr.UnOp) expr, s);								
		} else if(expr instanceof JilExpr.Cast) {
			return derefs((JilExpr.Cast) expr, s);			 			
		}  else if(expr instanceof JilExpr.Convert) {
			return derefs((JilExpr.Convert) expr, s);			 			
		} else if(expr instanceof JilExpr.ClassVariable) {
			return derefs((JilExpr.ClassVariable) expr, s);			 			
		} else if(expr instanceof JilExpr.Deref) {
			return derefs((JilExpr.Deref) expr, s);			 							
		} else if(expr instanceof JilExpr.Variable) {
			return derefs((JilExpr.Variable) expr, s);
		} else if(expr instanceof JilExpr.InstanceOf) {
			return derefs((JilExpr.InstanceOf) expr, s);
		} else if(expr instanceof JilExpr.Invoke) {
			return derefs((JilExpr.Invoke) expr, s);
		} else if(expr instanceof JilExpr.New) {
			return derefs((JilExpr.New) expr, s);
		} else if(expr instanceof JilExpr.Value) {
			return derefs((JilExpr.Value) expr, s);
		} else {
			syntax_error("Unknown expression \"" + expr + "\" encoutered",s);
			return null;
		}		
	}
	
	public Set<String> derefs(JilExpr.ArrayIndex expr, SyntacticElement s) { 
		Set<String> r = derefs(expr.target(),s);
		r.addAll(derefs(expr.index(),s));
		return r;
	}	
	public Set<String> derefs(JilExpr.BinOp expr, SyntacticElement s) {
		Set<String> r = derefs(expr.lhs(),s);
		r.addAll(derefs(expr.rhs(),s));
		return r; 
	}
	public Set<String> derefs(JilExpr.UnOp expr, SyntacticElement s) { 		
		return derefs(expr.expr(),s); 
	}
	public Set<String> derefs(JilExpr.Cast expr, SyntacticElement s) { 
		return derefs(expr.expr(),s);		
	}
	public Set<String> derefs(JilExpr.Convert expr, SyntacticElement s) { 
		return derefs(expr.expr(),s);		
	}
	public Set<String> derefs(JilExpr.ClassVariable expr, SyntacticElement s) { 		
		return new HashSet<String>();
	}
	public Set<String> derefs(JilExpr.Deref expr, SyntacticElement s) { 		
		Set<String> derefs = derefs(expr.target(),s);		
		String dname = derefName(expr.target());
		derefs.add(dname);
		return derefs; 
	}	
	public Set<String> derefs(JilExpr.Variable expr, SyntacticElement s) { 
		HashSet<String> r = new HashSet<String>();
		r.add(expr.value());
		return r;
	}
	public Set<String> derefs(JilExpr.InstanceOf expr, SyntacticElement s) { 		
		return derefs(expr.lhs(),s);
	}
	public Set<String> derefs(JilExpr.Invoke expr, SyntacticElement s) { 
		Set<String> r = derefs(expr.target(),s);
		for(JilExpr e : expr.parameters()) {
			r.addAll(derefs(e,s));
		}
		return r; 		
	}
	public Set<String> derefs(JilExpr.New expr, SyntacticElement s) { 
		Set<String> r = new HashSet<String>();
		for(JilExpr e : expr.parameters()) {
			r.addAll(derefs(e,s));
		}
		return r; 			
	}
	public Set<String> derefs(JilExpr.Value expr, SyntacticElement s) { 
		HashSet<String> r = new HashSet<String>();
		if(expr instanceof JilExpr.Array) {
			JilExpr.Array ae = (JilExpr.Array) expr;
			for(JilExpr v : ae.values()) {
				r.addAll(derefs(v,s));
			}
		}
		return r; 
	}
	
	protected String derefName(JilExpr deref) {
		if(deref instanceof JilExpr.Variable) {
			JilExpr.Variable v = (JilExpr.Variable) deref;
			return v.value();
		} else if(deref instanceof JilExpr.Deref) {
			JilExpr.Deref d = (JilExpr.Deref) deref;
			return d.type().toString() + "$" + d.name();
		} else {
			return "?";
		}
	}
}
