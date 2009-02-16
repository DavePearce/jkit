package jkit.compiler;

/**
 * This exception is thrown when trying to resolve a method using
 * ClassTable.resolveMethod(). It indicates that no appropriate method could be
 * found,
 * 
 * @author djp
 * 
 */
public class MethodNotFoundException extends Exception {
	public MethodNotFoundException(String method, String owner) {
		super("Method \"" + owner + "."  + method + "\" not found.");
	}
	
	public static final long serialVersionUID = 1l;
}
