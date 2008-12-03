package jkit.jil;

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
		this.attributes = Arrays.asList(attributes);			
	}
	
	public List<Attribute> attributes() { return attributes; }
	
	public Attribute getFirst(Class c) {
		for(Attribute a : attributes) {
			if(a.getClass() == c) {
				return a;
			}
		}
		return null;
	}		
}



