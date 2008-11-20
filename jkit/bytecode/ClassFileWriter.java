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

import jkit.compiler.ClassTable;
import jkit.compiler.ClassWriter;
import jkit.compiler.FieldNotFoundException;
import jkit.compiler.InternalException;
import jkit.compiler.MethodNotFoundException;
import jkit.jkil.Clazz;
import jkit.jkil.Field;
import jkit.jkil.FlowGraph;
import jkit.jkil.Method;
import jkit.jkil.Type;
import jkit.jkil.FlowGraph.*;
import jkit.util.*;
import jkit.bytecode.*;
import jkit.util.graph.*;
import jkit.stages.codegen.Typing;

public class ClassFileWriter implements ClassWriter {
	protected final OutputStream output;
	protected final BytecodeOptimiser optimiser;
	
	protected final int version;

	/**
	 * Construct a ClassFile writer Object that uses the SimpleOptimiser for
	 * peephole optimisation. Uses the default classfile version of 49.
	 * 
	 * @param o
	 *            Output stream for class bytes
	 */
	public ClassFileWriter(OutputStream o) {
		output = o;
		version = 49;
		optimiser = new SimpleOptimiser();
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
	public ClassFileWriter(OutputStream o, List<Pair<String,String>> options) 
	throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		output = o;
		int myVersion = 49;
		BytecodeOptimiser myOptimiser = new SimpleOptimiser();
		
		for (Pair<String, String> option : options) {
			if (option.first().equals("version")) {
				myVersion = Integer.parseInt(option.second());
			} else if (option.first().equals("optimiser")) {
				myOptimiser = (BytecodeOptimiser) Class
						.forName(option.second()).newInstance();
			}
		}
		
		version = myVersion;
		optimiser = myOptimiser;
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
		write_u2(clazz.modifiers());
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
		if(clazz.inners().size() > 0 || clazz.isInnerClass()) { nattributes++; }
		
		write_u2(nattributes);
		
		if (needClassSignature(clazz)) {			
			writeClassSignature(clazz, constantPool);
		}
		
		if (clazz.inners().size() > 0 || clazz.isInnerClass()) {			
			writeInnerClassAttribute(clazz, constantPool);
		}
		
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
		
		int ninners = clazz.inners().size() + clazz.type().classes().length - 1;
		
		write_u4(2 + (8 * ninners));
		write_u2(ninners);
		
		if(clazz.isInnerClass()) {
			Type.Reference inner = clazz.type();
			Pair<String,Type[]>[] classes = clazz.type().classes();
			for(int i=classes.length-1;i>0;--i) {		
				// First, we need to construct the outer reference type.
				Pair<String,Type[]>[] nclasses = new Pair[i];
				System.arraycopy(classes,0,nclasses,0,nclasses.length);				
				Type.Reference outer = Type.referenceType(inner.pkg(),nclasses);
				// Now, we can actually write the information.
				write_u2(pmap.get(Constant.buildClass(inner)));
				write_u2(pmap.get(Constant.buildClass(outer)));
				write_u2(pmap.get(new Constant.Utf8(inner.name())));
				try {
					// This dependence on ClassTable here is annoying really.
					Clazz innerC = ClassTable.findClass(inner);
					write_u2(innerC.modifiers());
				} catch(ClassNotFoundException e) {
					write_u2(0); // this is a problem!!!!
				}
				inner = outer;				
			}
		}		
		
		for(Triple<Type.Reference,Integer,Boolean> i : clazz.inners()) {
			write_u2(pmap.get(Constant.buildClass(i.first())));
			write_u2(pmap.get(Constant.buildClass(clazz.type())));
			write_u2(pmap.get(new Constant.Utf8(i.first().name())));
			write_u2(i.second());			
		}
	}
	
	protected void writeField(Field f, HashMap<Constant.Info, Integer> pmap)
			throws IOException {
		write_u2(f.modifiers());
		write_u2(pmap.get(new Constant.Utf8(f.name())));
		write_u2(pmap.get(new Constant.Utf8(Types.descriptor(f.type(), false))));
		
		int attrNum = ((isGeneric(f.type())) ? 1 : 0) + ((f.constantValue() != null) ? 1 : 0);
		// Write number of attributes
		write_u2(attrNum);
		// We only need to write a Signature attribute if the field has generic params
		if (isGeneric(f.type())) {
			write_u2(pmap.get(new Constant.Utf8("Signature")));
			write_u4(2);
			write_u2(pmap.get(new Constant.Utf8(Types
					.descriptor(f.type(), true))));
		}
		if(f.constantValue() != null) {
			write_u2(pmap.get(new Constant.Utf8("ConstantValue")));
			write_u4(2);
			if(f.constantValue() instanceof java.lang.Number) {
				write_u2(pmap.get(Constant.fromNumber((java.lang.Number) f.constantValue())));
			} else {
				write_u2(pmap.get(Constant.fromString((String) f.constantValue())));
			}
		}
	}

	protected void writeMethod(Clazz c, Method m,
			HashMap<Constant.Info, Integer> pmap) throws IOException {

		write_u2(m.modifiers());

		if (m.name().equals(c.name())) {
			// this is a constructor
			write_u2(pmap.get(new Constant.Utf8("<init>")));
		} else {
			write_u2(pmap.get(new Constant.Utf8(m.name())));
		}

		write_u2(pmap.get(new Constant.Utf8(Types.descriptor(m.type(), false))));

		int nattrs = 0;
		if (m.code() != null) {
			nattrs++;
		}
		if (!m.exceptions().isEmpty()) {
			nattrs++;
		}
		if(isGeneric(m.type().returnType())) {
			nattrs++;
		}

		write_u2(nattrs);
		if (m.code() != null) {
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

		List<Type.Reference> exceptions = method.exceptions();
		write_u2(constantPool.get(new Constant.Utf8("Exceptions")));
		write_u4(2 + (2 * exceptions.size()));
		write_u2(exceptions.size());
		for (Type.Reference e : exceptions) {
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
		// traverse the control-flow graph to obtain a topological ordering of
		// the statements. Then, we translate each statement into it's bytecode
		// representation. One difficulty here, is that we must defer
		// calculating thetargets of branch statements until after this is done,
		// since we can't do the calculation without exact values.

		// === TRANSLATE BYTECODES ===

		int maxLocals = 0;// method.code().localVariables().size();
		for (LocalVarDef lvd : method.code().localVariables()) {
			maxLocals += Types.slotSize(lvd.type());
		}
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
			if (h.exception.unqualifiedName().equals("java.lang.Throwable")) {
				write_u2(0);
			} else {
				write_u2(constantPool.get(Constant.buildClass(h.exception)));
			}
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

		FlowGraph cfg = method.code();

		// === CREATE TYPE ENVIRONMENT ===

		// create the local variable slot mapping
		HashMap<String, Integer> localVarMap = new HashMap<String, Integer>();
		HashMap<String, Type> environment = FlowGraph.buildEnvironment(method,
				clazz);

		int maxLocals = 0;
		int maxStack = 0;

		if (!method.isStatic()) {
			// observe that "super" and "this" are actually aliases from a
			// bytecode generation point of view.
			localVarMap.put("this", maxLocals);
			localVarMap.put("super", maxLocals++);
		}

		// determine slot allocations
		for (LocalVarDef vd : cfg.localVariables()) {
			if (vd.type() == null) {
				System.out.println(vd.name() + " type null");
				System.out.println(method.name() + " " + method.type());
			}
			localVarMap.put(vd.name(), maxLocals);
			maxLocals += Types.slotSize(vd.type());
			environment.put(vd.name(), vd.type()); // FIXME:
		}

		// === ORDER CFG ===

		ArrayList<Point> ord = cfgOrder(cfg.entry(), cfg);

		// === TRANSLATE BYTECODES ===

		// Translate intermediate representation into Java Bytecodes.
		HashMap<Point, Integer> labels = new HashMap<Point, Integer>();

		for (int i = 0; i != ord.size(); ++i) {
			Point pp = ord.get(i);

			try {
				// If this point has more than one incoming edge, or this point
				// doesn't fall through automatically from the previous point in
				// the ordering, then we need to create a label so that some
				// branching instruction can jump here.
				if (cfg.to(pp).size() > 1
						|| !fallsThroughFromPredecessor(i, ord, cfg)) {
					int lab = newLabel(pp, labels);
					bytecodes.add(new Bytecode.Label("L" + lab));
				}

				// Now, translate the statement at this program point (if there
				// is one). Keep the start offset of this instruction in case
				// there is an exceptional edge from it.
				int startOffset = bytecodes.size();

				if (pp.statement() != null) {
					int ms = translateStatement(pp.statement(), localVarMap,
							environment, bytecodes);
					maxStack = Math.max(maxStack, ms);
				}

				// Now, split out the conditional and exceptional edges since we
				// need to treat them differently.
				ArrayList<Triple<Point, Point, Expr>> conditionals;
				ArrayList<Triple<Point, Point, Expr>> exceptionals;

				conditionals = new ArrayList<Triple<Point, Point, Expr>>();
				exceptionals = new ArrayList<Triple<Point, Point, Expr>>();

				for (Triple<Point, Point, Expr> edge : cfg.from(pp)) {
					if (!(edge.third() instanceof FlowGraph.Exception)) {
						conditionals.add(edge);
					} else {
						exceptionals.add(edge);
					}
				}

				// Finally, deal with real conditional edges
				if (conditionals.size() > 2) {
					Expr[] conditions = new Expr[conditionals.size()];
					Point[] targets = new Point[conditionals.size()];
					String[] lbls = new String[conditionals.size()];

					int id = 0;
					for (Triple<Point, Point, Expr> f : conditionals) {
						conditions[id] = f.third();
						targets[id] = f.second();
						lbls[id] = "L" + newLabel(targets[id], labels);
						id++;
					}

					int ms = translateSwitch(conditions, lbls, localVarMap,
							environment, bytecodes);
					maxStack = Math.max(maxStack, ms);
				} else if (conditionals.size() == 2) {
					Triple<Point, Point, Expr> branch = conditionals.get(0);
					Triple<Point, Point, Expr> fallThrough = conditionals
							.get(1);

					// Try to make sure that branch is the conditional branch,
					// whilst fallThrough represents the fall through case.
					if (branch.second() == ord.get(i + 1)) {
						Triple<Point, Point, Expr> tmp = branch;
						branch = fallThrough;
						fallThrough = tmp;
					}

					int ms = translateConditionalBranch(branch.third(), "L"
							+ newLabel(branch.second(), labels), localVarMap,
							environment, bytecodes);

					if (fallThrough.second() != ord.get(i + 1)) {
						// in this case, the fall through from the conditional
						// branch does not go to the following point in the
						// ordering. Therefore, we need an additional
						// unconditional branch to its destination
						bytecodes.add(new Bytecode.Goto("L"
								+ newLabel(fallThrough.second(), labels)));
					}
					maxStack = Math.max(maxStack, ms);
				} else if (conditionals.size() == 1) {
					// In this case, there is only one edge flowing out of this
					// point. Now, if that edge flows to the next point in the
					// ordering then do nothing; otherwise, put in an
					// unconditional redirect to the actual destination.
					Triple<Point, Point, Expr> fallThrough = conditionals
							.get(0);
					if (ord.size() == (i + 1)
							|| fallThrough.second() != ord.get(i + 1)) {
						assert fallThrough.third() == null;
						bytecodes.add(new Bytecode.Goto("L"
								+ newLabel(fallThrough.second(), labels)));
					}
				}

				// Check whether any exceptions eminating from this node. If it
				// does, and they are to exception handlers within the method,
				// then add them to the handlers list. Note, nodes without
				// statements can still give rise to exceptions if they are e.g.
				// the head of an if-statement (since the exception can be
				// caused by the condition itself),
				if (!exceptionals.isEmpty()) {
					for (Triple<Point, Point, Expr> edge : exceptionals) {
						int label = newLabel(edge.second(), labels);
						Type.Reference exception = ((FlowGraph.Exception) edge
								.third()).type;
						handlers.add(new ExceptionHandler(startOffset,
								bytecodes.size(), label, exception));
					}
				}
			} catch (ClassNotFoundException e) {
				throw new InternalException(e, pp, method, clazz);
			} catch (FieldNotFoundException e) {
				throw new InternalException(e, pp, method, clazz);
			} catch (MethodNotFoundException e) {
				throw new InternalException(e, pp, method, clazz);
			} catch (RuntimeException e) {
				throw new InternalException(e, pp, method, clazz);
			}
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
	 * This method determines whether or not a given CFG point flows
	 * automatically from its predecessor in the topological ordering. This is
	 * used to determine whether or not a label is required before this
	 * instruction. For example, suppose we have the following CFG:
	 * 
	 * <pre>
	 * A -&gt; B, A -&gt; C, B -&gt; D, C -&gt; D
	 * </pre>
	 * 
	 * Now, consider the ordering A,B,C,D. In this case, B flows from its
	 * predecessor (A), whilst C does not. Therefore, we know that a label is
	 * required before the instruction at C, since otherwise it could not be
	 * reached.
	 * 
	 * @param index
	 *            position in order of node in question.
	 * @param order
	 *            topological ordering of cfg
	 * @param cfg
	 *            Flow-Graph
	 * @return true if point at index does flow from its predecessor.
	 */
	protected boolean fallsThroughFromPredecessor(int index, List<Point> order,
			FlowGraph cfg) {

		if (index > 0 && cfg.from(order.get(index - 1)).size() > 2) {
			// In this case the previous node is actually a switch statement.
			// Switch statements differ from other branching instructions, since
			// they have no follow through case. Therefore, we always need a
			// label for every case statement of a switch.
			return false;
		} else if (index > 0) {
			// The following loop is needed, but really wouldn't be
			// if this were a half-decent programming language.
			Point last = order.get(index - 1);
			Point pos = order.get(index);

			for (Triple<Point, Point, Expr> t : cfg.to(pos)) {
				if (t.first() == last
						&& !(t.third() instanceof FlowGraph.Exception)) {
					return true;
				}
			}
		}

		return false;
	}

	/**	 
	 * Create a new entry the labels map that maps the target program points to
	 * a (unique) label identifier.
	 * 
	 * @param target
	 *            Program Point which label is to be associated with
	 * @param labels
	 *            Maps (unique) label identifiers to program points.
	 * @return label identifier of target program point
	 */
	protected int newLabel(Point target, Map<Point, Integer> labels) {
		Integer label;
		if ((label = labels.get(target)) == null) {
			label = new Integer(labels.size());
			labels.put(target, label);
		}
		return label;
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
		// firstly, sort them into the correct order
		Collections.sort(handlers, new Comparator<ExceptionHandler>() {
			public int compare(ExceptionHandler e1, ExceptionHandler e2) {
				if (e1.exception.supsetEqOf(e2.exception)
						&& e2.exception.supsetEqOf(e1.exception)) {
					if (e1.start < e2.start) {
						return -1;
					} else if (e1.start > e2.start) {
						return 1;
					} else {
						return 0;
					}
				} else if (e1.exception.supsetEqOf(e2.exception)) {
					return 1;
				} else {
					return -1;
				}
			}
		});

		// now, we compact them together.
		ArrayList<ExceptionHandler> oldhandlers = new ArrayList<ExceptionHandler>(
				handlers);
		handlers.clear();

		for (int i = 0; i != oldhandlers.size();) {
			ExceptionHandler handler = oldhandlers.get(i);
			int end = handler.end;
			ExceptionHandler tmp;
			i = i + 1;
			while (i < oldhandlers.size()
					&& (tmp = oldhandlers.get(i)).start == (end + 1)
					&& tmp.label == handler.label
					&& tmp.exception.equals(handler.exception)) {
				end = tmp.end;
				i = i + 1;
			}
			tmp = new ExceptionHandler(handler.start, end, handler.label,
					handler.exception);
			handlers.add(tmp);
		}
	}

	/**
	 * This method is responsible for translating a statement from the
	 * intermediate language into Java Bytecode(s).
	 * 
	 * @param stmt
	 *            the statement to be translated
	 * @param bytecodes
	 *            Java bytecodes representing statement appended onto this
	 * @return the maximum stack size required by this statement.
	 * @throws ClassNotFoundException,
	 *             MethodNotFoundException, FieldNotFoundException If it needs
	 *             to access a Class which cannot be found.
	 */
	protected int translateStatement(Stmt stmt,
			HashMap<String, Integer> varmap, HashMap<String, Type> environment,
			ArrayList<Bytecode> bytecodes) throws ClassNotFoundException,
			MethodNotFoundException, FieldNotFoundException {
		if (stmt instanceof Return) {
			return translateReturn((Return) stmt, varmap, environment,
					bytecodes);
		} else if (stmt instanceof Assign) {
			return translateAssign((Assign) stmt, varmap, environment,
					bytecodes);
		} else if (stmt instanceof Invoke) {
			return translateInvoke((Invoke) stmt, varmap, environment,
					bytecodes, false);
		} else if (stmt instanceof New) {
			return translateNew((New) stmt, varmap, environment, bytecodes,
					false);
		} else if (stmt instanceof Nop) {
			bytecodes.add(new Bytecode.Nop());
			return 0;
		} else if (stmt instanceof Throw) {
			return translateThrow((Throw) stmt, varmap, environment, bytecodes);
		} else if (stmt instanceof TernOp) {
			return translateTernOp((TernOp) stmt, varmap, environment,
					bytecodes);
		} else if (stmt instanceof Lock) {
			return translateLock((Lock) stmt, varmap, environment, bytecodes);
		} else if (stmt instanceof Unlock) {
			return translateUnlock((Unlock) stmt, varmap, environment,
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
			HashMap<String, Integer> varmap, HashMap<String, Type> environment,
			ArrayList<Bytecode> bytecodes) throws ClassNotFoundException,
			MethodNotFoundException, FieldNotFoundException {

		if (condition instanceof BinOp) {
			BinOp bop = (BinOp) condition;

			switch (bop.op) {
			case BinOp.LAND: {
				String exitLabel = "CL" + condLabelCount++;
				int ms_lhs = translateConditionalBranch(FlowGraph
						.invertBoolean(bop.lhs), exitLabel, varmap,
						environment, bytecodes);
				int ms_rhs = translateConditionalBranch(FlowGraph
						.invertBoolean(bop.rhs), exitLabel, varmap,
						environment, bytecodes);
				bytecodes.add(new Bytecode.Goto(trueLabel));
				bytecodes.add(new Bytecode.Label(exitLabel));
				return Math.max(ms_lhs, ms_rhs);
			}
			case BinOp.LOR: {
				int ms_lhs = translateConditionalBranch(bop.lhs, trueLabel,
						varmap, environment, bytecodes);
				int ms_rhs = translateConditionalBranch(bop.rhs, trueLabel,
						varmap, environment, bytecodes);
				return Math.max(ms_lhs, ms_rhs);
			}
			}

			// There are a number of optimisations which could be applied
			// here. For example, using ifnull and ifnotnull bytecodes. Also,
			// using ifeq when comparing directly against zero.

			int ms_lhs = translateExpression(bop.lhs, varmap, environment,
					bytecodes);

			int ms_rhs = translateExpression(bop.rhs, varmap, environment,
					bytecodes);

			Type cmpT = bop.lhs.type;
			int code = -1;
			switch (bop.op) {
			case BinOp.EQ:
				code = Bytecode.IfCmp.EQ;
				break;
			case BinOp.NEQ:
				code = Bytecode.IfCmp.NE;
				break;
			case BinOp.LT:
				code = Bytecode.IfCmp.LT;
				break;
			case BinOp.GT:
				code = Bytecode.IfCmp.GT;
				break;
			case BinOp.LTEQ:
				code = Bytecode.IfCmp.LE;
				break;
			case BinOp.GTEQ:
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
			return Math.max(ms_lhs, ms_rhs + Types.slotSize(bop.lhs.type));

		} else if (condition instanceof UnOp) {
			UnOp uop = (UnOp) condition;
			if (uop.op == UnOp.NOT) {
				// First attempt to eliminate the NOT altogether!
				Expr e1 = FlowGraph.eliminateNot(uop);

				if (e1 instanceof UnOp) {
					UnOp e2 = (UnOp) e1;
					if (e2.op == UnOp.NOT) {
						// not elimination was unsuccessful
						int ms = translateExpression(uop.expr, varmap,
								environment, bytecodes);
						bytecodes
								.add(new Bytecode.If(Bytecode.If.EQ, trueLabel));
						return ms;
					}
				}
				// not elimination was successful ...
				return translateConditionalBranch(e1, trueLabel, varmap,
						environment, bytecodes);

			}
			// anything else doesn't make sense
			throw new RuntimeException(
					"Invalid use of unary operator in conditional ("
							+ condition + ")");
		} else if (condition instanceof Invoke) {
			int ms = translateInvoke((Invoke) condition, varmap, environment,
					bytecodes, true);
			bytecodes.add(new Bytecode.If(Bytecode.If.NE, trueLabel));
			return ms;
		} else if (condition instanceof InstanceOf) {
			int ms = translateExpression(condition, varmap, environment,
					bytecodes);
			bytecodes.add(new Bytecode.If(Bytecode.If.NE, trueLabel));
			return ms;
		} else if (condition instanceof BoolVal 
					|| condition instanceof ArrayIndex
					|| condition instanceof LocalVar
					|| condition instanceof Deref) {
			int ms = translateExpression(condition, varmap, environment,
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
	 * @param environment
	 *            Maps local variables to their current type
	 * @param bytecodes
	 *            bytecodes representing this statement are appended onto this
	 * @return maximum size of stack required for this statement
	 */
	protected int translateSwitch(FlowGraph.Expr[] conditions, String[] labels,
			HashMap<String, Integer> varmap, HashMap<String, Type> environment,
			ArrayList<Bytecode> bytecodes) throws ClassNotFoundException,
			MethodNotFoundException, FieldNotFoundException,
			MethodNotFoundException, FieldNotFoundException {

		int maxStack = 0; // max stack required for this statement

		FlowGraph.Expr expr = null;
		for (FlowGraph.Expr c : conditions) {
			FlowGraph.BinOp cond = (FlowGraph.BinOp) c;
			if (cond.op == FlowGraph.BinOp.EQ) {
				expr = cond.lhs;
				break;
			}
		}

		int ms = translateExpression(expr, varmap, environment, bytecodes);
		maxStack = Math.max(ms, maxStack);

		String def = null;
		List<Pair<Integer, String>> cases = new ArrayList<Pair<Integer, String>>();
		for (int i = 0; i < conditions.length; i++) {
			FlowGraph.Expr e = ((FlowGraph.BinOp) conditions[i]).rhs;
			if (e instanceof FlowGraph.IntVal) {
				int c = ((FlowGraph.IntVal) e).value;
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

	protected int translateInvoke(Invoke stmt, HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes,
			boolean needReturnValue) throws ClassNotFoundException,
			MethodNotFoundException, FieldNotFoundException {

		int stackUsed = 0; // stack slots used by store results
		int maxStack = 0; // max stack required for this statement

		// Figure out what method is being called. This is
		// necessary to determine whether or not it's a static invocation.
		Triple<Clazz, Method, Type.Function> minfo = FlowGraph
				.resolveMethod(stmt);

		Method method = minfo.second();
		Type.Function actualMethodT = minfo.second().type();

		if (!method.isStatic()) {
			// must be non-static invocation
			maxStack = translateExpression(stmt.target, varmap, environment,
					bytecodes);
			stackUsed++; // it's a reference, so don't worry about slot size
		}
		// translate parameters
		for (Expr p : stmt.parameters) {
			int ms = translateExpression(p, varmap, environment, bytecodes);
			maxStack = Math.max(ms + stackUsed, maxStack);
			stackUsed += Types.slotSize(p.type); // this is correct here,
													// since don't account for
													// our slot
		}
		if (method.isStatic()) {
			// STATIC
			bytecodes.add(new Bytecode.Invoke(minfo.first().type(), stmt.name,
					actualMethodT, Bytecode.STATIC));
		} else if (stmt.target.type instanceof Type.Reference
				&& stmt.name.equals(((Type.Reference) stmt.target.type).name())) {
			// this is a constructor call
			bytecodes.add(new Bytecode.Invoke(minfo.first().type(), "<init>",
					actualMethodT, Bytecode.SPECIAL));
		} else {
			// check whether this is an interface or a class call.
			if (minfo.first().isInterface()) {
				bytecodes.add(new Bytecode.Invoke(minfo.first().type(),
						stmt.name, actualMethodT, Bytecode.INTERFACE));
			} else {
				bytecodes
						.add(new Bytecode.Invoke(minfo.first().type(),
								stmt.name, actualMethodT,
								stmt.polymorphic ? Bytecode.VIRTUAL
										: Bytecode.SPECIAL));
			}
		}

		Type retT = actualMethodT.returnType();
				
		if (!(retT instanceof Type.Void)) {
			// Need to account for space occupied by return type!
			maxStack = Math.max(Types.slotSize(retT) + stackUsed, maxStack);
			if (!needReturnValue) {
				// the return value is not required, so we need to pop it from
				// the stack
				bytecodes.add(new Bytecode.Pop(retT));
			} else if ((retT instanceof Type.Variable ||
						isGenericArray(retT))
					&& !(stmt.type instanceof Type.Variable)
					&& !stmt.type.equals(
							Type.referenceType("java.lang", "Object"))) {				
				// Here, the actual return type is a (generic) type variable
				// (e.g. T or T[]), and we're expecting it to return a real
                // value (e.g. String, substituted for T). This issue is
				// that, because of erasure, the returned type will be Object
				// and we need to cast it to whatever it needs to be (e.g.
				// String). Note, if the value substituted for T is actually
				// Object, then we just do nothing!
				bytecodes.add(new Bytecode.CheckCast(stmt.type));								
			}
		}

		return maxStack;
	}

	/**
	 * Translate a Return statement.
	 * 
	 * @param ret
	 * @param varmap
	 * @param environment
	 * @param bytecodes
	 * @return
	 * @throws ClassNotFoundException
	 * @throws MethodNotFoundException
	 * @throws FieldNotFoundException
	 */
	protected int translateReturn(Return ret, HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes)
			throws ClassNotFoundException, MethodNotFoundException,
			FieldNotFoundException {

		if (ret.expr != null) {
			int ms = translateExpression(ret.expr, varmap, environment,
					bytecodes);
			bytecodes.add(new Bytecode.Return(ret.expr.type));
			return ms;
		} else {
			bytecodes.add(new Bytecode.Return(null));
			return 0;
		}
	}

	protected int translateAssign(Assign stmt, HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes)
			throws ClassNotFoundException, MethodNotFoundException,
			FieldNotFoundException {

		int maxStack = 0;

		if (stmt.lhs instanceof LocalVar) {
			LocalVar var = (LocalVar) stmt.lhs;
			assert varmap.keySet().contains(var.name);
			int slot = varmap.get(var.name);
			maxStack = translateExpression(stmt.rhs, varmap, environment,
					bytecodes);
			bytecodes.add(new Bytecode.Store(slot, stmt.lhs.type));
		} else if (stmt.lhs instanceof Deref) {
			Deref der = (Deref) stmt.lhs;
			int ms_lhs = translateExpression(der.target, varmap, environment,
					bytecodes);
			int ms_rhs = translateExpression(stmt.rhs, varmap, environment,
					bytecodes);
			// figure out the type of the field involved
			Type.Reference lhs_t = (Type.Reference) der.target.type;
			Triple<Clazz, Field, Type> fieldInfo = ClassTable.resolveField(
					lhs_t, der.name);
			if (fieldInfo.second().isStatic()) {
				bytecodes.add(new Bytecode.PutField(lhs_t, der.name, fieldInfo
						.third(), Bytecode.STATIC));
			} else {
				bytecodes.add(new Bytecode.PutField(lhs_t, der.name, fieldInfo
						.third(), Bytecode.NONSTATIC));
			}
			maxStack = Math.max(ms_lhs, ms_rhs + 1);

		} else if (stmt.lhs instanceof ArrayIndex) {
			ArrayIndex ai = (ArrayIndex) stmt.lhs;
			int ms_arr = translateExpression(ai.array, varmap, environment,
					bytecodes);
			int ms_idx = translateExpression(ai.idx, varmap, environment,
					bytecodes);
			int ms_rhs = translateExpression(stmt.rhs, varmap, environment,
					bytecodes);
			bytecodes.add(new Bytecode.ArrayStore((Type.Array) ai.array.type));
			maxStack = Math.max(ms_arr, Math.max(ms_idx + 1, ms_rhs + 2));
		} else {
			throw new RuntimeException("Unknown lval encountered");
		}
		return maxStack;
	}

	protected int translateThrow(Throw stmt, HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes)
			throws ClassNotFoundException, MethodNotFoundException,
			FieldNotFoundException {
		int maxStack = translateExpression(stmt.expr, varmap, environment,
				bytecodes);
		bytecodes.add(new Bytecode.Throw());
		return maxStack;
	}

	protected int translateLock(Lock stmt, HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes) {
		bytecodes.add(new Bytecode.Load(varmap.get(stmt.var.name),
				stmt.var.type));
		bytecodes.add(new Bytecode.MonitorEnter());
		return 1;
	}

	protected int translateUnlock(Unlock stmt, HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes) {
		bytecodes.add(new Bytecode.Load(varmap.get(stmt.var.name),
				stmt.var.type));
		bytecodes.add(new Bytecode.MonitorExit());
		return 1;
	}

	/**
	 * This method flatterns an expression into bytecodes, such that the result
	 * is left on top of the stack.
	 * 
	 * @param expr
	 * @param varmap
	 *            Map from local variables to local array slot numbers
	 * @param environment
	 *            Map from local variables to their type
	 * @param bytecodes
	 *            translated bytecodes are appended to this
	 * @return the maximum stack size required for this expression
	 */
	protected int translateExpression(Expr expr,
			HashMap<String, Integer> varmap, HashMap<String, Type> environment,
			ArrayList<Bytecode> bytecodes) throws ClassNotFoundException,
			MethodNotFoundException, FieldNotFoundException {

		int maxStack = 0;

		if (expr instanceof FlowGraph.Number) {
			bytecodes.add(new Bytecode.LoadConst(
					((FlowGraph.Number) expr).value));
			maxStack = 1;
		} else if (expr instanceof LongVal) {
			bytecodes.add(new Bytecode.LoadConst(((LongVal) expr).value));
			maxStack = 2;
		} else if (expr instanceof FloatVal) {
			bytecodes.add(new Bytecode.LoadConst(((FloatVal) expr).value));
			maxStack = 1;
		} else if (expr instanceof DoubleVal) {
			bytecodes.add(new Bytecode.LoadConst(((DoubleVal) expr).value));
			maxStack = 2;
		} else if (expr instanceof NullVal) {
			bytecodes.add(new Bytecode.LoadConst(null));
			maxStack = 1;
		} else if (expr instanceof StringVal) {
			bytecodes.add(new Bytecode.LoadConst(((StringVal) expr).value));
			maxStack = 1;
		} else if (expr instanceof ArrayVal) {
			return translateArrayVal((ArrayVal) expr, varmap, environment,
					bytecodes);
		} else if (expr instanceof ClassVal) {
			return translateClassVal((ClassVal) expr, varmap, environment,
					bytecodes);			
		} else if (expr instanceof LocalVar) {
			LocalVar lv = (LocalVar) expr;
			if (varmap.containsKey(lv.name)) {
				bytecodes.add(new Bytecode.Load(varmap.get(lv.name), lv.type));
				maxStack = Types.slotSize(lv.type);
			} else if (lv.name.equals("$")) {
				// This variable is the "magic" variable. It basically gives
				// access to a value which has been loaded onto the stack (which
				// occurs, for example, when exceptions are thrown)!
				// 				
				// The really exciting thing is that we don't need any kind of
				// translation!
			} else {
				throw new RuntimeException(
						"internal failure (looking for variable " + lv.name
								+ ") " + expr);
			}
		} else if (expr instanceof ClassAccess) {
			// Actually, don't need to do anything here!
		} else if (expr instanceof New) {
			maxStack = translateNew((New) expr, varmap, environment, bytecodes,
					true);
		} else if (expr instanceof Deref) {
			maxStack = translateDeref((Deref) expr, varmap, environment,
					bytecodes);
		} else if (expr instanceof ArrayIndex) {
			ArrayIndex ai = (ArrayIndex) expr;
			int ms_arr = translateExpression(ai.array, varmap, environment,
					bytecodes);
			int ms_idx = translateExpression(ai.idx, varmap, environment,
					bytecodes);
			Type arr_t = ai.array.type;
			bytecodes.add(new Bytecode.ArrayLoad((Type.Array) arr_t));
			maxStack = Math.max(ms_arr, ms_idx + Types.slotSize(arr_t));
		} else if (expr instanceof Invoke) {
			maxStack = translateInvoke((Invoke) expr, varmap, environment,
					bytecodes, true);
		} else if (expr instanceof UnOp) {
			maxStack = translateUnaryOp((UnOp) expr, varmap, environment,
					bytecodes);
		} else if (expr instanceof BinOp) {
			maxStack = translateBinaryOp((BinOp) expr, varmap, environment,
					bytecodes);
		} else if (expr instanceof InstanceOf) {
			InstanceOf iof = (InstanceOf) expr;
			int ms = translateExpression(iof.lhs, varmap, environment,
					bytecodes);
			bytecodes.add(new Bytecode.InstanceOf(iof.rhs));
			maxStack = ms;
		} else if (expr instanceof Cast) {
			maxStack = translateCast((Cast) expr, varmap, environment,
					bytecodes);
		} else if (expr instanceof TernOp) {
			maxStack = translateTernOp((TernOp) expr, varmap, environment,
					bytecodes);
		} else {
			throw new RuntimeException("Unknown expression encountered ("
					+ expr + ")");
		}
		return maxStack;
	}

	public int translateClassVal(ClassVal cval,  HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes) {
		if(cval.classType instanceof Type.Primitive) {
			bytecodes.add(new Bytecode.GetField(Typing
					.boxedType((Type.Primitive) cval.classType), "TYPE", Type
					.referenceType("java.lang", "Class"), Bytecode.STATIC));
		} else {
			bytecodes.add(new Bytecode.LoadConst(cval.classType));
		}
		return 1;
	}
	
	public int translateDeref(Deref def, HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes)
			throws ClassNotFoundException, MethodNotFoundException,
			FieldNotFoundException {

		int maxStack = 0;
		Type tmp_t = def.target.type;

		if (tmp_t instanceof Type.Reference) {
			Type.Reference lhs_t = (Type.Reference) tmp_t;

			// Figure out what field is being referenced
			Triple<Clazz, Field, Type> fieldInfo = ClassTable.resolveField(
					lhs_t, def.name);

			Field field = fieldInfo.second();
			Type actualFieldType = field.type();
			Type desiredFieldType = fieldInfo.third();

			if (field.isStatic()) {
				// This is a static field load					
				bytecodes.add(new Bytecode.GetField(lhs_t, def.name,
						actualFieldType, Bytecode.STATIC));
				maxStack = Types.slotSize(actualFieldType);		
			} else {
				// Non-static field load
				maxStack = translateExpression(def.target, varmap, environment,
						bytecodes);

				bytecodes.add(new Bytecode.GetField(lhs_t, def.name,
						actualFieldType, Bytecode.NONSTATIC));

				if (actualFieldType instanceof Type.Variable
						&& !(desiredFieldType instanceof Type.Variable)
						&& !desiredFieldType.equals(Type.referenceType(
								"java.lang", "Object"))) {
					// Ok, actual type is a (generic) type variable. Need to
					// cast to the desired type!
					bytecodes.add(new Bytecode.CheckCast(desiredFieldType));					
				}
				
				maxStack = Math.max(maxStack,Types.slotSize(actualFieldType));	
			}
		} else if (tmp_t instanceof Type.Array && def.name.equals("length")) {
			maxStack = translateExpression(def.target, varmap, environment,
					bytecodes);
			bytecodes.add(new Bytecode.ArrayLength());
		} else {
			throw new RuntimeException(
					"Attempt to dereference variable with type "
							+ tmp_t.toString());
		}

		return maxStack;
	}

	public int translateArrayVal(ArrayVal av, HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes)
			throws ClassNotFoundException, MethodNotFoundException,
			FieldNotFoundException {
		int maxStack = translateNew(new New(av.type, new IntVal(av.values
				.size())), varmap, environment, bytecodes, true);

		int index = 0;
		for (Expr e : av.values) {
			bytecodes.add(new Bytecode.Dup(av.type));
			bytecodes.add(new Bytecode.LoadConst(index++));
			int ms = translateExpression(e, varmap, environment, bytecodes);
			bytecodes.add(new Bytecode.ArrayStore((Type.Array) av.type));
			maxStack = Math.max(maxStack, 3 + ms);
		}

		return maxStack;
	}

	public int translateNew(New news, HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes,
			boolean needReturnValue) throws ClassNotFoundException,
			MethodNotFoundException, FieldNotFoundException {

		int maxStack = 0;

		if (news.type instanceof Type.Reference) {
			Type.Reference type = (Type.Reference) news.type;

			bytecodes.add(new Bytecode.New(news.type));
			bytecodes.add(new Bytecode.Dup(news.type));

			maxStack = 3;
			int usedStack = 3;

			ArrayList<Type> paramTypes = new ArrayList<Type>();
			for (Expr p : news.parameters) {
				int ms = translateExpression(p, varmap, environment, bytecodes);
				maxStack = Math.max(ms + usedStack, maxStack);
				usedStack += Types.slotSize(p.type);
				paramTypes.add(p.type);
			}

			// Find the appropriate construction method.

			Triple<Clazz, Method, Type.Function> minfo = ClassTable
					.resolveMethod(type, type.name(), paramTypes);
			Type.Function actualMethodT = minfo.second().type();
			// call the appropriate constructor
			bytecodes.add(new Bytecode.Invoke(type, "<init>", actualMethodT,
					Bytecode.SPECIAL));
		} else if (news.type instanceof Type.Array) {
			// Type.Array type = (Type.Array) news.type; //never used

			int usedStack = 0;

			for (Expr p : news.parameters) {
				int ms = translateExpression(p, varmap, environment, bytecodes);
				maxStack = Math.max(ms + usedStack, maxStack);
				usedStack += Types.slotSize(p.type);
			}

			bytecodes.add(new Bytecode.New(news.type, news.parameters.size()));
		}

		if (!needReturnValue) {
			// the return value is not required, so we need to pop it from
			// the stack
			bytecodes.add(new Bytecode.Pop(news.type));
		}
		return maxStack;
	}

	protected int translateBinaryOp(BinOp bop, HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes)
			throws ClassNotFoundException, MethodNotFoundException,
			FieldNotFoundException {

		// second, translate the binary operator.
		switch (bop.op) {
		case BinOp.LT:
		case BinOp.LTEQ:
		case BinOp.GT:
		case BinOp.GTEQ:
		case BinOp.EQ:
		case BinOp.NEQ:
		case BinOp.LAND:
		case BinOp.LOR: {
			String trueLabel = "CL" + condLabelCount++;
			String exitLabel = "CL" + condLabelCount++;
			int maxStack = translateConditionalBranch(bop, trueLabel, varmap,
					environment, bytecodes);
			bytecodes.add(new Bytecode.LoadConst(0));
			bytecodes.add(new Bytecode.Goto(exitLabel));
			bytecodes.add(new Bytecode.Label(trueLabel));
			bytecodes.add(new Bytecode.LoadConst(1));
			bytecodes.add(new Bytecode.Label(exitLabel));
			return maxStack;
		}
		case BinOp.CONCAT:
			return translateStringConcatenate(bop, varmap, environment,
					bytecodes);
		}

		// must be a standard arithmetic operation.
		int ms_lhs = translateExpression(bop.lhs, varmap, environment,
				bytecodes);
		int ms_rhs = translateExpression(bop.rhs, varmap, environment,
				bytecodes);

		bytecodes.add(new Bytecode.BinOp(bop.op, bop.type));
		return Math.max(ms_lhs, ms_rhs + Types.slotSize(bop.type));
	}

	protected int translateStringConcatenate(BinOp bop,
			HashMap<String, Integer> varmap, HashMap<String, Type> environment,
			ArrayList<Bytecode> bytecodes) throws ClassNotFoundException,
			MethodNotFoundException, FieldNotFoundException {

		Type.Reference builder = new Type.Reference("java.lang",
				"StringBuilder");

		bytecodes.add(new Bytecode.New(builder));
		bytecodes.add(new Bytecode.Dup(builder));

		int maxStack = 3;

		Triple<Clazz, Method, Type.Function> minfo = ClassTable.resolveMethod(
				builder, "StringBuilder", new LinkedList<Type>());
		Type.Function actualMethodT = minfo.second().type();
		bytecodes.add(new Bytecode.Invoke(builder, "<init>", actualMethodT,
				Bytecode.SPECIAL));

		int ms = translateStringConcatenateSubExpr(builder, bop, varmap,
				environment, bytecodes);

		minfo = ClassTable.resolveMethod(builder, "toString",
				new LinkedList<Type>());
		actualMethodT = minfo.second().type();
		bytecodes.add(new Bytecode.Invoke(builder, "toString", actualMethodT,
				Bytecode.VIRTUAL));

		return Math.max(maxStack, ms + 1);
	}

	protected int translateStringConcatenateSubExpr(Type.Reference builder,
			Expr expr, HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes)
			throws ClassNotFoundException, MethodNotFoundException,
			FieldNotFoundException {
		int ms;
		if (expr instanceof BinOp && ((BinOp) expr).op == BinOp.CONCAT) {
			BinOp bop = (BinOp) expr;
			int ml = translateStringConcatenateSubExpr(builder, bop.lhs,
					varmap, environment, bytecodes);
			int mr = translateStringConcatenateSubExpr(builder, bop.rhs,
					varmap, environment, bytecodes);
			ms = Math.max(ml, mr);
		} else {
			ms = translateExpression(expr, varmap, environment, bytecodes);
			List<Type> params = new LinkedList<Type>();
			params.add(expr.type);
			Triple<Clazz, Method, Type.Function> minfo = ClassTable
					.resolveMethod(builder, "append", params);
			Type.Function actualMethodT = minfo.second().type();
			bytecodes.add(new Bytecode.Invoke(builder, "append", actualMethodT,
					Bytecode.VIRTUAL));
		}
		return ms;
	}

	protected int translateUnaryOp(UnOp uop, HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes)
			throws ClassNotFoundException, MethodNotFoundException,
			FieldNotFoundException {
		// second, translate the operation.
		// FIXME: resolve operator type

		switch (uop.op) {
		case UnOp.NOT:
			String trueLabel = "CL" + condLabelCount++;
			String exitLabel = "CL" + condLabelCount++;
			int ms = translateConditionalBranch(uop, trueLabel, varmap,
					environment, bytecodes);
			bytecodes.add(new Bytecode.LoadConst(0));
			bytecodes.add(new Bytecode.Goto(exitLabel));
			bytecodes.add(new Bytecode.Label(trueLabel));
			bytecodes.add(new Bytecode.LoadConst(1));
			bytecodes.add(new Bytecode.Label(exitLabel));
			return ms;
		case UnOp.PREINC:
			if (uop.expr instanceof LocalVar) {
				LocalVar lvar = (LocalVar) uop.expr;
				bytecodes.add(new Bytecode.Iinc(varmap.get(lvar.name), 1));
			} else {
				// the else clause is implemented after translateExpression
			}
			break;
		case UnOp.PREDEC:
			if (uop.expr instanceof LocalVar) {
				LocalVar lvar = (LocalVar) uop.expr;
				bytecodes.add(new Bytecode.Iinc(varmap.get(lvar.name), -1));
			} else {
				// the else clause is implemented after translateExpression
			}
			break;
		}

		// first, translate the expression.
		int maxStack = translateExpression(uop.expr, varmap, environment,
				bytecodes);

		switch (uop.op) {
		case UnOp.INV:
			bytecodes.add(new Bytecode.LoadConst(new Integer(-1)));
			bytecodes
					.add(new Bytecode.BinOp(Bytecode.BinOp.XOR, Type.intType()));
			maxStack = Math.max(maxStack, 2);
			break;
		case UnOp.NEG:
			bytecodes.add(new Bytecode.Neg(uop.type));
			break;
		case UnOp.POSTINC:
			if (uop.expr instanceof LocalVar) {
				LocalVar lvar = (LocalVar) uop.expr;
				bytecodes.add(new Bytecode.Iinc(varmap.get(lvar.name), 1));
			} else if (uop.expr instanceof Deref) {
				// this is all a little bit ugly.
				Bytecode tmp = (Bytecode) bytecodes.get(bytecodes.size() - 1);

				// check whether this was a static or instance field load
				if (tmp instanceof Bytecode.GetField) {
					Bytecode.GetField getField = (Bytecode.GetField) tmp;
					if (getField.mode != Bytecode.STATIC) {
						bytecodes.add(bytecodes.size() - 1, new Bytecode.Dup(
								Type.intType()));
						bytecodes.add(new Bytecode.DupX1());
						bytecodes.add(new Bytecode.LoadConst(1));
						bytecodes.add(new Bytecode.BinOp(Bytecode.BinOp.ADD,
								Type.intType()));
						bytecodes.add(new Bytecode.PutField(getField.owner,
								getField.name, getField.type,
								Bytecode.NONSTATIC));
					} else {
						// static field load, so no target stack element
						bytecodes.add(new Bytecode.Dup(Type.intType()));
						bytecodes.add(new Bytecode.LoadConst(1));
						bytecodes.add(new Bytecode.BinOp(Bytecode.BinOp.ADD,
								Type.intType()));
						bytecodes.add(new Bytecode.PutField(getField.owner,
								getField.name, getField.type, Bytecode.STATIC));
					}
				}
				maxStack = maxStack + 3;
			}
			break;
		case UnOp.POSTDEC:
			if (uop.expr instanceof LocalVar) {
				LocalVar lvar = (LocalVar) uop.expr;
				bytecodes.add(new Bytecode.Iinc(varmap.get(lvar.name), -1));
			} else {
				// this is all a little but ugly
				// this is all a little bit ugly.
				Bytecode tmp = (Bytecode) bytecodes.get(bytecodes.size() - 1);

				// check whether this was a static or instance field load
				if (tmp instanceof Bytecode.GetField) {
					Bytecode.GetField getField = (Bytecode.GetField) tmp;
					if (getField.mode != Bytecode.STATIC) {
						bytecodes.add(bytecodes.size() - 1, new Bytecode.Dup(
								Type.intType()));
						bytecodes.add(new Bytecode.DupX1());
						bytecodes.add(new Bytecode.LoadConst(1));
						bytecodes.add(new Bytecode.BinOp(Bytecode.BinOp.SUB,
								Type.intType()));
						bytecodes.add(new Bytecode.PutField(getField.owner,
								getField.name, getField.type,
								Bytecode.NONSTATIC));
					} else {
						// static field load, so no target stack element
						bytecodes.add(new Bytecode.Dup(Type.intType()));
						bytecodes.add(new Bytecode.LoadConst(1));
						bytecodes.add(new Bytecode.BinOp(Bytecode.BinOp.SUB,
								Type.intType()));
						bytecodes.add(new Bytecode.PutField(getField.owner,
								getField.name, getField.type, Bytecode.STATIC));
					}
				}
				maxStack = maxStack + 3;
			}
			break;
		case UnOp.PREINC:
			if (uop.expr instanceof Deref) {
				// this is all a little bit ugly.
				Bytecode tmp = (Bytecode) bytecodes.get(bytecodes.size() - 1);

				// check whether this was a static or instance field load
				if (tmp instanceof Bytecode.GetField) {
					Bytecode.GetField getField = (Bytecode.GetField) tmp;
					if (getField.mode != Bytecode.STATIC) {
						bytecodes.add(bytecodes.size() - 1, new Bytecode.Dup(
								Type.intType()));
						bytecodes.add(new Bytecode.LoadConst(1));
						bytecodes.add(new Bytecode.BinOp(Bytecode.BinOp.ADD,
								Type.intType()));
						bytecodes.add(new Bytecode.DupX1());
						bytecodes.add(new Bytecode.PutField(getField.owner,
								getField.name, getField.type,
								Bytecode.NONSTATIC));
					} else {
						// static field load, so no target stack element
						bytecodes.add(new Bytecode.LoadConst(1));
						bytecodes.add(new Bytecode.BinOp(Bytecode.BinOp.ADD,
								Type.intType()));
						bytecodes.add(new Bytecode.Dup(Type.intType()));
						bytecodes.add(new Bytecode.PutField(getField.owner,
								getField.name, getField.type, Bytecode.STATIC));
					}
				}
				maxStack = maxStack + 3;
			}
			break;
		case UnOp.PREDEC:
			if (uop.expr instanceof Deref) {
				// this is all a little bit ugly.
				Bytecode tmp = (Bytecode) bytecodes.get(bytecodes.size() - 1);

				// check whether this was a static or instance field load
				if (tmp instanceof Bytecode.GetField) {
					Bytecode.GetField getField = (Bytecode.GetField) tmp;
					if (getField.mode != Bytecode.STATIC) {
						bytecodes.add(bytecodes.size() - 1, new Bytecode.Dup(
								Type.intType()));
						bytecodes.add(new Bytecode.LoadConst(1));
						bytecodes.add(new Bytecode.BinOp(Bytecode.BinOp.SUB,
								Type.intType()));
						bytecodes.add(new Bytecode.DupX1());
						bytecodes.add(new Bytecode.PutField(getField.owner,
								getField.name, getField.type,
								Bytecode.NONSTATIC));
					} else {
						// static field load, so no target stack element
						bytecodes.add(new Bytecode.LoadConst(1));
						bytecodes.add(new Bytecode.BinOp(Bytecode.BinOp.SUB,
								Type.intType()));
						bytecodes.add(new Bytecode.Dup(Type.intType()));
						bytecodes.add(new Bytecode.PutField(getField.owner,
								getField.name, getField.type, Bytecode.STATIC));
					}
				}
				maxStack = maxStack + 3;
			}
			break;
		default:
			throw new RuntimeException("Unknown unary expression encountered ("
					+ uop + ")");
		}

		return maxStack;
	}

	protected int translateTernOp(TernOp top, HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes)
			throws ClassNotFoundException, MethodNotFoundException,
			FieldNotFoundException {
		Random r = new Random();
		Integer tLabel = new Integer(r.nextInt());
		// Integer fLabel = new Integer(r.nextInt()); never used
		Integer endLabel = new Integer(r.nextInt());

		int ms = translateConditionalBranch(top.cond, "L" + tLabel, varmap,
				environment, bytecodes);
		ms = Math.max(ms, translateExpression(top.foption, varmap, environment,
				bytecodes));
		bytecodes.add(new Bytecode.Goto("L" + endLabel));
		bytecodes.add(new Bytecode.Label("L" + tLabel));
		ms = Math.max(ms, translateExpression(top.toption, varmap, environment,
				bytecodes));
		bytecodes.add(new Bytecode.Label("L" + endLabel));

		return ms;
	}

	protected int translateCast(Cast cast, HashMap<String, Integer> varmap,
			HashMap<String, Type> environment, ArrayList<Bytecode> bytecodes)
			throws ClassNotFoundException, MethodNotFoundException,
			FieldNotFoundException {
		
		int maxStack = translateExpression(cast.expr, varmap, environment,
				bytecodes);

		Type srcType = cast.expr.type;
		// Now, do implicit conversions
		if (cast.type instanceof Type.Primitive
				&& srcType instanceof Type.Primitive) {
			bytecodes.add(new Bytecode.Conversion((Type.Primitive) srcType,
					(Type.Primitive) cast.type));
		} else if (!cast.type.supsetEqOf(srcType)) {
			bytecodes.add(new Bytecode.CheckCast(cast.type));
		} else if (cast.type.supsetEqOf(srcType)) {
			// this cast automatically succeeds!
		} else {
			throw new RuntimeException("Error implementing cast");
		}
		
		return Math.max(maxStack, Types.slotSize(cast.type));
	}

	// ============================================================
	// OTHER HELPER METHODS
	// ============================================================

	/**
	 * This method flattens the control-flow graph into a sequence of
	 * statements. This is done using a depth-first traversal of the CFG, whilst
	 * ignoring exception edges.
	 * 
	 * @param entry
	 *            entry point of method in control-flow graph.
	 * @param cfg
	 *            control-flow graph of method.
	 * @return List of statements in their flattened order
	 */
	public static ArrayList<Point> cfgOrder(Point entry,
			Graph<Point, Triple<Point, Point, Expr>> cfg) {
		// first, perform the depth-first search.
		ArrayList<Point> ord = new ArrayList<Point>();
		cfgVisit(entry, new HashSet<Point>(), ord, cfg);
		// we need to reverse the ordering here, since cfg_visit
		// will have added the statements in reverse topological order!
		Collections.reverse(ord);
		return ord;
	}

	/**
	 * This method performs a standard depth-first search.
	 * 
	 * @param cfg
	 *            the control-flow graph.
	 */
	protected static void cfgVisit(Point v, Set<Point> visited,
			List<Point> ord, Graph<Point, Triple<Point, Point, Expr>> cfg) {
		visited.add(v);

		// Sort out-edges according to their target position in the program.
		// Doing this helps ensure blocks which are located close together in
		// the source remain close together. Otherwise, for example, you end up
		// with for-loops where the code after the for loop comes before the
		// for-loop body!!!
		ArrayList<Pair<Point, Point>> outs;
		outs = new ArrayList<Pair<Point, Point>>(cfg.from(v));

		Collections.sort(outs, new Comparator<Pair<Point, Point>>() {
			public int compare(Pair<Point, Point> p1, Pair<Point, Point> p2) {
				Point e1 = p1.second();
				Point e2 = p2.second();
				if (e1.line() < e2.line()) {
					return -1;
				} else if (e1.line() == e2.line()) {
					if (e1.column() < e2.column()) {
						return -1;
					} else if (e1.column() == e2.column()) {
						return 0;
					}
				}
				return 1;
			}
		});

		// Now, visit the edges in their sorted order
		for (Pair<Point, Point> e : outs) {
			if (!visited.contains(e.second())) {
				cfgVisit(e.second(), visited, ord, cfg);
			}
		}
		ord.add(v);
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
		
		if (clazz.inners().size() > 0 || clazz.isInnerClass()) {
			Constant.addPoolItem(new Constant.Utf8("InnerClasses"),constantPool);
			for(Triple<Type.Reference,Integer,Boolean> i : clazz.inners()) {
				Constant.addPoolItem(Constant.buildClass(i.first()), constantPool);
				Constant.addPoolItem(new Constant.Utf8(i.first().name()), constantPool);
			}
			if(clazz.isInnerClass()) {
				Type.Reference inner = clazz.type();
				Pair<String,Type[]>[] classes = clazz.type().classes();
				for(int i=classes.length-1;i>0;--i) {
					// First, we need to construct the outer reference type.
					Pair<String,Type[]>[] nclasses = new Pair[i];
					System.arraycopy(classes,0,nclasses,0,nclasses.length);				
					Type.Reference outer = Type.referenceType(inner.pkg(),nclasses);
					// Now, we can actually write the information.									
					Constant.addPoolItem(Constant.buildClass(outer), constantPool);
					Constant.addPoolItem(Constant.buildClass(inner), constantPool);
					Constant.addPoolItem(new Constant.Utf8(inner.name()), constantPool);									
					inner = outer;				
				}
			}	
		}				
		
		for (Field f : clazz.fields()) {
			Constant.addPoolItem(new Constant.Utf8(f.name()), constantPool);
			Constant.addPoolItem(new Constant.Utf8(Types.descriptor(f.type(),false)),
					constantPool);
			if(isGeneric(f.type())) {
				Constant.addPoolItem(new Constant.Utf8(Types.descriptor(f
						.type(), true)), constantPool);
			}
			if(f.constantValue() != null) {
				if(f.constantValue() instanceof java.lang.Number) {
					Constant.addPoolItem(Constant.fromNumber((java.lang.Number) f.constantValue()), constantPool);	
				} else {
					Constant.addPoolItem(Constant.fromString((String) f.constantValue()), constantPool);
				}
			}
		}

		for (Method m : clazz.methods()) {
			if (m.name().equals(clazz.name())) {
				Constant.addPoolItem(new Constant.Utf8("<init>"), constantPool);
			} else {
				Constant.addPoolItem(new Constant.Utf8(m.name()), constantPool);
			}
			
			Constant.addPoolItem(new Constant.Utf8(Types.descriptor(m.type(),
					false)), constantPool);
			if (m.code() != null) {
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
				
				for(ExceptionHandler h : handlers) {
					if(!h.exception.unqualifiedName().equals("java.lang.Throwable")) {
						Constant.addPoolItem(Constant.buildClass(h.exception), constantPool);
					}
				}
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
		if(!(t instanceof Type.Reference)) {
			return false;
		}
		Type.Reference ref = (Type.Reference) t;
		for(Pair<String, Type[]> p : ref.classes()) {
			if(p.second().length > 0) {
				return true;
			}
		}
		return false;
	}
	
	protected boolean isGenericArray(Type t) {
		if(t instanceof Type.Array) {
			Type et = ((Type.Array)t).elementType();
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