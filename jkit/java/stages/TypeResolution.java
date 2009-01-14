package jkit.java.stages;

/**
 * This Class goes through all of the types that have been declared in the
 * source file, and resolves them to fully qualified types. For example,
 * consider this code:
 * 
 * <pre>
 * import java.util.*;
 * 
 * public class Test extends Vector {
 * 	public static void main(String[] args) {
 *       ... 
 *      }
 * }
 * </pre>
 * 
 * After type resolution, we will have resolved the following types:
 * 
 * <pre>
 *    Vector -&gt; java.util.Vector
 *    String -&gt; java.lang.String
 * </pre>
 * 
 * Thus, in principle, we could use this information to eliminate any import
 * statements (although type resolution by itself does not do this).
 * 
 * This operation must be performed indenpendently from type propagation, since
 * we need to determine the full skeleton for all classes being compiled before
 * we can do any propagation.  
 */
public class TypeResolution {

}
