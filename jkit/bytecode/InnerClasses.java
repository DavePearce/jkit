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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jkit.compiler.ClassLoader;
import jkit.jil.tree.JilClass;
import jkit.jil.tree.Modifier;
import jkit.jil.tree.Type;
import jkit.util.Triple;

public class InnerClasses implements Attribute {
	protected List<Triple<Type.Clazz,Type.Clazz,List<Modifier>>> inners;	
	protected Type.Clazz type;
	
	public String name() {
		return "InnerClasses";
	}
	
	/**
	 * Create an InnerClasses attribute (see JLS Section 4.7.5).
	 * 
	 * @param type - the type of the class containing this attribute.
	 * @param inners - the types and modifiers for all classes contained in this class.
	 * @param outers-  the types and modifiers for all classes containing this class.
	 */
	public InnerClasses(Type.Clazz type,
			List<Triple<Type.Clazz, Type.Clazz, List<Modifier>>> inners) {
		this.type = type;
		this.inners = inners;		
	}
	
	protected List<Triple<Type.Clazz,Type.Clazz,List<Modifier>>> inners() {
		return inners;
	}
	
	protected Type.Clazz type() {
		return type;
	}
	
	/**
	 * When this method is called, the attribute must add all items that it
	 * needs to the constant pool.
	 * 
	 * @param constantPool
	 */
	public void addPoolItems(Set<Constant.Info> constantPool, ClassLoader loader) {
		Constant.addPoolItem(new Constant.Utf8("InnerClasses"), constantPool);
		for(Triple<Type.Clazz,Type.Clazz,List<Modifier>> i : inners) {			
			if(i.first() != null) {
				Constant.addPoolItem(Constant.buildClass(i.first()),constantPool);
			}
			if(i.second() != null) {
				Constant.addPoolItem(Constant.buildClass(i.second()),constantPool);
			}
			String name = i.second().lastComponent().first();
			Constant.addPoolItem(new Constant.Utf8(name),constantPool);										
		}		
	}
	
	public void print(PrintWriter output, Map<Constant.Info, Integer> constantPool, ClassLoader loader) {
		output.println("  InnerClasses:");
		
		for(Triple<Type.Clazz,Type.Clazz,List<Modifier>> i : inners) {
			String name = i.second().lastComponent().first();
			int nameIndex = constantPool.get(new Constant.Utf8(name));
			int outerIndex = 0;
			if(i.first() != null) {
				outerIndex = constantPool.get(Constant.buildClass(i.first()));
			}			
			int innerIndex = 0;
			if(i.second() != null) {
				innerIndex = constantPool.get(Constant.buildClass(i.second()));
			}
			output.print("   ");			
			output.print(nameIndex + " (");
			BytecodeFileWriter.writeModifiers(i.third(),output);					
			output.println(") = " + innerIndex + " of " + outerIndex);
		}			
	}
	
	/**
     * Write attribute detailing what direct inner classes there are for this
     * class, or what inner class this class is in.
     * 
     * @param clazz
     * @param constantPool
     */
	public void write(BinaryOutputStream output,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader)
			throws IOException {
		output.write_u2(constantPool.get(new Constant.Utf8("InnerClasses")));
		
		int ninners = inners.size();
		
		output.write_u4(2 + (8 * ninners));
		output.write_u2(ninners);
		
		for(Triple<Type.Clazz,Type.Clazz,List<Modifier>> i : inners) {
			if(i.second() == null) {								
				output.write_u2(0);
			} else {
				output.write_u2(constantPool.get(Constant.buildClass(i.second())));
			}
			if(i.first() == null) {
				output.write_u2(0);
			} else {
				output.write_u2(constantPool.get(Constant.buildClass(i.first())));
			}
			String name = i.second().lastComponent().first();
			output.write_u2(constantPool.get(new Constant.Utf8(name)));			
			writeInnerModifiers(i.third(),output);			
		}		
	}
	
	private static void writeInnerModifiers(List<Modifier> modifiers,
			BinaryOutputStream output)
			throws IOException {
		int mods = 0;
		for (Modifier m : modifiers) {
			if (m instanceof Modifier.Public) {
				mods |= ClassFileReader.ACC_PUBLIC;
			} else if (m instanceof Modifier.Private) {
				mods |= ClassFileReader.ACC_PRIVATE;
			} else if (m instanceof Modifier.Protected) {
				mods |= ClassFileReader.ACC_PROTECTED;
			} else if (m instanceof Modifier.Static) {
				mods |= ClassFileReader.ACC_STATIC;
			} else if (m instanceof Modifier.Final) {
				mods |= ClassFileReader.ACC_FINAL;
			} else if (m instanceof Modifier.Interface) {
				mods |= ClassFileReader.ACC_INTERFACE;
			} else if (m instanceof Modifier.Abstract) {
				mods |= ClassFileReader.ACC_ABSTRACT;
			} else if (m instanceof Modifier.Synthetic) {
				mods |= ClassFileReader.ACC_SYNTHETIC;
			} else if (m instanceof Modifier.Annotation) {
				mods |= ClassFileReader.ACC_ANNOTATION;
			} else if (m instanceof Modifier.Enum) {
				mods |= ClassFileReader.ACC_ENUM;
			} 
		}

		output.write_u2(mods);
}
	
}
