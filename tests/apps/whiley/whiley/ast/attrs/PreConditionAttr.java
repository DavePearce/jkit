package whiley.ast.attrs;

import wone.lang.*;

/**
 * A precondition attribute is used to store the determined precondition which
 * holds before this statement.
 * 
 * @author djp
 * 
 */
public class PreConditionAttr implements Attribute {
	private Formula preCondition;

	public PreConditionAttr(Formula preCondition) {
		this.preCondition = preCondition;
	}

	public Formula preCondition() {
		return preCondition;
	}
}
