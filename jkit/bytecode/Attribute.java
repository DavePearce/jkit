package jkit.bytecode;

import java.util.*;
import java.io.*;

public abstract class Attribute {
	public abstract String name();

	/**
	 * This method requires the attribute to write itself to the binary stream.
	 * 
	 * @param writer
	 * @returns the number of bytes written.
	 * @throws IOException
	 */
	public abstract void write(BinaryWriter writer,
			Map<Constant.Info, Integer> constantPool) throws IOException;
}
