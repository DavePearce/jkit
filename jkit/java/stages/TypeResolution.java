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
// (C) David James Pearce, 2009. 

package jkit.java.stages;

import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.compiler.Clazz;
import jkit.compiler.SyntacticElement;
import static jkit.compiler.SyntaxError.*;
import static jkit.jil.util.Types.*;
import jkit.java.io.JavaFile;
import jkit.java.tree.Decl;
import jkit.java.tree.Expr;
import jkit.java.tree.Stmt;
import jkit.java.tree.Value;
import jkit.java.tree.Decl.JavaClass;
import jkit.java.tree.Decl.JavaEnum;
import jkit.java.tree.Decl.JavaField;
import jkit.java.tree.Decl.JavaInterface;
import jkit.java.tree.Decl.JavaMethod;
import jkit.java.tree.Stmt.Case;
import jkit.java.tree.Annotation;
import jkit.jil.tree.Modifier;
import jkit.jil.tree.SourceLocation;
import jkit.jil.tree.Type;
import jkit.jil.util.Types;
import jkit.util.Pair;
import jkit.util.Triple;

/**
 * This Class goes through all of the types that have been declared in the
 * source file, and resolves them to fully qualified types. For example,
 * consider this code:
 * 
 * <pre>
 * import java.util.*;
 * 
 * public class Test extends Vector {
 * 	public static void main(String[] args) {
 *       ... 
 *      }
 * }
 * </pre>
 * 
 * After type resolution, we will have resolved the following types:
 * 
 * <pre>
 *    Vector -&gt; java.util.Vector
 *    String -&gt; java.lang.String
 * </pre>
 * 
 * Thus, in principle, we could use this information to eliminate any import
 * statements (although type resolution by itself does not do this).
 * 
 * Other examples of declared types which are resolved include the declared
 * superclass of a class, the type of a field, the parameters of a method and
 * the type constructed by a new statement.
 * 
 * Note that this operation must be performed independently from type
 * propagation, since we must determine the skeleton of all classes being
 * compiled before we can do any type propagation. Note, a skeleton of a class
 * provides the fully qualified types of all interfaces, superclasses, fields
 * and methods (including return types, and parameters).
 */
public class TypeResolution {
	private ClassLoader loader;
	private TypeSystem types;
	// the classes stack is used to keep track of the full type for the inner
	// classes.
	
	private static class Scope {
		// The type vars map is used to map type variables to their proper type.
		public final HashMap<String,Type.Variable> typeVars = new HashMap();		
		public Type.Clazz type;	
				
		public Scope(Type.Clazz type) { this.type = type; }
	}	
	
	private static class MethodScope extends Scope {		
		// This set identifies what classes are currently visible.
		public HashMap<String,String> localClasses = new HashMap();
		
		public MethodScope() {
			super(null);
		}
	}
	
	private static class ClassScope extends Scope {
		// The local classes map is used for classes which are declared in
		// methods.
		public final HashMap<String,Integer> localClasses = new HashMap();
		
		public ClassScope() {
			super(null);
		}
	}
	
	private Stack<Scope> scopes = new Stack<Scope>();
	private LinkedList<String> imports = new LinkedList<String>();
	
	public TypeResolution(ClassLoader loader, TypeSystem types) {
		this.loader = loader; 
		this.types = types;
	}
	
	public void apply(JavaFile file) { 		
		imports.add(file.pkg() + ".*");	
		for(Pair<Boolean,String> i : file.imports()) {
			imports.add(1,i.second());			
		}											
		imports.add("java.lang.*");
		
		// The first entry on to the classes stack is a dummy to set the package
		// for remaining classes.		
		scopes.push(new Scope(new Type.Clazz(file.pkg(), new ArrayList())));		
		
		// Now, examine all the declarations contain here-in
		for(Decl d : file.declarations()) {			
			doDeclaration(d);
		}
	}
	
	protected void doDeclaration(Decl d) {
		try {
			if(d instanceof JavaInterface) {
				doInterface((JavaInterface)d);
			} else if(d instanceof JavaEnum) {
				doEnum((JavaEnum)d);
			} else if(d instanceof JavaClass) {
				doClass((JavaClass)d);
			} else if(d instanceof JavaMethod) {
				doMethod((JavaMethod)d);
			} else if(d instanceof JavaField) {
				doField((JavaField)d);
			} else if (d instanceof Decl.InitialiserBlock) {
				doInitialiserBlock((Decl.InitialiserBlock) d);
			} else if (d instanceof Decl.StaticInitialiserBlock) {
				doStaticInitialiserBlock((Decl.StaticInitialiserBlock) d);
			} else {
				syntax_error("internal failure (unknown declaration \"" + d
						+ "\" encountered)",d);
			}
		} catch(Exception ex) {
			internal_error(d,ex);
		}
	}
	

	protected void doEnum(JavaEnum en) throws ClassNotFoundException {				
		doClass(en);
		
		for (Decl.EnumConstant c : en.constants()) {
			for(Expr e : c.arguments()) {
				doExpression(e);
			}
			for(Decl d : c.declarations()) {
				// THERE'S PROBABLY A BUG HERE, SINCE THE CLASS SCOPE IS NOT ON
                // STACK AT THIS POINT.
				doDeclaration(d);
			}
		}
	}
	
	protected void doInterface(JavaInterface d) throws ClassNotFoundException {
		doClass(d);
	}
	
	protected void doClass(JavaClass c) throws ClassNotFoundException {		
		// First, add myself to the import list, since that means we'll search
		// in my class for types before searching anywhere else i've declared
		// and/or on the CLASSPATH.
		Type.Clazz parentType = getEnclosingClassType();
		ClassScope classScope = getEnclosingScope(ClassScope.class);
		MethodScope methodScope = getEnclosingScope(MethodScope.class);
		
		LinkedList<String> oldImports = (LinkedList<String>) imports.clone();
		
		imports.addFirst(computeImportDecl(parentType,c.name()));
		
		resolve(c.modifiers());
		
		// Second, create my scope.				
		ClassScope myScope = new ClassScope();
		scopes.push(myScope);
		
		// Third, build my fully qualified type!
		
		List<Pair<String, List<Type.Reference>>> components = new ArrayList(
				parentType.components());
		
		if(c.isStatic() || c.isInterface()) {
			// In the case of a static class, the fully qualified type only
			// contains those generic types explicitly declared here, but not
			// hose of its parent. So, I strip off those components of the
			// parent here. Note, that this is necessary in order to deal with
			// some icky type binding stuff.						
			
			for(int i=0;i!=components.size();++i) {
				Pair<String,List<Type.Reference>> p = components.get(i);
				components.set(i,new Pair(p.first(),new ArrayList()));
			}
		} 
		
		ArrayList<Type.Reference> typevars = new ArrayList<Type.Reference>();
		for (jkit.java.tree.Type.Variable v : c.typeParameters()) {
			Type.Variable tv = (Type.Variable) substituteTypeVars(resolve(v));
			typevars.add(tv);
			myScope.typeVars.put(tv.variable(), tv);
		}
		
		String name = c.name();
		if(methodScope != null) {
			// this is for the rather unusual situation when a class is definied
			// inside a method.
			Integer localCount = classScope.localClasses.get(name);
			int lc = localCount == null ? 1 : localCount;						
			String newname = lc + name;			
			classScope.localClasses.put(name,lc+1);
			methodScope.localClasses.put(name,newname);
			name = newname;
		}
				
		components.add(new Pair(name,typevars));
		Type.Clazz myType = new Type.Clazz(parentType.pkg(),components);		
		c.attributes().add(myType); // record the type
						
		myScope.type = myType;		
		
		// 1) resolve types in my declared super class.
		if(c.superclass() != null) {
			Type.Clazz superType = (Type.Clazz) substituteTypeVars(resolve(c.superclass()));
			c.superclass().attributes().add(superType);
			imports.addAll(1,computeRecursiveImportDecls(superType));
			
		}		

		// 2) resolve types in my declared interfaces
		for(jkit.java.tree.Type.Clazz i : c.interfaces()) {
			Type.Clazz interType = (Type.Clazz) substituteTypeVars(resolve(i)); 
			i.attributes().add(interType);
			imports.addAll(1,computeRecursiveImportDecls(interType));
		}			 						
		
		// 3) resolve types in my other declarations (e.g. fields, methods,inner
		// classes, etc)
		for(Decl d : c.declarations()) {
			doDeclaration(d);
		}
		
		scopes.pop(); // undo my type
		
		imports = oldImports; // undo my old imports
	}

	protected void doMethod(JavaMethod d) throws ClassNotFoundException {
		Scope myScope = new MethodScope();		
		scopes.push(myScope);
				
		// Add my generic variables (sorry, a bit yucky)
		ArrayList<Type.Variable> typeVars = new ArrayList<Type.Variable>();
		for (jkit.java.tree.Type.Variable v : d.typeParameters()) {			
			Type.Variable tv = (Type.Variable) substituteTypeVars(resolve(v));
			myScope.typeVars.put(v.variable(), tv);
			typeVars.add(tv);
			v.attributes().add(tv);
		}		
		
		// First, resolve any annotations present
		resolve(d.modifiers());
		
		// Second, resolve return type and parameter types. 
		for(jkit.java.tree.Type.Clazz e : d.exceptions()) {
			e.attributes().add(substituteTypeVars(resolve(e)));
		}		
				
		Type returnType = T_VOID;
		List<Type> parameterTypes = new ArrayList<Type>();
		
		if(d.returnType() != null) {
			// The return type may be null iff this is a constructor.
			returnType = substituteTypeVars(resolve(d.returnType()));
			d.returnType().attributes().add(returnType);			
		}
		
		int paramLength = d.parameters().size();
		
		if(d.isVariableArity()) { paramLength--; }
		
		for (int i = 0; i != paramLength; ++i) {
			Decl.JavaParameter p = d
					.parameters().get(i);
			Type pt = substituteTypeVars(resolve(p.type()));						
			p.type().attributes().add(pt);
			parameterTypes.add(pt);
		}
		
		if(d.isVariableArity()) {
			Decl.JavaParameter p = d
					.parameters().get(paramLength);
			Type pt = new Type.Array(resolve(p.type()));
			p.type().attributes().add(pt);
			parameterTypes.add(pt);
		}
				
		d.attributes().add(
				new Type.Function(returnType, parameterTypes,
						typeVars));		
		
		// Now, explore the method body for any other things to resolve.
		doStatement(d.body());
		
		scopes.pop();
	}
	
	protected void doField(JavaField d) throws ClassNotFoundException {
		resolve(d.modifiers());
		doExpression(d.initialiser());				
		d.type().attributes().add(substituteTypeVars(resolve(d.type())));				
	}
	
	protected void doInitialiserBlock(Decl.InitialiserBlock d) {
		// will need to add code here for dealing with classes nested in
		// methods.
		for(Stmt s : d.statements()) {
			doStatement(s);
		}		
	}
	
	protected void doStaticInitialiserBlock(Decl.StaticInitialiserBlock d) {
		// will need to add code here for dealing with classes nested in
		// methods.
		for(Stmt s : d.statements()) {
			doStatement(s);
		}		
	}
	
	protected void doStatement(Stmt e) {
		try {
			if(e instanceof Stmt.SynchronisedBlock) {
				doSynchronisedBlock((Stmt.SynchronisedBlock)e);
			} else if(e instanceof Stmt.TryCatchBlock) {
				doTryCatchBlock((Stmt.TryCatchBlock)e);
			} else if(e instanceof Stmt.Block) {
				doBlock((Stmt.Block)e);
			} else if(e instanceof Stmt.VarDef) {
				doVarDef((Stmt.VarDef) e);
			} else if(e instanceof Stmt.Assignment) {
				doAssignment((Stmt.Assignment) e);
			} else if(e instanceof Stmt.Return) {
				doReturn((Stmt.Return) e);
			} else if(e instanceof Stmt.Throw) {
				doThrow((Stmt.Throw) e);
			} else if(e instanceof Stmt.Assert) {
				doAssert((Stmt.Assert) e);
			} else if(e instanceof Stmt.Break) {
				doBreak((Stmt.Break) e);
			} else if(e instanceof Stmt.Continue) {
				doContinue((Stmt.Continue) e);
			} else if(e instanceof Stmt.Label) {
				doLabel((Stmt.Label) e);
			} else if(e instanceof Stmt.If) {
				doIf((Stmt.If) e);
			} else if(e instanceof Stmt.For) {
				doFor((Stmt.For) e);
			} else if(e instanceof Stmt.ForEach) {
				doForEach((Stmt.ForEach) e);
			} else if(e instanceof Stmt.While) {
				doWhile((Stmt.While) e);
			} else if(e instanceof Stmt.DoWhile) {
				doDoWhile((Stmt.DoWhile) e);
			} else if(e instanceof Stmt.Switch) {
				doSwitch((Stmt.Switch) e);
			} else if(e instanceof Expr.Invoke) {
				doInvoke((Expr.Invoke) e);
			} else if(e instanceof Expr.New) {
				doNew((Expr.New) e);
			} else if(e instanceof Stmt.PrePostIncDec) {
				doExpression((Stmt.PrePostIncDec)e);
			} else if(e instanceof Decl.JavaClass) {				
				doClass((Decl.JavaClass)e);
			} else if(e != null) {
				syntax_error("Invalid statement encountered: "
						+ e.getClass(),e);
			}		
		} catch(Exception ex) {
			internal_error(e,ex);
		}
	}
	
	protected void doBlock(Stmt.Block block) {
		if(block != null) {
			// I need to push a scope to deal with classes defined locally in
			// the method.  
			MethodScope methodScope = getEnclosingScope(MethodScope.class);
			HashMap<String,String> localClasses = null;
			if(methodScope != null) {
				localClasses = methodScope.localClasses;
				methodScope.localClasses = new HashMap(localClasses);
			}
			
			// now process every statement in this block.
			for(Stmt s : block.statements()) {
				doStatement(s);
			}
			
			if(methodScope != null) {
				methodScope.localClasses = localClasses;
			}
		}
	}
	
	protected void doSynchronisedBlock(Stmt.SynchronisedBlock block) {
		doBlock(block);
		doExpression(block.expr());
	}
	
	protected void doTryCatchBlock(Stmt.TryCatchBlock block) throws ClassNotFoundException {
		doBlock(block);		
		doBlock(block.finaly());		
		
		for(Stmt.CatchBlock cb : block.handlers()) {
			cb.type().attributes().add(substituteTypeVars(resolve(cb.type())));
			doBlock(cb);			
		}
	}
	
	protected void doVarDef(Stmt.VarDef def) throws ClassNotFoundException {
		Type t = substituteTypeVars(resolve(def.type()));		
		def.type().attributes().add(t);
		
		List<Triple<String, Integer, Expr>> defs = def.definitions();
		for(int i=0;i!=defs.size();++i) {
			Triple<String, Integer, Expr> d = defs.get(i);			
			doExpression(d.third());														
		}
	}
	
	protected void doAssignment(Stmt.Assignment def) {
		doExpression(def.lhs());	
		doExpression(def.rhs());			
	}
	
	protected void doReturn(Stmt.Return ret) {
		doExpression(ret.expr());
	}
	
	protected void doThrow(Stmt.Throw ret) {
		doExpression(ret.expr());
	}
	
	protected void doAssert(Stmt.Assert ret) {
		doExpression(ret.expr());
	}
	
	protected void doBreak(Stmt.Break brk) {
		// nothing	
	}
	
	protected void doContinue(Stmt.Continue brk) {
		// nothing
	}
	
	protected void doLabel(Stmt.Label lab) {						
		doStatement(lab.statement());
	}
	
	protected void doIf(Stmt.If stmt) {
		doExpression(stmt.condition());
		doStatement(stmt.trueStatement());
		doStatement(stmt.falseStatement());
	}
	
	protected void doWhile(Stmt.While stmt) {
		doExpression(stmt.condition());
		doStatement(stmt.body());		
	}
	
	protected void doDoWhile(Stmt.DoWhile stmt) {
		doExpression(stmt.condition());
		doStatement(stmt.body());
	}
	
	protected void doFor(Stmt.For stmt) {
		doStatement(stmt.initialiser());
		doExpression(stmt.condition());
		doStatement(stmt.increment());
		doStatement(stmt.body());	
	}
	
	protected void doForEach(Stmt.ForEach stmt) throws ClassNotFoundException {
		Type t = substituteTypeVars(resolve(stmt.type()));
		stmt.type().attributes().add(t);
		doExpression(stmt.source());
		doStatement(stmt.body());
	}
	
	protected void doSwitch(Stmt.Switch sw) {
		doExpression(sw.condition());
		for(Case c : sw.cases()) {
			doExpression(c.condition());
			for(Stmt s : c.statements()) {
				doStatement(s);
			}
		}
		
		// should check that case conditions are final constants here.
	}
	
	protected void doExpression(Expr e) {
		try {
			if(e instanceof Value.Bool) {
				doBoolVal((Value.Bool)e);
			} else if(e instanceof Value.Char) {
				doCharVal((Value.Char)e);
			} else if(e instanceof Value.Int) {
				doIntVal((Value.Int)e);
			} else if(e instanceof Value.Long) {
				doLongVal((Value.Long)e);
			} else if(e instanceof Value.Float) {
				doFloatVal((Value.Float)e);
			} else if(e instanceof Value.Double) {
				doDoubleVal((Value.Double)e);
			} else if(e instanceof Value.String) {
				doStringVal((Value.String)e);
			} else if(e instanceof Value.Null) {
				doNullVal((Value.Null)e);
			} else if(e instanceof Value.TypedArray) {
				doTypedArrayVal((Value.TypedArray)e);
			} else if(e instanceof Value.Array) {
				doArrayVal((Value.Array)e);
			} else if(e instanceof Value.Class) {
				doClassVal((Value.Class) e);
			} else if(e instanceof Expr.UnresolvedVariable) {
				doVariable((Expr.UnresolvedVariable)e);
			} else if(e instanceof Expr.UnOp) {
				doUnOp((Expr.UnOp)e);
			} else if(e instanceof Expr.BinOp) {
				doBinOp((Expr.BinOp)e);
			} else if(e instanceof Expr.TernOp) {
				doTernOp((Expr.TernOp)e);
			} else if(e instanceof Expr.Cast) {
				doCast((Expr.Cast)e);
			} else if(e instanceof Expr.InstanceOf) {
				doInstanceOf((Expr.InstanceOf)e);
			} else if(e instanceof Expr.Invoke) {
				doInvoke((Expr.Invoke) e);
			} else if(e instanceof Expr.New) {
				doNew((Expr.New) e);
			} else if(e instanceof Expr.ArrayIndex) {
				doArrayIndex((Expr.ArrayIndex) e);
			} else if(e instanceof Expr.Deref) {
				doDeref((Expr.Deref) e);
			} else if(e instanceof Stmt.Assignment) {
				// force brackets			
				doAssignment((Stmt.Assignment) e);			
			} else if(e != null) {
				syntax_error("Invalid expression encountered: "
						+ e.getClass(),e);
			}
		} catch(Exception ex) {
			internal_error(e,ex);
		}
	}
	
	protected void doDeref(Expr.Deref e) {
		doExpression(e.target());		
		// need to perform field lookup here!
	}
	
	protected void doArrayIndex(Expr.ArrayIndex e) {
		doExpression(e.target());
		doExpression(e.index());
	}
	
	protected void doNew(Expr.New e) throws ClassNotFoundException {
		// First, figure out the type being created.		
		Type t = substituteTypeVars(resolve(e.type()));	
						
		e.type().attributes().add(t);			
		
		doExpression(e.context());
		
		// Second, recurse through any parameters supplied ...
		for(Expr p : e.parameters()) {
			doExpression(p);
		}
		
		if(e.declarations().size() > 0) {			
			// Third, check whether this is constructing an anonymous class ...
			for(Decl d : e.declarations()) {
				doDeclaration(d);
			}
		}				
	}
	
	protected void doInvoke(Expr.Invoke e) {
		doExpression(e.target());
		
		for(Expr p : e.parameters()) {
			doExpression(p);
		}
	}
	
	protected void doInstanceOf(Expr.InstanceOf e) throws ClassNotFoundException {		
		e.rhs().attributes().add(substituteTypeVars(resolve(e.rhs())));
		doExpression(e.lhs());
	}
	
	protected void doCast(Expr.Cast e) throws ClassNotFoundException {
		e.type().attributes().add(substituteTypeVars(resolve(e.type())));
		doExpression(e.expr());
	}
	
	protected void doBoolVal(Value.Bool e) {}
	
	protected void doCharVal(Value.Char e) {}
	
	protected void doIntVal(Value.Int e) {}
	
	protected void doLongVal(Value.Long e) {}
	
	protected void doFloatVal(Value.Float e) {}
	
	protected void doDoubleVal(Value.Double e) {}
	
	protected void doStringVal(Value.String e) {}
	
	protected void doNullVal(Value.Null e) {}
	
	protected void doTypedArrayVal(Value.TypedArray e) throws ClassNotFoundException  {
		e.type().attributes().add(substituteTypeVars(resolve(e.type())));
		for(Expr v : e.values()) {
			doExpression(v);
		}
	}
	
	protected void doArrayVal(Value.Array e) {
		for(Expr v : e.values()) {
			doExpression(v);
		}
	}
		
	protected void doClassVal(Value.Class e) throws ClassNotFoundException  {		
		Type t = substituteTypeVars(resolve(e.value()));		
		e.value().attributes().add(t);
	}
	
	protected void doVariable(Expr.UnresolvedVariable e) {					
	}

	protected void doUnOp(Expr.UnOp e) {		
		doExpression(e.expr());
	}
		
	protected void doBinOp(Expr.BinOp e) {				
		doExpression(e.lhs());
		doExpression(e.rhs());		
	}
	
	protected void doTernOp(Expr.TernOp e) {		
		doExpression(e.condition());
		doExpression(e.falseBranch());
		doExpression(e.trueBranch());
	}
		
	/**
	 * The purpose of the resolve method is to examine the type in question, and
	 * determine the fully qualified type it represents, based on the current
	 * import list.
	 * 
	 * @param t
	 * @param file
	 * @return
	 */
	protected jkit.jil.tree.Type resolve(jkit.java.tree.Type t) throws ClassNotFoundException {
		if(t instanceof jkit.java.tree.Type.Primitive) {
			return resolve((jkit.java.tree.Type.Primitive)t);
		} else if(t instanceof jkit.java.tree.Type.Clazz) {
			return resolve((jkit.java.tree.Type.Clazz)t);			
		} else if(t instanceof jkit.java.tree.Type.Array) {
			return resolve((jkit.java.tree.Type.Array)t);
		} else if(t instanceof jkit.java.tree.Type.Wildcard) {
			return resolve((jkit.java.tree.Type.Wildcard)t);
		} else if(t instanceof jkit.java.tree.Type.Variable) {
			return resolve((jkit.java.tree.Type.Variable)t);
		} else if(t instanceof jkit.java.tree.Type.Intersection) {
			return resolve((jkit.java.tree.Type.Intersection)t);
		}
		
		return null;
	}
	
	protected jkit.jil.tree.Type.Intersection resolve(jkit.java.tree.Type.Intersection pt) throws ClassNotFoundException {
		ArrayList<jkit.jil.tree.Type.Reference> bounds = new ArrayList();
		for(jkit.java.tree.Type.Reference b : pt.bounds()) {
			bounds.add((Type.Reference) resolve(b));
		}
		return new jkit.jil.tree.Type.Intersection(bounds);
	}
	
	protected jkit.jil.tree.Type.Primitive resolve(jkit.java.tree.Type.Primitive pt) {
		if(pt instanceof jkit.java.tree.Type.Void) {
			return new jkit.jil.tree.Type.Void();
		} else if(pt instanceof jkit.java.tree.Type.Bool) {
			return new jkit.jil.tree.Type.Bool();
		} else if(pt instanceof jkit.java.tree.Type.Byte) {
			return new jkit.jil.tree.Type.Byte();
		} else if(pt instanceof jkit.java.tree.Type.Char) {
			return new jkit.jil.tree.Type.Char();
		} else if(pt instanceof jkit.java.tree.Type.Short) {
			return new jkit.jil.tree.Type.Short();
		} else if(pt instanceof jkit.java.tree.Type.Int) {
			return new jkit.jil.tree.Type.Int();
		} else if(pt instanceof jkit.java.tree.Type.Long) {
			return new jkit.jil.tree.Type.Long();
		} else if(pt instanceof jkit.java.tree.Type.Float) {
			return new jkit.jil.tree.Type.Float();
		} else {
			return new jkit.jil.tree.Type.Double();
		}
	}
	
	protected jkit.jil.tree.Type.Array resolve(jkit.java.tree.Type.Array t) throws ClassNotFoundException {
		return new jkit.jil.tree.Type.Array(resolve(t.element()));
	}
	
	protected jkit.jil.tree.Type.Wildcard resolve(jkit.java.tree.Type.Wildcard t) throws ClassNotFoundException {				
		 jkit.jil.tree.Type.Wildcard r = new jkit.jil.tree.Type.Wildcard((Type.Reference) resolve(t
				.lowerBound()), (Type.Reference) resolve(t.upperBound()));		 
		 return r;
	}
	
	protected jkit.jil.tree.Type.Variable resolve(jkit.java.tree.Type.Variable t) throws ClassNotFoundException  {		
		Type.Reference arg;
		if(t.lowerBound() != null) {					
			arg = (Type.Reference) resolve(t.lowerBound());
		} else {
			arg = Types.JAVA_LANG_OBJECT;
		}
		return new jkit.jil.tree.Type.Variable(t.variable(),arg);
	}
	
	/**
	 * The key challenge of this method, is that we have a Type.Clazz object
	 * which is incorrectly initialised and/or not fully qualified. An example
	 * of the former would arise from this code:
	 * 
	 * <pre>
	 * public void f(java.util.Vector v) { ... }
	 * </pre>
	 * 
	 * Here, the JavaFileReader will assume that "java" is the outerclass, and
	 * that "util" and "Vector" are inner classes. Thus, we must correct this.
	 * 
	 * An example of the second case, is the following:
	 * 
	 * <pre>
	 * public void f(Vector v) { ... }
	 * </pre>
	 * 
	 * Here, the JavaFileReader will not prepend the appropriate package
	 * information onto the type Vector. Thus, we must look this up here.
	 * 
	 * @param ct
	 *            --- the class type to resolve.
	 * @param file
	 *            --- the JavaFile containing this type; this is required to
	 *            determine the import list.
	 * @return
	 */
	protected jkit.jil.tree.Type.Reference resolve(jkit.java.tree.Type.Clazz ct)
			throws ClassNotFoundException {		
		ArrayList<Pair<String,List<jkit.jil.tree.Type.Reference>>> ncomponents = new ArrayList();
		String className = "";
		String pkg = "";
		MethodScope methodScope = getEnclosingScope(MethodScope.class);
		
		boolean firstTime = true;
		for (int i = 0; i != ct.components().size(); ++i) {
			String tmp = ct.components().get(i).first();
			String tmppkg = pkg.equals("") ? tmp : pkg + "." + tmp;
			
			if (firstTime && loader.isPackage(tmppkg)) {
				pkg = tmppkg;				
			} else {
				String name = ct.components().get(i).first();
				if (!firstTime) {
					className += "$";
				} else if(methodScope != null) {
					String rname = methodScope.localClasses.get(name);
					if(rname != null) {
						name = rname;
					}
				}
				firstTime = false;
				className += name;

				// now, rebuild the component list
				List<jkit.java.tree.Type.Reference> vars = ct.components().get(
						i).second();
				ArrayList<jkit.jil.tree.Type.Reference> nvars = new ArrayList();

				for (jkit.java.tree.Type.Reference r : vars) {
					nvars.add((jkit.jil.tree.Type.Reference) resolve(r));
				}

				ncomponents
						.add(new Pair<String, List<jkit.jil.tree.Type.Reference>>(
								name, nvars));
			}
		}
				
		// now, some sanity checking.
		if(className.equals("")) {
			syntax_error("unable to find class " + pkg,ct);
		} else if(pkg.length() > 0) {
			// could add "containsClass" check here. Need to modify
			// classLoader though.
			return new jkit.jil.tree.Type.Clazz(pkg,ncomponents);			
		}
		
		// So, at this point, it seems there was no package information in the
		// source code and, hence, we need to determine this from the CLASSPATH
		// and the import list. There are two phases. 
						
		Type.Clazz r = loader.resolve(className,imports);		
		
		// The following loop is required for two reasons:
		//
		// 1) we may not have full type information in the source code.
		// 2) we may have generic type information in the source code which
		// need to keep.
		//
		// For example, imagine a class Test, with inner class Inner<G>. The
		// source code may include a reference "Inner<String>". Via resolve
		// above, we'll determine the type to be "Test.Inner<G>". But, the
		// actual type we want is "Test.Inner<String>". Therefore, the
		// following loop combines all the information we have together to
		// achieve this.

		List<Pair<String,List<jkit.jil.tree.Type.Reference>>> rcomponents = r.components();
		for(int i=0;i!=r.components().size();++i) {
			Pair<String,List<jkit.jil.tree.Type.Reference>> p = rcomponents.get(i); 
			if(p.first().equals(ncomponents.get(i).first())) {
				break;
			} else {
				ncomponents.add(i,p);
			}
		}

		return new jkit.jil.tree.Type.Clazz(r.pkg(),ncomponents);					
	}
	

	protected void resolve(List<Modifier> modifiers)
			throws ClassNotFoundException {
		for (int i = 0; i != modifiers.size(); ++i) {
			Modifier m = modifiers.get(i);
			if (m instanceof Annotation) {
				Annotation a = (Annotation) m;
				a.type().attributes().add(resolve(a.type()));
			}
		}
	}
	
	/**
	 * The aim of this method is to substitute occurrences of type variables for
	 * their "full" generic type. For example, consider this code:
	 * 
	 * <pre>
	 * public class Test&lt;T extends String&gt; {
	 *  public void add(T x) { ... }
	 * }
	 * </pre>
	 * 
	 * Here, the declared type of variable x is "T". However, the full type of
	 * variable x is "T extends String". Having the full type is crucial to
	 * being able to resolve method invocations, for example, on that variable.
	 * Therefore, this method identifies occurrences of a variable and replaces
	 * it with the version from the actual declaration.
	 * 
	 * @param t
	 * @return
	 */
	protected Type substituteTypeVars(Type t) {
		if(t instanceof Type.Array) {
			return substituteTypeVars((Type.Array)t);
		} else if(t instanceof Type.Clazz) {
			return substituteTypeVars((Type.Clazz)t);
		} else if(t instanceof Type.Variable) {
			return substituteTypeVars((Type.Variable)t);
		} else if(t instanceof Type.Wildcard) {
			return substituteTypeVars((Type.Wildcard)t);
		}
		
		return t;
	}
	
	protected Type substituteTypeVars(Type.Array t) {
		return new Type.Array(substituteTypeVars(t.element()));
	}
	
	protected Type substituteTypeVars(Type.Clazz t) {
		ArrayList<Pair<String, List<Type.Reference>>> ncomponents = new ArrayList();
		
		for(Pair<String, List<Type.Reference>> p : t.components()) {
			ArrayList<Type.Reference> vars = new ArrayList();
			for(Type.Reference r : p.second()) {
				vars.add((Type.Reference) substituteTypeVars(r));
			}
			ncomponents.add(new Pair(p.first(),vars));
		}
		
		return new Type.Clazz(t.pkg(),ncomponents);
	}
	
	protected Type substituteTypeVars(Type.Wildcard t) {
		return new Type.Wildcard((Type.Reference) substituteTypeVars(t.lowerBound()),
				(Type.Reference) substituteTypeVars(t.upperBound()));
	}
	
	protected Type substituteTypeVars(Type.Variable t) {
		for(int i=scopes.size()-1;i>=0;--i) {
			Scope scope = scopes.get(i);
			Type.Variable v = scope.typeVars.get(t.variable());
			if(v != null) {
				return v;
			}
		}
		// this is probably a syntax error.
		return t;
	}
	
	protected Type.Clazz getEnclosingClassType() {
		for(int i=scopes.size()-1;i>=0;--i) {
			Scope c = scopes.get(i);
			if(c.type != null) {
				return c.type;
			}
		}
		return null;
	}	
	
	protected <T extends Scope> T getEnclosingScope(Class c) {
		for (int i = scopes.size() - 1; i >= 0; --i) {
			Scope s = scopes.get(i);
			if (s.getClass().equals(c)) {
				return (T) s;
			}
		}
		return null;
	}
	
	/**
	 * The purpose of this method is to compute an import declaration from a
	 * given class Type. For example, consider the type "mypkg.MyClass". From
	 * this, we compute an import declaration "import mypkg.MyClass.*". This
	 * simplifies the problem of resolving an internal class, since we can use
	 * the existing method for looking up classes in the ClassLoader.
	 * 
	 * @param type
	 * @return
	 */
	protected String computeImportDecl(Type.Clazz parentType, String clazz) {
		String decl = parentType.pkg();
		if(!decl.equals("")) { decl = decl + "."; }
		for(Pair<String,List<Type.Reference>> p : parentType.components()) {
			decl = decl + p.first() + ".";
		}
		return decl + clazz + ".*";
	}
	
	protected List<String> computeRecursiveImportDecls(Type.Clazz parentType) {
		ArrayList<String> decls = new ArrayList<String>();
				
		try {
			for(Type.Reference st : types.listSupertypes(parentType, loader)) {												
				Clazz c = loader.loadClass((Type.Clazz) st);
				Type.Clazz type = c.type();
				String decl = type.pkg();
				if(!decl.equals("")) { decl = decl + "."; }
				for(Pair<String,List<Type.Reference>> p : type.components()) {
					decl = decl + p.first() + ".";
				}
				decls.add(decl + "*");
			}		
		} catch(ClassNotFoundException cne) {
			// silently give up for now.
		} 
		
		return decls;
	}	
}
