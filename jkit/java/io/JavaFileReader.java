//This file is part of the Java Compiler Kit (JKit)

//The Java Compiler Kit is free software; you can 
//redistribute it and/or modify it under the terms of the 
//GNU General Public License as published by the Free Software 
//Foundation; either version 2 of the License, or (at your 
//option) any later version.

//The Java Compiler Kit is distributed in the hope
//that it will be useful, but WITHOUT ANY WARRANTY; without 
//even the implied warranty of MERCHANTABILITY or FITNESS FOR 
//A PARTICULAR PURPOSE.  See the GNU General Public License 
//for more details.

//You should have received a copy of the GNU General Public 
//License along with the Java Compiler Kit; if not, 
//write to the Free Software Foundation, Inc., 59 Temple Place, 
//Suite 330, Boston, MA  02111-1307  USA

//(C) David James Pearce, 2009. 

package jkit.java.io;

import java.io.*;
import java.util.*;

import jkit.java.parser.*;
import jkit.compiler.SyntaxError;
import jkit.java.tree.Decl;
import jkit.java.tree.Expr;
import jkit.java.tree.Stmt;
import jkit.java.tree.Type;
import jkit.java.tree.Value;
import jkit.java.tree.Decl.*;
import jkit.java.tree.Annotation;
import jkit.util.Pair;
import jkit.util.Triple;
import jkit.jil.*;
import jkit.jil.tree.Modifier;
import jkit.jil.tree.SourceLocation;

import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;

public class JavaFileReader {

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
	public JavaFileReader(String file) throws IOException {
		ast = parseInputFile(new FileReader(file));
	}

	/**
     * Create a JavaFileReader from a general Reader.
     * 
     * @param r
     *            the reader to read from
     * 
     * @throws IOException
     */
	public JavaFileReader(Reader r) throws IOException {		
		ast = parseInputFile(r);
	}

	/**
     * Create a JavaFileReader from a general InputStream
     * 
     * @param in
     *            the input stream to read from
     * 
     * @throws IOException
     */
	public JavaFileReader(InputStream in) throws IOException {		
		ast = parseInputFile(new InputStreamReader(in));
	}

	protected Tree parseInputFile(Reader reader) throws IOException {
		CommonTokenStream tokenStream = new CommonTokenStream(new JavaLexer(
				new ANTLRReaderStream(reader)));
		JavaParser parser = new JavaParser(tokenStream);
		try {
			Tree tree = (Tree) parser.compilationUnit().getTree();
			// printTree(tree,0,0);
			return tree;
		} catch (RecognitionException e) {
		}
		return null;
	}
	
	public JavaFile read() {
		ArrayList<Decl> classes = new ArrayList<Decl>();
		ArrayList<Pair<Boolean, String>> imports = new ArrayList<Pair<Boolean, String>>();
		String pkg = "";

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
					imports.add(new Pair(static_import, filter));
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
			classes.addAll(parseDeclaration(ast.getChild(i), new HashSet<String>()));
		}

		return new JavaFile(pkg, imports, classes);
	}

	protected List<Decl> parseDeclaration(Tree decl, HashSet<String> genericVariables) {
		ArrayList<Decl> declarations = new ArrayList<Decl>();

		switch (decl.getType()) {
			case FIELD :
				declarations.addAll(parseField(decl, genericVariables));
				break;
			case METHOD :
				declarations.add(parseMethod(decl, genericVariables));
				break;
			case CLASS :
			case INTERFACE :
				declarations.add(parseClass(decl, genericVariables));
				break;
			case ENUM :
				declarations.add(parseEnum(decl, genericVariables));
				break;
			case STATIC :
				// static initialiser block
				declarations.add(new Decl.StaticInitialiserBlock(parseBlock(
					decl.getChild(0), genericVariables).statements(),
					new SourceLocation(decl.getLine(), decl
							.getCharPositionInLine())));
				break;
			case BLOCK :
				// non-static initialiser block
				declarations.add(new Decl.InitialiserBlock(parseBlock(decl, genericVariables)
						.statements(), new SourceLocation(decl.getLine(), decl
						.getCharPositionInLine())));
				break;
			case ANNOTATION :
				declarations.add(parseAnnotation(decl, genericVariables));
				break;
		}

		return declarations;
	}

	protected Decl.JavaClass parseClass(Tree decl, HashSet<String> genericVariables) {
				
		// ====================================================================
		// =========================== PARSE MODIFIERS ========================
		// ====================================================================

		List<Modifier> modifiers = parseModifiers(decl.getChild(0), genericVariables);
				
		genericVariables = (HashSet<String>) genericVariables.clone();

		// ====================================================================
		// ======================== PARSE TYPE VARIABLES ======================
		// ====================================================================
		
		ArrayList<Type.Variable> typeArgs = parseTypeVariables(decl.getChild(1),genericVariables); 

		// ====================================================================
		// ============================== PARSE NAME ==========================
		// ====================================================================
		
		String name = decl.getChild(1).getText();
		
		// ====================================================================
		// ====================== PARSE EXTENDS CLAUSE ========================
		// ====================================================================

		Type.Clazz superclass = parseExtends(decl.getChild(2),genericVariables);
		
		// ====================================================================
		// ===================== PARSE IMPLEMENTS CLAUSE ======================
		// ====================================================================

		ArrayList<Type.Clazz> interfaces = parseImplements(decl.getChild(3), genericVariables);

		// ====================================================================
		// ======================== PARSE DECLARATIONS ========================
		// ====================================================================

		ArrayList<Decl> declarations = new ArrayList<Decl>();

		for (int i = 4; i < decl.getChildCount(); ++i) {
			declarations.addAll(parseDeclaration(decl.getChild(i), genericVariables));
		}

		SourceLocation loc = new SourceLocation(decl.getLine(), decl
				.getCharPositionInLine());

		if (decl.getType() == INTERFACE) {
			return new Decl.JavaInterface(modifiers, name, typeArgs,
					superclass, interfaces, declarations, loc);
		} else {
			return new Decl.JavaClass(modifiers, name, typeArgs, superclass,
					interfaces, declarations, loc);
		}
	}

	protected Type.Clazz parseExtends(Tree tree, HashSet<String> genericVariables) {
		if (tree.getChildCount() > 0 && tree.getType() == EXTENDS) {
			return parseClassType(tree.getChild(0),
					genericVariables);
		}
		return null;
	}
	
	protected ArrayList<Type.Clazz> parseImplements(Tree tree, HashSet<String> genericVariables) {		
		ArrayList<Type.Clazz> interfaces = new ArrayList();	
		for (int i = 0; i < tree.getChildCount(); ++i) {
			interfaces.add(parseClassType(tree.getChild(i), genericVariables));
		}		
		return interfaces;
	}
	
	protected Decl.JavaEnum parseEnum(Tree decl, HashSet<String> genericVariables) {		
		// ====================================================================
		// ========================= PARSE MODIFIERS ==========================
		// ====================================================================

		List<Modifier> modifiers = parseModifiers(decl.getChild(0), genericVariables);
		
		// ====================================================================
		// ============================ PARSE NAME ============================
		// ====================================================================
		
		String name = decl.getChild(1).getText();
		
		// ====================================================================
		// ===================== PARSE IMPLEMENTS CLAUSE ======================
		// ====================================================================
		ArrayList<Type.Clazz> interfaces = parseImplements(decl.getChild(2), genericVariables);
						
		// ====================================================================
		// ========================= PARSE CONSTANTS ==========================
		// ====================================================================
		
		int idx = 3;
		ArrayList<Decl.EnumConstant> constants = new ArrayList<Decl.EnumConstant>();
		while (idx < decl.getChildCount()
				&& decl.getChild(idx).getType() == ENUM_CONSTANT) {
			constants.add(parseEnumConstant(decl.getChild(idx), genericVariables));
			idx = idx + 1;
		}

		// ====================================================================
		// ======================== PARSE DECLARATIONS ========================
		// ====================================================================

		ArrayList<Decl> declarations = new ArrayList<Decl>();
		for (; idx < decl.getChildCount(); ++idx) {			
			declarations.addAll(parseDeclaration(decl.getChild(idx), genericVariables));
		}

		return new Decl.JavaEnum(modifiers, name, interfaces, constants,
				declarations, new SourceLocation(decl.getLine(), decl
						.getCharPositionInLine()));
	}

	protected Decl.EnumConstant parseEnumConstant(Tree decl, HashSet<String> genericVariables) {
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
					declarations.addAll(parseDeclaration(child, genericVariables));
					break;
				default :
					// in the default case, we must have an expression which is
                    // actually an argument.
					arguments.add(parseExpression(child, genericVariables));
			}
		}

		return new Decl.EnumConstant(
				name,
				arguments,
				declarations,
				new SourceLocation(decl.getLine(), decl.getCharPositionInLine()));
	}

	protected Decl.AnnotationInterface parseAnnotation(Tree decl,
			HashSet<String> genericVariables) {
		// === TYPE MODIFIERS ===

		List<Modifier> modifiers = parseModifiers(decl.getChild(0), genericVariables);
		int idx = 1;
		
		String name = decl.getChild(idx++).getText();

		ArrayList<Triple<Type, String, Value>> methods = new ArrayList<Triple<Type, String, Value>>();

		for (; idx < decl.getChildCount(); ++idx) {
			Tree child = decl.getChild(idx);
			if (child.getType() == METHOD) {
				Type t = parseType(child.getChild(1), genericVariables);
				String n = child.getChild(2).getText();
				Value v = null;
				if (child.getChildCount() > 3) {
					v = (Value) parseExpression(child.getChild(3), genericVariables);
				}

				methods.add(new Triple(t, n, v));
			}
		}

		return new Decl.AnnotationInterface(
				modifiers,
				name,
				methods,
				new SourceLocation(decl.getLine(), decl.getCharPositionInLine()-1));
	}

	protected Decl.JavaMethod parseMethod(Tree method, HashSet<String> genericVariables) {
		genericVariables = (HashSet<String>) genericVariables.clone(); 
		
		SourceLocation loc = determineStartLocation(method);
		
		// ====================================================================
		// =========================== PARSE MODIFIERS ========================
		// ====================================================================

		List<Modifier> modifiers = parseModifiers(method.getChild(0), genericVariables);

		// ====================================================================
		// ========================= PARSE TYPE VARIABLES =====================
		// ====================================================================

		ArrayList<Type.Variable> typeArgs = parseTypeVariables(method.getChild(1),genericVariables);		

		// ====================================================================
		// =============================== PARSE NAME =========================
		// ====================================================================
		
		String name = method.getChild(2).getText();
				
		Type returnType = null;

		// ====================================================================
		// ========================== PARSE RETURN TYPE =======================
		// ====================================================================		
		
		// if no return type, then is a constructor
		if (method.getChild(3).getType() == TYPE) {
			returnType = parseType(method.getChild(3), genericVariables);
		}		

		// ====================================================================
		// =========================== PARSE PARAMETERS =======================
		// ====================================================================		
	
		ArrayList<Decl.JavaParameter> params = parseParameters(
				method.getChild(4), genericVariables);		

		boolean varargs = hasVarArgs(method.getChild(4));
				
		// ====================================================================
		// ============================= PARSE THROWS =========================
		// ====================================================================		

		ArrayList<Type.Clazz> exceptions = parseThrows(method.getChild(5),genericVariables);

		// ====================================================================
		// ============================== PARSE BODY ==========================
		// ====================================================================		

		Stmt.Block block = null;

		if(method.getChildCount() > 6) {			
			block = parseBlock(method.getChild(6), genericVariables);
		}

		// ====================================================================
		// ========================== CREATE DECLARATION ======================
		// ====================================================================		
		
		if (returnType == null) {
			return new Decl.JavaConstructor(modifiers, name, params, varargs,
					typeArgs, exceptions, block, loc);
		} else {
			return new Decl.JavaMethod(modifiers, name, returnType, params,
					varargs, typeArgs, exceptions, block, loc);
		}
	}

	protected ArrayList<Type.Clazz> parseThrows(Tree tree,
			HashSet<String> genericVariables) {
		ArrayList<Type.Clazz> exceptions = new ArrayList();
		for (int i = 0; i != tree.getChildCount(); ++i) {
			exceptions.add(parseClassType(tree.getChild(i), genericVariables));
		}
		return exceptions;
	}
	
	protected List<Decl.JavaField> parseField(Tree tree, HashSet<String> genericVariables) {
		assert tree.getType() == FIELD;

		ArrayList<Decl.JavaField> fields = new ArrayList<Decl.JavaField>();

		// === MODIFIERS ===
		List<Modifier> modifiers = parseModifiers(tree.getChild(0), genericVariables);
		int idx = 1;
		
		// === FIELD TYPE ===
		Type type = parseType(tree.getChild(idx++), genericVariables);

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
				initialiser = parseExpression(child.getChild(aindx), genericVariables);
			}
			fields.add(new Decl.JavaField(modifiers, name, type, initialiser,
					determineStartLocation(tree)));
		}
		return fields;
	}

	protected Stmt parseStatement(Tree stmt, HashSet<String> genericVariables) {
		switch (stmt.getType()) {
			case BLOCK :
				return parseBlock(stmt, genericVariables);
			case VARDEF :
				return parseVarDef(stmt, genericVariables);
			case ASSIGN :
				return parseAssign(stmt, genericVariables);
			case ASSIGNOP :
				return parseAssignOp(stmt, genericVariables);
			case RETURN :
				return parseReturn(stmt, genericVariables);
			case THROW :
				return parseThrow(stmt, genericVariables);
			case NEW :
				return parseNew(stmt, genericVariables);
			case INVOKE :
				return parseInvoke(stmt, genericVariables);
			case IF :
				return parseIf(stmt, genericVariables);
			case SWITCH :
				return parseSwitch(stmt, genericVariables);
			case FOR :
				return parseFor(stmt, genericVariables);
			case WHILE :
				return parseWhile(stmt, genericVariables);
			case DOWHILE :
				return parseDoWhile(stmt, genericVariables);
			case SELECTOR :
				return parseSelectorStmt(stmt, genericVariables);
			case CONTINUE :
				return parseContinue(stmt, genericVariables);
			case BREAK :
				return parseBreak(stmt, genericVariables);
			case LABEL :
				return parseLabel(stmt, genericVariables);
			case POSTINC :
				return parseIncDec(Expr.UnOp.POSTINC, stmt, genericVariables);
			case PREINC :
				return parseIncDec(Expr.UnOp.PREINC, stmt, genericVariables);
			case POSTDEC :
				return parseIncDec(Expr.UnOp.POSTDEC, stmt, genericVariables);
			case PREDEC :
				return parseIncDec(Expr.UnOp.PREDEC, stmt, genericVariables);				
			case ASSERT :
				return parseAssert(stmt, genericVariables);
			case TRY :
				return parseTry(stmt, genericVariables);
			case SYNCHRONIZED :
				return parseSynchronisedBlock(stmt, genericVariables);
			case CLASS :
				// this is a strange case for inner classes
				return parseClass(stmt, genericVariables);
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
	protected Stmt.Block parseBlock(Tree tree, HashSet<String> genericVariables) {
		ArrayList<Stmt> stmts = new ArrayList<Stmt>();

		// === ITERATE STATEMENTS ===

		for (int i = 0; i != tree.getChildCount(); ++i) {
			Stmt stmt = parseStatement(tree.getChild(i), genericVariables);
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
	protected Stmt parseSynchronisedBlock(Tree tree, HashSet<String> genericVariables) {
		ArrayList<Stmt> stmts = new ArrayList<Stmt>();
		Expr e = parseExpression(tree.getChild(0), genericVariables);

		// === ITERATE STATEMENTS ===
		Tree child = tree.getChild(1);
		for (int i = 0; i != child.getChildCount(); ++i) {
			Stmt stmt = parseStatement(child.getChild(i), genericVariables);
			stmts.add(stmt);
		}

		return new Stmt.SynchronisedBlock(e, stmts, new SourceLocation(tree
				.getLine(), tree.getCharPositionInLine()));
	}

	protected Stmt parseTry(Tree tree, HashSet<String> genericVariables) {
		ArrayList<Stmt> stmts = new ArrayList<Stmt>();
		ArrayList<Stmt.CatchBlock> handlers = new ArrayList<Stmt.CatchBlock>();
		Stmt.Block finallyBlk = null;

		// === ITERATE STATEMENTS ===
		Tree child = tree.getChild(0);
		for (int i = 0; i != child.getChildCount(); ++i) {
			Stmt stmt = parseStatement(child.getChild(i), genericVariables);
			stmts.add(stmt);
		}

		for (int i = 1; i < tree.getChildCount(); ++i) {
			ArrayList<Stmt> cbstmts = new ArrayList<Stmt>();
			child = tree.getChild(i);

			if (child.getType() == CATCH) {
				Tree cb = child.getChild(0);
				Type.Clazz type = parseClassType(cb.getChild(0), genericVariables);
				Tree cbb = child.getChild(1);
				for (int j = 0; j != cbb.getChildCount(); ++j) {
					Stmt stmt = parseStatement(cbb.getChild(j), genericVariables);
					cbstmts.add(stmt);
				}
				handlers.add(new Stmt.CatchBlock(type, cb.getChild(1)
						.getText(), cbstmts, new SourceLocation(cb.getLine(),
						cb.getCharPositionInLine())));
			} else {
				finallyBlk = parseBlock(child.getChild(0), genericVariables);
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
	protected Stmt parseVarDef(Tree tree, HashSet<String> genericVariables) {
		ArrayList<Triple<String, Integer, Expr>> vardefs = new ArrayList<Triple<String, Integer, Expr>>();

		// === MODIFIERS ===
		List<Modifier> modifiers = parseModifiers(tree.getChild(0), genericVariables);

		Type type = parseType(tree.getChild(1), genericVariables);

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
					myInitialiser = parseExpression(am, genericVariables);
				}
			}
			vardefs.add(new Triple<String, Integer, Expr>(
					myName, dims, myInitialiser));
		}				
		
		return new Stmt.VarDef(
				modifiers,
				type,
				vardefs,
				new SourceLocation(tree.getChild(1).getLine(), tree.getChild(1).getCharPositionInLine()));
	}

	/**
     * This method parses an assignment statement.
     * 
     * @param tree
     * @return
     */
	protected Stmt.Assignment parseAssign(Tree tree, HashSet<String> genericVariables) {
		Expr lhs = parseExpression(tree.getChild(0), genericVariables);
		Expr rhs = parseExpression(tree.getChild(1), genericVariables);		
		return new Stmt.Assignment(lhs, rhs, new SourceLocation(tree
				.getLine(), tree.getCharPositionInLine()));
	}

	/**
     * This method parses an assignment op statement.
     * 
     * @param tree
     * @return
     */
	protected Stmt.AssignmentOp parseAssignOp(Tree tree, HashSet<String> genericVariables) {
		int op;
		String _op = tree.getChild(0).getText();
		if(_op.equals("ADD")) {
			op = Expr.BinOp.ADD;
		} else if(_op.equals("SUB")) {
			op = Expr.BinOp.SUB;
		} else if(_op.equals("MUL")) {
			op = Expr.BinOp.MUL;
		} else if(_op.equals("DIV")) {
			op = Expr.BinOp.DIV;
		} else if(_op.equals("AND")) {
			op = Expr.BinOp.AND;
		} else if(_op.equals("OR")) {
			op = Expr.BinOp.OR;
		} else if(_op.equals("XOR")) {
			op = Expr.BinOp.XOR;
		} else if(_op.equals("SHL")) {
			op = Expr.BinOp.SHL;
		} else if(_op.equals("SHR")) {
			op = Expr.BinOp.SHR;
		} else {
			op = Expr.BinOp.USHR;
		}
		Expr lhs = parseExpression(tree.getChild(1), genericVariables);
		Expr rhs = parseExpression(tree.getChild(2), genericVariables);
						
		return new Stmt.AssignmentOp(op, lhs, rhs, new SourceLocation(tree
				.getLine(), tree.getCharPositionInLine()));
	}
	
	protected Stmt parseReturn(Tree tree, HashSet<String> genericVariables) {
		if (tree.getChildCount() > 0) {
			return new Stmt.Return(parseExpression(tree.getChild(0), genericVariables),
					new SourceLocation(tree.getLine(), tree
							.getCharPositionInLine()));
		} else {
			return new Stmt.Return(null, new SourceLocation(tree.getLine(),
					tree.getCharPositionInLine()));
		}
	}

	protected Stmt parseThrow(Tree tree, HashSet<String> genericVariables) {
		return new Stmt.Throw(
				parseExpression(tree.getChild(0), genericVariables),
				new SourceLocation(tree.getLine(), tree.getCharPositionInLine()));
	}

	protected Stmt parseAssert(Tree tree, HashSet<String> genericVariables) {
		Expr expr = parseExpression(tree.getChild(0), genericVariables);
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
	protected Stmt parseBreak(Tree tree, HashSet<String> genericVariables) {
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
	protected Stmt parseContinue(Tree tree, HashSet<String> genericVariables) {
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
	protected Stmt parseLabel(Tree stmt, HashSet<String> genericVariables) {
		String label = stmt.getChild(0).getText();
		Stmt s = parseStatement(stmt.getChild(1), genericVariables);

		return new Stmt.Label(label, s, new SourceLocation(stmt.getLine(),
				stmt.getCharPositionInLine()));
	}

	protected Stmt parseIf(Tree stmt, HashSet<String> genericVariables) {
		Expr condition = parseExpression(stmt.getChild(0), genericVariables);
		Stmt trueStmt = parseStatement(stmt.getChild(1), genericVariables);
		Stmt falseStmt = stmt.getChildCount() < 3
				? null
				: parseStatement(stmt.getChild(2), genericVariables);
		return new Stmt.If(
				condition,
				trueStmt,
				falseStmt,
				new SourceLocation(stmt.getLine(), stmt.getCharPositionInLine()));
	}

	protected Stmt parseWhile(Tree stmt, HashSet<String> genericVariables) {
		Expr condition = parseExpression(stmt.getChild(0)
				.getChild(0), genericVariables);
		Stmt body = parseStatement(stmt.getChild(1), genericVariables);
		return new Stmt.While(condition, body, new SourceLocation(stmt
				.getLine(), stmt.getCharPositionInLine()));
	}

	protected Stmt parseDoWhile(Tree stmt, HashSet<String> genericVariables) {
		Expr condition = parseExpression(stmt.getChild(0)
				.getChild(0), genericVariables);
		Stmt body = parseStatement(stmt.getChild(1), genericVariables);
		return new Stmt.DoWhile(condition, body, new SourceLocation(stmt
				.getLine(), stmt.getCharPositionInLine()));
	}

	protected Stmt parseFor(Tree stmt, HashSet<String> genericVariables) {
		Stmt initialiser = null;
		Expr condition = null;
		Stmt increment = null;
		Stmt body = null;

		if (stmt.getChild(0).getType() == FOREACH) {
			return parseForEach(stmt.getChild(0), stmt.getChild(1), genericVariables);
		}

		if (stmt.getChild(0).getChildCount() > 0) {
			int childCount = stmt.getChild(0).getChildCount(); 
			if(childCount == 1) {
				initialiser = parseStatement(stmt.getChild(0).getChild(0), genericVariables);
			} else {
				// need a block
				ArrayList<Stmt> inits = new ArrayList();
				for(int i=0;i!=childCount;++i) {
					Stmt s = parseStatement(stmt.getChild(0).getChild(i), genericVariables);
					inits.add(s);
				}
				initialiser = new Stmt.Block(inits);
			}
		}
		if (stmt.getChild(1).getChildCount() > 0) {
			condition = parseExpression(stmt.getChild(1).getChild(0), genericVariables);
		}
		if (stmt.getChild(2).getChildCount() > 0) {
			int childCount = stmt.getChild(2).getChildCount(); 
			if(childCount == 1) {
				increment = parseStatement(stmt.getChild(2).getChild(0), genericVariables);
			} else {
				ArrayList<Stmt> incs = new ArrayList();
				for(int i=0;i!=childCount;++i) {
					Stmt s = parseStatement(stmt.getChild(2).getChild(i), genericVariables);
					incs.add(s);
				}
				increment = new Stmt.Block(incs);	
			}
		}
		if (stmt.getChild(3).getChildCount() > 0) {
			body = parseStatement(stmt.getChild(3), genericVariables);
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
	protected Stmt parseForEach(Tree stmt, Tree body, HashSet<String> genericVariables) {

		Tree varDef = stmt.getChild(0);
		List<Modifier> varMods = parseModifiers(varDef.getChild(0), genericVariables);
		Type varType = parseType(varDef.getChild(1), genericVariables);
		String varName = varDef.getChild(2).getText();
		Expr src = parseExpression(stmt.getChild(1), genericVariables);
		Stmt loopBody = parseStatement(body, genericVariables);

		return new Stmt.ForEach(
				varMods,
				varName,
				varType,
				src,
				loopBody,
				new SourceLocation(stmt.getLine(), stmt.getCharPositionInLine()));
	}

	protected Stmt parseSwitch(Tree stmt, HashSet<String> genericVariables) {
		// Second, process the expression to switch on
		Expr condition = parseExpression(stmt.getChild(0), genericVariables);
		ArrayList<Stmt.Case> cases = new ArrayList<Stmt.Case>();

		for (int i = 1; i < stmt.getChildCount(); i++) {
			Tree child = stmt.getChild(i);
			if (child.getType() == CASE) {
				Expr c = parseExpression(child.getChild(0), genericVariables);
				List<Stmt> stmts = null;
				if (child.getChild(1) != null) {
					Stmt.Block b = parseBlock(child.getChild(1), genericVariables);
					stmts = b.statements();
				}
				cases.add(new Stmt.Case(c, stmts, new SourceLocation(child
						.getLine(), child.getCharPositionInLine())));
			} else {
				// default label
				List<Stmt> stmts = new ArrayList<Stmt>();
				if (child.getChild(0) != null) {
					Stmt.Block b = parseBlock(child.getChild(0), genericVariables);
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
     * Parse a standalone pre/post inc/dec statement (e.g. ++i, --i, etc).
     * 
     * @param stmt
     * @return
     */
	public Stmt parseIncDec(int op, Tree stmt, HashSet<String> genericVariables) {
		Expr.UnOp lhs = (Expr.UnOp) parseExpression(stmt, genericVariables);
		return new Stmt.PrePostIncDec(op,lhs.expr(),lhs.attributes());
	}

	public Stmt parseSelectorStmt(Tree stmt, HashSet<String> genericVariables) {
		Expr e = parseExpression(stmt, genericVariables);
		if (e instanceof Stmt) {
			return (Stmt) e;
		} else {
			throw new RuntimeException("Syntax Error");
		}
	}

	protected Expr parseExpression(Tree expr, HashSet<String> genericVariables) {
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
				return parseArrayVal(expr, genericVariables);
			case ARRAYINIT :
				return parseTypedArrayVal(expr, genericVariables);
			case VAR :
				return parseVariable(expr, genericVariables);
			case NEW :
				return parseNew(expr, genericVariables);
			case INVOKE :
				return parseInvoke(expr, genericVariables);
			case SELECTOR :
				return parseSelector(expr, genericVariables);
			case GETCLASS :
				return parseGetClass(expr, genericVariables);
			case PREINC :
				return parseUnOp(Expr.UnOp.PREINC, expr, genericVariables);
			case PREDEC :
				return parseUnOp(Expr.UnOp.PREDEC, expr, genericVariables);
			case POSTINC :
				return parseUnOp(Expr.UnOp.POSTINC, expr, genericVariables);
			case POSTDEC :
				return parseUnOp(Expr.UnOp.POSTDEC, expr, genericVariables);
			case NEG :
				return parseUnOp(Expr.UnOp.NEG, expr, genericVariables);
			case NOT :
				return parseUnOp(Expr.UnOp.NOT, expr, genericVariables);
			case INV :
				return parseUnOp(Expr.UnOp.INV, expr, genericVariables);
			case CAST :
				return parseCast(expr, genericVariables);
			case LABINOP :
				return parseLeftAssociativeBinOp(expr, genericVariables);
			case USHR :
				return parseBinOp(Expr.BinOp.USHR, expr, genericVariables);
			case LAND :
				return parseBinOp(Expr.BinOp.LAND, expr, genericVariables);
			case LOR :
				return parseBinOp(Expr.BinOp.LOR, expr, genericVariables);
			case AND :
				return parseBinOp(Expr.BinOp.AND, expr, genericVariables);
			case OR :
				return parseBinOp(Expr.BinOp.OR, expr, genericVariables);
			case XOR :
				return parseBinOp(Expr.BinOp.XOR, expr, genericVariables);
			case EQ :
				return parseBinOp(Expr.BinOp.EQ, expr, genericVariables);
			case NEQ :
				return parseBinOp(Expr.BinOp.NEQ, expr, genericVariables);
			case LT :
				return parseBinOp(Expr.BinOp.LT, expr, genericVariables);
			case LTEQ :
				return parseBinOp(Expr.BinOp.LTEQ, expr, genericVariables);
			case GT :
				return parseBinOp(Expr.BinOp.GT, expr, genericVariables);
			case GTEQ :
				return parseBinOp(Expr.BinOp.GTEQ, expr, genericVariables);
			case INSTANCEOF :
				return parseInstanceOf(expr, genericVariables);
			case TERNOP :
				return parseTernOp(expr, genericVariables);
			case ASSIGN :
				return parseAssign(expr, genericVariables);
			case ASSIGNOP:
				return parseAssignOp(expr, genericVariables);
			default :
				throw new SyntaxError("Unknown expression encountered ("
						+ expr.getText() + ")", expr.getLine(), expr
						.getCharPositionInLine(), expr.getText().length());
		}
	}

	protected Expr parseSelector(Tree selector, HashSet<String> genericVariables) {
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
		Expr expr = parseExpression(target, genericVariables);

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
							.getChild(0), genericVariables), loc);
					break;
				}
				case INVOKE : {
					int start = 0;
					ArrayList<Type> typeParameters = new ArrayList<Type>();
					if (child.getChild(0).getType() == TYPE_PARAMETER) {
						Tree c = child.getChild(0);
						for (int j = 0; j != c.getChildCount(); ++j) {
							typeParameters.add(parseType(c.getChild(j), genericVariables));
						}
						start++;
					}

					String method = child.getChild(start).getText();

					List<Expr> params = parseExpressionList(
							start + 1, child.getChildCount(), child, genericVariables);

					expr = new Expr.Invoke(expr, method, params,
							typeParameters, loc);
					break;
				}
				case NEW : {
					Expr.New tmp = parseNew(child, genericVariables);
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
	protected Expr.New parseNew(Tree expr, HashSet<String> genericVariables) {
		// first, parse any parameters supplied
		ArrayList<Decl> declarations = new ArrayList<Decl>();

		int end = expr.getChildCount();
		for (int i = 1; i < expr.getChildCount(); ++i) {
			Tree child = expr.getChild(i);
			if (child.getType() == METHOD) {
				// Store anonymous class methods
				declarations.add(parseMethod(child, genericVariables));
				end = Math.min(i, end);
			} else if (child.getType() == FIELD) {
				declarations.addAll(parseField(child, genericVariables));
				end = Math.min(i, end);
			} else if (child.getType() == BLOCK) {
				declarations.addAll(parseDeclaration(child, genericVariables));
				end = Math.min(i, end);
			}
		}

		List<Expr> params = parseExpressionList(1, end, expr, genericVariables);

		return new Expr.New(parseType(expr.getChild(0), genericVariables), null, params,
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
	public Expr.Invoke parseInvoke(Tree expr, HashSet<String> genericVariables) {

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
				typeParameters.add(parseType(child.getChild(i), genericVariables));
			}
			start++;
		}

		String method = expr.getChild(start).getText();

		List<Expr> params = parseExpressionList(start + 1, expr
				.getChildCount(), expr, genericVariables);

		return new Expr.Invoke(
				null,
				method,
				params,
				typeParameters,
				new SourceLocation(expr.getLine(), expr.getCharPositionInLine()));
	}

	public Expr parseGetClass(Tree expr, HashSet<String> genericVariables) {		
		return new Value.Class(parseType(expr.getChild(0), genericVariables));
	}

	protected Expr parseTernOp(Tree expr, HashSet<String> genericVariables) {
		Expr cond = parseExpression(expr.getChild(0), genericVariables);
		Expr tbranch = parseExpression(expr.getChild(1), genericVariables);
		Expr fbranch = parseExpression(expr.getChild(2), genericVariables);
		return new Expr.TernOp(cond, tbranch, fbranch, new SourceLocation(
				expr.getLine(), expr.getCharPositionInLine()));
	}

	protected Expr parseInstanceOf(Tree expr, HashSet<String> genericVariables) {
		Expr e = parseExpression(expr.getChild(0), genericVariables);
		return new Expr.InstanceOf(
				e,
				parseType(expr.getChild(1), genericVariables),
				new SourceLocation(expr.getLine(), expr.getCharPositionInLine()));
	}

	protected Expr parseCast(Tree expr, HashSet<String> genericVariables) {
		return new Expr.Cast(parseType(expr.getChild(0), genericVariables),
				parseExpression(expr.getChild(1), genericVariables), new SourceLocation(expr
						.getLine(), expr.getCharPositionInLine()));
	}

	protected Expr parseUnOp(int uop, Tree expr, HashSet<String> genericVariables) {
		
		Expr e = parseExpression(expr.getChild(0), genericVariables); 
		
		if(e instanceof Value) {
			// this means we can propagate the constant
			if(e instanceof Value.Int) {
				int x = ((Value.Int)e).value();
				switch(uop) {
					case Expr.UnOp.NEG:
						return new Value.Int(-x,e.attributes());
					case Expr.UnOp.INV:
						return new Value.Int(~x,e.attributes());					
				}				
			} else if(e instanceof Value.Long) {
				long x = ((Value.Long)e).value();
				switch(uop) {
					case Expr.UnOp.NEG:
						return new Value.Long(-x,e.attributes());
					case Expr.UnOp.INV:
						return new Value.Long(~x,e.attributes());					
				}
			} else if(e instanceof Value.Float) {
				float x = ((Value.Float)e).value();
				switch(uop) {
					case Expr.UnOp.NEG:
						return new Value.Float(-x,e.attributes());								
				}
			} else if(e instanceof Value.Double) {
				double x = ((Value.Double)e).value();
				switch(uop) {
					case Expr.UnOp.NEG:
						return new Value.Double(-x,e.attributes());								
				}
			} 
		}
		
		return new Expr.UnOp(
				uop,
				e,
				new SourceLocation(expr.getLine(), expr.getCharPositionInLine()));
	}

	// Binary operations which can be left associative are more complex and have
	// to be delt with using a special LABINOP operator.
	protected Expr parseLeftAssociativeBinOp(Tree expr, HashSet<String> genericVariables) {
		Expr lhs = parseExpression(expr.getChild(0), genericVariables);

		for (int i = 1; i < expr.getChildCount(); i = i + 2) {
			Tree child = expr.getChild(i + 1);
			int bop = parseBinOpOp(expr.getChild(i).getText(), expr, genericVariables);
			lhs = new Expr.BinOp(bop, lhs, parseExpression(child, genericVariables),
					new SourceLocation(child.getLine(), child
							.getCharPositionInLine()));
		}

		return lhs;
	}

	protected Expr parseBinOp(int bop, Tree expr, HashSet<String> genericVariables) {
		Expr lhs = parseExpression(expr.getChild(0), genericVariables);
		Expr rhs = parseExpression(expr.getChild(1), genericVariables);

		return new Expr.BinOp(bop, lhs, rhs, new SourceLocation(expr
				.getLine(), expr.getCharPositionInLine()));
	}

	protected int parseBinOpOp(String op, Tree expr, HashSet<String> genericVariables) {
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

	protected Expr parseVariable(Tree expr, HashSet<String> genericVariables) {
		String name = expr.getChild(0).getText();
		return new Expr.UnresolvedVariable(name, new SourceLocation(expr.getLine(),
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
		} else if(value.startsWith("0")) {
			radix = 8; // octal
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
	protected long parseLongVal(String in, int radix) {
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
	protected Expr parseArrayVal(Tree expr, HashSet<String> genericVariables) {
		List<Expr> values = parseExpressionList(0, expr
				.getChildCount(), expr, genericVariables);
		
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
	protected Expr parseTypedArrayVal(Tree expr, HashSet<String> genericVariables) {
		Type type = parseType(expr.getChild(0), genericVariables);
		Tree aval = expr.getChild(1);
		List<Expr> values = parseExpressionList(0, aval
				.getChildCount(), aval, genericVariables);
			
		return new Value.TypedArray(type, values, new SourceLocation(expr
				.getLine(), expr.getCharPositionInLine()));
	}

	protected List<Expr> parseExpressionList(int start, int end,
			Tree expr, HashSet<String> genericVariables) {

		ArrayList<Expr> es = new ArrayList<Expr>();

		for (int i = start; i < end; i++) {
			es.add(parseExpression(expr.getChild(i), genericVariables));
		}

		return es;
	}

	protected List<Modifier> parseModifiers(Tree ms, HashSet<String> genericVariables) {
		ArrayList<Modifier> mods = new ArrayList<Modifier>();
		for (int i = 0; i != ms.getChildCount(); ++i) {
			Tree modifier = ms.getChild(i);						
			mods.add(parseModifier(modifier,genericVariables));			
		}
		return mods;
	}
	
	protected Modifier parseModifier(Tree tree, HashSet<String> genericVariables) {
		String modifier = tree.getText();
		SourceLocation loc = new SourceLocation(tree.getLine(), tree
				.getCharPositionInLine());
		if (tree.getType() == ANNOTATION) {
			String name = tree.getChild(0).getText();
			ArrayList<Expr> arguments = new ArrayList<Expr>();
			for (int j = 1; j != tree.getChildCount(); ++j) {
				arguments.add(parseExpression(tree.getChild(j),
						genericVariables));
			}						
			return new Annotation(new Type.Clazz(name), arguments, loc);			
		} else if (modifier.charAt(0) <= 'p') {
			if (modifier.equals("public")) {
				return new Modifier.Public(loc);
			} else if (modifier.equals("private")) {
				return new Modifier.Private(loc);
			} else if (modifier.equals("protected")) {
				return new Modifier.Protected(loc);
			} else if (modifier.equals("abstract")) {
				return new Modifier.Abstract(loc);
			} else if (modifier.equals("final")) {
				return new Modifier.Final(loc);
			} else if (modifier.equals("native")) {
				return new Modifier.Native(loc);
			}
		} else if (modifier.charAt(0) > 'p') {
			if (modifier.equals("static")) {
				return new Modifier.Static(loc);
			} else if (modifier.equals("synchronized")) {
				return new Modifier.Synchronized(loc);
			} else if (modifier.equals("strictfp")) {
				return new Modifier.StrictFP(loc);
			} else if (modifier.equals("transient")) {
				return new Modifier.Transient(loc);
			} else if (modifier.equals("volatile")) {
				return new Modifier.Volatile(loc);
			}
		} 
		
		throw new SyntaxError("not expecting " + modifier, tree.getLine(), tree
					.getCharPositionInLine());					
	}
	
	protected ArrayList<Type.Variable> parseTypeVariables(Tree child,
			HashSet<String> genericVariables) {
		
		// NOTE. I LEAVE THE '<' IN THE ANTLR TREE TO ENSURE CORRECT LINE NUMBER
		// INFORMATION FOR GENERIC METHODS.
		
		ArrayList<Type.Variable> typeArgs = new ArrayList<Type.Variable>();
		for (int i = 1; i < child.getChildCount(); ++i) {
			Type.Variable tvar = parseVariableType(child.getChild(i),
					genericVariables);
									
			typeArgs.add(tvar);
			genericVariables.add(tvar.variable());
		}
		return typeArgs;
	}

	protected boolean hasVarArgs(Tree paramList) {
		for (int i = 0; i != paramList.getChildCount(); ++i) {
			Tree c = paramList.getChild(i);
			if (c.getType() == VARARGS) {
				return true;
			}
		}
		return false;
	}
	
	protected ArrayList<Decl.JavaParameter> parseParameters(
			Tree paramList, HashSet<String> genericVariables) {
		ArrayList<Decl.JavaParameter> params = new ArrayList();		
		
		for (int i = 0; i != paramList.getChildCount(); ++i) {
			Tree c = paramList.getChild(i);			
														
			List<Modifier> pModifiers = parseModifiers(c.getChild(0),
					genericVariables);
			Type t = parseType(c.getChild(1), genericVariables);
			
			SourceLocation loc;
			
			if(pModifiers.isEmpty()) {
				loc = (SourceLocation) t.attribute(SourceLocation.class);
			} else {			
				loc = (SourceLocation) pModifiers.get(0).attribute(SourceLocation.class);
			}
			
			String n = c.getChild(2).getText();

			for (int j = 3; j < c.getChildCount(); j = j + 2) {
				t = new Type.Array(t);
			}

			params.add(new Decl.JavaParameter(n, pModifiers, t, loc));			
		}

		return params;
	}
	
	protected Type parseType(Tree type, HashSet<String> genericVariables) {
		assert type.getType() == TYPE;

		SourceLocation loc = new SourceLocation(type.getLine(), type
				.getCharPositionInLine());
		
		if (type.getChild(0).getText().equals("?")) {
			// special case to deal with wildcards
			Tree child = type.getChild(0);

			Type.Reference lowerBound = null;
			Type.Reference upperBound = null;

			if (child.getChildCount() > 0
					&& child.getChild(0).getType() == EXTENDS) {			
				lowerBound = parseClassVarType(child.getChild(0).getChild(0), genericVariables);				
			} else if (child.getChildCount() > 0
					&& child.getChild(0).getType() == SUPER) {
				upperBound = parseClassVarType(child.getChild(0).getChild(0), genericVariables);
			}
			// Ok, all done!
			return new Type.Wildcard(lowerBound, upperBound,loc);			
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
			
			if (ct.equals("void")) { 
				r = new Type.Void(loc);
			} else if (ct.equals("boolean")) {
				r = new Type.Bool(loc);
			} else if (ct.equals("byte")) {
				r = new Type.Byte(loc);
			} else if (ct.equals("char")) {
				r = new Type.Char(loc);
			} else if (ct.equals("short")) {
				r = new Type.Short(loc);
			} else if (ct.equals("int")) {								
				r = new Type.Int(loc);
			} else if (ct.equals("long")) {
				r = new Type.Long(loc);
			} else if (ct.equals("float")) {
				r = new Type.Float(loc);
			} else if (ct.equals("double")) {
				r = new Type.Double(loc);
			} else if(genericVariables.contains(ct)) {				
				r = new Type.Variable(ct,null,loc);
			} else {

				// === NON-PRIMITIVE TYPES ===

				ArrayList<Pair<String, List<Type.Reference>>> components = new ArrayList<Pair<String, List<Type.Reference>>>();

				for (int i = 0; i != (type.getChildCount() - dims); ++i) {
					Tree child = type.getChild(i);

					String text = child.getText();
					ArrayList<Type.Reference> genArgs = new ArrayList<Type.Reference>();

					for (int j = 0; j != child.getChildCount(); ++j) {
						Tree childchild = child.getChild(j);
						genArgs.add((Type.Reference) parseType(childchild, genericVariables));
					}

					components.add(new Pair<String, List<Type.Reference>>(text,
							genArgs));
				}

				r = new Type.Clazz(components, loc);

			}
			
			for (int i = 0; i != dims; ++i) {
				r = new Type.Array(r,loc);
			}
			
			return r;
		}
	}

	protected Type.Reference parseClassVarType(Tree type, HashSet<String> genericVariables) {
		if(type.getChildCount() == 1 && genericVariables.contains(type.getChild(0).getText())) {
			SourceLocation loc = new SourceLocation(type.getLine(), type
					.getCharPositionInLine());
			
			return new Type.Variable(type.getChild(0).getText(),
					null,loc);
		} else {
			return parseClassType(type,genericVariables);
		}
	}
	
	protected Type.Clazz parseClassType(Tree type, HashSet<String> genericVariables) {
		assert type.getType() == TYPE;
		
		// === COMPONENTS ===

		ArrayList<Pair<String, List<Type.Reference>>> components = new ArrayList<Pair<String, List<Type.Reference>>>();

		for (int i = 0; i != type.getChildCount(); ++i) {
			Tree child = type.getChild(i);

			String text = child.getText();
			
			if (text.equals("VOID")) {
				text = "void"; // hack!
			}
			
			ArrayList<Type.Reference> genArgs = new ArrayList<Type.Reference>();

			for (int j = 0; j != child.getChildCount(); ++j) {
				Tree childchild = child.getChild(j);
				if (childchild.getType() == EXTENDS) {
					// this is a lower bound, not a generic argument.
				} else {
					genArgs.add((Type.Reference) parseType(childchild, genericVariables));
				}
			}

			components
					.add(new Pair<String, List<Type.Reference>>(text, genArgs));
		}

		return new Type.Clazz(components, new SourceLocation(
				type.getLine(), type.getCharPositionInLine()));
	}

	protected Type.Variable parseVariableType(Tree type, HashSet<String> genericVariables) {
		Tree child = type.getChild(0);
		String text = child.getText();
		
		genericVariables = (HashSet) genericVariables.clone();
		genericVariables.add(text); // needed for recursive type bounds
		
		List<Type.Reference> lowerBounds = new ArrayList<Type.Reference>();

		if (child.getChildCount() > 0 && child.getChild(0).getType() == EXTENDS) {
			Tree childchild = child.getChild(0);
			for (int i = 0; i != childchild.getChildCount(); ++i) {
				lowerBounds.add((Type.Reference) parseType(childchild
						.getChild(i), genericVariables));
			}
		}
		
		// here, we need to build an intersection type.
		SourceLocation loc = new SourceLocation(type.getLine(), type
				.getCharPositionInLine());
		
		if(lowerBounds.size() > 1) {			
			Type.Intersection lowerBound = new Type.Intersection(lowerBounds, loc);			
			return new Type.Variable(text, lowerBound, loc);
		} else if(lowerBounds.size() == 1) {
			return new Type.Variable(text, lowerBounds.get(0), loc);		
		} else {
			return new Type.Variable(text, null, loc);
		}
	}

	public SourceLocation determineStartLocation(Tree ast) {
		if(ast.getChildCount() == 0) {			
			return new SourceLocation(ast.getLine(),ast.getCharPositionInLine());			
		} else {			
			int line = ast.getLine();
			int col = ast.getCharPositionInLine();
			for(int i=0;i!=ast.getChildCount();++i) {
				SourceLocation l = determineStartLocation(ast.getChild(i));				
				if(l.line() != 0 && (line == 0 || l.line() < line)) {										
					line = l.line();
					col = l.column();
				} else if(line != 0 && l.line() == line && l.column() < col) {
					col = l.column();
				}
			}
			return new SourceLocation(line,col);
		}		
	}
	
	public void printTree(Tree ast, int n, int line) {
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
	
	protected static final int ASSIGNOP = JavaParser.ASSIGNOP;

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
