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

public final class BytecodeOptimiser {
	
	public int optimise(ClassFile classFile) {
		int numRewrites = 0;
		if(!classFile.isInterface()) {
			// no point trying to optimise interfaces
			for(ClassFile.Method method : classFile.methods) {
				if(!method.isAbstract()) {
					// likewise, no point trying to optimise abstract methods.
					numRewrites += optimise(method);	
				}				
			}
		}
		return numRewrites;
	}
	
	protected int optimise(ClassFile.Method method) {		
		ArrayList<Code.Rewriteable> rewritables = new ArrayList<Code.Rewriteable>();
		Code code = null;
		
		for(Attribute attr : method.attributes) {
			if(attr instanceof Code) {
				code = (Code) attr;				
			} else if(attr instanceof Code.Rewriteable) {
				rewritables.add((Code.Rewriteable) attr);
			}
		}
		
		if(code != null) {
			return optimise(method,code,rewritables);
		} else {
			return 0;
		}
	}
	
	protected int optimise(ClassFile.Method method, Code code,
			ArrayList<Code.Rewriteable> rewritables) {
		int numRewrites = 0;
		List<Bytecode> bytecodes = code.bytecodes();
		ArrayList<Code.Rewrite> rewrites = new ArrayList<Code.Rewrite>();
		
		do {
			rewrites.clear();
			for(int i=0;i<bytecodes.size();++i) {			
				Code.Rewrite rewrite;
				rewrite = tryPushPop(i,bytecodes);
				if(rewrite == null) {
					rewrite = tryIncPlusOne(i,bytecodes);
				}
				if(rewrite == null) {
					rewrite = tryIfNonNull(i,bytecodes);
				}
				if(rewrite != null) {
					rewrites.add(rewrite);
					i = i + rewrite.length - 1;
				}
			}

			// At this stage, we apply the rewrites that we have.
			for(Code.Rewriteable cr : rewritables) {
				cr.apply(rewrites);
			}
			code.apply(rewrites);
			numRewrites += rewrites.size();
		} while(rewrites.size() > 0);
				
		return numRewrites;
	}
	
	/**
	 * This rewrite looks for the following patterns:
	 * <pre>
	 * ldc null
	 * ifeq X
	 * </pre>
	 * and replaces them with:
	 * <pre>
	 * ifnull X
	 * </pre>
	 * It also catches the ifnonnull case.
	 * @param i
	 * @param bytecodes
	 * @return
	 */
	protected Code.Rewrite tryIfNonNull(int i, List<Bytecode> bytecodes) {
		// Need at least two bytecodes remaining
		if((i+1) >= bytecodes.size()) { return null; }
		Bytecode b1 = bytecodes.get(i);
		Bytecode b2 = bytecodes.get(i+1);
		if(b1 instanceof LoadConst && b2 instanceof IfCmp) {
			LoadConst lc1 = (LoadConst) b1;
			IfCmp ic1 = (IfCmp) b2;
			if(lc1.constant == null && ic1.cond == IfCmp.EQ) {
				// ifnull case
				return new Code.Rewrite(i,2,new Bytecode.If(If.NULL,ic1.label));
			} else if(lc1.constant == null && ic1.cond == IfCmp.NE) {
				// ifnonnull case
				return new Code.Rewrite(i,2,new Bytecode.If(If.NONNULL,ic1.label));
			}
		}
		return null;
	}
	
	/**
	 * This rewrite looks for the pattern where a value is pushed onto the
	 * stack, and then immediately popped off. In this case, it simply removes
	 * the bytecodes in question.
	 * 
	 * @param i
	 * @param bytecodes
	 * @return
	 */
	protected Code.Rewrite tryPushPop(int i, List<Bytecode> bytecodes) {
		// Need at least two bytecodes remaining
		if((i+1) >= bytecodes.size()) { return null; }
		Bytecode b1 = bytecodes.get(i);
		Bytecode b2 = bytecodes.get(i+1);
		// Now, try to match sequence
		if ((b1 instanceof Load || b1 instanceof LoadConst)
				&& b2 instanceof Pop) {
			return new Code.Rewrite(i,2);
		}
		return null;
	}
	
	/**
	 * This rewrite looks for the following pattern:
	 * <pre>
	 * iload x
	 * ldc 1
	 * add
	 * istore x
	 * </pre>
	 * and replaces it with the following:
	 * <pre>
	 * iinc x,1
	 * </pre>
	 * @param i
	 * @param bytecodes
	 * @return
	 */
	protected Code.Rewrite tryIncPlusOne(int i, List<Bytecode> bytecodes) {
		// Need at least four bytecodes remaining
		if((i+3) >= bytecodes.size()) { return null; }
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
				return new Code.Rewrite(i,4,new Bytecode.Iinc(l1.slot, 1));				
			}
		}
		return null;
	}
}
