package jkit.stages.codegen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jkit.compiler.ClassTable;
import jkit.compiler.FieldNotFoundException;
import jkit.compiler.InternalException;
import jkit.compiler.MethodNotFoundException;
import jkit.core.*;
import jkit.jkil.Clazz;
import jkit.jkil.FlowGraph;
import jkit.jkil.Method;
import jkit.jkil.Type;
import jkit.jkil.FlowGraph.*;
import jkit.stages.Stage;
import jkit.util.Triple;


/**
 * The purpose of this stage is to break down try-catch blocks and connect
 * exception edges from the statements which may generate them to the
 * appropriate catch handlers.
 * 
 * @author djp
 */
public class Exceptions implements Stage {
	public String description() {
		return "Add exceptional edges to method flow graphs";
	}
	
	public void apply(Clazz owner) {		
		for(Method m : owner.methods()) {
			if(m.code() != null) {
				apply(m,owner);
			}
		}
	}
	
	/**
	 * Add exceptional edges to a particular method
	 * 
	 * @param method
	 */
	public void apply(Method method, Clazz owner) {
		FlowGraph cfg = method.code();
		
		// the following is needed as a temporary work around
		HashSet<Point> domain = new HashSet<Point>(cfg.domain());
		Map<String,Type> environment = FlowGraph.buildEnvironment(method,owner);
		for(Point p : domain) {
			apply(p,environment,method,owner);			
		}
		
		// Now, check the conditional edges
		HashSet<Triple<Point,Point,Expr>> edges = new HashSet<Triple<Point,Point,Expr>>(cfg);
		for(Triple<Point,Point,Expr> edge : edges) {
			try {
				if(edge.third() != null) {
					Set<Type.Reference> es = exceptionsOf(edge.third());					
					addExceptionalEdges(edge.first(),es,method.code());
				}
			} catch(ClassNotFoundException e) {
				throw new InternalException(e.getMessage(), edge.first(),method,owner);
			} catch(MethodNotFoundException e) {
				throw new InternalException(e.getMessage(), edge.first(),method,owner);
			} catch(FieldNotFoundException e) {
				throw new InternalException(e.getMessage(), edge.first(),method,owner);
			}
		}
	}
	
	/**
	 * Add exceptional edges for a particular program point
	 * 
	 * @param p The point to add exceptional edges to
	 * @param method The enclosing method
	 * @param owner The enclosing class
	 * @throws InternalError
	 */
	public void apply(Point p, Map<String,Type> environment, Method method, Clazz owner) {
		Stmt s = p.statement();
		
		try {
			if(s instanceof Assign) {
				Assign a = (Assign) s;
				Set<Type.Reference> es = exceptionsOf(a.lhs);
				es.addAll(exceptionsOf(a.rhs));							
				if(a.lhs instanceof ArrayIndex) {					
					es.add(Type.referenceType("java.lang","ArrayStoreException"));
				}
				addExceptionalEdges(p,es,method.code());
			} else if(s instanceof Return) {
				Return r = (Return) s;
				if(r.expr != null) {
					Set<Type.Reference> es = exceptionsOf(r.expr);
					addExceptionalEdges(p,es,method.code());
				}
			} else if(s instanceof Invoke) {
				Invoke c = (Invoke) s;			
				Set<Type.Reference> es = exceptionsOf(c);			
				addExceptionalEdges(p,es,method.code());
			} else if(s instanceof New) {
				New c = (New) s;			
				Set<Type.Reference> es = exceptionsOf(c);			
				addExceptionalEdges(p,es,method.code());
			} else if(s instanceof Throw) {
				Throw t = (Throw) s;
				Set<Type.Reference> es = exceptionsOf(t.expr);
				if(t.expr.type instanceof Type.Reference) {
					es.add((Type.Reference) t.expr.type);
				}
				addExceptionalEdges(p,es,method.code());
			} else {
				// can ignore Nop
			}
		} catch(ClassNotFoundException e) {
			throw new InternalException(e, p,method,owner);
		} catch(MethodNotFoundException e) {
			throw new InternalException(e,p,method,owner);
		} catch(FieldNotFoundException e) {
			throw new InternalException(e,p,method,owner);
		}
	}
	
	/**
     * Add exceptional edges for those exceptions listed from a given point in
     * the control-flow graph
     * 
     * @param p The point where the exceptions may occur
     * @param exceptions The list of exceptions which may occur
     * @param cfg The control-flow graph to add the exceptional edges to
     */
	public void addExceptionalEdges(Point p, Set<Type.Reference> exceptions,
			FlowGraph cfg) {
		if(exceptions == null) { return; }
		
		for(Type.Reference e : exceptions) {
			for(Region r : cfg.regions()) {
				if(r instanceof CatchBlock && r.contains(p)) {					
					// found a candidate catch block
					CatchBlock cb = (CatchBlock) r;
					// so check whether it catches any of the required exceptions
					if(cb.type.supsetEqOf(e)) {		
						// so, add the exceptional edge!!!
						if(cb.type.unqualifiedName().equals("java.lang.Throwable")) {
							cfg.add(new Triple<Point, Point, Expr>(p, cb.entry, new FlowGraph.Exception(cb.type)));
						} else {
							cfg.add(new Triple<Point, Point, Expr>(p,cb.entry,new FlowGraph.Exception(e)));
						}
						// This exception is completely subsumed by this catch
						// handler.  Therefore, do not consider other handlers.  
						break;
					} else if(e.supsetEqOf(cb.type)) {						
						// so, add the exceptional edge!!!
						cfg.add(new Triple<Point, Point, Expr>(p, cb.entry, new FlowGraph.Exception(cb.type)));												
						// This exception is not completely subsumed by this catch
						// handler.  Therefore, do continue to consider other handlers.  
					}
				}
			}
		}
	}
	
	/**
     * This method determines the possible exceptions thrown by an expression
     * given a typing environment.
     * 
     * @param expr
     *            The expression whose exceptions we want to know
     * @return The set of exceptions which can be thrown
     * @throws ClassNotFoundException
     *             If it needs to access a Class which cannot be found.
     * @throws FieldNotFoundException
     *             When a field is accessed which cannot be located in the given
     *             class.
     * @throws MethodNotFoundException
     *             When a method is accessed which cannot be located in the
     *             given class.
     */
	public Set<Type.Reference> exceptionsOf(Expr expr)
			throws ClassNotFoundException, FieldNotFoundException,
			MethodNotFoundException {
		
		if(expr instanceof BinOp) {				
			BinOp bop = (BinOp) expr;
			Set<Type.Reference> lhs = exceptionsOf(bop.lhs);
			Set<Type.Reference> rhs = exceptionsOf(bop.rhs);
			HashSet<Type.Reference> r = new HashSet<Type.Reference>();
			r.addAll(lhs);
			r.addAll(rhs);
			if(bop.op == BinOp.DIV) {
				r.add(Type.referenceType("java.lang","ArithmeticException"));
			}
			return r;
		} if(expr instanceof UnOp) {						
			UnOp uop = (UnOp) expr;
			return exceptionsOf(uop.expr);			
		} if(expr instanceof InstanceOf) {						
			InstanceOf uop = (InstanceOf) expr;
			return exceptionsOf(uop.lhs);			
		} else if(expr instanceof ArrayIndex) {
			ArrayIndex ai = (ArrayIndex) expr;
			Set<Type.Reference> r = exceptionsOf(ai.idx);
			r.addAll(exceptionsOf(ai.array));
			r.add(Type.referenceType("java.lang","NullPointerException"));
			r.add(Type.referenceType("java.lang","ArrayIndexOutOfBoundsException"));			
			return r;
		} else if(expr instanceof Deref) {
			Deref d = (Deref) expr;
			Set<Type.Reference> r = exceptionsOf(d.target);
			r.add(Type.referenceType("java.lang","NullPointerException"));
			return r;
		} else if (expr instanceof Cast) {
			Cast c = (Cast) expr;
			Set<Type.Reference> r = exceptionsOf(c.expr);
			r.add(Type.referenceType("java.lang","ClassCastException"));
			return r;
		} else if(expr instanceof New) {
			Set<Type.Reference> r = exceptionsOf((New) expr);
			if(expr.type instanceof Type.Array) {
				r.add(Type.referenceType("java.lang","NegativeArraySizeException"));
			}
			r.add(Type.referenceType("java.lang","OutOfMemoryError"));
			return r;
		} else if(expr instanceof Invoke) {		
			return exceptionsOf((Invoke) expr);			
		} else if(expr instanceof ArrayVal) {
			ArrayVal av = (ArrayVal) expr;
			Set<Type.Reference> r = new HashSet<Type.Reference>();
			for(Expr e : av.values) {
				r.addAll(exceptionsOf(e));
			}
			return r;
		} else {
			// Value, ClassAccess, LocalVar, UnOp, Instanceof
			// None of these throw exceptions
			return new HashSet<Type.Reference>();
		}
	}
		
	/**
	 * Compute the set of exceptions thrown by a constructor call.
	 * 
	 * @param news
	 * @return
	 * @throws ClassNotFoundException
	 * @throws FieldNotFoundException
	 * @throws MethodNotFoundException
	 */
	private Set<Type.Reference> exceptionsOf(New news)
			throws ClassNotFoundException,
			FieldNotFoundException, MethodNotFoundException {
				
		HashSet<Type.Reference> exceptions = new HashSet<Type.Reference>();

		if(!(news.type instanceof Type.Reference)) {
			// this must be an array creation, which can't throw an exception
			// so we can ignore;
			return exceptions;			
		}
				
		// translate parameters
		ArrayList<Type> parameterTypes = new ArrayList<Type>();
		for(Expr p : news.parameters) {						
			parameterTypes.add(p.type);
			exceptions.addAll(exceptionsOf(p));
		}
				
		// Now, perform the resolve method to figure out who is actually being
        // called!		 				
		Type.Reference net = (Type.Reference) news.type; 
		Method method = ClassTable.resolveMethod(net,net.name(), parameterTypes).second();		
		
		// finally, we can add any exceptions which are explicitly thrown by
        // this method invocation ...
				
		for(Type.Reference e : method.exceptions()) {		
			exceptions.add(e);
		}
		exceptions.add(Type.referenceType("java.lang","RuntimeException"));
		
		return exceptions;
	}
	
	protected Set<Type.Reference> exceptionsOf(Invoke call)
			throws ClassNotFoundException, FieldNotFoundException,
			MethodNotFoundException {
		Set<Type.Reference> exceptions = exceptionsOf(call.target);

		// Add exceptions arising from parameters		
		for(Expr p : call.parameters) {						
			exceptions.addAll(exceptionsOf(p));
		}
				
		// Determine actual method being called
		Method method = FlowGraph.resolveMethod(call).second();		
		
		// Add exceptions explicitly thrown by this method invocation.		
		for(Type.Reference e : method.exceptions()) {
			exceptions.add(e);
		}
		
		// Finally, add catch for all remaining (unchecked) exceptions
		exceptions.add(Type.referenceType("java.lang","RuntimeException"));
		
		return exceptions;
	}
}
