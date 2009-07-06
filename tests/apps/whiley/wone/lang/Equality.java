package wone.lang;

import java.util.Map;
import java.util.Set;

public final class Equality implements Equation {
	private final boolean sign;
	private final Polynomial lhs;
	private final Polynomial rhs;

	public Equality(boolean sign, Polynomial lhs, Polynomial rhs) {
		this.sign = sign;
		this.lhs = lhs;
		this.rhs = rhs;
	}
	
	public Equality(boolean sign, Rational lhs, Rational rhs) {
		this.sign = sign;
		this.lhs = lhs.numerator().multiply(rhs.denominator());
		this.rhs = rhs.numerator().multiply(lhs.denominator());
	}

	public Polynomial lhs() {
		return lhs;
	}

	public Polynomial rhs() {
		return rhs;
	}
	
	public boolean sign() {
		return sign;
	}
	
	public String literal() {
		return lhs + "==" + rhs;
	}
	
	public boolean isTrue() {
		if(lhs.isConstant() && rhs.isConstant()) {
			return sign ? !lhs.equals(rhs) : lhs.equals(rhs);			
		} else if(sign) {
			return lhs.equals(rhs);
		}
		return false;				
	}
	
	public boolean isFalse() {
		if(lhs.isConstant() && rhs.isConstant()) {
			return sign ? !lhs.equals(rhs) : lhs.equals(rhs);			
		} else if(!sign) {
			return lhs.equals(rhs);
		}
		return false;
	}	
	
	public String toString() {
		if(sign) {
			return lhs + "==" + rhs;
		} else {
			return lhs + "!=" + rhs;
		}
	}
	
	public Formula and(Formula f) {
		// Room for some simple optimisations here.
		if(f.isTrue()) {
			return this;
		} else if(f.isFalse()) {
			return f;
		} else if(!f.equals(this)) {
			return new Conjunct(this,f);
		} else {
			return this;
		}
	}
	
	public Formula or(Formula f) {
		if (f.isTrue()) {
			return f;
		} else if (f.isFalse()) {
			return this;
		} else if (!f.equals(this)) {
			return new Disjunct(this, f);
		} else {
			return this;
		}
	}
	
	public Formula not() {
		return new Equality(!sign,lhs,rhs);
	}
	
	public Equality normalise() {
		Polynomial nrhs = rhs;
		
		for(Term e : lhs) {
			nrhs = nrhs.subtract(e);			
		}
		
		return new Equality(sign,Polynomial.ZERO,nrhs);
	}
	
	/**
	 * The purpose of this method is to rearrange the equality in order to
	 * collect terms involving a given variable on the left-hand side. For
	 * example:
	 * 
	 * <pre>
	 * x == 2xy + 2 =====> x-2xy == 2
	 * y == 3 + x   =====> -x == 3 - y
	 * </pre>
	 * 
	 * @param var
	 * @return
	 */
	public Equality rearrange(String var) {
		Polynomial nlhs = new Polynomial();
		Polynomial nrhs = new Polynomial();										
				
		for(Term e : lhs) {
			if(e.variables().contains(var)) {
				nlhs = nlhs.add(e);
			} else {
				nrhs = nrhs.subtract(e);
			}
		}
		
		for(Term e : rhs) {
			if(e.variables().contains(var)) {
				nlhs = nlhs.subtract(e);
			} else {
				nrhs = nrhs.add(e);
			}
		}	
		
		return new Equality(sign,nlhs,nrhs);
	}
	
	public Equality substitute(Map<String, String> environment) {
		return new Equality(sign,lhs.substitute(environment), rhs
				.substitute(environment));
	}
	
	public boolean equals(Object o) {
		if (o instanceof Equality) {
			Equality e = (Equality) o;
			return sign == e.sign && lhs.equals(e.lhs) && rhs.equals(e.rhs);
		}
		return false;
	}
	
	public int hashCode() {
		return lhs.hashCode() ^ rhs.hashCode();
	}
}
