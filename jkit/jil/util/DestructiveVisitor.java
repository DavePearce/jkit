package jkit.jil.util;

import java.util.*;
import static jkit.compiler.SyntaxError.*;
import jkit.jil.tree.*;

public class DestructiveVisitor {
	protected void apply(JilClass owner) {		
		for (JilMethod m : owner.methods()) {
			apply(m);
		}			
	}
	
	protected void apply(JilMethod m) {
		List<JilStmt> m_body = m.body();
		for(int i=0;i!=m_body.size();++i) {
			JilStmt s = m_body.get(i);
			s = apply(s);
			m_body.set(i,s);
		}
	}
	
	protected JilStmt apply(JilStmt stmt) {
		if(stmt instanceof JilStmt.Assign) {
			return apply((JilStmt.Assign)stmt);					
		} else if(stmt instanceof JilExpr.Invoke) {
			return apply((JilExpr.Invoke)stmt);										
		} else if(stmt instanceof JilExpr.New) {
			return apply((JilExpr.New) stmt);						
		} else if(stmt instanceof JilStmt.Return) {
			return apply((JilStmt.Return) stmt);
		} else if(stmt instanceof JilStmt.Throw) {
			return apply((JilStmt.Throw) stmt);
		} else if(stmt instanceof JilStmt.Nop) {		
			return apply((JilStmt.Nop) stmt);
		} else if(stmt instanceof JilStmt.Lock) {		
			return apply((JilStmt.Lock) stmt);
		} else if(stmt instanceof JilStmt.Unlock) {		
			return apply((JilStmt.Unlock) stmt);
		} else if(stmt instanceof JilStmt.Goto) {		
			return stmt;
		} else if(stmt instanceof JilStmt.IfGoto) {		
			return apply((JilStmt.IfGoto)stmt);
		} else if(stmt instanceof JilStmt.Switch) {		
			return apply((JilStmt.Switch)stmt);
		} else if(stmt instanceof JilStmt.Label) {		
			return apply((JilStmt.Label) stmt);
		} else {
			syntax_error("unknown statement encountered (" + stmt.getClass().getName() + ")",stmt);
			return null;
		}		
	}
	
	protected JilStmt apply(JilStmt.Assign stmt) {
		JilExpr lhs = apply(stmt.lhs());
		JilExpr rhs = apply(stmt.rhs());
		return new JilStmt.Assign(lhs,rhs,stmt.exceptions(),stmt.attributes());
	}
		
	protected JilStmt apply(JilStmt.Return stmt) {
		JilExpr expr = stmt.expr();
		if(expr != null) {
			expr = apply(expr);
		}
		return new JilStmt.Return(expr,stmt.exceptions(),stmt.attributes());
	}
		
	protected JilStmt apply(JilStmt.Throw stmt) {		
		return new JilStmt.Throw(apply(stmt.expr()), stmt.exceptions(), stmt
				.attributes());		
	}
	
	protected JilStmt apply(JilStmt.Nop stmt) {
		return stmt;
	}		
	
	protected JilStmt apply(JilStmt.Label stmt) {
		return stmt;
	}
	
	protected JilStmt apply(JilStmt.Lock stmt) {
		return new JilStmt.Lock(apply(stmt.expr()), stmt.exceptions(), stmt
				.attributes());
				
	}
	
	protected JilStmt apply(JilStmt.Unlock stmt) {
		return new JilStmt.Unlock(apply(stmt.expr()), stmt.exceptions(), stmt
				.attributes());
	}
	
	protected JilStmt apply(JilStmt.IfGoto stmt) {
		return new JilStmt.IfGoto(apply(stmt.condition()), stmt.label(), stmt
				.exceptions(), stmt.attributes());		
	}
	
	protected JilStmt apply(JilStmt.Switch stmt) {
		JilExpr condition = apply(stmt.condition());
		
		return new JilStmt.Switch(condition,stmt.cases(),stmt.defaultLabel(), stmt
				.exceptions(), stmt.attributes());		
	}
	
	protected JilExpr apply(JilExpr expr) {
		if(expr instanceof JilExpr.ArrayIndex) {
			return apply((JilExpr.ArrayIndex) expr);
		} else if(expr instanceof JilExpr.BinOp) {		
			return apply((JilExpr.BinOp) expr);
		} else if(expr instanceof JilExpr.UnOp) {		
			return apply((JilExpr.UnOp) expr);								
		} else if(expr instanceof JilExpr.Cast) {
			return apply((JilExpr.Cast) expr);			 			
		}  else if(expr instanceof JilExpr.Convert) {
			return apply((JilExpr.Convert) expr);			 			
		} else if(expr instanceof JilExpr.ClassVariable) {
			return apply((JilExpr.ClassVariable) expr);			 			
		} else if(expr instanceof JilExpr.Deref) {
			return apply((JilExpr.Deref) expr);			 							
		} else if(expr instanceof JilExpr.Variable) {
			return apply((JilExpr.Variable) expr);
		} else if(expr instanceof JilExpr.InstanceOf) {
			return apply((JilExpr.InstanceOf) expr);
		} else if(expr instanceof JilExpr.Invoke) {
			return apply((JilExpr.Invoke) expr);
		} else if(expr instanceof JilExpr.New) {
			return apply((JilExpr.New) expr);
		} else if(expr instanceof JilExpr.Value) {
			return apply((JilExpr.Value) expr);
		} else {
			syntax_error("Unknown expression \"" + expr + "\" encoutered",expr);
			return null;
		}				
	}
	
	protected JilExpr apply(JilExpr.ArrayIndex expr) {
		return new JilExpr.ArrayIndex(apply(expr.target()),
				apply(expr.index()), expr.type(), expr.attributes());
	}
	
	protected JilExpr apply(JilExpr.BinOp expr) {
		return new JilExpr.BinOp(apply(expr.lhs()), apply(expr.rhs()), expr
				.op(), expr.type(), expr.attributes());		
	}
	
	protected JilExpr apply(JilExpr.UnOp expr) {
		return new JilExpr.UnOp(apply(expr.expr()), expr
				.op(), expr.type(), expr.attributes());
	}
	
	protected JilExpr apply(JilExpr.Cast expr) {
		return new JilExpr.Cast(apply(expr.expr()), expr.type(), expr.attributes());
	}
	
	protected JilExpr apply(JilExpr.Convert expr) {
		return new JilExpr.Convert(expr.type(), apply(expr.expr()), expr.attributes());
	}
	
	protected JilExpr apply(JilExpr.ClassVariable expr) {
		return expr;
	}
	
	protected JilExpr apply(JilExpr.Deref expr) {
		return new JilExpr.Deref(apply(expr.target()), expr.name(), expr
				.isStatic(), expr.type(), expr.attributes());
	}
	
	protected JilExpr apply(JilExpr.Variable expr) {
		return expr;
	}
	
	protected JilExpr apply(JilExpr.InstanceOf expr) {
		return new JilExpr.InstanceOf(apply(expr.lhs()), expr.rhs(), expr.type(), expr.attributes());		
	}
	
	protected JilExpr.Invoke apply(JilExpr.Invoke stmt) {
		ArrayList<JilExpr> params = new ArrayList<JilExpr>();
		for(JilExpr p : stmt.parameters()) {
			params.add(apply(p));
		}
		
		JilExpr target = apply(stmt.target());
		
		if(stmt instanceof JilExpr.SpecialInvoke) {
			return new JilExpr.SpecialInvoke(target, stmt.name(), params, stmt.funType(),
					stmt.type(), stmt.exceptions(), stmt.attributes());
		} else {
			return new JilExpr.Invoke(target, stmt.name(), params, stmt.funType(),
					stmt.type(), stmt.exceptions(), stmt.attributes());			
		}
	}
	
	protected JilExpr.New apply(JilExpr.New stmt) {
		ArrayList<JilExpr> params = new ArrayList<JilExpr>();
		for(JilExpr p : stmt.parameters()) {
			params.add(apply(p));
		}
		
		return new JilExpr.New(stmt.type(), params, stmt.funType(),
				stmt.exceptions(), stmt.attributes());		
	}
	
	protected JilExpr apply(JilExpr.Value expr) {
		if(expr instanceof JilExpr.Array) {
			JilExpr.Array a = (JilExpr.Array) expr;
			ArrayList<JilExpr> values = new ArrayList<JilExpr>();
			for(JilExpr e : a.values()) {
				values.add(apply(e));
			}
			return new JilExpr.Array(values,a.type(),a.attributes());
		} else {
			return expr;
		}
	}
}
