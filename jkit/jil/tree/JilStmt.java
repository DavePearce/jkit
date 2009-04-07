package jkit.jil.tree;

import java.util.*;
import jkit.util.*;

public interface JilStmt extends SyntacticElement {
	
	/**
	 * This method returns the exceptional branches associated with this
	 * statement. The exceptions should be traversed for matches in the order
	 * returned.
	 * 
	 * @return
	 */
	public List<Pair<Type.Clazz,String>> exceptions();
	
	public static class AbstractStmt extends SyntacticElementImpl implements
			JilStmt,SyntacticElement {
		protected final ArrayList<Pair<Type.Clazz, String>> exceptions = new ArrayList();

		public AbstractStmt(Attribute... attributes) {
			super(attributes);
		}
		
		public AbstractStmt(List<Attribute> attributes) {
			super(attributes);
		}
		
		public List<Pair<Type.Clazz, String>> exceptions() {
			return exceptions;
		}
	}
	
	/**
	 * An assignment statement.
	 * 
	 * @author djp
	 *
	 */
	public static final class Assign extends AbstractStmt {
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
	public static final class Return extends AbstractStmt {
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
	public static final class Throw extends AbstractStmt {
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
	public static final class Goto extends AbstractStmt {
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
	public static final class IfGoto extends AbstractStmt {
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
	
	public static final class Label extends AbstractStmt {
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
	
	public static final class Nop extends AbstractStmt {
	}
	
	public static final class Lock extends AbstractStmt {
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
	
	public static final class Unlock extends AbstractStmt {
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
	
	public static final class Switch extends AbstractStmt {
		private JilExpr condition;
		private final List<Pair<JilExpr.Number,String>> cases;
		private String defaultLab;
		
		public Switch(JilExpr condition, List<Pair<JilExpr.Number, String>> cases,
				String defaultLab, Attribute... attributes) {
			super(attributes);
			this.condition = condition;
			this.cases = cases;
			this.defaultLab = defaultLab;
		}

		public Switch(JilExpr condition, List<Pair<JilExpr.Number, String>> cases,
				String defaultLab, List<Attribute> attributes) {
			super(attributes);
			this.condition = condition;
			this.cases = cases;
			this.defaultLab = defaultLab;
		}
		
		public JilExpr condition() {
			return condition;
		}
		
		public void setCondition(JilExpr condition) {
			this.condition = condition;
		}
		
		public List<Pair<JilExpr.Number,String>> cases() {
			return cases;
		}		
		
		/**
		 * Return the destination for all values which don't match any of the
		 * given case(s).
		 * 
		 * @return
		 */
		public String defaultLabel() {
			return defaultLab;
		}
	}
}
