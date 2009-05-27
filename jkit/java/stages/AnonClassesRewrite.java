package jkit.java.stages;

import static jkit.compiler.SyntaxError.syntax_error;
import static jkit.java.tree.Type.fromJilType;

import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.java.io.JavaFile;
import jkit.java.tree.Decl;
import jkit.java.tree.Expr;
import jkit.java.tree.Stmt;
import jkit.java.tree.Value;
import jkit.java.tree.Decl.JavaClass;
import jkit.java.tree.Decl.JavaField;
import jkit.java.tree.Decl.JavaInterface;
import jkit.java.tree.Decl.JavaMethod;
import jkit.java.tree.Stmt.Case;
import jkit.jil.tree.*;
import jkit.jil.util.*;
import jkit.util.Pair;
import jkit.util.Triple;

public class AnonClassesRewrite {
	private ClassLoader loader;
	private TypeSystem types;
	private int anonymousClassCount = 0;
	private final Stack<JavaClass> context = new Stack<JavaClass>(); 
	
	public AnonClassesRewrite(ClassLoader loader, TypeSystem types) {
		this.loader = loader; 
		this.types = types;
	}
	
	public void apply(JavaFile file) {	
		anonymousClassCount = 0;
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
	
	protected void doInterface(JavaInterface d) {
		doClass(d);
	}
	
	protected void doClass(JavaClass c) {					
		context.push(c);

		// NOTE: in the following loop we cannot use an iterator, otherwise a
		// concurrent modification exception can arise when we rewrite a
		// anonymous class.
		List<Decl> declarations = c.declarations();
		for(int i=0;i!=declarations.size();++i) {		
			doDeclaration(declarations.get(i));
		}
		context.pop();
	}

	protected void doMethod(JavaMethod d) {	
		// Now, explore the method body for any other things to resolve.
		doStatement(d.body());		
	}
	
	protected void doField(JavaField d) {
		doExpression(d.initialiser());				
	}
	
	protected void doInitialiserBlock(Decl.InitialiserBlock d) {
		// will need to add code here for dealing with classes nested in
		// methods.
		for(Stmt s : d.statements()) {
			doStatement(s);
		}		
	}
	
	protected void doStaticInitialiserBlock(Decl.StaticInitialiserBlock d) {
		// will need to add code here for dealing with classes nested in
		// methods.
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
			doExpression(d.third());														
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
		} else if(e instanceof Value.Byte) {
			doByteVal((Value.Byte)e);
		} else if(e instanceof Value.Char) {
			doCharVal((Value.Char)e);
		} else if(e instanceof Value.Short) {
			doShortVal((Value.Short)e);
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
		} else if(e instanceof Expr.Convert) {
			doConvert((Expr.Convert)e);
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
			syntax_error("Invalid expression encountered: "
					+ e.getClass(),e);
		}
	}
	
	protected void doDeref(Expr.Deref e) {
		doExpression(e.target());		
		// need to perform field lookup here!
	}
	
	protected void doArrayIndex(Expr.ArrayIndex e) {
		doExpression(e.target());
		doExpression(e.index());
	}
	
	protected void doNew(Expr.New e) {				
		doExpression(e.context());
			
		for(Expr p : e.parameters()) {
			doExpression(p);			
		}
		
		if(e.declarations().size() > 0) {
			// This is an anonymous class declaration.  We need to do several
			// things here. Firstly, update the type of the new expression to be
			// that of the anonymous inner class. Second, we need to add
			// appropriate constructors to the anonymous inner class.
			String name = Integer.toString(++anonymousClassCount);
			Type.Clazz parent = (Type.Clazz) e.type().attribute(Type.Clazz.class);			
			Type.Clazz aType = anonClassType(name);
				
			try {
				JilClass parentClass = (JilClass) loader.loadClass(parent);
				JilClass anonClass = (JilClass) loader.loadClass(aType);
				SourceLocation loc = (SourceLocation) e
						.attribute(SourceLocation.class);
				
				// First, update the type of the new expression
				e.type().attributes().remove(parent);
				e.type().attributes().add(aType);

				// Second, create an appropriate constructor.
				JilBuilder.MethodInfo mi = (JilBuilder.MethodInfo) e
				.attribute(JilBuilder.MethodInfo.class);
				
				Decl.JavaMethod constructor = buildAnonConstructor(name,
						mi.type, mi.exceptions, parentClass, anonClass, loc);
				
				Decl.JavaClass ac = buildAnonClass(anonClass, loc);
				
				ac.declarations().add(constructor);
				ac.declarations().addAll(e.declarations());
				
				context.peek().declarations().add(ac);
				
			} catch(ClassNotFoundException cne) {
				syntax_error(cne.getMessage(),e,cne);
			}
						
			// Finally, iterate the declarations in this anon class.
			for(Decl d : e.declarations()) {
				doDeclaration(d);			
			}
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
	
	protected void doConvert(Expr.Convert e) {		
		doExpression(e.expr());
	}
	
	protected void doBoolVal(Value.Bool e) {}
	
	protected void doByteVal(Value.Byte e) {}
	
	protected void doCharVal(Value.Char e) {}
	
	protected void doShortVal(Value.Short e) {}
	
	protected void doIntVal(Value.Int e) {}
	
	protected void doLongVal(Value.Long e) {}
	
	protected void doFloatVal(Value.Float e) {}
	
	protected void doDoubleVal(Value.Double e) {}
	
	protected void doStringVal(Value.String e) {}
	
	protected void doNullVal(Value.Null e) {}
	
	protected void doTypedArrayVal(Value.TypedArray e) {
		for(Expr v : e.values()) {
			doExpression(v);
		}
	}
	
	protected void doArrayVal(Value.Array e) {
		for(Expr v : e.values()) {
			doExpression(v);
		}
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
	
	protected Decl.JavaClass buildAnonClass(JilClass anonClass, SourceLocation loc) {
		
		jkit.java.tree.Type.Clazz superClass = fromJilType(anonClass.superClass());
		ArrayList<jkit.java.tree.Type.Clazz> interfaces = new ArrayList();
		for(Type.Clazz i : anonClass.interfaces()) {
			interfaces.add(fromJilType(i));
		}
		
		Decl.JavaClass jc = new Decl.JavaClass(new ArrayList(anonClass
				.modifiers()), anonClass.name(), new ArrayList(), superClass,
				interfaces, new ArrayList<Decl>(), loc, anonClass.type());				
		
		return jc;
	}
	
	protected Decl.JavaMethod buildAnonConstructor(String name,
			Type.Function type, ArrayList<Type.Clazz> exceptions,
			JilClass parentClass, JilClass anonClass, SourceLocation loc) {
		
		ArrayList<Pair<String, List<Modifier>>> jilparams = new ArrayList();
		ArrayList<Triple<String, List<Modifier>, jkit.java.tree.Type>> javaparams = new ArrayList();
		ArrayList<Expr> args = new ArrayList();
						
		ArrayList<Modifier> mods = new ArrayList();
		mods.add(new Modifier.Base(java.lang.reflect.Modifier.FINAL));
		int p = 0;
						
		for (Type t : type.parameterTypes()) {
			// don't include the first parameter *if* it's the parent pointer,
			// and the super class is static.
			jilparams.add(new Pair("x" + p, mods));
			javaparams.add(new Triple("x" + p, mods, fromJilType(t)));
			args.add(new Expr.LocalVariable("x" + p, t));
			p = p + 1;
		}
		
		ArrayList<Stmt> stmts = new ArrayList<Stmt>();
		Expr.LocalVariable target = new Expr.LocalVariable("super",parentClass.type());		
		Expr.Invoke ivk = new Expr.Invoke(target, "super", args,
				new ArrayList(), loc, new JilBuilder.MethodInfo(exceptions,
						type));
		stmts.add(ivk);
		
		Stmt.Block block = new Stmt.Block(stmts,loc);
		
		Decl.JavaMethod mc = new Decl.JavaMethod(mods, name,
				fromJilType(Types.T_VOID), javaparams, false, new ArrayList(),
				new ArrayList(), block, loc, type);
		
		// finally, update skeleton accordingly.
		anonClass.methods().add(new JilMethod(name, type, jilparams,
				new ArrayList<Modifier>(), exceptions));
						
		return mc;
	}
	
	protected Type.Clazz anonClassType(String name) {
		Type.Clazz parent = (Type.Clazz) context.peek().attribute(
				Type.Clazz.class);

		ArrayList<Pair<String, List<Type.Reference>>> ncomponents = new ArrayList(
				parent.components());
		ncomponents.add(new Pair(name, new ArrayList()));
		return new Type.Clazz(parent.pkg(), ncomponents);
	}
}
