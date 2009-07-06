package whiley.stages;

import java.util.*;
import whiley.util.*;
import whiley.ast.Function;
import whiley.ast.attrs.*;
import whiley.ast.exprs.*;
import whiley.ast.stmts.*;
import whiley.ast.types.*;


/**
 * <p>
 * The purpose of this class is to check that all variables are defined before
 * being used. For example:
 * </p>
 * 
 * <pre>
 * int f() {
 * 	int z;
 * 	return z + 1;
 * }
 * </pre>
 * 
 * <p>
 * In the above example, variable z is used in the return statement before it
 * has been defined any value. This is considered a syntax error in whiley.
 * </p>
 * @author djp
 * 
 */
public class VarDefChecker {
	public void verify(Function f) {
		HashSet<String> definitions = new HashSet<String>();
		
		for(Pair<Type,String> p : f.getParameterTypes()) {
			definitions.add(p.second());
		}
		
		if(f.getPrecondition() != null) {
			check(f.getPrecondition(),definitions);
		}
		
		if(f.getPostcondition() != null) {
			if(!(f.getReturnType() instanceof VoidType)) {
				definitions.add("$");
				check(f.getPostcondition(),definitions);
				definitions.remove("$");
			} else {
				check(f.getPostcondition(),definitions);
			}
		}
		
		
		for(Stmt s : f.getStatements()) {
			check(s,definitions);
		}
	}
	
	/**
     * This method checks the given statement to see whether there are any
     * variables which are used that have not been defined. The definitions set
     * contains the set of variables which have been defined prior to this
     * statement.
     * 
     * @param statement
     * @param definitions
     * @param function
     */
	protected void check(Stmt statement, HashSet<String> definitions) {
		if (statement instanceof Skip || statement instanceof Read) {
			// nothing to do here.
		} else if (statement instanceof Print) {
			check((Print) statement, definitions);
		} else if (statement instanceof Assign) {
			check((Assign) statement, definitions);
		} else if (statement instanceof IfElse) {
			check((IfElse) statement, definitions);
		} else if (statement instanceof While) {
			check((While) statement, definitions);
		} else if (statement instanceof Return) {
			check((Return) statement, definitions);
		} else if (statement instanceof Assertion) {
			check((Assertion) statement, definitions);
		} else if (statement instanceof Invoke) {
			check((Invoke) statement, definitions);
		} else if (statement instanceof VarDecl) {
			check((VarDecl) statement, definitions);
		} else {
			syntaxError("Unknown statement encountered: " + statement,
					statement);
		}
	}
	
	protected void check(VarDecl s, HashSet<String> definitions) {		
		// actually don't do anything here, as currently variable definitions
        // cannot assign variables as well.
	}
	
	protected void check(Print s, HashSet<String> definitions) {
		check(s.getExpression(),definitions);				
	}

	protected void check(Assign s, HashSet<String> definitions) {
		check(s.rhs(),definitions);
		if(s.lhs() instanceof Variable) {
			Variable v = (Variable) s.lhs();
			definitions.add(v.getVariable());
		}
		check(s.lhs(),definitions);						
	}

	protected void check(IfElse statement, HashSet<String> definitions) {
		check(statement.getCondition(),definitions);
		HashSet<String> trueDefinitions = (HashSet<String>) definitions.clone();
		for(Stmt st : statement.getTrueBranch()) {
			check(st,trueDefinitions);	
		}
		
		if(statement.getFalseBranch() != null) {			
			HashSet<String> falseDefinitions = (HashSet<String>) definitions.clone();
			
			for(Stmt st : statement.getFalseBranch()) {
				check(st,falseDefinitions);	
			}
			// At this point, we need to compute the intersection of definitions
            // coming from the true and false branches.
			for(String v : trueDefinitions) {
				if(falseDefinitions.contains(v)) {
					definitions.add(v);
				}
			}			
		} 
	}

	protected void check(While s, HashSet<String> definitions) {
		check(s.condition(),definitions);
		HashSet<String> bodyDefinitions = (HashSet<String>) definitions.clone();
		for(Stmt st : s.body()) {
			check(st,bodyDefinitions);	
		}		
	}
	
	protected void check(Return s, HashSet<String> definitions) {
		if(s.getExpression() != null) {
			check(s.getExpression(),definitions);
		}
	}
	
	protected void check(Assertion s, HashSet<String> definitions) {
		check(s.getExpression(),definitions);
	}
	
	protected void check(Invoke s, HashSet<String> definitions) {
		
		List<Expression> args = s.getArguments();
		for(int i=0;i!=args.size();++i) {
			Expression arg = args.get(i);
			check(arg,definitions);			
		}		
	}
	
	/**
     * This method determines what variables are used in the expression e.
     * 
     * @param e
     * @return
     */
	protected void check(Expression e, HashSet<String> definitions) {		
		if(e instanceof Constant) {
			// do nothing.
		} else if(e instanceof Variable) {
			check((Variable)e, definitions);						
		} else if(e instanceof BinOp) {
			check((BinOp)e, definitions);
		} else if(e instanceof Negate) {
			check((Negate)e, definitions);
		} else if(e instanceof Invoke) {
			check((Invoke)e, definitions);				
		} else if(e instanceof Not) {
			check((Not)e, definitions);
		} else if(e instanceof LengthOf) {
			check((LengthOf)e, definitions);
		} else if(e instanceof ListVal) {
			check((ListVal)e, definitions);
		} else if(e instanceof ListAccess) {
			check((ListAccess)e, definitions);
		} else {
			syntaxError("Unknown expression encountered.",e);			
		}		
	}
	
	protected void check(ListAccess e, HashSet<String> definitions) {
		check(e.index(),definitions);
		check(e.source(),definitions);
	}
	
	protected void check(ListVal e, HashSet<String> definitions) {
		for(Expression v : e.getValues()) {
			check(v,definitions);
		}
	}
	
	protected void check(LengthOf e, HashSet<String> definitions) {
		check(e.getExpr(),definitions);
	}
	
	protected void check(Not e, HashSet<String> definitions) {
		check(e.getCondition(),definitions);
	}	
	
	protected void check(Negate e, HashSet<String> definitions) {
		check(e.getExpr(),definitions);
	}
	
	protected void check(BinOp e, HashSet<String> definitions) {	
		check(e.getLeftExpr(),definitions);
		check(e.getRightExpr(),definitions);
	}
	
	protected void check(Variable e, HashSet<String> definitions) {		
		if(!definitions.contains(e.getVariable())) {
			syntaxError("variable used before defined",e);
		}
	}
		
	private static void syntaxError(String msg, SyntacticElement elem) {
		int start = -1;
		int end = -1;
		String filename = null;
			
		SourceAttr attr = (SourceAttr) elem.attribute(SourceAttr.class);
		if(attr != null) {
			start=attr.start();
			end=attr.end();
			filename = attr.filename();
		}
		
		throw new SyntaxError(msg, filename, start, end);
	}
}
