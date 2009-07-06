package wone.lang;

import java.util.*;
import java.math.*;
import wone.util.Pair;

/**
 * Provides a general class for representing polynomials. <b>Please note that
 * polynomial division and gcd is experimental and probably contains bugs</b>.
 * 
 * @author djp
 * 
 */
public final class Polynomial implements Iterable<Term> {
	public static final Polynomial MTWO = new Polynomial(-2);
	public static final Polynomial MONE = new Polynomial(-1);
	public static final Polynomial ZERO = new Polynomial(0);	
	public static final Polynomial ONE = new Polynomial(1);
	public static final Polynomial TWO = new Polynomial(2);
	public static final Polynomial THREE = new Polynomial(3);
	public static final Polynomial FOUR = new Polynomial(4);
	public static final Polynomial FIVE = new Polynomial(5);	
	public static final Polynomial TEN = new Polynomial(10);
	
	// NOTE: ZERO is represented only by the empty set of terms and not, for
	// example, as a single term with no variables and zero coefficient.
	//
	private final HashSet<Term> terms;	
	
	public Polynomial() {
		terms = new HashSet<Term>();		
	}
	
	public Polynomial(int constant) {
		terms = new HashSet<Term>();
		if(constant != 0) {
			this.terms.add(new Term(constant));
		}
	}
	
	public Polynomial(long constant) {
		terms = new HashSet<Term>();
		if(constant != 0) {
			this.terms.add(new Term(constant));
		}		
	}
	
	public Polynomial(BigInteger constant) {
		terms = new HashSet<Term>();
		if(!constant.equals(BigInteger.ZERO)) {
			this.terms.add(new Term(constant));
		}		
	}
	
	public Polynomial(String variable) {
		terms = new HashSet<Term>();
		terms.add(new Term(1,variable));
	}
	
	public Polynomial(Term term) {
		terms = new HashSet<Term>();
		if(!term.coefficient().equals(BigInteger.ZERO)) {
			this.terms.add(term);
		}
	}
	
	public Polynomial(Term... terms) {
		this.terms = new HashSet<Term>();
		for (Term t : terms) {
			if (!t.coefficient().equals(BigInteger.ZERO)) {
				this.terms.add(t);
			}
		}		
	}	
	
	public Polynomial(Set<Term> terms) {
		this.terms = new HashSet<Term>();
		for (Term t : terms) {
			if (!t.coefficient().equals(BigInteger.ZERO)) {
				this.terms.add(t);
			}
		}			
	}
	
	public Polynomial(Polynomial poly) {		
		this.terms = (HashSet<Term>) poly.terms.clone();
	}

	/* =========================================================== */
	/* ========================== ACCESSORS ====================== */
	/* =========================================================== */


	public Iterator<Term> iterator() {
		return terms.iterator();
	}
	
	public Set<Term> terms() {
		return Collections.unmodifiableSet(terms);
	}
	
	public boolean isConstant() {
		for(Term e : terms) {
			if(!e.isConstant()) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Assumes that isConstant() holds.
	 * @return
	 */
	public BigInteger constant() {
		BigInteger c = BigInteger.ZERO;
		for(Term e : terms) {
			c = c.add(e.coefficient());
		}
		return c;
	}
	
	public boolean isLinear() {
		for(Term e : terms) {
			if(e.variables().size() > 1) {
				return false;
			}
		}
		return true;
	}

	public boolean isVariable() {
		if(terms.size() != 1) {
			return false;
		}
		Term t = terms.iterator().next();
		if (t.variables().size() != 1
				|| !t.coefficient().equals(BigInteger.ONE)) {
			return false;
		}
		
		return true;
	}
		
	public String variable() {
		assert isVariable();
		return terms.iterator().next().variables().get(0);
	}
	
	public Set<String> freeVariables() {
		HashSet<String> fvs = new HashSet<String>();
		for(Term e : terms) {
			fvs.addAll(e.variables());
		}
		return fvs;
	}
	
	/* =========================================================== */
	/* ========================== ADDITION ======================= */
	/* =========================================================== */
	
	public Polynomial add(int i) {
		return add(new Term(i));
	}
	
	public Polynomial add(long i) {
		return add(new Term(i));
	}
	
	public Polynomial add(BigInteger i) {
		return add(new Term(i));
	}
	
	public Polynomial add(Term e) {
		final BigInteger zero = BigInteger.ZERO;
		
		for(Term me : terms) {
			if (me.variables().equals(e.variables())) {
				BigInteger ncoeff = me.coefficient().add(e.coefficient());
				Polynomial r = new Polynomial(this);
				r.terms.remove(me);
				if (!(ncoeff.equals(zero))) {
					r = r.add(new Term(ncoeff, me.variables()));
				}
				return r;
			}
		}		
		if(!e.coefficient().equals(zero)) {
			Polynomial r = new Polynomial(this);		
			r.terms.add(e);
			return r;		
		} else {
			return this;
		}
	}
	
	public Polynomial add(Polynomial poly) {
		Polynomial r = this;
		for(Term e : poly.terms) {
			r = r.add(e);
		}
		return r;
	}		
	
	/* =========================================================== */
	/* ========================= SUBTRACTION ===================== */
	/* =========================================================== */
	
	public Polynomial subtract(int i) {
		return subtract(new Term(i));
	}
	
	public Polynomial subtract(long i) {
		return subtract(new Term(i));
	}
	
	public Polynomial subtract(BigInteger i) {
		return subtract(new Term(i));
	}
	
	public Polynomial subtract(Term e) {
		Term ne = new Term(e.coefficient().negate(), e.variables());
		return add(ne);
	}
	

	public Polynomial subtract(Polynomial poly) {
		Polynomial r = this;
		for (Term e : poly.terms) {			
			r = r.subtract(e);
		}
		return r;
	}	
				
	/* =========================================================== */
	/* ======================= MULTIPLICATION ==================== */
	/* =========================================================== */
	
	public Polynomial multiply(int i) {
		return multiply(new Term(i));		
	}
	
	public Polynomial multiply(long i) {
		return multiply(new Term(i));		
	}
	
	public Polynomial multiply(BigInteger i) {
		return multiply(new Term(i));		
	}
	
	public Polynomial multiply(Term e1) {
		Polynomial r = new Polynomial();
		for (Term e2 : terms) {
			r = r.add(e1.multiply(e2));
		}	
		return r;
	}
	
	public Polynomial multiply(Polynomial poly) {
		Polynomial r = new Polynomial();
		for (Term e : poly.terms) {			
			r = r.add(this.multiply(e));
		}
		return r;
	}		
	
	/* =========================================================== */
	/* ============================ GCD ========================== */
	/* =========================================================== */

	
	/**
	 * <p>
	 * This method computes the Greatest Common Divisor of this Polynomial and
	 * the supplied polynomial. That is, the "biggest" polynomial that divides
	 * evenly into both polynomials. For example:
	 * </p>
	 * 
	 * <pre>
	 * gcd(2x,x) = x
	 * gcd(2x+1,x) = 1
	 * </pre>
	 * 
	 * <p>
	 * In the special case that both polynomials are constants, then it simply
	 * resolves to the normal gcd operation.
	 * </p>
	 * <p>
	 * For more information, see for example this <a
	 * href="http://en.wikipedia.org/wiki/Greatest_common_divisor_of_two_polynomials">Wikipedia
	 * page</a>.
	 * </p>
	 * <b>NOTE: THE IMPLEMENTATION OF THIS METHOD IS CURRENTLY BUGGY. IN
	 * PARTICULAR, IT CAN RETURN NEGATIVE INTEGERS UNLIKE NORMAL GCD</b>
	 */
	public Polynomial gcd(Polynomial a) {		
		final Polynomial zero = Polynomial.ZERO;
		Polynomial b = this;
		Polynomial c;				
						
		// First, decide the right way around for a + b
		
		// BUG HERE: currently there is a bug here, since it doesn't always make
		// the right choice. In particular, if both polynomials are in fact
		// integers, then it doesn't always pick the largest.
		Pair<Polynomial,Polynomial> r = a.divide(b);				
		
		if(r.first().equals(zero)) {			
			r = a.divide(b);
			if(r.first().equals(zero)) {
				// a + b are mutually indivisible
				return Polynomial.ONE;
			} else {
				// b is divisible by a, but not the other way around.
				c = a;
				a = b;
				b = c;
			}
		}		
		
		while (!b.equals(Polynomial.ZERO)) {			
			r = a.divide(b); 
			c = r.second();			
			if (c.equals(Polynomial.ZERO)) {
				return b;
			} else if(r.first().equals(Polynomial.ZERO)) {
				// no further division is possible.
				return b;
			}
			a = b;
			b = c;
		}
		return a;
	}
		
	/**
	 * This method divides this polynomial by the term argument using simple
	 * division. The method produces the pair (quotient,remainder). For example:
	 * 
	 * <pre>
	 * (x + 2xy) / x = (1+2y,0)
	 * (x + 2xy) / y = (2x,x)
	 * 
	 * &#064;param x
	 * &#064;return
	 * 
	 * For more information on polynomial division see: &lt;a href=&quot;http://en.wikipedia.org/wiki/Polynomial_long_division&quot;&gt;wikipedia&lt;/a&gt;
	 * 
	 */
	public Pair<Polynomial,Polynomial> divide(Term t1) {
		Polynomial quotient = new Polynomial(0);
		Polynomial remainder = new Polynomial(0);
		
		for(Term t2 : terms) {
			Pair<Term,Term> r = t2.divide(t1);
			quotient = quotient.add(r.first());
			remainder = remainder.add(r.second());
		}
		
		return new Pair(quotient,remainder);
	}
	
	/**
	 * This method divides this polynomial by the polynomial argument using long
	 * division. The method produces the pair (quotient,remainder). For example:
	 * 
	 * <pre>
	 * (x + 2xy) / x = (1+2y,0)
	 * (x + 2xy) / y = (2x,x)
	 * 
	 * &#064;param x
	 * &#064;return
	 * 
	 * For more information on polynomial long division see: &lt;a href=&quot;http://en.wikipedia.org/wiki/Polynomial_long_division&quot;&gt;wikipedia&lt;/a&gt;
	 * 
	 */
	public Pair<Polynomial,Polynomial> divide(Polynomial x) {
		
		// Ok, yes, this piece of code is horribly inefficient. But, it's tough
		// even to make it work properly, let alone make it work fast.

		Term max = null;
		
		for(Term t : x) {
			if (max == null || max.compareTo(t) > 0) {
				max = t;
			}
		}	

		if (max == null) {
			// this indicates an attempt at division by zero!
			throw new ArithmeticException("polynomial division by zero");
		}
		
		ArrayList<Term> myterms = new ArrayList<Term>(terms);		
		Collections.sort(myterms);
		
		for(Term t1 : myterms) {
			Pair<Term,Term> d = t1.divide(max);	
			if(!d.first().equals(Term.ZERO)) {			
				Term quotient = d.first();	

				Polynomial remainder = this.subtract(x.multiply(quotient));				

				Pair<Polynomial,Polynomial> r = remainder.divide(x);										
				return new Pair(r.first().add(quotient),r.second());
			}	
		}
		
		// base case for recursion.
		return new Pair(Polynomial.ZERO,this);
	}
	
	/* =========================================================== */
	/* ========================== NEGATION ======================= */
	/* =========================================================== */

	public Polynomial negate() {
		Polynomial r = new Polynomial(0);
		
		for(Term t : terms) {
			r.terms.add(t.negate());
		}
		
		return r;
	}		

	/* =========================================================== */
	/* ======================= FACTORISATION ===================== */
	/* =========================================================== */

	/**
	 * The purpose of this method is to factorise the polynomial for a given
	 * variable, producing a factor and a remainder. For example:
	 * 
	 * <pre>
	 * 2x + xy + 2 ======&gt; (2+y, 2) 
	 * </pre>
	 * 
	 * Here, <code>2+y</code> is the factor, whilst <code>2</code> is the
	 * remainder. Thus, <code>x * (2+y) + 2</code> yields the original
	 * polynomial.
	 * 
	 * Notice, that in the case where the variable in question is raised to a
	 * power, then the factor will contain the original variable. For example:
	 * 
	 * <pre>
	 * 2x&circ;2 + xy + 2 ======&gt; (2x+y, 2) 
	 * </pre>
	 */
	public Pair<Polynomial,Polynomial> factoriseFor(String var) {
		Polynomial factor = new Polynomial(0);
		Polynomial remainder = new Polynomial(0);
		
		for(Term t : terms) {
			if(t.variables().contains(var)) {
				ArrayList<String> vars = new ArrayList<String>(t.variables());
				vars.remove(var); // remove one instance of var only
				factor = factor.add(new Term(t.coefficient(),vars));
			} else {
				remainder = remainder.add(t);
			}
		}
		
		return new Pair(factor,remainder);
	}
	
	/* =========================================================== */
	/* ============================ OTHER ======================== */
	/* =========================================================== */

	public boolean equals(Object o) {
		if(o instanceof Polynomial) {
			Polynomial p = (Polynomial) o;
			return p.terms.equals(terms);
		}
		return false;
	}
	
	public int hashCode() {
		return terms.hashCode();
	}
	
	public String toString() {
		if(terms.isEmpty()) {
			return "0";
		}
		
		String r = "";
		boolean firstTime=true;	
		if(terms.size() > 1) {
			r += "(";
		}
		for (Term e : terms) {			
			if (!firstTime) {
				if (e.coefficient().compareTo(BigInteger.ZERO) > 0) {
					r += "+";					
				}
			} 			
			boolean ffirstTime = true;
			
			// yugly.
			if (!e.coefficient().equals(BigInteger.ONE.negate())
					&& (!e.coefficient().equals(BigInteger.ONE))
					|| e.variables().isEmpty()) {
				firstTime = false;
				r += e.coefficient();
			} else if (e.coefficient().equals(BigInteger.ONE.negate())) {
				firstTime = false;
				r += "-";
			} else if (e.variables().size() > 0) {
				firstTime = false;
			}
			for (String v : e.variables()) {
				if (!ffirstTime) {
					r += "*";
				}
				ffirstTime = false;
				r += v;
			}
		}
		if(terms.size() > 1) {
			r += ")";
		}
		return r;
	}
	
	/**
     * This method substitutes all variable names for names given in the
	 * binding. If no binding is given for a variable, then it retains its
	 * original name.
	 * 
	 * @param environment
	 * @return
	 */
	public Polynomial substitute(Map<String, String> environment) {
		Polynomial r = new Polynomial();
		for (Term e : terms) {
			ArrayList<String> nvars = new ArrayList<String>();
			for (String v : e.variables()) {
				if(environment.containsKey(v)) {
					nvars.add(environment.get(v));
				} else {
					nvars.add(v);
				}
			}
			r = r.add(new Term(e.coefficient(), nvars));
		}
		return r;
	}	
			
	/**
	 * This method generates a (relatively exhaustive) list of polynomials with
	 * a given number of variables.
	 * 
	 * @param nvars
	 * @return
	 */
	public static ArrayList<Polynomial> generate(int nvars) {
		ArrayList<Polynomial> r = new ArrayList<Polynomial>();
		if(nvars == 0) {
			for(int i = -2;i<2;++i) {
				if(i != 0) {
					r.add(new Polynomial(i));
				}
			}			
		} else {
			ArrayList<Polynomial> ps = generate(nvars-1);			
			Polynomial x = new Polynomial("x" + nvars);
			for(Polynomial p1 : ps) {
				r.add(x.add(p1));
				r.add(x.multiply(p1));
				for(Polynomial p2 : ps) {
					r.add(x.multiply(p1).add(p2));
				}
			}
		}
		return r;
	}
	
	public static boolean test(Polynomial dividend, Polynomial divisor,
			Polynomial quotient, Polynomial remainder) {
		// Basically, undo the division and see if we have the same thing!
		Polynomial r = quotient.multiply(divisor).add(remainder);
		
		if(!r.equals(dividend)) {			
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * The following provides a useful unit test for the polynomial class. It's
	 * not complete, but it does do a fairly useful amount of testing.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ArrayList<Polynomial> polys = generate(3);
		ArrayList<Pair<Polynomial,Polynomial>> errors = new ArrayList();
		System.out.println("Generated " + polys.size() + " polynomials with 2 variables.");
				
		Polynomial zero = Polynomial.ZERO;
		
		long count = 0;		
		long upperBound = ((long)polys.size()) * polys.size();
		
		for(Polynomial p1 : polys) {
			for(Polynomial p2 : polys) {
				if(!p2.equals(zero) && p2.terms.size() <= p1.terms.size()) {
					Pair<Polynomial,Polynomial> p = p1.divide(p2);
					if(!test(p1,p2,p.first(),p.second())) {
						errors.add(new Pair(p1,p2));
					}
					count++;
					
					if((count % 1000) == 0) {
						double done = count;
						
						done = done / upperBound;						
						done = done * 1000;
						done = Math.round(done) / 10;
						System.err.println("Complete " + count
								+ " polynomial divisions with " + errors.size() + " errors.  Roughly "
								+ done + "% completed.");
					}
				}
			}
		}
		
		// now, print out errors.
		System.out.println("**************** ERRORS ****************");
		for(Pair<Polynomial,Polynomial> e : errors) {			
			System.out.println(e.first() + " / " + e.second());
		}
	}
}
