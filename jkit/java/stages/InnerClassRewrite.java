// This file is part of the Java Compiler Kit (JKit)
//
// The Java Compiler Kit is free software; you can 
// redistribute it and/or modify it under the terms of the 
// GNU General Public License as published by the Free Software 
// Foundation; either version 2 of the License, or (at your 
// option) any later version.
//
// The Java Compiler Kit is distributed in the hope
// that it will be useful, but WITHOUT ANY WARRANTY; without 
// even the implied warranty of MERCHANTABILITY or FITNESS FOR 
// A PARTICULAR PURPOSE.  See the GNU General Public License 
// for more details.
//
// You should have received a copy of the GNU General Public 
// License along with the Java Compiler Kit; if not, 
// write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA
//
// (C) David James Pearce, 2009. 

package jkit.java.stages;

import static jkit.compiler.SyntaxError.internal_error;

import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.compiler.Clazz;
import jkit.compiler.FieldNotFoundException;
import jkit.java.io.JavaFile;
import jkit.java.tree.*;
import jkit.java.tree.Decl.*;
import jkit.java.tree.Stmt.Case;
import static jkit.java.tree.Type.fromJilType;
import static jkit.jil.util.Types.parentType;
import jkit.jil.tree.*;
import jkit.jil.tree.Type;
import jkit.jil.util.*;
import jkit.util.Pair;
import jkit.util.Triple;

/**
 * <p>
 * The purpose of this stage is to perform several rewrites related to inner
 * classes:
 * <ol>
 * <li>Non-static inner classes require parent pointer fields</li>
 * <li>Parent pointer fields must be initialised in all constructors</li>
 * <li>Access methods must be added when an inner class attempts to access a
 * private field of an enclosing class.</li>
 * </ol>
 * For example, consider the following:
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
public class InnerClassRewrite {
	private ClassLoader loader;	
	private TypeSystem types;
	private final Stack<Type.Clazz> enclosingClasses = new Stack<Type.Clazz>();
	private final HashMap<Type.Clazz, HashMap<String, JilMethod>> readAccessors = new HashMap();
	private final HashMap<Type.Clazz, HashMap<String, JilMethod>> writeAccessors = new HashMap();
	
	public InnerClassRewrite(ClassLoader loader, TypeSystem types) {
		this.loader = loader; 
		this.types = types;
	}
	
	public void apply(JavaFile file) {				
		readAccessors.clear();
		writeAccessors.clear();
		
		// Traverse the declarations
		for(Decl d : file.declarations()) {
			doDeclaration(d);
		}	
	}
	
	protected void doDeclaration(Decl d) {
		try {
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
				internal_error("unknown declaration \"" + d + "\" encountered", d);
			}
		} catch(Exception ex) {
			internal_error(d,ex);
		}								
	}
	
	protected void doInterface(JavaInterface d) throws ClassNotFoundException {
		doClass(d);
	}
	
	protected void doClass(JavaClass c) throws ClassNotFoundException {
		Type.Clazz type = c.attribute(Type.Clazz.class);
		enclosingClasses.add(type);		
		
		for(Decl d : c.declarations()) {
			doDeclaration(d);
		}
		
		JilClass oc = (JilClass) loader.loadClass(type);
		
		if (type.components().size() > 1 && !oc.isStatic()
				&& !(c instanceof JavaInterface)
				&& !(c instanceof JavaEnum)) {
			// Ok, we've found a non-static inner class here. Therefore, we
			// need
			// to rewrite all constructors to accept a parent pointer.

			addParentPtr(c, oc, type, Types.parentType(type));

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
		List<Stmt> statements = d.statements();
		for(int i=0;i!=statements.size();++i) {		
			statements.set(i, doStatement(statements.get(i)));
		}
	}
	
	protected void doStaticInitialiserBlock(Decl.StaticInitialiserBlock d) {		
		List<Stmt> statements = d.statements();
		for(int i=0;i!=statements.size();++i) {		
			statements.set(i, doStatement(statements.get(i)));
		}		
	}
	
	protected Stmt doStatement(Stmt e) {
		try {
			if(e instanceof Stmt.SynchronisedBlock) {
				doSynchronisedBlock((Stmt.SynchronisedBlock)e);
			} else if(e instanceof Stmt.TryCatchBlock) {
				doTryCatchBlock((Stmt.TryCatchBlock)e);
			} else if(e instanceof Stmt.Block) {
				doBlock((Stmt.Block)e);
			} else if(e instanceof Stmt.VarDef) {
				doVarDef((Stmt.VarDef) e);
			} else if(e instanceof Stmt.Assignment) {
				return (Stmt) doAssignment((Stmt.Assignment) e);
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
				internal_error("invalid statement encountered: "
						+ e.getClass(),e);
			}
		} catch(Exception ex) {
			internal_error(e,ex);
		}
		
		
		return e;
	}
	
	protected void doBlock(Stmt.Block block) {
		if(block != null) {
			List<Stmt> statements = block.statements();
			for(int i=0;i!=statements.size();++i) {		
				statements.set(i, doStatement(statements.get(i)));
			}
		}
	}
	
	protected void doCatchBlock(Stmt.CatchBlock block) {
		if(block != null) {			
			List<Stmt> statements = block.statements();
			for(int i=0;i!=statements.size();++i) {		
				statements.set(i, doStatement(statements.get(i)));
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
		Type t = def.type().attribute(Type.class);
		
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
	
	protected Expr doAssignment(Stmt.Assignment def) throws ClassNotFoundException,FieldNotFoundException {			
		// first, do the right-hand side
		def.setRhs(doExpression(def.rhs()));
		
		// second, so the left-hand side.		
		if(def.lhs() instanceof Expr.Deref) {
			Expr.Deref e = (Expr.Deref) def.lhs();			
			Type tmp = e.target().attribute(Type.class);
			
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
					Triple<Clazz, Clazz.Field, Type> r = types
					.resolveField(target, e.name(), loader);

					Clazz.Field f = r.second();															
					Clazz c = r.first();

					if (f.isPrivate()
							&& isStrictInnerClass(enclosingClasses.peek(), c.type())) {
						// Ok, we have found a dereference of a field. This
						// means we need to add an accessor method, unless there
						// already is one.

						if (!(c instanceof jkit.jil.tree.JilClass)) {
							// it should be impossible to get here.
							internal_error(
									"internal failure --- jil class required, found "
									+ c.getClass().getName(), e);
						}

						ArrayList<jkit.compiler.SyntacticAttribute> attributes = new ArrayList(e.attributes());
						Clazz.Method accessor = createWriteAccessor(f, (jkit.jil.tree.JilClass) c);
						attributes.add(new JilBuilder.MethodInfo(accessor.exceptions(),accessor.type()));						
						ArrayList<Expr> params = new ArrayList<Expr>();							
						params.add(e.target());
						params.add(def.rhs());

						return new Expr.Invoke(new Expr.ClassVariable(c
								.type().toString(), c.type()), accessor
								.name(), params, new ArrayList(),
								attributes);
					}
				}
			}
		} else {
			Expr lhs = doExpression(def.lhs());
			def.setLhs(lhs);
		}					
		
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
		lab.setStatement(doStatement(lab.statement()));
	}
	
	protected void doIf(Stmt.If stmt) {
		stmt.setCondition(doExpression(stmt.condition()));
		stmt.setTrueStatement(doStatement(stmt.trueStatement()));
		stmt.setFalseStatement(doStatement(stmt.falseStatement()));
	}
	
	protected void doWhile(Stmt.While stmt) {
		stmt.setCondition(doExpression(stmt.condition()));
		stmt.setBody(doStatement(stmt.body()));		
	}
	
	protected void doDoWhile(Stmt.DoWhile stmt) {
		stmt.setCondition(doExpression(stmt.condition()));
		stmt.setBody(doStatement(stmt.body()));
	}
	
	protected void doFor(Stmt.For stmt) {
		stmt.setInitialiser(doStatement(stmt.initialiser()));
		stmt.setCondition(doExpression(stmt.condition()));
		stmt.setIncrement(doStatement(stmt.increment()));
		stmt.setBody(doStatement(stmt.body()));		
	}
	
	protected void doForEach(Stmt.ForEach stmt) {
		stmt.setSource(doExpression(stmt.source()));
		stmt.setBody(doStatement(stmt.body()));		
	}
	
	protected void doSwitch(Stmt.Switch sw) {
		sw.setCondition(doExpression(sw.condition()));
		for(Case c : sw.cases()) {
			c.setCondition(doExpression(c.condition()));
			List<Stmt> statements = c.statements();
			for(int i=0;i!=statements.size();++i) {		
				statements.set(i, doStatement(statements.get(i)));
			}			
		}
		
		// should check that case conditions are final constants here.
	}
	
	protected Expr doExpression(Expr e) {	
		try {
			if(e instanceof Value.Bool) {
				return doBoolVal((Value.Bool)e);
			} if(e instanceof Value.Byte) {
				return doByteVal((Value.Byte)e);
			} else if(e instanceof Value.Char) {
				return doCharVal((Value.Char)e);
			} else if(e instanceof Value.Short) {
				return doShortVal((Value.Short)e);
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
			} else if(e instanceof Expr.Convert) {
				return doConvert((Expr.Convert)e);
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
			}
		} catch(Exception ex) {
			internal_error(e,ex);
		}
			
		if (e != null) {
			internal_error("Invalid expression encountered: " + e.getClass(), e);
		}
		
		return null;
	}
	
	protected Expr doDeref(Expr.Deref e) throws ClassNotFoundException,FieldNotFoundException {		
		e.setTarget(doExpression(e.target()));
		
		Type tmp = e.target().attribute(Type.class);
		
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
						internal_error(
								"internal failure --- jil class required, found "
								+ c.getClass().getName(), e);
					}

					ArrayList<jkit.compiler.SyntacticAttribute> attributes = new ArrayList(e.attributes());
					Clazz.Method accessor = createReadAccessor(f, (jkit.jil.tree.JilClass) c);
					attributes.add(new JilBuilder.MethodInfo(accessor.exceptions(),accessor.type()));						
					ArrayList<Expr> params = new ArrayList<Expr>();
					params.add(e.target());
					return new Expr.Invoke(new Expr.ClassVariable(c.type()
							.toString(), c.type()), accessor.name(),
							params, new ArrayList(), attributes);
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
	
	protected Expr doNew(Expr.New e) throws ClassNotFoundException {
		// Second, recurse through any parameters supplied ...
		SourceLocation loc = e.type().attribute(SourceLocation.class);
		Type type = e.type().attribute(Type.class);
		List<Expr> parameters = e.parameters();
		for(int i=0;i!=parameters.size();++i) {
			Expr p = parameters.get(i);
			parameters.set(i,doExpression(p));
		}
		
		doExpression(e.context());		
		
		if(e.declarations().size() > 0) {	
			Type.Clazz clazz = e.type().attribute(Type.Clazz.class);
			enclosingClasses.add(clazz);
			
			for(Decl d : e.declarations()) {
				doDeclaration(d);
			}
			
			enclosingClasses.pop();
		}
		
		// Now, check whether we're constructing a non-static inner class. If
		// so, then we need supply the parent pointer.
		if(type instanceof Type.Clazz) {
			Type.Clazz tc = (Type.Clazz) type;
			if(tc.components().size() > 1) {
				// Ok, this is an inner class construction. So, we need to check
				// whether it's static or not.				
				Clazz clazz = loader.loadClass(tc);										

				if(!clazz.isStatic()) {						
					// First, update the arguments to the new call
					Type.Clazz parentType = parentType(tc);

					if(e.context() == null) {							
						Expr.LocalVariable thiz = new Expr.LocalVariable(
								"this", parentType,loc);
						e.parameters().add(0,thiz);							
					} else {
						e.parameters().add(0,e.context());
						e.setContext(null); // bypassed now!
					}

					// Second, update the function type.
					JilBuilder.MethodInfo mi = e.attribute(JilBuilder.MethodInfo.class); 
					Type.Function mt = mi.type;
					ArrayList<Type> nparamtypes = new ArrayList<Type>(mt.parameterTypes());	
					nparamtypes.add(0,parentType);
					mi.type = new Type.Function(mt.returnType(), nparamtypes,
							mt.typeArguments());
				}
			}			
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
	
	protected Expr doConvert(Expr.Convert e) {
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
	
	protected Expr doByteVal(Value.Byte e) {
		return e;
	}
	
	protected Expr doCharVal(Value.Char e) {
		return e;
	}
	
	protected Expr doShortVal(Value.Short e) {
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
		
	/**
	 * The purpose of this method is to add a parent pointer to the class in
	 * question. This involves several things: firstly, we add the field with
	 * the special name "this$0"; second, we modify every constructor to accept
	 * it as the first parameter and then assign directly to the field; finally,
	 * we update all skeletons accordingly.
	 *
	 * @param type
	 * @param owner
	 */
	protected void addParentPtr(JavaClass owner, JilClass oc, Type.Clazz ownerType,
			Type.Clazz parentType)
			throws ClassNotFoundException {
		SourceLocation loc = owner.attribute(SourceLocation.class);				
		
		// First, update the source code for constructors			
		for(Decl o : owner.declarations()) {
			if(o instanceof JavaConstructor) {
				JavaMethod m = (JavaMethod) o;							
				rewriteConstructor(m, ownerType, parentType, loc);				
			}
		}
		
		// Second, update the skeleton types. I know it's a JilClass here, since
		// it must be the skeleton for the enclosing class which I'm compiling!
				
		for(JilMethod m : oc.methods(oc.name())) {					
			ArrayList<Type> nparams = new ArrayList<Type>(m.type().parameterTypes());
			nparams.add(0,parentType);
			Type.Function ntype = new Type.Function(m.type().returnType(),nparams);
			m.setType(ntype);
			ArrayList<Modifier> mods = new ArrayList<Modifier>();
			mods.add(Modifier.ACC_FINAL);
			mods.add(Modifier.ACC_SYNTHETIC);
			m.parameters().add(0, new JilMethod.Parameter("this$0",mods));
		}
		
		// Second, add a field with the appropriate name.
		ArrayList<Modifier> modifiers = new ArrayList<Modifier>();
		modifiers.add(Modifier.ACC_FINAL);
		modifiers.add(Modifier.ACC_SYNTHETIC);
		// note: parent pointers must have package access.
		
		JilField field = new JilField("this$0",
				parentType, modifiers, loc);
		
		oc.fields().add(field);		
	}
	
	protected void rewriteConstructor(JavaMethod constructor, Type.Clazz ownerType,
			Type.Clazz parentType, SourceLocation loc) {
				
		ArrayList<Modifier> mods = new ArrayList<Modifier>();
		mods.add(Modifier.ACC_FINAL);
		constructor.parameters().add(0, new Decl.JavaParameter("this$0", mods, fromJilType(parentType)));
		Expr.LocalVariable param = new Expr.LocalVariable("this$0", parentType, loc);
		Expr.LocalVariable thiz = new Expr.LocalVariable("this", ownerType, loc);
		Expr.Deref lhs = new Expr.Deref(thiz, "this$0", parentType, loc);
		Stmt.Assignment assign = new Stmt.Assignment(lhs, param);
		constructor.body().statements().add(0, assign);		
		
		// Now, update the inferred jil type for this method.
		Type.Function type = constructor.attribute(Type.Function.class);
		
		ArrayList<Type> nparams = new ArrayList<Type>(type.parameterTypes());
		nparams.add(0,parentType);
		
		constructor.attributes().remove(type);
		constructor.attributes().add(new Type.Function(type.returnType(),nparams));
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
			
			JilExpr thisVar = null;
			ArrayList<Modifier> mods = new ArrayList<Modifier>();
			mods.add(Modifier.ACC_FINAL);
			ArrayList<JilMethod.Parameter> params = new ArrayList(); 
			Type.Function ft;
			
			modifiers.add(Modifier.ACC_STATIC);
			modifiers.add(Modifier.ACC_SYNTHETIC);
			
			if(field.isStatic()) {
				thisVar = new JilExpr.ClassVariable(clazz.type());				
				ft = new Type.Function(field.type());
			} else {
				thisVar = new JilExpr.Variable("thisp",clazz.type());
				params.add(new JilMethod.Parameter("thisp",mods));
				ft = new Type.Function(field.type(),clazz.type());
			}						
			
			accessor = new JilMethod("access$" + accessors.size() + "00",
					ft, params, modifiers, new ArrayList<Type.Clazz>()); 
			
			JilExpr expr = new JilExpr.Deref(thisVar, field.name(), field
					.isStatic(), field.type());
			
			JilStmt stmt = new JilStmt.Return(expr,field.type());
			
			accessor.body().add(stmt);
			
			accessors.put(field.name(),accessor);
			clazz.methods().add(accessor);
		}				
		
		return accessor;
	}
	
	protected Clazz.Method createWriteAccessor(Clazz.Field field, jkit.jil.tree.JilClass clazz) {		
		// The first thing we need to do is check whether or not we've actually
		// created an accessor already.
		
		HashMap<String,JilMethod> accessors = writeAccessors.get(clazz.type());
		
		JilMethod accessor = null;
		
		if(accessors == null) {			
			accessors = new HashMap<String,JilMethod>();
			writeAccessors.put(clazz.type(),accessors);
		} else {
			accessor = accessors.get(field.name());
		}
		
		if(accessor == null) {			
			// no, we haven't so construct one.
			List<Modifier> modifiers = new ArrayList<Modifier>();
			
			JilExpr thisVar = null;
			
			ArrayList<Modifier> mods = new ArrayList<Modifier>();
			mods.add(Modifier.ACC_FINAL);
			ArrayList<JilMethod.Parameter> params = new ArrayList(); 
			
			Type.Function ft;
			
			modifiers.add(Modifier.ACC_STATIC);
			modifiers.add(Modifier.ACC_SYNTHETIC);
			
			if(field.isStatic()) {				
				thisVar = new JilExpr.ClassVariable(clazz.type());
				ft = new Type.Function(field.type(),field.type());
			} else {
				thisVar = new JilExpr.Variable("thisp",clazz.type());
				params.add(new JilMethod.Parameter("thisp",mods));
				ft = new Type.Function(field.type(),clazz.type(),field.type());
			}
			
			params.add(new JilMethod.Parameter("tmp",mods));									
			
			accessor = new JilMethod("access$" + accessors.size() + "02", ft,
					params, modifiers, new ArrayList<Type.Clazz>()); 						
						
			JilExpr tmpVar = new JilExpr.Variable("old", field.type());
			
			JilStmt copy = new JilStmt.Assign(tmpVar, new JilExpr.Deref(
					thisVar, field.name(), field.isStatic(), field.type()));

			JilExpr lhs = new JilExpr.Deref(thisVar, field.name(), field
					.isStatic(), field.type());						
			
			JilStmt assign = new JilStmt.Assign(lhs, new JilExpr.Variable(
					"tmp", field.type()));
			
			JilStmt ret = new JilStmt.Return(tmpVar,field.type());
			
			accessor.body().add(copy);
			accessor.body().add(assign);			
			accessor.body().add(ret);
			
			accessors.put(field.name(),accessor);
			clazz.methods().add(accessor);
		}			
		
		return accessor;
	}
}
