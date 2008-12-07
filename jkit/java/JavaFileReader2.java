package jkit.java;

import java.io.*;
import java.util.*;

import jkit.compiler.SyntaxError;
import jkit.java.Decl.*;
import jkit.util.Pair;
import jkit.util.Triple;
import jkit.jil.*;

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

		ArrayList<Decl> classes = new ArrayList<Decl>();
		ArrayList<Pair<Boolean, String>> imports = new ArrayList<Pair<Boolean, String>>();
		String pkg = null;

		// Read top declarations first.
		outer : for (int i = 0; i != ast.getChildCount(); ++i) {
			boolean static_import = false;
			Tree c = ast.getChild(i);
			switch (c.getType()) {
				case STATIC_IMPORT :
					static_import = true;
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
					imports.add(0, new Pair(static_import, filter));
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
			classes.addAll(parseDeclaration(ast.getChild(i)));
		}

		return new JavaFile(pkg, imports, classes);
	}

	protected List<Decl> parseDeclaration(Tree decl) {
		ArrayList<Decl> declarations = new ArrayList<Decl>();

		switch (decl.getType()) {
			case FIELD :
				declarations.addAll(parseField(decl));
				break;
			case METHOD :
				declarations.add(parseMethod(decl));
				break;
			case CLASS :
			case INTERFACE :
				declarations.add(parseClass(decl));
				break;
			case ENUM :
				declarations.add(parseEnum(decl));
				break;
			case STATIC :
				// static initialiser block
				declarations.add(new Decl.StaticInitialiserBlock(
						parseBlock(decl.getChild(0)).statements()));
				break;
			case BLOCK :
				// non-static initialiser block
				declarations.add(new Decl.InitialiserBlock(parseBlock(decl)
						.statements(), new SourceLocation(decl.getLine(), decl
						.getCharPositionInLine())));
				break;
			case ANNOTATION :
				declarations.add(parseAnnotation(decl));
				break;
		}

		return declarations;
	}

	protected Decl.Clazz parseClass(Tree decl) {
		int idx = 0;
		List<Modifier> modifiers = new ArrayList<Modifier>();
		if (decl.getChild(idx).getType() == MODIFIERS) {
			modifiers = parseModifiers(decl.getChild(0));
			idx++;
		}

		ArrayList<Type.Variable> typeArgs = new ArrayList<Type.Variable>();
		for (int i = 0; i != decl.getChild(idx).getChildCount(); ++i) {
			typeArgs.add(parseVariableType(decl.getChild(idx).getChild(i)));
		}

		String name = decl.getChild(idx++).getText();

		// ====================================================================
		// ====================== PARSE EXTENDS CLAUSE ========================
		// ====================================================================

		Type.Clazz superclass = null;
		if (idx < decl.getChildCount()
				&& decl.getChild(idx).getType() == EXTENDS) {
			superclass = parseClassType(decl.getChild(idx++).getChild(0));
		}

		// ====================================================================
		// ===================== PARSE IMPLEMENTS CLAUSE ======================
		// ====================================================================

		ArrayList<Type.Clazz> interfaces = new ArrayList<Type.Clazz>();
		if (idx < decl.getChildCount()
				&& decl.getChild(idx).getType() == IMPLEMENTS) {
			Tree ch = decl.getChild(idx++);
			for (int i = 0; i != ch.getChildCount(); ++i) {
				interfaces.add(parseClassType(ch.getChild(i)));
			}
		}

		// ====================================================================
		// ======================== PARSE DECLARATIONS ========================
		// ====================================================================

		ArrayList<Decl> declarations = new ArrayList<Decl>();

		for (int i = idx; i < decl.getChildCount(); ++i) {
			declarations.addAll(parseDeclaration(decl.getChild(i)));
		}

		SourceLocation loc = new SourceLocation(decl.getLine(), decl
				.getCharPositionInLine());

		if (decl.getType() == INTERFACE) {
			return new Decl.Interface(modifiers, name, typeArgs,
					superclass, interfaces, declarations, loc);
		} else {
			return new Decl.Clazz(modifiers, name, typeArgs, superclass,
					interfaces, declarations, loc);
		}
	}

	protected Decl.Enum parseEnum(Tree decl) {
		int idx = 0;
		List<Modifier> modifiers = new ArrayList<Modifier>();
		if (decl.getChild(idx).getType() == MODIFIERS) {
			modifiers = parseModifiers(decl.getChild(0));
			idx++;
		}

		String name = decl.getChild(idx++).getText();

		// ====================================================================
		// ===================== PARSE IMPLEMENTS CLAUSE ======================
		// ====================================================================

		ArrayList<Type.Clazz> interfaces = new ArrayList<Type.Clazz>();
		if (idx < decl.getChildCount()
				&& decl.getChild(idx).getType() == IMPLEMENTS) {
			Tree ch = decl.getChild(idx++);
			for (int i = 0; i != ch.getChildCount(); ++i) {
				interfaces.add(parseClassType(ch.getChild(i)));
			}
		}

		// ====================================================================
		// ========================= PARSE CONSTANTS ==========================
		// ====================================================================
		ArrayList<Decl.EnumConstant> constants = new ArrayList<Decl.EnumConstant>();
		while (idx < decl.getChildCount()
				&& decl.getChild(idx).getType() == ENUM_CONSTANT) {
			constants.add(parseEnumConstant(decl.getChild(idx)));
			idx = idx + 1;
		}

		// ====================================================================
		// ======================== PARSE DECLARATIONS ========================
		// ====================================================================

		ArrayList<Decl> declarations = new ArrayList<Decl>();
		for (; idx < decl.getChildCount(); ++idx) {
			declarations.addAll(parseDeclaration(decl.getChild(idx)));
		}

		return new Decl.Enum(modifiers, name, interfaces, constants,
				declarations, new SourceLocation(decl.getLine(), decl
						.getCharPositionInLine()));
	}

	protected Decl.EnumConstant parseEnumConstant(Tree decl) {
		// annotation support is required.
		String name = decl.getChild(0).getText();

		ArrayList<Expr> arguments = new ArrayList<Expr>();
		ArrayList<Decl> declarations = new ArrayList<Decl>();
		for (int i = 1; i != decl.getChildCount(); ++i) {
			Tree child = decl.getChild(i);
			switch (child.getType()) {
				case FIELD :
				case METHOD :
				case CLASS :
				case INTERFACE :
				case ENUM :
				case STATIC :
				case BLOCK :
					declarations.addAll(parseDeclaration(child));
					break;
				default :
					// in the default case, we must have an expression which is
                    // actually an argument.
					arguments.add(parseExpression(child));
			}
		}

		return new Decl.EnumConstant(
				name,
				arguments,
				declarations,
				new SourceLocation(decl.getLine(), decl.getCharPositionInLine()));
	}

	protected Decl.AnnotationInterface parseAnnotation(Tree decl) {
		// === TYPE MODIFIERS ===

		List<Modifier> modifiers = new ArrayList<Modifier>();
		int idx = 0;
		if (decl.getChild(idx).getType() == MODIFIERS) {
			modifiers = parseModifiers(decl.getChild(0));
			idx++;
		}

		String name = decl.getChild(idx++).getText();

		ArrayList<Triple<Type, String, Value>> methods = new ArrayList<Triple<Type, String, Value>>();

		for (; idx < decl.getChildCount(); ++idx) {
			Tree child = decl.getChild(idx);
			if (child.getType() == METHOD) {
				Type t = parseType(child.getChild(0));
				String n = child.getChild(1).getText();
				Value v = null;
				if (child.getChildCount() > 2) {
					v = (Value) parseExpression(child.getChild(2));
				}

				methods.add(new Triple(t, n, v));
			}
		}

		return new Decl.AnnotationInterface(
				modifiers,
				name,
				methods,
				new SourceLocation(decl.getLine(), decl.getCharPositionInLine()));
	}

	protected Decl.Method parseMethod(Tree method) {

		// === TYPE MODIFIERS ===

		List<Modifier> modifiers = new ArrayList<Modifier>();
		int idx = 0;
		if (method.getChild(idx).getType() == MODIFIERS) {
			modifiers = parseModifiers(method.getChild(0));
			idx++;
		}

		// === TYPE ARGUMENTS ===

		ArrayList<Type.Variable> typeArgs = new ArrayList<Type.Variable>();
		while (method.getChild(idx).getType() == TYPE_PARAMETER) {
			typeArgs.add(parseVariableType(method.getChild(idx++)));
		}

		String name = method.getChild(idx++).getText();

		Type returnType = null;

		// if no return type, then is a constructor
		if (method.getChild(idx).getType() == TYPE) {
			returnType = parseType(method.getChild(idx));
		}
		idx = idx + 1;

		// === FORMAL PARAMETERS ===

		ArrayList<Triple<String, List<Modifier>, Type>> params = new ArrayList<Triple<String, List<Modifier>, Type>>();

		while (idx < method.getChildCount()
				&& method.getChild(idx).getType() == PARAMETER) {
			Tree c = method.getChild(idx);
			List<Modifier> pModifiers = parseModifiers(c.getChild(0));
			Type t = parseType(c.getChild(1));
			String n = c.getChild(2).getText();

			for (int i = 3; i < c.getChildCount(); i = i + 2) {
				t = new Type.Array(t);
			}

			params.add(new Triple(n, pModifiers, t));
			idx++;
		}

		// === VAR ARGS ===
		boolean varargs = false;
		if (idx < method.getChildCount()
				&& method.getChild(idx).getType() == VARARGS) {
			Tree c = method.getChild(idx);
			List<Modifier> pModifiers = parseModifiers(c.getChild(0));
			Type t = parseType(c.getChild(1));
			String n = c.getChild(2).getText();
			params.add(new Triple(n, pModifiers, t));
			idx++;
			varargs = true;
		}

		// === THROWS CLAUSE ===

		ArrayList<Type.Clazz> exceptions = new ArrayList<Type.Clazz>();

		if (idx < method.getChildCount()
				&& method.getChild(idx).getType() == THROWS) {
			Tree tt = method.getChild(idx++);
			for (int i = 0; i != tt.getChildCount(); ++i) {
				exceptions.add(parseClassType(tt.getChild(i)));
			}
		}

		// === METHOD BODY ===

		Stmt.Block block = null;

		if (idx < method.getChildCount()
				&& method.getChild(idx).getType() == BLOCK) {
			// do nothing
			block = parseBlock(method.getChild(idx));
		}

		if (returnType == null) {
			return new Decl.Constructor(modifiers, name, params, varargs,
					typeArgs, exceptions, block, new SourceLocation(method
							.getLine(), method.getCharPositionInLine()));
		} else {
			return new Decl.Method(modifiers, name, returnType, params,
					varargs, typeArgs, exceptions, block, new SourceLocation(
							method.getLine(), method.getCharPositionInLine()));
		}
	}

	protected List<Decl.Field> parseField(Tree tree) {
		assert tree.getType() == FIELD;

		ArrayList<Decl.Field> fields = new ArrayList<Decl.Field>();

		// === MODIFIERS ===
		List<Modifier> modifiers = new ArrayList<Modifier>();
		int idx = 0;
		if (tree.getChild(idx).getType() == MODIFIERS) {
			modifiers = parseModifiers(tree.getChild(0));
			idx++;
		}

		// === FIELD TYPE ===
		Type type = parseType(tree.getChild(idx++));

		// === FIELD NAME(S) ===

		for (int i = idx; i < tree.getChildCount(); ++i) {
			Tree child = tree.getChild(i);
			String name = child.getText();
			Expr initialiser = null;
			// A single "[" indicates an array
			int aindx = 0;
			while (aindx < child.getChildCount()
					&& child.getChild(aindx).getText().equals("[")) {
				type = new Type.Array(type);
				aindx++;
			}
			if (aindx < child.getChildCount()) {
				// FIXME: problem of side effects in initialisers. The only real
				// solution is to require that initialisers are inlined into
				// constructors!
				initialiser = parseExpression(child.getChild(aindx));
			}
			fields.add(new Decl.Field(modifiers, name, type, initialiser,
					new SourceLocation(child.getLine(), child
							.getCharPositionInLine())));
		}
		return fields;
	}

	protected Stmt parseStatement(Tree stmt) {
		switch (stmt.getType()) {
			case BLOCK :
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
				return parseNew(stmt);
			case INVOKE :
				return parseInvoke(stmt);
			case IF :
				return parseIf(stmt);
			case SWITCH :
				return parseSwitch(stmt);
			case FOR :
				return parseFor(stmt);
			case WHILE :
				return parseWhile(stmt);
			case DOWHILE :
				return parseDoWhile(stmt);
			case SELECTOR :
				return parseSelectorStmt(stmt);
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
				return parseIncDec(stmt);
			case ASSERT :
				return parseAssert(stmt);
			case TRY :
				return parseTry(stmt);
			case SYNCHRONIZED :
				return parseSynchronisedBlock(stmt);
			case CLASS :
				// this is a strange case for inner classes
				return parseClass(stmt);
			default :
				throw new SyntaxError("Unknown statement encountered ("
						+ stmt.getText() + ")", stmt.getLine(), stmt
						.getCharPositionInLine(), stmt.getText().length());
		}
	}

	/**
     * This method is responsible for parsing a block of code, which is a set of
     * statements between '{' and '}'
     * 
     * @param tree
     *            block to parse
     * @return A Block containing all the statements in this block
     * 
     */
	protected Stmt.Block parseBlock(Tree tree) {
		ArrayList<Stmt> stmts = new ArrayList<Stmt>();

		// === ITERATE STATEMENTS ===

		for (int i = 0; i != tree.getChildCount(); ++i) {
			Stmt stmt = parseStatement(tree.getChild(i));
			stmts.add(stmt);
		}

		return new Stmt.Block(stmts, new SourceLocation(tree.getLine(),
				tree.getCharPositionInLine()));
	}

	/**
     * This method is responsible for parsing a synchronized block of code, e.g,
     * "synchronised(e.list()) { ... }"
     * 
     * @param tree
     *            block to parse
     * @return A Block containing all the statements in this block
     * 
     */
	protected Stmt parseSynchronisedBlock(Tree tree) {
		ArrayList<Stmt> stmts = new ArrayList<Stmt>();
		Expr e = parseExpression(tree.getChild(0));

		// === ITERATE STATEMENTS ===
		Tree child = tree.getChild(1);
		for (int i = 0; i != child.getChildCount(); ++i) {
			Stmt stmt = parseStatement(child.getChild(i));
			stmts.add(stmt);
		}

		return new Stmt.SynchronisedBlock(e, stmts, new SourceLocation(tree
				.getLine(), tree.getCharPositionInLine()));
	}

	protected Stmt parseTry(Tree tree) {
		ArrayList<Stmt> stmts = new ArrayList<Stmt>();
		ArrayList<Stmt.CatchBlock> handlers = new ArrayList<Stmt.CatchBlock>();
		Stmt.Block finallyBlk = null;

		// === ITERATE STATEMENTS ===
		Tree child = tree.getChild(0);
		for (int i = 0; i != child.getChildCount(); ++i) {
			Stmt stmt = parseStatement(child.getChild(i));
			stmts.add(stmt);
		}

		for (int i = 1; i < tree.getChildCount(); ++i) {
			ArrayList<Stmt> cbstmts = new ArrayList<Stmt>();
			child = tree.getChild(i);

			if (child.getType() == CATCH) {
				Tree cb = child.getChild(0);
				Type.Clazz type = parseClassType(cb.getChild(0));
				Tree cbb = child.getChild(1);
				for (int j = 0; j != cbb.getChildCount(); ++j) {
					Stmt stmt = parseStatement(cbb.getChild(j));
					cbstmts.add(stmt);
				}
				handlers.add(new Stmt.CatchBlock(type, cb.getChild(1)
						.getText(), cbstmts, new SourceLocation(cb.getLine(),
						cb.getCharPositionInLine())));
			} else {
				finallyBlk = parseBlock(child.getChild(0));
			}
		}

		return new Stmt.TryCatchBlock(
				handlers,
				finallyBlk,
				stmts,
				new SourceLocation(tree.getLine(), tree.getCharPositionInLine()));
	}

	/**
     * Responsible for translating variable declarations. ANTLR tree format:
     * 
     * VARDEF MODIFIERS? TYPE NAME [= EXPRESSION]
     * 
     * @param tree
     *            ANTLR if-statement tree
     * @return
     */
	protected Stmt parseVarDef(Tree tree) {
		ArrayList<Triple<String, Integer, Expr>> vardefs = new ArrayList<Triple<String, Integer, Expr>>();

		// === MODIFIERS ===
		List<Modifier> modifiers = parseModifiers(tree.getChild(0));

		Type type = parseType(tree.getChild(1));

		for (int i = 2; i < tree.getChildCount(); i = i + 1) {
			Tree nameTree = tree.getChild(i);
			String myName = nameTree.getText();

			Expr myInitialiser = null;
			// Parse array type modifiers (if there are any)
			int dims = 0;
			for (int j = 0; j < nameTree.getChildCount(); j = j + 1) {
				Tree am = nameTree.getChild(j);
				if (am.getText().equals("[")) {
					dims++;
				} else {
					// If we get here, then we've hit an initialiser
					myInitialiser = parseExpression(am);
				}
			}
			vardefs.add(new Triple<String, Integer, Expr>(
					myName, dims, myInitialiser));
		}

		return new Stmt.VarDef(
				modifiers,
				type,
				vardefs,
				new SourceLocation(tree.getLine(), tree.getCharPositionInLine()));
	}

	/**
     * This method parses an assignment statement.
     * 
     * @param tree
     * @return
     */
	protected Stmt.Assignment parseAssign(Tree tree) {
		Expr lhs = parseExpression(tree.getChild(0));
		Expr rhs = parseExpression(tree.getChild(1));
		return new Stmt.Assignment(lhs, rhs, new SourceLocation(tree
				.getLine(), tree.getCharPositionInLine()));
	}

	protected Stmt parseReturn(Tree tree) {
		if (tree.getChildCount() > 0) {
			return new Stmt.Return(parseExpression(tree.getChild(0)),
					new SourceLocation(tree.getLine(), tree
							.getCharPositionInLine()));
		} else {
			return new Stmt.Return(null, new SourceLocation(tree.getLine(),
					tree.getCharPositionInLine()));
		}
	}

	protected Stmt parseThrow(Tree tree) {
		return new Stmt.Throw(
				parseExpression(tree.getChild(0)),
				new SourceLocation(tree.getLine(), tree.getCharPositionInLine()));
	}

	protected Stmt parseAssert(Tree tree) {
		Expr expr = parseExpression(tree.getChild(0));
		return new Stmt.Assert(expr, new SourceLocation(tree.getLine(),
				tree.getCharPositionInLine()));
	}

	/**
     * Responsible for parsing break statements.
     * 
     * @param tree
     * @param label
     * @param cfg
     * @return
     */
	protected Stmt parseBreak(Tree tree) {
		if (tree.getChildCount() > 0) {
			// this is a labelled break statement.
			return new Stmt.Break(tree.getChild(0).getText(),
					new SourceLocation(tree.getLine(), tree
							.getCharPositionInLine()));
		} else {
			return new Stmt.Break(null, new SourceLocation(tree.getLine(),
					tree.getCharPositionInLine()));
		}
	}

	/**
     * Responsible for parsing continue statements.
     * 
     * @param tree
     * @param label
     * @param cfg
     * @return
     */
	protected Stmt parseContinue(Tree tree) {
		if (tree.getChildCount() > 0) {
			// this is a labelled continue statement.
			return new Stmt.Continue(tree.getChild(0).getText(),
					new SourceLocation(tree.getLine(), tree
							.getCharPositionInLine()));
		} else {
			return new Stmt.Continue(null, new SourceLocation(tree
					.getLine(), tree.getCharPositionInLine()));
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
	protected Stmt parseLabel(Tree stmt) {
		String label = stmt.getChild(0).getText();
		Stmt s = parseStatement(stmt.getChild(1));

		return new Stmt.Label(label, s, new SourceLocation(stmt.getLine(),
				stmt.getCharPositionInLine()));
	}

	protected Stmt parseIf(Tree stmt) {
		Expr condition = parseExpression(stmt.getChild(0));
		Stmt trueStmt = parseStatement(stmt.getChild(1));
		Stmt falseStmt = stmt.getChildCount() < 3
				? null
				: parseStatement(stmt.getChild(2));
		return new Stmt.If(
				condition,
				trueStmt,
				falseStmt,
				new SourceLocation(stmt.getLine(), stmt.getCharPositionInLine()));
	}

	protected Stmt parseWhile(Tree stmt) {
		Expr condition = parseExpression(stmt.getChild(0)
				.getChild(0));
		Stmt body = parseStatement(stmt.getChild(1));
		return new Stmt.While(condition, body, new SourceLocation(stmt
				.getLine(), stmt.getCharPositionInLine()));
	}

	protected Stmt parseDoWhile(Tree stmt) {
		Expr condition = parseExpression(stmt.getChild(0)
				.getChild(0));
		Stmt body = parseStatement(stmt.getChild(1));
		return new Stmt.DoWhile(condition, body, new SourceLocation(stmt
				.getLine(), stmt.getCharPositionInLine()));
	}

	protected Stmt parseFor(Tree stmt) {
		Stmt initialiser = null;
		Expr condition = null;
		Stmt increment = null;
		Stmt body = null;

		if (stmt.getChild(0).getType() == FOREACH) {
			return parseForEach(stmt.getChild(0), stmt.getChild(1));
		}

		if (stmt.getChild(0).getChildCount() > 0) {
			initialiser = parseStatement(stmt.getChild(0).getChild(0));
		}
		if (stmt.getChild(1).getChildCount() > 0) {
			condition = parseExpression(stmt.getChild(1).getChild(0));
		}
		if (stmt.getChild(2).getChildCount() > 0) {
			increment = parseStatement(stmt.getChild(2).getChild(0));
		}
		if (stmt.getChild(3).getChildCount() > 0) {
			body = parseStatement(stmt.getChild(3));
		}

		return new Stmt.For(
				initialiser,
				condition,
				increment,
				body,
				new SourceLocation(stmt.getLine(), stmt.getCharPositionInLine()));
	}

	/**
     * Responsible for translating Java 1.5 for statements. ANTLR tree format:
     * 
     * FOR FOREACH [YUCK NEED TO FIX THIS] VARDEF VAR STATEMENT
     * 
     * @param stmt
     *            ANTLR for-statement tree
     * @param cfg
     *            control-flow graph
     * @return
     */
	protected Stmt parseForEach(Tree stmt, Tree body) {

		Tree varDef = stmt.getChild(0);
		List<Modifier> varMods = parseModifiers(varDef.getChild(0));
		Type varType = parseType(varDef.getChild(1));
		String varName = varDef.getChild(2).getText();
		Expr src = parseExpression(stmt.getChild(1));
		Stmt loopBody = parseStatement(body);

		return new Stmt.ForEach(
				varMods,
				varName,
				varType,
				src,
				loopBody,
				new SourceLocation(stmt.getLine(), stmt.getCharPositionInLine()));
	}

	protected Stmt parseSwitch(Tree stmt) {
		// Second, process the expression to switch on
		Expr condition = parseExpression(stmt.getChild(0));
		ArrayList<Stmt.Case> cases = new ArrayList<Stmt.Case>();

		for (int i = 1; i < stmt.getChildCount(); i++) {
			Tree child = stmt.getChild(i);
			if (child.getType() == CASE) {
				Expr c = parseExpression(child.getChild(0));
				List<Stmt> stmts = null;
				if (child.getChild(1) != null) {
					Stmt.Block b = parseBlock(child.getChild(1));
					stmts = b.statements();
				}
				cases.add(new Stmt.Case(c, stmts, new SourceLocation(child
						.getLine(), child.getCharPositionInLine())));
			} else {
				// default label
				List<Stmt> stmts = new ArrayList<Stmt>();
				if (child.getChild(0) != null) {
					Stmt.Block b = parseBlock(child.getChild(0));
					stmts = b.statements();
				}
				cases.add(new Stmt.DefaultCase(stmts, new SourceLocation(
						child.getLine(), child.getCharPositionInLine())));
			}
		}
		return new Stmt.Switch(condition, cases, new SourceLocation(stmt
				.getLine(), stmt.getCharPositionInLine()));
	}

	/**
     * Parse a standalone pre/post inc/dec statement (e.g. ++i, --i, etc)
     * 
     * @param stmt
     * @return
     */
	public Stmt parseIncDec(Tree stmt) {
		Expr.UnOp lhs = (Expr.UnOp) parseExpression(stmt);
		Expr lval = lhs.expr();
		SourceLocation loc = new SourceLocation(stmt.getLine(), stmt
				.getCharPositionInLine());
		if (lhs.op() == Expr.UnOp.POSTDEC
				|| lhs.op() == Expr.UnOp.PREDEC) {
			return new Stmt.Assignment(lval,
					new Expr.BinOp(Expr.BinOp.SUB, lval,
							new Value.Int(1, loc), loc), loc);
		} else {
			// must be preinc or postinc
			return new Stmt.Assignment(lval,
					new Expr.BinOp(Expr.BinOp.ADD, lval,
							new Value.Int(1, loc), loc), loc);
		}
	}

	public Stmt parseSelectorStmt(Tree stmt) {
		Expr e = parseExpression(stmt);
		if (e instanceof Stmt) {
			return (Stmt) e;
		} else {
			throw new RuntimeException("Syntax Error");
		}
	}

	protected Expr parseExpression(Tree expr) {
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
				return parseUnOp(Expr.UnOp.PREINC, expr);
			case PREDEC :
				return parseUnOp(Expr.UnOp.PREDEC, expr);
			case POSTINC :
				return parseUnOp(Expr.UnOp.POSTINC, expr);
			case POSTDEC :
				return parseUnOp(Expr.UnOp.POSTDEC, expr);
			case NEG :
				return parseUnOp(Expr.UnOp.NEG, expr);
			case NOT :
				return parseUnOp(Expr.UnOp.NOT, expr);
			case INV :
				return parseUnOp(Expr.UnOp.INV, expr);
			case CAST :
				return parseCast(expr);
			case LABINOP :
				return parseLeftAssociativeBinOp(expr);
			case USHR :
				return parseBinOp(Expr.BinOp.USHR, expr);
			case LAND :
				return parseBinOp(Expr.BinOp.LAND, expr);
			case LOR :
				return parseBinOp(Expr.BinOp.LOR, expr);
			case AND :
				return parseBinOp(Expr.BinOp.AND, expr);
			case OR :
				return parseBinOp(Expr.BinOp.OR, expr);
			case XOR :
				return parseBinOp(Expr.BinOp.XOR, expr);
			case EQ :
				return parseBinOp(Expr.BinOp.EQ, expr);
			case NEQ :
				return parseBinOp(Expr.BinOp.NEQ, expr);
			case LT :
				return parseBinOp(Expr.BinOp.LT, expr);
			case LTEQ :
				return parseBinOp(Expr.BinOp.LTEQ, expr);
			case GT :
				return parseBinOp(Expr.BinOp.GT, expr);
			case GTEQ :
				return parseBinOp(Expr.BinOp.GTEQ, expr);
			case INSTANCEOF :
				return parseInstanceOf(expr);
			case TERNOP :
				return parseTernOp(expr);
			case ASSIGN :
				return parseAssign(expr);
			default :
				throw new SyntaxError("Unknown expression encountered ("
						+ expr.getText() + ")", expr.getLine(), expr
						.getCharPositionInLine(), expr.getText().length());
		}
	}

	protected Expr parseSelector(Tree selector) {
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
		Expr expr = parseExpression(target);

		for (int i = idx; i != selector.getChildCount(); ++i) {
			Tree child = selector.getChild(i);
			SourceLocation loc = new SourceLocation(child.getLine(), child
					.getCharPositionInLine());
			switch (child.getType()) {
				case DEREF :
					expr = new Expr.Deref(expr,
							child.getChild(0).getText(), loc);
					break;
				case ARRAYINDEX : {
					expr = new Expr.ArrayIndex(expr, parseExpression(child
							.getChild(0)), loc);
					break;
				}
				case INVOKE : {
					int start = 0;
					ArrayList<Type> typeParameters = new ArrayList<Type>();
					if (child.getChild(0).getType() == TYPE_PARAMETER) {
						Tree c = child.getChild(0);
						for (int j = 0; j != c.getChildCount(); ++j) {
							typeParameters.add(parseType(c.getChild(j)));
						}
						start++;
					}

					String method = child.getChild(start).getText();

					List<Expr> params = parseExpressionList(
							start + 1, child.getChildCount(), child);

					expr = new Expr.Invoke(expr, method, params,
							typeParameters, loc);
					break;
				}
				case NEW : {
					Expr.New tmp = parseNew(child);
					tmp.setContext(expr);
					expr = tmp;
					break;
				}
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
	protected Expr.New parseNew(Tree expr) {
		// first, parse any parameters supplied
		ArrayList<Decl> declarations = new ArrayList<Decl>();

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

		List<Expr> params = parseExpressionList(1, end, expr);

		return new Expr.New(parseType(expr.getChild(0)), null, params,
				declarations, new SourceLocation(expr.getLine(), expr
						.getCharPositionInLine()));
	}

	/**
     * This method parses an isolated invoke call. For example, "f()" is
     * isolated, whilst "x.f()" or "this.f()" etc are not.
     * 
     * @param expr
     * @return
     */
	public Expr.Invoke parseInvoke(Tree expr) {

		// =================================================
		// ======== PARSE TYPE PARAMETERS (IF ANY) =========
		// =================================================
		int start = 0;

		// First, check for type parameters. These are present for
		// method invocations which explicitly indicate the type
		// parameters to use. For example, x.<K>someMethod();
		ArrayList<Type> typeParameters = new ArrayList<Type>();

		if (expr.getChild(0).getType() == TYPE_PARAMETER) {
			Tree child = expr.getChild(0);
			for (int i = 0; i != child.getChildCount(); ++i) {
				typeParameters.add(parseType(child.getChild(i)));
			}
			start++;
		}

		String method = expr.getChild(start).getText();

		List<Expr> params = parseExpressionList(start + 1, expr
				.getChildCount(), expr);

		return new Expr.Invoke(
				null,
				method,
				params,
				typeParameters,
				new SourceLocation(expr.getLine(), expr.getCharPositionInLine()));
	}

	public Expr parseGetClass(Tree expr) {
		return new Value.Class(parseClassType(expr.getChild(0)));
	}

	protected Expr parseTernOp(Tree expr) {
		Expr cond = parseExpression(expr.getChild(0));
		Expr tbranch = parseExpression(expr.getChild(1));
		Expr fbranch = parseExpression(expr.getChild(2));
		return new Expr.TernOp(cond, tbranch, fbranch, new SourceLocation(
				expr.getLine(), expr.getCharPositionInLine()));
	}

	protected Expr parseInstanceOf(Tree expr) {
		Expr e = parseExpression(expr.getChild(0));
		return new Expr.InstanceOf(
				e,
				parseType(expr.getChild(1)),
				new SourceLocation(expr.getLine(), expr.getCharPositionInLine()));
	}

	protected Expr parseCast(Tree expr) {
		return new Expr.Cast(parseType(expr.getChild(0)),
				parseExpression(expr.getChild(1)), new SourceLocation(expr
						.getLine(), expr.getCharPositionInLine()));
	}

	protected Expr parseUnOp(int uop, Tree expr) {
		return new Expr.UnOp(
				uop,
				parseExpression(expr.getChild(0)),
				new SourceLocation(expr.getLine(), expr.getCharPositionInLine()));
	}

	// Binary operations which can be left associative are more complex and have
	// to be delt with using a special LABINOP operator.
	protected Expr parseLeftAssociativeBinOp(Tree expr) {
		Expr lhs = parseExpression(expr.getChild(0));

		for (int i = 1; i < expr.getChildCount(); i = i + 2) {
			Tree child = expr.getChild(i + 1);
			int bop = parseBinOpOp(expr.getChild(i).getText(), expr);
			lhs = new Expr.BinOp(bop, lhs, parseExpression(child),
					new SourceLocation(child.getLine(), child
							.getCharPositionInLine()));
		}

		return lhs;
	}

	protected Expr parseBinOp(int bop, Tree expr) {
		Expr lhs = parseExpression(expr.getChild(0));
		Expr rhs = parseExpression(expr.getChild(1));

		return new Expr.BinOp(bop, lhs, rhs, new SourceLocation(expr
				.getLine(), expr.getCharPositionInLine()));
	}

	protected int parseBinOpOp(String op, Tree expr) {
		if (op.equals("+")) {
			return Expr.BinOp.ADD;
		} else if (op.equals("-")) {
			return Expr.BinOp.SUB;
		} else if (op.equals("/")) {
			return Expr.BinOp.DIV;
		} else if (op.equals("*")) {
			return Expr.BinOp.MUL;
		} else if (op.equals("%")) {
			return Expr.BinOp.MOD;
		} else if (op.equals("<")) {
			return Expr.BinOp.SHL;
		} else if (op.equals(">")) {
			return Expr.BinOp.SHR;
		} else {
			throw new SyntaxError(
					"Unknown left-associative binary operator encountered ('"
							+ op + "').", expr.getLine(), expr
							.getCharPositionInLine(), expr.getText().length());
		}
	}

	protected Expr parseVariable(Tree expr) {
		String name = expr.getChild(0).getText();
		return new Expr.Variable(name, new SourceLocation(expr.getLine(),
				expr.getCharPositionInLine()));
	}

	protected Expr parseCharVal(Tree expr) {
		String charv = expr.getChild(0).getText();
		Value.Char v = null;
		SourceLocation loc = new SourceLocation(expr.getLine(), expr
				.getCharPositionInLine());
		if (charv.length() == 3) {
			v = new Value.Char(charv.charAt(1), loc);
		} else {
			String tmp = charv.substring(1, charv.length() - 1);
			if (tmp.equals("\\b"))
				v = new Value.Char('\b', loc);
			else if (tmp.equals("\\t"))
				v = new Value.Char('\t', loc);
			else if (tmp.equals("\\f"))
				v = new Value.Char('\f', loc);
			else if (tmp.equals("\\n"))
				v = new Value.Char('\n', loc);
			else if (tmp.equals("\\r"))
				v = new Value.Char('\r', loc);
			else if (tmp.equals("\\\""))
				v = new Value.Char('\"', loc);
			else if (tmp.equals("\\\\"))
				v = new Value.Char('\\', loc);
			else if (tmp.equals("\\'"))
				v = new Value.Char('\'', loc);
			else if (Character.isDigit(tmp.charAt(1))) {
				int octal_val = Integer.parseInt(
						tmp.substring(1, tmp.length()), 8);
				v = new Value.Char((char) octal_val);
			} else if (tmp.startsWith("\\u")) {
				// including "slash u"
				String unicode = tmp.substring(2, 6);
				v = new Value.Char((char) Integer.parseInt(unicode, 16),
						loc);
			} else {
				throw new SyntaxError("Unable to parse character constant: "
						+ tmp, expr.getLine(), expr.getCharPositionInLine(),
						expr.getText().length());
			}
		}
		return v;
	}

	protected Expr parseBoolVal(Tree expr) {
		Value.Bool v = new Value.Bool(Boolean.parseBoolean(expr
				.getChild(0).getText()), new SourceLocation(expr.getLine(),
				expr.getCharPositionInLine()));
		return v;
	}

	protected Expr parseIntVal(Tree expr) {
		int radix = 10;
		String value = expr.getChild(0).getText();
		if (value.startsWith("0x")) {
			// HEX value
			radix = 16;
			value = value.substring(2);
		}
		char lc = value.charAt(value.length() - 1);

		long val = parseLongVal(value.substring(0, value.length() - 1), radix);

		SourceLocation loc = new SourceLocation(expr.getLine(),expr.getCharPositionInLine());
		
		if (lc == 'l' || lc == 'L') {
			// return new LongVal(Long.parseLong(value.substring(0,
			// value.length() - 1), radix));
			return new Value.Long(val, loc);
		} else if (radix == 10 && (lc == 'f' || lc == 'F')) {
			return new Value.Float(val, loc);
		} else if (radix == 10 && (lc == 'd' || lc == 'D')) {
			return new Value.Double(val,loc);
		}

		val = parseLongVal(value, radix);
		return new Value.Int((int) val, loc);
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

	protected Expr parseStringVal(Tree expr) {
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
							if (Character.isDigit(v.charAt(i + 1))) {
								// Handle octal escape codes here.
								//
								// Octal escapes are upto 4 characters long. So,
								// we need to figure out exactly how long!
								for (len = 1; len != 4; ++len) {
									if ((i + len) >= v.length()
											|| !Character.isDigit(v.charAt(i
													+ len))) {
										break;
									}
								}
								int octal_val = Integer.parseInt(v.substring(
										i + 1, i + len), 8);
								replace = (char) octal_val;
							} else {
								throw new SyntaxError(
										"Unable to escape character: " + v,
										expr.getLine(), expr
												.getCharPositionInLine(), expr
												.getText().length());
							}
					}
					v = v.substring(0, i) + replace + v.substring(i + len);
				}
			}
		}
		// finally, construct a new string in the FlowGraph
		return new Value.String(v,new SourceLocation(expr.getLine(),expr.getCharPositionInLine()));
	}

	protected Expr parseNullVal(Tree expr) {
		return new Value.Null(new SourceLocation(expr.getLine(),expr.getCharPositionInLine()));
	}

	/**
     * This parses a floating point value. Note that this may correspond to a
     * Java float, or a Java double!
     */
	protected Expr parseFloatVal(Tree expr) {
		String val = expr.getChild(0).getText();

		char lc = val.charAt(val.length() - 1);
		Expr r;
		if (lc == 'f' || lc == 'F') {
			r = new Value.Float(Float.parseFloat(val),
					new SourceLocation(expr.getLine(), expr
							.getCharPositionInLine()));
		} else {
			r = new Value.Double(Double.parseDouble(val),
					new SourceLocation(expr.getLine(), expr
							.getCharPositionInLine()));
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
	protected Expr parseArrayVal(Tree expr) {
		List<Expr> values = parseExpressionList(0, expr
				.getChildCount(), expr);
		
		return new Value.Array(values, new SourceLocation(expr.getLine(),
				expr.getCharPositionInLine()));
	}

	/**
     * Parse a typed array initialiser expression. This is distinct from an
     * array initialiser in a subtle way. To generate a typed array initiliser
     * you must specify the class of array to construct. For example:
     * 
     * <pre>
     * Object[] test = new Object[]{&quot;abc&quot;, new Integer(2)};
     * </pre>
     * 
     * 
     * @param expr
     * @return
     */
	protected Expr parseTypedArrayVal(Tree expr) {
		Type type = parseType(expr.getChild(0));
		Tree aval = expr.getChild(1);
		List<Expr> values = parseExpressionList(0, aval
				.getChildCount(), aval);
		
		return new Value.TypedArray(type, values, new SourceLocation(expr
				.getLine(), expr.getCharPositionInLine()));
	}

	protected List<Expr> parseExpressionList(int start, int end,
			Tree expr) {

		ArrayList<Expr> es = new ArrayList<Expr>();

		for (int i = start; i < end; i++) {
			es.add(parseExpression(expr.getChild(i)));
		}

		return es;
	}

	protected List<Modifier> parseModifiers(Tree ms) {
		ArrayList<Modifier> mods = new ArrayList<Modifier>();
		for (int i = 0; i != ms.getChildCount(); ++i) {
			Tree mc = ms.getChild(i);
			SourceLocation loc = new SourceLocation(mc.getLine(),mc.getCharPositionInLine());
			String m = mc.getText();
			if (m.equals("public")) {
				mods.add(new Modifier.Base(java.lang.reflect.Modifier.PUBLIC,loc));
			} else if (m.equals("private")) {
				mods.add(new Modifier.Base(java.lang.reflect.Modifier.PRIVATE,loc));
			} else if (m.equals("protected")) {
				mods.add(new Modifier.Base(java.lang.reflect.Modifier.PROTECTED,loc));
			} else if (m.equals("static")) {
				mods.add(new Modifier.Base(java.lang.reflect.Modifier.STATIC,loc));
			} else if (m.equals("abstract")) {
				mods.add(new Modifier.Base(java.lang.reflect.Modifier.ABSTRACT,loc));
			} else if (m.equals("final")) {
				mods.add(new Modifier.Base(java.lang.reflect.Modifier.FINAL,loc));
			} else if (m.equals("native")) {
				mods.add(new Modifier.Base(java.lang.reflect.Modifier.NATIVE,loc));
			} else if (m.equals("synchronized")) {
				mods.add(new Modifier.Base(java.lang.reflect.Modifier.SYNCHRONIZED,loc));
			} else if (m.equals("transient")) {
				mods.add(new Modifier.Base(java.lang.reflect.Modifier.TRANSIENT,loc));
			} else if (m.equals("volatile")) {
				mods.add(new Modifier.Base(java.lang.reflect.Modifier.VOLATILE,loc));
			} else if (m.equals("strictfp")) {
				mods.add(new Modifier.Base(java.lang.reflect.Modifier.STRICT,loc));
			} else if (mc.getType() == ANNOTATION) {
				String name = mc.getChild(0).getText();
				ArrayList<Expr> arguments = new ArrayList<Expr>();
				for (int j = 1; j != mc.getChildCount(); ++j) {
					arguments.add(parseExpression(mc.getChild(j)));
				}
				mods.add(new Modifier.Annotation(name, arguments,loc));
			} else {
				throw new SyntaxError("not expecting " + m, mc.getLine(), mc
						.getCharPositionInLine());
			}
		}
		return mods;
	}

	protected static Type parseType(Tree type) {
		assert type.getType() == TYPE;

		SourceLocation loc = new SourceLocation(type.getLine(), type
				.getCharPositionInLine());
		
		if (type.getChild(0).getText().equals("?")) {
			// special case to deal with wildcards
			Tree child = type.getChild(0);

			Type.Clazz lowerBound = null;
			Type.Clazz upperBound = null;

			if (child.getChildCount() > 0
					&& child.getChild(0).getType() == EXTENDS) {
				lowerBound = parseClassType(child.getChild(0).getChild(0));

			} else if (child.getChildCount() > 0
					&& child.getChild(0).getType() == SUPER) {

				upperBound = parseClassType(child.getChild(0).getChild(0));
			}
			// Ok, all done!
			return new Type.Wildcard(lowerBound, upperBound);
		} else {

			// === ARRAY DIMENSIONS ===

			int dims = 0;

			for (int i = type.getChildCount() - 1; i > 0; --i) {
				if (!type.getChild(i).getText().equals("[")) {
					break;
				}
				dims++;
			}

			// === PRIMITIVE TYPES ===

			Type r;
			
			String ct = type.getChild(0).getText();						
			
			if (ct.equals("boolean")) {
				r = new Type.Bool();
			} else if (ct.equals("byte")) {
				r = new Type.Byte();
			} else if (ct.equals("char")) {
				r = new Type.Char();
			} else if (ct.equals("short")) {
				r = new Type.Short();
			} else if (ct.equals("int")) {								
				r = new Type.Int();
			} else if (ct.equals("long")) {
				r = new Type.Long();
			} else if (ct.equals("float")) {
				r = new Type.Float();
			} else if (ct.equals("double")) {
				r = new Type.Double();
			} else {

				// === NON-PRIMITIVE TYPES ===

				ArrayList<Pair<String, List<Type>>> components = new ArrayList<Pair<String, List<Type>>>();

				for (int i = 0; i != (type.getChildCount() - dims); ++i) {
					Tree child = type.getChild(i);

					String text = child.getText();
					if (text.equals("VOID")) {
						text = "void"; // hack!
					}
					ArrayList<Type> genArgs = new ArrayList<Type>();

					for (int j = 0; j != child.getChildCount(); ++j) {
						Tree childchild = child.getChild(j);
						genArgs.add(parseType(childchild));
					}

					components.add(new Pair<String, List<Type>>(text,
							genArgs));
				}

				r = new Type.Clazz(components);

			}
			
			for (int i = 0; i != dims; ++i) {
				r = new Type.Array(r);
			}
			
			return r;
		}
	}

	protected static Type.Clazz parseClassType(Tree type) {
		assert type.getType() == TYPE;

		// === COMPONENTS ===

		ArrayList<Pair<String, List<Type>>> components = new ArrayList<Pair<String, List<Type>>>();

		for (int i = 0; i != type.getChildCount(); ++i) {
			Tree child = type.getChild(i);

			String text = child.getText();
			if (text.equals("VOID")) {
				text = "void"; // hack!
			}
			ArrayList<Type> genArgs = new ArrayList<Type>();

			for (int j = 0; j != child.getChildCount(); ++j) {
				Tree childchild = child.getChild(j);
				if (childchild.getType() == EXTENDS) {
					// this is a lower bound, not a generic argument.
				} else {
					genArgs.add(parseType(childchild));
				}
			}

			components
					.add(new Pair<String, List<Type>>(text, genArgs));
		}

		return new Type.Clazz(components);
	}

	protected static Type.Variable parseVariableType(Tree type) {
		Tree child = type.getChild(0);
		String text = child.getText();
		List<Type> lowerBounds = new ArrayList<Type>();

		if (child.getChildCount() > 0 && child.getChild(0).getType() == EXTENDS) {
			Tree childchild = child.getChild(0);
			for (int i = 0; i != childchild.getChildCount(); ++i) {
				lowerBounds.add(parseType(childchild.getChild(i)));
			}
		}
		return new Type.Variable(text, lowerBounds, new SourceLocation(
				type.getLine(), type.getCharPositionInLine()));
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

	protected static final int STATIC_IMPORT = JavaParser.STATIC_IMPORT;

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

	protected static final int STATIC = JavaParser.STATIC;

	protected static final int ENUM_CONSTANT = JavaParser.ENUM_CONSTANT;
}
