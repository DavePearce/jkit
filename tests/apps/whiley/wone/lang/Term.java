package wone.lang;

import java.util.*;
import java.math.*;
import wone.util.*;

public class Term implements Comparable<Term> {
	public static final Term ZERO = new Term(0);
	public static final Term ONE = new Term(1);
	
	private final BigInteger coefficient;
	private final List<String> variables;	

	public Term(int coeff, List<String> variables) {
		coefficient = BigInteger.valueOf(coeff);
		if(coeff != 0) {
			this.variables = new ArrayList<String>(variables);
			Collections.sort(this.variables);
		} else {
			this.variables = new ArrayList<String>();
		}		
	}

	public Term(int coeff, String... variables) {
		coefficient = BigInteger.valueOf(coeff);
		this.variables = new ArrayList<String>();
		if(coeff != 0) {
			for(String v : variables) {
				this.variables.add(v);
			}
			Collections.sort(this.variables);
		}
	}

	public Term(long coeff, List<String> variables) {
		coefficient = BigInteger.valueOf(coeff);
		if(coeff != 0) {
			this.variables = new ArrayList<String>(variables);
			Collections.sort(this.variables);
		} else {
			this.variables = new ArrayList<String>();
		}				
	}

	public Term(long coeff, String... variables) {
		coefficient = BigInteger.valueOf(coeff);
		this.variables = new ArrayList<String>();
		if(coeff != 0L) {
			for(String v : variables) {
				this.variables.add(v);
			}
			Collections.sort(this.variables);
		}		
	}
	
	public Term(BigInteger coeff, List<String> variables) {
		coefficient = coeff;
		if(!coeff.equals(BigInteger.ZERO)) {
			this.variables = new ArrayList<String>(variables);
			Collections.sort(this.variables);
		} else {
			this.variables = new ArrayList<String>();
		}				
	}

	public Term(BigInteger coeff, String... variables) {
		coefficient = coeff;
		this.variables = new ArrayList<String>();
		if (!coeff.equals(BigInteger.ZERO)) {
			for (String v : variables) {
				this.variables.add(v);
			}
			Collections.sort(this.variables);
		}		
	}	
	
	public BigInteger coefficient() {
		return coefficient;
	}
	
	public List<String> variables() {
		return Collections.unmodifiableList(variables);
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Term)) {
			return false;
		}
		Term e = (Term) o;
		return e.coefficient.equals(coefficient)
		&& e.variables.equals(variables);
	}		
	
	public String toString() {
		String r = "";
		
		if(variables.size() == 0) {
			return coefficient.toString();
		} else if(coefficient.equals(BigInteger.ONE.negate())) {
			r += "-";
		} else if(!coefficient.equals(BigInteger.ONE)) {
			r += coefficient.toString();
		} 
		boolean firstTime=true;
		for(String v : variables) {
			if(!firstTime) {
				r += "*";
			}
			firstTime=false;
			r += v;
		}
		return r;
	}

	public int hashCode() {
		return variables.hashCode();
	}

	public int compareTo(Term e) {
		int maxp = maxPower();
		int emaxp = e.maxPower();
		if(maxp > emaxp) {
			return -1;
		} else if(maxp < emaxp) {
			return 1;
		}
		if (variables.size() < e.variables.size()) {
			return 1;
		} else if (variables.size() > e.variables.size()) {
			return -1;
		}
		if (coefficient.compareTo(e.coefficient) < 0) {
			return 1;
		} else if (coefficient.compareTo(e.coefficient) > 0) {
			return -1;
		}
		for (int i = 0; i < Math.min(variables.size(), e.variables.size()); ++i) {
			String v = variables.get(i);
			String ev = e.variables.get(i);
			int r = v.compareTo(ev);
			if (r != 0) {
				return r;
			}
		}
		return 0;

	}

	public boolean isConstant() {
		return variables.isEmpty() || coefficient.equals(BigInteger.ZERO);
	}

	public Polynomial add(int i) {		
		return new Polynomial(this).add(i);
	}
	
	public Polynomial add(long i) {		
		return new Polynomial(this).add(i);
	}
	
	public Polynomial add(BigInteger i) {		
		return new Polynomial(this).add(i);
	}
	
	public Polynomial add(Term e) {		
		return new Polynomial(this).add(e);
	}
	
	public Polynomial add(Polynomial p) {		
		return new Polynomial(this).add(p);
	}
	
	public Polynomial subtract(int i) {		
		return new Polynomial(this).subtract(i);
	}
	
	public Polynomial subtract(long i) {		
		return new Polynomial(this).subtract(i);
	}
	
	public Polynomial subtract(BigInteger i) {		
		return new Polynomial(this).subtract(i);
	}
	
	public Polynomial subtract(Term e) {		
		return new Polynomial(this).subtract(e);
	}
	
	public Polynomial subtract(Polynomial p) {		
		return new Polynomial(this).subtract(p);
	}
	
	public Term multiply(int i) {
		return new Term(coefficient.multiply(BigInteger.valueOf(i)),variables);
	}
	
	public Term multiply(long i) {
		return new Term(coefficient.multiply(BigInteger.valueOf(i)),variables);
	}
	
	public Term multiply(BigInteger i) {
		return new Term(coefficient.multiply(i),variables);
	}
	
	public Term multiply(Term e) {
		ArrayList<String> nvars = new ArrayList<String>(variables);
		nvars.addAll(e.variables);	
		return new Term(coefficient.multiply(e.coefficient),nvars);
	}	
	
	public Polynomial multiply(Polynomial e) {
		return e.multiply(this);		
	}	
	
	public Pair<Term,Term> divide(Term t) {
		if (variables.containsAll(t.variables)
				&& coefficient.compareTo(t.coefficient) >= 0) {				
			BigInteger[] ncoeff = coefficient.divideAndRemainder(t.coefficient);			
			ArrayList<String> nvars = new ArrayList<String>(variables);
			for(String v : t.variables) {
				nvars.remove(v);
			}			
			Term quotient = new Term(ncoeff[0],nvars);
			Term remainder = new Term(ncoeff[1],nvars);			
			return new Pair(quotient,remainder);		
		} else {
			// no division is possible.
			return new Pair(new Term(0),this);
		}
	}
	
	public Term negate() {
		return new Term(coefficient.negate(),variables);
	}		
	
	private int maxPower() {
		int max = 0;
		String last = null;
		int cur = 0;
		for(String v : variables){
			if(last == null) {
				cur = 1;
				last = v;
			} else if(v.equals(last)){
				cur = cur + 1;
			} else {
				max = Math.max(max,cur);
				cur = 1;
				last = v;
			}
		}
		return Math.max(max,cur);		
	}
}

