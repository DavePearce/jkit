package wone.lang;

import java.util.Map;

// This represents an inequality of the form lower <= upper

public final class Inequality implements Equation {
	protected final boolean sign;
	protected final Polynomial lhs;
	protected final Polynomial rhs;
	
	public Inequality(boolean sign, Polynomial lower, Polynomial upper) {
		this.sign = sign;
		this.lhs = lower;
		this.rhs = upper;
	}
	
	public Inequality(boolean sign, Rational lower, Rational upper) {
		this.sign = sign;
		this.lhs = lower.numerator().multiply(upper.denominator());
		this.rhs = upper.numerator().multiply(lower.denominator());
	}
	
	public Polynomial lhs() {
		return lhs;
	}
	
	public Polynomial rhs() {
		return rhs;
	}
	
	public String literal() {
		return lhs + "<=" + rhs;
	}
	
	public boolean sign() {
		return sign;
	}
	
	public String toString() {
		if(sign) {
			return lhs + "<=" + rhs;
		} else {	
			return rhs + "<" + lhs;
		}
	}
	
	/*
	public boolean isLinear() {
		return lower.isLinear() && upper.isLinear();
	}
	*/
	
	public boolean isTrue() {
		if (lhs.isConstant() && rhs.isConstant()) {	
			if(sign) {
				return lhs.constant().compareTo(rhs.constant()) <= 0;
			} else {
				return lhs.constant().compareTo(rhs.constant()) > 0;
			}
		} else if(sign) {
			return lhs.equals(rhs);
		}
		return false;
	}
	
	public boolean isFalse() {
		if (lhs.isConstant() && rhs.isConstant()) {			
			if(sign) {
				return lhs.constant().compareTo(rhs.constant()) > 0;
			} else {
				return lhs.constant().compareTo(rhs.constant()) <= 0;
			}
		} else if(!sign) {
			return lhs.equals(rhs);
		}
		
		return false;
	}
	
	public Formula and(Formula f) {
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
		return new Inequality(!sign,lhs, rhs);
	}
	
	public Inequality normalise() {
		Polynomial nrhs = rhs;

		for (Term e : lhs) {
			nrhs = nrhs.subtract(e);
		}

		return new Inequality(sign, Polynomial.ZERO, nrhs);
	}
	
	/**
	 * The purpose of this method is to rearrange the inequality in order to
	 * collect terms involving a given variable on the left-hand side. For
	 * example:
	 * 
	 * <pre>
	 * x <= 2xy + 2 =====> x-2xy <= 2
	 * y <= 3 + x   =====> -x <= 3 - y
	 * </pre>
	 * 
	 * @param var
	 * @return
	 */
	public Inequality rearrange(String var) {
		Polynomial nlower = new Polynomial();
		Polynomial nupper = new Polynomial();										
				
		for(Term e : lhs) {
			if(e.variables().contains(var)) {
				nlower = nlower.add(e);
			} else {
				nupper = nupper.subtract(e);
			}
		}
		
		for(Term e : rhs) {
			if(e.variables().contains(var)) {
				nlower = nlower.subtract(e);
			} else {
				nupper = nupper.add(e);
			}
		}	
		
		return new Inequality(sign,nlower,nupper);
	}
	
	public Inequality substitute(Map<String, String> environment) {
		return new Inequality(sign,lhs.substitute(environment),rhs.substitute(environment));
	}
	
	public boolean equals(Object o) {
		if (o instanceof Inequality) {
			Inequality e = (Inequality) o;
			return sign == e.sign && lhs.equals(e.lhs)
					&& rhs.equals(e.rhs);
		}
		return false;
	}

	public int hashCode() {
		return lhs.hashCode() ^ rhs.hashCode();
	}
}
