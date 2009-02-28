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
import jkit.util.Pair;
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
public class CodeGeneration {

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
	
	protected Pair<Expr,List<Stmt>> doAssignment(Stmt.Assignment def) {
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
	
	protected Pair<Expr,List<Stmt>> doExpression(Expr e) {	
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
			return doLocalVariable((Expr.LocalVariable)e);
		} else if(e instanceof Expr.NonLocalVariable) {
			return doNonLocalVariable((Expr.NonLocalVariable)e);
		} else if(e instanceof Expr.ClassVariable) {
			return doClassVariable((Expr.ClassVariable)e);
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
	
	protected Pair<Expr,List<Stmt>> doDeref(Expr.Deref e) {
		Pair<Expr,List<Stmt>> target = doExpression(e.target());
		e.setTarget(target.first());		
		return new Pair(e,target.second());
	}
	
	protected Pair<Expr,List<Stmt>> doArrayIndex(Expr.ArrayIndex e) {
		Pair<Expr,List<Stmt>> target = doExpression(e.target());
		Pair<Expr,List<Stmt>> index = doExpression(e.index());
		e.setTarget(target.first());
		e.setIndex(index.first());
		
		List<Stmt> r = target.second();
		r.addAll(index.second());
		
		return new Pair(e,r);
	}
	
	protected Pair<Expr,List<Stmt>> doNew(Expr.New e) {
		// Second, recurse through any parameters supplied ...
		ArrayList<Stmt> r = new ArrayList();
		List<Expr> parameters = e.parameters();
		for(int i=0;i!=parameters.size();++i) {
			Expr p = parameters.get(i);
			Pair<Expr,List<Stmt>> tmp = doExpression(p);
			parameters.set(i,tmp.first());
			r.addAll(tmp.second());
		}
		
		if(e.declarations().size() > 0) {
			for(Decl d : e.declarations()) {
				doDeclaration(d);
			}			
		}
		
		return new Pair(e,r);
	}
	
	protected Pair<Expr,List<Stmt>> doInvoke(Expr.Invoke e) {
		ArrayList<Stmt> r = new ArrayList();
		
		Pair<Expr,List<Stmt>> tmp = doExpression(e.target());
		e.setTarget(tmp.first());		
		
		r.addAll(tmp.second());
		
		List<Expr> parameters = e.parameters();
		for(int i=0;i!=parameters.size();++i) {
			Expr p = parameters.get(i);
			tmp = doExpression(p);
			parameters.set(i, tmp.first());
			r.addAll(tmp.second());
		}				
		
		return new Pair(e,r);
	}
	
	protected Pair<Expr,List<Stmt>> doInstanceOf(Expr.InstanceOf e) {
		Pair<Expr,List<Stmt>> expr = doExpression(e.lhs());
		e.setLhs(expr.first());
		return new Pair(e,expr.second());
	}
	
	protected Pair<Expr,List<Stmt>> doCast(Expr.Cast e) {
		Pair<Expr,List<Stmt>> expr = doExpression(e.expr()); 
		e.setExpr(expr.first());
		return new Pair(e,expr.second());
	}
	
	protected Pair<Expr,List<Stmt>> doBoolVal(Value.Bool e) {
		return new Pair(e, new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doCharVal(Value.Char e) {
		return new Pair(e, new ArrayList<Stmt>());		
	}
	
	protected Pair<Expr,List<Stmt>> doIntVal(Value.Int e) {
		return new Pair(e, new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doLongVal(Value.Long e) {
		return new Pair(e, new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doFloatVal(Value.Float e) {
		return new Pair(e, new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doDoubleVal(Value.Double e) {
		return new Pair(e, new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doStringVal(Value.String e) {
		return new Pair(e, new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doNullVal(Value.Null e) {
		return new Pair(e, new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doTypedArrayVal(Value.TypedArray e) {
		ArrayList<Stmt> r = new ArrayList<Stmt>();
		for(int i=0;i!=e.values().size();++i) {
			Expr v = e.values().get(i);
			Pair<Expr,List<Stmt>> p = doExpression(v);
			e.values().set(i,p.first());
			r.addAll(p.second());
		}
		return new Pair(e,r);
	}
	
	protected Pair<Expr,List<Stmt>> doArrayVal(Value.Array e) {
		ArrayList<Stmt> r = new ArrayList<Stmt>();
		for(int i=0;i!=e.values().size();++i) {
			Expr v = e.values().get(i);			
			Pair<Expr,List<Stmt>> p = doExpression(v);
			e.values().set(i,p.first());
			r.addAll(p.second());
		}
		return new Pair(e,r);
	}
	
	protected Pair<Expr,List<Stmt>> doClassVal(Value.Class e) {
		return new Pair(e, new ArrayList<Stmt>());
	}
		
	protected Pair<Expr,List<Stmt>> doLocalVariable(Expr.LocalVariable e) {
		return new Pair(e, new ArrayList<Stmt>());		
	}

	protected Pair<Expr,List<Stmt>> doNonLocalVariable(Expr.NonLocalVariable e) {
		return new Pair(e, new ArrayList<Stmt>());		
	}
	
	protected Pair<Expr,List<Stmt>> doClassVariable(Expr.ClassVariable e) {
		return new Pair(e, new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doUnOp(Expr.UnOp e) {		
		Pair<Expr,List<Stmt>> r = doExpression(e.expr());
		e.setExpr(r.first());
		return new Pair(e,r.second());
	}
		
	protected Pair<Expr,List<Stmt>> doBinOp(Expr.BinOp e) {				
		Pair<Expr,List<Stmt>> lhs = doExpression(e.lhs());
		Pair<Expr,List<Stmt>> rhs = doExpression(e.rhs());
		e.setLhs(lhs.first());
		e.setRhs(rhs.first());
		List<Stmt> r = lhs.second();
		r.addAll(rhs.second());
		return new Pair(e,r);
	}
	
	protected Pair<Expr,List<Stmt>> doTernOp(Expr.TernOp e) {	
		syntax_error("internal failure --- problem processing ternary operator.",e);
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
