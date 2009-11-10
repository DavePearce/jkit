package jkit.jil.stages;

import static jkit.compiler.SyntaxError.internal_error;
import static jkit.compiler.SyntaxError.syntax_error;

import java.util.*;

import jkit.compiler.Clazz;
import jkit.compiler.ClassLoader;
import jkit.compiler.MethodNotFoundException;
import jkit.jil.tree.*;
import jkit.jil.util.*;
import jkit.jil.dfa.BackwardAnalysis;
import jkit.jil.dfa.UnionFlowSet;
import jkit.util.Pair;

public class FieldLoadConversion extends BackwardAnalysis<UnionFlowSet<Exprs.Equiv>> {
	private final ClassLoader loader;
	
	public FieldLoadConversion(ClassLoader loader) {
		this.loader = loader;
	}
	
	// First thing I need is some kind of expression comparator. This is because
	// JilExpr doesn't have a .equals method (and probably shouldn't)
	
	public void apply(JilClass owner) {
		for(JilMethod m : owner.methods()) {			
			if (m.body() != null && !m.name().equals(owner.name())) {
				// First, infer the "live dereference expressions".
				infer(m,owner);
				// Second, implement the rewrite.
				rewrite(m.body());
			} 
		}
	}
	
	public void infer(JilMethod method, JilClass owner) {
		start(method,new UnionFlowSet(),new UnionFlowSet());
	}
	
	public UnionFlowSet<Exprs.Equiv> transfer(JilStmt stmt,
			UnionFlowSet<Exprs.Equiv> in) {
		try {
			if(stmt instanceof JilStmt.Assign) {
				in = transfer((JilStmt.Assign)stmt,in);					
			} else if(stmt instanceof JilExpr.Invoke) {
				in = transfer((JilExpr.Invoke)stmt,in);										
			} else if(stmt instanceof JilExpr.New) {
				in = transfer((JilExpr.New) stmt,in);						
			} else if(stmt instanceof JilStmt.Return) {
				in = transfer((JilStmt.Return) stmt,in);
			} else if(stmt instanceof JilStmt.Throw) {
				in = transfer((JilStmt.Throw) stmt,in);
			} else if (stmt instanceof JilStmt.Nop
					|| stmt instanceof JilStmt.Label) {
				// nop
			} else if(stmt instanceof JilStmt.Lock) {		
				in = transfer((JilStmt.Lock) stmt,in);
			} else if(stmt instanceof JilStmt.Unlock) {		
				in = transfer((JilStmt.Unlock) stmt,in);
			} else {
				syntax_error("unknown statement encountered (" + stmt.getClass().getName() + ")",stmt);
				return null;
			}		
		} catch(ClassNotFoundException e) {
			internal_error(stmt,e);
			return null;
		}
		
		return in;
	}
	
	public UnionFlowSet<Exprs.Equiv> transfer(JilStmt.Assign stmt,
			UnionFlowSet<Exprs.Equiv> in) throws ClassNotFoundException {										
		JilExpr lhs = stmt.lhs();
		JilExpr rhs = stmt.rhs();
		
		Set<Exprs.Equiv> uses = in.toSet();
		killUses(lhs,uses);
		killUses(rhs,uses);
		
		if(lhs instanceof JilExpr.Variable) {
			JilExpr.Variable v = (JilExpr.Variable) lhs;
			if(rhs instanceof JilExpr.Variable) {
				// here, we can perform a substitution
				JilExpr.Variable rv = (JilExpr.Variable) rhs;
				ArrayList<Exprs.Equiv> olduses = new ArrayList(uses);
				uses.clear();				
				for(Exprs.Equiv ee : olduses) {
					JilExpr ne = Exprs.substitute(ee.expr(),v.value(),rv);
					uses.add(new Exprs.Equiv(ne));
				}
			} else {
				// otherwise, we need to eliminate all uses involving this
				// variable
				ArrayList<Exprs.Equiv> delta = new ArrayList();
				for(Exprs.Equiv ee : uses) {
					Map<String,Type> vars = Exprs.localVariables(ee.expr());
					if(vars.keySet().contains(v.value())) {
						delta.add(ee);
					}
				}
				uses.removeAll(delta);
			}
		} else if(lhs instanceof JilExpr.Deref) {
			JilExpr.Deref d = (JilExpr.Deref) lhs;
			genUses(d.target(),uses);
		} else if(lhs instanceof JilExpr.ArrayIndex) {
			JilExpr.ArrayIndex a = (JilExpr.ArrayIndex) lhs;
			genUses(a.target(),uses);
			genUses(a.index(),uses);				
		}
				
		genUses(rhs,uses);		
		return new UnionFlowSet(uses);
	}
	
	public UnionFlowSet<Exprs.Equiv> transfer(JilExpr.Invoke stmt,
			UnionFlowSet<Exprs.Equiv> in) throws ClassNotFoundException {										
		Set<Exprs.Equiv> uses = in.toSet();
		killUses(stmt,uses);
		genUses(stmt,uses);		
		return new UnionFlowSet(uses);		
	}
	
	public UnionFlowSet<Exprs.Equiv> transfer(JilExpr.New stmt,
			UnionFlowSet<Exprs.Equiv> in) throws ClassNotFoundException {
		Set<Exprs.Equiv> uses = in.toSet();
		killUses(stmt,uses);
		genUses(stmt,uses);		
		return new UnionFlowSet(uses);		
	}
	
	public UnionFlowSet<Exprs.Equiv> transfer(JilStmt.Return stmt,
			UnionFlowSet<Exprs.Equiv> in) throws ClassNotFoundException {
		if(stmt.expr() != null) {
			Set<Exprs.Equiv> uses = in.toSet();
			killUses(stmt.expr(),uses);
			genUses(stmt.expr(),uses);		
			return new UnionFlowSet(uses);
		}
		return in;
	}
	
	public UnionFlowSet<Exprs.Equiv> transfer(JilStmt.Throw stmt,
			UnionFlowSet<Exprs.Equiv> in) throws ClassNotFoundException {
		Set<Exprs.Equiv> uses = in.toSet();
		killUses(stmt.expr(),uses);
		genUses(stmt.expr(),uses);		
		return new UnionFlowSet(uses);		
	}
	
	public UnionFlowSet<Exprs.Equiv> transfer(JilStmt.Lock stmt,
			UnionFlowSet<Exprs.Equiv> in) throws ClassNotFoundException {
		Set<Exprs.Equiv> uses = in.toSet();
		killUses(stmt.expr(),uses);
		genUses(stmt.expr(),uses);		
		return new UnionFlowSet(uses);		
	}
	
	public UnionFlowSet<Exprs.Equiv> transfer(JilStmt.Unlock stmt,
			UnionFlowSet<Exprs.Equiv> in) throws ClassNotFoundException {
		Set<Exprs.Equiv> uses = in.toSet();
		killUses(stmt.expr(),uses);
		genUses(stmt.expr(),uses);	
		return new UnionFlowSet(uses);		
	}
	
	public UnionFlowSet<Exprs.Equiv> transfer(JilExpr e, UnionFlowSet<Exprs.Equiv> in) {
		return in; // conditions have no effect.
	}
	
	protected void genUses(JilExpr expr, Set<Exprs.Equiv> uses) {
		if(expr instanceof JilExpr.ArrayIndex) {
			genUses((JilExpr.ArrayIndex) expr, uses);
		} else if(expr instanceof JilExpr.BinOp) {		
			genUses((JilExpr.BinOp) expr, uses);
		} else if(expr instanceof JilExpr.UnOp) {		
			genUses((JilExpr.UnOp) expr, uses);								
		} else if(expr instanceof JilExpr.Cast) {
			genUses((JilExpr.Cast) expr, uses);			 			
		}  else if(expr instanceof JilExpr.Convert) {
			genUses((JilExpr.Convert) expr, uses);			 			
		} else if(expr instanceof JilExpr.ClassVariable) {
			genUses((JilExpr.ClassVariable) expr, uses);			 			
		} else if(expr instanceof JilExpr.Deref) {
			genUses((JilExpr.Deref) expr, uses);			 							
		} else if(expr instanceof JilExpr.Variable) {
			genUses((JilExpr.Variable) expr, uses);
		} else if(expr instanceof JilExpr.InstanceOf) {
			genUses((JilExpr.InstanceOf) expr, uses);
		} else if(expr instanceof JilExpr.Invoke) {
			genUses((JilExpr.Invoke) expr, uses);
		} else if(expr instanceof JilExpr.New) {
			genUses((JilExpr.New) expr, uses);
		} else if(expr instanceof JilExpr.Value) {
			genUses((JilExpr.Value) expr, uses);
		} else {
			syntax_error("Unknown expression \"" + expr + "\" encoutered",expr);			
		}		
	}
	
	protected void genUses(JilExpr.ArrayIndex e, Set<Exprs.Equiv> uses) {
		genUses(e.target(),uses);
		genUses(e.index(),uses);
	}
	
	protected void genUses(JilExpr.BinOp e, Set<Exprs.Equiv> uses) {
		genUses(e.lhs(),uses);
		genUses(e.rhs(),uses);
	}

	protected void genUses(JilExpr.UnOp e, Set<Exprs.Equiv> uses) {
		genUses(e.expr(),uses);
	}
	
	protected void genUses(JilExpr.Cast e, Set<Exprs.Equiv> uses) {
		genUses(e.expr(),uses);
	}
	
	protected void genUses(JilExpr.Deref e, Set<Exprs.Equiv> uses) {
		genUses(e.target(),uses);
		uses.add(new Exprs.Equiv(e));
	}
	
	protected void genUses(JilExpr.Variable e, Set<Exprs.Equiv> uses) {
		// do nout
	}
	
	protected void genUses(JilExpr.ClassVariable e, Set<Exprs.Equiv> uses) {
		// do nout
	}
	
	protected void genUses(JilExpr.InstanceOf e, Set<Exprs.Equiv> uses) {
		genUses(e.lhs(),uses);
	}
	
	protected void genUses(JilExpr.Invoke e, Set<Exprs.Equiv> uses) {
		genUses(e.target(),uses);
		for(JilExpr p : e.parameters()) {
			genUses(p,uses);
		}
		// uses.add(new Exprs.Equiv(e));		
	}
	
	protected void genUses(JilExpr.New e, Set<Exprs.Equiv> uses) {		
		for(JilExpr p : e.parameters()) {
			genUses(p,uses);
		}
	}
	
	protected void genUses(JilExpr.Value e, Set<Exprs.Equiv> uses) {
		if(e instanceof JilExpr.Array) {
			JilExpr.Array a = (JilExpr.Array) e;
			for(JilExpr p : a.values()) {
				genUses(p,uses);
			}
		}
	}
	
	protected void killUses(JilExpr expr, Set<Exprs.Equiv> uses) {
		if(expr instanceof JilExpr.ArrayIndex) {
			killUses((JilExpr.ArrayIndex) expr, uses);
		} else if(expr instanceof JilExpr.BinOp) {		
			killUses((JilExpr.BinOp) expr, uses);
		} else if(expr instanceof JilExpr.UnOp) {		
			killUses((JilExpr.UnOp) expr, uses);								
		} else if(expr instanceof JilExpr.Cast) {
			killUses((JilExpr.Cast) expr, uses);			 			
		}  else if(expr instanceof JilExpr.Convert) {
			killUses((JilExpr.Convert) expr, uses);			 			
		} else if(expr instanceof JilExpr.ClassVariable) {
			killUses((JilExpr.ClassVariable) expr, uses);			 			
		} else if(expr instanceof JilExpr.Deref) {
			killUses((JilExpr.Deref) expr, uses);			 							
		} else if(expr instanceof JilExpr.Variable) {
			killUses((JilExpr.Variable) expr, uses);
		} else if(expr instanceof JilExpr.InstanceOf) {
			killUses((JilExpr.InstanceOf) expr, uses);
		} else if(expr instanceof JilExpr.Invoke) {
			killUses((JilExpr.Invoke) expr, uses);
		} else if(expr instanceof JilExpr.New) {
			killUses((JilExpr.New) expr, uses);
		} else if(expr instanceof JilExpr.Value) {
			killUses((JilExpr.Value) expr, uses);
		} else {
			syntax_error("Unknown expression \"" + expr + "\" encoutered",expr);			
		}		
	}
	
	protected void killUses(JilExpr.ArrayIndex e, Set<Exprs.Equiv> uses) {
		killUses(e.target(),uses);
		killUses(e.index(),uses);
	}
	
	protected void killUses(JilExpr.BinOp e, Set<Exprs.Equiv> uses) {
		killUses(e.lhs(),uses);
		killUses(e.rhs(),uses);
	}

	protected void killUses(JilExpr.UnOp e, Set<Exprs.Equiv> uses) {
		killUses(e.expr(),uses);
	}
	
	protected void killUses(JilExpr.Cast e, Set<Exprs.Equiv> uses) {
		killUses(e.expr(),uses);
	}
	
	protected void killUses(JilExpr.Deref e, Set<Exprs.Equiv> uses) {
		killUses(e.target(),uses);
	}
	
	protected void killUses(JilExpr.Variable e, Set<Exprs.Equiv> uses) {
		// do nout
	}
	
	protected void killUses(JilExpr.ClassVariable e, Set<Exprs.Equiv> uses) {
		// do nout
	}
	
	protected void killUses(JilExpr.InstanceOf e, Set<Exprs.Equiv> uses) {
		killUses(e.lhs(),uses);
	}
	
	protected void killUses(JilExpr.Invoke e, Set<Exprs.Equiv> uses) {
		killUses(e.target(), uses);
		for (JilExpr p : e.parameters()) {
			killUses(p, uses);
		}

		try {
			Pair<Clazz, Clazz.Method> rt = loader.determineMethod(
					(Type.Reference) e.target().type(), e.name(), e.funType());

			if (!rt.second().isPure()) {
				uses.clear(); // should be able to do better in some cases
			}

		} catch (MethodNotFoundException mnfe) {
			internal_error(e, mnfe);
		} catch (ClassNotFoundException cnfe) {
			internal_error(e, cnfe);
		}
	}
	
	protected void killUses(JilExpr.New e, Set<Exprs.Equiv> uses) {		
		for(JilExpr p : e.parameters()) {
			killUses(p,uses);
		}
	}
	
	protected void killUses(JilExpr.Value e, Set<Exprs.Equiv> uses) {
		if(e instanceof JilExpr.Array) {
			JilExpr.Array a = (JilExpr.Array) e;
			for(JilExpr p : a.values()) {
				killUses(p,uses);
			}
		}
	}
	public void rewrite(List<JilStmt> body) {				
		HashMap<Integer,List<JilStmt>> imap = new HashMap();

		// The emap maps the expressions to the temporary variables they are
		// stored in.
		HashMap<Exprs.Equiv,String> emap = new HashMap();
		int tmpidx = 0;

		for(int i=0;i!=body.size();++i) {
			JilStmt stmt = body.get(i);
			ArrayList<JilStmt> inserts = new ArrayList<JilStmt>();

			if(i == 0) {
				UnionFlowSet<Exprs.Equiv> uses = stores.get(i);
				for(Exprs.Equiv ee : uses) {
					// the order in which we do this really does matter
					String var = "flc_tmp$" + tmpidx++;					
					emap.put(ee,var);
					JilExpr e = ee.expr();
					inserts.add(new JilStmt.Assign(new JilExpr.Variable(var, e
							.type()), e));
				}
			} /* else if(...) */

			body.set(i,rewrite(stmt,emap));

			imap.put(i,inserts);
		}

		// Finally, add the new inserts
		int idx = 0;
		for(int i=0;i!=body.size();++i) {
			List<JilStmt> inserts = imap.get(idx++);
			for(JilStmt s : inserts) {
				body.add(i++,s);
			}
		}		
	}	
	
	public JilStmt rewrite(JilStmt stmt, HashMap<Exprs.Equiv,String> emap) {	
		try {
			if (stmt instanceof JilStmt.Assign) {
				return rewrite((JilStmt.Assign) stmt, emap);
			} else if (stmt instanceof JilExpr.Invoke) {
				return rewrite((JilExpr.Invoke) stmt, emap);
			} else if (stmt instanceof JilExpr.New) {
				return rewrite((JilExpr.New) stmt, emap);
			} else if (stmt instanceof JilStmt.Return) {
				return rewrite((JilStmt.Return) stmt, emap);
			} else if (stmt instanceof JilStmt.Throw) {
				return rewrite((JilStmt.Throw) stmt, emap);
			} else if (stmt instanceof JilStmt.Nop
					|| stmt instanceof JilStmt.Label) {
				// nop
				return stmt;
			} else if (stmt instanceof JilStmt.Lock) {
				return rewrite((JilStmt.Lock) stmt, emap);
			} else if (stmt instanceof JilStmt.Unlock) {
				return rewrite((JilStmt.Unlock) stmt, emap);
			} else {
				syntax_error("unknown statement encountered ("
						+ stmt.getClass().getName() + ")", stmt);
			}
		} catch (Exception e) {
			internal_error(stmt, e);
		}
		return null;
	}
	
	protected JilStmt rewrite(JilStmt.Assign stmt, HashMap<Exprs.Equiv,String> emap) {
		JilExpr lhs = stmt.lhs();
		
		if(lhs instanceof JilExpr.Deref) {
			JilExpr.Deref d = (JilExpr.Deref) lhs;
			lhs = new JilExpr.Deref(rewrite(d.target(), emap), d.name(), d
					.isStatic(), d.type(), d.attributes());
		} else if(lhs instanceof JilExpr.ArrayIndex) {
			JilExpr.ArrayIndex d = (JilExpr.ArrayIndex) lhs;
			lhs = new JilExpr.ArrayIndex(rewrite(d.target(), emap), rewrite(d
					.index(), emap), d.type(), d.attributes());
		} /* otherwise, do nothing */
		
		JilExpr rhs = rewrite(stmt.rhs(),emap);
		return new JilStmt.Assign(lhs,rhs,stmt.exceptions(),stmt.attributes());
	}
		
	protected JilStmt rewrite(JilStmt.Return stmt, HashMap<Exprs.Equiv,String> emap) {
		JilExpr expr = stmt.expr();
		if(expr != null) {
			expr = rewrite(expr,emap);
		}
		return new JilStmt.Return(expr,stmt.exceptions(),stmt.attributes());
	}
		
	protected JilStmt rewrite(JilStmt.Throw stmt, HashMap<Exprs.Equiv,String> emap) {		
		return new JilStmt.Throw(rewrite(stmt.expr(),emap), stmt.exceptions(), stmt
				.attributes());		
	}
	
	protected JilStmt rewrite(JilStmt.Lock stmt, HashMap<Exprs.Equiv,String> emap) {
		return new JilStmt.Lock(rewrite(stmt.expr(),emap), stmt.exceptions(), stmt
				.attributes());
				
	}
	
	protected JilStmt rewrite(JilStmt.Unlock stmt, HashMap<Exprs.Equiv,String> emap) {
		return new JilStmt.Unlock(rewrite(stmt.expr(),emap), stmt.exceptions(), stmt
				.attributes());
	}
	
	protected JilStmt rewrite(JilStmt.IfGoto stmt, HashMap<Exprs.Equiv,String> emap) {
		return new JilStmt.IfGoto(rewrite(stmt.condition(),emap), stmt.label(), stmt
				.exceptions(), stmt.attributes());		
	}
	
	protected JilStmt rewrite(JilStmt.Switch stmt, HashMap<Exprs.Equiv,String> emap) {
		JilExpr condition = rewrite(stmt.condition(),emap);
		
		return new JilStmt.Switch(condition,stmt.cases(),stmt.defaultLabel(), stmt
				.exceptions(), stmt.attributes());		
	}
	
	protected JilExpr rewrite(JilExpr expr, HashMap<Exprs.Equiv,String> emap) {
		try {
			if (expr instanceof JilExpr.ArrayIndex) {
				return rewrite((JilExpr.ArrayIndex) expr, emap);
			} else if (expr instanceof JilExpr.BinOp) {
				return rewrite((JilExpr.BinOp) expr, emap);
			} else if (expr instanceof JilExpr.UnOp) {
				return rewrite((JilExpr.UnOp) expr, emap);
			} else if (expr instanceof JilExpr.Cast) {
				return rewrite((JilExpr.Cast) expr, emap);
			} else if (expr instanceof JilExpr.Convert) {
				return rewrite((JilExpr.Convert) expr, emap);
			} else if (expr instanceof JilExpr.ClassVariable) {
				return rewrite((JilExpr.ClassVariable) expr, emap);
			} else if (expr instanceof JilExpr.Deref) {
				return rewrite((JilExpr.Deref) expr, emap);
			} else if (expr instanceof JilExpr.Variable) {
				return rewrite((JilExpr.Variable) expr, emap);
			} else if (expr instanceof JilExpr.InstanceOf) {
				return rewrite((JilExpr.InstanceOf) expr, emap);
			} else if (expr instanceof JilExpr.Invoke) {
				return rewrite((JilExpr.Invoke) expr, emap);
			} else if (expr instanceof JilExpr.New) {
				return rewrite((JilExpr.New) expr, emap);
			} else if (expr instanceof JilExpr.Value) {
				return rewrite((JilExpr.Value) expr, emap);
			} else {
				syntax_error("Unknown expression \"" + expr + "\" encoutered",
						expr);
				return null;
			}
		} catch (Exception e) {
			internal_error(expr, e);
			return null;
		}
	}
	
	protected JilExpr rewrite(JilExpr.ArrayIndex expr, HashMap<Exprs.Equiv,String> emap) {
		return new JilExpr.ArrayIndex(rewrite(expr.target(),emap),
				rewrite(expr.index(),emap), expr.type(), expr.attributes());
	}
	
	protected JilExpr rewrite(JilExpr.BinOp expr, HashMap<Exprs.Equiv,String> emap) {
		return new JilExpr.BinOp(rewrite(expr.lhs(),emap), rewrite(expr.rhs(),emap), expr
				.op(), expr.type(), expr.attributes());		
	}
	
	protected JilExpr rewrite(JilExpr.UnOp expr, HashMap<Exprs.Equiv,String> emap) {
		return new JilExpr.UnOp(rewrite(expr.expr(),emap), expr
				.op(), expr.type(), expr.attributes());
	}
	
	protected JilExpr rewrite(JilExpr.Cast expr, HashMap<Exprs.Equiv,String> emap) {
		return new JilExpr.Cast(rewrite(expr.expr(),emap), expr.type(), expr.attributes());
	}
	
	protected JilExpr rewrite(JilExpr.Convert expr, HashMap<Exprs.Equiv,String> emap) {
		return new JilExpr.Convert(expr.type(), rewrite(expr.expr(),emap), expr.attributes());
	}
	
	protected JilExpr rewrite(JilExpr.ClassVariable expr, HashMap<Exprs.Equiv,String> emap) {
		return expr;
	}
	
	protected JilExpr rewrite(JilExpr.Deref expr,
			HashMap<Exprs.Equiv, String> emap) {
		JilExpr lhs = expr.target();
		Exprs.Equiv ee = new Exprs.Equiv(expr);
		
		String var = emap.get(ee);
		if (var != null) {
			return new JilExpr.Variable(var, expr.type(), expr.attributes());
		} else {
			lhs = rewrite(expr.target(), emap);
		}

		return new JilExpr.Deref(lhs, expr.name(), expr.isStatic(),
				expr.type(), expr.attributes());
	}
	
	protected JilExpr rewrite(JilExpr.Variable expr, HashMap<Exprs.Equiv,String> emap) {
		return expr;
	}
	
	protected JilExpr rewrite(JilExpr.InstanceOf expr, HashMap<Exprs.Equiv,String> emap) {
		return new JilExpr.InstanceOf(rewrite(expr.lhs(),emap), expr.rhs(), expr.type(), expr.attributes());		
	}
	
	protected JilExpr.Invoke rewrite(JilExpr.Invoke stmt, HashMap<Exprs.Equiv,String> emap) {
		ArrayList<JilExpr> params = new ArrayList<JilExpr>();
		for(JilExpr p : stmt.parameters()) {
			params.add(rewrite(p,emap));
		}
		
		JilExpr target = rewrite(stmt.target(),emap);
		
		if(stmt instanceof JilExpr.SpecialInvoke) {
			return new JilExpr.SpecialInvoke(target, stmt.name(), params, stmt.funType(),
					stmt.type(), stmt.exceptions(), stmt.attributes());
		} else {
			return new JilExpr.Invoke(target, stmt.name(), params, stmt.funType(),
					stmt.type(), stmt.exceptions(), stmt.attributes());			
		}
	}
	
	protected JilExpr.New rewrite(JilExpr.New stmt, HashMap<Exprs.Equiv,String> emap) {
		ArrayList<JilExpr> params = new ArrayList<JilExpr>();
		for(JilExpr p : stmt.parameters()) {
			params.add(rewrite(p,emap));
		}
		
		return new JilExpr.New(stmt.type(), params, stmt.funType(),
				stmt.exceptions(), stmt.attributes());		
	}
	
	protected JilExpr rewrite(JilExpr.Value expr, HashMap<Exprs.Equiv,String> emap) {
		if(expr instanceof JilExpr.Array) {
			JilExpr.Array a = (JilExpr.Array) expr;
			ArrayList<JilExpr> values = new ArrayList<JilExpr>();
			for(JilExpr e : a.values()) {
				values.add(rewrite(e,emap));
			}
			return new JilExpr.Array(values,a.type(),a.attributes());
		} else {
			return expr;
		}
	}
}
