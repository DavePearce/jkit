package mocha.core;

import java.util.ArrayList;
import java.util.Map;

import mocha.util.Utils;

import jkit.compiler.InternalException;
import jkit.java.stages.old.Subtyping;
import jkit.java.stages.old.Typing;
import jkit.jkil.Clazz;
import jkit.jkil.FlowGraph;
import jkit.jkil.Method;
import jkit.jkil.Type;
import jkit.jkil.FlowGraph.Assign;
import jkit.jkil.FlowGraph.Cast;
import jkit.jkil.FlowGraph.Expr;
import jkit.jkil.FlowGraph.LocalVar;
import jkit.jkil.FlowGraph.Point;
import jkit.jkil.FlowGraph.Stmt;
import jkit.util.Triple;

public class MochaTyping extends Typing {
	
	private Inferrer inf;
	private boolean verbose;
	
	public MochaTyping(Inferrer i, boolean verb) {
		inf = i;
		verbose = verb;
	}
	
	protected void translate(Method method, Clazz owner) {
		FlowGraph cfg = method.code();
		
		Map<String, Type> origEnv = FlowGraph.buildEnvironment(method, owner);
		
		Map<Point, TypeMap> environments = inf.environments(method);
		
		if(verbose) {
			System.out.println("=== Typing " + Utils.formatMethod(method) + " ===\n");
		}
		
		for(Point p : environments.keySet()) {
			TypeMap map = environments.get(p);
			Map<String, Type> nEnv = map.environment();
			
			Stmt stmt = p.statement();
			if(stmt != null) {
				Stmt nstmt = translate(stmt,nEnv,p,method,owner);			
				p.setStatement(nstmt);
				if(verbose) {
					System.out.println(stmt + " --> " + nstmt + "\n");
				}
			}
		}
		
		// Second, do edges
		ArrayList<Triple<Point,Point,Expr>> newEdges = new ArrayList<Triple<Point,Point,Expr>>();
		for(Triple<Point,Point,Expr> edge : cfg) {
			Expr e = edge.third();
			TypeMap tmp = environments.get(edge.first());
			if(e!=null && tmp != null) {
				e = translate(e,tmp.environment(),edge.first(),method,owner);
				if(!(e.type instanceof Type.Boolean || e instanceof FlowGraph.Exception)) {
					throw new InternalException(
							"Conditional requires \"bool\" type, found " + e.type,
							edge.first(), method, owner);
				} else {
					newEdges.add(new Triple(edge.first(),edge.second(),e));
				}
			} else {
				newEdges.add(edge);
			}
		}
		
		cfg.clear();
		cfg.addAll(newEdges);
	}
	
	protected Stmt translate(Stmt stmt,Map<String,Type> environment,Point p,Method m,Clazz o) {
		if(stmt instanceof Assign) {
			Assign asgn = (Assign) stmt;
			if(asgn.lhs.type instanceof Type.Any && asgn.lhs instanceof LocalVar) {
				LocalVar lv = (FlowGraph.LocalVar) asgn.lhs;
				Type t = environment.get(lv.name);
				LocalVar nlv = new FlowGraph.LocalVar(lv.name, t);
				Assign nasgn = null;
				
				if(asgn.rhs instanceof Cast) {
					Cast cast = (Cast) asgn.rhs;
					if(cast.expr instanceof LocalVar) {
						LocalVar blah = (LocalVar) cast.expr;
						if(blah.name.equals(lv.name)) {
							nasgn = new Assign(nlv, asgn.rhs);
							return nasgn;
						}
					}
				}
				Expr nex = translate(asgn.rhs, environment, p, m, o);
				nasgn = new Assign(nlv, nex);
				return nasgn;
			}
		}
		return super.translate(stmt, environment, p, m, o);
	}

}
