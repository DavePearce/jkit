package jkit.java.stages;

import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.compiler.SyntaxError;
import jkit.java.*;
import jkit.java.Decl.*;
import jkit.util.*;
import jkit.jil.Type;
import jkit.jil.Modifier;
import jkit.jil.SourceLocation;
import jkit.jkil.FlowGraph.ArrayVal;

/**
 * This class goes through a JavaFile and propagate type attributes
 * appropriately. The stage also checks that types are used appropriately in
 * expressions and statements and, where not, emits error messages accordingly.
 * 
 * @author djp
 * 
 */
public class TypingChecking {
	private ClassLoader loader;
	
	public TypingChecking(ClassLoader loader) {
		this.loader = loader; 
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
		// First, we need to construct a typing environment for local variables.
		HashMap<String,Type> environment = new HashMap<String,Type>();
		for(Triple<String,List<Modifier>,Type> p : d.parameters()) {
			environment.put(p.first(), p.third());
		}
						
		doStatement(d.body(),environment);
	}

	protected void doField(Field d) {

	}
	
	protected void doStatement(Stmt e, HashMap<String,Type> environment) {
		if(e instanceof Stmt.SynchronisedBlock) {
			doSynchronisedBlock((Stmt.SynchronisedBlock)e, environment);
		} else if(e instanceof Stmt.TryCatchBlock) {
			doTryCatchBlock((Stmt.TryCatchBlock)e, environment);
		} else if(e instanceof Stmt.Block) {
			doBlock((Stmt.Block)e, environment);
		} else if(e instanceof Stmt.VarDef) {
			doVarDef((Stmt.VarDef) e, environment);
		} else if(e instanceof Stmt.Assignment) {
			doAssignment((Stmt.Assignment) e, environment);
		} else if(e instanceof Stmt.Return) {
			doReturn((Stmt.Return) e, environment);
		} else if(e instanceof Stmt.Throw) {
			doThrow((Stmt.Throw) e, environment);
		} else if(e instanceof Stmt.Assert) {
			doAssert((Stmt.Assert) e, environment);
		} else if(e instanceof Stmt.Break) {
			doBreak((Stmt.Break) e, environment);
		} else if(e instanceof Stmt.Continue) {
			doContinue((Stmt.Continue) e, environment);
		} else if(e instanceof Stmt.Label) {
			doLabel((Stmt.Label) e, environment);
		} else if(e instanceof Stmt.If) {
			doIf((Stmt.If) e, environment);
		} else if(e instanceof Stmt.For) {
			doFor((Stmt.For) e, environment);
		} else if(e instanceof Stmt.ForEach) {
			doForEach((Stmt.ForEach) e, environment);
		} else if(e instanceof Stmt.While) {
			doWhile((Stmt.While) e, environment);
		} else if(e instanceof Stmt.DoWhile) {
			doDoWhile((Stmt.DoWhile) e, environment);
		} else if(e instanceof Stmt.Switch) {
			doSwitch((Stmt.Switch) e, environment);
		} else if(e instanceof Expr.Invoke) {
			doInvoke((Expr.Invoke) e, environment);
		} else if(e instanceof Expr.New) {
			doNew((Expr.New) e, environment);
		} else if(e instanceof Decl.Clazz) {
			doClass((Decl.Clazz)e);
		} else if(e != null) {
			throw new RuntimeException("Invalid statement encountered: "
					+ e.getClass());
		}		
	}
	
	protected void doBlock(Stmt.Block block, HashMap<String,Type> environment) {
		// The following clone is required, so that any additions to the
        // environment via local variable defintions in this block are
        // preserved.
		HashMap<String,Type> newEnv = (HashMap<String,Type>) environment.clone();
		
		// now process every statement in this block.
		for(Stmt s : block.statements()) {
			doStatement(s,environment);
		}
	}
	
	protected void doSynchronisedBlock(Stmt.SynchronisedBlock block, HashMap<String,Type> environment) {
		// The following clone is required, so that any additions to the
        // environment via local variable defintions in this block are
        // preserved.
		HashMap<String,Type> newEnv = (HashMap<String,Type>) environment.clone();
		
		// now process every statement in this block.
		for(Stmt s : block.statements()) {
			doStatement(s,environment);
		}
	}
	
	protected void doTryCatchBlock(Stmt.TryCatchBlock block, HashMap<String,Type> environment) {
		
	}
	
	protected void doVarDef(Stmt.VarDef def, HashMap<String,Type> environment) {
		Type t = def.type();
		
		for(Triple<String, Integer, Expr> d : def.definitions()) {
			Type nt = t;
			if(d.third() != null) {
				doExpression(d.third(),environment);
			}
			for(int i=0;i!=d.second();++i) {
				nt = new Type.Array(nt);
			}									
			environment.put(d.first(),nt);
		}
	}
	
	protected void doAssignment(Stmt.Assignment def, HashMap<String,Type> environment) {
		doExpression(def.lhs(),environment);	
		doExpression(def.rhs(),environment);			

		// type inference stuff goes here. 
	}
	
	protected void doReturn(Stmt.Return ret, HashMap<String,Type> environment) {
		
	}
	
	protected void doThrow(Stmt.Throw ret, HashMap<String,Type> environment) {
						
	}
	
	protected void doAssert(Stmt.Assert ret, HashMap<String,Type> environment) {
								
	}
	
	protected void doBreak(Stmt.Break brk, HashMap<String,Type> environment) {
			
	}
	
	protected void doContinue(Stmt.Continue brk, HashMap<String,Type> environment) {
			
	}
	
	protected void doLabel(Stmt.Label lab, HashMap<String,Type> environment) {				

	}
	
	protected void doIf(Stmt.If stmt, HashMap<String,Type> environment) {
		doExpression(stmt.condition(),environment);
		doStatement(stmt.trueStatement(),environment);
		doStatement(stmt.falseStatement(),environment);
	}
	
	protected void doWhile(Stmt.While stmt, HashMap<String,Type> environment) {
			
	}
	
	protected void doDoWhile(Stmt.DoWhile stmt, HashMap<String,Type> environment) {
		
	}
	
	protected void doFor(Stmt.For stmt, HashMap<String,Type> environment) {
		
	}
	
	protected void doForEach(Stmt.ForEach stmt, HashMap<String,Type> environment) {
		
	}
	
	protected void doSwitch(Stmt.Switch s, HashMap<String,Type> environment) {
		
	}
	
	protected void doExpression(Expr e, HashMap<String,Type> environment) {	
		if(e instanceof Value.Bool) {
			doBoolVal((Value.Bool)e,environment);
		} else if(e instanceof Value.Char) {
			doCharVal((Value.Char)e,environment);
		} else if(e instanceof Value.Int) {
			doIntVal((Value.Int)e,environment);
		} else if(e instanceof Value.Long) {
			doLongVal((Value.Long)e,environment);
		} else if(e instanceof Value.Float) {
			doFloatVal((Value.Float)e,environment);
		} else if(e instanceof Value.Double) {
			doDoubleVal((Value.Double)e,environment);
		} else if(e instanceof Value.String) {
			doStringVal((Value.String)e,environment);
		} else if(e instanceof Value.Null) {
			doNullVal((Value.Null)e,environment);
		} else if(e instanceof Value.TypedArray) {
			doTypedArrayVal((Value.TypedArray)e,environment);
		} else if(e instanceof Value.Array) {
			doArrayVal((Value.Array)e,environment);
		} else if(e instanceof Value.Class) {
			doClassVal((Value.Class) e,environment);
		} else if(e instanceof Expr.Variable) {
			doVariable((Expr.Variable)e,environment);
		} else if(e instanceof Expr.UnOp) {
			doUnOp((Expr.UnOp)e,environment);
		} else if(e instanceof Expr.BinOp) {
			doBinOp((Expr.BinOp)e,environment);
		} else if(e instanceof Expr.TernOp) {
			doTernOp((Expr.TernOp)e,environment);
		} else if(e instanceof Expr.Cast) {
			doCast((Expr.Cast)e,environment);
		} else if(e instanceof Expr.InstanceOf) {
			doInstanceOf((Expr.InstanceOf)e,environment);
		} else if(e instanceof Expr.Invoke) {
			doInvoke((Expr.Invoke) e,environment);
		} else if(e instanceof Expr.New) {
			doNew((Expr.New) e,environment);
		} else if(e instanceof Expr.ArrayIndex) {
			doArrayIndex((Expr.ArrayIndex) e,environment);
		} else if(e instanceof Expr.Deref) {
			doDeref((Expr.Deref) e,environment);
		} else if(e instanceof Stmt.Assignment) {
			// force brackets			
			doAssignment((Stmt.Assignment) e,environment);			
		} else if(e != null) {
			throw new RuntimeException("Invalid expression encountered: "
					+ e.getClass());
		}
	}
	
	protected void doDeref(Expr.Deref e, HashMap<String,Type> environment) {
			
	}
	
	protected void doArrayIndex(Expr.ArrayIndex e, HashMap<String,Type> environment) {
		
	}
	
	protected void doNew(Expr.New e, HashMap<String,Type> environment) {
		
	}
	
	protected void doInvoke(Expr.Invoke e, HashMap<String,Type> environment) {
		
	}
	
	protected void doInstanceOf(Expr.InstanceOf e, HashMap<String,Type> environment) {		
			
	}
	
	protected void doCast(Expr.Cast e, HashMap<String,Type> environment) {
	
	}
	
	protected void doBoolVal(Value.Bool e, HashMap<String,Type> environment) {
		e.attributes().add(new Type.Bool());
	}
	
	protected void doCharVal(Value.Char e, HashMap<String,Type> environment) {
		e.attributes().add(new Type.Char());
	}
	
	protected void doIntVal(Value.Int e, HashMap<String,Type> environment) {
		e.attributes().add(new Type.Int());
	}
	
	protected void doLongVal(Value.Long e, HashMap<String,Type> environment) {		
		e.attributes().add(new Type.Long());
	}
	
	protected void doFloatVal(Value.Float e, HashMap<String,Type> environment) {		
		e.attributes().add(new Type.Float());
	}
	
	protected void doDoubleVal(Value.Double e, HashMap<String,Type> environment) {		
		e.attributes().add(new Type.Double());
	}
	
	protected void doStringVal(Value.String e, HashMap<String,Type> environment) {		
		e.attributes().add(new Type.Clazz("java.lang","String"));
	}
	
	protected void doNullVal(Value.Null e, HashMap<String,Type> environment) {		
		e.attributes().add(new Type.Null());
	}
	
	protected void doTypedArrayVal(Value.TypedArray e, HashMap<String,Type> environment) {		
		e.attributes().add(e.type());
	}
	
	protected void doArrayVal(Value.Array e, HashMap<String,Type> environment) {		
		// not sure what to do here.
	}
	
	protected void doClassVal(Value.Class e, HashMap<String,Type> environment) {
		
	}
	
	protected void doVariable(Expr.Variable e, HashMap<String,Type> environment) {			
		Type t = environment.get(e.value());
		if(t == null) {
			SourceLocation loc = (SourceLocation) e.attribute(SourceLocation.class);
			throw new SyntaxError("Cannot find symbol - variable \""
					+ e.value() + "\"", loc.line(), loc.column());
		} else {
			e.attributes().add(t);
		}
	}

	protected void doUnOp(Expr.UnOp e, HashMap<String,Type> environment) {		
		
	}
		
	protected void doBinOp(Expr.BinOp e, HashMap<String,Type> environment) {				
		doExpression(e.lhs(),environment);
		doExpression(e.rhs(),environment);
		
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
	
	protected void doTernOp(Expr.TernOp e, HashMap<String,Type> environment) {		
		
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
	 * the primitive type int yields the type java.lang.Integer.
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
	private static Type.Primitive unboxedType(Type.Clazz p) {
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
	public static Expr implicitCast(Expr e, Type t) {
		Type e_t = (Type) e.attribute(Type.class);
		// insert implicit casts for primitive types.
		if (!e_t.equals(t)
				&& (t instanceof Type.Primitive && e_t instanceof Type.Primitive)) {			
			e = new Expr.Convert((Type.Primitive)t, e, t, e.attribute(SourceLocation.class));
		} else if(t instanceof Type.Primitive && e_t instanceof Type.Clazz) {
			Type.Clazz r = (Type.Clazz) e_t;
			if (r.pkg().equals("java.lang") && r.components().size() == 1) {
				String c = r.components().get(0).first();
				if (c.equals("Byte")) {
					return implicitCast(new Expr.Invoke(e, "byteValue",
							new ArrayList<Expr>(), new ArrayList<Type>(),
							new Type.Byte()), t);
				} else if (c.equals("Character")) {
					return implicitCast(new Expr.Invoke(e, "charValue",
							new ArrayList<Expr>(), new ArrayList<Type>(),
							new Type.Char()), t);
				} else if (c.equals("Short")) {
					return implicitCast(new Expr.Invoke(e, "shortValue",
							new ArrayList<Expr>(), new ArrayList<Type>(),
							new Type.Short()), t);
				} else if (c.equals("Integer")) {
					return implicitCast(new Expr.Invoke(e, "intValue",
							new ArrayList<Expr>(), new ArrayList<Type>(),
							new Type.Int()), t);
				} else if (c.equals("Long")) {
					return implicitCast(new Expr.Invoke(e, "longValue",
							new ArrayList<Expr>(), new ArrayList<Type>(),
							new Type.Long()), t);
				} else if (c.equals("Float")) {
					return implicitCast(new Expr.Invoke(e, "floatValue",
							new ArrayList<Expr>(), new ArrayList<Type>(),
							new Type.Float()), t);
				} else if (c.equals("Double")) {
					return implicitCast(new Expr.Invoke(e, "doubleValue",
							new ArrayList<Expr>(), new ArrayList<Type>(),
							new Type.Double()), t);
				} else if (c.equals("Boolean")) {
					return implicitCast(new Expr.Invoke(e, "booleanValue",
							new ArrayList<Expr>(), new ArrayList<Type>(),
							new Type.Bool()), t);
				} else {
					throw new RuntimeException("Unreachable code reached!");
				}
			}
		} else if(e_t instanceof Type.Primitive && t instanceof Type.Clazz) {		
			if (isWrapper(t) && unboxedType((Type.Clazz) t) instanceof Type.Byte && e_t instanceof Type.Byte) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(e);
				return new Expr.New(boxedType((Type.Primitive)e_t),null,params,new ArrayList<Decl>(), e_t, e.attribute(SourceLocation.class));
			} else if (isWrapper(t) && unboxedType((Type.Clazz) t) instanceof Type.Char && e_t instanceof Type.Char) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(e);
				return new Expr.New(boxedType((Type.Primitive)e_t),null,params,new ArrayList<Decl>(), e_t, e.attribute(SourceLocation.class));
			} else if (isWrapper(t) && unboxedType((Type.Clazz) t) instanceof Type.Short && e_t instanceof Type.Short) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(e);
				return new Expr.New(boxedType((Type.Primitive)e_t),null,params,new ArrayList<Decl>(), e_t, e.attribute(SourceLocation.class));
			} else if (isWrapper(t) && unboxedType((Type.Clazz) t) instanceof Type.Int && e_t instanceof Type.Int) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(e);
				return new Expr.New(boxedType((Type.Primitive)e_t),null,params,new ArrayList<Decl>(), e_t, e.attribute(SourceLocation.class));
			} else if (isWrapper(t) && unboxedType((Type.Clazz) t) instanceof Type.Long && e_t instanceof Type.Long) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(e);
				return new Expr.New(boxedType((Type.Primitive)e_t),null,params,new ArrayList<Decl>(), e_t, e.attribute(SourceLocation.class));
			} else if (isWrapper(t) && unboxedType((Type.Clazz) t) instanceof Type.Float && e_t instanceof Type.Float) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(e);
				return new Expr.New(boxedType((Type.Primitive)e_t),null,params,new ArrayList<Decl>(), e_t, e.attribute(SourceLocation.class));
			} else if (isWrapper(t) && unboxedType((Type.Clazz) t) instanceof Type.Double && e_t instanceof Type.Double) {
				ArrayList<Expr> params = new ArrayList<Expr>();
				params.add(e);
				return new Expr.New(boxedType((Type.Primitive)e_t),null,params,new ArrayList<Decl>(), e_t, e.attribute(SourceLocation.class));
			} else {
				throw new RuntimeException("Unreachable code reached! (" + e_t + ", " + t + ")");
			}			
		} 
		
		return e;
	}
	
	/**
     * Check wither a given type is a reference to java.lang.String or not.
     * 
     * @param t
     * @return
     */
	private static boolean isString(Type t) {
		if(t instanceof Type.Clazz) {
			Type.Clazz c = (Type.Clazz) t;
			 return c.pkg().equals("java.lang") && c.components().size() == 1
					&& c.components().get(0).first().equals("String");			
		}
		return false;
	}
}

