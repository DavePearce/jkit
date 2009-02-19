package jkit.java.stages;

import java.util.ArrayList;
import java.util.List;

import jkit.compiler.FieldNotFoundException;
import jkit.compiler.MethodNotFoundException;
import jkit.compiler.SyntaxError;
import jkit.java.io.JavaFile;
import jkit.java.tree.Decl;
import jkit.java.tree.Expr;
import jkit.java.tree.Stmt;
import jkit.java.tree.Value;
import jkit.java.tree.Decl.Clazz;
import jkit.java.tree.Decl.Field;
import jkit.java.tree.Decl.Interface;
import jkit.java.tree.Decl.Method;
import jkit.java.tree.Stmt.Case;
import jkit.jil.SourceLocation;
import jkit.jil.SyntacticElement;
import jkit.jil.Type;
import jkit.util.Pair;
import jkit.util.Triple;

/**
 * The purpose of the skeleton builder is to construct skeletons from the parsed
 * source file. A skeleton comes in one of two forms:
 * <ol>
 * <li>Simply an empty class. The reason these exist, is that we need someway
 * to extract all of the classes which are defined in a particular source file.
 * This is crucial for the type resolution phase.</li>
 * <li>A class with fully qualified types, fields and methods. In particular,
 * all superclasses, interfaces, fields, methods and their parameters are
 * identified. However, all forms of executable code are not. For example, no
 * field initialisers or method bodies are present.</li>
 * </ol>
 * 
 * @author djp
 * 
 */
public class SkeletonInitialiser {
	private int anonymousClassCount = 1;
	
	public void apply(JavaFile file) {		
		// Now, traverse the declarations
		for(Decl d : file.declarations()) {
			doDeclaration(d);
		}
	}
	
	protected void doDeclaration(Decl d) {
		if(d instanceof Interface) {
			doInterface((Interface)d);
		} else if(d instanceof Clazz) {
			doClass((Clazz)d);
		} else if(d instanceof Method) {
			doMethod((Method)d);
		} else if(d instanceof Field) {
			doField((Field)d);
		}
	}
	
	protected void doInterface(Interface d) {
		doClass(d);
	}
	
	protected void doClass(Clazz c) {
		for(Decl d : c.declarations()) {
			doDeclaration(d);
		}		
	}

	protected void doMethod(Method d) {
		doStatement(d.body());
	}

	protected void doField(Field d) {
		doExpression(d.initialiser());
	}
	
	protected void doStatement(Stmt e) {
		if(e instanceof Stmt.SynchronisedBlock) {
			doSynchronisedBlock((Stmt.SynchronisedBlock)e);
		} else if(e instanceof Stmt.TryCatchBlock) {
			doTryCatchBlock((Stmt.TryCatchBlock)e);
		} else if(e instanceof Stmt.Block) {
			doBlock((Stmt.Block)e);
		} else if(e instanceof Stmt.VarDef) {
			doVarDef((Stmt.VarDef) e);
		} else if(e instanceof Stmt.Assignment) {
			doAssignment((Stmt.Assignment) e);
		} else if(e instanceof Stmt.Return) {
			doReturn((Stmt.Return) e);
		} else if(e instanceof Stmt.Throw) {
			doThrow((Stmt.Throw) e);
		} else if(e instanceof Stmt.Assert) {
			doAssert((Stmt.Assert) e);
		} else if(e instanceof Stmt.Break) {
			doBreak((Stmt.Break) e);
		} else if(e instanceof Stmt.Continue) {
			doContinue((Stmt.Continue) e);
		} else if(e instanceof Stmt.Label) {
			doLabel((Stmt.Label) e);
		} else if(e instanceof Stmt.If) {
			doIf((Stmt.If) e);
		} else if(e instanceof Stmt.For) {
			doFor((Stmt.For) e);
		} else if(e instanceof Stmt.ForEach) {
			doForEach((Stmt.ForEach) e);
		} else if(e instanceof Stmt.While) {
			doWhile((Stmt.While) e);
		} else if(e instanceof Stmt.DoWhile) {
			doDoWhile((Stmt.DoWhile) e);
		} else if(e instanceof Stmt.Switch) {
			doSwitch((Stmt.Switch) e);
		} else if(e instanceof Expr.Invoke) {
			doInvoke((Expr.Invoke) e);
		} else if(e instanceof Expr.New) {
			doNew((Expr.New) e);
		} else if(e instanceof Decl.Clazz) {
			doClass((Decl.Clazz)e);
		} else if(e != null) {
			syntax_error("Internal failure (invalid statement \""
					+ e.getClass() + "\" encountered)", e);			
		}		
	}
	
	protected void doBlock(Stmt.Block block) {
		if(block != null) {			
			// now process every statement in this block.
			for(Stmt s : block.statements()) {
				doStatement(s);
			}
		}
	}
	
	protected void doSynchronisedBlock(Stmt.SynchronisedBlock block) {
		doBlock(block);
		doExpression(block.expr());
	}
	
	protected void doTryCatchBlock(Stmt.TryCatchBlock block) {
		doBlock(block);
		doBlock(block.finaly());

		for (Stmt.CatchBlock cb : block.handlers()) {			
			doBlock(cb);
		}
	}
	
	protected void doVarDef(Stmt.VarDef def) {
		List<Triple<String, Integer, Expr>> defs = def.definitions();
		for(int i=0;i!=defs.size();++i) {			
			doExpression(defs.get(i).third());			
		}
	}
	
	protected void doAssignment(Stmt.Assignment def) {
		doExpression(def.lhs());	
		doExpression(def.rhs());			
	}
	
	protected void doReturn(Stmt.Return ret) {
		doExpression(ret.expr());
	}
	
	protected void doThrow(Stmt.Throw ret) {
		doExpression(ret.expr());
	}
	
	protected void doAssert(Stmt.Assert ret) {
		doExpression(ret.expr());
	}
	
	protected void doBreak(Stmt.Break brk) {
		// nothing	
	}
	
	protected void doContinue(Stmt.Continue brk) {
		// nothing
	}
	
	protected void doLabel(Stmt.Label lab) {						
		doStatement(lab.statement());
	}
	
	protected void doIf(Stmt.If stmt) {
		doExpression(stmt.condition());
		doStatement(stmt.trueStatement());
		doStatement(stmt.falseStatement());
	}
	
	protected void doWhile(Stmt.While stmt) {
		doExpression(stmt.condition());
		doStatement(stmt.body());		
	}
	
	protected void doDoWhile(Stmt.DoWhile stmt) {
		doExpression(stmt.condition());
		doStatement(stmt.body());
	}
	
	protected void doFor(Stmt.For stmt) {		
		doStatement(stmt.initialiser());
		doExpression(stmt.condition());
		doStatement(stmt.increment());
		doStatement(stmt.body());	
	}
	
	protected void doForEach(Stmt.ForEach stmt) {
		doExpression(stmt.source());
		doStatement(stmt.body());
	}
	
	protected void doSwitch(Stmt.Switch sw) {
		doExpression(sw.condition());
		for(Case c : sw.cases()) {
			doExpression(c.condition());
			for(Stmt s : c.statements()) {
				doStatement(s);
			}
		}
		
		// should check that case conditions are final constants here.
	}
	
	protected void doExpression(Expr e) {	
		if(e instanceof Value.Bool) {
			doBoolVal((Value.Bool)e);
		} else if(e instanceof Value.Char) {
			doCharVal((Value.Char)e);
		} else if(e instanceof Value.Int) {
			doIntVal((Value.Int)e);
		} else if(e instanceof Value.Long) {
			doLongVal((Value.Long)e);
		} else if(e instanceof Value.Float) {
			doFloatVal((Value.Float)e);
		} else if(e instanceof Value.Double) {
			doDoubleVal((Value.Double)e);
		} else if(e instanceof Value.String) {
			doStringVal((Value.String)e);
		} else if(e instanceof Value.Null) {
			doNullVal((Value.Null)e);
		} else if(e instanceof Value.TypedArray) {
			doTypedArrayVal((Value.TypedArray)e);
		} else if(e instanceof Value.Array) {
			doArrayVal((Value.Array)e);
		} else if(e instanceof Value.Class) {
			doClassVal((Value.Class) e);
		} else if(e instanceof Expr.LocalVariable) {
			doLocalVariable((Expr.LocalVariable)e);
		} else if(e instanceof Expr.NonLocalVariable) {
			doNonLocalVariable((Expr.NonLocalVariable)e);
		} else if(e instanceof Expr.ClassVariable) {
			doClassVariable((Expr.ClassVariable)e);
		} else if(e instanceof Expr.UnOp) {
			doUnOp((Expr.UnOp)e);
		} else if(e instanceof Expr.BinOp) {
			doBinOp((Expr.BinOp)e);
		} else if(e instanceof Expr.TernOp) {
			doTernOp((Expr.TernOp)e);
		} else if(e instanceof Expr.Cast) {
			doCast((Expr.Cast)e);
		} else if(e instanceof Expr.InstanceOf) {
			doInstanceOf((Expr.InstanceOf)e);
		} else if(e instanceof Expr.Invoke) {
			doInvoke((Expr.Invoke) e);
		} else if(e instanceof Expr.New) {
			doNew((Expr.New) e);
		} else if(e instanceof Expr.ArrayIndex) {
			doArrayIndex((Expr.ArrayIndex) e);
		} else if(e instanceof Expr.Deref) {
			doDeref((Expr.Deref) e);
		} else if(e instanceof Stmt.Assignment) {
			// force brackets			
			doAssignment((Stmt.Assignment) e);			
		} else if(e != null) {
			syntax_error("Internal failure (invalid expression \""
					+ e.getClass() + "\" encountered)", e);			
		}
	}
	
	protected void doDeref(Expr.Deref e) {		
		doExpression(e.target());					
	}
	
	protected void doArrayIndex(Expr.ArrayIndex e) {
		doExpression(e.target());
		doExpression(e.index());		
	}
	
	protected void doNew(Expr.New e) {
		// Second, recurse through any parameters supplied ...
		for(Expr p : e.parameters()) {
			doExpression(p);
		}
		
		// Third, check whether this is constructing an anonymous class ...
		for(Decl d : e.declarations()) {
			doDeclaration(d);
		}
	}
	
	protected void doInvoke(Expr.Invoke e) {
		doExpression(e.target());
		
		for(Expr p : e.parameters()) {
			doExpression(p);
		}
	}
	
	protected void doInstanceOf(Expr.InstanceOf e) {		
		doExpression(e.lhs());		
	}
	
	protected void doCast(Expr.Cast e) {
		doExpression(e.expr());
	}
	
	protected void doBoolVal(Value.Bool e) {		
	}
	
	protected void doCharVal(Value.Char e) {		
	}
	
	protected void doIntVal(Value.Int e) {		
	}
	
	protected void doLongVal(Value.Long e) {				
	}
	
	protected void doFloatVal(Value.Float e) {				
	}
	
	protected void doDoubleVal(Value.Double e) {				
	}
	
	protected void doStringVal(Value.String e) {				
	}
	
	protected void doNullVal(Value.Null e) {		
	}
	
	protected void doTypedArrayVal(Value.TypedArray e) {		
	}
	
	protected void doArrayVal(Value.Array e) {		
	}
	
	protected void doClassVal(Value.Class e) {
	}
	
	protected void doLocalVariable(Expr.LocalVariable e) {		
	}

	protected void doNonLocalVariable(Expr.NonLocalVariable e) {
	}
	
	protected void doClassVariable(Expr.ClassVariable e) {	
	}
	
	protected void doUnOp(Expr.UnOp e) {		
		doExpression(e.expr());		
	}
		
	protected void doBinOp(Expr.BinOp e) {				
		doExpression(e.lhs());
		doExpression(e.rhs());		
	}
	
	protected void doTernOp(Expr.TernOp e) {		
		doExpression(e.condition());
		doExpression(e.falseBranch());
		doExpression(e.trueBranch());		
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
