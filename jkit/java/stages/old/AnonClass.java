package jkit.java.stages.old;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import mocha.core.Types;

import jkit.compiler.ClassTable;
import jkit.compiler.FieldNotFoundException;
import jkit.compiler.InternalException;
import jkit.compiler.MethodNotFoundException;
import jkit.compiler.Translator;
import jkit.jkil.Clazz;
import jkit.jkil.Field;
import jkit.jkil.FlowGraph;
import jkit.jkil.Method;
import jkit.jkil.Type;
import jkit.jkil.FlowGraph.*;
import jkit.util.*;

/**
 * This stage does two things (and probably should be split into two separate
 * stages):
 * <ol>
 * <li>It looks for situations where an inner class is trying to invoke a
 * method, or access a field on its enclosing class, <i>but doesn't have access
 * to do this</i>. In particular, this happens when the method or field in
 * question is declared <code>private</code>. In this case, a special public
 * accessor method is added to the enclosing class which, when invoked, either
 * returns the field or invokes the method in question.</li>
 * <li>If an anonymous inner class refers to local variables which come from
 * its enclosing method, then these must be passed in as parameters to the
 * anonymous inner class' constructor and then assigned to fields. Thus, they
 * can now be accessed from methods inside the anonymous inner class.
 * </ol>
 * 
 * @author chrismale, djp
 * 
 */
// this class is a bit of mess :(
public class AnonClass extends Translator {
	
	private static int accessNum = 0;
	
	public void apply(Clazz owner) {		
		ArrayList<Method> methClone = new ArrayList<Method>(owner.methods());
		for(Method m : methClone) {
			if(m.code() != null) {
				translate(m, owner);
			}
		}
		
		// Now, if this is an inner class we need to identify any enclosing
		// class accesses as well.
		for(Triple<Type.Reference,Integer,Boolean> info : owner.inners()) {						
			try {
				Clazz innerClass = ClassTable.findClass(info.first());
				AnonClassInfoCollector collector = new AnonClassInfoCollector(
						innerClass, owner, new HashMap());
				collector.apply(innerClass);
			} catch(ClassNotFoundException ce) {
				throw new InternalException(ce.getMessage(), null, null, owner);
			} 	
		}
	}
	
	protected Expr translate(New ne,Map<String,Type> environment,Point point,Method method,Clazz owner) {
		ArrayList<Expr> params = new ArrayList<Expr>();
		for(Expr e : ne.parameters) {
			Expr n = translate(e,environment,point,method,owner);
			params.add(n);			
		}
		
		Type t = ne.type;
		if(t instanceof Type.Reference) {
			try {
				Clazz anonClass = ClassTable.findClass((Type.Reference) t);
				if(anonClass.isAnonymous()) {
					AnonClassInfoCollector collector = new AnonClassInfoCollector(anonClass, owner, environment);
					try {
						List<LocalVar> used = collector.collect(anonClass, owner, ne.parameters, method.isStatic());
						params.addAll(used);
						if(!method.isStatic()) {
							params.add(0, new LocalVar("this"));
						}
					} catch(ClassNotFoundException ce) {
						throw new InternalException(ce.getMessage(), point, method, owner);
					} catch(FieldNotFoundException fe) {
						throw new InternalException(fe.getMessage(), point, method, owner);
					} catch(MethodNotFoundException me) {
						throw new InternalException(me.getMessage(), point, method, owner);
					}
				}
			} catch(ClassNotFoundException ce) {
				
			}
		}
		
		return new New(ne.type,params);
	}

	public String description() {
		return "Creates anonymous class code";
	}
	
	private class AnonClassInfoCollector extends Translator {
		private ArrayList<LocalVar> used = new ArrayList<LocalVar>();
		private HashMap<String, String> lvFieldMap = new HashMap<String, String>();
		private Type.Reference ownerType;
		private Map<String, Type> ownerEnv;
		private Clazz ownerClazz;
		
		AnonClassInfoCollector(Clazz c, Clazz owner, Map<String, Type> environment) {
			ownerType = owner.type();
			ownerEnv = environment;
			ownerClazz = owner;			
		}
		
		public List<LocalVar> collect(Clazz c, Clazz owner,
				ArrayList<Expr> params, boolean isStatic)
				throws ClassNotFoundException, MethodNotFoundException,
				FieldNotFoundException {
			// Store info about the class and method this anon class was created in
			
			if(!isStatic) {
				// Create a field for this$0, which is a reference to the outer class
				Field outerThis = new Field(Modifier.FINAL, owner.type(), "this$0");
				c.fields().add(outerThis);
			}
			
			// Begin the translation of the class
			super.apply(c);
			
			// Translation complete, begin to fill out the rest of the class
			
			int thisInc = (isStatic) ? 0 : 1;
			
			// Create parameters for the anon class's constructor
			Type[] paramTypes = new Type[used.size() + params.size() + thisInc];
			
			// Add the outer class referencer
			if(!isStatic) {
				paramTypes[0] = owner.type();
			}
			
			// Add in any parameters to the superclass's constructor
			for(int i = 0; i < params.size(); i++) {
				paramTypes[i + thisInc] = Types.typeOf(params.get(i), ownerEnv);
			}
			
			LocalVar[] usedVars = used.toArray(new LocalVar[0]);
			// Any local variables belonging to the outer method must be passed
			// in as parameters
			for(int i = 0; i < usedVars.length; i++) {
				paramTypes[i+params.size() + thisInc] = ownerEnv.get(usedVars[i].name);
			}

			// Create constructor's type
			Type.Function cType = Type.functionType(Type.voidType(), paramTypes);
			
			// Create local variables for constructor
			ArrayList<LocalVarDef> localVarDefs = new ArrayList<LocalVarDef>();
			
			if(!isStatic) {
				// Add owner class this parameter
				localVarDefs.add(new LocalVarDef("this0", owner.type(), 0, true));
			}
			
			// Store the parameters which are gonna be in the super call
			ArrayList<Expr> superParams = new ArrayList<Expr>();
			// Add the super parameters as local variables
			for(int i = 0; i < params.size(); i++) {
				localVarDefs.add(new LocalVarDef("param$" + i, Types.typeOf(params.get(i), ownerEnv), 0, true));
				superParams.add(new LocalVar("param$" + i));
			}
			
			// Add local variable parameters
			for(LocalVar lv : used) {
				localVarDefs.add(new LocalVarDef(lv.name + "0", ownerEnv.get(lv.name), 0, true));
			}
			
			// Create points which assign each parameter to their field
			ArrayList<Point> lvdAssigns = new ArrayList<Point>();
			if(!isStatic) {
				lvdAssigns.add(assignPoint("this$0", "this0", c.type()));
			}
			for(LocalVar lv : used) {
				Point p = assignPoint("val$" + lv.name, lv.name + "0", c.type());
				lvdAssigns.add(p);
			}
			Clazz superC = ClassTable.findClass(c.superClass());
			if(!superC.isInterface()) {
				// Create super constructor call
				lvdAssigns.add(new Point(new Invoke(
						new Cast(c.superClass(),
								new LocalVar("this")), c.superClass().name(),
								superParams)));
			}
			
			// Create constructor's cfg
			FlowGraph cfg = new FlowGraph(localVarDefs, lvdAssigns.get(0));
			
			// Connect assignments to CFG
			Point lPoint = lvdAssigns.get(0);
			for(int i = 1; i < lvdAssigns.size(); i++) {
				Point p = lvdAssigns.get(i);
				cfg.add(new Triple<Point, Point, Expr>(lPoint, p, null));
				lPoint = p;
			}
			
			// Create return call
			cfg.add(new Triple<Point, Point, Expr>(lPoint,new Point(new Return(null)), null));
			
			// Finally create constructor
			Pair<String,Type[]>[] c_type_classes = c.type().classes();			
			Method cons = new Method(Modifier.PUBLIC, cType,
					c_type_classes[c_type_classes.length - 1].first(),
					new ArrayList<Type.Reference>(), null, cfg);
			c.methods().add(cons);						
			// Return list of local variable's used
			return used;
		}
		
		private Point assignPoint(String fName, String lName, Type.Reference owner) {
			Assign asgn = new Assign(new Deref(new LocalVar("this", owner), fName), new LocalVar(lName));
			return new Point(asgn);
		}
		
		protected Expr translate(LocalVar var, Map<String, Type> environment,
				Point point, Method method, Clazz owner) {
			// Look for local variable to see if it was actually
			// declared in this method, or whether it belongs to the
			// enclosing method
			if(var.name.equals("$") || var.name.equals("this")) {
				return var;
			}
			List<LocalVarDef> lvds = method.code().localVariables();
			for(LocalVarDef lvd : lvds) {
				if(lvd.name().equals(var.name)) {
					// Found it, so we declared it
					return var;
				}
			}
						
			// See if a field for the local variable has been created before
			String fName = lvFieldMap.get(var.name);
			if(fName == null) {
				// No field exists, so create it				
				Type t = ownerEnv.get(var.name);
				fName = "val$" + var.name;				
				Field f = new Field(Modifier.FINAL, t, fName);
				owner.fields().add(f);
				lvFieldMap.put(var.name,fName);
				used.add(var); 
			}
			// Return an expression which reads the field
			Deref deref = new Deref(new LocalVar("this", owner.type()), fName);
			return deref;
		}
		
		protected Stmt translate(Assign a, Map<String,Type> environment,Point point,Method method,Clazz owner) {
			Expr rhs = translate(a.rhs,environment,point,method,owner);
			
			if(a.lhs instanceof Deref) {
				Deref targ = (Deref) a.lhs;				
				boolean outerAccess = false;
				if(targ.target instanceof Deref) {					
					Deref nDeref = (Deref) targ.target;					
					if(nDeref.target instanceof LocalVar) {
						LocalVar lv = (LocalVar) nDeref.target;
						if(lv.name.equals("this") && nDeref.name.equals("this$0")) {
							outerAccess = true;
						}
					} 
				} else if(targ.target instanceof ClassAccess) {
					ClassAccess ca = (ClassAccess) targ.target;
					if(ca.clazz.equals(ownerType)) {				
						outerAccess = true;
					}						
				} 

				if(outerAccess) {
					try {
						Triple<Clazz, Field, Type> finfo = ClassTable.resolveField(ownerType, targ.name);
						Type t = finfo.third();					
						boolean staticAccess = finfo.second().isStatic();														

						Method m = genAccessFieldWrite(targ.name, t, staticAccess);

						ownerClazz.methods().add(m);

						ArrayList<Expr> params = new ArrayList<Expr>();
						if(!staticAccess) {
							params.add(new Deref(new LocalVar("this"), "this$0", ownerType));							
						}
						params.add(rhs);
						return new Invoke(new ClassAccess(ownerType), m.name(), params, t);
					} catch(ClassNotFoundException cne) {
						throw new InternalException(cne.getMessage(), point, method, owner);
					} catch(FieldNotFoundException fne) {
						throw new InternalException(fne.getMessage(), point, method, owner);
					} 
				}
			}
			
			LVal lhs = (LVal) translate(a.lhs,environment,point,method,owner);

			return new Assign(lhs,rhs); 
		}
		
		private Method genAccessFieldWrite(String fName, Type t, boolean isStatic) {
			Type[] paramTypes;
			
			ArrayList<LocalVarDef> localVars = new ArrayList<LocalVarDef>();
			Point entry;
			if(!isStatic) {
				paramTypes = new Type[2];
				paramTypes[0] = ownerType;
				paramTypes[1] = t;
				localVars.add(new LocalVarDef("owner", ownerType, 0, true));
				localVars.add(new LocalVarDef("param$0", t, 0, true));
				entry = new Point(new Assign(new Deref(new LocalVar("owner"), fName), new LocalVar("param$0")));
			} else {
				paramTypes = new Type[1];
				paramTypes[0] = t;
				localVars.add(new LocalVarDef("param$0", t, 0, true));
				entry = new Point(new Assign(new Deref(new ClassAccess(ownerType), fName), new LocalVar("param$0")));
			}
			
			Type.Function methType = Type.functionType(Type.voidType(), paramTypes);						
			
			FlowGraph cfg = new FlowGraph(localVars, entry);
			
			// add a return from the method.
			cfg.add(new Triple<Point, Point, Expr>(entry, new Point(new Return(
					null)), null));
			
			Method meth = new Method(Modifier.PUBLIC | Modifier.STATIC,
					methType, genAccessName(), new ArrayList<Type.Reference>(),
					null, cfg);
						
			return meth;
		}
		
		protected Expr translate(Deref deref,Map<String,Type> environment,Point point, Method method, Clazz owner) {			
			boolean outerAccess = false;
			
			if(deref.target instanceof Deref) {				
				Deref targ = (Deref) deref.target;		
				if(targ.target instanceof LocalVar) {					
					LocalVar lv = (LocalVar) targ.target;
					outerAccess = lv.name.equals("this") && targ.name.equals("this$0");
				}
			} else if(deref.target instanceof ClassAccess) {
				ClassAccess ca = (ClassAccess) deref.target;
				if(ca.clazz.equals(ownerType)) {				
					outerAccess = true;
				}
			}
									
			if(outerAccess) {
				// Okay we are accessing something via this.this$0
				// We need to generate an access method, add it to the
				// owner, and return a method invocation
				try {
					// First, resolve the field's type
					Triple<Clazz, Field, Type> finfo = ClassTable.resolveField(ownerType, deref.name);
					Type t = finfo.third();					
					boolean staticAccess = finfo.second().isStatic();
					// Second, generate access method
					Method m = genAccessField(deref.name, t, staticAccess);
					// Add to owner
					ownerClazz.methods().add(m);
					// Create method invocation
					ArrayList<Expr> params = new ArrayList<Expr>();
					if(!staticAccess) {
						params.add(new Deref(new LocalVar("this"), "this$0", ownerType));
					}
					Invoke inv = new Invoke(new ClassAccess(ownerType), m.name(), params, t);

					return inv;
				} catch(FieldNotFoundException fe) {
					throw new InternalException(fe.getMessage(), point, method, owner);
				} catch(ClassNotFoundException ce) {
					throw new InternalException(ce.getMessage(), point, method, owner);
				}

			}
			return super.translate(deref, environment, point, method, owner);
		}
		
		private Method genAccessField(String fName, Type fType, boolean isStatic) {
			Type[] paramTypes = new Type[isStatic ? 0 : 1];
			ArrayList<LocalVarDef> localVars = new ArrayList<LocalVarDef>();
			Point entry; // entry point to Accessor Method
			
			if(!isStatic) { 
				paramTypes[0] = ownerType; 				
				localVars.add(new LocalVarDef("owner", ownerType, 0, true));
				entry = new Point(new Return(new Deref(new LocalVar("owner"), fName, fType)));
				
			} else {
				entry = new Point(new Return(new Deref(new ClassAccess(ownerType),fName))); 
			}
			
			FlowGraph cfg = new FlowGraph(localVars, entry);
			Type.Function methType = Type.functionType(fType, paramTypes);
			
			
			Method meth = new Method(Modifier.PUBLIC | Modifier.STATIC,
					methType, genAccessName(), new ArrayList<Type.Reference>(),
					null, cfg);						
			
			return meth;
		}
		
		protected Expr translate(Invoke ivk, Map<String, Type> environment,
				Point point, Method method, Clazz owner) {
			ArrayList<Expr> params = new ArrayList<Expr>();	
			for(Expr e : ivk.parameters) {
				Expr n = translate(e,environment,point,method,owner);
				params.add(n);			
			}

			if(ivk.target instanceof Deref) {
				Deref targ = (Deref) ivk.target;
				
				// Horrible hack TODO Get rid of
				if (targ.target.toString().equals(ownerType.unqualifiedName())) {
					Expr nTarg = new Deref(new LocalVar("this"), "this$0");
					return this.translate(new Invoke(nTarg, ivk.name, params,
							ivk.type), environment, point, method, owner);
				}
				
				if(targ.target instanceof LocalVar) {
					LocalVar lv = (LocalVar) targ.target;
					if(lv.name.equals("this") && targ.name.equals("this$0")) {
						try {
							// First, figure out the method being invoked.
							ArrayList<Type> paramTypes = new ArrayList<Type>();
							for(int i = 0; i < params.size(); i++) {
								paramTypes.add(Types.typeOf(params.get(i), environment));
							}
							
							Triple<Clazz, Method, Type.Function> minfo = ClassTable
									.resolveMethod(ownerType, ivk.name,
											paramTypes);
							Type.Function t = minfo.third();					
							boolean staticAccess = minfo.second().isStatic();
							// Second, generate access method
							Method m = genAccessMethod(ivk.name, t, staticAccess);
							// Add to owner
							ownerClazz.methods().add(m);
							// now, create method invocation
							if(!staticAccess) {
								params.add(0,new Deref(new LocalVar("this"), "this$0", ownerType));
							}				
							
							Invoke inv = new Invoke(new ClassAccess(ownerType),
									m.name(), params, t.returnType(), ivk.polymorphic);
							// all done!
							return inv;
						} catch(ClassNotFoundException ce) {
							throw new InternalException(ce.getMessage(), point, method, owner);
						} catch (MethodNotFoundException me) {
							throw new InternalException(me.getMessage(), point, method, owner);
						} catch(FieldNotFoundException fe) {
							throw new InternalException(fe.getMessage(), point, method, owner);
						}
					}
				}
			}
			
			Expr target = translate(ivk.target,environment,point,method,owner);
			return new Invoke(target,ivk.name,params,ivk.type,ivk.polymorphic);							
		}
		
		private Method genAccessMethod(String name, Type.Function mtype,
				boolean isStatic) throws ClassNotFoundException,
				FieldNotFoundException, MethodNotFoundException {
									
			ArrayList<LocalVarDef> localvars = new ArrayList<LocalVarDef>();
			ArrayList<Expr> invokeArgs = new ArrayList<Expr>();
			Type[] paramTypes = mtype.parameterTypes();
			Type[] newParamTypes = new Type[paramTypes.length+1];			
			localvars.add(new LocalVarDef("owner", ownerType, 0, true));
			newParamTypes[0] = ownerType;
			
			for(int i = 0; i < paramTypes.length; i++) {								
				newParamTypes[i+1] = paramTypes[i];
				localvars.add(new LocalVarDef("param$" + i, paramTypes[i], Modifier.FINAL, true));
				invokeArgs.add(new LocalVar("param$" + i));
			}
			
			Type.Function newFuncType = Type.functionType(mtype.returnType(), newParamTypes);
			
			Invoke inv = new Invoke(new LocalVar("owner"), name, invokeArgs, mtype.returnType());
			
			Point entry = null;
			
			
			// Now, we need to check whether this method actually returns
			// anything or not.
			if(mtype.returnType() instanceof Type.Void) {
				entry = new Point(inv);				
			} else{
				entry = new Point(new Return(inv));				
			}
			
			FlowGraph cfg = new FlowGraph(localvars, entry);
			
			if(mtype.returnType() instanceof Type.Void) {
				cfg.add(new Triple<Point, Point, Expr>(entry, new Point(new Return(
					null)), null));
			}
			
			return new Method(Modifier.PUBLIC | Modifier.STATIC,
					newFuncType, genAccessName(),
					new ArrayList<Type.Reference>(), null, cfg);					
		}
		
		private String genAccessName() {
			// Create the name of an accessor method which matches 
			// the format used by javac
			String num = String.valueOf(accessNum++);
			for(int i = num.length(); i < 3; i++) {
				num = "0" + num;
			}
			
			return "access$" + num;
		}

		public String description() {
			return "Collects on what variables an anon class uses";
		}		
	}
}
