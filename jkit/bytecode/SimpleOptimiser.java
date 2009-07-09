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

package jkit.bytecode;

import java.util.List;

import jkit.jil.tree.Type;
import jkit.bytecode.Bytecode.*;

/**
 * The simple optimiser provides a very simple implementation which does nothing
 * complex applies a small and simple set of transformation rules.
 * 
 * @author djp
 * 
 */
public class SimpleOptimiser extends BytecodeOptimiser {
	public void optimise(List<Bytecode> bytecodes,
			List<ClassFileWriter.ExceptionHandler> handlers) {
		
		for(int i=0;i<bytecodes.size();++i) {			
			i += tryPushPop(i,bytecodes,handlers);
			i += tryIncPlusOne(i,bytecodes,handlers);
			i += tryIfNonNull(i,bytecodes,handlers);
		}
				
	}
	
	private int tryIfNonNull(int i, List<Bytecode> bytecodes,
			List<ClassFileWriter.ExceptionHandler> handlers) {
		// Need at least two bytecodes remaining
		if((i+1) >= bytecodes.size()) { return 0; }
		Bytecode b1 = bytecodes.get(i);
		Bytecode b2 = bytecodes.get(i+1);
		if(b1 instanceof LoadConst && b2 instanceof IfCmp) {
			LoadConst lc1 = (LoadConst) b1;
			IfCmp ic1 = (IfCmp) b2;
			if(lc1.constant == null && ic1.cond == IfCmp.EQ) {
				// ifnull case
				replace(i,2, bytecodes, handlers, new Bytecode.If(If.NULL,ic1.label));
				return -1;
			} else if(lc1.constant == null && ic1.cond == IfCmp.NE) {
				// ifnonnull case
				replace(i,2, bytecodes, handlers, new Bytecode.If(If.NONNULL,ic1.label));
				return -1;
			}
		}
		return 0;
	}
	
	private int tryPushPop(int i, List<Bytecode> bytecodes,
			List<ClassFileWriter.ExceptionHandler> handlers) {
		// Need at least two bytecodes remaining
		if((i+1) >= bytecodes.size()) { return 0; }
		Bytecode b1 = bytecodes.get(i);
		Bytecode b2 = bytecodes.get(i+1);
		// Now, try to match sequence
		if ((b1 instanceof Load || b1 instanceof LoadConst)
				&& b2 instanceof Pop) {
			replace(i,2,bytecodes,handlers);
			return -2;
		}
		return 0;
	}
	
	private int tryIncPlusOne(int i, List<Bytecode> bytecodes,
			List<ClassFileWriter.ExceptionHandler> handlers) {
		// Need at least four bytecodes remaining
		if((i+3) >= bytecodes.size()) { return 0; }
		Bytecode b1 = bytecodes.get(i);
		Bytecode b2 = bytecodes.get(i+1);
		Bytecode b3 = bytecodes.get(i+2);
		Bytecode b4 = bytecodes.get(i+3);
		// Now, try to match sequence
		if (b1 instanceof Load && b2 instanceof LoadConst
				&& b3 instanceof BinOp && b4 instanceof Store) {

			Load l1 = (Load) b1;
			LoadConst lc2 = (LoadConst) b2;
			BinOp a3 = (BinOp) b3;
			Store s4 = (Store) b4;
			// Need more sanity checks
			if (l1.slot == s4.slot && lc2.constant.equals(1)
					&& a3.op == BinOp.ADD && l1.type instanceof Type.Int) {
				// Ok, matched!
				replace(i, 4, bytecodes, handlers,
						new Bytecode.Iinc(l1.slot, 1));
				return -3;
			}
		}
		return 0;
	}
}
