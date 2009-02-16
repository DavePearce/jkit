package jkit.java.stages.old;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import jkit.compiler.ClassTable;
import jkit.compiler.FieldNotFoundException;
import jkit.compiler.InternalException;
import jkit.compiler.Translator;
import jkit.jkil.Clazz;
import jkit.jkil.Field;
import jkit.jkil.FlowGraph;
import jkit.jkil.Method;
import jkit.jkil.Type;
import jkit.jkil.FlowGraph.BinOp;
import jkit.jkil.FlowGraph.Cast;
import jkit.jkil.FlowGraph.Deref;
import jkit.jkil.FlowGraph.Expr;
import jkit.jkil.FlowGraph.LocalVar;
import jkit.jkil.FlowGraph.Point;
import jkit.jkil.FlowGraph.Value;
import jkit.util.Triple;

public class SwitchConstants extends Translator {

	public String description() {
		return "Switch Constant propogation.";
	}
	
	protected void translate(Method method,Clazz owner) {
		FlowGraph cfg = method.code();
		
		Map<String,Type> environment = FlowGraph.buildEnvironment(method,owner);
		environment.put("$",Type.voidType()); // this is pretty ugly ... :) 
		
		// Second, do edges
		ArrayList<Triple<Point,Point,Expr>> newEdges = new ArrayList<Triple<Point,Point,Expr>>();
		ArrayList<Triple<Point, Point, Expr>> oldEdges = new ArrayList<Triple<Point, Point, Expr>>();
		for(Point p : cfg.domain()) {
			Set<Triple<Point, Point, Expr>> from = cfg.from(p);
			
			ArrayList<Triple<Point, Point, Expr>> edges = new ArrayList<Triple<Point, Point, Expr>>();
			for(Triple<Point, Point, Expr> t : from) {
				if(!(t.third() instanceof FlowGraph.Exception)) {
					edges.add(t);
				}
			}
			
			if(edges.size() > 2) {
				// Okay so p is a switch statement
				for(Triple<Point, Point, Expr> t : edges) {
					Expr ne = translate(t.third(), environment, p, method, owner);
					oldEdges.add(t);
					newEdges.add(new Triple<Point, Point, Expr>(t.first(), t.second(), ne));
				}
			}
			
		}
		cfg.removeAll(oldEdges);
		cfg.addAll(newEdges);
	}
	
	
	protected Expr translate(Deref deref,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		if(!(deref.target.type instanceof Type.Reference)) {
			return deref;
		}
		try {
			Triple<Clazz, Field, Type> x = ClassTable.resolveField((Type.Reference) deref.target.type, deref.name);
			if(x.second().constantValue() != null) {
				Object o = x.second().constantValue();
				if(o instanceof Integer) {
					FlowGraph.IntVal i = new FlowGraph.IntVal(((Integer) o).intValue());
					return i;
				}
			}
		} catch(FieldNotFoundException fe) {
			throw new InternalException(fe.getMessage(), point, method, owner);
		} catch(ClassNotFoundException ce) {
			throw new InternalException(ce.getMessage(), point, method, owner);
		}
		Expr target = translate(deref.target,environment,point,method,owner);
		return new Deref(target,deref.name,deref.type);
	}

	protected Expr translate(Value val,Map<String,Type> environment, Point point, Method method, Clazz owner) {
		if(val instanceof FlowGraph.CharVal) {
			FlowGraph.CharVal cv = (FlowGraph.CharVal) val;
			FlowGraph.IntVal iv = new FlowGraph.IntVal(cv.value);
			return iv;
		}
		return val;
	}
	
	protected Expr translate(Cast cast,Map<String,Type> environment,Point point, Method method, Clazz owner) {
		Expr expr = translate(cast.expr,environment,point,method,owner);
		if(expr.type instanceof Type.Int && cast.type instanceof Type.Int) {
			return expr;
		}
		return new Cast(cast.type,expr);
	}
	
	protected Expr translate(LocalVar var,Map<String,Type> environment, Point point, Method method, Clazz owner) {
		if(var.type instanceof Type.Char) {
			return new LocalVar(var.name, Type.intType());
		}
		return var;
	}
	

}
