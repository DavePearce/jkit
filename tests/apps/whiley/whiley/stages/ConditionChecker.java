package whiley.stages;

import java.util.*;
import java.math.BigInteger;

import whiley.ast.*;
import whiley.ast.attrs.PostConditionAttr;
import whiley.ast.attrs.PreConditionAttr;
import whiley.ast.attrs.SourceAttr;
import whiley.ast.attrs.SyntacticElement;
import whiley.ast.exprs.*;
import whiley.ast.stmts.*;
import whiley.ast.types.Type;
import whiley.util.*;
import wone.lang.*;
import wone.solver.*;
import whiley.ast.Function;
import whiley.util.BigRational;

/**
 * <p>
 * Condition checking is the process of, for each method, assuming the
 * pre-condition for the method and ensuring its post-condition holds, including
 * those of any statements it contains. Thus, it is a more detailed form of
 * checking than simple type checking. For example, consider the following
 * method:
 * </p>
 * 
 * <pre>
 * int min(int x, int y) requires x &gt; 1, ensures $ &gt; 2
 * {
 *   return x;
 * }
 * </pre>
 * 
 * <p>
 * This method would pass <i>type checking</i>, since the returned value is of
 * type <code>int</code>. However, it will fail condition checking, since the
 * method does not always ensure that the return value is greater than 2.
 * </p>
 * <p>
 * In order to perform the detailed checking required, this stages employs an <a
 * href="http://en.wikipedia.org/wiki/Satisfiability_Modulo_Theories">SMT solver</a>.
 * This component accepts formulas written in first-order logic and determines
 * whether or not they are satisfiable.
 * </p>
 * 
 * @author djp
 */
public class ConditionChecker {
	private Solver solver;
	
	public ConditionChecker(Solver solver) {
		this.solver = solver;
	}
	
	/**
	 * The purpose of this method is to check that the method's postcondition,
	 * and that of any statements it contains, is implied by the precondition.
	 * 
	 * @param f
	 *             Method in question.
	 * @return
	 */
	public void verify(Function f) {
		Formula condition; 
		
		if (f.getPrecondition() != null) {
			Pair<Formula,Formula> pc = convert(f.getPrecondition());			
			condition = pc.first().and(pc.second());			
		} else {
			condition = new Bool(true);
		}
		
		for(Stmt s : f.getStatements()) {
			check(s,condition);
			condition = propagate(s,condition,f);			
		}				
	}
	
	/**
	 * <p>
	 * The purpose of this method is to check that the requirements of a
	 * statement are satisfied by the pre-condition for that statement. For
	 * example, consider the following method:
	 * </p>
	 * 
	 * <pre>
	 * int f(int x, int y) requires y &gt;= 0 {
	 *  x = x + 1;
	 *  return x / y; 
	 * }
	 * </pre>
	 * 
	 * <p>
	 * Here, we must check the requirement that <code>y!=0</code> holds before
	 * the division operation. Thus, we must determine the precondition which
	 * holds before that statement (by propagating from the methods
	 * pre-condition) and then call this method to check that this implies
	 * <code>y!=0</code> (which it doesn't in this case).</p>
	 * 
	 * @param s
	 * @param preCondition
	 */
	protected void check(Stmt s, Formula preCondition) {
		s.attributes().add(new PreConditionAttr(preCondition));
		if (s instanceof Skip || s instanceof Read
				|| s instanceof VarDecl) {
			// these statements can have no requirements
		} else if(s instanceof Print) {
			check(((Print)s).getExpression(),preCondition);
		} else if (s instanceof Assign) {
			check(((Assign)s).lhs(),preCondition);
			check(((Assign)s).rhs(),preCondition);			
		} else if (s instanceof IfElse) {
			check(((IfElse) s).getCondition(), preCondition);	
		} else if (s instanceof While) {
			check(((While) s).condition(), preCondition);	
		} else if (s instanceof Return) {
			check(((Return)s).getExpression(),preCondition);			
		} else if (s instanceof Assertion) {
			check((Assertion) s, preCondition);
		} else if (s instanceof Invoke) {
			check((Invoke) s, preCondition);
		} else {
			syntaxError("Unknown statement encountered: ", s);
		}
	}	
	
	private void check(Assertion stmt, Formula preCondition) {
		check(stmt.getExpression(),preCondition);
		Pair<Formula,Formula> assertCond = convert(stmt.getExpression());
		preCondition = preCondition.and(assertCond.second());
		Formula vc = preCondition.and(assertCond.first().not());				
		System.out.println("VC: " + vc);
		if(!solver.checkUnsatisfiable(vc)) {
			syntaxError("Assertion does not hold.", stmt);
		}		
	}
	
	protected static int invoke_label = 0;
	private void check(Invoke ivk, Formula preCondition) {
		// First, we'll check the requirements of the argument expressions.
		List<Expression> args = ivk.getArguments(); 
		for(Expression e : args) {
			check(e,preCondition);					
		}
		
		// Now, check the requirements for the method itself.		
		Function f = ivk.getFunction();			
		
		if(f.getPrecondition() != null) {
			HashMap<String,String> binding = new HashMap<String,String>();
			List<Pair<Type,String>> params = f.getParameterTypes();
			Formula requirements = new Bool(true);

			for(int i=0;i!=params.size();++i) {			
				Pair<Type,String> p = params.get(i);
				// I use the label here to ensure that constraints on different
				// invocations of the same method do not overlap.
				String pname = "$" + ivk.getName() + "$" + p.second() + invoke_label++;
				binding.put(p.second(), pname);
				Pair<Rational,Formula> arg = convert(args.get(i));
				Polynomial den = arg.first().denominator();
				Polynomial num = arg.first().numerator();
				Formula tmp = new Equality(true,new Polynomial(
						pname).multiply(den), num);
				preCondition = preCondition.and(tmp);
				preCondition = preCondition.and(arg.second());
			}

			Pair<Formula, Formula> precond = convert(f.getPrecondition());

			preCondition = preCondition.and(precond.second()).substitute(
					binding);
			requirements = requirements.and(precond.first()).substitute(
					binding);

			Formula vc = preCondition.and(requirements.not());						
			
			if (!solver
					.checkUnsatisfiable(vc)) {
				syntaxError("Method precondition not satisfied.", ivk);
			}
		}

		
	}
	
	/**
	 * <p>
	 * The purpose of this method is to check the requirements that must hold
	 * for a given expression to make sense. For example, consider an expression
	 * <code>y/z</code>. A requirement for this expression to make sense is
	 * that <code>z!=0</code>.
	 * </p>
	 * 
	 * @param expression
	 *            expression to generate requirements for.
	 * @param preCondition
	 *            A formula capturing the known facts which hold true before the
	 *            expression is evaluated.
	 * @return
	 */
	public void check(Expression expression, Formula preCondition) {		
		if (expression instanceof Variable || expression instanceof Constant) {
			// nothing to do
		} else if(expression instanceof ListVal) {
			ListVal lv = (ListVal) expression;
			for(Expression e : lv.getValues()) {
				check(e,preCondition);
			}
		} else if(expression instanceof LengthOf) {
			LengthOf lo = (LengthOf) expression;
			check(lo.getExpr(),preCondition);
		} else if(expression instanceof ListAccess) {
			ListAccess la = (ListAccess) expression;
			check(la.index(),preCondition);
			check(la.source(),preCondition);
			// Now, check that the index is less than the source length and
			// greater than zero.
			Pair<Rational,Formula> idx = convert(la.index());
			Pair<Rational,Formula> src = convert(la.source());
			Polynomial num = idx.first().numerator();
			Polynomial den = idx.first().denominator();
			String lenVar = freshVar();
			String srcVar = src.first().variable();
			
			Formula accessCond = new Inequality(true, Polynomial.ZERO, num)
					.and(new Inequality(false, new Polynomial(lenVar).multiply(den), num));												
			
			preCondition = preCondition.and(idx.second());
			preCondition = preCondition.and(src.second());
			preCondition = preCondition.and(new wone.lang.Function(true,
					"length", lenVar, srcVar));
			
			Formula vc = preCondition.and(accessCond.not());						
			
			if (!solver.checkUnsatisfiable(vc)) {
				syntaxError("array access out-of-bounds is possible",
						expression);
			}
		} else if(expression instanceof Negate) {					
			Negate ne = (Negate) expression;
			check(ne.getExpr(),preCondition);
		} else if(expression instanceof Not) {
			Not ne = (Not) expression;
			check(ne.getCondition(),preCondition);
		} else if(expression instanceof Invoke) {
			check((Invoke) expression,preCondition);								
		} else if(expression instanceof BinOp) {
			BinOp be = (BinOp) expression;
			check(be.getLeftExpr(),preCondition);
			check(be.getRightExpr(),preCondition);
			
			if(expression instanceof Div) {
				// The following encodes the requirement that a division cannot have
				// a zero right-hand side
				Pair<Rational,Formula> rhs = convert(be.getRightExpr());
				Polynomial den = rhs.first().denominator();
				Polynomial num = rhs.first().numerator();
				Formula divZeroCond = new Inequality(false,num,Polynomial.ZERO)
						.or(new Inequality(false,Polynomial.ZERO,num));
				preCondition = preCondition.and(rhs.second());
				if (!solver.checkUnsatisfiable(preCondition.and(divZeroCond
						.not()))) {
					syntaxError("division by zero is possible",expression);
				}
			}
		} else {
			syntaxError("unknown expression encountered",expression);			
		}
	}
	
	/**
	 * <p>
	 * The purpose of this method is to check any conditions which must hold
	 * true before the statement in question, and to compute an appropriate
	 * condition which must hold true after this statement has executed.
	 * </p>
	 * 
	 * @param s
	 *            Statement in question.
	 * @param preCondition
	 *            condition which holds immediately before this statement.
	 * @return
	 */
	protected Formula propagate(Stmt s, Formula preCondition, Function f) {		
		Formula postCondition;		
		if (s instanceof Skip || s instanceof Read
				|| s instanceof Invoke || s instanceof VarDecl) {
			postCondition = preCondition;
		} else if (s instanceof Assign) {
			postCondition = propagate((Assign) s, preCondition, f);
		} else if (s instanceof IfElse) {
			postCondition = propagate((IfElse) s, preCondition, f);
		} else if (s instanceof While) {
			postCondition = propagate((While) s, preCondition, f);
		} else if (s instanceof Return) {
			postCondition = propagate((Return) s, preCondition, f);
		} else if (s instanceof Assertion) {
			postCondition = propagate((Assertion) s, preCondition, f);
		} else if (s instanceof Print) {
			postCondition = propagate((Print) s, preCondition, f);
		} else {
			syntaxError("Unknown statement encountered: ",s);
			return null;
		}
		s.attributes().add(new PostConditionAttr(postCondition));
		return postCondition;
	}
	
	/**
	 * <p>
	 * The purpose of this method is to check any conditions which must hold
	 * true before the assignment, and to compute an appropriate
	 * condition which must hold after it.
	 * </p>
	 * 
	 * <p>
	 * In processing assignment, we need to eliminate all occurrences of a
	 * variable from this formula, whilst preserving all information implied
	 * about other variables. For example, consider this formula:
	 * </p>
	 * 
	 * <pre>
	 * x &lt;= y &amp;&amp; z &lt;= x
	 * </pre>
	 * 
	 * <p>
	 * Now, suppose we want to eliminate variable x. Clearly, we don't want to
	 * loose the knowledge we have that z < y must hold. Therefore, we introduce
	 * "shadow" variables to represent the original variable. For example, after
	 * eliminating x, we get the following formula:
	 * </p>
	 * 
	 * <pre>
	 * x$1 &lt;= y &amp;&amp; z &lt;= x$1
	 * </pre>
	 * 
	 * <p>
	 * Here, the variable <code>x$1</code> is a "shadow variable" which is
	 * used to ensure information is not lost.
	 * </p>
	 * 
	 * <p>
	 * Shadow variables are treated slightly differently from normal variables,
	 * in that we assume one cannot access them directly within the program.
	 * Instead, all accesses must go through the non-shadow variables. This can
	 * expose some opportunities for simplification, since shadow variables
	 * which cannot be "reached" from non-shadow variables can be eliminated
	 * altogether.
	 * </p>
	 * @return
	 */
	private static int shadow_label = 0;
	private Formula propagate(Assign s, Formula preCondition, Function f) {	
		String var = "";
		if(s.lhs() instanceof Variable) {
			var = ((Variable)s.lhs()).getVariable();
		} else {
			return preCondition; // hack for now.
		}
		String shadowVariable = "$" + var + shadow_label++;
		HashMap<String,String> binding = new HashMap<String,String>();
		binding.put(var,shadowVariable);		
		Formula post = preCondition.substitute(binding);
		
		if(s.rhs() instanceof Condition) {
			Pair<Formula,Formula> rhs = convert((Condition)s.rhs());						
			// Could do with "iff" predicate kind here I think.
			post = post.and(rhs.second());
			post = post.and(rhs.first().or(new Atom(false,var)));
			post = post.and(rhs.first().not().or(new Atom(true,var)));
		} else if (s.rhs() instanceof Constant
				&& ((Constant) s.rhs()).value() instanceof Boolean) {
			// This is a special case for dealing with assignment of boolean
			// values.
			Boolean sign = (Boolean) ((Constant)s.rhs()).value();
			post = post.and(new Atom(sign,var));
		} else {
			Pair<Rational,Formula> rhs = convert(s.rhs());		
			Polynomial num = rhs.first().numerator().substitute(binding);
			Polynomial den = rhs.first().denominator().substitute(binding);
			Polynomial lhs = new Polynomial(var).multiply(den);
			post = post.and(new Equality(true,lhs,num));
			post = post.and(rhs.second().substitute(binding));
		}
		
		// Now, we could attempt to simplify the formula here, since it will
		// eventually get full of crud that is no longer accessible.
		
		return post;		
	}
	
	/**
	 * <p>
	 * The purpose of this method is to check any conditions which must hold
	 * true before the if statement, and to compute an appropriate condition
	 * which must hold after it.
	 * </p>
	 */ 
	private Formula propagate(IfElse s, Formula preCondition, Function f) {
		// First, create two near identical pre-conditions which differ in that
		// one represents the case when the condition is true, whilst the other
		// represents the case when it is false.
		Pair<Formula,Formula> condition = convert(s.getCondition());
		
		preCondition = preCondition.and(condition.second());
		
		Formula trueCond = preCondition.and(condition.first());
		Formula falseCond = preCondition.and(condition.first().not());
		
		for (Stmt st : s.getTrueBranch()) {
			check(st,trueCond);
			trueCond = propagate(st, trueCond, f);			
		}

		if(s.getFalseBranch() != null) {
			for (Stmt st : s.getFalseBranch()) {
				check(st,falseCond);
				falseCond = propagate(st, falseCond, f);			
			}		
		}
		
		return trueCond.or(falseCond);
	}
	
	/**
	 * <p>
	 * The purpose of this method is to check any conditions which must hold
	 * true before the while statement, and to compute an appropriate condition
	 * which must hold after it. For the loop body, we check that the loop
	 * invariant holds at the beginning of the loop body, and that it holds at
	 * the end of the body as well (i.e. it is reestablished).
	 * </p>
	 */ 
	private Formula propagate(While s, Formula preCondition, Function f) {
		// First, create two near identical pre-conditions which differ in that
		// one represents the case when the condition is true, whilst the other
		// represents the case when it is false.
		Pair<Formula,Formula> condition = convert(s.condition());		
		Pair<Formula,Formula> invariant = null;
		preCondition = preCondition.and(condition.second());
		Formula bodyCond = preCondition.and(condition.first());
		Formula postCond = preCondition.and(condition.first().not());				
		
		if(s.invariant() != null) {
			invariant = convert(s.invariant());
			bodyCond = bodyCond.and(invariant.second());
			Formula vc = bodyCond.and(invariant.first().not());
			if (!solver.checkUnsatisfiable(vc)) {
				syntaxError("Loop invariant does not hold on entry.", s
						.invariant());
			}
		}
		
		for (Stmt st : s.body()) {
			check(st,bodyCond);
			bodyCond = propagate(st, bodyCond, f);			
		}

		if(invariant != null) {			
			Formula vc = bodyCond.and(invariant.first().not());
			if (!solver.checkUnsatisfiable(vc)) {
				syntaxError("Loop invariant is not re-established.", s
						.invariant());
			}
		}
		
		return postCond;
	}
	
	/**
	 * <p>
	 * The purpose of this method is to check any conditions which must hold
	 * true before the return statement, and to compute an appropriate condition
	 * which must hold after it.
	 * </p>
	 */ 
	private Formula propagate(Return s, Formula preCondition, Function f) {		
		Polynomial lhs = new Polynomial("$");
		Pair<Rational,Formula> rhs = convert(s.getExpression());
		Rational r = rhs.first();				
		
		Formula cond = preCondition.and(new Equality(true,lhs.multiply(r
				.denominator()), r.numerator()));
		cond = cond.and(rhs.second());
		s.attributes().add(new PostConditionAttr(cond));
		
		// At this point, we need to check whether or not the post condition for
		// the method in question is satisfied.
		
		if(f.getPostcondition() != null) {				
			Pair<Formula,Formula> postCondition = convert(f.getPostcondition());			
			cond = cond.and(postCondition.second());
			Formula vc = cond.and(postCondition.first().not());											
			if(!solver.checkUnsatisfiable(vc)) {			
				syntaxError("post-condition not satisfied (requires "
						+ postCondition + ")", s);
			}			
		}
		
		return new Bool(false); // nothing is true after this
	}
	
	/**
	 * <p>
	 * The purpose of this method is to check any conditions which must hold
	 * true before the assert statement (including the asserted expression
	 * itself), and to compute an appropriate condition which must hold after
	 * it.
	 * </p>
	 */ 
	private Formula propagate(Assertion s, Formula preCondition, Function f) {		
		return preCondition;
	}		
	
	/**
	 * <p>
	 * The purpose of this method is to check any conditions which must hold
	 * true before the print statement, and to compute an appropriate condition
	 * which must hold after it.
	 * </p>
	 */ 
	protected Formula propagate(Print s, Formula preCondition, Function f) {
		// no constraints for print stmt ...
		return preCondition;
	}	
	
	/**
     * <p>
     * This method constructs a Formula from an Expression by expanding it using
     * the appropriate distributive laws. The reason for this is that Formulas
     * are simpler for the solver to reason about.
     * </p>
     * 
     * <p>
     * For example, the following expression:
     * </p>
     * 
     * <pre>
     * (a &lt; b &amp;&amp; c != d) || c &gt;= d
     * </pre>
     * 
     * is transformed into the following:
     * 
     * <pre>
     * (a &lt; b || d &lt; c || c == d) &amp;&amp; (c &lt; d || d &lt; c || c == d)
     * </pre>
     * 
     * <p>
     * Observe that, in the worst case, this transformation can produce an
     * expression which is exponentially larger. However, this is unlikely in
     * practice. Also, a Formula is essentially an expression in <a
     * href="http://en.wikipedia.org/wiki/Conjunctive_normal_form">Conjunctive
     * Normal Form (CNF)</a>.
     * </p>
     * 
     * @param cond
     * @return a pair of formulas. The first represents the original condition,
     *         whilst the second represents the constraints implied by that
     *         condition.
     */
	protected Pair<Formula,Formula> convert(Condition cond) {						
		if(cond instanceof And) {
			And ae = (And) cond;
			Pair<Formula,Formula> lhs = convert(ae.getLeftExpr());
			Pair<Formula,Formula> rhs = convert(ae.getRightExpr());
			return new Pair(lhs.first().and(rhs.first()), lhs.second().and(
					rhs.second()));					
		} else if(cond instanceof Or) {
			Or oe = (Or) cond;	
			Pair<Formula,Formula> lhs = convert(oe.getLeftExpr());
			Pair<Formula,Formula> rhs = convert(oe.getRightExpr());
			return new Pair(lhs.first().or(rhs.first()), lhs.second().and(
					rhs.second()));		
		} else if(cond instanceof LessThan) {
			LessThan lt = (LessThan) cond;
			Pair<Rational,Formula> lhs = convert(lt.getLeftExpr());
			Pair<Rational,Formula> rhs = convert(lt.getRightExpr());			
			Formula tmp = new Inequality(false,rhs.first(), lhs.first());
			return new Pair(tmp,lhs.second().and(rhs.second()));
		} else if(cond instanceof LessThanEquals) {
			LessThanEquals lt = (LessThanEquals) cond;
			Pair<Rational,Formula> lhs = convert(lt.getLeftExpr());
			Pair<Rational,Formula> rhs = convert(lt.getRightExpr());
			Formula tmp = new Inequality(true,lhs.first(), rhs.first());
			return new Pair(tmp,lhs.second().and(rhs.second()));
		} else if(cond instanceof GreaterThan) {
			GreaterThan gt = (GreaterThan) cond;
			Pair<Rational,Formula> lhs = convert(gt.getLeftExpr());
			Pair<Rational,Formula> rhs = convert(gt.getRightExpr());			
			Formula tmp = new Inequality(false, lhs.first(), rhs.first());
			return new Pair(tmp,lhs.second().and(rhs.second()));
		} else if(cond instanceof GreaterThanEquals) {
			GreaterThanEquals gt = (GreaterThanEquals) cond;
			Pair<Rational,Formula> lhs = convert(gt.getLeftExpr());
			Pair<Rational,Formula> rhs = convert(gt.getRightExpr());
			Formula tmp = new Inequality(true,rhs.first(),lhs.first());
			return new Pair(tmp,lhs.second().and(rhs.second()));
		} else if(cond instanceof NotEquals) {
			NotEquals ne = (NotEquals) cond;
			Pair<Rational,Formula> lhs = convert(ne.getLeftExpr());
			Pair<Rational,Formula> rhs = convert(ne.getRightExpr());				
			Formula tmp = new Equality(false,lhs.first(), rhs
					.first());
			return new Pair(tmp,lhs.second().and(rhs.second()));
		} else if(cond instanceof Equals) {
			Equals ne = (Equals) cond;
			Pair<Rational,Formula> lhs = convert(ne.getLeftExpr());
			Pair<Rational,Formula> rhs = convert(ne.getRightExpr());			
			Formula tmp = new Equality(true,lhs.first(), rhs
					.first());
			return new Pair(tmp,lhs.second().and(rhs.second()));
		} else if (cond instanceof Constant) {
			Constant c = (Constant) cond;
			Object o = c.value();
			if(o instanceof Boolean) {
				return new Pair(new Bool((Boolean)o),new Bool(true));
			} else {
				throw new RuntimeException("internal error, invalid expression: "
						+ cond);
			}
		} else if (cond instanceof Variable) {
			Variable le = (Variable) cond;
			return new Pair(new Atom(true, le.getVariable()), new Bool(true));
		} else if (cond instanceof Not) {
			Not ne = (Not) cond;
			Pair<Formula,Formula> r = convert(ne.getCondition());
			return new Pair(r.first().not(),r.second());
		} else if (cond == null) {
			return new Pair(new Bool(true),new Bool(true));
		} else {
			throw new RuntimeException("internal error, unknown expression: "
					+ cond);
		}		
	}
	
	/**
	 * <p>This method computes a polynomial representing the expression in
	 * question.</p>
	 * 
	 * @param e
	 * @return
	 */		
	protected Pair<Rational,Formula> convert(Expression e) {
		Rational poly;
		Formula constraints;
		if(e instanceof Variable) {
			poly = new Rational(((Variable)e).getVariable());
			constraints = new Bool(true);
		} else if(e instanceof Constant) {
			Object c = ((Constant) e).value();			
			constraints = new Bool(true);
			if(c instanceof BigInteger) {
				BigInteger i = (BigInteger) c;
				poly = new Rational(i);	
			} else if(c instanceof BigRational){
				BigRational r = (BigRational) c;
				poly = new Rational(r.numerator(),r.denominator());
			} else {
				throw new RuntimeException("internal error, unknown expression: "
						+ e);
			}
		} else if(e instanceof Add) {
			Add ae = (Add) e;
			Pair<Rational,Formula> lhs = convert(ae.getLeftExpr());
			Pair<Rational,Formula> rhs = convert(ae.getRightExpr());
			poly = lhs.first().add(rhs.first());
			constraints = lhs.second().and(rhs.second());			
		} else if(e instanceof Sub) {
			Sub ae = (Sub) e;
			Pair<Rational,Formula> lhs = convert(ae.getLeftExpr());
			Pair<Rational,Formula> rhs = convert(ae.getRightExpr());
			poly = lhs.first().subtract(rhs.first());					
			constraints = lhs.second().and(rhs.second());
		} else if(e instanceof Mul) {
			Mul ae = (Mul) e;
			Pair<Rational,Formula> lhs = convert(ae.getLeftExpr());
			Pair<Rational,Formula> rhs = convert(ae.getRightExpr());
			poly = lhs.first().multiply(rhs.first());						
			constraints = lhs.second().and(rhs.second());
		} else if(e instanceof Div) {
			Div ae = (Div) e;			
			Pair<Rational,Formula> lhs = convert(ae.getLeftExpr());
			Pair<Rational,Formula> rhs = convert(ae.getRightExpr());						
			// FIXME: bug here for integer division.
			poly = lhs.first().divide(rhs.first());			
			constraints = lhs.second().and(rhs.second());									
		} else if(e instanceof Negate) {
			Negate ne = (Negate) e;
			Pair<Rational,Formula> p = convert(ne.getExpr());			
			poly = p.first().negate();
			constraints = p.second();
		} else if(e instanceof Invoke) {
			Invoke ivk = (Invoke) e;
			// I use the label here to ensure that constraints on different
			// invocations of the same method do not overlap.
			String retLabel = "$" + ivk.getName() + "$" + invoke_label++;
			HashMap<String,String> binding = new HashMap<String,String>();
			binding.put("$", retLabel);			
			Pair<Formula,Formula> pc = convert(ivk.getFunction().getPostcondition());
			constraints = pc.first().and(pc.second()).substitute(binding);
			poly = new Rational(retLabel);
		} else if(e instanceof LengthOf) {
			LengthOf lo = (LengthOf) e;
			Pair<Rational,Formula> src = convert(lo.getExpr());
			constraints = src.second();
			String srcVar = src.first().variable();
			String retVar = freshVar();
			
			constraints = constraints.and(new wone.lang.Function(true, "length",
					retVar, srcVar));
			
			poly = new Rational(retVar);
		} else if(e instanceof ListAccess) {
			ListAccess la = (ListAccess) e;
			Pair<Rational,Formula> idx = convert(la.index());
			Pair<Rational,Formula> src = convert(la.source());
			constraints = idx.second().and(src.second());			
			String idxVar;
			if (idx.first().isVariable()) {
				idxVar = idx.first().variable();
			} else {
				idxVar = freshVar(); 
				Polynomial lhs = new Polynomial(idxVar).multiply(idx.first()
						.denominator());
				Polynomial rhs = idx.first().numerator();
				constraints = constraints.and(new Equality(true,lhs,rhs));
			}			
			
			String srcVar = src.first().variable();			
			String retVar = freshVar();
			
			constraints = constraints.and(new wone.lang.Function(true, "get",
					retVar, srcVar, idxVar));
			
			poly = new Rational(retVar);
			
		} else if(e instanceof ListVal) {
			ListVal lv = (ListVal) e;
			String tmpVar = freshVar();
			String lenVar = freshVar();
			constraints = new wone.lang.Function(true,"length",lenVar,tmpVar);
			constraints = constraints.and(new Equality(true,new Polynomial(lenVar), new Polynomial(
					lv.getValues().size())));
			
			int idx=0;
			for(Expression p : lv.getValues()) {				
				Pair<Rational,Formula> ps = convert(p);
				
				String idxVar = freshVar();
				constraints = constraints.and(new Equality(true,
						new Polynomial(idxVar), new Polynomial(idx)));
				constraints = constraints.and(ps.second());
				
				String retVar;
				if (ps.first().isVariable()) {
					retVar = ps.first().variable();
				} else {
					retVar = freshVar();
					Polynomial lhs = new Polynomial(retVar).multiply(ps.first()
							.denominator());
					Polynomial rhs = ps.first().numerator();
					constraints = constraints.and(new Equality(true,lhs,rhs));
				}
				
				wone.lang.Function f = new wone.lang.Function(true,"get",retVar,tmpVar,idxVar); 
				constraints = constraints.and(f);
				idx = idx + 1;
			}
			
			poly = new Rational(tmpVar);
		} else {
			throw new RuntimeException("internal error, unknown expression: "
					+ e);
		}
		return new Pair<Rational,Formula>(poly,constraints);
	}
	
	protected int fresh_label = 0;
	private String freshVar() {
		return "$" + fresh_label++;
	}
	
	private static void syntaxError(String msg, SyntacticElement elem) {
		int start = -1;
		int end = -1;
		String filename = "unknown";
		
		SourceAttr attr = (SourceAttr) elem.attribute(SourceAttr.class);
		if(attr != null) {
			start=attr.start();
			end=attr.end();
			filename = attr.filename();
		}
		
		throw new SyntaxError(msg, filename, start, end);
	}
}
