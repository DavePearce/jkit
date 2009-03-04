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

package jkit.bytecode;

import java.io.*;
import java.util.*;

import jkit.compiler.FieldNotFoundException;
import jkit.compiler.MethodNotFoundException;

import jkit.jil.tree.*;
import jkit.jil.util.*;
import jkit.util.*;

public class ClassFileWriter {
	protected final OutputStream output;
	protected final BytecodeOptimiser optimiser;
	protected final ClassLoader loader;
	protected final boolean outputText;
	protected final int version;

	/**
	 * Construct a ClassFile writer Object that uses the SimpleOptimiser for
	 * peephole optimisation. Uses the default classfile version of 49.
	 * 
	 * @param o
	 *            Output stream for class bytes
	 */
	public ClassFileWriter(OutputStream o, ClassLoader loader) {
		output = o;
		version = 49;
		optimiser = new SimpleOptimiser();
		outputText = false;
		this.loader = loader;
	}

	/**
     * Construct a ClassFile writer Object that accepts a variable length list
     * of options (this is used by the JKit Main method).
     * 
     * @param o
     *            Output stream for class bytes
     * @param options
     *            List of string pairs (x,y), means that property x should have
     *            value y.  Current list of supported properties is:
     *            
     * <table>
     * <tr><td>Propery</td><td>Default Value</td></tr>
     * <tr><td>version</td><td>49</td></tr>
     * <tr><td>optimiser</td><td>jkit.util.bytecode.SimpleOptimiser</td></tr>
     * </table>            
     */
	public ClassFileWriter(OutputStream o, ClassLoader loader,
			List<Pair<String, String>> options) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		output = o;
		int myVersion = 49;
		boolean myOutputText = false;
		BytecodeOptimiser myOptimiser = new SimpleOptimiser();

		for (Pair<String, String> option : options) {
			if (option.first().equals("version")) {
				myVersion = Integer.parseInt(option.second());
			} else if (option.first().equals("optimiser")) {
				myOptimiser = (BytecodeOptimiser) Class
						.forName(option.second()).newInstance();
			} else if (option.first().equals("outputText")) {
				myOutputText = Boolean.parseBoolean(option.second());
			}
		}

		version = myVersion;
		optimiser = myOptimiser;
		outputText = myOutputText;
		this.loader = loader;
	}
	
	public void writeClass(Clazz clazz) throws IOException {
		HashMap<Constant.Info, Integer> constantPool = buildConstantPool(clazz);

		// build reverse map
		Constant.Info[] poolSequence = new Constant.Info[constantPool.size()];
		for (Map.Entry<Constant.Info, Integer> e : constantPool.entrySet()) {
			poolSequence[e.getValue()] = e.getKey();
		}

		write_u1(0xCA);
		write_u1(0xFE);
		write_u1(0xBA);
		write_u1(0xBE);
		write_u4(version);
		write_u2(constantPool.size());
		// now, write the constant pool
		for (Constant.Info c : poolSequence) {
			if (c != null) { // item at index 0 is always null
				output.write(c.toBytes(constantPool));
			}
		}
		// ok, done that now write more stuff
		writeModifiers(clazz.modifiers());
		write_u2(constantPool.get(Constant.buildClass(clazz.type())));
		if (clazz.superClass() != null) {
			write_u2(constantPool.get(Constant.buildClass(clazz.superClass())));
		}
		write_u2(clazz.interfaces().size());
		for (Type.Reference i : clazz.interfaces()) {
			write_u2(constantPool.get(Constant.buildClass(i)));
		}
		write_u2(clazz.fields().size());
		for (Field f : clazz.fields()) {
			writeField(f, constantPool);
		}
		write_u2(clazz.methods().size());
		for (Method m : clazz.methods()) {
			writeMethod(clazz, m, constantPool);
		}

		int nattributes = 0;
		
		if(needClassSignature(clazz)) { nattributes++; }
		
		// FIXME: support for inner classes
		// if(clazz.inners().size() > 0 || clazz.isInnerClass()) { nattributes++; }
		
		write_u2(nattributes);
		
		if (needClassSignature(clazz)) {			
			writeClassSignature(clazz, constantPool);
		}

		// FIXME: support for inner classes
		// if (clazz.inners().size() > 0 || clazz.isInnerClass()) {			
		//	writeInnerClassAttribute(clazz, constantPool);
		// }
		
		output.flush();
	}

	protected void writeClassSignature(Clazz clazz,
			HashMap<Constant.Info, Integer> pmap) throws IOException {		
		write_u2(pmap.get(new Constant.Utf8("Signature")));
		write_u4(2); 
		write_u2(pmap.get(new Constant.Utf8(Types.classSignature(clazz))));
	}

	/**
     * Write attribute detailing what direct inner classes there are for this
     * class, or what inner class this class is in.
     * 
     * @param clazz
     * @param pmap
     */
	protected void writeInnerClassAttribute(Clazz clazz,
			HashMap<Constant.Info, Integer> pmap) throws IOException {
		write_u2(pmap.get(new Constant.Utf8("InnerClasses")));
		
		// FIXME: support for inner classes
		// int ninners = clazz.inners().size() + clazz.type().components().size() - 1;
		int ninners = clazz.type().components().size() - 1;
		
		write_u4(2 + (8 * ninners));
		write_u2(ninners);
		
		if(clazz.isInnerClass()) {
			Type.Clazz inner = clazz.type();
			List<Pair<String,List<Type.Reference>>> classes = clazz.type().components();
			for(int i=classes.size()-1;i>0;--i) {		
				// First, we need to construct the outer reference type.
				List<Pair<String,List<Type.Reference>>> nclasses = new ArrayList();
				for(Pair<String,List<Type.Reference>> p : classes) {
					nclasses.add(p);
				}							
				Type.Clazz outer = new Type.Clazz(inner.pkg(),nclasses);
				// Now, we can actually write the information.
				write_u2(pmap.get(Constant.buildClass(inner)));
				write_u2(pmap.get(Constant.buildClass(outer)));
				write_u2(pmap.get(new Constant.Utf8(inner.components().get(
						inner.components().size() - 1).first())));
				// FIXME: support for inner classes
				// try {
					// This dependence on ClassTable here is annoying really.
				//	Clazz innerC = ClassTable.findClass(inner);
				//	write_u2(innerC.modifiers());
				//} catch(ClassNotFoundException e) {
					write_u2(0); // this is a problem!!!!
				// }
				inner = outer;				
			}
		}		
		
		// FIXME: support for inner classes
		/*
		for(Triple<Type.Reference,Integer,Boolean> i : clazz.inners()) {
			write_u2(pmap.get(Constant.buildClass(i.first())));
			write_u2(pmap.get(Constant.buildClass(clazz.type())));
			write_u2(pmap.get(new Constant.Utf8(i.first().name())));
			write_u2(i.second());			
		}
		*/
	}
	
	protected void writeField(Field f, HashMap<Constant.Info, Integer> pmap)
			throws IOException {
		writeModifiers(f.modifiers());
		write_u2(pmap.get(new Constant.Utf8(f.name())));
		write_u2(pmap.get(new Constant.Utf8(Types.descriptor(f.type(), false))));
		
		// FIXME: support for constant values
		// int attrNum = ((isGeneric(f.type())) ? 1 : 0) + ((f.constantValue() != null) ? 1 : 0);
		
		int attrNum = ((isGeneric(f.type())) ? 1 : 0);
		// Write number of attributes
		write_u2(attrNum);
		// We only need to write a Signature attribute if the field has generic params
		if (isGeneric(f.type())) {
			write_u2(pmap.get(new Constant.Utf8("Signature")));
			write_u4(2);
			write_u2(pmap.get(new Constant.Utf8(Types
					.descriptor(f.type(), true))));
		}
		// FIXME: support for constant values
//		if(f.constantValue() != null) {
//			write_u2(pmap.get(new Constant.Utf8("ConstantValue")));
//			write_u4(2);
//			if(f.constantValue() instanceof java.lang.Number) {
//				write_u2(pmap.get(Constant.fromNumber((java.lang.Number) f.constantValue())));
//			} else {
//				write_u2(pmap.get(Constant.fromString((String) f.constantValue())));
//			}
//		}
	}

	protected void writeMethod(Clazz c, Method m,
			HashMap<Constant.Info, Integer> pmap) throws IOException {

		writeModifiers(m.modifiers());

		if (m.name().equals(c.name())) {
			// this is a constructor
			write_u2(pmap.get(new Constant.Utf8("<init>")));
		} else {
			write_u2(pmap.get(new Constant.Utf8(m.name())));
		}

		write_u2(pmap.get(new Constant.Utf8(Types.descriptor(m.type(), false))));

		int nattrs = 0;
		if (m.body() != null) {
			nattrs++;
		}
		if (!m.exceptions().isEmpty()) {
			nattrs++;
		}
		if(isGeneric(m.type().returnType())) {
			nattrs++;
		}

		write_u2(nattrs);
		if (m.body() != null) {
			writeCodeAttribute(c, m, pmap);
		}
		if (!m.exceptions().isEmpty()) {
			writeExceptionsAttribute(c, m, pmap);
		}
		if(isGeneric(m.type().returnType())) {
			write_u2(pmap.get(new Constant.Utf8("Signature")));
			write_u4(2);
			write_u2(pmap.get(new Constant.Utf8(Types
					.descriptor(m.type(), true))));
		}
	}

	protected void writeExceptionsAttribute(Clazz c, Method method,
			HashMap<Constant.Info, Integer> constantPool) throws IOException {

		List<Type.Clazz> exceptions = method.exceptions();
		write_u2(constantPool.get(new Constant.Utf8("Exceptions")));
		write_u4(2 + (2 * exceptions.size()));
		write_u2(exceptions.size());
		for (Type.Clazz e : exceptions) {
			write_u2(constantPool.get(Constant.buildClass(e)));
		}
	}

	/**
	 * The exception handler class is used to store the necessary information
	 * for patching up the exceptional edges.
	 * 
	 * @author djp
	 * 
	 */
	public class ExceptionHandler {
		/**
		 * The start index of bytecodes covered by the handler.
		 */
		public int start;
		/**
		 * One past the last index covered by the handler.
		 */
		public int end;
		public int label; // label for exception handler
		public Type.Reference exception;

		public ExceptionHandler(int start, int end, int label,
				Type.Reference exception) {
			this.start = start;
			this.end = end;
			this.label = label;
			this.exception = exception;
		}
	}

	protected void writeCodeAttribute(Clazz c, Method method,
			HashMap<Constant.Info, Integer> constantPool) throws IOException {

		// This method is a little tricky. The basic strategy is to first
		// translate each statement into it's bytecode representation. One
		// difficulty here, is that we must defer calculating the targets of
		// branch statements until after this is done, since we can't do the
		// calculation without exact values.

		// === TRANSLATE BYTECODES ===

		int maxLocals = 0;// method.code().localVariables().size();
		
		// FIXME: support for local variables
//		for (LocalVarDef lvd : method.code().localVariables()) {
//			maxLocals += Types.slotSize(lvd.type());
//		}
		
		if (!method.isStatic()) {
			maxLocals++;
		}

		ArrayList<Bytecode> bytecodes = new ArrayList<Bytecode>();
		ArrayList<ExceptionHandler> handlers = new ArrayList<ExceptionHandler>();

		int maxStack = translateMethodCode(c, method, bytecodes, handlers);

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

		write_u2(constantPool.get(new Constant.Utf8("Code")));
		// need to figure out exception_table length
		int exception_table_length = handlers.size() * 8;
		// need to figure out attribute_table length
		int attribute_table_length = 0;
		// write attribute length
		write_u4(bytes.length + exception_table_length + attribute_table_length
				+ 12);
		// now write data
		write_u2(maxStack);
		write_u2(maxLocals);
		write_u4(bytes.length);
		// write bytecode instructions
		for (int i = 0; i != bytes.length; ++i) {
			write_u1(bytes[i]);
		}

		// write exception handlers
		write_u2(handlers.size());
		for (ExceptionHandler h : handlers) {
			write_u2(insnOffsets[h.start]);
			write_u2(insnOffsets[h.end]);
			write_u2(labelOffsets.get("L" + h.label));
			
			// FIXME: support for exception handles
//			if (h.exception.unqualifiedName().equals("java.lang.Throwable")) {
//				write_u2(0);
//			} else {
//				write_u2(constantPool.get(Constant.buildClass(h.exception)));
//			}
		}
		write_u2(0); // no attributes for now
	}

	/**
	 * This method translates a method's control-flow graph into a sequence of
	 * bytecodes. Any missing constant pool items are added to the constant
	 * pool.
	 * 
	 * @param clazz
	 *            Enclosing class
	 * @param method
	 *            Method whose code block is to be translated
	 * @param constantPool
	 *            Required constant Pool items are placed into this
	 * @param bytecodes
	 *            Bytecodes generated for this method will be appended to this
	 * @return maximum size of stack required
	 * @throws ClassNotFoundException,
	 *             MethodNotFoundException, FieldNotFoundException If it needs
	 *             to access a Class which cannot be found.
	 */
	protected int translateMethodCode(Clazz clazz, Method method,
			ArrayList<Bytecode> bytecodes, ArrayList<ExceptionHandler> handlers) {

		// === CREATE TYPE ENVIRONMENT ===

		// create the local variable slot mapping
		HashMap<String, Integer> localVarMap = new HashMap<String, Integer>();		

		int maxLocals = 0;
		int maxStack = 0;

		if (!method.isStatic()) {
			// observe that "super" and "this" are actually aliases from a
			// bytecode generation point of view.
			localVarMap.put("this", maxLocals);
			localVarMap.put("super", maxLocals++);
		}

		// determine slot allocations
		
		// FIXME: support for local variable declarations
//		for (LocalVarDef vd : cfg.localVariables()) {
//			if (vd.type() == null) {
//				System.out.println(vd.name() + " type null");
//				System.out.println(method.name() + " " + method.type());
//			}
//			localVarMap.put(vd.name(), maxLocals);
//			maxLocals += Types.slotSize(vd.type());
//			environment.put(vd.name(), vd.type()); // FIXME:
//		}

		// === TRANSLATE BYTECODES ===
		for(Stmt s : method.body()) {
			translateStatement(s,localVarMap,bytecodes);
		}
		

		// Now make sure the exception handlers are compacted and
		// also arranged in the correct order.
		sortAndCompactExceptionHandlers(handlers);

		// Now, apply the peep hole optimser to get rid of common, but bad
		// bytecode patterns.
		optimiser.optimise(bytecodes, handlers);

		return maxStack;
	}

	/**
	 * This method aims to sort compact handlers that are next to each other
	 * together. For example, if we have the following handlers:
	 * 
	 * <p>
	 * <table border=1>
	 * <tr>
	 * <td>from</td>
	 * <td>to</td>
	 * <td>handler</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td>3</td>
	 * <td>RuntimeException</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td>3</td>
	 * <td>ArrayIndexOutOfBoundsException</td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td>6</td>
	 * <td>RuntimeException</td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td>6</td>
	 * <td>ArrayIndexOutOfBoundsException</td>
	 * </tr>
	 * </table>
	 * </p>
	 * <br>
	 * 
	 * Then we need to compact the four handlers into two and reorder them as
	 * follows:
	 * 
	 * <p>
	 * <table border=1>
	 * <tr>
	 * <td>from</td>
	 * <td>to</td>
	 * <td>handler</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td>6</td>
	 * <td>ArrayIndexOutOfBoundsException</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td>6</td>
	 * <td>RuntimeException</td>
	 * </tr>
	 * </table>
	 * </p>
	 * <br>
	 * 
	 * Observe here that the order of exceptions matters in the classfile. If
	 * the RuntimeException were to come first, then it would prevent the
	 * ArrayIndexOutOfBoundsException from ever being called. Note that, had the
	 * user actually specified this to happen in the source code then we
	 * wouldn't even see the ArrayIndexOutOfBoundsException handler here.
	 * 
	 * @param handlers.
	 */
	protected void sortAndCompactExceptionHandlers(
			ArrayList<ExceptionHandler> handlers) {

		// FIXME: support for sorting exception handlers
		
//		// firstly, sort them into the correct order
//		Collections.sort(handlers, new Comparator<ExceptionHandler>() {
//			public int compare(ExceptionHandler e1, ExceptionHandler e2) {
//				if (e1.exception.supsetEqOf(e2.exception)
//						&& e2.exception.supsetEqOf(e1.exception)) {
//					if (e1.start < e2.start) {
//						return -1;
//					} else if (e1.start > e2.start) {
//						return 1;
//					} else {
//						return 0;
//					}
//				} else if (e1.exception.supsetEqOf(e2.exception)) {
//					return 1;
//				} else {
//					return -1;
//				}
//			}
//		});
//
//		// now, we compact them together.
//		ArrayList<ExceptionHandler> oldhandlers = new ArrayList<ExceptionHandler>(
//				handlers);
//		handlers.clear();
//
//		for (int i = 0; i != oldhandlers.size();) {
//			ExceptionHandler handler = oldhandlers.get(i);
//			int end = handler.end;
//			ExceptionHandler tmp;
//			i = i + 1;
//			while (i < oldhandlers.size()
//					&& (tmp = oldhandlers.get(i)).start == (end + 1)
//					&& tmp.label == handler.label
//					&& tmp.exception.equals(handler.exception)) {
//				end = tmp.end;
//				i = i + 1;
//			}
//			tmp = new ExceptionHandler(handler.start, end, handler.label,
//					handler.exception);
//			handlers.add(tmp);
//		}
	}

	/**
	 * This method is responsible for translating a statement from the
	 * intermediate language into Java Bytecode(s).
	 * 
	 * @param stmt
	 *            the statement to be translated
	 * @param bytecodes
	 *            Java bytecodes representing statement appended onto this
	 * @param varmap
	 *            maps local variable names to their slot numbers.
	 * @return the maximum stack size required by this statement.
	 */
	protected int translateStatement(Stmt stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {
		if (stmt instanceof Stmt.Return) {
			return translateReturn((Stmt.Return) stmt, varmap,
					bytecodes);
		} else if (stmt instanceof Stmt.Assign) {
			return translateAssign((Stmt.Assign) stmt, varmap,
					bytecodes);
		} else if (stmt instanceof Expr.Invoke) {
			return translateInvoke((Expr.Invoke) stmt, varmap,
					bytecodes, false);
		} else if (stmt instanceof Expr.New) {
			return translateNew((Expr.New) stmt, varmap, bytecodes,
					false);
		} else if (stmt instanceof Stmt.Nop) {
			bytecodes.add(new Bytecode.Nop());
			return 0;
		} else if (stmt instanceof Stmt.Throw) {
			return translateThrow((Stmt.Throw) stmt, varmap, bytecodes);
		} else if (stmt instanceof Stmt.Lock) {
			return translateLock((Stmt.Lock) stmt, varmap, bytecodes);
		} else if (stmt instanceof Stmt.Unlock) {
			return translateUnlock((Stmt.Unlock) stmt, varmap,
					bytecodes);
		} else {
			throw new RuntimeException("Unknown statement encountered: " + stmt);
		}
	}

	/**
	 * This method is responsible for translating a conditional expression, such
	 * as if(x < 0) etc.
	 * 
	 * @param condition
	 *            Conditional expression to translate
	 * @param trueLabel
	 *            destination branch when conditional is true
	 * @param varmap
	 *            Maps local variables to their slot number
	 * @param bytecodes
	 *            bytecodes representing this statement are appended onto this
	 * @return maximum size of stack required for this statement
	 */
	protected static int condLabelCount = 0;

	protected int translateConditionalBranch(Expr condition, String trueLabel,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {

		if (condition instanceof Expr.BinOp) {
			Expr.BinOp bop = (Expr.BinOp) condition;

			switch (bop.op()) {
			case Expr.BinOp.LAND: {
				String exitLabel = "CL" + condLabelCount++;
				int ms_lhs = translateConditionalBranch(Exprs.invertBoolean(bop
						.lhs()), exitLabel, varmap, bytecodes);
				int ms_rhs = translateConditionalBranch(Exprs.invertBoolean(bop
						.rhs()), exitLabel, varmap, bytecodes);
				bytecodes.add(new Bytecode.Goto(trueLabel));
				bytecodes.add(new Bytecode.Label(exitLabel));
				return Math.max(ms_lhs, ms_rhs);
			}
			case Expr.BinOp.LOR: {
				int ms_lhs = translateConditionalBranch(bop.lhs(), trueLabel,
						varmap, bytecodes);
				int ms_rhs = translateConditionalBranch(bop.rhs(), trueLabel,
						varmap, bytecodes);
				return Math.max(ms_lhs, ms_rhs);
			}
			}

			// There are a number of optimisations which could be applied
			// here. For example, using ifnull and ifnotnull bytecodes. Also,
			// using ifeq when comparing directly against zero.

			int ms_lhs = translateExpression(bop.lhs(), varmap, 
					bytecodes);

			int ms_rhs = translateExpression(bop.rhs(), varmap, 
					bytecodes);

			Type cmpT = bop.lhs().type();
			int code = -1;
			switch (bop.op()) {
			case Expr.BinOp.EQ:
				code = Bytecode.IfCmp.EQ;
				break;
			case Expr.BinOp.NEQ:
				code = Bytecode.IfCmp.NE;
				break;
			case Expr.BinOp.LT:
				code = Bytecode.IfCmp.LT;
				break;
			case Expr.BinOp.GT:
				code = Bytecode.IfCmp.GT;
				break;
			case Expr.BinOp.LTEQ:
				code = Bytecode.IfCmp.LE;
				break;
			case Expr.BinOp.GTEQ:
				code = Bytecode.IfCmp.GE;
				break;
			}
			if (cmpT instanceof Type.Double || cmpT instanceof Type.Float
					|| cmpT instanceof Type.Long) {
				bytecodes.add(new Bytecode.Cmp(cmpT, Bytecode.Cmp.LT));
				bytecodes.add(new Bytecode.If(code, trueLabel));
			} else {
				bytecodes.add(new Bytecode.IfCmp(code, cmpT, trueLabel));
			}
			return Math.max(ms_lhs, ms_rhs + Types.slotSize(bop.lhs().type()));

		} else if (condition instanceof Expr.UnOp) {
			Expr.UnOp uop = (Expr.UnOp) condition;
			if (uop.op() == Expr.UnOp.NOT) {
				// First attempt to eliminate the NOT altogether!
				Expr e1 = Exprs.eliminateNot(uop);

				if (e1 instanceof Expr.UnOp) {
					Expr.UnOp e2 = (Expr.UnOp) e1;
					if (e2.op() == Expr.UnOp.NOT) {
						// not elimination was unsuccessful
						int ms = translateExpression(uop.expr(), varmap,
								bytecodes);
						bytecodes
								.add(new Bytecode.If(Bytecode.If.EQ, trueLabel));
						return ms;
					}
				}
				// not elimination was successful ...
				return translateConditionalBranch(e1, trueLabel, varmap,
						bytecodes);

			}
			// anything else doesn't make sense
			throw new RuntimeException(
					"Invalid use of unary operator in conditional ("
							+ condition + ")");
		} else if (condition instanceof Expr.Invoke) {
			int ms = translateInvoke((Expr.Invoke) condition, varmap, 
					bytecodes, true);
			bytecodes.add(new Bytecode.If(Bytecode.If.NE, trueLabel));
			return ms;
		} else if (condition instanceof Expr.InstanceOf) {
			int ms = translateExpression(condition, varmap, 
					bytecodes);
			bytecodes.add(new Bytecode.If(Bytecode.If.NE, trueLabel));
			return ms;
		} else if (condition instanceof Expr.Bool 
					|| condition instanceof Expr.ArrayIndex
					|| condition instanceof Expr.Variable
					|| condition instanceof Expr.Deref) {
			int ms = translateExpression(condition, varmap,
					bytecodes);
			bytecodes.add(new Bytecode.If(Bytecode.If.NE, trueLabel));
			return ms;
		} 

		throw new RuntimeException("Unknown conditional expression ("
				+ condition + ")");
	}

	/**
	 * This method is responsible for translating a multi-conditional branching
	 * instruction (i.e. a switch).
	 * 
	 * @param conditions
	 *            The list of conditional expressions to translate
	 * @param labels
	 *            The corresponding destination of each conditional expression
	 * @param varmap
	 *            Maps local variables to their slot number
	 * @param bytecodes
	 *            bytecodes representing this statement are appended onto this
	 * @return maximum size of stack required for this statement
	 */
	protected int translateSwitch(Expr[] conditions, String[] labels,
			HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {

		int maxStack = 0; // max stack required for this statement

		Expr expr = null;
		for (Expr c : conditions) {
			Expr.BinOp cond = (Expr.BinOp) c;
			if (cond.op() == Expr.BinOp.EQ) {
				expr = cond.lhs();
				break;
			}
		}

		int ms = translateExpression(expr, varmap, bytecodes);
		maxStack = Math.max(ms, maxStack);

		String def = null;
		List<Pair<Integer, String>> cases = new ArrayList<Pair<Integer, String>>();
		for (int i = 0; i < conditions.length; i++) {
			Expr e = ((Expr.BinOp) conditions[i]).rhs();
			if (e instanceof Expr.Int) {
				int c = ((Expr.Int) e).value();
				String label = labels[i];
				cases.add(new Pair<Integer, String>(c, label));
			} else {
				def = labels[i];
			}
		}

		Collections.sort(cases, new Comparator<Pair<Integer, String>>() {
			public int compare(Pair<Integer, String> p1,
					Pair<Integer, String> p2) {
				return p1.first() - p2.first();
			}
		});

		bytecodes.add(new Bytecode.Switch(def, cases));

		return maxStack;
	}

	protected int translateInvoke(Expr.Invoke stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes,
			boolean needReturnValue) {

		int stackUsed = 0; // stack slots used by store results
		int maxStack = 0; // max stack required for this statement

		if (!stmt.isStatic()) {
			// must be non-static invocation
			maxStack = translateExpression(stmt.target(), varmap, bytecodes);
			stackUsed++; // it's a reference, so don't worry about slot size
		}
		// translate parameters
		for (Expr p : stmt.parameters()) {
			int ms = translateExpression(p, varmap, bytecodes);
			maxStack = Math.max(ms + stackUsed, maxStack);
			stackUsed += Types.slotSize(p.type()); // this is correct here,
												   // since don't account for
												   // our slot
		}
		
		Type.Clazz targetT = (Type.Clazz) stmt.target().type(); 
		String targetName = targetT.components().get(targetT.components().size()-1).first();
		
		if (stmt.isStatic()) {
			// STATIC
			bytecodes.add(new Bytecode.Invoke(
					targetT, stmt.name(), stmt.funType(), Bytecode.STATIC));
		} else if (stmt.target().type() instanceof Type.Clazz
				&& stmt.name().equals(targetName)) {
			// this is a constructor call
			bytecodes.add(new Bytecode.Invoke(targetT, "<init>",
					stmt.funType(), Bytecode.SPECIAL));
		} else {
			// check whether this is an interface or a class call.
			if (stmt.isInterface()) {
				bytecodes.add(new Bytecode.Invoke(targetT, stmt.name(), stmt
						.funType(), Bytecode.INTERFACE));
			} else {
				bytecodes.add(new Bytecode.Invoke(targetT, stmt
						.name(), stmt.funType(),
						stmt.isPolymorphic() ? Bytecode.VIRTUAL
								: Bytecode.SPECIAL));
			}
		}

		Type retT = stmt.funType().returnType();
				
		if (!(retT instanceof Type.Void)) {
			// Need to account for space occupied by return type!
			maxStack = Math.max(Types.slotSize(retT) + stackUsed, maxStack);
			if (!needReturnValue) {
				// the return value is not required, so we need to pop it from
				// the stack
				bytecodes.add(new Bytecode.Pop(retT));
			} else if ((retT instanceof Type.Variable ||
						isGenericArray(retT))
					&& !(stmt.type() instanceof Type.Variable)
					&& !stmt.type().equals(
							new Type.Clazz("java.lang", "Object"))) {				
				// Here, the actual return type is a (generic) type variable
				// (e.g. T or T[]), and we're expecting it to return a real
                // value (e.g. String, substituted for T). This issue is
				// that, because of erasure, the returned type will be Object
				// and we need to cast it to whatever it needs to be (e.g.
				// String). Note, if the value substituted for T is actually
				// Object, then we just do nothing!
				bytecodes.add(new Bytecode.CheckCast(stmt.type()));								
			}
		}

		return maxStack;
	}

	/**
	 * Translate a Return statement.
	 * 
	 * @param ret
	 * @param varmap
	 * @param bytecodes
	 * @return
	 */
	protected int translateReturn(Stmt.Return ret,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {

		if (ret.expr() != null) {
			int ms = translateExpression(ret.expr(), varmap,
					bytecodes);
			bytecodes.add(new Bytecode.Return(ret.expr().type()));
			return ms;
		} else {
			bytecodes.add(new Bytecode.Return(null));
			return 0;
		}
	}

	protected int translateAssign(Stmt.Assign stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {

		int maxStack = 0;

		if (stmt.lhs() instanceof Expr.Variable) {
			Expr.Variable var = (Expr.Variable) stmt.lhs();
			assert varmap.keySet().contains(var.value());
			int slot = varmap.get(var.value());
			maxStack = translateExpression(stmt.rhs(), varmap, bytecodes);
			bytecodes.add(new Bytecode.Store(slot, stmt.lhs().type()));
		} else if (stmt.lhs() instanceof Expr.Deref) {
			Expr.Deref der = (Expr.Deref) stmt.lhs();
			int ms_lhs = translateExpression(der.target(), varmap,
					bytecodes);
			int ms_rhs = translateExpression(stmt.rhs(), varmap,
					bytecodes);
			// figure out the type of the field involved
			Type.Reference lhs_t = (Type.Reference) der.target().type();

			if (der.isStatic()) {
				bytecodes.add(new Bytecode.PutField(lhs_t, der.name(), der
						.type(), Bytecode.STATIC));
			} else {
				bytecodes.add(new Bytecode.PutField(lhs_t, der.name(), der
						.type(), Bytecode.NONSTATIC));
			}
			maxStack = Math.max(ms_lhs, ms_rhs + 1);

		} else if (stmt.lhs() instanceof Expr.ArrayIndex) {
			Expr.ArrayIndex ai = (Expr.ArrayIndex) stmt.lhs();
			int ms_arr = translateExpression(ai.target(), varmap,
					bytecodes);
			int ms_idx = translateExpression(ai.index(), varmap,
					bytecodes);
			int ms_rhs = translateExpression(stmt.rhs(), varmap,
					bytecodes);
			bytecodes.add(new Bytecode.ArrayStore((Type.Array) ai.target()
					.type()));
			maxStack = Math.max(ms_arr, Math.max(ms_idx + 1, ms_rhs + 2));
		} else {
			throw new RuntimeException("Unknown lval encountered");
		}
		return maxStack;
	}

	protected int translateThrow(Stmt.Throw stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {
		int maxStack = translateExpression(stmt.expr(), varmap, 
				bytecodes);
		bytecodes.add(new Bytecode.Throw());
		return maxStack;
	}

	protected int translateLock(Stmt.Lock stmt, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		int maxStack = translateExpression(stmt.expr(), varmap, 
				bytecodes);
		
		bytecodes.add(new Bytecode.MonitorEnter());
		return maxStack;
	}

	protected int translateUnlock(Stmt.Unlock stmt, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		int maxStack = translateExpression(stmt.expr(), varmap, 
				bytecodes);
		
		bytecodes.add(new Bytecode.MonitorExit());
		return maxStack;
	}

	/**
	 * This method flatterns an expression into bytecodes, such that the result
	 * is left on top of the stack.
	 * 
	 * @param expr
	 * @param varmap
	 *            Map from local variables to local array slot numbers
	 * @param bytecodes
	 *            translated bytecodes are appended to this
	 * @return the maximum stack size required for this expression
	 */
	protected int translateExpression(Expr expr,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {

		int maxStack = 0;

		if (expr instanceof Expr.Bool) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Bool) expr).value()));
			maxStack = 1;
		} else if (expr instanceof Expr.Byte) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Byte) expr).value()));
			maxStack = 1;
		} else if (expr instanceof Expr.Char) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Char) expr).value()));
			maxStack = 1;
		} else if (expr instanceof Expr.Short) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Short) expr).value()));
			maxStack = 1;
		} else if (expr instanceof Expr.Int) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Int) expr).value()));
			maxStack = 1;
		} else if (expr instanceof Expr.Long) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Long) expr).value()));
			maxStack = 2;
		} else if (expr instanceof Expr.Float) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Float) expr).value()));
			maxStack = 1;
		} else if (expr instanceof Expr.Double) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Double) expr).value()));
			maxStack = 2;
		} else if (expr instanceof Expr.Null) {
			bytecodes.add(new Bytecode.LoadConst(null));
			maxStack = 1;
		} else if (expr instanceof Expr.StringVal) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.StringVal) expr).value()));
			maxStack = 1;
		} else if (expr instanceof Expr.Array) {
			return translateArrayVal((Expr.Array) expr, varmap, 
					bytecodes);
		} else if (expr instanceof Expr.Class) {
			return translateClassVal((Expr.Class) expr, varmap,
					bytecodes);			
		} else if (expr instanceof Expr.Variable) {
			Expr.Variable lv = (Expr.Variable) expr;
			if (varmap.containsKey(lv.value())) {
				bytecodes.add(new Bytecode.Load(varmap.get(lv.value()), lv.type()));
				maxStack = Types.slotSize(lv.type());
			} else {
				throw new RuntimeException(
						"internal failure (looking for variable " + lv.value()
								+ ") " + expr);
			}
		} else if (expr instanceof Expr.New) {
			maxStack = translateNew((Expr.New) expr, varmap, bytecodes,
					true);
		} else if (expr instanceof Expr.Deref) {
			maxStack = translateDeref((Expr.Deref) expr, varmap, 
					bytecodes);
		} else if (expr instanceof Expr.ArrayIndex) {
			Expr.ArrayIndex ai = (Expr.ArrayIndex) expr;
			int ms_arr = translateExpression(ai.target(), varmap, 
					bytecodes);
			int ms_idx = translateExpression(ai.index(), varmap, 
					bytecodes);
			Type arr_t = ai.target().type();
			bytecodes.add(new Bytecode.ArrayLoad((Type.Array) arr_t));
			maxStack = Math.max(ms_arr, ms_idx + Types.slotSize(arr_t));
		} else if (expr instanceof Expr.Invoke) {
			maxStack = translateInvoke((Expr.Invoke) expr, varmap, 
					bytecodes, true);
		} else if (expr instanceof Expr.UnOp) {
			maxStack = translateUnaryOp((Expr.UnOp) expr, varmap,
					bytecodes);
		} else if (expr instanceof Expr.BinOp) {
			maxStack = translateBinaryOp((Expr.BinOp) expr, varmap,
					bytecodes);
		} else if (expr instanceof Expr.InstanceOf) {
			Expr.InstanceOf iof = (Expr.InstanceOf) expr;
			int ms = translateExpression(iof.lhs(), varmap, 
					bytecodes);
			bytecodes.add(new Bytecode.InstanceOf(iof.rhs()));
			maxStack = ms;
		} else if (expr instanceof Expr.Cast) {
			maxStack = translateCast((Expr.Cast) expr, varmap,
					bytecodes);
		} else {
			throw new RuntimeException("Unknown expression encountered ("
					+ expr + ")");
		}
		return maxStack;
	}

	public int translateClassVal(Expr.Class cval,  HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		if(cval.type() instanceof Type.Primitive) {
			// FIXME: fix class access to primitive types
//			bytecodes.add(new Bytecode.GetField(Typing
//					.boxedType((Type.Primitive) cval.classType), "TYPE", Type
//					.referenceType("java.lang", "Class"), Bytecode.STATIC));
		} else {
			bytecodes.add(new Bytecode.LoadConst(cval.type()));
		}
		return 1;
	}
	
	public int translateDeref(Expr.Deref def, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {

		int maxStack = 0;
		Type tmp_t = def.target().type();

		if (tmp_t instanceof Type.Reference) {
			Type.Reference lhs_t = (Type.Reference) tmp_t;

			if (def.isStatic()) {
				// This is a static field load					
				bytecodes.add(new Bytecode.GetField(lhs_t, def.name(),
						def.type(), Bytecode.STATIC));
				maxStack = Types.slotSize(def.type());		
			} else {
				// Non-static field load
				maxStack = translateExpression(def.target(), varmap,
						bytecodes);

				bytecodes.add(new Bytecode.GetField(lhs_t, def.name(),
						def.type(), Bytecode.NONSTATIC));
				
				// FIXME: generic type conversions
//				if (actualFieldType instanceof Type.Variable
//						&& !(def.type() instanceof Type.Variable)
//						&& !def.type().equals(new Type.Clazz("java.lang",
//								"Object"))) {
//					// Ok, actual type is a (generic) type variable. Need to
//					// cast to the desired type!
//					bytecodes.add(new Bytecode.CheckCast(def.type()));					
//				}
				
				maxStack = Math.max(maxStack,Types.slotSize(def.type()));	
			}
		} else if (tmp_t instanceof Type.Array && def.name().equals("length")) {
			maxStack = translateExpression(def.target(), varmap,
					bytecodes);
			bytecodes.add(new Bytecode.ArrayLength());
		} else {
			throw new RuntimeException(
					"Attempt to dereference variable with type "
							+ tmp_t.toString());
		}

		return maxStack;
	}

	public int translateArrayVal(Expr.Array av, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		
		List<Expr> params = new ArrayList<Expr>();
		params.add(new Expr.Int(av.values().size()));
		int maxStack = translateNew(new Expr.New(av.type(), params, null),
				varmap, bytecodes, true);

		int index = 0;
		for (Expr e : av.values()) {
			bytecodes.add(new Bytecode.Dup(av.type()));
			bytecodes.add(new Bytecode.LoadConst(index++));
			int ms = translateExpression(e, varmap, bytecodes);
			bytecodes.add(new Bytecode.ArrayStore((Type.Array) av.type()));
			maxStack = Math.max(maxStack, 3 + ms);
		}

		return maxStack;
	}

	public int translateNew(Expr.New news, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes, boolean needReturnValue) {

		int maxStack = 0;

		if (news.type() instanceof Type.Clazz) {
			Type.Clazz type = (Type.Clazz) news.type();

			bytecodes.add(new Bytecode.New(news.type()));
			bytecodes.add(new Bytecode.Dup(news.type()));

			maxStack = 3;
			int usedStack = 3;

			ArrayList<Type> paramTypes = new ArrayList<Type>();
			for (Expr p : news.parameters()) {
				int ms = translateExpression(p, varmap, bytecodes);
				maxStack = Math.max(ms + usedStack, maxStack);
				usedStack += Types.slotSize(p.type());
				paramTypes.add(p.type());
			}

						// call the appropriate constructor
			bytecodes.add(new Bytecode.Invoke(type, "<init>", news.funType(),
					Bytecode.SPECIAL));
		} else if (news.type() instanceof Type.Array) {
			int usedStack = 0;

			for (Expr p : news.parameters()) {
				int ms = translateExpression(p, varmap, bytecodes);
				maxStack = Math.max(ms + usedStack, maxStack);
				usedStack += Types.slotSize(p.type());
			}

			bytecodes.add(new Bytecode.New(news.type(), news.parameters()
					.size()));
		}

		if (!needReturnValue) {
			// the return value is not required, so we need to pop it from
			// the stack
			bytecodes.add(new Bytecode.Pop(news.type()));
		}
		return maxStack;
	}

	protected int translateBinaryOp(Expr.BinOp bop, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {

		// second, translate the binary operator.
		switch (bop.op()) {
		case Expr.BinOp.LT:
		case Expr.BinOp.LTEQ:
		case Expr.BinOp.GT:
		case Expr.BinOp.GTEQ:
		case Expr.BinOp.EQ:
		case Expr.BinOp.NEQ:
		case Expr.BinOp.LAND:
		case Expr.BinOp.LOR: {
			String trueLabel = "CL" + condLabelCount++;
			String exitLabel = "CL" + condLabelCount++;
			int maxStack = translateConditionalBranch(bop, trueLabel, varmap,
					bytecodes);
			bytecodes.add(new Bytecode.LoadConst(0));
			bytecodes.add(new Bytecode.Goto(exitLabel));
			bytecodes.add(new Bytecode.Label(trueLabel));
			bytecodes.add(new Bytecode.LoadConst(1));
			bytecodes.add(new Bytecode.Label(exitLabel));
			return maxStack;
		}
		}

		// must be a standard arithmetic operation.
		int ms_lhs = translateExpression(bop.lhs(), varmap,
				bytecodes);
		int ms_rhs = translateExpression(bop.rhs(), varmap,
				bytecodes);

		bytecodes.add(new Bytecode.BinOp(bop.op(), bop.type()));
		return Math.max(ms_lhs, ms_rhs + Types.slotSize(bop.type()));
	}

	protected int translateUnaryOp(Expr.UnOp uop, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		// second, translate the operation.
		// FIXME: resolve operator type

		switch (uop.op()) {
		case Expr.UnOp.NOT:
			String trueLabel = "CL" + condLabelCount++;
			String exitLabel = "CL" + condLabelCount++;
			int ms = translateConditionalBranch(uop, trueLabel, varmap,
					bytecodes);
			bytecodes.add(new Bytecode.LoadConst(0));
			bytecodes.add(new Bytecode.Goto(exitLabel));
			bytecodes.add(new Bytecode.Label(trueLabel));
			bytecodes.add(new Bytecode.LoadConst(1));
			bytecodes.add(new Bytecode.Label(exitLabel));
			return ms;
		}

		// first, translate the expression.
		int maxStack = translateExpression(uop.expr(), varmap,
				bytecodes);

		switch (uop.op()) {
		case Expr.UnOp.INV:
			bytecodes.add(new Bytecode.LoadConst(new Integer(-1)));
			bytecodes
					.add(new Bytecode.BinOp(Bytecode.BinOp.XOR, new Type.Int()));
			maxStack = Math.max(maxStack, 2);
			break;
		case Expr.UnOp.NEG:
			bytecodes.add(new Bytecode.Neg(uop.type()));
			break;
		default:
			throw new RuntimeException("Unknown unary expression encountered ("
					+ uop + ")");
		}

		return maxStack;
	}

	protected int translateCast(Expr.Cast cast, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		
		int maxStack = translateExpression(cast.expr(), varmap, bytecodes);

		Type srcType = cast.expr().type();
		// Now, do implicit conversions
		if (cast.type() instanceof Type.Primitive
				&& srcType instanceof Type.Primitive) {
			bytecodes.add(new Bytecode.Conversion((Type.Primitive) srcType,
					(Type.Primitive) cast.type()));
		} else {
			bytecodes.add(new Bytecode.CheckCast(cast.type()));
		} 
		
		return Math.max(maxStack, Types.slotSize(cast.type()));
	}

	// ============================================================
	// OTHER HELPER METHODS
	// ============================================================

	protected void writeModifiers(List<Modifier> modifiers) throws IOException {
		int mods = 0;

		for (Modifier x : modifiers) {
			if (x instanceof Modifier.Base) {
				mods |= ((Modifier.Base) x).modifier();
			}
		}

		write_u2(mods);
	}
	
	protected HashMap<Constant.Info, Integer> buildConstantPool(Clazz clazz) {
		HashMap<Constant.Info, Integer> constantPool = new HashMap<Constant.Info, Integer>();
		constantPool.put(null, 0);
		Constant.addPoolItem(Constant.buildClass(clazz.type()), constantPool);
		Constant.addPoolItem(new Constant.Utf8("Signature"), constantPool);
		Constant.addPoolItem(new Constant.Utf8("ConstantValue"), constantPool);
		
		if (clazz.superClass() != null) {
			Constant.addPoolItem(Constant.buildClass(clazz.superClass()),
					constantPool);
		}

		for (Type.Reference i : clazz.interfaces()) {
			Constant.addPoolItem(Constant.buildClass(i), constantPool);
		}

		if (needClassSignature(clazz)) {
			Constant.addPoolItem(
					new Constant.Utf8(Types.classSignature(clazz)),
					constantPool);
		}
		
		// FIXME: support for inner classes
//		if (clazz.inners().size() > 0 || clazz.isInnerClass()) {
//			Constant.addPoolItem(new Constant.Utf8("InnerClasses"),constantPool);
//			for(Triple<Type.Reference,Integer,Boolean> i : clazz.inners()) {
//				Constant.addPoolItem(Constant.buildClass(i.first()), constantPool);
//				Constant.addPoolItem(new Constant.Utf8(i.first().name()), constantPool);
//			}
//			if(clazz.isInnerClass()) {
//				Type.Reference inner = clazz.type();
//				Pair<String,Type[]>[] classes = clazz.type().classes();
//				for(int i=classes.length-1;i>0;--i) {
//					// First, we need to construct the outer reference type.
//					Pair<String,Type[]>[] nclasses = new Pair[i];
//					System.arraycopy(classes,0,nclasses,0,nclasses.length);				
//					Type.Reference outer = Type.referenceType(inner.pkg(),nclasses);
//					// Now, we can actually write the information.									
//					Constant.addPoolItem(Constant.buildClass(outer), constantPool);
//					Constant.addPoolItem(Constant.buildClass(inner), constantPool);
//					Constant.addPoolItem(new Constant.Utf8(inner.name()), constantPool);									
//					inner = outer;				
//				}
//			}	
//		}				
		
		for (Field f : clazz.fields()) {
			Constant.addPoolItem(new Constant.Utf8(f.name()), constantPool);
			Constant.addPoolItem(new Constant.Utf8(Types.descriptor(f.type(),false)),
					constantPool);
			if(isGeneric(f.type())) {
				Constant.addPoolItem(new Constant.Utf8(Types.descriptor(f
						.type(), true)), constantPool);
			}
			// FIXME: support for constant values
//			if(f.constantValue() != null) {
//				if(f.constantValue() instanceof java.lang.Number) {
//					Constant.addPoolItem(Constant.fromNumber((java.lang.Number) f.constantValue()), constantPool);	
//				} else {
//					Constant.addPoolItem(Constant.fromString((String) f.constantValue()), constantPool);
//				}
//			}
		}

		for (Method m : clazz.methods()) {
			if (m.name().equals(clazz.name())) {
				Constant.addPoolItem(new Constant.Utf8("<init>"), constantPool);
			} else {
				Constant.addPoolItem(new Constant.Utf8(m.name()), constantPool);
			}
			
			Constant.addPoolItem(new Constant.Utf8(Types.descriptor(m.type(),
					false)), constantPool);
			if (m.body() != null) {
				Constant.addPoolItem(new Constant.Utf8("Code"), constantPool);

				// Now, use the Bytecode.toBytes method to ensure that required
				// contant pool items are added to the constant pool.
				//
				// It's a bit naughty of me to just translate the whole CFG and
				// then throw it away. This could be optimised so that we save
				// the resulting bytecodes here, but for now it doesn't matter.
				ArrayList<Bytecode> bytecodes = new ArrayList<Bytecode>();
				ArrayList<ExceptionHandler> handlers = new ArrayList<ExceptionHandler>();
				translateMethodCode(clazz, m, bytecodes, handlers);
								
				for (Bytecode b : bytecodes) {
					if (!(b instanceof Bytecode.Branch) && !(b instanceof Bytecode.Switch)) {
						b.toBytes(0, null, constantPool);
					}
				}
				
				// FIXME: support for exception handlers
//				for(ExceptionHandler h : handlers) {
//					if(!h.exception.unqualifiedName().equals("java.lang.Throwable")) {
//						Constant.addPoolItem(Constant.buildClass(h.exception), constantPool);
//					}
//				}
			}
			if(!m.exceptions().isEmpty()) {
				Constant.addPoolItem(new Constant.Utf8("Exceptions"), constantPool);
				for(Type.Reference e : m.exceptions()) {
					Constant.addPoolItem(Constant.buildClass(e), constantPool);	
				}
			}
			if(isGeneric(m.type().returnType())) {
				Constant.addPoolItem(new Constant.Utf8(Types.descriptor(m
						.type(), true)), constantPool);
			}
		}

		return constantPool;
	}
	
	protected boolean needClassSignature(Clazz clazz) {
		if (isGeneric(clazz.type())
				|| (clazz.superClass() != null && isGeneric(clazz.superClass()))) {
			return true;
		}
		for (Type.Reference t : clazz.interfaces()) {
			if (isGeneric(t)) {
				return true;
			}
		}
		return false;
	}
		
	protected boolean isGeneric(Type t) {
		if(!(t instanceof Type.Clazz)) {
			return false;
		}
		Type.Clazz ref = (Type.Clazz) t;
		for(Pair<String, List<Type.Reference>> p : ref.components()) {
			if(p.second().size() > 0) {
				return true;
			}
		}
		return false;
	}
	
	protected boolean isGenericArray(Type t) {
		if(t instanceof Type.Array) {
			Type et = ((Type.Array)t).element();
			if(et instanceof Type.Variable) {
				return true;
			} else {
				return isGenericArray(et);
			}
		} 
		
		return false;	
	}

	protected void write_u1(int w) throws IOException {
		write_u1(output, w);
	}

	protected void write_u2(int w) throws IOException {
		write_u2(output, w);
	}

	protected void write_u4(int w) throws IOException {
		output.write((w >> 24) & 0xFF);
		output.write((w >> 16) & 0xFF);
		output.write((w >> 8) & 0xFF);
		output.write(w & 0xFF);
	}

	protected static void write_u1(OutputStream output, int w) throws IOException {
		output.write(w & 0xFF);
	}

	protected static void write_u2(OutputStream output, int w) throws IOException {
		output.write((w >> 8) & 0xFF);
		output.write(w & 0xFF);
	}
}