package jkit.jil.tree;

import java.util.*;

public class SyntacticElementImpl  implements SyntacticElement {
	private List<Attribute> attributes;
	
	public SyntacticElementImpl() {
		attributes = new ArrayList<Attribute>();
	}
	
	public SyntacticElementImpl(Attribute x) {
		attributes = new ArrayList<Attribute>();
		attributes.add(x);
	}
	
	public SyntacticElementImpl(List<Attribute> attributes) {
		this.attributes = attributes;			
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



