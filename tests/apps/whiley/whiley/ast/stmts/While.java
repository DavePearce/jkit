package whiley.ast.stmts;

import java.util.*;

import whiley.ast.Function;
import whiley.ast.attrs.*;
import whiley.ast.exprs.*;

public final class While extends SyntacticElementImpl implements Stmt {
	private Condition condition;
	private Condition invariant;
	private ArrayList<Stmt> statements;
	
	public While(Condition condition, Condition invariant, List<Stmt> body, Attribute... attributes) {
		super(attributes);
		this.condition = condition;
		this.invariant = invariant;
		this.statements = new ArrayList<Stmt>(body);
	}
	
	public List<Stmt> body() {
		return statements;
	}
	
	public Condition condition() {
		return condition;
	}
	
	public Condition invariant() {
		return invariant;
	}
	
	public void bind(Map<String,Function> fmap) {
		for(Stmt s : statements) {
			s.bind(fmap);
		}
	}
}
