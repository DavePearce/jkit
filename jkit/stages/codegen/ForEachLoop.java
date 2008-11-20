package jkit.stages.codegen;

import java.util.*;

import jkit.core.*;
import jkit.core.FlowGraph.*;
import jkit.stages.*;
import jkit.util.*;

public class ForEachLoop extends Translator {
	// First item in pair gives collection type; second gives iterator type
	private HashMap<String,Pair<Type,Type>> iterators;
	
	public String description() {
		return "Breaks down Java 1.5 for-each loops.";
	}
	
	protected void translate(Method method,Clazz owner) {
		FlowGraph cfg = method.code();
		
		iterators = new HashMap<String,Pair<Type,Type>>();
		
		// First, go through each statement looking for direct assignments to
        // iterator variables.
		for(FlowGraph.Point p : cfg.domain()) {						
			Stmt stmt = p.statement();			
			if(stmt != null&& stmt instanceof Assign) {
				Assign a = (Assign) stmt;
				if(a.lhs instanceof LocalVar) {
					LocalVar lv = (LocalVar) a.lhs;					
					if(lv.name.contains("$iterator") && a.lhs.type instanceof Type.Any) {
						// need to infer the type of this iterator variable.
						// first, update method declaration
						Pair<Type,Type> info = elementIterType(a.rhs.type,p,method,owner);
						// Finally, update the variables type
						List<LocalVarDef> lvs = method.code().localVariables();
						for(LocalVarDef lvd : lvs) {						
							if(lvd.name().equals(lv.name)) {							
								lvd.setType(info.second());
								break;
							}
						}						
						// now, save the necessary information
						iterators.put(lv.name, info);
						// UPDATE RHS HERE						
						if(info.second() instanceof Type.Int) {
							// Array src
							p.setStatement(new Assign(a.lhs,new IntVal(0)));
							// Add assignment to size and src variables
							Point ap = cfg.split(p);
							Point sp = cfg.split(ap);
							LocalVar arrayVar = new LocalVar(lv.name + "$array",a.rhs.type);
							LocalVar sizeVar = new LocalVar(lv.name + "$size",Type.intType());
							ap.setStatement(new Assign(arrayVar,a.rhs));
							sp.setStatement(new Assign(sizeVar,new Deref(arrayVar,"length",Type.intType())));
							// Finally, add new variables to locals list
							lvs.add(new LocalVarDef(lv.name + "$array",a.rhs.type,0,false));
							lvs.add(new LocalVarDef(lv.name + "$size",Type.intType(),0,false));							
						} else {
							// Collection src
							p.setStatement(new Assign(a.lhs, new Invoke(a.rhs,
									"iterator", new ArrayList<Expr>(), info
											.second())));
						}
						
					}
				}
			}
		}
		
		// now, continue with translation ...
		
		super.translate(method,owner);		
	}
	
	protected Stmt translate(Assign a,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		// this method is needed for Assignments to the Java 1.5 loop variables.  For example:
		//
		// float[] fs = ...;
		// for(double d : fs) { ... }
		// 
		// here, we need to have an implicit conversion from double to float.
		LVal lhs = (LVal) translate(a.lhs,environment,point,method,owner);
		Expr rhs = translate(a.rhs, environment, point, method, owner);		
		return new Assign(lhs,Typing.implicitCast(rhs,lhs.type));
	}
	
	protected Expr translate(LocalVar var, Map<String,Type> environment,Point point, Method method, Clazz owner) {
		if(var.name.endsWith("$iterator")) {
			return new LocalVar(var.name,iterators.get(var.name).second());
		} else {
			return var;
		}
	}
	
	protected Expr translate(Invoke ivk, Map<String,Type> environment,Point point, Method method, Clazz owner) {
		if(ivk.target instanceof LocalVar && ivk.type instanceof Type.Any) {
			LocalVar target = (LocalVar) ivk.target;
			if(target.name.contains("$iterator")) {				
				Pair<Type,Type> info = iterators.get(target.name);
				Type elemT = info.first();
				Type iterT = info.second();
				LocalVar var = new LocalVar(target.name, iterT);
				if(iterT instanceof Type.Int) {
					// array src
					if(ivk.name.equals("hasNext")) {	
						return new BinOp(BinOp.LT, var, new LocalVar(
								target.name + "$size", Type.intType()), Type
								.booleanType());
					} else if(ivk.name.equals("next")) {
						Type arrT = environment.get(target.name + "$array");
						// FIXME: this line is a problem for eliminating PRE/POSTINC
						return new ArrayIndex(new LocalVar(target.name
								+ "$array", arrT), new UnOp(
								UnOp.POSTINC, var, Type.intType()), elemT);						
					}
				} else {
					// Collection src 
					if(ivk.name.equals("hasNext")) {	
						return new Invoke(var, ivk.name, new ArrayList<Expr>(),
								Type.booleanType());
					} else if(ivk.name.equals("next")) {					
						return new Invoke(var, ivk.name, new ArrayList<Expr>(),
								elemT);
					}
				}
			}
		} 
		return ivk;
	}
	
	protected Expr translate(UnOp uop, Map<String,Type> environment,Point point, Method method, Clazz owner) {
		Expr expr = translate(uop.expr,environment,point,method,owner);
		if(uop.type instanceof Type.Any) {
			return new UnOp(uop.op,expr,expr.type);
		} else {
			return new UnOp(uop.op,expr,uop.type);
		}
	}

	/**
	 * This method returns the iterator and element type, given the src
	 * collection type.
	 * 
	 * @param srcT
	 * @return pair of (element type,iterator type)
	 */
	protected Pair<Type,Type> elementIterType(Type srcT, Point point, Method method, Clazz owner) {
		if(srcT instanceof Type.Array) {
			Type.Array a = (Type.Array) srcT;			
			// this indicates a for each over an array
			return new Pair<Type, Type>(a.elementType(),Type.intType());
		} else if(srcT instanceof Type.Reference) {
			// this indicates a for each over a Collection							
			try {
			Triple<Clazz, Method, Type.Function> minfo = ClassTable
						.resolveMethod(
								(Type.Reference) srcT,
								"iterator",
								new ArrayList<Type>());
			Type.Reference iterT = (Type.Reference) minfo.third().returnType();			
			return new Pair<Type, Type>(iterT.classes()[0].second()[0],iterT);			
			} catch(ClassNotFoundException e) {
				throw new InternalException(e.getMessage(),point,method,owner);
			} catch(MethodNotFoundException e) {
				throw new InternalException(e.getMessage(),point,method,owner);
			}			
		} else {
			return null; // impossible
		}	
	}
}
