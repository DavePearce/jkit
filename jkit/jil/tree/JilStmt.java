package jkit.jil.tree;

import java.util.*;

public interface JilStmt extends SyntacticElement {
	
	/**
	 * An assignment statement.
	 * 
	 * @author djp
	 *
	 */
	public static final class Assign extends SyntacticElementImpl implements
			JilStmt {
		private JilExpr lhs, rhs;

		public Assign(JilExpr lhs, JilExpr rhs,
				Attribute... attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public Assign(JilExpr lhs, JilExpr rhs,
				List<Attribute> attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
		}
		
		public JilExpr lhs() {
			return lhs;
		}

		public JilExpr rhs() {
			return rhs;
		}
		
		public void setLhs(JilExpr lhs) {
			this.lhs = lhs;
		}

		public void setRhs(JilExpr rhs) {
			this.rhs = rhs;
		}
	}
	
	/**
	 * A return statement.
	 */
	public static final class Return extends SyntacticElementImpl implements JilStmt {
		private JilExpr expr;

		public Return(JilExpr expr, Attribute... attributes) {
			super(attributes);
			this.expr = expr;
		}
		
		public Return(JilExpr expr, List<Attribute> attributes) {
			super(attributes);
			this.expr = expr;
		}

		public JilExpr expr() {
			return expr;
		}
		
		public void setExpr(JilExpr expr) {
			this.expr = expr;
		}
	}
	
	/**
	 * A throw statement.
	 * 
	 * @author djp
	 *
	 */
	public static final class Throw extends SyntacticElementImpl implements JilStmt {
		private JilExpr expr;

		public Throw(JilExpr expr, Attribute... attributes) {
			super(attributes);
			this.expr = expr;
		}

		public Throw(JilExpr expr, List<Attribute> attributes) {
			super(attributes);
			this.expr = expr;
		}
		
		public JilExpr expr() {
			return expr;
		}
		
		public void setExpr(JilExpr expr) {
			this.expr = expr;
		}
	}
	
	/**
	 * An unconditional goto statement.
	 * 
	 * @author djp
	 *
	 */
	public static final class Goto extends SyntacticElementImpl implements JilStmt {
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
	public static final class IfGoto extends SyntacticElementImpl implements JilStmt {
		private JilExpr condition;
		private String label;

		public IfGoto(JilExpr condition, String label, Attribute... attributes) {
			super(attributes);
			this.condition = condition;
			this.label = label;
		}

		public IfGoto(JilExpr condition, String label, List<Attribute> attributes) {
			super(attributes);
			this.condition = condition;
			this.label = label;
		}
		
		public JilExpr condition() {
			return condition;
		}
		
		public void setCondition(JilExpr expr) {
			this.condition = expr;
		}
		
		public String label() {
			return label;
		}
		
		public void setLabel(String label) {
			this.label = label;
		}
	}
	
	public static final class Label extends SyntacticElementImpl implements JilStmt {
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
	
	public static final class Nop extends SyntacticElementImpl implements JilStmt {
	}
	
	public static final class Lock extends SyntacticElementImpl implements JilStmt {
		private JilExpr expr;

		public Lock(JilExpr expr, Attribute... attributes) {
			super(attributes);
			this.expr = expr;
		}

		public Lock(JilExpr expr, List<Attribute> attributes) {
			super(attributes);
			this.expr = expr;
		}
		
		public JilExpr expr() {
			return expr;
		}
		
		public void setExpr(JilExpr expr) {
			this.expr = expr;
		}
	}
	
	public static final class Unlock extends SyntacticElementImpl implements JilStmt {
		private JilExpr expr;

		public Unlock(JilExpr expr, Attribute... attributes) {
			super(attributes);
			this.expr = expr;
		}

		public Unlock(JilExpr expr, List<Attribute> attributes) {
			super(attributes);
			this.expr = expr;
		}
		
		public JilExpr expr() {
			return expr;
		}
		
		public void setExpr(JilExpr expr) {
			this.expr = expr;
		}
	}
}
