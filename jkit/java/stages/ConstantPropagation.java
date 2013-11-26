package jkit.java.stages;

import static jkit.compiler.SyntaxError.internal_error;
import static jkit.compiler.SyntaxError.syntax_error;
import static jkit.jil.util.Types.JAVA_LANG_OBJECT;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import jkit.compiler.ClassLoader;
import jkit.compiler.Clazz;
import jkit.compiler.SyntacticAttribute;
import jkit.error.FieldNotFoundException;
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
import jkit.jil.tree.Modifier;
import jkit.jil.tree.Type;
import jkit.jil.tree.JilClass;
import jkit.jil.tree.JilField;
import jkit.jil.tree.JilConstant;
import jkit.util.Pair;
import jkit.util.Triple;

/**
 * The purpose of this class is to convert expressions which are known constants
 * into their actual constant value. For example:
 * 
 * <pre>
 * class Test {
 * 	static final int CONSTANT = 12;
 * 	public int f() {
 * 		return Test.CONSTANT;
 * 	}
 * }
 * </pre>
 * 
 * This code would be translated into:
 * 
 * <pre>
 * class Test {
 * 	static final int CONSTANT = 12;
 * 	public int f() {
 * 		return 12;
 * 	}
 * }
 * </pre>
 * 
 * The primary purpose of doing this is to ensure that case statements work on
 * constants, rather than arbitrary values.
 * 
 * @author djp
 * 
 */
public class ConstantPropagation {
	private ClassLoader loader;
	private TypeSystem types;	
	private Stack<Type.Clazz> enclosingClass = new Stack<Type.Clazz>();
	
	public ConstantPropagation(ClassLoader loader, TypeSystem types) {
		this.loader = loader; 
		this.types = types;
	}
	
	public void apply(JavaFile file) {		
		// Now, traverse the declarations
		for(Decl d : file.declarations()) {
			doDeclaration(d,file);
		}		
	}
	
	protected void doDeclaration(Decl d, JavaFile file) {
		try {
			if(d instanceof JavaInterface) {
				doInterface((JavaInterface)d, file);
			} else if(d instanceof JavaClass) {
				doClass((JavaClass)d, file);
			} else if(d instanceof JavaMethod) {
				doMethod((JavaMethod)d, file);
			} else if(d instanceof JavaField) {
				doField((JavaField)d, file);
			} else if (d instanceof Decl.InitialiserBlock) {
				doInitialiserBlock((Decl.InitialiserBlock) d, file);
			} else if (d instanceof Decl.StaticInitialiserBlock) {
				doStaticInitialiserBlock((Decl.StaticInitialiserBlock) d, file);
			} else {
				syntax_error("internal failure (unknown declaration \"" + d
						+ "\" encountered)",d);
			}
		} catch(Exception ex) {
			internal_error(d,ex);
		}
	}
	
	protected void doInterface(JavaInterface d, JavaFile file) {
		doClass(d,file);
	}
	
	protected void doClass(JavaClass c, JavaFile file) {		
		Type.Clazz type = c.attribute(Type.Clazz.class);
		enclosingClass.push(type);
		
		for(Decl d : c.declarations()) {
			doDeclaration(d, file);
		}
		
		enclosingClass.pop();
	}

	protected void doMethod(JavaMethod d, JavaFile file) {		
		doStatement(d.body(), file);
	}

	protected void doField(JavaField d, JavaFile file) {		
		Type.Clazz owner = enclosingClass.lastElement();
		
		try {				
			d.setInitialiser(doExpression(d.initialiser(), file));
								
			if(d.isConstant()) {				
				Clazz c = loader.loadClass(owner);			
				if(!c.field(d.name()).isConstant()) {
					// Ok, in this instance, we have determined that this field
					// is actually a constant. Therefore, we need to propagate
					// this information into the relevant JilClass.
					JilClass jc = (JilClass) c;
					
					List<JilField> fields = jc.fields();
					for(int i=0;i!=fields.size();++i) {
						JilField f = fields.get(i);
						if(f.name().equals(d.name())) {
							// found it.
							JilConstant jilc = new JilConstant(f.name(), f
									.type(), d.constant(), f.modifiers(), f
									.attributes());
							fields.set(i,jilc); // done
							break;
						}
					}
				}
			}			
		} catch(ClassNotFoundException cne) {
			internal_error(d,cne);
		}
	}
	
	protected void doInitialiserBlock(Decl.InitialiserBlock d,
			JavaFile file) {

		for (Stmt s : d.statements()) {
			doStatement(s, file);
		}
	}
	
	protected void doStaticInitialiserBlock(Decl.StaticInitialiserBlock d,
			JavaFile file) {
		for (Stmt s : d.statements()) {
			doStatement(s, file);
		}
	}
	
	protected void doStatement(Stmt e, JavaFile file) {
		try {
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
			} else if(e instanceof Decl.JavaClass) {
				doClass((Decl.JavaClass)e, file);
			} else if(e instanceof Stmt.PrePostIncDec) {
				doExpression((Stmt.PrePostIncDec)e, file);
			} else if(e != null) {
				syntax_error("Invalid statement encountered: "
						+ e.getClass(),e);
			}	
		} catch(Exception ex) {
			internal_error(e,ex);
		}
	}
	
	protected void doBlock(Stmt.Block block, JavaFile file) {
		if(block != null) {
			// now process every statement in this block.
			for(Stmt s : block.statements()) {
				doStatement(s, file);
			}
		}
	}
	
	protected void doCatchBlock(Stmt.CatchBlock block, JavaFile file) {
		if(block != null) {
			// now process every statement in this block.
			for(Stmt s : block.statements()) {
				doStatement(s, file);
			}
		}
	}
	
	protected void doSynchronisedBlock(Stmt.SynchronisedBlock block,
			JavaFile file) {
		doBlock(block, file);
		block.setExpr(doExpression(block.expr(), file));
	}
	
	protected void doTryCatchBlock(Stmt.TryCatchBlock block, JavaFile file) {
		doBlock(block, file);		
		doBlock(block.finaly(), file);		
		
		for(Stmt.CatchBlock cb : block.handlers()) {
			doCatchBlock(cb, file);
		}
	}
	
	protected void doVarDef(Stmt.VarDef def, JavaFile file) {
		List<Triple<String, Integer, Expr>> defs = def.definitions();				
		
		for(int i=0;i!=defs.size();++i) {
			Triple<String, Integer, Expr> d = defs.get(i);
			Expr e = doExpression(d.third(), file);
			defs.set(i, new Triple(d.first(),d.second(),e));			
		}		
	}
	
	protected Expr doAssignment(Stmt.Assignment def, JavaFile file) {
		
		// At the moment, I don't perform constant propagation on the lhs of an
        // assignment. This is absolutely less than ideal. However, it's
        // difficult to deal with it, so for now I ignore it.
		
		// def.setLhs(doExpression(def.lhs(), file));	
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
		doStatement(stmt.initialiser(), file);
		stmt.setCondition(doExpression(stmt.condition(), file));
		doStatement(stmt.increment(), file);
		doStatement(stmt.body(), file);
	}
	
	protected void doForEach(Stmt.ForEach stmt, JavaFile file) {
		stmt.setSource(doExpression(stmt.source(), file));
		doStatement(stmt.body(), file);		
	}
	
	protected void doSwitch(Stmt.Switch sw, JavaFile file) {
		
		sw.setCondition(doExpression(sw.condition(), file));
		for(Case c : sw.cases()) {									
			c.setCondition(doExpression(c.condition(), file));									
			for(Stmt s : c.statements()) {
				doStatement(s, file);
			}
		}		
		// should check that case conditions are final constants here.
	}
	
	protected Expr doExpression(Expr e, JavaFile file) {	
		try {
			if(e instanceof Value.Bool) {
				return doBoolVal((Value.Bool)e, file);
			} else if(e instanceof Value.Byte) {
				return doByteVal((Value.Byte)e, file);
			} else if(e instanceof Value.Char) {
				return doCharVal((Value.Char)e, file);
			} else if(e instanceof Value.Short) {
				return doShortVal((Value.Short)e, file);
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
			} else if(e instanceof Expr.LocalVariable) {
				return doVariable((Expr.LocalVariable)e, file);
			} else if(e instanceof Expr.NonLocalVariable) {
				return doNonLocalVariable((Expr.NonLocalVariable)e, file);
			} else if(e instanceof Expr.ClassVariable) {
				return doClassVariable((Expr.ClassVariable)e, file);
			} else if(e instanceof Expr.UnOp) {
				return doUnOp((Expr.UnOp)e, file);
			} else if(e instanceof Expr.BinOp) {
				return doBinOp((Expr.BinOp)e, file);
			} else if(e instanceof Expr.TernOp) {
				return doTernOp((Expr.TernOp)e, file);
			} else if(e instanceof Expr.Cast) {
				return doCast((Expr.Cast)e, file);
			} else if(e instanceof Expr.Convert) {
				return doConvert((Expr.Convert)e, file);
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
			}
		} catch(Exception ex) {
			internal_error(e,ex);
		}
		if(e != null) {		
			syntax_error("Invalid expression encountered: "
					+ e.getClass(),e);			
		}
		
		return null;
	}
	
	protected Expr doDeref(Expr.Deref e, JavaFile file) throws ClassNotFoundException, FieldNotFoundException {
		Expr target = doExpression(e.target(), file);				
		e.setTarget(target);
		
		if(target instanceof Expr.ClassVariable && !e.name().equals("this")) {						
			Type.Clazz owner = target.attribute(Type.Clazz.class);
									
			// static field access, which could be a constant
			Triple<Clazz,Clazz.Field,Type> r = types.resolveField(owner, e.name(), loader);			
			if(r.second().isConstant()) {								
				return buildConstant(r.second().constant(),e);												
			}
		}
		
		return e;
	}
	
	protected Expr doArrayIndex(Expr.ArrayIndex e, JavaFile file) {
		e.setTarget(doExpression(e.target(), file));
		e.setIndex(doExpression(e.index(), file));
		
		return e;
	}
	
	protected Expr doNew(Expr.New e, JavaFile file) {
		e.setContext(doExpression(e.context(),file));
		
		// Second, recurse through any parameters supplied ...
		List<Expr> parameters = e.parameters();
		for(int i=0;i!=parameters.size();++i) {
			Expr p = parameters.get(i);
			parameters.set(i,doExpression(p, file));
		}						
		
		return e;
	}
	
	protected Expr doInvoke(Expr.Invoke e, JavaFile file) throws ClassNotFoundException {				
		Expr target = doExpression(e.target(), file);
					
		e.setTarget(target);		
		
		List<Expr> parameters = e.parameters();
		for(int i=0;i!=parameters.size();++i) {
			Expr p = parameters.get(i);
			parameters.set(i, doExpression(p, file));
		}				
		
		return e;
	}
	
	protected Expr doInstanceOf(Expr.InstanceOf e, JavaFile file) {
		e.setLhs(doExpression(e.lhs(),file));
		return e;
	}
	
	protected Expr doVariable(Expr.LocalVariable v, JavaFile file) { 
		return v;
	}
	
	protected Expr doNonLocalVariable(Expr.NonLocalVariable v, JavaFile file) {
		return v;
	}
	
	protected Expr doClassVariable(Expr.ClassVariable v, JavaFile file) {
		return v;
	}
	
	protected Expr doCast(Expr.Cast e, JavaFile file) {
		e.setExpr(doExpression(e.expr(),file));
		return e;
	}
	
	protected Expr doConvert(Expr.Convert e, JavaFile file) {
		Expr expr = doExpression(e.expr(),file);		
		
		if(expr instanceof Value.Number) {
			Value.Number v = (Value.Number) expr;
			int n = v.intValue();
			Type e_type = e.attribute(Type.class);
			if(e_type instanceof Type.Byte && Byte.MIN_VALUE <= n && n < Byte.MAX_VALUE) {
				return new Value.Byte((byte)n,e.attributes());
			} else if(e_type instanceof Type.Char && Character.MIN_VALUE <= n && n < Character.MAX_VALUE) {
				return new Value.Char((char)n,e.attributes());
			} else if(e_type instanceof Type.Short && Short.MIN_VALUE <= n && n < Short.MAX_VALUE) {
				return new Value.Short((short)n,e.attributes());
			}
			// not sure if more rewrites are needed here.
		}
		
		e.setExpr(expr);
		return e;
	}
	
	protected Expr doBoolVal(Value.Bool e, JavaFile file) {
		return e;
	}
	
	protected Expr doByteVal(Value.Byte e, JavaFile file) {
		return e;
	}
	
	protected Expr doCharVal(Value.Char e, JavaFile file) {
		return e;
	}
	
	protected Expr doShortVal(Value.Short e, JavaFile file) {
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
		for(int i=0;i!=e.values().size();++i) {
			Expr v = e.values().get(i);			
			e.values().set(i,doExpression(v,file));			
		}
		return e;
	}
	
	protected Expr doArrayVal(Value.Array e, JavaFile file) {
		for(int i=0;i!=e.values().size();++i) {
			Expr v = e.values().get(i);			
			e.values().set(i,doExpression(v,file));			
		}
		return e;
	}
	
	protected Expr doClassVal(Value.Class e, JavaFile file) {
		return e;
	}
		
	protected Expr doUnOp(Expr.UnOp e, JavaFile file) {		
		
		if(e.op() == Expr.UnOp.PREDEC 
			|| e.op() == Expr.UnOp.POSTDEC
			|| e.op() == Expr.UnOp.POSTINC
			|| e.op() == Expr.UnOp.PREINC) {
			return e;
		}
		
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
	
	protected Value buildConstant(Object constant, Expr src) {
		List<SyntacticAttribute> attributes = src.attributes();
		if(constant instanceof Boolean) {			
			return new Value.Bool((Boolean) constant,attributes);
		} else if(constant instanceof Character) {			
			return new Value.Char((Character) constant,attributes);
		} else if(constant instanceof Short) {			
			return new Value.Short((Short) constant,attributes);
		} else if(constant instanceof Integer) {
			return new Value.Int((Integer) constant,attributes);
		} else if(constant instanceof Long) {			
			return new Value.Long((Long) constant,attributes);
		} else if(constant instanceof Float) {			
			return new Value.Float((Float) constant,attributes);
		} else if(constant instanceof Double) {			
			return new Value.Double((Double) constant,attributes);
		} else if(constant instanceof String) {			
			return new Value.String((String) constant,attributes);
		} 
		syntax_error("unknown constant encountered: " + constant,src);
		return null; // dead
	}
}
