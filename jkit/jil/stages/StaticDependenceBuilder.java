package jkit.jil.stages;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import jkit.jil.tree.*;

import jkit.util.*;
import jkit.util.graph.*;

/**
 * The purpose of this class is to build a static call graph. That is, a graph
 * whose edges represent potential calls between methods. More specifically, an
 * edge from method A to method B exists iff there is an invocation of method B
 * within method A. Observe that, since the construction process is static, the
 * edge will exist regardless of whether or not the method is ever be called (in
 * that context).
 * 
 * @author djp
 * 
 */
public class StaticDependenceBuilder {
	
	public static class Method extends Triple<Type.Clazz, String, Type.Function> {
		public Method(Type.Clazz owner, String name, Type.Function type) {
			super(owner, name, type);
		}

		public Type.Clazz owner() {
			return first();
		}

		public String name() {
			return second();
		}

		public Type.Function type() {
			return third();
		}
		
		public String toString() {
			return first() + "." + second() + ":" + type();
		}		
	}
	
	public static class Field extends Pair<Type.Clazz, String> {
		public Field(Type.Clazz owner, String name) {
			super(owner, name);
		}

		public Type.Clazz owner() {
			return first();
		}

		public String name() {
			return second();
		}
		
		public String toString() {
			return first() + "." + second();
		}		
	}
	
	public static class Invocation extends Pair<Method,Method> {
		public Invocation(Method from, Method to) {
			super(from,to);
		}
		public Method from() {
			return first();
		}
		public Method to() {
			return second();
		}
		public String toString() {
			return first() + "->" + second();
		}
	}
	
	private Graph<Method,Invocation> callGraph;
	
	/**
     * Access the constructed call graph. Each node in the call graph is
     * identified by a triple (O,N,T), where O=owner, N=name and T=type.
     */	
	public Graph<Method,Invocation> callGraph() {
		return callGraph;
	}
	
	public void apply(List<JilClass> classes) {
		callGraph = new DirectedAdjacencyList();
		for(JilClass owner : classes) {
			for (JilMethod method : owner.methods()) {
				build(method,owner);
			}
		}
	}
	
	protected void build(JilMethod method, JilClass owner) {
		Method myNode = new Method(owner.type(), method.name(), method.type());
		List<JilStmt> body = method.body();
		
		// first, initialiser label map		
		for(JilStmt s : body) {
			if(s instanceof JilExpr.Invoke) {
				addCallGraphEdges((JilExpr.Invoke)s, myNode);				
			}			
		}
	}
	
	protected void addCallGraphEdges(JilExpr expr, Method myNode) {
		if(expr instanceof JilExpr.ArrayIndex) {
			addCallGraphEdges((JilExpr.ArrayIndex) expr, myNode);
		} else if(expr instanceof JilExpr.BinOp) {		
			addCallGraphEdges((JilExpr.BinOp) expr, myNode);
		} else if(expr instanceof JilExpr.UnOp) {		
			addCallGraphEdges((JilExpr.UnOp) expr, myNode);								
		} else if(expr instanceof JilExpr.Cast) {
			addCallGraphEdges((JilExpr.Cast) expr, myNode);			 			
		}  else if(expr instanceof JilExpr.Convert) {
			addCallGraphEdges((JilExpr.Convert) expr, myNode);			 			
		} else if(expr instanceof JilExpr.ClassVariable) {
			addCallGraphEdges((JilExpr.ClassVariable) expr, myNode);			 			
		} else if(expr instanceof JilExpr.Deref) {
			addCallGraphEdges((JilExpr.Deref) expr, myNode);			 							
		} else if(expr instanceof JilExpr.Variable) {
			addCallGraphEdges((JilExpr.Variable) expr, myNode);
		} else if(expr instanceof JilExpr.InstanceOf) {
			addCallGraphEdges((JilExpr.InstanceOf) expr, myNode);
		} else if(expr instanceof JilExpr.Invoke) {
			addCallGraphEdges((JilExpr.Invoke) expr, myNode);
		} else if(expr instanceof JilExpr.New) {
			addCallGraphEdges((JilExpr.New) expr, myNode);
		} else if(expr instanceof JilExpr.Value) {
			addCallGraphEdges((JilExpr.Value) expr, myNode);
		}
	}
	
	public void addCallGraphEdges(JilExpr.ArrayIndex expr, Method myNode) { 
		addCallGraphEdges(expr.target(), myNode);
		addCallGraphEdges(expr.index(), myNode);		
	}	
	public void addCallGraphEdges(JilExpr.BinOp expr, Method myNode) {
		addCallGraphEdges(expr.lhs(), myNode);
		addCallGraphEdges(expr.rhs(), myNode);		
	}
	public void addCallGraphEdges(JilExpr.UnOp expr, Method myNode) { 		
		addCallGraphEdges(expr.expr(), myNode); 
	}
	public void addCallGraphEdges(JilExpr.Cast expr, Method myNode) { 
		addCallGraphEdges(expr.expr(), myNode);		
	}
	public void addCallGraphEdges(JilExpr.Convert expr, Method myNode) { 
		addCallGraphEdges(expr.expr(), myNode);		
	}
	public void addCallGraphEdges(JilExpr.ClassVariable expr, Method myNode) { 		
		// do nothing!
	}
	public void addCallGraphEdges(JilExpr.Deref expr, Method myNode) { 		
		addCallGraphEdges(expr.target(), myNode);						
	}	
	public void addCallGraphEdges(JilExpr.Variable expr, Method myNode) { 
		// do nothing!
	}
	public void addCallGraphEdges(JilExpr.InstanceOf expr, Method myNode) { 		
		addCallGraphEdges(expr.lhs(), myNode);
	}
	public void addCallGraphEdges(JilExpr.Invoke expr, Method myNode) { 
		JilExpr target = expr.target();
		addCallGraphEdges(target, myNode);
		for(JilExpr e : expr.parameters()) {
			addCallGraphEdges(e, myNode);
		}		
		
		// Interesting issue here if target is not a class. Could be an array,
        // for example.
		
		Method targetNode = new Method((Type.Clazz) target.type(), expr.name(),
				expr.funType());
		
		// Add the call graph edge!
		callGraph.add(new Invocation(myNode,targetNode));
	}

	public void addCallGraphEdges(JilExpr.New expr, Method myNode) { 		
		for(JilExpr e : expr.parameters()) {
			addCallGraphEdges(e, myNode);
		}

		// Interesting issue here if target is not a class. Could be an array,
        // for example.
		Type.Clazz type = (Type.Clazz) expr.type();
		Method targetNode = new Method(type, type.lastComponent().first(), expr
				.funType());
		
		// Add the call graph edge!
		callGraph.add(new Invocation(myNode,targetNode));
	}
	
	public void addCallGraphEdges(JilExpr.Value expr, Method myNode) { 		
		if(expr instanceof JilExpr.Array) {
			JilExpr.Array ae = (JilExpr.Array) expr;
			for(JilExpr v : ae.values()) {
				addCallGraphEdges(v, myNode);
			}
		}		
	}
}
