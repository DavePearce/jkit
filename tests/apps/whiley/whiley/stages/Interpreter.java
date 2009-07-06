package whiley.stages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.math.BigInteger;

import whiley.util.*;
import whiley.ast.*;
import whiley.ast.attrs.*;
import whiley.ast.exprs.*;
import whiley.ast.stmts.*;
import whiley.ast.types.*;
import whiley.util.BigRational;

public final class Interpreter {
	
	/**
	 * Evaluate the method with the arguments provided.
	 * 
	 * @param args -
	 *            the list of argument values
	 */
	public Object evaluate(Function fun, List<Object> args) {		
		String name = fun.getName();		
		Condition preCondition = fun.getPrecondition();
		Condition postCondition = fun.getPostcondition();
		List<Pair<Type,String>> paramTypes = fun.getParameterTypes();
		List<Stmt> statements = fun.getStatements();
		
		HashMap<String,Object> store = new HashMap<String,Object>();

		// First, setup and check the parameters.
		if (args.size() != paramTypes.size()) {
			throw new RuntimeException(
					"Incorrect number of parameters supplied to "
							+ fun.getName());
		}

		for(int i=0;i!=args.size();++i) {
			Object val = args.get(i);		
			if(val instanceof Object[]) {
				val = ((Object[])val).clone();
			}
			// update store.
			store.put(paramTypes.get(i).second(), val);
		}

		// Second, check the pre condition.
		if (preCondition != null && !((Boolean) evaluate(preCondition, store))) {
			// failed!
			throw new RuntimeException("Precondition failed on entry to: "
					+ name);
		}

		for(Stmt stmt : statements) {
			evaluate(stmt,store);			
		}

		// Fourth, evaluate the post condition
		if (postCondition != null
				&& !((Boolean) evaluate(postCondition, store))) {
			// failed!
			throw new RuntimeException("Postcondition failed on exit from: "
					+ name);
		}

		return store.get("$");
	}		
	
	/**
	 * Execute this statement, using the program store provided.
	 * @param store - the program store, which maps variables to their current values.
	 * @return the index of the next statement to execute (i.e. the next pc value).
	 */
	private void evaluate(Stmt stmt, HashMap<String,Object> store) {		
		if(stmt instanceof Assertion) {
			evaluate((Assertion)stmt,store);
		} else if(stmt instanceof Assign) {
			evaluate((Assign)stmt,store);
		} else if(stmt instanceof IfElse) {
			evaluate((IfElse)stmt,store);
		} else if(stmt instanceof While) {
			evaluate((While)stmt,store);
		} else if(stmt instanceof Print) {
			evaluate((Print)stmt,store);
		} else if(stmt instanceof Read) {
			evaluate((Read)stmt,store);
		} else if(stmt instanceof Return) {
			evaluate((Return)stmt,store);
		} else if(stmt instanceof Skip) {
			evaluate((Skip)stmt,store);
		} else if(stmt instanceof VarDecl) {
			evaluate((VarDecl)stmt,store);
		} else if(stmt instanceof Invoke) {
			evaluate((Invoke)stmt,store);
		} else {
			runtimeError("unknown statement encountered",stmt);		
		}
	}
	
	private void evaluate(Assertion stmt, HashMap<String,Object> store) { 
		Object o = evaluate(stmt.getExpression(),store);
		checkBoolean(o,stmt.getExpression());
		if(!((Boolean)o)) {
			runtimeError("assertion failed",stmt);
		}				
	}
	
	private void evaluate(Assign stmt, HashMap<String, Object> store) {
		Expression lhs = stmt.lhs();
		Expression rhs = stmt.rhs();

		Object rv = evaluate(rhs,store);

		if (rv instanceof Object[]) {
			rv = ((Object[]) rv).clone();
		}

		if (lhs instanceof Variable) {
			Variable lv = (Variable) lhs;
			store.put(lv.getVariable(), rv);
		} else {
			ListAccess lv = (ListAccess) lhs;
			Object _src = (Object[]) evaluate(lv.source(),store);
			Object _idx = evaluate(lv.index(),store);
			checkList(_src,lv.source());
			checkInteger(_idx,lv.index());
			Object[] src = (Object[]) _src;
			int idx;
			if(_idx instanceof Integer) {
				idx = (Integer)_idx;
			} else {
				BigInteger bi = (BigInteger) _idx;
				idx = bi.intValue();
				// this is strange
				if(!BigInteger.valueOf(idx).equals(bi)) {
					runtimeError("index too large for list access",lv);
				}
			} 
			
			if(idx >= src.length || idx < 0) {
				runtimeError("index out of bounds",lv);
			}
			
			src[idx] = rv;
		}
	}
	
	private void evaluate(IfElse stmt, HashMap<String,Object> store) {	
		List<Stmt> branch = null;
		
		if((Boolean) evaluate(stmt.getCondition(),store)) {
			branch = stmt.getTrueBranch();
		} else {
			branch = stmt.getFalseBranch();
		}
		
		for(Stmt s : branch) {
			evaluate(s,store);
		}				
	}
	
	private void evaluate(While stmt, HashMap<String,Object> store) {			
		while(true) {
			Object o = evaluate(stmt.condition(),store);
			checkBoolean(o, stmt);
			if(!((Boolean)o)) { break; }
			
			// first, check loop invariant
			if(stmt.invariant() != null) {
				Object i = evaluate(stmt.invariant(),store);
				checkBoolean(i,stmt.invariant());
				if (!((Boolean) i)) {
					// loop invariant not satisfied
					runtimeError("loop invariant not satisfied", stmt
							.invariant());
				}
			}
			// second, evaluate body
			
			for(Stmt s : stmt.body()) {
				evaluate(s,store);
			}				
		}
	}
	
	private void evaluate(Print stmt, HashMap<String, Object> store) {
		Object v = evaluate(stmt.getExpression(),store);
		if (v instanceof Object[]) {
			System.out.println(Arrays.deepToString((Object[]) v));
		} else {
			System.out.println(v);
		}
	}
	
	private void evaluate(Read stmt, HashMap<String, Object> store) {
		// the following code will read a line from the standard
		// input, and attempt to convert into an integer.
		BufferedReader input = new BufferedReader(new InputStreamReader(
				System.in));
		try {
			String v = input.readLine();
			try {
				store.put(stmt.getVariable(), Integer.parseInt(v));
			} catch (NumberFormatException e) {
				runtimeError("input not recognised as an integer", stmt);
			}
		} catch (IOException e) {
			runtimeError("I/O error.", stmt);
		}
	}
	
	private void evaluate(Return stmt, HashMap<String, Object> store) {
		Object v = evaluate(stmt.getExpression(),store);
		checkSubtype(v,(Type)stmt.attribute(Type.class),stmt);
		store.put("$", v);
	}
	
	private void evaluate(Skip stmt, HashMap<String,Object> store) {	
		// skip ;)
	}
	
	private void evaluate(VarDecl stmt, HashMap<String,Object> store) {	
	}
	
	private Object evaluate(Expression expr, HashMap<String,Object> store) {
		if(expr instanceof Add) {
			return evaluate((Add)expr,store);
		} else if(expr instanceof And) {
			return evaluate((And)expr,store);
		} else if(expr instanceof ListVal) {
			return evaluate((ListVal)expr,store);
		} else if(expr instanceof Div) {
			return evaluate((Div)expr,store);
		} else if(expr instanceof Equals) {
			return evaluate((Equals)expr,store);
		} else if(expr instanceof GreaterThan) {
			return evaluate((GreaterThan)expr,store);
		} else if(expr instanceof GreaterThanEquals) {
			return evaluate((GreaterThanEquals)expr,store);
		} else if(expr instanceof Constant) {			
			return evaluate((Constant)expr,store);
		} else if(expr instanceof Invoke) {
			return evaluate((Invoke)expr,store);
		} else if(expr instanceof LengthOf) {
			return evaluate((LengthOf)expr,store);
		} else if(expr instanceof LessThan) {
			return evaluate((LessThan)expr,store);
		} else if(expr instanceof LessThanEquals) {
			return evaluate((LessThanEquals)expr,store);
		} else if(expr instanceof ListAccess) {
			return evaluate((ListAccess)expr,store);
		} else if(expr instanceof Mul) {
			return evaluate((Mul)expr,store);
		} else if(expr instanceof Negate) {
			return evaluate((Negate)expr,store);
		} else if(expr instanceof Not) {
			return evaluate((Not)expr,store);
		} else if(expr instanceof NotEquals) {
			return evaluate((NotEquals)expr,store);
		} else if(expr instanceof Or) {
			return evaluate((Or)expr,store);
		} else if(expr instanceof Sub) {
			return evaluate((Sub)expr,store);
		} else if(expr instanceof Variable) {
			return evaluate((Variable)expr,store);
		} else {
			runtimeError("unknown expression encountered",expr);
			return null;
		}
	}
	
	private Object evaluate(Add expr, HashMap<String,Object> store) {
		Object lval = evaluate(expr.getLeftExpr(),store);
		Object rval = evaluate(expr.getRightExpr(),store);
		if(rval instanceof BigRational || lval instanceof BigRational) {
			BigRational bl = bigrat(lval);
			BigRational br = bigrat(rval);
			return bl.add(br);
		} else if(rval instanceof BigInteger && lval instanceof BigInteger) {		
			BigInteger bl = (BigInteger) lval;
			BigInteger br = (BigInteger) rval;
			return bl.add(br);			
		} else {
			runtimeError("Invalid expression: " + lval + " + "
					+ rval, expr);
			return null;
		}	
	}
	
	private Object evaluate(And expr, HashMap<String,Object> store) {
		Object lval = evaluate(expr.getLeftExpr(),store);
		Object rval = evaluate(expr.getRightExpr(),store);
		checkBoolean(lval,expr.getLeftExpr());
		checkBoolean(rval,expr.getRightExpr());
		return ((Boolean)lval) && ((Boolean)rval);
	}
	
	private Object evaluate(ListVal expr, HashMap<String,Object> store) {
		Object[] vals = new Object[expr.getValues().size()];
		int i = 0;
		for(Expression e : expr.getValues()) {
			vals[i++] = evaluate(e,store);
		}		
		return vals;
	}
	
	private Object evaluate(Div expr, HashMap<String,Object> store) {
		Object lval = evaluate(expr.getLeftExpr(),store);
		Object rval = evaluate(expr.getRightExpr(),store);
		
		if(typeof(expr) instanceof RealType) {
			BigRational bl = bigrat(lval);
			BigRational br = bigrat(rval);			
			return bl.divide(br);
		} else if(rval instanceof BigInteger && lval instanceof BigInteger) {		
			BigInteger bl = (BigInteger) lval;
			BigInteger br = (BigInteger) rval;
			return bl.divide(br);			
		} else {
			runtimeError("Invalid expression: " + lval + " / "
					+ rval, expr);
			return null;
		}	
	}

	private Object evaluate(Equals expr, HashMap<String,Object> store) {
		// The order of execution here is back-to-front, but it
		// doesn't matter in the flow-chart language.  Right?
		Object rval = evaluate(expr.getLeftExpr(),store);
		Object lval = evaluate(expr.getRightExpr(),store);
		
		if(rval instanceof BigRational || lval instanceof BigRational) {
			BigRational bl = bigrat(lval);
			BigRational br = bigrat(rval);
			return bl.compareTo(br) == 0;
		} else if(rval instanceof BigInteger && lval instanceof BigInteger) {		
			BigInteger bl = (BigInteger) lval;
			BigInteger br = (BigInteger) rval;
			return bl.compareTo(br) == 0;			
		} else {
			runtimeError("Invalid expression: " + lval + " / "
					+ rval, expr);
			return null;
		}	
	}
	
	private Object evaluate(GreaterThan expr, HashMap<String,Object> store) {
		Object lval = evaluate(expr.getLeftExpr(),store);
		Object rval = evaluate(expr.getRightExpr(),store);
		
		if(rval instanceof BigRational || lval instanceof BigRational) {
			BigRational bl = bigrat(lval);
			BigRational br = bigrat(rval);
			return bl.compareTo(br) == 1;
		} else if(rval instanceof BigInteger && lval instanceof BigInteger) {		
			BigInteger bl = (BigInteger) lval;
			BigInteger br = (BigInteger) rval;
			return bl.compareTo(br) == 1;			
		} else {
			runtimeError("Invalid numerical comparison: " + lval + " > "
					+ rval, expr);
			return null;
		}
	}
	
	private Object evaluate(GreaterThanEquals expr, HashMap<String,Object> store) {
		Object lval = evaluate(expr.getLeftExpr(),store);
		Object rval = evaluate(expr.getRightExpr(),store);
		
		if(rval instanceof BigRational || lval instanceof BigRational) {
			BigRational bl = bigrat(lval);
			BigRational br = bigrat(rval);
			return bl.compareTo(br) >= 0;
		} else if(rval instanceof BigInteger && lval instanceof BigInteger) {		
			BigInteger bl = (BigInteger) lval;
			BigInteger br = (BigInteger) rval;
			return bl.compareTo(br) >= 0;			
		} else {
			runtimeError("Invalid numerical comparison: " + lval + " >= "
					+ rval, expr);
			return null;
		}
	}
	
	private Object evaluate(Constant expr, HashMap<String,Object> store) {		
		return expr.value();
	}
	
	private Object evaluate(Invoke expr, HashMap<String, Object> store) {
		Function function = expr.getFunction();
		List<Object> argvals = new ArrayList<Object>();

		for (Expression e : expr.getArguments()) {
			argvals.add(evaluate(e,store));
		}

		if (function != null) {
			return evaluate(function, argvals);
		} else {
			runtimeError("unable to find method \"" + expr.getName() + "\"",
					expr);
			return null;
		}
	}
	
	private Object evaluate(LengthOf lo, HashMap<String,Object> store) {
		Object arr = evaluate(lo.getExpr(),store);
		checkList(arr,lo.getExpr());
		return BigInteger.valueOf(((Object[])arr).length);
	}
	
	private Object evaluate(LessThan expr, HashMap<String,Object> store) {
		Object lval = evaluate(expr.getLeftExpr(),store);
		Object rval = evaluate(expr.getRightExpr(),store);
		
		if(rval instanceof BigRational || lval instanceof BigRational) {
			BigRational bl = bigrat(lval);
			BigRational br = bigrat(rval);
			return bl.compareTo(br) < 0;
		} else if(rval instanceof BigInteger && lval instanceof BigInteger) {		
			BigInteger bl = (BigInteger) lval;
			BigInteger br = (BigInteger) rval;
			return bl.compareTo(br) < 0;			
		} else {
			runtimeError("Invalid numerical comparison: " + lval + " < "
					+ rval, expr);
			return null;
		}
	}
	
	private Object evaluate(LessThanEquals expr, HashMap<String,Object> store) {
		Object lval = evaluate(expr.getLeftExpr(),store);
		Object rval = evaluate(expr.getRightExpr(),store);
		if(rval instanceof BigRational || lval instanceof BigRational) {
			BigRational bl = bigrat(lval);
			BigRational br = bigrat(rval);
			return bl.compareTo(br) <= 0;
		} else if(rval instanceof BigInteger && lval instanceof BigInteger) {		
			BigInteger bl = (BigInteger) lval;
			BigInteger br = (BigInteger) rval;
			return bl.compareTo(br) <= 0;			
		} else {
			runtimeError("Invalid numerical comparison: " + lval + " < "
					+ rval, expr);
			return null;
		}
	}
	
	private Object evaluate(ListAccess expr, HashMap<String,Object> store) {
		Object _src = evaluate(expr.source(),store);
		Object _idx = evaluate(expr.index(),store);
		checkList(_src,expr);
		Object[] src = (Object[]) _src;		
		BigInteger bi = checkInteger(_idx,expr);
		int idx;					
		idx = bi.intValue();
		if (!bi.equals(BigInteger.valueOf(idx))) {
			runtimeError("index too large for list access", expr);
		}								
		if(idx < 0 || idx >= src.length) {
			runtimeError("index out of bounds",expr);
		}
		return src[idx];
	}
	
	private Object evaluate(Mul expr, HashMap<String,Object> store) {
		Object lval = evaluate(expr.getLeftExpr(),store);
		Object rval = evaluate(expr.getRightExpr(),store);
		
		if(rval instanceof BigRational || lval instanceof BigRational) {
			BigRational bl = bigrat(lval);
			BigRational br = bigrat(rval);
			return bl.multiply(br);
		} else if(rval instanceof BigInteger && lval instanceof BigInteger) {		
			BigInteger bl = (BigInteger) lval;
			BigInteger br = (BigInteger) rval;
			return bl.multiply(br);			
		} else {
			runtimeError("Invalid expression: " + lval + " * "
					+ rval, expr);
			return null;
		}		
	}
	
	private Object evaluate(Negate expr, HashMap<String,Object> store) {
		Object o = evaluate(expr.getExpr(),store);
		checkInteger(o,expr.getExpr());
		Integer i = (Integer) o;
		return -i;
	}
	
	private Object evaluate(Not expr, HashMap<String,Object> store) {
		Object o = evaluate(expr.getCondition(),store);
		checkBoolean(o,expr.getCondition());
		return !((Boolean)o);
	}
	
	private Object evaluate(NotEquals expr, HashMap<String,Object> store) {
		// The order of execution here is back-to-front, but it
		// doesn't matter in the flow-chart language.  Right?
		Object lval = evaluate(expr.getLeftExpr(),store);
		Object rval = evaluate(expr.getRightExpr(),store);
		if(rval instanceof BigRational || lval instanceof BigRational) {
			BigRational bl = bigrat(lval);
			BigRational br = bigrat(rval);
			return bl.compareTo(br) != 0;
		} else if(rval instanceof BigInteger && lval instanceof BigInteger) {		
			BigInteger bl = (BigInteger) lval;
			BigInteger br = (BigInteger) rval;
			return bl.compareTo(br) != 0;			
		} else {
			runtimeError("Invalid expression: " + lval + " / "
					+ rval, expr);
			return null;
		}		
	}
	
	private Object evaluate(Or expr, HashMap<String,Object> store) {
		Object lval = evaluate(expr.getLeftExpr(),store);
		Object rval = evaluate(expr.getRightExpr(),store);
		checkBoolean(lval,expr.getLeftExpr());
		checkBoolean(rval,expr.getRightExpr());
		return ((Boolean)lval) || ((Boolean)rval);
	}
	
	private Object evaluate(Sub expr, HashMap<String,Object> store) {
		Object lval = evaluate(expr.getLeftExpr(),store);
		Object rval = evaluate(expr.getRightExpr(),store);
		if(rval instanceof BigRational || lval instanceof BigRational) {
			BigRational bl = bigrat(lval);
			BigRational br = bigrat(rval);
			return bl.subtract(br);
		} else if(rval instanceof BigInteger && lval instanceof BigInteger) {		
			BigInteger bl = (BigInteger) lval;
			BigInteger br = (BigInteger) rval;
			return bl.subtract(br);			
		} else {
			runtimeError("Invalid expression: " + lval + " - "
					+ rval, expr);
			return null;
		}		
	}
	
	private Object evaluate(Variable expr, HashMap<String,Object> store) {
		String var = expr.getVariable();
		if (store.keySet().contains(var)) {
			return store.get(var);
		} else {
			runtimeError("variable " + var + " is used before being defined",expr);
			return null;
		}
	}
	
	private BigInteger checkInteger(Object o, SyntacticElement elem) {
		if(o instanceof BigRational) {
			BigRational r = (BigRational) o;
			if(r.denominator().equals(BigInteger.ONE)) {
				return r.numerator();
			}			
		} else if(o instanceof BigInteger) {
			return (BigInteger) o;
		}
		runtimeError("integer expected, got " + o,elem);
		return null;
	}
	
	private void checkBoolean(Object o, SyntacticElement elem) {
		if(!(o instanceof Boolean)) {
			runtimeError("boolean expected",elem);
		}
	}
	
	private void checkList(Object o, SyntacticElement elem) {
		if(!(o instanceof Object[])) {
			runtimeError("list expected",elem);
		}
	}
	
	private void checkSubtype(Object o, Type t, SyntacticElement elem) {
		
	}
	
	private Type typeof(Expression e) {
		TypeAttr t = (TypeAttr) e.attribute(TypeAttr.class);
		if(t == null) {
			runtimeError("internal failure --- expression has not been typed",e);
		}
		return t.type();
	}
	
	private BigRational bigrat(Object o) {
		if(o instanceof BigInteger) {
			return new BigRational((BigInteger)o);			
		} else {
			return (BigRational)o;
		}
	}
	
	private static void runtimeError(String msg, SyntacticElement elem) {
		int start = -1;
		int end = -1;
		String filename = null;
		
		SourceAttr attr = (SourceAttr) elem.attribute(SourceAttr.class);
		if(attr != null) {
			start=attr.start();
			end=attr.end();
			filename = attr.filename();
		}
		
		throw new StuckError(msg, filename, start, end);
	}
}
