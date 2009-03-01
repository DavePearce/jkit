package jkit.jil.tree;

import java.util.*;

public interface Stmt extends SyntacticElement {
	
	/**
	 * An assignment statement.
	 * 
	 * @author djp
	 *
	 */
	public static final class Assign extends SyntacticElementImpl implements
			Stmt {
		private Expr lhs, rhs;

		public Assign(Expr lhs, Expr rhs,
				Attribute... attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public Assign(Expr lhs, Expr rhs,
				List<Attribute> attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
		}
		
		public Expr lhs() {
			return lhs;
		}

		public Expr rhs() {
			return rhs;
		}
		
		public void setLhs(Expr lhs) {
			this.lhs = lhs;
		}

		public void setRhs(Expr rhs) {
			this.rhs = rhs;
		}
	}
	
	/**
	 * A return statement.
	 */
	public static final class Return extends SyntacticElementImpl implements Stmt {
		private Expr expr;

		public Return(Expr expr, Attribute... attributes) {
			super(attributes);
			this.expr = expr;
		}
		
		public Return(Expr expr, List<Attribute> attributes) {
			super(attributes);
			this.expr = expr;
		}

		public Expr expr() {
			return expr;
		}
		
		public void setExpr(Expr expr) {
			this.expr = expr;
		}
	}
	
	/**
	 * A throw statement.
	 * 
	 * @author djp
	 *
	 */
	public static final class Throw extends SyntacticElementImpl implements Stmt {
		private Expr expr;

		public Throw(Expr expr, Attribute... attributes) {
			super(attributes);
			this.expr = expr;
		}

		public Throw(Expr expr, List<Attribute> attributes) {
			super(attributes);
			this.expr = expr;
		}
		
		public Expr expr() {
			return expr;
		}
		
		public void setExpr(Expr expr) {
			this.expr = expr;
		}
	}
	
	/**
	 * An unconditional goto statement.
	 * 
	 * @author djp
	 *
	 */
	public static final class Goto extends SyntacticElementImpl implements Stmt {
		private String label;

		public Goto(String label, Attribute... attributes) {
			super(attributes);
			this.label = label;
		}

		public Goto(String label, List<Attribute> attributes) {
			super(attributes);
			this.label = label;
		}
		
		public String label() {
			return label;
		}
		
		public void setLabel(String label) {
			this.label = label;
		}
	}
	
	/**
	 * A conditional goto statement.
	 * 
	 * @author djp
	 *
	 */
	public static final class IfGoto extends SyntacticElementImpl implements Stmt {
		private Expr condition;
		private String label;

		public IfGoto(Expr condition, String label, Attribute... attributes) {
			super(attributes);
			this.condition = condition;
			this.label = label;
		}

		public IfGoto(Expr condition, String label, List<Attribute> attributes) {
			super(attributes);
			this.condition = condition;
			this.label = label;
		}
		
		public Expr condition() {
			return condition;
		}
		
		public void setCondition(Expr expr) {
			this.condition = expr;
		}
		
		public String label() {
			return label;
		}
		
		public void setLabel(String label) {
			this.label = label;
		}
	}
	
	public static final class Label extends SyntacticElementImpl implements Stmt {
		private String label;

		public Label(String label, Attribute... attributes) {
			super(attributes);
			this.label = label;
		}

		public Label(String label, List<Attribute> attributes) {
			super(attributes);
			this.label = label;
		}
		
		public String label() {
			return label;
		}
		
		public void setLabel(String label) {
			this.label = label;
		}
	}
}
