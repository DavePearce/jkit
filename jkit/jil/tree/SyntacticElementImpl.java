package jkit.jil.tree;

import java.util.*;
import java.util.concurrent.*;

public class SyntacticElementImpl  implements SyntacticElement {
	protected ArrayList<Attribute> attributes;
	
	public SyntacticElementImpl() {
		attributes = new ArrayList<Attribute>();
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



