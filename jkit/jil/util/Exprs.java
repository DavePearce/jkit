package jkit.jil.util;

import jkit.jil.tree.Expr;
import jkit.jil.tree.Expr.*;
import jkit.jil.tree.Type;

public class Exprs {
	/**
	 * This method simply inverts a boolean comparison.
	 */
	public static Expr invertBoolean(Expr e) {
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
	public static Expr eliminateNot(UnOp e) {
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
				Expr lhs = eliminateNot(new UnOp(e2.lhs(), UnOp.NOT,
						new Type.Bool(), e.attributes()));
				Expr rhs = eliminateNot(new UnOp(e2.rhs(), UnOp.NOT,
						new Type.Bool(), e.attributes()));
				return new BinOp(lhs, rhs, BinOp.LOR, new Type.Bool(), e
						.attributes());
			}
			case BinOp.LOR: {
				Expr lhs = eliminateNot(new UnOp(e2.lhs(), UnOp.NOT,
						new Type.Bool(), e.attributes()));
				Expr rhs = eliminateNot(new UnOp(e2.rhs(), UnOp.NOT,
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
