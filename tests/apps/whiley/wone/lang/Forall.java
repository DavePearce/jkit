package wone.lang;

import java.util.*;

public class Forall implements Formula {
	private Set<String> variables;
	private Formula formula;
	
	public Forall(String variable, Formula formula) {
		this.variables = new HashSet<String>();
		this.variables.add(variable);
		this.formula = formula;
	}	
	
	public Forall(Set<String> variables, Formula formula) {
		this.variables = new HashSet<String>(variables);
		this.formula = formula;
	}	
	
	public Set<String> variables() {
		return variables;
	}
	
	public Formula formula() {
		return formula;
	}
	
	public boolean isTrue() {
		return formula.isTrue();
	}
	
	public boolean isFalse() {
		return formula.isFalse();
	}
		
	public Formula not() {
		return new Exists(variables,formula.not());
	}
	
	/**
	 * <p>
	 * Compute the logical and of this formula.
	 * </p>
	 * @return
	 */
	public Formula and(Formula f) {
		if (f.isTrue()) {
			return this;
		} else if (f.isFalse()) {
			return f;
		} else {
			return new Conjunct(this,f);
		}
	}
	
	/**
	 * <p>
	 * Compute the logical or of this formula.
	 * </p>
	 * @return
	 */
	public Formula or(Formula f) {
		if (f.isTrue()) {
			return f;
		} else if (f.isFalse()) {
			return this;
		} else {
			return new Disjunct(this,f);
		}
	}
	
	/**
	 * This method substitutes all variable names for names given in the
	 * binding. If no binding is given for a variable, then it retains its
	 * original name.
	 * 
	 * @param binding
	 * @return
	 */
	public Formula substitute(Map<String, String> binding) {
		Set<String> nvars = new HashSet<String>();

		for (String v : variables) {
			String b = binding.get(v);
			if (b == null) {
				nvars.add(v);
			} else {
				nvars.add(b);
			}
		}

		return new Forall(nvars, formula.substitute(binding));
	}
	
	public boolean equals(Object o) {
		if (o instanceof Forall) {
			Forall f = (Forall) o;
			return f.variables.equals(variables) && f.formula.equals(formula);
		}
		return false;
	}
	
	public int hashCode() {
		return variables.hashCode() + formula.hashCode();
	}
	
	public String toString() {
		String r = "forall ";
		boolean firstTime = true;
		for (String v : variables) {
			if (!firstTime) {
				r += ",";
			}
			firstTime = false;
			r += v;
		}
		return r + " [" + formula.toString() + "]";
	}
}
