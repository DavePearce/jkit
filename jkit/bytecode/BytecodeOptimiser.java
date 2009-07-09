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
	/**
	 * This method is called to optimise the list of bytecodes provided by any
	 * means possible; of course, only safe transformations are permitted!
	 * 
	 * @param bytecodes
	 *            The list of bytecodes to optimise!
	 * @param handlers
	 *            The list of exception handlers associated with the bytecodes
	 */
	public abstract void optimise(List<Bytecode> bytecodes,			
			List<ClassFileWriter.ExceptionHandler> handlers);

	/**
	 * The purpose of this method is to replace a number of bytecodes with 0 or
	 * more bytecodes, whilst updating the exception handlers list accordingly.
	 * 
	 * @param start
	 *            The index of the first bytecode to replace
	 * @param length
	 *            The number of bytecodes to replace
	 * @param bytecodes
	 *            The list of bytecodes to update
	 * @param handlers
	 *            The list of exception handlers that must be updated properly
	 * @param replacements
	 *            A list of bytecodes to put in their place
	 */
	public void replace(int start, int length, List<Bytecode> bytecodes,
			List<ClassFileWriter.ExceptionHandler> handlers,
			Bytecode... replacements) {
		int delta = replacements.length - length;
		
		for(int i=0;i!=length;++i) {
			bytecodes.remove(start);
		}
		
		bytecodes.addAll(start,Arrays.asList(replacements));
		
		// Now, update the handlers appropriately		
		int end = start+length;		
		for(ClassFileWriter.ExceptionHandler h : handlers) {			
			if(h.start <= start && h.end > start) {				
				h.end += delta;
			} else if(h.start >= end) {
				h.start += delta;
				h.end += delta;
			} else if (h.start > start) {
				throw new RuntimeException(
						"Attempt to optimise an instruction that partially straddles an exception boundary!");
			}
		}
	}
}
