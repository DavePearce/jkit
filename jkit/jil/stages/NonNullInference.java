package jkit.jil.stages;

import static jkit.compiler.SyntaxError.syntax_error;

import java.util.*;

import jkit.jil.dfa.*;
import jkit.jil.tree.*;

public class NonNullInference extends BackwardAnalysis<UnionFlowSet<NonNullInference.Location>> {
	
	public static class Location {
		private List<String> names;
		
		public Location(List<String> names) {
			this.names = names;
		}
		
		public Location(String... names) {
			this.names = new ArrayList<String>();
			for(String n : names) {
				this.names.add(n);
			}
		}
		
		public List<String> names() {
			return names;
		}
		
		public void append(String n) {			
			names.add(n);
		}
		
		public boolean equals(Object o) {
			if(o instanceof Location) {
				Location l = (Location) o;
				return l.names.equals(names);
			}
			return false;
		}
		
		public int hashCode() {
			return names.hashCode();
		}
		
		public String toString() {
			String r = "";
			boolean firstTime=true;
			for(String s : names) {
				if(!firstTime) {
					r += ".";
				}
				firstTime=false;
				r += s;
			}
			return r;
		}
	}
	
	public static class Attr implements Attribute {
		private Set<Location> nonnulls; // parameters and fields which must be
                                        // non-null on entry
		public Attr(Set<Location> nonnulls) {
			this.nonnulls = nonnulls;
		}
		public Set<Location> nonnulls() {
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
		start(method,new UnionFlowSet<Location>(new HashSet()));
		
		UnionFlowSet<Location> entryStore = stores.get(0);
		
		// System.out.println("METHOD: " + owner.type() + "." + method.name() + " " + method.type() + ": " + entryStore);
		
		method.attributes().add(new Attr(entryStore.toSet()));
	}

	public UnionFlowSet<Location> transfer(JilStmt stmt, UnionFlowSet<Location> in) {		
		UnionFlowSet<Location> r;
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
	
	public UnionFlowSet<Location> transfer(JilStmt.Assign stmt, UnionFlowSet<Location> nonnulls) {				
		
		Location lhsName = derefName(stmt.lhs());
		Location rhsName = derefName(stmt.rhs());
		
		if(nonnulls.contains(lhsName)) {
			nonnulls = nonnulls.remove(lhsName);
			nonnulls = nonnulls.add(rhsName);
		}
		
		nonnulls = nonnulls.addAll(derefs(stmt.lhs()));
		nonnulls = nonnulls.addAll(derefs(stmt.rhs()));		
		return nonnulls;
	}
	
	public UnionFlowSet<Location> transfer(JilExpr.Invoke stmt, UnionFlowSet<Location> nonnulls) {				
		JilExpr target = stmt.target();
		
		nonnulls = nonnulls.addAll(derefs(target));
		for(JilExpr p : stmt.parameters()) {			
			nonnulls = nonnulls.addAll(derefs(p));
		}
		nonnulls = addDeref(target,nonnulls);				
		
		return nonnulls;
	}

	public UnionFlowSet<Location> transfer(JilExpr.New stmt, UnionFlowSet<Location> nonnulls) {
		return nonnulls;
	}
	
	public UnionFlowSet<Location> transfer(JilStmt.Return stmt, UnionFlowSet<Location> nonnulls) {
		return nonnulls;
	}
	
	public UnionFlowSet<Location> transfer(JilStmt.Throw stmt, UnionFlowSet<Location> nonnulls) {
		nonnulls = addDeref(stmt.expr(),nonnulls);						
		return nonnulls;
	}
	
	public UnionFlowSet<Location> transfer(JilStmt.Lock stmt, UnionFlowSet<Location> nonnulls) {
		return nonnulls;
	}
	
	public UnionFlowSet<Location> transfer(JilStmt.Unlock stmt, UnionFlowSet<Location> nonnulls) {
		return nonnulls;
	}
	
	public UnionFlowSet<Location> transfer(JilExpr e, UnionFlowSet<Location> nonnulls) {				
		return nonnulls;
	}
	
	public Set<Location> derefs(JilExpr expr) {
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
	
	public Set<Location> derefs(JilExpr.ArrayIndex expr) { 
		Set<Location> r = derefs(expr.target());
		r.addAll(derefs(expr.index()));
		return r;
	}	
	public Set<Location> derefs(JilExpr.BinOp expr) {
		Set<Location> r = derefs(expr.lhs());
		r.addAll(derefs(expr.rhs()));
		return r; 
	}
	public Set<Location> derefs(JilExpr.UnOp expr) { 		
		return derefs(expr.expr()); 
	}
	public Set<Location> derefs(JilExpr.Cast expr) { 
		return derefs(expr.expr());		
	}
	public Set<Location> derefs(JilExpr.Convert expr) { 
		return derefs(expr.expr());		
	}
	public Set<Location> derefs(JilExpr.ClassVariable expr) { 		
		return new HashSet<Location>();
	}
	public Set<Location> derefs(JilExpr.Deref expr) { 		
		Set<Location> derefs = derefs(expr.target());				
		addDeref(expr.target(),derefs);		
		return derefs; 
	}	
	public Set<Location> derefs(JilExpr.Variable expr) { 
		return new HashSet<Location>();		
	}
	public Set<Location> derefs(JilExpr.InstanceOf expr) { 		
		return derefs(expr.lhs());
	}
	public Set<Location> derefs(JilExpr.Invoke expr) { 
		Set<Location> r = derefs(expr.target());
		for(JilExpr e : expr.parameters()) {
			r.addAll(derefs(e));
		}
		addDeref(expr.target(),r);	
		return r; 		
	}
	public Set<Location> derefs(JilExpr.New expr) { 
		Set<Location> r = new HashSet<Location>();
		for(JilExpr e : expr.parameters()) {
			r.addAll(derefs(e));
		}
		return r; 			
	}
	public Set<Location> derefs(JilExpr.Value expr) { 
		HashSet<Location> r = new HashSet<Location>();
		if(expr instanceof JilExpr.Array) {
			JilExpr.Array ae = (JilExpr.Array) expr;
			for(JilExpr v : ae.values()) {
				r.addAll(derefs(v));
			}
		}
		return r; 
	}
	
	protected void addDeref(JilExpr deref, Collection<Location> nonnulls) {
		Location dn = derefName(deref);
		if(dn != null) {
			nonnulls.add(dn);
		}
	}
	
	protected UnionFlowSet<Location> addDeref(JilExpr deref,UnionFlowSet<Location> nonnulls) {
		Location dn = derefName(deref);
		if(dn != null) {
			nonnulls = nonnulls.add(dn);
		}
		return nonnulls;
	}
	
	protected Location derefName(JilExpr deref) {
		if(deref instanceof JilExpr.Variable) {
			JilExpr.Variable v = (JilExpr.Variable) deref;
			return new Location(v.value());
		} else if(deref instanceof JilExpr.Deref) {
			JilExpr.Deref d = (JilExpr.Deref) deref;
			Location l = derefName(d.target());
			l.append(d.name());
			return l;
		} else if(deref instanceof JilExpr.ClassVariable) {
			return null; // no deref implied here 
		} else {
			return new Location("?" + deref.getClass().getName());
		}
	}
}
