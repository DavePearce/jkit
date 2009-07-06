package wone.solver;

import java.util.*;

import wone.lang.*;

public final class Solver {
	private FourierMotzkinEliminator fme = new FourierMotzkinEliminator();
	
	/**
	 * <p>The purpose of this method is to accept a simple formula, and check that
	 * it is unsatisfiable.</p>
	 * 
	 * @return
	 */
	public boolean checkUnsatisfiable(Formula formula) {
		if (formula instanceof Bool) {
			Bool c = (Bool) formula;
			return !c.constant();
		} else if (formula instanceof Atom) {
			return false;
		} else if (formula instanceof Inequality || formula instanceof Equality) {			
			Stack<Formula> stack = new Stack<Formula>();
			stack.push(formula);			
			return checkUnsatisfiable(stack, new ArrayList<Literal>());
		} else if (formula instanceof Conjunct) {
			Conjunct conjuncts = (Conjunct) formula;
			Stack<Formula> stack = new Stack<Formula>();
			for (Formula d : conjuncts) {
				stack.push(d);
			}
			return checkUnsatisfiable(stack, new ArrayList<Literal>());
		} else if (formula instanceof Disjunct){
			Disjunct disjuncts = (Disjunct) formula;

			for (Formula d : disjuncts) {
				Stack<Formula> stack = new Stack<Formula>();
				stack.push(d);
				if (!checkUnsatisfiable(stack, new ArrayList<Literal>())) {
					return false;
				}
			}

			return true;
		} else if(formula instanceof Forall) {
			Forall all = (Forall) formula;
			// for the moment I don't do anything here.
			return false;
		} else {
			throw new RuntimeException("unknown formula encountered: " + formula);
		}
	}
	
	private boolean checkUnsatisfiable(Stack<Formula> stack,
			ArrayList<Literal> literals) {				
		
		// debug(stack);
		
		while(!stack.empty()) {			
			Formula f = stack.pop(); // heuristic could help here			
															
			if(f instanceof Atom) {
				if(unitPropagation((Atom)f,stack)) {
					// unit propagation encountered a contradiction.
					return true;
				}
			} else if(f instanceof Disjunct) {
				Disjunct disjuncts = (Disjunct) f;				
				for(Formula d : disjuncts) {
					Stack<Formula> cstack = (Stack<Formula>) stack.clone();
					cstack.push(d);					
					if (!checkUnsatisfiable(cstack, (ArrayList<Literal>) literals.clone())) {						
						return false;
					}									
				}
				return true;
			} else if (f instanceof Conjunct) {
				Conjunct conjuncts = (Conjunct) f;			
				for(Formula d : conjuncts) {				
					stack.push(d);				
				}						
			} else if(f instanceof Equality) {
				Equality eq = (Equality) f;
				if(eq.sign()) {
					Polynomial lhs = eq.lhs();
					Polynomial rhs = eq.rhs();
					literals.add(new Inequality(true,lhs,rhs));
					literals.add(new Inequality(true,rhs,lhs));
				} else {
					Polynomial lhs = eq.lhs();
					Polynomial rhs = eq.rhs();
					Inequality ieq1 = new Inequality(false,lhs,rhs);
					Inequality ieq2 = new Inequality(false,rhs,lhs);
					stack.push(ieq1.or(ieq2));
				}
			} else if(f instanceof Literal) {
				literals.add((Literal)f);				
			} 
		}
		
		// At this point, we have a list of literals to check satisfiability for
		
		// First off, try to test for unsatisfiability straight off the bat
		if(fme.checkUnsatisfiable(literals)) {
			return true;
		}
		
		// Second, apply the theory of uninterpreted functions
		uninterpretedFunctionElimination(stack,literals);
		if (!stack.isEmpty() && checkUnsatisfiable(stack, literals)) {
			return true;
		}
		
		// Ok, give up.
		return false;
	}
	
	/**
	 * 
	 * <p>The purpose of this method is to apply rules arising from the theory of 
	 * uninterpreted functions.</p>
	 */
	private void uninterpretedFunctionElimination(Stack<Formula> stack,
			ArrayList<Literal> literals) {		
		HashMap<String,ArrayList<Function>> fmap = new HashMap<String,ArrayList<Function>>();
				
		
		for(int i=0;i!=literals.size();) {
			Literal l = literals.get(i);
			if(l instanceof Function) {					
				Function f = (Function) l;
				literals.remove(i);
				ArrayList<Function> fs = fmap.get(f.function());
				if(fs == null) {
					fs = new ArrayList<Function>();
					fmap.put(f.function(),fs);
				}
				fs.add(f);
			} else {				
				i++;
			}
		}
		
		for(Map.Entry<String,ArrayList<Function>> e : fmap.entrySet()) {
			ArrayList<Function> fns = e.getValue();
			for(int i=0;i!=fns.size();++i) {
				for(int j=i+1;j<fns.size();++j) {
					Function fi = fns.get(i);
					Function fj = fns.get(j);
					
					if(fi.parameters().size() != fj.parameters().size()) {
						continue;
					}
					
					Formula params = new Bool(true);
					List<String> fi_params = fi.parameters();
					List<String> fj_params = fj.parameters();
					for (int k = 0; k != fi_params.size(); ++k) {
						String p1 = fi_params.get(k);
						String p2 = fj_params.get(k);
						params = params.and(new Equality(true, new Polynomial(
								p1), new Polynomial(p2)));						
					}										
					
					if (!fi.sign() && !fj.sign()) {
						// do nothing since i can't think of a situation where
						// this is a problem.
					} else if(!fi.sign() || !fj.sign()) {
						Equality eq = new Equality(false, new Polynomial(fi
								.variable()), new Polynomial(fj.variable()));						
						stack.push(eq.or(params.not()));
					} else {
						Equality eq = new Equality(true, new Polynomial(fi
								.variable()), new Polynomial(fj.variable()));
						stack.push(eq.or(params.not()));
					}
					
					// System.out.println("ADDED: " + ieq1.and(ieq2).or(params.not()));
					
				}
			}
		}
	}
	
	private boolean unitPropagation(Atom l, Vector<Formula> stack) {
		for(int i=0;i!=stack.size();++i) {
			Formula f = stack.get(i);			
			f = unitPropagation(l,f);			
			if(f.isFalse()) {
				return true; // contradiction
			} else {
				stack.set(i,f);
			}
		}
		return false;
	}
	
	private Formula unitPropagation(Atom l, Formula formula) {
		if (formula instanceof Bool) {
			return formula;
		} else if (formula instanceof Equality || formula instanceof Inequality) {			
			return formula;
		} else if (formula instanceof Atom) {
			Atom m = (Atom) formula;
			
			if(m.atom().equals(l.atom())) {
				return new Bool(m.sign() == l.sign()); 
			} else {
				return m;
			}			
		} else if (formula instanceof Conjunct) {
			Conjunct conjunct = (Conjunct) formula;
			Formula r = new Bool(true);
			for(Formula f : conjunct) {
				r = r.and(unitPropagation(l,f));				
			}
			return r;			
		} else {
			Disjunct disjunct = (Disjunct) formula;
			Formula r = new Bool(false);
			for(Formula f : disjunct) {
				r = r.or(unitPropagation(l,f));				
			}
			return r;
		}
	}
	
	private static void debug(Stack<Formula> stack) {		
		System.err.print("[");
		boolean firstTime = true;
		for(Formula i : stack) {
			if(!firstTime) {
				System.err.print(", ");
			}
			firstTime=false;
			System.err.print(i);
		}
		System.err.println("]");
	}
}
