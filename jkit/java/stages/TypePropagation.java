package jkit.java.stages;

import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.compiler.SyntaxError;
import jkit.java.*;
import jkit.java.Decl.*;
import jkit.java.Stmt.Case;
import jkit.util.*;
import jkit.jil.SyntacticElement;
import jkit.jil.Type;
import jkit.jil.Modifier;
import jkit.jil.SourceLocation;

/**
 * This Class does two main things:
 * 
 * 1) it goes through all of the types that have been declared in the source
 * file, and resolves them to fully qualified types. For example, consider this
 * code:
 * 
 * <pre>
 * import java.util.*;
 * 
 * public class Test extends Vector {
 * 	public static void main(String[] args) {
 *      ... 
 *     }
 * }
 * </pre>
 * 
 * After type resolution, we will have resolved the following types:
 * 
 * <pre>
 *   Vector -&gt; java.util.Vector
 *   String -&gt; java.lang.String
 * </pre>
 * 
 * Thus, in principle, we could use this information to eliminate any import
 * statements (although type resolution by itself does not do this).
 * 
 * 2) all of the type information found on expressions is propagated as type
 * attributes appropriately. The stage also checks that types are used
 * appropriately in expressions and statements and, where not, emits error
 * messages accordingly.
 * 
 * @author djp
 * 
 */
public class TypePropagation {
	private ClassLoader loader;
	
	public TypePropagation(ClassLoader loader) {
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
			doInterface((Interface)d, imports);
		} else if(d instanceof Clazz) {
			doClass((Clazz)d, imports);
		} else if(d instanceof Method) {
			doMethod((Method)d, imports);
		} else if(d instanceof Field) {
			doField((Field)d, imports);
		}
	}
	
	protected void doInterface(Interface d, List<String> imports) {
		
	}
	
	protected void doClass(Clazz c, List<String> imports) {
		if(c.superclass() != null) {
			c.superclass().attributes().add(resolve(c.superclass(), imports));
		}
		
		for(jkit.java.Type.Variable v : c.typeParameters()) {
			v.attributes().add(resolve(v, imports));
		}
		
		for(jkit.java.Type.Clazz i : c.interfaces()) {
			i.attributes().add(resolve(i, imports));
		}
		
		for(Decl d : c.declarations()) {
			doDeclaration(d, imports);
		}
	}

	protected void doMethod(Method d, List<String> imports) {
		// First, we need to construct a typing environment for local variables.
		HashMap<String,Type> environment = new HashMap<String,Type>();
		
		for(Triple<String,List<Modifier>,jkit.java.Type> p : d.parameters()) {
			environment.put(p.first(), (jkit.jil.Type) p.third().attribute(Type.class));
		}
						
		doStatement(d.body(),environment, imports);
	}

	protected void doField(Field d, List<String> imports) {
		doExpression(d.initialiser(), new HashMap<String,Type>(), imports);
	}
	
	protected void doStatement(Stmt e, HashMap<String,Type> environment, List<String> imports) {
		if(e instanceof Stmt.SynchronisedBlock) {
			doSynchronisedBlock((Stmt.SynchronisedBlock)e, environment, imports);
		} else if(e instanceof Stmt.TryCatchBlock) {
			doTryCatchBlock((Stmt.TryCatchBlock)e, environment, imports);
		} else if(e instanceof Stmt.Block) {
			doBlock((Stmt.Block)e, environment, imports);
		} else if(e instanceof Stmt.VarDef) {
			doVarDef((Stmt.VarDef) e, environment, imports);
		} else if(e instanceof Stmt.Assignment) {
			doAssignment((Stmt.Assignment) e, environment, imports);
		} else if(e instanceof Stmt.Return) {
			doReturn((Stmt.Return) e, environment, imports);
		} else if(e instanceof Stmt.Throw) {
			doThrow((Stmt.Throw) e, environment, imports);
		} else if(e instanceof Stmt.Assert) {
			doAssert((Stmt.Assert) e, environment, imports);
		} else if(e instanceof Stmt.Break) {
			doBreak((Stmt.Break) e, environment, imports);
		} else if(e instanceof Stmt.Continue) {
			doContinue((Stmt.Continue) e, environment, imports);
		} else if(e instanceof Stmt.Label) {
			doLabel((Stmt.Label) e, environment, imports);
		} else if(e instanceof Stmt.If) {
			doIf((Stmt.If) e, environment, imports);
		} else if(e instanceof Stmt.For) {
			doFor((Stmt.For) e, environment, imports);
		} else if(e instanceof Stmt.ForEach) {
			doForEach((Stmt.ForEach) e, environment, imports);
		} else if(e instanceof Stmt.While) {
			doWhile((Stmt.While) e, environment, imports);
		} else if(e instanceof Stmt.DoWhile) {
			doDoWhile((Stmt.DoWhile) e, environment, imports);
		} else if(e instanceof Stmt.Switch) {
			doSwitch((Stmt.Switch) e, environment, imports);
		} else if(e instanceof Expr.Invoke) {
			doInvoke((Expr.Invoke) e, environment, imports);
		} else if(e instanceof Expr.New) {
			doNew((Expr.New) e, environment, imports);
		} else if(e instanceof Decl.Clazz) {
			doClass((Decl.Clazz)e, imports);
		} else if(e != null) {
			throw new RuntimeException("Invalid statement encountered: "
					+ e.getClass());
		}		
	}
	
	protected void doBlock(Stmt.Block block, HashMap<String,Type> environment, List<String> imports) {
		if(block != null) {
			// The following clone is required, so that any additions to the
			// environment via local variable defintions in this block are
			// not preserved.
			HashMap<String,Type> newEnv = (HashMap<String,Type>) environment.clone();

			// now process every statement in this block.
			for(Stmt s : block.statements()) {
				doStatement(s,newEnv, imports);
			}
		}
	}
	
	protected void doSynchronisedBlock(Stmt.SynchronisedBlock block, HashMap<String,Type> environment, List<String> imports) {
		doBlock(block,environment, imports);
		doExpression(block.expr(),environment, imports);
	}
	
	protected void doTryCatchBlock(Stmt.TryCatchBlock block, HashMap<String,Type> environment, List<String> imports) {
		doBlock(block,environment, imports);		
		doBlock(block.finaly(),environment, imports);		
		
		for(Stmt.CatchBlock cb : block.handlers()) {
			doBlock(cb, environment, imports);
		}
	}
	
	protected void doVarDef(Stmt.VarDef def, HashMap<String,Type> environment, List<String> imports) {
		Type t = (Type) def.type().attribute(Type.class);
		
		List<Triple<String, Integer, Expr>> defs = def.definitions();
		for(int i=0;i!=defs.size();++i) {
			Triple<String, Integer, Expr> d = defs.get(i);
			
			Type nt = t;						
			doExpression(d.third(),environment, imports);						
			
			for(int j=0;j!=d.second();++j) {
				nt = new Type.Array(nt);
			}
			
			environment.put(d.first(),nt);
			
			// perform type inference (if necesssary)
			if(d.third() != null && isUnknownConstant(d.third())) {
				Expr c = unknownConstantInference(d.third(), nt,
						(SourceLocation) d.third
								.attribute(SourceLocation.class));
				
				defs.set(i,new Triple(d.first(),d.second(),c));
			}
		}
	}
	
	protected void doAssignment(Stmt.Assignment def, HashMap<String,Type> environment, List<String> imports) {
		doExpression(def.lhs(),environment, imports);	
		doExpression(def.rhs(),environment, imports);			

		Type lhs_t = (Type) def.lhs().attribute(Type.class);
		
		// perform type inference (if necesssary)
		if(isUnknownConstant(def.rhs())) {
			Expr c = unknownConstantInference(def.rhs(), lhs_t,
					(SourceLocation) def.rhs()
							.attribute(SourceLocation.class));
			
			def.setRhs(c);			
		}		
	}
	
	protected void doReturn(Stmt.Return ret, HashMap<String,Type> environment, List<String> imports) {
		doExpression(ret.expr(), environment, imports);
	}
	
	protected void doThrow(Stmt.Throw ret, HashMap<String,Type> environment, List<String> imports) {
		doExpression(ret.expr(), environment, imports);

		// should check whether enclosing method declares checked exceptions
        // appropriately.
	}
	
	protected void doAssert(Stmt.Assert ret, HashMap<String,Type> environment, List<String> imports) {
		doExpression(ret.expr(), environment, imports);
	}
	
	protected void doBreak(Stmt.Break brk, HashMap<String,Type> environment, List<String> imports) {
		// nothing	
	}
	
	protected void doContinue(Stmt.Continue brk, HashMap<String,Type> environment, List<String> imports) {
		// nothing
	}
	
	protected void doLabel(Stmt.Label lab, HashMap<String,Type> environment, List<String> imports) {						
		doStatement(lab.statement(), environment, imports);
	}
	
	protected void doIf(Stmt.If stmt, HashMap<String,Type> environment, List<String> imports) {
		doExpression(stmt.condition(),environment, imports);
		doStatement(stmt.trueStatement(),environment, imports);
		doStatement(stmt.falseStatement(),environment, imports);
	}
	
	protected void doWhile(Stmt.While stmt, HashMap<String,Type> environment, List<String> imports) {
		doExpression(stmt.condition(),environment, imports);
		doStatement(stmt.body(),environment, imports);		
	}
	
	protected void doDoWhile(Stmt.DoWhile stmt, HashMap<String,Type> environment, List<String> imports) {
		doExpression(stmt.condition(),environment, imports);
		doStatement(stmt.body(),environment, imports);
	}
	
	protected void doFor(Stmt.For stmt, HashMap<String,Type> environment, List<String> imports) {
		doStatement(stmt.initialiser(),environment, imports);
		doExpression(stmt.condition(),environment, imports);
		doStatement(stmt.increment(),environment, imports);
		doStatement(stmt.body(),environment, imports);	
	}
	
	protected void doForEach(Stmt.ForEach stmt, HashMap<String,Type> environment, List<String> imports) {
		doExpression(stmt.source(),environment, imports);
		doStatement(stmt.body(),environment, imports);
	}
	
	protected void doSwitch(Stmt.Switch sw, HashMap<String,Type> environment, List<String> imports) {
		doExpression(sw.condition(), environment, imports);
		for(Case c : sw.cases()) {
			doExpression(c.condition(), environment, imports);
			for(Stmt s : c.statements()) {
				doStatement(s, environment, imports);
			}
		}
		
		// should check that case conditions are final constants here.
	}
	
	protected void doExpression(Expr e, HashMap<String,Type> environment, List<String> imports) {	
		if(e instanceof Value.Bool) {
			doBoolVal((Value.Bool)e,environment, imports);
		} else if(e instanceof Value.Char) {
			doCharVal((Value.Char)e,environment, imports);
		} else if(e instanceof Value.Int) {
			doIntVal((Value.Int)e,environment, imports);
		} else if(e instanceof Value.Long) {
			doLongVal((Value.Long)e,environment, imports);
		} else if(e instanceof Value.Float) {
			doFloatVal((Value.Float)e,environment, imports);
		} else if(e instanceof Value.Double) {
			doDoubleVal((Value.Double)e,environment, imports);
		} else if(e instanceof Value.String) {
			doStringVal((Value.String)e,environment, imports);
		} else if(e instanceof Value.Null) {
			doNullVal((Value.Null)e,environment, imports);
		} else if(e instanceof Value.TypedArray) {
			doTypedArrayVal((Value.TypedArray)e,environment, imports);
		} else if(e instanceof Value.Array) {
			doArrayVal((Value.Array)e,environment, imports);
		} else if(e instanceof Value.Class) {
			doClassVal((Value.Class) e,environment, imports);
		} else if(e instanceof Expr.Variable) {
			doVariable((Expr.Variable)e,environment, imports);
		} else if(e instanceof Expr.UnOp) {
			doUnOp((Expr.UnOp)e,environment, imports);
		} else if(e instanceof Expr.BinOp) {
			doBinOp((Expr.BinOp)e,environment, imports);
		} else if(e instanceof Expr.TernOp) {
			doTernOp((Expr.TernOp)e,environment, imports);
		} else if(e instanceof Expr.Cast) {
			doCast((Expr.Cast)e,environment, imports);
		} else if(e instanceof Expr.InstanceOf) {
			doInstanceOf((Expr.InstanceOf)e,environment, imports);
		} else if(e instanceof Expr.Invoke) {
			doInvoke((Expr.Invoke) e,environment, imports);
		} else if(e instanceof Expr.New) {
			doNew((Expr.New) e,environment, imports);
		} else if(e instanceof Expr.ArrayIndex) {
			doArrayIndex((Expr.ArrayIndex) e,environment, imports);
		} else if(e instanceof Expr.Deref) {
			doDeref((Expr.Deref) e,environment, imports);
		} else if(e instanceof Stmt.Assignment) {
			// force brackets			
			doAssignment((Stmt.Assignment) e,environment, imports);			
		} else if(e != null) {
			throw new RuntimeException("Invalid expression encountered: "
					+ e.getClass());
		}
	}
	
	protected void doDeref(Expr.Deref e, HashMap<String,Type> environment, List<String> imports) {
		doExpression(e.target(), environment, imports);		
		// need to perform field lookup here!
	}
	
	protected void doArrayIndex(Expr.ArrayIndex e, HashMap<String,Type> environment, List<String> imports) {
		doExpression(e.target(), environment, imports);
		doExpression(e.index(), environment, imports);
		
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
	
	protected void doNew(Expr.New e, HashMap<String,Type> environment, List<String> imports) {
		
	}
	
	protected void doInvoke(Expr.Invoke e, HashMap<String,Type> environment, List<String> imports) {
		for(Expr p : e.parameters()) {
			doExpression(p, environment, imports);
		}
	}
	
	protected void doInstanceOf(Expr.InstanceOf e, HashMap<String,Type> environment, List<String> imports) {		
			
	}
	
	protected void doCast(Expr.Cast e, HashMap<String,Type> environment, List<String> imports) {
	
	}
	
	protected void doBoolVal(Value.Bool e, HashMap<String,Type> environment, List<String> imports) {
		e.attributes().add(new Type.Bool());
	}
	
	protected void doCharVal(Value.Char e, HashMap<String,Type> environment, List<String> imports) {
		e.attributes().add(new Type.Char());
	}
	
	protected void doIntVal(Value.Int e, HashMap<String,Type> environment, List<String> imports) {
		e.attributes().add(new Type.Int());
	}
	
	protected void doLongVal(Value.Long e, HashMap<String,Type> environment, List<String> imports) {		
		e.attributes().add(new Type.Long());
	}
	
	protected void doFloatVal(Value.Float e, HashMap<String,Type> environment, List<String> imports) {		
		e.attributes().add(new Type.Float());
	}
	
	protected void doDoubleVal(Value.Double e, HashMap<String,Type> environment, List<String> imports) {		
		e.attributes().add(new Type.Double());
	}
	
	protected void doStringVal(Value.String e, HashMap<String,Type> environment, List<String> imports) {		
		e.attributes().add(new Type.Clazz("java.lang","String"));
	}
	
	protected void doNullVal(Value.Null e, HashMap<String,Type> environment, List<String> imports) {		
		e.attributes().add(new Type.Null());
	}
	
	protected void doTypedArrayVal(Value.TypedArray e, HashMap<String,Type> environment, List<String> imports) {		
		e.attributes().add((Type) e.type().attribute(Type.class));
	}
	
	protected void doArrayVal(Value.Array e, HashMap<String,Type> environment, List<String> imports) {		
		// not sure what to do here.
	}
	
	protected void doClassVal(Value.Class e, HashMap<String,Type> environment, List<String> imports) {
		
	}
	
	protected void doVariable(Expr.Variable e, HashMap<String,Type> environment, List<String> imports) {			
		Type t = environment.get(e.value());
		if(t == null) {			
			syntax_error("Cannot find symbol - variable \"" + e.value() + "\"",
					e);
		} else {
			e.attributes().add(t);
		}
	}

	protected void doUnOp(Expr.UnOp e, HashMap<String,Type> environment, List<String> imports) {		
		
	}
		
	protected void doBinOp(Expr.BinOp e, HashMap<String,Type> environment, List<String> imports) {				
		doExpression(e.lhs(),environment, imports);
		doExpression(e.rhs(),environment, imports);
		
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
				if ((lhs_t instanceof Type.Primitive || isWrapper(lhs_t))
						&& (rhs_t instanceof Type.Primitive || isWrapper(rhs_t))) {
					Type rt = binaryNumericPromotion(lhs_t, rhs_t);
					e.setLhs(implicitCast(e.lhs(), rt));
					e.setRhs(implicitCast(e.rhs(), rt));
					e.attributes().add(new Type.Bool());
				} else if (e.op() == Expr.BinOp.EQ || e.op() == Expr.BinOp.NEQ) {
					e.attributes().add(new Type.Bool());
				} else {
					// some kind of error here.
				}
				break;
			}
			case Expr.BinOp.ADD:
			case Expr.BinOp.SUB:
			case Expr.BinOp.MUL:
			case Expr.BinOp.DIV:
			case Expr.BinOp.MOD:
			{						
				if ((lhs_t instanceof Type.Primitive || isWrapper(lhs_t))
						&& (rhs_t instanceof Type.Primitive || isWrapper(rhs_t))) {
					Type rt = binaryNumericPromotion(lhs_t, rhs_t);
					e.setLhs(implicitCast(e.lhs(), rt));
					e.setRhs(implicitCast(e.rhs(), rt));
					e.attributes().add(rt);
				} else if (e.op() == Expr.BinOp.ADD
						&& (isString(lhs_t) || isString(rhs_t))) {
					e.attributes().add(new Type.Clazz("java.lang", "String"));
					e.setOp(Expr.BinOp.CONCAT);
				}
				break;
			}
			case Expr.BinOp.SHL:
			case Expr.BinOp.SHR:
			case Expr.BinOp.USHR:
			{					
				if ((lhs_t instanceof Type.Primitive || isWrapper(lhs_t))
						&& (rhs_t instanceof Type.Primitive || isWrapper(rhs_t))) {
					Type rt_left = unaryNumericPromotion(lhs_t);
					e.setLhs(implicitCast(e.lhs(), rt_left));
					e.setRhs(implicitCast(e.rhs(), new Type.Int()));
					e.attributes().add(rt_left);
				}
				break;
			}
			case Expr.BinOp.LAND:
			case Expr.BinOp.LOR:
			{
				Type rt = binaryNumericPromotion(lhs_t,rhs_t);
				e.setLhs(implicitCast(e.lhs(),rt));
				e.setRhs(implicitCast(e.rhs(),rt));
				e.attributes().add(new Type.Bool());				
			}
			case Expr.BinOp.AND:
			case Expr.BinOp.OR:
			case Expr.BinOp.XOR:
			{								
				if ((lhs_t instanceof Type.Primitive || isWrapper(lhs_t))
						&& (rhs_t instanceof Type.Primitive || isWrapper(rhs_t))) {
					Type rt = binaryNumericPromotion(lhs_t, rhs_t);
					e.setLhs(implicitCast(e.lhs(),rt));
					e.setRhs(implicitCast(e.rhs(),rt));
					e.attributes().add(rt);						
				} 
				break;
			}					
		}
	}
	
	protected void doTernOp(Expr.TernOp e, HashMap<String,Type> environment, List<String> imports) {		
		
	}
	
	
	/**
	 * Determine whether or not the given type is a wrapper for a primitive
	 * type.  E.g. java.lang.Integer is a wrapper for int.
	 * 
	 * @param t
	 * @return
	 */
	protected static boolean isWrapper(Type t) {
		if(!(t instanceof Type.Clazz)) {
			return false;
		}
		Type.Clazz ref = (Type.Clazz) t;
		if(ref.pkg().equals("java.lang") && ref.components().size() == 1) {
			String s = ref.components().get(0).first();
			if(s.equals("Byte") || s.equals("Character") || s.equals("Short") ||
				s.equals("Integer") || s.equals("Long")
					|| s.equals("Float") || s.equals("Double")
					|| s.equals("Boolean")) {
				return true;
			}
		}
		return false;
	}
		
	/**
     * Given the types of the left-hand and right-hand sides for a binary
     * operator, determine the appropriate type for that operator. This method
     * follows the Java Language Specification 5.6.1:
     * 
     * @param lhs
     * @param rhs
     * @return
     */
	public Type.Primitive unaryNumericPromotion(Type lhs) {
		// First, we must unbox either operand if they are boxed.		
		if(lhs instanceof Type.Clazz) {
			lhs = unboxedType((Type.Clazz) lhs);
		}
		
		if (lhs instanceof Type.Char || lhs instanceof Type.Short
				|| lhs instanceof Type.Byte) {
			return new Type.Int();
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
	public Type.Primitive binaryNumericPromotion(Type lhs, Type rhs) {
		
		// First, we must unbox either operand if they are boxed.
		if(lhs instanceof Type.Clazz) {
			lhs = unboxedType((Type.Clazz) lhs);
		}
		if(rhs instanceof Type.Clazz) {
			rhs = unboxedType((Type.Clazz) rhs);
		}
		
		// Second, convert to the appropriate type
		if(lhs instanceof Type.Double || rhs instanceof Type.Double) {
			return new Type.Double();
		}
		if(lhs instanceof Type.Float || rhs instanceof Type.Float) {
			return new Type.Float();
		}
		if(lhs instanceof Type.Long || rhs instanceof Type.Long) {
			return new Type.Long();
		}
		
		// The following is not part of JLS 5.6.2, but is handy for dealing with
        // boolean operators &, |, ^ etc.
		if(lhs instanceof Type.Bool && rhs instanceof Type.Bool) {
			return new Type.Bool();
		}
		
		return new Type.Int();		
	}
	
	/**
     * Given a primitive type, determine the equivalent boxed type. For example,
     * the primitive type int yields the type java.lang.Integer. For simplicity
     * in the code using this, it returns in the form a java.Type, rather than a
     * jil.Type.
     * 
     * @param p
     * @return
     */
	public static Type.Reference boxedType(Type.Primitive p) {
		if(p instanceof Type.Bool) {
			return new Type.Clazz("java.lang","Boolean");
		} else if(p instanceof Type.Byte) {
			return new Type.Clazz("java.lang","Byte");
		} else if(p instanceof Type.Char) {
			return new Type.Clazz("java.lang","Character");
		} else if(p instanceof Type.Short) {
			return new Type.Clazz("java.lang","Short");
		} else if(p instanceof Type.Int) {
			return new Type.Clazz("java.lang","Integer");
		} else if(p instanceof Type.Long) {
			return new Type.Clazz("java.lang","Long");
		} else if(p instanceof Type.Float) {
			return new Type.Clazz("java.lang","Float");
		} else {
			return new Type.Clazz("java.lang","Double");
		}
	}
	
	/**
	 * Given a primitive wrapper class (i.e. a boxed type), return the unboxed
	 * equivalent. For example, java.lang.Integer yields int, whilst
	 * java.lang.Boolean yields bool.
	 * 
	 * @param p
	 * @return
	 */
	protected Type.Primitive unboxedType(Type.Clazz p) {
		assert isWrapper(p);		
		String type = p.components().get(p.components().size()-1).first();
		
		if(type.equals("Boolean")) {
			return new Type.Bool();
		} else if(type.equals("Byte")) {
			return new Type.Byte();
		} else if(type.equals("Character")) {
			return new Type.Char();
		} else if(type.equals("Short")) {
			return new Type.Short();
		} else if(type.equals("Integer")) {
			return new Type.Int();
		} else if(type.equals("Long")) {
			return new Type.Long();
		} else if(type.equals("Float")) {
			return new Type.Float();
		} else if(type.equals("Double")) {
			return new Type.Double();
		} else {
			throw new RuntimeException("Unknown boxed type \"" + p.toString()
					+ "\" encountered.");
		}
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
	protected Expr implicitCast(Expr e, Type t) {
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
					return implicitCast(new Expr.Invoke(e, "byteValue",
							new ArrayList<Expr>(), new ArrayList(),
							new Type.Byte()), t);
				} else if (c.equals("Character")) {
					return implicitCast(new Expr.Invoke(e, "charValue",
							new ArrayList<Expr>(), new ArrayList(),
							new Type.Char()), t);
				} else if (c.equals("Short")) {
					return implicitCast(new Expr.Invoke(e, "shortValue",
							new ArrayList<Expr>(), new ArrayList(),
							new Type.Short()), t);
				} else if (c.equals("Integer")) {
					return implicitCast(new Expr.Invoke(e, "intValue",
							new ArrayList<Expr>(), new ArrayList(),
							new Type.Int()), t);
				} else if (c.equals("Long")) {
					return implicitCast(new Expr.Invoke(e, "longValue",
							new ArrayList<Expr>(), new ArrayList(),
							new Type.Long()), t);
				} else if (c.equals("Float")) {
					return implicitCast(new Expr.Invoke(e, "floatValue",
							new ArrayList<Expr>(), new ArrayList(),
							new Type.Float()), t);
				} else if (c.equals("Double")) {
					return implicitCast(new Expr.Invoke(e, "doubleValue",
							new ArrayList<Expr>(), new ArrayList(),
							new Type.Double()), t);
				} else if (c.equals("Boolean")) {
					return implicitCast(new Expr.Invoke(e, "booleanValue",
							new ArrayList<Expr>(), new ArrayList(),
							new Type.Bool()), t);
				} else {
					throw new RuntimeException("Unreachable code reached!");
				}
			}
		} else if(e_t instanceof Type.Primitive && t instanceof Type.Clazz) {		
			if (isWrapper(t) && unboxedType((Type.Clazz) t) instanceof Type.Byte && e_t instanceof Type.Byte) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(e);
				return new Expr.New(fromJilType(boxedType((Type.Primitive)e_t)),null,params,new ArrayList<Decl>(), e_t, e.attribute(SourceLocation.class));
			} else if (isWrapper(t) && unboxedType((Type.Clazz) t) instanceof Type.Char && e_t instanceof Type.Char) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(e);
				return new Expr.New(fromJilType(boxedType((Type.Primitive)e_t)),null,params,new ArrayList<Decl>(), e_t, e.attribute(SourceLocation.class));
			} else if (isWrapper(t) && unboxedType((Type.Clazz) t) instanceof Type.Short && e_t instanceof Type.Short) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(e);
				return new Expr.New(fromJilType(boxedType((Type.Primitive)e_t)),null,params,new ArrayList<Decl>(), e_t, e.attribute(SourceLocation.class));
			} else if (isWrapper(t) && unboxedType((Type.Clazz) t) instanceof Type.Int && e_t instanceof Type.Int) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(e);
				return new Expr.New(fromJilType(boxedType((Type.Primitive)e_t)),null,params,new ArrayList<Decl>(), e_t, e.attribute(SourceLocation.class));
			} else if (isWrapper(t) && unboxedType((Type.Clazz) t) instanceof Type.Long && e_t instanceof Type.Long) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(e);
				return new Expr.New(fromJilType(boxedType((Type.Primitive)e_t)),null,params,new ArrayList<Decl>(), e_t, e.attribute(SourceLocation.class));
			} else if (isWrapper(t) && unboxedType((Type.Clazz) t) instanceof Type.Float && e_t instanceof Type.Float) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(e);
				return new Expr.New(fromJilType(boxedType((Type.Primitive)e_t)),null,params,new ArrayList<Decl>(), e_t, e.attribute(SourceLocation.class));
			} else if (isWrapper(t) && unboxedType((Type.Clazz) t) instanceof Type.Double && e_t instanceof Type.Double) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(e);
				return new Expr.New(fromJilType(boxedType((Type.Primitive)e_t)),null,params,new ArrayList<Decl>(), e_t, e.attribute(SourceLocation.class));
			} else {
				throw new RuntimeException("Unreachable code reached! (" + e_t + ", " + t + ")");
			}			
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
					return isUnknownConstant(bop.lhs()); 								
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
			return new Value.Byte((byte)val, new Type.Byte(), loc);				
		} else if(lhs_t instanceof Type.Char && val >= 0 && val <= 65535) {
			return new Value.Char((char)val, new Type.Char(), loc);				
		} else if(lhs_t instanceof Type.Short && val >= -32768 && val <= 32768) {
			return new Value.Short((short)val, new Type.Short(), loc);				
		} else if(isWrapper(lhs_t)) {
			Type.Clazz ref = (Type.Clazz) lhs_t;			
			String s = ref.components().get(0).first();				
			if(s.equals("Byte") && val >= -128 && val <= 127) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(new Value.Byte((byte)val));
				return new Expr.New(fromJilType(lhs_t),null,params,new ArrayList<Decl>(), lhs_t, loc);				
			} else if(s.equals("Character") && val >= 0 && val <= 65535) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(new Value.Byte((byte)val));
				return new Expr.New(fromJilType(lhs_t),null,params,new ArrayList<Decl>(), lhs_t, loc);				
			} else if(s.equals("Short") && val >= -32768 && val <= 32768) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(new Value.Byte((byte)val));
				return new Expr.New(fromJilType(lhs_t),null,params,new ArrayList<Decl>(), lhs_t, loc);				
			}
		} 
		
		return new Value.Int(val,new Type.Int(),loc);
	}
	
	/**
     * Convert a type in jil to a type in java. This method is annoying, since
     * it seems to be converting to the same thing. However, there is a subtle
     * difference, in that a Java type represents a type as written in the
     * source code, rather than the abstract notion of a type.
     * 
     * @param jt
     * @return
     */
	protected jkit.java.Type fromJilType(jkit.jil.Type t) {
		if(t instanceof jkit.jil.Type.Primitive) {
			return fromJilType((jkit.jil.Type.Primitive)t);
		} else if(t instanceof jkit.jil.Type.Array) {
			return fromJilType((jkit.jil.Type.Array)t);
		} else if(t instanceof jkit.jil.Type.Clazz) {
			return fromJilType((jkit.jil.Type.Clazz)t);
		}
		throw new RuntimeException("Need to finish fromJilType off!");
	}
	
	protected jkit.java.Type.Primitive fromJilType(jkit.jil.Type.Primitive pt) {
		if(pt instanceof jkit.jil.Type.Void) {
			return new jkit.java.Type.Void(pt);
		} else if(pt instanceof jkit.jil.Type.Bool) {
			return new jkit.java.Type.Bool(pt);
		} else if(pt instanceof jkit.jil.Type.Byte) {
			return new jkit.java.Type.Byte(pt);
		} else if(pt instanceof jkit.jil.Type.Char) {
			return new jkit.java.Type.Char(pt);
		} else if(pt instanceof jkit.jil.Type.Short) {
			return new jkit.java.Type.Short(pt);
		} else if(pt instanceof jkit.jil.Type.Int) {
			return new jkit.java.Type.Int(pt);
		} else if(pt instanceof jkit.jil.Type.Long) {
			return new jkit.java.Type.Long(pt);
		} else if(pt instanceof jkit.jil.Type.Float) {
			return new jkit.java.Type.Float(pt);
		} else {
			return new jkit.java.Type.Double(pt);
		}
	}
	
	protected jkit.java.Type.Array fromJilType(jkit.jil.Type.Array at) {
		return new jkit.java.Type.Array(fromJilType(at.element()),at);
	}
	
	protected jkit.java.Type.Clazz fromJilType(jkit.jil.Type.Clazz jt) {
		// I will make it fully qualified for simplicity.
		ArrayList<Pair<String,List<jkit.java.Type.Reference>>> ncomponents = new ArrayList();
		// So, we need to split out the package into the component parts
		String pkg = jt.pkg();
		int idx = 0;
		int start = 0;
		while((idx = pkg.indexOf('.',idx)) != -1) {
			ncomponents.add(new Pair(pkg.substring(start,idx),new ArrayList()));
			start = idx;
		}
		
		// Now, complete the components list
		for(Pair<String,List<jkit.jil.Type.Reference>> c : jt.components()) {
			ArrayList<jkit.java.Type.Reference> l = new ArrayList();
			for(jkit.jil.Type.Reference r : c.second()) {
				l.add((jkit.java.Type.Reference)fromJilType(r));
			}
			ncomponents.add(new Pair(c.first(),l));
		}
		
		return new jkit.java.Type.Clazz(ncomponents,jt);
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
	protected jkit.jil.Type resolve(jkit.java.Type t, List<String> imports) {
		if(t instanceof jkit.java.Type.Primitive) {
			return resolve((jkit.java.Type.Primitive)t, imports);
		} else if(t instanceof jkit.java.Type.Clazz) {
			return resolve((jkit.java.Type.Clazz)t, imports);			
		} else if(t instanceof jkit.java.Type.Array) {
			return resolve((jkit.java.Type.Array)t, imports);
		} 
		
		return null;
	}
	
	protected jkit.jil.Type.Primitive resolve(jkit.java.Type.Primitive pt, List<String> imports) {
		if(pt instanceof jkit.java.Type.Void) {
			return new jkit.jil.Type.Void();
		} else if(pt instanceof jkit.java.Type.Bool) {
			return new jkit.jil.Type.Bool();
		} else if(pt instanceof jkit.java.Type.Byte) {
			return new jkit.jil.Type.Byte();
		} else if(pt instanceof jkit.java.Type.Char) {
			return new jkit.jil.Type.Char();
		} else if(pt instanceof jkit.java.Type.Short) {
			return new jkit.jil.Type.Short();
		} else if(pt instanceof jkit.java.Type.Int) {
			return new jkit.jil.Type.Int();
		} else if(pt instanceof jkit.java.Type.Long) {
			return new jkit.jil.Type.Long();
		} else if(pt instanceof jkit.java.Type.Float) {
			return new jkit.jil.Type.Float();
		} else {
			return new jkit.jil.Type.Double();
		}
	}
	
	protected jkit.jil.Type.Array resolve(jkit.java.Type.Array t, List<String> imports) {
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
	protected jkit.jil.Type.Reference resolve(jkit.java.Type.Clazz ct, List<String> imports) {
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
				Pair<String,List<jkit.java.Type.Reference>> component = ct.components().get(i);
				ArrayList<jkit.jil.Type.Reference> nvars = new ArrayList();
				
				for(jkit.java.Type.Reference r : component.second()) {
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
}

