package whiley.ast.stmts;

import java.util.*;

import whiley.ast.Function;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;
import whiley.ast.exprs.Condition;

/**
 * This represents a "if c then l_1 else l_2" statement.
 */
public final class IfElse extends SyntacticElementImpl implements Stmt {
	private final Condition cond;
	private final List<Stmt> trueBranch;
	private final List<Stmt> falseBranch;

	public IfElse(Condition cond, List<Stmt> trueBranch,
			List<Stmt> falseBranch, Attribute... attributes) { 
		super(attributes);
		this.cond = cond;
		this.trueBranch = trueBranch;
		this.falseBranch = falseBranch;
	}

	/**
	 * Get the condition for this If statement
	 */
	public Condition getCondition() { 
		return cond; 
	}

	/**
	 * Get true label to be taken when the condition evaluates to
	 * true.
	 */
	public List<Stmt> getTrueBranch() { return trueBranch; }

	/**
	 * Get true label to be taken when the condition evaluates to
	 * false.
	 */
	public List<Stmt> getFalseBranch() { return falseBranch; }

	public void bind(Map<String,Function> fmap) {
		for(Stmt s : trueBranch) {
			s.bind(fmap);
		}
		
		if(falseBranch != null) {
			for(Stmt s : falseBranch) {
				s.bind(fmap);
			}
		}
	}	
}
