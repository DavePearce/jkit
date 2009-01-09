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
 *    ... 
 *   }
 * }
 * </pre>
 * 
 * After type resolution, this code will look like the following:
 * 
 * <pre>
 * public class Test extends java.util.Vector {
 * 	public static void main(java.lang.String[] args) {
 *    ... 
 *   }
 * }
 * </pre>
 * 
 * Thus, we can see that the import statements are no longer required since the
 * full package for everytype has been provided.
 * 
 * @author djp
 * 
 */
public class TypeResolution {

}
