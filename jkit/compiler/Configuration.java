package jkit.compiler;

import java.io.*;
import java.util.*;
import jkit.jil.*;

/**
 * A Configuration is essentially a factory for compilers. A compiler is created
 * for each source file, and is used to compile that source file.
 * 
 * @author djp
 * 
 */
public interface Configuration {
	
	/**
     * Get the list of supported extensions. E.g. "java". Note that this list
     * will be searched according to its natural ordering. This is important in
     * compilers which support multiple source types. For example, suppose we
     * have a compiler which supports both "mocha" and "java" extensions. Then,
     * if we see a file named e.g. "x.mocha", and another named "x.java" then
     * the one which actually gets compiled will be the first match in this
     * list.
     * 
     * @return
     */
	public List<String> extensions();
	
	
	/** 
	 * Create a compiler from a given (absolute) filename.
	 * 
	 * @param filename
	 * @return
	 */
	public Compiler createCompiler(String filename);
	
	/**
     * Create a compiler from an input stream. The filename argument provided
     * (if non-null) will be stored in the binary produced.
     * 
     * @param filename
     * @return
     */
	public Compiler createCompiler(InputStream in, String filename);
}
