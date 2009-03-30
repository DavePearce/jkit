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

import java.io.*;
import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.jil.tree.*;
import jkit.jil.util.*;
import jkit.util.*;

public class ClassFileWriter {
	protected final OutputStream output;
	protected final PrintWriter printer;
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
		printer = null;
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
		PrintWriter myPrinter = null;
		
		for (Pair<String, String> option : options) {
			if (option.first().equals("version")) {
				myVersion = Integer.parseInt(option.second());
			} else if (option.first().equals("optimiser")) {
				myOptimiser = (BytecodeOptimiser) Class
						.forName(option.second()).newInstance();
			} else if (option.first().equals("outputText")) {
				myOutputText = Boolean.parseBoolean(option.second());
				myPrinter = new PrintWriter(o);
			}
		}

		version = myVersion;
		optimiser = myOptimiser;
		outputText = myOutputText;
		printer = myPrinter;
		this.loader = loader;		
	}
	
	public void write(ClassFile cfile) throws IOException {
		if(outputText) {
			writeTextClass(clazz);			
		} else {
			ArrayList<Constant.Info> constantPool = cfile.constantPool();

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
	}

	protected void writeTextClass(Clazz jc) throws IOException {
		HashMap<Constant.Info, Integer> constantPool = buildConstantPool(jc);
		Constant.Info[] poolSequence = new Constant.Info[constantPool.size()];
		for (Map.Entry<Constant.Info, Integer> e : constantPool.entrySet()) {
			poolSequence[e.getValue()] = e.getKey();
		}

		for (Constant.Info c : poolSequence) {
			if (c != null) { // item at index 0 is always null
				printer.println(c);
			}
		}
		
		printer.print("\nclass " + jc.type() + " ");
		if(jc.superClass() != null) {
			printer.println("");
			printer.print("\t extends " + jc.superClass());
		}
		if(jc.interfaces().size() > 0) {
			printer.println("");
			printer.print("\t implements ");
			boolean firstTime=true;
			for(Type.Clazz i : jc.interfaces()) {
				if(!firstTime) {
					printer.print(", ");
				}
				firstTime=false;
				printer.print(i);
			}			
		}				
		
		printer.println(" {\n");
		
		for(Field f : jc.fields()) {
			writeTextField(f);
		}
		
		printer.println("");
		
		for(Method m : jc.methods()) {
			writeTextMethod(jc,m,constantPool);
		}
	
		printer.println("}");
		
		printer.flush();
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
		write_u2(pmap.get(new Constant.Utf8(ClassFile.descriptor(f.type(), false))));
		
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

	protected void writeTextField(Field f) {
		printer.print("\t");
		writeTextModifiers(f.modifiers());
		printer.print(f.type());
		printer.println(" " + f.name() + ";");		
	
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

		write_u2(pmap.get(new Constant.Utf8(ClassFile.descriptor(m.type(), false))));

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

	protected void writeTextMethod(Clazz clazz, Method method,
			HashMap<Constant.Info, Integer> pmap) throws IOException {
		printer.print("\t");
		writeTextModifiers(method.modifiers());
		Type.Function type = method.type(); 
		printer.print(type.returnType() + " " + method.name());
		printer.print("(");
		boolean firstTime=true;
		
		List<Type> paramTypes = type.parameterTypes();
		List<Pair<String,List<Modifier>>> params = method.parameters();
		
		for(int i = 0; i != params.size();++i) {
			if(!firstTime) {
				printer.print(", ");
			}
			firstTime=false;
			writeTextModifiers(params.get(i).second());			
			printer.print(paramTypes.get(i));
			printer.print(" " + params.get(i).first());
		}
		
		printer.println(") {");
		
		if(method.body() != null) {
			writeTextCodeAttribute(clazz,method,pmap);
		}		
		
		printer.println("\t}");
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

	protected void writeTextCodeAttribute(Clazz c, Method method,
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

		printer.println("\t\tmaxstack = " + maxStack + ", maxLocals = "
				+ maxLocals);
		
		for(Bytecode b : bytecodes) {
			printer.println("\t\t" + b);
		}

		// need to dump out exception handlers here.
	}

	
	protected int translateMethodCode(Clazz clazz, Method method,
			ArrayList<Bytecode> bytecodes, ArrayList<ExceptionHandler> handlers) {

		
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