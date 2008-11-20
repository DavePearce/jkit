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

public class RawTypeFactory implements TypeElementFactory {
	public @NonNull RawType classConstant(String s) {
		return new RawType(true);
	}
	
	public @NonNull RawType thisType(TypeInfo t) {
		return new RawType(true);
	}

	public @NonNull RawType newType(TypeInfo t) {
		return new RawType(true);
	}
	
	public @NonNull RawType unknownType(TypeInfo t, String... annotations) {
		if(t.isReference()) {			
			for(String a : annotations) {
				if(a.equals("jack/types/Raw")) {
					return new RawType(false);
				}
			}			
		}
		return new RawType(true);		
	}

	public @NonNull RawType knownType(TypeInfo t, Object c, String... annotations) {				
		return new RawType(true);
	}
	
	public @NonNull RawType derefType() { return new RawType(false); }
}
