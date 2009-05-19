package jkit.java.stages;

import static jkit.compiler.SyntaxError.syntax_error;

import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.compiler.Clazz;
import jkit.compiler.FieldNotFoundException;
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
import jkit.jil.tree.JilMethod;
import jkit.jil.tree.JilStmt;
import jkit.jil.tree.JilExpr;
import jkit.jil.tree.Modifier;
import jkit.jil.tree.Type;
import jkit.util.Triple;

/**
 * <p>
 * The purpose of this stage is to add <code>access</code> methods to classes
 * containing inner classes where necessary. Such accessors are required when an
 * inner class attempts to access a private field of an enclosing class. For
 * example, consider the following:
 * </p>
 * 
 * <pre>
 * class Parent {
 * 	private String outer;
 * 
 * 	public static class Inner {
 * 		public void f() {
 * 			outer = &quot;Hello World&quot;;
 * 		}
 * 	}
 * }
 * </pre>
 * 
 * <p>
 * Now, compiling these classes without adding an accessor would cause a
 * problem, since the JVM does not permit private fields to be accessed from
 * other classes. That is, it would not permit field <code>Parent.outer</code>
 * to be accessed from the separate class <code>Parent$Inner</code>.
 * </p>
 * <p>
 * To workaround this problem, the Java compiler inserts <code>access</code>
 * methods. We can think of this as applying a simple transformation of the
 * original program before compilation. So, for example, the above program is
 * transformed into the following before being compiled:
 * </p>
 * 
 * <pre>
 * class Parent {
 * 	private String outer;
 * 
 * 	String access$001(String tmp) {
 * 		out = tmp;
 * 	}
 * 
 * 	public static class Inner {
 * 		public void f() {
 * 			access$001(&quot;Hello World&quot;);
 * 		}
 * 	}
 * }
 * </pre>
 * 
 * <p>
 * Here, we see that the access method has two interesting properties: firstly,
 * it has a name which cannot be expressed in Java source code; secondly, it has
 * <code>package</code> visibility (since no <code>public</code>/<code>protected</code>/<code>private</code>
 * modifier is given).
 * </p>
 * The naming scheme for access methods is as follows:
 * <ol>
 * <li><b>access$xy0</b> - indicates a read access of some field.</li>
 * <li><b>access$xy2</b> - indicates a write access of some field.</li>
 * <li><b>access$0yz</b> - indicates some kind of access to the first such
 * access encountered in the source file. Then, 1xy would indicate the next, and
 * so on.</li>
 * </ol>
 * <p>
 * Thefore, this stage traverses the source file looking for such inner class
 * accesses, and inserting <code>access</code> methods where appropriate.
 * </p>
 * 
 * @author djp
 * 
 */
public class InnerClassAccessors {
	private ClassLoader loader;	
	private TypeSystem types;
	private final Stack<Type.Clazz> enclosingClasses = new Stack<Type.Clazz>();
	private final HashMap<Type.Clazz, HashMap<String, JilMethod>> readAccessors = new HashMap();
	
	public InnerClassAccessors(ClassLoader loader, TypeSystem types) {
		this.loader = loader; 
		this.types = types;
	}
	
	public void apply(JavaFile file) {				
		readAccessors.clear();
		
		// Traverse the declarations
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
		enclosingClasses.add((Type.Clazz) c.attribute(Type.class));		
		
		for(Decl d : c.declarations()) {
			doDeclaration(d);
		}
		
		enclosingClasses.pop();
	}

	protected void doMethod(JavaMethod d) {
		doStatement(d.body());		
	}

	protected void doField(JavaField d) {
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
		Type t = (Type) def.type().attribute(Type.class);
		
		for(int i=0;i!=defs.size();++i) {
			Triple<String, Integer, Expr> d = defs.get(i);
			Type nt = t;											
			
			for(int j=0;j!=d.second();++j) {
				nt = new Type.Array(nt);
			}			
									
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
	
	protected Expr doDeref(Expr.Deref e) {
		if(e.target() == null) {
			System.out.println("NULL TARGET");
		}
		e.setTarget(doExpression(e.target()));
		
		Type tmp = (Type) e.target().attribute(Type.class);
		
		if(!(tmp instanceof Type.Reference) || tmp instanceof Type.Array) {
			// don't need to do anything in this case			
		} else {

			Type.Clazz target = (Type.Clazz) tmp;

			if(e.name().equals("this")) {
				// This is a special case, where we're trying to look up a field
				// called "this". No such field can exist! What this means is that
				// we're inside an inner class, and we're trying to access the this
				// pointer of an enclosing class. This is easy to deal with here,
				// since the type returned by this expression will be the target
				// type of the dereference.
				
				// don't need to do anything here.
			} else {
				// now, perform field lookup!
				try {
					Triple<Clazz, Clazz.Field, Type> r = types
							.resolveField(target, e.name(), loader);
					
					Clazz.Field f = r.second();															
					Clazz c = r.first();
										
					if (f.isPrivate()
							&& isStrictInnerClass(enclosingClasses.peek(), c.type())) {
						// Ok, we have found a dereference of a field. This
						// means we need to add an accessor method, unless there
						// already is one.
					
						if(!(c instanceof jkit.jil.tree.JilClass)) {
							// it should be impossible to get here.
							syntax_error(
									"internal failure --- jil class required, found "
											+ c.getClass().getName(), e);
						}
												
						ArrayList<jkit.jil.tree.Attribute> attributes = new ArrayList(e.attributes());
						Clazz.Method accessor = createReadAccessor(f, (jkit.jil.tree.JilClass) c);
						attributes.add(new JilBuilder.MethodInfo(accessor.exceptions(),accessor.type()));						
						return new Expr.Invoke(e.target(), accessor.name(),
								new ArrayList<Expr>(), new ArrayList(),
								attributes);
					}
					
				} catch(ClassNotFoundException cne) {
					syntax_error("class not found: " + target,e,cne);
				} catch(FieldNotFoundException fne) {
					syntax_error("field not found: " + target + "." + e.name(),e,fne);
				}
			}
		}	
					
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
		/*
			Type.Clazz superType = (Type.Clazz) e.type().attribute(Type.class);
			ArrayList<Pair<String, List<Type.Reference>>> ncomponents = new ArrayList(
					cs.type.components());
			ncomponents.add(new Pair(Integer.toString(++anonymousClassCount),
					new ArrayList()));
			Type.Clazz myType = new Type.Clazz(cs.type.pkg(), ncomponents);			
			
			for(Decl d : e.declarations()) {
				doDeclaration(d);
			}
			*/
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
	
	protected Expr doLocalVariable(Expr.LocalVariable e) {
		return e;
	}

	protected Expr doNonLocalVariable(Expr.NonLocalVariable e) {
		return e;
	}
	
	protected Expr doClassVariable(Expr.ClassVariable e) {
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
	 * Test whether inner is a strict inner class of parent.
	 * 
	 * @param inner
	 * @param Parent
	 */
	protected boolean isStrictInnerClass(Type.Clazz inner, Type.Clazz parent) {
		if(!inner.pkg().equals(parent.pkg())) {
			return false;
		}
		
		if(inner.components().size() <= parent.components().size()) {
			return false;
		}
		
		for(int i=0;i!=parent.components().size();++i) {
			String parentName = parent.components().get(i).first();
			String innerName = inner.components().get(i).first();
			if(!parentName.equals(innerName)) {
				return false;
			}
		}
		
		return true;
	}
	
	protected Clazz.Method createReadAccessor(Clazz.Field field, jkit.jil.tree.JilClass clazz) {				
		
		// The first thing we need to do is check whether or not we've actually
		// created an accessor already.
		
		HashMap<String,JilMethod> accessors = readAccessors.get(clazz.type());
		
		JilMethod accessor = null;
		
		if(accessors == null) {
			accessors = new HashMap<String,JilMethod>();
			readAccessors.put(clazz.type(),accessors);
		} else {
			accessor = accessors.get(field.name());
		}
		
		if(accessor == null) {
			// no, we haven't so construct one.
			List<Modifier> modifiers = new ArrayList<Modifier>();
			
			if(field.isStatic()) {
				modifiers.add(new Modifier.Base(java.lang.reflect.Modifier.STATIC));
			}
			
			accessor = new JilMethod("access$" + accessors.size() + "00",
					new Type.Function(field.type()), new ArrayList(),
					modifiers, new ArrayList<Type.Clazz>()); 
			
			JilExpr expr = new JilExpr.Deref(new JilExpr.Variable("this", clazz
					.type()), field.name(), field.isStatic(), field.type());
			JilStmt stmt = new JilStmt.Return(expr,field.type());
			
			accessor.body().add(stmt);
			
			accessors.put(field.name(),accessor);
		}
		
		clazz.methods().add(accessor);
		
		return accessor;
	}
}
