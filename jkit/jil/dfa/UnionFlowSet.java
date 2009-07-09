package jkit.util.dfa;

import java.util.*;

/**
 * A union flow set implements the flow set as a simple set of elements, using
 * set union to join them.
 * 
 * This implementation extends java.util.HashSet and, hence, element types must
 * provide appropriate hashcode() methods.
 * 
 * @author djp
 * 
 */
public class UnionFlowSet<T> extends HashSet<T> implements FlowSet, Set<T>  {
	public UnionFlowSet() {}
	public UnionFlowSet(Collection<? extends T> src) { super(src); }
	
	public boolean join(FlowSet _fs) {
		if(_fs instanceof UnionFlowSet) {
			// there is probably a way to get rid of the unsafe cast here.
			UnionFlowSet fs = (UnionFlowSet) _fs;
			return this.addAll(fs);
		}
		return false;
	}
}
