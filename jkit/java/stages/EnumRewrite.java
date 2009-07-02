package jkit.java.stages;

import static jkit.compiler.SyntaxError.syntax_error;

import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.java.io.JavaFile;
import jkit.java.tree.*;
import jkit.java.tree.Decl.*;
import jkit.java.tree.Stmt.Case;
import jkit.jil.tree.JilClass;
import jkit.jil.tree.JilField;
import jkit.jil.tree.Modifier;
import jkit.jil.tree.SourceLocation;
import jkit.jil.tree.Type;
import jkit.util.Triple;

public class EnumRewrite {
	private ClassLoader loader;
	private TypeSystem types;	
	
	public EnumRewrite(ClassLoader loader, TypeSystem types) {
		this.loader = loader; 
		this.types = types;
	}
	
	public void apply(JavaFile file) {			
		// Now, examine all the declarations contain here-in
		for(Decl d : file.declarations()) {			
			doDeclaration(d);
		}
	}
	
	protected void doDeclaration(Decl d) {
		if(d instanceof JavaInterface) {
			doInterface((JavaInterface)d);
		} else if(d instanceof JavaClass) {
			doClass((JavaClass)d);
		} else if(d instanceof JavaMethod) {
			doMethod((JavaMethod)d);
		} else if(d instanceof JavaField) {
			doField((JavaField)d);
		} else if (d instanceof Decl.InitialiserBlock) {
			doInitialiserBlock((Decl.InitialiserBlock) d);
		} else if (d instanceof Decl.StaticInitialiserBlock) {
			doStaticInitialiserBlock((Decl.StaticInitialiserBlock) d);
		} else {
			syntax_error("internal failure (unknown declaration \"" + d
					+ "\" encountered)",d);			
		}
	}
	
	protected void doEnum(Decl.JavaEnum ec, JilClass skeleton) {	
		
		// Now add the $VALUES field
		List<Modifier> modifiers = new ArrayList<Modifier>();
		modifiers.add(Modifier.ACC_PRIVATE);
		modifiers.add(Modifier.ACC_STATIC);
		modifiers.add(Modifier.ACC_FINAL);
		
		skeleton.fields()
				.add(
						new JilField("$VALUES", new Type.Array(type),
								modifiers));
		
		// Now, create the values() method
		
	}
	
	protected void doInterface(JavaInterface d) {
		doClass(d);
	}
	
	protected void doClass(JavaClass c) {							
		List<Decl> declarations = c.declarations();
		for(int i=0;i!=declarations.size();++i) {		
			doDeclaration(declarations.get(i));
		}	
	}

	protected void doMethod(JavaMethod d) {	
		// Now, explore the method body for any other things to resolve.
		doStatement(d.body());		
	}
	
	protected void doField(JavaField d) {
		d.setInitialiser(doExpression(d.initialiser()));				
	}
	
	protected void doInitialiserBlock(Decl.InitialiserBlock d) {
		for(Stmt s : d.statements()) {
			doStatement(s);
		}		
	}
	
	protected void doStaticInitialiserBlock(Decl.StaticInitialiserBlock d) {	
		for(Stmt s : d.statements()) {
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
		} else if(e instanceof Decl.JavaClass) {
			doClass((Decl.JavaClass)e);
		} else if(e instanceof Stmt.PrePostIncDec) {
			doExpression((Stmt.PrePostIncDec)e);
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
	
	protected void doSynchronisedBlock(Stmt.SynchronisedBlock block) {		
		doBlock(block);
		doExpression(block.expr());		
	}
	
	protected void doTryCatchBlock(Stmt.TryCatchBlock block) {		
		doBlock(block);		
		doBlock(block.finaly());		
		
		for(Stmt.CatchBlock cb : block.handlers()) {			
			doBlock(cb);			
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
	}
	
	protected void doContinue(Stmt.Continue brk) {		
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
	}
	
	protected Expr doExpression(Expr e) {	
		if(e instanceof Value.Bool) {
			return doBoolVal((Value.Bool)e);
		} else if(e instanceof Value.Byte) {
			return doByteVal((Value.Byte)e);
		} else if(e instanceof Value.Char) {
			return doCharVal((Value.Char)e);
		} else if(e instanceof Value.Short) {
			return doShortVal((Value.Short)e);
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
		} else if(e instanceof Expr.Convert) {
			return doConvert((Expr.Convert)e);
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
		e.setContext(doExpression(e.context()));
			
		for(int i = 0;i!=e.parameters().size();++i) {
			e.parameters().set(i,doExpression(e.parameters().get(i)));			
		}						
		
		return e;
	}
	
	protected Expr doInvoke(Expr.Invoke e) {
		e.setTarget(doExpression(e.target()));
		
		for(int i=0;i!=e.parameters().size();++i) {
			Expr p = e.parameters().get(i);
			e.parameters().set(i,doExpression(p));
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
	
	protected Expr doConvert(Expr.Convert e) {		
		e.setExpr(doExpression(e.expr()));
		return e;
	}
	
	protected Expr doBoolVal(Value.Bool e) { return e; }
	
	protected Expr doByteVal(Value.Byte e) { return e; }
	
	protected Expr doCharVal(Value.Char e) { return e; }
	
	protected Expr doShortVal(Value.Short e) { return e; }
	
	protected Expr doIntVal(Value.Int e) { return e; }
	
	protected Expr doLongVal(Value.Long e) { return e; }
	
	protected Expr doFloatVal(Value.Float e) { return e; }
	
	protected Expr doDoubleVal(Value.Double e) { return e; }
	
	protected Expr doStringVal(Value.String e) { return e; }
	
	protected Expr doNullVal(Value.Null e) { return e; }
	
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
	
	protected Expr doLocalVariable(Expr.LocalVariable e) {
		return e; 
	}

	protected Expr doNonLocalVariable(Expr.NonLocalVariable e) {
		return e;
	}
	
	protected Expr doClassVariable(Expr.ClassVariable e) {
		return e;
	}
	
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
		e.setFalseBranch(doExpression(e.falseBranch()));
		e.setTrueBranch(doExpression(e.trueBranch()));
		return e;
	}
	
	protected Decl.JavaMethod createValuesMethod(Decl.JavaEnum ec, Type.Clazz type) {
		SourceLocation loc = (SourceLocation) ec
				.attribute(SourceLocation.class);
		
		Type.Function ftype = new Type.Function(new Type.Array(type));
		ArrayList<Modifier> mods = new ArrayList<Modifier>();
		mods.add(Modifier.ACC_PUBLIC);		
		ArrayList<Stmt> stmts = new ArrayList();
		
		// load, clone, cast and return array
		Expr.UnresolvedVariable uv = new Expr.UnresolvedVariable(ec.name());
		Expr.Deref load = new Expr.Deref(uv, "$VALUES",loc);
		Expr.Invoke clone = new Expr.Invoke(load,"clone",new ArrayList(),
				new ArrayList(), loc);
		jkit.java.tree.Type rtype = new jkit.java.tree.Type.Array(
				new jkit.java.tree.Type.Clazz(ec.name(), loc));  
		Expr.Cast cast = new Expr.Cast(rtype,clone,loc);
		cast.type().attributes().add(new Type.Array(type));
		Stmt.Return ret = new Stmt.Return(cast,loc); 
		stmts.add(ret);
		
		Stmt.Block block = new Stmt.Block(stmts, loc);

		Decl.JavaMethod m = new Decl.JavaMethod(mods, "values",
				rtype, new ArrayList(), false,
				new ArrayList(), new ArrayList(), block, loc);

		m.attributes().add(ftype);
		
		return m;		
	}
	
	protected Decl.StaticInitialiserBlock createInitialiser(Decl.JavaEnum ec, Type.Clazz type) {
		SourceLocation loc = (SourceLocation) ec
				.attribute(SourceLocation.class);
		jkit.java.tree.Type.Clazz ecType = new jkit.java.tree.Type.Clazz(ec.name(), loc);
		int i=0;
		ArrayList<Stmt> stmts = new ArrayList();
		for(Decl.EnumConstant c : ec.constants()) {
			ArrayList<Expr> arguments = new ArrayList();
			arguments.add(new Value.String(c.name()));
			arguments.add(new Value.Int(i));
			arguments.addAll(c.arguments());
			Expr.New nuw = new Expr.New(ecType,null,arguments, new ArrayList(),new ArrayList(c.attributes())); 
			nuw.type().attributes().add(type);
			Expr.UnresolvedVariable uv = new Expr.UnresolvedVariable("$VALUES",new ArrayList(c.attributes()));
			Expr.ArrayIndex array = new Expr.ArrayIndex(uv,new Value.Int(i++),new ArrayList(c.attributes()));
			Stmt.Assignment assign = new Stmt.Assignment(array,nuw);
			stmts.add(assign);
		}

		Decl.StaticInitialiserBlock blk = new Decl.StaticInitialiserBlock(stmts,loc);
				
		return blk;	
	}
}
