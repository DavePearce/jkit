package jkit.compiler;

import java.util.*;
import jkit.jil.*;

/**
 * A compiler is an object responsible for compiling a particular source file.
 * 
 * @author djp
 * 
 */
public interface Compiler {
	
	/**
	 * Get the list of types defined in the source file being compiled.
	 * @return
	 */
	public List<Type> types();
	
	
}
