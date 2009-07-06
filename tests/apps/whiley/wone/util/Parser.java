package wone.util;

import java.io.*;
import java.util.*;
import java.math.*;

import wone.lang.*;

public class Parser {
	private String filename;
	private String input;
	private int index;
	
	public Parser(File file) throws IOException {		
		this.filename = file.getPath();
		BufferedReader in = new BufferedReader(new FileReader(filename));
		
		StringBuffer text = new StringBuffer();
		while (in.ready()) {
			text.append(in.readLine());
			text.append("\n");
		}
		input = text.toString();
		index = 0;		
	}
	
	public Parser(String input) {		
		this.filename = "";		
		this.input = input;
		index = 0;		
	}
	
	public Formula readFormula() {
		return parseConjunctDisjunct();
	}
	
	private Formula parseConjunctDisjunct() {
		Formula c1 = parsePredicate();
		
		parseWhiteSpace();				 
		
		if(index < input.length() && input.charAt(index) == '&') {
			match("&&");
			Formula c2 = parseConjunctDisjunct();			
			return c1.and(c2);
		} else if(index < input.length() && input.charAt(index) == '|') {
			match("||");
			Formula c2 = parseConjunctDisjunct();			
			return c1.or(c2);
		}
		return c1;
	}
		
	private Formula parsePredicate() {
		parseWhiteSpace();
		
		int start = index;
		
		String lookahead = lookahead();
		
		if(lookahead.equals("(")) {
			match("(");			
			Formula r = parseConjunctDisjunct();			
			match(")");
			return r;
		} else if(lookahead.equals("!")) {
			match("!");			
			return parsePredicate().not();			
		} else if(lookahead.equals("forall")) {
			match("forall");
			lookahead = lookahead();			
			 
			HashSet<String> vars = new HashSet<String>();
			boolean firstTime=true;
			while(firstTime || lookahead.equals(",")) {
				if(!firstTime) {
					match(",");
					lookahead = lookahead();
				}
				firstTime=false;			
				match(lookahead);				
				vars.add(lookahead);				
				lookahead = lookahead();
			}
			
			match("[");
			Formula f = parseConjunctDisjunct();
			match("]");
			return new Forall(vars,f);
		} else if(lookahead.equals("exists")) {
			match("exists");
			lookahead = lookahead();
			HashSet<String> vars = new HashSet<String>();
			boolean firstTime=true;
			while(firstTime || lookahead.equals(",")) {
				if(!firstTime) {
					match(",");
					lookahead = lookahead();
				}
				firstTime=false;
				match(lookahead);
				vars.add(lookahead);				
				lookahead = lookahead();
			}
			
			match("[");
			Formula f = parseConjunctDisjunct();
			match("]");
			return new Exists(vars,f);
		} else if(lookahead(2).equals("(")) {
			// this indicates a function application
			String fun = lookahead;
			match(lookahead);
			match("(");
			ArrayList<String> params = new ArrayList<String>();
			lookahead = lookahead();
			boolean firstTime = true;
			while(!lookahead.equals(")")) {
				if(!firstTime) {
					match(",");
					lookahead = lookahead();
				}
				firstTime=false;
				match(lookahead);
				params.add(lookahead);
				lookahead = lookahead();
			}
			match(")");
			boolean sign;
			if(lookahead().equals("!")) {
				match("!=");
				sign=false;
			} else {
				match("==");
				sign=true;
			}
			lookahead = lookahead();
			match(lookahead);
			return new Function(sign,fun,lookahead,params);
		}
			
		Rational lhs = parseExpression();
		
		parseWhiteSpace();				 
		
		if ((index + 1) < input.length() && input.charAt(index) == '<'
				&& input.charAt(index + 1) == '=') {
			match("<=");
			Rational rhs = parseExpression();			
			return new Inequality(true,lhs, rhs);
		} else if (index < input.length() && input.charAt(index) == '<') {
			match("<");
			Rational rhs = parseExpression();
			return new Inequality(false,rhs, lhs);
		} else if ((index + 1) < input.length() && input.charAt(index) == '>'
				&& input.charAt(index + 1) == '=') {
			match(">=");
			Rational rhs = parseExpression();
			return new Inequality(true,rhs, lhs);
		} else if (index < input.length() && input.charAt(index) == '>') {
			match(">");
			Rational rhs = parseExpression();
			return new Inequality(false,lhs, rhs);
		} else if ((index + 1) < input.length() && input.charAt(index) == '='
				&& input.charAt(index + 1) == '=') {
			match("==");
			Rational rhs = parseExpression();
			return new Equality(true,lhs,rhs);
			// return new Equality(true,lhs, rhs);
		} else if ((index + 1) < input.length() && input.charAt(index) == '!'
				&& input.charAt(index + 1) == '=') {
			match("!=");
			Rational rhs = parseExpression();
			return new Equality(false,lhs, rhs);			
		} else {
			if (lhs.numerator().terms().size() != 1 || lhs.isConstant()
					|| !lhs.denominator().equals(Polynomial.ONE)) {
				throw new SyntaxError("syntax error", filename, start,
						index - 1);
			}
			Term t = lhs.numerator().iterator().next();
			if (!t.coefficient().equals(BigInteger.ONE)
					|| t.variables().size() != 1) {
				throw new SyntaxError("syntax error", filename, start,
						index - 1);
			}
			return new Atom(true, t.variables().get(0));
		} 					
	}
	
	private Rational parseExpression() {
		int start = index;
		Rational lhs = parseAddSubExpression();
		
		parseWhiteSpace();				 
		
		if(index < input.length() && input.charAt(index) == '*') {
			match("*");
			Rational rhs = parseExpression();
			return lhs.multiply(rhs);			
		} else if(index < input.length() && input.charAt(index) == '/') {
			throw new SyntaxError("Support for divide is lacking", filename,
					start, index - 1);
		}
		
		return lhs;
	}
	
	private Rational parseAddSubExpression() {
		Rational lhs = parseTerm();
		
		parseWhiteSpace();				 
		
		if(index < input.length() && input.charAt(index) == '+') {
			match("+");
			Rational rhs = parseAddSubExpression();
			return lhs.add(rhs);	
		} else if(index < input.length() && input.charAt(index) == '-') {
			match("-");
			Rational rhs = parseAddSubExpression();
			return lhs.subtract(rhs);
		}
		
		return lhs;
	}
	
	private Rational parseTerm() {
		parseWhiteSpace();
		
		int start = index;
		if (index < input.length() && input.charAt(index) == '(') {
			match("(");
			Rational e = parseExpression();
			match(")");
			return e;
		} else if (index < input.length()
				&& Character.isJavaIdentifierStart(input.charAt(index))) {
			return new Rational(parseIdentifier());
		} else if (index < input.length()
				&& Character.isDigit(input.charAt(index))) {
			return parseNumber();
		} else if (input.charAt(index) == '-') {
			return parseNegation();
		} else {
			throw new SyntaxError("syntax error", filename, start, index);
		}
	}
	
	private Rational parseNegation() {		
		match("-");
		Rational e = parseTerm();		
		return e.negate();
	}
	
	private String parseIdentifier() {		
		String id = Character.toString(input.charAt(index));
		index++; // skip past the identifier start
		while(index < input.length() && Character.isJavaIdentifierPart(input.charAt(index))) {
			id += Character.toString(input.charAt(index));
			index = index + 1;			
		}
		return id;
	}
	
	private Rational parseNumber() {
		int start = index;
		while (index < input.length() && Character.isDigit(input.charAt(index))) {
			index = index + 1;
		}
		if(index < input.length() && input.charAt(index) == '.') {
			index = index + 1;
			int start2 = index;
			while (index < input.length() && Character.isDigit(input.charAt(index))) {
				index = index + 1;
			}
			String lhs = input.substring(start, start2-1);
			String rhs = input.substring(start2, index);
			lhs = lhs + rhs;
			BigInteger bottom = BigInteger.valueOf(10);
			bottom = bottom.pow(rhs.length());
			return new Rational(new BigInteger(lhs),bottom);
		} else {
			return new Rational(new BigInteger(input.substring(start, index)));
		}
	}
	
	private void match(String x) {
		parseWhiteSpace();
		
		if((input.length()-index) < x.length()) {
			throw new SyntaxError("expecting " + x,filename,index,index);
		}
		if(!input.substring(index,index+x.length()).equals(x)) {
			throw new SyntaxError("expecting " + x,filename,index,index);			
		}
		index += x.length();
	}

	private String lookahead() {
		return lookahead(1);
	}
	
	private String lookahead(int k) {
		int end = index;

		for (int i = 1; i != k; ++i) {
			// first, skip whitespace
			while (end < input.length()
					&& Character.isWhitespace(input.charAt(end))) {
				end = end + 1;
			}
			// skip this token
			end += lookaheadFrom(end).length();
		}

		return lookaheadFrom(end);
	}
	
	private String lookaheadFrom(int start) {		
		StringBuilder str = new StringBuilder();
		int end = start;
	
		while (end < input.length()
				&& Character.isWhitespace(input.charAt(end))) {
			end = end + 1;
		}					

		if (end < input.length() && isOperatorStart(input.charAt(end))) {
			str = str.append(input.charAt(end));
		} else {
			while (end < input.length()
					&& !Character.isWhitespace(input.charAt(end))
					&& !isOperatorStart(input.charAt(end))) {
				str = str.append(input.charAt(end));
				end = end + 1;
			}
		}	
		return str.toString();
	}	

	private static final char[] operators = { '+', '-', '*', '/', '(', ')',
			'<', '>', '}', '{', ';', '[', ']', '|', '!', '=', ','};
	
	private static boolean isOperatorStart(char c) {
		for(char op : operators) {
			if(c == op) { 
				return true;
			}
		}
		return false;
	}			
	
	private void parseWhiteSpace() {
		while (index < input.length()
				&& Character.isWhitespace(input.charAt(index))) {			
			index = index + 1;
		}
	}
}
