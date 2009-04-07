package jkit.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import jkit.bytecode.ClassFile.Method;
import jkit.jil.tree.Type;
import jkit.jil.util.*;

/**
 * This represents the Code attribute from the JVM Spec.
 * 
 * @author djp
 */
public class Code implements Attribute {

	protected List<Bytecode> bytecodes;
	protected List<Handler> handlers;
	protected Method method; // enclosing method

	public Code(List<Bytecode> bytecodes,
			List<Handler> handlers, Method method) {			
		this.bytecodes = bytecodes;
		this.handlers = handlers;
		this.method = method;
	}

	public String name() { return "Code"; }

	/**
	 * Determine the maximum number of local variable slots required for
	 * this method.
	 * 
	 * @return
	 */
	public int maxLocals() {
		int max = 0;
		for(Bytecode b :  bytecodes) {
			if(b instanceof Bytecode.Store) {
				Bytecode.Store s = (Bytecode.Store) b;
				max = Math.max(max, s.slot + ClassFile.slotSize(s.type) - 1);
			} else if(b instanceof Bytecode.Load) {
				Bytecode.Load l = (Bytecode.Load) b;
				max = Math.max(max, l.slot + ClassFile.slotSize(l.type) - 1);					
			} else if(b instanceof Bytecode.Iinc) {
				Bytecode.Iinc l = (Bytecode.Iinc) b;
				max = Math.max(max, l.slot);					
			}
		}

		// The reason for the following, is that we must compute the
		// *minimal* number of slots required. Essentially, this is enough
		// to hold the "this" pointer (if appropriate) and the parameters
		// supplied. The issue is that the bytecodes might not actually
		// access all of the parameters supplied, so just looking at them
		// might produce an underestimate.

		int min = method.isStatic() ? 0 : 1;

		for(Type p :  method.type().parameterTypes()) {
			min += ClassFile.slotSize(p);
		}

		return Math.max(max+1,min);
	}

	/**
	 * Determine the maximum number of stack slots required for this method.
	 * 
	 * @return
	 */
	public int maxStack() {
		// This algorithm computes a conservative over approximation. In
		// theory, we can do better, but there's little need to.
		int max = 0;
		int current = 0;
		int idx = 0;
		HashSet<Integer> handlerStarts = new HashSet<Integer>();
		for(Handler h : handlers) {
			handlerStarts.add(h.start);
		}
		for(Bytecode b : bytecodes) {
			if(handlerStarts.contains(idx)) {
				// This bytecode is the first of an exception handler. Such
				// handlers begin with the thrown exception object on the stack,
				// hence we must account for this.
				current = current + 1;
			}
			current = current + b.stackDiff();
			max = Math.max(current,max);
			idx = idx + 1;
		}
		return max;
	}

	public List<Bytecode> bytecodes() { 
		return bytecodes;
	}

	public List<Handler> handlers() {
		return handlers;
	}

	public void addPoolItems(Set<Constant.Info> constantPool) {
		Constant.addPoolItem(new Constant.Utf8("Code"), constantPool);

		for (Bytecode b : bytecodes()) {
			b.addPoolItems(constantPool);
		}

		for(Handler h : handlers) {
			if(!Types.isClass("java.lang","Throwable",h.exception)) {
				Constant.addPoolItem(Constant.buildClass(h.exception), constantPool);
			}
		}
	}
	
	/**
	 * The exception handler class is used to store the necessary information
	 * about where control-flow is directed when an exception is raised.
	 * 
	 * @author djp
	 * 
	 */
	public static class Handler {
		/**
		 * The start index of bytecodes covered by the handler.
		 */
		public int start;
		/**
		 * One past the last index covered by the handler.
		 */
		public int end;
		public String label; 
		public Type.Clazz exception;

		public Handler(int start, int end, String label,
				Type.Clazz exception) {
			this.start = start;
			this.end = end;
			this.label = label;
			this.exception = exception;
		}
	}	
	
	public void write(BinaryOutputStream writer,
			Map<Constant.Info, Integer> constantPool) throws IOException {

		// This method is a little tricky. The basic strategy is to first
		// translate each bytecode into it's binary representation. One
		// difficulty here, is that we must defer calculating the targets of
		// branch statements until after this is done, since we can't do the
		// calculation without exact values.

		// === DETERMINE LABEL OFFSETS ===

		HashMap<String, Integer> labelOffsets = new HashMap<String, Integer>();			

		int offset = 0;

		// The insnOffsets is used to map the statement index to the
		// corresponding bytecodes. This is used in determining the start and
		// end offsets for the exception handlers

		int[] insnOffsets = new int[bytecodes.size()];
		int omi = 0;

		for (Bytecode b : bytecodes) {
			insnOffsets[omi++] = offset;
			if (b instanceof Bytecode.Label) {
				Bytecode.Label l = (Bytecode.Label) b;
				labelOffsets.put(l.name, offset);
			} else if (b instanceof Bytecode.Branch) {
				// In this case, we can't determine the offset of the
				// label, since we may not have passed it yet!
				// Therefore, for now, I assume that the bytecode requires 3
				// bytes (which is true, except for goto_w).
				offset += 3;
			} else if (b instanceof Bytecode.Switch) {
				// calculate switch statement size
				offset += ((Bytecode.Switch) b).getSize(offset);
			} else {
				offset += b.toBytes(offset, labelOffsets, constantPool).length;
			}
		}

		// === CREATE BYTECODE BYTES ===

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		offset = 0;
		for (Bytecode b : bytecodes) {
			byte[] bs = b.toBytes(offset, labelOffsets, constantPool);
			bout.write(bs);
			offset += bs.length;
			if (b instanceof Bytecode.Branch && bs.length > 3) {
				// In this situation the size of the branch is bigger than
				// originally calculated in the second sweep. For now, I just
				// abort in this somewhat unlikely event.
				throw new RuntimeException(
				"Internal failure --- branch too big!");
			}
		}

		// === WRITE CODE ATTRIBUTE ===
		byte[] bytes = bout.toByteArray();

		writer.write_u2(constantPool.get(new Constant.Utf8("Code")));
		// need to figure out exception_table length
		int exception_table_length = handlers().size() * 8;
		// need to figure out attribute_table length
		int attribute_table_length = 0;
		// write attribute length
		writer.write_u4(bytes.length + exception_table_length + attribute_table_length
				+ 12);
		// now write data
		writer.write_u2(maxStack());
		writer.write_u2(maxLocals());
		writer.write_u4(bytes.length);
		// write bytecode instructions
		for (int i = 0; i != bytes.length; ++i) {
			writer.write_u1(bytes[i]);
		}

		// write exception handlers
		writer.write_u2(handlers().size());
		for (Handler h : handlers()) {
			writer.write_u2(insnOffsets[h.start]);
			writer.write_u2(insnOffsets[h.end]);
			writer.write_u2(labelOffsets.get(h.label));

			if (Types.isClass("java.lang", "Throwable", h.exception)) {
				writer.write_u2(0);
			} else {
				writer.write_u2(constantPool.get(Constant
						.buildClass(h.exception)));
			}
		}
		writer.write_u2(0); // no attributes for now
	}
	
	public void print(PrintWriter output, Map<Constant.Info, Integer> constantPool) {
		output.println("  Code:");
		output.println("   stack = " + maxStack() + ", locals = "
				+ maxLocals());

		for (Bytecode b : bytecodes) {
			if(b instanceof Bytecode.Label) {
				output.println("  " + b);
			} else {
				output.println("   " + b);
			}
		}
	}		
}	
