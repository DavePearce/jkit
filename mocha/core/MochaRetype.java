package mocha.core;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import jkit.jkil.Clazz;
import jkit.jkil.FlowGraph;
import jkit.jkil.Method;
import jkit.jkil.Type;
import jkit.jkil.FlowGraph.Expr;
import jkit.jkil.FlowGraph.Point;
import jkit.stages.Stage;
import jkit.util.Pair;
import jkit.util.Triple;

public class MochaRetype implements Stage {
	
	private Inferrer inf;
	private ArrayList<Triple<Point, Point, Expr>> toAdd = new ArrayList<Triple<Point, Point, Expr>>();
	private ArrayList<Triple<Point, Point, Expr>> toRem = new ArrayList<Triple<Point, Point, Expr>>();
	private ArrayList<Pair<Point, TypeMap>> envToAdd = new ArrayList<Pair<Point, TypeMap>>();
	
	public MochaRetype(Inferrer i) {
		inf = i;
	}

	public void apply(Clazz owner) {
		for(Method m : owner.methods()) {
			apply(m, owner);
		}
		
	}
	
	public void apply(Method m, Clazz owner) {
		FlowGraph cfg = m.code();
		if(cfg == null)
			return;
		
		Map<Point, TypeMap> environments = inf.environments(m);
		
		for(Point p : environments.keySet()) {
			Set<Triple<Point, Point, Expr>> to = cfg.to(p);
			Set<Triple<Point, Point, Expr>> from = cfg.from(p);
			
			TypeMap map = environments.get(p);
			if(map == null) {
				throw new RuntimeException("No Typemap at " + p + " " + p.hashCode());
			}
			Map<String, Type> env = map.environment();

			if(to.size() == 1 && from.size() == 1) {
				
				Triple<Point, Point, Expr> trip = to.iterator().next();

				Point pred = trip.first();
				TypeMap oMap = environments.get(pred);
				Map<String, Type> oEnv = oMap.environment();

				for(String s : env.keySet()) {
					Type t = env.get(s);
					Type oT = oEnv.get(s);
					if(!t.equals(oT) && !t.equals(map.lastAssigned(s))) {
						// Types have changed
						FlowGraph.LVal lv = new FlowGraph.LocalVar(s);
						FlowGraph.LVal olv = new FlowGraph.LocalVar(s, oT);
						FlowGraph.Assign asgn = new FlowGraph.Assign(lv, new FlowGraph.Cast(t, olv));
						Point nP = new Point(asgn, p.source(), p.line(), p.column());
						Triple<Point, Point, Expr> edge1 = new Triple<Point, Point, Expr>(pred, nP, trip.third());
						Triple<Point, Point, Expr> edge2 = new Triple<Point, Point, Expr>(nP, p, null);

						envToAdd.add(new Pair<Point, TypeMap>(nP, (TypeMap) map.clone()));

						toRem.add(to.iterator().next());
						toAdd.add(edge1);
						toAdd.add(edge2);
					}
				}

			}
		}
		for(Triple<Point, Point, Expr> t : toRem) {
			cfg.remove(t);
		}
		for(Triple<Point, Point, Expr> t : toAdd) {
			cfg.add(t);
		}
		
		for(Pair<Point, TypeMap> t : envToAdd) {
			inf.setEnvironmentAt(t.first(), t.second(), m);
		}
		
		toRem.clear();
		toAdd.clear();
		envToAdd.clear();
	}

	public String description() {
		return "Handles retyping of variables due to conditionals";
	}

}
