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
import jkit.jil.tree.*;
import jkit.jil.tree.JilExpr;
import jkit.jil.tree.JilExpr.*;
import jkit.jil.tree.Type;

public class Exprs {
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
