package jkit.jil.stages;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.media.sound.HsbParser;
import com.sun.org.apache.xml.internal.utils.IntVector;

import jkit.compiler.InternalException;
import jkit.compiler.Stage;
import jkit.util.dfa.BackwardAnalysis;
import jkit.util.dfa.UnionFlowSet;


/**
 * TODO comments
 * 
 * @author davenphugh
 */
public class ArrayIndexChecker extends BackwardAnalysis<UnionFlowSet<String>> implements Stage {


	public String description() {
		return "Checks all array index's to see if its in range";
	}

	
	public void apply(Clazz owner) {
		for(Method m : owner.methods()) {			
			if(m.code() != null) {
				check(m,owner);
			} 
		}
	}

	private Map<String, Range> ranges; 
	private Map<String, Set<String>> arrays;
	public void check(Method method,Clazz owner) {
		ranges = new HashMap<String, Range>();
		arrays = new HashMap<String, Set<String>>();
		System.out.println("method = " + method.name());
		FlowGraph cfg = method.code();
		//System.out.println(cfg);
		
		UnionFlowSet<String> initStore = new UnionFlowSet<String>();
		// should we do this so we can ignore in method var's
		for(LocalVarDef v : cfg.localVariables()) {
			if(v.isParameter()) {				
				initStore.add(v.name());
			}
		}
		
		start(cfg, initStore);//cfg.entry(),initStore);
		for (LocalVarDef v : cfg.localVariables()) {
			if (!v.isParameter()) ranges.remove(v.name());
		}
		System.out.println("ranges = " + ranges);
		//System.out.println("arrays still undef = " + arrays);
	}


	public void transfer(Point p, UnionFlowSet<String> in) {
		Stmt stmt = p.statement();
		transfer(p, stmt, in);
	}
	
	private void transfer(Point p, Stmt stmt, UnionFlowSet<String> in) {
		//System.out.println("stmt = " + stmt + " at point " + p);
		//System.out.println("in (*)= " + in);
		if(stmt instanceof Assign) {
			transfer(p,(Assign)stmt,in);					
		} else if(stmt instanceof Invoke) {
			transfer(p,(Invoke)stmt,in);										
		} else if(stmt instanceof New) {
			transfer(p,(New) stmt,in);						
		} else if(stmt instanceof Return) {
			transfer(p,(Return) stmt,in);
		} else if(stmt instanceof Throw) {
			transfer(p,(Throw) stmt,in);
		} else if(stmt instanceof Nop) {		
			// do nothing
		} else if(stmt instanceof Lock) {		
			transfer(p,(Lock) stmt,in);
		} else if(stmt instanceof Unlock) {		
			transfer(p,(Unlock) stmt,in);
		} else if(stmt != null){
			throw new InternalException("Unknown statement encountered: " + stmt,p,null,null);
		}		
	}

	private void transfer(Point p, Unlock stmt, UnionFlowSet<String> in) {
		// don't need
	}

	private void transfer(Point p, Lock stmt, UnionFlowSet<String> in) {
		// don't need
	}

	private void transfer(Point p, Throw stmt, UnionFlowSet<String> in) {
		System.out.println("throw " + stmt.expr);
		getRange(stmt.expr, p, null);
	}

	private void transfer(Point p, Return stmt, UnionFlowSet<String> in) {
		System.out.println("return " + stmt.expr);
		getRange(stmt.expr, p, null);
	}
	
	private void transfer(Point p, New stmt, UnionFlowSet<String> in) {
		System.out.println("New " + stmt.parameters);
		for (Expr e : stmt.parameters) {
			getRange(e, p, null);
		}
	}
	
	private void transfer(Point p, Invoke stmt, UnionFlowSet<String> in) {
		System.out.println("invoke " + stmt.target + ", " + stmt.parameters + " " + stmt.polymorphic);
		System.out.println(stmt.name);
		getRange(stmt.target, p, null);
		for (Expr e : stmt.parameters) {
			getRange(e, p, null);
		}
	}

	private void transfer(Point p, Assign stmt, UnionFlowSet<String> in) {
		// TODO fix up a bit
		//System.out.println("Assign = " + stmt.lhs + "=" + stmt.rhs);
		Range rhs = getRange(stmt.rhs, p, null);
		if (stmt.lhs instanceof ArrayIndex) {
			getRange(stmt.lhs, p, null);
		}
		//if (!(stmt.lhs instanceof ArrayIndex)) {
		else {
			Range lhs = getRange(stmt.lhs, p, null);
			String name = containsVariable(stmt.rhs);
			if (name != null) {
				// rearrange
				Range r = new Equation(lhs, stmt.rhs).rearrange();
				ranges.put(name, r);
			} else {
				name = containsVariable(stmt.lhs);
				if (name != null) {
					ranges.put(name, rhs);
				}
			}
		}
		if ((stmt.lhs instanceof LocalVar) && (stmt.lhs.type instanceof Type.Array)
				&& ((stmt.rhs instanceof LocalVar) || (stmt.rhs instanceof Deref))
				&& (stmt.rhs.type instanceof Type.Array)) {
			String lh = ((LocalVar)stmt.lhs).name;
			String rh = (stmt.rhs instanceof LocalVar) ? ((LocalVar)stmt.rhs).name 
					: ((Deref)stmt.rhs).name;
			if (in.contains(rh) || (stmt.rhs instanceof Deref)) {
				Set<String> vars = arrays.remove(lh);
				if (vars != null) {
					for (String s : vars) {
						Range r = ranges.get(s);
						r.setVariable(lh + ".length", new LocalVar(rh + ".length", Type.intType(new TypeElement[0])));
					}
				}
				arrays.put(rh, vars);
			}
		}
		if ((stmt.lhs instanceof LocalVar) && (stmt.rhs.type instanceof Type.Array) &&
				(((stmt.rhs instanceof New) && ((New)stmt.rhs).parameters.size() > 0)
						|| (stmt.rhs instanceof ArrayVal))) {
			String arrayname = ((LocalVar)stmt.lhs).name;
			Expr length = null;
			if (stmt.rhs instanceof New) {
				// TODO multidimensional
				length = ((New)stmt.rhs).parameters.get(0);
			} else if (stmt.rhs instanceof ArrayVal) {
				length = new IntVal(((ArrayVal)stmt.rhs).values.size());
			}
			Set<String> vars = arrays.remove(arrayname);
			System.out.println(vars + " + " + length);
			if (vars != null) {
				for (String s : vars) {
					Range r = ranges.get(s);
					r.setVariable(arrayname + ".length", length);
				}
			}
		}
	}

	public void transfer(Expr e, UnionFlowSet<String> in) {
		//System.out.println("in = " + in);
		// this is where conditionals are done
		System.out.println("expr == == " + e);
		
	}
	
//	private void getRanges(Expr expr, Point point, boolean inArray) {
//		System.out.println("getRanges called for expr = " + expr);
//		if(expr instanceof ArrayIndex) {
//			getRanges((ArrayIndex) expr, point, inArray);
//		} else if(expr instanceof TernOp) {
//			getRanges((TernOp) expr, point, inArray);
//		} else if(expr instanceof BinOp) {		
//			getRanges((BinOp) expr, point, inArray);
//		} else if(expr instanceof UnOp) {		
//			getRanges((UnOp) expr, point, inArray);					
//		} else if(expr instanceof Cast) {
//			getRanges((Cast) expr, point, inArray); 			
//		} else if(expr instanceof ClassAccess) {
//			getRanges((ClassAccess) expr, point, inArray);		 			
//		} else if(expr instanceof Deref) {
//			getRanges((Deref) expr, point, inArray);					
//		} else if(expr instanceof FlowGraph.Exception) {
//			getRanges((FlowGraph.Exception) expr, point, inArray);			 							
//		} else if(expr instanceof LocalVar) {
//			getRanges((LocalVar) expr, point, inArray);
//		} else if(expr instanceof InstanceOf) {
//			getRanges((InstanceOf) expr, point, inArray);
//		} else if(expr instanceof Invoke) {
//			getRanges((Invoke) expr, point, inArray);
//		} else if(expr instanceof New) {
//			getRanges((New) expr, point, inArray);
//		} else if(expr instanceof Value) {
//			getRanges((Value) expr, point, inArray);
//		} else {
//			throw new InternalException("Unknown expression \"" + expr + "\" encoutered",point,null,null);
//		}		
//
//	}
//	
	private void getRanges(Value expr, Point point, boolean inArray) {
	}

	
	private void getRanges(New expr, Point point, boolean inArray) {
	}
	
	
	// TODO remove arrayname, not used since the revamp
	private Range getRange(Expr expr, Point point, String arrayname) {
		if (expr == null) return null;
		System.out.println("Get range called on " + expr + " " + expr.getClass().getCanonicalName());
		if (expr instanceof ArrayIndex) {
			return getRange((ArrayIndex)expr, point, arrayname);
		} else if (expr instanceof Number) {
			return new Range(((Number)expr).value, ((Number)expr).value);
		} else if (expr instanceof LocalVar) {
			return getRange((LocalVar)expr, point, arrayname);
		} else if (expr instanceof Deref) {
			System.out.println("deref" + expr);
			return null;
		} else if (expr instanceof UnOp) {
			Range r = getRange(((UnOp)expr).expr, point, arrayname);
			// make a tmp variable
			Range tmp = new Range(r.low, r.high);
			tmp.doUnOp(((UnOp)expr).op);
			return tmp;
		} else if (expr instanceof BinOp) {
			// get both sides then do op on both
			Range lhs = getRange(((BinOp)expr).lhs, point, arrayname);
			Range rhs = getRange(((BinOp)expr).rhs, point, arrayname);
			// make a tmp variable so we don't change original
			Range tmp = new Range(lhs.low, lhs.high);
			tmp.doBinOp(((BinOp)expr).op, rhs);
			return tmp;
		} else if (expr instanceof New) {
			// TODO
			getRanges((New)expr, point, false);
			return new Range(0,0);
		} else if (expr instanceof ArrayVal) {
			getRanges((Value)expr, point, false);
			return new Range(0,0);
		} else if (expr instanceof Stmt) {
			transfer(point, (Stmt)expr, null);
			return null;
		} else if (expr instanceof ClassAccess) {
			return null;
		} else if (expr instanceof Cast) {
			return null;
		} else {
			throw new InternalException("Unknown expression \"" + expr + "\" encoutered",point,null,null);
		}
	}
	
	private Range getRange(ArrayIndex expr, Point point, String arrayname) {
		if (((ArrayIndex)expr).array instanceof LocalVar) {
			arrayname = ((LocalVar)((ArrayIndex)expr).array).name;
		} else if (((ArrayIndex)expr).array instanceof Deref) {
			arrayname = ((Deref)((ArrayIndex)expr).array).name;
		}
		Range tmp = new Range(0, Integer.MAX_VALUE);
		String var = containsVariable(((ArrayIndex)expr).idx); 
		if (var != null) {
			Set<String> l = arrays.get(arrayname);
			if (l == null) {
				l = new HashSet<String>();
				arrays.put(arrayname, l);
			}
			l.add(var);
			tmp.addHighVariable(arrayname + ".length", 
					new FlowGraph.BinOp(BinOp.SUB,
							new FlowGraph.LocalVar(arrayname + ".length", Type.intType(new TypeElement[0])),
							new FlowGraph.IntVal(1), Type.intType(new TypeElement[0])));
			Range ret = new Equation(tmp, ((ArrayIndex)expr).idx).rearrange();
			return ret;
		}
		// index doesn't contain a variable, not much we can do
		return null;
	}
	
	
	/**
	 * Gets the range associated with this LocalVar.
	 * 
	 * Adds the localvar to a set of variables associated with arrayname
	 * Makes new range 0,arrayname+".length" and combines it with existing range if exists
	 * Add's new range to ranges
	 * If not in arrayindex (arrayname == null), then return <MIN,MAX> 
	 * 
	 * @param expr
	 * @param point
	 * @param arrayname
	 * @return
	 */
	private Range getRange(LocalVar expr, Point point, String arrayname) {
		Range r = new Range(0, Integer.MAX_VALUE);
		if (arrayname == null) {
			r = new Range(Integer.MIN_VALUE, Integer.MAX_VALUE);
		}
		Range orig = ranges.get(expr.name);
		if (orig == null) {
			orig = r;
			ranges.put(expr.name, orig);
		} else {
			orig.and(r);
		}
		return orig; 
	}
	
	
	
	
	private String containsVariable(Expr e) {
		if (e instanceof LocalVar) 
			return ((LocalVar)e).name;
		if (e instanceof Deref) 
			return ((Deref)e).name;
		if (e instanceof UnOp) {
			return containsVariable(((UnOp)e).expr);
		} else if (e instanceof BinOp) {
			String lhs = containsVariable(((BinOp)e).lhs);
			if (lhs != null) return lhs;
			return containsVariable(((BinOp)e).rhs);
		}
		// TODO more types of Expr's
		return null;
	}
	
	/**
	 * Purpose of this class is to represent an mathematical equation.
	 * 
	 * It will always have one token on lhs (a precomputed @see Range, which will be reset after used here)
	 * It has arbitary number of tokens on rhs, (at least one)
	 * Only really useful if one of the tokens on rhs is a variable/field
	 * Method in this class is used to rearrange the equation to be unknown variable=..tmp..
	 * Ie. tmp = i + 1 -> i = tmp - 1
	 * 
	 * @author davenphugh
	 *
	 */
	private class Equation {
		
		Range lhs;
		Expr rhs;
		
		public Equation (Range l, Expr r) {
			lhs = l;
			rhs = r;
		}
		
		public Range rearrange() {
			//System.out.println("Rearranging " + lhs + " = " + rhs);
			// need to find LocalVar/Deref on rhs
			// while looking for it, shift all non-variables to lhs
			// take first one found as gospel ?
			// return final range for variable found
			if (rhs instanceof UnOp) {
				// always going to be UnOp.NEG
				lhs.negate();
				rhs = ((UnOp)rhs).expr;
				return rearrange();
			} else if (rhs instanceof BinOp) {
				// this is quite intensive, but not common to have more than say 2-3 levels deep
				if (containsVariable(((BinOp)rhs).lhs) != null) {
					lhs.doBinOp(reverseBinOp(((BinOp)rhs).op), getRange(((BinOp)rhs).rhs, null, null));
					rhs = ((BinOp)rhs).lhs;
					return rearrange();
				} else { 
					switch (((BinOp)rhs).op) {
					case BinOp.SUB:
					case BinOp.DIV:
						Range tmp = getRange(((BinOp)rhs).lhs, null, null);
						tmp.doBinOp(((BinOp)rhs).op, lhs);
						lhs = tmp;
						break;
					default:
						lhs.doBinOp(reverseBinOp(((BinOp)rhs).op), getRange(((BinOp)rhs).lhs, null, null));
					}
					
					rhs = ((BinOp)rhs).rhs;
					return rearrange();
				}
			} else if ((rhs instanceof LocalVar) || (rhs instanceof Deref)) {
				Range var = getRange(rhs, null, null);
				// much better way of doing this !!!
				var.and(lhs);
				return var;
			} else {
				return null;
			}
		}
		
	}
	
	int reverseBinOp(int op) {
		switch (op) {
		case BinOp.ADD:
			return BinOp.SUB;
		case BinOp.SUB:
			return BinOp.ADD;
		case BinOp.MUL:
			return BinOp.DIV;
		case BinOp.DIV:
			return BinOp.MUL;
		default:
			return op;
		}
	}
	
	Expr negateExpr(Expr e) {
		//return new FlowGraph.UnOp(UnOp.NEG, e);
		if (e instanceof UnOp && ((UnOp)e).op == UnOp.NEG) {
			return ((UnOp)e).expr;
		} else if (e instanceof BinOp) {
			int op = ((BinOp)e).op;
			Expr lhs = ((BinOp)e).lhs;
			Expr rhs = ((BinOp)e).rhs;
			switch (op) {
			case BinOp.ADD:
			case BinOp.SUB:
				rhs = negateExpr(rhs);
				// fall through
			case BinOp.MUL:
			case BinOp.DIV:
				lhs = negateExpr(lhs);
			}
			if (op == BinOp.SUB && (rhs instanceof UnOp) && ((UnOp)rhs).op == UnOp.NEG) {
				op = BinOp.ADD;
				rhs = ((UnOp)rhs).expr;
			} else if (op == BinOp.SUB && (rhs instanceof Number) && ((Number)rhs).value < 0) {
				op = BinOp.ADD;
				rhs = new FlowGraph.IntVal(-((Number)rhs).value);
			}
			return new FlowGraph.BinOp(op, lhs, rhs, Type.intType(new TypeElement[0]));
		} else if (e instanceof Number) {
			return new FlowGraph.IntVal(-((Number)e).value);
		} else {
			return new FlowGraph.UnOp(UnOp.NEG, e, Type.intType(new TypeElement[0]));
		}
	}
	
	Expr simplifyExpr(Expr e) {
		//System.out.println("Simplifying " + e);
		if (e instanceof UnOp && ((UnOp)e).op == UnOp.NEG
				&& (((UnOp)e).expr instanceof Number) && ((Number)((UnOp)e).expr).value < 0) {
			return new FlowGraph.IntVal(-((Number)((UnOp)e).expr).value);
		} else if (e instanceof BinOp) {
			BinOp b = (BinOp)e;
			Expr lhs = b.lhs;
			Expr rhs = b.rhs;
			if ((lhs instanceof BinOp) && (rhs instanceof Number) && (((BinOp)lhs).rhs instanceof Number)) {
				BinOp l = (BinOp)lhs;
				// (A-b)-c=A-(b+c)
				// (A-b)+c=A+(c-b)
				// (A+b)-c=A+(b-c)
				// (A+b)+c=A+(b+c)
				if (l.op == BinOp.SUB && b.op == BinOp.SUB) {
					return simplifyExpr(new FlowGraph.BinOp(BinOp.SUB,l.lhs,
							simplifyExpr(new FlowGraph.BinOp(BinOp.ADD,l.rhs,b.rhs, Type.intType(new TypeElement[0]))),
							Type.intType(new TypeElement[0])));
				} else if ((l.op == BinOp.SUB && b.op == BinOp.ADD) || (b.op == BinOp.SUB && l.op == BinOp.ADD)) {
					return simplifyExpr(new FlowGraph.BinOp(BinOp.ADD,l.lhs,
							simplifyExpr(new FlowGraph.BinOp(BinOp.SUB,l.op == BinOp.SUB ? b.rhs : l.rhs,
									l.op == BinOp.SUB ? l.rhs : b.rhs, Type.intType(new TypeElement[0]))),
							Type.intType(new TypeElement[0])));
				} else if (l.op == BinOp.ADD && b.op == BinOp.ADD) {
					return simplifyExpr(new FlowGraph.BinOp(BinOp.ADD,l.lhs,
							simplifyExpr(new FlowGraph.BinOp(BinOp.ADD,l.rhs,b.rhs,Type.intType(new TypeElement[0]))),
							Type.intType(new TypeElement[0])));
				}
				// (A/b)/c=A/(b*c)
				// (A/b)*c=A*(c/b)
				// (A*b)/c=A*(b/c)
				// (A*b)*c=A*(b*c)
				else if (l.op == BinOp.DIV && b.op == BinOp.DIV) {
					return simplifyExpr(new FlowGraph.BinOp(BinOp.DIV,l.lhs,
							simplifyExpr(new FlowGraph.BinOp(BinOp.MUL,l.rhs,b.rhs, Type.intType(new TypeElement[0]))),
							Type.intType(new TypeElement[0])));
				} else if ((l.op == BinOp.DIV && b.op == BinOp.MUL) || (b.op == BinOp.DIV && l.op == BinOp.MUL)) {
					return simplifyExpr(new FlowGraph.BinOp(BinOp.MUL,l.lhs,
							simplifyExpr(new FlowGraph.BinOp(BinOp.DIV,l.op == BinOp.DIV ? b.rhs : l.rhs,
									l.op == BinOp.DIV ? l.rhs : b.rhs, Type.intType(new TypeElement[0]))),
							Type.intType(new TypeElement[0])));
				} else if (l.op == BinOp.MUL && b.op == BinOp.MUL) {
					return simplifyExpr(new FlowGraph.BinOp(BinOp.MUL,l.lhs,
							simplifyExpr(new FlowGraph.BinOp(BinOp.MUL,l.rhs,b.rhs,Type.intType(new TypeElement[0]))),
							Type.intType(new TypeElement[0])));
				}
				// TODO more complex
				// (A+b)*c=(A*c)+(b*c)
				// (A-b)*c=(A*c)-(b*c)
				// (A+b)/c=(A/c)+(b/c)
				// (A-b)/c=(A/c)-(b/c)
			}
			
			if (b.op == BinOp.SUB && (rhs instanceof Number) && ((Number)rhs).value < 0) {
				return simplifyExpr(new BinOp(BinOp.ADD, lhs, new FlowGraph.IntVal(-((Number)rhs).value), Type.intType(new TypeElement[0])));
			} else if (b.op == BinOp.ADD && (rhs instanceof Number) && ((Number)rhs).value < 0) {
				return simplifyExpr(new BinOp(BinOp.SUB, lhs, new FlowGraph.IntVal(-((Number)rhs).value), Type.intType(new TypeElement[0])));
			}
			if ((b.op == BinOp.ADD || b.op == BinOp.SUB) && (rhs instanceof Number) && ((Number)rhs).value == 0) {
				return lhs;
			}
			if ((b.op == BinOp.MUL || b.op == BinOp.DIV) && (rhs instanceof Number) && ((Number)rhs).value == 1) {
				return lhs;
			}
			
			if (!((lhs instanceof Number) && (rhs instanceof Number)))
				return e;
			int l = ((Number)lhs).value;
			int r = ((Number)rhs).value;
			int val = 0;
			switch (b.op) {
			case BinOp.ADD:
				val = l + r;
				break;
			case BinOp.SUB:
				val = l - r;
				break;
			case BinOp.MUL:
				val = l * r;
				break;
			case BinOp.DIV:
				val = l / r;
				break;
			// TODO rest of BinOp
			default:
				return e;
			}
			return new FlowGraph.IntVal(val);
		}
		return e;
	}
	
	Expr setVariableExpr(Expr e, String variable, Expr set) {
		//System.out.println("Setting " + variable + " to " + i + " in " + e);
		// not using deref, this was made by this class
		if (e instanceof LocalVar && ((LocalVar)e).name.equals(variable)) {
			//System.out.println ("Found " + variable);
			return set;
		} else if (e instanceof UnOp && ((UnOp)e).op == UnOp.NEG) {
			Expr expr = ((UnOp)e).expr;
			expr = setVariableExpr(expr, variable, set);
			if (!(expr instanceof Number)) return e;
			return new FlowGraph.IntVal(-((Number)expr).value);
		} else if (e instanceof BinOp) {
			Expr lhs = ((BinOp)e).lhs;
			Expr rhs = ((BinOp)e).rhs;
			lhs = setVariableExpr(lhs, variable, set);
			// do this unconditionally, may have var in twice
			//if (lhs == ((BinOp)e).lhs)
				rhs = setVariableExpr(rhs, variable, set);
			return simplifyExpr(new FlowGraph.BinOp(((BinOp)e).op, lhs, rhs, Type.intType(new TypeElement[0])));
		}
		return e;
	}
	
	
	
	private class Range {
		int low = Integer.MIN_VALUE;
		int high = 0;
		
		// maybe change so not dependant on one variable??
		Map<String, Expr> lowVar = new HashMap<String, Expr>();
		Map<String, Expr> highVar = new HashMap<String, Expr>();
		
		public Range(int l, int h) {
			low = l;
			high = h;
		}
		
		public void addLowVariable(String variable, Expr e) {
			lowVar.put(variable, e);
		}
		
		public void addHighVariable(String variable, Expr e) {
			highVar.put(variable, e);
		}
		
		public void setVariable(String variable, Expr set) {
			//System.out.println("Setting " + variable + " to " + set + " in " + this);
			Expr e = lowVar.remove(variable);
			if (e != null) {
				e = setVariableExpr(e, variable, set);
				if (e instanceof Number) {
					//System.out.println("Got number " + e);
					low = Math.max(low, ((Number)e).value);
				} else {
					lowVar.put(containsVariable(e), e);
				}
			}
			e = highVar.remove(variable);
			if (e != null) {
				e = setVariableExpr(e, variable, set);
				if (e instanceof Number) {
					//System.out.println("Got number " + e);
					high = Math.min(high, ((Number)e).value);
				} else {
					highVar.put(containsVariable(e), e);
				}
			}
			//System.out.println("Set " + variable + " to " + set + " in " + this);
		}
		
		public void doUnOp(int op) {
			// do we need to worry about POST{DEC,INC}. No it is converted earlier
			// might need to worry about it if earlier in stages list
			//System.out.println("Doing UnOp=" + op + "[" + UnOp.opstr[op] + "], on " + this);
			switch (op) {
			case UnOp.NEG:
				negate();
				break;
			default:
				System.err.println("*** *** haven't coded to deal with UnOp=" + op + "[" + UnOp.opstr[op] + "] yet");
				System.exit(1);
			}
		}
		
		
		private void negate() {
			int tmp = -high;
			high = -low;
			low = tmp;
			
			Map<String, Expr> tmpVar = highVar;
			highVar = lowVar;
			lowVar = tmpVar;
			
			for (Map.Entry<String, Expr> e : lowVar.entrySet()) {
				e.setValue(negateExpr(e.getValue()));
			}
			for (Map.Entry<String, Expr> e : highVar.entrySet()) {
				e.setValue(negateExpr(e.getValue()));
			}
		}
		
		
		public void doBinOp(int op, Range rhs) {
			//System.out.println("Doing BinOp=" + op + "[" + BinOp.opstr[op] + "], on " + this + " and " + rhs);
			switch (op) {
			case BinOp.ADD:
				add(rhs);
				break;
			case BinOp.SUB:
				sub(rhs);
				break;
			case BinOp.MUL:
				mul(rhs);
				break;
			case BinOp.DIV:
				div(rhs);
				break;
			// TODO need to do all comparison operators
			default:
				System.err.println("*** *** haven't coded to deal with BinOp=" + op + "[" + BinOp.opstr[op] + "] yet");
				System.exit(1);
			}
		}

		
		
		public void and(Range r) {
			low = Math.max(low, r.low);
			high = Math.min(high, r.high);
			
			// TODO is the following right?
			lowVar.putAll(r.lowVar);
			highVar.putAll(r.highVar);
		}
		
		public void or(Range r) {
			// TODO ???
		}
		

		
		private void add(Range r) {
			//System.out.println("Adding " + this + " to + " + r);
			if (low + r.low < low && r.low > 0) 
				low = Integer.MAX_VALUE;
			else if (low + r.low > low && r.low < 0)
				low = Integer.MIN_VALUE;
			else
				low += r.low;
			
			if (high + r.high < high && r.high > 0)
				high = Integer.MAX_VALUE;
			else if (high + r.high > high && r.high < 0)
				high = Integer.MIN_VALUE;
			else
					high += r.high;
			
			for (Map.Entry<String, Expr> e : lowVar.entrySet()) {
				IntVal lval = new FlowGraph.IntVal(r.low);
				e.setValue(simplifyExpr(new FlowGraph.BinOp(BinOp.ADD, e.getValue(), lval)));
			}
			for (Map.Entry<String, Expr> e : highVar.entrySet()) {
				IntVal hval = new FlowGraph.IntVal(r.high);
				e.setValue(simplifyExpr(new FlowGraph.BinOp(BinOp.ADD, e.getValue(), hval)));
			}
			
			// ones we don't know about
			for (Map.Entry<String, Expr> e : r.lowVar.entrySet()) {
				if (lowVar.containsKey(e.getKey())) continue;
				IntVal lval = new FlowGraph.IntVal(low);
				lowVar.put(e.getKey(), simplifyExpr(new FlowGraph.BinOp(BinOp.ADD, e.getValue(), lval)));
			}
			for (Map.Entry<String, Expr> e : r.highVar.entrySet()) {
				if (highVar.containsKey(e.getKey())) continue;
				IntVal hval = new FlowGraph.IntVal(high);
				highVar.put(e.getKey(), simplifyExpr(new FlowGraph.BinOp(BinOp.ADD, e.getValue(), hval)));
			}
			// add expr's together, gets a bit complex, might need to optimize
		}
		
		private void sub(Range r) {
			if (low - r.high < low && r.high < 0) 
				low = Integer.MAX_VALUE;
			else if (low - r.high > low && r.high > 0)
				low = Integer.MIN_VALUE;
			else
				low -= r.high;
		
			if (high - r.low < high && r.low < 0)
				high = Integer.MAX_VALUE;
			else if (high - r.low > high && r.low > 0)
				high = Integer.MIN_VALUE;
			else
				high -= r.low;
			
			for (Map.Entry<String, Expr> e : lowVar.entrySet()) {
				IntVal lval = new FlowGraph.IntVal(r.high);
				e.setValue(simplifyExpr(new FlowGraph.BinOp(BinOp.SUB, e.getValue(), lval)));
			}
			for (Map.Entry<String, Expr> e : highVar.entrySet()) {
				IntVal hval = new FlowGraph.IntVal(r.low);
				e.setValue(simplifyExpr(new FlowGraph.BinOp(BinOp.SUB, e.getValue(), hval)));
			}
			
			// ones we don't know about
			for (Map.Entry<String, Expr> e : r.lowVar.entrySet()) {
				if (highVar.containsKey(e.getKey())) continue;
				IntVal hval = new FlowGraph.IntVal(high);
				highVar.put(e.getKey(), simplifyExpr(new FlowGraph.BinOp(BinOp.SUB, e.getValue(), hval)));
			}
			for (Map.Entry<String, Expr> e : r.highVar.entrySet()) {
				if (lowVar.containsKey(e.getKey())) continue;
				IntVal lval = new FlowGraph.IntVal(low);
				lowVar.put(e.getKey(), simplifyExpr(new FlowGraph.BinOp(BinOp.SUB, e.getValue(), lval)));
			}

			// sub expr's, gets a bit complex, might need to optimize
		}
		
		private void mul(Range r) {
			int tmp = low;
			low = Math.min(Math.min(tmp * r.low, tmp * r.high),
						   Math.min(high * r.low, high * r.high));
			high = Math.max(Math.max(tmp * r.low, tmp * r.high),
					        Math.max(high * r.low, high * r.high));
			// TODO cleaner way?
			// TODO variables
			
		}
		
		private void div(Range r) {
			// TODO
		}
		
		// TODO more operators
		
		public String toString() {
			return "@Range(l=" + low + ",u=" + high + ")"
			+ (lowVar.size() > 0 ? "\tlow=" + lowVar : "") + (highVar.size() > 0 ? "\thigh=" + highVar : "");
		}
		
	}
	

}

//package jkit.jkil.stages;
//
//import java.util.*;
//
//import jkit.compiler.InternalException;
//import jkit.compiler.Stage;
//import jkit.jkil.Clazz;
//import jkit.jkil.Field;
//import jkit.jkil.FlowGraph;
//import jkit.jkil.Method;
//import jkit.jkil.Type;
//import jkit.jkil.FlowGraph.*;
//import jkit.util.Pair;
//import jkit.util.dfa.*;
//
///**
// * This stage performs a definite assignment analysis to check that every
// * variable is always defined before being used. For example, consider the
// * following code snippets:
// * 
// *  1)
// *	   int x,y;
// *	   x = y + 1;
// *	   System.out.println("GOT: " + x);
// *  
// *  2)
// *     int x;
// *     if(args == null) { x = 1; }
// *     System.out.println("GOT: " + x);
// *  
// *  3)
// *     int x;
// *     try { x = 1/0; } 
// *     catch(Exception e) {}
// *     System.out.println("GOT: " + x);
// *
// *
// * In each of the above cases, variable "x" is not definitely assigned
// * before being used. @author djp
// * 
// */
//public class VariableDefinitions extends ForwardAnalysis<UnionFlowSet<String>>
//		implements Stage {
//	public String description() {
//		return "Ensure variables are defined before used";
//	}
//	
//	FlowGraph cfg;
//	
//	public void apply(Clazz owner) {
//		for(Method m : owner.methods()) {			
//			if(m.code() != null) {
//				check(m,owner);
//			} 
//		}
//	}
//	
//	public void check(Method method,Clazz owner) {
//		FlowGraph cfg = method.code();
//		this.cfg = cfg;
//		// In the following dataflow analysis, a variable is in the flow set if
//		// it is undefined.
//		
//		UnionFlowSet<String> initStore = new UnionFlowSet<String>();
//		for(LocalVarDef v : cfg.localVariables()) {
//			if(!v.isParameter()) {				
//				initStore.add(v.name());
//			}
//		}
//		
//		start(cfg, cfg.entry(),initStore);
//	}
//	
//	public void transfer(Point p, UnionFlowSet<String> in) {		
//		Stmt stmt = p.statement();
////		System.out.println("stmt class = " + stmt.getClass().getCanonicalName() + ", " + stmt);
//		//System.out.println("Looking at point " + p);
//		if(stmt instanceof Assign) {
//			transfer(p,(Assign)stmt,in);					
//		} else if(stmt instanceof Invoke) {
//			transfer(p,(Invoke)stmt,in);										
//		} else if(stmt instanceof New) {
//			transfer(p,(New) stmt,in);						
//		} else if(stmt instanceof Return) {
//			transfer(p,(Return) stmt,in);
//		} else if(stmt instanceof Throw) {
//			transfer(p,(Throw) stmt,in);
//		} else if(stmt instanceof Nop) {		
//			// do nothing
//		} else if(stmt instanceof Lock) {		
//			transfer(p,(Lock) stmt,in);
//		} else if(stmt instanceof Unlock) {		
//			transfer(p,(Unlock) stmt,in);
//		} else if(stmt != null){
//			throw new InternalException("Unknown statement encountered: " + stmt,p,null,null);
//		}		
//	}
//	
//	public void transfer(Point p, Assign stmt, UnionFlowSet<String> undefs) {
////		System.out.println("assign " + stmt);
////		System.out.println("rhs = " + stmt.rhs.getClass().getCanonicalName() + ", " + stmt.rhs);
////		System.out.println("rhs type = " + stmt.rhs.type.getClass().getCanonicalName() + ", " + stmt.rhs.type);
//		Set<String> uses = uses(stmt.rhs,p,null,null);
//		if (stmt.rhs.type instanceof Type.Array && stmt.lhs instanceof LocalVar) {
//			((Type.Array)cfg.localVariable(((LocalVar)stmt.lhs).name).type()).setLength(((Type.Array)stmt.rhs.type).getLength());
//		}
////		System.out.println("lhs = " + stmt.lhs.getClass().getCanonicalName() + ", " + stmt.lhs);
//		if(!(stmt.lhs instanceof LocalVar)) {
//			uses.addAll(uses(stmt.lhs,p,null,null));
//		}
//		checkUses(uses,undefs,p,null,null);
//		if(stmt.lhs instanceof LocalVar) {			
//			undefs.remove(((LocalVar) stmt.lhs).name);
//		}
//	}
//	
//	public void transfer(Point p, Invoke stmt, UnionFlowSet<String> undefs) {
//		Set<String> uses = uses(stmt.target,p,null,null);
//		for(Expr e : stmt.parameters) {
//			uses.addAll(uses(e,p,null,null));
//		}
//		checkUses(uses,undefs,p,null,null);		
//	}
//
//	public void transfer(Point p, New stmt, UnionFlowSet<String> undefs) {
//		Set<String> uses = new HashSet<String>();
//		for(Expr e : stmt.parameters) {
//			uses.addAll(uses(e,p,null,null));
//		}
//		checkUses(uses,undefs,p,null,null);
//	}
//	
//	public void transfer(Point p, Return stmt, UnionFlowSet<String> undefs) {
//		if(stmt.expr != null) {
//			Set<String> uses = uses(stmt.expr,p,null,null);		
//			checkUses(uses,undefs,p,null,null);
//		}
//	}
//	
//	public void transfer(Point p, Throw stmt, UnionFlowSet<String> undefs) {
//		Set<String> uses = uses(stmt.expr,p,null,null);		
//		checkUses(uses,undefs,p,null,null);
//	}
//	
//	public void transfer(Point p, Lock stmt, UnionFlowSet<String> undefs) {
//		Set<String> uses = uses(stmt.var,p,null,null);		
//		checkUses(uses,undefs,p,null,null);
//	}
//	
//	public void transfer(Point p, Unlock stmt, UnionFlowSet<String> undefs) {
//		Set<String> uses = uses(stmt.var,p,null,null);		
//		checkUses(uses,undefs,p,null,null);
//	}
//	
//	public void transfer(Point p, Expr e, UnionFlowSet<String> undefs) {				
//		checkUses(uses(e, p, null, null), undefs, p,null,null);
//	}
//	
//	public Set<String> uses(Expr expr, Point point, Method method, Clazz owner) {
////		System.out.println("uses = " + expr.getClass().getCanonicalName() + ", " + expr);
//		if(expr instanceof ArrayIndex) {
//			return uses((ArrayIndex) expr,  point, method,owner);
//		} else if(expr instanceof TernOp) {
//			return uses((TernOp) expr, point,method,owner);
//		} else if(expr instanceof BinOp) {		
//			return uses((BinOp) expr, point,method,owner);
//		} else if(expr instanceof UnOp) {		
//			return uses((UnOp) expr, point,method,owner);								
//		} else if(expr instanceof Cast) {
//			return uses((Cast) expr, point,method,owner);			 			
//		} else if(expr instanceof ClassAccess) {
//			return uses((ClassAccess) expr, point,method,owner);			 			
//		} else if(expr instanceof Deref) {
//			return uses((Deref) expr, point,method,owner);			 							
//		} else if(expr instanceof FlowGraph.Exception) {
//			return uses((FlowGraph.Exception) expr, point,method,owner);			 							
//		} else if(expr instanceof LocalVar) {
//			return uses((LocalVar) expr, point,method,owner);
//		} else if(expr instanceof InstanceOf) {
//			return uses((InstanceOf) expr, point,method,owner);
//		} else if(expr instanceof Invoke) {
//			return uses((Invoke) expr, point,method,owner);
//		} else if(expr instanceof New) {
//			return uses((New) expr, point,method,owner);
//		} else if(expr instanceof Value) {
//			return uses((Value) expr, point,method,owner);
//		} else {
//			throw new InternalException("Unknown expression \"" + expr + "\" encoutered",point,method,owner);
//		}		
//	}
//	
//	public Object evaluate(FlowGraph.Expr e, Clazz c, Field f) {
//		if(e instanceof FlowGraph.DoubleVal) {
//			 FlowGraph.DoubleVal iv = (FlowGraph.DoubleVal) e;
//			 return new Double(iv.value);
//		} else if(e instanceof FlowGraph.IntVal) {
//			FlowGraph.IntVal iv = (FlowGraph.IntVal) e;
//			return new Integer(iv.value);
//		} else if(e instanceof FlowGraph.LongVal) {
//			return new Long(((FlowGraph.LongVal) e).value);
//		} else if(e instanceof FlowGraph.CharVal) {
//			return new Integer(((FlowGraph.CharVal)e).value);
//		} else if(e instanceof FlowGraph.BoolVal) {
//			return new Integer(((FlowGraph.BoolVal)e).value);
//		} else if(e instanceof FlowGraph.StringVal) {
//			return new String(((FlowGraph.StringVal)e).value);
//		} else if(e instanceof FlowGraph.UnOp) {
//			FlowGraph.UnOp unop = (FlowGraph.UnOp) e;
//			Object var = evaluate(unop.expr, c, f);
//			if(!(var instanceof java.lang.Number)) {
//				throw new RuntimeException("Unop for non-numerical value");
//			}
//			java.lang.Number num = (java.lang.Number) var;
//			if(num instanceof Long) {
//				return unop(num.longValue(), unop.op);
//			} else if(num instanceof Integer) {
//				return new Integer(unop(num.intValue(), unop.op).intValue());
//			}
//		} else if(e instanceof FlowGraph.BinOp) {
//			FlowGraph.BinOp binop = (FlowGraph.BinOp) e;
//			Object lhs = evaluate(binop.lhs, c, f);
//			Object rhs = evaluate(binop.rhs, c, f);
//			if(binop.op == FlowGraph.BinOp.ADD) {
//				if(lhs instanceof String) {
//					return ((String) lhs) + rhs;
//				} else if(rhs instanceof String) {
//					return lhs + ((String)rhs);
//				}
//			}
//			if(!(lhs instanceof java.lang.Number) || !(rhs instanceof java.lang.Number)) {
//				throw new RuntimeException("Binop for non-numerical values");
//			}
//			java.lang.Number nlhs = (java.lang.Number) lhs;
//			java.lang.Number nrhs = (java.lang.Number) rhs;
//			if(lhs instanceof Long || rhs instanceof Long) {
//				return binop(nlhs.longValue(), nrhs.longValue(), binop.op);
//			} else {
//				return new Integer(binop(nlhs.intValue(), nrhs.intValue(), binop.op).intValue());
//			}
//			
//		}
//		//System.out.println("EXPR " + e);
//		return null;
//	}
//	
//	private Long binop(long lhs, long rhs, int op) {
//		switch(op) {
//		case FlowGraph.BinOp.ADD:
//			return new Long(lhs + rhs);
//		case FlowGraph.BinOp.SUB:
//			return new Long(lhs - rhs);
//		case FlowGraph.BinOp.AND:
//			return new Long(lhs & rhs);
//		case FlowGraph.BinOp.DIV:
//			return new Long(lhs / rhs);
//		case FlowGraph.BinOp.MOD:
//			return new Long(lhs % rhs);
//		case FlowGraph.BinOp.MUL:
//			return new Long(lhs * rhs);
//		case FlowGraph.BinOp.OR:
//			return new Long(lhs | rhs);
//		case FlowGraph.BinOp.SHL:
//			return new Long(lhs << rhs);
//		case FlowGraph.BinOp.SHR:
//			return new Long(lhs >> rhs);
//		case FlowGraph.BinOp.USHR:
//			return new Long(lhs >>> rhs);
//		default:
//			throw new RuntimeException("Unknown binary op " + FlowGraph.BinOp.opstr[op]);
//		}
//	}
//	
//	private Long unop(long i, int op) {
//		switch(op) {
//		case FlowGraph.UnOp.NEG:
//			return new Long(-i);
//		case FlowGraph.UnOp.POSTDEC:
//			return new Long(i--);
//		case FlowGraph.UnOp.PREDEC:
//			return new Long(--i);
//		case FlowGraph.UnOp.POSTINC:
//			return new Long(i++);
//		case FlowGraph.UnOp.PREINC:
//			return new Long(++i);
//		default:
//			throw new RuntimeException("Unknown unary op " + FlowGraph.UnOp.opstr[op]);
//		}
//
//	}
//	
//	public Set<String> uses(ArrayIndex expr, Point point, Method method, Clazz owner) {
////		System.out.println("expr = " + expr);
////		System.out.println("array expr class = " + expr.array.getClass().getCanonicalName());
////		System.out.println("array expr = " + expr.array);
//		if (!(expr.array.type instanceof Type.Array)) {
//			//System.out.println("NOT ARRAY!!! ***");
//		}
//		int length = ((Type.Array)expr.array.type).getLength();
////		System.out.println(expr.idx + ", " + expr.idx.getClass().getCanonicalName());
////		System.out.println("length = " + length);
//		Object eval = null;//evaluate(expr.idx, null, null);
//		if (eval instanceof java.lang.Number) {
//			int index = ((java.lang.Number)eval).intValue();
//			//System.out.println("is 0 <= " + index + " < " + length + "??");
//			//System.out.println((0 <= index && index < length) + " ***********");
//			if (!(0 <= index && index < length)) {
//			//	System.out.println ("*** BAD THINGS WITH CODE ***");
//			}
//		} else {
//			//System.out.println(eval + " isn't a number");
//		}
//		Set<String> r = uses(expr.array,point,method,owner);
//		r.addAll(uses(expr.idx,point,method,owner));
//		return r;
//	}
//	public Set<String> uses(TernOp expr, Point point, Method method, Clazz owner) { 
//		Set<String> r = uses(expr.cond,point,method,owner);
//		r.addAll(uses(expr.foption,point,method,owner));
//		r.addAll(uses(expr.toption,point,method,owner));
//		return r;		
//	}
//	public Set<String> uses(BinOp expr, Point point, Method method, Clazz owner) {
//		Set<String> r = uses(expr.lhs,point,method,owner);
//		r.addAll(uses(expr.rhs,point,method,owner));
//		return r; 
//	}
//	public Set<String> uses(UnOp expr, Point point, Method method, Clazz owner) { 		
//		return uses(expr.expr,point,method,owner); 
//	}
//	public Set<String> uses(Cast expr, Point point, Method method, Clazz owner) { 
//		return uses(expr.expr,point,method,owner);		
//	}
//	public Set<String> uses(ClassAccess expr, Point point, Method method, Clazz owner) { 		
//		return new HashSet<String>();
//	}
//	public Set<String> uses(Deref expr, Point point, Method method, Clazz owner) { 		
//		return uses(expr.target,point,method,owner);
//	}
//	public Set<String> uses(FlowGraph.Exception expr, Point point, Method method, Clazz owner) { 
//		return new HashSet<String>(); 
//	}
//	public Set<String> uses(LocalVar expr, Point point, Method method, Clazz owner) { 
////		System.out.println("Local var name = " + expr.name + ", type = " + expr.type.getClass().getCanonicalName() + ", " + expr.type);
//		HashSet<String> r = new HashSet<String>();
//		r.add(expr.name);
//		return r;
//	}
//	public Set<String> uses(InstanceOf expr, Point point, Method method, Clazz owner) { 		
//		return uses(expr.lhs,point,method,owner);
//	}
//	public Set<String> uses(Invoke expr, Point point, Method method, Clazz owner) { 
//		Set<String> r = uses(expr.target,point,method,owner);
//		for(Expr e : expr.parameters) {
//			r.addAll(uses(e,point,method,owner));
//		}
//		return r; 		
//	}
//	public Set<String> uses(New expr, Point point, Method method, Clazz owner) { 
////		System.out.println("New = " + expr);
////		System.out.println("New type = " + expr.type);
////		System.out.println("New type class = " + expr.type.getClass().getCanonicalName());
////		System.out.println("New type array? = " + (expr.type instanceof Type.Array));
//		if (expr.type instanceof Type.Array) {
//			// TODO, multidimensional arrays
//			if (expr.parameters.size() > 0) // should be for arrays !!
//				// TODO variables in size??
//				if (expr.parameters.get(0) instanceof FlowGraph.Number) {
//					
////					System.out.println("Setting length at " + expr.parameters.get(0));
//					((Type.Array)expr.type).setLength(((FlowGraph.Number)expr.parameters.get(0)).value);
//				}
////			System.out.println("Length = " + ((Type.Array)expr.type).getLength());
//			
//		}
//		Set<String> r = new HashSet<String>();
//		for(Expr e : expr.parameters) {
//			r.addAll(uses(e,point,method,owner));
//		}
//		return r; 			
//	}
//	public Set<String> uses(Value expr, Point point, Method method, Clazz owner) { 
//		if (expr instanceof ArrayVal) {
//			if (!(expr.type instanceof Type.Array)) {
//				//System.out.println("NOT ARRAY ***");
//			}
//			((Type.Array)expr.type).setLength(((ArrayVal)expr).values.size());
//		}
////		System.out.println("value = " + expr);
//		return new HashSet<String>(); 
//	}
//
//	/**
//	 * This method simply checks whether there is any variable being used that
//	 * has not yet been defined.  And, if so, throws an InternalException
//	 * 
//	 * @param uses the set of variables used at this point
//	 * @param undefs the set of variables not defined at this point
//	 * @param point the program point in question
//	 * @param method enclosing method
//	 * @param owner enclosing class
//	 */
//	private void checkUses(Set<String> uses, UnionFlowSet<String> undefs, Point point, Method method, Clazz owner) {
//		for(String v : uses) {			
//			if(undefs.contains(v)) {
//				throw new InternalException("Variable might not have been initialised",point,method,owner);
//			}
//		}
//	}
//}
