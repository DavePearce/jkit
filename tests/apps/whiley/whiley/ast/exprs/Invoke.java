package whiley.ast.exprs;

import java.util.*;

import whiley.ast.*;
import whiley.ast.attrs.Attribute;
import whiley.ast.attrs.SyntacticElementImpl;
import whiley.ast.stmts.Stmt;
import whiley.util.SyntaxError;

public class Invoke extends SyntacticElementImpl implements Stmt, Expression {
	private String name;
	private Function function;
	private List<Expression> arguments;
	
	public Invoke(String name, Function function, List<Expression> arguments,
			Attribute... attributes) {
		super(attributes);
		this.name = name;
		this.function = function;
		this.arguments = arguments;
	}
	
	public void bind(Map<String,Function> fmap) {
		this.function = fmap.get(name);
				
		for(Expression e : arguments) {
			e.bind(fmap);
		}		
	}
	
	public String getName() {
		return name;
	}
	
	public Function getFunction() {
		return function;
	}
	
	public List<Expression> getArguments() {
		return arguments;
	}	
	
	public String toString() {
		String r = name + "(";
		boolean firstTime = true;
		for(Expression e : arguments) {
			if(!firstTime) {
				r += ",";
			}
			firstTime=false;
			r += e;
		}
		return r + ")";
		
	}
}
