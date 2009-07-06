package wone.lang;

import java.math.*;
import java.util.Map;
import java.util.Set;
import wone.util.Pair;

/**
 * <p>
 * A rational object represents a rational number. Examples include:
 * </p>
 * 
 * <pre>
 *  
 * 1/x
 * x/y
 * 1/3
 * (10x+6)/(y+1)
 * </pre>
 * 
 * <b>Please note that this class exploits <code>Polynomial.divide()</code>
 * and <code>Polynomial.gcd()</code>, which is experimental and probably
 * contains bugs</b>.
 * 
 * @author djp
 * 
 */

public class Rational {
	private Polynomial numerator;
	private Polynomial denominator;
	
	public Rational(int i) {
		numerator = new Polynomial(i);
		denominator = new Polynomial(1);
	}
	
	public Rational(int i, int j) {
		if ((i < 0 && j < 0) || (i > 0 && j < 0)) {
			i = -i;
			j = -j;
		} 

		int gcd = gcd(i,j);			
		numerator = new Polynomial(i/gcd);
		denominator = new Polynomial(j/gcd);								
	}
	
	public Rational(BigInteger i) {
		numerator = new Polynomial(i);
		denominator = new Polynomial(1);
	}
	
	public Rational(String x) {
		numerator = new Polynomial(x);
		denominator = new Polynomial(1);
	}
	
	public Rational(BigInteger i, BigInteger j) {
		// the aim here is simplify the rational as much as possible.		
		BigInteger gcd = i.gcd(j);		
		BigInteger zero = BigInteger.ZERO;
	
		if(!gcd.equals(BigInteger.ONE) && !gcd.equals(zero)) {
			i = i.divide(gcd);
			j = j.divide(gcd);				
		} 		
		
		if ((i.compareTo(zero) < 0 && j.compareTo(zero) < 0)
				|| (i.compareTo(zero) < 0 && j.compareTo(zero) > 0)) {
			i = i.negate();
			j = j.negate();
		}
		
		numerator = new Polynomial(i);
		denominator = new Polynomial(j);		
	}
	
	public Rational(Polynomial i) {
		numerator = i;
		denominator = new Polynomial(1);
	}
	
	public Rational(Polynomial i, Polynomial j) {
		// Now, we attempt to do some simplification here. The main reason is
		// that, if this rational is actually a constant, then we want to know
		// this explicitly. For example, 2x / x -> 2. Thus, we can always
		// compare a rational against a constant value (such as one or zero) and
		// be confident of the result.
		Pair<Polynomial, Polynomial> tmp = i.divide(j);

		if (tmp.second().equals(Polynomial.ZERO)) {
			numerator = tmp.first();
			denominator = Polynomial.ONE;
		}

		numerator = i;
		denominator = j;
	}
	
	public Polynomial numerator() {
		return numerator;
	}
	
	public Polynomial denominator() {
		return denominator;
	}
	
	public boolean isConstant() {
		return numerator.isConstant() && denominator.isConstant();
	}
	
	public boolean isVariable() {
		return numerator.isVariable() && denominator.equals(Polynomial.ONE);
	}
	
	public String variable() {
		return numerator.variable();
	}
	
	public Rational substitute(Map<String, String> environment) {
		return new Rational(numerator.substitute(environment), denominator
				.substitute(environment));
	}
	
	public Set<String> freeVariables() {
		Set<String> fvs = numerator.freeVariables();
		fvs.addAll(denominator.freeVariables());
		return fvs;
	}
	
	/* =========================================================== */
	/* ========================== ADDITION ======================= */
	/* =========================================================== */
	
	public Rational add(int i) {
		Polynomial top = denominator.multiply(i);
		top = top.add(numerator);
		return new Rational(top, denominator);
	}
	
	public Rational add(long i) {
		Polynomial top = denominator.multiply(i);
		top = top.add(numerator);
		return new Rational(top, denominator);
	}
	
	public Rational add(BigInteger i) {
		Polynomial top = denominator.multiply(i);
		top = top.add(numerator);
		return new Rational(top, denominator);
	}
	
	public Rational add(Term i) {
		Polynomial top = denominator.multiply(i);
		top = top.add(numerator);
		return new Rational(top, denominator);
	}
	
	public Rational add(Polynomial p) {
		Polynomial top = denominator.multiply(p);
		top = top.add(numerator);
		return new Rational(top, denominator);
	}
	
	public Rational add(Rational r) {
		Polynomial top = r.numerator().multiply(denominator);
		top = top.add(numerator.multiply(r.denominator));
		return new Rational(top, denominator.multiply(r.denominator));
	}
	
	/* =========================================================== */
	/* ======================= SUBTRACTION ======================= */
	/* =========================================================== */

	public Rational subtract(int i) {
		Polynomial top = numerator;
		top = top.subtract(denominator.multiply(i));
		return new Rational(top, denominator);
	}
	
	public Rational subtract(long i) {
		Polynomial top = numerator;
		top = top.subtract(denominator.multiply(i));
		return new Rational(top, denominator);
	}
	
	public Rational subtract(Term t) {
		Polynomial top = numerator;
		top = top.subtract(denominator.multiply(t));
		return new Rational(top, denominator);
	}
	
	public Rational subtract(Polynomial p) {
		Polynomial top = numerator;
		top = top.subtract(denominator.multiply(p));
		return new Rational(top, denominator);
	}
	
	public Rational subtract(Rational r) {
		Polynomial top = numerator.multiply(r.denominator);
		top = top.subtract(denominator.multiply(r.numerator));
		return new Rational(top, denominator.multiply(r.denominator));
	}
	
	/* =========================================================== */
	/* ====================== MULTIPLICATION ===================== */
	/* =========================================================== */

	public Rational multiply(int i) {
		Polynomial top = numerator.multiply(i);		
		return new Rational(top, denominator);
	}
	
	public Rational multiply(long i) {
		Polynomial top = numerator.multiply(i);		
		return new Rational(top, denominator);
	}
	
	public Rational multiply(BigInteger i) {
		Polynomial top = numerator.multiply(i);		
		return new Rational(top, denominator);
	}
	
	public Rational multiply(Term t) {
		Polynomial top = numerator.multiply(t);		
		return new Rational(top, denominator);
	}
	
	public Rational multiply(Polynomial p) {
		Polynomial top = numerator.multiply(p);		
		return new Rational(top, denominator);
	}
	
	public Rational multiply(Rational r) {
		Polynomial top = numerator.multiply(r.numerator());		
		return new Rational(top, denominator.multiply(r.denominator));
	}
	
	/* =========================================================== */
	/* ========================= DIVISION ======================== */
	/* =========================================================== */

	public Rational divide(int r) {
		Polynomial top = numerator;		
		return new Rational(top, denominator.multiply(r));
	}
	
	public Rational divide(long r) {
		Polynomial top = numerator;		
		return new Rational(top, denominator.multiply(r));
	}
	
	public Rational divide(BigInteger r) {
		Polynomial top = numerator;		
		return new Rational(top, denominator.multiply(r));
	}
	
	public Rational divide(Term r) {
		Polynomial top = numerator;		
		return new Rational(top, denominator.multiply(r));
	}
	
	public Rational divide(Polynomial r) {
		Polynomial top = numerator;		
		return new Rational(top, denominator.multiply(r));
	}
	
	public Rational divide(Rational r) {
		Polynomial top = numerator.multiply(r.denominator());		
		return new Rational(top, denominator.multiply(r.numerator));
	}
	
	/* =========================================================== */
	/* =========================== OTHER ========================= */
	/* =========================================================== */

	public Rational negate() {		
		return new Rational(numerator.negate(),denominator());
	}
	
	public boolean equals(Object o) {
		if(o instanceof Rational) {
			Rational r = (Rational) o;
			Polynomial lhs = numerator.multiply(r.denominator);
			Polynomial rhs = r.numerator.multiply(denominator);
			return lhs.equals(rhs);
		}
		return false;
	}
	
	public int hashCode() {
		// we have to be careful here to ensure the Java contract.
		Set<String> fvs = numerator.freeVariables();
		fvs.addAll(denominator.freeVariables());
		return fvs.hashCode();
	}
	
	public String toString() {
		if (denominator.equals(Polynomial.ONE)) {
			return numerator.toString();
		} else if (numerator.isConstant() && denominator.isConstant()) {
			return numerator + " / " + denominator;
		} else if (numerator.isConstant()) {
			return numerator + " / (" + denominator + ")";
		} else if (denominator.isConstant()) {
			return "(" + numerator + ") / " + denominator;
		} else {
			return "(" + numerator + ") / (" + denominator + ")";
		}
	}

	/* =========================================================== */
	/* ========================= HELPER ======================== */
	/* =========================================================== */

	private int gcd(int a, int b) {
		int c;
		boolean asign = a < 0;
		boolean bsign = b < 0;
		
		a = asign ? -a : a;
		b = bsign ? -b : b;								
		
		if (a < b) {
			c = a;
			a = b;
			b = c;

		}
		while (b != 0) {
			c = a % b;
			if (c == 0) {
				return b;
			}
			a = b;
			b = c;
		}
		
		asign = asign ^ bsign;
		
		if(asign) {
			return -a;
		} else {		
			return a;
		}
	}
	
	public static void main(String[] args) {
		Rational r = new Rational(1,-2);
		System.out.println("GOT: " + r);
		System.out.println("GOT: " + r.negate());
	}
}
