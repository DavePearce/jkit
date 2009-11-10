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

package jkit.jil.util;

import java.util.*;

import static jkit.compiler.SyntaxError.*;
import static jkit.jil.util.Types.*;
import jkit.compiler.Clazz;
import jkit.compiler.ClassLoader;
import jkit.compiler.MethodNotFoundException;
import jkit.jil.tree.*;
import jkit.jil.tree.JilExpr.*;
import jkit.jil.tree.Type;
import jkit.util.Pair;

public class Exprs {
	
	
	/**
	 * The following method determines whether an expression is side-effect
	 * free. Essentially, a JilExpr is side:effect free if either: doesn't use
	 * any method invocations; or, those that it does use are marked pure.
	 * 
	 * @param e
	 * @return
	 */
	public static boolean isSideEffectFree(JilExpr e, ClassLoader loader) {
		if(e instanceof Variable) {
			return true;
		} else if(e instanceof ClassVariable) {
			return true;
		} else if(e instanceof Cast) {
			Cast c1 = (Cast) e;
			return isSideEffectFree(c1.expr(),loader);
		} else if(e instanceof Convert) {
			Convert c1 = (Convert) e;
			return isSideEffectFree(c1.expr(),loader);
		} else if(e instanceof InstanceOf) {
			InstanceOf c1 = (InstanceOf) e;
			return isSideEffectFree(c1.lhs(),loader);
		} else if(e instanceof UnOp) {
			UnOp c1 = (UnOp) e;
			return isSideEffectFree(c1.expr(),loader);			
		} else if(e instanceof Deref) {
			Deref c1 = (Deref) e;			
			return isSideEffectFree(c1.target(),loader);
		} else if(e instanceof BinOp) {
			BinOp c1 = (BinOp) e;
			return isSideEffectFree(c1.lhs(),loader) && isSideEffectFree(c1.rhs(),loader);
		} else if(e instanceof ArrayIndex) {
			ArrayIndex c1 = (ArrayIndex) e;
			return isSideEffectFree(c1.target(), loader)
					&& isSideEffectFree(c1.index(), loader);			
		} else if(e instanceof Invoke) {
			Invoke c1 = (Invoke) e;

			try {

				if (!isSideEffectFree(c1.target(), loader)) {
					return false;
				}

				for (JilExpr p : c1.parameters()) {
					if (!isSideEffectFree(p, loader)) {
						return false;
					}
				}

				Pair<Clazz, Clazz.Method> rt = loader.determineMethod(
						(Type.Reference) c1.target().type(), c1.name(), c1
								.funType());

				return rt.second().isPure();
			} catch (MethodNotFoundException mnfe) {
				internal_error(e, mnfe);
			} catch (ClassNotFoundException cnfe) {
				internal_error(e, cnfe);
			}
			return false;
		} else if (e instanceof New) {
			New c1 = (New) e;
			
			// FIXME: this is broken

			for (JilExpr p : c1.parameters()) {
				if (!isSideEffectFree(p, loader)) {
					return false;
				}
			}

			return true;
		} else if(e instanceof Array) {
			Array c1 = (Array) e;
						
			for(JilExpr p : c1.values()) {
				if(!isSideEffectFree(p,loader)) {
					return false;
				}
			}	

			return true;			
		} else {
			syntax_error("unknown expression encountered ("
					+ e.getClass().getName() + ")", e);
		}
		return false;
	}
	
	/**
	 * The following class is provided for situations where you want to use
	 * JilExpr's in, for example, a HashSet and, furthermore, you want them to
	 * be considered equal upto attributes.
	 * 
	 * @author djp
	 * 
	 */
	public static class Equiv {
		private final JilExpr expr;

		public Equiv(JilExpr expr) {
			this.expr = expr;
		}

		public JilExpr expr() {
			return expr;
		}
		
		public boolean equals(Object o) {
			if (o instanceof Equiv) {
				Equiv ee = (Equiv) o;
				return equivalent(expr, ee.expr);
			}
			return false;
		}

		public int hashCode() {
			// the followng is something of a hack for now.
			return expr.toString().hashCode();
		}
		
		public String toString() {
			return "~[" + expr.toString() + "]";
		}
	}
	/**
	 * The following method determines whether two expressions are equivalent or
	 * not. Essentially, this means they are identical, up to attributes. Hence,
	 * two expressions which have different attributes, but were otherwise equal
	 * would be considered "equivalent".
	 * 
	 * @param e1
	 * @param e2
	 * @return
	 */
	public static boolean equivalent(JilExpr e1, JilExpr e2) {
		if(e1.getClass() != e2.getClass()) {
			return false;
		} else if(e1 instanceof Variable) {
			Variable v1 = (Variable) e1;
			Variable v2 = (Variable) e2;
			return v1.value().equals(v2.value());
		} else if(e1 instanceof ClassVariable) {
			ClassVariable v1 = (ClassVariable) e1;
			ClassVariable v2 = (ClassVariable) e2;
			return v1.type().equals(v2.type());
		} else if(e1 instanceof Cast) {
			Cast c1 = (Cast) e1;
			Cast c2 = (Cast) e2;
			return c1.type().equals(c2.type())
					&& equivalent(c1.expr(), c2.expr());
		} else if(e1 instanceof Convert) {
			Convert c1 = (Convert) e1;
			Convert c2 = (Convert) e2;
			return c1.type().equals(c2.type())
			&& equivalent(c1.expr(), c2.expr());
		} else if(e1 instanceof InstanceOf) {
			InstanceOf c1 = (InstanceOf) e1;
			InstanceOf c2 = (InstanceOf) e2;
			return c1.type().equals(c2.type())
			&& equivalent(c1.lhs(), c2.lhs());
		} else if(e1 instanceof UnOp) {
			UnOp c1 = (UnOp) e1;
			UnOp c2 = (UnOp) e2;
			return c1.op() == c2.op() && equivalent(c1.expr(), c2.expr());
		} else if(e1 instanceof Deref) {
			Deref c1 = (Deref) e1;
			Deref c2 = (Deref) e2;						
			
			boolean r = c1.name().equals(c2.name())
					&& equivalent(c1.target(), c2.target());
			
			return r;
		} else if(e1 instanceof BinOp) {
			BinOp c1 = (BinOp) e1;
			BinOp c2 = (BinOp) e2;
			return c1.op() == c2.op() && equivalent(c1.lhs(), c2.lhs())
					&& equivalent(c1.lhs(), c2.lhs());
		} else if(e1 instanceof ArrayIndex) {
			ArrayIndex c1 = (ArrayIndex) e1;
			ArrayIndex c2 = (ArrayIndex) e2;
			return equivalent(c1.target(), c2.target())
			&& equivalent(c1.index(), c2.index());
		} else if(e1 instanceof Invoke) {
			Invoke c1 = (Invoke) e1;
			Invoke c2 = (Invoke) e2;
			
			List<? extends JilExpr> c1_params = c1.parameters();
			List<? extends JilExpr> c2_params = c2.parameters();
			
			if (!c1.name().equals(c2.name())
					|| c1_params.size() != c2_params.size()) {				
				return false;
			}
			
			for (int i = 0; i != c1_params.size(); ++i) {
				if (!equivalent(c1_params.get(i), c2_params.get(i))) {					
					return false;
				}
			}			
			
			return true;
		} else if(e1 instanceof New) {
			New c1 = (New) e1;
			New c2 = (New) e2;	
			List<? extends JilExpr> c1_params = c1.parameters();
			List<? extends JilExpr> c2_params = c2.parameters();
			
			if (!c1.type().equals(c2.type())
					|| c1_params.size() != c2_params.size()) {
				return false;
			}

			for (int i = 0; i != c1_params.size(); ++i) {
				if (!equivalent(c1_params.get(i), c2_params.get(i))) {
					return false;
				}
			}			
			
			return true;
						
		} else if(e1 instanceof Array) {
			Array c1 = (Array) e1;
			Array c2 = (Array) e2;		
			List<? extends JilExpr> c1_values = c1.values();
			List<? extends JilExpr> c2_values = c2.values();
			
			if (!c1.type().equals(c2.type())
					|| c1_values.size() != c2_values.size()) {
				return false;
			}

			for (int i = 0; i != c1_values.size(); ++i) {
				if (!equivalent(c1_values.get(i), c2_values.get(i))) {
					return false;
				}
			}			
			
			return true;			
		} else {
			syntax_error("unknown expression encountered ("
					+ e1.getClass().getName() + ")", e1);
		}
		return false;
	}
	
	/**
	 * This method determines the set of local variables used within an
	 * expression.
	 * 
	 * @param e
	 * @return
	 */
	public static Map<String,Type> localVariables(JilExpr e) {
		HashMap<String,Type> vars = new HashMap<String,Type>(); 
		
		if(e instanceof Variable) {
			Variable v = (Variable) e;			
			vars.put(v.value(),v.type());
		} else if(e instanceof Cast) {
			Cast c = (Cast) e;
			vars.putAll(localVariables(c.expr()));
		} else if(e instanceof Convert) {
			Convert c = (Convert) e;
			vars.putAll(localVariables(c.expr()));
		} else if(e instanceof InstanceOf) {
			InstanceOf c = (InstanceOf) e;
			vars.putAll(localVariables(c.lhs()));
		} else if(e instanceof UnOp) {
			UnOp c = (UnOp) e;
			vars.putAll(localVariables(c.expr()));
		} else if(e instanceof Deref) {
			Deref c = (Deref) e;
			vars.putAll(localVariables(c.target()));
		} else if(e instanceof BinOp) {
			BinOp c = (BinOp) e;
			vars.putAll(localVariables(c.lhs()));
			vars.putAll(localVariables(c.rhs()));
		} else if(e instanceof ArrayIndex) {
			ArrayIndex c = (ArrayIndex) e;
			vars.putAll(localVariables(c.target()));
			vars.putAll(localVariables(c.index()));
		} else if(e instanceof Invoke) {
			Invoke c = (Invoke) e;
			vars.putAll(localVariables(c.target()));
			for(JilExpr p : c.parameters()) {
				vars.putAll(localVariables(p));
			}			
		} else if(e instanceof New) {
			New c = (New) e;			
			for(JilExpr p : c.parameters()) {
				vars.putAll(localVariables(p));
			}			
		} else if(e instanceof Array) {
			Array c = (Array) e;			
			for(JilExpr p : c.values()) {
				vars.putAll(localVariables(p));
			}			
		}
		
		return vars;
	}
	
	/**
	 * This method substitutes all occurrences of a given local variable with an
	 * expression.
	 * 
	 * @param e
	 * @return
	 */
	public static JilExpr substitute(JilExpr e, String var, JilExpr expr) {
		HashMap<String,Type> vars = new HashMap<String,Type>(); 
		
		if(e instanceof Variable) {
			Variable v = (Variable) e;			
			if(v.value().equals(var)) {
				return expr;
			} else {
				return v;
			}
		} else if(e instanceof ClassVariable) {
			return e;
		}else if(e instanceof Cast) {
			Cast c = (Cast) e;
			return new Cast(substitute(c.expr(),var,expr),c.type(),c.attributes());
		} else if(e instanceof Convert) {
			Convert c = (Convert) e;
			return new Cast(substitute(c.expr(),var,expr),c.type(),c.attributes());
		} else if(e instanceof InstanceOf) {
			InstanceOf c = (InstanceOf) e;
			return new InstanceOf(substitute(c.lhs(), var, expr), c.rhs(), c
					.type(), c.attributes());
		} else if(e instanceof UnOp) {
			UnOp c = (UnOp) e;
			return new UnOp(substitute(c.expr(),var,expr),c.op(),c.type(),c.attributes());			
		} else if(e instanceof Deref) {
			Deref c = (Deref) e;
			return new Deref(substitute(c.target(), var, expr), c.name(), c
					.isStatic(), c.type(), c.attributes());					
		} else if(e instanceof BinOp) {
			BinOp c = (BinOp) e;
			return new BinOp(substitute(c.lhs(), var, expr), substitute(
					c.rhs(), var, expr), c.op(), c.type(), c.attributes());			
		} else if(e instanceof ArrayIndex) {
			ArrayIndex c = (ArrayIndex) e;
			return new ArrayIndex(substitute(c.target(), var, expr), substitute(
					c.index(), var, expr), c.type(), c.attributes());			
		} else if(e instanceof Invoke) {
			Invoke c = (Invoke) e;
			JilExpr target = substitute(c.target(), var, expr);
			ArrayList<JilExpr> params = new ArrayList();
			for(JilExpr p : c.parameters()) {
				params.add(substitute(p, var, expr));
			}			
			if(e instanceof SpecialInvoke) {				
				return new SpecialInvoke(target, c.name(), params, c
						.funType(), c.type(), c.attributes());
			} else {
				return new Invoke(target, c.name(), params, c
						.funType(), c.type(), c.attributes());
			}
		} else if(e instanceof New) {
			New c = (New) e;

			ArrayList<JilExpr> params = new ArrayList();
			for (JilExpr p : c.parameters()) {
				params.add(substitute(p, var, expr));
			}

			return new New(c.type(), params, c.funType(), c.attributes());
							
		} else if(e instanceof Array) {
			Array c = (Array) e;			
			
			ArrayList<JilExpr> values = new ArrayList();
			for (JilExpr p : c.values()) {
				values.add(substitute(p, var, expr));
			}

			return new Array(values, c.type(), c.attributes());			
		} else {
			syntax_error("unknown expression encountered ("
					+ e.getClass().getName() + ")", e);
		}
		
		return null;
	}
	
	/**
	 * This method simply inverts a boolean comparison.
	 */
	public static JilExpr invertBoolean(JilExpr e) {
		if(e instanceof BinOp) {
			BinOp be = (BinOp) e;
			switch(be.op()) {
			case BinOp.EQ:
				return new BinOp(be.lhs(), be.rhs(), BinOp.NEQ,
						new Type.Bool(), be.attributes());
			case BinOp.NEQ:
				return new BinOp(be.lhs(), be.rhs(), BinOp.EQ, new Type.Bool(),
						be.attributes());
			case BinOp.LT:
				return new BinOp(be.lhs(), be.rhs(), BinOp.GTEQ,
						new Type.Bool(), be.attributes());
			case BinOp.LTEQ:
				return new BinOp(be.lhs(), be.rhs(), BinOp.GT, new Type.Bool(),
						be.attributes());
			case BinOp.GT:
				return new BinOp(be.lhs(), be.rhs(), BinOp.LTEQ,
						new Type.Bool(), be.attributes());
			case BinOp.GTEQ:
				return new BinOp(be.lhs(), be.rhs(), BinOp.LT, new Type.Bool(),
						be.attributes());
			}
		} else if(e instanceof UnOp) {
			UnOp uop = (UnOp) e;
			if(uop.op() == UnOp.NOT) {
				return uop.expr();
			}
		}
		return new UnOp(e, UnOp.NOT, new Type.Bool(), e.attributes());		
	}
	
	/**
     * This method attempts to eliminate an expressio of the form !e, by
     * applying demorgans theorem and various well-known equivalences.
     * Specifically, the rules are:
     * 
     * !!X       ===> X
     * !(X == Y) ===> X != Y
     * !(X != Y) ===> X == Y
     * !(X < Y)  ===> X >= Y
     * !(X <= Y) ===> X > Y
     * !(X > Y)  ===> X <= Y
     * !(X >= Y) ===> X <  Y
     * 
     * !(X && Y) ===> !X || !Y
     * !(X || Y) ===> !X && !Y
     * 
     * Note that, in the case of !(X instanceof Y), or !f(...), no rewrite 
     * is possible, so the original expression is simply returned.
     */
	public static JilExpr eliminateNot(JilExpr _e) {
		if(!(_e instanceof JilExpr.UnOp)) {
			return _e;
		} 
		
		UnOp e = (UnOp) _e;
		
		if(e.op() != UnOp.NOT) {
			return e;
		}
		
		
		if(e.expr() instanceof UnOp) {
			UnOp e2 = (UnOp) e.expr();
			if(e2.op() == UnOp.NOT) {
				// now, check for another not!
				if(e2.expr() instanceof UnOp) {
					UnOp e3 = (UnOp) e.expr();
					if(e3.op() == UnOp.NOT) {
						// expression originally had form !!!e
						return eliminateNot(e3);
					}
				}
				return e2.expr();
			} else { return e; } // must be a type error					
		} else if(e.expr() instanceof BinOp) {
			BinOp e2 = (BinOp) e.expr();
			switch (e2.op()) {
			case BinOp.EQ:
				return new BinOp(e2.lhs(), e2.rhs(), BinOp.NEQ,
						new Type.Bool(), e.attributes());
			case BinOp.NEQ:
				return new BinOp(e2.lhs(), e2.rhs(), BinOp.EQ, new Type.Bool(),
						e.attributes());
			case BinOp.LT:
				return new BinOp(e2.lhs(), e2.rhs(), BinOp.GTEQ,
						new Type.Bool(), e.attributes());
			case BinOp.LTEQ:
				return new BinOp(e2.lhs(), e2.rhs(), BinOp.GT, new Type.Bool(),
						e.attributes());
			case BinOp.GT:
				return new BinOp(e2.lhs(), e2.rhs(), BinOp.LTEQ,
						new Type.Bool(), e.attributes());
			case BinOp.GTEQ:
				return new BinOp(e2.lhs(), e2.rhs(), BinOp.LT, new Type.Bool(),
						e.attributes());
			case BinOp.LAND: {
				JilExpr lhs = eliminateNot(new UnOp(e2.lhs(), UnOp.NOT,
						new Type.Bool(), e.attributes()));
				JilExpr rhs = eliminateNot(new UnOp(e2.rhs(), UnOp.NOT,
						new Type.Bool(), e.attributes()));
				return new BinOp(lhs, rhs, BinOp.LOR, new Type.Bool(), e
						.attributes());
			}
			case BinOp.LOR: {
				JilExpr lhs = eliminateNot(new UnOp(e2.lhs(), UnOp.NOT,
						new Type.Bool(), e.attributes()));
				JilExpr rhs = eliminateNot(new UnOp(e2.rhs(), UnOp.NOT,
						new Type.Bool(), e.attributes()));
				return new BinOp(lhs, rhs, BinOp.LAND, new Type.Bool(), e
						.attributes());
			}
			}
		}

		// no rewrite rules apply here, so do nothing!
		return e;
	}

	/**
	 * This method looks at the actual type of an expression (1st param), and
	 * compares it with the required type (2nd param). If they are different it
	 * inserts an implicit type conversion. This is useful, since it means we
	 * only have to work out these type conversions the once, rather than every
	 * time we encounter an expression.
	 * 
	 * @param e - the expression whose actual type is to be compared.
	 * @param t - the required type of the expression.
	 * @return
	 */
	public static JilExpr implicitCast(JilExpr e, Type t) {
		if(e == null) { return null; }
		Type e_t = e.type();
		// insert implicit casts for primitive types.
		if (!e_t.equals(t)
				&& (t instanceof Type.Primitive && e_t instanceof Type.Primitive)) {			
			e = new JilExpr.Convert((Type.Primitive) t, e, e.attribute(SourceLocation.class));
		} else if(t instanceof Type.Primitive && e_t instanceof Type.Clazz) {
			Type.Clazz r = (Type.Clazz) e_t;
			if (r.pkg().equals("java.lang") && r.components().size() == 1) {
				String c = r.components().get(0).first();
				if (c.equals("Byte")) {
					Type.Function funType = new Type.Function(new Type.Byte());
					return implicitCast(
							new JilExpr.Invoke(e, "byteValue",
									new ArrayList<JilExpr>(), funType,
									new Type.Byte()), t);
				} else if (c.equals("Character")) {
					Type.Function funType = new Type.Function(new Type.Char());
					return implicitCast(new JilExpr.Invoke(e, "charValue",
							new ArrayList<JilExpr>(), funType,
							new Type.Char()), t);
				} else if (c.equals("Short")) {
					Type.Function funType = new Type.Function(new Type.Short());
					return implicitCast(
							new JilExpr.Invoke(e, "shortValue",
									new ArrayList<JilExpr>(), funType,
									new Type.Short()), t);
				} else if (c.equals("Integer")) {
					Type.Function funType = new Type.Function(new Type.Int());
					return implicitCast(new JilExpr.Invoke(e, "intValue",
							new ArrayList<JilExpr>(), funType,
							new Type.Int()), t);
				} else if (c.equals("Long")) {
					Type.Function funType = new Type.Function(new Type.Long());
					return implicitCast(
							new JilExpr.Invoke(e, "longValue",
									new ArrayList<JilExpr>(), funType,
									new Type.Long()), t);
				} else if (c.equals("Float")) {
					Type.Function funType = new Type.Function(new Type.Float());
					return implicitCast(
							new JilExpr.Invoke(e, "floatValue",
									new ArrayList<JilExpr>(), funType,
									new Type.Float()), t);
				} else if (c.equals("Double")) {
					Type.Function funType = new Type.Function(new Type.Double());
					return implicitCast(new JilExpr.Invoke(e, "doubleValue",
							new ArrayList<JilExpr>(), funType,
							new Type.Double()), t);
				} else if (c.equals("Boolean")) {
					Type.Function funType = new Type.Function(new Type.Bool());
					return implicitCast(
							new JilExpr.Invoke(e, "booleanValue",
									new ArrayList<JilExpr>(), funType,
									new Type.Bool()), t);
				} else {
					syntax_error("found type " + e_t + ", required " + t,e);
				}
			}
		} else if(e_t instanceof Type.Primitive && t instanceof Type.Clazz) {
			ArrayList<JilExpr> params = new ArrayList<JilExpr>();
			params.add(e);
			Type.Function funType = new Type.Function(new Type.Void(), e_t);
			return new JilExpr.New(boxedType((Type.Primitive) e_t), params,
					funType, e.attribute(SourceLocation.class));			
		} 
		
		return e;
	}
}
