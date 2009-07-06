package wone.lang;

import java.util.*;

public final class Disjunct implements Formula, Iterable<Formula> {
	private HashSet<Formula> disjuncts;
	
	/**
	 * <p>
	 * Construct a formula from a collection of formulas.
	 * </p>
	 * 
	 * @param clauses
	 */
	Disjunct(Collection<Formula> fs) {
		disjuncts = new HashSet<Formula>();
		disjuncts.addAll(fs);
	}
	
	/**
	 * <p>
	 * Construct a formula from a collection of formulas.
	 * </p>
	 * 
	 * @param clauses
	 */
	Disjunct(Formula... fs) {
		disjuncts = new HashSet<Formula>();
		for(Formula f : fs) {
			disjuncts.add(f);
		}
	}
	
	public Iterator<Formula> iterator() {
		return disjuncts.iterator();
	}
	
	public boolean isTrue() {
		for(Formula f : disjuncts) {
			if(f.isTrue()) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isFalse() {
		for(Formula f : disjuncts) {
			if(!f.isFalse()) {
				return false;
			}
		}
		return true;
	}
	
	public Formula and(Formula f) {
		if (f.isTrue()) {
			return this;
		} else if (f.isFalse()) {
			return f;
		} else if (!f.equals(this)) {
			return new Conjunct(this, f);
		} else {
			return this;
		}
	}
	
	public Formula or(Formula f) {
		if (f.isTrue()) {
			return f;
		} else if (f.isFalse()) {
			return this;
		} else {
			// flattern the hierarchy if possible
			Disjunct r = new Disjunct(this.disjuncts);
			if (f instanceof Disjunct) {
				Disjunct d = (Disjunct) f;
				r.disjuncts.addAll(d.disjuncts);
			} else {
				r.disjuncts.add(f);
			}
			return r;
		}		
	}
	
	public Formula not() {
		ArrayList<Formula> nots = new ArrayList<Formula>();
		for(Formula f : disjuncts) {
			nots.add(f.not());
		}
		return new Conjunct(nots);
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
		for(Formula c : disjuncts) {			
			rs.add(c.substitute(binding));
		}
		return new Disjunct(rs);
	}
	
	public String toString() {
		String r = "(";
		boolean firstTime=true;		
		for(Formula f : disjuncts) {
			if(!firstTime) {
				r += " || ";
			}
			firstTime=false;
			r += f.toString();
		}
		return r + ")";
	}
	
	public boolean equals(Object o) {
		if (o instanceof Disjunct) {
			Disjunct c = (Disjunct) o;
			return c.disjuncts.equals(disjuncts);
		}
		return false;
	}
	
	public int hashCode() {
		return disjuncts.hashCode();
	}
}
