package jkit.jil.util;

import java.util.*;
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
	public static JilExpr eliminateNot(UnOp e) {
		assert e.op() == UnOp.NOT;
		
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

}
