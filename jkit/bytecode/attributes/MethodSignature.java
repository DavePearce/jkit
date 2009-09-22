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
import java.util.*;

import jkit.bytecode.BytecodeAttribute;
import jkit.bytecode.BinaryOutputStream;
import jkit.bytecode.ClassFile;
import jkit.bytecode.Constant;
import jkit.bytecode.Constant.Info;
import jkit.bytecode.Constant.Utf8;
import jkit.compiler.ClassLoader;
import jkit.compiler.*;
import jkit.jil.tree.*;

/**
 * This is for method and/or field signatures
 * @author djp 
 */
public class MethodSignature implements BytecodeAttribute {
	protected Type.Function type;
	
	public MethodSignature(Type.Function type) {
		this.type = type;
	}
	
	public String name() {
		return "Signature";
	}
	
	public Type.Function type() {
		return type;
	}
	
	public void write(BinaryOutputStream writer,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader)
			throws IOException {
		writer.write_u2(constantPool.get(new Constant.Utf8("Signature")));
		writer.write_u4(2);
		writer.write_u2(constantPool.get(new Constant.Utf8(descriptor(loader))));		
	}
	
	public void addPoolItems(Set<Constant.Info> constantPool, ClassLoader loader) {
		Constant.addPoolItem(
				new Constant.Utf8(descriptor(loader)),
				constantPool);
	}
	
	public void print(PrintWriter output,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader) {
		output.println("Signature: "
				+ constantPool.get(new Constant.Utf8(descriptor(loader))));
	}
	
	private String descriptor(ClassLoader loader) {		

		String r = "";

		List<Type.Variable> typeArgs = type.typeArguments();
		if(!typeArgs.isEmpty()) {				
			r += "<";
			for(Type.Variable v : typeArgs) {
				Type lb = v.lowerBound();
								
				r += v.variable() + ":";
			
				if(lb != null) {

					// The following check is needed to deal with the case
					// where the type bounds are only interfaces. In this
					// case, there must be an extra colon indicating the
					// absence of super class. It's actually really annoying
					// since it couples this code with ClassTable ... grrr.

					try {
						Clazz tmp = loader.loadClass((Type.Clazz) lb);
						if (tmp.isInterface()) {
							r += ":";
						}
						r += ClassFile.descriptor(lb, true);
					} catch (ClassNotFoundException ce) {
						throw new RuntimeException("Type bound " + lb
								+ " not found");									
					}				
				}
			}
			r += ">";
		}

		r += "(";

		for (Type pt : type.parameterTypes()) {				
			r += ClassFile.descriptor(pt,true);
		}

		r = r + ")" + ClassFile.descriptor(type.returnType(),true);
		return r;		
	}
}
