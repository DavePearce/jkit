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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import jkit.bytecode.BytecodeAttribute;
import jkit.bytecode.BinaryOutputStream;
import jkit.bytecode.Constant;
import jkit.bytecode.Constant.Info;
import jkit.bytecode.Constant.Utf8;
import jkit.compiler.ClassLoader;
import jkit.jil.tree.Type;

public class Exceptions implements BytecodeAttribute {
	protected List<Type.Clazz> exceptions;
	
	public Exceptions(List<Type.Clazz> exceptions) {
		this.exceptions = exceptions;
	}
	
	public String name() {
		return "Exceptions";
	}
	
	public List<Type.Clazz> exceptions() {
		return exceptions;
	}
	
	/**
	 * This method requires the attribute to write itself to the binary stream.
	 * 
	 * @param writer
	 * @returns the number of bytes written.
	 * @throws IOException
	 */
	public void write(BinaryOutputStream writer,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader) throws IOException {
		
		writer.write_u2(constantPool.get(new Constant.Utf8("Exceptions")));
		writer.write_u4(2 + (2 * exceptions.size()));
		writer.write_u2(exceptions.size());
		for (Type.Clazz e : exceptions) {
			writer.write_u2(constantPool.get(Constant.buildClass(e)));
		}
	}
	
	/**
	 * When this method is called, the attribute must add all items that it
	 * needs to the constant pool.
	 * 
	 * @param constantPool
	 */
	public void addPoolItems(Set<Constant.Info> constantPool, ClassLoader loader) {
		Constant.addPoolItem(new Constant.Utf8("Exceptions"), constantPool);
		for (Type.Clazz e : exceptions) {
			Constant.addPoolItem(Constant.buildClass(e), constantPool);
		}
	}
	
	public void print(PrintWriter output, Map<Constant.Info, Integer> constantPool, ClassLoader loader) {
		output.println("  Exceptions:");
		boolean firstTime = true; 
		output.print("   ");
		for(Type.Clazz e : exceptions) {
			if(!firstTime) {
				output.print(", ");
			}
			firstTime=false;
			output.print(e);
		}
		output.println();
	}	
}
