package jkit.java.stages;

import java.util.ArrayList;
import java.util.List;

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
	private ClassLoader loader;
	private TypeSystem types;
	
	public ScopeResolution(ClassLoader loader, TypeSystem types) {
		this.loader = loader; 
		this.types = types;
	}
	
	public void apply(JavaFile file) {
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
		}
	}
	
	protected void doInterface(Interface d) {
		
	}
	
	protected void doClass(Clazz c) {
		for(Decl d : c.declarations()) {
			doDeclaration(d);
		}
	}

	protected void doMethod(Method d) {
		// Now, explore the method body for any other things to resolve.
		doStatement(d.body());
	}

	protected void doField(Field d) {
		doExpression(d.initialiser());
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
			throw new RuntimeException("Invalid statement encountered: "
					+ e.getClass());
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
		} else if(e instanceof Value.Char) {
			doCharVal((Value.Char)e);
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
		} else if(e instanceof Expr.Variable) {
			doVariable((Expr.Variable)e);
		} else if(e instanceof Expr.UnOp) {
			doUnOp((Expr.UnOp)e);
		} else if(e instanceof Expr.BinOp) {
			doBinOp((Expr.BinOp)e);
		} else if(e instanceof Expr.TernOp) {
			doTernOp((Expr.TernOp)e);
		} else if(e instanceof Expr.Cast) {
			doCast((Expr.Cast)e);
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
			throw new RuntimeException("Invalid expression encountered: "
					+ e.getClass());
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
		// Second, recurse through any parameters supplied ...
		for(Expr p : e.parameters()) {
			doExpression(p);
		}
		
		// Third, check whether this is constructing an anonymous class ...
		for(Decl d : e.declarations()) {
			doDeclaration(d);
		}
	}
	
	protected void doInvoke(Expr.Invoke e) {
		doExpression(e.target());
		
		for(Expr p : e.parameters()) {
			doExpression(p);
		}
	}
	
	protected void doInstanceOf(Expr.InstanceOf e) {}
	
	protected void doCast(Expr.Cast e) {}
	
	protected void doBoolVal(Value.Bool e) {}
	
	protected void doCharVal(Value.Char e) {}
	
	protected void doIntVal(Value.Int e) {}
	
	protected void doLongVal(Value.Long e) {}
	
	protected void doFloatVal(Value.Float e) {}
	
	protected void doDoubleVal(Value.Double e) {}
	
	protected void doStringVal(Value.String e) {}
	
	protected void doNullVal(Value.Null e) {}
	
	protected void doTypedArrayVal(Value.TypedArray e) {}
	
	protected void doArrayVal(Value.Array e) {}
	
	protected void doClassVal(Value.Class e) {}
	
	protected void doVariable(Expr.Variable e) {					
	}

	protected void doUnOp(Expr.UnOp e) {		
		
	}
		
	protected void doBinOp(Expr.BinOp e) {				
		doExpression(e.lhs());
		doExpression(e.rhs());		
	}
	
	protected void doTernOp(Expr.TernOp e) {		
		
	}
}
