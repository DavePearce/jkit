package wone.lang;

import java.util.*;

/**
 * <p>
 * A constant represents either true or false. This is helpful sometimes to
 * indicate a formula which is unsatisfiable, for example.
 * </p>
 * 
 * @author djp
 * 
 */
public final class Bool implements Formula {
	private boolean constant;
	
	public Bool(boolean constant) {
		this.constant = constant;
	}
	
	public boolean constant() {
		return constant;
	}
	
	public boolean isTrue() {
		return constant;
	}
	
	public boolean isFalse() {
		return !constant;
	}
	
	public Formula not() {
		return new Bool(!constant);
	}
	
	public Formula and(Formula f) {
		if(constant) {
			return f;
		} else {
			return this;
		}		
	}
	
	public Formula or(Formula f) {
		if(constant) {
			return this;
		} else {
			return f;
		}
	}
	
	public Formula substitute(Map<String,String> binding) {
		return this;
	}
	
	public String toString() {
		return Boolean.toString(constant);
	}
	
	public boolean equals(Object o) {
		if(o instanceof Bool) {
			Bool c = (Bool)o;
			return c.constant == constant;
		}
		return false;
	}
	
	public int hashCode() {
		if (constant) {
			return 0;
		} else {
			return 1;
		}
	}
}
