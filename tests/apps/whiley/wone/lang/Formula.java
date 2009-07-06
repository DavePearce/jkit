package wone.lang;

import java.util.Map;

public interface Formula {
	
	/**
	 * <p>
	 * Check whether this formula is trivially true or not.
	 * </p>
	 * 
	 * @return
	 */
	public boolean isTrue();
	
	/**
	 * <p>
	 * Check whether this formula is trivially false or not.
	 * </p>
	 * 
	 * @return
	 */
	public boolean isFalse();
	
	/**
	 * <p>
	 * Compute the logical not of this formula.
	 * </p>
	 * @return
	 */
	public Formula not();
	
	/**
	 * <p>
	 * Compute the logical and of this formula.
	 * </p>
	 * @return
	 */
	public Formula and(Formula f);
	
	/**
	 * <p>
	 * Compute the logical or of this formula.
	 * </p>
	 * @return
	 */
	public Formula or(Formula f);
	
	/**
	 * This method substitutes all variable names for names given in the
	 * binding. If no binding is given for a variable, then it retains its
	 * original name.
	 * 
	 * @param binding
	 * @return
	 */
	public Formula substitute(Map<String,String> binding);

}
