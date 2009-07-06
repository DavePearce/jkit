package whiley.ast.attrs;

import wone.lang.*;

/**
 * A precondition attribute is used to store the determined precondition which
 * holds before this statement.
 * 
 * @author djp
 * 
 */
public class PostConditionAttr implements Attribute {
	private Formula postCondition;

	public PostConditionAttr(Formula postCondition) {
		this.postCondition = postCondition;
	}

	public Formula postCondition() {
		return postCondition;
	}
}
