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

public abstract class LispFunction implements LispExpr {
    private final int minParams; // minimal number of parameters
    private final int numEval;   // number of parameters to evaluate	
    
    public LispFunction(int mp, int ne) { minParams=mp; numEval=ne; }
    public LispFunction(int mp) { minParams=mp; numEval=mp; }
    
    public LispExpr invoke(List<LispExpr> es, 
    		HashMap<java.lang.String, LispExpr> locals,
    		HashMap<java.lang.String, LispExpr> globals) {
    	
    	if(es.size() < (minParams+1)) {
    		// insufficient parameters passed
    		throw new Error("insufficient parameters!");
    	} else {
    		// evaluate parameters as necessary
    		LispExpr[] vals = new LispExpr[es.size()-1];
    		
    		for(int i=1;i!=es.size();++i) {
    			LispExpr e = es.get(i);
    			if(i < (1+numEval)) {
    				vals[i-1] = e.evaluate(locals,globals);
    			} else {
    				vals[i-1] = e;
    			}
    		}
    		
    		return internal_invoke(vals,locals,globals);
    	}
    }
    
    public LispExpr evaluate(HashMap<String, LispExpr> locals, 
    		HashMap<String, LispExpr> globals) {
    	return this;
    } 
    
    public boolean equals(Object o) { return this == o; }
    
    // subclasses actually do the invoke ...
    protected abstract LispExpr internal_invoke(LispExpr[] es, 
    		HashMap<java.lang.String, LispExpr> locals,
    		HashMap<java.lang.String, LispExpr> globals);
}
