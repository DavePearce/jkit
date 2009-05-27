package jkit.java.stages;

import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.compiler.Clazz;
import jkit.compiler.FieldNotFoundException;
import jkit.compiler.MethodNotFoundException;
import jkit.compiler.Clazz.Method;
import static jkit.compiler.SyntaxError.*;
import static jkit.jil.util.Types.*;
import static jkit.java.tree.Type.fromJilType;
import jkit.java.io.JavaFile;
import jkit.java.tree.Decl;
import jkit.java.tree.Expr;
import jkit.java.tree.Stmt;
import jkit.java.tree.Value;
import jkit.java.tree.Decl.*;
import jkit.java.tree.Stmt.Case;
import jkit.util.*;
import jkit.jil.tree.Attribute;
import jkit.jil.tree.JilMethod;
import jkit.jil.tree.Modifier;
import jkit.jil.tree.SourceLocation;
import jkit.jil.tree.SyntacticElement;
import jkit.jil.tree.Type;

/**
 * The purpose of this operation, is to propagate type information throughout
 * the expressions and statements of the Javafile. The key challenge is that, in
 * many places, we must apply rules from the Java Language Spec to determine
 * what the resulting type. For example, in the context of a binary expression
 * (e.g. +) if the types of the left and right operands differ, we must
 * carefully determine the resulting type.
 * 
 * This operation also introduces "Convert" objects in places to avoid future
 * ambiguity. For example, if we have a binary operation whose left and
 * right-hand operand types differ then, under the JLS, we may need to apply an
 * up-conversion (e.g. int -> long) to the parameter before the operation. The
 * Convert objects introduced thus capture the situations where such conversions
 * are required; this helps later on, since we don't have to repeat the working
 * to determine where a conversion is needed.
 * 
 * @author djp
 * 
 */
public class TypePropagation {
	private ClassLoader loader;
	private TypeSystem types;
	private Stack<JavaClass> scopes = new Stack<JavaClass>();
	
	public TypePropagation(ClassLoader loader, TypeSystem types) {
		this.loader = loader; 
		this.types = types;
	}
	
	public void apply(JavaFile file) {
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
		scopes.push(c);
		
		for(Decl d : c.declarations()) {
			doDeclaration(d);
		}
		
		scopes.pop();
	}

	protected void doMethod(JavaMethod d) {
		doStatement(d.body(),d);
	}

	protected void doField(JavaField d) {
		Expr init = d.initialiser();
		Type type = (Type) d.type().attribute(Type.class);
		
		// special case for dealing with array values.
		// perform type inference (if necesssary)
		if(init != null) {
			if(isUnknownConstant(init)) {			
				Expr c = unknownConstantInference(init, type,
						(SourceLocation) init
						.attribute(SourceLocation.class));
				d.setInitialiser(implicitCast(c,type));
			} else if(init instanceof Value.Array) {
				doArrayVal(type,(Value.Array) init);
			} else {
				doExpression(init);			
				d.setInitialiser(implicitCast(init,type));					
			}
			
		}
	}
	
	protected void doInitialiserBlock(Decl.InitialiserBlock d) {
		// will need to add code here for dealing with classes nested in
		// methods.
		for (Stmt s : d.statements()) {
			doStatement(s, null);
		}
	}
	
	protected void doStaticInitialiserBlock(Decl.StaticInitialiserBlock d) {
		// will need to add code here for dealing with classes nested in
		// methods.
		for (Stmt s : d.statements()) {
			doStatement(s, null);
		}
	}
	
	protected void doStatement(Stmt e, JavaMethod m) {
		if(e instanceof Stmt.SynchronisedBlock) {
			doSynchronisedBlock((Stmt.SynchronisedBlock)e, m);
		} else if(e instanceof Stmt.TryCatchBlock) {
			doTryCatchBlock((Stmt.TryCatchBlock)e, m);
		} else if(e instanceof Stmt.Block) {
			doBlock((Stmt.Block)e, m);
		} else if(e instanceof Stmt.VarDef) {
			doVarDef((Stmt.VarDef) e, m);
		} else if(e instanceof Stmt.Assignment) {
			doAssignment((Stmt.Assignment) e, m);
		} else if(e instanceof Stmt.Return) {
			doReturn((Stmt.Return) e, m);
		} else if(e instanceof Stmt.Throw) {
			doThrow((Stmt.Throw) e, m);
		} else if(e instanceof Stmt.Assert) {
			doAssert((Stmt.Assert) e, m);
		} else if(e instanceof Stmt.Break) {
			doBreak((Stmt.Break) e, m);
		} else if(e instanceof Stmt.Continue) {
			doContinue((Stmt.Continue) e, m);
		} else if(e instanceof Stmt.Label) {
			doLabel((Stmt.Label) e, m);
		} else if(e instanceof Stmt.If) {
			doIf((Stmt.If) e, m);
		} else if(e instanceof Stmt.For) {
			doFor((Stmt.For) e, m);
		} else if(e instanceof Stmt.ForEach) {
			doForEach((Stmt.ForEach) e, m);
		} else if(e instanceof Stmt.While) {
			doWhile((Stmt.While) e, m);
		} else if(e instanceof Stmt.DoWhile) {
			doDoWhile((Stmt.DoWhile) e, m);
		} else if(e instanceof Stmt.Switch) {
			doSwitch((Stmt.Switch) e, m);
		} else if(e instanceof Expr.Invoke) {
			doInvoke((Expr.Invoke) e);
		} else if(e instanceof Expr.New) {
			doNew((Expr.New) e);
		} else if(e instanceof Decl.JavaClass) {
			doClass((Decl.JavaClass)e);
		} else if(e instanceof Stmt.PrePostIncDec) {
			doExpression((Stmt.PrePostIncDec)e);
		} else if(e != null) {
			syntax_error("Internal failure (invalid statement \""
					+ e.getClass() + "\" encountered)", e);			
		}		
	}
	
	protected void doBlock(Stmt.Block block, JavaMethod m) {
		if(block != null) {			
			// now process every statement in this block.
			for(Stmt s : block.statements()) {
				doStatement(s, m);
			}
		}
	}
	
	protected void doSynchronisedBlock(Stmt.SynchronisedBlock block, JavaMethod m) {
		doBlock(block,m);
		doExpression(block.expr());
	}
	
	protected void doTryCatchBlock(Stmt.TryCatchBlock block, JavaMethod m) {
		doBlock(block,m);
		doBlock(block.finaly(),m);

		for (Stmt.CatchBlock cb : block.handlers()) {			
			doBlock(cb,m);
		}
	}
	
	protected void doVarDef(Stmt.VarDef def, JavaMethod m) {
		Type t = (Type) def.type().attribute(Type.class);
		
		List<Triple<String, Integer, Expr>> defs = def.definitions();
		for(int i=0;i!=defs.size();++i) {
			Triple<String, Integer, Expr> d = defs.get(i);			
			
			// calculate the actual type of this variable.
			Type nt = t;						
			for(int j=0;j!=d.second();++j) {
				nt = new Type.Array(nt);
			}						
			
			// perform type inference (if necesssary)
			if(d.third() != null) {
				if(isUnknownConstant(d.third())) {
					Expr c = unknownConstantInference(d.third(), nt,
							(SourceLocation) d.third
							.attribute(SourceLocation.class));
					defs.set(i, new Triple(d.first(), d.second(), implicitCast(c,nt)));
				} else if(d.third() instanceof Value.Array) {
					doArrayVal(nt,(Value.Array) d.third());
				} else {
					doExpression(d.third());
					defs.set(i, new Triple(d.first(), d.second(), implicitCast(
							d.third(), nt)));
				}
				
			}
		}
	}
	
	protected void doAssignment(Stmt.Assignment def, JavaMethod m) {
		doExpression(def.lhs());	
		doExpression(def.rhs());			

		Type lhs_t = (Type) def.lhs().attribute(Type.class);												
		
		// perform type inference (if necesssary)
		if(isUnknownConstant(def.rhs())) {			
			Expr c = unknownConstantInference(def.rhs(), lhs_t,
					(SourceLocation) def.rhs()
							.attribute(SourceLocation.class));
			
			def.setRhs(c);			
		} 
		
		def.setRhs(implicitCast(def.rhs(),lhs_t));		
		
		def.attributes().add(lhs_t);
	}
	
	protected void doReturn(Stmt.Return ret, JavaMethod m) {
		if(ret.expr() != null) {
			// We need to do an implict cast here to account for autoboxing, and
			// other conversions. For example, a method declared to return
			// Integer that actually returns "1" must box this at the point of
			// return.
			doExpression(ret.expr());
			ret.setExpr(implicitCast(ret.expr(), (Type) m.returnType()
					.attribute(Type.class)));
		}
	}
	
	protected void doThrow(Stmt.Throw ret, JavaMethod m) {
		doExpression(ret.expr());
	}
	
	protected void doAssert(Stmt.Assert ret, JavaMethod m) {
		doExpression(ret.expr());
	}
	
	protected void doBreak(Stmt.Break brk, JavaMethod m) {
		// nothing	
	}
	
	protected void doContinue(Stmt.Continue brk, JavaMethod m) {
		// nothing
	}
	
	protected void doLabel(Stmt.Label lab, JavaMethod m) {						
		doStatement(lab.statement(),m);
	}
	
	protected void doIf(Stmt.If stmt, JavaMethod m) {
		doExpression(stmt.condition());
		stmt.setCondition(implicitCast(stmt.condition(), T_BOOL));
		doStatement(stmt.trueStatement(),m);
		doStatement(stmt.falseStatement(),m);
	}
	
	protected void doWhile(Stmt.While stmt, JavaMethod m) {
		doExpression(stmt.condition());
		stmt.setCondition(implicitCast(stmt.condition(), T_BOOL));
		doStatement(stmt.body(),m);		
	}
	
	protected void doDoWhile(Stmt.DoWhile stmt, JavaMethod m) {
		doExpression(stmt.condition());
		stmt.setCondition(implicitCast(stmt.condition(), T_BOOL));
		doStatement(stmt.body(),m);
	}
	
	protected void doFor(Stmt.For stmt, JavaMethod m) {		
		doStatement(stmt.initialiser(),m);
		doExpression(stmt.condition());		
		stmt.setCondition(implicitCast(stmt.condition(), T_BOOL));
		doStatement(stmt.increment(),m);
		doStatement(stmt.body(),m);	
	}
	
	protected void doForEach(Stmt.ForEach stmt, JavaMethod m) {
		doExpression(stmt.source());
		doStatement(stmt.body(),m);
	}
	
	protected void doSwitch(Stmt.Switch sw, JavaMethod m) {
		doExpression(sw.condition());
		sw.setCondition(implicitCast(sw.condition(),T_INT));
		for(Case c : sw.cases()) {
			doExpression(c.condition());
			for(Stmt s : c.statements()) {
				doStatement(s,m);
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
		} else if(e instanceof Value.Class) {
			doClassVal((Value.Class) e);
		} else if(e instanceof Expr.LocalVariable) {
			doLocalVariable((Expr.LocalVariable)e);
		} else if(e instanceof Expr.NonLocalVariable) {
			doNonLocalVariable((Expr.NonLocalVariable)e);
		} else if(e instanceof Expr.ClassVariable) {
			doClassVariable((Expr.ClassVariable)e);
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
			doAssignment((Stmt.Assignment) e, null);			
		} else if(e != null) {
			syntax_error("Internal failure (invalid expression \"" + e.getClass() + "\" encountered)",e);			
		}
	}
	
	protected void doDeref(Expr.Deref e) {		
		doExpression(e.target());	
		
		Type tmp = (Type) e.target().attribute(Type.class);
		
		if(!(tmp instanceof Type.Reference)) {
			syntax_error("cannot dereference type: " + tmp,e);
		} else if(tmp instanceof Type.Array) {
			// This dereference must represent an internal method access.
			if(e.name().equals("length")) {
				// most common case.
				e.attributes().add(T_INT);
			} else {
				syntax_error("field not found: " + tmp + "." + e.name(),e);
			}
		} else {

			Type.Clazz target = (Type.Clazz) tmp;

			if(e.name().equals("this")) {
				// This is a special case, where we're trying to look up a field
				// called "this". No such field can exist! What this means is that
				// we're inside an inner class, and we're trying to access the this
				// pointer of an enclosing class. This is easy to deal with here,
				// since the type returned by this expression will be the target
				// type of the dereference.
				e.attributes().add(target);
			} else {
				// now, perform field lookup!
				try {
					Triple<Clazz, Clazz.Field, Type> r = types
							.resolveField(target, e.name(), loader);
					e.attributes().add(r.third());			
				} catch(ClassNotFoundException cne) {
					syntax_error("class not found: " + target,e,cne);
				} catch(FieldNotFoundException fne) {
					syntax_error("field not found: " + target + "." + e.name(),e,fne);
				}
			}
		}
	}
	
	protected void doArrayIndex(Expr.ArrayIndex e) {
		doExpression(e.target());
		doExpression(e.index());
		
		e.setIndex(implicitCast(e.index(),T_INT));
				
		Type target_t = (Type) e.target().attribute(Type.class);
		
		if(target_t instanceof Type.Array) {
			Type.Array at = (Type.Array) target_t;
			e.attributes().add(at.element());
		} else {
			// this is really a syntax error
			syntax_error("array required, but " + target_t + " found", e);
		}
	}
	
	protected void doNew(Expr.New e) {
		// First, figure out the type being created.		
		Type t = (Type) e.type().attribute(Type.class);
		e.attributes().add(t);
		
		doExpression(e.context());
		
		// Second, recurse through any parameters supplied ...
		ArrayList<Type> parameterTypes = new ArrayList<Type>();
		
		for(Expr p : e.parameters()) {
			doExpression(p);
			parameterTypes.add((Type) p.attribute(Type.class));
		}
		
		if(t instanceof Type.Clazz) {
			Type.Clazz tc = (Type.Clazz) t;
			try {
				String constructorName = tc.components().get(
						tc.components().size() - 1).first();
				Triple<Clazz, Clazz.Method, Type.Function> r = types
						.resolveMethod(tc, constructorName, parameterTypes,
								loader);
				Type.Function f = r.third();
				
				// At this stage, we have (finally) figured out what method is to be
				// called. There are a few things that remain to be done, however.
				// Firstly, we must add any implicitCasts that are required for
				// boxing conversions.  
					
				List<Expr> e_parameters = e.parameters();
				List<Type> ft_parameters = f.parameterTypes();
				for (int i = 0; i != e_parameters.size(); ++i) {
					Type pt = ft_parameters.get(i);
					e_parameters.set(i, implicitCast(e_parameters.get(i), pt));
				}
				
				Method m = r.second();
				e.attributes().add(new JilBuilder.MethodInfo(m.exceptions(),m.type()));								
			} catch(ClassNotFoundException cnfe) {
				syntax_error(cnfe.getMessage(), e, cnfe);
			} catch(MethodNotFoundException mfne) {
				String msg = "constructor not found: " + tc + "(";
				boolean firstTime = true;
				for (Type pt : parameterTypes) {
					if (!firstTime) {
						msg += ", ";
					}
					firstTime = false;
					msg += pt;
				}
				syntax_error(msg + ")", e, mfne);
			} catch(TypeSystem.BindError be) {
				// This can happen if the parameters supplied to bind, which is
				// called by resolveMethod are somehow not "base equivalent"
				syntax_error(be.getMessage(),e,be);
			} catch(Exception be) {
				// General catch all. The reason for having it is so we can
				// attribute the cause of the internal failure with a line number.
				syntax_error("internal failure (" + be.getMessage() + ")",e,be);
			} 
		} else if(t instanceof Type.Array) {
			// need to do something here also ...
		}
		
		// Third, check whether this is constructing an anonymous class ...
		for(Decl d : e.declarations()) {
			doDeclaration(d);
		}
	}
	
	protected void doInvoke(Expr.Invoke e) {
		ArrayList<Type> parameterTypes = new ArrayList<Type>();
		
		doExpression(e.target());
		
		for(Expr p : e.parameters()) {
			doExpression(p);
			parameterTypes.add((Type) p.attribute(Type.class));
		}
		
		// Now, to determine the return type of this method, we need to lookup
		// the method in the class hierarchy. This lookup procedure is seriously
		// non-trivial, and is implemented in the TypeSystem module.
			
		Type.Reference receiver = null;
		String e_name = e.name();
		
		try {		
			if(e.name().equals("super") || e.name().equals("this")) {				
				Type.Clazz r = (Type.Clazz) e.attribute(Type.class);				
				e_name = r.components().get(r.components().size() - 1).first();
				receiver = r;
			} else {
				Type rt = (Type) e.target().attribute(Type.class);
				
				if(rt instanceof Type.Variable) {
					// in this situation, we're trying to dereference a generic
					// variable. Therefore, we choose the largest type which
					// this could possibly, and assume the receiver is this type.
					Type.Variable vt = (Type.Variable) rt;										
					
					if(vt.lowerBound() != null) {
						receiver = vt.lowerBound(); 
					} else {						
						receiver = JAVA_LANG_OBJECT;
					}
				} else if(rt instanceof Type.Array) {
					receiver = JAVA_LANG_OBJECT;
				} else {
					receiver = (Type.Clazz) e.target().attribute(Type.class);
				}
			}							
			
			Triple<Clazz, Clazz.Method, Type.Function> r = types
					.resolveMethod(receiver, e_name, parameterTypes, loader);
									
			Type.Function f = r.third();
			
			// At this stage, we have (finally) figured out what method is to be
			// called. There are a few things that remain to be done, however.
			// Firstly, we must add any implicitCasts that are required for
			// boxing conversions.  
				
			List<Expr> e_parameters = e.parameters();
			List<Type> ft_parameters = f.parameterTypes();
			for (int i = 0; i != e_parameters.size(); ++i) {
				Type pt = ft_parameters.get(i);
				e_parameters.set(i, implicitCast(e_parameters.get(i), pt));
			}
			
			// Secondly, we must add type information to the expression.
			
			if (!(f.returnType() instanceof Type.Void)) {				
				e.attributes().add(f.returnType());
			}						
			
			Method m = r.second();
			e.attributes().add(new JilBuilder.MethodInfo(m.exceptions(),m.type()));								
		} catch(ClassNotFoundException cnfe) {
			syntax_error(cnfe.getMessage(), e, cnfe);
		} catch(MethodNotFoundException mfne) {
			String msg = "method not found: " + receiver + "." + e_name + "(";
			boolean firstTime = true;
			for (Type t : parameterTypes) {
				if (!firstTime) {
					msg += ", ";
				}
				firstTime = false;
				msg += t;
			}
			syntax_error(msg + ")", e, mfne);
		} catch(TypeSystem.BindError be) {
			// This can happen if the parameters supplied to bind, which is
			// called by resolveMethod are somehow not "base equivalent"
			syntax_error(be.getMessage(),e,be);
		} catch(Exception be) {
			// General catch all. The reason for having it is so we can
			// attribute the cause of the internal failure with a line number.
			syntax_error("internal failure (" + be.getMessage() + ")",e,be);
		} 
	}
	
	protected void doInstanceOf(Expr.InstanceOf e) {
		doExpression(e.lhs());
		e.attributes().add(T_BOOL);
	}
	
	protected void doCast(Expr.Cast e) {
		Type ct = (Type) e.type().attribute(Type.class);
		doExpression(e.expr());
		// the implicit cast is required to deal with boxing/unboxing (amongst
		// other things?)
		e.setExpr(implicitCast(e.expr(),ct));
		e.attributes().add(ct);
	}
	
	protected void doBoolVal(Value.Bool e) {
		e.attributes().add(T_BOOL);
	}
	
	protected void doCharVal(Value.Char e) {
		e.attributes().add(T_CHAR);
	}
	
	protected void doIntVal(Value.Int e) {
		e.attributes().add(T_INT);
	}
	
	protected void doLongVal(Value.Long e) {		
		e.attributes().add(T_LONG);
	}
	
	protected void doFloatVal(Value.Float e) {		
		e.attributes().add(T_FLOAT);
	}
	
	protected void doDoubleVal(Value.Double e) {		
		e.attributes().add(T_DOUBLE);
	}
	
	protected void doStringVal(Value.String e) {		
		e.attributes().add(JAVA_LANG_STRING);
	}
	
	protected void doNullVal(Value.Null e) {		
		e.attributes().add(new Type.Null());
	}
	
	protected void doTypedArrayVal(Value.TypedArray e) {		
		Type _type = (Type) e.type().attribute(Type.class);
		if(!(_type instanceof Type.Array)) {
			syntax_error("cannot assign array value to type " + _type,e);
		}		
		Type.Array type = (Type.Array) _type;
		
		for(int i=0;i!=e.values().size();++i) {
			Expr v = e.values().get(i);
			if(v instanceof Value.Array) {
				Type.Array ta = (Type.Array) type;
				doArrayVal(ta,(Value.Array)v);
			} else if (isUnknownConstant(v)) {
				v = unknownConstantInference(v, type.element(),
						(SourceLocation) v.attribute(SourceLocation.class));				
			} else {
				doExpression(v);				
			}			
			e.values().set(i,implicitCast(v,type.element()));
		}

		e.attributes().add(type);
	}
	
	
	/**
	 * Dealing with Array Value's requireas a special method, where the result
	 * type of the ArrayVal is known. This is important since the type of an
	 * array initialiser is actually computed from the type of the
	 * variable/field/... that it's being assigned to. For example, in the
	 * following:
	 * <p>
	 * 
	 * <pre>
	 * Object[] x = { 1, 2, 3 };
	 * </pre>
	 * 
	 * </p>
	 * 
	 * the type of ArrayVal must be Object[], not int[] (which would otherwise
	 * be inferred).
	 * 
	 * @param type
	 *            --- type to cast the lhs into
	 * @param e
	 *            --- Array expression
	 * @return
	 */
	protected void doArrayVal(Type _lhs, Value.Array e) {		
		if(!(_lhs instanceof Type.Array)) {
			syntax_error("cannot assign array value to type " + _lhs,e);
		}		
		Type.Array lhs = (Type.Array)_lhs;
		for(int i=0;i!=e.values().size();++i) {
			Expr v = e.values().get(i);
			if(v instanceof Value.Array) {
				Type.Array ta = (Type.Array) lhs;
				doArrayVal(ta,(Value.Array)v);
			} else if(isUnknownConstant(v)) {			
				v = unknownConstantInference(v, lhs.element(),
						(SourceLocation) v.attribute(SourceLocation.class));										
			} else {
				doExpression(v);				
			}					
			e.values().set(i,implicitCast(v,lhs.element()));
		}
		
		e.attributes().add(lhs);
	}
	
	protected void doClassVal(Value.Class e) {
		// Basically, this corresponds to some code like this:
		// <pre>
		// void f() {
		// String x = String.class.getName();
		// }
		// <pre>
		// Here, the Class Value is that returned by "String.class" and it
		// corresponds to an instance of java.lang.Class<String>. Therefore, we
		// need to construct a type representing java.lang.Class<X> here.
		
		Type t = (Type) e.value().attribute(Type.class);
		if(t instanceof Type.Clazz) {
			Type.Clazz c = (Type.Clazz) t;
			List<Type.Reference> tvars = new ArrayList();
			tvars.add(c);
			List<Pair<String, List<Type.Reference>>> components = new ArrayList();
			components.add(new Pair("Class",tvars));
			e.attributes().add(new Type.Clazz("java.lang",components));
		} else {
			// Could possibly do a bit better here, in the case of type
			// variables.  No matter !
			List<Type.Reference> tvars = new ArrayList();
			tvars.add(new Type.Wildcard(null,null));
			List<Pair<String, List<Type.Reference>>> components = new ArrayList();
			components.add(new Pair("Class",tvars));
			e.attributes().add(new Type.Clazz("java.lang",components));			
		}
	}
	
	protected void doLocalVariable(Expr.LocalVariable e) {
		// don't need to do anything here --- ScopeResolution has initialised
		// the type of all variable accesses.
	}

	protected void doNonLocalVariable(Expr.NonLocalVariable e) {
		// don't need to do anything here --- ScopeResolution has initialised
		// the type of all variable accesses.
	}
	
	protected void doClassVariable(Expr.ClassVariable e) {
		// don't need to do anything here --- ScopeResolution has initialised
		// the type of all variable accesses.		
	}
	
	protected void doUnOp(Expr.UnOp e) {		
		doExpression(e.expr());
		Type expr_t = (Type) e.expr().attribute(Type.class);
		
		switch(e.op()) {
		case Expr.UnOp.NEG:
			if (expr_t instanceof Type.Byte || expr_t instanceof Type.Char
					|| expr_t instanceof Type.Short) {
				// This is a strange feature of javac. I don't really understand
				// why it's necessary.
				e.setExpr(implicitCast(e.expr(),T_INT));
				e.attributes().add(T_INT);				
			} else {
				e.attributes().add(expr_t);				
			}
			break;		
		case Expr.UnOp.INV:
			if (expr_t instanceof Type.Byte || expr_t instanceof Type.Char
					|| expr_t instanceof Type.Short) {
				// This is a strange feature of javac. I don't really understand
				// why it's necessary.
				e.setExpr(implicitCast(e.expr(),T_INT));
				e.attributes().add(T_INT);				
			} else {
				e.attributes().add(expr_t);
			} 
			break;	
		default:
			e.attributes().add(expr_t);	
		}		
	}
		
	protected void doBinOp(Expr.BinOp e) {				
		doExpression(e.lhs());
		doExpression(e.rhs());
		
		Type lhs_t = (Type) e.lhs().attribute(Type.class);
		Type rhs_t = (Type) e.rhs().attribute(Type.class);
		
		switch(e.op()) {
			case Expr.BinOp.EQ:
			case Expr.BinOp.NEQ:
			case Expr.BinOp.LT:
			case Expr.BinOp.LTEQ:
			case Expr.BinOp.GT:
			case Expr.BinOp.GTEQ:
			{
				if ((lhs_t instanceof Type.Primitive || isBoxedType(lhs_t))
						&& (rhs_t instanceof Type.Primitive || isBoxedType(rhs_t))) {
					Type rt = binaryNumericPromotion(lhs_t, rhs_t, e);
					e.setLhs(implicitCast(e.lhs(), rt));
					e.setRhs(implicitCast(e.rhs(), rt));
					e.attributes().add(T_BOOL);
				} else if (e.op() == Expr.BinOp.EQ || e.op() == Expr.BinOp.NEQ) {
					e.attributes().add(T_BOOL);
				} else {
					syntax_error("operands have invalid types " + lhs_t + " and "
						+ rhs_t, e);
				}
				break;
			}
			case Expr.BinOp.ADD:
			case Expr.BinOp.SUB:
			case Expr.BinOp.MUL:
			case Expr.BinOp.DIV:
			case Expr.BinOp.MOD:
			{						
				if ((lhs_t instanceof Type.Primitive || isBoxedType(lhs_t))
						&& (rhs_t instanceof Type.Primitive || isBoxedType(rhs_t))) {
					Type rt = binaryNumericPromotion(lhs_t, rhs_t, e);
					e.setLhs(implicitCast(e.lhs(), rt));
					e.setRhs(implicitCast(e.rhs(), rt));
					e.attributes().add(rt);
				} else if (e.op() == Expr.BinOp.ADD
						&& (isJavaLangString(lhs_t) || isJavaLangString(rhs_t))) {
					e.attributes().add(JAVA_LANG_STRING);
					e.setOp(Expr.BinOp.CONCAT);
				} else {
					syntax_error("operands have invalid types " + lhs_t + " and "
						+ rhs_t, e);
				}
				break;
			}
			case Expr.BinOp.SHL:
			case Expr.BinOp.SHR:
			case Expr.BinOp.USHR:
			{					
				if ((lhs_t instanceof Type.Primitive || isBoxedType(lhs_t))
						&& (rhs_t instanceof Type.Primitive || isBoxedType(rhs_t))) {
					Type rt_left = unaryNumericPromotion(lhs_t, e);
					e.setLhs(implicitCast(e.lhs(), rt_left));
					if(rhs_t instanceof Type.Long) {
						// This case is rather strange. Here, javac
						// automatically converts a long to an int without
						// complaining. The reason for this is that, presumably,
						// any loss of precision will not affect the outcome of
						// the shift (since we're shifting at most 64 bits).						
						Type ti = T_INT;
						Expr.Cast cast = new Expr.Cast(fromJilType(ti),
								e.rhs(), e.attributes());						
						cast.attributes().add(ti);
						e.setRhs(cast);
					} else {
						e.setRhs(implicitCast(e.rhs(), T_INT));
					}
					e.attributes().add(rt_left);
				} else {
					syntax_error("operands have invalid types " + lhs_t + " and " + rhs_t,e);
				}
				break;
			}
			case Expr.BinOp.LAND:
			case Expr.BinOp.LOR:
			{				
				e.setLhs(implicitCast(e.lhs(),T_BOOL));
				e.setRhs(implicitCast(e.rhs(),T_BOOL));
				e.attributes().add(T_BOOL);
				break;
			}
			case Expr.BinOp.AND:
			case Expr.BinOp.OR:
			case Expr.BinOp.XOR:
			{								
				if ((lhs_t instanceof Type.Primitive || isBoxedType(lhs_t))
						&& (rhs_t instanceof Type.Primitive || isBoxedType(rhs_t))) {
					Type rt = binaryNumericPromotion(lhs_t, rhs_t, e);
					e.setLhs(implicitCast(e.lhs(),rt));
					e.setRhs(implicitCast(e.rhs(),rt));
					e.attributes().add(rt);						
				} else {
					syntax_error("operands have invalid types " + lhs_t + " and "
						+ rhs_t, e);
				}
				break;
			}					
		}		
	}
	
	protected void doTernOp(Expr.TernOp e) {		
		doExpression(e.condition());
		doExpression(e.falseBranch());
		doExpression(e.trueBranch());
		
		Type lhs_t = (Type) e.trueBranch().attribute(Type.class);
		Type rhs_t = (Type) e.falseBranch().attribute(Type.class);
		
		/*
		 * See JLS Section 15.25 for more details on the rules that apply here. 
		 */
		if(lhs_t.equals(rhs_t)) {
			e.attributes().add(lhs_t);
		} else if((lhs_t instanceof Type.Bool || rhs_t instanceof Type.Bool)
				&& (isBoxedTypeOf(lhs_t,"Boolean") || isBoxedTypeOf(rhs_t,"Boolean"))) {
			e.attributes().add(T_BOOL);			
		} else if(lhs_t instanceof Type.Null) {			
			e.attributes().add(rhs_t);
		} else if(rhs_t instanceof Type.Null) {
			e.attributes().add(lhs_t);			
		} else if((lhs_t instanceof Type.Byte || rhs_t instanceof Type.Byte) && 
				(lhs_t instanceof Type.Short || rhs_t instanceof Type.Short)) {
			e.attributes().add(T_SHORT);
		} else if ((lhs_t instanceof Type.Byte || isBoxedTypeOf(lhs_t, "Byte"))
				&& rhs_t instanceof Type.Int
				&& isUnknownConstant(e.falseBranch())) {
			int v = evaluateUnknownConstant(e.falseBranch());
			if(v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
				e.setFalseBranch(implicitCast(e.falseBranch(),lhs_t));
				e.attributes().add(lhs_t);
				return;
			}
		} else if ((rhs_t instanceof Type.Byte || isBoxedTypeOf(rhs_t, "Byte"))
				&& lhs_t instanceof Type.Int
				&& isUnknownConstant(e.trueBranch())) {
			int v = evaluateUnknownConstant(e.trueBranch());
			if(v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
				e.setTrueBranch(implicitCast(e.trueBranch(),rhs_t));
				e.attributes().add(rhs_t);
				return;
			}
		} else if ((lhs_t instanceof Type.Char || isBoxedTypeOf(lhs_t, "Character"))
				&& rhs_t instanceof Type.Int
				&& isUnknownConstant(e.falseBranch())) {
			int v = evaluateUnknownConstant(e.falseBranch());
			if(v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
				e.setFalseBranch(implicitCast(e.falseBranch(),lhs_t));
				e.attributes().add(lhs_t);
				return;
			}
		} else if ((rhs_t instanceof Type.Char || isBoxedTypeOf(rhs_t, "Character"))
				&& lhs_t instanceof Type.Int
				&& isUnknownConstant(e.trueBranch())) {
			int v = evaluateUnknownConstant(e.trueBranch());
			if(v >= Character.MIN_VALUE && v <= Character.MAX_VALUE) {
				e.setTrueBranch(implicitCast(e.trueBranch(),rhs_t));
				e.attributes().add(rhs_t);
				return;
			}
		} else if ((lhs_t instanceof Type.Short || isBoxedTypeOf(lhs_t, "Short"))
				&& rhs_t instanceof Type.Int
				&& isUnknownConstant(e.falseBranch())) {
			int v = evaluateUnknownConstant(e.falseBranch());
			if(v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
				e.setFalseBranch(implicitCast(e.falseBranch(),lhs_t));
				e.attributes().add(lhs_t);
				return;
			}
		} else if ((rhs_t instanceof Type.Short || isBoxedTypeOf(rhs_t, "Short"))
				&& lhs_t instanceof Type.Int
				&& isUnknownConstant(e.trueBranch())) {
			int v = evaluateUnknownConstant(e.trueBranch());
			if(v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
				e.setTrueBranch(implicitCast(e.trueBranch(),rhs_t));
				e.attributes().add(rhs_t);
				return;
			}
		} else if ((isBoxedType(lhs_t) || isBoxedType(rhs_t))
				&& (lhs_t instanceof Type.Primitive || rhs_t instanceof Type.Primitive)) {
			Type rt = binaryNumericPromotion(lhs_t,rhs_t,e);
			e.attributes().add(rt);
			e.setTrueBranch(implicitCast(e.trueBranch(),rt));
			e.setFalseBranch(implicitCast(e.falseBranch(),rt));
		} else if(lhs_t instanceof Type.Reference && rhs_t instanceof Type.Reference) {
			// At this point, we have some class types and we need to determine
			// their greatest lower bound.
			Type rt;			
			if(lhs_t instanceof Type.Clazz && rhs_t instanceof Type.Clazz) {
				try {					
					rt = types.greatestSupertype((Type.Clazz) lhs_t,
							(Type.Clazz) rhs_t, loader);					
				} catch(ClassNotFoundException cne) {
					syntax_error(cne.getMessage(),e,cne);
					return; // dead code
				}				
			} else if(lhs_t instanceof Type.Clazz || rhs_t instanceof Type.Clazz) {
				rt = JAVA_LANG_OBJECT;
			} else if(lhs_t.equals(rhs_t)) {
				rt = lhs_t;
			} else {
				syntax_error("cannot determine result type for ternary operator",e);
				return; // dead code
			}
			
			e.attributes().add(rt);
		} else {
			// i'm not sure how you can get here.
			syntax_error("cannot determine result type for ternary operator",e);
		}
	}
		
	/**
     * Given the types of the left-hand and right-hand sides for a binary
     * operator, determine the appropriate type for that operator. This method
     * follows the Java Language Specification 5.6.1:
     * 
     * @param lhs
     * @param var
     * @return
     */
	public Type.Primitive unaryNumericPromotion(Type lhs, SyntacticElement e) {
		// First, we must unbox either operand if they are boxed.		
		if(lhs instanceof Type.Clazz) {
			lhs = unboxedType((Type.Clazz) lhs,e);
		}
		
		if (lhs instanceof Type.Char || lhs instanceof Type.Short
				|| lhs instanceof Type.Byte) {
			return T_INT;
		}
		
		return (Type.Primitive) lhs;
	}
	
	/**
     * Given the types of the left-hand and right-hand sides for a binary
     * operator, determine the appropriate type for that operator. This method
     * follows the Java Language Specification 5.6.2:
     * 
     * @param lhs
     * @param rhs
     * @return
     */
	public Type.Primitive binaryNumericPromotion(Type lhs, Type rhs, SyntacticElement e) {
		
		// First, we must unbox either operand if they are boxed.
		if(lhs instanceof Type.Clazz) {
			lhs = unboxedType((Type.Clazz) lhs,e);
		}
		if(rhs instanceof Type.Clazz) {
			rhs = unboxedType((Type.Clazz) rhs,e);
		}
		
		// Second, convert to the appropriate type
		if(lhs instanceof Type.Double || rhs instanceof Type.Double) {
			return T_DOUBLE;
		}
		if(lhs instanceof Type.Float || rhs instanceof Type.Float) {
			return T_FLOAT;
		}
		if(lhs instanceof Type.Long || rhs instanceof Type.Long) {
			return T_LONG;
		}
		
		// The following is not part of JLS 5.6.2, but is handy for dealing with
        // boolean operators &, |, ^ etc.
		if(lhs instanceof Type.Bool && rhs instanceof Type.Bool) {
			return T_BOOL;
		}
		
		return T_INT;		
	}
	
	/**
	 * This method looks at the actual type of an expression (1st param), and
	 * compares it with the required type (2nd param). If they are different it
	 * inserts an implicit type conversion. This is useful, since it means we
	 * only have to work out these type conversions the once, rather than every
	 * time we encounter an expression.
	 * 
	 * @param e - the expression whose actual type is to be compared.
	 * @param t - the required type of the expression.
	 * @return
	 */
	public static Expr implicitCast(Expr e, Type t) {
		if(e == null) { return null; }
		Type e_t = (Type) e.attribute(Type.class);
		// insert implicit casts for primitive types.
		if (!e_t.equals(t)
				&& (t instanceof Type.Primitive && e_t instanceof Type.Primitive)) {			
			e = new Expr.Convert(fromJilType((Type.Primitive)t), e, t, e.attribute(SourceLocation.class));
		} else if(t instanceof Type.Primitive && e_t instanceof Type.Clazz) {
			Type.Clazz r = (Type.Clazz) e_t;
			if (r.pkg().equals("java.lang") && r.components().size() == 1) {
				String c = r.components().get(0).first();
				if (c.equals("Byte")) {
					JilBuilder.MethodInfo mi = new JilBuilder.MethodInfo(
							new ArrayList(), new Type.Function(T_BYTE));
					return implicitCast(new Expr.Invoke(e, "byteValue",
							new ArrayList<Expr>(), new ArrayList(),
							T_BYTE, mi), t);
				} else if (c.equals("Character")) {
					JilBuilder.MethodInfo mi = new JilBuilder.MethodInfo(
							new ArrayList(), new Type.Function(T_CHAR));					
					return implicitCast(new Expr.Invoke(e, "charValue",
							new ArrayList<Expr>(), new ArrayList(),
							T_CHAR, mi), t);
				} else if (c.equals("Short")) {
					JilBuilder.MethodInfo mi = new JilBuilder.MethodInfo(
							new ArrayList(), new Type.Function(T_SHORT));					
					return implicitCast(new Expr.Invoke(e, "shortValue",
							new ArrayList<Expr>(), new ArrayList(),
							T_SHORT, mi), t);
				} else if (c.equals("Integer")) {
					JilBuilder.MethodInfo mi = new JilBuilder.MethodInfo(
							new ArrayList(), new Type.Function(T_INT));					
					return implicitCast(new Expr.Invoke(e, "intValue",
							new ArrayList<Expr>(), new ArrayList(),
							T_INT, mi), t);
				} else if (c.equals("Long")) {
					JilBuilder.MethodInfo mi = new JilBuilder.MethodInfo(
							new ArrayList(), new Type.Function(T_LONG));					
					return implicitCast(new Expr.Invoke(e, "longValue",
							new ArrayList<Expr>(), new ArrayList(),
							T_LONG, mi), t);
				} else if (c.equals("Float")) {
					JilBuilder.MethodInfo mi = new JilBuilder.MethodInfo(
							new ArrayList(), new Type.Function(T_FLOAT));					
					return implicitCast(new Expr.Invoke(e, "floatValue",
							new ArrayList<Expr>(), new ArrayList(),
							T_FLOAT, mi), t);
				} else if (c.equals("Double")) {
					JilBuilder.MethodInfo mi = new JilBuilder.MethodInfo(
							new ArrayList(), new Type.Function(T_DOUBLE));					
					return implicitCast(new Expr.Invoke(e, "doubleValue",
							new ArrayList<Expr>(), new ArrayList(),
							T_DOUBLE, mi), t);
				} else if (c.equals("Boolean")) {
					JilBuilder.MethodInfo mi = new JilBuilder.MethodInfo(
							new ArrayList(), new Type.Function(T_BOOL));					
					return implicitCast(new Expr.Invoke(e, "booleanValue",
							new ArrayList<Expr>(), new ArrayList(),
							T_BOOL, mi), t);
				} else {
					syntax_error("found type " + e_t + ", required " + t,e);
				}
			}
		} else if(e_t instanceof Type.Primitive && t instanceof Type.Clazz) {
			ArrayList<Expr> params = new ArrayList<Expr>();
			params.add(e);
			JilBuilder.MethodInfo mi = new JilBuilder.MethodInfo(
					new ArrayList(), new Type.Function(new Type.Void(), e_t));
			return new Expr.New(fromJilType(boxedType((Type.Primitive) e_t)),
					null, params, new ArrayList<Decl>(),
					boxedType((Type.Primitive) e_t), mi, e
							.attribute(SourceLocation.class));			
		} 
		
		return e;
	}
	
	/**
     * An unknown constant is a constant expression without any explicit type
     * labels. For example:
     * 
     * <pre>
     * short x = 1 + 1;
     * </pre>
     * 
     * Here, "1 + 1" is an unknown constant expression, since the type of it
     * must be inferred from the assignment. That is because, if the type of
     * "1+1" were resolved to be int, then the above could not compile!
     * 
     * @param e
     * @return
     */
	protected boolean isUnknownConstant(Expr e) {		
		if(e instanceof Value.Int) {			
			return true;
		} else if(e instanceof Expr.BinOp) {
			Expr.BinOp bop = (Expr.BinOp) e;
			
			switch(bop.op()) {
				case Expr.BinOp.ADD:
				case Expr.BinOp.SUB:
				case Expr.BinOp.MUL:
				case Expr.BinOp.DIV:
				case Expr.BinOp.MOD:
					return isUnknownConstant(bop.lhs()) && isUnknownConstant(bop.rhs());
					
				case Expr.BinOp.SHL:
				case Expr.BinOp.SHR:
				case Expr.BinOp.USHR:								
					return isUnknownConstant(bop.lhs()) && isUnknownConstant(bop.rhs()); 								
			}
		} else if(e instanceof Expr.UnOp) {
			Expr.UnOp uop = (Expr.UnOp) e;
			switch(uop.op()) {
				case Expr.UnOp.NEG:
				case Expr.UnOp.INV:
					return isUnknownConstant(uop.expr());
			}
		}
		
		return false;
	}
	
	/**
     * An unknown constant is a constant expression without any explicit type
     * labels. For example:
     * 
     * <pre>
     * short x = 1 + 1;
     * </pre>
     * 
     * Here, "1 + 1" is an unknown constant expression, since the type of it
     * must be inferred from the assignment. That is because, if the type of
     * "1+1" were resolved to be int, then the above could not compile!
     * 
     * @param e
     * @return
     */
	protected int evaluateUnknownConstant(Expr e) {
		if(e instanceof Value.Int) {
			return ((Value.Int)e).value();
		} else if(e instanceof Expr.BinOp) {
			Expr.BinOp bop = (Expr.BinOp) e;
			
			int lhs = evaluateUnknownConstant(bop.lhs());
			int rhs = evaluateUnknownConstant(bop.rhs());
			
			switch(bop.op()) {
				case Expr.BinOp.ADD:
					return lhs + rhs;					
				case Expr.BinOp.SUB:
					return lhs - rhs;					
				case Expr.BinOp.MUL:
					return lhs * rhs;					
				case Expr.BinOp.DIV:
					return lhs / rhs;					
				case Expr.BinOp.MOD:
					return lhs % rhs;					
				case Expr.BinOp.SHL:
					return lhs << rhs;
				case Expr.BinOp.SHR:
					return lhs >> rhs;
				case Expr.BinOp.USHR:
					return lhs >>> rhs;					 							
			}
		} else if(e instanceof Expr.UnOp) {
			Expr.UnOp uop = (Expr.UnOp) e;
			int lhs = evaluateUnknownConstant(uop.expr());
			
			switch(uop.op()) {
				case Expr.UnOp.NEG:
					return -lhs;
				case Expr.UnOp.INV:
					return ~lhs;
			}
		}
		
		syntax_error("cannot evaluate a known expression!",e);
		return 0; // unreachable
	}
	
	/**
     * This method accepts an unknown constant expression, and a required type
     * and creates the appropriate value object.
     * 
     * @param c
     * @param t
     */
	protected Expr unknownConstantInference(Expr e, Type lhs_t, SourceLocation loc) {
		int val = evaluateUnknownConstant(e);
		// first do primitive types
		if(lhs_t instanceof Type.Byte && val >= -128 && val <= 127) {
			return new Value.Byte((byte)val, T_BYTE, loc);				
		} else if(lhs_t instanceof Type.Char && val >= 0 && val <= 65535) {
			return new Value.Char((char)val, T_CHAR, loc);				
		} else if(lhs_t instanceof Type.Short && val >= -32768 && val <= 32768) {
			return new Value.Short((short)val, T_SHORT, loc);				
		} else if(isBoxedType(lhs_t)) {
			Type.Clazz ref = (Type.Clazz) lhs_t;			
			String s = ref.components().get(0).first();				
			if(s.equals("Byte") && val >= -128 && val <= 127) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(new Value.Byte((byte)val));
				JilBuilder.MethodInfo mi = new JilBuilder.MethodInfo(
						new ArrayList(), new Type.Function(new Type.Void(),
								T_BYTE));
				return new Expr.New(fromJilType(lhs_t),null,params,new ArrayList<Decl>(), lhs_t, mi, loc);				
			} else if(s.equals("Character") && val >= 0 && val <= 65535) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(new Value.Char((char)val));
				JilBuilder.MethodInfo mi = new JilBuilder.MethodInfo(
						new ArrayList(), new Type.Function(new Type.Void(),
								T_CHAR));
				return new Expr.New(fromJilType(lhs_t),null,params,new ArrayList<Decl>(), lhs_t, mi, loc);				
			} else if(s.equals("Short") && val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(new Value.Short((short)val));
				JilBuilder.MethodInfo mi = new JilBuilder.MethodInfo(
						new ArrayList(), new Type.Function(new Type.Void(),
								T_SHORT));
				return new Expr.New(fromJilType(lhs_t),null,params,new ArrayList<Decl>(), lhs_t, mi, loc);				
			} else if(s.equals("Integer") && val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(new Value.Int(val));
				JilBuilder.MethodInfo mi = new JilBuilder.MethodInfo(
						new ArrayList(), new Type.Function(new Type.Void(),
								T_INT));
				return new Expr.New(fromJilType(lhs_t),null,params,new ArrayList<Decl>(), lhs_t, mi, loc);				
			} 
		} 
		return new Value.Int(val,T_INT,loc);
	}		
	
	/**
	 * This method simply determines the super class of the given class.
	 * 
	 * @param c
	 * @return
	 */
	protected Type.Clazz getSuperClass(Type.Clazz c) throws ClassNotFoundException {
		Clazz cc = loader.loadClass(c);
		return cc.superClass();
	}
}

