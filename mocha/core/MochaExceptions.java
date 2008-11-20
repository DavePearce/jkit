package mocha.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jkit.compiler.ClassTable;
import jkit.compiler.FieldNotFoundException;
import jkit.compiler.InternalException;
import jkit.compiler.MethodNotFoundException;
import jkit.jkil.Clazz;
import jkit.jkil.FlowGraph;
import jkit.jkil.Method;
import jkit.jkil.Type;
import jkit.jkil.FlowGraph.ArrayIndex;
import jkit.jkil.FlowGraph.ArrayVal;
import jkit.jkil.FlowGraph.Assign;
import jkit.jkil.FlowGraph.BinOp;
import jkit.jkil.FlowGraph.Cast;
import jkit.jkil.FlowGraph.CatchBlock;
import jkit.jkil.FlowGraph.Deref;
import jkit.jkil.FlowGraph.Expr;
import jkit.jkil.FlowGraph.InstanceOf;
import jkit.jkil.FlowGraph.Invoke;
import jkit.jkil.FlowGraph.LocalVar;
import jkit.jkil.FlowGraph.New;
import jkit.jkil.FlowGraph.Point;
import jkit.jkil.FlowGraph.Region;
import jkit.jkil.FlowGraph.Return;
import jkit.jkil.FlowGraph.Stmt;
import jkit.jkil.FlowGraph.Throw;
import jkit.jkil.FlowGraph.UnOp;
import jkit.util.Pair;
import jkit.util.Triple;

public class MochaExceptions {

	private Inferrer inf;
	private Map<String, Expr> feIterators = new HashMap<String, Expr>(); 
	
	public MochaExceptions(Inferrer i) {
		inf = i;
	}
	
	public boolean run(Method m, Clazz c) {
		FlowGraph cfg = m.code();
		
		if(cfg == null) { return false;}
		
		buildIterators(cfg);
		boolean changed = false;
		
		Map<Point, TypeMap> environments = inf.environments(m);
		
		for(Point p : environments.keySet()) {
			TypeMap map = environments.get(p);
			Map<String, Type> environment = map.environment();
			changed |= run(p,environment,m,c);
		}
		return changed;
	}
	
	private void buildIterators(FlowGraph cfg) {
		feIterators.clear();
		Set<Point> domain = cfg.domain();
		for(Point p : domain) {
			if(p.statement() instanceof Assign) {
				Assign a = (Assign) p.statement();
				if(a.lhs instanceof LocalVar) {
					LocalVar lv = (LocalVar) a.lhs;
					if(lv.name.contains("$iterator")) {
						feIterators.put(lv.name, a.rhs);
					}
				}
			}
		}
	}
	
	public boolean run(Point p, Map<String, Type> environment, Method method, Clazz owner) {
		Stmt s = p.statement();
		boolean changed = false;
		try {
			if(s instanceof Assign) {
				Assign a = (Assign) s;
				try {
					if(a.lhs instanceof LocalVar) {
						LocalVar lv = (LocalVar) a.lhs;
						if(lv.name.contains("$iterator")) {
							a = translateInit(a, environment);
						} else if(a.rhs instanceof Invoke) {
							Invoke inv = (Invoke) a.rhs;
							if(inv.target instanceof LocalVar) {
								LocalVar tlv = (LocalVar) inv.target;
								if(tlv.name.contains("$iterator")) {
									a = translateAssign(a, environment);
								}
							}
						}
					}
				} catch(ClassNotFoundException cne) {
					throw new InternalException(cne.getMessage(), p, method, owner);
				} catch(FieldNotFoundException fne) {
					throw new InternalException(fne.getMessage(), p, method, owner);
				} catch(MethodNotFoundException mne) {
					throw new InternalException(mne.getMessage(), p, method, owner);
				}
				
				Set<Type.Reference> es = exceptionsOf(a.lhs, environment);
				es.addAll(exceptionsOf(a.rhs, environment));
				if(a.lhs instanceof ArrayIndex) {					
					es.add(Type.referenceType("java.lang","ArrayStoreException"));
				}
				changed = addEdges(p,es,method.code());
			} else if(s instanceof Return) {
				Return r = (Return) s;
				if(r.expr != null) {
					Set<Type.Reference> es = exceptionsOf(r.expr, environment);
					changed = addEdges(p,es,method.code());
				}
			} else if(s instanceof Invoke) {
				Invoke c = (Invoke) s;			
				Set<Type.Reference> es = exceptionsOf(c, environment);			
				changed = addEdges(p,es,method.code());
			} else if(s instanceof New) {
				New c = (New) s;			
				Set<Type.Reference> es = exceptionsOf(c, environment);			
				changed = addEdges(p,es,method.code());
			} else if(s instanceof Throw) {
				Throw t = (Throw) s;
				Set<Type.Reference> es = exceptionsOf(t.expr, environment);
				if(t.expr.type instanceof Type.Reference) {
					es.add((Type.Reference) t.expr.type);
				}
				changed = addEdges(p,es,method.code());
			} else {
				// can ignore Nop
			}
		} catch(ClassNotFoundException e) {
			throw new InternalException(e.getMessage(), p,method,owner);
		} catch(MethodNotFoundException e) {
			throw new InternalException(e.getMessage(),p,method,owner);
		} catch(FieldNotFoundException e) {
			throw new InternalException(e.getMessage(),p,method,owner);
		}
		return changed;
	}
	
	public boolean addEdges(Point p,
			Set<Type.Reference> exceptions, FlowGraph cfg) {
		if(exceptions == null) { return false; }
		
		boolean changed = false;
		for(Type.Reference e : exceptions) {
			for(Region r : cfg.regions()) {
				if(r instanceof CatchBlock && r.contains(p)) {
					// found a candidate catch block
					CatchBlock cb = (CatchBlock) r;
					// so check whether it catches any of the required exceptions
					if(cb.type.supsetEqOf(e)) {
						Set<Triple<Point, Point, Expr>> from = cfg.from(p);
						boolean found = false;
						for(Triple<Point, Point, Expr> t : from) {
							if(t.second() == cb.entry) {
								found = true;
								break;
							}
						}
						if(!found) {
							System.out.println("Adding edge from " + p + " to " + cb.entry);
							if(cb.type.unqualifiedName().equals("java.lang.Throwable")) {
								cfg.add(new Triple(p, cb.entry, new FlowGraph.Exception(cb.type)));
							} else {
								cfg.add(new Triple(p,cb.entry,new FlowGraph.Exception(e)));
							}
							changed = true;
						}
						break;
					} else if(e.supsetEqOf(cb.type)) {
						Set<Triple<Point, Point, Expr>> from = cfg.from(p);
						boolean found = false;
						for(Triple<Point, Point, Expr> t : from) {
							if(t.second() == cb.entry) {
								found = true;
								break;
							}
						}
						if(!found) {
							System.out.println("Adding edge from " + p + " to " + cb.entry);
							// so, add the exceptional edge!!!
							cfg.add(new Triple<Point, Point, Expr>(p, cb.entry, new FlowGraph.Exception(cb.type)));												
							// This exception is not completely subsumed by this catch
							// handler.  Therefore, do continue to consider other handlers.
							changed = true;
						}
					}
				}
			}
		}
		return changed;
	}
	
	public Set<Type.Reference> exceptionsOf(Expr expr, Map<String, Type> environment)
						throws ClassNotFoundException, FieldNotFoundException,
						MethodNotFoundException {

		if(expr instanceof BinOp) {				
			BinOp bop = (BinOp) expr;
			Set<Type.Reference> lhs = exceptionsOf(bop.lhs, environment);
			Set<Type.Reference> rhs = exceptionsOf(bop.rhs, environment);
			HashSet<Type.Reference> r = new HashSet<Type.Reference>();
			r.addAll(lhs);
			r.addAll(rhs);
			if(bop.op == BinOp.DIV) {
				r.add(Type.referenceType("java.lang","ArithmeticException"));
			}
			return r;
		} if(expr instanceof UnOp) {						
			UnOp uop = (UnOp) expr;
			return exceptionsOf(uop.expr, environment);			
		} if(expr instanceof InstanceOf) {						
			InstanceOf uop = (InstanceOf) expr;
			return exceptionsOf(uop.lhs, environment);			
		} else if(expr instanceof ArrayIndex) {
			ArrayIndex ai = (ArrayIndex) expr;
			Set<Type.Reference> r = exceptionsOf(ai.idx, environment);
			r.addAll(exceptionsOf(ai.array, environment));
			r.add(Type.referenceType("java.lang","NullPointerException"));
			r.add(Type.referenceType("java.lang","ArrayIndexOutOfBoundsException"));
			return r;
		} else if(expr instanceof Deref) {
			Deref d = (Deref) expr;
			Set<Type.Reference> r = exceptionsOf(d.target, environment);
			r.add(Type.referenceType("java.lang","NullPointerException"));
			return r;
		} else if (expr instanceof Cast) {
			Cast c = (Cast) expr;
			Set<Type.Reference> r = exceptionsOf(c.expr, environment);
			r.add(Type.referenceType("java.lang","ClassCastException"));
			return r;
		} else if(expr instanceof New) {
			Set<Type.Reference> r = exceptionsOf((New) expr, environment);
			if(expr.type instanceof Type.Array) {
				r.add(Type.referenceType("java.lang","NegativeArraySizeException"));
			}
			return r;
		} else if(expr instanceof Invoke) {		
			return exceptionsOf((Invoke) expr, environment);			
		} else if(expr instanceof ArrayVal) {
			ArrayVal av = (ArrayVal) expr;
			Set<Type.Reference> r = new HashSet<Type.Reference>();
			for(Expr e : av.values) {
				r.addAll(exceptionsOf(e, environment));
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
	private Set<Type.Reference> exceptionsOf(New news, Map<String, Type> environment)
								throws ClassNotFoundException,
								FieldNotFoundException, MethodNotFoundException {

		HashSet<Type.Reference> exceptions = new HashSet<Type.Reference>();

		if(!(Types.typeOf(news, environment) instanceof Type.Reference)) {
			// this must be an array creation, which can't throw an exception
			// so we can ignore;
			return exceptions;			
		}

//		translate parameters
		ArrayList<Type> parameterTypes = new ArrayList<Type>();
		for(Expr p : news.parameters) {						
			parameterTypes.add(Types.typeOf(p, environment));
			exceptions.addAll(exceptionsOf(p, environment));
		}

//		Now, perform the resolve method to figure out who is actually being
//		called!		 				
		Type.Reference net = (Type.Reference) news.type;

		Method method = ClassTable.resolveMethod(net,net.name(), parameterTypes).second();		

//		finally, we can add any exceptions which are explicitly thrown by
//		this method invocation ...

		for(Type.Reference e : method.exceptions()) {		
			exceptions.add(e);
		}
		exceptions.add(Type.referenceType("java.lang","RuntimeException"));

		return exceptions;
	}

	private Set<Type.Reference> exceptionsOf(Invoke call, Map<String, Type> environment) 
								throws ClassNotFoundException, FieldNotFoundException,
								MethodNotFoundException {
		Set<Type.Reference> exceptions = exceptionsOf(call.target, environment);

//		Add exceptions arising from parameters		
		for(Expr p : call.parameters) {						
			exceptions.addAll(exceptionsOf(p, environment));
		}

		ArrayList<Type> parameterTypes = new ArrayList<Type>();
		for(FlowGraph.Expr p : call.parameters) {						
			parameterTypes.add(Types.typeOf(p, environment));
		}

		Type t = Types.typeOf(call.target, environment);
		if(t instanceof Type.Reference) {
			Type.Reference receiver = (Type.Reference) t;
		
						
			Method method = ClassTable.resolveMethod(receiver,call.name,parameterTypes).second();	
	
	//		Add exceptions explicitly thrown by this method invocation.		
			for(Type.Reference e : method.exceptions()) {
				exceptions.add(e);
			}
		}

//		Finally, add catch for all remaining (unchecked) exceptions
		exceptions.add(Type.referenceType("java.lang","RuntimeException"));

		return exceptions;
	}
	
	private Assign translateInit(Assign asgn, Map<String, Type> environment) 
	throws ClassNotFoundException, MethodNotFoundException, FieldNotFoundException {
		Type rhs = Types.typeOf(asgn.rhs, environment);
		LocalVar lv = (LocalVar) asgn.lhs;

		Assign nAsgn = null;

		if(rhs instanceof Type.Array) {
			nAsgn = new Assign(lv, new FlowGraph.IntVal(0));
		} else if(rhs instanceof Type.Reference) {
			Invoke nInvoke = new Invoke(asgn.rhs, "iterator", new ArrayList<Expr>());
			nAsgn = new Assign(lv, nInvoke);
		}
		feIterators.put(lv.name, asgn.rhs);

		return nAsgn; 
	}
	
	private Assign translateAssign(Assign asgn, Map<String, Type> environment)
	throws ClassNotFoundException, MethodNotFoundException, FieldNotFoundException {
		Invoke inv = (Invoke) asgn.rhs;
		LocalVar lv = (LocalVar) asgn.lhs;
		LocalVar invT = (LocalVar) inv.target;

		Expr e = feIterators.get(invT.name);
		Type t = Types.typeOf(e, environment);

		Assign nAsgn = null;

		if(t instanceof Type.Array) {
			nAsgn = new Assign(lv, new ArrayIndex(e, new UnOp(UnOp.POSTINC, inv.target, Type.intType())));
		} else if(t instanceof Type.Reference) {
			Invoke nInv = new Invoke(inv.target, "next", new ArrayList<Expr>());
			nAsgn = new Assign(lv, nInv);
		}
		return nAsgn;
	}

}
