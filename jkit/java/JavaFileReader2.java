package jkit.java;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.*;

import jkit.compiler.SyntaxError;
import jkit.jkil.Field;
import jkit.jkil.Type;

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
			// printTree(ast, 0, -1);
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
				break;
			case METHOD:
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
			components.add(type.getChild(i).getText());
		}
		
		return new JavaFile.Type(components,dims);
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
