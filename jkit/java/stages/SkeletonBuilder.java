package jkit.java.stages;

import java.util.ArrayList;
import java.util.List;

import jkit.compiler.SyntaxError;
import jkit.compiler.ClassLoader;
import jkit.java.io.JavaFile;
import jkit.java.tree.Decl;
import jkit.java.tree.Expr;
import jkit.java.tree.Stmt;
import jkit.java.tree.Value;
import jkit.java.tree.Stmt.Case;
import jkit.jil.Clazz;
import jkit.jil.Method;
import jkit.jil.Field;
import jkit.jil.Modifier;
import jkit.jil.Type;
import jkit.jil.SourceLocation;
import jkit.jil.SyntacticElement;
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
	private ClassLoader loader = null;
	
	public SkeletonBuilder(ClassLoader loader) {
		this.loader = loader;
	}
	
	public void apply(JavaFile file) {		
		this.file = file;
		// Now, traverse the declarations
		for(Decl d : file.declarations()) {
			doDeclaration(d,null);
		}
	}
	
	protected void doDeclaration(Decl d, Clazz skeleton) {
		if(d instanceof Decl.Interface) {
			doInterface((Decl.Interface)d, skeleton);
		} else if(d instanceof Decl.Clazz) {
			doClass((Decl.Clazz)d, skeleton);
		} else if(d instanceof Decl.Method) {
			doMethod((Decl.Method)d, skeleton);
		} else if(d instanceof Decl.Field) {
			doField((Decl.Field)d, skeleton);
		} else if (d instanceof Decl.StaticInitialiserBlock) {
			doStaticInitialiserBlock((Decl.StaticInitialiserBlock) d, skeleton);
		} else {
			syntax_error("internal failure (unknown declaration \"" + d
					+ "\" encountered)",d);
		}
	}
			
	protected void doInterface(Decl.Interface d, Clazz skeleton) {
		doClass(d, skeleton);
	}
	
	protected void doClass(Decl.Clazz c, Clazz skeleton) {
		Type.Clazz type = (Type.Clazz) c.attribute(Type.class);
		try {
			// We, need to update the skeleton so that any methods and fields
			// discovered below this are attributed to this class!			
			skeleton = loader.loadClass(type);	
			
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
			if(c instanceof Decl.Enum) {
				Decl.Enum ec = (Decl.Enum) c;
				for(Decl.EnumConstant enc : ec.constants()) {
					Type t = (Type) enc.attribute(Type.class);
					if(enc.declarations().size() > 0) {
						syntax_error("No support for ENUMS that have methods",enc);
					} else {
						List<Modifier> modifiers = new ArrayList<Modifier>();
						modifiers.add(new Modifier.Base(
								java.lang.reflect.Modifier.PUBLIC));
						skeleton.fields().add(
								new Field(enc.name(), t, modifiers,
										new ArrayList(enc.attributes())));
					}
				}
			}
		} catch(ClassNotFoundException cne) {
			syntax_error("internal failure (skeleton for \"" + type
					+ "\" not found)", c, cne);
		}
	}

	protected void doMethod(Decl.Method d, Clazz skeleton) {		
		Decl.Method m = (Decl.Method) d;
		Type.Function t = (Type.Function) m.attribute(Type.class);
		List<Type.Clazz> exceptions = new ArrayList<Type.Clazz>();
		
		for(jkit.java.tree.Type.Clazz tc : m.exceptions()) {
			exceptions.add((Type.Clazz)tc.attribute(Type.class));
		}
		 
		skeleton.methods().add(
				new Method(m.name(), t, m.modifiers(), exceptions,
						new ArrayList(m.attributes())));
		
		doStatement(d.body(), skeleton);
	}

	protected void doField(Decl.Field d, Clazz skeleton) {
		Decl.Field f = (Decl.Field) d;
		Type t = (Type) f.type().attribute(Type.class);
		skeleton.fields().add(
				new Field(f.name(), t, f.modifiers(), new ArrayList(f
						.attributes())));

		doExpression(d.initialiser(), skeleton);
	}
	
	protected void doStaticInitialiserBlock(Decl.StaticInitialiserBlock d,
			Clazz skeleton) {
		// will need to add code here for dealing with classes nested in
		// methods.
		for (Stmt s : d.statements()) {
			doStatement(s, skeleton);
		}
	}
	
	protected void doStatement(Stmt e, Clazz skeleton) {
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
		} else if(e instanceof Decl.Clazz) {
			doClass((Decl.Clazz)e, skeleton);
		} else if(e != null) {
			syntax_error("Internal failure (invalid statement \""
					+ e.getClass() + "\" encountered)", e);			
		}		
	}
	
	protected void doBlock(Stmt.Block block, Clazz skeleton) {
		if(block != null) {			
			// now process every statement in this block.
			for(Stmt s : block.statements()) {
				doStatement(s, skeleton);
			}
		}
	}
	
	protected void doSynchronisedBlock(Stmt.SynchronisedBlock block, Clazz skeleton) {
		doBlock(block, skeleton);
		doExpression(block.expr(), skeleton);
	}
	
	protected void doTryCatchBlock(Stmt.TryCatchBlock block, Clazz skeleton) {
		doBlock(block, skeleton);
		doBlock(block.finaly(), skeleton);

		for (Stmt.CatchBlock cb : block.handlers()) {			
			doBlock(cb, skeleton);
		}
	}
	
	protected void doVarDef(Stmt.VarDef def, Clazz skeleton) {
		List<Triple<String, Integer, Expr>> defs = def.definitions();
		for(int i=0;i!=defs.size();++i) {			
			doExpression(defs.get(i).third(), skeleton);			
		}
	}
	
	protected void doAssignment(Stmt.Assignment def, Clazz skeleton) {
		doExpression(def.lhs(), skeleton);	
		doExpression(def.rhs(), skeleton);			
	}
	
	protected void doReturn(Stmt.Return ret, Clazz skeleton) {
		doExpression(ret.expr(), skeleton);
	}
	
	protected void doThrow(Stmt.Throw ret, Clazz skeleton) {
		doExpression(ret.expr(), skeleton);
	}
	
	protected void doAssert(Stmt.Assert ret, Clazz skeleton) {
		doExpression(ret.expr(), skeleton);
	}
	
	protected void doBreak(Stmt.Break brk, Clazz skeleton) {
		// nothing	
	}
	
	protected void doContinue(Stmt.Continue brk, Clazz skeleton) {
		// nothing
	}
	
	protected void doLabel(Stmt.Label lab, Clazz skeleton) {						
		doStatement(lab.statement(), skeleton);
	}
	
	protected void doIf(Stmt.If stmt, Clazz skeleton) {
		doExpression(stmt.condition(), skeleton);
		doStatement(stmt.trueStatement(), skeleton);
		doStatement(stmt.falseStatement(), skeleton);
	}
	
	protected void doWhile(Stmt.While stmt, Clazz skeleton) {
		doExpression(stmt.condition(), skeleton);
		doStatement(stmt.body(), skeleton);		
	}
	
	protected void doDoWhile(Stmt.DoWhile stmt, Clazz skeleton) {
		doExpression(stmt.condition(), skeleton);
		doStatement(stmt.body(), skeleton);
	}
	
	protected void doFor(Stmt.For stmt, Clazz skeleton) {		
		doStatement(stmt.initialiser(), skeleton);
		doExpression(stmt.condition(), skeleton);
		doStatement(stmt.increment(), skeleton);
		doStatement(stmt.body(), skeleton);	
	}
	
	protected void doForEach(Stmt.ForEach stmt, Clazz skeleton) {
		doExpression(stmt.source(), skeleton);
		doStatement(stmt.body(), skeleton);
	}
	
	protected void doSwitch(Stmt.Switch sw, Clazz skeleton) {
		doExpression(sw.condition(), skeleton);
		for(Case c : sw.cases()) {
			doExpression(c.condition(), skeleton);
			for(Stmt s : c.statements()) {
				doStatement(s, skeleton);
			}
		}
		
		// should check that case conditions are final constants here.
	}
	
	protected void doExpression(Expr e, Clazz skeleton) {	
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
	
	protected void doDeref(Expr.Deref e, Clazz skeleton) {		
		doExpression(e.target(), skeleton);					
	}
	
	protected void doArrayIndex(Expr.ArrayIndex e, Clazz skeleton) {
		doExpression(e.target(), skeleton);
		doExpression(e.index(), skeleton);		
	}
	
	protected void doNew(Expr.New e, Clazz skeleton) {
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
				
				Clazz c = loader.loadClass(superType);
							
				ArrayList<Pair<String, List<Type.Reference>>> ncomponents = new ArrayList(
						skeleton.type().components());
				ncomponents.add(new Pair(Integer.toString(++anonymousClassCount),
						new ArrayList()));
				Type.Clazz myType = new Type.Clazz(skeleton.type().pkg(),
						ncomponents);
				
				if (c.isInterface()) {
					// In this case, we're extending from an interface rather
					// than a super class.
					ArrayList<Type.Clazz> interfaces = new ArrayList<Type.Clazz>();
					interfaces.add(superType);
					skeleton = new Clazz(myType, new ArrayList<Modifier>(),
							new Type.Clazz("java.lang", "Object"), interfaces,
							new ArrayList<Field>(), new ArrayList<Method>(), e
									.attributes());
				} else {
					// In this case, we're extending directly from a super class.
					skeleton = new Clazz(myType, new ArrayList<Modifier>(),
							superType, new ArrayList<Type.Clazz>(),
							new ArrayList<Field>(), new ArrayList<Method>(), e
									.attributes());
				}
				
				loader.register(skeleton);

				for(Decl d : e.declarations()) {
					doDeclaration(d, skeleton);
				}
				
			} catch (ClassNotFoundException cne) {
				syntax_error("Unable to load class " + superType, e, cne);
			}
		}
	}
	
	protected void doInvoke(Expr.Invoke e, Clazz skeleton) {
		doExpression(e.target(), skeleton);
		
		for(Expr p : e.parameters()) {
			doExpression(p, skeleton);
		}
	}
	
	protected void doInstanceOf(Expr.InstanceOf e, Clazz skeleton) {		
		doExpression(e.lhs(), skeleton);		
	}
	
	protected void doCast(Expr.Cast e, Clazz skeleton) {
		doExpression(e.expr(), skeleton);
	}
	
	protected void doBoolVal(Value.Bool e, Clazz skeleton) {		
	}
	
	protected void doCharVal(Value.Char e, Clazz skeleton) {		
	}
	
	protected void doIntVal(Value.Int e, Clazz skeleton) {		
	}
	
	protected void doLongVal(Value.Long e, Clazz skeleton) {				
	}
	
	protected void doFloatVal(Value.Float e, Clazz skeleton) {				
	}
	
	protected void doDoubleVal(Value.Double e, Clazz skeleton) {				
	}
	
	protected void doStringVal(Value.String e, Clazz skeleton) {				
	}
	
	protected void doNullVal(Value.Null e, Clazz skeleton) {		
	}
	
	protected void doTypedArrayVal(Value.TypedArray e, Clazz skeleton) {		
	}
	
	protected void doArrayVal(Value.Array e, Clazz skeleton) {		
	}
	
	protected void doClassVal(Value.Class e, Clazz skeleton) {
	}
	
	protected void doUnresolvedVariable(Expr.UnresolvedVariable e, Clazz skeleton) {		
	}
	
	protected void doClassVariable(Expr.ClassVariable e, Clazz skeleton) {	
	}
	
	protected void doUnOp(Expr.UnOp e, Clazz skeleton) {		
		doExpression(e.expr(), skeleton);		
	}
		
	protected void doBinOp(Expr.BinOp e, Clazz skeleton) {				
		doExpression(e.lhs(), skeleton);
		doExpression(e.rhs(), skeleton);		
	}
	
	protected void doTernOp(Expr.TernOp e, Clazz skeleton) {		
		doExpression(e.condition(), skeleton);
		doExpression(e.falseBranch(), skeleton);
		doExpression(e.trueBranch(), skeleton);		
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
