package mocha.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import jkit.core.Type;
import jkit.util.dfa.*;

public class TypeMap implements FlowSet {
	
	private HashMap<String, Type> store;
	private HashMap<String, Type> lastAssigned;
	
	public TypeMap() {
		store = new HashMap<String, Type>();
		lastAssigned = new HashMap<String, Type>();
	}
	
	public TypeMap(HashMap<String, Type> s, HashMap<String, Type> l) {
		store = s;
		lastAssigned = l;
	}
	
	public FlowSet clone() {
		return new TypeMap((HashMap<String, Type>) store.clone(), 
					(HashMap<String, Type>) lastAssigned.clone());
	}
	
	public boolean join(FlowSet fs) {
		TypeMap m = (TypeMap) fs;
		
		assert store.size() == m.store.size();
		boolean r = false;
		
		for(String s : m.store.keySet()) {
			Type a = store.get(s);
			Type b = m.store.get(s);
			store.put(s, internal_join(a, b));
			r |= !(store.get(s).equals(a));
		}
		
		for(String s : m.lastAssigned.keySet()) {
			Type a = lastAssigned.get(s);
			Type b = m.lastAssigned.get(s);
			lastAssigned.put(s, internal_join(a,b));
		}
		
		return r;
	}
	
	public void addVar(String s, Type t) {
		store.put(s, t);
	}
	
	public Type varType(String s) {
		return store.get(s);
	}
	
	public void assign(String s, Type t) {
		lastAssigned.put(s, t);
		addVar(s, t);
	}
	
	public Type lastAssigned(String s) {
		return lastAssigned.get(s);
	}
	
	public void instanceOf(Type lhs, Type rhs) {
		replace(lhs, rhs);
	}
	
	public Map<String, Type> environment() {
		return store;
	}
	
	private void replace(Type old, Type nw) {
		Set<String> vars = store.keySet();
		for(String s : vars) {
			Type t = store.get(s);
			if(t == old) {
				store.put(s, nw);
			}
		}
	}
	
	// Copied from TypedStore. Why I'm not sure.  Seems to handle some
	// stuff I might have to otherwise.
	private final Type internal_join(Type a, Type b) {
		// this is a little icky ;)
		if(b == null) {
			return a;
		} else if(a == null) {
			return b;
		} else {
			// easy case
			return a.union(b);
		}
	}
	
	public String toString() {
		return store.toString();
	}
	
	public String toShortString() {
		String s = "{";
		boolean first = true;
		for(Entry<String, Type> e : store.entrySet()) {
			if(first) {
				s += e.getKey() + "=";
				if(e.getValue() == null) {
					s += "var";
				}
				else {
					s += e.getValue().toShortString();
				}
				first = false;
			}
			else {
				s += ", " + e.getKey() + "=";
				if(e.getValue() == null) {
					s += "var";
				}
				else {
					s += e.getValue().toShortString();
				}
			}
			
		}
		return s += "}";
	}

}
