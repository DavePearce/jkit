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
import static jkit.java.tree.Type.fromJilType;

import java.util.*;

import jkit.compiler.*;
import jkit.compiler.ClassLoader;
import jkit.java.io.JavaFile;
import jkit.java.tree.Decl;
import jkit.java.tree.Expr;
import jkit.java.tree.Stmt;
import jkit.java.tree.Value;
import jkit.java.tree.Decl.JavaEnum;
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
	private final Stack<JavaClass> context = new Stack<JavaClass>(); 
	private final Stack<HashMap<String,Type>> nonLocals = new Stack();
	private final Stack<Type.Clazz> anonClasses = new Stack();
	
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
		try {
			if(d instanceof JavaInterface) {
				doInterface((JavaInterface)d);
			} else if(d instanceof JavaEnum) {
				doEnum((JavaEnum)d);
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
				internal_error("unknown declaration \"" + d
						+ "\" encountered",d);			
			}
		} catch(Exception ex) {
			internal_error(d,ex);
		}
	}
	
	protected void doInterface(JavaInterface d) {
		doClass(d);
	}
	
	protected void doEnum(JavaEnum en) throws ClassNotFoundException {
		doClass(en);
		
		Type.Clazz type = en.attribute(Type.Clazz.class);
		
		// Deal with any complex enumeration constants that require     
		// their own classes. Essentially, these are treated just like anonymous
        // inner classes.
		int extraClassCount = 0;
		for(Decl.EnumConstant enc : en.constants()) {			
			if(enc.declarations().size() > 0) {
				String name = Integer.toString(++extraClassCount);
				ArrayList<Pair<String, List<Type.Reference>>> ncomponents = new ArrayList(
						type.components());
				ncomponents.add(new Pair(name, new ArrayList()));
				Type.Clazz aType = new Type.Clazz(type.pkg(),
						ncomponents);
								
				Clazz parentClass = loader.loadClass(type);
				JilClass anonClass = (JilClass) loader.loadClass(aType);
				SourceLocation loc = enc.attribute(SourceLocation.class);
				Decl.JavaClass ac = buildAnonClass(anonClass, loc);
				
				anonClasses.push(aType);			
				context.push(ac);
				// break down any anonymous classes held internally to this
				// anonymous class.
				for(Decl d : enc.declarations()) {
					doDeclaration(d);
				}
				context.pop();
				anonClasses.pop();				

				// Third, create an appropriate constructor.
				Decl.JavaMethod constructor = buildEnumConstantConstructor(name,
						parentClass, anonClass, loc);								
				
				// Finally, create an appropriate java class
				ac.declarations().add(constructor);
				ac.declarations().addAll(enc.declarations());								

				context.peek().declarations().add(ac);						
			}
		}		
	}
	
	protected void doClass(JavaClass c) {					
		context.push(c);

		// NOTE: in the following loop we cannot use an iterator, otherwise a
		// concurrent modification exception can arise when we rewrite a
		// anonymous class.
		List<Decl> declarations = c.declarations();
		for(int i=0;i!=declarations.size();++i) {		
			doDeclaration(declarations.get(i));
		}
		context.pop();		
	}

	protected void doMethod(JavaMethod d) {	
		// Now, explore the method body for any other things to resolve.
		doStatement(d.body());		
	}
	
	protected void doField(JavaField d) {
		d.setInitialiser(doExpression(d.initialiser()));				
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
				doMethodLocalClass((Decl.JavaClass)e);
			} else if(e instanceof Stmt.PrePostIncDec) {
				doExpression((Stmt.PrePostIncDec)e);
			} else if (e != null) {
				internal_error("Invalid statement encountered: " + e.getClass(), e);
			}	
		} catch(Exception ex) {
			internal_error(e,ex);
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
	}
	
	protected void doContinue(Stmt.Continue brk) {		
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
	}
	
	protected void doMethodLocalClass(Decl.JavaClass mlclass) throws ClassNotFoundException {
		Type.Clazz type = mlclass.attribute(Type.Clazz.class);
		HashMap<String,Type> params = new HashMap();		
		nonLocals.push(params);
		anonClasses.push(type);
		doClass(mlclass);
		anonClasses.pop();
		nonLocals.pop();
		
		// Now, augment constructors if there are non-local variables.
		if (params.size() > 0) {
			internal_error(
					"Method-local classes which access non-local variables are not currently supported.  You'll need rewrite them to explicitly pass the non-local variables via constructors.",
					mlclass);
			
			// The following code actually works correctly. The remaining
			// problem is that we need to connect new calls to the method-local
			// class with those non-local fields/variables.
			/*			 
			SourceLocation loc = (SourceLocation) mlclass.attribute(SourceLocation.class);
			JilClass mlc = (JilClass) loader.loadClass(type);
			
			for(Decl d : mlclass.declarations()) {
				if(d instanceof Decl.JavaConstructor) {
					augmentConstructor((Decl.JavaConstructor) d,mlc,params);
				}
			}
						
			addNonLocalFields(mlc,params,loc);
			*/
		}	
	}
	
	protected Expr doExpression(Expr e) {
		try {
			if(e instanceof Value.Bool) {
				return doBoolVal((Value.Bool)e);
			} else if(e instanceof Value.Byte) {
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
		
		if(e != null) {
			internal_error("Invalid expression encountered: "
					+ e.getClass(),e);
		}
		return null;
	}
	
	protected Expr doDeref(Expr.Deref e) {
		e.setTarget(doExpression(e.target()));
		return e;
	}
	
	protected Expr doArrayIndex(Expr.ArrayIndex e) {
		e.setTarget(doExpression(e.target()));
		e.setIndex(doExpression(e.index()));
		return e;
	}
	
	protected Expr doNew(Expr.New e) throws ClassNotFoundException {				
		Type.Clazz parent = e.type().attribute(Type.Clazz.class);
		e.setContext(doExpression(e.context()));
			
		for(int i = 0;i!=e.parameters().size();++i) {
			e.parameters().set(i,doExpression(e.parameters().get(i)));			
		}
				
		// Second, if this is an anonymous class declaration, then break it down!
		if(e.declarations().size() > 0) {			
			// This is an anonymous class declaration.  We need to do several
			// things here. Firstly, update the type of the new expression to be
			// that of the anonymous inner class. Second, we need to add
			// appropriate constructors to the anonymous inner class.
			String name = Integer.toString(++anonymousClassCount);					
			Type.Clazz aType = anonClassType(name);											
						
			Clazz parentClass = (Clazz) loader.loadClass(parent);
			JilClass anonClass = (JilClass) loader.loadClass(aType);
			SourceLocation loc = e.attribute(SourceLocation.class);
			Decl.JavaClass ac = buildAnonClass(anonClass, loc);

			HashMap<String,Type> params = new HashMap();
			nonLocals.push(params);
			anonClasses.push(aType);			
			context.push(ac);
			// break down any anonymous classes held internally to this
			// anonymous class.
			for(Decl d : e.declarations()) {
				doDeclaration(d);
			}
			context.pop();
			anonClasses.pop();
			nonLocals.pop();

			// First, update the type of the new expression
			e.type().attributes().remove(parent);
			e.type().attributes().add(aType);

			JilBuilder.MethodInfo mi = e.attribute(JilBuilder.MethodInfo.class);

			// Second, determine non-local variables
			if (params.size() > 0) {
				ArrayList<Type> nparams = new ArrayList<Type>(mi.type
						.parameterTypes());
				for (Map.Entry<String, Type> en : params.entrySet()) {
					nparams.add(en.getValue());
					// FIXME: Actually need to determine whether or not this
					// is a local variable, or a non-local from a method
					// further up.
					Expr arg = new Expr.LocalVariable(en.getKey(), en
							.getValue(), loc);
					e.parameters().add(arg);
				}
				mi.type = new Type.Function(mi.type.returnType(), nparams);
			}

			// Third, create an appropriate constructor.
			Decl.JavaMethod constructor = buildAnonConstructor(name,
					mi.type, mi.exceptions, params, parentClass,
					anonClass, loc);

			// Finally, create an appropriate java class.

			addNonLocalFields(anonClass,params,loc);			

			ac.declarations().add(constructor);
			ac.declarations().addAll(e.declarations());								

			context.peek().declarations().add(ac);
			e.declarations().clear(); // need to do this.					
		} 
		
		return e;
	}	
	
	protected Expr doInvoke(Expr.Invoke e) {
		e.setTarget(doExpression(e.target()));
		
		for(int i=0;i!=e.parameters().size();++i) {
			Expr p = e.parameters().get(i);
			e.parameters().set(i,doExpression(p));
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
	
	protected Expr doBoolVal(Value.Bool e) { return e; }
	
	protected Expr doByteVal(Value.Byte e) { return e; }
	
	protected Expr doCharVal(Value.Char e) { return e; }
	
	protected Expr doShortVal(Value.Short e) { return e; }
	
	protected Expr doIntVal(Value.Int e) { return e; }
	
	protected Expr doLongVal(Value.Long e) { return e; }
	
	protected Expr doFloatVal(Value.Float e) { return e; }
	
	protected Expr doDoubleVal(Value.Double e) { return e; }
	
	protected Expr doStringVal(Value.String e) { return e; }
	
	protected Expr doNullVal(Value.Null e) { return e; }
	
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
	
	protected Expr doLocalVariable(Expr.LocalVariable e) {
		return e; 
	}

	protected Expr doNonLocalVariable(Expr.NonLocalVariable e) {
		Type t = e.attribute(Type.class);		
		nonLocals.peek().put(e.value(),t);
		
		SourceLocation loc = e.attribute(SourceLocation.class);
		
		Expr.LocalVariable thiz = new Expr.LocalVariable("this",anonClasses.peek(),loc);
		return new Expr.Deref(thiz,"val$" + e.value(),t,loc);
	}
	
	protected Expr doClassVariable(Expr.ClassVariable e) {
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
		e.setFalseBranch(doExpression(e.falseBranch()));
		e.setTrueBranch(doExpression(e.trueBranch()));
		return e;
	}
	
	protected Decl.JavaClass buildAnonClass(JilClass anonClass, SourceLocation loc) {
		
		jkit.java.tree.Type.Clazz superClass = fromJilType(anonClass.superClass());
		ArrayList<jkit.java.tree.Type.Clazz> interfaces = new ArrayList();
		for(Type.Clazz i : anonClass.interfaces()) {
			interfaces.add(fromJilType(i));
		}
		
		Decl.JavaClass jc = new Decl.JavaClass(new ArrayList(anonClass
				.modifiers()), anonClass.name(), new ArrayList(), superClass,
				interfaces, new ArrayList<Decl>(), loc, anonClass.type());
		
		return jc;
	}
	
	protected void addNonLocalFields(JilClass anonClass,
			HashMap<String, Type> nonlocalParams,
			SourceLocation loc) {
		
		// now add fields for non-local variables.
		for(Map.Entry<String,Type> en : nonlocalParams.entrySet()) {
			ArrayList<Modifier> mods = new ArrayList();
			mods.add(Modifier.ACC_FINAL);
			mods.add(Modifier.ACC_PRIVATE);			
			Decl.JavaField f = new Decl.JavaField(mods, "val$" + en.getKey(),
					fromJilType(en.getValue()), null, loc);		
			anonClass.fields().add(new JilField("val$" + en.getKey(),en.getValue(),mods));
		}		
	}
	
	protected void augmentConstructor(Decl.JavaConstructor m, JilClass owner,
			HashMap<String, Type> nonlocalParams) {

		Type.Function oftype = m.attribute(Type.Function.class);
		SourceLocation loc = m.attribute(SourceLocation.class);
		
		// First, find the skeleton constructor
		JilMethod skeletonMethod = null;
		for (JilMethod om : owner.methods()) {
			if (om.name().equals(owner.name()) && om.type().equals(oftype)) {
				skeletonMethod = om;
				break;
			}
		}
		
		List<Stmt> stmts = m.body().statements();		
		
		List<JilMethod.Parameter> params = skeletonMethod.parameters();
		ArrayList<Type> nparams = new ArrayList<Type>(oftype.parameterTypes());
		
		int pnum = 0;		
		for(Map.Entry<String,Type> en : nonlocalParams.entrySet()) {
			String vn = "x$" + pnum;
			ArrayList<Modifier> mods = new ArrayList<Modifier>();
			mods.add(Modifier.ACC_FINAL);
			Type t = en.getValue();
			params.add(new JilMethod.Parameter(vn,mods));
			nparams.add(t);
			Expr thiz = new Expr.LocalVariable("this",owner.type(),loc);
			Expr lhs = new Expr.Deref(thiz,"val$" + en.getKey(),t,loc);
			Expr rhs = new Expr.LocalVariable(vn,t);			
			stmts.add(new Stmt.Assignment(lhs,rhs,loc));
			pnum = pnum + 1;
		}
		
		Type.Function ftype = new Type.Function(Types.T_VOID,nparams);		
		m.attributes().remove(oftype);
		m.attributes().add(ftype);		
		skeletonMethod.setType(ftype);
	}
	
	protected Decl.JavaConstructor buildAnonConstructor(String name,
			Type.Function type, ArrayList<Type.Clazz> exceptions,
			HashMap<String, Type> nonlocalParams, Clazz parentClass,
			JilClass anonClass, SourceLocation loc) {
		
		// ... yes, this method is ugly.
		
		ArrayList<JilMethod.Parameter> jilparams = new ArrayList();
		ArrayList<Decl.JavaParameter> javaparams = new ArrayList();
		ArrayList<Expr> args = new ArrayList<Expr>();
		ArrayList<Type> superParams = new ArrayList<Type>();
		ArrayList<Modifier> mods = new ArrayList<Modifier>();
		mods.add(Modifier.ACC_FINAL);
		int p = 0;
		int trigger = (type.parameterTypes().size() - nonlocalParams.size());
		for (Type t : type.parameterTypes()) {
			// don't include the first parameter *if* it's the parent pointer,
			// and the super class is static.
			jilparams.add(new JilMethod.Parameter("x$" + p, mods));
			javaparams.add(new Decl.JavaParameter("x$" + p, mods, fromJilType(t)));
			if(p < trigger) {
				superParams.add(t);
				args.add(new Expr.LocalVariable("x$" + p, t));
			}
			p = p + 1;
		}			
		
		ArrayList<Stmt> stmts = new ArrayList<Stmt>();
		
		for(Map.Entry<String,Type> en : nonlocalParams.entrySet()) {
			Type t = en.getValue();
			Expr thiz = new Expr.LocalVariable("this",anonClass.type(),loc);
			Expr lhs = new Expr.Deref(thiz,"val$" + en.getKey(),t,loc);
			Expr rhs = new Expr.LocalVariable("x$" + trigger++,t);
			
			stmts.add(new Stmt.Assignment(lhs,rhs,loc));
		}
		
		Expr.LocalVariable target;
		
		if(parentClass.isInterface()) {
			target = new Expr.LocalVariable("super",Types.JAVA_LANG_OBJECT);
		} else {
			target = new Expr.LocalVariable("super",parentClass.type());
		}
		Expr.Invoke ivk = new Expr.Invoke(target, "super", args,
				new ArrayList(), loc, new JilBuilder.MethodInfo(exceptions,
						new Type.Function(type.returnType(),superParams)),type.returnType());
		stmts.add(ivk);
		
		Stmt.Block block = new Stmt.Block(stmts,loc);
		
		Decl.JavaConstructor mc = new Decl.JavaConstructor(mods, name,
				javaparams, false, new ArrayList(), new ArrayList(), block,
				loc, type);
		
		// finally, update skeleton accordingly.
		anonClass.methods().add(
				new JilMethod(name, type, jilparams, new ArrayList<Modifier>(),
						exceptions));
						
		return mc;
	}
	
	
	protected Decl.JavaConstructor buildEnumConstantConstructor(String name,			
			Clazz parentClass, JilClass anonClass, SourceLocation loc) {
		
		Type.Function type = new Type.Function(Types.T_VOID,Types.JAVA_LANG_STRING,Types.T_INT);
		
		// ... yes, this method is ugly.
		
		ArrayList<JilMethod.Parameter> jilparams = new ArrayList();
		ArrayList<Decl.JavaParameter> javaparams = new ArrayList();
		ArrayList<Expr> args = new ArrayList<Expr>();
		ArrayList<Type> superParams = new ArrayList<Type>();
		ArrayList<Modifier> mods = new ArrayList<Modifier>();
		mods.add(Modifier.ACC_FINAL);
		
		ArrayList<Stmt> stmts = new ArrayList<Stmt>();
		
		int p = 0;
		for (Type t : type.parameterTypes()) {
			jilparams.add(new JilMethod.Parameter("x$" + p, mods));
			javaparams.add(new Decl.JavaParameter("x$" + p, mods, fromJilType(t)));
			superParams.add(t);
			args.add(new Expr.LocalVariable("x$" + p, t));			
			p = p + 1;
		}	
		
		Expr.LocalVariable target = new Expr.LocalVariable("super",parentClass.type());		
		Expr.Invoke ivk = new Expr.Invoke(target, "super", args,
				new ArrayList(), loc, new JilBuilder.MethodInfo(new ArrayList(),
						new Type.Function(type.returnType(),superParams)),type.returnType());
		stmts.add(ivk);
		
		Stmt.Block block = new Stmt.Block(stmts,loc);
		
		Decl.JavaConstructor mc = new Decl.JavaConstructor(mods, name,
				javaparams, false, new ArrayList(), new ArrayList(), block,
				loc, type);
		
		// finally, update skeleton accordingly.
		anonClass.methods().add(
				new JilMethod(name, type, jilparams, new ArrayList<Modifier>(),
						new ArrayList()));
						
		return mc;
	}
	
	protected Type.Clazz anonClassType(String name) {
		Type.Clazz parent = context.peek().attribute(Type.Clazz.class);

		ArrayList<Pair<String, List<Type.Reference>>> ncomponents = new ArrayList(
				parent.components());
		ncomponents.add(new Pair(name, new ArrayList()));
		return new Type.Clazz(parent.pkg(), ncomponents);
	}	
}
