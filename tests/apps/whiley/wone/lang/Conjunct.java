package wone.lang;

import java.util.*;

public final class Conjunct implements Formula, Iterable<Formula> {
	private HashSet<Formula> conjuncts;
	
	/**
	 * <p>
	 * Construct a formula from a collection of formulas.
	 * </p>
	 * 
	 * @param clauses
	 */
	Conjunct(Collection<Formula> fs) {
		conjuncts = new HashSet<Formula>();
		conjuncts.addAll(fs);
	}
	
	/**
	 * <p>
	 * Construct a formula from a collection of formulas.
	 * </p>
	 * 
	 * @param clauses
	 */
	Conjunct(Formula... fs) {
		conjuncts = new HashSet<Formula>();
		for(Formula f : fs) {
			conjuncts.add(f);
		}
	}
	

	public Iterator<Formula> iterator() {
		return conjuncts.iterator();
	}
	
	public boolean isTrue() {
		for(Formula f : conjuncts) {
			if(!f.isTrue()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isFalse() {
		for(Formula f : conjuncts) {
			if(f.isFalse()) {
				return true;
			}
		}
		return false;
	}
	
	public Formula and(Formula f) {
		if (f.isTrue()) {
			return this;
		} else if (f.isFalse() || isTrue()) {
			return f;
		} else {
			// flattern the hierarchy if possible
			Conjunct r = new Conjunct(this.conjuncts);
			if (f instanceof Conjunct) {
				Conjunct c = (Conjunct) f;
				r.conjuncts.addAll(c.conjuncts);
			} else {
				r.conjuncts.add(f);
			}
			return r;
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
		ArrayList<Formula> nots = new ArrayList<Formula>();
		for(Formula f : conjuncts) {
			nots.add(f.not());
		}
		return new Disjunct(nots);
	}
	
	/**
	 * This method substitutes all variable names for names given in the
	 * binding. If no binding is given for a variable, then it retains its
	 * original name.
	 * 
	 * @param binding
	 * @return
	 */
	public Formula substitute(Map<String,String> binding) {	
		HashSet<Formula> rs = new HashSet<Formula>();
		for(Formula c : conjuncts) {			
			rs.add(c.substitute(binding));
		}
		return new Conjunct(rs);
	}
	
	public String toString() {
		String r = "(";
		boolean firstTime=true;
		for(Formula f : conjuncts) {
			if(!firstTime) {
				r += " && ";
			}
			firstTime=false;
			r += f.toString();
		}
		return r + ")";
	}
	
	public boolean equals(Object o) {
		if (o instanceof Conjunct) {
			Conjunct c = (Conjunct) o;
			return c.conjuncts.equals(conjuncts);
		}
		return false;
	}
	
	public int hashCode() {
		return conjuncts.hashCode();
	}
}
