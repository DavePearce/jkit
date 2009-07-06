package whiley.ast.attrs;

import java.util.List;


/**
 * A Syntactic Element represents any part of the file for which is relevant
 * to the syntactic structure of the file, and in particular parts we may
 * wish to add information too (e.g. line numbers, types, etc).
 * 
 * @author djp
 * 
 */
public interface SyntacticElement {
	/**
     * Get the list of attributes associated with this syntactice element.
     * 
     * @return
     */
	public List<Attribute> attributes();
	
	/**
     * Get the first attribute of the given class type. This is useful
     * short-hand.
     * 
     * @param c
     * @return
     */
	public Attribute attribute(Class c);
}
