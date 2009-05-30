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
