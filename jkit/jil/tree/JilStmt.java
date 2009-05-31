package jkit.jil.tree;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import jkit.util.*;

public interface JilStmt extends SyntacticElement, Cloneable {
	
	public JilStmt clone() throws CloneNotSupportedException;
	
	/**
	 * This method returns the exceptional branches associated with this
	 * statement. The exceptions should be traversed for matches in the order
	 * returned.
	 * 
	 * @return
	 */
	public List<? extends Pair<Type.Clazz,String>> exceptions();
	
	public JilStmt addException(Type.Clazz t, String v);
	public JilStmt addExceptions(List<Pair<Type.Clazz,String>> exceptions);
	
	public static abstract class AbstractStmt implements
			JilStmt,SyntacticElement{
		private final ArrayList<Attribute> attributes;
		private final ArrayList<Pair<Type.Clazz, String>> exceptions = new ArrayList();

		public AbstractStmt(Attribute... attributes) {
			this.attributes = new ArrayList();
			for(Attribute a : attributes) {
				this.attributes.add(a);
			}
		}
		
		public AbstractStmt(List<Attribute> attributes) {
			this.attributes = new ArrayList(attributes);			
		}
		
		public List<? extends Pair<Type.Clazz, String>> exceptions() {
			return exceptions;
		}			
		
		public JilStmt addExceptions(
				List<Pair<Type.Clazz, String>> exceptions) {
			AbstractStmt r = clone();
			r.exceptions.addAll(exceptions);
			return r;
		}

		public JilStmt addException(Type.Clazz exception, String label) {
			AbstractStmt r = clone();
			r.exceptions.add(new Pair(exception,label));
			return r;
		}

		public Attribute attribute(java.lang.Class ac) {
			for (Attribute a : attributes) {
				if (a.getClass().equals(ac)) {
					return a;
				}
			}
			return null;
		}
		
		public List<Attribute> attributes() {
			// this is to prevent any kind of aliasing issues.
			return new CopyOnWriteArrayList<Attribute>(attributes);
		}
		
		public abstract AbstractStmt clone();
	}
	
	/**
	 * An assignment statement.
	 * 
	 * @author djp
	 *
	 */
	public static final class Assign extends AbstractStmt {
		private final JilExpr lhs, rhs;

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
	
		public Assign clone() {
			return new Assign(lhs,rhs,attributes());
		}
	}
	
	/**
	 * A return statement.
	 */
	public static final class Return extends AbstractStmt {
		private final JilExpr expr;

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
		
		public Return clone() {
			return new Return(expr,attributes());
		}
	}
	
	/**
	 * A throw statement.
	 * 
	 * @author djp
	 *
	 */
	public static final class Throw extends AbstractStmt {
		private final JilExpr expr;

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
		
		public Throw clone() {
			return new Throw(expr,attributes());
		}
	}
	
	/**
	 * An unconditional goto statement.
	 * 
	 * @author djp
	 * 
	 */
	public static final class Goto extends AbstractStmt {
		private final String label;

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
		
		public Goto clone() {
			return new Goto(label,attributes());
		}
	}
	
	/**
	 * A conditional goto statement.
	 * 
	 * @author djp
	 *
	 */
	public static final class IfGoto extends AbstractStmt {
		private final JilExpr condition;
		private final String label;

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
		
		public String label() {
			return label;
		}		
		
		public IfGoto clone() {
			return new IfGoto(condition,label,attributes());
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
		
		public Label clone() {
			return new Label(label,attributes());
		}
	}
	
	public static final class Nop extends AbstractStmt {
		public Nop(Attribute... attributes) {
			super(attributes);
		}
		public Nop(List<Attribute> attributes) {
			super(attributes);
		}
		public Nop clone() {
			return new Nop(attributes());
		}
	}
	
	public static final class Lock extends AbstractStmt {
		private final JilExpr expr;

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
		
		public Lock clone() {
			return new Lock(expr,attributes());
		}
	}
	
	public static final class Unlock extends AbstractStmt {
		private final JilExpr expr;

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
		
		public Unlock clone() {
			return new Unlock(expr,attributes());
		}
	}
	
	public static final class Switch extends AbstractStmt {
		private final JilExpr condition;
		private final ArrayList<Pair<JilExpr.Number,String>> cases;
		private final String defaultLab;
		
		public Switch(JilExpr condition, List<Pair<JilExpr.Number, String>> cases,
				String defaultLab, Attribute... attributes) {
			super(attributes);
			this.condition = condition;
			this.cases = new ArrayList(cases);
			this.defaultLab = defaultLab;
		}

		public Switch(JilExpr condition, List<Pair<JilExpr.Number, String>> cases,
				String defaultLab, List<Attribute> attributes) {
			super(attributes);
			this.condition = condition;
			this.cases = new ArrayList(cases);
			this.defaultLab = defaultLab;
		}
		
		public JilExpr condition() {
			return condition;
		}
		
		
		public List<? extends Pair<JilExpr.Number,String>> cases() {
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
		
		public Switch clone() {
			return new Switch(condition, cases, defaultLab, attributes());
		}
	}
}
