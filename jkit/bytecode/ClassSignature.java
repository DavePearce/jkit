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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jkit.compiler.Clazz;
import jkit.compiler.ClassLoader;
import jkit.jil.tree.*;
import jkit.util.Pair;

public class ClassSignature implements Attribute {
	protected Type.Clazz type;
	protected Type.Clazz superClazz;
	protected List<Type.Clazz> interfaces;
	
	public ClassSignature(Type.Clazz type, Type.Clazz superClazz,
			List<Type.Clazz> interfaces) {
		this.type = type;
		this.superClazz = superClazz;
		this.interfaces = interfaces;
	}
	
	public String name() {
		return "Signature";
	}
	
	public Type.Clazz type() {
		return type;
	}
	
	public Type.Clazz superClass() {
		return superClazz;
	}
	
	public List<Type.Clazz> interfaces() {
		return interfaces;
	}
	
	public void write(BinaryOutputStream writer,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader)
			throws IOException {
		writer.write_u2(constantPool.get(new Constant.Utf8("Signature")));
		writer.write_u4(2);
		writer.write_u2(constantPool.get(new Constant.Utf8(
				classSignature(loader))));
	}
	
	public void addPoolItems(Set<Constant.Info> constantPool, ClassLoader loader) {
		Constant.addPoolItem(new Constant.Utf8(classSignature(loader)),
				constantPool);
	}
	
	/**
	 * This method constructors a class signature string from a clazz.
	 * 
	 * @param clazz
	 * @return
	 */
	public String classSignature(ClassLoader loader) {
		String desc = "";
		
		List<Pair<String,List<Type.Reference>>> classes = type.components();
		if(classes.get(classes.size()-1).second().size() > 0) { 
			desc += "<"; 
			for(Type t : classes.get(classes.size()-1).second()) {
				if(t instanceof Type.Variable) {
					Type.Variable tv = (Type.Variable) t;
					desc += tv.variable() + ":";
					Type lb = tv.lowerBound();
					// The following check is needed to deal with the case
					// where the type bounds are only interfaces. In this
					// case, there must be an extra colon indicating the
					// absence of super class. It's actually really annoying
					// since it couples this code with ClassTable ... grrr.

					try {
						Clazz tmp = loader.loadClass((Type.Clazz) lb);
						if (tmp.isInterface()) {
							desc += ":";
						}
						desc += ClassFile.descriptor(lb, true);
					} catch (ClassNotFoundException ce) {
						throw new RuntimeException("Type bound " + lb
								+ " not found");									
					}
				} else {
					throw new RuntimeException("Type Variable required in Class Signature!");
				}
			}
			desc += ">";
		}
		if (superClazz != null) {
			desc += ClassFile.descriptor(superClazz, true);
		}
		for (Type t : interfaces) {
			desc += ClassFile.descriptor(t, true);
		}
		return desc;
	}
	
	public void print(PrintWriter output,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader) {
		output.println("Signature: "
				+ constantPool.get(new Constant.Utf8(classSignature(loader))));
	}
}
