package jkit.compiler;

import java.io.*;
import java.util.*;
import jkit.jil.Clazz;

/**
 * A compiler is an object responsible for compiling a particular source file.
 * 
 * @author djp
 * 
 */
public interface Compiler {
	
	/**
     * Compile the source file to produce a list of one or more jil classes.
     * 
     * @return
     */
	public List<Clazz> compile(String srcFile) throws IOException,SyntaxError;
}
