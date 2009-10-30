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
// (C) David James Pearce, 2009. 

package jkit.java.stages;

import java.util.*;

import static jkit.compiler.SyntaxError.*;
import static jkit.jil.util.Types.*;
import jkit.compiler.ClassLoader;
import jkit.compiler.Clazz;
import jkit.java.io.JavaFile;
import jkit.java.tree.Decl;
import jkit.java.tree.Expr;
import jkit.java.tree.Stmt;
import jkit.java.tree.Value;
import jkit.java.tree.Decl.*;
import jkit.java.tree.Expr.*;
import jkit.java.tree.Stmt.*;
import jkit.jil.tree.SourceLocation;
import jkit.jil.tree.Type;
import jkit.jil.util.Types;
import jkit.util.Triple;

/**
 * The purpose of this class is to type check the statements and expressions
 * within a Java File. The process of propogating type information (i.e. the
 * Typing stage) is separated from the process of checking those types. This is
 * for two reasons: firstly, it divides the problem into two (simpler)
 * subproblems; secondly, it provides for different ways of propagating type
 * information (e.e.g type inference).
 * 
 * @author djp
 * 
 */

public class TypeChecking {
	private ClassLoader loader;
	private Stack<Decl> enclosingScopes = new Stack<Decl>();
	private TypeSystem types;
	
	public TypeChecking(ClassLoader loader, TypeSystem types) {
		this.loader = loader; 
		this.types = types;
	}
	
	public void apply(JavaFile file) {
		for(Decl d : file.declarations()) {
			checkDeclaration(d);
		}
	}
	
	protected void checkDeclaration(Decl d) {
		try {
			if(d instanceof JavaInterface) {
				checkInterface((JavaInterface)d);
			} else if(d instanceof JavaClass) {
				checkClass((JavaClass)d);
			} else if(d instanceof JavaMethod) {
				checkMethod((JavaMethod)d);
			} else if(d instanceof JavaField) {
				checkField((JavaField)d);
			}
		} catch(Exception ex) {
			internal_error(d,ex);
		}
	}
	
	protected void checkInterface(JavaInterface c) {
		enclosingScopes.push(c);
		
		for(Decl d : c.declarations()) {
			checkDeclaration(d);
		}
		
		enclosingScopes.pop();
	}
	
	protected void checkClass(JavaClass c) {
		enclosingScopes.push(c);
		
		for(Decl d : c.declarations()) {
			checkDeclaration(d);
		}
		
		enclosingScopes.pop();
	}

	protected void checkMethod(JavaMethod d) {
		enclosingScopes.push(d);
		
		checkStatement(d.body());
		
		enclosingScopes.pop();
	}

	protected void checkField(JavaField d) {
		checkExpression(d.initialiser());
		
		Type lhs_t = d.type().attribute(Type.class);
		
		if(d.initialiser() != null) {
			Type rhs_t = d.initialiser().attribute(Type.class);

			try {			
				if (!types.subtype(lhs_t, rhs_t, loader)) {
					syntax_error(
							"required type " + lhs_t + ", found type " + rhs_t, d);
				}
			} catch (ClassNotFoundException ex) {
				syntax_error(ex.getMessage(), d);
			}	
		}
	}
	
	protected void checkStatement(Stmt e) {
		try {
			if(e instanceof Stmt.SynchronisedBlock) {
				checkSynchronisedBlock((Stmt.SynchronisedBlock)e);
			} else if(e instanceof Stmt.TryCatchBlock) {
				checkTryCatchBlock((Stmt.TryCatchBlock)e);
			} else if(e instanceof Stmt.Block) {
				checkBlock((Stmt.Block)e);
			} else if(e instanceof Stmt.VarDef) {
				checkVarDef((Stmt.VarDef) e);
			} else if(e instanceof Stmt.Assignment) {
				checkAssignment((Stmt.Assignment) e);
			} else if(e instanceof Stmt.Return) {
				checkReturn((Stmt.Return) e);
			} else if(e instanceof Stmt.Throw) {
				checkThrow((Stmt.Throw) e);
			} else if(e instanceof Stmt.Assert) {
				checkAssert((Stmt.Assert) e);
			} else if(e instanceof Stmt.Break) {
				checkBreak((Stmt.Break) e);
			} else if(e instanceof Stmt.Continue) {
				checkContinue((Stmt.Continue) e);
			} else if(e instanceof Stmt.Label) {
				checkLabel((Stmt.Label) e);
			} else if(e instanceof Stmt.If) {
				checkIf((Stmt.If) e);
			} else if(e instanceof Stmt.For) {
				checkFor((Stmt.For) e);
			} else if(e instanceof Stmt.ForEach) {
				checkForEach((Stmt.ForEach) e);
			} else if(e instanceof Stmt.While) {
				checkWhile((Stmt.While) e);
			} else if(e instanceof Stmt.DoWhile) {
				checkDoWhile((Stmt.DoWhile) e);
			} else if(e instanceof Stmt.Switch) {
				checkSwitch((Stmt.Switch) e);
			} else if(e instanceof Expr.Invoke) {
				checkInvoke((Expr.Invoke) e);
			} else if(e instanceof Expr.New) {
				checkNew((Expr.New) e);
			} else if(e instanceof Decl.JavaClass) {
				checkClass((Decl.JavaClass)e);
			} else if(e instanceof Stmt.PrePostIncDec) {
				checkExpression((Stmt.PrePostIncDec)e);
			} else if(e != null) {
				throw new RuntimeException("Invalid statement encountered: "
						+ e.getClass());
			}		
		} catch(Exception ex) {
			internal_error(e,ex);
		}
	}
	
	protected void checkBlock(Stmt.Block block) {	
		if(block != null) {
			for(Stmt s : block.statements()) {
				checkStatement(s);
			}
		}
	}
	
	protected void checkSynchronisedBlock(Stmt.SynchronisedBlock block) {
		checkExpression(block.expr());
		checkBlock(block);
		
		Type e_t = block.expr().attribute(Type.class);
		
		if (!(e_t instanceof Type.Reference)) {
			syntax_error("required reference type, found type "
					+ e_t, block);
		} 
	}
	
	protected void checkTryCatchBlock(Stmt.TryCatchBlock block) {
		checkBlock(block);
		checkBlock(block.finaly());
		
		for(Stmt.CatchBlock cb : block.handlers()) {
			checkBlock(cb);
			try {
				if (!types.subtype(Types.JAVA_LANG_THROWABLE,
						cb.type().attribute(Type.class), loader)) {
					syntax_error(
							"required subtype of java.lang.Throwable, found type "
							+ cb.type(), cb);
				}
			} catch (ClassNotFoundException ex) {
				syntax_error(ex.getMessage(), block);
			}	
		}
	}
	
	protected void checkVarDef(Stmt.VarDef def) {
		// Observe that we cannot use the declared type here, rather we have to
        // use the resolved type!
		Type t = def.type().attribute(Type.class);
		
		for(Triple<String, Integer, Expr> d : def.definitions()) {								
			if(d.third() != null) {
				checkExpression(d.third());

				Type nt = t;
				for(int i=0;i!=d.second();++i) {
					nt = new Type.Array(nt);
				}

				Type i_t = d.third().attribute(Type.class);
				try {
					if (!types.subtype(nt, i_t, loader)) {
						syntax_error("required type " + nt + ", found type " + i_t, def);
					}
				} catch (ClassNotFoundException ex) {
					syntax_error(ex.getMessage(), def);
				}	
			}
		}
	}
	
	protected void checkAssignment(Stmt.Assignment def) {
		checkExpression(def.lhs());	
		checkExpression(def.rhs());					
		
		Type lhs_t = def.lhs().attribute(Type.class);
		Type rhs_t = def.rhs().attribute(Type.class);
		
		try {			
			if (!types.subtype(lhs_t, rhs_t, loader)) {
				syntax_error(
						"required type " + lhs_t + ", found type " + rhs_t, def);
			}
		} catch (ClassNotFoundException ex) {
			syntax_error(ex.getMessage(), def);
		}	
	}
	
	protected void checkReturn(Stmt.Return ret) {		
		JavaMethod method = (JavaMethod) getEnclosingScope(JavaMethod.class);				
		
		if(method instanceof JavaConstructor) {
			return; // could do better than this.
		}
		
		Type retType = method.returnType().attribute(Type.class);
		
		if(ret.expr() != null) { 
			checkExpression(ret.expr());						
			
			Type ret_t = ret.expr().attribute(Type.class);
			try {
				if(ret_t.equals(new Type.Void())) {
					syntax_error(
							"cannot return a value from method whose result type is void",
							ret);	
				} else if (!types.subtype(retType, ret_t, loader)) {
					syntax_error("required return type " + retType
							+ ",  found type " + ret_t, ret);
				}

			} catch (ClassNotFoundException ex) {
				syntax_error(ex.getMessage(), ret);
			}				
		} else if(!(retType instanceof Type.Void)) {
			syntax_error("missing return value", ret);
		}
	}
	
	protected void checkThrow(Stmt.Throw ret) {
		checkExpression(ret.expr());
	}
	
	protected void checkAssert(Stmt.Assert ret) {
		checkExpression(ret.expr());
	}
	
	protected void checkBreak(Stmt.Break brk) {
		// could check break label exists (if there is one)
	}
	
	protected void checkContinue(Stmt.Continue brk) {
		// could check continue label exists (if there is one)			
	}
	
	protected void checkLabel(Stmt.Label lab) {				
		// do nothing
	}
	
	protected void checkIf(Stmt.If stmt) {		
		checkExpression(stmt.condition());
		checkStatement(stmt.trueStatement());		
		checkStatement(stmt.falseStatement());		
		
		if(stmt.condition() != null) {
			Type c_t = stmt.condition().attribute(Type.class);

			if(!(c_t instanceof Type.Bool)) {
				syntax_error("required type boolean, found " + c_t, stmt);								
			}
		}
	}
	
	protected void checkWhile(Stmt.While stmt) {
		checkExpression(stmt.condition());
		checkStatement(stmt.body());

		if(stmt.condition() != null) {
			Type c_t = stmt.condition().attribute(Type.class);

			if (!(c_t instanceof Type.Bool)) {
				syntax_error("required type boolean, found " + c_t, stmt);
			}
		}
	}
	
	protected void checkDoWhile(Stmt.DoWhile stmt) {
		checkExpression(stmt.condition());
		checkStatement(stmt.body());

		if(stmt.condition() != null) {
			Type c_t = stmt.condition().attribute(Type.class);

			if (!(c_t instanceof Type.Bool)) {
				syntax_error("required type boolean, found " + c_t, stmt);			
			}
		}
	}
	
	protected void checkFor(Stmt.For stmt) {
		checkStatement(stmt.initialiser());
		checkExpression(stmt.condition());
		checkStatement(stmt.increment());
		checkStatement(stmt.body());

		if(stmt.condition() != null) {
			Type c_t = stmt.condition().attribute(Type.class);

			if (!(c_t instanceof Type.Bool)) {
				syntax_error("required type boolean, found " + c_t, stmt);			
			}
		}
	}
	
	protected void checkForEach(Stmt.ForEach stmt) {		
		checkExpression(stmt.source());
		checkStatement(stmt.body());

		// need to check that the static type of the source expression
		// implements java.lang.iterable
		Type s_t = stmt.source().attribute(Type.class);
		try {
			if (!(s_t instanceof Type.Array)
					&& !types.subtype(new Type.Clazz("java.lang", "Iterable"),
							s_t, loader)) {
				syntax_error("foreach not applicable to expression type", stmt);
			} 
		} catch (ClassNotFoundException ex) {
			syntax_error(ex.getMessage(), stmt);
		}		
	}
	
	protected void checkSwitch(Stmt.Switch sw) {
		checkExpression(sw.condition());
		
		Type condT = sw.condition().attribute(Type.class);
		
		if(!(condT instanceof Type.Int)) {
			syntax_error("found type " + condT + ", required int",sw);
		}
		
		for(Case c : sw.cases()) {
			checkExpression(c.condition());
			for(Stmt s : c.statements()) {
				checkStatement(s);
			}
		}
	}
	
	protected void checkExpression(Expr e) {
		try {
			if(e instanceof Value.Bool) {
				checkBoolVal((Value.Bool)e);
			} else if(e instanceof Value.Byte) {
				checkByteVal((Value.Byte)e);
			} else if(e instanceof Value.Char) {
				checkCharVal((Value.Char)e);
			} else if(e instanceof Value.Int) {
				checkIntVal((Value.Int)e);
			} else if(e instanceof Value.Short) {
				checkShortVal((Value.Short)e);
			} else if(e instanceof Value.Long) {
				checkLongVal((Value.Long)e);
			} else if(e instanceof Value.Float) {
				checkFloatVal((Value.Float)e);
			} else if(e instanceof Value.Double) {
				checkDoubleVal((Value.Double)e);
			} else if(e instanceof Value.String) {
				checkStringVal((Value.String)e);
			} else if(e instanceof Value.Null) {
				checkNullVal((Value.Null)e);
			} else if(e instanceof Value.TypedArray) {
				checkTypedArrayVal((Value.TypedArray)e);
			} else if(e instanceof Value.Array) {
				checkArrayVal((Value.Array)e);
			} else if(e instanceof Value.Class) {
				checkClassVal((Value.Class) e);
			} else if(e instanceof Expr.LocalVariable) {
				checkLocalVariable((Expr.LocalVariable)e);
			} else if(e instanceof Expr.NonLocalVariable) {
				checkNonLocalVariable((Expr.NonLocalVariable)e);
			} else if(e instanceof Expr.ClassVariable) {
				checkClassVariable((Expr.ClassVariable)e);
			} else if(e instanceof Expr.UnOp) {
				checkUnOp((Expr.UnOp)e);
			} else if(e instanceof Expr.BinOp) {
				checkBinOp((Expr.BinOp)e);
			} else if(e instanceof Expr.TernOp) {
				checkTernOp((Expr.TernOp)e);
			} else if(e instanceof Expr.Cast) {
				checkCast((Expr.Cast)e);
			} else if(e instanceof Expr.Convert) {
				checkConvert((Expr.Convert)e);
			} else if(e instanceof Expr.InstanceOf) {
				checkInstanceOf((Expr.InstanceOf)e);
			} else if(e instanceof Expr.Invoke) {
				checkInvoke((Expr.Invoke) e);
			} else if(e instanceof Expr.New) {
				checkNew((Expr.New) e);
			} else if(e instanceof Expr.ArrayIndex) {
				checkArrayIndex((Expr.ArrayIndex) e);
			} else if(e instanceof Expr.Deref) {
				checkDeref((Expr.Deref) e);
			} else if(e instanceof Stmt.Assignment) {
				checkAssignment((Stmt.Assignment) e);			
			} else if(e != null) {
				throw new RuntimeException("Invalid expression encountered: "
						+ e.getClass());
			}
		} catch(Exception ex) {
			internal_error(e,ex);
		}
	}
	
	protected void checkDeref(Expr.Deref e) {
		checkExpression(e.target());
		
		// here, we need to check that the field in question actually exists!
	}
	
	protected void checkArrayIndex(Expr.ArrayIndex e) {
		checkExpression(e.index());
		checkExpression(e.target());
		
		Type i_t = e.index().attribute(Type.class);
		
		if(!(i_t instanceof Type.Int)) {
			syntax_error("required type int, found type " + i_t, e);
		}
		
		Type t_t = e.target().attribute(Type.class);		
		if(!(t_t instanceof Type.Array)) {			
			syntax_error("array required, but " + t_t + " found", e);
		}
	}
	
	protected void checkNew(Expr.New e) {
		checkExpression(e.context());
		
		for(Decl d : e.declarations()) {
			checkDeclaration(d);
		}
	}
	
	protected void checkInvoke(Expr.Invoke e) {
		for(Expr p : e.parameters()) {
			checkExpression(p);
		}
	}
	
	protected void checkInstanceOf(Expr.InstanceOf e) throws ClassNotFoundException {		
		checkExpression(e.lhs());
		
		Type lhs_t = e.lhs().attribute(Type.class);
		Type rhs_t = e.rhs().attribute(Type.class);

		if(lhs_t instanceof Type.Primitive) {
			syntax_error("required reference type, found " + lhs_t , e);			
		} else if(!(rhs_t instanceof Type.Reference)) {
			syntax_error("required class or array type, found " + rhs_t , e);
		} else if((lhs_t instanceof Type.Array || rhs_t instanceof Type.Array)
				&& !(types.subtype(lhs_t,rhs_t,loader))) {
			syntax_error("inconvertible types: " + lhs_t + ", " + rhs_t, e);
		}	
	}
	
	protected void checkCast(Expr.Cast e) {
		Type e_t = e.expr().attribute(Type.class);
		Type c_t = e.type().attribute(Type.class);
		try {		
			if(e_t instanceof Type.Clazz && c_t instanceof Type.Clazz) {
				Clazz c_c = loader.loadClass((Type.Clazz) c_t);
				Clazz e_c = loader.loadClass((Type.Clazz) e_t);
				
				// the trick here, is that javac will never reject a cast
				// between an interface and a class or interface. However, if we
				// have a cast from one class to another class, then it will
				// reject this if neither is a subclass of the other.
				if(c_c.isInterface() || e_c.isInterface()) {					
					// cast cannot fail here.
					return;
				}
			} else if ((e_t instanceof Type.Variable || e_t instanceof Type.Wildcard)
					&& c_t instanceof Type.Reference) {
				// javac always lets this pass, no matter what
				return;
			}
									
			if (types.boxSubtype(c_t, e_t, loader)
					|| types.boxSubtype(e_t, c_t, loader)) {
				// this is OK
				return;
			} else if (c_t instanceof Type.Primitive
					&& e_t instanceof Type.Primitive) {
				if (e_t instanceof Type.Char
						&& (c_t instanceof Type.Byte || c_t instanceof Type.Short)) {
					return;
				} else if (c_t instanceof Type.Char
						&& (e_t instanceof Type.Byte || e_t instanceof Type.Short)) {
					return;
				}
			} 
			
			syntax_error("inconvertible types: " + e_t + ", " + c_t, e);			
		} catch(ClassNotFoundException ex) {
			syntax_error (ex.getMessage(),e);
		}
	}
	
	protected void checkConvert(Expr.Convert e) {
		Type rhs_t = e.expr().attribute(Type.class);
		Type c_t = (Type) e.type().attribute(Type.class);		
		try {
			if(!types.subtype(c_t,rhs_t, loader)) {
				if(rhs_t instanceof Type.Primitive) {
					syntax_error("possible loss of precision (" + rhs_t + "=>" + c_t+")",e);
				} else {
					syntax_error("incompatible types",e);
				}
			}
		} catch(ClassNotFoundException ex) {
			syntax_error (ex.getMessage(),e);			
		}
	}
	
	protected void checkBoolVal(Value.Bool e) {
		// do nothing!
	}
	
	protected void checkByteVal(Value.Byte e) {
		// do nothing!
	}
	
	protected void checkCharVal(Value.Char e) {
		// do nothing!		
	}
	
	protected void checkShortVal(Value.Short e) {		
		// do nothing!
	}
	
	protected void checkIntVal(Value.Int e) {
		// do nothing!
	}	
	
	protected void checkLongVal(Value.Long e) {		
		// do nothing!
	}
	
	protected void checkFloatVal(Value.Float e) {		
		// do nothing!
	}
	
	protected void checkDoubleVal(Value.Double e) {		
		// do nothing!
	}
	
	protected void checkStringVal(Value.String e) {		
		// do nothing!
	}
	
	protected void checkNullVal(Value.Null e) {		
		// do nothing!
	}
	
	protected void checkTypedArrayVal(Value.TypedArray e) {		
		for(Expr v : e.values()) {
			checkExpression(v);
		}
	}
	
	protected void checkArrayVal(Value.Array e) {		 
		for(Expr v : e.values()) {
			checkExpression(v);
		}
	}
	
	protected void checkClassVal(Value.Class e) {
		// do nothing!	
	}
	
	protected void checkLocalVariable(Expr.LocalVariable e) {			
		// do nothing!
	}

	protected void checkNonLocalVariable(Expr.NonLocalVariable e) {			
		// do nothing!
	}
	
	protected void checkClassVariable(Expr.ClassVariable e) {			
		// do nothing!
	}
	
	protected void checkUnOp(Expr.UnOp uop) {		
		checkExpression(uop.expr());
		
		Type e_t = uop.expr().attribute(Type.class);
		
		switch(uop.op()) {
			case UnOp.NEG:
				if (!(e_t instanceof Type.Byte 
						|| e_t instanceof Type.Char
						|| e_t instanceof Type.Short 
						|| e_t instanceof Type.Int
						|| e_t instanceof Type.Long
						|| e_t instanceof Type.Float
						|| e_t instanceof Type.Double)) {	
					syntax_error("cannot negate type " + e_t, uop);					
				}
				break;
			case UnOp.NOT:
				if (!(e_t instanceof Type.Bool)) {				
					syntax_error("required type boolean, found " + e_t, uop);					
				}
				break;
			case UnOp.INV:
				if (!(e_t instanceof Type.Byte 
						|| e_t instanceof Type.Char
						|| e_t instanceof Type.Short 
						|| e_t instanceof Type.Int
						|| e_t instanceof Type.Long)) {
					syntax_error("cannot invert type " + e_t, uop);					
				}
				break;								
			}	
	}
		
	protected void checkBinOp(Expr.BinOp e) {				
		checkExpression(e.lhs());
		checkExpression(e.rhs());
		
		Type lhs_t = e.lhs().attribute(Type.class);
		Type rhs_t = e.rhs().attribute(Type.class);
		Type e_t = e.attribute(Type.class);		

		if ((lhs_t instanceof Type.Primitive || rhs_t instanceof Type.Primitive)
				&& !lhs_t.equals(rhs_t)) {
			if ((lhs_t instanceof Type.Long
					|| lhs_t instanceof Type.Int
					|| lhs_t instanceof Type.Short
					|| lhs_t instanceof Type.Char || lhs_t instanceof Type.Byte)
					&& rhs_t instanceof Type.Int
					&& (e.op() == BinOp.SHL || e.op() == BinOp.SHR || e.op() == BinOp.USHR)) {
				return; // Ok!
			} else if((isJavaLangString(lhs_t) || isJavaLangString(rhs_t)) && e.op() == BinOp.CONCAT) {
				return; // OK					
			}
		} else if((lhs_t instanceof Type.Char || lhs_t instanceof Type.Byte 
				|| lhs_t instanceof Type.Int || lhs_t instanceof Type.Long 
				|| lhs_t instanceof Type.Short || lhs_t instanceof Type.Float
				|| lhs_t instanceof Type.Double) && 
				
				(rhs_t instanceof Type.Char || rhs_t instanceof Type.Byte
						|| rhs_t instanceof Type.Int || rhs_t instanceof Type.Long 
						|| rhs_t instanceof Type.Short || rhs_t instanceof Type.Float
						|| rhs_t instanceof Type.Double)) {
			
			switch(e.op()) {
				// easy cases first
				case BinOp.EQ:
				case BinOp.NEQ:
				case BinOp.LT:
				case BinOp.LTEQ:
				case BinOp.GT:
				case BinOp.GTEQ:
					// need more checks here
					if(!(e_t instanceof Type.Bool)) {
						syntax_error("required type boolean, found "
								+ rhs_t,e);								
					}
					return;
				case BinOp.ADD:
				case BinOp.SUB:
				case BinOp.MUL:
				case BinOp.DIV:
				case BinOp.MOD:
				{
					// hmmmm ?
					return;
				}
				case BinOp.SHL:
				case BinOp.SHR:
				case BinOp.USHR:
				{										
					// bit-shift operations always take an int as their rhs, so
                    // make sure we have an int type
					if (lhs_t instanceof Type.Float
							|| lhs_t instanceof Type.Double) {
						syntax_error("Invalid operation on type "
								+ lhs_t, e);
					} else if (!(rhs_t instanceof Type.Int)) {
						syntax_error("Invalid operation on type "
								+ rhs_t, e);
					} 
					return;
				}
				case BinOp.AND:
				case BinOp.OR:
				case BinOp.XOR:
				{
					if (rhs_t instanceof Type.Float || rhs_t instanceof Type.Double) {
						syntax_error("Invalid operation on type " + rhs_t, e);
					}
					return;
				}					
			}
		} else if(lhs_t instanceof Type.Bool && rhs_t instanceof Type.Bool) {
			switch(e.op()) {
			case BinOp.LOR:
			case BinOp.LAND:				
			case BinOp.AND:
			case BinOp.OR:
			case BinOp.XOR:
			case BinOp.EQ:
			case BinOp.NEQ:
				return; // OK							
			}
		} else if((isJavaLangString(lhs_t) || isJavaLangString(rhs_t)) && e.op() == Expr.BinOp.CONCAT) {
			return; // OK
		} else if (lhs_t instanceof Type.Reference
				&& rhs_t instanceof Type.Reference
				&& (e.op() == Expr.BinOp.EQ || e.op() == Expr.BinOp.NEQ)) {
			return;
		} 
		
		syntax_error("operand types do not go together: " + lhs_t + ", " + rhs_t,e);		
	}
	
	protected void checkTernOp(Expr.TernOp e) {		
		checkExpression(e.condition());
		checkExpression(e.trueBranch());
		checkExpression(e.falseBranch());
		
		Type c_t = e.condition().attribute(Type.class);

		if (!(c_t instanceof Type.Bool)) {
			syntax_error("required type boolean, found " + c_t, e);
		}		
	}		
	
	protected Decl getEnclosingScope(Class c) {
		for(int i=enclosingScopes.size()-1;i>=0;--i) {
			Decl d = enclosingScopes.get(i);
			if(c.isInstance(d)) {
				return d;
			}
		}
		return null;
	}	
}
