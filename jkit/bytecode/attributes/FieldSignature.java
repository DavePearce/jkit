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

package jkit.bytecode.attributes;

import java.io.*;
import java.util.Map;
import java.util.Set;

import jkit.bytecode.BytecodeAttribute;
import jkit.bytecode.BinaryOutputStream;
import jkit.bytecode.ClassFile;
import jkit.bytecode.Constant;
import jkit.bytecode.Constant.Info;
import jkit.bytecode.Constant.Utf8;
import jkit.compiler.ClassLoader;
import jkit.jil.tree.*;

/**
 * This is for method and/or field signatures
 * @author djp 
 */
public class FieldSignature implements BytecodeAttribute {
	protected Type type;
	
	public FieldSignature(Type type) {
		this.type = type;
	}
	
	public String name() {
		return "Signature";
	}
	
	public Type type() {
		return type;
	}
	
	public void write(BinaryOutputStream writer,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader) throws IOException {
		writer.write_u2(constantPool.get(new Constant.Utf8("Signature")));
		writer.write_u4(2);
		writer.write_u2(constantPool.get(new Constant.Utf8(ClassFile
				.descriptor(type, true))));
	}
	
	public void addPoolItems(Set<Constant.Info> constantPool, ClassLoader loader) {
		Constant.addPoolItem(
				new Constant.Utf8(ClassFile.descriptor(type, true)),
				constantPool);
	}
	
	public void print(PrintWriter output,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader) {
		output.println("Signature: "
				+ constantPool.get(new Constant.Utf8(ClassFile.descriptor(type,
						true))));
	}
}
