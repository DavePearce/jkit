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

import java.util.*;
import java.io.*;
import jkit.compiler.ClassLoader;

public interface Attribute {
	public String name();
	
	/**
	 * This method requires the attribute to write itself to the binary stream.
	 * 
	 * @param writer
	 * @returns the number of bytes written.
	 * @throws IOException
	 */
	public void write(BinaryOutputStream writer,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader)
			throws IOException;
	
	/**
	 * When this method is called, the attribute must add all items that it
	 * needs to the constant pool.
	 * 
	 * @param constantPool
	 */
	public void addPoolItems(Set<Constant.Info> constantPool, ClassLoader loader);
	
	/**
	 * This method is used to print the contents of the attribute in
	 * human-readable form, similar to that produced by "javap".
	 * 
	 * @param output
	 * @param constantPool
	 * @throws IOException
	 */
	public void print(PrintWriter output,
			Map<Constant.Info, Integer> constantPool, ClassLoader loader)
			throws IOException;
	
	/**
	 * Class for representing unknown attributes
	 * 
	 * @author djp	 
	 */
	public static class Unknown implements Attribute {
		private byte[] bytes;
		private String name;
		
		public Unknown(String n, byte[] bs) { 
			bytes = bs;
			name = n;
		}
		
		public String name() {
			return name;
		}
		
		public void addPoolItems(Set<Constant.Info> constantPool, ClassLoader loader) {
			// this seems a little broken, but what can we do?
		}
		
		public void write(BinaryOutputStream writer,
				Map<Constant.Info, Integer> constantPool, ClassLoader loader) throws IOException {
			writer.write(bytes);
		}
		
		public void print(PrintWriter output,
				Map<Constant.Info, Integer> constantPool, ClassLoader loader) throws IOException {
			output.println("  Unknown: " + name);
			output.println("   Size: " + bytes.length);
		}
	}

}
