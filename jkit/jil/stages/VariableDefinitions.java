package jkit.jil.stages;

import java.util.*;

import jkit.compiler.InternalException;
import jkit.compiler.Stage;
import jkit.util.dfa.*;

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
public class VariableDefinitions extends ForwardAnalysis<UnionFlowSet<String>>
		implements Stage {
	public String description() {
		return "Ensure variables are defined before used";
	}
	
	public void apply(Clazz owner) {
		for(Method m : owner.methods()) {			
			if(m.code() != null) {
				check(m,owner);
			} 
		}
	}
	
	public void check(Method method,Clazz owner) {
		FlowGraph cfg = method.code();
		
		// In the following dataflow analysis, a variable is in the flow set if
		// it is undefined.
		
		UnionFlowSet<String> initStore = new UnionFlowSet<String>();
		for(LocalVarDef v : cfg.localVariables()) {
			if(!v.isParameter()) {				
				initStore.add(v.name());
			}
		}
		
		start(cfg, cfg.entry(),initStore);
	}
	
	public void transfer(Point p, UnionFlowSet<String> in) {		
		Stmt stmt = p.statement();
		if(stmt instanceof Assign) {
			transfer(p,(Assign)stmt,in);					
		} else if(stmt instanceof Invoke) {
			transfer(p,(Invoke)stmt,in);										
		} else if(stmt instanceof New) {
			transfer(p,(New) stmt,in);						
		} else if(stmt instanceof Return) {
			transfer(p,(Return) stmt,in);
		} else if(stmt instanceof Throw) {
			transfer(p,(Throw) stmt,in);
		} else if(stmt instanceof Nop) {		
			// do nothing
		} else if(stmt instanceof Lock) {		
			transfer(p,(Lock) stmt,in);
		} else if(stmt instanceof Unlock) {		
			transfer(p,(Unlock) stmt,in);
		} else if(stmt != null){
			throw new InternalException("Unknown statement encountered: " + stmt,p,null,null);
		}		
	}
	
	public void transfer(Point p, Assign stmt, UnionFlowSet<String> undefs) {
		Set<String> uses = uses(stmt.rhs,p,null,null);
		if(!(stmt.lhs instanceof LocalVar)) {
			uses.addAll(uses(stmt.lhs,p,null,null));
		}
		checkUses(uses,undefs,p,null,null);
		if(stmt.lhs instanceof LocalVar) {			
			undefs.remove(((LocalVar) stmt.lhs).name);
		}
	}
	
	public void transfer(Point p, Invoke stmt, UnionFlowSet<String> undefs) {
		Set<String> uses = uses(stmt.target,p,null,null);
		for(Expr e : stmt.parameters) {
			uses.addAll(uses(e,p,null,null));
		}
		checkUses(uses,undefs,p,null,null);		
	}

	public void transfer(Point p, New stmt, UnionFlowSet<String> undefs) {
		Set<String> uses = new HashSet<String>();
		for(Expr e : stmt.parameters) {
			uses.addAll(uses(e,p,null,null));
		}
		checkUses(uses,undefs,p,null,null);
	}
	
	public void transfer(Point p, Return stmt, UnionFlowSet<String> undefs) {
		if(stmt.expr != null) {
			Set<String> uses = uses(stmt.expr,p,null,null);		
			checkUses(uses,undefs,p,null,null);
		}
	}
	
	public void transfer(Point p, Throw stmt, UnionFlowSet<String> undefs) {
		Set<String> uses = uses(stmt.expr,p,null,null);		
		checkUses(uses,undefs,p,null,null);
	}
	
	public void transfer(Point p, Lock stmt, UnionFlowSet<String> undefs) {
		Set<String> uses = uses(stmt.var,p,null,null);		
		checkUses(uses,undefs,p,null,null);
	}
	
	public void transfer(Point p, Unlock stmt, UnionFlowSet<String> undefs) {
		Set<String> uses = uses(stmt.var,p,null,null);		
		checkUses(uses,undefs,p,null,null);
	}
	
	public void transfer(Point p, Expr e, UnionFlowSet<String> undefs) {				
		checkUses(uses(e, p, null, null), undefs, p,null,null);
	}
	
	public Set<String> uses(Expr expr, Point point, Method method, Clazz owner) {
		if(expr instanceof ArrayIndex) {
			return uses((ArrayIndex) expr,  point, method,owner);
		} else if(expr instanceof TernOp) {
			return uses((TernOp) expr, point,method,owner);
		} else if(expr instanceof BinOp) {		
			return uses((BinOp) expr, point,method,owner);
		} else if(expr instanceof UnOp) {		
			return uses((UnOp) expr, point,method,owner);								
		} else if(expr instanceof Cast) {
			return uses((Cast) expr, point,method,owner);			 			
		} else if(expr instanceof ClassAccess) {
			return uses((ClassAccess) expr, point,method,owner);			 			
		} else if(expr instanceof Deref) {
			return uses((Deref) expr, point,method,owner);			 							
		} else if(expr instanceof FlowGraph.Exception) {
			return uses((FlowGraph.Exception) expr, point,method,owner);			 							
		} else if(expr instanceof LocalVar) {
			return uses((LocalVar) expr, point,method,owner);
		} else if(expr instanceof InstanceOf) {
			return uses((InstanceOf) expr, point,method,owner);
		} else if(expr instanceof Invoke) {
			return uses((Invoke) expr, point,method,owner);
		} else if(expr instanceof New) {
			return uses((New) expr, point,method,owner);
		} else if(expr instanceof Value) {
			return uses((Value) expr, point,method,owner);
		} else {
			throw new InternalException("Unknown expression \"" + expr + "\" encoutered",point,method,owner);
		}		
	}
	
	public Set<String> uses(ArrayIndex expr, Point point, Method method, Clazz owner) { 
		Set<String> r = uses(expr.array,point,method,owner);
		r.addAll(uses(expr.idx,point,method,owner));
		return r;
	}
	public Set<String> uses(TernOp expr, Point point, Method method, Clazz owner) { 
		Set<String> r = uses(expr.cond,point,method,owner);
		r.addAll(uses(expr.foption,point,method,owner));
		r.addAll(uses(expr.toption,point,method,owner));
		return r;		
	}
	public Set<String> uses(BinOp expr, Point point, Method method, Clazz owner) {
		Set<String> r = uses(expr.lhs,point,method,owner);
		r.addAll(uses(expr.rhs,point,method,owner));
		return r; 
	}
	public Set<String> uses(UnOp expr, Point point, Method method, Clazz owner) { 		
		return uses(expr.expr,point,method,owner); 
	}
	public Set<String> uses(Cast expr, Point point, Method method, Clazz owner) { 
		return uses(expr.expr,point,method,owner);		
	}
	public Set<String> uses(ClassAccess expr, Point point, Method method, Clazz owner) { 		
		return new HashSet<String>();
	}
	public Set<String> uses(Deref expr, Point point, Method method, Clazz owner) { 		
		return uses(expr.target,point,method,owner);
	}
	public Set<String> uses(FlowGraph.Exception expr, Point point, Method method, Clazz owner) { 
		return new HashSet<String>(); 
	}
	public Set<String> uses(LocalVar expr, Point point, Method method, Clazz owner) { 
		HashSet<String> r = new HashSet<String>();
		r.add(expr.name);
		return r;
	}
	public Set<String> uses(InstanceOf expr, Point point, Method method, Clazz owner) { 		
		return uses(expr.lhs,point,method,owner);
	}
	public Set<String> uses(Invoke expr, Point point, Method method, Clazz owner) { 
		Set<String> r = uses(expr.target,point,method,owner);
		for(Expr e : expr.parameters) {
			r.addAll(uses(e,point,method,owner));
		}
		return r; 		
	}
	public Set<String> uses(New expr, Point point, Method method, Clazz owner) { 
		Set<String> r = new HashSet<String>();
		for(Expr e : expr.parameters) {
			r.addAll(uses(e,point,method,owner));
		}
		return r; 			
	}
	public Set<String> uses(Value expr, Point point, Method method, Clazz owner) { 
		return new HashSet<String>(); 
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
	private void checkUses(Set<String> uses, UnionFlowSet<String> undefs, Point point, Method method, Clazz owner) {
		for(String v : uses) {			
			if(undefs.contains(v)) {
				throw new InternalException("Variable might not have been initialised",point,method,owner);
			}
		}
	}
}