package wone.solver;

import java.util.*;
import wone.lang.*;

/**
 * <p>
 * Congruence closure is about applying the theory of inequality to look for
 * contradictions. For example:
 * </p>
 * 
 * <pre>
 * (1) x == y &amp;&amp; y == z &amp;&amp; x != z
 * </pre>
 * 
 * <p>
 * This example is unsatisfiable as, by [transitive] closure, we can infer
 * that <code>x==z</code> but this contradicts the assertion that
 * <code>x!=z</code>. 
 * 
 * @author djp
 * 
 */
public class CongruenceClosure {
	
	public boolean checkUnsatisfiable(Collection<Literal> literals) {
		return false;
	}	
}
