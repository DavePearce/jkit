package whiley.ast.stmts;

import java.util.*;
import whiley.ast.*;
import whiley.ast.attrs.SyntacticElement;

public interface Stmt extends SyntacticElement {	
    /**
	 * If this statement represents a function invocation, then bind it to the
	 * actual function that it's going to invoke. Likewise, for any expressions
	 * used in this statement.
	 * 
	 * @param fmap -
	 *            Binds the function name to the function object.
	 */
    public void bind(Map<String,Function> fmap);
}
