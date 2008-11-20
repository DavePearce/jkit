package jkit.stages.codegen;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jkit.compiler.ClassTable;
import jkit.compiler.FieldNotFoundException;
import jkit.compiler.InternalException;
import jkit.jkil.Clazz;
import jkit.jkil.Field;
import jkit.jkil.FlowGraph;
import jkit.jkil.Method;
import jkit.jkil.Type;
import jkit.jkil.Type.Reference;
import jkit.stages.Stage;
import jkit.util.Pair;
import jkit.util.Triple;
import jkit.util.graph.Graphs;

public class FieldInitialisation implements Stage {

	public String description() {
		return "Move field initialisation code into constructors and static blocks.";
	}
	
	public void apply(Clazz owner) {	
		for(Method m : owner.methods()) {
			if(m.name().equals(owner.name())) {
				apply(m,owner);
			}
		}
		
		Map<FlowGraph.Point, Object> statics = new HashMap<FlowGraph.Point, Object>();
		List<FlowGraph.Point> points = new ArrayList<FlowGraph.Point>();
		for (Field f: owner.fields()) {
			if(f.isFinal() && f.initialiser() != null) {
				// We have a much more complicated situation. We need to handle
				// constant fields
				if(findConstValue(owner, f)) {
					continue;
				}
			}
			if (f.isStatic() && f.initialiser() != null) {
				statics.put(f.point(), f);
				points.add(f.point());
			}
		}
		
		ArrayList<FlowGraph.LocalVarDef> lvds = new ArrayList<FlowGraph.LocalVarDef>();
		
		for (Iterator<Method> i = owner.methods().iterator(); i.hasNext();) {
			Method m = i.next();
			if (m.name() == "<clinit>") {
				statics.put(m.point(), m);
				points.add(m.point());
				i.remove();
				if(m.code() != null) {
					lvds.addAll(m.code().localVariables());
				}
			}
		}
		
		if (!points.isEmpty()) {
			Collections.sort(points);
			Method statc = new Method(Modifier.STATIC,
					new jkit.jkil.Type.Function(Type.voidType(), new Type[0],
							new Type.Variable[0]), "<clinit>",
					new LinkedList<Reference>());
			
			apply(statics, points, statc, owner, lvds);
			owner.methods().add(statc);
		}
	}
	
	public void apply(Method m, Clazz owner) {
		FlowGraph.Point first = m.code().entry();
		Set<Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr>> from = m.code().from(first);
//		if(from.size() < 1) {
//			return;
//		}
		assert from.size() == 1;
		Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr> zuper = from.iterator().next();
		assert zuper != null && first.statement() instanceof FlowGraph.Invoke;
		assert ((FlowGraph.Invoke)first.statement()).equals("super");
		
		m.code().remove(zuper);
		
		FlowGraph.Point body = zuper.second();
		
		FlowGraph.Point last = first;
		for (Field f: owner.fields()) {
			FlowGraph.Expr expr = f.initialiser();
			if (expr == null || f.isStatic()) continue;
			FlowGraph.Stmt stmt = new FlowGraph.Assign(
					new FlowGraph.Deref(
							new FlowGraph.LocalVar("this"), f.name()),
							expr);
			FlowGraph.Point p = new FlowGraph.Point(stmt); 
			Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr> c = 
				new Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr>(last, p, null);
			m.code().add(c);
			last = p;
		}
		
		Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr> c = 
			new Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr>(last, body, null);
		m.code().add(c);
	}
	
	public void apply(Map<FlowGraph.Point, Object> statics, List<FlowGraph.Point> points, 
			Method init, Clazz owner, ArrayList<FlowGraph.LocalVarDef> localDefinitions) {
		
		//ArrayList<FlowGraph.LocalVarDef> localDefinitions = new ArrayList<FlowGraph.LocalVarDef>();
		FlowGraph cfg = new FlowGraph(localDefinitions, null);
		
		Set<FlowGraph.Point> last = new HashSet<FlowGraph.Point>();
		for (FlowGraph.Point point: points) {
			Object o = statics.get(point);
			if (o instanceof Field) {
				Field f = (Field)o;
				FlowGraph.Expr expr = f.initialiser();
				assert expr != null && f.isStatic();
				FlowGraph.Stmt stmt = new FlowGraph.Assign(
						new FlowGraph.Deref(
								new FlowGraph.LocalVar("this"), f.name()), expr);
				FlowGraph.Point p = new FlowGraph.Point(stmt);
				if (last.isEmpty()) {
					cfg.setEntry(p);
				} else {
					for (FlowGraph.Point l: last) {
						Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr> t = 
							new Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr>(l, p, null);
						cfg.add(t);
					}
					last.clear();
				}
				last.add(p);
			}
			else {
				FlowGraph fg = ((Method)o).code();
				cfg.addAll(fg);
				FlowGraph.Point p = fg.entry();
				if (last.isEmpty()) {
					cfg.setEntry(p);
				} else {
					for (FlowGraph.Point l: last) {
						Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr> t = 
							new Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr>(l, p, null);
						cfg.add(t);
					}
					last.clear();
				}
				last.addAll(Graphs.sinks(fg)); //if multiple returns from fg
				if (last.isEmpty()) last.add(p); //if there is only one stmt in fg
			}
		}
		
		FlowGraph.Point ret = new FlowGraph.Point(new FlowGraph.Return(null));
		for (FlowGraph.Point p: last) {
			Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr> t = 
				new Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr>(p, ret, null);
			cfg.add(t);
		}
		init.setCode(cfg);
	}
	
	public boolean findConstValue(Clazz c, Field f) {
		//System.out.println("Finding constant for " + f.name());
		FlowGraph.Expr init = f.initialiser();
		Object o = evaluate(init, c, f);
		if(o != null) {
			f.setConstantValue(o);
			return true;
		} else {
			return false;
		}
	}
	
	public Object evaluate(FlowGraph.Expr e, Clazz c, Field f) {
		if(e instanceof FlowGraph.Deref) {
			FlowGraph.Deref df = (FlowGraph.Deref) e;
			Pair<Field, Type> p = handleDerefs(df, c, f);
			if (p.first().constantValue() == null) {
				findConstValue(c, p.first());
			}
			return p.first().constantValue();
		} else if(e instanceof FlowGraph.DoubleVal) {
			 FlowGraph.DoubleVal iv = (FlowGraph.DoubleVal) e;
			 return new Double(iv.value);
		} else if(e instanceof FlowGraph.IntVal) {
			FlowGraph.IntVal iv = (FlowGraph.IntVal) e;
			return new Integer(iv.value);
		} else if(e instanceof FlowGraph.LongVal) {
			return new Long(((FlowGraph.LongVal) e).value);
		} else if(e instanceof FlowGraph.CharVal) {
			return new Integer(((FlowGraph.CharVal)e).value);
		} else if(e instanceof FlowGraph.BoolVal) {
			return new Integer(((FlowGraph.BoolVal)e).value);
		} else if(e instanceof FlowGraph.StringVal) {
			return new String(((FlowGraph.StringVal)e).value);
		} else if(e instanceof FlowGraph.UnOp) {
			FlowGraph.UnOp unop = (FlowGraph.UnOp) e;
			Object var = evaluate(unop.expr, c, f);
			if(!(var instanceof Number)) {
				throw new RuntimeException("Unop for non-numerical value");
			}
			Number num = (Number) var;
			if(num instanceof Long) {
				return unop(num.longValue(), unop.op);
			} else if(num instanceof Integer) {
				return new Integer(unop(num.intValue(), unop.op).intValue());
			}
		} else if(e instanceof FlowGraph.BinOp) {
			FlowGraph.BinOp binop = (FlowGraph.BinOp) e;
			Object lhs = evaluate(binop.lhs, c, f);
			Object rhs = evaluate(binop.rhs, c, f);
			if(binop.op == FlowGraph.BinOp.ADD) {
				if(lhs instanceof String) {
					return ((String) lhs) + rhs;
				} else if(rhs instanceof String) {
					return lhs + ((String)rhs);
				}
			}
			if(!(lhs instanceof Number) || !(rhs instanceof Number)) {
				throw new RuntimeException("Binop for non-numerical values");
			}
			Number nlhs = (Number) lhs;
			Number nrhs = (Number) rhs;
			if(lhs instanceof Long || rhs instanceof Long) {
				return binop(nlhs.longValue(), nrhs.longValue(), binop.op);
			} else {
				return new Integer(binop(nlhs.intValue(), nrhs.intValue(), binop.op).intValue());
			}
			
		}
		//System.out.println("EXPR " + e);
		return null;
	}
	
	private Long binop(long lhs, long rhs, int op) {
		switch(op) {
		case FlowGraph.BinOp.ADD:
			return new Long(lhs + rhs);
		case FlowGraph.BinOp.SUB:
			return new Long(lhs - rhs);
		case FlowGraph.BinOp.AND:
			return new Long(lhs & rhs);
		case FlowGraph.BinOp.DIV:
			return new Long(lhs / rhs);
		case FlowGraph.BinOp.MOD:
			return new Long(lhs % rhs);
		case FlowGraph.BinOp.MUL:
			return new Long(lhs * rhs);
		case FlowGraph.BinOp.OR:
			return new Long(lhs | rhs);
		case FlowGraph.BinOp.SHL:
			return new Long(lhs << rhs);
		case FlowGraph.BinOp.SHR:
			return new Long(lhs >> rhs);
		case FlowGraph.BinOp.USHR:
			return new Long(lhs >>> rhs);
		default:
			throw new RuntimeException("Unknown binary op " + FlowGraph.BinOp.opstr[op]);
		}
	}
	
	private Long unop(long i, int op) {
		switch(op) {
		case FlowGraph.UnOp.NEG:
			return new Long(-i);
		case FlowGraph.UnOp.POSTDEC:
			return new Long(i--);
		case FlowGraph.UnOp.PREDEC:
			return new Long(--i);
		case FlowGraph.UnOp.POSTINC:
			return new Long(i++);
		case FlowGraph.UnOp.PREINC:
			return new Long(++i);
		default:
			throw new RuntimeException("Unknown unary op " + FlowGraph.UnOp.opstr[op]);
		}

	}
	
	private Pair<Field, Type> handleDerefs(FlowGraph.Deref d, Clazz c, Field f) {		
		if(d.target instanceof FlowGraph.ClassAccess) {
			FlowGraph.ClassAccess ca = (FlowGraph.ClassAccess) d.target;
			try {
				Triple<Clazz, Field, Type> x = ClassTable.resolveField((Type.Reference) d.target.type, d.name);
				return new Pair<Field, Type>(x.second(), x.third());
			} catch(FieldNotFoundException fe) {
				throw new InternalException(fe.getMessage(), f.point(), null, c);
			} catch(ClassNotFoundException ce) {
				throw new InternalException(ce.getMessage(), f.point(), null, c);
			}
		} else if(d.target instanceof FlowGraph.Deref) {
			FlowGraph.Deref df = (FlowGraph.Deref) d.target;
			Pair<Field, Type> p = handleDerefs(df, c, f);
			try {
				Triple<Clazz, Field, Type> x = ClassTable.resolveField((Type.Reference) p.second(), d.name);				
				return new Pair<Field, Type>(x.second(), x.third());
			} catch(FieldNotFoundException fe) {
				throw new InternalException(fe.getMessage(), f.point(), null, c);
			} catch(ClassNotFoundException ce) {
				throw new InternalException(ce.getMessage(), f.point(), null, c);
			}
		} else if(d.target instanceof FlowGraph.LocalVar) {
	           try {
	               Triple<Clazz, Field, Type> x = ClassTable.resolveField((Type.Reference) c.type(), d.name);                              
	               return new Pair<Field, Type>(x.second(), x.third());
	           } catch(FieldNotFoundException fe) {
	               throw new InternalException(fe.getMessage(), f.point(), null, c);
	           } catch(ClassNotFoundException ce) {
	               throw new InternalException(ce.getMessage(), f.point(), null, c);
	           }
		}		
		return null;
	}
}
