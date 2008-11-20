package jkit.stages.codegen;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import jkit.compiler.ClassTable;
import jkit.compiler.FieldNotFoundException;
import jkit.compiler.InternalException;
import jkit.compiler.MethodNotFoundException;
import jkit.jkil.Clazz;
import jkit.jkil.Field;
import jkit.jkil.FlowGraph;
import jkit.jkil.Method;
import jkit.jkil.Type;
import jkit.jkil.FlowGraph.*;
import jkit.stages.Translator;
import jkit.util.Pair;
import jkit.util.Triple;
import jkit.util.graph.Graph;

public class Typing extends Translator {
	public String description() {
		return "Ensure types used correctly within statements and expressions.  Make conversions explicit.";
	}
	
	public void apply(Clazz owner) {		
		for(Method m : owner.methods()) {			
			if(m.code() != null) {
				translate(m,owner);
			}			
		}		
	}
			
	protected Stmt translate(Assign a,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		LVal lhs = (LVal) translate(a.lhs,environment,point,method,owner);
		Expr rhs;
		
		if (a.rhs instanceof ArrayVal && a.rhs.type instanceof Type.Any) {
			// This is a special case for infering the type of array vals.
			// For example, consider this bit of code:
			//
			// Object[] x = {1,2,3};
			//
			// This is valid and should compile. However, the problem is that
			// the type inferred automatically for the rhs will be int[] which
			// is incompatible with the lhs. To get around this, we assign the
			// lhs type to the rhs type.
			rhs = translate(lhs.type, (ArrayVal) a.rhs, environment, point,
					method, owner);
		} else {
			rhs = translate(a.rhs, environment, point, method, owner);
		}
		
		if(lhs.type.supsetEqOf(rhs.type)) {
			// Attempt some slightly hacky type inference here!
			//
			// In order to follow javac, we also need to eliminate constant
			// subexpressions as well...
			if(lhs.type instanceof Type.Float && rhs instanceof FlowGraph.IntVal) {					
				return new Assign(lhs,new FlowGraph.FloatVal(((FlowGraph.IntVal)rhs).value));
			} else if(lhs.type instanceof Type.Double && rhs instanceof FlowGraph.IntVal) {					
				return new Assign(lhs,new FlowGraph.DoubleVal(((FlowGraph.IntVal)rhs).value));
			} else if (lhs.type instanceof Type.Any && lhs instanceof LocalVar
					&& !(((LocalVar) lhs).name.contains("$iterator"))) { 
				// This case is for dealing with temporary variables constructed
				// e.g. for dealing with side-effects.
				LocalVar lv = (LocalVar) lhs;
				method.code().localVariable(lv.name).setType(rhs.type);
				environment.put(lv.name,rhs.type);
				return new Assign(new LocalVar(lv.name,rhs.type),rhs);
			} else {
				// normal case
				return new Assign(lhs,implicitCast(rhs,lhs.type));
			}
		} else if ((lhs.type instanceof Type.Primitive || isWrapper(lhs.type))
				&& (rhs instanceof FlowGraph.IntVal
						|| (rhs instanceof FlowGraph.UnOp
								&& ((FlowGraph.UnOp)rhs).expr instanceof FlowGraph.IntVal
								&& ((FlowGraph.UnOp)rhs).op == FlowGraph.UnOp.NEG))) {
			// More slightly hacky type inference here!
			int val = 0;
			if (rhs instanceof FlowGraph.IntVal) {
				val = ((FlowGraph.IntVal)rhs).value;
			} else {
				val = -((FlowGraph.IntVal)((FlowGraph.UnOp)rhs).expr).value;
			}
			
			// first do primitive types
			if(lhs.type instanceof Type.Byte && val >= -128 && val <= 127) {
				return new Assign(lhs,new FlowGraph.ByteVal((byte) val));
			} else if(lhs.type instanceof Type.Char && val >= 0 && val <= 65535) {
				return new Assign(lhs,new FlowGraph.CharVal((char) val));
			} else if(lhs.type instanceof Type.Short && val >= -32768 && val <= 32768) {
				return new Assign(lhs,new FlowGraph.ShortVal((short) val));
			} 							
			
			// second do boxed types
			// first do primitive types
			if(isWrapper(lhs.type)) {
				Type.Reference ref = (Type.Reference) lhs.type;			
				String s = ref.classes()[0].first();				
				if(s.equals("Byte") && val >= -128 && val <= 127) {
					return new Assign(lhs,new New(Type.referenceType("java.lang","Byte"),new FlowGraph.ByteVal((byte) val)));					
				} else if(s.equals("Character") && val >= 0 && val <= 65535) {
					return new Assign(lhs,new New(Type.referenceType("java.lang","Character"),new FlowGraph.CharVal((char) val)));							
				} else if(s.equals("Short") && val >= -32768 && val <= 32768) {
					return new Assign(lhs,new New(Type.referenceType("java.lang","Short"),new FlowGraph.ShortVal((short) val)));
				}
			}
			
		} else if(lhs.type instanceof Type.Primitive && rhs instanceof FlowGraph.DoubleVal) {
			// More hacky type inference ...
			double val = ((FlowGraph.DoubleVal)rhs).value;
			if(lhs.type instanceof Type.Float) {
				return new Assign(lhs,new FlowGraph.FloatVal((float) val));
			}
		} 
		
		// default case (probably an error) ?
		return new Assign(lhs,rhs); 		
	}
			
	protected Stmt translate(Return r,Map<String,Type> environment,Point point, Method method, Clazz owner) {			
		if(r.expr == null) { return r; }
		Expr e = translate(r.expr,environment,point,method,owner);
		return new Return(implicitCast(e,method.type().returnType()));
	}	
		
	/**
	 * Type check ArrayIndex expression.
	 *  
	 * @param ai
	 * @param environment
	 * @param p
	 * @param m
	 * @param o
	 * @return
	 */
	protected Expr translate(ArrayIndex ai, Map<String, Type> environment,
			Point point, Method method, Clazz owner) {		
		Expr lhs = translate(ai.array,environment,point,method,owner);
		Expr rhs = translate(ai.idx,environment,point,method,owner);
		
		rhs = implicitCast(rhs,Type.intType()); 
		
		if(lhs.type instanceof Type.Array) {
			Type.Array at = (Type.Array) lhs.type;
			return new ArrayIndex(lhs,rhs,at.elementType());							
		} else {
			return new ArrayIndex(lhs,rhs,ai.type);
		}
	}
		
	protected Expr translate(BinOp bop,Map<String,Type> environment,Point point, Method method, Clazz owner) {		
		Expr lhs = translate(bop.lhs,environment,point,method,owner);
		Expr rhs = translate(bop.rhs,environment,point,method,owner);
						
		switch(bop.op) {
			case FlowGraph.BinOp.EQ:
			case FlowGraph.BinOp.NEQ:
			case FlowGraph.BinOp.LT:
			case FlowGraph.BinOp.LTEQ:
			case FlowGraph.BinOp.GT:
			case FlowGraph.BinOp.GTEQ:
			{				
				if((lhs.type instanceof Type.Primitive || isWrapper(lhs.type)) && (rhs.type instanceof Type.Primitive || isWrapper(rhs.type))) {
					Type rt = binaryNumericPromotion(lhs.type,rhs.type);					
					return new BinOp(bop.op,implicitCast(lhs,rt),implicitCast(rhs,rt),Type.booleanType());
				} else if(bop.op == FlowGraph.BinOp.EQ || bop.op == FlowGraph.BinOp.NEQ) {
					return new BinOp(bop.op,lhs,rhs,Type.booleanType());		
				}				
				break;
			}
			case FlowGraph.BinOp.ADD:
			case FlowGraph.BinOp.SUB:
			case FlowGraph.BinOp.MUL:
			case FlowGraph.BinOp.DIV:
			case FlowGraph.BinOp.MOD:
			{						
				if((lhs.type instanceof Type.Primitive || isWrapper(lhs.type)) && (rhs.type instanceof Type.Primitive || isWrapper(rhs.type))) {					
					Type rt = binaryNumericPromotion(lhs.type,rhs.type);					
					return new BinOp(bop.op,implicitCast(lhs,rt),implicitCast(rhs,rt),rt);
				} else if(bop.op == BinOp.ADD && (isString(lhs.type) || isString(rhs.type))) {
					return new BinOp(FlowGraph.BinOp.CONCAT,lhs,rhs,Type.referenceType("java.lang","String"));
				}
				break;
			}
			case FlowGraph.BinOp.SHL:
			case FlowGraph.BinOp.SHR:
			case FlowGraph.BinOp.USHR:
			{					
				if((lhs.type instanceof Type.Primitive || isWrapper(lhs.type)) && (rhs.type instanceof Type.Primitive || isWrapper(rhs.type))) {
					Type rt_left = unaryNumericPromotion(lhs.type);
					Type rt_right = Type.intType();				
					return new BinOp(bop.op,implicitCast(lhs,rt_left),implicitCast(rhs,rt_right),rt_left);
				}
				break;
			}
			case FlowGraph.BinOp.LAND:
			case FlowGraph.BinOp.LOR:
			{
				Type rt = Type.booleanType();
				return new BinOp(bop.op,implicitCast(lhs,rt),implicitCast(rhs,rt),Type.booleanType());
			}
			case FlowGraph.BinOp.AND:
			case FlowGraph.BinOp.OR:
			case FlowGraph.BinOp.XOR:
			{								
				if((lhs.type instanceof Type.Primitive || isWrapper(lhs.type)) && (rhs.type instanceof Type.Primitive || isWrapper(rhs.type))) {
					Type rt = binaryNumericPromotion(lhs.type,rhs.type);							
					return new BinOp(bop.op,implicitCast(lhs,rt),implicitCast(rhs,rt),rt);					
				} 
				break;
			}					
		}
		
		// default case --- do nothing.
		return new BinOp(bop.op,lhs,rhs,bop.type); 
	}	
		
	protected Expr translate(Deref deref,Map<String,Type> environment,Point point, Method method, Clazz owner) {	
		Expr target = translate(deref.target,environment,point,method,owner);
		
		if(target.type instanceof Type.Array) {			
			return new Deref(target,deref.name,Type.intType());			
		} 
		
		try {
			Triple<Clazz, Field, Type> finfo = ClassTable.resolveField((Type.Reference) target.type, deref.name);
			if(finfo.second().isStatic()) {
				return new Deref(new ClassAccess(finfo.first().type()),deref.name,finfo.third());
			} else {
				return new Deref(target,deref.name,finfo.third());
			}
		} catch (ClassNotFoundException e) {
			throw new InternalException(e.getMessage(),point,method,owner);
		} catch(FieldNotFoundException e) {
			throw new InternalException(e.getMessage(),point,method,owner);
		}		
	}	
	
	protected Expr translate(Invoke ivk,Map<String,Type> environment,Point point, Method method, Clazz owner) {					
		Expr target = translate(ivk.target,environment,point,method,owner);		
		ArrayList<Expr> params = new ArrayList<Expr>();
		ArrayList<Type> paramTypes = new ArrayList<Type>();
				
		for(Expr e : ivk.parameters) {
			Expr ne = translate(e,environment,point,method,owner);		
			params.add(ne);
			paramTypes.add(ne.type);
		}
				
		try {
			if (target.type instanceof Type.Reference
					|| target.type instanceof Type.Variable
					|| target.type instanceof Type.Wildcard) {					
				Triple<Clazz, Method, Type.Function> minfo;
				if(target.type instanceof Type.Reference) {
					minfo = ClassTable
						.resolveMethod((Type.Reference) target.type, ivk.name,
								paramTypes);
				} else {
					// TODO: add support for wildcards here
					minfo = ClassTable.resolveMethod(Type.referenceType(
							"java.lang", "Object"), ivk.name, paramTypes);
				}
				Type.Function ft = minfo.third();			
				// check for necessary casts
				Type[] ft_paramTypes = ft.parameterTypes();
				for(int i=0;i!=ft_paramTypes.length;++i) {
					Type t = ft_paramTypes[i]; 
					if ((i + 1) == ft_paramTypes.length
							&& minfo.second().isVariableArity()) {
						// This is a variable arity method, hence the last
						// element is an array. Therefore, we must wrap up each
						// of the remaining concrete parameters into an array
						// val and pass them in accordingly. The exception to
						// this rule is when the argument provided is an array
						// matching the variable arity list type so we need to
						// explicitly check for this.
						if (params.size() != ft_paramTypes.length ||
						   !ft_paramTypes[i].supsetEqOf(params.get(i).type)) {
							
							Type.Array at = (Type.Array) t;
							// Extract the remaining concrete parameters that need
							// to be packaged up into an array.
							List<Expr> varargs = params.subList(i, params.size());
							// Now, build new list whilst applying casts as
							// necessary (this is really needed for autoboxing and
							// implicit type conversions).
							ArrayList<Expr> nvarargs = new ArrayList<Expr>();
							for(Expr e : varargs) {
								nvarargs.add(implicitCast(e,at.elementType()));
							}
							Expr av = new ArrayVal(nvarargs,at);
							// Now, remove the remaining concrete parameters and
							// replace them with the array
							varargs.clear();
							params.add(av);
						}
					} else {
						// Apply casts as necessary (this is really needed for
						// autoboxing and implicit type conversions).
						params.set(i,implicitCast(params.get(i),t));
					}
				}				

				if(minfo.second().isStatic()) {
					return new Invoke(new ClassAccess(minfo.first().type()),ivk.name,params,minfo.third().returnType());
				} else {
					return new Invoke(target,ivk.name,params,minfo.third().returnType(),ivk.polymorphic);
				}

			} else if(target.type instanceof Type.Array) {
				if(ivk.name.equals("getClass") || ivk.name.equals("clone")) {
					Triple<Clazz, Method, Type.Function> minfo = ClassTable.resolveMethod(
							Type.referenceType("java.lang","Object"), ivk.name, paramTypes);
					return new Invoke(target,ivk.name,params,minfo.third().returnType());
				} 			
			} 
		} catch(ClassNotFoundException e) {
			throw new InternalException(e.getMessage(),point,method,owner);
		} catch(MethodNotFoundException e) {
			throw new InternalException(e.getMessage(),point,method,owner);
		}
		// continue with unknown type
		return new Invoke(target,ivk.name,params,ivk.type,ivk.polymorphic);		
	}
		
	protected Expr translate(LocalVar var,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		Type rhs_t = environment.get(var.name);
		if(rhs_t != null) {			
			return new LocalVar(var.name,rhs_t);
		} else {
			return var;	
		}
	}
	
	protected Expr translate(New ne,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		if(!(ne.type instanceof Type.Reference || ne.type instanceof Type.Array)) {
			throw new InternalException("Cannot construct type \"" + ne.type + "\"",point,method,owner);
		}
		
		ArrayList<Expr> params = new ArrayList<Expr>();
		ArrayList<Type> paramTypes = new ArrayList<Type>();
		for(Expr e : ne.parameters) {
			Expr n = translate(e,environment,point,method,owner);
			params.add(n);
			paramTypes.add(n.type);
		}		
		
		if(ne.type instanceof Type.Reference) {
			Type.Reference ner = (Type.Reference) ne.type;
			// if this is a class type, check that the constructor actually exists!
			try {				
				Triple<Clazz, Method, Type.Function> minfo = ClassTable.resolveMethod(
						(Type.Reference) ner, ner.name(), paramTypes);
				
				Type.Function ft = minfo.third();
				
				// check for necessary casts
				Type[] ft_paramTypes = ft.parameterTypes();
				for(int i=0;i!=ft_paramTypes.length;++i) {
					Type t = ft.parameterTypes()[i]; 
					
					if ((i + 1) == ft.parameterTypes().length
							&& minfo.second().isVariableArity()) {
						// This is a variable arity constructor, hence the last
						// element is an array. Therefore, we must wrap up each
						// of the remaining concrete parameters into an array
						// val and pass them in accordingly. The exception to
						// this rule is when the argument provided is an array
						// matching the variable arity list type so we need to
						// explicitly check for this.
						if (params.size() !=  ft_paramTypes.length ||
						   !ft_paramTypes[i].supsetEqOf(params.get(i).type)) {
							
							Type.Array at = (Type.Array) t;
							// Extract the remaining concrete parameters that need
							// to be packaged up into an array.
							List<Expr> varargs = params.subList(i, params.size());
							// Now, build new list whilst applying casts as
							// necessary (this is really needed for autoboxing and
							// implicit type conversions).
							ArrayList<Expr> nvarargs = new ArrayList<Expr>();
							for(Expr e : varargs) {
								nvarargs.add(implicitCast(e,at.elementType()));
							}
							Expr av = new ArrayVal(nvarargs,at);
							// Now, remove the remaining concrete parameters and
							// replace them with the array
							varargs.clear();
							params.add(av);
						}
					} else {										
						params.set(i,implicitCast(params.get(i),t));
					} 
				}
			} catch(ClassNotFoundException e) {
				throw new InternalException(e.getMessage(),point,method,owner);
			} catch(MethodNotFoundException e) {
				throw new InternalException(e.getMessage(),point,method,owner);
			}
		}
		return new New(ne.type,params);
	}	
	
	protected Expr translate(Cast cast, Map<String,Type> environment,Point point, Method method, Clazz owner) {
		Expr expr = translate(cast.expr,environment,point,method,owner);
			
		// Deal with auto-boxing and -unboxing here.
		if(cast.type instanceof Type.Primitive && isWrapper(expr.type)) {
			return implicitCast(expr,cast.type);
		} else if ((isWrapper(cast.type) || isJavaLangObject(cast.type)
				|| isJavaLangNumber(cast.type))
				&& expr.type instanceof Type.Primitive) {
			return implicitCast(expr,cast.type);
		}
	
		return new Cast(cast.type,expr);
	}
	
	protected Expr translate(UnOp uop,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		Expr expr = translate(uop.expr,environment,point,method,owner);
		
		switch(uop.op) {
		case FlowGraph.UnOp.NEG:
			if (expr.type instanceof Type.Byte || expr.type instanceof Type.Char
					|| expr.type instanceof Type.Short) {
				// This is a strange feature of javac. I don't really understand
				// why it's necessary.
				return new UnOp(uop.op,expr,Type.intType());
			} 
			break;		
		case FlowGraph.UnOp.INV:
			if (expr.type instanceof Type.Byte || expr.type instanceof Type.Char
					|| expr.type instanceof Type.Short) {
				// This is a strange feature of javac. I don't really understand
				// why it's necessary.
				return new UnOp(uop.op,expr,Type.intType());
			} 
			break;		
		}
		
		return new UnOp(uop.op,expr,expr.type);
	}
	
	protected Expr translate(TernOp top, Map<String, Type> environment, Point point, Method method, Clazz owner) {
		Expr cond = translate(top.cond, environment, point, method, owner);
		Expr trop = translate(top.toption, environment, point, method, owner);
		Expr fop = translate(top.foption, environment, point, method, owner);
		return new TernOp(cond, trop, fop, trop.type.union(fop.type));
	}
	
	
	protected Expr translate(ArrayVal av,
			Map<String, Type> environment, Point point, Method method,
			Clazz owner) {				
		return translate(av.type,av,environment,point,method,owner);
	}
	
	/**
	 * This is a specialisation of the translate(ArrayVal,...) method, where the
	 * result type of the ArrayVal is known. This is important since the type of
	 * an array initialiser is actually computed from the type of the
	 * variable/field/... that it's being assigned to. For example, in the
	 * following:
	 * <p>
	 * 
	 * <pre>
	 * Object[] x = { 1, 2, 3 };
	 * </pre>
	 * 
	 * </p>
	 * 
	 * the type of ArrayVal must be Object[], not int[] (which would otherwise
	 * be inferred).
	 * 
	 * @param type
	 * @param av
	 * @param environment
	 * @param point
	 * @param method
	 * @param owner
	 * @return
	 */
	protected Expr translate(Type type, ArrayVal av,
			Map<String, Type> environment, Point point, Method method,
			Clazz owner) {
		ArrayList<Expr> r = new ArrayList<Expr>();		
		Type elementType = type instanceof Type.Array ? ((Type.Array)type).elementType() : Type.anyType(); 	
			
		for(Expr e : av.values) {
			Expr ne = translate(e,environment,point,method,owner);
			r.add(implicitCast(ne,elementType));						
		}
				
		return new ArrayVal(r, Type.arrayType(elementType));
	}
	
	private static boolean isString(Type t) {
		return t instanceof Type.Reference
				&& ((Type.Reference) t).unqualifiedName().equals(
						"java.lang.String");
	}
	
	private static boolean isWrapper(Type t) {
		if(!(t instanceof Type.Reference)) {
			return false;
		}
		Type.Reference ref = (Type.Reference) t;
		if(ref.pkg().equals("java.lang") && ref.classes().length == 1) {
			String s = ref.classes()[0].first();
			if(s.equals("Byte") || s.equals("Character") || s.equals("Short") ||
				s.equals("Integer") || s.equals("Long")
					|| s.equals("Float") || s.equals("Double")
					|| s.equals("Boolean")) {
				return true;
			}
		}
		return false;
	}

	public static Type.Reference boxedType(Type.Primitive p) {
		if(p instanceof Type.Boolean) {
			return Type.referenceType("java.lang","Boolean");
		} else if(p instanceof Type.Byte) {
			return Type.referenceType("java.lang","Byte");
		} else if(p instanceof Type.Char) {
			return Type.referenceType("java.lang","Character");
		} else if(p instanceof Type.Short) {
			return Type.referenceType("java.lang","Short");
		} else if(p instanceof Type.Int) {
			return Type.referenceType("java.lang","Integer");
		} else if(p instanceof Type.Long) {
			return Type.referenceType("java.lang","Long");
		} else if(p instanceof Type.Float) {
			return Type.referenceType("java.lang","Float");
		} else {
			return Type.referenceType("java.lang","Double");
		}
	}
	
	private static Type.Primitive unboxedType(Type.Reference p) {
		assert isWrapper(p);
		String type = p.classes()[p.classes().length-1].first();
		
		if(type.equals("Boolean")) {
			return Type.booleanType();
		} else if(type.equals("Byte")) {
			return Type.byteType();
		} else if(type.equals("Character")) {
			return Type.charType();
		} else if(type.equals("Short")) {
			return Type.shortType();
		} else if(type.equals("Integer")) {
			return Type.intType();
		} else if(type.equals("Long")) {
			return Type.longType();
		} else if(type.equals("Float")) {
			return Type.floatType();
		} else if(type.equals("Double")) {
			return Type.doubleType();
		} else {
			throw new RuntimeException("Unknown boxed type \"" + p.toString()
					+ "\" encountered.");
		}
	}
	
	public static Expr implicitCast(Expr e, Type t) {
		// insert implicit casts for primitive types.
		if (!e.type.equals(t)
				&& (t instanceof Type.Primitive && e.type instanceof Type.Primitive)) {			
			e = new FlowGraph.Cast(t, e);
		} else if(t instanceof Type.Primitive && e.type instanceof Type.Reference) {
			Type.Reference r = (Type.Reference) e.type;
			if (r.pkg().equals("java.lang") && r.classes().length == 1) {
				String c = r.classes()[0].first();
				if (c.equals("Byte")) {
					return implicitCast(new Invoke(e, "byteValue",
							new ArrayList<Expr>(), Type.byteType()), t);
				} else if (c.equals("Character")) {
					return implicitCast(new Invoke(e, "charValue",
							new ArrayList<Expr>(), Type.charType()), t);
				} else if (c.equals("Short")) {
					return implicitCast(new Invoke(e, "shortValue",
							new ArrayList<Expr>(), Type.shortType()), t);
				} else if (c.equals("Integer")) {
					return implicitCast(new Invoke(e, "intValue",
							new ArrayList<Expr>(), Type.intType()), t);
				} else if (c.equals("Long")) {
					return implicitCast(new Invoke(e, "longValue",
							new ArrayList<Expr>(), Type.longType()), t);
				} else if (c.equals("Float")) {
					return implicitCast(new Invoke(e, "floatValue",
							new ArrayList<Expr>(), Type.floatType()), t);
				} else if (c.equals("Double")) {
					return implicitCast(new Invoke(e, "doubleValue",
							new ArrayList<Expr>(), Type.doubleType()), t);
				} else if (c.equals("Boolean")) {
					return implicitCast(new Invoke(e, "booleanValue",
							new ArrayList<Expr>(), Type.booleanType()), t);
				} else {
					throw new RuntimeException("Unreachable code reached!");
				}
			}
		} else if(e.type instanceof Type.Primitive && t instanceof Type.Reference) {
			//Type.Reference r = (Type.Reference) t; //never used			
			if (t.supsetEqOf(Type.referenceType("java.lang","Byte")) && e.type instanceof Type.Byte) {
				return new New(Type.referenceType("java.lang","Byte"),e);
			} else if (t.supsetEqOf(Type.referenceType("java.lang","Character")) && e.type instanceof Type.Char) {
				return new New(Type.referenceType("java.lang","Character"),e);
			} else if (t.supsetEqOf(Type.referenceType("java.lang","Short")) && e.type instanceof Type.Short) {
				return new New(Type.referenceType("java.lang","Short"),e);
			} else if (t.supsetEqOf(Type.referenceType("java.lang","Integer"))&& e.type instanceof Type.Int) {
				return new New(Type.referenceType("java.lang","Integer"),e);
			} else if (t.supsetEqOf(Type.referenceType("java.lang","Long"))&& e.type instanceof Type.Long) {
				return new New(Type.referenceType("java.lang","Long"),e);
			} else if (t.supsetEqOf(Type.referenceType("java.lang","Float")) && e.type instanceof Type.Float) {
				return new New(Type.referenceType("java.lang","Float"),e);
			} else if (t.supsetEqOf(Type.referenceType("java.lang","Double"))&& e.type instanceof Type.Double) {
				return new New(Type.referenceType("java.lang","Double"),e);
			} else {
				throw new RuntimeException("Unreachable code reached! (" + e.type + ", " + t + ")");
			}			
		} 
		
		return e;
	}
	
	/**
     * Given the types of the left-hand and right-hand sides for a binary
     * operator, determine the appropriate type for that operator. This method
     * follows the Java Language Specification 5.6.1:
     * 
     * @param lhs
     * @param rhs
     * @return
     */
	public Type.Primitive unaryNumericPromotion(Type lhs) {
		// First, we must unbox either operand if they are boxed.		
		if(lhs instanceof Type.Reference) {
			lhs = unboxedType((Type.Reference) lhs);
		}
		
		if (lhs instanceof Type.Char || lhs instanceof Type.Short
				|| lhs instanceof Type.Byte) {
			return Type.intType();
		}
		
		return (Type.Primitive) lhs;
	}
	
	/**
     * Given the types of the left-hand and right-hand sides for a binary
     * operator, determine the appropriate type for that operator. This method
     * follows the Java Language Specification 5.6.2:
     * 
     * @param lhs
     * @param rhs
     * @return
     */
	public Type.Primitive binaryNumericPromotion(Type lhs, Type rhs) {
		
		// First, we must unbox either operand if they are boxed.
		if(lhs instanceof Type.Reference) {
			lhs = unboxedType((Type.Reference) lhs);
		}
		if(rhs instanceof Type.Reference) {
			rhs = unboxedType((Type.Reference) rhs);
		}
		
		// Second, convert to the appropriate type
		if(lhs instanceof Type.Double || rhs instanceof Type.Double) {
			return Type.doubleType();
		}
		if(lhs instanceof Type.Float || rhs instanceof Type.Float) {
			return Type.floatType();
		}
		if(lhs instanceof Type.Long || rhs instanceof Type.Long) {
			return Type.longType();
		}
		
		// The following is not part of JLS 5.6.2, but is handy for dealing with
        // boolean operators &, |, ^ etc.
		if(lhs instanceof Type.Boolean && rhs instanceof Type.Boolean) {
			return Type.booleanType();
		}
		
		return Type.intType();		
	}
	
	public boolean isJavaLangObject(Type type) {
		if(type instanceof Type.Reference) {
			Type.Reference t = (Type.Reference) type;		
			if(t.pkg().equals("java.lang") && t.classes().length == 1) {
				return t.classes()[0].first().equals("Object");
			}
		}
		return false;
	}
	
	public boolean isJavaLangNumber(Type type) {
		if(type instanceof Type.Reference) {
			Type.Reference t = (Type.Reference) type;		
			if(t.pkg().equals("java.lang") && t.classes().length == 1) {
				return t.classes()[0].first().equals("Number");
			}
		}
		return false;
	}
}
