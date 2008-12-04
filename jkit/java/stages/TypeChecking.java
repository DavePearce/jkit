package jkit.java.stages;

import jkit.java.*;
import jkit.java.Decl.*;
import jkit.java.Expr.*;
import jkit.java.Stmt.*;
import jkit.jil.Type;

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
	
	protected void checkInterface(Interface d) {
		
	}
	
	protected void checkClass(Clazz c) {
		for(Decl d : c.declarations()) {
			checkDeclaration(d);
		}
	}

	protected void checkMethod(Method d) {
		checkStatement(d.body());
	}

	protected void checkField(Field d) {

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
		} else {
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
		
	}
	
	protected void checkTryCatchBlock(Stmt.TryCatchBlock block) {
		
	}
	
	protected void checkVarDef(Stmt.VarDef def) {
		
	}
	
	protected void checkAssignment(Stmt.Assignment def) {
		checkExpression(def.lhs());	
		checkExpression(def.rhs());			
		
		System.out.println("LHS: " + def.lhs().attribute(Type.class));
		System.out.println("RHS: " + def.rhs().attribute(Type.class));
	}
	
	protected void checkReturn(Stmt.Return ret) {
		
	}
	
	protected void checkThrow(Stmt.Throw ret) {
						
	}
	
	protected void checkAssert(Stmt.Assert ret) {
								
	}
	
	protected void checkBreak(Stmt.Break brk) {
			
	}
	
	protected void checkContinue(Stmt.Continue brk) {
			
	}
	
	protected void checkLabel(Stmt.Label lab) {				

	}
	
	protected void checkIf(Stmt.If stmt) {
		
	}
	
	protected void checkWhile(Stmt.While stmt) {
			
	}
	
	protected void checkDoWhile(Stmt.DoWhile stmt) {
		
	}
	
	protected void checkFor(Stmt.For stmt) {
		
	}
	
	protected void checkForEach(Stmt.ForEach stmt) {
		
	}
	
	protected void checkSwitch(Stmt.Switch s) {
		
	}
	
	protected void checkExpression(Expr e) {	
		if(e instanceof Value.Bool) {
			checkBoolVal((Value.Bool)e);
		} else if(e instanceof Value.Char) {
			checkCharVal((Value.Char)e);
		} else if(e instanceof Value.Int) {
			checkIntVal((Value.Int)e);
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
			// force brackets			
			checkAssignment((Stmt.Assignment) e);			
		} else {
			throw new RuntimeException("Invalid expression encountered: "
					+ e.getClass());
		}
	}
	
	protected void checkDeref(Expr.Deref e) {
			
	}
	
	protected void checkArrayIndex(Expr.ArrayIndex e) {
		
	}
	
	protected void checkNew(Expr.New e) {
		
	}
	
	protected void checkInvoke(Expr.Invoke e) {
		
	}
	
	protected void checkInstanceOf(Expr.InstanceOf e) {		
			
	}
	
	protected void checkCast(Expr.Cast e) {
	
	}
	
	protected void checkBoolVal(Value.Bool e) {
		
	}
	
	protected void checkCharVal(Value.Char e) {
		
	}
	
	protected void checkIntVal(Value.Int e) {
		
	}
	
	protected void checkLongVal(Value.Long e) {		
		
	}
	
	protected void checkFloatVal(Value.Float e) {		
		
	}
	
	protected void checkDoubleVal(Value.Double e) {		
		
	}
	
	protected void checkStringVal(Value.String e) {		
		
	}
	
	protected void checkNullVal(Value.Null e) {		
		
	}
	
	protected void checkTypedArrayVal(Value.TypedArray e) {		
		
	}
	
	protected void checkArrayVal(Value.Array e) {		
		// not sure what to check here.
	}
	
	protected void checkClassVal(Value.Class e) {
		
	}
	
	protected void checkVariable(Expr.Variable e) {			
		
	}

	protected void checkUnOp(Expr.UnOp e) {		
		
	}
		
	protected void checkBinOp(Expr.BinOp e) {				
		
	}
	
	protected void checkTernOp(Expr.TernOp e) {		
		
	}
}
