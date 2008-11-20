// This file is part of the Java Compiler Kit (JKit)
//
// The Java Compiler Kit is free software; you can 
// redistribute it and/or modify it under the terms of the 
// GNU General Public License as published by the Free Software 
// Foundation; either version 2 of the License, or (at your 
// option) any later version.
//
// The Java Compiler Kit is distributed in the hope
// that it will be useful, but WITHOUT ANY WARRANTY; without 
// even the implied warranty of MERCHANTABILITY or FITNESS FOR 
// A PARTICULAR PURPOSE.  See the GNU General Public License 
// for more details.
//
// You should have received a copy of the GNU General Public 
// License along with the Java Compiler Kit; if not, 
// write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA
//
// (C) David James Pearce, 2007. 

package mocha.core;

import java.util.ArrayList;
import java.util.Map;

import jkit.core.ClassTable;
import jkit.core.FieldNotFoundException;
import jkit.core.MethodNotFoundException;
import jkit.jkil.Clazz;
import jkit.jkil.Field;
import jkit.jkil.FlowGraph;
import jkit.jkil.Type;
import jkit.util.Pair;
import jkit.util.Triple;

/**
 * This class provides a set of helpful functions for reasoning about and
 * manipulating types of code expressions and statements.
 * 
 * @author djp
 * 
 */

public class Types {

	/**
     * This method determines the type of an expression given a typing
     * environment. If the expression is ill-typed then it will return Type.Void
     * (i.e. bottom) which indicates no valid type exists.
     * 
     * @param expr
     *            The expression to be typed
     * @param environment
     *            A map from local variables to their types
     * @return
     * @throws ClassNotFoundException
     *             If it needs to access a Class which cannot be found.
     * @throws FieldNotFoundException
     *             When a field is accessed which cannot be located in the given
     *             class.
     * @throws MethodNotFoundException
     *             When a method is accessed which cannot be located in the
     *             given class.
     */
	public static Type typeOf(FlowGraph.Expr expr, Map<String, Type> environment)
			throws ClassNotFoundException, MethodNotFoundException,
			FieldNotFoundException {
		if(expr instanceof FlowGraph.Value) {
			if(expr instanceof FlowGraph.BoolVal) {
				return Type.booleanType();
			} else if(expr instanceof FlowGraph.CharVal) {
				return Type.charType();
			} else if(expr instanceof FlowGraph.ByteVal) {
				return Type.byteType();
			} else if(expr instanceof FlowGraph.ShortVal) {
				return Type.shortType();
			} else if(expr instanceof FlowGraph.IntVal) {
				return Type.intType();
			} else if(expr instanceof FlowGraph.LongVal) {
				return Type.longType();
			} else if(expr instanceof FlowGraph.FloatVal) {
				return Type.floatType();
			} else if(expr instanceof FlowGraph.DoubleVal) {
				return Type.doubleType();
			} else if(expr instanceof FlowGraph.StringVal) {
				@SuppressWarnings("unchecked")
				Pair<String,Type[]>[] classes = new Pair[1];
				classes[0] = new Pair<String,Type[]>("String",new Type[0]);
				return Type.referenceType("java.lang",classes);
			} else if(expr instanceof FlowGraph.NullVal) {
				return Type.nullType();
			} else if(expr instanceof FlowGraph.New) {
				return ((FlowGraph.New) expr).type;
			} else if(expr instanceof FlowGraph.ClassVal) {
				String pkg = "java.lang";
				@SuppressWarnings("unchecked")
				Pair<String, Type[]>[] classes = new Pair[1];
				Type[] t = new Type[1];
				t[0] = ((FlowGraph.ClassVal) expr).classType;
				classes[0] = new Pair<String, Type[]>("Class", t);
				return Type.referenceType(pkg, classes);
			} else if(expr instanceof FlowGraph.ArrayVal) {
				FlowGraph.ArrayVal aval = (FlowGraph.ArrayVal) expr;
				Type t = typeOf(aval.values.get(0), environment);
				for(int i = 1; i < aval.values.size(); i++) {
					t = t.union(typeOf(aval.values.get(i), environment));
				}
				return Type.arrayType(t);
			}
		} else if(expr instanceof FlowGraph.ClassAccess) {
			return ((FlowGraph.ClassAccess) expr).clazz;
		} else if(expr instanceof FlowGraph.ArrayIndex) {		
			FlowGraph.ArrayIndex ai = (FlowGraph.ArrayIndex) expr;
			Type at = typeOf(ai.array, environment);
			return ((Type.Array) at).elementType();			
		} else if(expr instanceof FlowGraph.LocalVar) {
			FlowGraph.LocalVar lv = (FlowGraph.LocalVar) expr;
			assert environment.containsKey(lv.name);
			// Easy, it's a local variable access
			return environment.get(lv.name);
		} else if(expr instanceof FlowGraph.Deref) {
			return typeOfDeref((FlowGraph.Deref) expr, environment);
		} else if(expr instanceof FlowGraph.Invoke) {
			return typeOfInvoke((FlowGraph.Invoke) expr, environment);
		} else if(expr instanceof FlowGraph.UnOp) { 		
			return typeOfUnOp((FlowGraph.UnOp) expr, environment);
		} else if(expr instanceof FlowGraph.BinOp) {
			return typeOfBinOp((FlowGraph.BinOp) expr, environment);
		} else if(expr instanceof FlowGraph.Cast) {
			return typeOfCast((FlowGraph.Cast) expr, environment);
		} else if(expr instanceof FlowGraph.InstanceOf) {
			return Type.booleanType();
		} else if(expr instanceof FlowGraph.TernOp) {
			FlowGraph.TernOp top = (FlowGraph.TernOp) expr;
			return typeOf(top.toption, environment).union(typeOf(top.foption, environment));
		} 
		throw new RuntimeException("Unknown expression encountered (" + expr + ")");
	}	
	
	private static Type typeOfInvoke(FlowGraph.Invoke call,
			Map<String, Type> environment) throws ClassNotFoundException,
			MethodNotFoundException, FieldNotFoundException {
		// first, determine the parameter types
		
		ArrayList<Type> parameterTypes = new ArrayList<Type>();
		for(FlowGraph.Expr p : call.parameters) {						
			parameterTypes.add(typeOf(p, environment));
		}

		Type.Reference receiver = (Type.Reference) typeOf(call.target, environment);
	
					
		return ClassTable.resolveMethod(receiver,call.name,parameterTypes).third().returnType();		
	}
	
	private static Type typeOfDeref(FlowGraph.Deref deref,
			Map<String, Type> environment) throws ClassNotFoundException,
			MethodNotFoundException, FieldNotFoundException {
		Type lhsT = typeOf(deref.target,environment);
		if(lhsT instanceof Type.Reference) {
			Triple<Clazz, Field, Type> fieldInfo = ClassTable.resolveField(
				(Type.Reference) lhsT, deref.name);
			return fieldInfo.third();			
		} else if(lhsT instanceof Type.Array && deref.name.equals("length")){
			return new Type.Int();
		} else {
			return Type.voidType();
		}
	}
	
	private static Type typeOfUnOp(FlowGraph.UnOp uop,
			Map<String, Type> environment) throws ClassNotFoundException,
			MethodNotFoundException, FieldNotFoundException {
		Type expr_t = typeOf(uop.expr,environment);
		switch(uop.op) {
		case FlowGraph.UnOp.NEG:
			if (expr_t instanceof Type.Byte || expr_t instanceof Type.Char
					|| expr_t instanceof Type.Short) {
				// This is a strange feature of javac. I don't really understand
				// why it's necessary.
				return Type.intType();
			} else if (expr_t instanceof Type.Int
					|| expr_t instanceof Type.Long
					|| expr_t instanceof Type.Float
					|| expr_t instanceof Type.Double) {
				return expr_t;
			} else {
				return Type.voidType();
			}
		
		case FlowGraph.UnOp.NOT:
			if (expr_t instanceof Type.Boolean) {
				return expr_t;
			} else {
				return Type.voidType();
			}
			
		case FlowGraph.UnOp.INV:
			if (expr_t instanceof Type.Byte || expr_t instanceof Type.Char
					|| expr_t instanceof Type.Short) {
				// This is a strange feature of javac. I don't really understand
				// why it's necessary.
				return Type.intType();
			} else if (expr_t instanceof Type.Int
					|| expr_t instanceof Type.Long) {
				return expr_t;
			} else {
				return Type.voidType();
			}
		case FlowGraph.UnOp.POSTDEC:
		case FlowGraph.UnOp.POSTINC:
		case FlowGraph.UnOp.PREDEC:
		case FlowGraph.UnOp.PREINC:
			return typeOf(uop.expr, environment);
		}
			
		throw new RuntimeException("Unknown unary operator encountered");
	}
	
	private static Type typeOfBinOp(FlowGraph.BinOp bop,
			Map<String, Type> environment) throws ClassNotFoundException,
			MethodNotFoundException, FieldNotFoundException {		
		Type lhs_t = typeOf(bop.lhs,environment);
		Type rhs_t = typeOf(bop.rhs,environment);
		if(lhs_t instanceof Type.Boolean) {
			switch(bop.op) {
			case FlowGraph.BinOp.EQ:
			case FlowGraph.BinOp.NEQ:
			case FlowGraph.BinOp.LAND:
			case FlowGraph.BinOp.LOR:			
			case FlowGraph.BinOp.AND:
			case FlowGraph.BinOp.OR:
			case FlowGraph.BinOp.XOR:
				return lhs_t.union(rhs_t);
			}
			// all other cases don't make sense
			return Type.voidType();
		} else if((lhs_t instanceof Type.Char || lhs_t instanceof Type.Byte 
				|| lhs_t instanceof Type.Int || lhs_t instanceof Type.Long 
				|| lhs_t instanceof Type.Short || lhs_t instanceof Type.Float
				|| lhs_t instanceof Type.Double) && 
				(rhs_t instanceof Type.Char || rhs_t instanceof Type.Byte
						|| rhs_t instanceof Type.Int || rhs_t instanceof Type.Long 
						|| rhs_t instanceof Type.Short || rhs_t instanceof Type.Float
						|| rhs_t instanceof Type.Double)) {						
			switch(bop.op) {
				// easy cases first
				case FlowGraph.BinOp.EQ:
				case FlowGraph.BinOp.NEQ:
				case FlowGraph.BinOp.LT:
				case FlowGraph.BinOp.LTEQ:
				case FlowGraph.BinOp.GT:
				case FlowGraph.BinOp.GTEQ:
					return Type.booleanType();
				case FlowGraph.BinOp.ADD:
				case FlowGraph.BinOp.SUB:
				case FlowGraph.BinOp.MUL:
				case FlowGraph.BinOp.DIV:
				case FlowGraph.BinOp.MOD:
					return joinPrimitiveType((Type.Primitive)lhs_t,(Type.Primitive)rhs_t);
				case FlowGraph.BinOp.SHL:
				case FlowGraph.BinOp.SHR:
				case FlowGraph.BinOp.USHR:
				case FlowGraph.BinOp.AND:
				case FlowGraph.BinOp.OR:
				case FlowGraph.BinOp.XOR:
				{					
					Type rt = joinPrimitiveType((Type.Primitive)lhs_t,(Type.Primitive)rhs_t);					
					if(rt instanceof Type.Float || rt instanceof Type.Double) {
						return Type.voidType();
					} else {
						return rt;
					}
				}					
			}
		} else if(lhs_t instanceof Type.Reference || lhs_t instanceof Type.Array 
				|| rhs_t instanceof Type.Reference || rhs_t instanceof Type.Array) {
			switch(bop.op) {
				// easy cases first
				case FlowGraph.BinOp.EQ:
				case FlowGraph.BinOp.NEQ:
					return Type.booleanType();
				case FlowGraph.BinOp.ADD:
					if(isString(lhs_t)) {
						return lhs_t;
					} else if(isString(rhs_t)) {
						return rhs_t;
					} 
			}
		} 
		
		return Type.voidType();
	}

	private static Type typeOfCast(FlowGraph.Cast cast, Map<String,Type> environment) {
		return cast.type;
	}
	
	private static boolean isString(Type t) {
		return t instanceof Type.Reference && ((Type.Reference)t).unqualifiedName().equals("java.lang.String");
	}
	
	private static Type joinPrimitiveType(Type.Primitive t1, Type.Primitive t2) {
		return Type.Primitive.join(t1, t2);
	}
	
}
