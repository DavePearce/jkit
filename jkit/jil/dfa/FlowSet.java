package jkit.util.dfa;

public interface FlowSet {

	/**
	 * FlowSets must be cloneable to facilitate multiple flows of execution
	 * from conditionals
	 * 
	 * @return A Clone of the current FlowSet
	 */
	public Object clone();
	
	/**
	 * Computes the least upper bound of this flowset and that provided.
	 * 
	 * @param s Another FlowSet to join with this
	 * @return true if this FlowSet has changed due to the computation, false otherwise
	 */
	public boolean join(FlowSet s);
}
