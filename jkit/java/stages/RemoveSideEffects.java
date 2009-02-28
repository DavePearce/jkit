package jkit.java.stages;

import java.util.*;

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
import jkit.util.Triple;

/**
 * <p>
 * The aim of this stage is to eliminate all side-effects from expressions. For
 * example, the following code:
 * </p>
 * 
 * <pre>
 * int x = 0;
 * int y = ++x + --x;
 * </pre>
 * 
 * <p>
 * will be translated into this:
 * </p>
 * 
 * <pre>
 * int x = 0;
 * x = x + 1;
 * int __jkit_tmp_1 = x;
 * x = x - 1;
 * int y = __jkit_tmp_1 + x;
 * </pre>
 * 
 * <p>
 * Note, method calls are not eliminate from expressions, even though they may
 * well have side-effects.
 * </p>
 * 
 * @author djp
 * 
 */
public class RemoveSideEffects {

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
		} else if (d instanceof Decl.InitialiserBlock) {
			doInitialiserBlock((Decl.InitialiserBlock) d);
		} else if (d instanceof Decl.StaticInitialiserBlock) {
			doStaticInitialiserBlock((Decl.StaticInitialiserBlock) d);
		} else {
			syntax_error("internal failure (unknown declaration \"" + d
					+ "\" encountered)",d);
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
		d.setInitialiser(doExpression(d.initialiser()));		
	}
	
	protected void doInitialiserBlock(Decl.InitialiserBlock d) {
		for (Stmt s : d.statements()) {
			doStatement(s);
		}	
	}
	
	protected void doStaticInitialiserBlock(Decl.StaticInitialiserBlock d) {		
		for (Stmt s : d.statements()) {
			doStatement(s);
		}		
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
			syntax_error("Invalid statement encountered: "
					+ e.getClass(),e);
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
	
	protected void doCatchBlock(Stmt.CatchBlock block) {
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
		
		for(Stmt.CatchBlock cb : block.handlers()) {
			doCatchBlock(cb);
		}
	}
	
	protected void doVarDef(Stmt.VarDef def) {
		List<Triple<String, Integer, Expr>> defs = def.definitions();
		for(int i=0;i!=defs.size();++i) {
			Triple<String, Integer, Expr> d = defs.get(i);
			Expr e = doExpression(d.third());
			defs.set(i, new Triple(d.first(),d.second(),e));			
		}		
	}
	
	protected Expr doAssignment(Stmt.Assignment def) {
		def.setLhs(doExpression(def.lhs()));	
		def.setRhs(doExpression(def.rhs()));
		return def;
	}
	
	protected void doReturn(Stmt.Return ret) {
		ret.setExpr(doExpression(ret.expr()));
	}
	
	protected void doThrow(Stmt.Throw ret) {
		ret.setExpr(doExpression(ret.expr()));
	}
	
	protected void doAssert(Stmt.Assert ret) {
		ret.setExpr(doExpression(ret.expr()));
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
		stmt.setCondition(doExpression(stmt.condition()));
		doStatement(stmt.trueStatement());
		doStatement(stmt.falseStatement());
	}
	
	protected void doWhile(Stmt.While stmt) {
		stmt.setCondition(doExpression(stmt.condition()));
		doStatement(stmt.body());		
	}
	
	protected void doDoWhile(Stmt.DoWhile stmt) {
		stmt.setCondition(doExpression(stmt.condition()));
		doStatement(stmt.body());
	}
	
	protected void doFor(Stmt.For stmt) {
		doStatement(stmt.initialiser());
		stmt.setCondition(doExpression(stmt.condition()));
		doStatement(stmt.increment());
		doStatement(stmt.body());		
	}
	
	protected void doForEach(Stmt.ForEach stmt) {
		stmt.setSource(doExpression(stmt.source()));
		doStatement(stmt.body());		
	}
	
	protected void doSwitch(Stmt.Switch sw) {
		sw.setCondition(doExpression(sw.condition()));
		for(Case c : sw.cases()) {
			c.setCondition(doExpression(c.condition()));
			for(Stmt s : c.statements()) {
				doStatement(s);
			}
		}
		
		// should check that case conditions are final constants here.
	}
	
	protected Expr doExpression(Expr e) {	
		if(e instanceof Value.Bool) {
			return doBoolVal((Value.Bool)e);
		} else if(e instanceof Value.Char) {
			return doCharVal((Value.Char)e);
		} else if(e instanceof Value.Int) {
			return doIntVal((Value.Int)e);
		} else if(e instanceof Value.Long) {
			return doLongVal((Value.Long)e);
		} else if(e instanceof Value.Float) {
			return doFloatVal((Value.Float)e);
		} else if(e instanceof Value.Double) {
			return doDoubleVal((Value.Double)e);
		} else if(e instanceof Value.String) {
			return doStringVal((Value.String)e);
		} else if(e instanceof Value.Null) {
			return doNullVal((Value.Null)e);
		} else if(e instanceof Value.TypedArray) {
			return doTypedArrayVal((Value.TypedArray)e);
		} else if(e instanceof Value.Array) {
			return doArrayVal((Value.Array)e);
		} else if(e instanceof Value.Class) {
			return doClassVal((Value.Class) e);
		} else if(e instanceof Expr.LocalVariable) {
			doLocalVariable((Expr.LocalVariable)e);
		} else if(e instanceof Expr.NonLocalVariable) {
			doNonLocalVariable((Expr.NonLocalVariable)e);
		} else if(e instanceof Expr.UnOp) {
			return doUnOp((Expr.UnOp)e);
		} else if(e instanceof Expr.BinOp) {
			return doBinOp((Expr.BinOp)e);
		} else if(e instanceof Expr.TernOp) {
			return doTernOp((Expr.TernOp)e);
		} else if(e instanceof Expr.Cast) {
			return doCast((Expr.Cast)e);
		} else if(e instanceof Expr.InstanceOf) {
			return doInstanceOf((Expr.InstanceOf)e);
		} else if(e instanceof Expr.Invoke) {
			return doInvoke((Expr.Invoke) e);
		} else if(e instanceof Expr.New) {
			return doNew((Expr.New) e);
		} else if(e instanceof Expr.ArrayIndex) {
			return doArrayIndex((Expr.ArrayIndex) e);
		} else if(e instanceof Expr.Deref) {
			return doDeref((Expr.Deref) e);
		} else if(e instanceof Stmt.Assignment) {
			// force brackets			
			return doAssignment((Stmt.Assignment) e);			
		} else if(e != null) {
			syntax_error("Invalid expression encountered: "
					+ e.getClass(),e);			
		}
		
		return null;
	}
	
	protected Expr doDeref(Expr.Deref e) {
		e.setTarget(doExpression(e.target()));		
		return e;
	}
	
	protected Expr doArrayIndex(Expr.ArrayIndex e) {
		e.setTarget(doExpression(e.target()));
		e.setIndex(doExpression(e.index()));
		
		return e;
	}
	
	protected Expr doNew(Expr.New e) {
		// Second, recurse through any parameters supplied ...
		List<Expr> parameters = e.parameters();
		for(int i=0;i!=parameters.size();++i) {
			Expr p = parameters.get(i);
			parameters.set(i,doExpression(p));
		}
		
		if(e.declarations().size() > 0) {
			for(Decl d : e.declarations()) {
				doDeclaration(d);
			}			
		}
		
		return e;
	}
	
	protected Expr doInvoke(Expr.Invoke e) {						
		e.setTarget(doExpression(e.target()));		
		
		List<Expr> parameters = e.parameters();
		for(int i=0;i!=parameters.size();++i) {
			Expr p = parameters.get(i);
			parameters.set(i, doExpression(p));
		}				
		
		return e;
	}
	
	protected Expr doInstanceOf(Expr.InstanceOf e) {
		e.setLhs(doExpression(e.lhs()));
		return e;
	}
	
	protected Expr doCast(Expr.Cast e) {
		e.setExpr(doExpression(e.expr()));
		return e;
	}
	
	protected Expr doBoolVal(Value.Bool e) {
		return e;
	}
	
	protected Expr doCharVal(Value.Char e) {
		return e;
	}
	
	protected Expr doIntVal(Value.Int e) {
		return e;
	}
	
	protected Expr doLongVal(Value.Long e) {
		return e;
	}
	
	protected Expr doFloatVal(Value.Float e) {
		return e;
	}
	
	protected Expr doDoubleVal(Value.Double e) {
		return e;
	}
	
	protected Expr doStringVal(Value.String e) {
		return e;
	}
	
	protected Expr doNullVal(Value.Null e) {
		return e;
	}
	
	protected Expr doTypedArrayVal(Value.TypedArray e) {
		for(int i=0;i!=e.values().size();++i) {
			Expr v = e.values().get(i);			
			e.values().set(i,doExpression(v));			
		}
		return e;
	}
	
	protected Expr doArrayVal(Value.Array e) {
		for(int i=0;i!=e.values().size();++i) {
			Expr v = e.values().get(i);			
			e.values().set(i,doExpression(v));			
		}
		return e;
	}
	
	protected Expr doClassVal(Value.Class e) {
		return e;
	}
		
	protected void doLocalVariable(Expr.LocalVariable e) {}

	protected void doNonLocalVariable(Expr.NonLocalVariable e) {}
	
	protected Expr doUnOp(Expr.UnOp e) {		
		e.setExpr(doExpression(e.expr()));
		return e;
	}
		
	protected Expr doBinOp(Expr.BinOp e) {				
		e.setLhs(doExpression(e.lhs()));
		e.setRhs(doExpression(e.rhs()));
		return e;
	}
	
	protected Expr doTernOp(Expr.TernOp e) {	
		e.setCondition(doExpression(e.condition()));
		e.setTrueBranch(doExpression(e.trueBranch()));
		e.setFalseBranch(doExpression(e.falseBranch()));
		return e;
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
	
	/**
	 * This method is just to factor out the code for looking up the source
	 * location and throwing an exception based on that. In this case, we also
	 * have an internal exception which has given rise to this particular
	 * problem.
	 * 
	 * @param msg
	 *            --- the error message
	 * @param e
	 *            --- the syntactic element causing the error
	 * @parem ex --- an internal exception, the details of which we want to
	 *        keep.
	 */
	protected void syntax_error(String msg, SyntacticElement e, Throwable ex) {
		SourceLocation loc = (SourceLocation) e.attribute(SourceLocation.class);
		throw new SyntaxError(msg,loc.line(),loc.column(),ex);
	}
}
