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
import jkit.compiler.MethodNotFoundException;
import jkit.java.io.JavaFile;
import jkit.java.tree.*;
import jkit.java.tree.Decl.*;
import jkit.java.tree.Stmt.Case;
import jkit.jil.tree.JilClass;
import jkit.jil.tree.JilField;
import jkit.jil.tree.JilMethod;
import jkit.jil.tree.Modifier;
import jkit.jil.tree.SourceLocation;
import jkit.jil.tree.Type;
import jkit.jil.util.Types;
import jkit.util.Pair;
import jkit.util.Triple;

public class EnumRewrite {
	private ClassLoader loader;
	private TypeSystem types;	
	
	public EnumRewrite(ClassLoader loader, TypeSystem types) {
		this.loader = loader; 
		this.types = types;
	}
	
	public void apply(JavaFile file) {			
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
			internal_error(d, ex);
		}		
						
	}
	
	protected void doEnum(Decl.JavaEnum ec) throws ClassNotFoundException, MethodNotFoundException {	
		Type.Clazz type = ec.attribute(Type.Clazz.class);		
		JilClass skeleton = (JilClass) loader.loadClass(type);

		// First, add the $VALUES field
		List<Modifier> modifiers = new ArrayList<Modifier>();
		modifiers.add(Modifier.ACC_PRIVATE);
		modifiers.add(Modifier.ACC_STATIC);
		modifiers.add(Modifier.ACC_FINAL);

		skeleton.fields()
		.add(
				new JilField("$VALUES", new Type.Array(type),
						modifiers));

		// Second, create necessary public methods.
		Decl.JavaMethod values = createValuesMethod(ec,type);
		Decl.JavaMethod valueOf = createValueOfMethod(ec,type);			
		ec.declarations().add(values);
		ec.declarations().add(valueOf);

		// Third, augment the constructor(s) appropriately.
		if(skeleton.methods(ec.name()).isEmpty()) {
			createDefaultConstructor(ec,skeleton);
		} else {
			augmentConstructors(ec,skeleton);
		}
		
		// Now, create the static initialiser
		Decl.StaticInitialiserBlock init = createStaticInitialiser(ec,type,skeleton);
		ec.declarations().add(init);		
	}
	
	protected void doInterface(JavaInterface d) {
		doClass(d);
	}
	
	protected void doClass(JavaClass c) {							
		List<Decl> declarations = c.declarations();
		for(int i=0;i!=declarations.size();++i) {		
			doDeclaration(declarations.get(i));
		}	
	}

	protected void doMethod(JavaMethod d) {	
		// Now, explore the method body for any other things to resolve.
		doStatement(d.body());		
	}
	
	protected void doField(JavaField d) {
		d.setInitialiser(doExpression(d.initialiser()));				
	}
	
	protected void doInitialiserBlock(Decl.InitialiserBlock d) {
		for(Stmt s : d.statements()) {
			doStatement(s);
		}		
	}
	
	protected void doStaticInitialiserBlock(Decl.StaticInitialiserBlock d) {	
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
		} else if(e instanceof Decl.JavaClass) {
			doClass((Decl.JavaClass)e);
		} else if(e instanceof Stmt.PrePostIncDec) {
			doExpression((Stmt.PrePostIncDec)e);
		} else if(e != null) {
			internal_error("invalid statement encountered: "
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
			} else if(e != null) {
				internal_error("invalid expression encountered: "
						+ e.getClass(),e);
			}
		} catch(Exception ex) {
			internal_error(e,ex);
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
	
	protected Expr doNew(Expr.New e) {				
		e.setContext(doExpression(e.context()));
			
		for(int i = 0;i!=e.parameters().size();++i) {
			e.parameters().set(i,doExpression(e.parameters().get(i)));			
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
		return e;
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
	
	protected Decl.JavaMethod createValuesMethod(Decl.JavaEnum ec, Type.Clazz type) {
		SourceLocation loc = ec.attribute(SourceLocation.class);
		
		Type.Function ftype = new Type.Function(new Type.Array(type));
		ArrayList<Modifier> mods = new ArrayList<Modifier>();
		mods.add(Modifier.ACC_PUBLIC);		
		ArrayList<Stmt> stmts = new ArrayList();
		
		// load, clone, cast and return array
		Expr.ClassVariable thisClass = new Expr.ClassVariable(ec.name(),loc);
		thisClass.attributes().add(type);
		Expr.Deref load = new Expr.Deref(thisClass, "$VALUES",loc);
		load.attributes().add(new Type.Array(type));
		
		Expr.Invoke clone = new Expr.Invoke(load,"clone",new ArrayList(),
				new ArrayList(), loc);
		clone.attributes().add(Types.JAVA_LANG_OBJECT);
		Type.Function cloneFtype = new Type.Function(Types.JAVA_LANG_OBJECT);
		clone.attributes().add(new JilBuilder.MethodInfo(new ArrayList(),cloneFtype));
		
		Type.Array rtype = new Type.Array(type);
		jkit.java.tree.Type jrtype = new jkit.java.tree.Type.Array(
				new jkit.java.tree.Type.Clazz(ec.name(), loc));  
		Expr.Cast cast = new Expr.Cast(jrtype,clone,loc);
		cast.type().attributes().add(rtype);
		cast.attributes().add(rtype);
		
		Stmt.Return ret = new Stmt.Return(cast,loc); 
		stmts.add(ret);
		
		Stmt.Block block = new Stmt.Block(stmts, loc);

		Decl.JavaMethod m = new Decl.JavaMethod(mods, "values",
				jrtype, new ArrayList(), false,
				new ArrayList(), new ArrayList(), block, loc);

		m.attributes().add(ftype);
		
		return m;		
	}
	
	protected Decl.JavaMethod createValueOfMethod(Decl.JavaEnum ec, Type.Clazz type) {
		SourceLocation loc = ec.attribute(SourceLocation.class);
		
		jkit.java.tree.Type jtype = new jkit.java.tree.Type.Clazz(ec.name(), loc);  
		Type.Function ftype = new Type.Function(type,Types.JAVA_LANG_STRING);
		ArrayList<Modifier> mods = new ArrayList<Modifier>();
		mods.add(Modifier.ACC_PUBLIC);		
		ArrayList<Stmt> stmts = new ArrayList();
		
		Expr.ClassVariable enumClass = new Expr.ClassVariable("java.lang.Enum");
		enumClass.attributes().add(new Type.Clazz("java.lang","Enum"));
					
		Value.Class p1 = new Value.Class(jtype,loc);
		p1.value().attributes().add(type);
		p1.attributes().add(new Type.Clazz("java.lang", "Class"));		
				
		Expr.LocalVariable p2 = new Expr.LocalVariable("key",loc);
		p2.attributes().add(Types.JAVA_LANG_STRING);		
				
		ArrayList<Expr> params = new ArrayList();
		params.add(p1);
		params.add(p2);
				
		Expr.Invoke ivk = new Expr.Invoke(enumClass,"valueOf",params,
				new ArrayList(), loc);
		ivk.attributes().add(Types.JAVA_LANG_ENUM);
		Type.Function ivkFtype = new Type.Function(Types.JAVA_LANG_ENUM,
				new Type.Clazz("java.lang", "Class"), Types.JAVA_LANG_STRING);
		ivk.attributes().add(new JilBuilder.MethodInfo(new ArrayList(),ivkFtype));
						
		Expr.Cast cast = new Expr.Cast(jtype,ivk,loc);		
		cast.type().attributes().add(type);
		cast.attributes().add(type);
		
		Stmt.Return ret = new Stmt.Return(cast,loc); 
		stmts.add(ret);
		
		Stmt.Block block = new Stmt.Block(stmts, loc);

		ArrayList<Decl.JavaParameter> mparams = new ArrayList();
		jkit.java.tree.Type stype = new jkit.java.tree.Type.Clazz("java.lang.String");
		stype.attributes().add(Types.JAVA_LANG_STRING);
		mparams.add(new Decl.JavaParameter("key",new ArrayList(), stype));
		
		Decl.JavaMethod m = new Decl.JavaMethod(mods, "valueOf",
				jtype, mparams, false,
				new ArrayList(), new ArrayList(), block, loc);

		m.attributes().add(ftype);
		
		return m;		
	}
	
	protected Decl.StaticInitialiserBlock createStaticInitialiser(
			Decl.JavaEnum ec, Type.Clazz type, JilClass skeleton)
			throws ClassNotFoundException, MethodNotFoundException {
		SourceLocation loc = ec.attribute(SourceLocation.class);
		jkit.java.tree.Type.Clazz ecType = new jkit.java.tree.Type.Clazz(ec.name(), loc);
		jkit.java.tree.Type.Array aecType = new jkit.java.tree.Type.Array(ecType);
		Type aType = new Type.Array(type);

		Expr.ClassVariable thisClass = new Expr.ClassVariable(ec.name(),loc,type);
		
		ArrayList<Stmt> stmts = new ArrayList();	

		// First, create the array to hold the types.
		Expr param = new Value.Int(ec.constants().size(),Types.T_INT);
		ArrayList<Expr> arguments = new ArrayList();
		arguments.add(param);
		
		Expr.New anew = new Expr.New(aecType, null, arguments, new ArrayList(),loc,aType);
		anew.type().attributes().add(aType);
		Expr.Deref aderef = new Expr.Deref(thisClass, "$VALUES",aType);					
		stmts.add(new Stmt.Assignment(aderef,anew));
		
		// Second, add the enum constants to the array
		int i=0;
		int extraClassCount = 0;
		for (Decl.EnumConstant c : ec.constants()) {
			Type.Clazz mytype = type;
			jkit.java.tree.Type.Clazz myecType = new jkit.java.tree.Type.Clazz(ec.name(), loc);
			
			if(c.declarations().size() > 0) {
				String name = Integer.toString(++extraClassCount);				
				ArrayList<Pair<String, List<Type.Reference>>> ncomponents = new ArrayList(
						type.components());
				ncomponents.add(new Pair(name, new ArrayList()));
				mytype = new Type.Clazz(type.pkg(),
						ncomponents);							
			}
			
			arguments = new ArrayList();									
			arguments.add(new Value.String(c.name(),Types.JAVA_LANG_STRING));
			arguments.add(new Value.Int(i,Types.T_INT));
			arguments.addAll(c.arguments());
			
			myecType.attributes().add(mytype);
			Expr.New nuw = new Expr.New(myecType, null, arguments,
					new ArrayList(), loc,mytype);
			
			ArrayList<Type> paramTypes = new ArrayList<Type>();
			paramTypes.add(Types.JAVA_LANG_STRING);
			paramTypes.add(Types.T_INT);
			
			for(Expr e : c.arguments()) {
				Type t = e.attribute(Type.class);												
				paramTypes.add(t);
			}
			
			// At this point, we have to resolve the constructor.
			Type.Function ftype = types.resolveMethod(mytype,
					mytype.lastComponent().first(), paramTypes, loader).second()
					.type();
			
			nuw.attributes().add(new JilBuilder.MethodInfo(new ArrayList(),ftype));		
						
			Expr.Deref deref = new Expr.Deref(thisClass, "$VALUES",
					aType);									
			Expr index = new Value.Int(i++, Types.T_INT);			
			Expr.ArrayIndex array = new Expr.ArrayIndex(deref, index, type);
			
			Expr.Deref fderef = new Expr.Deref(thisClass, c.name(), type);			
			stmts.add(new Stmt.Assignment(fderef, nuw));
			stmts.add(new Stmt.Assignment(array, fderef));						
		}

		Decl.StaticInitialiserBlock blk = new Decl.StaticInitialiserBlock(stmts,loc);
				
		return blk;	
	}
	
	protected void augmentConstructors(Decl.JavaEnum ec, JilClass skeleton) {
		// First do the skeleton's constructors		
		for(JilMethod m : skeleton.methods()) {
			if(m.name().equals(ec.name())) {
				// this is a constructor.
				
				// first, update it's type
				Type.Function oldType = m.type();
				ArrayList<Type> nparams = new ArrayList<Type>();
				nparams.add(Types.JAVA_LANG_STRING);
				nparams.add(Types.T_INT);
				nparams.addAll(oldType.parameterTypes());
				Type.Function newType = new Type.Function(Types.T_VOID,nparams);
				m.setType(newType);

				// second, update its parameters
				List<JilMethod.JilParameter> params = m.parameters();
				List<Modifier> mods = new ArrayList<Modifier>();
				mods.add(Modifier.ACC_FINAL);
				params.add(0,new JilMethod.JilParameter("$1",mods));
				params.add(1,new JilMethod.JilParameter("$2",new ArrayList(mods)));
				
				// third, update its modifiers
				mods = m.modifiers();
				if(mods.contains(Modifier.ACC_PUBLIC)) {
					// This should probably be done earlier in the pipeline.
                    // Otherwise, you could call the constructor explicitly and
                    // it would not fail type checking.
					mods.remove(Modifier.ACC_PUBLIC);
					mods.add(Modifier.ACC_PRIVATE);
				}
			}
		}
		
		// Second (harder), do the source constructors.
		for(Decl d : ec.declarations()) {
			if(d instanceof Decl.JavaMethod) {
				Decl.JavaMethod m = (Decl.JavaMethod) d;
				if(m.name().equals(ec.name())) {
					augmentConstructor(ec,m);
				}
			}
		}
	}
	
	protected void augmentConstructor(Decl.JavaEnum ec, Decl.JavaMethod m) {
		SourceLocation loc = ec.attribute(SourceLocation.class);
		List<Decl.JavaParameter> params = m.parameters();
		
		// First, update method type
		Type.Function oldType = m.attribute(Type.Function.class);
		m.attributes().remove(oldType);
		ArrayList<Type> nparams = new ArrayList<Type>();
		nparams.add(Types.JAVA_LANG_STRING);
		nparams.add(Types.T_INT);
		nparams.addAll(oldType.parameterTypes());
		Type.Function newType = new Type.Function(Types.T_VOID,nparams);
		m.attributes().add(newType);
		
		// First, update the number of parameters to the constructor.
		jkit.java.tree.Type intType = new jkit.java.tree.Type.Int();
		intType.attributes().add(Types.T_INT);
		jkit.java.tree.Type stringType = new jkit.java.tree.Type.Clazz("java.lang.String");
		stringType.attributes().add(Types.JAVA_LANG_STRING);
		
		// parameters must go at front of list.		
		params.add(0,new Decl.JavaParameter("$1",new ArrayList(),stringType));
		params.add(1,new Decl.JavaParameter("$2",new ArrayList(),intType));				
						
		// Second, add code to the constructor.
		Type.Function ftype = new Type.Function(Types.T_VOID,Types.JAVA_LANG_STRING,Types.T_INT);
		
		ArrayList<Modifier> mods = new ArrayList<Modifier>();
		mods.add(Modifier.ACC_PRIVATE);
		
		Expr.LocalVariable p1 = new Expr.LocalVariable("$1");
		p1.attributes().add(Types.JAVA_LANG_STRING);
		Expr.LocalVariable p2 = new Expr.LocalVariable("$2");
		p2.attributes().add(Types.T_INT);		
		ArrayList<Expr> superParams = new ArrayList<Expr>();
		superParams.add(p1);
		superParams.add(p2);
		
		Expr.LocalVariable supeR = new Expr.LocalVariable("super",loc);
		supeR.attributes().add(new Type.Clazz("java.lang","Enum"));
		
		Expr.Invoke ivk = new Expr.Invoke(supeR, "super", superParams,
				new ArrayList(), loc);
		ivk.attributes().add(ftype.returnType());
		ivk.attributes().add(new JilBuilder.MethodInfo(new ArrayList(), ftype));
		
		m.body().statements().add(0,ivk);
	}
	
	protected void createDefaultConstructor(Decl.JavaEnum ec, JilClass skeleton) {		
		SourceLocation loc = ec.attribute(SourceLocation.class);
		Type.Function ftype = new Type.Function(Types.T_VOID,Types.JAVA_LANG_STRING,Types.T_INT);
		
		// First, create a source code constructor.
		ArrayList<Modifier> mods = new ArrayList<Modifier>();
		mods.add(Modifier.ACC_PRIVATE);
		mods.add(Modifier.ACC_SYNTHETIC);
		
		Expr.LocalVariable p1 = new Expr.LocalVariable("$1");
		p1.attributes().add(Types.JAVA_LANG_STRING);
		Expr.LocalVariable p2 = new Expr.LocalVariable("$2");
		p2.attributes().add(Types.T_INT);		
		ArrayList<Expr> params = new ArrayList<Expr>();
		params.add(p1);
		params.add(p2);
		
		Expr.LocalVariable supeR = new Expr.LocalVariable("super",loc);
		supeR.attributes().add(new Type.Clazz("java.lang","Enum"));
		
		Expr.Invoke ivk = new Expr.Invoke(supeR, "super", params,
				new ArrayList(), loc);
		ivk.attributes().add(ftype.returnType());
		ivk.attributes().add(new JilBuilder.MethodInfo(new ArrayList(), ftype));
		
		ArrayList<Stmt> stmts = new ArrayList();
		stmts.add(ivk);
		Stmt.Block block = new Stmt.Block(stmts, loc);
		ArrayList<Decl.JavaParameter> mparams = new ArrayList();
		jkit.java.tree.Type intType = new jkit.java.tree.Type.Int();
		intType.attributes().add(Types.T_INT);
		jkit.java.tree.Type stringType = new jkit.java.tree.Type.Clazz("java.lang.String");
		stringType.attributes().add(Types.JAVA_LANG_STRING);
		// parameters must go at front of list.		
		mparams.add(0,new Decl.JavaParameter("$1",new ArrayList(),stringType));
		mparams.add(1,new Decl.JavaParameter("$2",new ArrayList(),intType));
				
		Decl.JavaConstructor m = new Decl.JavaConstructor(mods, ec.name(),
				mparams, false, new ArrayList(), new ArrayList(), block, loc);


		m.attributes().add(ftype);
		
		ec.declarations().add(m);
		
		// second, add a skeleton constructor
		ArrayList<JilMethod.JilParameter> nparams = new ArrayList();
		mods = new ArrayList();
		mods.add(Modifier.ACC_FINAL);		
		nparams.add(new JilMethod.JilParameter("$1",mods));
		nparams.add(new JilMethod.JilParameter("$2",new ArrayList(mods)));
		mods = new ArrayList();
		mods.add(Modifier.ACC_PRIVATE);
		mods.add(Modifier.ACC_SYNTHETIC);
		JilMethod jm = new JilMethod(ec.name(), ftype, nparams, mods,
				new ArrayList(), loc);
		
		skeleton.methods().add(jm);
	}
}
