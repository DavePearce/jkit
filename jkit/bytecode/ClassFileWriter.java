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
	protected final ClassLoader loader;

	/**
	 * Construct a ClassFileWriter Object that the given output stream to write
	 * ClassFiles.
	 * 
	 * @param o
	 *            Output stream for class bytes
	 */
	public ClassFileWriter(OutputStream o, ClassLoader loader) {
		output = o;		
		this.loader = loader;
	}
	
	public void write(ClassFile cfile) throws IOException {
		ArrayList<Constant.Info> constantPool = cfile.constantPool();
		HashMap<Constant.Info,Integer> poolMap = new HashMap<Constant.Info,Integer>();
		
		int index = 0;
		for(Constant.Info ci : constantPool) {
			poolMap.put(ci, index++);
		}
		
		write_u1(0xCA);
		write_u1(0xFE);
		write_u1(0xBA);
		write_u1(0xBE);
		write_u4(cfile.version());
		write_u2(constantPool.size());
		// now, write the constant pool
		for (Constant.Info c : constantPool) {
			if (c != null) { // item at index 0 is always null
				output.write(c.toBytes(poolMap));
			}
		}
		
		// ok, done that now write more stuff
		writeModifiers(cfile.modifiers());
		write_u2(poolMap.get(Constant.buildClass(cfile.type())));
		if (cfile.superClazz() != null) {
			write_u2(poolMap.get(Constant.buildClass(cfile.superClazz())));
		}
		write_u2(cfile.interfaces().size());
		for (Type.Reference i : cfile.interfaces()) {
			write_u2(poolMap.get(Constant.buildClass(i)));
		}

		write_u2(cfile.fields().size());
		for (ClassFile.Field f : cfile.fields()) {
			writeField(f, poolMap);
		}

		write_u2(cfile.methods().size());
		for (ClassFile.Method m : cfile.methods()) {
			writeMethod(m, poolMap);
		}

		int nattributes = 0;

		if(cfile.needClassSignature()) { nattributes++; }

		if(cfile.inners().size() > 0 || cfile.isInnerClass()) { nattributes++; }


		write_u2(nattributes);		
		if (cfile.needClassSignature()) {			
			writeClassSignature(cfile, poolMap);
		}

		 if (cfile.inners().size() > 0 || cfile.isInnerClass()) {			
			writeInnerClassAttribute(cfile, poolMap);
		 }

		output.flush();
	}
	
	protected void writeClassSignature(ClassFile clazz,
			HashMap<Constant.Info, Integer> pmap) throws IOException {		
		write_u2(pmap.get(new Constant.Utf8("Signature")));
		write_u4(2); 
		write_u2(pmap.get(new Constant.Utf8(clazz.classSignature())));
	}

	/**
     * Write attribute detailing what direct inner classes there are for this
     * class, or what inner class this class is in.
     * 
     * @param clazz
     * @param pmap
     */
	protected void writeInnerClassAttribute(ClassFile clazz,
			Map<Constant.Info, Integer> pmap) throws IOException {
		write_u2(pmap.get(new Constant.Utf8("InnerClasses")));
		
		int ninners = clazz.inners().size() + clazz.type().components().size()
				- 1;
		
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
				try {
					// This dependence on ClassTable here is annoying really.
					Clazz innerC = loader.loadClass(inner);
					writeModifiers(innerC.modifiers());
				} catch(ClassNotFoundException e) {
					write_u2(0); // this is a problem!!!!
				 }
				inner = outer;				
			}
		}		
		
		for(Pair<Type.Clazz,List<Modifier>> i : clazz.inners()) {
			write_u2(pmap.get(Constant.buildClass(i.first())));
			write_u2(pmap.get(Constant.buildClass(clazz.type())));
			String name = i.first().lastComponent().first();
			write_u2(pmap.get(new Constant.Utf8(name)));
			writeModifiers(i.second());			
		}		
	}
	
	protected void writeField(ClassFile.Field f, HashMap<Constant.Info, Integer> pmap)
			throws IOException {
		writeModifiers(f.modifiers());
		write_u2(pmap.get(new Constant.Utf8(f.name())));
		write_u2(pmap.get(new Constant.Utf8(ClassFile.descriptor(f.type(), false))));
		
		// FIXME: support for constant values
		// int attrNum = ((isGeneric(f.type())) ? 1 : 0) + ((f.constantValue() != null) ? 1 : 0);
		
		int attrNum = ((ClassFile.isGeneric(f.type())) ? 1 : 0);
		// Write number of attributes
		write_u2(attrNum);
		// We only need to write a Signature attribute if the field has generic params
		if (ClassFile.isGeneric(f.type())) {
			write_u2(pmap.get(new Constant.Utf8("Signature")));
			write_u4(2);
			write_u2(pmap.get(new Constant.Utf8(ClassFile.descriptor(f.type(),
					true))));
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

	protected void writeMethod(ClassFile.Method m,
			HashMap<Constant.Info, Integer> pmap) throws IOException {

		writeModifiers(m.modifiers());
		write_u2(pmap.get(new Constant.Utf8(m.name())));		
		write_u2(pmap.get(new Constant.Utf8(ClassFile.descriptor(m.type(), false))));

		int nattrs = 0;
		Code codeAttr = (Code) m.attribute(Code.class);
		
		if (codeAttr != null) {
			nattrs++;
		}
		if (!m.exceptions().isEmpty()) {
			nattrs++;
		}
		if(ClassFile.isGeneric(m.type().returnType())) {
			nattrs++;
		}

		write_u2(nattrs);
		if (codeAttr != null) {
			writeCodeAttribute(codeAttr, pmap);
		}
		if (!m.exceptions().isEmpty()) {
			writeExceptionsAttribute(m, pmap);
		}
		if(ClassFile.isGeneric(m.type().returnType())) {
			write_u2(pmap.get(new Constant.Utf8("Signature")));
			write_u4(2);
			write_u2(pmap.get(new Constant.Utf8(ClassFile
					.descriptor(m.type(), true))));
		}
	}

	protected void writeExceptionsAttribute(ClassFile.Method method,
			HashMap<Constant.Info, Integer> constantPool) throws IOException {

		List<Type.Clazz> exceptions = method.exceptions();
		write_u2(constantPool.get(new Constant.Utf8("Exceptions")));
		write_u4(2 + (2 * exceptions.size()));
		write_u2(exceptions.size());
		for (Type.Clazz e : exceptions) {
			write_u2(constantPool.get(Constant.buildClass(e)));
		}
	}

	protected void writeCodeAttribute(ClassFile.Code code,
			HashMap<Constant.Info, Integer> constantPool) throws IOException {

		// This method is a little tricky. The basic strategy is to first
		// translate each bytecode into it's binary representation. One
		// difficulty here, is that we must defer calculating the targets of
		// branch statements until after this is done, since we can't do the
		// calculation without exact values.

		// === DETERMINE LABEL OFFSETS ===

		HashMap<String, Integer> labelOffsets = new HashMap<String, Integer>();
		List<Bytecode> bytecodes = code.bytecodes();
		
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
		int exception_table_length = code.handlers().size() * 8;
		// need to figure out attribute_table length
		int attribute_table_length = 0;
		// write attribute length
		write_u4(bytes.length + exception_table_length + attribute_table_length
				+ 12);
		// now write data
		write_u2(code.maxStack());
		write_u2(code.maxLocals());
		write_u4(bytes.length);
		// write bytecode instructions
		for (int i = 0; i != bytes.length; ++i) {
			write_u1(bytes[i]);
		}

		// write exception handlers
		write_u2(code.handlers().size());
		for (ClassFile.Handler h : code.handlers()) {
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