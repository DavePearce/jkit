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
	protected final BinaryOutputStream output;
	protected final ClassLoader loader;

	/**
	 * Construct a ClassFileWriter Object that the given output stream to write
	 * ClassFiles.
	 * 
	 * @param o
	 *            Output stream for class bytes
	 */
	public ClassFileWriter(OutputStream o, ClassLoader loader) {
		output = new BinaryOutputStream(o);		
		this.loader = loader;
	}
	
	public void write(ClassFile cfile) throws IOException {
		ArrayList<Constant.Info> constantPool = cfile.constantPool();
		HashMap<Constant.Info,Integer> poolMap = new HashMap<Constant.Info,Integer>();
		
		int index = 0;
		for(Constant.Info ci : constantPool) {
			poolMap.put(ci, index++);
		}
		
		output.write_u1(0xCA);
		output.write_u1(0xFE);
		output.write_u1(0xBA);
		output.write_u1(0xBE);
		output.write_u4(cfile.version());
		output.write_u2(constantPool.size());
		// now, write the constant pool
		for (Constant.Info c : constantPool) {
			if (c != null) { // item at index 0 is always null
				output.write(c.toBytes(poolMap));
			}
		}
		
		// ok, done that now write more stuff
		writeModifiers(cfile.modifiers());
		output.write_u2(poolMap.get(Constant.buildClass(cfile.type())));
		if (cfile.superClazz() != null) {
			output.write_u2(poolMap.get(Constant.buildClass(cfile.superClazz())));
		}
		output.write_u2(cfile.interfaces().size());
		for (Type.Reference i : cfile.interfaces()) {
			output.write_u2(poolMap.get(Constant.buildClass(i)));
		}

		output.write_u2(cfile.fields().size());
		for (ClassFile.Field f : cfile.fields()) {
			writeField(f, poolMap);
		}

		output.write_u2(cfile.methods().size());
		for (ClassFile.Method m : cfile.methods()) {
			writeMethod(m, poolMap);
		}

		output.write_u2(cfile.attributes.size());
		for(Attribute a : cfile.attributes()) {
			a.write(output, poolMap);
		}
		
		output.flush();
	}
	
	protected void writeField(ClassFile.Field f,
			HashMap<Constant.Info, Integer> constantPool) throws IOException {
		writeModifiers(f.modifiers());
		output.write_u2(constantPool.get(new Constant.Utf8(f.name())));
		output.write_u2(constantPool.get(new Constant.Utf8(ClassFile
				.descriptor(f.type(), false))));

		// Write number of attributes
		output.write_u2(f.attributes().size());

		for (Attribute a : f.attributes()) {
			a.write(output, constantPool);
		}
	}

	protected void writeMethod(ClassFile.Method m,
			HashMap<Constant.Info, Integer> constantPool) throws IOException {

		writeModifiers(m.modifiers());
		output.write_u2(constantPool.get(new Constant.Utf8(m.name())));
		output.write_u2(constantPool.get(new Constant.Utf8(ClassFile
				.descriptor(m.type(), false))));

		output.write_u2(m.attributes.size());

		for (Attribute a : m.attributes) {
			a.write(output, constantPool);
		}
	}

	protected void writeModifiers(List<Modifier> modifiers) throws IOException {
		writeModifiers(modifiers,output);
	}
	
	// static so that it can be reused.
	public static void writeModifiers(List<Modifier> modifiers,
			BinaryOutputStream output) throws IOException {
		int mods = 0;

		for (Modifier x : modifiers) {
			if (x instanceof Modifier.Base) {
				mods |= ((Modifier.Base) x).modifier();
			}
		}

		output.write_u2(mods);
	}	
}