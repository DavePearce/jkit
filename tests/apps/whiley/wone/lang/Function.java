package wone.lang;

import java.util.*;

/**
 * <p>
 * This represents an uninterpreted function application.
 * </p>  
 * 
 * @author djp
 *
 */
public final class Function implements Literal {
	private boolean sign; // true = positive literal, false = negative literal.
	private String fun; // function name
	private ArrayList<String> parameters; // function parameters
	private String var; // variable being assigned 
		
	public Function(boolean sign, String fun, String var, String... parameters) {
		if(fun == null) {
			throw new IllegalArgumentException("wone.lang.Function, fun argument cannot be null");
		}
		if(var == null) {
			throw new IllegalArgumentException("wone.lang.Function, var argument cannot be null");
		}
		this.sign = sign;
		this.fun = fun;
		this.var = var;
		this.parameters = new ArrayList<String>();
		for(String p : parameters) {
			this.parameters.add(p);
		}
	}		
	
	public Function(boolean sign, String fun, String var, Collection<String> parameters) {
		if(fun == null) {
			throw new IllegalArgumentException("wone.lang.Function, fun argument cannot be null");
		}
		if(var == null) {
			throw new IllegalArgumentException("wone.lang.Function, var argument cannot be null");
		}
		this.sign = sign;
		this.fun = fun;
		this.var = var;
		this.parameters = new ArrayList<String>(parameters);
	}
	
	public String function() { 
		return fun;
	}
	
	public String variable() {
		return var;
	}
	
	public String literal() {
		String r = fun + "(";
		boolean firstTime=true;		
		for(String p : parameters) {
			if(!firstTime) {
				r = r + ",";
			}
			firstTime=false;
			r = r + p;
		}			
		return r + ")==" + var;		
	}
	
	public List<String> parameters() {
		return Collections.unmodifiableList(parameters);
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
	
	public Formula not() {
		return new Function(!sign,fun,var,parameters);
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
	
	public Formula substitute(Map<String,String> binding) {
		ArrayList<String> nparams = new ArrayList<String>();
		boolean pchanged = false;
		for(String p : parameters) {
			String np = binding.get(p);
			if(np != null) {
				nparams.add(np);
				pchanged=true;
			} else {
				nparams.add(p);
			}
		}
		String nvar = binding.get(var);
		
		if(nvar != null) {
			return new Function(sign,fun,nvar,nparams);
		} else if(pchanged) {
			return new Function(sign,fun,var,nparams);
		} else {
			return this;
		}
	}
	
	public String toString() {
		String r = fun + "(";
		boolean firstTime=true;
		
		for(String p : parameters) {
			if(!firstTime) {
				r = r + ",";
			}
			firstTime=false;
			r = r + p;
		}
		r = r + ")";
		
		if(!sign) {
			return r + "!=" + var;
		} else {
			return r + "==" + var;
		}
	}
	
	public boolean equals(Object o) {
		if(o instanceof Function) {
			Function f = (Function)o;
			return sign == f.sign && var.equals(f.var) && fun.equals(f.fun)
					&& parameters.equals(f.parameters);
		}
		return false;
	}
	
	public int hashCode() {
		return var.hashCode() + fun.hashCode() + parameters.hashCode();
	}
}
