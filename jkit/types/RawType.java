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
// (C) David James Pearce, 2007. 

package jkit.types;

import jkit.core.*;

// THIS CLASS NEEDS MORE THOUGHT!

@Immutable 
public final class RawType implements TypeElement {
	private final byte NOTRAW=0;
	private final byte RAW=1;
	private byte rawness;
	
	public RawType(boolean b) { 
		if(b) { rawness = NOTRAW; } 
		else { rawness = RAW; }
	}
	
	public TypeElement union(TypeElement _t) {
		RawType t = (RawType) _t;
		
		if(t.rawness == NOTRAW && rawness == NOTRAW) {
			return this;
		} else if(rawness == RAW) {
			return this;
		} else {
			return new RawType(false);
		}
	}

	public TypeElement intersect(TypeElement _t) { 
		RawType t = (RawType) _t;
		if(rawness == RAW && t.rawness == NOTRAW) {
			return t;
		} 
		return this; 
	}
	
	public TypeElement difference(TypeElement _t) { 
		RawType t = (RawType) _t;
		return this; 
	}
	
	public boolean supsetEqOf(TypeElement _t) { 
		RawType t = (RawType) _t;
		return rawness >= t.rawness; 
	}
	
	public boolean supsetOf(TypeElement _t) { 
		RawType t = (RawType) _t;
		return rawness > t.rawness; 
	}
	
	public boolean isEmpty() { return false; }
	
	public boolean equals(Object _t) {
		RawType t = (RawType) _t;
		return rawness == t.rawness; 
	}
		
	public int hashCode() { return rawness; }
	
	public TypeElement emptySet() { return this; }
	
	public TypeElement fullSet() { return this; }

	public String toString() { 
		if(rawness == RAW) { return "@Raw"; } 
		else return "";
	}
	
}
