// This file is part of the Java Compiler Kit (JKit)
//
// The Java Compiler Kit is free software; you can 
// redistribute it and/or modify it under the terms of the 
// GNU General Public License as published by the Free Software 
// Foundation; either version 2 of the License, or (at your 
// option) any later version.
//
// The Java Compiler Kit is distributed in the hope
// that it will be useful, but WITHOUT ANY WARRANTY; without 
// even the implied warranty of MERCHANTABILITY or FITNESS FOR 
// A PARTICULAR PURPOSE.  See the GNU General Public License 
// for more details.
//
// You should have received a copy of the GNU General Public 
// License along with the Java Compiler Kit; if not, 
// write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA
//
// (C) David James Pearce, 2009. 

package jkit.jil.tree;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import jkit.compiler.SyntacticAttribute;
import jkit.compiler.SyntacticElement;
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
	public JilStmt clearAddExceptions(List<Pair<Type.Clazz,String>> exceptions);
	
	public static abstract class AbstractStmt implements
			JilStmt,SyntacticElement{
		private final ArrayList<SyntacticAttribute> attributes;
		private final ArrayList<Pair<Type.Clazz, String>> exceptions;

		public AbstractStmt(SyntacticAttribute... attributes) {
			this.attributes = new ArrayList();
			for(SyntacticAttribute a : attributes) {
				this.attributes.add(a);
			}
			this.exceptions = new ArrayList();
		}
		
		public AbstractStmt(List<SyntacticAttribute> attributes) {
			this.attributes = new ArrayList(attributes);			
			this.exceptions = new ArrayList();
		}
		
		public AbstractStmt(List<Pair<Type.Clazz, String>> exceptions,
				List<SyntacticAttribute> attributes) {
			this.attributes = new ArrayList(attributes);
			this.exceptions = new ArrayList(exceptions);
		}
		
		
		public List<Pair<Type.Clazz, String>> exceptions() {
			return exceptions;
		}			
		
		public JilStmt addExceptions(
				List<Pair<Type.Clazz, String>> exceptions) {
			AbstractStmt r = clone();
			r.exceptions.addAll(exceptions);
			return r;
		}

		public JilStmt clearAddExceptions(List<Pair<Type.Clazz, String>> exceptions) {
			AbstractStmt r = clone();
			r.exceptions.clear();
			r.exceptions.addAll(exceptions);
			return r;
		}
		
		public JilStmt addException(Type.Clazz exception, String label) {
			AbstractStmt r = clone();
			r.exceptions.add(new Pair(exception,label));
			return r;
		}

		public <T extends SyntacticAttribute> T attribute(java.lang.Class<T> ac) {
			for (SyntacticAttribute a : attributes) {
				if (a.getClass().equals(ac)) {
					return (T) a;
				}
			}
			return null;
		}
		
		public <T extends SyntacticAttribute> List<T> attributes(java.lang.Class<T> ac) {
			ArrayList<T> r = new ArrayList();
			for(SyntacticAttribute a : attributes) {
				if(a.getClass().equals(ac)) {
					r.add((T) a);
				}
			}
			return r;
		}
		
		public List<SyntacticAttribute> attributes() {
			// this is to prevent any kind of aliasing issues.
			return new CopyOnWriteArrayList<SyntacticAttribute>(attributes);
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
				SyntacticAttribute... attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public Assign(JilExpr lhs, JilExpr rhs,
				List<SyntacticAttribute> attributes) {
			super(attributes);
			this.lhs = lhs;
			this.rhs = rhs;
		}
		
		public Assign(JilExpr lhs, JilExpr rhs,
				List<Pair<Type.Clazz, String>> exceptions,
				List<SyntacticAttribute> attributes) {
			super(exceptions,attributes);
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
			return new Assign(lhs, rhs,
					(List<Pair<Type.Clazz, String>>) exceptions(), attributes());
		}
		
		public String toString() {
			return lhs.type() + " " + lhs.toString() + " = " + rhs.toString() + ";";
		}
	}
	
	/**
	 * A return statement.
	 */
	public static final class Return extends AbstractStmt {
		private final JilExpr expr;

		public Return(JilExpr expr, SyntacticAttribute... attributes) {
			super(attributes);
			this.expr = expr;
		}
		
		public Return(JilExpr expr, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.expr = expr;
		}

		public Return(JilExpr expr, List<Pair<Type.Clazz, String>> exceptions,
				List<SyntacticAttribute> attributes) {
			super(exceptions,attributes);
			this.expr = expr;
		}
		
		public JilExpr expr() {
			return expr;
		}		
		
		public Return clone() {
			return new Return(expr,
					(List<Pair<Type.Clazz, String>>) exceptions(), attributes());
		}
		
		public String toString() {
			if(expr == null) {
				return "return;";
			} else {
				return "return " + expr.toString() + ";";
			}
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

		public Throw(JilExpr expr, SyntacticAttribute... attributes) {
			super(attributes);
			this.expr = expr;
		}

		public Throw(JilExpr expr, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.expr = expr;
		}
		
		public Throw(JilExpr expr, List<Pair<Type.Clazz, String>> exceptions,
				List<SyntacticAttribute> attributes) {
			super(exceptions,attributes);
			this.expr = expr;
		}
		
		public JilExpr expr() {
			return expr;
		}		
		
		public Throw clone() {
			return new Throw(expr,
					(List<Pair<Type.Clazz, String>>) exceptions(),attributes());
		}
		
		public String toString() {
			return "throw " + expr.toString() + ";";
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

		public Goto(String label, SyntacticAttribute... attributes) {
			super(attributes);
			this.label = label;
		}

		public Goto(String label, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.label = label;
		}
		
		public Goto(String label, List<Pair<Type.Clazz, String>> exceptions,
				List<SyntacticAttribute> attributes) {
			super(exceptions,attributes);
			this.label = label;
		}
		
		
		public String label() {
			return label;
		}		
		
		public Goto clone() {
			return new Goto(label,
					(List<Pair<Type.Clazz, String>>) exceptions(),attributes());
		}
		
		public String toString() {
			return "goto " + label + ";";
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

		public IfGoto(JilExpr condition, String label, SyntacticAttribute... attributes) {
			super(attributes);
			this.condition = condition;
			this.label = label;
		}

		public IfGoto(JilExpr condition, String label, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.condition = condition;
			this.label = label;
		}
		
		public IfGoto(JilExpr condition, String label,
				List<Pair<Type.Clazz, String>> exceptions,
				List<SyntacticAttribute> attributes) {
			super(exceptions,attributes);
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
			return new IfGoto(condition, label,
					(List<Pair<Type.Clazz, String>>) exceptions(), attributes());
		}
		
		public String toString() {
			return "if(" + condition + ") goto " + label + ";";
		}
	}
	
	public static final class Label extends AbstractStmt {
		private String label;

		public Label(String label, SyntacticAttribute... attributes) {
			super(attributes);
			this.label = label;
		}

		public Label(String label, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.label = label;
		}
		
		public Label(String label, List<Pair<Type.Clazz, String>> exceptions,
				List<SyntacticAttribute> attributes) {
			super(exceptions,attributes);
			this.label = label;
		}
		
		public String label() {
			return label;
		}		
		
		public Label clone() {
			return new Label(label,
					(List<Pair<Type.Clazz, String>>) exceptions(),attributes());
		}
		
		public String toString() {
			return label + ":";
		}
	}
	
	public static final class Nop extends AbstractStmt {
		public Nop(SyntacticAttribute... attributes) {
			super(attributes);
		}
		public Nop(List<SyntacticAttribute> attributes) {
			super(attributes);
		}
		public Nop(List<Pair<Type.Clazz, String>> exceptions,
				List<SyntacticAttribute> attributes) {
			super(exceptions,attributes);
		}
		public Nop clone() {
			return new Nop((List<Pair<Type.Clazz, String>>) exceptions(),attributes());
		}
		public String toString() {
			return "nop;";
		}
	}
	
	public static final class Lock extends AbstractStmt {
		private final JilExpr expr;

		public Lock(JilExpr expr, SyntacticAttribute... attributes) {
			super(attributes);
			this.expr = expr;
		}

		public Lock(JilExpr expr, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.expr = expr;
		}
		
		public Lock(JilExpr expr, List<Pair<Type.Clazz, String>> exceptions,
				List<SyntacticAttribute> attributes) {
			super(exceptions,attributes);
			this.expr = expr;
		}
		
		public JilExpr expr() {
			return expr;
		}	
		
		public Lock clone() {
			return new Lock(expr,
					(List<Pair<Type.Clazz, String>>) exceptions(),attributes());
		}
		public String toString() {
			return "lock " + expr;
		}
	}
	
	public static final class Unlock extends AbstractStmt {
		private final JilExpr expr;

		public Unlock(JilExpr expr, SyntacticAttribute... attributes) {
			super(attributes);
			this.expr = expr;
		}

		public Unlock(JilExpr expr, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.expr = expr;
		}
		
		public Unlock(JilExpr expr, List<Pair<Type.Clazz, String>> exceptions,
				List<SyntacticAttribute> attributes) {
			super(exceptions,attributes);
			this.expr = expr;
		}
		
		public JilExpr expr() {
			return expr;
		}
		
		public Unlock clone() {
			return new Unlock(expr,
					(List<Pair<Type.Clazz, String>>) exceptions(),attributes());
		}
		
		public String toString() {
			return "unlock " + expr;
		}
	}
	
	public static final class Switch extends AbstractStmt {
		private final JilExpr condition;
		private final ArrayList<Pair<JilExpr.Number,String>> cases;
		private final String defaultLab;
		
		public Switch(JilExpr condition, List<Pair<JilExpr.Number, String>> cases,
				String defaultLab, SyntacticAttribute... attributes) {
			super(attributes);
			this.condition = condition;
			this.cases = new ArrayList(cases);
			this.defaultLab = defaultLab;
		}

		public Switch(JilExpr condition, List<Pair<JilExpr.Number, String>> cases,
				String defaultLab, List<SyntacticAttribute> attributes) {
			super(attributes);
			this.condition = condition;
			this.cases = new ArrayList(cases);
			this.defaultLab = defaultLab;
		}
		
		public Switch(JilExpr condition, List<Pair<JilExpr.Number, String>> cases,
				String defaultLab,
				List<Pair<Type.Clazz, String>> exceptions,
				List<SyntacticAttribute> attributes) {
			super(exceptions,attributes);
			this.condition = condition;
			this.cases = new ArrayList(cases);
			this.defaultLab = defaultLab;
		}
		
		public JilExpr condition() {
			return condition;
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
		
		public Switch clone() {
			return new Switch(condition, cases, defaultLab,
					(List<Pair<Type.Clazz, String>>) exceptions(), attributes());
		}
		
		public String toString() {
			return "switch(" + condition + ")";
		}
	}
}
