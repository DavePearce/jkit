package jkit.compiler;

import java.io.*;
import java.util.*;

import jkit.jil.tree.JilClass;

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
	public List<? extends Clazz> compile(File srcFile) throws IOException,SyntaxError;
	
	/**
	 * The purpose of this method is to indicate that a source file is currently
	 * being compiled. This is crucial for the ClassLoader, since it prevents
	 * infinite recursive loops where the classloader attempts to compile the
	 * class in question, but it's already being compiled.
	 */
	public boolean isCompiling(File srcFile);
	
	/**
	 * This method is to permit log messages from other components, such as the
	 * class loader;
	 */
	public void logMessage(String msg);
	
	/**
	 * This method is to permit log messages from other components, such as the
	 * class loader;
	 */
	public void logTimedMessage(String msg, long time);
}
