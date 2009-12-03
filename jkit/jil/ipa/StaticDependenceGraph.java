package jkit.jil.ipa;

import static jkit.compiler.SyntaxError.*;

import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.compiler.*;
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
	
	public static class FieldAccess extends Pair<Tag,Tag> {
		public FieldAccess(Tag.Method from, Tag.Field to) {
			super(from,to);
		}
		public Tag.Method method() {
			return (Tag.Method) first();
		}
		public Tag.Field field() {
			return (Tag.Field) second();
		}
		public String toString() {
			return first() + "=>" + second();
		}
	}
	
	private Graph<Tag.Method,Invocation> callGraph;
	private Graph<Tag,FieldAccess> fieldReads;
	private Graph<Tag,FieldAccess> fieldWrites;
	
	private ClassLoader loader;
	
	public StaticDependenceGraph(ClassLoader loader) {
		this.loader = loader;
	}
	
	/**
     * Access the constructed call graph. Each node in the call graph is
     * identified by a triple (O,N,T), where O=owner, N=name and T=type.
     */	
	public Graph<Tag.Method,Invocation> callGraph() {
		return callGraph;
	}
	
	public Graph<Tag,FieldAccess> fieldReads() {
		return fieldReads;
	}
	
	public Graph<Tag,FieldAccess> fieldWrites() {
		return fieldWrites;
	}
	
	public void apply(List<JilClass> classes) {
		callGraph = new DirectedAdjacencyList();
		fieldReads = new DirectedAdjacencyList();
		fieldWrites = new DirectedAdjacencyList();
		for(JilClass owner : classes) {
			for (JilMethod method : owner.methods()) {				
				build(method,owner);				
			}
		}
	}
	
	protected void build(JilMethod method, JilClass owner) {
		Tag.Method myNode = Tag.create(owner, method.name(), method.type());
		List<JilStmt> body = method.body();
		
		// first, initialise label map		
		for(JilStmt s : body) {
			addEdges(s, myNode);			 			
		}
	}
	
	protected void addEdges(JilStmt stmt, Tag.Method myNode) {
		if(stmt instanceof JilStmt.IfGoto) {
			addEdges((JilStmt.IfGoto)stmt,myNode);					
		} else if(stmt instanceof JilStmt.Switch) {
			addEdges((JilStmt.Switch)stmt,myNode);					
		} else if(stmt instanceof JilStmt.Assign) {
			addEdges((JilStmt.Assign)stmt,myNode);					
		} else if(stmt instanceof JilExpr.Invoke) {
			addEdges((JilExpr.Invoke)stmt,myNode);										
		} else if(stmt instanceof JilExpr.New) {
			addEdges((JilExpr.New) stmt,myNode);						
		} else if(stmt instanceof JilStmt.Return) {
			addEdges((JilStmt.Return) stmt,myNode);
		} else if(stmt instanceof JilStmt.Throw) {
			addEdges((JilStmt.Throw) stmt,myNode);
		} else if(stmt instanceof JilStmt.Nop) {		
			
		} else if(stmt instanceof JilStmt.Label) {		
			
		} else if(stmt instanceof JilStmt.Goto) {		
			
		} else if(stmt instanceof JilStmt.Lock) {		
			addEdges((JilStmt.Lock) stmt, myNode);
		} else if(stmt instanceof JilStmt.Unlock) {		
			addEdges((JilStmt.Unlock) stmt, myNode);
		} else {
			syntax_error("unknown statement encountered (" + stmt.getClass().getName() + ")",stmt);			
		}		
	}
	
	protected void addEdges(JilStmt.IfGoto stmt, Tag.Method myNode) {		
		addEdges(stmt.condition(),myNode);			
	}
	
	protected void addEdges(JilStmt.Switch stmt, Tag.Method myNode) {		
		addEdges(stmt.condition(),myNode);			
	}
	
	protected void addEdges(JilStmt.Assign stmt, Tag.Method myNode) {		
		if(stmt.lhs() instanceof JilExpr.Deref) {
			// this indicates a field write			
			JilExpr.Deref df = (JilExpr.Deref) stmt.lhs();
			
			addEdges(df.target(),myNode);
			
			try {
				Tag.Field targetNode = Tag.create((Type.Reference) df.target()
						.type(), df.name(), loader);				

				// Add the call graph edge!
				fieldWrites.add(new FieldAccess(myNode,targetNode));


			} catch(FieldNotFoundException mnfe) {
				internal_error(df,mnfe);
			} catch(ClassNotFoundException cnfe) {
				internal_error(df,cnfe);
			}
		} else  {
			addEdges(stmt.lhs(),myNode);
		}
		
		addEdges(stmt.rhs(),myNode);			
	}
	
	protected void addEdges(JilStmt.Return stmt, Tag.Method myNode) {
		if(stmt.expr() != null) {
			addEdges(stmt.expr(),myNode);
		}		
	}
	
	protected void addEdges(JilStmt.Throw stmt, Tag.Method myNode) {		
		addEdges(stmt.expr(),myNode);			
	}
	
	protected void addEdges(JilStmt.Lock stmt, Tag.Method myNode) {		
		addEdges(stmt.expr(),myNode);			
	}
	
	protected void addEdges(JilStmt.Unlock stmt, Tag.Method myNode) {		
		addEdges(stmt.expr(),myNode);			
	}
	
	protected void addEdges(JilExpr expr, Tag.Method myNode) {
		if(expr instanceof JilExpr.ArrayIndex) {
			addEdges((JilExpr.ArrayIndex) expr, myNode);
		} else if(expr instanceof JilExpr.BinOp) {		
			addEdges((JilExpr.BinOp) expr, myNode);
		} else if(expr instanceof JilExpr.UnOp) {		
			addEdges((JilExpr.UnOp) expr, myNode);								
		} else if(expr instanceof JilExpr.Cast) {
			addEdges((JilExpr.Cast) expr, myNode);			 			
		}  else if(expr instanceof JilExpr.Convert) {
			addEdges((JilExpr.Convert) expr, myNode);			 			
		} else if(expr instanceof JilExpr.ClassVariable) {
			addEdges((JilExpr.ClassVariable) expr, myNode);			 			
		} else if(expr instanceof JilExpr.Deref) {
			addEdges((JilExpr.Deref) expr, myNode);			 							
		} else if(expr instanceof JilExpr.Variable) {
			addEdges((JilExpr.Variable) expr, myNode);
		} else if(expr instanceof JilExpr.InstanceOf) {
			addEdges((JilExpr.InstanceOf) expr, myNode);
		} else if(expr instanceof JilExpr.Invoke) {
			addEdges((JilExpr.Invoke) expr, myNode);
		} else if(expr instanceof JilExpr.New) {
			addEdges((JilExpr.New) expr, myNode);
		} else if(expr instanceof JilExpr.Value) {
			addEdges((JilExpr.Value) expr, myNode);
		}
	}
	
	public void addEdges(JilExpr.ArrayIndex expr, Tag.Method myNode) { 
		addEdges(expr.target(), myNode);
		addEdges(expr.index(), myNode);		
	}	
	public void addEdges(JilExpr.BinOp expr, Tag.Method myNode) {
		addEdges(expr.lhs(), myNode);
		addEdges(expr.rhs(), myNode);		
	}
	public void addEdges(JilExpr.UnOp expr, Tag.Method myNode) { 		
		addEdges(expr.expr(), myNode); 
	}
	public void addEdges(JilExpr.Cast expr, Tag.Method myNode) { 
		addEdges(expr.expr(), myNode);		
	}
	public void addEdges(JilExpr.Convert expr, Tag.Method myNode) { 
		addEdges(expr.expr(), myNode);		
	}
	public void addEdges(JilExpr.ClassVariable expr, Tag.Method myNode) { 		
		// do nothing!
	}
	public void addEdges(JilExpr.Deref expr, Tag.Method myNode) {
		addEdges(expr.target(), myNode);

		Type.Reference target = (Type.Reference) expr.target().type();
		
		if(target instanceof Type.Clazz) {

			try {				
				Tag.Field targetNode = Tag.create((Type.Reference) expr.target()
						.type(), expr.name(), loader);				
						
				fieldReads.add(new FieldAccess(myNode, targetNode));
			} catch (FieldNotFoundException mnfe) {
				internal_error(expr, mnfe);
			} catch (ClassNotFoundException cnfe) {
				internal_error(expr, cnfe);
			}
		}
	}	
	public void addEdges(JilExpr.Variable expr, Tag.Method myNode) { 
		// do nothing!
	}
	public void addEdges(JilExpr.InstanceOf expr, Tag.Method myNode) { 		
		addEdges(expr.lhs(), myNode);
	}
	public void addEdges(JilExpr.Invoke expr, Tag.Method myNode) { 
		JilExpr target = expr.target();
		
		// So, at this point, we appear have a method call to the given
		// target.type(). However, in practice, it's not quite that simple. In
		// particular, it may occur that the method in question doesn't actually
		// exist. This happens when the method being called is actually in some
		// supertype of the target.type().

		try {			
			Pair<Clazz,Clazz.Method> rt = loader.determineMethod((Type.Reference) target.type(),expr.name(),expr.funType());
		
			if (!rt.second().isStatic()) {
				// In the case of a static method call, the target is not "used"
				addEdges(target, myNode);
			}
			
			for(JilExpr e : expr.parameters()) {
				addEdges(e, myNode);
			}		
								
						
			// FIXME: One potential problem arises here when the set of potential
            // target methods is greater than one. In this case, we miss edges
            // to all of them
			
			Tag.Method targetNode = Tag.create(rt.first(), expr.name(),
				expr.funType());				
									
			// Add the call graph edge!
			callGraph.add(new Invocation(myNode,targetNode));
		
		} catch(MethodNotFoundException mnfe) {
			internal_error(expr,mnfe);
		} catch(ClassNotFoundException cnfe) {
			internal_error(expr,cnfe);
		}
		
	}

	public void addEdges(JilExpr.New expr, Tag.Method myNode) {
		for (JilExpr e : expr.parameters()) {
			addEdges(e, myNode);
		}

		// Interesting issue here if target is not a class. Could be an array,
		// for example.
		if (expr.type() instanceof Type.Clazz) {
			try {
				Type.Clazz type = (Type.Clazz) expr.type();
				Tag.Method targetNode = Tag.create(type, type.lastComponent()
						.first(), expr.funType(), loader);

				// Add the call graph edge!
				callGraph.add(new Invocation(myNode, targetNode));
			} catch (MethodNotFoundException mnfe) {
				internal_error(expr, mnfe);
			} catch (ClassNotFoundException cnfe) {
				internal_error(expr, cnfe);
			}
		}
	}
	
	public void addEdges(JilExpr.Value expr, Tag.Method myNode) { 		
		if(expr instanceof JilExpr.Array) {
			JilExpr.Array ae = (JilExpr.Array) expr;
			for(JilExpr v : ae.values()) {
				addEdges(v, myNode);
			}
		}		
	}
}
