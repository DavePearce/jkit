package jkit.java;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.*;

import jkit.compiler.ClassTable;
import jkit.compiler.SyntaxError;
import jkit.java.JavaFileReader.Block;
import jkit.java.JavaFileReader.ClassScope;
import jkit.java.JavaFileReader.LoopScope;
import jkit.java.JavaFileReader.MethodScope;
import jkit.java.JavaFileReader.Scope;
import jkit.java.JavaFileReader.SwitchScope;
import jkit.jkil.Clazz;
import jkit.jkil.Field;
import jkit.jkil.FlowGraph;
import jkit.jkil.Type;
import jkit.jkil.FlowGraph.ArrayIndex;
import jkit.jkil.FlowGraph.ArrayVal;
import jkit.jkil.FlowGraph.Assign;
import jkit.jkil.FlowGraph.BinOp;
import jkit.jkil.FlowGraph.BoolVal;
import jkit.jkil.FlowGraph.Cast;
import jkit.jkil.FlowGraph.CharVal;
import jkit.jkil.FlowGraph.ClassAccess;
import jkit.jkil.FlowGraph.ClassVal;
import jkit.jkil.FlowGraph.Deref;
import jkit.jkil.FlowGraph.DoubleVal;
import jkit.jkil.FlowGraph.Expr;
import jkit.jkil.FlowGraph.FloatVal;
import jkit.jkil.FlowGraph.InstanceOf;
import jkit.jkil.FlowGraph.IntVal;
import jkit.jkil.FlowGraph.Invoke;
import jkit.jkil.FlowGraph.LVal;
import jkit.jkil.FlowGraph.LocalVar;
import jkit.jkil.FlowGraph.LocalVarDef;
import jkit.jkil.FlowGraph.LongVal;
import jkit.jkil.FlowGraph.New;
import jkit.jkil.FlowGraph.NullVal;
import jkit.jkil.FlowGraph.Point;
import jkit.jkil.FlowGraph.Return;
import jkit.jkil.FlowGraph.StringVal;
import jkit.jkil.FlowGraph.TernOp;
import jkit.jkil.FlowGraph.Throw;
import jkit.jkil.FlowGraph.UnOp;
import jkit.util.Pair;
import jkit.util.Triple;

import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;

public class JavaFileReader2 {
	
	private Tree ast;
	
	/**
     * Create a JavaFileReader from a file.
     * 
     * @param file
     *            the filename to read from.
     * 
     * @param loader
     *            the class loader to use for resolving types
     * 
     * @throws IOException
     */
	public JavaFileReader2(String file) throws IOException {
		CommonTokenStream tokenStream = new CommonTokenStream(new JavaLexer(
				new ANTLRFileStream(file)));

		JavaParser parser = new JavaParser(tokenStream);

		try {
			ast = (Tree) parser.compilationUnit().getTree();
			// printTree(ast, 0, -1);
		} catch (RecognitionException e) {
		}
	}
	
	/**
     * Create a JavaFileReader from a general Reader.
     * 
     * @param r
     *            the reader to read from
     * 
     * @throws IOException
     */
	public JavaFileReader2(Reader r) throws IOException {
		CommonTokenStream tokenStream = new CommonTokenStream(new JavaLexer(
				new ANTLRReaderStream(r)));

		JavaParser parser = new JavaParser(tokenStream);
		try {
			ast = (Tree) parser.compilationUnit().getTree();
		} catch (RecognitionException e) {
		}
	}

	/**
     * Create a JavaFileReader from a general InputStream
     * 
     * @param in
     *            the input stream to read from
     * 
     * @throws IOException
     */
	public JavaFileReader2(InputStream in) throws IOException {
		CommonTokenStream tokenStream = new CommonTokenStream(new JavaLexer(
				new ANTLRInputStream(in)));
		JavaParser parser = new JavaParser(tokenStream);
		try {
			ast = (Tree) parser.compilationUnit().getTree();			
		} catch (RecognitionException e) {
		}
	}
	
	public JavaFile read() {
		
		ArrayList<JavaFile.Clazz> classes = new ArrayList<JavaFile.Clazz>();
		ArrayList<String> imports = new ArrayList<String>();
		String pkg = null;
		
		// Read top declarations first.
		outer : for (int i = 0; i != ast.getChildCount(); ++i) {
			Tree c = ast.getChild(i);
			switch (c.getType()) {
				case IMPORT : {
					// rebuild import string
					String filter = "";
					for (int j = 0; j != c.getChildCount(); ++j) {
						if (j != 0) {
							filter += ".";
						}
						filter += c.getChild(j).getText();
					}
					// make sure later imports get higher priority
					// in the search.
					imports.add(0, filter);
					break;
				}
				case PACKAGE :
					for (int j = 0; j != c.getChildCount(); ++j) {
						if (j == 0) {
							pkg = c.getChild(j).getText();
						} else {
							pkg += "." + c.getChild(j).getText();
						}
					}
					break;
				default :
					break outer;
			}
		}
		
		for (int i = 0; i != ast.getChildCount(); ++i) {
			Tree c = ast.getChild(i);
			switch (c.getType()) {
				case CLASS :
				case INTERFACE :
					classes.add(parseClass(c));
					break;				
			}
		}
		
		return new JavaFile(pkg,imports,classes);
	}
	
	protected JavaFile.Clazz parseClass(Tree decl) {
		int idx = 0;
		int modifiers = 0;
		if (decl.getChild(idx).getType() == MODIFIERS) {
			modifiers = parseModifiers(decl.getChild(0));
			idx++;
		}
		if (decl.getType() == INTERFACE) {
			modifiers |= Modifier.INTERFACE;
		}
		String name = decl.getChild(idx++).getText();
		
		// ====================================================================
		// ====================== PARSE EXTENDS CLAUSE ========================
		// ====================================================================

		JavaFile.Type superclass = null;
		if (idx < decl.getChildCount() && decl.getChild(idx).getType() == EXTENDS) {
			superclass = parseType(decl.getChild(idx++).getChild(0));
		}
		
		// ====================================================================
		// ===================== PARSE IMPLEMENTS CLAUSE ======================
		// ====================================================================

		ArrayList<JavaFile.Type> interfaces = new ArrayList<JavaFile.Type>();
		if (idx < decl.getChildCount()
				&& decl.getChild(idx).getType() == IMPLEMENTS) {
			Tree ch = decl.getChild(idx++);
			for (int i = 0; i != ch.getChildCount(); ++i) {
				interfaces.add(parseType(ch.getChild(i)));
			}
		}
		
		// ====================================================================
		// ======================== PARSE DECLARATIONS ========================
		// ====================================================================
		
		ArrayList<JavaFile.Declaration> declarations = new ArrayList<JavaFile.Declaration>();

		for (int i = idx; i < decl.getChildCount(); ++i) {
			Tree child = decl.getChild(i);
			switch(child.getType()) {
			case FIELD:
				declarations.addAll(parseField(child));
				break;
			case METHOD:
				declarations.add(parseMethod(child));
				break;
			case CLASS:				
			case INTERFACE:
				declarations.add(parseClass(child));
				break;				
			case BLOCK:
				break;
			}				
		}
		
		return new JavaFile.Clazz(modifiers,name,superclass,interfaces,declarations);
	}
	
	protected JavaFile.Method parseMethod(Tree method) {
		
		// === TYPE MODIFIERS ===

		int modifiers = 0;
		int idx = 0;
		if (method.getChild(idx).getType() == MODIFIERS) {
			modifiers = parseModifiers(method.getChild(0));
			idx++;
		}

		// === TYPE ARGUMENTS ===

		ArrayList<JavaFile.Type> typeArgs = new ArrayList<JavaFile.Type>();
		while (method.getChild(idx).getType() == TYPE_PARAMETER) {			
			typeArgs.add(parseType(method.getChild(idx++)));			
		}

		String name = method.getChild(idx++).getText();
		
		JavaFile.Type returnType = parseType(method.getChild(idx++));
		
		// === FORMAL PARAMETERS ===

		ArrayList<Pair<String,JavaFile.Type> > params = new ArrayList<Pair<String,JavaFile.Type>>();
		
		while (idx < method.getChildCount()
				&& method.getChild(idx).getType() == PARAMETER) {
			Tree c = method.getChild(idx);
			JavaFile.Type t = parseType(c.getChild(0));
			String n = c.getChild(1).getText();

			for (int i = 2; i < c.getChildCount(); i = i + 2) {
				t.setDims(t.dims()+1);
			}

			params.add(new Pair(n,t));
			idx++;
		}
		
		// === THROWS CLAUSE ===

		ArrayList<JavaFile.Type> exceptions = new ArrayList<JavaFile.Type>();

		if (idx < method.getChildCount()
				&& method.getChild(idx).getType() == THROWS) {
			Tree tt = method.getChild(idx++);
			for (int i = 0; i != tt.getChildCount(); ++i) {
				exceptions.add(parseType(tt.getChild(i)));
			}
		}

		// === METHOD BODY ===
		
		JavaFile.Block block = null;
		
		if (idx < method.getChildCount()
				&& method.getChild(idx).getType() == BLOCK) {
			// do nothing
			block = parseBlock(method.getChild(idx));
		}
		
		return new JavaFile.Method(modifiers,name,returnType,params,exceptions,block);
	}
	
	protected List<JavaFile.Field> parseField(Tree field) {
		assert field.getType() == FIELD;

		ArrayList<JavaFile.Field> fields = new ArrayList<JavaFile.Field>();

		// === MODIFIERS ===
		int modifiers = 0;
		int idx = 0;
		if (field.getChild(idx).getType() == MODIFIERS) {
			modifiers = parseModifiers(field.getChild(0));
			idx++;
		}

		// === FIELD TYPE ===
		JavaFile.Type type = parseType(field.getChild(idx++));

		// === FIELD NAME(S) ===

		for (int i = idx; i < field.getChildCount(); ++i) {
			Tree child = field.getChild(i);
			String name = child.getText();
			JavaFile.Expression initialiser = null;			
			// A single "[" indicates an array
			int aindx = 0;
			while (aindx < child.getChildCount()
					&& child.getChild(aindx).getText().equals("[")) {
				type.setDims(type.dims()+1);
				aindx++;
			}
			if (aindx < child.getChildCount()) {
				// FIXME: problem of side effects in initialisers. The only real
				// solution is to require that initialisers are inlined into
				// constructors!
				initialiser = parseExpression(child.getChild(aindx));
			}
			fields.add(new JavaFile.Field(modifiers, name, type, initialiser));
		}
		return fields;
	}
	
	protected JavaFile.Statement parseStatement(Tree stmt) {
		switch (stmt.getType()) {
			case BLOCK:
				return parseBlock(stmt);
			case VARDEF :
				return parseVarDef(stmt);
			case ASSIGN :
				return parseAssign(stmt);
			case RETURN :
				return parseReturn(stmt);
			case THROW :
				return parseThrow(stmt);
			case NEW :
				// return parseNewStmt(stmt);
			case INVOKE :
				// return parseInvokeStmt(stmt);
			case IF :
				return parseIf(stmt);
			case SWITCH :
				// return parseSwitch(stmt);
			case FOR :
				// return parseFor(stmt, label);
			case WHILE :
				return parseWhile(stmt);
			case DOWHILE :
				// return parseDoWhile(stmt, label);
			case SELECTOR :
				// return parseSelectorStmt(stmt);
			case CONTINUE :
				return parseContinue(stmt);
			case BREAK :
				return parseBreak(stmt);
			case LABEL :
				return parseLabel(stmt);
			case POSTINC :
			case PREINC :
			case POSTDEC :
			case PREDEC :
				// return parseIncDec(stmt);
			case ASSERT :
				// return parseAssert(stmt);
			case TRY :
				return parseTry(stmt);
			case SYNCHRONIZED :
				return parseSynchronisedBlock(stmt);
			default :
				throw new SyntaxError("Unknown expression encountered ("
						+ stmt.getText() + ")", stmt.getLine(),
						stmt.getCharPositionInLine(), stmt.getText()
								.length());
		}
	}
	
	/**
     * This method is responsible for parsing a block of code, which is a set of
     * statements between '{' and '}'
     * 
     * @param block
     *            block to parse           
     * @return A Block containing all the statements in this block
     * 
     */
	protected JavaFile.Block parseBlock(Tree block) {
		ArrayList<JavaFile.Statement> stmts = new ArrayList<JavaFile.Statement>();

		// === ITERATE STATEMENTS ===

		for (int i = 0; i != block.getChildCount(); ++i) {
			JavaFile.Statement stmt = parseStatement(block.getChild(i));			
			stmts.add(stmt);
		}

		return new JavaFile.Block(stmts);
	}
	
	/**
     * This method is responsible for parsing a synchronized block of code,
     * e.g, "synchronised(e.list()) { ... }"
     * 
     * @param block
     *            block to parse
     * @return A Block containing all the statements in this block
     * 
     */
	protected JavaFile.Statement parseSynchronisedBlock(Tree block) {
		ArrayList<JavaFile.Statement> stmts = new ArrayList<JavaFile.Statement>();
		JavaFile.Expression e = parseExpression(block.getChild(0));
		
		// === ITERATE STATEMENTS ===
		Tree child = block.getChild(1);
		for (int i = 0; i != child.getChildCount(); ++i) {
			JavaFile.Statement stmt = parseStatement(child.getChild(i));			
			stmts.add(stmt);
		}

		return new JavaFile.SynchronisedBlock(e,stmts);
	}
	
	protected JavaFile.Statement parseTry(Tree block) {
		ArrayList<JavaFile.Statement> stmts = new ArrayList<JavaFile.Statement>();
		ArrayList<JavaFile.CatchBlock> handlers = new ArrayList<JavaFile.CatchBlock>();

		// === ITERATE STATEMENTS ===
		Tree child = block.getChild(0);
		for (int i = 0; i != child.getChildCount(); ++i) {
			JavaFile.Statement stmt = parseStatement(child.getChild(i));			
			stmts.add(stmt);
		}

		for (int i = 1; i < block.getChildCount(); ++i) {
			ArrayList<JavaFile.Statement> cbstmts = new ArrayList<JavaFile.Statement>();			
			Tree cb = block.getChild(i).getChild(0);
			JavaFile.Type type = parseType(cb.getChild(0));
			Tree cbb = block.getChild(i).getChild(1);
			for (int j = 0; j != cbb.getChildCount(); ++j) {
				JavaFile.Statement stmt = parseStatement(cbb.getChild(j));
				cbstmts.add(stmt);
			}
			handlers.add(new JavaFile.CatchBlock(type,cb.getChild(1).getText(),cbstmts));
		}
		
		return new JavaFile.TryCatchBlock(handlers,stmts);
	}
	
	
	/**
     * Responsible for translating variable declarations. ANTLR tree format:
     * 
     * VARDEF MODIFIERS? TYPE NAME [= EXPRESSION]
     * 
     * @param stmt
     *            ANTLR if-statement tree
     * @return
     */
	protected JavaFile.Statement parseVarDef(Tree stmt) {
		ArrayList<Triple<String, JavaFile.Type, JavaFile.Expression>> vardefs = new ArrayList<Triple<String, JavaFile.Type, JavaFile.Expression>>();

		// === MODIFIERS ===
		int modifiers = parseModifiers(stmt.getChild(0));

		// === NAME(S) ===

		for (int i = 2; i < stmt.getChildCount(); i = i + 1) {
			Tree nameTree = stmt.getChild(i);
			String myName = nameTree.getText();
			JavaFile.Type myType = parseType(stmt.getChild(1));
			JavaFile.Expression myInitialiser = null;
			// Parse array type modifiers (if there are any)
			for (int j = 0; j < nameTree.getChildCount(); j = j + 1) {
				Tree am = nameTree.getChild(j);
				if (am.getText().equals("[")) {
					myType.setDims(myType.dims() + 1);
				} else {
					// If we get here, then we've hit an initialiser
					myInitialiser = parseExpression(am);
				}
			}

			vardefs.add(new Triple(myName, myType, myInitialiser));
		}

		return new JavaFile.VarDef(modifiers, vardefs);
	}
	
	/**
     * This method parses an assignment statement.
     * 
     * @param stmt
     * @return
     */
	protected JavaFile.Statement parseAssign(Tree stmt) {
		JavaFile.Expression lhs = parseExpression(stmt.getChild(0));
		JavaFile.Expression rhs = parseExpression(stmt.getChild(1));
		return new JavaFile.Assignment(lhs,rhs);
	}
	
	protected JavaFile.Statement parseReturn(Tree stmt) {
		if (stmt.getChildCount() > 0) {			
			return new JavaFile.Return(parseExpression(stmt.getChild(0)));
		} else {
			return new JavaFile.Return(null);
		}
	}

	protected JavaFile.Statement parseThrow(Tree stmt) {				
		return new JavaFile.Throw(parseExpression(stmt.getChild(0)));		
	}
	
	
	/**
     * Responsible for parsing break statements.
     * 
     * @param stmt
     * @param label
     * @param cfg
     * @return
     */
	protected JavaFile.Statement parseBreak(Tree stmt) {
		if (stmt.getChildCount() > 0) {
			// this is a labelled break statement.			
			return new JavaFile.Break(stmt.getChild(0).getText());
		} else {
			return new JavaFile.Break(null);			
		}
	}

	/**
     * Responsible for parsing continue statements.
     * 
     * @param stmt
     * @param label
     * @param cfg
     * @return
     */
	protected JavaFile.Statement parseContinue(Tree stmt) {
		if (stmt.getChildCount() > 0) {
			// this is a labelled continue statement.			
			return new JavaFile.Continue(stmt.getChild(0).getText());
		} else {
			return new JavaFile.Continue(null);			
		}
	}
	
	/**
     * Responsible for parsing labelled statements.
     * 
     * @param stmt
     * @param label
     * @param cfg
     * @return
     */
	protected JavaFile.Statement parseLabel(Tree stmt) {
		String label = stmt.getChild(0).getText();
		JavaFile.Statement s = parseStatement(stmt.getChild(1));
		
		return new JavaFile.Label(label,s);
	}
	
	protected JavaFile.Statement parseIf(Tree stmt) {
		JavaFile.Expression condition = parseExpression(stmt.getChild(0));
		JavaFile.Statement trueStmt = parseStatement(stmt.getChild(1));
		JavaFile.Statement falseStmt = stmt.getChildCount() < 3 ? null : parseStatement(stmt.getChild(2));
		return new JavaFile.If(condition,trueStmt,falseStmt);		
	}
	
	protected JavaFile.Statement parseWhile(Tree stmt) {
		JavaFile.Expression condition = parseExpression(stmt.getChild(0).getChild(0));
		JavaFile.Statement body = parseStatement(stmt.getChild(1));		
		return new JavaFile.While(condition,body);		
	}
	
	protected JavaFile.Expression parseExpression(Tree expr) {
		switch (expr.getType()) {
			case CHARVAL :
				return parseCharVal(expr);
			case BOOLVAL :
				return parseBoolVal(expr);
			case INTVAL :
				return parseIntVal(expr);
			case FLOATVAL :
				return parseFloatVal(expr);
			case STRINGVAL :
				return parseStringVal(expr);
			case NULLVAL :
				return parseNullVal(expr);
			case ARRAYVAL :
				return parseArrayVal(expr);
			case ARRAYINIT :
				return parseTypedArrayVal(expr);
			case VAR :
				return parseVariable(expr);
			case NEW :
				return parseNew(expr);
			case INVOKE :
				return parseInvoke(expr);
			case SELECTOR :
				return parseSelector(expr);
			case GETCLASS :
				return parseGetClass(expr);
			case PREINC :
				return parseUnOp(UnOp.PREINC,expr);
			case PREDEC :
				return parseUnOp(UnOp.PREDEC,expr);
			case POSTINC :
				return parseUnOp(UnOp.POSTINC,expr);
			case POSTDEC :
				return parseUnOp(UnOp.POSTDEC,expr);
			case NEG :
				return parseUnOp(UnOp.NEG, expr);
			case NOT :
				return parseUnOp(UnOp.NOT, expr);
			case INV :
				return parseUnOp(UnOp.INV, expr);
			case CAST :
				return parseCast(expr);
			case LABINOP :
				return parseLeftAssociativeBinOp(expr);
			case USHR :
				return parseBinOp(BinOp.USHR, expr);
			case LAND :
				return parseBinOp(BinOp.LAND, expr);
			case LOR :
				return parseBinOp(BinOp.LOR, expr);
			case AND :
				return parseBinOp(BinOp.AND, expr);
			case OR :
				return parseBinOp(BinOp.OR, expr);
			case XOR :
				return parseBinOp(BinOp.XOR, expr);
			case EQ :
				return parseBinOp(BinOp.EQ, expr);
			case NEQ :
				return parseBinOp(BinOp.NEQ, expr);
			case LT :
				return parseBinOp(BinOp.LT, expr);
			case LTEQ :
				return parseBinOp(BinOp.LTEQ, expr);
			case GT :
				return parseBinOp(BinOp.GT, expr);
			case GTEQ :
				return parseBinOp(BinOp.GTEQ, expr);
			case INSTANCEOF :
				return parseInstanceOf(expr);
			case TERNOP :
				return parseTernOp(expr);
			case ASSIGN :
			default :
				throw new SyntaxError("Unknown expression encountered ("
						+ expr.getText() + ")", expr.getLine(), expr
						.getCharPositionInLine(), expr.getText().length());
		}
	}	
	
	protected JavaFile.Expression parseSelector(Tree selector) {		
		Tree target = selector.getChild(0);

		// The following is basically dealing with an awkward situation. For
		// example, if you write e.g. java.lang.Float.floatToIntBits(x), then
		// this gets translated into:
		//
		// SELECTOR
		// VAR
		// java
		// DEREF
		// lang
		// DEREF
		// Float
		// INVOKE
		// floatToIntBits
		// ...
		//
		// Therefore, in fact, "java" is not a variable, but *part* of a class
		// access. Thus, we first try "java" as a variable (which it can be)
		// and,
		// if this fails, assume it is part of a class access and then try to
		// figure out which class.

		int idx = 1;
		JavaFile.Expression expr = new JavaFile.Variable(target.getChild(0).getText());
		
		for (int i = idx; i != selector.getChildCount(); ++i) {
			Tree child = selector.getChild(i);
			switch (child.getType()) {
				case DEREF :
					expr = new JavaFile.Deref(expr,child.getChild(0).getText());
					break;
				case ARRAYINDEX : {
					expr = new JavaFile.ArrayIndex(expr,parseExpression(child
							.getChild(0)));
					break;
				}
				case INVOKE : {
					int start = 0;
					if(child.getChild(0).getType() == TYPE_PARAMETER) {		
						start++;
					} 
					String method = child.getChild(start).getText();
					
					List<JavaFile.Expression> params = parseExpressionList(
							start+1, child.getChildCount(), child);
					
					expr = new JavaFile.Invoke(expr, method, params,
							new ArrayList<JavaFile.Type>());
					break;
				}
				case NEW :
					throw new RuntimeException("Selector new construction case not supported (yet)");					
				default :
					throw new SyntaxError("Unknown expression encountered.",
							selector.getLine(), selector
									.getCharPositionInLine(), selector
									.getText().length());
			}
		}
		return expr;
	}
	
	/**
     * This parses a "new" expression. The key difficulties here, lie in the
     * fact that a new statement can involve an anonymous class declaration.
     * 
     * @param expr
     * @return
     */
	protected JavaFile.Expression parseNew(Tree expr) {
		// first, parse any parameters supplied
		ArrayList<JavaFile.Declaration> declarations = new ArrayList<JavaFile.Declaration>();

		int end = expr.getChildCount();
		for (int i = 1; i < expr.getChildCount(); ++i) {
			Tree child = expr.getChild(i);
			if (child.getType() == METHOD) {
				// Store anonymous class methods
				declarations.add(parseMethod(child));
				end = Math.min(i, end);
			} else if (child.getType() == FIELD) {
				declarations.addAll(parseField(child));
				end = Math.min(i, end);
			}
		}

		List<JavaFile.Expression> params = parseExpressionList(1, end, expr);

		return new JavaFile.New(parseType(expr.getChild(0)), params,
				declarations);
	}
	
	/**
     * This method parses an isolated invoke call. For example, "f()" is
     * isolated, whilst "x.f()" or "this.f()" etc are not.
     * 
     * @param expr
     * @return
     */
	public JavaFile.Expression parseInvoke(Tree expr) {
				
		// =================================================
		// ======== PARSE TYPE PARAMETERS (IF ANY) =========
		// =================================================
		int start = 0;
		
		// First, check for type parameters. These are present for
        // method invocations which explicitly indicate the type
        // parameters to use. For example, x.<K>someMethod();
		if(expr.getChild(0).getType() == TYPE_PARAMETER) {		
			start++;
		} 

		String method = expr.getChild(start).getText();
		
		List<JavaFile.Expression> params = parseExpressionList(start+1, expr
				.getChildCount(), expr);

		// Need to do something with type parameters.
		
		return new JavaFile.Invoke(null, method, params,
				new ArrayList<JavaFile.Type>());
	}
	
	public JavaFile.Expression parseGetClass(Tree expr) {		
		return new JavaFile.ClassVal(parseType(expr.getChild(0)));
	}
	
	protected JavaFile.Expression parseTernOp(Tree expr) {
		JavaFile.Expression cond = parseExpression(expr.getChild(0));
		JavaFile.Expression tbranch = parseExpression(expr.getChild(1));
		JavaFile.Expression fbranch = parseExpression(expr.getChild(2));
		return new JavaFile.TernOp(cond, tbranch, fbranch);
	}
	
	protected JavaFile.Expression parseInstanceOf(Tree expr) {
		JavaFile.Expression e = parseExpression(expr.getChild(0));
		return new JavaFile.InstanceOf(e, parseType(expr.getChild(1)));
	}
	
	protected JavaFile.Expression parseCast(Tree expr) {		
		return new JavaFile.Cast(parseType(expr.getChild(0)), parseExpression(expr.getChild(1)));
	}
	
	protected JavaFile.Expression parseUnOp(int uop, Tree expr) {
		return new JavaFile.UnOp(uop, parseExpression(expr.getChild(0)));
	}
	
	// Binary operations which can be left associative are more complex and have
	// to be delt with using a special LABINOP operator.
	protected JavaFile.Expression parseLeftAssociativeBinOp(Tree expr) {
		JavaFile.Expression lhs = parseExpression(expr.getChild(0));				

		for (int i = 1; i < expr.getChildCount(); i = i + 2) {
			int bop = parseBinOpOp(expr.getChild(i).getText(), expr);
			lhs = new JavaFile.BinOp(bop, lhs, parseExpression(
					expr.getChild(i + 1)));
		}

		return lhs;
	}
	
	protected JavaFile.Expression parseBinOp(int bop, Tree expr) {
		JavaFile.Expression lhs = parseExpression(expr.getChild(0));
		JavaFile.Expression rhs = parseExpression(expr.getChild(1));		
		
		return new JavaFile.BinOp(bop, lhs, rhs);
	}
	
	protected int parseBinOpOp(String op, Tree expr) {
		if (op.equals("+")) {
			return BinOp.ADD;
		} else if (op.equals("-")) {
			return BinOp.SUB;
		} else if (op.equals("/")) {
			return BinOp.DIV;
		} else if (op.equals("*")) {
			return BinOp.MUL;
		} else if (op.equals("%")) {
			return BinOp.MOD;
		} else if (op.equals("<")) {
			return BinOp.SHL;
		} else if (op.equals(">")) {
			return BinOp.SHR;
		} else {
			throw new SyntaxError(
					"Unknown left-associative binary operator encountered ('"
							+ op + "').", expr.getLine(), expr
							.getCharPositionInLine(), expr.getText().length());
		}
	}
	
	protected JavaFile.Expression parseVariable(Tree expr) {
		String name = expr.getChild(0).getText();
		return new JavaFile.Variable(name);
	}
	
	protected JavaFile.Expression parseCharVal(Tree expr) {
		String charv = expr.getChild(0).getText();
		JavaFile.CharVal v = null;
		if (charv.length() == 3) {
			v = new JavaFile.CharVal(charv.charAt(1));
		} else {
			String tmp = charv.substring(1, charv.length() - 1);
			if (tmp.equals("\\b"))
				v = new JavaFile.CharVal('\b');
			else if (tmp.equals("\\t"))
				v = new JavaFile.CharVal('\t');
			else if (tmp.equals("\\f"))
				v = new JavaFile.CharVal('\f');
			else if (tmp.equals("\\n"))
				v = new JavaFile.CharVal('\n');
			else if (tmp.equals("\\r"))
				v = new JavaFile.CharVal('\r');
			else if (tmp.equals("\\\""))
				v = new JavaFile.CharVal('\"');
			else if (tmp.equals("\\\\"))
				v = new JavaFile.CharVal('\\');
			else if (tmp.equals("\\'"))
				v = new JavaFile.CharVal('\'');
			else if (Character.isDigit(tmp.charAt(1)))  {
				int octal_val = Integer.parseInt(tmp.substring(1,tmp.length()),8);
				v = new JavaFile.CharVal((char) octal_val);
			} else {
				throw new RuntimeException(
						"Unable to parse character constant: " + tmp);
			}
		} 
		return v;
	}
	
	protected JavaFile.Expression parseBoolVal(Tree expr) {
		JavaFile.BoolVal v = new JavaFile.BoolVal(Boolean
				.parseBoolean(expr.getChild(0).getText()));
		return v;
	}

	protected JavaFile.Expression parseIntVal(Tree expr) {
		int radix = 10;
		String value = expr.getChild(0).getText();
		if (value.startsWith("0x")) {
			// HEX value
			radix = 16;
			value = value.substring(2);
		}
		char lc = value.charAt(value.length() - 1);

		long val = parseLongVal(value.substring(0, value.length() - 1), radix);

		if (lc == 'l' || lc == 'L') {
			// return new LongVal(Long.parseLong(value.substring(0,
			// value.length() - 1), radix));
			return new JavaFile.LongVal(val);
		} else if (radix == 10 && (lc == 'f' || lc == 'F')) {
			return new JavaFile.FloatVal(val);
		} else if (radix == 10 && (lc == 'd' || lc == 'D')) {
			return new JavaFile.DoubleVal(val);
		}

		val = parseLongVal(value, radix);
		return new JavaFile.IntVal((int) val);
	}

	/**
     * Java's Long.parseLong() method throws an exception if the long parsed is
     * too large whereas javac simply wraps. Hence, we need our own
     * implementation.
     */
	protected static long parseLongVal(String in, int radix) {
		char[] c = in.toCharArray();
		long out = 0;
		for (int i = 0; i < c.length; i++) {
			int d = Character.digit(c[i], radix);
			out = out * radix + d;
		}
		return out;
	}

	protected JavaFile.Expression parseStringVal(Tree expr) {
		String v = expr.getChild(0).getText();

		/*
         * Parsing a string requires several steps to be taken. First, we need
         * to strip quotes from the ends of the string.
         */
		v = v.substring(1, v.length() - 1);
		// Second, step through the string and replace escaped characters
		for (int i = 0; i < v.length(); i++) {
			if (v.charAt(i) == '\\') {
				if (v.length() <= i + 1) {
					System.err
							.println("String ends with escape character: dropped ("
									+ v + ")");
					// TODO this should never happen because the parser
					// shouldn't allow. Verify.
					throw new RuntimeException("bad string: " + v);
				} else {
					char replace = 0;
					int len = 2;
					switch (v.charAt(i + 1)) {
						case 'b' :
							replace = '\b';
							break;
						case 't' :
							replace = '\t';
							break;
						case 'n' :
							replace = '\n';
							break;
						case 'f' :
							replace = '\f';
							break;
						case 'r' :
							replace = '\r';
							break;
						case '"' :
							replace = '\"';
							break;
						case '\'' :
							replace = '\'';
							break;
						case '\\' :
							replace = '\\';
							break;
						case 'u' :
							len = 6; // unicode escapes are six digits long,
							// including "slash u"
							String unicode = v.substring(i + 2, i + 6);
							replace = (char) Integer.parseInt(unicode, 16); // unicode
							// string
							// is
							// in
							// hex
							break;
						default :
							// TODO: handle octal, catchall
							throw new RuntimeException(
									"unknown escaped character, not implemented: "
											+ v.charAt(i + 1) + " (" + v + ")");
					}
					v = v.substring(0, i) + replace + v.substring(i + len);
				}
			}
		}
		// finally, construct a new string in the FlowGraph
		return new JavaFile.StringVal(v);
	}

	protected JavaFile.Expression parseNullVal(Tree expr) {
		return new JavaFile.NullVal();
	}


	/**
	 * This parses a floating point value. Note that this may correspond to a
	 * Java float, or a Java double!
	 */
	protected JavaFile.Expression parseFloatVal(Tree expr) {
		String val = expr.getChild(0).getText();

		char lc = val.charAt(val.length() - 1);
		JavaFile.Expression r;
		if (lc == 'f' || lc == 'F') {
			r = new JavaFile.FloatVal(Float.parseFloat(val));
		} else {
			r = new JavaFile.DoubleVal(Double.parseDouble(val));
		}
		return r;
	}

	/**
     * Parse an array initialiser expression. For example:
     * 
     * <pre>
     * Object[] test = {&quot;abc&quot;, new Integer(2)};
     * </pre>
     * 
     * @param expr
     * @return
     */
	protected JavaFile.Expression parseArrayVal(Tree expr) {
		List<JavaFile.Expression> values = parseExpressionList(0, expr
				.getChildCount(), expr);
		return new JavaFile.ArrayVal(values);
	}

	/**
	 * Parse a typed array initialiser expression. This is distinct from an
	 * array initialiser in a subtle way. To generate a typed array initiliser
	 * you must specify the class of array to construct. For example:
	 * 
	 * <pre>
	 * Object[] test = new Object[] { &quot;abc&quot;, new Integer(2) };
	 * </pre>
	 * 
	 * 
	 * @param expr
	 * @return
	 */
	protected JavaFile.Expression parseTypedArrayVal(Tree expr) {
		JavaFile.Type type = parseType(expr.getChild(0));
		Tree aval = expr.getChild(1);
		List<JavaFile.Expression> values = parseExpressionList(0, aval
				.getChildCount(), aval);
		return new JavaFile.TypedArrayVal(type, values);
	}

	protected List<JavaFile.Expression> parseExpressionList(int start,
			int end, Tree expr) {

		ArrayList<JavaFile.Expression> es = new ArrayList<JavaFile.Expression>();
		
		for (int i = start; i < end; i++) {
			es.add(parseExpression(expr.getChild(i)));			
		}

		return es;
	}
	
	protected int parseModifiers(Tree ms) {
		int mods = 0;
		for (int i = 0; i != ms.getChildCount(); ++i) {
			Tree mc = ms.getChild(i);
			String m = mc.getText();
			if (m.equals("public")) {
				mods |= Modifier.PUBLIC;
			} else if (m.equals("private")) {
				mods |= Modifier.PRIVATE;
			} else if (m.equals("protected")) {
				mods |= Modifier.PROTECTED;
			} else if (m.equals("static")) {
				mods |= Modifier.STATIC;
			} else if (m.equals("abstract")) {
				mods |= Modifier.ABSTRACT;
			} else if (m.equals("final")) {
				mods |= Modifier.FINAL;
			} else if (m.equals("native")) {
				mods |= Modifier.NATIVE;
			} else if (m.equals("synchronized")) {
				mods |= Modifier.SYNCHRONIZED;
			} else if (m.equals("transient")) {
				mods |= Modifier.TRANSIENT;
			} else if (m.equals("volatile")) {
				mods |= Modifier.VOLATILE;
			} else if (mc.getType() == ANNOTATION) {
				// ignore annotations for now.
			} else {
				throw new SyntaxError("not expecting " + m, mc.getLine(), mc
						.getCharPositionInLine());
			}
		}
		return mods;
	}
	
	protected static JavaFile.Type parseType(Tree type) {		
		assert type.getType() == TYPE;
						
		// === ARRAY DIMENSIONS ===

		int dims = 0;

		for (int i = type.getChildCount() - 1; i > 0; --i) {
			if (!type.getChild(i).getText().equals("[")) {
				break;
			}
			dims++;
		}
		
		// === COMPONENTS ===

		ArrayList<String> components = new ArrayList<String>();
		for(int i=0;i!=(type.getChildCount()-dims);++i) {
			String text = type.getChild(i).getText();
			if(text.equals("VOID")) {
				text = "void"; // hack!
			}
			components.add(text);
		}
		
		return new JavaFile.Type(components,dims);
	}
	
	public static void printTree(Tree ast, int n, int line) {
		if (ast.getLine() != line) {
			System.out.print("(line " + ast.getLine() + ")\t");
		} else {
			int ls = Integer.toString(line).length();
			for (int i = 0; i != ls + 7; ++i) {
				System.out.print(" ");
			}
			System.out.print("\t");
		}
		for (int i = 0; i != n; ++i) {
			System.out.print(" ");
		}
		System.out.println(ast.getText() + " ");
		for (int i = 0; i != ast.getChildCount(); ++i) {
			printTree(ast.getChild(i), n + 1, ast.getLine());
		}
	}
	
	// ANTLR Token Types
	protected static final int PACKAGE = JavaParser.PACKAGE;

	protected static final int TYPE = JavaParser.TYPE;

	protected static final int SHL = JavaParser.SHL;

	protected static final int LOR = JavaParser.LOR;

	protected static final int METHOD = JavaParser.METHOD;

	protected static final int THROWS = JavaParser.THROWS;

	protected static final int MOD = JavaParser.MOD;

	protected static final int IMPORT = JavaParser.IMPORT;

	protected static final int INTVAL = JavaParser.INTVAL;

	protected static final int DOWHILE = JavaParser.DOWHILE;

	protected static final int OR = JavaParser.OR;

	protected static final int SHR = JavaParser.SHR;

	protected static final int VARDEF = JavaParser.VARDEF;

	protected static final int DOUBLEVAL = JavaParser.DOUBLEVAL;

	protected static final int CONTINUE = JavaParser.CONTINUE;

	protected static final int BLOCK = JavaParser.BLOCK;

	protected static final int AND = JavaParser.AND;

	protected static final int TYPE_PARAMETER = JavaParser.TYPE_PARAMETER;

	protected static final int NULLVAL = JavaParser.NULLVAL;

	protected static final int ASSIGN = JavaParser.ASSIGN;

	protected static final int INSTANCEOF = JavaParser.INSTANCEOF;

	protected static final int VOID = JavaParser.VOID;

	protected static final int FLOATVAL = JavaParser.FLOATVAL;

	protected static final int ASSERT = JavaParser.ASSERT;

	protected static final int VARARGS = JavaParser.VARARGS;

	protected static final int USHR = JavaParser.USHR;

	protected static final int NEQ = JavaParser.NEQ;

	protected static final int EXTENDS = JavaParser.EXTENDS;

	protected static final int ENUM = JavaParser.ENUM;

	protected static final int Exponent = JavaParser.Exponent;

	protected static final int SUPER = JavaParser.SUPER;

	protected static final int IMPLEMENTS = JavaParser.IMPLEMENTS;

	protected static final int WHILE = JavaParser.WHILE;

	protected static final int WS = JavaParser.WS;

	protected static final int EQ = JavaParser.EQ;

	protected static final int LT = JavaParser.LT;

	protected static final int BOOLVAL = JavaParser.BOOLVAL;

	protected static final int GT = JavaParser.GT;

	protected static final int GTEQ = JavaParser.GTEQ;

	protected static final int FIELD = JavaParser.FIELD;

	protected static final int LAND = JavaParser.LAND;

	protected static final int VAR = JavaParser.VAR;

	protected static final int MUL = JavaParser.MUL;

	protected static final int CLASS = JavaParser.CLASS;

	protected static final int INTERFACE = JavaParser.INTERFACE;

	protected static final int CHARVAL = JavaParser.CHARVAL;

	protected static final int MODIFIERS = JavaParser.MODIFIERS;

	protected static final int RETURN = JavaParser.RETURN;

	protected static final int LTEQ = JavaParser.LTEQ;

	protected static final int IF = JavaParser.IF;

	protected static final int BREAK = JavaParser.BREAK;

	protected static final int XOR = JavaParser.XOR;

	protected static final int FOR = JavaParser.FOR;

	protected static final int PARAMETER = JavaParser.PARAMETER;

	protected static final int ANNOTATION = JavaParser.ANNOTATION;

	protected static final int DIV = JavaParser.DIV;

	protected static final int STRINGVAL = JavaParser.STRINGVAL;

	protected static final int SUB = JavaParser.SUB;

	protected static final int ADD = JavaParser.ADD;

	protected static final int THROW = JavaParser.THROW;

	protected static final int DEREF = JavaParser.DEREF;

	protected static final int NEG = JavaParser.NEG;

	protected static final int PREINC = JavaParser.PREINC;

	protected static final int POSTINC = JavaParser.POSTINC;

	protected static final int PREDEC = JavaParser.PREDEC;

	protected static final int POSTDEC = JavaParser.POSTDEC;

	protected static final int NOT = JavaParser.NOT;

	protected static final int INV = JavaParser.INV;

	protected static final int CAST = JavaParser.CAST;

	protected static final int NEW = JavaParser.NEW;

	protected static final int SELECTOR = JavaParser.SELECTOR;

	protected static final int INVOKE = JavaParser.INVOKE;

	protected static final int ARRAYINDEX = JavaParser.ARRAYINDEX;

	protected static final int SYNCHRONIZED = JavaParser.SYNCHRONIZED;

	protected static final int GETCLASS = JavaParser.GETCLASS;

	protected static final int LABEL = JavaParser.LABEL;

	protected static final int FOREACH = JavaParser.FOREACH;

	protected static final int TRY = JavaParser.TRY;

	protected static final int CATCH = JavaParser.CATCH;

	protected static final int FINALLY = JavaParser.FINALLY;

	protected static final int TERNOP = JavaParser.CONDEXPR;

	protected static final int ARRAYVAL = JavaParser.ARRAYVAL;

	protected static final int SWITCH = JavaParser.SWITCH;

	protected static final int CASE = JavaParser.CASE;

	protected static final int DEFAULT = JavaParser.DEFAULT;

	protected static final int ARRAYINIT = JavaParser.ARRAYINIT;

	protected static final int LABINOP = JavaParser.LABINOP;
}
