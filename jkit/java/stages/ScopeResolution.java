package jkit.java.stages;

import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.compiler.FieldNotFoundException;
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
import jkit.jil.Modifier;
import jkit.jil.SourceLocation;
import jkit.jil.SyntacticElement;
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
		public boolean isStatic;
		public MethodScope(Set<String> variables, boolean isStatic) {
			super(variables);
			this.isStatic = isStatic;
		}
	}
	
	private ClassLoader loader;
	private TypeSystem types;
	private final Stack<Scope> scopes = new Stack<Scope>();
	private final Stack<String> imports = new Stack<String>();
	
	public ScopeResolution(ClassLoader loader, TypeSystem types) {
		this.loader = loader; 
		this.types = types;
	}
	
	public void apply(JavaFile file) {
		// First, setup the imports list					
		imports.push("java.lang.*");		
		imports.push(file.pkg() + ".*");
		for(Pair<Boolean,String> i : file.imports()) {
			imports.push(i.second());
		}
		
		// Now, traverse the declarations
		for(Decl d : file.declarations()) {
			doDeclaration(d,file);
		}
		
		imports.clear();
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
		Type.Clazz myType = (Type.Clazz) c.attribute(Type.class);
		
		// Create an appropriate import declaration for this class.
		imports.push(computeImportDecl(myType));
				
		// And, push on a scope representing this class definition.
		scopes.push(new ClassScope(myType,c.isStatic()));
		
		for(Decl d : c.declarations()) {
			doDeclaration(d, file);
		}
		
		imports.pop();
		scopes.pop();
	}

	protected void doMethod(Method d, JavaFile file) {
		
		Set<String> params = new HashSet<String>();
		for(Triple<String,List<Modifier>,jkit.java.tree.Type> t : d.parameters()) {
			params.add(t.first());
		}		
		scopes.push(new MethodScope(params,d.isStatic()));
		
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
		Expr target = doExpression(e.target(), file);
		
		if(target instanceof Expr.ClassVariable) {
			// The question we need to consider here is. If we're dereference a
			// ClassVariable, then does it actually contain the field given, or is
			// it an inner class?						
			Expr.ClassVariable cv = (Expr.ClassVariable) target;
			Type.Clazz type = (Type.Clazz) target.attribute(Type.class);
			
			try {
				Triple<jkit.jil.Clazz, jkit.jil.Field, Type> r = types
						.resolveField(type, e.name(), loader);
				// if we get here, then there is such a field.
				//
				// so do nothing!
			} catch(ClassNotFoundException cne) {
				syntax_error(cne.getMessage(),e);				
			} catch(FieldNotFoundException fne) {	
				// Right, if we get here then there is no field ... so maybe
				// this is actually an inner class (or a syntax error :)
				try {								
					Type.Clazz c = loader.resolve(cv.type().replace('.','$') + "$" + e.name(),
							imports);
					Expr r = new Expr.ClassVariable(cv.type() + "." + e.name(),
							new ArrayList(e.attributes()));					
					r.attributes().add(c);
					return r;
				} catch(ClassNotFoundException cne) {
					// this must be an error...
					syntax_error("field does not exist: " + cv.type() + "." + e.name(),e);
				}
			}
		}
		
		e.setTarget(target);		
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
		Expr target = doExpression(e.target(), file);
		if(target == null) {
			boolean isThis = true;		
			MethodScope ms = (MethodScope) findEnclosingScope(MethodScope.class);
			for(int i=scopes.size()-1;i>=0;--i) {
				Scope s = scopes.get(i);
				if(s instanceof ClassScope) {
					// resolve field from here
					ClassScope cs = (ClassScope) s;		
					try {
						if(types.hasMethod(cs.type,e.name(),loader)) {
							// Ok, we have found the relevant method in question.
							if(isThis && !ms.isStatic) {
								target = new Expr.Variable("this",
										new ArrayList(e.attributes()));								
							} else if(!ms.isStatic) {
								Expr.ClassVariable cv = new Expr.ClassVariable(cs.type.toString());
								cv.attributes().add(cs.type);
								target = new Expr.Deref(cv, "this",
										new ArrayList(e.attributes()));								
							} else {
								target = new Expr.ClassVariable(cs.type.toString());
								target.attributes().add(cs.type);
							}
							break;
						}
					} catch(ClassNotFoundException cne) {
						syntax_error(cne.getMessage(), e);
					}
					
					isThis = false;
					if(cs.isStatic) { break; }
				} 		
			}	
		} 
		
		e.setTarget(target);		
		
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
		
		MethodScope ms = (MethodScope) findEnclosingScope(MethodScope.class);
		
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
					if(isThis && !ms.isStatic) {
						return new Expr.Deref(new Expr.Variable("this",
							new ArrayList(e.attributes())), e.value(), e
							.attributes());
					} else if(!!ms.isStatic){						
						// Create a class access variable.
						Expr.ClassVariable cv = new Expr.ClassVariable(cs.type.toString());
						cv.attributes().add(cs.type);
						return new Expr.Deref(new Expr.Deref(cv, "this",
								new ArrayList(e.attributes())), e.value(), e
								.attributes());
					} else {						
						// Create a class access variable.
						Expr.ClassVariable cv = new Expr.ClassVariable(cs.type.toString());
						cv.attributes().add(cs.type);
						return new Expr.Deref(cv, e.value(), e
								.attributes());
					} 
				} catch(ClassNotFoundException cne) {					
				} catch(FieldNotFoundException fne) {					
				}
				isThis = false;
				if(cs.isStatic) { break; }
			} else if(s.variables.contains(e.value())) {
				// found scope
				return e;
			} 			
		}
		
		// If we get here, then this variable access is either a syntax error,
		// or a static class access. For example, in "System.out" we initially
		// have "System" marked as a variable. In practice, we need to extend
		// this to be a ClassVariable. So, we check whether or not it actually
		// could represent a class.
					
		try {			
			Type.Clazz c = loader.resolve(e.value(), imports);
			Expr r = new Expr.ClassVariable(e.value(),new ArrayList(e.attributes()));
			r.attributes().add(c);
			return r;
		} catch(ClassNotFoundException ex) {			
			// no, can't find any class which could represent this variable.
			return e;
		}
	}

	protected Expr doUnOp(Expr.UnOp e, JavaFile file) {		
		e.setExpr(doExpression(e.expr(), file));
		return e;
	}
		
	protected Expr doBinOp(Expr.BinOp e, JavaFile file) {				
		e.setLhs(doExpression(e.lhs(), file));
		e.setRhs(doExpression(e.rhs(), file));
		return e;
	}
	
	protected Expr doTernOp(Expr.TernOp e, JavaFile file) {	
		e.setCondition(doExpression(e.condition(), file));
		e.setTrueBranch(doExpression(e.trueBranch(), file));
		e.setFalseBranch(doExpression(e.falseBranch(), file));
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
	
	/**
	 * Convert a class reference type into a proper name.
	 */
	protected String refName(Type.Clazz ref) {
		String descriptor = ref.pkg();
		if(!descriptor.equals("")) {
			descriptor += ".";
		}
		boolean firstTime=true;
		for(Pair<String,List<Type.Reference>> c : ref.components()) {
			if(!firstTime) {
				descriptor += "$";
			}	
			firstTime=false;
			descriptor += c.first();
		}
		return descriptor;
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
	 * The purpose of this method is to compute an import declaration from a
	 * given class Type. For example, consider the type "mypkg.MyClass". From
	 * this, we compute an import declaration "import mypkg.MyClass.*". This
	 * simplifies the problem of resolving an internal class, since we can use
	 * the existing method for looking up classes in the ClassLoader.
	 * 
	 * @param type
	 * @return
	 */
	protected String computeImportDecl(Type.Clazz type) {
		String decl = type.pkg();
		if(!decl.equals("")) { decl = decl + "."; }
		for(Pair<String,List<Type.Reference>> p : type.components()) {
			decl = decl + p.first() + ".";
		}
		return decl + "*";
	}
}
