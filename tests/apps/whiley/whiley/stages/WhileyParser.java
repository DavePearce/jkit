package whiley.stages;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import whiley.ast.*;
import whiley.ast.attrs.SourceAttr;
import whiley.ast.attrs.SyntacticElement;
import whiley.ast.exprs.*;
import whiley.ast.stmts.*;
import whiley.ast.types.*;
import whiley.util.*;

public class WhileyParser {
	private String filename;
	private String input;
	private int index;

	public WhileyParser(String filename) throws IOException {
		this(new FileReader(filename));
		this.filename = filename;
	}
	
	public WhileyParser(InputStream instream) throws IOException {
		this(new InputStreamReader(instream));
		this.filename = null;
	}
	
	public WhileyParser(Reader reader) throws IOException {
		BufferedReader in = new BufferedReader(reader);
		
		StringBuffer text = new StringBuffer();
		while (in.ready()) {
			text.append(in.readLine());
			text.append("\n");
		}
		input = text.toString();
		index = 0;
		this.filename = null;
	}
	
	/**
	 * The purpose of this method is to traverse the parse tree produced by
	 * ANTLR, and construct a simpler tree known as the "Abstract Syntax Tree".
	 * This can then be used more easily to syntax check and evaluate the
	 * program.
	 * 
	 * @param ast -
	 *            The parse tree returned by ANTLR
	 * @return A map from function names to the function definitions.
	 * 
	 */
	public WhileyFile read() {
		ArrayList<Function> decls = new ArrayList<Function>();
		
		while(index < input.length()) {
			parseWhiteSpace();
			Function f = parseFunction();
			decls.add(f);
			parseWhiteSpace();
		}
		
		return new WhileyFile(filename,decls);
	}
	
	private Function parseFunction() {
		int start = index;
		Type returnType = parseType();		
		String name = parseIdentifier();
		match("(");		
		
		// Now build up the parameter types
		List<Pair<Type,String>> paramTypes = new ArrayList<Pair<Type,String>>();
		boolean firstTime=true;
		while(index < input.length() && input.charAt(index) != ')') {
			if(!firstTime) {
				match(",");
			}
			firstTime=false;
			Type t = parseType();
			String n = parseIdentifier();
			paramTypes.add(new Pair(t,n));
			parseWhiteSpace();
		}
		
		match(")");
		
		// now, look to see if we have requires and ensures clauses.
		Pair<Condition,Condition> ppc = parseRequiresEnsures();
		
		List<Stmt> stmts = parseBlock();
		
		return new Function(name, returnType, paramTypes, ppc.first(), ppc
				.second(), stmts, new SourceAttr(filename, start,
						index - 1));
	}
	
	private List<Stmt> parseBlock() {		
		match("{");
		ArrayList<Stmt> stmts = new ArrayList<Stmt>();
		parseWhiteSpace();
		while(index < input.length() && input.charAt(index) != '}') {						
			stmts.add(parseStatement());			
			parseWhiteSpace();
		}
		match("}");
		return stmts;
	}
	
	private Pair<Condition,Condition> parseRequiresEnsures() {
		Condition preCondition = null;
		Condition postCondition = null;
		String token = lookahead(1);
		if(token.equals("requires")) {
			match("requires");
			parseWhiteSpace();
			preCondition = parseRealCondition();
			token = lookahead(1);
			if(token.equals(",")) {
				match(",");
				token = lookahead(1);
			}
		}
		
		if(token.equals("ensures")) {
			match("ensures");
			parseWhiteSpace();
			postCondition = parseRealCondition();
		}
		
		return new Pair(preCondition,postCondition);
	}
	
	private Stmt parseStatement() {
		parseWhiteSpace();		
		String token = lookahead(1);		
		if(token.equals("return")) {
			return parseReturn();
		} else if(token.equals("assert")) {
			return parseAssert();
		} else if(token.equals("print")) {
			return parsePrint();
		} else if(token.equals("if")) {			
			return parseIf();
		} else if(token.equals("while")) {			
			return parseWhile();
		} else if(lookahead(2).equals("(")) {
			// must be a method invocation
			// must be an assignment statement.
			Stmt s = parseInvoke();
			match(";");
			return s;
		} else if(isTypeStart(lookahead(1))) {
			return parseVarDecl();
		} else {
			// must be either an assignment statement
			return parseAssign();
		} 
	}
	
	private Invoke parseInvoke() {
		int start = index;
		String name = parseIdentifier();		
		match("(");
		boolean firstTime=true;
		ArrayList<Expression> args = new ArrayList<Expression>();
		while(index < input.length() && input.charAt(index) != ')') {
			if(!firstTime) {
				match(",");
			} else {
				firstTime=false;
			}			
			Expression e = parseMulDivExpression();
			args.add(e);
			
		}
		match(")");
		return new Invoke(name, null, args, new SourceAttr(filename, start,
				index - 1));
	}
	
	private Stmt parseReturn() {
		int start = index;
		match("return");		
		parseWhiteSpace();
		checkNotEof();
		Expression e = null;
		if(input.charAt(index) != ';') {			
			e = parseMulDivExpression();			
		} 		
		match(";");
		return new Return(e, new SourceAttr(filename,start,index-1));
	}
	
	private Stmt parseAssert() {
		int start = index;		
		match("assert");		
		parseWhiteSpace();
		checkNotEof();
		Condition e = parseRealCondition();
		match(";");
		return new Assertion(e, new SourceAttr(filename,start,index-1));
	}
	
	private Stmt parsePrint() {
		int start = index;			
		match("print");
		parseWhiteSpace();
		checkNotEof();
		Expression e = parseMulDivExpression();
		match(";");
		return new Print(e, new SourceAttr(filename,start,index-1));
	}
	
	private Stmt parseWhile() {
		int start = index;
		match("while");
		match("(");
		Condition c = parseRealCondition();
		match(")");
		Condition invariant = null;
		
		if(lookahead(1).equals("invariant")) {
			match("invariant");
			invariant = parseRealCondition();
		}
		
		List<Stmt> body = parseBlock();
		return new While(c, invariant, body, new SourceAttr(filename, start,
				index - 1));
	}
	
	private Stmt parseIf() {
		int start = index;
		match("if");		
		match("(");		
		Condition c = parseRealCondition();				
		match(")");				
		List<Stmt> tblk = parseBlock();				
		List<Stmt> fblk = null;
		if(lookahead(1).equals("else")) {
			match("else");
			fblk = parseBlock();
		}		
		return new IfElse(c,tblk,fblk, new SourceAttr(filename,start,index-1));
	}
	
	private Stmt parseVarDecl() {
		int start = index;
		Type type = parseType();			
		String var = parseIdentifier();
		match(";");
		return new VarDecl(type,var, new SourceAttr(filename,start,index-1));		
	}
		
	private Stmt parseAssign() {
		int start = index;
		// standard assignment
		Expression lhs = parseIndexTerm();
		if(!(lhs instanceof LVal)) {
			syntaxError("expecting lval, found " + lhs + ".", lhs);
		}		
		parseWhiteSpace();
		match("=");
		parseWhiteSpace();
		Expression rhs = parseCondition();
		match(";");
		return new Assign((LVal) lhs,rhs, new SourceAttr(filename,start,index-1));		
	}	
	
	private Expression parseCondition() {
		parseWhiteSpace();
		int start = index;		
		Expression c1 = parseConditionExpression();
		parseWhiteSpace();
		if(index < input.length() && input.charAt(index) == '&') {
			checkCondition(c1);
			match("&&");
			Condition c2 = parseRealCondition();			
			return new And((Condition)c1,c2, new SourceAttr(filename,start,index));
		} else if(index < input.length() && input.charAt(index) == '|') {
			checkCondition(c1);
			match("||");
			Condition c2 = parseRealCondition();
			return new Or((Condition)c1,c2, new SourceAttr(filename,start,index));
		} 
		return c1;		
	}
	
	private void checkCondition(Expression c) {
		if(!(c instanceof Condition)) {
			syntaxError("Bool expression expected.",c);
		}		
	}
	
	private Condition parseRealCondition() {
		Expression e = parseCondition();
		checkCondition(e);
		return (Condition)e;
	}
		
	private Expression parseConditionExpression() {
		parseWhiteSpace();
		int start = index;

		Expression lhs = parseMulDivExpression();
		parseWhiteSpace();
		if ((index + 1) < input.length() && input.charAt(index) == '<'
			&& input.charAt(index + 1) == '=') {
			match("<=");				
			Expression rhs = parseMulDivExpression();
			return new LessThanEquals(lhs, rhs, new SourceAttr(filename,start,index-1));
		} else if (index < input.length() && input.charAt(index) == '<') {
			match("<");				
			Expression rhs = parseMulDivExpression();
			return new LessThan(lhs, rhs, new SourceAttr(filename,start,index-1));
		} else if ((index + 1) < input.length() && input.charAt(index) == '>'
			&& input.charAt(index + 1) == '=') {
			match(">=");				
			Expression rhs = parseMulDivExpression();
			return new GreaterThanEquals(lhs, rhs, new SourceAttr(filename,start,index-1));
		} else if (index < input.length() && input.charAt(index) == '>') {
			match(">");
			Expression rhs = parseMulDivExpression();
			return new GreaterThan(lhs, rhs, new SourceAttr(filename,start,index-1));
		} else if ((index + 1) < input.length() && input.charAt(index) == '='
			&& input.charAt(index + 1) == '=') {
			match("==");			
			Expression rhs = parseMulDivExpression();
			return new Equals(lhs, rhs, new SourceAttr(filename,start,index-1));
		} else if ((index + 1) < input.length() && input.charAt(index) == '!'
			&& input.charAt(index + 1) == '=') {
			match("!=");
			Expression rhs = parseMulDivExpression();
			return new NotEquals(lhs, rhs, new SourceAttr(filename,start,index-1));
		} else if(lhs instanceof Variable) {
			return (Variable) lhs;
		} else if(lhs instanceof Not) {
			return (Not) lhs;
		} else {
			return lhs;
		}	
	}
	
	private Expression parseMulDivExpression() {
		int start = index;
		Expression lhs = parseAddSubExpression();
		
		parseWhiteSpace();				 
		
		if(index < input.length() && input.charAt(index) == '*') {
			match("*");
			Expression rhs = parseMulDivExpression();
			return new Mul(lhs,rhs, new SourceAttr(filename,start,index));	
		} else if(index < input.length() && input.charAt(index) == '/') {
			match("/");
			Expression rhs = parseMulDivExpression();
			return new Div(lhs,rhs, new SourceAttr(filename,start,index));
		}
		
		return lhs;
	}
	
	private Expression parseAddSubExpression() {
		int start = index;
		Expression lhs = parseIndexTerm();
		
		parseWhiteSpace();				 
		
		if(index < input.length() && input.charAt(index) == '+') {
			match("+");
			Expression rhs = parseAddSubExpression();
			return new Add(lhs,rhs, new SourceAttr(filename,start,index));	
		} else if(index < input.length() && input.charAt(index) == '-') {
			match("-");
			Expression rhs = parseAddSubExpression();
			return new Sub(lhs,rhs, new SourceAttr(filename,start,index));
		}
		
		return lhs;
	}
	
	private Expression parseIndexTerm() {
		int start = index;
		Expression lhs = parseTerm();
		while(lookahead(1).equals("[")) {
			match("[");
			Expression rhs = parseMulDivExpression();
			match("]");
			lhs =  new ListAccess(lhs, rhs, new SourceAttr(filename, start,
					index - 1));
		}
		
		return lhs;		
	}
	
	private Expression parseTerm() {
		parseWhiteSpace();
		
		checkNotEof();		
		
		int start = index;
		if(input.charAt(index) == '(') {
			match("(");
			Expression e = parseMulDivExpression();
			match(")");
			return e;
		} else if (Character.isJavaIdentifierStart(input.charAt(index))
				&& lookahead(2).equals("(")) {
			// must be a method invocation
			// must be an assignment statement.
			return parseInvoke();
		} else if (lookahead(1).equals("true")) {
			match("true");			
			return new Constant(true,
					new SourceAttr(filename, start, index - 1));
		} else if (lookahead(1).equals("false")) {	
			match("false");
			return new Constant(false, new SourceAttr(filename, start,
					index - 1));
		} else if (Character.isJavaIdentifierStart(input.charAt(index))) {
			return new Variable(parseIdentifier(), new SourceAttr(filename, start,
					index - 1));			
		} else if (Character.isDigit(input.charAt(index))) {
			return parseNumber();
		} else if (input.charAt(index) == '-') {
			return parseNegation();
		} else if (input.charAt(index) == '|') {
			return parseArrayLength();
		} else if (input.charAt(index) == '[') {
			return parseArrayVal();
		} else if (index < input.length() && input.charAt(index) == '!') {
			match("!");
			return new Not(parseRealCondition(), new SourceAttr(filename,
					start, index - 1));
		} else {
			syntaxError("unrecognised term.");
			return null;
		}
	}
	
	private Expression parseArrayVal() {
		int start = index;
		ArrayList<Expression> exprs = new ArrayList<Expression>();
		match("[");
		boolean firstTime = true;
		while(!lookahead(1).equals("]")) {
			if(!firstTime) {
				match(",");
			}
			firstTime=false;
			exprs.add(parseCondition());
		}
		match("]");
		return new ListVal(exprs,new SourceAttr(filename,
					start, index - 1));
	}
	
	private Expression parseArrayLength() {
		int start = index;
		match("|");
		Expression e = parseIndexTerm();
		match("|");
		return new LengthOf(e, new SourceAttr(filename, start, index - 1));
	}

	private Expression parseNegation() {
		int start = index;
		match("-");
		Expression e = parseIndexTerm();
		if(e instanceof Constant) {
			Constant ne = (Constant) e;
			Object o = ne.value();
			if(o instanceof BigInteger) {
				java.math.BigInteger bi = (java.math.BigInteger) o;
				return new Constant(bi.negate(), new SourceAttr(filename,start,index));
			} else if(o instanceof BigRational) {
				BigRational br = (BigRational) o;
				return new Constant(br.negate(), new SourceAttr(filename,start,index));
			} else {
				syntaxError("unknown constant " + o,e);
				return null;
			}
		} else {
			return new Negate(e, new SourceAttr(filename,start,index));
		}
	}
	
	private String parseIdentifier() {
		parseWhiteSpace();		
		int start = index;
		if (index >= input.length()
				|| !Character.isJavaIdentifierStart(input.charAt(index))) {
			syntaxError("identifier expected.");
		}
		index++; // skip past the identifier start
		while (index < input.length()
				&& Character.isJavaIdentifierPart(input.charAt(index))) {
			index = index + 1;
		}
		return input.substring(start, index);
	}
	
	private Expression parseNumber() {
		int start = index;
		while (index < input.length() && Character.isDigit(input.charAt(index))) {
			index = index + 1;
		}
		if(index < input.length() && input.charAt(index) == '.') {
			index = index + 1;			
			while (index < input.length() && Character.isDigit(input.charAt(index))) {
				index = index + 1;
			}			
			BigRational r = new BigRational(input.substring(start, index));
			return new Constant(r, new SourceAttr(filename, start, index));
		} else {
			BigInteger r = new BigInteger(input.substring(start, index));
			return new Constant(r, new SourceAttr(filename, start, index));
		}
	}
	

	private Type parseType() {		
		parseWhiteSpace();		
		String id = parseIdentifier();
		Type t;
		if(id.equals("int")) {
			t = new IntType();
		} else if(id.equals("real")) {
			t = new RealType();
		} else if(id.equals("void")) {
			t = new VoidType();
		} else if(id.equals("bool")) {
			t = new BoolType();
		} else {
			syntaxError("unknown type encountered");
			return null; // unreachable
		}
		
		while(lookahead(1).equals("[")) {
			match("[]");
			t = new ListType(t);
		}
		
		return t;
	}		
	
	private boolean isTypeStart(String token) {
		return token.equals("int") || token.equals("void")
				|| token.equals("bool") || token.equals("real");
	}
	
	private void match(String x) {
		parseWhiteSpace();
		if((input.length()-index) < x.length()) {
			syntaxError("expecting " + x);			
		}
		if(!input.substring(index,index+x.length()).equals(x)) {
			syntaxError("expecting " + x);
		}		
		String token = input.substring(index,index+x.length());
		if(!token.equals(x)) {
			syntaxError("expecting " + x);
		}
		index += x.length();
	}
	
	private void parseWhiteSpace() {
		while (index < input.length()
				&& Character.isWhitespace(input.charAt(index))) {			
			index = index + 1;
		}
	}
		
	private String lookahead(int k) {		
		StringBuilder str = null;
		int end = index;
		for (int i = 0; i < k; ++i) {
			str = new StringBuilder();
		
			while (end < input.length()
					&& Character.isWhitespace(input.charAt(end))) {			
				end = end + 1;
			}					
			
			if (end < input.length() && isOperatorStart(input.charAt(end))) {
				str = str.append(input.charAt(end));
				break;
			} else {
				while (end < input.length()
						&& !Character.isWhitespace(input.charAt(end))
						&& !isOperatorStart(input.charAt(end))) {
					str = str.append(input.charAt(end));
					if (isKeyword(str.toString())) {
						break;
					}
					end = end + 1;
				}
			}
		}
		return str.toString();
	}
	
	private static boolean isKeyword(String str) {
		return str.equals("if") || str.equals("while") || str.equals("return")
				|| str.equals("assert") || str.equals("for");
	}

	private static final char[] operators = { '+', '-', '*', '/', '(', ')',
			'<', '>', '}', '{', ';', '[', ']', '|'};
	
	private static boolean isOperatorStart(char c) {
		for(char op : operators) {
			if(c == op) { 
				return true;
			}
		}
		return false;
	}			
	
	private void checkNotEof() {
		if(index >= input.length()) {
			syntaxError("unexpected end-of-file");
		}
	}
	
	private void syntaxError(String msg) {
		throw new SyntaxError(msg, filename, index, index);
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
