package jkit.java.stages;

import static jkit.compiler.SyntaxError.syntax_error;

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
	private final Stack<Type.Clazz> context = new Stack<Type.Clazz>(); 
	
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
		Type.Clazz type = (Type.Clazz) c.attribute(Type.Clazz.class);
		context.push(type);
		for(Decl d : c.declarations()) {
			doDeclaration(d);
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
			Type.Clazz aType = anonClassType(name,context.peek());
				
			try {
				JilClass parentClass = (JilClass) loader.loadClass(parent);
				JilClass anonClass = (JilClass) loader.loadClass(aType);

				// First, update the type of the new expression
				e.type().attributes().remove(parent);
				e.type().attributes().add(aType);

				// Second, add an appropriate constructor.
				JilBuilder.MethodInfo mi = (JilBuilder.MethodInfo) e
				.attribute(JilBuilder.MethodInfo.class);
				JilMethod cm = buildConstructor(name, mi.type, mi.exceptions,
						parentClass,anonClass);
					
				anonClass.methods().add(cm);
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
	
	protected JilMethod buildConstructor(String name, Type.Function type,
			ArrayList<Type.Clazz> exceptions, JilClass parent, JilClass owner) {
		
		ArrayList<Pair<String, List<Modifier>>> params = new ArrayList();
		ArrayList<JilExpr> args = new ArrayList();
		Type.Function superCallType = type; 		
		
		// superParentPtr is true if the super class requiers a parent ptr
		boolean superParentPtr = (parent.isInnerClass() && !parent.isStatic());
		// likewise, the parent ptr is true if the anon class requires a parent ptr.
		boolean myParentPtr = !owner.isStatic();
		
		
		if (superParentPtr || myParentPtr) {
			// non-static anon class constructor requires parent
			// pointer.
			ArrayList<Type> ptypes = new ArrayList<Type>(type.parameterTypes());
			ptypes.add(0,context.peek());
		    type = new Type.Function(type.returnType(),ptypes);
		    
		    if(superParentPtr) {
		    	superCallType = type;
		    }
		}
						
		ArrayList<Modifier> mods = new ArrayList();
		mods.add(new Modifier.Base(java.lang.reflect.Modifier.FINAL));
		int p = 0;
						
		for (Type t : type.parameterTypes()) {
			// don't include the first parameter *if* it's the parent pointer,
			// and the super class is static.			
			params.add(new Pair("x" + p, mods));
			if(p != 0 || superParentPtr) {
				args.add(new JilExpr.Variable("x" + p, t));
			}
			p = p + 1;
		}
		
		JilMethod m = new JilMethod(name, type, params,
				new ArrayList<Modifier>(), exceptions);		
		
		JilExpr.Variable ths = new JilExpr.Variable("this", parent.type());
		JilExpr.Invoke ivk = new JilExpr.Invoke(ths, "super", args, superCallType,
				Types.T_VOID);

		m.body().add(ivk);
		m.body().add(new JilStmt.Return(null));

		return m;
	}
	
	protected Type.Clazz anonClassType(String name, Type.Clazz parent) {
		ArrayList<Pair<String, List<Type.Reference>>> ncomponents = new ArrayList(
				parent.components());
		ncomponents.add(new Pair(name, new ArrayList()));
		return new Type.Clazz(parent.pkg(), ncomponents);
	}
}
