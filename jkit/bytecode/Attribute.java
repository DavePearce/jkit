package jkit.bytecode;

import java.util.*;
import java.io.*;

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
			Map<Constant.Info, Integer> constantPool) throws IOException;
	
	/**
	 * When this method is called, the attribute must add all items that it
	 * needs to the constant pool.
	 * 
	 * @param constantPool
	 */
	public void addPoolItems(Set<Constant.Info> constantPool);
	
	/**
	 * This method is used to print the contents of the attribute in
	 * human-readable form, similar to that produced by "javap".
	 * 
	 * @param output
	 * @param constantPool
	 * @throws IOException
	 */
	public void print(PrintWriter output, Map<Constant.Info, Integer> constantPool)
			throws IOException;
}
