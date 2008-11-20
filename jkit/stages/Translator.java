package jkit.stages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jkit.core.*;
import jkit.core.FlowGraph.*;
import jkit.util.*;
import jkit.util.graph.Graph;

public abstract class Translator implements Stage {
	public void apply(Clazz owner) {
		for(Method m : owner.methods()) {			
			if(m.code() != null) { translate(m,owner); } 
		}
	}
	
	protected void translate(Method method,Clazz owner) {
		FlowGraph cfg = method.code();
		
		Map<String,Type> environment = FlowGraph.buildEnvironment(method,owner);
		environment.put("$",Type.voidType()); // this is pretty ugly ... :) 
		// First, do statements in topological order (this facilitates simple
		// forms of type inference)
						
		List<Point> ordering = cfgOrder(cfg);
		for(FlowGraph.Point p : ordering) {			
			Stmt stmt = p.statement();					
			if(stmt != null) {				
				Stmt nstmt = translate(stmt,environment,p,method,owner);			
				p.setStatement(nstmt);
			}
		}
		
		// Second, do edges
		ArrayList<Triple<Point,Point,Expr>> newEdges = new ArrayList<Triple<Point,Point,Expr>>();
		for(Triple<Point,Point,Expr> edge : cfg) {
			Expr e = edge.third();
			if(e!=null) {
				e = translate(e,environment,edge.first(),method,owner);				
				newEdges.add(new Triple<Point, Point, Expr>(edge.first(),edge.second(),e));				
			} else {
				newEdges.add(edge);
			}
		}
		
		cfg.clear();
		cfg.addAll(newEdges);
	}
	
	protected Stmt translate(Stmt stmt, Map<String, Type> environment, Point point,
			Method method, Clazz owner) {
		// Only intercept statement types that may require name resolution.
		if(stmt instanceof Assign) {
			return translate((Assign) stmt, environment, point,method,owner);						
		} else if(stmt instanceof Invoke) {
			return (Stmt) translate((Invoke)stmt,environment,point,method,owner);										
		} else if(stmt instanceof New) {
			return (Stmt) translate((New) stmt,environment,point,method,owner);						
		} else if(stmt instanceof Return) {
			return translate((Return) stmt, environment, point,method,owner);									
		} else if(stmt instanceof Throw) {
			return translate((Throw) stmt, environment, point,method,owner);								
		} else if(stmt instanceof Nop) {
			return translate((Nop) stmt, environment, point,method,owner);
		} else if(stmt instanceof TernOp) {
			return (Stmt) translate((TernOp) stmt, environment, point, method, owner);
		} else if(stmt instanceof Lock) {
			return translate((Lock) stmt, environment, point,method,owner);
		} else if(stmt instanceof Unlock) {
			return translate((Unlock) stmt, environment, point,method,owner);
		} else {		
			throw new InternalException("Unknown statement encountered \""
					+ stmt + "\" encoutered", point, method, owner);
		}
	}
	
	protected Stmt translate(Assign a, Map<String,Type> environment,Point point,Method method,Clazz owner) {
		LVal lhs = (LVal) translate(a.lhs,environment,point,method,owner);
		Expr rhs = translate(a.rhs,environment,point,method,owner);
		return new Assign(lhs,rhs); 
	}
	
	protected Stmt translate(Return r, Map<String,Type> environment,Point point,Method method,Clazz owner) {
		if(r.expr != null) {			
			Expr expr = translate(r.expr,environment,point,method,owner);
			return new Return(expr);
		} else {
			return r;
		}		
	}
	
	protected Stmt translate(Throw t, Map<String,Type> environment,Point point,Method method,Clazz owner) {
		Expr expr = translate(t.expr,environment,point,method,owner);
		return new Throw(expr);		
	}
	
	protected Stmt translate(Nop n, Map<String,Type> environment,Point point,Method method,Clazz owner) {
		return n;
	}
	
	protected Stmt translate(Lock n, Map<String,Type> environment,Point point,Method method,Clazz owner) {
		return n;
	}
	
	protected Stmt translate(Unlock n, Map<String,Type> environment,Point point,Method method,Clazz owner) {
		return n;
	}
	
	protected Expr translate(Expr expr,Map<String,Type> environment,Point point,Method method,Clazz owner) {
		if(expr instanceof ArrayIndex) {
			return translate((ArrayIndex) expr, environment,point,method,owner);
		} else if(expr instanceof BinOp) {
			return translate((BinOp) expr, environment,point,method,owner);
		} else if(expr instanceof Cast) {
			return translate((Cast) expr, environment,point,method,owner);			 			
		} else if(expr instanceof ClassAccess) {
			return translate((ClassAccess) expr, environment,point,method,owner);			 			
		} else if(expr instanceof Deref) {
			return translate((Deref) expr, environment,point,method,owner);			 							
		} else if(expr instanceof FlowGraph.Exception) {
			return translate((FlowGraph.Exception) expr, environment,point,method,owner);			 							
		} else if(expr instanceof LocalVar) {
			return translate((LocalVar) expr, environment,point,method,owner);
		} else if(expr instanceof InstanceOf) {
			return translate((InstanceOf) expr, environment,point,method,owner);
		} else if(expr instanceof Invoke) {
			return translate((Invoke) expr, environment,point,method,owner);
		} else if(expr instanceof New) {
			return translate((New) expr, environment,point,method,owner);
		} else if(expr instanceof UnOp) {		
			return translate((UnOp) expr, environment,point,method,owner);								
		} else if(expr instanceof ArrayVal) {
			return translate((ArrayVal) expr, environment,point,method,owner);
		} else if(expr instanceof Value) {		
			return translate((Value) expr, environment,point,method,owner);
		} else if(expr instanceof TernOp) {
			return translate((TernOp) expr, environment, point, method, owner);
		} else {
			throw new InternalException("Unknown expression \"" + expr + "\" encoutered",point,method,owner);
		}
	}
	
	/**
	 * Type check ArrayIndex expression.
	 */
	protected Expr translate(ArrayIndex ai,Map<String,Type> environment,Point point, Method method, Clazz owner) {		
		Expr lhs = translate(ai.array,environment,point,method,owner);
		Expr rhs = translate(ai.idx,environment,point,method,owner);
		return new ArrayIndex(lhs,rhs,ai.type);	
	}
		
	protected Expr translate(BinOp bop,Map<String,Type> environment,Point point, Method method, Clazz owner) {		
		Expr lhs = translate(bop.lhs,environment,point,method,owner);
		Expr rhs = translate(bop.rhs,environment,point,method,owner);
		return new BinOp(bop.op,lhs,rhs,bop.type); 
	}
	
	protected Expr translate(Cast cast,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		Expr expr = translate(cast.expr,environment,point,method,owner);
		return new Cast(cast.type,expr);
	}
	
	protected Expr translate(ClassAccess ca,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		return ca;
	}
	
	protected Expr translate(Deref deref,Map<String,Type> environment,Point point, Method method, Clazz owner) {	
		Expr target = translate(deref.target,environment,point,method,owner);
		return new Deref(target,deref.name,deref.type);
	}
	
	protected Expr translate(FlowGraph.Exception exception, Map<String, Type> environment, Point point, Method method, Clazz owner) {
		return exception;
	}
	
	protected Expr translate(InstanceOf iof,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		Expr lhs = translate(iof.lhs,environment,point,method,owner);		
		return new InstanceOf(lhs,iof.rhs);		
	}
	
	protected Expr translate(Invoke ivk,Map<String,Type> environment,Point point, Method method, Clazz owner) {			
		Expr target = translate(ivk.target,environment,point,method,owner);
		ArrayList<Expr> params = new ArrayList<Expr>();		
		for(Expr e : ivk.parameters) {
			Expr n = translate(e,environment,point,method,owner);
			params.add(n);			
		}		
		return new Invoke(target,ivk.name,params,ivk.type,ivk.polymorphic);							
	}
		
	protected Expr translate(New ne,Map<String,Type> environment,Point point,Method method,Clazz owner) {			
		ArrayList<Expr> params = new ArrayList<Expr>();
		for(Expr e : ne.parameters) {
			Expr n = translate(e,environment,point,method,owner);
			params.add(n);			
		}				
		return new New(ne.type,params);
	}
	
	protected Expr translate(UnOp uop,Map<String,Type> environment,Point point,Method method,Clazz owner) {
		Expr expr = translate(uop.expr,environment,point,method,owner);			
		return new UnOp(uop.op,expr,uop.type);				
	}
	
	protected Expr translate(TernOp top, Map<String, Type> environment, Point point, Method method, Clazz owner) {
		Expr cond = translate(top.cond, environment, point, method, owner);
		Expr trop = translate(top.toption, environment, point, method, owner);
		Expr fop = translate(top.foption, environment, point, method, owner);
		return new TernOp(cond, trop, fop, top.type);
	}
	
	protected Expr translate(LocalVar var,Map<String,Type> environment, Point point, Method method, Clazz owner) {
		return var;
	}
	
	protected Expr translate(ArrayVal val,Map<String,Type> environment, Point point, Method method, Clazz owner) {
		return val;
	}
	
	protected Expr translate(Value val,Map<String,Type> environment, Point point, Method method, Clazz owner) {
		return val;
	}

	
	/**
	 * This method flattens the control-flow graph into a sequence of
	 * statements. This is done using a depth-first traversal of the CFG, whilst
	 * ignoring exception edges.
	 * 
	 * @param entry
	 *            entry point of method in control-flow graph.
	 * @param cfg
	 *            control-flow graph of method.
	 * @return List of statements in their flattened order
	 */
	public static ArrayList<Point> cfgOrder(FlowGraph cfg) {
		// first, perform the depth-first search.
		ArrayList<Point> ord = new ArrayList<Point>();
		HashSet<Point> visited = new HashSet<Point>();
		cfgVisit(cfg.entry(), visited, ord, cfg);
		// Find other starting points in the CFG, since exception handlers may
        // not yet be connected!
		for(Point p : cfg.domain()) {
			if(cfg.to(p).isEmpty() && !visited.contains(p)) {
				cfgVisit(p, visited, ord, cfg);
			}
		}		
		// we need to reverse the ordering here, since cfg_visit
		// will have added the statements in reverse topological order!
		Collections.reverse(ord);
		return ord;
	}

	/**
	 * This method performs a standard depth-first search.
	 * 
	 * @param cfg
	 *            the control-flow graph.
	 */
	protected static void cfgVisit(Point v, Set<Point> visited,
			List<Point> ord, Graph<Point, Triple<Point, Point, Expr>> cfg) {		
		visited.add(v);

		// Sort out-edges according to their target position in the program.
		// Doing this helps ensure blocks which are located close together in
		// the source remain close together. Otherwise, for example, you end up
		// with for-loops where the code after the for loop comes before the
		// for-loop body!!!
		ArrayList<Pair<Point, Point>> outs;
		outs = new ArrayList<Pair<Point, Point>>(cfg.from(v));

		Collections.sort(outs, new Comparator<Pair<Point, Point>>() {
			public int compare(Pair<Point, Point> p1, Pair<Point, Point> p2) {
				Point e1 = p1.second();
				Point e2 = p2.second();
				if (e1.line() < e2.line()) {
					return -1;
				} else if (e1.line() == e2.line()) {
					if (e1.column() < e2.column()) {
						return -1;
					} else if (e1.column() == e2.column()) {
						return 0;
					}
				}
				return 1;
			}
		});

		// Now, visit the edges in their sorted order
		for (Pair<Point, Point> e : outs) {
			if (!visited.contains(e.second())) {
				cfgVisit(e.second(), visited, ord, cfg);
			}
		}
		ord.add(v);
	}
}
