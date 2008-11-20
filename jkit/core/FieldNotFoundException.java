package jkit.core;

/**
 * This exception is thrown when trying to resolve a field using
 * ClassTable.resolveField(). It indicates that no appropriate field could be
 * found.
 * 
 * @author djp
 * 
 */
public class FieldNotFoundException extends Exception {
	public FieldNotFoundException(String field, String owner) {
		super("Field \"" + owner + "."  + field + "\" not found.");
	}
	
	public static final long serialVersionUID = 1l;
}
