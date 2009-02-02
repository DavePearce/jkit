package jkit.java.stages;

import java.util.*;
import jkit.compiler.ClassLoader;
import jkit.java.Decl;
import jkit.java.Expr;
import jkit.java.JavaFile;
import jkit.java.Stmt;
import jkit.java.Value;
import jkit.java.Decl.Clazz;
import jkit.java.Decl.Field;
import jkit.java.Decl.Interface;
import jkit.java.Decl.Method;
import jkit.java.Stmt.Case;
import jkit.java.FieldNotFoundException;
import jkit.jil.Modifier;
import jkit.jil.Type;
import jkit.util.Pair;
import jkit.util.Triple;

/**
 * The aim of this class is purely to eliminate ambiguities over the scope of a
 * variable. There are several different situations that can arise for a given
 * variable:
 * 
 * <ol>
 * <li>It is declared locally (this is the easiest)</li>
 * <li>It is declared as a field of the current class</li>
 * <li>It is declared as a field of a superclass for the current class</li>
 * <li>It is declared as a field of an enclosing class (i.e. the current class
 * is an inner class)</li>
 * <li>It is declared as a field of a superclass of the enclosing class</li>
 * <li>It is declared as a local variable for an enclosing method (i.e. the
 * variable is used in an anonymous inner class, and the enclosing method has
 * final variable of the appropriate name).</li>
 * </ol>
 * 
 * As an example, consider the following case:
 * 
 * <pre>
 * public class Test {
 * 	public int f = 0;
 * 
 * 	public class Inner1 {
 * 		public int f = 1;
 * 	}
 * 
 * 	public class Inner2 extends Inner1 {
 * 		public void print() {
 * 			System.out.println(f);
 * 		}
 * 	}
 * 
 * 	public static void main(final String[] args) {
 * 		Test x = new Test();
 * 		Inner2 i = x.new Inner2();
 * 		i.print();
 * 	}
 * }
 * </pre>
 * 
 * Here, the question is: <i>what gets printed?</i> The answer is "1". The
 * reason is that scope resolution priorities superclasses over enclosing
 * classes.
 * 
 * The purpose of this class is to apply the rules from the Java Language Spec
 * to determine where a variable is defined, since this is not trivial. In
 * addition to determining the scope of variables, this class also must
 * determine the scope of method calls in a similar fashion. For example,
 * consider the following variant of the above:
 * 
 * <pre>
 * public class Test {
 * 	public int f() {
 * 		return 0;
 * 	}
 * 
 * 	public class Inner1 {
 * 		public int f() {
 * 			return 1;
 * 		}
 * 	}
 * 
 * 	public class Inner2 extends Inner1 {
 * 		public void print() {
 * 			System.out.println(f());
 * 		}
 * 	}
 * 
 * 	public static void main(final String[] args) {
 * 		Test x = new Test();
 * 		Inner2 i = x.new Inner2();
 * 		i.print();
 * 	}
 * }
 * </pre>
 * 
 * In order to resolve these situations, this class introduces "this" variables
 * appropriately. Thus, it modifies the source code slightly to do this. Let us
 * consider a final example to illustrate:
 * 
 * <pre>
 * public class Test {
 *   public int g() { return 0; }
 *
 *   public class Inner {
 *	   public int f;
 *
 *	   public void print() {
 *	     System.out.println(f);
 *	     System.out.println(g());
 * } } }
 * </pre>
 * 
 * This code would be transformed into the following, which remains valid Java:
 * 
 * <pre>
 * public class Test {
 *   public int g() { return 0; }
 *
 *   public class Inner {
 *	   public int f;
 *
 *	   public void print() {
 *	     System.out.println(this.f);
 *	     System.out.println(Test.this.g());
 * } } }
 * </pre>
 *
 * @author djp
 * 
 */
public class ScopeResolution {
	
	/*
	 * A Scope represents a declaration which defines some variables that may be
	 * accessed directly by code contained within this scope. 
	 */
	private static class Scope {
		public final Set<String> variables;
		public Scope() {
			this.variables = new HashSet<String>();
		}
		public Scope(Set<String> variables) {
			this.variables = variables;
		}
	}
	
	private static class ClassScope extends Scope {
		public Type.Clazz type; 	
		public boolean isStatic;
		public ClassScope(Type.Clazz type, boolean isStatic) {
			super(new HashSet<String>());
			this.type = type;
			this.isStatic = isStatic;
		}
	}
	
	private static class MethodScope extends Scope {
		public MethodScope(Set<String> variables) {
			super(variables);
		}
	}
	
	private ClassLoader loader;
	private TypeSystem types;
	private final Stack<Scope> scopes = new Stack<Scope>();
	
	public ScopeResolution(ClassLoader loader, TypeSystem types) {
		this.loader = loader; 
		this.types = types;
	}
	
	public void apply(JavaFile file) {		
		for(Decl d : file.declarations()) {
			doDeclaration(d,file);
		}
	}
	
	protected void doDeclaration(Decl d, JavaFile file) {
		if(d instanceof Interface) {
			doInterface((Interface)d, file);
		} else if(d instanceof Clazz) {
			doClass((Clazz)d, file);
		} else if(d instanceof Method) {
			doMethod((Method)d, file);
		} else if(d instanceof Field) {
			doField((Field)d, file);
		}
	}
	
	protected void doInterface(Interface d, JavaFile file) {
		
	}
	
	protected void doClass(Clazz c, JavaFile file) {
		// Create a Type.Class representing this class.
		ClassScope enclosingScope = (ClassScope) findEnclosingScope(ClassScope.class);
		List<Pair<String,List<Type.Reference>>> components = new ArrayList();
		if (enclosingScope != null) {
			for (Pair<String, List<Type.Reference>> i : enclosingScope.type
					.components()) {
				components.add(i);
			}
			components.add(new Pair(c.name(), new ArrayList()));
		} else {
			components.add(new Pair(c.name(), new ArrayList()));
		}
				
		// Ok, push on a scope representing this class definition.
		scopes.push(new ClassScope(new Type.Clazz(file.pkg(), components),c.isStatic()));
		
		for(Decl d : c.declarations()) {
			doDeclaration(d, file);
		}
		
		scopes.pop();
	}

	protected void doMethod(Method d, JavaFile file) {
		
		Set<String> params = new HashSet<String>();
		for(Triple<String,List<Modifier>,jkit.java.Type> t : d.parameters()) {
			params.add(t.first());
		}		
		scopes.push(new MethodScope(params));
		
		// Now, explore the method body for any other things to resolve.
		doStatement(d.body(), file);
		
		scopes.pop(); // leaving scope
	}

	protected void doField(Field d, JavaFile file) {
		doExpression(d.initialiser(), file);
	}
	
	protected void doStatement(Stmt e, JavaFile file) {
		if(e instanceof Stmt.SynchronisedBlock) {
			doSynchronisedBlock((Stmt.SynchronisedBlock)e, file);
		} else if(e instanceof Stmt.TryCatchBlock) {
			doTryCatchBlock((Stmt.TryCatchBlock)e, file);
		} else if(e instanceof Stmt.Block) {
			doBlock((Stmt.Block)e, file);
		} else if(e instanceof Stmt.VarDef) {
			doVarDef((Stmt.VarDef) e, file);
		} else if(e instanceof Stmt.Assignment) {
			doAssignment((Stmt.Assignment) e, file);
		} else if(e instanceof Stmt.Return) {
			doReturn((Stmt.Return) e, file);
		} else if(e instanceof Stmt.Throw) {
			doThrow((Stmt.Throw) e, file);
		} else if(e instanceof Stmt.Assert) {
			doAssert((Stmt.Assert) e, file);
		} else if(e instanceof Stmt.Break) {
			doBreak((Stmt.Break) e, file);
		} else if(e instanceof Stmt.Continue) {
			doContinue((Stmt.Continue) e, file);
		} else if(e instanceof Stmt.Label) {
			doLabel((Stmt.Label) e, file);
		} else if(e instanceof Stmt.If) {
			doIf((Stmt.If) e, file);
		} else if(e instanceof Stmt.For) {
			doFor((Stmt.For) e, file);
		} else if(e instanceof Stmt.ForEach) {
			doForEach((Stmt.ForEach) e, file);
		} else if(e instanceof Stmt.While) {
			doWhile((Stmt.While) e, file);
		} else if(e instanceof Stmt.DoWhile) {
			doDoWhile((Stmt.DoWhile) e, file);
		} else if(e instanceof Stmt.Switch) {
			doSwitch((Stmt.Switch) e, file);
		} else if(e instanceof Expr.Invoke) {
			doInvoke((Expr.Invoke) e, file);
		} else if(e instanceof Expr.New) {
			doNew((Expr.New) e, file);
		} else if(e instanceof Decl.Clazz) {
			doClass((Decl.Clazz)e, file);
		} else if(e != null) {
			throw new RuntimeException("Invalid statement encountered: "
					+ e.getClass());
		}		
	}
	
	protected void doBlock(Stmt.Block block, JavaFile file) {
		if(block != null) {
			scopes.push(new Scope());
			
			// now process every statement in this block.
			for(Stmt s : block.statements()) {
				doStatement(s, file);
			}
		
			scopes.pop();
		}
	}
	
	protected void doSynchronisedBlock(Stmt.SynchronisedBlock block, JavaFile file) {
		doBlock(block, file);
		doExpression(block.expr(), file);
	}
	
	protected void doTryCatchBlock(Stmt.TryCatchBlock block, JavaFile file) {
		doBlock(block, file);		
		doBlock(block.finaly(), file);		
		
		for(Stmt.CatchBlock cb : block.handlers()) {
			doBlock(cb, file);
		}
	}
	
	protected void doVarDef(Stmt.VarDef def, JavaFile file) {
		List<Triple<String, Integer, Expr>> defs = def.definitions();
		Scope enclosingScope = findEnclosingScope();
		
		for(int i=0;i!=defs.size();++i) {
			Triple<String, Integer, Expr> d = defs.get(i);			
			enclosingScope.variables.add(d.first());
			doExpression(d.third(), file);														
		}		
	}
	
	protected Expr doAssignment(Stmt.Assignment def, JavaFile file) {
		def.setLhs(doExpression(def.lhs(), file));	
		def.setRhs(doExpression(def.rhs(), file));
		return def;
	}
	
	protected void doReturn(Stmt.Return ret, JavaFile file) {
		ret.setExpr(doExpression(ret.expr(), file));
	}
	
	protected void doThrow(Stmt.Throw ret, JavaFile file) {
		ret.setExpr(doExpression(ret.expr(), file));
	}
	
	protected void doAssert(Stmt.Assert ret, JavaFile file) {
		ret.setExpr(doExpression(ret.expr(), file));
	}
	
	protected void doBreak(Stmt.Break brk, JavaFile file) {
		// nothing	
	}
	
	protected void doContinue(Stmt.Continue brk, JavaFile file) {
		// nothing
	}
	
	protected void doLabel(Stmt.Label lab, JavaFile file) {						
		doStatement(lab.statement(), file);
	}
	
	protected void doIf(Stmt.If stmt, JavaFile file) {
		stmt.setCondition(doExpression(stmt.condition(), file));
		doStatement(stmt.trueStatement(), file);
		doStatement(stmt.falseStatement(), file);
	}
	
	protected void doWhile(Stmt.While stmt, JavaFile file) {
		stmt.setCondition(doExpression(stmt.condition(), file));
		doStatement(stmt.body(), file);		
	}
	
	protected void doDoWhile(Stmt.DoWhile stmt, JavaFile file) {
		stmt.setCondition(doExpression(stmt.condition(), file));
		doStatement(stmt.body(), file);
	}
	
	protected void doFor(Stmt.For stmt, JavaFile file) {
		scopes.push(new Scope());
		
		doStatement(stmt.initialiser(), file);
		stmt.setCondition(doExpression(stmt.condition(), file));
		doStatement(stmt.increment(), file);
		doStatement(stmt.body(), file);
		
		scopes.pop();
	}
	
	protected void doForEach(Stmt.ForEach stmt, JavaFile file) {
		scopes.push(new Scope());
		
		stmt.setSource(doExpression(stmt.source(), file));
		doStatement(stmt.body(), file);
		
		scopes.pop();
	}
	
	protected void doSwitch(Stmt.Switch sw, JavaFile file) {
		sw.setCondition(doExpression(sw.condition(), file));
		for(Case c : sw.cases()) {
			doExpression(c.condition(), file);
			for(Stmt s : c.statements()) {
				doStatement(s, file);
			}
		}
		
		// should check that case conditions are final constants here.
	}
	
	protected Expr doExpression(Expr e, JavaFile file) {	
		if(e instanceof Value.Bool) {
			return doBoolVal((Value.Bool)e, file);
		} else if(e instanceof Value.Char) {
			return doCharVal((Value.Char)e, file);
		} else if(e instanceof Value.Int) {
			return doIntVal((Value.Int)e, file);
		} else if(e instanceof Value.Long) {
			return doLongVal((Value.Long)e, file);
		} else if(e instanceof Value.Float) {
			return doFloatVal((Value.Float)e, file);
		} else if(e instanceof Value.Double) {
			return doDoubleVal((Value.Double)e, file);
		} else if(e instanceof Value.String) {
			return doStringVal((Value.String)e, file);
		} else if(e instanceof Value.Null) {
			return doNullVal((Value.Null)e, file);
		} else if(e instanceof Value.TypedArray) {
			return doTypedArrayVal((Value.TypedArray)e, file);
		} else if(e instanceof Value.Array) {
			return doArrayVal((Value.Array)e, file);
		} else if(e instanceof Value.Class) {
			return doClassVal((Value.Class) e, file);
		} else if(e instanceof Expr.Variable) {
			return doVariable((Expr.Variable)e, file);
		} else if(e instanceof Expr.UnOp) {
			return doUnOp((Expr.UnOp)e, file);
		} else if(e instanceof Expr.BinOp) {
			return doBinOp((Expr.BinOp)e, file);
		} else if(e instanceof Expr.TernOp) {
			return doTernOp((Expr.TernOp)e, file);
		} else if(e instanceof Expr.Cast) {
			return doCast((Expr.Cast)e, file);
		} else if(e instanceof Expr.InstanceOf) {
			return doInstanceOf((Expr.InstanceOf)e, file);
		} else if(e instanceof Expr.Invoke) {
			return doInvoke((Expr.Invoke) e, file);
		} else if(e instanceof Expr.New) {
			return doNew((Expr.New) e, file);
		} else if(e instanceof Expr.ArrayIndex) {
			return doArrayIndex((Expr.ArrayIndex) e, file);
		} else if(e instanceof Expr.Deref) {
			return doDeref((Expr.Deref) e, file);
		} else if(e instanceof Stmt.Assignment) {
			// force brackets			
			return doAssignment((Stmt.Assignment) e, file);			
		} else if(e != null) {
			throw new RuntimeException("Invalid expression encountered: "
					+ e.getClass());
		}
		
		return null;
	}
	
	protected Expr doDeref(Expr.Deref e, JavaFile file) {
		e.setTarget(doExpression(e.target(), file));		
		return e;
	}
	
	protected Expr doArrayIndex(Expr.ArrayIndex e, JavaFile file) {
		e.setTarget(doExpression(e.target(), file));
		e.setIndex(doExpression(e.index(), file));
		
		return e;
	}
	
	protected Expr doNew(Expr.New e, JavaFile file) {
		// Second, recurse through any parameters supplied ...
		List<Expr> parameters = e.parameters();
		for(int i=0;i!=parameters.size();++i) {
			Expr p = parameters.get(i);
			parameters.set(i,doExpression(p, file));
		}
		
		// Third, check whether this is constructing an anonymous class ...
		for(Decl d : e.declarations()) {
			doDeclaration(d, file);
		}
		
		return e;
	}
	
	protected Expr doInvoke(Expr.Invoke e, JavaFile file) {		
		e.setTarget(doExpression(e.target(), file));
		
		List<Expr> parameters = e.parameters();
		for(int i=0;i!=parameters.size();++i) {
			Expr p = parameters.get(i);
			parameters.set(i, doExpression(p, file));
		}				
		
		return e;
	}
	
	protected Expr doInstanceOf(Expr.InstanceOf e, JavaFile file) {
		return e;
	}
	
	protected Expr doCast(Expr.Cast e, JavaFile file) {
		return e;
	}
	
	protected Expr doBoolVal(Value.Bool e, JavaFile file) {
		return e;
	}
	
	protected Expr doCharVal(Value.Char e, JavaFile file) {
		return e;
	}
	
	protected Expr doIntVal(Value.Int e, JavaFile file) {
		return e;
	}
	
	protected Expr doLongVal(Value.Long e, JavaFile file) {
		return e;
	}
	
	protected Expr doFloatVal(Value.Float e, JavaFile file) {
		return e;
	}
	
	protected Expr doDoubleVal(Value.Double e, JavaFile file) {
		return e;
	}
	
	protected Expr doStringVal(Value.String e, JavaFile file) {
		return e;
	}
	
	protected Expr doNullVal(Value.Null e, JavaFile file) {
		return e;
	}
	
	protected Expr doTypedArrayVal(Value.TypedArray e, JavaFile file) {
		return e;
	}
	
	protected Expr doArrayVal(Value.Array e, JavaFile file) {
		return e;
	}
	
	protected Expr doClassVal(Value.Class e, JavaFile file) {
		return e;
	}
	
	protected Expr doVariable(Expr.Variable e, JavaFile file) {
		// This method is really the heart of the whole operation defined in
		// this class. It is at this point that we have encountered a variable
		// and we now need to determine what it's scope is. To do this, we
		// traverse up the stack of scopes looking for an enclosing scope which
		// contains a variable with the same name.
		
		boolean isThis = true;		
		for(int i=scopes.size()-1;i>=0;--i) {
			Scope s = scopes.get(i);
			if(s instanceof ClassScope) {
				// resolve field from here
				ClassScope cs = (ClassScope) s;				
				try {
					Triple<jkit.jil.Clazz, jkit.jil.Field, Type> r = types
							.resolveField(cs.type, e.value(), loader);
					// Ok, this variable access corresponds to a field load.
					if(isThis) {
						return new Expr.Deref(new Expr.Variable("this",
							new ArrayList(e.attributes())), e.value(), e
							.attributes());
					} else {						
						// Create a class access variable.
						Expr.ClassVariable cv = new Expr.ClassVariable(cs.type.toString());
						cv.attributes().add(cs.type);
						return new Expr.Deref(new Expr.Deref(cv, "this",
								new ArrayList(e.attributes())), e.value(), e
								.attributes());
					}
				} catch(ClassNotFoundException cne) {					
				} catch(FieldNotFoundException fne) {					
				}
				isThis = false;
				if(cs.isStatic) { break; }
			} else if(s.variables.contains(e.value())) {
				// found scope
				System.out.println("FOUND IT");
			} 			
		}
		return e;
	}

	protected Expr doUnOp(Expr.UnOp e, JavaFile file) {		
		return e;
	}
		
	protected Expr doBinOp(Expr.BinOp e, JavaFile file) {				
		e.setLhs(doExpression(e.lhs(), file));
		e.setRhs(doExpression(e.rhs(), file));
		return e;
	}
	
	protected Expr doTernOp(Expr.TernOp e, JavaFile file) {		
		return e;
	}
	
	protected Scope findEnclosingScope() {
		return scopes.get(scopes.size()-1);
	}
	
	protected Scope findEnclosingScope(Class c) {
		for(int i=scopes.size()-1;i>=0;--i) {
			Scope s = scopes.get(i);
			if(s.getClass().equals(c)) {
				return s;
			}
		}
		return null;
	}
}
