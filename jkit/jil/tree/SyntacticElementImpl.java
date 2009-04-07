package jkit.jil.tree;

import java.util.*;
import java.util.concurrent.*;

public class SyntacticElementImpl  implements SyntacticElement {
	private List<Attribute> attributes;
	
	public SyntacticElementImpl() {
		// I use copy on write here, since for the most part I don't expect
		// attributes to change, and hence can be safely aliased. But, when they
		// do change I need fresh copies.
		attributes = new CopyOnWriteArrayList<Attribute>();
	}
	
	public SyntacticElementImpl(Attribute x) {
		attributes = new ArrayList<Attribute>();
		attributes.add(x);
	}
	
	public SyntacticElementImpl(List<Attribute> attributes) {
		// the following is really necessary to get rid of annoying aliasing
		// problems.
		this.attributes = new ArrayList<Attribute>(attributes);			
	}
	
	public SyntacticElementImpl(Attribute[] attributes) {
		this.attributes = new ArrayList<Attribute>(Arrays.asList(attributes));			
	}
	
	public List<Attribute> attributes() { return attributes; }
	
	public Attribute attribute(Class c) {
		for(Attribute a : attributes) {
			if(c.isInstance(a)) {
				return a;
			}
		}
		return null;
	}		
}



