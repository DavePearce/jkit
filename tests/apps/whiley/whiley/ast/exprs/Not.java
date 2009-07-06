package whiley.ast.exprs;

import java.util.Map;

import whiley.ast.Function;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;

public class Not extends SyntacticElementImpl implements Condition {
	private Condition condition;

	public Not(Condition c, Attribute... attributes) {
		super(attributes);
		this.condition = c;
	}

	public Condition getCondition() {
		return condition;
	}

	public void bind(Map<String,Function> fmap) {
		condition.bind(fmap);
	}

	public String toString() {
		if(condition instanceof BinOp) {
			return "!(" + condition.toString() + ")";
		} else {
			return "!" + condition.toString();
		}
	}
}
