// This file is part of the Simple Lisp Interpreter.
//
// The Simple Lisp Interpreter is free software; you can 
// redistribute it and/or modify it under the terms of the 
// GNU General Public License as published by the Free Software 
// Foundation; either version 2 of the License, or (at your 
// option) any later version.
//
// The Simpular Program Interpreter is distributed in the hope
// that it will be useful, but WITHOUT ANY WARRANTY; without 
// even the implied warranty of MERCHANTABILITY or FITNESS FOR 
// A PARTICULAR PURPOSE.  See the GNU General Public License 
// for more details.
//
// You should have received a copy of the GNU General Public 
// License along with the Simpular Program Interpreter; if not, 
// write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA
//
// (C) David James Pearce, 2005. 

package org.simplelisp.interpreter;

import java.util.*;
import java.io.*;

public class PrettyPrinter {
	public static String prettyPrint(LispExpr e) {
		// first list is always a progn ...
		LispList es = (LispList) e;
		String r = "";
		for(int i=1;i!=es.size();++i) {
			r += print(es.get(i),0) + "\n";
		}
		return r;
	}
	private static String print(LispExpr e, int l) {
		// this function recursively traverses
		// the Abstract Syntax Tree and prints it out,
		// whilst making an effort to do this neatly.
		
		if(e instanceof LispInteger ||
			e instanceof LispChar ||
			e instanceof LispNil ||
			e instanceof LispSymbol) {
			return e.toString();	    
			
		} else if(e instanceof LispString) {
			return "\"" + e.toString() + "\"";
			
		} else if(e instanceof LispQuote) {
			LispQuote q = (LispQuote) e;
			return "'" + print(q.getExpr(), l);
			
		} else if(e instanceof LispList) {
				    
			LispList es = (LispList) e;
			if(es.size() == 0) { return "()"; }
			else {
				LispExpr head = es.get(0);
				String r = "(";
				
				// catch special cases
				if(head instanceof LispSymbol) {					
					LispSymbol fn = (LispSymbol) head;
					if(fn.name().equals("progn")) {
						r += "progn ";						
						for(int i=1;i<es.size();++i) {
							r += newLine(l+6) + print(es.get(i), l+6);
						}		
						return r + ")";
					} else if(fn.name().equals("defun") && es.size() > 1) {
						r += "defun ";
						r += print(es.get(1),l) + " ";
						r += print(es.get(2),l);
						for(int i=3;i<es.size();++i) {
							r += newLine(l+6) + print(es.get(i), l+6);
						}		
						return r + ")" + newLine(l) + newLine(l);
					} else if(fn.name().equals("if") && es.size() == 4) {						
						r += "if ";
						r += print(es.get(1),l);
						r += newLine(l+6) + print(es.get(2), l+6);
						r += newLine(l+3) + print(es.get(3), l+3);
						return r + ")";
					} else if(fn.name().equals("if") && es.size() == 3) {						
						r += "if ";
						r += print(es.get(1),l);
						r += newLine(l+6) + print(es.get(2), l+6);						
						return r + ")";
					} else if(fn.name().equals("let") && es.size() == 3) {						
						r += "let (";
						LispList vars = (LispList) es.get(1);
						for(LispExpr v : vars) {
							r += newLine(l+3);
							r += print(v,l+3);
						}
						r += ")" + newLine(l+6);						
						r += print(es.get(2),l);											
						return r + ")";
					}
				
				} 
				
				boolean firstTime=true;			
				for(LispExpr le : (LispList) e) {
					if(!firstTime) { r += " "; }
					firstTime=false;
					r += print(le, l);
				}
				return r + ")";
			}			
		} else if(e instanceof LispVector) {
			String r = "#(";	    
			boolean firstTime=true;
			for(LispExpr le : (LispVector) e) {
				if(!firstTime) { r += " "; }
				firstTime=false;
				r += print(le, l);
			}
			return r + ")";
		} 
		return "UNKNOWN";	
	}	
	
	public static String newLine(int l) {
		String r = "\n";
		while(l > 0) {
			r += " ";
			l=l-1;
		}
		return r;
	}
}
