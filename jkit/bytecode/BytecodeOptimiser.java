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

import jkit.jil.util.Types;
import jkit.bytecode.Bytecode.*;
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
				
				// This could be improved by narrowing down based on e.g. the
				// first bytecode.
				if(rewrite == null) {
					rewrite = tryIncPlusConst(i,bytecodes);
				}
				if(rewrite == null) {
					rewrite = tryIfNonNull(i,bytecodes);
				}
				if(rewrite == null) {
					rewrite = tryConstPlusStore(i,bytecodes);
				}
				if(rewrite == null) {
					rewrite = tryNegRemoval(i,bytecodes);
				}
				if(rewrite == null) {
					rewrite = tryDupLoad(i,bytecodes);
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
				return new Code.Rewrite(i,2,new If(If.NULL,ic1.label));
			} else if(lc1.constant == null && ic1.cond == IfCmp.NE) {
				// ifnonnull case
				return new Code.Rewrite(i,2,new If(If.NONNULL,ic1.label));
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
	 * ldc y or iconst y
	 * iadd or isub
	 * istore x
	 * </pre>
	 * and replaces it with the following:
	 * <pre>
	 * iinc x,y
	 * </pre>
	 * @param i
	 * @param bytecodes
	 * @return
	 */
	protected Code.Rewrite tryIncPlusConst(int i, List<Bytecode> bytecodes) {
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
			Object constant = lc2.constant;
			if (l1.slot == s4.slot && constant instanceof Number
					&& l1.type instanceof Type.Int) {
				int c = ((Number) constant).intValue();
				
				if (a3.op == BinOp.ADD) {
					return new Code.Rewrite(i, 4, new Bytecode.Iinc(l1.slot, c));
				} else if (a3.op == BinOp.SUB) {
					return new Code.Rewrite(i, 4,
							new Iinc(l1.slot, -c));
				}
			}
		}
		return null;
	}
	
	/**
	 * This rewrite looks for the following pattern:
	 * <pre>	 
	 * ldc y or iconst y
	 * iadd or isub
	 * istore x
	 * </pre>
	 * and replaces it with the following:
	 * <pre>
	 * istore x
	 * iinc x,y
	 * </pre>
	 * @param i
	 * @param bytecodes
	 * @return
	 */
	protected Code.Rewrite tryConstPlusStore(int i, List<Bytecode> bytecodes) {
		// Need at least three bytecodes remaining
		if((i+2) >= bytecodes.size()) { return null; }
		Bytecode b1 = bytecodes.get(i);
		Bytecode b2 = bytecodes.get(i+1);
		Bytecode b3 = bytecodes.get(i+2);		
		// Now, try to match sequence
		if (b1 instanceof LoadConst && b2 instanceof BinOp
				&& b3 instanceof Store) {
			LoadConst lc1 = (LoadConst) b1;
			BinOp bo2 = (BinOp) b2;
			Store st3 = (Store) b3;
			if(st3.type instanceof Type.Int && lc1.constant instanceof Number) {
				int c = ((Number) lc1.constant).intValue();
				if(bo2.op == BinOp.ADD) {
					return new Code.Rewrite(i, 3,
							st3,
							new Iinc(st3.slot, c));
				} else if (bo2.op == BinOp.SUB) {
					return new Code.Rewrite(i, 3,
							st3,
							new Iinc(st3.slot, -c));
				}
			}
		}
		return null;
	}
	
	/**
	 * This rewrite looks for the following pattern:
	 * <pre>	 
	 * ldc y or iconst y
	 * ineg
	 * </pre>
	 * and replaces it with the following:
	 * <pre>
	 * ldc -y or iconst -y
	 * </pre>
	 * @param i
	 * @param bytecodes
	 * @return
	 */
	protected Code.Rewrite tryNegRemoval(int i, List<Bytecode> bytecodes) {
		// Need at least two bytecodes remaining
		if((i+1) >= bytecodes.size()) { return null; }
		Bytecode b1 = bytecodes.get(i);
		Bytecode b2 = bytecodes.get(i+1);
		if(b1 instanceof LoadConst && b2 instanceof Neg) {
			LoadConst lc1 = (LoadConst) b1;
			Object constant = lc1.constant;
		
			if (constant instanceof Long) {
				long c = (Long) constant;
				if (c != Long.MIN_VALUE) {
					return new Code.Rewrite(i, 2, new LoadConst(-c));
				}				
			} else if (constant instanceof Float) {
				float c = (Float) constant;				
				return new Code.Rewrite(i, 2, new LoadConst(-c));				
			} else if (constant instanceof Double) {
				double c = (Double) constant;				
				return new Code.Rewrite(i, 2, new LoadConst(-c));				
			} else if (constant instanceof Number) {
				int c = ((Number) constant).intValue();
				if (c != Integer.MIN_VALUE) {
					return new Code.Rewrite(i, 2, new LoadConst(-c));
				}				
			}  
		}
		return null;
	}	
	
	/**
	 * This rewrite looks for the following pattern:
	 * <pre>	 
	 * ldc y or iconst y or load x
	 * ldc y or iconst y or load x
	 * </pre>
	 * and replaces it with the following:
	 * <pre>
	 * ldc y or iconst y
	 * dup
	 * </pre>
	 * @param i
	 * @param bytecodes
	 * @return
	 */
	protected Code.Rewrite tryDupLoad(int i, List<Bytecode> bytecodes) {
		if((i+1) >= bytecodes.size()) { return null; }
		Bytecode b1 = bytecodes.get(i);
		Bytecode b2 = bytecodes.get(i+1);
		if(b1 instanceof Load && b1.equals(b2)) {
			Load l1 = (Load) b1;
			return new Code.Rewrite(i, 2, l1,new Dup(l1.type));
		} else if(b1 instanceof GetField && b1.equals(b2)) {
			GetField l1 = (GetField) b1;
			return new Code.Rewrite(i, 2, l1,new Dup(l1.type));
		} else if(b1 instanceof LoadConst && b1.equals(b2)) {
			LoadConst lc1 = (LoadConst) b1;
			Object constant = lc1.constant;
			
			if(constant instanceof Long) {
				return new Code.Rewrite(i, 2, b1,new Dup(Types.T_LONG));
			} else if(constant instanceof Float) {
				return new Code.Rewrite(i, 2, b1,new Dup(Types.T_FLOAT));
			} else if(constant instanceof Double) {
				return new Code.Rewrite(i, 2, b1,new Dup(Types.T_DOUBLE));
			} else if(constant instanceof Number) {
				return new Code.Rewrite(i, 2, b1,new Dup(Types.T_INT));
			} else {
				// this is a general catch all for aconst instructions.
				return new Code.Rewrite(i, 2, b1,new Dup(Types.JAVA_LANG_OBJECT));
			} 
		}
		return null;
	}
}
