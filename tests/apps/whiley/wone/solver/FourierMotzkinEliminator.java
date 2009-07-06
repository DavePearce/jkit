package wone.solver;

import java.util.*;
import java.math.*;

import wone.lang.*;
import wone.util.Pair;
import wone.util.Triple;

/**
 * <p>
 * This algorithm is based on the well-known fourier-motzkin elimination
 * procedure. See <a
 * href="http://en.wikipedia.org/wiki/Fourier-Motzkin_elimination">Wikipedia</a>
 * for more information on this.
 * </p>
 * <p>
 * <b>NOTE:</b> This code is unsound in the presence of non-linear equations.
 * <b>ALSO:</b> I have made no effort thus far to optimise this implementation,
 * and it is currently extremly inefficient.
 * </p>
 * 
 * @author djp
 * 
 */
public final class FourierMotzkinEliminator {
	
	public boolean checkUnsatisfiable(ArrayList<Literal> literals) {
		// This is where all the action happens.
				
		ArrayList<Inequality> worklist = generateEquations(literals);		
		
		if(worklist == null) { 
			return true;
		}
		
		while(!worklist.isEmpty()) {			
			String v = select(worklist);
			//debug(worklist);
			worklist = eliminate(v,worklist);
			if(worklist == null) {				
				// null signals a contradiction was encountered.				
				return true;
			}							
		}								
		
		// if we get here, then we did not find any contradiction during
		// elimination.
		return false;
	}
	
	private ArrayList<Inequality> generateEquations(ArrayList<Literal> literals) {		
		ArrayList<Inequality> equations = new ArrayList<Inequality>();
		
		// First, generate list of equations.
		for(Literal i : literals) {			
			if(i.isFalse()) {
				return null; // trivially unsat
			} else if (i instanceof Inequality) {				
				Inequality eq = ((Inequality) i).normalise();
				if(eq.isFalse()) {
					return null; // also trivially unsat
				} else if(!eq.isTrue()) {					
					equations.add(eq);					
				} 
			} 
		}
			
		return equations;
	}	
	
	private String select(ArrayList<Inequality> is) {
		for(Inequality i : is) {
			Set<String> fvs = i.lhs().freeVariables();
			fvs.addAll(i.rhs().freeVariables());
			if(!fvs.isEmpty()) {
				return fvs.iterator().next();
			}
		}
		
		// should be unreachable
		throw new RuntimeException(
				"internal error during Fourier-Motzkin Elimination --- cannot select variable");
	}
	
	private ArrayList<Inequality> eliminate(String var, ArrayList<Inequality> eqs) {
		ArrayList<Triple<Polynomial, BigInteger, Boolean>> lowerBounds = new ArrayList();
		ArrayList<Triple<Polynomial, BigInteger, Boolean>> upperBounds = new ArrayList();		
		ArrayList<Inequality> rs = new ArrayList<Inequality>();						
		
		for (Inequality ieq : eqs) {
			if (ieq.rhs().freeVariables().contains(var)) {				
				eliminate(var, ieq, lowerBounds, upperBounds);				
			} else {
				rs.add(ieq);
			}
		}
				
		for (Triple<Polynomial, BigInteger, Boolean> p1 : lowerBounds) {
			for (Triple<Polynomial, BigInteger, Boolean> p2 : upperBounds) {
				Polynomial lb = p1.first().multiply(p2.second());
				Polynomial ub = p2.first().multiply(p1.second());
				Inequality i;
				
				if(p1.third() || p2.third()) {
					i = new Inequality(false,ub,lb).normalise();
				} else {
					i = new Inequality(true,lb, ub).normalise();
				}								
											
				if(i.isFalse()) {
					return null; // contradiction detected!
				} else if(!i.isTrue()) {				
					rs.add(i);
				}
			}
		}		
		
		return rs;
	}
		
	/**
	 * The purpose of this method is to eliminate a variable from an inequality.
	 * In the case of non-linear inequalities, it will fail however. For
	 * example, x^2 < 2. The method adds the lowerbounds and upperbounds which
	 * are found, and a boolean value is used to indicate whether these are
	 * strict or non-strict.
	 * 
	 * @param var
	 * @param ieq
	 * @param lowerBounds
	 * @param upperBounds
	 * @param worklist
	 * @return
	 */
	private void eliminate(String var, Inequality ieq,
			ArrayList<Triple<Polynomial,BigInteger,Boolean>> lowerBounds,			
			ArrayList<Triple<Polynomial,BigInteger,Boolean>> upperBounds) {
		// To do this, I start by shifting all var terms to the lower position,
		// and everything else to the upper position.					
		
		ieq = ieq.rearrange(var);				
				
		// Now, we factorise the lower bound for the variable in question.
		// Notice that we know the remainder will be zero by construction.
		Polynomial factor = ieq.lhs().factoriseFor(var).first();						

		if(!factor.isConstant()) {
			// In this case, the factor is not a constant which indicates a
			// non-linear constraint. At the moment, there's nothing we can
			// do about this so we silently drop it ... making the system
			// unsound.
			return;
		}

		BigInteger constant = factor.constant();

		// Finally, we need to look at the sign of the coefficient to
		// determine whether or not we have an upper or lower bound.			

		if (constant.compareTo(BigInteger.ZERO) < 0) {
			if(ieq.sign()) {
				lowerBounds.add(new Triple(ieq.rhs().negate(), constant
						.negate(), false));	
			} else {
				upperBounds.add(new Triple(ieq.rhs().negate(), constant
						.negate(), true));
			}				
		} else if (constant.compareTo(BigInteger.ZERO) > 0) {
			if(ieq.sign()) {
				upperBounds.add(new Triple(ieq.rhs(), constant,
						false));
			} else {
				lowerBounds.add(new Triple(ieq.rhs(), constant,
						true));
			}
		}				
	}
	
	private static void debug(ArrayList<Inequality> ieqs) {		
		boolean firstTime = true;
		for(Equation i : ieqs) {
			if(!firstTime) {
				System.err.print(" && ");
			}
			firstTime=false;
			System.err.print(i);
		}
		System.err.println();
	}
}

