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

import java.util.*;

import jkit.bytecode.Bytecode.BinOp;
import jkit.bytecode.Bytecode.If;
import jkit.bytecode.Bytecode.IfCmp;
import jkit.bytecode.Bytecode.Load;
import jkit.bytecode.Bytecode.LoadConst;
import jkit.bytecode.Bytecode.Pop;
import jkit.bytecode.Bytecode.Store;
import jkit.jil.tree.Type;

/**
 * A bytecode optimiser is essentially a peep hole optimiser. The aim is to
 * identify and reduce common patterns that are inefficient. For example, the
 * following pattern:
 * 
 * <pre>
 * public static void main(java.lang.String[]);
 * Code:
 *  Stack=2, Locals=2, Args_size=1
 *  0:   iconst_0
 *  1:   istore_1
 *  2:   iload_1
 *  3:   iconst_1
 *  4:   iadd
 *  5:   istore_1
 *  6:   return
 * </pre>
 * 
 * Can be reduced to this:
 * 
 * <pre>
 * public static void main(java.lang.String[]);
 * Code:
 *  Stack=2, Locals=2, Args_size=1
 *  0:   iconst_0
 *  1:   istore_1
 *  2:   iinc 1,1
 *  5:   return
 * </pre>
 * 
 * Likewise, the following code:
 * 
 * <pre>
 * public static void main(java.lang.String[]);
 * Code:
 *  Stack=1, Locals=2, Args_size=1
 *  0:   iconst_0
 *  1:   istore_1
 *  2:   aload_0
 *  3:   arraylength
 *  4:   tableswitch{ //0 to 1
 *               0: 31;
 *               1: 28;
 *               default: 33 }
 *  28:  goto    33
 *  31:  iconst_1
 *  32:  istore_1
 *  33:  return
 * </pre>
 * 
 * Can be reduced to this:
 * 
 * <pre>
 * public static void main(java.lang.String[]);
 * Code:
 *  Stack=1, Locals=2, Args_size=1
 *  0:   iconst_0
 *  1:   istore_1
 *  2:   aload_0
 *  3:   arraylength
 *  4:   lookupswitch{ //2
 *               0: 32;
 *               1: 37;
 *               default: 37 }
 *  32:  iconst_1
 *  33:  istore_1
 *  34:  goto    37
 *  37:  return
 * </pre>
 * 
 * @author djp
 * 
 */

public abstract class BytecodeOptimiser {	
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
