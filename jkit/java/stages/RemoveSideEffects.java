package jkit.java.stages;

/**
 * <p>
 * The aim of this stage is to eliminate all side-effects from expressions. For
 * example, the following code:
 * </p>
 * 
 * <pre>
 * int x = 0;
 * int y = ++x + --x;
 * </pre>
 * 
 * <p>
 * will be translated into this:
 * </p>
 * 
 * <pre>
 * int x = 0;
 * x = x + 1;
 * int __jkit_tmp_1 = x;
 * x = x - 1;
 * int y = __jkit_tmp_1 + x;
 * </pre>
 * 
 * <p>
 * Note, method calls are not eliminate from expressions, even though they may
 * well have side-effects.
 * </p>
 * 
 * @author djp
 * 
 */

public class RemoveSideEffects {

}
