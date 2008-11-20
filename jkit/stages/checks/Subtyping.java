package jkit.stages.checks;

import java.util.Map;

import jkit.core.*;
import jkit.core.FlowGraph.*;
import jkit.stages.*;
import jkit.util.*;

/**
 * <p>This stage checks that any value flowing into a variable, parameter or field
 * or from a method return is a subtype of its destination. For example,
 * consider the following simple program:</p>
 * 
 * <pre>
 * void f(byte x) { ... }
 * 
 * int x = 1;
 * String y = x;  <--- error!
 * f(x);          <--- error!
 * </pre>
 * <p>The above errors are check for by this stage.</p>
 * @author djp
 * 
 */
public class Subtyping implements Stage {
	public String description() {
		return "Ensure types used correctly within statements and expressions.  Make conversions explicit.";
	}
	
	public void apply(Clazz owner) {
		for(Method m : owner.methods()) {			
			if(m.code() != null) {
				check(m,owner);
			} 
		}
	}
	
	public void check(Method method,Clazz owner) {
		FlowGraph cfg = method.code();
		
		Map<String,Type> environment = FlowGraph.buildEnvironment(method,owner);
		environment.put("$",Type.voidType()); // this is pretty ugly ... :) 
		// First, do statements
		for(Point p : cfg.domain()) {		
			if(p.statement() != null) {
				check(p.statement(),environment,p,method,owner);
			}
		}
		
		// Second, do edges		
		for(Triple<Point,Point,Expr> edge : cfg) {
			Expr e = edge.third();
			if(e!=null) {
				check(e,environment,edge.first(),method,owner);
				if(!(e.type instanceof Type.Boolean || e instanceof FlowGraph.Exception)) {
					throw new InternalException(
							"Conditional requires \"boolean\" type, found " + e.type,
							edge.first(), method, owner);
				} 
			} 
		}		
	}
		
	public void check(Stmt stmt,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		// Only intercept statement types that may require name resolution.
		if(stmt instanceof Assign) {
			check((Assign)stmt,environment,point,method,owner);					
		} else if(stmt instanceof Invoke) {
			check((Invoke)stmt,environment,point,method,owner);										
		} else if(stmt instanceof New) {
			check((New) stmt,environment,point,method,owner);						
		} else if(stmt instanceof Return) {
			check((Return) stmt,environment,point,method,owner);
		} else if(stmt instanceof Throw) {
			check((Throw) stmt,environment,point,method,owner);
		} else if(stmt instanceof Nop) {		
			check((Nop) stmt,environment,point,method,owner);
		} else if(stmt instanceof Lock) {		
			check((Lock) stmt,environment,point,method,owner);
		} else if(stmt instanceof Unlock) {		
			check((Unlock) stmt,environment,point,method,owner);
		} else {
			throw new InternalException("Unknown statement encountered: " + stmt,point,method,owner);
		}
	}
	
	public void check(Assign a, Map<String,Type> environment,Point point, Method method, Clazz owner) {		
		check(a.lhs,environment,point,method,owner);
		check(a.rhs,environment,point,method,owner);			
		if(!a.lhs.type.supsetEqOf(a.rhs.type)) {			
			throw new InternalException("Required type \"" + a.lhs.type
					+ "\",  found type \"" + a.rhs.type + "\"", point,method,owner);
		} 				
	}
	
	public void check(Return r, Map<String,Type> environment,Point point, Method method, Clazz owner) {		
		if(r.expr != null) { 
			check(r.expr,environment,point,method,owner);
			if(!method.type().returnType().supsetEqOf(r.expr.type)) {
				throw new InternalException("Required return type \"" + method.type().returnType()
					+ "\",  found type \"" + r.expr.type + "\"", point,method,owner);	
			}
		} else if(!(method.type().returnType() instanceof Type.Void)) {
			throw new InternalException("Return value required!", point,method,owner);
		}
	}
	
	public void check(Throw t, Map<String,Type> environment,Point point, Method method, Clazz owner) {				
		check(t.expr,environment,point,method,owner);		
	}

	public void check(Nop n, Map<String,Type> environment,Point point, Method method, Clazz owner) {				
		// do nothing		
	}
	
	public void check(Lock n, Map<String,Type> environment,Point point, Method method, Clazz owner) {				
		// do nothing		
	}
	
	public void check(Unlock n, Map<String,Type> environment,Point point, Method method, Clazz owner) {				
		// do nothing		
	}
	
	public void check(Expr expr,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		if(expr instanceof ArrayIndex) {
			check((ArrayIndex) expr, environment,point,method,owner);
		} else if(expr instanceof TernOp) {
			check((TernOp) expr, environment,point,method,owner);
		} else if(expr instanceof BinOp) {		
			check((BinOp) expr, environment,point,method,owner);
		} else if(expr instanceof UnOp) {		
			check((UnOp) expr, environment,point,method,owner);								
		} else if(expr instanceof Cast) {
			check((Cast) expr, environment,point,method,owner);			 			
		} else if(expr instanceof ClassAccess) {
			check((ClassAccess) expr, environment,point,method,owner);			 			
		} else if(expr instanceof Deref) {
			check((Deref) expr, environment,point,method,owner);			 							
		} else if(expr instanceof FlowGraph.Exception) {
			check((FlowGraph.Exception) expr, environment,point,method,owner);			 							
		} else if(expr instanceof LocalVar) {
			check((LocalVar) expr, environment,point,method,owner);
		} else if(expr instanceof InstanceOf) {
			check((InstanceOf) expr, environment,point,method,owner);
		} else if(expr instanceof Invoke) {
			check((Invoke) expr, environment,point,method,owner);
		} else if(expr instanceof New) {
			check((New) expr, environment,point,method,owner);
		} else if(expr instanceof Value) {
			check((Value) expr, environment,point,method,owner);
		} else {
			throw new InternalException("Unknown expression \"" + expr + "\" encoutered",point,method,owner);
		}
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
	protected void check(ArrayIndex ai,Map<String,Type> environment,Point point, Method method, Clazz owner) {		
		check(ai.array,environment,point,method,owner);
		check(ai.idx,environment,point,method,owner);
		
		if(!(ai.array.type instanceof Type.Array)) {
			throw new InternalException("Array required, but \"" + ai.array.type + "\" found",point,method,owner);
		}
		// ensure that the index must have type int.
		if (!(ai.idx.type.supsetEqOf(Type.intType()) && ai.idx.type instanceof Type.Primitive)) {
			throw new InternalException(
					"Possible loss of precision; \"int\" required, but \""
							+ ai.idx.type + "\" found", point, method, owner);
		}						
	}
		
	protected void check(BinOp bop,Map<String,Type> environment,Point point, Method method, Clazz owner) {		
		check(bop.lhs,environment,point,method,owner);
		check(bop.rhs,environment,point,method,owner);
	
		//if we find a concat operator either the lhs or the rhs was detected as a string,
		//and we can accept any type for the other
		if (bop.op == BinOp.CONCAT) return; //concat can take any types
		
		else if ((bop.lhs.type instanceof Type.Primitive || bop.rhs.type instanceof Type.Primitive)
				&& !bop.lhs.type.equals(bop.rhs.type)) {
			if ((bop.lhs.type instanceof Type.Long
					|| bop.lhs.type instanceof Type.Int
					|| bop.lhs.type instanceof Type.Short
					|| bop.lhs.type instanceof Type.Char || bop.lhs.type instanceof Type.Byte)
					&& bop.rhs.type instanceof Type.Int
					&& (bop.op == BinOp.SHL || bop.op == BinOp.SHR || bop.op == BinOp.USHR)) {
				// Ok!
			} else throw new InternalException("Operand types do not go together: "
					+ bop.rhs.type + " and " + bop.lhs.type, point,method,owner);	
		}
		
		if((bop.lhs.type instanceof Type.Char || bop.lhs.type instanceof Type.Byte 
				|| bop.lhs.type instanceof Type.Int || bop.lhs.type instanceof Type.Long 
				|| bop.lhs.type instanceof Type.Short || bop.lhs.type instanceof Type.Float
				|| bop.lhs.type instanceof Type.Double) && 
				(bop.rhs.type instanceof Type.Char || bop.rhs.type instanceof Type.Byte
						|| bop.rhs.type instanceof Type.Int || bop.rhs.type instanceof Type.Long 
						|| bop.rhs.type instanceof Type.Short || bop.rhs.type instanceof Type.Float
						|| bop.rhs.type instanceof Type.Double)) {
			switch(bop.op) {
				// easy cases first
				case BinOp.EQ:
				case BinOp.NEQ:
				case BinOp.LT:
				case BinOp.LTEQ:
				case BinOp.GT:
				case BinOp.GTEQ:
					// need more checks here
					if(!(bop.type instanceof Type.Boolean)) {
						throw new InternalException("Required type \"boolean\", found "
								+ bop.rhs.type + bop.lhs.type, point,method,owner);
					}
					break;
				case BinOp.ADD:
				case BinOp.SUB:
				case BinOp.MUL:
				case BinOp.DIV:
				case BinOp.MOD:
				{
					// hmmmm ?
					break;
				}
				case BinOp.SHL:
				case BinOp.SHR:
				case BinOp.USHR:
				{										
					//bit-shift operations always take an int as their rhs, so make sure we have an int type								
					if(bop.lhs.type instanceof Type.Float || bop.lhs.type instanceof Type.Double) {
						throw new InternalException("Invalid operation on type \"" + bop.lhs.type+ "\"", point,method,owner);
					} else if (!(bop.rhs.type instanceof Type.Int)) {
						throw new InternalException("Invalid operation on type \"" + bop.rhs.type + "\"", point,method,owner);
					} 
					break;
				}
				case BinOp.AND:
				case BinOp.OR:
				case BinOp.XOR:
				{														
					if(bop.rhs.type instanceof Type.Float || bop.rhs.type instanceof Type.Double) {
						throw new InternalException("Invalid operation on type \"" + bop.rhs.type + "\"", point,method,owner);
					} 
				}					
			}
		} 
		// probably need more checks here!
	}	
	
	protected void check(TernOp top, Map<String, Type> environment, Point point, Method method, Clazz owner) {
		check(top.cond,environment,point,method,owner);
		check(top.toption,environment,point,method,owner);
		check(top.foption,environment,point,method,owner);
		if(!(top.cond.type instanceof Type.Boolean)) {
			throw new InternalException("Required type \"boolean\", found \""
					+ top.cond.type + "\"", point,method,owner);
		}
	}
	
	protected void check(Cast cast,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		check(cast.expr,environment,point,method,owner);
		Type type = cast.expr.type;
		
		// NOTE: we must ignore references here. This is because knowing whether
		// or not one is a subtype of the other is insufficient to know whether
		// the cast will be OK.  For example, consider the following:
		//
		// static interface I1 { }
	    // static interface I2 { }
	    // static class Inner implements I1,I2 {}
		//
	    // public static void main(String[] args) {
		//  I1 x = new Inner();
		//  I2 y = (I2) x; <-- OK despite not I1 <= I2 and vice versa.		
	    // }
		
		if((type instanceof Type.Long || type instanceof Type.Int || type instanceof Type.Short || type instanceof Type.Char || type instanceof Type.Byte)
				&& (cast.type instanceof Type.Long || cast.type instanceof Type.Int || cast.type instanceof Type.Short || cast.type instanceof Type.Char || cast.type instanceof Type.Byte)) {
			// all OK
		} else if (type instanceof Type.Primitive
				|| cast.type instanceof Type.Primitive
				|| type instanceof Type.Array
				|| cast.type instanceof Type.Array) {
			
			if(!(cast.expr.type.supsetEqOf(cast.type) || cast.type
					.supsetEqOf(cast.expr.type))) {
				throw new InternalException("Inconvertible types; found "
						+ cast.expr.type + ", required " + cast.type, point,
						method, owner);
			}
		}				
	}
	
	protected void check(ClassAccess ca,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		// do nothing?
	}
	
	protected void check(Deref deref,Map<String,Type> environment,Point point, Method method, Clazz owner) {	
		check(deref.target,environment,point,method,owner);
		
		if(deref.target.type instanceof Type.Array) {
			if(!deref.name.equals("length")) {
				throw new InternalException("Illegal operation on type \"" + deref.target.type + "\"",point,method,owner);
			}
		} else if(!(deref.target.type instanceof Type.Reference)) {
			throw new InternalException("Cannot dereference non-reference type \"" + deref.target.type + "\"",point,method,owner);
		}		
	}	
	
	protected void check(InstanceOf iof,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		check(iof.lhs,environment,point,method,owner);
		if(!(iof.rhs instanceof Type.Reference || iof.rhs instanceof Type.Array)) {
			throw new InternalException("Reference type required, found \"" + iof.rhs + "\"",point,method,owner);
		}
		
		// NOTE: we must ignore references here. This is because knowing whether
		// or not one is a subtype of the other is insufficient to know whether
		// the cast will be OK.  For example, consider the following:
		//
		// static interface I1 { }
	    // static interface I2 { }
	    // static class Inner implements I1,I2 {}
		//
	    // public static void main(String[] args) {
		//  I1 x = new Inner();
		//  I2 y = (I2) x; <-- OK despite not I1 <= I2 and vice versa.		
	    // }
		
		if(iof.lhs.type instanceof Type.Reference && iof.rhs instanceof Type.Reference) {
			return;
		} else if(!(iof.lhs.type.supsetEqOf(iof.rhs) || iof.rhs.supsetEqOf(iof.lhs.type))) {
			throw new InternalException("inconvertible types",point,method,owner);		
		}
	}
	
	protected void check(Invoke ivk,Map<String,Type> environment,Point point, Method method, Clazz owner) {			
		check(ivk.target,environment,point,method,owner);
		
		for(Expr e : ivk.parameters) {
			check(e,environment,point,method,owner);			
		}				
	}
		
	protected void check(LocalVar var,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		if(environment.get(var.name) == null) {						
			throw new InternalException("Unknown local variable \"" + var.name + "\"",point,method,owner);	
		}
	}
	
	protected void check(New ne,Map<String,Type> environment,Point point, Method method, Clazz owner) {			
		if(!(ne.type instanceof Type.Reference || ne.type instanceof Type.Array)) {
			throw new InternalException("Cannot construct type \"" + ne.type + "\"",point,method,owner);
		}
		
		for(Expr e : ne.parameters) {
			check(e,environment,point,method,owner);			
		}
	}
	
	protected void check(UnOp uop,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		check(uop.expr,environment,point,method,owner);
		
		switch(uop.op) {
		case UnOp.NEG:
			if (!(uop.expr.type instanceof Type.Byte 
					|| uop.expr.type instanceof Type.Char
					|| uop.expr.type instanceof Type.Short 
					|| uop.expr.type instanceof Type.Int
					|| uop.expr.type instanceof Type.Long
					|| uop.expr.type instanceof Type.Float
					|| uop.expr.type instanceof Type.Double)) {				
				throw new InternalException("Cannot negate type \""
						+ uop.expr.type + "\"", point,method,owner);
			}
			break;
		case UnOp.NOT:
			if (!(uop.expr.type instanceof Type.Boolean)) {				
				throw new InternalException("Required type \"boolean\", found \""
						+ uop.expr.type + "\"", point,method,owner);
			}
			break;
		case UnOp.INV:
			if (!(uop.expr.type instanceof Type.Byte 
					|| uop.expr.type instanceof Type.Char
					|| uop.expr.type instanceof Type.Short 
					|| uop.expr.type instanceof Type.Int
					|| uop.expr.type instanceof Type.Long)) {
				throw new InternalException("Cannot invert type \""
						+ uop.expr.type + "\"", point,method,owner);	
			}
			break;								
		}	
	}	
	
	protected void check(FlowGraph.Exception e,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		// do nothing
	}
	
	protected void check(Value e,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		if(e instanceof ArrayVal) {
			ArrayVal av = (ArrayVal) e;
			for(Expr v : av.values) {
				check(v,environment,point,method,owner);
			}
		}
	}
}
