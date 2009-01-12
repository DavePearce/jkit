package jkit.java.stages;

import java.util.*;

import jkit.*;
import jkit.compiler.ClassLoader;
import jkit.compiler.SyntaxError;
import jkit.java.*;
import jkit.java.Decl.Clazz;
import jkit.java.Decl.Field;
import jkit.java.Decl.Interface;
import jkit.java.Decl.Method;
import jkit.jil.Modifier;
import jkit.jil.SourceLocation;
import jkit.jil.SyntacticElement;
import jkit.util.*;

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
 *     ... 
 *    }
 * }
 * </pre>
 * 
 * After type resolution, we will have resolved the following types:
 * 
 * <pre>
 *  Vector -&gt; java.util.Vector
 *  String -&gt; java.lang.String
 * </pre>
 * 
 * Thus, in principle, we could use this information to eliminate any import
 * statements (although type resolution by itself does not do this).
 * 
 * @author djp
 * 
 */
public class TypeResolution {
	private ClassLoader loader;
	
	public TypeResolution(ClassLoader loader) {
		this.loader = loader; 
	}
	
	public void apply(JavaFile file) {		
		// the following may cause problems with static imports.
		ArrayList<String> imports = new ArrayList<String>();
		for(Pair<Boolean,String> i : file.imports()) {
			imports.add(i.second());
		}	
		
		imports.add(0,"java.lang.*");
		
		for(Decl d : file.declarations()) {
			doDeclaration(d, imports);
		}
	}
	
	protected void doDeclaration(Decl d, List<String> imports) {
		if(d instanceof Interface) {
			doClass((Interface)d, imports);
		} else if(d instanceof Clazz) {
			doClass((Clazz)d, imports);
		} else if(d instanceof Method) {
			doMethod((Method)d, imports);
		} else if(d instanceof Field) {
			doField((Field)d, imports);
		}
	}	
	
	protected void doClass(Clazz c, List<String> imports) {		
		if(c.superclass() != null) {
			c.superclass().attributes().add(resolve(c.superclass(), imports));
		}
		
		for(Type.Variable v : c.typeParameters()) {
			v.attributes().add(resolve(v, imports));
		}
		
		for(Type.Clazz i : c.interfaces()) {
			i.attributes().add(resolve(i, imports));
		}
		
		for(Decl d : c.declarations()) {
			doDeclaration(d, imports);
		}
	}

	protected void doMethod(Method d, List<String> imports) {
		// First, resolve parameter types.		
		for(Triple<String,List<Modifier>,Type> p : d.parameters()) {
			p.third().attributes().add(resolve(p.third(), imports));			
		}
		// Second, resolve return type
		d.returnType().attributes().add(resolve(d.returnType(), imports));
		
		// Third, resolve exceptions
		for(Type.Clazz e : d.exceptions()) {
			e.attributes().add(resolve(e,imports));
		}
		
		// Finally, resolve any types in the method body
		doStatement(d.body(),imports);
	}
			
	protected void doField(Field d, List<String> imports) {
		d.type().attributes().add(resolve(d.type(),imports));
						
		// doExpression(d.initialiser(), new HashMap<String,Type>());
	}
	
	protected void doStatement(Stmt e, List<String> imports) {
		if(e instanceof Stmt.SynchronisedBlock) {
			doSynchronisedBlock((Stmt.SynchronisedBlock)e, imports);
		} else if(e instanceof Stmt.TryCatchBlock) {
			doTryCatchBlock((Stmt.TryCatchBlock)e, imports);
		} else if(e instanceof Stmt.Block) {
			doBlock((Stmt.Block)e, imports);
		} else if(e instanceof Stmt.VarDef) {
			doVarDef((Stmt.VarDef) e, imports);
		} else if(e instanceof Stmt.Assignment) {
			doAssignment((Stmt.Assignment) e, imports);
		} else if(e instanceof Stmt.Return) {
			doReturn((Stmt.Return) e, imports);
		} else if(e instanceof Stmt.Throw) {
			doThrow((Stmt.Throw) e, imports);
		} else if(e instanceof Stmt.Assert) {
			doAssert((Stmt.Assert) e, imports);
		} else if(e instanceof Stmt.Break) {
			doBreak((Stmt.Break) e, imports);
		} else if(e instanceof Stmt.Continue) {
			doContinue((Stmt.Continue) e, imports);
		} else if(e instanceof Stmt.Label) {
			doLabel((Stmt.Label) e, imports);
		} else if(e instanceof Stmt.If) {
			doIf((Stmt.If) e, imports);
		} else if(e instanceof Stmt.For) {
			doFor((Stmt.For) e, imports);
		} else if(e instanceof Stmt.ForEach) {
			doForEach((Stmt.ForEach) e, imports);
		} else if(e instanceof Stmt.While) {
			doWhile((Stmt.While) e, imports);
		} else if(e instanceof Stmt.DoWhile) {
			doDoWhile((Stmt.DoWhile) e, imports);
		} else if(e instanceof Stmt.Switch) {
			doSwitch((Stmt.Switch) e, imports);
		} else if(e instanceof Expr.Invoke) {
			doInvoke((Expr.Invoke) e, imports);
		} else if(e instanceof Expr.New) {
			doNew((Expr.New) e, imports);
		} else if(e instanceof Decl.Clazz) {
			doClass((Decl.Clazz)e);
		} else if(e != null) {
			throw new RuntimeException("Invalid statement encountered: "
					+ e.getClass());
		}		
	}
	
	protected void doBlock(Stmt.Block block, List<String> imports) {
		if(block != null) {			
			for(Stmt s : block.statements()) {
				doStatement(s,imports);
			}
		}
	}
	
	protected void doSynchronisedBlock(Stmt.SynchronisedBlock block, List<String> imports) {
		doBlock(block,imports);
		doExpression(block.expr(),imports);
	}
	
	protected void doTryCatchBlock(Stmt.TryCatchBlock block, List<String> imports) {
		doBlock(block,imports);		
		doBlock(block.finaly(),imports);		
		
		for(Stmt.CatchBlock cb : block.handlers()) {
			doBlock(cb, imports);
		}
	}
	
	protected void doVarDef(Stmt.VarDef def, List<String> imports) {
		Type t = (Type) def.type().attribute(Type.class);
		
		List<Triple<String, Integer, Expr>> defs = def.definitions();
		for(int i=0;i!=defs.size();++i) {
			Triple<String, Integer, Expr> d = defs.get(i);
			
			doExpression(d.third(),imports);									
		}
	}
	
	protected void doAssignment(Stmt.Assignment def, List<String> imports) {
		doExpression(def.lhs(),imports);	
		doExpression(def.rhs(),imports);			
	}
	
	protected void doReturn(Stmt.Return ret, List<String> imports) {
		doExpression(ret.expr(), imports);
	}
	
	protected void doThrow(Stmt.Throw ret, List<String> imports) {
		doExpression(ret.expr(), imports);
	}
	
	protected void doAssert(Stmt.Assert ret, List<String> imports) {
		doExpression(ret.expr(), imports);
	}
	
	protected void doBreak(Stmt.Break brk, List<String> imports) {
		// nothing	
	}
	
	protected void doContinue(Stmt.Continue brk, List<String> imports) {
		// nothing
	}
	
	protected void doLabel(Stmt.Label lab, List<String> imports) {						
		doStatement(lab.statement(), imports);
	}
	
	protected void doIf(Stmt.If stmt, List<String> imports) {
		doExpression(stmt.condition(),imports);
		doStatement(stmt.trueStatement(),imports);
		doStatement(stmt.falseStatement(),imports);
	}
	
	protected void doWhile(Stmt.While stmt, List<String> imports) {
		doExpression(stmt.condition(),imports);
		doStatement(stmt.body(),imports);		
	}
	
	protected void doDoWhile(Stmt.DoWhile stmt, List<String> imports) {
		doExpression(stmt.condition(),imports);
		doStatement(stmt.body(),imports);
	}
	
	protected void doFor(Stmt.For stmt, List<String> imports) {
		doStatement(stmt.initialiser(),imports);
		doExpression(stmt.condition(),imports);
		doStatement(stmt.increment(),imports);
		doStatement(stmt.body(),imports);	
	}
	
	protected void doForEach(Stmt.ForEach stmt, List<String> imports) {
		doExpression(stmt.source(),imports);
		doStatement(stmt.body(),imports);
	}
	
	protected void doSwitch(Stmt.Switch sw, List<String> imports) {
		doExpression(sw.condition(), imports);
		for(Stmt.Case c : sw.cases()) {
			doExpression(c.condition(), imports);
			for(Stmt s : c.statements()) {
				doStatement(s, imports);
			}
		}
		
		// should check that case conditions are final constants here.
	}
	
	protected void doExpression(Expr e, List<String> imports) {
		if(e instanceof Value.Bool) {
			doBoolVal((Value.Bool)e, imports);
		} else if(e instanceof Value.Char) {
			doCharVal((Value.Char)e, imports);
		} else if(e instanceof Value.Int) {
			doIntVal((Value.Int)e, imports);
		} else if(e instanceof Value.Long) {
			doLongVal((Value.Long)e, imports);
		} else if(e instanceof Value.Float) {
			doFloatVal((Value.Float)e, imports);
		} else if(e instanceof Value.Double) {
			doDoubleVal((Value.Double)e, imports);
		} else if(e instanceof Value.String) {
			doStringVal((Value.String)e, imports);
		} else if(e instanceof Value.Null) {
			doNullVal((Value.Null)e, imports);
		} else if(e instanceof Value.TypedArray) {
			doTypedArrayVal((Value.TypedArray)e, imports);
		} else if(e instanceof Value.Array) {
			doArrayVal((Value.Array)e, imports);
		} else if(e instanceof Value.Class) {
			doClassVal((Value.Class) e, imports);
		} else if(e instanceof Expr.Variable) {
			doVariable((Expr.Variable)e, imports);
		} else if(e instanceof Expr.UnOp) {
			doUnOp((Expr.UnOp)e, imports);
		} else if(e instanceof Expr.BinOp) {
			doBinOp((Expr.BinOp)e, imports);
		} else if(e instanceof Expr.TernOp) {
			doTernOp((Expr.TernOp)e, imports);
		} else if(e instanceof Expr.Cast) {
			doCast((Expr.Cast)e, imports);
		} else if(e instanceof Expr.InstanceOf) {
			doInstanceOf((Expr.InstanceOf)e, imports);
		} else if(e instanceof Expr.Invoke) {
			doInvoke((Expr.Invoke) e, imports);
		} else if(e instanceof Expr.New) {
			doNew((Expr.New) e, imports);
		} else if(e instanceof Expr.ArrayIndex) {
			doArrayIndex((Expr.ArrayIndex) e, imports);
		} else if(e instanceof Expr.Deref) {
			doDeref((Expr.Deref) e, imports);
		} else if(e instanceof Stmt.Assignment) {
			// force brackets			
			doAssignment((Stmt.Assignment) e, imports);			
		} else if(e != null) {
			throw new RuntimeException("Invalid expression encountered: "
					+ e.getClass());
		}
	}
	
	protected void doDeref(Expr.Deref e, List<String> imports) {
		doExpression(e.target(), imports);		
		// need to perform field lookup here!
	}
	
	protected void doArrayIndex(Expr.ArrayIndex e, List<String> imports) {
		doExpression(e.target(), imports);
		doExpression(e.index(), imports);
		
		e.setIndex(implicitCast(e.index(),new Type.Int()));
				
		Type target_t = (Type) e.target().attribute(Type.class);
		
		if(target_t instanceof Type.Array) {
			Type.Array at = (Type.Array) target_t;
			e.attributes().add(at.element());
		} else {
			// this is really a syntax error
			syntax_error("array required, but " + target_t + " found", e);
		}
	}
	
	protected void doNew(Expr.New e, List<String> imports) {
		
	}
	
	protected void doInvoke(Expr.Invoke e, List<String> imports) {
		for(Expr p : e.parameters()) {
			doExpression(p, imports);
		}
	}
	
	protected void doInstanceOf(Expr.InstanceOf e, List<String> imports) {		
			
	}
	
	protected void doCast(Expr.Cast e, List<String> imports) {
	
	}
	
	protected void doBoolVal(Value.Bool e, List<String> imports) {
		e.attributes().add(new Type.Bool());
	}
	
	protected void doCharVal(Value.Char e, List<String> imports) {
		e.attributes().add(new Type.Char());
	}
	
	protected void doIntVal(Value.Int e, List<String> imports) {
		e.attributes().add(new Type.Int());
	}
	
	protected void doLongVal(Value.Long e, List<String> imports) {		
		e.attributes().add(new Type.Long());
	}
	
	protected void doFloatVal(Value.Float e, List<String> imports) {		
		e.attributes().add(new Type.Float());
	}
	
	protected void doDoubleVal(Value.Double e, List<String> imports) {		
		e.attributes().add(new Type.Double());
	}
	
	protected void doStringVal(Value.String e, List<String> imports) {		
		e.attributes().add(new Type.Clazz("java.lang","String"));
	}
	
	protected void doNullVal(Value.Null e, List<String> imports) {		
		e.attributes().add(new Type.Null());
	}
	
	protected void doTypedArrayVal(Value.TypedArray e, List<String> imports) {		
		e.attributes().add((Type) e.type().attribute(Type.class));
	}
	
	protected void doArrayVal(Value.Array e, List<String> imports) {		
		// not sure what to do here.
	}
	
	protected void doClassVal(Value.Class e, List<String> imports) {
		
	}
	
	protected void doVariable(Expr.Variable e, List<String> imports) {			
		Type t = environment.get(e.value());
		if(t == null) {			
			syntax_error("Cannot find symbol - variable \"" + e.value() + "\"",
					e);
		} else {
			e.attributes().add(t);
		}
	}

	protected void doUnOp(Expr.UnOp e, List<String> imports) {		
		
	}
		
	protected void doBinOp(Expr.BinOp e, List<String> imports) {				
		doExpression(e.lhs(),environment);
		doExpression(e.rhs(),environment);		
	}
	
	protected void doTernOp(Expr.TernOp e, List<String> imports) {		
		
	}
	
	/**
     * The purpose of the resovle method is to examine the type in question, and
     * determine the fully qualified it represents, based on the current import
     * list. 
     * 
     * @param t
     * @param file
     * @return
     */
	protected jkit.jil.Type resolve(Type t, List<String> imports) {
		if(t instanceof Type.Primitive) {
			return resolve((Type.Primitive)t, imports);
		} else if(t instanceof jkit.java.Type.Clazz) {
			return resolve((Type.Clazz)t, imports);			
		} else if(t instanceof Type.Array) {
			return resolve((Type.Array)t, imports);
		} 
		
		return null;
	}
	
	protected jkit.jil.Type.Primitive resolve(Type.Primitive pt, List<String> imports) {
		if(pt instanceof Type.Void) {
			return new jkit.jil.Type.Void();
		} else if(pt instanceof Type.Bool) {
			return new jkit.jil.Type.Bool();
		} else if(pt instanceof Type.Byte) {
			return new jkit.jil.Type.Byte();
		} else if(pt instanceof Type.Char) {
			return new jkit.jil.Type.Char();
		} else if(pt instanceof Type.Short) {
			return new jkit.jil.Type.Short();
		} else if(pt instanceof Type.Int) {
			return new jkit.jil.Type.Int();
		} else if(pt instanceof Type.Long) {
			return new jkit.jil.Type.Long();
		} else if(pt instanceof Type.Float) {
			return new jkit.jil.Type.Float();
		} else {
			return new jkit.jil.Type.Double();
		}
	}
	
	protected jkit.jil.Type.Array resolve(Type.Array t, List<String> imports) {
		return new jkit.jil.Type.Array(resolve(t.element(), imports));
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
	protected jkit.jil.Type.Reference resolve(Type.Clazz ct, List<String> imports) {
		ArrayList<Pair<String,List<jkit.jil.Type.Reference>>> ncomponents = new ArrayList();
		String className = "";
		String pkg = "";
				
		boolean firstTime = true;
		for(int i=0;i!=ct.components().size();++i) {
			String tmp = ct.components().get(i).first();
			String tmppkg = pkg.equals("") ? tmp : pkg + "." + tmp;
			if(firstTime && loader.isPackage(tmppkg))  {
				pkg = tmppkg;
			} else {
				if(!firstTime) {
					className += "$";
				}
				firstTime = false;
				className += ct.components().get(i).first();
				
				// now, rebuild the component list
				Pair<String,List<Type.Reference>> component = ct.components().get(i);
				ArrayList<jkit.jil.Type.Reference> nvars = new ArrayList();
				
				for(Type.Reference r : component.second()) {
					nvars.add((jkit.jil.Type.Reference) resolve(r, imports));
				}
				
				ncomponents.add(new Pair<String,List<jkit.jil.Type.Reference>>(component.first(),nvars));
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
		// source code and, hence, we need to determine this fromt he CLASSPATH
		// and the import list.
									
		try {			
			System.out.println("LOADING: " + className);
			return loader.resolve(className, imports);			
		} catch(ClassNotFoundException e) {}

		throw new SyntaxError("unable to find class " + className,0,0);
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
}
