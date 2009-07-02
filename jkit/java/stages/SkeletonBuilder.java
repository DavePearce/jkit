package jkit.java.stages;

import java.util.*;

import static jkit.compiler.SyntaxError.*;
import jkit.compiler.*;
import jkit.compiler.ClassLoader;
import jkit.java.io.JavaFile;
import jkit.java.tree.Decl;
import jkit.java.tree.Expr;
import jkit.java.tree.Stmt;
import jkit.java.tree.Value;
import jkit.java.tree.Stmt.Case;
import jkit.jil.tree.JilClass;
import jkit.jil.tree.JilField;
import jkit.jil.tree.JilConstant;
import jkit.jil.tree.JilMethod;
import jkit.jil.tree.Modifier;
import jkit.jil.tree.SourceLocation;
import jkit.jil.tree.Type;
import jkit.util.*;

/**
 * The purpose of the skeleton builder is to flesh out the skeletons identified
 * during discovery, so that they now include fully qualified type information,
 * superclasses and interfaces, fields and methods. However, the skeletons do
 * not include any executable code; in particular, field initialisers and method
 * bodies are not included in the skeletons. This is because, at the point where
 * this stage is run, we do not have sufficient information to type such code
 * correctly. Finally, anonymous inner classes are discovered for the first time
 * during this phase.
 * 
 * @author djp
 * 
 */

public class SkeletonBuilder {
	private int anonymousClassCount = 0;
	private JavaFile file;
	private ArrayList<JilClass> skeletons;
	private ClassLoader loader = null;
	private final Stack<Decl> context = new Stack();
	
	public SkeletonBuilder(ClassLoader loader) {
		this.loader = loader;
	}
	
	public List<JilClass> apply(JavaFile file) {		
		anonymousClassCount = 0;
		this.file = file;
		this.skeletons = new ArrayList<JilClass>();
					
		// Now, traverse the declarations
		for(Decl d : file.declarations()) {
			doDeclaration(d,null);
		}
		
		return skeletons;
	}
	
	protected void doDeclaration(Decl d, JilClass skeleton) {		
		context.push(d);
		
		if(d instanceof Decl.JavaInterface) {
			doInterface((Decl.JavaInterface)d, skeleton);
		} else if(d instanceof Decl.JavaClass) {
			doClass((Decl.JavaClass)d, skeleton);
		} else if(d instanceof Decl.JavaMethod) {
			doMethod((Decl.JavaMethod)d, skeleton);
		} else if(d instanceof Decl.JavaField) {
			doField((Decl.JavaField)d, skeleton);
		} else if (d instanceof Decl.InitialiserBlock) {
			doInitialiserBlock((Decl.InitialiserBlock) d, skeleton);
		} else if (d instanceof Decl.StaticInitialiserBlock) {
			doStaticInitialiserBlock((Decl.StaticInitialiserBlock) d, skeleton);
		} else {
			syntax_error("internal failure (unknown declaration \"" + d
					+ "\" encountered)",d);
		}
		
		context.pop();		
	}
			
	protected void doInterface(Decl.JavaInterface d, JilClass skeleton) {
		doClass(d, skeleton);
	}
	
	protected void doClass(Decl.JavaClass c, JilClass skeleton) {
		Type.Clazz type = (Type.Clazz) c.attribute(Type.class);
		try {
			// We, need to update the skeleton so that any methods and fields
			// discovered below this are attributed to this class!			
			skeleton = (JilClass) loader.loadClass(type);	
			
			// Next, we need to update as much information about the skeleton as
			// we can.
			Type.Clazz superClass = new Type.Clazz("java.lang","Object");
			
			if(c.superclass() != null) {
				// Observe, after type resolution, this will give the correct
				// superclass type. However, prior to type resolution it will just
				// return null.
				superClass = (Type.Clazz) c.superclass().attribute(Type.class);
			}			
			
			ArrayList<Type.Clazz> interfaces = new ArrayList();
			for(jkit.java.tree.Type.Clazz i : c.interfaces()) {
				Type.Clazz t = (Type.Clazz) i.attribute(Type.class);
				if(t != null) {
					interfaces.add(t);
				}
			}
			
			skeleton.setType(type);
			skeleton.setSuperClass(superClass);
			skeleton.setInterfaces(interfaces);
			
			// ok, now update information for declarations in this skeleton.
			for(Decl d : c.declarations()) {
				doDeclaration(d, skeleton);
			}		
			
			// Now, deal with some special cases when this is not actually a
			// class			
			if(c instanceof Decl.JavaEnum) {
				Decl.JavaEnum ec = (Decl.JavaEnum) c;
				for(Decl.EnumConstant enc : ec.constants()) {
					if(enc.declarations().size() > 0) {
						syntax_error("No support for ENUMS that have methods",enc);
					} else {
						List<Modifier> modifiers = new ArrayList<Modifier>();
						modifiers.add(Modifier.ACC_PUBLIC);
						modifiers.add(Modifier.ACC_STATIC);
						modifiers.add(Modifier.ACC_FINAL);
						skeleton.fields().add(
								new JilField(enc.name(), type, modifiers,
										new ArrayList(enc.attributes())));
					}
				}
			}
			
			if (!skeleton.isInterface()
					&& skeleton.methods(skeleton.name()).isEmpty()) {
				// if we get here, then no constructor has been provided.
				// Therefore, must add the default constructor.
				SourceLocation loc = (SourceLocation) c.attribute(SourceLocation.class);
				Decl.JavaMethod m = createDefaultConstructor(skeleton.name(),loc);
				c.declarations().add(m);	
								
				// Finally, add the constructor to the skeleton.
				doDeclaration(m,skeleton);				
			}
		} catch(ClassNotFoundException cne) {
			syntax_error("internal failure (skeleton for \"" + type
					+ "\" not found)", c, cne);
		}		
	}

	protected void doMethod(Decl.JavaMethod d, JilClass skeleton) {		
		Type.Function type = (Type.Function) d.attribute(Type.class);
		List<Type.Clazz> exceptions = new ArrayList<Type.Clazz>();
		List<Pair<String,List<Modifier>>> parameters = new ArrayList();
		
		for(Triple<String,List<Modifier>,jkit.java.tree.Type> t : d.parameters()) {
			parameters.add(new Pair(t.first(),t.second()));
		}
		
		for(jkit.java.tree.Type.Clazz tc : d.exceptions()) {
			exceptions.add((Type.Clazz)tc.attribute(Type.class));
		}				
		
		skeleton.methods().add(
				new JilMethod(d.name(), type, parameters, d.modifiers(),
						exceptions, new ArrayList(d.attributes())));				
		
		doStatement(d.body(), skeleton);
	}

	protected void doField(Decl.JavaField f, JilClass skeleton) {				
		Type t = (Type) f.type().attribute(Type.class);	
		
		if(skeleton.isInterface()) {
			if(f.isConstant()) {
				ArrayList<Modifier> mods = new ArrayList(f.modifiers());
				if(!f.isProtected()) {
					mods.add(Modifier.ACC_PUBLIC);
				} 
				if(!f.isFinal()) {
					mods.add(Modifier.ACC_FINAL);
				}
				if(!f.isStatic()) {
					mods.add(Modifier.ACC_STATIC);
				}
				skeleton.fields().add(
						new JilConstant(f.name(), t, f.constant(), mods,
								new ArrayList(f.attributes())));
			} else {
				syntax_error("invalid constant initialiser",f);
			}
		} else if(f.isConstant() && f.isStatic() && f.isFinal()) {
			// Ok, this is actually a constant field.
			skeleton.fields().add(
					new JilConstant(f.name(), t, f.constant(), f.modifiers(),
							new ArrayList(f.attributes())));
		} else {
			skeleton.fields().add(
					new JilField(f.name(), t, f.modifiers(), new ArrayList(f
							.attributes())));
		}

		doExpression(f.initialiser(), skeleton);		
	}
	
	protected void doInitialiserBlock(Decl.InitialiserBlock d,
			JilClass skeleton) {		
		// will need to add code here for dealing with classes nested in
		// methods.
		for (Stmt s : d.statements()) {
			doStatement(s, skeleton);
		}
	}
	
	protected void doStaticInitialiserBlock(Decl.StaticInitialiserBlock d,
			JilClass skeleton) {
		// will need to add code here for dealing with classes nested in
		// methods.
		for (Stmt s : d.statements()) {
			doStatement(s, skeleton);
		}
	}
	
	protected void doStatement(Stmt e, JilClass skeleton) {
		if(e instanceof Stmt.SynchronisedBlock) {
			doSynchronisedBlock((Stmt.SynchronisedBlock)e, skeleton);
		} else if(e instanceof Stmt.TryCatchBlock) {
			doTryCatchBlock((Stmt.TryCatchBlock)e, skeleton);
		} else if(e instanceof Stmt.Block) {
			doBlock((Stmt.Block)e, skeleton);
		} else if(e instanceof Stmt.VarDef) {
			doVarDef((Stmt.VarDef) e, skeleton);
		} else if(e instanceof Stmt.Assignment) {
			doAssignment((Stmt.Assignment) e, skeleton);
		} else if(e instanceof Stmt.Return) {
			doReturn((Stmt.Return) e, skeleton);
		} else if(e instanceof Stmt.Throw) {
			doThrow((Stmt.Throw) e, skeleton);
		} else if(e instanceof Stmt.Assert) {
			doAssert((Stmt.Assert) e, skeleton);
		} else if(e instanceof Stmt.Break) {
			doBreak((Stmt.Break) e, skeleton);
		} else if(e instanceof Stmt.Continue) {
			doContinue((Stmt.Continue) e, skeleton);
		} else if(e instanceof Stmt.Label) {
			doLabel((Stmt.Label) e, skeleton);
		} else if(e instanceof Stmt.If) {
			doIf((Stmt.If) e, skeleton);
		} else if(e instanceof Stmt.For) {
			doFor((Stmt.For) e, skeleton);
		} else if(e instanceof Stmt.ForEach) {
			doForEach((Stmt.ForEach) e, skeleton);
		} else if(e instanceof Stmt.While) {
			doWhile((Stmt.While) e, skeleton);
		} else if(e instanceof Stmt.DoWhile) {
			doDoWhile((Stmt.DoWhile) e, skeleton);
		} else if(e instanceof Stmt.Switch) {
			doSwitch((Stmt.Switch) e, skeleton);
		} else if(e instanceof Expr.Invoke) {
			doInvoke((Expr.Invoke) e, skeleton);
		} else if(e instanceof Expr.New) {
			doNew((Expr.New) e, skeleton);
		} else if(e instanceof Decl.JavaClass) {
			doClass((Decl.JavaClass)e, skeleton);
		} else if(e instanceof Stmt.PrePostIncDec) {
			doExpression((Stmt.PrePostIncDec)e, skeleton);
		} else if(e != null) {
			syntax_error("Internal failure (invalid statement \""
					+ e.getClass() + "\" encountered)", e);			
		}		
	}
	
	protected void doBlock(Stmt.Block block, JilClass skeleton) {
		if(block != null) {			
			// now process every statement in this block.
			for(Stmt s : block.statements()) {
				doStatement(s, skeleton);
			}
		}
	}
	
	protected void doSynchronisedBlock(Stmt.SynchronisedBlock block, JilClass skeleton) {
		doBlock(block, skeleton);
		doExpression(block.expr(), skeleton);
	}
	
	protected void doTryCatchBlock(Stmt.TryCatchBlock block, JilClass skeleton) {
		doBlock(block, skeleton);
		doBlock(block.finaly(), skeleton);

		for (Stmt.CatchBlock cb : block.handlers()) {			
			doBlock(cb, skeleton);
		}
	}
	
	protected void doVarDef(Stmt.VarDef def, JilClass skeleton) {
		List<Triple<String, Integer, Expr>> defs = def.definitions();
		for(int i=0;i!=defs.size();++i) {			
			doExpression(defs.get(i).third(), skeleton);			
		}
	}
	
	protected void doAssignment(Stmt.Assignment def, JilClass skeleton) {
		doExpression(def.lhs(), skeleton);	
		doExpression(def.rhs(), skeleton);			
	}
	
	protected void doReturn(Stmt.Return ret, JilClass skeleton) {
		doExpression(ret.expr(), skeleton);
	}
	
	protected void doThrow(Stmt.Throw ret, JilClass skeleton) {
		doExpression(ret.expr(), skeleton);
	}
	
	protected void doAssert(Stmt.Assert ret, JilClass skeleton) {
		doExpression(ret.expr(), skeleton);
	}
	
	protected void doBreak(Stmt.Break brk, JilClass skeleton) {
		// nothing	
	}
	
	protected void doContinue(Stmt.Continue brk, JilClass skeleton) {
		// nothing
	}
	
	protected void doLabel(Stmt.Label lab, JilClass skeleton) {						
		doStatement(lab.statement(), skeleton);
	}
	
	protected void doIf(Stmt.If stmt, JilClass skeleton) {
		doExpression(stmt.condition(), skeleton);
		doStatement(stmt.trueStatement(), skeleton);
		doStatement(stmt.falseStatement(), skeleton);
	}
	
	protected void doWhile(Stmt.While stmt, JilClass skeleton) {
		doExpression(stmt.condition(), skeleton);
		doStatement(stmt.body(), skeleton);		
	}
	
	protected void doDoWhile(Stmt.DoWhile stmt, JilClass skeleton) {
		doExpression(stmt.condition(), skeleton);
		doStatement(stmt.body(), skeleton);
	}
	
	protected void doFor(Stmt.For stmt, JilClass skeleton) {		
		doStatement(stmt.initialiser(), skeleton);
		doExpression(stmt.condition(), skeleton);
		doStatement(stmt.increment(), skeleton);
		doStatement(stmt.body(), skeleton);	
	}
	
	protected void doForEach(Stmt.ForEach stmt, JilClass skeleton) {
		doExpression(stmt.source(), skeleton);
		doStatement(stmt.body(), skeleton);
	}
	
	protected void doSwitch(Stmt.Switch sw, JilClass skeleton) {
		doExpression(sw.condition(), skeleton);
		for(Case c : sw.cases()) {
			doExpression(c.condition(), skeleton);
			for(Stmt s : c.statements()) {
				doStatement(s, skeleton);
			}
		}
		
		// should check that case conditions are final constants here.
	}
	
	protected void doExpression(Expr e, JilClass skeleton) {	
		if(e instanceof Value.Bool) {
			doBoolVal((Value.Bool)e, skeleton);
		} else if(e instanceof Value.Char) {
			doCharVal((Value.Char)e, skeleton);
		} else if(e instanceof Value.Int) {
			doIntVal((Value.Int)e, skeleton);
		} else if(e instanceof Value.Long) {
			doLongVal((Value.Long)e, skeleton);
		} else if(e instanceof Value.Float) {
			doFloatVal((Value.Float)e, skeleton);
		} else if(e instanceof Value.Double) {
			doDoubleVal((Value.Double)e, skeleton);
		} else if(e instanceof Value.String) {
			doStringVal((Value.String)e, skeleton);
		} else if(e instanceof Value.Null) {
			doNullVal((Value.Null)e, skeleton);
		} else if(e instanceof Value.TypedArray) {
			doTypedArrayVal((Value.TypedArray)e, skeleton);
		} else if(e instanceof Value.Array) {
			doArrayVal((Value.Array)e, skeleton);
		} else if(e instanceof Value.Class) {
			doClassVal((Value.Class) e, skeleton);
		} else if(e instanceof Expr.UnresolvedVariable) {
			doUnresolvedVariable((Expr.UnresolvedVariable)e, skeleton);
		} else if(e instanceof Expr.ClassVariable) {
			doClassVariable((Expr.ClassVariable)e, skeleton);
		} else if(e instanceof Expr.UnOp) {
			doUnOp((Expr.UnOp)e, skeleton);
		} else if(e instanceof Expr.BinOp) {
			doBinOp((Expr.BinOp)e, skeleton);
		} else if(e instanceof Expr.TernOp) {
			doTernOp((Expr.TernOp)e, skeleton);
		} else if(e instanceof Expr.Cast) {
			doCast((Expr.Cast)e, skeleton);
		} else if(e instanceof Expr.InstanceOf) {
			doInstanceOf((Expr.InstanceOf)e, skeleton);
		} else if(e instanceof Expr.Invoke) {
			doInvoke((Expr.Invoke) e, skeleton);
		} else if(e instanceof Expr.New) {
			doNew((Expr.New) e, skeleton);
		} else if(e instanceof Expr.ArrayIndex) {
			doArrayIndex((Expr.ArrayIndex) e, skeleton);
		} else if(e instanceof Expr.Deref) {
			doDeref((Expr.Deref) e, skeleton);
		} else if(e instanceof Stmt.Assignment) {
			// force brackets			
			doAssignment((Stmt.Assignment) e, skeleton);			
		} else if(e != null) {
			syntax_error("Internal failure (invalid expression \""
					+ e.getClass() + "\" encountered)", e);			
		}
	}
	
	protected void doDeref(Expr.Deref e, JilClass skeleton) {		
		doExpression(e.target(), skeleton);					
	}
	
	protected void doArrayIndex(Expr.ArrayIndex e, JilClass skeleton) {
		doExpression(e.target(), skeleton);
		doExpression(e.index(), skeleton);		
	}
	
	protected void doNew(Expr.New e, JilClass skeleton) {
		// Second, recurse through any parameters supplied ...
		for(Expr p : e.parameters()) {
			doExpression(p, skeleton);
		}
		
		if(e.declarations().size() > 0) {
			// Ok, this represents an anonymous class declaration. Since
			// anonymous classes are not discovered during skeleton discovery,
			// the class loader will not know about them.
			
			Type.Clazz superType = (Type.Clazz) e.type().attribute(Type.class);
			
			try {								
				Clazz superClazz = (Clazz) loader.loadClass(superType);
				String name = Integer.toString(++anonymousClassCount);
				
				ArrayList<Pair<String, List<Type.Reference>>> ncomponents = new ArrayList(
						skeleton.type().components());
				ncomponents.add(new Pair(name,
						new ArrayList()));
				Type.Clazz myType = new Type.Clazz(skeleton.type().pkg(),
						ncomponents);											
				
				ArrayList<Modifier> modifiers = new ArrayList<Modifier>();
				
				if(inStaticContext()) {
					modifiers.add(Modifier.ACC_STATIC);
				}
				
				if (superClazz.isInterface()) {
					// In this case, we're extending from an interface rather
					// than a super class.
					ArrayList<Type.Clazz> interfaces = new ArrayList<Type.Clazz>();
					interfaces.add(superType);										
					
					skeleton = new JilClass(myType, modifiers,
							new Type.Clazz("java.lang", "Object"), interfaces,
							new ArrayList<Type.Clazz>(),
							new ArrayList<JilField>(),
							new ArrayList<JilMethod>(), e.attributes());									
				} else {
					// In this case, we're extending directly from a super
					// class.
					skeleton = new JilClass(myType, modifiers,
							superType, new ArrayList<Type.Clazz>(),
							new ArrayList<Type.Clazz>(),
							new ArrayList<JilField>(),
							new ArrayList<JilMethod>(), e.attributes());
				}																				
				
				skeletons.add(skeleton);
				loader.register(skeleton);

				for(Decl d : e.declarations()) {
					doDeclaration(d, skeleton);
				}								
			} catch (ClassNotFoundException cne) {
				syntax_error("Unable to load class " + superType, e, cne);
			}
		}
	}
	
	protected void doInvoke(Expr.Invoke e, JilClass skeleton) {
		doExpression(e.target(), skeleton);
		
		for(Expr p : e.parameters()) {
			doExpression(p, skeleton);
		}
	}
	
	protected void doInstanceOf(Expr.InstanceOf e, JilClass skeleton) {		
		doExpression(e.lhs(), skeleton);		
	}
	
	protected void doCast(Expr.Cast e, JilClass skeleton) {
		doExpression(e.expr(), skeleton);
	}
	
	protected void doBoolVal(Value.Bool e, JilClass skeleton) {		
	}
	
	protected void doCharVal(Value.Char e, JilClass skeleton) {		
	}
	
	protected void doIntVal(Value.Int e, JilClass skeleton) {		
	}
	
	protected void doLongVal(Value.Long e, JilClass skeleton) {				
	}
	
	protected void doFloatVal(Value.Float e, JilClass skeleton) {				
	}
	
	protected void doDoubleVal(Value.Double e, JilClass skeleton) {				
	}
	
	protected void doStringVal(Value.String e, JilClass skeleton) {				
	}
	
	protected void doNullVal(Value.Null e, JilClass skeleton) {		
	}
	
	protected void doTypedArrayVal(Value.TypedArray e, JilClass skeleton) {		
	}
	
	protected void doArrayVal(Value.Array e, JilClass skeleton) {		
	}
	
	protected void doClassVal(Value.Class e, JilClass skeleton) {
	}
	
	protected void doUnresolvedVariable(Expr.UnresolvedVariable e, JilClass skeleton) {		
	}
	
	protected void doClassVariable(Expr.ClassVariable e, JilClass skeleton) {	
	}
	
	protected void doUnOp(Expr.UnOp e, JilClass skeleton) {		
		doExpression(e.expr(), skeleton);		
	}
		
	protected void doBinOp(Expr.BinOp e, JilClass skeleton) {				
		doExpression(e.lhs(), skeleton);
		doExpression(e.rhs(), skeleton);		
	}
	
	protected void doTernOp(Expr.TernOp e, JilClass skeleton) {		
		doExpression(e.condition(), skeleton);
		doExpression(e.falseBranch(), skeleton);
		doExpression(e.trueBranch(), skeleton);		
	}	
	
	protected Decl.JavaMethod createDefaultConstructor(String name,
			SourceLocation loc) {

		ArrayList<Modifier> mods = new ArrayList<Modifier>();
		mods.add(Modifier.ACC_PUBLIC);		
		Expr.Invoke ivk = new Expr.Invoke(null, "super", new ArrayList(),
				new ArrayList(), loc);
		ArrayList<Stmt> stmts = new ArrayList();
		stmts.add(ivk);
		Stmt.Block block = new Stmt.Block(stmts, loc);

		Decl.JavaMethod m = new Decl.JavaMethod(mods, name,
				new jkit.java.tree.Type.Void(), new ArrayList(), false,
				new ArrayList(), new ArrayList(), block, loc);

		m.attributes().add(new Type.Function(new Type.Void()));
		
		return m;
	}
	
	protected boolean inStaticContext() {
		Decl d = context.peek();
		
		if(d instanceof Decl.StaticInitialiserBlock) {
			return true;
		} else if(d instanceof Decl.JavaMethod) {
			return ((Decl.JavaMethod)d).isStatic();
		} else if(d instanceof Decl.JavaField) {
			return ((Decl.JavaField)d).isStatic();
		} else if(d instanceof Decl.JavaClass) {
			return ((Decl.JavaClass)d).isStatic();
		}
		
		return false;
	}
}
