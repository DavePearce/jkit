package whiley.ast.exprs;

import java.util.*;

import whiley.ast.Function;
import whiley.ast.attrs.SyntacticElement;

public interface Condition extends Expression, SyntacticElement {
    /**
	 * If this expression represents a function invocation, then bind it to the
	 * actual function that it's going to invoke.
	 * 
	 * @param fmap -
	 *            Binds the function name to the function object.
	 */
    public void bind(Map<String,Function> fmap);

}
