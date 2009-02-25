package jkit.java.stages;

import java.util.*;

import jkit.compiler.ClassLoader;
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
 * This Class goes through all of the types that have been declared in the
 * source file, and resolves them to fully qualified types. For example,
 * consider this code:
 * 
 * <pre>
 * import java.util.*;
 * 
 * public class Test extends Vector {
 * 	public static void main(String[] args) {
 *       ... 
 *      }
 * }
 * </pre>
 * 
 * After type resolution, we will have resolved the following types:
 * 
 * <pre>
 *    Vector -&gt; java.util.Vector
 *    String -&gt; java.lang.String
 * </pre>
 * 
 * Thus, in principle, we could use this information to eliminate any import
 * statements (although type resolution by itself does not do this).
 * 
 * Other examples of declared types which are resolved include the declared
 * superclass of a class, the type of a field, the parameters of a method and
 * the type constructed by a new statement.
 * 
 * Note that this operation must be performed independently from type
 * propagation, since we must determine the skeleton of all classes being
 * compiled before we can do any type propagation. Note, a skeleton of a class
 * provides the fully qualified types of all interfaces, superclasses, fields
 * and methods (including return types, and parameters).
 */
public class TypeResolution {
	private ClassLoader loader;
	private TypeSystem types;
	// the classes stack is used to keep track of the full type for the inner
	// classes.
	private Stack<Type.Clazz> scopes = new Stack<Type.Clazz>();
	private LinkedList<String> imports = new LinkedList<String>();
	
	public TypeResolution(ClassLoader loader, TypeSystem types) {
		this.loader = loader; 
		this.types = types;
	}
	
	public void apply(JavaFile file) {
		// the following may cause problems with static imports.
		imports.add(file.pkg() + ".*");	
		for(Pair<Boolean,String> i : file.imports()) {
			imports.add(i.second());
		}											
		imports.add("java.lang.*");
		
		// The first entry on to the classes stack is a dummy to set the package
		// for remaining classes.		
		scopes.push(new Type.Clazz(file.pkg(),new ArrayList()));		
		
		// Now, examine all the declarations contain here-in
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
		} else if (d instanceof Decl.InitialiserBlock) {
			doInitialiserBlock((Decl.InitialiserBlock) d);
		} else if (d instanceof Decl.StaticInitialiserBlock) {
			doStaticInitialiserBlock((Decl.StaticInitialiserBlock) d);
		} else {
			syntax_error("internal failure (unknown declaration \"" + d
					+ "\" encountered)",d);
		}
	}
	
	protected void doInterface(Interface d) {
		doClass(d);
	}
	
	protected void doClass(Clazz c) {		
		// First, add myself to the import list, since that means we'll search
		// in my class for types before searching anywhere else i've declared
		// and/or on the CLASSPATH.
		Type.Clazz parentType = scopes.peek();		
		
		imports.addFirst(computeImportDecl(parentType,c.name()));
		
		// Second, build my fully qualified type!		
		List<Pair<String, List<Type.Reference>>> components = new ArrayList(parentType.components());
		ArrayList<Type.Reference> typevars = new ArrayList<Type.Reference>();
		for(jkit.java.tree.Type.Variable v : c.typeParameters()) {
			typevars.add((Type.Reference) resolve(v));
		}		
		components.add(new Pair(c.name(),typevars));
		Type.Clazz myType = new Type.Clazz(parentType.pkg(),components);
		c.attributes().add(myType); // record the type
		
		scopes.push(myType);
		
		// 1) resolve types in my declared super class.
		if(c.superclass() != null) {
			c.superclass().attributes().add(resolve(c.superclass()));
		}		

		// 2) resolve types in my declared interfaces
		for(jkit.java.tree.Type.Clazz i : c.interfaces()) {
			i.attributes().add(resolve(i));
		}			 						
		
		// 3) resolve types in my other declarations (e.g. fields, methods,inner
		// classes, etc)
		for(Decl d : c.declarations()) {
			doDeclaration(d);
		}
		
		imports.removeFirst();
		scopes.pop(); // undo my type
	}

	protected void doMethod(Method d) {
		// First, resolve return type and parameter types. 
		for(jkit.java.tree.Type.Clazz e : d.exceptions()) {
			e.attributes().add(resolve(e));
		}		
		
		Type returnType = new Type.Void();
		List<Type> parameterTypes = new ArrayList<Type>();
		
		if(d.returnType() != null) {
			// The return type may be null iff this is a constructor.
			returnType = resolve(d.returnType());
			d.returnType().attributes().add(returnType);			
		}
							
		for(Triple<String,List<Modifier>,jkit.java.tree.Type> p : d.parameters()) {
			Type pt = resolve(p.third());
			p.third().attributes().add(pt);				
			parameterTypes.add(pt);
		}
		
		d.attributes().add(
				new Type.Function(returnType, parameterTypes,
						new ArrayList<Type.Variable>()));		
		
		// Now, explore the method body for any other things to resolve.
		doStatement(d.body());
	}

	protected void doField(Field d) {
		doExpression(d.initialiser());		
		d.type().attributes().add(resolve(d.type()));
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
			cb.type().attributes().add(resolve(cb.type()));
			doBlock(cb);			
		}
	}
	
	protected void doVarDef(Stmt.VarDef def) {
		Type t = resolve(def.type());		
		def.type().attributes().add(t);
		
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
		Type t = resolve(stmt.type());
		stmt.type().attributes().add(t);
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
		} else if(e instanceof Expr.UnresolvedVariable) {
			doVariable((Expr.UnresolvedVariable)e);
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
		// First, figure out the type being created.		
		Type t = resolve(e.type());			
		e.type().attributes().add(t);
		
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
	
	protected void doInstanceOf(Expr.InstanceOf e) {		
		e.rhs().attributes().add(resolve(e.rhs()));
	}
	
	protected void doCast(Expr.Cast e) {
		e.type().attributes().add(resolve(e.type()));
		doExpression(e.expr());
	}
	
	protected void doBoolVal(Value.Bool e) {}
	
	protected void doCharVal(Value.Char e) {}
	
	protected void doIntVal(Value.Int e) {}
	
	protected void doLongVal(Value.Long e) {}
	
	protected void doFloatVal(Value.Float e) {}
	
	protected void doDoubleVal(Value.Double e) {}
	
	protected void doStringVal(Value.String e) {}
	
	protected void doNullVal(Value.Null e) {}
	
	protected void doTypedArrayVal(Value.TypedArray e) {
		e.type().attributes().add(resolve(e.type()));
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
		e.value().attributes().add(resolve(e.value()));
	}
	
	protected void doVariable(Expr.UnresolvedVariable e) {					
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
		
	/**
	 * The purpose of the resolve method is to examine the type in question, and
	 * determine the fully qualified type it represents, based on the current
	 * import list.
	 * 
	 * @param t
	 * @param file
	 * @return
	 */
	protected jkit.jil.Type resolve(jkit.java.tree.Type t) {
		if(t instanceof jkit.java.tree.Type.Primitive) {
			return resolve((jkit.java.tree.Type.Primitive)t);
		} else if(t instanceof jkit.java.tree.Type.Clazz) {
			return resolve((jkit.java.tree.Type.Clazz)t);			
		} else if(t instanceof jkit.java.tree.Type.Array) {
			return resolve((jkit.java.tree.Type.Array)t);
		} else if(t instanceof jkit.java.tree.Type.Wildcard) {
			return resolve((jkit.java.tree.Type.Wildcard)t);
		} else if(t instanceof jkit.java.tree.Type.Variable) {
			return resolve((jkit.java.tree.Type.Variable)t);
		}
		
		return null;
	}
	
	protected jkit.jil.Type.Primitive resolve(jkit.java.tree.Type.Primitive pt) {
		if(pt instanceof jkit.java.tree.Type.Void) {
			return new jkit.jil.Type.Void();
		} else if(pt instanceof jkit.java.tree.Type.Bool) {
			return new jkit.jil.Type.Bool();
		} else if(pt instanceof jkit.java.tree.Type.Byte) {
			return new jkit.jil.Type.Byte();
		} else if(pt instanceof jkit.java.tree.Type.Char) {
			return new jkit.jil.Type.Char();
		} else if(pt instanceof jkit.java.tree.Type.Short) {
			return new jkit.jil.Type.Short();
		} else if(pt instanceof jkit.java.tree.Type.Int) {
			return new jkit.jil.Type.Int();
		} else if(pt instanceof jkit.java.tree.Type.Long) {
			return new jkit.jil.Type.Long();
		} else if(pt instanceof jkit.java.tree.Type.Float) {
			return new jkit.jil.Type.Float();
		} else {
			return new jkit.jil.Type.Double();
		}
	}
	
	protected jkit.jil.Type.Array resolve(jkit.java.tree.Type.Array t) {
		return new jkit.jil.Type.Array(resolve(t.element()));
	}
	
	protected jkit.jil.Type.Wildcard resolve(jkit.java.tree.Type.Wildcard t) {				
		 jkit.jil.Type.Wildcard r = new jkit.jil.Type.Wildcard((Type.Reference) resolve(t
				.lowerBound()), (Type.Reference) resolve(t.upperBound()));		 
		 return r;
	}
	
	protected jkit.jil.Type.Variable resolve(jkit.java.tree.Type.Variable t) {		
		List<Type.Reference> args = null;
		if(t.lowerBounds() != null) {
			args = new ArrayList<Type.Reference>();
			for(jkit.java.tree.Type.Reference r : t.lowerBounds()) {
				args.add((Type.Reference) resolve(r));
			}
		}
		return new jkit.jil.Type.Variable(t.variable(),args);
	}
	
	/**
	 * The key challenge of this method, is that we have a Type.Clazz object
	 * which is incorrectly initialised and/or not fully qualified. An example
	 * of the former would arise from this code:
	 * 
	 * <pre>
	 * public void f(java.util.Vector v) { ... }
	 * </pre>
	 * 
	 * Here, the JavaFileReader will assume that "java" is the outerclass, and
	 * that "util" and "Vector" are inner classes. Thus, we must correct this.
	 * 
	 * An example of the second case, is the following:
	 * 
	 * <pre>
	 * public void f(Vector v) { ... }
	 * </pre>
	 * 
	 * Here, the JavaFileReader will not prepend the appropriate package
	 * information onto the type Vector. Thus, we must look this up here.
	 * 
	 * @param ct
	 *            --- the class type to resolve.
	 * @param file
	 *            --- the JavaFile containing this type; this is required to
	 *            determine the import list.
	 * @return
	 */
	protected jkit.jil.Type.Reference resolve(jkit.java.tree.Type.Clazz ct) {		
		ArrayList<Pair<String,List<jkit.jil.Type.Reference>>> ncomponents = new ArrayList();
		String className = "";
		String pkg = "";
				
		boolean firstTime = true;
		for (int i = 0; i != ct.components().size(); ++i) {
			String tmp = ct.components().get(i).first();
			String tmppkg = pkg.equals("") ? tmp : pkg + "." + tmp;
			if (firstTime && loader.isPackage(tmppkg)) {
				pkg = tmppkg;
			} else {
				if (!firstTime) {
					className += "$";
				}
				firstTime = false;
				className += ct.components().get(i).first();

				// now, rebuild the component list
				Pair<String, List<jkit.java.tree.Type.Reference>> component = ct
						.components().get(i);
				ArrayList<jkit.jil.Type.Reference> nvars = new ArrayList();

				for (jkit.java.tree.Type.Reference r : component.second()) {					
					nvars.add((jkit.jil.Type.Reference) resolve(r));
				}

				ncomponents
						.add(new Pair<String, List<jkit.jil.Type.Reference>>(
								component.first(), nvars));
			}
		}
		
		// now, some sanity checking.
		if(className.equals("")) {
			throw new SyntaxError("unable to find class " + pkg,0,0);
		} else if(pkg.length() > 0) {
			// could add "containsClass" check here. Need to modify
			// classLoader though.
			return new jkit.jil.Type.Clazz(pkg,ncomponents);			
		}
		
		// So, at this point, it seems there was no package information in the
		// source code and, hence, we need to determine this from the CLASSPATH
		// and the import list. There are two phases. 
		
		try {			
			Type.Clazz r = loader.resolve(className,imports);
			
			// The following loop is required for two reasons:
			//
			// 1) we may not have full type information in the source code.
			// 2) we may have generic type information in the source code which
			// need to keep.
			//
			// For example, imagine a class Test, with inner class Inner<G>. The
			// source code may include a reference "Inner<String>". Via resolve
			// above, we'll determine the type to be "Test.Inner<G>". But, the
			// actual type we want is "Test.Inner<String>". Therefore, the
			// following loop combines all the information we have together to
			// achieve this.
			
			List<Pair<String,List<jkit.jil.Type.Reference>>> rcomponents = r.components();
			for(int i=0;i!=r.components().size();++i) {
				Pair<String,List<jkit.jil.Type.Reference>> p = rcomponents.get(i); 
				if(p.first().equals(ncomponents.get(i).first())) {
					break;
				} else {
					ncomponents.add(i,p);
				}
			}
			
			return new jkit.jil.Type.Clazz(r.pkg(),ncomponents);					
		} catch(ClassNotFoundException e) {
			syntax_error("unable to find class " + className,ct,e);
			return null;
		}
	}
	
	/**
     * Check wither a given type is a reference to java.lang.String or not.
     * 
     * @param t
     * @return
     */
	protected static boolean isString(Type t) {
		if(t instanceof Type.Clazz) {
			Type.Clazz c = (Type.Clazz) t;
			 return c.pkg().equals("java.lang") && c.components().size() == 1
					&& c.components().get(0).first().equals("String");			
		}
		return false;
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
	protected String computeImportDecl(Type.Clazz parentType, String clazz) {
		String decl = parentType.pkg();
		if(!decl.equals("")) { decl = decl + "."; }
		for(Pair<String,List<Type.Reference>> p : parentType.components()) {
			decl = decl + p.first() + ".";
		}
		return decl + clazz + ".*";
	}
}
