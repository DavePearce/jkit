package wone.lang;

import java.util.*;

public final class Atom implements Formula {
	private boolean sign; // true = positive literal, false = negative literal
	private String atom;
	
	public Atom(boolean sign, String atom) {
		this.sign = sign;
		this.atom = atom;
	}
	
	public String atom() {
		return atom;
	}
	
	public boolean sign() {
		return sign;
	}
	
	public boolean isTrue() {
		return false;
	}
	
	public boolean isFalse() {
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
		if(f.isTrue()) {
			return f;
		} else if(f.isFalse()) {
			return this;
		} else if(!f.equals(this)) {
			return new Disjunct(this,f);
		} else {
			return this;
		}
	}
	
	public Formula not() {
		return new Atom(!sign,atom);
	}
	
	public Formula substitute(Map<String,String> binding) {
		String v = binding.get(atom);
		if(v != null) {
			return new Atom(sign,v);
		} else {
			return this;
		}
	}
	
	public String toString() {
		if(!sign) {
			return "!" + atom;
		} else {
			return atom;
		}
	}
	
	public boolean equals(Object o) {
		if(o instanceof Atom) {
			Atom l = (Atom)o;
			return l.atom.equals(atom) && l.sign == sign;
		}
		return false;
	}
	
	public int hashCode() {
		return atom.hashCode();
	}
}
