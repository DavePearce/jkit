package jkit.jil.ipa;

import static jkit.compiler.SyntaxError.syntax_error;

import java.util.List;

import jkit.jil.tree.*;
import jkit.jil.util.*;

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
public class StaticDependenceGraph {
	
	public static class Invocation extends Pair<Tag.Method,Tag.Method> {
		public Invocation(Tag.Method from, Tag.Method to) {
			super(from,to);
		}
		public Tag.Method from() {
			return first();
		}
		public Tag.Method to() {
			return second();
		}
		public String toString() {
			return first() + "->" + second();
		}
	}
	
	private Graph<Tag.Method,Invocation> callGraph;
	
	/**
     * Access the constructed call graph. Each node in the call graph is
     * identified by a triple (O,N,T), where O=owner, N=name and T=type.
     */	
	public Graph<Tag.Method,Invocation> callGraph() {
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
		Tag.Method myNode = new Tag.Method(owner.type(), method.name(), method.type());
		List<JilStmt> body = method.body();
		
		// first, initialise label map		
		for(JilStmt s : body) {
			addCallGraphEdges(s, myNode);			 			
		}
	}
	
	protected void addCallGraphEdges(JilStmt stmt, Tag.Method myNode) {
		if(stmt instanceof JilStmt.IfGoto) {
			addCallGraphEdges((JilStmt.IfGoto)stmt,myNode);					
		} else if(stmt instanceof JilStmt.Switch) {
			addCallGraphEdges((JilStmt.Switch)stmt,myNode);					
		} else if(stmt instanceof JilStmt.Assign) {
			addCallGraphEdges((JilStmt.Assign)stmt,myNode);					
		} else if(stmt instanceof JilExpr.Invoke) {
			addCallGraphEdges((JilExpr.Invoke)stmt,myNode);										
		} else if(stmt instanceof JilExpr.New) {
			addCallGraphEdges((JilExpr.New) stmt,myNode);						
		} else if(stmt instanceof JilStmt.Return) {
			addCallGraphEdges((JilStmt.Return) stmt,myNode);
		} else if(stmt instanceof JilStmt.Throw) {
			addCallGraphEdges((JilStmt.Throw) stmt,myNode);
		} else if(stmt instanceof JilStmt.Nop) {		
			
		} else if(stmt instanceof JilStmt.Label) {		
			
		} else if(stmt instanceof JilStmt.Goto) {		
			
		} else if(stmt instanceof JilStmt.Lock) {		
			addCallGraphEdges((JilStmt.Lock) stmt, myNode);
		} else if(stmt instanceof JilStmt.Unlock) {		
			addCallGraphEdges((JilStmt.Unlock) stmt, myNode);
		} else {
			syntax_error("unknown statement encountered (" + stmt.getClass().getName() + ")",stmt);			
		}		
	}
	
	protected void addCallGraphEdges(JilStmt.IfGoto stmt, Tag.Method myNode) {		
		addCallGraphEdges(stmt.condition(),myNode);			
	}
	
	protected void addCallGraphEdges(JilStmt.Switch stmt, Tag.Method myNode) {		
		addCallGraphEdges(stmt.condition(),myNode);			
	}
	
	protected void addCallGraphEdges(JilStmt.Assign stmt, Tag.Method myNode) {
		addCallGraphEdges(stmt.lhs(),myNode);
		addCallGraphEdges(stmt.rhs(),myNode);
	}
	
	protected void addCallGraphEdges(JilStmt.Return stmt, Tag.Method myNode) {
		if(stmt.expr() != null) {
			addCallGraphEdges(stmt.expr(),myNode);
		}		
	}
	
	protected void addCallGraphEdges(JilStmt.Throw stmt, Tag.Method myNode) {		
		addCallGraphEdges(stmt.expr(),myNode);			
	}
	
	protected void addCallGraphEdges(JilStmt.Lock stmt, Tag.Method myNode) {		
		addCallGraphEdges(stmt.expr(),myNode);			
	}
	
	protected void addCallGraphEdges(JilStmt.Unlock stmt, Tag.Method myNode) {		
		addCallGraphEdges(stmt.expr(),myNode);			
	}
	
	protected void addCallGraphEdges(JilExpr expr, Tag.Method myNode) {
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
	
	public void addCallGraphEdges(JilExpr.ArrayIndex expr, Tag.Method myNode) { 
		addCallGraphEdges(expr.target(), myNode);
		addCallGraphEdges(expr.index(), myNode);		
	}	
	public void addCallGraphEdges(JilExpr.BinOp expr, Tag.Method myNode) {
		addCallGraphEdges(expr.lhs(), myNode);
		addCallGraphEdges(expr.rhs(), myNode);		
	}
	public void addCallGraphEdges(JilExpr.UnOp expr, Tag.Method myNode) { 		
		addCallGraphEdges(expr.expr(), myNode); 
	}
	public void addCallGraphEdges(JilExpr.Cast expr, Tag.Method myNode) { 
		addCallGraphEdges(expr.expr(), myNode);		
	}
	public void addCallGraphEdges(JilExpr.Convert expr, Tag.Method myNode) { 
		addCallGraphEdges(expr.expr(), myNode);		
	}
	public void addCallGraphEdges(JilExpr.ClassVariable expr, Tag.Method myNode) { 		
		// do nothing!
	}
	public void addCallGraphEdges(JilExpr.Deref expr, Tag.Method myNode) { 		
		addCallGraphEdges(expr.target(), myNode);						
	}	
	public void addCallGraphEdges(JilExpr.Variable expr, Tag.Method myNode) { 
		// do nothing!
	}
	public void addCallGraphEdges(JilExpr.InstanceOf expr, Tag.Method myNode) { 		
		addCallGraphEdges(expr.lhs(), myNode);
	}
	public void addCallGraphEdges(JilExpr.Invoke expr, Tag.Method myNode) { 
		JilExpr target = expr.target();
		addCallGraphEdges(target, myNode);
		for(JilExpr e : expr.parameters()) {
			addCallGraphEdges(e, myNode);
		}		
			
		Tag.Method targetNode = new Tag.Method((Type.Reference) target.type(), expr.name(),
				expr.funType());
		
		// Add the call graph edge!
		callGraph.add(new Invocation(myNode,targetNode));
	}

	public void addCallGraphEdges(JilExpr.New expr, Tag.Method myNode) { 		
		for(JilExpr e : expr.parameters()) {
			addCallGraphEdges(e, myNode);
		}

		// Interesting issue here if target is not a class. Could be an array,
        // for example.		
		if(expr.type() instanceof Type.Clazz) {
			Type.Clazz type = (Type.Clazz) expr.type();
			Tag.Method targetNode = new Tag.Method(type, type.lastComponent().first(), expr
				.funType());
		
			// Add the call graph edge!
			callGraph.add(new Invocation(myNode,targetNode));
		}
	}
	
	public void addCallGraphEdges(JilExpr.Value expr, Tag.Method myNode) { 		
		if(expr instanceof JilExpr.Array) {
			JilExpr.Array ae = (JilExpr.Array) expr;
			for(JilExpr v : ae.values()) {
				addCallGraphEdges(v, myNode);
			}
		}		
	}
}
