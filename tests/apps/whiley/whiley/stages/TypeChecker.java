package whiley.stages;

import java.util.*;
import java.math.BigInteger;
import whiley.util.*;
import whiley.ast.Function;
import whiley.ast.attrs.*;
import whiley.ast.exprs.*;
import whiley.ast.stmts.*;
import whiley.ast.types.*;
import whiley.util.BigRational;

/**
 * <p>
 * This class makes various simple checks related to the way variables are used.
 * In particular, it does the following things:
 * </p>
 * <ol>
 * <li>It checks that every variable used in a method is declared
 * appropriately.</li>
 * <li>It checks that expressions are coherent. For example, that we're not
 * adding an integer to an array, or trying to do something with the return
 * value from a void method.</li>
 * </ol>
 * 
 * @author djp
 * 
 */
public class TypeChecker {
	public void verify(Function f) {
		HashMap<String,Type> environment = new HashMap<String,Type>();
		
		for(Pair<Type,String> p : f.getParameterTypes()) {
			if(p.first() instanceof VoidType) {
					syntaxError("parameter cannot be declared void",f);
			}
			environment.put(p.second(), p.first());
		}
		
		for(Stmt s : f.getStatements()) {
			check(s,environment,f);
		}
	}
	
	protected void check(Stmt s, HashMap<String,Type> environment, Function f) {
		if (s instanceof Skip || s instanceof Read) {
			// nothing to do here.
		} else if(s instanceof Print) {
			check((Print)s,environment);
		} else if (s instanceof Assign) {
			check((Assign)s,environment);			
		} else if (s instanceof IfElse) {
			check((IfElse)s,environment,f);	
		} else if (s instanceof While) {
			check((While)s,environment,f);	
		} else if (s instanceof Return) {
			check((Return)s,environment,f);			
		} else if (s instanceof Assertion) {
			check((Assertion) s,environment);
		} else if (s instanceof Invoke) {
			check((Invoke) s, environment);
		} else if (s instanceof VarDecl) {
			check((VarDecl) s, environment);
		} else {
			syntaxError("Unknown statement encountered: " + s, s);
		}
	}
	
	protected void check(VarDecl s, HashMap<String, Type> environment) {
		if(environment.get(s.name()) != null) {
			syntaxError("duplicate variable declaration.",s);
		} else if(s.type() instanceof VoidType) {
			// cannot declare a variable to have void type!
			syntaxError("variable cannot be declared void",s);
		}
		
		environment.put(s.name(), s.type());
	}
	
	protected void check(Print s, HashMap<String, Type> environment) {
		check(s.getExpression(),environment);				
	}

	protected void check(Assign s, HashMap<String, Type> environment) {		
		Type lhs = check(s.lhs(),environment);		
		Type rhs = check(s.rhs(),environment);
		checkSubtype(lhs,rhs,s);		
	}

	protected void check(IfElse s, HashMap<String, Type> environment, Function f) {
		Type cond = check(s.getCondition(),environment);
		checkSubtype(new BoolType(),cond,s);
		for(Stmt st : s.getTrueBranch()) {
			check(st,environment,f);	
		}
		if(s.getFalseBranch() != null) {
			for(Stmt st : s.getFalseBranch()) {
				check(st,environment,f);	
			}
		}
	}

	protected void check(While s, HashMap<String, Type> environment, Function f) {
		Type cond = check(s.condition(),environment);
		checkSubtype(new BoolType(),cond,s);
		for(Stmt st : s.body()) {
			check(st,environment,f);	
		}		
	}
	
	protected void check(Return s, HashMap<String, Type> environment, Function f) {
		Type rhs = check(s.getExpression(),environment);
		Type lhs = f.getReturnType();
		checkSubtype(lhs,rhs,s);
	}
	
	protected void check(Assertion s, HashMap<String, Type> environment) {
		Type cond = check(s.getExpression(),environment);
		checkSubtype(new BoolType(),cond,s);
	}
	
	protected Type check(Invoke s, HashMap<String, Type> environment) {
		
		Function f = s.getFunction();
		List<Pair<Type,String>> params = f.getParameterTypes();
		List<Expression> args = s.getArguments();
		
		if(params.size() < args.size()) {
			syntaxError("Too many method arguments provided.",s);
		} else if(params.size() > args.size()) {
			syntaxError("Not enough method arguments provided.",s);
		}
				
		for(int i=0;i!=args.size();++i) {
			Expression arg = args.get(i);
			Type rhs = check(arg,environment);
			checkSubtype(params.get(i).first(),rhs,arg);
		}
		
		return s.getFunction().getReturnType();
	}
	
	protected Type check(Expression e, HashMap<String, Type> environment) {
		Type retType;
		if(e instanceof Constant) {
			Object o = ((Constant)e).value();
			if(o instanceof BigInteger) {
				retType = new IntType();
			} else if(o instanceof BigRational) {
				retType = new RealType();
			} else {
				retType = new BoolType();
			}
		} else if(e instanceof Variable) {
			retType = check((Variable)e,environment);						
		} else if(e instanceof BinOp) {
			retType = check((BinOp)e,environment);
		} else if(e instanceof Negate) {
			retType = check((Negate)e,environment);
		} else if(e instanceof Invoke) {
			retType = check((Invoke)e,environment);
			if(retType instanceof VoidType) {
				syntaxError("method has void return type.",e);
				return null;
			} 			
		} else if(e instanceof Not) {
			retType = check((Not)e,environment);
		} else if(e instanceof LengthOf) {
			retType = check((LengthOf)e,environment);
		} else if(e instanceof ListVal) {
			retType = check((ListVal)e,environment);
		} else if(e instanceof ListAccess) {
			retType = check((ListAccess)e,environment);
		} else {
			syntaxError("Unknown expression encountered.",e);
			return null;
		}
		
		e.attributes().add(new TypeAttr(retType));
		return retType;
	}
	
	protected Type check(ListAccess e, HashMap<String, Type> environment) {
		Type lhs = check(e.source(), environment);
		Type rhs = check(e.index(), environment);
		
		if (!(lhs instanceof ListType)) {
			syntaxError("expected array type, found type " + lhs + ".", e.source());
		}
		if (!(rhs instanceof IntType)) {
			syntaxError("expected int type, found type " + rhs + ".", e.index());
		}
		ListType at = (ListType) lhs;
		return at.element();		
	}
	
	protected Type check(ListVal e, HashMap<String, Type> environment) {
		List<Expression> exprs = e.getValues();
		if(exprs.size() == 0) {
			return new ListType(new AnyType());
		}
		Type t = check(exprs.get(0), environment);
		for(int i=1;i!=exprs.size();++i) {
			Type et = check(exprs.get(i), environment);
			checkSubtype(t,et,exprs.get(i));
		}
		return new ListType(t);
	}
	
	protected Type check(LengthOf e, HashMap<String, Type> environment) {
		Type lhs = check(e.getExpr(), environment);
		if(!(lhs instanceof ListType)) {
			syntaxError("expression must have bool type.",e.getExpr());
		}		
		return new IntType();
	}
	
	protected Type check(Not e, HashMap<String, Type> environment) {
		Type lhs = check(e.getCondition(), environment);
		if(!(lhs instanceof BoolType)) {
			syntaxError("expression must have bool type.",e.getCondition());
		}
		return lhs;
	}	
	
	protected Type check(Negate e, HashMap<String, Type> environment) {
		Type lhs = check(e.getExpr(),environment);
		if(!(lhs instanceof IntType)) {
			syntaxError("expecting int type, found " + lhs + ".",e.getExpr());
		}
		return lhs;
	}
	
	protected Type check(BinOp e, HashMap<String, Type> environment) {		
		Type lhs = check(e.getLeftExpr(),environment);
		Type rhs = check(e.getRightExpr(),environment);
		
		if(e instanceof And || e instanceof Or) {
			if(!(lhs instanceof BoolType)) {
				syntaxError("expecting bool type, found " + lhs + ".",e.getLeftExpr());
			} else if(!(rhs instanceof BoolType)) {
				syntaxError("expecting bool type, found " + rhs + ".",e.getRightExpr());
			}
		} else {
			if(!(lhs instanceof IntType || lhs instanceof RealType)) {
				syntaxError("expecting int type, found " + lhs + ".",e.getLeftExpr());
			} else if(!(rhs instanceof IntType || rhs instanceof RealType)) {
				syntaxError("expecting int type, found " + rhs + ".",e.getRightExpr());
			}
		}
		
		if(e instanceof Condition) {
			return new BoolType();
		} else if(lhs instanceof IntType && rhs instanceof IntType){
			return new IntType();
		} else {
			return new RealType();
		}
	}
	
	protected Type check(Variable e, HashMap<String, Type> environment) {
		Variable v = (Variable) e;
		Type t = environment.get(v.getVariable());		
		if (t == null) {
			syntaxError("variable " + e.getVariable() + " has not been declared.", e);
		} 
		return t;
	}
	
	/**
	 * <p>This method checks that type t2 is a subtype of type t1.</p>
	 * @param t1
	 * @param t2
	 * @param elem
	 */
	protected void checkSubtype(Type t1, Type t2, SyntacticElement elem) {
		if (t1 instanceof ListType && t2 instanceof ListType) {
			ListType at1 = (ListType) t1;
			ListType at2 = (ListType) t2;
			checkSubtype(at1.element(), at2.element(), elem);
		} else if (t1 instanceof RealType && t2 instanceof IntType) {
			// this is OK
		} else if (!t1.equals(t2) && !(t2 instanceof AnyType)) {
			syntaxError("expected type " + t1 + ", got type " + t2 + ".", elem);
		}
	}
	
	private static void syntaxError(String msg, SyntacticElement elem) {
		int start = -1;
		int end = -1;
		String filename = null;
		
		SourceAttr attr = (SourceAttr) elem.attribute(SourceAttr.class);
		if(attr != null) {
			start=attr.start();
			end=attr.end();
			filename = attr.filename();
		}
		
		throw new SyntaxError(msg, filename, start, end);
	}
}
