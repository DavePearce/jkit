package jkit.java.stages;

import java.util.*;

import jkit.compiler.SyntaxError;
import jkit.java.*;
import jkit.java.Decl.*;
import jkit.java.Expr.*;
import jkit.java.Stmt.*;
import jkit.jil.Type;
import jkit.jil.SyntacticElement;
import jkit.jil.SourceLocation;
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
	
	public TypeChecking(ClassLoader loader) {
		this.loader = loader; 
	}
	
	public void apply(JavaFile file) {
		for(Decl d : file.declarations()) {
			checkDeclaration(d);
		}
	}
	
	protected void checkDeclaration(Decl d) {
		if(d instanceof Interface) {
			checkInterface((Interface)d);
		} else if(d instanceof Clazz) {
			checkClass((Clazz)d);
		} else if(d instanceof Method) {
			checkMethod((Method)d);
		} else if(d instanceof Field) {
			checkField((Field)d);
		}
	}
	
	protected void checkInterface(Interface c) {
		enclosingScopes.push(c);
		
		for(Decl d : c.declarations()) {
			checkDeclaration(d);
		}
		
		enclosingScopes.pop();
	}
	
	protected void checkClass(Clazz c) {
		enclosingScopes.push(c);
		
		for(Decl d : c.declarations()) {
			checkDeclaration(d);
		}
		
		enclosingScopes.pop();
	}

	protected void checkMethod(Method d) {
		enclosingScopes.push(d);
		
		checkStatement(d.body());
		
		enclosingScopes.pop();
	}

	protected void checkField(Field d) {
		checkExpression(d.initialiser());
	}
	
	protected void checkStatement(Stmt e) {
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
		} else if(e instanceof Decl.Clazz) {
			checkClass((Decl.Clazz)e);
		} else if(e != null) {
			throw new RuntimeException("Invalid statement encountered: "
					+ e.getClass());
		}		
	}
	
	protected void checkBlock(Stmt.Block block) {		
		for(Stmt s : block.statements()) {
			checkStatement(s);
		}
	}
	
	protected void checkSynchronisedBlock(Stmt.SynchronisedBlock block) {
		checkBlock(block);
	}
	
	protected void checkTryCatchBlock(Stmt.TryCatchBlock block) {
		checkBlock(block);
	}
	
	protected void checkVarDef(Stmt.VarDef def) {
		Type t = def.type();
		
		for(Triple<String, Integer, Expr> d : def.definitions()) {								
			if(d.third() != null) {
				checkExpression(d.third());

				Type nt = t;
				for(int i=0;i!=d.second();++i) {
					nt = new Type.Array(nt);
				}

				Type i_t = (Type) d.third().attribute(Type.class);

				if (!subtype(nt, i_t)) {
					syntax_error("required type " + nt + ", found type " + i_t, def);
				}
			}
		}
	}
	
	protected void checkAssignment(Stmt.Assignment def) {
		checkExpression(def.lhs());	
		checkExpression(def.rhs());					
		
		Type lhs_t = (Type) def.lhs().attribute(Type.class);
		Type rhs_t = (Type) def.rhs().attribute(Type.class);
					
		if (!subtype(lhs_t, rhs_t)) {
			syntax_error("required type " + lhs_t + ", found type "
					+ rhs_t, def);
		} 		
	}
	
	protected void checkReturn(Stmt.Return ret) {		
		Method method = (Method) getEnclosingScope(Method.class);
		
		if(ret.expr() != null) { 
			checkExpression(ret.expr());						
			
			Type ret_t = (Type) ret.expr().attribute(Type.class);
			
			if(ret_t.equals(new Type.Void())) {
				syntax_error(
						"cannot return a value from method whose result type is void",
						ret);	
			} else if(!subtype(method.returnType(),ret_t)) {
				syntax_error("required return type " + method.returnType()
						+ ",  found type " + ret_t , ret);	
			}
			
		} else if(!(method.returnType() instanceof Type.Void)) {
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
				
		Type c_t = (Type) stmt.condition().attribute(Type.class);
		
		if(!(c_t instanceof Type.Bool)) {
			syntax_error("required type boolean, found " + c_t, stmt);								
		}
	}
	
	protected void checkWhile(Stmt.While stmt) {
		checkExpression(stmt.condition());
		checkStatement(stmt.body());

		Type c_t = (Type) stmt.condition().attribute(Type.class);

		if (!(c_t instanceof Type.Bool)) {
			syntax_error("required type boolean, found " + c_t, stmt);
		}
	}
	
	protected void checkDoWhile(Stmt.DoWhile stmt) {
		checkExpression(stmt.condition());
		checkStatement(stmt.body());

		Type c_t = (Type) stmt.condition().attribute(Type.class);

		if (!(c_t instanceof Type.Bool)) {
			syntax_error("required type boolean, found " + c_t, stmt);			
		}
	}
	
	protected void checkFor(Stmt.For stmt) {
		checkStatement(stmt.initialiser());
		checkExpression(stmt.condition());
		checkStatement(stmt.increment());
		checkStatement(stmt.body());

		Type c_t = (Type) stmt.condition().attribute(Type.class);

		if (!(c_t instanceof Type.Bool)) {
			syntax_error("required type boolean, found " + c_t, stmt);			
		}
	}
	
	protected void checkForEach(Stmt.ForEach stmt) {		
		checkExpression(stmt.source());
		checkStatement(stmt.body());

		// need to check that the static type of the source expression
		// implements java.lang.iterable
		Type s_t = (Type) stmt.source().attribute(Type.class);

		if (!subtype(new Type.Clazz("java.lang", "Iterable"), s_t)) {
			syntax_error("foreach not applicable to expression type",stmt);
		}
	}
	
	protected void checkSwitch(Stmt.Switch sw) {
		checkExpression(sw.condition());
		for(Case c : sw.cases()) {
			checkExpression(c.condition());
			for(Stmt s : c.statements()) {
				checkStatement(s);
			}
		}
	}
	
	protected void checkExpression(Expr e) {	
		if(e instanceof Value.Bool) {
			checkBoolVal((Value.Bool)e);
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
		} else if(e instanceof Expr.Variable) {
			checkVariable((Expr.Variable)e);
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
	}
	
	protected void checkDeref(Expr.Deref e) {
		checkExpression(e.target());
		
		// here, we need to check that the field in question actually exists!
	}
	
	protected void checkArrayIndex(Expr.ArrayIndex e) {
		checkExpression(e.index());
		checkExpression(e.target());
		
		Type i_t = (Type) e.index().attribute(Type.class);
		
		if(!(i_t instanceof Type.Int)) {
			syntax_error("required type int, found type " + i_t, e);
		}
		
		Type t_t = (Type) e.target().attribute(Type.class);		
		if(!(t_t instanceof Type.Array)) {			
			syntax_error("array required, but " + t_t + " found", e);
		}
	}
	
	protected void checkNew(Expr.New e) {
		for(Decl d : e.declarations()) {
			checkDeclaration(d);
		}
	}
	
	protected void checkInvoke(Expr.Invoke e) {
		for(Expr p : e.parameters()) {
			checkExpression(p);
		}
	}
	
	protected void checkInstanceOf(Expr.InstanceOf e) {		
		checkExpression(e.lhs());
		
		Type lhs_t = (Type) e.lhs().attribute(Type.class);
		
		if(lhs_t instanceof Type.Primitive) {
			syntax_error("required reference type, found " + lhs_t , e);			
		}
	}
	
	protected void checkCast(Expr.Cast e) {
		Type e_t = (Type) e.expr().attribute(Type.class);
		
		if(!subtype(e.type(),e_t)) {
			syntax_error("inconvertible types: " + e_t + ", " + e.type(), e);
		}
	}
	
	protected void checkConvert(Expr.Convert e) {
		Type rhs_t = (Type) e.expr().attribute(Type.class);
		if(!subtype(e.type(),rhs_t)) {
			SourceLocation loc = (SourceLocation) e.attribute(SourceLocation.class);
			if(rhs_t instanceof Type.Primitive) {
				throw new SyntaxError("possible loss of precision",loc.line(),loc.column());
			} else {
				throw new SyntaxError("incompatible types",loc.line(),loc.column());
			}
		}
	}
	
	protected void checkBoolVal(Value.Bool e) {
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
	
	protected void checkVariable(Expr.Variable e) {			
		// do nothing!
	}

	protected void checkUnOp(Expr.UnOp uop) {		
		checkExpression(uop.expr());
		
		Type e_t = (Type) uop.expr().attribute(Type.class);
		
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
		
		Type lhs_t = (Type) e.lhs().attribute(Type.class);
		Type rhs_t = (Type) e.rhs().attribute(Type.class);
		Type e_t = (Type) e.attribute(Type.class);
		SourceLocation loc = (SourceLocation) e.attribute(SourceLocation.class);
		
		if ((lhs_t instanceof Type.Primitive || rhs_t instanceof Type.Primitive)
				&& !lhs_t.equals(rhs_t)) {
			if ((lhs_t instanceof Type.Long
					|| lhs_t instanceof Type.Int
					|| lhs_t instanceof Type.Short
					|| lhs_t instanceof Type.Char || lhs_t instanceof Type.Byte)
					&& rhs_t instanceof Type.Int
					&& (e.op() == BinOp.SHL || e.op() == BinOp.SHR || e.op() == BinOp.USHR)) {
				// Ok!
			} else throw new SyntaxError("Operand types do not go together: "
					+ rhs_t + " and " + lhs_t,loc.line(),loc.column());			
		}
		
		if((lhs_t instanceof Type.Char || lhs_t instanceof Type.Byte 
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
						throw new SyntaxError("required type boolean, found "
								+ rhs_t,loc.line(),loc.column());								
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
					// bit-shift operations always take an int as their rhs, so
                    // make sure we have an int type
					if (lhs_t instanceof Type.Float
							|| lhs_t instanceof Type.Double) {
						throw new SyntaxError("Invalid operation on type "
								+ lhs_t , loc.line(), loc.column());
					} else if (!(rhs_t instanceof Type.Int)) {
						throw new SyntaxError("Invalid operation on type "
								+ rhs_t , loc.line(), loc.column());
					} 
					break;
				}
				case BinOp.AND:
				case BinOp.OR:
				case BinOp.XOR:
				{														
					if (rhs_t instanceof Type.Float
							|| rhs_t instanceof Type.Double) {
						throw new SyntaxError("Invalid operation on type "
								+ rhs_t , loc.line(), loc.column());
					} 
				}					
			}
		}
	}
	
	protected void checkTernOp(Expr.TernOp e) {		
		checkExpression(e.condition());
		checkExpression(e.trueBranch());
		checkExpression(e.falseBranch());
		
		Type c_t = (Type) e.condition().attribute(Type.class);

		if (!(c_t instanceof Type.Bool)) {
			syntax_error("required type boolean, found " + c_t, e);
		}		
	}
	
	/**
     * This method determines wether t2 is a (non-strict) subtype of t2. In the
     * case of primitive types, this is relatively easy to determine. However,
     * in the case of reference types it is harder as we must navigate the class
     * heirarchy to determine this.
     * 
     * JLS 4.10.1 states that subtyping between primitives looks like this:
     * 
     * <pre>
     *   double :&gt; float 
     *   float :&gt; long
     *   long :&gt; int
     *   int :&gt; char 
     *   int :&gt; short 
     *   short :&gt; byte
     * </pre>
     * 
     * @param t1
     * @param t2
     * @return
     */
	protected boolean subtype(Type t1, Type t2) {			
		if (t1.equals(t2)) { return true; }				
		if(t1 instanceof Type.Primitive && t2 instanceof Type.Primitive) {			
			// First, do all (non-trivial) primitive subtyping options			
			if(t1 instanceof Type.Double && subtype(new Type.Float(),t2)) { 
				return true;
			} else if(t1 instanceof Type.Float && subtype(new Type.Long(),t2)) {
				return true;
			} else if(t1 instanceof Type.Long && subtype(new Type.Int(),t2)) {
				return true;
			} else if(t1 instanceof Type.Int && subtype(new Type.Short(),t2)) {
				return true;
			} else if(t1 instanceof Type.Int && t2 instanceof Type.Char) {
				return true;
			} else if (t1 instanceof Type.Short && t2 instanceof Type.Byte) {
				return true;
			}					
		} else {
			// Second, consider subtyping relationships between references			
		}
				
		return false;
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
	
	/**
     * This method is just to factor out the code for looking up the source
     * location and throwing an exception based on that.
     * 
     * @param msg --- the error message
     * @param e --- the syntactic element causing the error
     */
	protected void syntax_error(String msg, SyntacticElement e) {
		SourceLocation loc = (SourceLocation) e.attribute(SourceLocation.class);
		throw new SyntaxError(msg,loc.line(),loc.column());
	}
}
