// This file is part of the Java Compiler Kit (JKit)
//
// The Java Compiler Kit is free software; you can 
// redistribute it and/or modify it under the terms of the 
// GNU General Public License as published by the Free Software 
// Foundation; either version 2 of the License, or (at your 
// option) any later version.
//
// The Java Compiler Kit is distributed in the hope
// that it will be useful, but WITHOUT ANY WARRANTY; without 
// even the implied warranty of MERCHANTABILITY or FITNESS FOR 
// A PARTICULAR PURPOSE.  See the GNU General Public License 
// for more details.
//
// You should have received a copy of the GNU General Public 
// License along with the Java Compiler Kit; if not, 
// write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA
//
// (C) David James Pearce, 2007. 

package jkit.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import jkit.bytecode.ClassFileReader;
import jkit.core.*;
import jkit.core.FlowGraph.*;
import jkit.core.Type.Reference;
import jkit.parser.JavaLexer;
import jkit.parser.JavaParser;
import jkit.util.Pair;
import jkit.util.Triple;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;

public class JavaFileReader implements ClassReader {
	protected ArrayList<String> imports = null;
	protected HashSet<String> innerClasses = null; // local class defs
	protected ArrayList<Clazz> declaredClasses = null;
	protected ArrayList<Clazz> anonClasses = null;
	protected Tree ast = null;

	protected static int scopeId = 0; // ScopeID addition

	/**
     * The class loader is needed to resolve types
     */
	protected jkit.core.ClassLoader loader;

	/**
     * The declared package of the class being parsed.
     */
	protected String enclosingPkg;

	/**
     * A scope object captures various pieces of information necessary to
     * resolve types and variable names properly.
     * 
     * @author djp
     * 
     */
	static class Scope {
		/**
         * This contains the set of variables which are defined in this scope.
         * For example, as we enter a block, a new set of variables may be
         * declared. Thus, new variables declared in that block are placed into
         * its set, which is popped off the stack when the block is finished.
         * 
         */
		final int id = scopeId++;

		final HashSet<String> variables = new HashSet<String>();
	}

	static class GenericScope extends Scope {
		/**
         * This contains the set of type variables which are currently in scope.
         * This is required in parseType to determine whether a type is a type
         * variable, or a user-defined type. Furthermore, each variable is
         * paired with a Type.Variable instance which contains the lowerbounds
         * (if any) of that variable. These are important for correctly
         * determining the actual (erased) type of a method.
         */
		final HashMap<String, Type.Variable> typeVariables = new HashMap<String, Type.Variable>();
	}

	static class MethodScope extends GenericScope {
		/**
         * This identifies the stack of enclosing methods. The stack increases
         * in height with each new method. Normally, there is only one level of
         * nest for methods. However, in the special case of an anonymous inner
         * class declared inside a method then in fact there can be a greater
         * nesting depth.
         */

		final int modifiers;

		public MethodScope(int modifiers) {
			this.modifiers = modifiers;
		}
	}

	static class ClassScope extends GenericScope {
		/**
         * This identifies the stack of enclosing classes. The stack increases
         * in height with each inner class, and it's maximum height is
         * determined by the maximum nesting of inner classes.
         */
		Type.Reference type;

		Type.Reference superClass = null;

		Clazz clazz = null;

		boolean hasConstructor = false;

		public ClassScope(Type.Reference enclosingClass) {
			this.type = enclosingClass;
		}
	}

	static class SwitchScope extends Scope {
		/**
         * Indicates the code point to which a "break" goes.
         */
		final Point breakPoint;

		public SwitchScope(Point breakPoint) {
			this.breakPoint = breakPoint;
		}
	}

	static class LoopScope extends SwitchScope {
		/**
         * Indicates the code point to which a "continue" goes.
         */
		final Point continuePoint;

		/**
         * The label given to a loop; maybe null if no label.
         */
		final String label;

		public LoopScope(Point continuePoint, Point breakPoint, String label) {
			super(breakPoint);
			this.continuePoint = continuePoint;
			this.label = label;
		}
	}

	/**
     * The stack of scopes. The reflects the nesting of classes, methods and
     * blocks.
     */
	protected Stack<Scope> scopes = new Stack<Scope>();

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
	public JavaFileReader(String file, jkit.core.ClassLoader loader)
			throws IOException {
		CommonTokenStream tokenStream = new CommonTokenStream(new JavaLexer(
				new ANTLRFileStream(file)));
		this.loader = loader;
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
	public JavaFileReader(Reader r, jkit.core.ClassLoader loader)
			throws IOException {
		CommonTokenStream tokenStream = new CommonTokenStream(new JavaLexer(
				new ANTLRReaderStream(r)));
		this.loader = loader;
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
	public JavaFileReader(InputStream in, jkit.core.ClassLoader loader)
			throws IOException {
		CommonTokenStream tokenStream = new CommonTokenStream(new JavaLexer(
				new ANTLRInputStream(in)));
		this.loader = loader;
		JavaParser parser = new JavaParser(tokenStream);
		try {
			ast = (Tree) parser.compilationUnit().getTree();
			// printTree(ast, 0, -1);
		} catch (RecognitionException e) {
		}
	}

	/**
     * Create a JavaFileReader from an ANTLR parse tree. This class is useful
     * for extending JavaFileReader with a new grammar.
     * 
     * @param tree
     *            The ANTLR parse tree
     * 
     * @throws IOException
     */
	public JavaFileReader(Tree tree, jkit.core.ClassLoader loader)
			throws IOException {
		this.loader = loader;
		ast = tree;
	}
	/**
     * Read the skeleton class from the given file (i.e. all class information
     * except method bodies).
     */
	public List<Clazz> readSkeletons() {

		scopeId = 0;
		return readClassFile(true);
	}

	/**
     * Read the complete class from the given file.
     */
	public List<Clazz> readClasses() {
		scopeId = 0;
		return readClassFile(false);
	}

	/**
     * This method returns the set of classes defined in the source file;
     * Observe that multiple classes may be defined due to inner classes.
     * 
     * @return
     */
	protected List<Clazz> readClassFile(boolean ignoreMethodBodies) {
		// Create a parser that reads from the scanner
		enclosingPkg = "";
		imports = new ArrayList<String>();
		innerClasses = new HashSet<String>();
		declaredClasses = new ArrayList<Clazz>();
		anonClasses = new ArrayList<Clazz>();
		searchForInnerClasses(ast, null, innerClasses);
		imports.add("java.lang.*");

		// now, go through the various definitions in the file

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
							enclosingPkg = c.getChild(j).getText();
						} else {
							enclosingPkg += "." + c.getChild(j).getText();
						}
					}
					break;
				default :
					break outer;
			}
		}

		imports.add(0, enclosingPkg + ".*");

		for (int i = 0; i != ast.getChildCount(); ++i) {
			Tree c = ast.getChild(i);
			switch (c.getType()) {
				case CLASS :
					declaredClasses.addAll(parseClassDeclaration(c, false,
							ignoreMethodBodies));
					break;
				case INTERFACE :
					declaredClasses.addAll(parseClassDeclaration(c, true,
							ignoreMethodBodies));
					break;
			}
		}

		// anonymous classes *must* be compiled after their enclosing classes.
		for (Clazz a : anonClasses) {
			declaredClasses.add(a);
		}

		return declaredClasses;
	}

	/**
     * This parses a (potentially inner) class declaration.
     * 
     * @param cd
     * @param type
     *            null if this is the outermost class
     * @param isInterface
     *            indicates whether this is an interface or not
     * @return All classes defined in this declaration. First in the list is the
     *         outermost.
     */
	protected List<Clazz> parseClassDeclaration(Tree cd, boolean isInterface,
			boolean ignoreMethodBodies) {
		ArrayList<Clazz> newclasses = new ArrayList<Clazz>();

		// === MODIFERS ===
		int idx = 0;
		int modifiers = 0;
		if (cd.getChild(idx).getType() == MODIFIERS) {
			modifiers = parseModifiers(cd.getChild(0));
			idx++;
		}
		if (isInterface) {
			modifiers |= Modifier.INTERFACE;
		}

		// === CLASS NAME ===

		Tree nc = cd.getChild(idx++);
		String name = nc.getText();
		ClassScope enclosingClass = getEnclosingClass();
		int classNestingDepth = enclosingClass == null
				? 0
				: enclosingClass.type.classes().length;
		@SuppressWarnings("unchecked")
		Pair<String, Type[]>[] classes = new Pair[classNestingDepth + 1];

		// ====================================================================
		// ================== PARSE GENERIC TYPE VARIABLES ====================
		// ====================================================================

		// At this stage I create an incomplete scope, since
		// we need it for parsing type variables, but until we've
		// done that, we don't have the necessary type object.
		ClassScope myScope = new ClassScope(null);
		scopes.push(myScope);

		Type typeParams[] = new Type[nc.getChildCount()];

		for (int i = 0; i != nc.getChildCount(); ++i) {
			// NEED TO DEAL WITH BOUNDS HERE
			Type.Variable typeVar = parseTypeParam(nc.getChild(i));
			typeParams[i] = typeVar;
			myScope.typeVariables.put(typeVar.name(), typeVar);
		}

		// Create the reference type
		if (enclosingClass != null) {
			int i = 0;
			for (; i != enclosingClass.type.classes().length; ++i) {
				classes[i] = enclosingClass.type.classes()[i];
			}
			classes[i] = new Pair<String, Type[]>(name, typeParams);
		} else {
			classes[0] = new Pair<String, Type[]>(name, typeParams);
		}

		Type.Reference type = Type.referenceType(enclosingPkg, classes);

		// Now, we can begin to complete the scope for this class
		myScope.type = type;

		// ====================================================================
		// ====================== PARSE EXTENDS CLAUSE ========================
		// ====================================================================

		Type.Reference superclass = Type.referenceType("java.lang", "Object");;
		if (idx < cd.getChildCount() && cd.getChild(idx).getType() == EXTENDS) {
			superclass = (Type.Reference) parseType(cd.getChild(idx++)
					.getChild(0));
		}
		myScope.superClass = superclass;

		// ====================================================================
		// ===================== PARSE IMPLEMENTS CLAUSE ======================
		// ====================================================================

		ArrayList<Type.Reference> interfaces = new ArrayList<Type.Reference>();
		if (idx < cd.getChildCount()
				&& cd.getChild(idx).getType() == IMPLEMENTS) {
			Tree ch = cd.getChild(idx++);
			for (int i = 0; i != ch.getChildCount(); ++i) {
				interfaces.add((Type.Reference) parseType(ch.getChild(i)));
			}
		}

		// ====================================================================
		// =========================== CREATE CLASS ===========================
		// ====================================================================

		Clazz myClass = new Clazz(modifiers, type, superclass, interfaces);
		newclasses.add(myClass);
		myScope.clazz = myClass;

		// ====================================================================
		// ======================= PARSE FIELD SKELETONS ======================
		// ====================================================================

		for (int i = idx; i < cd.getChildCount(); ++i) {
			Tree child = cd.getChild(i);
			if (child.getType() == FIELD) {
				List<Field> fields = parseFieldDeclaration(child, true);
				if (isInterface) {
					for (Field f : fields) {
						f.setModifiers(f.modifiers() | Modifier.STATIC
								| Modifier.FINAL | Modifier.PUBLIC);
					}
				}
				myClass.fields().addAll(fields);
			}
		}

		// ====================================================================
		// =================== PARSE METHOD SKELETONS =========================
		// ====================================================================

		// Hmmm, methods get scanned 3 times in all! This could be optimised ...
		for (int i = idx; i < cd.getChildCount(); ++i) {
			Tree child = cd.getChild(i);
			switch (child.getType()) {
				case METHOD :
					Method m = parseMethodDeclaration(child, true);
					if (isInterface) {
						m.setModifiers(m.modifiers() | Modifier.ABSTRACT
								| Modifier.PUBLIC);
					}
					myClass.methods().add(m);
					break;
			}
		}

		// ====================================================================
		// ================== PARSE METHODS / INNER CLASSES ===================
		// ====================================================================

		for (int i = idx; i < cd.getChildCount(); ++i) {
			Tree child = cd.getChild(i);
			switch (child.getType()) {
				case CLASS : {
					List<Clazz> cs = parseClassDeclaration(child, false,
							ignoreMethodBodies);
					Clazz inner = cs.get(0);
					myClass.inners().add(
							new Triple(inner.type(), inner.modifiers(), false));
					newclasses.addAll(cs);
					break;
				}
				case INTERFACE : {
					List<Clazz> cs = parseClassDeclaration(child, true,
							ignoreMethodBodies);
					Clazz inner = cs.get(0);
					myClass.inners().add(
							new Triple(inner.type(), inner.modifiers(), false));
					newclasses.addAll(cs);
					break;
				}
				case METHOD : {
					Method m = parseMethodDeclaration(child, ignoreMethodBodies);
					if (isInterface) {
						m.setModifiers(m.modifiers());
					}
					for (Method rm : myClass.methods(m.name())) {

						if (rm.type().equals(m.type())) {
							rm.setCode(m.code());
							break;
						}
					}
					break;
				}
				case FIELD : {
					List<Field> lf = parseFieldDeclaration(child,
							ignoreMethodBodies);
					for (Field f : lf) {
						if (isInterface) {
							f.setModifiers(f.modifiers() | Modifier.ABSTRACT
									| Modifier.PUBLIC);
						}
						for (Field rf : myClass.fields(f.name())) {
							if (rf.type() == null) {
								rf.setInitialiser(f.initialiser());
							} else if (rf.type().equals(f.type())) {
								rf.setInitialiser(f.initialiser());
								break;
							}
						}
					}
					break;
				}
				case BLOCK :
					Method m = new Method(Modifier.STATIC,
							new jkit.core.Type.Function(Type.voidType(),
									new Type[0], new Type.Variable[0]),
							"<clinit>", new LinkedList<Reference>());

					FlowGraph cfg = new FlowGraph(new ArrayList<LocalVarDef>());
					Block block = parseBlock(child, cfg);
					cfg.setEntry(block.entry());
					m.setCode(cfg);
					m.setPoint(block.entry());
					myClass.methods().add(m);
			}
		}

		// ====================================================================
		// ======================= DEFAULT CONSTRUCTOR ========================
		// ====================================================================

		// Add default constructor if no constructor has been defined.));

		if (!myScope.hasConstructor && !isInterface) {
			Point first = new Point(new Invoke(new Cast(myClass.superClass(),
					new LocalVar("this")), myClass.superClass().name(),
					new ArrayList<Expr>()));

			Point second = new Point(new Return(null));

			FlowGraph cfg = new FlowGraph(new ArrayList<LocalVarDef>(), first);

			Type[] paramTypes;

			if (enclosingClass != null && (modifiers & Modifier.STATIC) == 0) {
				// This is actually a non-static enclosing class. So, we need to
				// add support for the enclosing class parameter.
				paramTypes = new Type[1];
				paramTypes[0] = enclosingClass.type;
				cfg.localVariables()
						.add(
								new LocalVarDef("this$0", enclosingClass.type,
										0, true));
				Point third = new Point(new Assign(new Deref(new LocalVar(
						"this", myClass.type()), "this$0"), new LocalVar(
						"this$0")));
				cfg.add(new Triple<Point, Point, Expr>(first, third, null));
				cfg.add(new Triple<Point, Point, Expr>(third, second, null));
			} else {
				paramTypes = new Type[0];
				cfg.add(new Triple<Point, Point, Expr>(first, second, null));
			}

			Type.Function ctype = Type
					.functionType(Type.voidType(), paramTypes);

			Point point = new Point(null, "unknown", cd.getLine(), cd
					.getCharPositionInLine());

			myClass.methods().add(
					new Method(Modifier.PUBLIC, ctype, myClass.type().name(),
							new ArrayList<Type.Reference>(), point, cfg));
		}

		// ====================================================================
		// =================== INNER CLASS OWNER FIELD ========================
		// ====================================================================

		if (enclosingClass != null && !myClass.isStatic()
				&& !myClass.isInterface()) {
			myClass.fields().add(
					new Field(Modifier.FINAL, enclosingClass.type, "this$0"));
		}

		// we're leaving this class' scope
		scopes.pop();

		return newclasses;
	}

	/**
     * Parse a method declaration in the AST.
     * 
     * @param method
     * @return
     */
	protected Method parseMethodDeclaration(Tree method,
			boolean ignoreMethodBodies) {
		assert method.getType() == METHOD;

		// === TYPE MODIFIERS ===

		int modifiers = 0;
		int idx = 0;
		if (method.getChild(idx).getType() == MODIFIERS) {
			modifiers = parseModifiers(method.getChild(0));
			idx++;
		}

		// === TYPE ARGUMENTS ===

		MethodScope myScope = new MethodScope(modifiers);
		scopes.push(myScope);

		ArrayList<Type.Variable> typeArgs = new ArrayList<Type.Variable>();
		while (method.getChild(idx).getType() == TYPE_PARAMETER) {
			Type.Variable tv = parseTypeParam(method.getChild(idx++));
			typeArgs.add(tv);
			myScope.typeVariables.put(tv.name(), tv);
		}

		String name = method.getChild(idx++).getText();

		Type returnType = parseType(method.getChild(idx++));

		// Insert super() call if there is none, and this is a constructor
		// Determine whether this is a constructor or not
		ClassScope enclosingClass = getEnclosingClass();

		boolean isConstructor = name
				.equals(enclosingClass.type.classes()[enclosingClass.type
						.classes().length - 1].first());

		boolean isNonStaticInnerClass = isConstructor
				&& enclosingClass.clazz.isInnerClass()
				&& !enclosingClass.clazz.isStatic();

		// === Parse parameter types and their names ===

		ArrayList<String> params = new ArrayList<String>();
		ArrayList<Type> paramTypes = new ArrayList<Type>();

		if (isNonStaticInnerClass) {
			// Special case, where we need an extra parameter for the enclosing
			// class.
			params.add("this$0");
			paramTypes.add(getEnclosingClassOf(enclosingClass.type).type);
		}

		while (idx < method.getChildCount()
				&& method.getChild(idx).getType() == PARAMETER) {
			Tree c = method.getChild(idx);
			Type t = parseType(c.getChild(0));
			String n = c.getChild(1).getText();

			for (int i = 2; i < c.getChildCount(); i = i + 2) {
				t = Type.arrayType(t);
			}

			params.add(n);
			paramTypes.add(t);
			idx++;
		}

		if (idx < method.getChildCount()
				&& method.getChild(idx).getType() == VARARGS) {
			// This method declaration has a variable arity declaration.
			Tree c = method.getChild(idx);
			Type t = parseType(c.getChild(0));
			String n = c.getChild(1).getText();
			params.add(n);
			paramTypes.add(Type.arrayType(t));
			idx++;
			modifiers |= ClassFileReader.ACC_VARARGS;
		}

		// === THROWS CLAUSE ===

		ArrayList<Type.Reference> exceptions = new ArrayList<Type.Reference>();

		if (idx < method.getChildCount()
				&& method.getChild(idx).getType() == THROWS) {
			Tree tt = method.getChild(idx++);
			for (int i = 0; i != tt.getChildCount(); ++i) {
				exceptions.add((Type.Reference) parseType(tt.getChild(i)));
			}
		}

		Type.Function ft = Type.functionType(returnType, paramTypes
				.toArray(new Type[paramTypes.size()]), typeArgs
				.toArray(new Type.Variable[typeArgs.size()]));

		FlowGraph cfg = null;

		// === METHOD BODY ===

		if (idx < method.getChildCount()
				&& method.getChild(idx).getType() == BLOCK
				&& !ignoreMethodBodies) {
			// Translate method block

			// first, build the initial local variable mapping from parameters
			ArrayList<LocalVarDef> localDefinitions = new ArrayList<LocalVarDef>(); // reset
			// local
			// variables
			// declared
			// in
			// previous
			// method

			for (int i = 0; i != params.size(); ++i) {
				localDefinitions.add(new LocalVarDef(params.get(i), paramTypes
						.get(i), 0, true));
				myScope.variables.add(params.get(i));
			}
			Tree block = method.getChild(idx);

			cfg = new FlowGraph(localDefinitions, null);

			// second, parse the statements in the methods block
			Block blk = parseBlock(block, cfg);

			// Insert return statement if there isn't one. Note that the method
			// may not have a void return type, but this will be caught later
			// on. For now, we need to ensure that there is an entry point!
			if (blk.isEmpty()) {
				blk.add(new Point(new Return(null)));
			} else if (blk.hasSequentialExit()
					&& !(blk.sequentialExit().statement() instanceof Return)
					&& ft.returnType().equals(Type.voidType())) {
				Point last = blk.sequentialExit();
				Point ret = new Point(new Return(null), last.source(), last
						.line(), last.column());
				cfg.add(new Triple<Point, Point, Expr>(last, ret, null));
				blk.add(ret);
			}

			if (isConstructor) {
				// add call to super
				enclosingClass.hasConstructor = true;
				if (!isSuperOrThisCall(blk.entry().statement(),
						enclosingClass.clazz)) {
					Point entry = blk.entry();
					Point superCall = new Point(new Invoke(new Cast(
							enclosingClass.clazz.superClass(), new LocalVar(
									"this")), enclosingClass.clazz.superClass()
							.name(), new ArrayList<Expr>()), entry.source(),
							entry.line(), entry.column());
					cfg.add(new Triple<Point, Point, Expr>(superCall, entry,
							null));
					blk.add(0, superCall); // add to the front
				}
			}

			if (isNonStaticInnerClass) {
				// add assignment to this$0
				Point entry = blk.entry();
				Stmt assignment = new Assign(new Deref(new LocalVar("this"),
						"this$0"), new LocalVar("this$0"));
				Point ap = new Point(assignment, entry.source(), entry.line(),
						entry.column());
				cfg.add(new Triple<Point, Point, Expr>(ap, entry, null));
				blk.add(0, ap); // add to the front
			}

			cfg.setEntry(blk.entry());
		}

		scopes.pop();

		return new Method(modifiers, ft, name, exceptions, new Point(null,
				"unknown", method.getLine(), method.getCharPositionInLine()),
				cfg);
	}
	/**
     * Parse a field declaration in the AST.
     * 
     * @param field
     *            root of the field declaration in the AST
     * @return the set of fields defined, since one declaration can define more
     *         than one field. For example, "class X { int x,y }" has a single
     *         field declaration which defines two distinct fields "x" and "y".
     */
	protected List<Field> parseFieldDeclaration(Tree field,
			boolean ignoreInitialisers) {
		assert field.getType() == FIELD;

		ArrayList<Field> fields = new ArrayList<Field>();

		// === MODIFIERS ===
		int modifiers = 0;
		int idx = 0;
		if (field.getChild(idx).getType() == MODIFIERS) {
			modifiers = parseModifiers(field.getChild(0));
			idx++;
		}

		// === FIELD TYPE ===
		Type type = parseType(field.getChild(idx++));

		// === FIELD NAME(S) ===

		for (int i = idx; i < field.getChildCount(); ++i) {
			Tree child = field.getChild(i);
			String name = child.getText();
			Expr initialiser = null;
			Point point = codePoint(null, child);
			// A single "[" indicates an array
			int aindx = 0;
			while (aindx < child.getChildCount()
					&& child.getChild(aindx).getText().equals("[")) {
				type = Type.arrayType(type);
				aindx++;
			}
			if (aindx < child.getChildCount() && !ignoreInitialisers) {
				// FIXME: problem of side effects in initialisers. The only real
				// solution is to require that initialisers are inlined into
				// constructors!
				initialiser = parseExpression(child.getChild(aindx),
						new FlowGraph(new ArrayList())).first();
			}
			fields.add(new Field(modifiers, type, name, point, initialiser));
		}

		return fields;
	}

	/**
     * This deals with parsing type parameters which can be given on both class
     * and method declarations. For example, "class MyMap<X,Y> { ... }" gives
     * type parameters "X" and "Y", whilst "<G> void aMethod(...)" gives type
     * parameter "G".
     * 
     * @param type
     * @return
     */
	protected Type.Variable parseTypeParam(Tree type) {
		assert type.getType() == TYPE_PARAMETER;

		// === NAME ===

		String name = type.getChild(0).getText();

		// === TYPE BOUNDS ===

		Type[] lowerbounds;
		if (type.getChildCount() > 1) {
			Tree et = type.getChild(1);
			lowerbounds = new Type[et.getChildCount()];
			for (int i = 0; i < lowerbounds.length; ++i) {
				lowerbounds[i] = parseType(et.getChild(i));
			}
		} else {
			lowerbounds = new Type[0];
		}

		return Type.variableType(name, lowerbounds, new TypeElement[0]);
	}

	/**
     * This is one of the most important methods. It parses a type declaration
     * in the AST and returns an appropriate Type object. In order to do this,
     * it traverses the set of all known packages/classes as defined by the
     * CLASSPATH. If it cannot resolve a reference type, then it throws an
     * error.
     * 
     * @param type
     * @return
     */
	protected Type parseType(Tree type) {
		assert type.getType() == TYPE;

		// === ARRAY DIMENSIONS ===

		int dims = 0;

		// TODO: Extend this to support annotations in array reference position
		for (int i = type.getChildCount() - 1; i > 0; --i) {
			if (!type.getChild(i).getText().equals("[")) {
				break;
			}
			dims++;
		}

		// === PRIMITIVE TYPES ===

		Tree c = type.getChild(0);
		String ct = c.getText();
		if (ct.equals("boolean")) {
			if (dims == 0) {
				return Type.booleanType();
			} else {
				return Type.arrayType(dims, Type.booleanType());
			}
		} else if (ct.equals("byte")) {
			if (dims == 0) {
				return Type.byteType();
			} else {
				return Type.arrayType(dims, Type.byteType());
			}
		} else if (ct.equals("char")) {
			if (dims == 0) {
				return Type.charType();
			} else {
				return Type.arrayType(dims, Type.charType());
			}
		} else if (ct.equals("short")) {
			if (dims == 0) {
				return Type.shortType();
			} else {
				return Type.arrayType(dims, Type.shortType());
			}
		} else if (ct.equals("int")) {
			if (dims == 0) {
				return Type.intType();
			} else {
				return Type.arrayType(dims, Type.intType());
			}
		} else if (ct.equals("long")) {
			if (dims == 0) {
				return Type.longType();
			} else {
				return Type.arrayType(dims, Type.longType());
			}
		} else if (ct.equals("float")) {
			if (dims == 0) {
				return Type.floatType();
			} else {
				return Type.arrayType(dims, Type.floatType());
			}
		} else if (ct.equals("double")) {
			if (dims == 0) {
				return Type.doubleType();
			} else {
				return Type.arrayType(dims, Type.doubleType());
			}
		} else if (ct.equals("?")) {
			assert dims == 0;

			// need to check for use or extends or super here as well
			Type[] lowerbounds = new Type[0];
			Type[] upperbounds = new Type[0];
			if (type.getChildCount() > 1
					&& type.getChild(1).getType() == EXTENDS) {
				lowerbounds = new Type[1];
				lowerbounds[0] = parseType(type.getChild(1).getChild(0));
			} else if (type.getChildCount() > 1
					&& type.getChild(1).getType() == SUPER) {
				upperbounds = new Type[1];
				upperbounds[0] = parseType(type.getChild(1).getChild(0));
			}
			// Ok, all done!
			return Type.wildcardType(lowerbounds, upperbounds,
					new TypeElement[0]);
		} else if (c.getType() == VOID) {
			assert dims == 0;
			return Type.voidType();
		}

		// === TYPE VARIABLES ===

		for (Scope scope : scopes) {
			if (scope instanceof GenericScope) {
				GenericScope gscope = (GenericScope) scope;
				if (gscope.typeVariables.keySet().contains(ct)) {
					Type vt = gscope.typeVariables.get(ct);
					if (dims != 0) {
						vt = Type.arrayType(dims, vt);
					}
					return vt;
				}
			}
		}

		// === REFERENCE TYPES ===

		// Now, the tricky case. Need to split reference type into
		// the "package" bit, and the "classes" bit.
		//
		// AAA.BBB.CCC.DDD
		// ^^^^^^^ ^^^^^^^
		// Package Classes
		//
		// Then, if no package bit is given, need to attempt to identify
		// the package.

		String pkg = "";
		int i;
		for (i = 0; i != (type.getChildCount() - (dims + 1)); ++i) {
			String tmp;
			if (i == 0) {
				tmp = type.getChild(i).getText();
			} else {
				tmp = pkg + "." + type.getChild(i).getText();
			}
			if (!loader.isPackage(tmp)) {
				break;
			}
			pkg = tmp;
		}

		// Build the list of class names and their type bounds
		@SuppressWarnings("unchecked")
		Pair<String, Type[]>[] classes = new Pair[type.getChildCount()
				- (dims + i)];
		String className = "";

		for (int j = 0; i != (type.getChildCount() - dims); ++i, ++j) {
			c = type.getChild(i);
			Type typeParams[] = new Type[c.getChildCount()];
			for (int k = 0; k != c.getChildCount(); ++k) {
				// TO DO: deal with generic bounds and wildcards here
				typeParams[k] = parseType(c.getChild(k));
			}
			classes[j] = new Pair<String, Type[]>(c.getText(), typeParams);
			className += j == 0 ? c.getText() : "$" + c.getText();
		}

		Type.Reference retType = null;

		if (pkg.length() != 0) {
			retType = Type.referenceType(pkg, classes);
		} else {
			// No package information given. Need to resolve it!
			try {
				retType = resolveLocalType(classes);
			} catch (ClassNotFoundException e) {
				throw new SyntaxError("class \"" + classes[0].first()
						+ "\" not found.", type.getLine(), type
						.getCharPositionInLine(), type.getText().length());
			}
		}
		if (dims == 0) {
			return retType;
		} else {
			return Type.arrayType(dims, retType);
		}
	}

	// This method attempts to obtain the full prefix for a type, given only
	// a class name. It first considers static inner classes, then other classes
	// defined in this java file and, finally, it searches the classpath
	// appropriately.
	protected Type.Reference resolveLocalType(Pair<String, Type[]>[] classes)
			throws ClassNotFoundException {
		String pkg = "";
		String className = classes[0].first();

		// Check whether it's an inner class first. This is the hardest bit!
		Type.Reference enclosingClass = getEnclosingClass().type;
		if (enclosingClass != null) {
			for (int j = enclosingClass.classes().length; j >= 0; --j) {
				String lcn = j == 0 ? className : localTypeString(
						enclosingClass, j)
						+ "$" + className;
				if (innerClasses.contains(lcn)) {
					// Ok, found the right scope level, prepend the full
					// scope information from the enclosing class type and
					// eject.
					pkg = enclosingPkg;

					Pair<String, Type[]>[] r = new Pair[j + classes.length];
					Pair<String, Type[]>[] ecs = enclosingClass.classes();
					for (int k = 0; k != j; ++k) {
						r[k] = ecs[k];
					}
					for (int k = 0; k != classes.length; ++k) {
						r[k + j] = classes[k];
					}

					return Type.referenceType(pkg, r);
				}
			}
		}

		// Second, check for non-inner classes declared in this file
		for (Clazz dc : declaredClasses) {
			if (dc.name().equals(classes[0].first())) {
				return dc.type();
			}
		}

		// Third, search class path
		Type.Reference retType = loader.resolve(classes[0].first(), imports);
		// Now, we've got a fully qualified type for the first of
		// the classes specified. However, we need to append any
		// generic type information that was present in the original
		// type.
		Pair<String, Type[]>[] retTypeClasses = retType.classes();
		Pair<String, Type[]>[] nclasses = new Pair[classes.length
				+ retTypeClasses.length - 1];
		System.arraycopy(retTypeClasses, 0, nclasses, 0,
				retTypeClasses.length - 1);
		System.arraycopy(classes, 0, nclasses, retTypeClasses.length - 1,
				classes.length);
		return Type.referenceType(retType.pkg(), nclasses);
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

	/**
     * A block represents a sequence of statements generated from the same
     * statement(s). Each block has a single entry point, and may have one or
     * more exit points. In particular, there is a well-defined "sequential"
     * exit point, which corresponds to execution following on from this block
     * to the next in sequence.
     * 
     * @author djp
     * 
     */
	static protected class Block extends ArrayList<Point> {
		protected boolean hasSequentialExit = false;

		/**
         * Construct a block from some Points. Note, if the last point is null,
         * then this signals that there is no sequential exit.
         * 
         * @param points
         */
		public Block(Point... points) {
			for (Point p : points) {
				add(p);
			}
		}

		public Block(Block points) {
			super();
			for (Point p : points) {
				add(p);
			}
			hasSequentialExit = points.hasSequentialExit;
		}

		public Block(Collection<Point> points) {
			super();
			for (Point p : points) {
				add(p);
			}
		}

		public boolean add(Point p) {
			if (p == null) {
				hasSequentialExit = false;
				return false;
			} else {
				hasSequentialExit = true;
				return super.add(p);
			}
		}

		public boolean addAll(Block blk) {
			if (blk.isEmpty())
				return false;
			boolean r = false;
			for (Point p : blk) {
				r |= add(p);
			}
			hasSequentialExit = blk.hasSequentialExit;
			return r;
		}

		public boolean addAll(Collection<? extends Point> ps) {
			boolean r = false;
			for (Point p : ps) {
				r |= add(p);
			}
			return r;
		}

		public Point entry() {
			return get(0);
		}

		/**
         * Check whether this block actually as an entry point or not
         * 
         * @return
         */
		public boolean hasEntry() {
			return !isEmpty();
		}

		public Point sequentialExit() {
			return get(size() - 1);
		}

		/**
         * Check whether this block actually has a sequential exit or not.
         * 
         * @return
         */
		public boolean hasSequentialExit() {
			return hasSequentialExit;
		}

		public static final long serialVersionUID = 1l;
	}

	/**
     * This method is responsible for parsing a block of code, which is a set of
     * statements between '{' and '}'
     * 
     * @param block
     *            block to parse
     * @param cfg
     *            Control-Flow Graph being constructed
     * @return A Block containing all the Points created whilst translating this
     *         block
     * 
     */
	Block parseBlock(Tree block, FlowGraph cfg) {
		Block rb = new Block();

		// === ENTER SCOPE ===

		Scope myScope = new Scope();
		scopes.push(myScope);

		// === ITERATE STATEMENTS ===

		for (int i = 0; i != block.getChildCount(); ++i) {
			Block blk = parseStatement(block.getChild(i), null, cfg);
			if (rb.hasSequentialExit() && !blk.isEmpty()) {
				// connect up sequential statements
				cfg.add(new Triple<Point, Point, Expr>(rb.sequentialExit(), blk
						.entry(), null));
			}
			rb.addAll(blk);
		}

		// === LEAVE SCOPE ===
		scopes.pop();

		return rb;
	}

	protected Block parseStatement(Tree stmt, String label, FlowGraph cfg) {

		switch (stmt.getType()) {
			case BLOCK :
				return parseBlock(stmt, cfg);
			case VARDEF :
				return parseVarDef(stmt, cfg);
			case ASSIGN :
				return parseAssign(stmt, cfg);
			case RETURN :
				return parseReturn(stmt, cfg);
			case THROW :
				return parseThrow(stmt, cfg);
			case NEW :
				return parseNewStmt(stmt, cfg);
			case INVOKE :
				return parseInvokeStmt(stmt, cfg);
			case IF :
				return parseIf(stmt, cfg);
			case SWITCH :
				return parseSwitch(stmt, cfg);
			case FOR :
				return parseFor(stmt, label, cfg);
			case WHILE :
				return parseWhile(stmt, label, cfg);
			case DOWHILE :
				return parseDoWhile(stmt, label, cfg);
			case SELECTOR :
				return parseSelectorStmt(stmt, cfg);
			case CONTINUE :
				return parseContinue(stmt, label, cfg);
			case BREAK :
				return parseBreak(stmt, label, cfg);
			case LABEL :
				return parseStatement(stmt.getChild(1), stmt.getChild(0)
						.getText(), cfg);
			case POSTINC :
			case PREINC :
			case POSTDEC :
			case PREDEC :
				return parseIncDec(stmt, cfg);
			case ASSERT :
				return parseAssert(stmt, cfg);
			case TRY :
				return parseTry(stmt, cfg);
			case SYNCHRONIZED :
				return parseSynchronisedBlock(stmt, label, cfg);
			default :
				throw new SyntaxError("Unknown statement encountered ("
						+ stmt.getText() + ")", stmt.getLine(), stmt
						.getCharPositionInLine(), stmt.getText().length());
		}
	}

	protected Block parseSelectorStmt(Tree stmt, FlowGraph cfg) {
		Pair<Expr, List<Assign>> el = parseSelector(stmt, cfg);
		Block b = new Block();
		b.addAll(newStmtBlock(stmt, cfg, el.second()));
		Point head = codePoint((Invoke) el.first(), stmt);
		if (b.hasSequentialExit()) {
			cfg.add(new Triple<Point, Point, Expr>(b.sequentialExit(), head,
					null));
		}
		b.add(head);
		return b;
	}

	/**
     * Responsible for parsing break statements.
     * 
     * @param stmt
     * @param label
     * @param cfg
     * @return
     */
	protected Block parseBreak(Tree stmt, String label, FlowGraph cfg) {
		if (stmt.getChildCount() > 0) {
			// this is a labelled break statement.
			String blabel = stmt.getChild(0).getText();
			LoopScope labelledLoop = getLabelledLoop(blabel);
			if (labelledLoop == null) {
				throw new SyntaxError("undefined label: " + blabel, stmt
						.getLine(), stmt.getCharPositionInLine(), stmt
						.getText().length());
			}
			Point bp = codePoint(null, stmt);
			cfg.add(new Triple<Point, Point, Expr>(bp, labelledLoop.breakPoint,
					null));
			return new Block(bp, null);
		} else {
			// this is an unlabelled break statement.
			SwitchScope enclosingLoop = getEnclosingSwitch();
			if (enclosingLoop == null) {
				throw new SyntaxError("break outside switch or loop", stmt
						.getLine(), stmt.getCharPositionInLine(), stmt
						.getText().length());
			}
			Point bp = codePoint(null, stmt);
			cfg.add(new Triple<Point, Point, Expr>(bp,
					enclosingLoop.breakPoint, null));
			return new Block(bp, null);
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
	protected Block parseContinue(Tree stmt, String label, FlowGraph cfg) {
		if (stmt.getChildCount() > 0) {
			// this is a labelled break statement.
			String blabel = stmt.getChild(0).getText();
			LoopScope labelledLoop = getLabelledLoop(blabel);
			if (labelledLoop == null) {
				throw new SyntaxError("undefined label: " + blabel, stmt
						.getLine(), stmt.getCharPositionInLine(), stmt
						.getText().length());
			}
			Point cp = codePoint(null, stmt);
			cfg.add(new Triple<Point, Point, Expr>(cp,
					labelledLoop.continuePoint, null));
			return new Block(cp, null);
		} else {
			LoopScope enclosingLoop = getEnclosingLoop();
			if (enclosingLoop == null) {
				throw new SyntaxError("continue outside switch or loop", stmt
						.getLine(), stmt.getCharPositionInLine(), stmt
						.getText().length());
			}
			Point cp = codePoint(null, stmt);
			cfg.add(new Triple<Point, Point, Expr>(cp,
					enclosingLoop.continuePoint, null));
			return new Block(cp, null);
		}
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
	protected Block parseForEach(Tree stmt, Tree body, String label,
			FlowGraph cfg) {
		// The basic translation strategy looks like this:
		//
		// head:
		// $iterator = X.iterator();
		// loopHead:
		// if !$iterator.hasNext goto tail
		// loopAssign:
		// v = $iterator.next()
		// ... loop body ...
		// loopTail:
		// goto loopHead
		// tail:

		Tree varDef = stmt.getChild(0);
		Tree exprT = stmt.getChild(1);

		// =====================================================================
		// ================== 1. CREATE AUXILLARY STATEMENTS ===================
		// =====================================================================

		String iterator = scopeId + "$" + varDef.getChild(1).getLine()
				+ "$iterator";

		int modifiers = parseModifiers(varDef.getChild(0));
		String name = scopeId + varDef.getChild(2).getText();
		cfg.add(new LocalVarDef(name, parseType(varDef.getChild(1)), modifiers,
				false));
		LocalVar var = new LocalVar(name);

		cfg.add(new LocalVarDef(iterator, Type.anyType(), 0, false));
		LocalVar lv = new LocalVar(iterator);
		Expr condition = new Invoke(lv, "hasNext", new ArrayList<Expr>());

		Stmt asgn = new Assign(var, new Invoke(lv, "next",
				new ArrayList<Expr>()));

		// build the head block
		Pair<Expr, List<Assign>> expr = parseExpression(exprT, cfg);
		Block head = newStmtBlock(stmt, cfg, expr.second());
		Point init = codePoint(new Assign(lv, expr.first()), stmt);

		if (head.hasSequentialExit()) {
			cfg.add(new Triple<Point, Point, Expr>(head.sequentialExit(), init,
					null));
		}
		head.add(init);

		Point loopHead = codePoint(null, stmt); // entry point to
		// loop body
		Point loopAssign = codePoint(asgn, exprT);
		Point tail = codePoint(null, stmt); // exit point from loop

		// =====================================================================
		// ======================= 2. TRANSLATE LOOP BODY ======================
		// =====================================================================

		scopes.push(new LoopScope(loopHead, tail, label));
		scopes.lastElement().variables.add(iterator);
		scopes.lastElement().variables.add(name);

		Block loopBody = parseStatement(body, label, cfg);

		scopes.pop();

		// =====================================================================
		// ================== 3. CONNECTED CONTROL-FLOW EDGES ==================
		// =====================================================================

		cfg.add(new Triple<Point, Point, Expr>(head.sequentialExit(), loopHead,
				null));
		cfg.add(new Triple<Point, Point, Expr>(loopHead, tail, FlowGraph
				.invertBoolean(condition)));
		cfg
				.add(new Triple<Point, Point, Expr>(loopHead, loopAssign,
						condition));
		if (loopBody.size() > 0) {
			cfg.add(new Triple<Point, Point, Expr>(loopAssign,
					loopBody.entry(), null));
		}

		if (loopBody.hasSequentialExit()) {
			cfg.add(new Triple<Point, Point, Expr>(loopBody.sequentialExit(),
					loopHead, null));
		}

		// =====================================================================
		// ====================== 4. BUILD UP FINAL BLOCK ======================
		// =====================================================================

		head.add(loopHead);
		head.add(loopAssign);
		head.addAll(loopBody);
		head.add(tail);
		return head;
	}

	/**
     * Responsible for translating for statements. ANTLR tree format:
     * 
     * FOR INIT? STATEMENT TEST? EXPRESSION? STEP? STATEMENT
     * 
     * @param stmt
     *            ANTLR for-statement tree
     * @param cfg
     *            control-flow graph
     * @return
     */
	protected Block parseFor(Tree stmt, String label, FlowGraph cfg) {
		// The basic translation strategy looks like this:
		//
		// head:
		// initialiser
		// loopHead:
		// if !condition goto tail
		// ... loop body ...
		// step
		// goto loopHead
		// tail:

		// Check whether this is a normal for, or a foreach statement
		Tree t = stmt.getChild(0);
		if (t.getType() == FOREACH) {
			return parseForEach(t, stmt.getChild(1), label, cfg);
		}

		// =========================================================================
		// ================== 1. CONSTRUCT LOOP CONTROL STATMENTS
		// ==================
		// =========================================================================

		// First, create the special head/tail nodes
		Point tail = codePoint(null, stmt);
		Block loopHead = new Block(codePoint(null, stmt));
		// Second, process the condition and loop body
		Block initialiser = null;
		Block step = null;

		Tree initTree = stmt.getChild(0);
		if (initTree.getChildCount() > 0) {
			// there are initialiser(s) ...
			initialiser = new Block();
			// do any additional initialisers if there are any
			for (int i = 0; i != initTree.getChildCount(); ++i) {
				Block b = parseStatement(initTree.getChild(i), null, cfg);
				if (initialiser.hasSequentialExit()) {
					cfg.add(new Triple<Point, Point, Expr>(initialiser
							.sequentialExit(), b.entry(), null));
				}
				initialiser.addAll(b);
			}
		}

		Pair<Expr, List<Assign>> condition = null;
		Tree condTree = stmt.getChild(1);
		if (condTree.getChildCount() > 0) {
			// there is a conditional expression
			condition = parseExpression(stmt.getChild(1).getChild(0), cfg);
			Block conditionSideEffects = newStmtBlock(stmt, cfg, condition
					.second());
			if (loopHead.hasSequentialExit() && !conditionSideEffects.isEmpty()) {
				cfg.add(new Triple<Point, Point, Expr>(loopHead
						.sequentialExit(), conditionSideEffects.entry(), null));
			}
			loopHead.addAll(conditionSideEffects);
		} else {
			// for the somewhat unusual situation where there is no condition
			// given
			condition = new Pair(null, new ArrayList<Assign>());
		}

		Tree incTree = stmt.getChild(2);
		if (incTree.getChildCount() > 0) {
			// there is an increment provided
			step = new Block();
			// do any additional initialisers if there are any
			for (int i = 0; i != incTree.getChildCount(); ++i) {
				Block b = parseStatement(incTree.getChild(i), null, cfg);
				if (step.hasSequentialExit()) {
					cfg.add(new Triple<Point, Point, Expr>(step
							.sequentialExit(), b.entry(), null));
				}
				step.addAll(b);
			}
		}

		scopes.push(new LoopScope(step == null ? loopHead.entry() : step
				.entry(), tail, label));

		// =========================================================================
		// ========================= 2. TRANSLATE LOOP BODY
		// ========================
		// =========================================================================

		Tree child = stmt.getChild(3);
		Block loopBody = parseStatement(child, label, cfg);;

		scopes.pop();

		// =========================================================================
		// ========================== 3. CONNECT UP BLOCKS
		// =========================
		// =========================================================================

		// The following case analysis covers the various annoying things
		// that can happen. For example, a for-loop with no body, or with a body
		// that never completes (e.g. contains just a return/break statement)!

		if (initialiser != null) {
			cfg.add(new Triple<Point, Point, Expr>(
					initialiser.sequentialExit(), loopHead.entry(), null));
		}

		if (step != null) {
			if (!loopBody.isEmpty()) {
				// Add edge from conditional to start of loop body
				cfg
						.add(new Triple<Point, Point, Expr>(loopHead
								.sequentialExit(), loopBody.entry(), condition
								.first()));
				if (loopBody.hasSequentialExit()) {
					// Add edge from end of loop body to start of step
					cfg.add(new Triple<Point, Point, Expr>(loopBody
							.sequentialExit(), step.entry(), null));
					// Add edge from end of step back around to loop head
					cfg.add(new Triple<Point, Point, Expr>(step
							.sequentialExit(), loopHead.entry(), null));
				}
			} else {
				// Add edge direct from loop head to step, since there's
				// no loop body.
				cfg.add(new Triple<Point, Point, Expr>(loopHead
						.sequentialExit(), step.entry(), condition.first()));
				// Add edge from end of step back around to loop head
				cfg.add(new Triple<Point, Point, Expr>(step.sequentialExit(),
						loopHead.entry(), null));
			}
		} else {
			if (loopBody.hasSequentialExit()) {
				// Add edge from conditional to start of loop body
				cfg
						.add(new Triple<Point, Point, Expr>(loopHead
								.sequentialExit(), loopBody.entry(), condition
								.first()));
				if (loopBody.sequentialExit() != null) {
					// Add edge from end of loop body back around to loop head
					cfg.add(new Triple<Point, Point, Expr>(loopBody
							.sequentialExit(), loopHead.entry(), null));
				}
			} else {
				// there's no step, and no loop body!
				cfg
						.add(new Triple<Point, Point, Expr>(loopHead
								.sequentialExit(), loopHead.entry(), condition
								.first()));
			}
		}
		if (condition.first() != null) {
			// Add edge from conditional to loop exit (if there is a condition)
			cfg.add(new Triple<Point, Point, Expr>(loopHead.sequentialExit(),
					tail, FlowGraph.invertBoolean(condition.first())));
		}

		// Create region for loop body
		LoopBody bodyRegion = new LoopBody(loopHead.entry(), loopBody);
		bodyRegion.addAll(loopHead);
		if (step != null) {
			bodyRegion.addAll(step);
		}
		cfg.regions().add(bodyRegion);

		// =========================================================================
		// ========================== 4. BUILD RETURN BLOCK
		// ========================
		// =========================================================================

		Block rb = null;
		if (initialiser != null) {
			rb = new Block(initialiser);
		} else {
			rb = new Block();
		}
		rb.addAll(loopHead);
		rb.addAll(loopBody);
		rb.add(tail);

		// Finally, return the end points
		return rb;
	}

	/**
     * Responsible for translating do-while statements. ANTLR tree format:
     * 
     * DOWHILE EXPRESSION STATEMENT
     * 
     * @param stmt
     *            ANTLR dowhile statement tree
     * @param cfg
     *            control-flow graph
     * @return
     */
	protected Block parseDoWhile(Tree stmt, String label, FlowGraph cfg) {

		// =========================================================================
		// ================== 1. CONSTRUCT LOOP CONTROL STATMENTS
		// ==================
		// =========================================================================

		Point head = codePoint(null, stmt);
		Block loopTail = new Block(codePoint(null, stmt));
		Point tail = codePoint(null, stmt);

		// Second, process the condition and loop body
		Pair<Expr, List<Assign>> condition = parseExpression(stmt.getChild(0)
				.getChild(0), cfg);
		Block conditionSideEffects = newStmtBlock(stmt, cfg, condition.second());
		if (loopTail.hasSequentialExit() && !conditionSideEffects.isEmpty()) {
			cfg.add(new Triple<Point, Point, Expr>(loopTail.sequentialExit(),
					conditionSideEffects.get(0), null));
		}
		loopTail.addAll(conditionSideEffects);

		Tree child = stmt.getChild(1);

		// =========================================================================
		// ========================= 2. TRANSLATE LOOP BODY
		// ========================
		// =========================================================================

		scopes.push(new LoopScope(loopTail.entry(), tail, label));

		Block loopBody = parseStatement(child, label, cfg);

		scopes.pop();

		// =========================================================================
		// ========================== 3. CONNECT UP BLOCKS
		// =========================
		// =========================================================================

		if (!loopBody.isEmpty()) {
			// Add edge from head to start of loop body
			cfg
					.add(new Triple<Point, Point, Expr>(head, loopBody.entry(),
							null));
			if (loopBody.hasSequentialExit()) {
				// Add conditional edge from end of loop body back around to
				// head
				cfg.add(new Triple<Point, Point, Expr>(loopBody
						.sequentialExit(), loopTail.entry(), null));
			}
			// Add edge from conditional to loop exit
			cfg.add(new Triple<Point, Point, Expr>(loopTail.sequentialExit(),
					tail, FlowGraph.invertBoolean(condition.first())));
			// Add edge from conditional to loop head
			cfg.add(new Triple<Point, Point, Expr>(loopTail.sequentialExit(),
					head, condition.first()));
		} else {
			// no loop body!!!
			cfg
					.add(new Triple<Point, Point, Expr>(head, loopTail.entry(),
							null));
			cfg.add(new Triple<Point, Point, Expr>(loopTail.sequentialExit(),
					head, condition.first()));
			cfg.add(new Triple<Point, Point, Expr>(loopTail.sequentialExit(),
					tail, FlowGraph.invertBoolean(condition.first())));
		}

		// TODO: Create region for loop body

		// =========================================================================
		// ========================== 4. BUILD RETURN BLOCK
		// ========================
		// =========================================================================

		Block rb = new Block(head);
		rb.addAll(loopBody);
		rb.add(tail);

		return rb;
	}

	/**
     * Responsible for translating while statements. ANTLR tree format:
     * 
     * WHILE EXPRESSION STATEMENT
     * 
     * @param stmt
     *            ANTLR for statement tree
     * @param cfg
     *            control-flow graph
     * @return
     */
	protected Block parseWhile(Tree stmt, String label, FlowGraph cfg) {

		// ==========================================================
		// =========== 1. CONSTRUCT LOOP CONTROL STATMENTS ==========
		// ==========================================================

		Block head = new Block(codePoint(null, stmt));
		Point tail = codePoint(null, stmt);

		// Second, process the condition and loop body
		Pair<Expr, List<Assign>> condition = parseExpression(stmt.getChild(0)
				.getChild(0), cfg);
		Block conditionSideEffects = newStmtBlock(stmt, cfg, condition.second());
		if (head.hasSequentialExit() && !conditionSideEffects.isEmpty()) {
			cfg.add(new Triple<Point, Point, Expr>(head.sequentialExit(),
					conditionSideEffects.get(0), null));
		}
		head.addAll(conditionSideEffects);
		Tree child = stmt.getChild(1);

		// ==========================================================
		// ================= 2. TRANSLATE LOOP BODY =================
		// ==========================================================
		scopes.push(new LoopScope(head.entry(), tail, label));

		Block loopBody = parseStatement(child, label, cfg);

		scopes.pop();

		// ==========================================================
		// ================== 3. CONNECT UP BLOCKS ==================
		// ==========================================================

		if (!loopBody.isEmpty()) {
			// Add edge from conditional to start of loop body
			cfg.add(new Triple<Point, Point, Expr>(head.sequentialExit(),
					loopBody.entry(), condition.first()));
			if (loopBody.hasSequentialExit()) {
				// Add edge from end of loop body back around to head
				cfg.add(new Triple<Point, Point, Expr>(loopBody
						.sequentialExit(), head.entry(), null));
			}
		} else {
			cfg.add(new Triple<Point, Point, Expr>(head.sequentialExit(), head
					.entry(), condition.first()));
		}
		// Add edge from conditional to loop exit
		cfg.add(new Triple<Point, Point, Expr>(head.sequentialExit(), tail,
				FlowGraph.invertBoolean(condition.first())));

		// Create region for loop body
		LoopBody bodyRegion = new LoopBody(head.entry(), loopBody);
		bodyRegion.addAll(head);
		cfg.regions().add(bodyRegion);

		// ==========================================================
		// ================== 4. BUILD RETURN BLOCK =================
		// ==========================================================

		Block rb = new Block(head);
		rb.addAll(loopBody);
		rb.add(tail);

		return rb;
	}

	/**
     * Responsible for translating if statements, including if-then-else.
     * If-statement tree format:
     * 
     * IF EXPRESSION STATEMENT STATEMENT?
     * 
     * @param stmt
     *            ANTLR if-statement tree
     * @param cfg
     *            control-flow graph
     * @return
     */
	protected Block parseIf(Tree stmt, FlowGraph cfg) {

		// ==========================================================
		// ============ 1. CONSTRUCT IF ENTRY/EXIT POINTS ===========
		// ==========================================================

		// First, create the special head/tail nodes
		Block head = new Block(codePoint(null, stmt));
		Point tail = codePoint(null, stmt);

		// Second, process the if-condition
		Pair<Expr, List<Assign>> condition = parseExpression(stmt.getChild(0),
				cfg);
		Block conditionSideEffects = newStmtBlock(stmt, cfg, condition.second());
		if (head.hasSequentialExit() && !conditionSideEffects.isEmpty()) {
			cfg.add(new Triple<Point, Point, Expr>(head.sequentialExit(),
					conditionSideEffects.entry(), null));
		}
		head.addAll(conditionSideEffects);

		// ==========================================================
		// ================= 2. TRUE/FALSE BODIES ===================
		// ==========================================================

		// Second, process true and false branches
		Block trueBranch = parseStatement(stmt.getChild(1), null, cfg);
		Block falseBranch = stmt.getChildCount() > 2 ? parseStatement(stmt
				.getChild(2), null, cfg) : null;

		// ==========================================================
		// ================= 3. CONNECT UP BLOCKS ===================
		// ==========================================================

		if (!trueBranch.isEmpty()) {
			cfg.add(new Triple<Point, Point, Expr>(head.sequentialExit(),
					trueBranch.entry(), condition.first()));
		}

		if (falseBranch != null && !falseBranch.isEmpty()) {
			cfg.add(new Triple<Point, Point, Expr>(head.sequentialExit(),
					falseBranch.entry(), FlowGraph.invertBoolean(condition
							.first())));
		}

		// Fourth, add edges from end of true/false branches to tail.
		if (trueBranch.isEmpty()) {
			cfg.add(new Triple<Point, Point, Expr>(head.sequentialExit(), tail,
					condition.first()));
		} else if (trueBranch.hasSequentialExit()) {
			cfg.add(new Triple<Point, Point, Expr>(trueBranch.sequentialExit(),
					tail, null));
		}

		if (falseBranch == null || falseBranch.isEmpty()) {
			cfg.add(new Triple<Point, Point, Expr>(head.sequentialExit(), tail,
					FlowGraph.invertBoolean(condition.first())));
		} else if (falseBranch.hasSequentialExit()) {
			cfg.add(new Triple<Point, Point, Expr>(
					falseBranch.sequentialExit(), tail, null));
		}

		// ==========================================================
		// ==================== 4. BUILD RETURN BLOCK ===============
		// ==========================================================

		Block rb = new Block(head);
		rb.addAll(trueBranch);
		if (falseBranch != null) {
			rb.addAll(falseBranch);
		}
		rb.add(tail);
		return rb;
	}

	/**
     * Responsible for translating switch statements. Switch-statement tree
     * format:
     * 
     * SWITCH PAREXPRESSION (CASE CONSTANTEXPRESSION BLOCKSTATEMENT |CASE
     * ENUMCONSTANTNAME BLOCKSTATEMENT |DEFAULT BLOCKSTATEMENT)*
     * 
     * @param stmt
     *            ANTLR if-statement tree
     * @param cfg
     *            control-flow graph
     * @return
     */
	protected Block parseSwitch(Tree stmt, FlowGraph cfg) {

		// ==========================================================
		// ============ 1. CONSTRUCT IF ENTRY/EXIT POINTS ===========
		// ==========================================================

		// First, create the special head/tail nodes
		Block head = new Block(codePoint(null, stmt));
		Point tail = codePoint(null, stmt);

		// Second, process the expression to switch on
		Pair<Expr, List<Assign>> condition = parseExpression(stmt.getChild(0),
				cfg);
		Block conditionSideEffects = newStmtBlock(stmt, cfg, condition.second());
		if (head.hasSequentialExit() && !conditionSideEffects.isEmpty()) {
			cfg.add(new Triple<Point, Point, Expr>(head.sequentialExit(),
					conditionSideEffects.entry(), null));
		}
		head.addAll(conditionSideEffects);

		// ==========================================================
		// ====================== 2. BODIES =========================
		// ==========================================================

		scopes.push(new SwitchScope(tail));

		// Second, process the case branches
		FlowGraph.Expr[] expressions = new FlowGraph.Expr[stmt.getChildCount() - 1];
		FlowGraph.Expr defAult = null;
		Block[] branches = new Block[stmt.getChildCount() - 1];
		for (int i = 1; i < stmt.getChildCount(); i++) {
			Tree t = stmt.getChild(i);
			if (t.getType() == CASE) {
				// FIXME: ADD CHECK FOR CONSTANT EXPRESSIONS!!!
				expressions[i - 1] = new FlowGraph.BinOp(FlowGraph.BinOp.EQ,
						condition.first(), parseExpression(t.getChild(0), cfg)
								.first());
				// and negative with default
				if (defAult == null) {
					defAult = new FlowGraph.UnOp(FlowGraph.UnOp.NOT,
							expressions[i - 1]);
				} else {
					defAult = new FlowGraph.BinOp(FlowGraph.BinOp.AND, defAult,
							new FlowGraph.UnOp(FlowGraph.UnOp.NOT,
									expressions[i - 1]));
				}
				if (t.getChild(1) != null) {
					branches[i - 1] = parseBlock(t.getChild(1), cfg);
				}
			} else { // t.getType() == DEFAULT
				if (t.getChild(0) != null) {
					branches[i - 1] = parseBlock(t.getChild(0), cfg);
				}
			}
		}

		scopes.pop();

		// ==========================================================
		// ================= 3. CONNECT UP BLOCKS ===================
		// ==========================================================

		// go through each of the cases, creating links from the switch
		// using the condition for each. Empty cases are linked to the
		// next non-empty case.
		boolean hasDefault = false;
		FlowGraph.Point last = tail;
		for (int i = expressions.length - 1; i >= 0; i--) {
			FlowGraph.Expr c = expressions[i];
			if (c == null) {
				hasDefault = true;
				c = defAult;
			}

			// Connect head of switch to start of case block. If case block is
			// empty then go straight to tail
			FlowGraph.Point caseStart = branches[i].isEmpty()
					? last
					: branches[i].entry();
			cfg.add(new Triple<Point, Point, Expr>(head.sequentialExit(),
					caseStart, c));

			// Connect open-ended case statements (i.e. those without break
			// statements) to fall-through case.
			if (!branches[i].isEmpty()) {
				if (branches[i].hasSequentialExit()) {
					cfg.add(new Triple<Point, Point, Expr>(branches[i]
							.sequentialExit(), last, null));
				}
				last = branches[i].entry();
			}
		}

		if (!hasDefault) {
			// In this case, no explicit "default" label was given. Therefore,
			// add one!
			cfg.add(new Triple<Point, Point, Expr>(head.sequentialExit(), tail,
					defAult));
		}

		// ==========================================================
		// ==================== 4. BUILD RETURN BLOCK ===============
		// ==========================================================

		Block rb = new Block(head);
		for (Block b : branches) {
			if (b != null)
				rb.addAll(b);
		}
		rb.add(tail);
		return rb;
	}

	/**
     * Responsible for translating invoke statements. ANTLR tree format:
     * 
     * INVOKE NAME [EXPRESSION]*
     * 
     * @param stmt
     *            ANTLR if-statement tree
     * @param cfg
     *            control-flow graph
     * @return
     */
	protected Block parseInvokeStmt(Tree stmt, FlowGraph cfg) {
		Pair<Expr, List<Assign>> eb = parseInvoke(stmt, cfg);
		Block b = newStmtBlock(stmt, cfg, eb.second());
		Point i = codePoint((Invoke) eb.first(), stmt);
		if (b.hasSequentialExit()) {
			cfg
			.add(new Triple<Point, Point, Expr>(b.sequentialExit(), i,
					null));
		}
		b.add(i);
		return b;
	}

	/**
     * Responsible for translating new statements. ANTLR tree format:
     * 
     * NEW TYPE [EXPRESSION]*
     * 
     * @param stmt
     *            ANTLR if-statement tree
     * @param cfg
     *            control-flow graph
     * @return
     */
	protected Block parseNewStmt(Tree stmt, FlowGraph cfg) {
		Pair<Expr, List<Assign>> eb = parseNew(stmt, cfg, null);
		Block b = newStmtBlock(stmt, cfg, eb.second());
		Point i = codePoint((New) eb.first(), stmt);
		if (b.hasSequentialExit()) {
			cfg
					.add(new Triple<Point, Point, Expr>(b.sequentialExit(), i,
							null));
		}
		b.add(i);
		return b;
	}

	/**
     * Responsible for translating variable declarations. ANTLR tree format:
     * 
     * VARDEF MODIFIERS? TYPE NAME [= EXPRESSION]
     * 
     * @param stmt
     *            ANTLR if-statement tree
     * @param cfg
     *            control-flow graph
     * @return
     */
	protected Block parseVarDef(Tree stmt, FlowGraph cfg) {

		Block blk = new Block();
		// === MODIFIERS ===
		int modifiers = parseModifiers(stmt.getChild(0));

		// === TYPE ===
		Type type = parseType(stmt.getChild(1));
		// === NAME(S) ===

		for (int i = 2; i < stmt.getChildCount(); i = i + 1) {
			Tree nameTree = stmt.getChild(i);
			String myName = scopes.peek().id + nameTree.getText();
			Type myType = type;
			Pair<Expr, List<Assign>> myInitialiser = null;
			// Parse array type modifiers (if there are any)
			for (int j = 0; j < nameTree.getChildCount(); j = j + 1) {
				Tree am = nameTree.getChild(j);
				if (am.getText().equals("[")) {
					myType = Type.arrayType(myType);
				} else {
					// If we get here, then we've hit an initialiser
					myInitialiser = parseExpression(am, cfg);
				}
			}

			// now, add the variable definition
			cfg.add(new LocalVarDef(myName, myType, modifiers, false));

			scopes.peek().variables.add(myName);

			// finally, add the initialiser (if there is one)
			if (myInitialiser != null) {
				LVal lhs = new LocalVar(myName);
				Block ap = newStmtBlock(stmt, cfg, myInitialiser.second());
				Point p = codePoint(new Assign(lhs, myInitialiser.first()),
						nameTree);
				if (ap.hasSequentialExit()) {
					cfg.add(new Triple<Point, Point, Expr>(ap.sequentialExit(),
							p, null));
				}

				ap.add(p);

				if (blk.hasSequentialExit()) {
					// System.out.println(p);
					cfg.add(new Triple<Point, Point, Expr>(
							blk.sequentialExit(), ap.entry(), null));
				}

				blk.addAll(ap);
			}
		}

		return blk;
	}

	protected Block parseReturn(Tree stmt, FlowGraph cfg) {
		Block b = new Block();
		if (stmt.getChildCount() > 0) {
			Pair<Expr, List<Assign>> rhs = parseExpression(stmt.getChild(0),
					cfg);
			Point i = codePoint(new Return(rhs.first()), stmt);
			b.addAll(newStmtBlock(stmt, cfg, rhs.second()));
			if (b.hasSequentialExit()) {
				cfg.add(new Triple<Point, Point, Expr>(b.sequentialExit(), i,
						null));
			}
			b.add(i);
		} else {
			b.add(codePoint(new Return(null), stmt));
		}
		b.add(null); // indicate no sequential exit

		return b;
	}

	protected Block parseThrow(Tree stmt, FlowGraph cfg) {
		Pair<Expr, List<Assign>> rhs = parseExpression(stmt.getChild(0), cfg);
		Block b = newStmtBlock(stmt, cfg, rhs.second());
		Point i = codePoint(new Throw(rhs.first()), stmt);
		if (b.hasSequentialExit()) {
			cfg
					.add(new Triple<Point, Point, Expr>(b.sequentialExit(), i,
							null));
		}
		b.add(i);
		b.add(null);
		return b;
	}

	/**
     * This method parses an assignment statement.
     * 
     * @param stmt
     * @return
     */
	protected Block parseAssign(Tree stmt, FlowGraph cfg) {
		Pair<Expr, List<Assign>> lhs = parseExpression(stmt.getChild(0), cfg);
		Pair<Expr, List<Assign>> rhs = parseExpression(stmt.getChild(1), cfg);
		Block b = new Block();
		Block lhsSideEffects = newStmtBlock(stmt, cfg, lhs.second());
		Block rhsSideEffects = newStmtBlock(stmt, cfg, rhs.second());
		Point ap = codePoint(new Assign((LVal) lhs.first(), rhs.first()), stmt);
		b.addAll(lhsSideEffects);
		b.addAll(rhsSideEffects);
		b.add(ap);

		if (lhsSideEffects.hasSequentialExit()) {
			if (rhsSideEffects.hasEntry()) {
				cfg.add(new Triple<Point, Point, Expr>(lhsSideEffects
						.sequentialExit(), rhsSideEffects.entry(), null));
				if (rhsSideEffects.hasSequentialExit()) {
					cfg.add(new Triple<Point, Point, Expr>(rhsSideEffects
							.sequentialExit(), ap, null));
				}
			} else {
				cfg.add(new Triple<Point, Point, Expr>(lhsSideEffects
						.sequentialExit(), ap, null));
			}
		} else if (rhsSideEffects.hasSequentialExit()) {
			cfg.add(new Triple<Point, Point, Expr>(rhsSideEffects
					.sequentialExit(), ap, null));
		}

		return b;
	}

	protected Block parseAssert(Tree stmt, FlowGraph cfg) {
		// TODO: add support for assert statements
		return new Block();
	}

	protected Block parseTry(Tree stmt, FlowGraph cfg) {

		Point exit = codePoint(null, stmt);

		// First, translate the block body
		Block body = parseStatement(stmt.getChild(0), null, cfg);
		Block rb = new Block(body);
		Tree finallyExpr = null;

		// Now, look to see whether there is a finally block. Finally blocks are
		// somewhat awkward to deal with. There are several things we need to
		// do:
		//
		// 1) Inline the finally block a the end of try block and
		// any exception handlers foro this (done in the second for-loop)
		//
		// 2) Construct an exception handler for any exception (i.e. any
		// instance of Throwable) which executes the finally block and
		// then rethrows that exception (done after second for loop)
		for (int i = 1; i < stmt.getChildCount(); ++i) {
			Tree child = stmt.getChild(i);
			if (child.getType() == FINALLY) {
				finallyExpr = child.getChild(0);
				break;
			}
		}

		// Second, process catch blocks
		for (int i = 1; i < stmt.getChildCount(); ++i) {
			Tree child = stmt.getChild(i);
			if (child.getType() == CATCH) {
				// System.out.println("Parsing catch " + child.getLine());
				Tree param = child.getChild(0);
				Type.Reference exceptionT = (Type.Reference) parseType(param
						.getChild(0));
				// add local variable to the list
				scopes.push(new Scope());
				String name = scopes.peek().id + param.getChild(1).getText();
				scopes.peek().variables.add(name);
				cfg.add(new LocalVarDef(name, exceptionT, 0, false));
				Block handler = parseStatement(child.getChild(1), null, cfg);

				scopes.pop();

				// Inline the finally block (if there is one).
				if (finallyExpr != null) {
					// FIXME: there's a big bug. I need to inline the
					// finally block before whatever return statements there
					// are in the block. The challenge is to figure out what
					// they are.
					Block finallyBody = parseStatement(finallyExpr, null, cfg);
					if (handler.hasSequentialExit()) {
						cfg.add(new Triple<Point, Point, Expr>(handler
								.sequentialExit(), finallyBody.entry(), null));
					}

					cfg.add(new Triple<Point, Point, Expr>(finallyBody
							.sequentialExit(), exit, null));
					handler.addAll(finallyBody);
				} else if (handler.hasSequentialExit()) {
					// connect up exit of handler to exit of block
					cfg.add(new Triple<Point, Point, Expr>(handler.get(handler
							.size() - 1), exit, null));
				}

				// Now, add to the front of the exception handler an assignment
				// to the catch variable from the "magic" variable $ (which
				// basically just loads off the stack directly)
				Point handlerEntry = codePoint(new Assign(new LocalVar(name),
						new LocalVar("$")), child);

				if (handler.hasEntry()) {
					// if the handler is an empty block, it won't have an
					// entry point!
					cfg.add(new Triple<Point, Point, Expr>(handlerEntry,
							handler.entry(), null));
				}

				if (handler.size() == 0) {
					// Handle the case where the catch block had no statements
					// So it didn't have a sequential exit. But when we add the
					// new handlerEntry, it will. So we need to connect the new
					// Entry
					// upto the exit point
					cfg.add(new Triple<Point, Point, Expr>(handlerEntry, exit,
							null));
				}

				handler.add(0, handlerEntry);

				// Finally, create the catch block!
				cfg.regions().add(
						new CatchBlock(handler.get(0), exceptionT, body));
				// add all points created to this block
				rb.addAll(handler);
			}
		}

		if (finallyExpr != null) {
			// Now, we need to:
			//
			// 1) Create an exception handler for all non-caught exceptions
			// thrown from try body. (this is case 2 from previous list).
			//
			// 2) inline the finally body into the end of the try handler.

			Block finallyBody = parseStatement(finallyExpr, null, cfg);

			// Case 1 (from above) --- create exception handler.

			// Construct and define variable to hold exception reference (so
			// we can rethrow it).
			Type.Reference throwable = Type.referenceType("java.lang",
					"Throwable");
			String finallyVarName = "$finally$" + finallyExpr.getLine() + "$"
					+ finallyExpr.getCharPositionInLine();
			cfg.add(new LocalVarDef(finallyVarName, throwable, 0, false));
			LocalVar finallyVar = new LocalVar(finallyVarName);
			Point finallyEntry = codePoint(new Assign(finallyVar, new LocalVar(
					"$")), finallyExpr);
			// Add assignment to front of finally block
			if (finallyBody.hasEntry()) {
				// if the handler is an empty block, it won't have an
				// entry point!
				cfg.add(new Triple<Point, Point, Expr>(finallyEntry,
						finallyBody.entry(), null));
			}
			finallyBody.add(0, finallyEntry);

			// Now, add rethrow statement to end of finally block.
			if (finallyBody.hasSequentialExit()) {
				Point rethrow = codePoint(new Throw(finallyVar), finallyExpr);
				cfg.add(new Triple<Point, Point, Expr>(finallyBody
						.sequentialExit(), rethrow, null));
				finallyBody.add(rethrow);
			}

			// Finally, setup exception handler.
			CatchBlock cb = new CatchBlock(finallyBody.get(0), throwable, body);
			cfg.regions().add(cb);
			rb.addAll(finallyBody);

			// Case 2 (from above) --- inline finally handler into end of
			// try-block

			finallyBody = parseStatement(finallyExpr, null, cfg);
			rb.addAll(finallyBody);
			if (body.hasSequentialExit()) {
				// FIXME: In this case, there's a big bug. I need to inline the
				// finally block before whatever return statements there
				// are in the block. The challenge is to figure out what
				// they are.
				cfg.add(new Triple<Point, Point, Expr>(body.sequentialExit(),
						finallyBody.entry(), null));
			}
			if (finallyBody.hasSequentialExit()) {
				cfg.add(new Triple<Point, Point, Expr>(finallyBody
						.sequentialExit(), exit, null));
			}
		} else {
			// Third, connect body to exit point
			if (body.hasSequentialExit()) {
				cfg.add(new Triple<Point, Point, Expr>(body.sequentialExit(),
						exit, null));
			}
		}

		rb.add(exit);

		return rb;
	}

	/**
     * Parse a standalone pre/post inc/dec statement (e.g. ++i, --i, etc)
     * 
     * @param inc
     *            the increment (1 or -1)
     * @param stmt
     * @param cfg
     * @return
     */
	public Block parseIncDec(Tree stmt, FlowGraph cfg) {
		Pair<Expr, List<Assign>> lhs = parseExpression(stmt, cfg);
		return newStmtBlock(stmt, cfg, lhs.second());
	}

	/**
     * Parse a synchronized block. This has the form:
     * 
     * SYNCHRONIZED EXPRESSION STATEMENT
     * 
     * @param stmt
     * @param label
     * @param cfg
     * @return
     */
	public Block parseSynchronisedBlock(Tree stmt, String label, FlowGraph cfg) {
		Block b = new Block();
		Pair<Expr, List<Assign>> e = parseExpression(stmt.getChild(0), cfg);
		String name = scopeId + "$" + stmt.getChild(0).getLine() + "$synch";
		LocalVar tmp = new LocalVar(name, Type.referenceType("java.lang",
				"Object"));
		cfg.add(new LocalVarDef(name,
				Type.referenceType("java.lang", "Object"), 0, false));

		// Construct the statements required
		Block sideEffects = newStmtBlock(stmt, cfg, e.second());
		Point assign = codePoint(new FlowGraph.Assign(tmp, e.first()), stmt);
		Point lock = codePoint(new FlowGraph.Lock(tmp), stmt);
		Point unlock = codePoint(new FlowGraph.Unlock(tmp), stmt);
		Block blk = parseBlock(stmt.getChild(1), cfg);
		b.addAll(sideEffects);
		b.add(assign);
		b.add(lock);
		b.addAll(blk);
		b.add(unlock);
		// Now, link the statements together!
		if (sideEffects.hasSequentialExit()) {
			cfg.add(new Triple<Point, Point, Expr>(
					sideEffects.sequentialExit(), assign, null));
		}
		cfg.add(new Triple<Point, Point, Expr>(assign, lock, null));
		cfg.add(new Triple<Point, Point, Expr>(lock, blk.entry(), null));
		if (blk.hasSequentialExit()) {
			cfg.add(new Triple<Point, Point, Expr>(blk.sequentialExit(),
					unlock, null));
		}
		return b;
	}

	/**
     * This method parses an expression.
     * 
     * @param expr
     *            The expression being parsed
     * @return A Pair (E,B), where E is the parsed expression, B contains
     *         statements that must run before this expression (but after any in
     *         the same statement that are to the left of this expression).
     */
	protected Pair<Expr, List<Assign>> parseExpression(Tree expr, FlowGraph cfg) {
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
				return parseArrayVal(expr, cfg);
			case ARRAYINIT :
				return parseDirectedArrayVal(expr, cfg);
			case VAR :
				return parseVariable(expr);
			case NEW :
				return parseNew(expr, cfg, null);
			case INVOKE :
				return parseInvoke(expr, cfg);
			case SELECTOR :
				return parseSelector(expr, cfg);
			case GETCLASS :
				return parseGetClass(expr);
			case PREINC :
				return parsePreIncrement(expr, cfg);
			case PREDEC :
				return parsePreDecrement(expr, cfg);
			case POSTINC :
				return parsePostIncrement(expr, cfg);
			case POSTDEC :
				return parsePostDecrement(expr, cfg);
			case NEG :
				return parseUnOp(UnOp.NEG, expr, cfg);
			case NOT :
				return parseUnOp(UnOp.NOT, expr, cfg);
			case INV :
				return parseUnOp(UnOp.INV, expr, cfg);
			case CAST :
				return parseCast(expr, cfg);
			case LABINOP :
				return parseLeftAssociativeBinOp(expr, cfg);
			case USHR :
				return parseBinOp(BinOp.USHR, expr, cfg);
			case LAND :
				return parseBinOp(BinOp.LAND, expr, cfg);
			case LOR :
				return parseBinOp(BinOp.LOR, expr, cfg);
			case AND :
				return parseBinOp(BinOp.AND, expr, cfg);
			case OR :
				return parseBinOp(BinOp.OR, expr, cfg);
			case XOR :
				return parseBinOp(BinOp.XOR, expr, cfg);
			case EQ :
				return parseBinOp(BinOp.EQ, expr, cfg);
			case NEQ :
				return parseBinOp(BinOp.NEQ, expr, cfg);
			case LT :
				return parseBinOp(BinOp.LT, expr, cfg);
			case LTEQ :
				return parseBinOp(BinOp.LTEQ, expr, cfg);
			case GT :
				return parseBinOp(BinOp.GT, expr, cfg);
			case GTEQ :
				return parseBinOp(BinOp.GTEQ, expr, cfg);
			case INSTANCEOF :
				return parseInstanceOf(expr, cfg);
			case TERNOP :
				return parseTernOp(expr, cfg);
			case ASSIGN :
				// inlined assignment statements.
				Block blk = parseAssign(expr, cfg);
				ArrayList<Assign> sideEffects = new ArrayList<Assign>();
				Expr lastAssigned = null;
				for (Point p : blk) {
					if (p.statement() instanceof Assign) {
						Assign a = (Assign) p.statement();
						sideEffects.add(a);
						lastAssigned = a.lhs;
					} else {
						// basically to deal with this case, we need to return
						// blocks not lists of assignments.
						throw new SyntaxError(
								"Sorry, can't handle ternary operations in expression side effects!",
								expr.getLine(), expr.getCharPositionInLine(),
								expr.getText().length());
					}
				}
				return new Pair(lastAssigned, sideEffects);
			default :
				throw new SyntaxError("Unknown expression encountered ("
						+ expr.getText() + ")", expr.getLine(), expr
						.getCharPositionInLine(), expr.getText().length());
		}
	}

	protected Pair<Expr, List<Assign>> parseTernOp(Tree expr, FlowGraph cfg) {
		Pair<Expr, List<Assign>> cond = parseExpression(expr.getChild(0), cfg);
		Pair<Expr, List<Assign>> tbranch = parseExpression(expr.getChild(1),
				cfg);
		Pair<Expr, List<Assign>> fbranch = parseExpression(expr.getChild(2),
				cfg);
		// FIXME: expand tern op as if statement, include side effects
		return new Pair(new TernOp(cond.first(), tbranch.first(), fbranch
				.first()), cond.second());
	}

	protected Pair<Expr, List<Assign>> parseInstanceOf(Tree expr, FlowGraph cfg) {
		Pair<Expr, List<Assign>> el = parseExpression(expr.getChild(0), cfg);
		return new Pair(
				new InstanceOf(el.first(), parseType(expr.getChild(1))), el
						.second());
	}

	protected Pair<Expr, List<Assign>> parseBinOp(int bop, Tree expr,
			FlowGraph cfg) {
		Pair<Expr, List<Assign>> lhs = parseExpression(expr.getChild(0), cfg);
		Pair<Expr, List<Assign>> rhs = parseExpression(expr.getChild(1), cfg);
		List<Assign> sideEffects = lhs.second();
		Expr lhs_expr = lhs.first();
		// Ok, we have side effects in the rhs. Therefore, we need to evaluate
		// the lhs expression *before* the side effects come into effect.
		if (!rhs.second().isEmpty()) {
			LVal v = createTemporary(cfg);
			sideEffects.add(new Assign(v, lhs.first()));
			lhs_expr = v;
		}
		sideEffects.addAll(rhs.second());
		return new Pair(new BinOp(bop, lhs_expr, rhs.first()), sideEffects);
	}

	// Binary operations which can be left associative are more complex and have
	// to be delt with using a special LABINOP operator.
	protected Pair<Expr, List<Assign>> parseLeftAssociativeBinOp(Tree expr,
			FlowGraph cfg) {
		Pair<Expr, List<Assign>> tmp = parseExpression(expr.getChild(0), cfg);
		List<Assign> sideEffects = tmp.second();
		Expr lhs = tmp.first();

		for (int i = 1; i < expr.getChildCount(); i = i + 2) {
			int bop = parseOp(expr.getChild(i).getText(), expr);
			Pair<Expr, List<Assign>> rhs = parseExpression(
					expr.getChild(i + 1), cfg);
			// Ok, we have side effects in the rhs. Therefore, we need to
			// evaluate
			// the lhs expression *before* the side effects come into effect.
			if (!rhs.second().isEmpty()) {
				LVal v = createTemporary(cfg);
				sideEffects.add(new Assign(v, lhs));
				lhs = v;
			}
			sideEffects.addAll(rhs.second());
			lhs = new BinOp(bop, lhs, rhs.first());
		}

		return new Pair(lhs, sideEffects);
	}

	protected int parseOp(String op, Tree expr) {
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

	protected Pair<Expr, List<Assign>> parseUnOp(int uop, Tree expr,
			FlowGraph cfg) {
		Pair<Expr, List<Assign>> el = parseExpression(expr.getChild(0), cfg);
		return new Pair(new UnOp(uop, el.first()), el.second());
	}

	protected Pair<Expr, List<Assign>> parseCast(Tree expr, FlowGraph cfg) {
		Pair<Expr, List<Assign>> el = parseExpression(expr.getChild(1), cfg);
		return new Pair(new Cast(parseType(expr.getChild(0)), el.first()), el
				.second());
	}

	protected Pair<Expr, List<Assign>> parseCharVal(Tree expr) {
		// TODO: handle escaped characters correctly
		String charv = expr.getChild(0).getText();
		CharVal v = null;
		if (charv.length() == 3) {
			v = new CharVal(charv.charAt(1));
		} else {
			String tmp = charv.substring(1, charv.length() - 1);
			if (tmp.equals("\\b"))
				v = new CharVal('\b');
			else if (tmp.equals("\\t"))
				v = new CharVal('\t');
			else if (tmp.equals("\\f"))
				v = new CharVal('\f');
			else if (tmp.equals("\\n"))
				v = new CharVal('\n');
			else if (tmp.equals("\\r"))
				v = new CharVal('\r');
			else if (tmp.equals("\\\""))
				v = new CharVal('\"');
			else if (tmp.equals("\\\\"))
				v = new CharVal('\\');
			else if (tmp.equals("\\'"))
				v = new CharVal('\'');
			else if (Character.isDigit(tmp.charAt(1)))  {
				int octal_val = Integer.parseInt(tmp.substring(1,tmp.length()),8);
				v = new CharVal((char) octal_val);
			} else {
				throw new RuntimeException(
						"Unable to parse character constant: " + tmp);
			}
		} 
		return new Pair(v, new ArrayList());
	}

	protected Pair<Expr, List<Assign>> parseBoolVal(Tree expr) {
		BoolVal v = new BoolVal(Boolean
				.parseBoolean(expr.getChild(0).getText()));
		return new Pair(v, new ArrayList());
	}

	protected Pair<Expr, List<Assign>> parseIntVal(Tree expr) {
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
			return new Pair(new LongVal(val), new Block());
		} else if (radix == 10 && (lc == 'f' || lc == 'F')) {
			return new Pair(new FloatVal(val), new Block());
		} else if (radix == 10 && (lc == 'd' || lc == 'D')) {
			return new Pair(new DoubleVal(val), new Block());
		}

		val = parseLongVal(value, radix);
		return new Pair(new IntVal((int) val), new ArrayList());
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

	protected Pair<Expr, List<Assign>> parseStringVal(Tree expr) {
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
		return new Pair(new StringVal(v), new ArrayList());
	}

	protected Pair<Expr, List<Assign>> parseNullVal(Tree expr) {
		return new Pair(new NullVal(), new ArrayList());
	}

	/**
     * This parses a floating point value. Note that this may correspond to a
     * Java float, or a Java double!
     */
	protected Pair<Expr, List<Assign>> parseFloatVal(Tree expr) {
		String val = expr.getChild(0).getText();

		char lc = val.charAt(val.length() - 1);
		Expr r;
		if (lc == 'f' || lc == 'F') {
			r = new FloatVal(Float.parseFloat(val));
		} else {
			r = new DoubleVal(Double.parseDouble(val));
		}
		return new Pair(r, new ArrayList());
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
	protected Pair<Expr, List<Assign>> parseArrayVal(Tree expr, FlowGraph cfg) {
		Pair<List<Expr>, List<Assign>> values = parseExpressionList(0, expr
				.getChildCount(), expr, cfg);
		return new Pair(new ArrayVal(values.first()), values.second());
	}

	/**
     * Parse a directed array initialiser expression. This is distinct from an
     * array initialiser in a subtle way. To generate an array initiliser you
     * must specify the class of array to construct. For example:
     * 
     * <pre>
     * Object[] test = new Object[]{&quot;abc&quot;, new Integer(2)};
     * </pre>
     * 
     * 
     * @param expr
     * @return
     */
	protected Pair<Expr, List<Assign>> parseDirectedArrayVal(Tree expr,
			FlowGraph cfg) {
		Type type = parseType(expr.getChild(0));
		Tree aval = expr.getChild(1);
		Pair<List<Expr>, List<Assign>> values = parseExpressionList(0, aval
				.getChildCount(), aval, cfg);
		return new Pair(new ArrayVal(values.first(), type), values.second());
	}

	protected Pair<Expr, List<Assign>> parsePreIncrement(Tree expr,
			FlowGraph cfg) {
		Pair<Expr, List<Assign>> eb = parseExpression(expr.getChild(0), cfg);
		List<Assign> b = eb.second();

		LVal v = (LVal) eb.first();
		Assign a1 = new Assign(v, new BinOp(BinOp.ADD, v, new IntVal(1)));

		b.add(a1);
		return new Pair(v, b);
	}

	protected Pair<Expr, List<Assign>> parsePreDecrement(Tree expr,
			FlowGraph cfg) {
		Pair<Expr, List<Assign>> eb = parseExpression(expr.getChild(0), cfg);
		List<Assign> b = eb.second();

		LVal v = (LVal) eb.first();
		Assign a1 = new Assign(v, new BinOp(BinOp.SUB, v, new IntVal(1)));

		b.add(a1);
		return new Pair(v, b);
	}

	protected Pair<Expr, List<Assign>> parsePostIncrement(Tree expr,
			FlowGraph cfg) {
		Pair<Expr, List<Assign>> eb = parseExpression(expr.getChild(0), cfg);
		List<Assign> b = eb.second();

		LVal v = (LVal) eb.first();
		LVal tmp = createTemporary(cfg);
		Assign a1 = new Assign(tmp, v);
		Assign a2 = new Assign(v, new BinOp(BinOp.ADD, v, new IntVal(1)));

		b.add(a1);
		b.add(a2);
		return new Pair(tmp, b);
	}

	protected Pair<Expr, List<Assign>> parsePostDecrement(Tree expr,
			FlowGraph cfg) {
		Pair<Expr, List<Assign>> eb = parseExpression(expr.getChild(0), cfg);
		List<Assign> b = eb.second();

		LVal v = (LVal) eb.first();
		LVal tmp = createTemporary(cfg);
		Assign a1 = new Assign(tmp, v);
		Assign a2 = new Assign(v, new BinOp(BinOp.SUB, v, new IntVal(1)));

		b.add(a1);
		b.add(a2);
		return new Pair(tmp, b);
	}

	protected Pair<Expr, List<Assign>> parseVariable(Tree expr) {
		// an important challenge here is to figure out the scope of the
		// variable referred to.
		String name = expr.getChild(0).getText();
		// check for implicit names first
		if (name.equals("this")) {
			return new Pair(new LocalVar(name), new ArrayList());
		} else if (name.equals("super")) {
			Clazz enclosingClass = getEnclosingClass().clazz;
			if (enclosingClass.superClass() == null) {
				throw new SyntaxError("No super class!", expr.getLine(), expr
						.getCharPositionInLine(), expr.getText().length());
			} else {
				return new Pair(new Cast(enclosingClass.superClass(),
						new LocalVar("this")), new ArrayList());
			}
		}
		// ==========================================================
		// =============== LOOK FOR LOCAL VARIABLES =================
		// ==========================================================

		int i;
		int enclosingDepth = 0;
		ClassScope enclosingClass = getEnclosingClass();
		boolean staticEnclosingClass = false;

		for (i = scopes.size() - 1; i >= 0; --i) {
			Scope scope = scopes.get(i);

			if (scope.variables.contains(name)) {
				return new Pair(new LocalVar(name), new ArrayList());
			} else if (scope.variables.contains(scope.id + name)) {
				return new Pair(new LocalVar(scope.id + name), new ArrayList());
			} else if (scope instanceof ClassScope) {
				// In this case, we need to first check the class hierarchy to
				// see if the field is present. If not, we also need to check
				// the enclosing class as well.
				ClassScope cs = (ClassScope) scope;

				// First, look in the class heirarchy for the field.
				try {
					Triple<Clazz, Field, Type> finfo = ClassTable.resolveField(
							cs.type, name);
					if (!staticEnclosingClass && !finfo.second().isStatic()) {
						Expr e = new LocalVar("this");
						for (int j = 0; j < enclosingDepth; ++j) {
							e = new Deref(e, "this$0");
						}
						return new Pair<Expr, List<Assign>>(new Deref(e, name),
								new ArrayList<Assign>());
					} else {
						// TODO: Is this right?
						return new Pair<Expr, List<Assign>>(new Deref(
								new ClassAccess(cs.type), name),
								new ArrayList<Assign>());
					}
				} catch (ClassNotFoundException e) {
					throw new SyntaxError(e.getMessage(), expr.getChild(0)
							.getLine(), expr.getChild(0)
							.getCharPositionInLine());
				} catch (FieldNotFoundException e) {
				}

				// Second, check enclosing class
				Field f = cs.clazz.getField(name);
				if (f != null) {
					if (!staticEnclosingClass) {
						Expr e = new LocalVar("this");
						for (int j = 0; j < enclosingDepth; ++j) {
							e = new Deref(e, "this$0");
						}
						return new Pair(new Deref(e, name), new ArrayList());
					} else {
						return new Pair(new Deref(new ClassAccess(cs.type),
								name), new ArrayList());
					}
				}
				if (cs.clazz.isStatic()) {
					// Once we reach a static enclosing class, then we can no
					// longer access enclosing class instance fields.
					staticEnclosingClass = true;
				}
				// no joy
				enclosingDepth++;
			}
		}

		// Ok, complete failure to find the problem.

		throw new SyntaxError("Unable to resolve variable \"" + name + "\"",
				expr.getChild(0).getLine(), expr.getChild(0)
						.getCharPositionInLine());
	}

	/**
     * This parses a "new" expression.
     * 
     * @param expr
     * @return
     */
	protected Pair<Expr, List<Assign>> parseNew(Tree expr, FlowGraph cfg,
			Expr outer) {

		// first, parse any parameters supplied
		ArrayList<Tree> methods = new ArrayList<Tree>();
		ArrayList<Tree> fields = new ArrayList<Tree>();

		int end = expr.getChildCount();
		for (int i = 1; i < expr.getChildCount(); ++i) {
			Tree child = expr.getChild(i);
			if (child.getType() == METHOD) {
				// Store anonymous class methods
				methods.add(child);
				end = Math.min(i, end);
			} else if (child.getType() == FIELD) {
				fields.add(child);
				end = Math.min(i, end);
			}
		}

		Pair<List<Expr>, List<Assign>> params = parseExpressionList(1, end,
				expr, cfg);
		List<Expr> parameters = params.first();
		List<Assign> sideEffects = params.second();

		Type type = null;

		if (outer == null) {
			type = parseType(expr.getChild(0));
			// need also to deal with array initialisers

			// === INNER CLASSES ===
			if (type instanceof Type.Reference) {
				Type.Reference enclosingClass = getEnclosingClass().type;
				Type.Reference tr = (Type.Reference) type;
				Pair<String, Type[]>[] targetClasses = tr.classes();
				Pair<String, Type[]>[] enclosingClassClasses = enclosingClass
						.classes();
				try {
					Clazz target = ClassTable.findClass(tr);

					if (!target.isStatic() && targetClasses.length > 1) {
						int matchDepth = 0;
						for (int i = 0; i != Math.min(targetClasses.length,
								enclosingClassClasses.length); ++i) {
							if (targetClasses[i].first().equals(
									enclosingClass.classes()[i].first())) {
								matchDepth = i + 1;
							}
						}
						if (matchDepth > 0
								&& (targetClasses.length - matchDepth) < 2) {
							Expr e = new LocalVar("this");
							for (; matchDepth < enclosingClassClasses.length; ++matchDepth) {
								e = new Deref(e, "this$0");
							}
							parameters.add(0, e);
						} else {
							throw new SyntaxError(
									"An enclosing instance that contains \""
											+ type + "\" is required.", expr
											.getChild(0).getLine(), expr
											.getChild(0)
											.getCharPositionInLine());
						}
					}
				} catch (ClassNotFoundException e) {
					throw new SyntaxError("Unknown class \"" + tr.toString()
							+ "\"", expr.getChild(0).getLine(), expr
							.getChild(0).getCharPositionInLine());
				}
			}
		} else {
			// The following code is for dealing with some rather exotic syntax
			// for allowing inner classes to be associated with their outer
			// class. For example:
			//
			// class Test {
			// public class Inner {}
			// public static void main(String[] args) {
			// Test test = new Test();
			// Test.Inner x = test.new Inner();
			// }
			// }
			//
			// Here, "outer" corresponds to the expression "test."
			parameters.add(0, outer);
			String name = expr.getChild(0).getText();
			Pair<String, Type[]>[] classes = new Pair[1];
			classes[0] = new Pair<String, Type[]>(name, new Type[0]);
			try {
				type = loader.resolve(classes[0].first(), imports);
			} catch (ClassNotFoundException e) {
				throw new SyntaxError("class \"" + classes[0].first()
						+ "\" not found.", expr.getLine(), expr
						.getCharPositionInLine(), expr.getText().length());
			}
		}

		if (methods.size() > 0 || fields.size() > 0) {
			// note, if methods.size() > 0 then type is always an instanceof
			// Type.Reference. The ANTLR parser enforces this constraint.
			type = parseAnonClass(expr, methods, fields, (Type.Reference) type);
		}

		return new Pair(new New(type, parameters), sideEffects);
	}

	private static int anonClassCount = 1;

	protected Type parseAnonClass(Tree expr, ArrayList<Tree> methods,
			ArrayList<Tree> fields, Type.Reference superType) {
		ClassScope cls = getEnclosingClass();
		Type.Reference clsType = cls.type;

		String pkg = clsType.pkg();

		Pair<String, Type[]>[] classes = clsType.classes();
		Pair<String, Type[]>[] nClasses = new Pair[classes.length + 1];
		System.arraycopy(classes, 0, nClasses, 0, classes.length);
		nClasses[classes.length] = new Pair<String, Type[]>(String
				.valueOf(anonClassCount++), new Type[0]);

		Type.Reference anonType = Type.referenceType(pkg, nClasses);

		ClassScope myScope = new ClassScope(null);
		scopes.push(myScope);

		myScope.type = anonType;
		myScope.superClass = (Type.Reference) superType;

		// At this point, we need to know whether the enclosing method is static
		// or not, as this affects whether or not this anonymous class is
		// static.
		//
		// There is a bug remaining when the anonymous class is part of
		// a field initialiser, rather than a method. In this case, if the field
		// is static then the anonymous class should be static.
		int modifiers = 0;
		MethodScope enclosingMethod = getEnclosingMethod();
		if (enclosingMethod != null
				&& (enclosingMethod.modifiers & Modifier.STATIC) != 0) {
			modifiers |= Modifier.STATIC;
		}

		// Determine whether the super type is actually a superclass, or an
		// implemented interface.
		List<Type.Reference> interfaces = new ArrayList<Type.Reference>();
		try {
			Clazz superClazz = ClassTable.findClass(superType);
			if(superClazz.isInterface()) {
				interfaces.add(superType);
				superType = Type.referenceType("java.lang","Object");				
			} 
		} catch (ClassNotFoundException e) {
			throw new SyntaxError("Unable to resolve class.", expr
					.getLine(), expr.getCharPositionInLine(),
					expr.getText().length());
		}

		// Now, create the class!
		Clazz myClazz = new Clazz(modifiers, anonType,
				(Type.Reference) superType, interfaces,
				true);
		myScope.clazz = myClazz;

		anonClasses.add(myClazz);

		HashMap<Type, Method> meths = new HashMap<Type, Method>();
		for (Tree t : methods) {
			Method m = parseMethodDeclaration(t, true);
			meths.put(m.type(), m);
			myClazz.methods().add(m);
		}

		HashMap<Type, Field> fs = new HashMap<Type, Field>();
		for (Tree t : fields) {
			List<Field> fss = parseFieldDeclaration(t, false);
			for (Field f : fss) {
				fs.put(f.type(), f);
				myClazz.fields().add(f);
			}
		}

		loader.updateInfo(myClazz);

		for (Tree t : methods) {
			Method m = parseMethodDeclaration(t, false);

			for (Method origM : myClazz.methods(m.name())) {
				if (origM.type().equals(m.type())) {
					origM.setCode(m.code());
				}
			}
		}

		scopes.pop();

		return anonType;
	}

	protected Pair<Expr, List<Assign>> parseSelector(Tree selector,
			FlowGraph cfg) {
		Expr expr;
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

		ArrayList<Assign> sideEffects = new ArrayList<Assign>();

		if (target.getType() == VAR) {
			try {
				expr = parseVariable(target).first();
			} catch (SyntaxError e) {
				// ok, must be a class access!
				String pkgname = "";
				String pkg = "";
				String name = "";
				// first resolve package part
				boolean firstTime = true;
				idx = 0;
				do {
					// This loop is just disgusting. Can it be improved?
					pkg = pkg.length() == 0 ? name : pkg + "." + name;
					name = target.getChild(0).getText();
					pkgname = firstTime ? name : pkg + "." + name;
					target = selector.getChild(++idx);
					firstTime = false;
				} while (idx < selector.getChildCount()
						&& target.getType() == DEREF
						&& loader.isPackage(pkgname));

				Type.Reference type;
				if (idx == selector.getChildCount()) {
					throw new SyntaxError("Unable to resolve class.", selector
							.getLine(), selector.getCharPositionInLine(),
							selector.getText().length());
				} else if (pkg.length() == 0) {
					// no package supplied, need to figure it out!
					Pair<String, Type[]>[] classes = new Pair[1];
					classes[0] = new Pair(name, new Type[0]);
					try {
						type = resolveLocalType(classes);
					} catch (ClassNotFoundException ex) {
						throw new SyntaxError("Unable to resolve class.",
								selector.getLine(), selector
										.getCharPositionInLine(), selector
										.getText().length());
					}
				} else {
					// package provided, so that's OK!!
					// FIXME: problem if it's actually a pkg/class
					type = Type.referenceType(pkg, name);
				}

				expr = new ClassAccess(type);
			}
		} else {
			// FIXME: side effects
			Pair<Expr, List<Assign>> el = parseExpression(target, cfg);
			expr = el.first();
			sideEffects.addAll(el.second());
		}
		// do stuff here
		for (int i = idx; i != selector.getChildCount(); ++i) {
			Tree child = selector.getChild(i);
			switch (child.getType()) {
				case DEREF :
					String dname = child.getChild(0).getText();
					if (dname.equals("this")) {
						for (int j = scopes.size() - 1; j >= 0; --j) {
							Scope sp = scopes.get(j);
							if (sp instanceof ClassScope) {
								ClassScope scp = (ClassScope) sp;
								if (expr instanceof ClassAccess
										&& scp.type.equals(expr.type)) {
									// TODO: This only works with one depth
									// inner classes
									// How do we make this more generic for
									// multiple depth?
									expr = new Deref(new LocalVar("this"),
											"this$0");
								}
							}
						}
					} else {
						if (expr instanceof ClassAccess) {
							Type.Reference tmpRef = ((ClassAccess) expr).clazz;
							Pair<String, Type[]>[] nclasses = new Pair[tmpRef
									.classes().length + 1];
							System.arraycopy(tmpRef.classes(), 0, nclasses, 0,
									tmpRef.classes().length);
							nclasses[nclasses.length - 1] = new Pair<String, Type[]>(
									dname, new Type[0]);
							Type.Reference nRef = Type.referenceType(tmpRef
									.pkg(), nclasses);
							try {
								ClassTable.findClass(nRef);
								expr = new ClassAccess(nRef);
							} catch (ClassNotFoundException ce) {
								expr = new Deref(expr, dname);
							}

						} else {
							expr = new Deref(expr, dname);
						}
					}
					break;
				case ARRAYINDEX : {
					Pair<Expr, List<Assign>> el = parseExpression(child
							.getChild(0), cfg);
					if (!el.second().isEmpty()) {
						// Deal with side effects here.
						LVal tv = createTemporary(cfg);
						sideEffects.add(new Assign(tv, expr));
						sideEffects.addAll(el.second());
						expr = tv;
					}
					expr = new ArrayIndex(expr, el.first());
					break;
				}
				case INVOKE : {
					// First, check for type parameters. These are present for
                    // method invocations which explicitly indicate the type
                    // parameters to use. For example, x.<K>someMethod();
					int start = 0;
					if(child.getChild(0).getType() == TYPE_PARAMETER) {		
						start++;
					} 
					String method = child.getChild(start).getText();
					Pair<List<Expr>, List<Assign>> params = parseExpressionList(
							start+1, child.getChildCount(), child, cfg);
					if (!params.second().isEmpty()) {
						// Deal with side effects here.
						LVal tv = createTemporary(cfg);
						sideEffects.add(new Assign(tv, expr));
						sideEffects.addAll(params.second());
						expr = tv;
					}
					// Need to catch situation where we have a call such as
					// super.f(). In such a case, we need to have a
					// non-polymorphic call otherwise things go wrong!
					if (i == idx && target.getType() == VAR
							&& target.getChild(0).getText().equals("super")) {
						expr = new Invoke(expr, method, params.first(), false);
					} else {
						expr = new Invoke(expr, method, params.first());
					}
					break;
				}
				case NEW :
					Pair<Expr, List<Assign>> el = parseNew(child, cfg, expr);
					if (!el.second().isEmpty()) {
						// Deal with side effects here.
						LVal tv = createTemporary(cfg);
						sideEffects.add(new Assign(tv, expr));
						sideEffects.addAll(el.second());
						expr = tv;
					}
					expr = el.first();
					break;
				default :
					throw new SyntaxError("Unknown expression encountered.",
							selector.getLine(), selector
									.getCharPositionInLine(), selector
									.getText().length());
			}
		}
		return new Pair(expr, sideEffects);
	}

	public Pair<List<Expr>, List<Assign>> parseExpressionList(int start,
			int end, Tree expr, FlowGraph cfg) {

		if (end - start == 1
				&& expr.getChild(start).getType() == JavaParser.ARGS) {
			Tree args = expr.getChild(start);
			return parseExpressionList(0, args.getChildCount(), args, cfg);
		}

		ArrayList<Assign> sideEffects = new ArrayList<Assign>();
		ArrayList<Expr> es = new ArrayList<Expr>();

		boolean hasSideEffects = false;
		for (int i = end; i > start; --i) {
			Pair<Expr, List<Assign>> el = parseExpression(expr.getChild(i - 1),
					cfg);
			if (hasSideEffects) {
				LVal v = createTemporary(cfg);
				sideEffects.add(0, new Assign(v, el.first()));
				es.add(0, v);
			} else {
				es.add(0, el.first());
			}
			sideEffects.addAll(0, el.second());
			if (!el.second().isEmpty()) {
				hasSideEffects = true;
			}
		}

		return new Pair<List<Expr>, List<Assign>>(es, sideEffects);
	}

	public Pair<Expr, List<Assign>> parseGetClass(Tree expr) {
		Type t = parseType(expr.getChild(0));
		return new Pair(new ClassVal(t), new ArrayList());
	}

	/**
     * This method parses an isolated invoke call. For example, "f()" is
     * isolated, whilst "x.f()" or "this.f()" etc are not.
     * 
     * @param expr
     * @return
     */
	public Pair<Expr, List<Assign>> parseInvoke(Tree expr, FlowGraph cfg) {
				
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
		
		Pair<List<Expr>, List<Assign>> params = parseExpressionList(start+1, expr
				.getChildCount(), expr, cfg);

		// =================================================
		// ========= CHECK INHERITANCE HEIRARCHY ===========
		// =================================================

		// At this stage, we check whether there is a method with the given name
		// in the inheritance hierarchy. If there is, then this is a simple
		// dereference via "this".

		Clazz enclosingClass = getEnclosingClass().clazz;
		Clazz c = enclosingClass;

		if (method.equals("super")) {
			if (c.superClass() == null) {
				throw new SyntaxError("No super class!", expr.getLine(), expr
						.getCharPositionInLine(), expr.getText().length());
			} else {
				method = c.superClass().name();
				return new Pair(new Invoke(new Cast(c.superClass(),
						new LocalVar("this")), method, params.first(), false),
						params.second());
			}
		} else if (method.equals("this")) {
			return new Pair(new Invoke(new LocalVar("this"), c.name(), params
					.first()), params.second());
		}

		int i;
		for (i = scopes.size() - 1; i >= 0; --i) {
			Scope scope = scopes.get(i);
			if (scope instanceof ClassScope) {
				c = ((ClassScope) scope).clazz;
				break;
			}
		}

		while (c != null) {
			if (c.methods(method).size() != 0) {
				return new Pair(new Invoke(new LocalVar("this"), method, params
						.first()), params.second());
			}
			try {
				if (c.superClass() == null) {
					c = null;
				} else {
					c = ClassTable.findClass(c.superClass());
				}
			} catch (ClassNotFoundException e) {
				throw new SyntaxError(e.getMessage(), expr.getLine(), expr
						.getCharPositionInLine(), expr.getText().length());
			}
		}

		// =================================================
		// ============ CHECK ENCLOSING CLASS ==============
		// =================================================

		int enclosingDepth = 1;
		boolean staticOnly = enclosingClass.isStatic();

		for (i = i - 1; i >= 0; --i) {
			// this could be optimised further.
			Scope scope = scopes.get(i);
			if (scope instanceof ClassScope) {
				c = ((ClassScope) scope).clazz;

				while (c != null) {
					if (c.methods(method).size() != 0) {
						if (!staticOnly) {
							Expr e = new LocalVar("this");
							for (int j = 0; j < enclosingDepth; ++j) {
								e = new Deref(e, "this$0");
							}
							return new Pair(new Invoke(e, method, params
									.first()), params.second());
						} else {
							return new Pair(new Invoke(
									new ClassAccess(c.type()), method, params
											.first()), params.second());
						}
					}

					try {
						if (c.superClass() == null) {
							c = null;
						} else {
							c = ClassTable.findClass(c.superClass());
						}
					} catch (ClassNotFoundException e) {
						throw new SyntaxError(e.getMessage(), expr.getLine(),
								expr.getCharPositionInLine(), expr.getText()
										.length());
					}
				}
				c = ((ClassScope) scope).clazz;
				if (c.isStatic()) {
					staticOnly = true;
				}
				enclosingDepth++;
			}
		}

		// out of options. must be an error!
		throw new SyntaxError("Unable to resolve method \"" + method + "\"",
				expr.getChild(0).getLine(), expr.getChild(0)
						.getCharPositionInLine());
	}

	/**
     * This method creates a temporary variable
     */
	protected LVal createTemporary(FlowGraph cfg) {
		String name = "$tmp" + cfg.localVariables().size();
		cfg.add(new LocalVarDef(name, Type.anyType(), 0, false));
		return new LocalVar(name);
	}

	/**
     * This method is used to construct sequences of statements
     */
	protected Block newStmtBlock(Tree stmt, FlowGraph cfg, List<Assign> stmts) {
		Block block = new Block();

		int line = stmt.getLine();
		int col = stmt.getCharPositionInLine();

		for (Stmt s : stmts) {
			Point next = new Point(s, "unknown", line, col);

			if (!block.isEmpty() && block.hasSequentialExit()) {
				cfg.add(new Triple<Point, Point, Expr>(block.sequentialExit(),
						next, null));
			}
			block.add(next);
		}

		return block;
	}

	/**
     * This method is used to help construct sequences of statements. It should
     * be pushed into a new class called ControlFlowGraph!
     */
	protected Block newStmtBlock(Tree stmt, FlowGraph cfg, Stmt... stmts) {
		Block block = new Block();

		int line = stmt.getLine();
		int col = stmt.getCharPositionInLine();

		for (Stmt s : stmts) {
			Point next = new Point(s, "unknown", line, col);

			if (!block.isEmpty() && block.hasSequentialExit()) {
				cfg.add(new Triple<Point, Point, Expr>(block.sequentialExit(),
						next, null));
			}
			block.add(next);
		}
		return block;
	}

	/**
     * This method simply checks whether a statement corresponds to a "super()",
     * or this() call
     */
	protected boolean isSuperOrThisCall(Stmt stmt, Clazz owner) {
		if (stmt instanceof Invoke) {
			Invoke ivk = (Invoke) stmt;
			return ivk.name.equals(owner.superClass().name())
					|| ivk.name.equals(owner.name());
		}
		return false;
	}

	/**
     * This method searches the scopes stack looking for the last defined class.
     * 
     */
	protected ClassScope getEnclosingClass() {
		for (int i = scopes.size() - 1; i >= 0; --i) {
			Scope s = scopes.get(i);
			if (s instanceof ClassScope) {
				return (ClassScope) s;
			}
		}
		return null;
	}

	/**
     * This method searches the scopes stack looking for the last defined class.
     * 
     */
	protected ClassScope getEnclosingClassOf(Type t) {
		boolean matched = false;
		for (int i = scopes.size() - 1; i >= 0; --i) {
			Scope s = scopes.get(i);
			if (s instanceof ClassScope) {
				ClassScope cs = (ClassScope) s;
				if (cs.type.equals(t)) {
					matched = true;
				} else if (matched) {
					return (ClassScope) s;
				}
			}
		}
		return null;
	}

	/**
     * This method searches the scopes stack looking for the last defined loop.
     * This is needed for figuring out where break/continue statements goto.
     */
	protected LoopScope getEnclosingLoop() {
		for (int i = scopes.size() - 1; i >= 0; --i) {
			Scope s = scopes.get(i);
			if (s instanceof LoopScope) {
				return (LoopScope) s;
			}
		}
		return null;
	}

	/**
     * This method searches the scopes stack looking for the last defined
     * switch. This is needed for figuring out where break/continue statements
     * goto.
     */
	protected SwitchScope getEnclosingSwitch() {
		for (int i = scopes.size() - 1; i >= 0; --i) {
			Scope s = scopes.get(i);
			if (s instanceof SwitchScope) {
				return (SwitchScope) s;
			}
		}
		return null;
	}

	/**
     * This method searches the scopes stack looking for a labelled loop. This
     * is needed for figuring out where labelled break/continue statements goto.
     */
	protected LoopScope getLabelledLoop(String label) {
		for (int i = scopes.size() - 1; i >= 0; --i) {
			Scope s = scopes.get(i);
			if (s instanceof LoopScope) {
				LoopScope ls = (LoopScope) s;
				if (ls.label != null && ls.label.equals(label)) {
					return ls;
				}
			}
		}
		return null;
	}

	/**
     * This method searches the scopes stack looking for a method scope. This is
     * needed when figuring out whether the enclosing method containing an
     * anonymous class declaration is static or not.
     * 
     * @return
     */
	protected MethodScope getEnclosingMethod() {
		for (int i = scopes.size() - 1; i >= 0; --i) {
			Scope s = scopes.get(i);
			if (s instanceof MethodScope) {
				return (MethodScope) s;
			}
		}
		return null;
	}

	/**
     * This method attempts to construct a Code.Point object representing the
     * corresponding location in the source file.
     */
	protected Point codePoint(Stmt s, Tree t) {
		// System.out.println("Creating Code Point for " + s);
		// System.out.println(t.getLine());
		return new Point(s, "unknown", t.getLine(), t.getCharPositionInLine());
	}

	protected String localTypeString(Type.Reference type, int len) {
		String fn = "";
		Pair<String, Type[]>[] cs = type.classes();
		for (int i = 0; i != len; ++i) {
			if (i == 0) {
				fn = cs[i].first();
			} else {
				fn += "$" + cs[i].first();
			}
		}
		return fn;
	}

	/**
     * The purpose of this method is to traverse the entire AST and look for all
     * inner class declarations. This allows forwarding binding for types during
     * the main traversal of the AST
     * 
     * @param ast
     * @param enclosingClass
     *            the name of the enclosing class
     * @param inners
     */
	protected static void searchForInnerClasses(Tree ast,
			String enclosingClass, Set<String> inners) {
		for (int i = 0; i != ast.getChildCount(); ++i) {
			Tree c = ast.getChild(i);
			if (c.getType() == CLASS || c.getType() == INTERFACE) {
				// found class definition, so get its NAME
				int idx = 0;
				if (c.getChild(idx).getType() == MODIFIERS) {
					idx++;
				}
				Tree nc = c.getChild(idx++);
				String name = enclosingClass == null
						? nc.getText()
						: enclosingClass + "$" + nc.getText();
				inners.add(name);
				searchForInnerClasses(c, name, inners);
			}
		}
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
