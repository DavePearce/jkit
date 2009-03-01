package jkit.java.stages;

import java.util.*;

import jkit.compiler.SyntaxError;
import jkit.compiler.ClassLoader;
import jkit.java.io.JavaFile;
import jkit.java.tree.Decl;
import jkit.java.*;
import jkit.jil.*;
import jkit.jil.tree.Clazz;
import jkit.jil.tree.Expr;
import jkit.jil.tree.SourceLocation;
import jkit.jil.tree.Stmt;
import jkit.jil.tree.SyntacticElement;
import jkit.jil.tree.Type;
import jkit.util.Pair;
import jkit.util.Triple;

/**
 * <p>
 * The aim of this stage is to generate Jil code representing this class.
 * </p>
 * 
 * @author djp
 * 
 */
public class CodeGeneration {
	private ClassLoader loader = null;
	
	public CodeGeneration(ClassLoader loader) {
		this.loader = loader;
	}
	
	public void apply(JavaFile file) {
		// Now, traverse the declarations
		for(Decl d : file.declarations()) {
			doDeclaration(d);
		}		
	}
	
	protected void doDeclaration(Decl d) {
		if(d instanceof Decl.Interface) {
			doInterface((Decl.Interface)d);
		} else if(d instanceof Decl.Clazz) {
			doClass((Decl.Clazz)d);
		} else if(d instanceof Decl.Method) {
			doMethod((Decl.Method)d);
		} else if(d instanceof Decl.Field) {
			doField((Decl.Field)d);
		} else if (d instanceof Decl.InitialiserBlock) {
			doInitialiserBlock((Decl.InitialiserBlock) d);
		} else if (d instanceof Decl.StaticInitialiserBlock) {
			doStaticInitialiserBlock((Decl.StaticInitialiserBlock) d);
		} else {
			syntax_error("internal failure (unknown declaration \"" + d
					+ "\" encountered)",d);
		}
	}
	
	protected void doInterface(Decl.Interface d) {
		doClass(d);
	}
	
	protected void doClass(Decl.Clazz c) {
		Type.Clazz type = (Type.Clazz) c.attribute(Type.class);
		try {
			// We, need to update the skeleton so that any methods and fields
			// discovered below this are attributed to this class!			
			Clazz skeleton = loader.loadClass(type);	
		} catch(ClassNotFoundException cne) {
			syntax_error("internal failure (skeleton not found for " + type,c,cne);
		}
		for(Decl d : c.declarations()) {
			doDeclaration(d);
		}		
	}

	protected void doMethod(Decl.Method d) {			
		doStatement(d.body());		
	}

	protected List<Stmt> doField(Decl.Field d) {		
		Pair<Expr,List<Stmt>> tmp = doExpression(d.initialiser());
		// bug here, as we need to assign to this field.
		return tmp.second();
	}
	
	protected void doInitialiserBlock(Decl.InitialiserBlock d) {
		for (jkit.java.tree.Stmt s : d.statements()) {
			doStatement(s);
		}	
	}
	
	protected void doStaticInitialiserBlock(Decl.StaticInitialiserBlock d) {		
		for (jkit.java.tree.Stmt s : d.statements()) {
			doStatement(s);
		}		
	}
	
	protected void doStatement(jkit.java.tree.Stmt e) {
		if(e instanceof jkit.java.tree.Stmt.SynchronisedBlock) {
			doSynchronisedBlock((jkit.java.tree.Stmt.SynchronisedBlock)e);
		} else if(e instanceof jkit.java.tree.Stmt.TryCatchBlock) {
			doTryCatchBlock((jkit.java.tree.Stmt.TryCatchBlock)e);
		} else if(e instanceof jkit.java.tree.Stmt.Block) {
			doBlock((jkit.java.tree.Stmt.Block)e);
		} else if(e instanceof jkit.java.tree.Stmt.VarDef) {
			doVarDef((jkit.java.tree.Stmt.VarDef) e);
		} else if(e instanceof jkit.java.tree.Stmt.Assignment) {
			doAssignment((jkit.java.tree.Stmt.Assignment) e);
		} else if(e instanceof jkit.java.tree.Stmt.Return) {
			doReturn((jkit.java.tree.Stmt.Return) e);
		} else if(e instanceof jkit.java.tree.Stmt.Throw) {
			doThrow((jkit.java.tree.Stmt.Throw) e);
		} else if(e instanceof jkit.java.tree.Stmt.Assert) {
			doAssert((jkit.java.tree.Stmt.Assert) e);
		} else if(e instanceof jkit.java.tree.Stmt.Break) {
			doBreak((jkit.java.tree.Stmt.Break) e);
		} else if(e instanceof jkit.java.tree.Stmt.Continue) {
			doContinue((jkit.java.tree.Stmt.Continue) e);
		} else if(e instanceof jkit.java.tree.Stmt.Label) {
			doLabel((jkit.java.tree.Stmt.Label) e);
		} else if(e instanceof jkit.java.tree.Stmt.If) {
			doIf((jkit.java.tree.Stmt.If) e);
		} else if(e instanceof jkit.java.tree.Stmt.For) {
			doFor((jkit.java.tree.Stmt.For) e);
		} else if(e instanceof jkit.java.tree.Stmt.ForEach) {
			doForEach((jkit.java.tree.Stmt.ForEach) e);
		} else if(e instanceof jkit.java.tree.Stmt.While) {
			doWhile((jkit.java.tree.Stmt.While) e);
		} else if(e instanceof jkit.java.tree.Stmt.DoWhile) {
			doDoWhile((jkit.java.tree.Stmt.DoWhile) e);
		} else if(e instanceof jkit.java.tree.Stmt.Switch) {
			doSwitch((jkit.java.tree.Stmt.Switch) e);
		} else if(e instanceof jkit.java.tree.Expr.Invoke) {
			doInvoke((jkit.java.tree.Expr.Invoke) e);
		} else if(e instanceof jkit.java.tree.Expr.New) {
			doNew((jkit.java.tree.Expr.New) e);
		} else if(e instanceof Decl.Clazz) {
			doClass((Decl.Clazz)e);
		} else if(e != null) {
			syntax_error("Invalid statement encountered: "
					+ e.getClass(),e);
		}		
	}
	
	protected void doBlock(jkit.java.tree.Stmt.Block block) {
		if(block != null) {		
			// now process every statement in this block.
			for(jkit.java.tree.Stmt s : block.statements()) {
				doStatement(s);
			}		
		}
	}
	
	protected void doCatchBlock(jkit.java.tree.Stmt.CatchBlock block) {
		if(block != null) {			
			// now process every statement in this block.
			for(jkit.java.tree.Stmt s : block.statements()) {
				doStatement(s);
			}		
		}
	}
	
	protected void doSynchronisedBlock(jkit.java.tree.Stmt.SynchronisedBlock block) {
		doBlock(block);
		doExpression(block.expr());
	}
	
	protected void doTryCatchBlock(jkit.java.tree.Stmt.TryCatchBlock block) {
		doBlock(block);		
		doBlock(block.finaly());		
		
		for(jkit.java.tree.Stmt.CatchBlock cb : block.handlers()) {
			doCatchBlock(cb);
		}
	}
	
	protected void doVarDef(jkit.java.tree.Stmt.VarDef def) {
		List<Triple<String, Integer, jkit.java.tree.Expr>> defs = def.definitions();
		for(int i=0;i!=defs.size();++i) {
			Triple<String, Integer, jkit.java.tree.Expr> d = defs.get(i);
			Pair<Expr,List<Stmt>> e = doExpression(d.third());
			defs.set(i, new Triple(d.first(),d.second(),e));			
		}		
	}
	
	protected Pair<Expr,List<Stmt>> doAssignment(jkit.java.tree.Stmt.Assignment def) {
		doExpression(def.lhs());	
		doExpression(def.rhs());
		return null; // needs fixing!!
	}
	
	protected void doReturn(jkit.java.tree.Stmt.Return ret) {
		doExpression(ret.expr());
	}
	
	protected void doThrow(jkit.java.tree.Stmt.Throw ret) {
		doExpression(ret.expr());
	}
	
	protected void doAssert(jkit.java.tree.Stmt.Assert ret) {
		doExpression(ret.expr());
	}
	
	protected void doBreak(jkit.java.tree.Stmt.Break brk) {
		// nothing	
	}
	
	protected void doContinue(jkit.java.tree.Stmt.Continue brk) {
		// nothing
	}
	
	protected void doLabel(jkit.java.tree.Stmt.Label lab) {						
		doStatement(lab.statement());
	}
	
	protected void doIf(jkit.java.tree.Stmt.If stmt) {
		doExpression(stmt.condition());
		doStatement(stmt.trueStatement());
		doStatement(stmt.falseStatement());
	}
	
	protected void doWhile(jkit.java.tree.Stmt.While stmt) {
		doExpression(stmt.condition());
		doStatement(stmt.body());		
	}
	
	protected void doDoWhile(jkit.java.tree.Stmt.DoWhile stmt) {
		doExpression(stmt.condition());
		doStatement(stmt.body());
	}
	
	protected void doFor(jkit.java.tree.Stmt.For stmt) {
		doStatement(stmt.initialiser());
		doExpression(stmt.condition());
		doStatement(stmt.increment());
		doStatement(stmt.body());		
	}
	
	protected void doForEach(jkit.java.tree.Stmt.ForEach stmt) {
		doExpression(stmt.source());
		doStatement(stmt.body());		
	}
	
	protected void doSwitch(jkit.java.tree.Stmt.Switch sw) {
		doExpression(sw.condition());
		for(jkit.java.tree.Stmt.Case c : sw.cases()) {
			doExpression(c.condition());
			for(jkit.java.tree.Stmt s : c.statements()) {
				doStatement(s);
			}
		}
		
		// should check that case conditions are final constants here.
	}
	
	protected Pair<Expr,List<Stmt>> doExpression(jkit.java.tree.Expr e) {	
		if(e instanceof jkit.java.tree.Value.Bool) {
			return doBoolVal((jkit.java.tree.Value.Bool)e);
		} else if(e instanceof jkit.java.tree.Value.Char) {
			return doCharVal((jkit.java.tree.Value.Char)e);
		} else if(e instanceof jkit.java.tree.Value.Int) {
			return doIntVal((jkit.java.tree.Value.Int)e);
		} else if(e instanceof jkit.java.tree.Value.Long) {
			return doLongVal((jkit.java.tree.Value.Long)e);
		} else if(e instanceof jkit.java.tree.Value.Float) {
			return doFloatVal((jkit.java.tree.Value.Float)e);
		} else if(e instanceof jkit.java.tree.Value.Double) {
			return doDoubleVal((jkit.java.tree.Value.Double)e);
		} else if(e instanceof jkit.java.tree.Value.String) {
			return doStringVal((jkit.java.tree.Value.String)e);
		} else if(e instanceof jkit.java.tree.Value.Null) {
			return doNullVal((jkit.java.tree.Value.Null)e);
		} else if(e instanceof jkit.java.tree.Value.TypedArray) {
			return doTypedArrayVal((jkit.java.tree.Value.TypedArray)e);
		} else if(e instanceof jkit.java.tree.Value.Array) {
			return doArrayVal((jkit.java.tree.Value.Array)e);
		} else if(e instanceof jkit.java.tree.Value.Class) {
			return doClassVal((jkit.java.tree.Value.Class) e);
		} else if(e instanceof jkit.java.tree.Expr.LocalVariable) {
			return doLocalVariable((jkit.java.tree.Expr.LocalVariable)e);
		} else if(e instanceof jkit.java.tree.Expr.NonLocalVariable) {
			return doNonLocalVariable((jkit.java.tree.Expr.NonLocalVariable)e);
		} else if(e instanceof jkit.java.tree.Expr.ClassVariable) {
			return doClassVariable((jkit.java.tree.Expr.ClassVariable)e);
		} else if(e instanceof jkit.java.tree.Expr.UnOp) {
			return doUnOp((jkit.java.tree.Expr.UnOp)e);
		} else if(e instanceof jkit.java.tree.Expr.BinOp) {
			return doBinOp((jkit.java.tree.Expr.BinOp)e);
		} else if(e instanceof jkit.java.tree.Expr.TernOp) {
			return doTernOp((jkit.java.tree.Expr.TernOp)e);
		} else if(e instanceof jkit.java.tree.Expr.Cast) {
			return doCast((jkit.java.tree.Expr.Cast)e);
		} else if(e instanceof jkit.java.tree.Expr.InstanceOf) {
			return doInstanceOf((jkit.java.tree.Expr.InstanceOf)e);
		} else if(e instanceof jkit.java.tree.Expr.Invoke) {
			return doInvoke((jkit.java.tree.Expr.Invoke) e);
		} else if(e instanceof jkit.java.tree.Expr.New) {
			return doNew((jkit.java.tree.Expr.New) e);
		} else if(e instanceof jkit.java.tree.Expr.ArrayIndex) {
			return doArrayIndex((jkit.java.tree.Expr.ArrayIndex) e);
		} else if(e instanceof jkit.java.tree.Expr.Deref) {
			return doDeref((jkit.java.tree.Expr.Deref) e);
		} else if(e instanceof jkit.java.tree.Stmt.Assignment) {
			// force brackets			
			return doAssignment((jkit.java.tree.Stmt.Assignment) e);			
		} else if(e != null) {
			syntax_error("Invalid expression encountered: "
					+ e.getClass(),e);			
		}
		
		return null;
	}
	
	protected Pair<Expr,List<Stmt>> doDeref(jkit.java.tree.Expr.Deref e) {
		Pair<Expr,List<Stmt>> target = doExpression(e.target());
		Type type = (Type) e.attribute(Type.class);
		return new Pair<Expr, List<Stmt>>(new Expr.Deref(target.first(), e
				.name(), type, e.attributes()), target.second());
	}
	
	protected Pair<Expr,List<Stmt>> doArrayIndex(jkit.java.tree.Expr.ArrayIndex e) {
		Pair<Expr,List<Stmt>> target = doExpression(e.target());
		Pair<Expr,List<Stmt>> index = doExpression(e.index());
		Type type = (Type) e.attribute(Type.class);
		
		List<Stmt> r = target.second();
		r.addAll(index.second());
		
		return new Pair<Expr, List<Stmt>>(new Expr.ArrayIndex(target.first(),
				index.first(), type, e.attributes()), r);
	}
	
	protected Pair<Expr,List<Stmt>> doNew(jkit.java.tree.Expr.New e) {
		// Second, recurse through any parameters supplied ...
		ArrayList<Stmt> r = new ArrayList();
		ArrayList<Expr> nparameters = new ArrayList();			
		Type.Reference type = (Type.Reference) e.attribute(Type.class);
		List<jkit.java.tree.Expr> parameters = e.parameters();
		
		for(int i=0;i!=parameters.size();++i) {
			jkit.java.tree.Expr p = parameters.get(i);
			Pair<Expr,List<Stmt>> tmp = doExpression(p);
			nparameters.add(tmp.first());
			r.addAll(tmp.second());
		}
		
		if(e.declarations().size() > 0) {
			for(Decl d : e.declarations()) {
				doDeclaration(d);
			}			
		}
		
		return new Pair<Expr,List<Stmt>>(new Expr.New(type,nparameters,e.attributes()),r);
	}
	
	protected Pair<Expr,List<Stmt>> doInvoke(jkit.java.tree.Expr.Invoke e) {
		ArrayList<Stmt> r = new ArrayList();
		ArrayList<Expr> nparameters = new ArrayList();
		Type type = (Type) e.attribute(Type.class);		

		Pair<Expr,List<Stmt>> target = doExpression(e.target());
		r.addAll(target.second());
		
		List<jkit.java.tree.Expr> parameters = e.parameters();
		for(int i=0;i!=parameters.size();++i) {
			jkit.java.tree.Expr p = parameters.get(i);
			Pair<Expr,List<Stmt>> tmp = doExpression(p);
			nparameters.add(tmp.first());
			r.addAll(tmp.second());
		}				
		
		return new Pair<Expr, List<Stmt>>(new Expr.Invoke(target.first(), e
				.name(), nparameters, type, e.attributes()), r);
	}
	
	protected Pair<Expr,List<Stmt>> doInstanceOf(jkit.java.tree.Expr.InstanceOf e) {
		Pair<Expr,List<Stmt>> lhs = doExpression(e.lhs());
		Type type = (Type) e.attribute(Type.class);
		Type rhs = (Type) e.rhs().attribute(Type.class);
		return new Pair<Expr, List<Stmt>>(new Expr.InstanceOf(lhs.first(), rhs,
				type, e.attributes()), lhs.second());
	}
	
	protected Pair<Expr,List<Stmt>> doCast(jkit.java.tree.Expr.Cast e) {
		Pair<Expr,List<Stmt>> expr = doExpression(e.expr());		
		Type type = (Type) e.attribute(Type.class);
		return new Pair<Expr, List<Stmt>>(new Expr.Cast(expr.first(),
				type, e.attributes()), expr.second());		
	}
	
	protected Pair<Expr,List<Stmt>> doBoolVal(jkit.java.tree.Value.Bool e) {
		return new Pair<Expr, List<Stmt>>(new Expr.Bool(e.value()),
				new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doCharVal(jkit.java.tree.Value.Char e) {
		return new Pair<Expr,List<Stmt>>(new Expr.Char(e.value()), new ArrayList<Stmt>());		
	}
	
	protected Pair<Expr,List<Stmt>> doIntVal(jkit.java.tree.Value.Int e) {
		return new Pair<Expr,List<Stmt>>(new Expr.Int(e.value()), new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doLongVal(jkit.java.tree.Value.Long e) {
		return new Pair<Expr,List<Stmt>>(new Expr.Long(e.value()), new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doFloatVal(jkit.java.tree.Value.Float e) {
		return new Pair<Expr,List<Stmt>>(new Expr.Float(e.value()), new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doDoubleVal(jkit.java.tree.Value.Double e) {
		return new Pair<Expr,List<Stmt>>(new Expr.Double(e.value()), new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doStringVal(jkit.java.tree.Value.String e) {
		return new Pair<Expr,List<Stmt>>(new Expr.StringVal(e.value()), new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doNullVal(jkit.java.tree.Value.Null e) {
		return new Pair<Expr,List<Stmt>>(new Expr.Null(), new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doTypedArrayVal(jkit.java.tree.Value.TypedArray e) {
		ArrayList<Stmt> r = new ArrayList<Stmt>();
		ArrayList<Expr> params = new ArrayList();
		Type.Array type = (Type.Array) e.attribute(Type.class);
		
		for(int i=0;i!=e.values().size();++i) {
			jkit.java.tree.Expr v = e.values().get(i);
			Pair<Expr,List<Stmt>> p = doExpression(v);
			params.add(p.first());
			r.addAll(p.second());
		}
		return new Pair<Expr,List<Stmt>>(new Expr.Array(params,type,e.attributes()),r);
	}
	
	protected Pair<Expr,List<Stmt>> doArrayVal(jkit.java.tree.Value.Array e) {
		ArrayList<Stmt> r = new ArrayList<Stmt>();
		ArrayList<Expr> params = new ArrayList();
		Type.Array type = (Type.Array) e.attribute(Type.class);

		for(int i=0;i!=e.values().size();++i) {
			jkit.java.tree.Expr v = e.values().get(i);			
			Pair<Expr,List<Stmt>> p = doExpression(v);
			params.add(p.first());
			r.addAll(p.second());
		}
		
		return new Pair<Expr,List<Stmt>>(new Expr.Array(params,type,e.attributes()),r);
	}
	
	protected Pair<Expr,List<Stmt>> doClassVal(jkit.java.tree.Value.Class e) {
		Type.Clazz type = (Type.Clazz) e.attribute(Type.class);		
		return new Pair<Expr, List<Stmt>>(new Expr.Class(type, e.attributes()),
				new ArrayList<Stmt>());
	}
		
	protected Pair<Expr, List<Stmt>> doLocalVariable(
			jkit.java.tree.Expr.LocalVariable e) {
		Type type = (Type) e.attribute(Type.class);
		return new Pair<Expr, List<Stmt>>(new Expr.Variable(e.value(), type, e
				.attributes()), new ArrayList<Stmt>());
	}

	protected Pair<Expr,List<Stmt>> doNonLocalVariable(jkit.java.tree.Expr.NonLocalVariable e) {
		syntax_error("internal failure (support for non-local variables not implemented!)",e);
		return null;
	}
	
	protected Pair<Expr,List<Stmt>> doClassVariable(jkit.java.tree.Expr.ClassVariable e) {
		Type.Clazz type = (Type.Clazz) e.attribute(Type.class);
		return new Pair<Expr, List<Stmt>>(new Expr.Class(type, e.attributes()),
				new ArrayList<Stmt>());
	}
	
	protected Pair<Expr,List<Stmt>> doUnOp(jkit.java.tree.Expr.UnOp e) {		
		Pair<Expr,List<Stmt>> r = doExpression(e.expr());	
		Type.Primitive type = (Type.Primitive) e.attribute(Type.class);
		
		return new Pair<Expr, List<Stmt>>(new Expr.UnOp(r.first(), e.op(),
				type, e.attributes()), r.second());
	}
		
	protected Pair<Expr,List<Stmt>> doBinOp(jkit.java.tree.Expr.BinOp e) {				
		Pair<Expr,List<Stmt>> lhs = doExpression(e.lhs());
		Pair<Expr,List<Stmt>> rhs = doExpression(e.rhs());
		Type.Primitive type = (Type.Primitive) e.attribute(Type.class);
		
		List<Stmt> r = lhs.second();
		r.addAll(rhs.second());
		
		return new Pair<Expr, List<Stmt>>(new Expr.BinOp(lhs.first(), rhs
				.first(), e.op(), type, e.attributes()), r);
	}
	
	protected Pair<Expr,List<Stmt>> doTernOp(jkit.java.tree.Expr.TernOp e) {	
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
