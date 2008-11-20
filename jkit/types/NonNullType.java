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

@Immutable
public class NonNullType implements TypeElement {
	public final static int NULLABLE = 3;
	public final static int NULL = 2;
	public final static int NONNULL = 1;
	public final static int UNDEFINED = 0;
	
	private int NonNull;
	
	public NonNullType(int nonnull) { NonNull = nonnull; }
	
	public TypeElement fullSet() { return new NonNullType(NULLABLE); }
	public TypeElement emptySet() { return new NonNullType(UNDEFINED); }
	
	// must return this if result is unchanged!
	public TypeElement union(TypeElement _t) {
		NonNullType t = (NonNullType) _t;

		int nNN = NonNull | t.NonNull;
		
		return (nNN == NonNull) ? this : new NonNullType(nNN);
		// this is ugly.
		/*if(NonNull == NULLABLE) { return this; }
		
		if((NonNull + t.NonNull) == NULLABLE)
			return new NonNullType(NULLABLE);
		
		if(NonNull >= t.NonNull) {
			return this;
		} else {
			return new NonNullType(t.NonNull);
		}*/
	}
	
	public TypeElement intersect(TypeElement _t) {
		NonNullType t = (NonNullType) _t;
		
		return new NonNullType(NonNull & t.NonNull);
		
		/*if((NonNull + t.NonNull) == NULLABLE)
			return new NonNullType(UNDEFINED);
		
		return new NonNullType(Math.min(NonNull, t.NonNull));*/
	}
	
	public TypeElement difference(TypeElement _t) {
		NonNullType t = (NonNullType) _t;
		
		/*if(NonNull == t.NonNull)
			return this;
		else if ((NonNull == NULLABLE) && (t.NonNull != UNDEFINED))
			return new NonNullType(NULLABLE - t.NonNull);
		
		return this;*/
		int nNN = NonNull - t.NonNull;
		
		if(nNN == NONNULL)
			return new NonNullType(nNN);
		
		return this;
	}
	
	public boolean supsetEqOf(TypeElement _t) {
		NonNullType t = (NonNullType) _t;
		
		/*if((NonNull * t.NonNull) == NULL)
			return false;
		
		return t.NonNull <= NonNull;*/
		return (NonNull | t.NonNull) == NonNull; 
	}

	public boolean supsetOf(TypeElement _t) {
		NonNullType t = (NonNullType) _t;
		
		/*if((NonNull * t.NonNull) == NULL)
			return false;
		
		return t.NonNull < NonNull;*/
		return (NonNull != t.NonNull) && ((NonNull | t.NonNull) == NonNull);
	}

	public boolean isEmpty() {
		return NonNull == NonNullType.UNDEFINED;
	}
	
	public boolean equals(Object _t) {
		NonNullType t = (NonNullType) _t;
		return NonNull == t.NonNull;
	}
		
	public int hashCode() { return NonNull; }
	
	public String toString() {
		switch(NonNull) {
		case 3:
			return "";
		case 2:
			return "@Null";
		case 1:
			return "@NonNull";
		case 0:
			return "";
		}
		
		return "NoType";
	}

	public static void main(String[] args) {
		TypeElement a = new NonNullType(NonNullType.NULLABLE);
		TypeElement b = new NonNullType(NonNullType.NULL);
		TypeElement c = new NonNullType(NonNullType.NONNULL);
		TypeElement d = new NonNullType(NonNullType.UNDEFINED);
		
		System.out.println(a.difference(c));
	}	
}
