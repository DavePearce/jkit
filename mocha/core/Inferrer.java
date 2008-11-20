package mocha.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import mocha.stats.StatsEngine;
import mocha.util.Utils;

import jkit.compiler.FieldNotFoundException;
import jkit.compiler.InternalException;
import jkit.compiler.MethodNotFoundException;
import jkit.compiler.Stage;
import jkit.compiler.Translator;
import jkit.jkil.Clazz;
import jkit.jkil.Field;
import jkit.jkil.FlowGraph;
import jkit.jkil.Method;
import jkit.jkil.Type;
import jkit.jkil.FlowGraph.*;
import jkit.util.Pair;
import jkit.util.Triple;
import jkit.util.dfa.ForwardAnalysis;

public class Inferrer extends ForwardAnalysis<TypeMap> implements Stage {
	
	private Clazz clazz;
	private Method meth;
	private List<FlowGraph.LocalVarDef> localvars;
	private boolean isConstructor;
	private HashMap<Field, Type> fieldTypes;
	private HashMap<String, Expr> feIterators;
	private IdentityHashMap<Stmt, Stmt> toChng = new IdentityHashMap<Stmt, Stmt>();
	private IdentityHashMap<Expr, Expr> toChngE = new IdentityHashMap<Expr, Expr>();
	private MochaExceptions exceptionHandler;
	
	private boolean verbose;
	
	private HashMap<Method, HashMap<Point, TypeMap>> indxedEnvs = new HashMap<Method, HashMap<Point, TypeMap>>();
	private HashMap<String, Type> biggestTypes;
	
	private StatsEngine eng;
	
	public Inferrer() { 
		this(false, false);
	}
	
	public Inferrer(boolean verb, boolean write) {
		verbose = verb;
		exceptionHandler = new MochaExceptions(this);
		eng = new StatsEngine(write);
	}
	
	public void apply(Clazz owner) {
		toChng.clear();
		toChngE.clear();
		indxedEnvs.clear();
		inferClass(owner);
	}
	
	private void inferClass(Clazz c) {
		clazz = c;
		eng.apply(c);
		
		println("=== Running Inferrer on Class " + c.type() + " ===\n");
		
		// Visit all constructors first so field types can be inferred
		HashMap<Field, Type> resFieldTypes = new HashMap<Field, Type>();

		println("=== Inferring Constructors ===\n");
		for(Method m : clazz.methods()) {
			isConstructor = m.name().equals(c.name()) || m.name().equals("<clinit>");
			if(isConstructor) {
				fieldTypes = new HashMap<Field, Type>();
				inferMethod(m);
				checkFieldInfer(resFieldTypes);
				updateLocalVarDefs();
			}
		}
		fieldTypes = new HashMap<Field, Type>();
		processConstFields();
		checkFieldInfer(resFieldTypes);
		
		insertFieldTypes(resFieldTypes);
		
		println("\n=== Inferring Remainder of Class ===\n");
		
		for(Method m : clazz.methods()) {
			isConstructor = m.name().equals(c.name())  || m.name().equals("<clinit>");
			if(!isConstructor) {
				boolean changed = true;
				while(changed) {
					inferMethod(m);
					changed = exceptionHandler.run(m, c);
				}
				if(m.code() != null) {
					updateLocalVarDefs();
				}
			}
		}
		
		println("\n=== Finished Inferring " + c.type() + " ===\n");
		eng.write();
	}
	
	private void inferMethod(Method m) {
		meth = m;
		feIterators = new HashMap<String, Expr>();
		biggestTypes = new HashMap<String, Type>();
		
		if(m.code() != null) {			
			println("=== Inferring Method " + Utils.formatMethod(m) + " ===");
			localvars = m.code().localVariables();
			TypeMap map = methodEntry(m.isStatic());
			super.start(m.code(), m.code().entry(), map);
		}
	}
	
	private TypeMap methodEntry(boolean isStatic) {
		TypeMap map = new TypeMap();
		
		List<LocalVarDef> localvars = meth.code().localVariables();
		
		for(LocalVarDef lvd : localvars) {
			if(lvd.type() != null) {
				map.assign(lvd.name(), lvd.type());
			}
		}
		map.addVar("this", clazz.type());
		map.addVar("super", clazz.superClass());

		return map;
	}
	
	public Map<Point, TypeMap> environments(Method m) {
		return indxedEnvs.get(m);
	}
	
	public void setEnvironmentAt(Point p, TypeMap m, Method md) {
		indxedEnvs.get(md).put(p, m);
	}
	
	public void transfer(Point p, TypeMap m) {
		try {
			if(p.statement() == null) {
				storeEnv(p, (TypeMap) m.clone());
				return;
			}
			print(p.statement() + ": " + m.toShortString() + " --> ");
			if(p.statement() instanceof Return) {
				inferExpr(((Return) p.statement()).expr, m);
			}
			else if(p.statement() instanceof Assign) {
				inferAssign(p, (Assign) p.statement(), m);
			}
			else if(p.statement() instanceof Invoke) {
				inferExpr((Invoke) p.statement(), m);
			}
			storeEnv(p, (TypeMap) m.clone());
			println(m.toShortString() + "\n");
		} catch(ClassNotFoundException cne) {
			throw new InternalException(cne.getMessage(), p, meth, clazz);
		} catch(FieldNotFoundException fne) {
			throw new InternalException(fne.getMessage(), p, meth, clazz);
		} catch(MethodNotFoundException mne) {
			throw new InternalException(mne.getMessage(), p, meth, clazz);
		}
	}
	
	/**
	 * Stores a TypeMap in the correct HashMap
	 * 
	 * @param p Point that the TypeMap belongs too
	 * @param m TypeMap to be stored
	 */
	private void storeEnv(Point p, TypeMap m) {
		HashMap<Point, TypeMap> map = indxedEnvs.get(meth);
		if(map == null) {
			map = new HashMap<Point, TypeMap>();
			indxedEnvs.put(meth, map);
		}
		map.put(p, m);
	}

	public void transfer(Point p, Expr expr, TypeMap m) {
		
		try {
			print(expr + ": " + m.toShortString() + " --> ");
			if(expr instanceof BinOp) {
				BinOp bop = (BinOp) expr;
				Type lhs = inferExpr(bop.lhs, m);
				Type rhs = inferExpr(bop.rhs, m);
				
//				if(bop.op == BinOp.EQ) {
//					m.equality(lhs, rhs);
//				}
//				else if(bop.op == BinOp.NEQ) {
//					m.inEquality(lhs, rhs);
//				}
			}
			else if(expr instanceof InstanceOf) {
				
				InstanceOf iof = (InstanceOf) expr;
				Type lhs = inferExpr(iof.lhs, m);
				m.instanceOf(lhs, iof.rhs);
			} else if(expr instanceof Invoke) {
				Invoke inv = (Invoke) expr;
				if(inv.target.toString().contains("$iterator")) {
					Expr e = inferForEachCond(expr, m);
					toChngE.put(expr, e);
				}
			} else if(expr instanceof UnOp) {
				UnOp unop = (UnOp) expr;
				if(unop.expr instanceof Invoke) {
					Invoke inv = (Invoke) unop.expr;
					if(inv.target.toString().contains("$iterator")) {
						Expr e = inferForEachCond(inv, m);
						toChngE.put(expr, FlowGraph.invertBoolean(e));
					}
				}
			}
			println(m.toShortString() + "\n");
		} catch(ClassNotFoundException cne) {
			throw new InternalException(cne.getMessage(), p, meth, clazz);
		} catch(FieldNotFoundException fne) {
			throw new InternalException(fne.getMessage(), p, meth, clazz);
		} catch(MethodNotFoundException mne) {
			throw new InternalException(mne.getMessage(), p, meth, clazz);
		}
		
	}
	
	/**
	 * Infers the type of an expression
	 * 
	 * @param expr Expression to infer the type of
	 * @param m TypeMap to use in the type inference
	 * @return Type of expr if it is not-null, null otherwise.
	 * @throws ClassNotFoundException
	 * @throws FieldNotFoundException
	 * @throws MethodNotFoundException
	 */
	private Type inferExpr(Expr expr, TypeMap m) 
	throws ClassNotFoundException, FieldNotFoundException, MethodNotFoundException {
		return (expr == null) ? null : Types.typeOf(expr, m.environment());
	}
	
	/**
	 * Handles the updating of the TypeMap based on an assignment expression.  Also handles
	 * the updating of the infered types of fields when they are assigned values. 
	 * 
	 * @param p
	 * @param asgn
	 * @param m
	 * @throws ClassNotFoundException
	 * @throws FieldNotFoundException
	 * @throws MethodNotFoundException
	 */
	private void inferAssign(Point p, Assign asgn, TypeMap m) 
	throws ClassNotFoundException, FieldNotFoundException, MethodNotFoundException {
		LVal lhs = asgn.lhs;
		
		if(lhs instanceof LocalVar) {
			LocalVar lv = (LocalVar) lhs;
			
			if(lv.name.contains("$iterator")) {
				inferForEachInit(p, asgn, m);
				return;
			}
			
			if(asgn.rhs instanceof LocalVar) {
				LocalVar lrhs = (LocalVar) asgn.rhs;
				if(lrhs.name.equals("$")) {
					return;
				}
			} else if(asgn.rhs instanceof Invoke) {
				Invoke inv = (Invoke) asgn.rhs;
				if(inv.target.toString().contains("$iterator")) {
					inferForEachAssign(p, asgn, m);
					return;
				}
			}
			
			String name = lv.name;
			
			Type rhs = inferExpr(asgn.rhs, m);
			
			if(localVarType(name) instanceof Type.Void ||
					localVarType(name) instanceof Type.Any) {
				assign(name, rhs);
				m.assign(name, rhs);
			}

		}
		else if(lhs instanceof Deref) {
			if(!isConstructor) {
				return;
			}
			Type rhs = inferExpr(asgn.rhs, m);
			
			Deref deref = (Deref) lhs;
			Expr dlhs = deref.target;
			if(dlhs instanceof LocalVar) {
				LocalVar lv = (LocalVar) dlhs;
				if(lv.name.equals("this")) {
					// A potentially local field is being assigned to.
					Field f = findField(deref.name);
					if(f != null) {
						updateFieldInfer(fieldTypes, f, rhs);
					}
				}
			}
		}
		else {
			// We dont care about array index stuff.
		}
	}
	
	private void inferForEachInit(Point p, Assign asgn, TypeMap m) 
	throws ClassNotFoundException, FieldNotFoundException, MethodNotFoundException {
		Type rhs = inferExpr(asgn.rhs, m);
		LocalVar lv = (LocalVar) asgn.lhs;
		
		Assign nAsgn = null;
		
		if(rhs instanceof Type.Array) {
			m.assign(lv.name, Type.intType());
			nAsgn = new Assign(lv, new FlowGraph.IntVal(0));
		} else if(rhs instanceof Type.Reference) {
			Type.Reference ref = (Type.Reference) rhs;
			
			Invoke nInvoke = new Invoke(asgn.rhs, "iterator", new ArrayList<Expr>());
			Type it = inferExpr(nInvoke, m);
			m.assign(lv.name, it);
			
			nAsgn = new Assign(lv, nInvoke);
		}
		feIterators.put(lv.name, asgn.rhs);
		
		toChng.put(asgn, nAsgn);
	}
	
	private void inferForEachAssign(Point p, Assign asgn, TypeMap m) 
	throws ClassNotFoundException, FieldNotFoundException, MethodNotFoundException {
		Invoke inv = (Invoke) asgn.rhs;
		LocalVar lv = (LocalVar) asgn.lhs;
		LocalVar invT = (LocalVar) inv.target;
		
		Expr e = feIterators.get(invT.name);
		Type t = inferExpr(e, m);
		
		Assign nAsgn = null;
		
		if(t instanceof Type.Array) {
			assign(lv.name, ((Type.Array)t).elementType());
			m.assign(lv.name, ((Type.Array)t).elementType());
			nAsgn = new Assign(lv, new ArrayIndex(e, new UnOp(UnOp.POSTINC, inv.target, Type.intType())));
		} else if(t instanceof Type.Reference) {
			Invoke nInv = new Invoke(inv.target, "next", new ArrayList<Expr>());
			Type nT = inferExpr(nInv, m);
			assign(lv.name, nT);
			m.assign(lv.name, nT);
			
			nAsgn = new Assign(lv, nInv);
		}
		
		toChng.put(asgn, nAsgn);
	}
	
	private Expr inferForEachCond(Expr e, TypeMap m) 
	throws ClassNotFoundException, FieldNotFoundException, MethodNotFoundException {
		Invoke inv = (Invoke) e;
		LocalVar lv = (LocalVar) inv.target;
		Expr ex = feIterators.get(lv.name);
		Type t = inferExpr(ex, m);
		Expr nExpr = null;
		if(t instanceof Type.Array) {
			nExpr = new BinOp(BinOp.LT, lv, new Deref(ex, "length", Type.intType()), Type.booleanType());
		} else {
			nExpr = new Invoke(lv, "hasNext", new ArrayList<Expr>(), Type.booleanType());
		}
		return nExpr;
	}
	
	private void assign(String s, Type t) {
		Type oT = biggestTypes.get(s);
		if(oT != null) {
			if (jkit.bytecode.Types.slotSize(t) > jkit.bytecode.Types
					.slotSize(oT)) {
				biggestTypes.put(s, t);
			}
		} else {
			biggestTypes.put(s, t);
		}
	}
	
	private void updateLocalVarDefs() {
		for(LocalVarDef lvd : localvars) {
			Type t = biggestTypes.get(lvd.name());
			if(t != null) {
				lvd.setType(t);
			}
		}
	}
	
	private Type localVarType(String s) {
		for(LocalVarDef lvd : localvars) {
			if(lvd.name().equals(s)) {
				return lvd.type();
			}
		}
		return null;
	}
	
	private Field findField(String s) {
		for(Field f : clazz.fields()) {
			if(f.name().equals(s)) {
				return f;
			}
		}
		return null;
	}
	
	private void updateFieldInfer(HashMap<Field, Type> map, Field f, Type t) {
		Type tp = map.get(f);
		if(tp == null || tp instanceof Type.Void) {
			map.put(f, t);
		}
		else {
			map.put(f, tp.union(t));
		}
	}
	
	private void checkFieldInfer(HashMap<Field, Type> resFieldTypes) {
		for(Map.Entry<Field, Type> e : fieldTypes.entrySet()) {
			updateFieldInfer(resFieldTypes, e.getKey(), e.getValue());
		}
	}
	
	private void processConstFields() {
		for(Field f : clazz.fields()) {
			if(f.type() instanceof Type.Void && f.constantValue() != null) {
				Object o = f.constantValue();
				if(o instanceof Integer) {
					updateFieldInfer(fieldTypes, f, Type.intType());
				} else if(o instanceof Long) {
					updateFieldInfer(fieldTypes, f, Type.longType());
				} else if(o instanceof String) {
					Pair<String, Type[]>[] classes = new Pair[1];
					classes[0] = new Pair<String, Type[]>("String", new Type[0]);
					updateFieldInfer(fieldTypes, f, Type.referenceType("java.lang", classes));
				}
			}
		}
	}
	
	private void insertFieldTypes(HashMap<Field, Type> resFieldTypes) {
		for(Field f : clazz.fields()) {
			if(f.type() instanceof Type.Void) {
				Type t = resFieldTypes.get(f);
				if(t != null) {
					f.type(t);
					// Update Field type
				}
			}
		}
	}

	public String description() {
		return "Mocha local variable and field type inference";
	}
	
	private void print(String s) {
		if(verbose)
			 System.out.print(s);
	}
	
	private void println(String s) {
		if(verbose)
			System.out.println(s);
	}
	
	public IdentityHashMap<Stmt, Stmt> stmtsToChange() { return toChng; }
	public IdentityHashMap<Expr, Expr> exprsToChange() { return toChngE; }
	
	public static class InferForEachTrans extends Translator {
		
		private Inferrer inf;
		
		public InferForEachTrans(Inferrer i) {
			inf = i;
		}
		
		protected void translate(Method method, Clazz owner) {
			IdentityHashMap<Stmt, Stmt> stmts = inf.stmtsToChange();
			IdentityHashMap<Expr, Expr> exprs = inf.exprsToChange();
			
			FlowGraph cfg = method.code();
			for(Point p : cfg.domain()) {
				if(p.statement() != null) {
					Stmt nS = stmts.get(p.statement());
					if(nS != null) {
						p.setStatement(nS);
					}
				}
			}
			
			for(Triple<Point, Point, Expr> t : cfg) {
				if(t.third() != null) {
					Expr nE = exprs.get(t.third());
					if(nE != null) {
						t.third = nE;
					}
				}
			}
		}
		
		public String description() {
			return "Translates ForEach Loops using Inference";
		}
		
	}
	
}
