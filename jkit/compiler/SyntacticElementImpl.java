// This file is part of the Java Compiler Kit (JKit)
//
// The Java Compiler Kit is free software; you can 
// redistribute it and/or modify it under the terms of the 
// GNU General Public License as published by the Free Software 
// Foundation; either version 2 of the License, or (at your 
// option) any later version.
//
// The Java Compiler Kit is distributed in the hope
// that it will be useful, but WITHOUT ANY WARRANTY; without 
// even the implied warranty of MERCHANTABILITY or FITNESS FOR 
// A PARTICULAR PURPOSE.  See the GNU General Public License 
// for more details.
//
// You should have received a copy of the GNU General Public 
// License along with the Java Compiler Kit; if not, 
// write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA
//
// (C) David James Pearce, 2009. 

package jkit.compiler;

import java.util.*;
import java.util.concurrent.*;


public class SyntacticElementImpl  implements SyntacticElement {
	protected ArrayList<SyntacticAttribute> attributes;
	
	public SyntacticElementImpl() {
		attributes = new ArrayList<SyntacticAttribute>();
	}
	
	public SyntacticElementImpl(SyntacticAttribute x) {
		attributes = new ArrayList<SyntacticAttribute>();
		attributes.add(x);
	}
	
	public SyntacticElementImpl(List<SyntacticAttribute> attributes) {
		// the following is really necessary to get rid of annoying aliasing
		// problems.
		this.attributes = new ArrayList<SyntacticAttribute>(attributes);			
	}
	
	public SyntacticElementImpl(SyntacticAttribute[] attributes) {
		this.attributes = new ArrayList<SyntacticAttribute>(Arrays.asList(attributes));			
	}
	
	public List<SyntacticAttribute> attributes() { return attributes; }
	
	public <T extends SyntacticAttribute> List<T> attributes(Class<T> c) {
		ArrayList<T> r = new ArrayList<T>();
		for (SyntacticAttribute a : attributes) {
			if (c.isInstance(a)) {
				r.add((T) a);
			}
		}
		return r;
	}
	
	public <T extends SyntacticAttribute> T attribute(Class<T> c) {
		for (SyntacticAttribute a : attributes) {
			if (c.isInstance(a)) {
				return (T) a;
			}
		}
		return null;
	}		
}



