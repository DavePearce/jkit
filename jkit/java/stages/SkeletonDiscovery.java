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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import jkit.compiler.ClassLoader;
import static jkit.compiler.SyntaxError.*;
import jkit.java.io.JavaFile;
import jkit.java.tree.Decl;
import jkit.java.tree.Expr;
import jkit.java.tree.Decl.JavaField;
import jkit.java.tree.Stmt.Case;
import jkit.java.tree.Stmt;
import jkit.jil.tree.JilClass;
import jkit.jil.tree.SourceLocation;
import jkit.jil.tree.SyntacticElement;
import jkit.jil.tree.Type;
import jkit.jil.tree.Modifier;
import jkit.util.Pair;
import jkit.util.Triple;

/**
 * The purpose of the skeleton discovery phase is to traverse the source file in
 * question and identify any classes which are declared within. For each, an
 * empty "skeleton" is created. This can be thought of as simply an empty class.
 * However, subsequent passes will flesh these skeletons out in more detail.
 * 
 * One minor detail, is that anonymous inner classes are not discovered during
 * this phase. The reason for this is that we cannot be sure of their proper
 * types until after type resolution. This does not cause a problem, since an
 * anonymous inner class cannot be referred to explicitly in the source code ---
 * hence, we'll never need to resolve a type which refers to it!
 * 
 * @author djp
 * 
 */
public class SkeletonDiscovery {		
	
	private JavaFile file;
	
	private static class Scope {}
	private static class MethodScope extends Scope {
		public final boolean isStatic;
		public MethodScope(boolean isStatic) {
			this.isStatic = isStatic;
		}
	}
	
	private static class ClassScope extends Scope {
		// The local classes map is used for classes which are declared in
		// methods.
		public final HashMap<String,Integer> localClasses = new HashMap();
	
		public final Type.Clazz type;
		
		public ClassScope(Type.Clazz type) {
			this.type = type;
		}
	}
	
	private Stack<Scope> scopes = new Stack<Scope>();
	
	public List<JilClass> apply(JavaFile file, ClassLoader loader) {
		this.file = file; 
		List<JilClass> skeletons = new ArrayList<JilClass>();
		// Now, traverse the declarations
		for(Decl d : file.declarations()) {
			skeletons.addAll(doDeclaration(d));
		}
		loader.register(skeletons);
		return skeletons;
	}
	
	protected List<JilClass> doDeclaration(Decl d) {	
		try {
			if(d instanceof Decl.JavaInterface) {
				return doInterface((Decl.JavaInterface)d);
			} else if(d instanceof Decl.JavaEnum) {
				return doEnum((Decl.JavaEnum)d);
			} else if(d instanceof Decl.JavaClass) {
				return doClass((Decl.JavaClass)d);
			} else if(d instanceof Decl.JavaMethod) {
				return doMethod((Decl.JavaMethod)d);
			} else if(d instanceof Decl.JavaField) {
				return doField((JavaField)d);
			} else if(d instanceof Decl.InitialiserBlock) {
				return doInitialiserBlock((Decl.InitialiserBlock)d);
			} else if(d instanceof Decl.StaticInitialiserBlock) {
				return doStaticInitialiserBlock((Decl.StaticInitialiserBlock)d);
			} else {
				syntax_error("internal failure (unknown declaration \"" + d
						+ "\" encountered)",d);		
			}
		} catch(Exception ex) {
			internal_error(d,ex);
		}
		return null; // dead code.
	}
	
	protected List<JilClass> doEnum(Decl.JavaEnum d) {
		List<JilClass> cs = doClass(d);
		List<Modifier> modifiers = cs.get(cs.size() - 1).modifiers();
		modifiers.add(Modifier.ACC_ENUM);		
		modifiers.add(Modifier.ACC_STATIC);
		
		boolean failed = false;
		for(Decl.EnumConstant c : d.constants()) {
			if(c.declarations().size() > 0) {
				modifiers.add(Modifier.ACC_ABSTRACT);
				failed=true;
				break;
			}
		}
		
		if(!failed) {
			modifiers.add(Modifier.ACC_FINAL);
		}
		
		return cs;		
	}
	
	protected List<JilClass> doInterface(Decl.JavaInterface d) {
		List<JilClass> cs = doClass(d);
		cs.get(cs.size() - 1).modifiers()
				.add(Modifier.ACC_INTERFACE);
		return cs;
	}
	
	protected List<JilClass> doClass(Decl.JavaClass c) {
		ArrayList<JilClass> skeletons = new ArrayList<JilClass>();
		// At this stage, type resolution has not already occurred and,
		// hence, we have only basic (i.e. non-generic) type information
		// available.
		
		List<Modifier> modifiers = new ArrayList<Modifier>(c.modifiers());
		ClassScope classScope = getEnclosingClassScope();
		List<Pair<String,List<Type.Reference>>> components = new ArrayList();
		String name = c.name();
		
		if(classScope != null) {
			// This class declaration represents some kind of inner class. There
			// are two options to consider: either it's a straight up inner
			// class; or, its a method-local inner class. Note that, anonymous
			// inner classes are actually ignored until skeletonbuilding since
			// they have no impact on type resolution.
			Type.Clazz enclosingType = classScope.type;			
			for (Pair<String, List<Type.Reference>> i : enclosingType.components()) {				
				components.add(i);											
			}
			
			// Finally, check whether or not this is a method-local inner class
			MethodScope methodScope = getEnclosingMethodScope();
			if(methodScope != null) {				
				// Ok, this class is declared in a method so determine its
				// unique name
				HashMap<String,Integer> localClasses = classScope.localClasses;
				Integer count = localClasses.get(name);
				int lc = count == null ? 1 : count;
				localClasses.put(name, lc+1);
				name = lc + name;
				
				if(methodScope.isStatic) {
					modifiers.add(Modifier.ACC_STATIC);
				}
			}
		} 
		
		components.add(new Pair(name, new ArrayList()));
		Type.Clazz type = new Type.Clazz(file.pkg(),components);	
		
		scopes.push(new ClassScope(type));
		
		for(Decl d : c.declarations()) {			
			skeletons.addAll(doDeclaration(d));
		}								
		
		/**
		 * Construct inner classes list.
		 */
		ArrayList<Type.Clazz> inners = new ArrayList<Type.Clazz>();
		for(JilClass jc : skeletons) {
			inners.add(jc.type());
		}
		
		scopes.pop();
		
		/**
		 * Now, construct the skeleton for this class!
		 */
		skeletons.add(new JilClass(type, modifiers, null, new ArrayList(),
				inners, new ArrayList(), new ArrayList()));
						
		return skeletons;
	}

	protected List<JilClass> doMethod(Decl.JavaMethod d) {
		scopes.push(new MethodScope(d.isStatic()));
		List<JilClass> classes = doStatement(d.body());
		scopes.pop();
		return classes;
	}

	protected List<JilClass> doField(Decl.JavaField d) {
		return new ArrayList();
	}
	
	protected List<JilClass> doInitialiserBlock(Decl.InitialiserBlock d) {		
		ArrayList<JilClass> classes = new ArrayList<JilClass>();
		for(Stmt s : d.statements()) {
			classes.addAll(doStatement(s));
		}
		return classes;
	}
	
	protected List<JilClass> doStaticInitialiserBlock(
			Decl.StaticInitialiserBlock d) {		
		ArrayList<JilClass> classes = new ArrayList<JilClass>();
		for(Stmt s : d.statements()) {
			classes.addAll(doStatement(s));
		}
		return classes;		
	}		
	
	protected List<JilClass> doStatement(Stmt e) {
		try {
			if(e instanceof Stmt.SynchronisedBlock) {
				return doSynchronisedBlock((Stmt.SynchronisedBlock)e);
			} else if(e instanceof Stmt.TryCatchBlock) {
				return doTryCatchBlock((Stmt.TryCatchBlock)e);
			} else if(e instanceof Stmt.Block) {
				return doBlock((Stmt.Block)e);
			} else if(e instanceof Stmt.Label) {
				return doLabel((Stmt.Label) e);
			} else if(e instanceof Stmt.If) {
				return doIf((Stmt.If) e);
			} else if(e instanceof Stmt.For) {
				return doFor((Stmt.For) e);
			} else if(e instanceof Stmt.ForEach) {
				return doForEach((Stmt.ForEach) e);
			} else if(e instanceof Stmt.While) {
				return doWhile((Stmt.While) e);
			} else if(e instanceof Stmt.DoWhile) {
				return doDoWhile((Stmt.DoWhile) e);
			} else if(e instanceof Stmt.Switch) {
				return doSwitch((Stmt.Switch) e);
			} else if(e instanceof Decl.JavaClass) {				
				return doClass((Decl.JavaClass)e);
			} else {
				return new ArrayList<JilClass>();
			}
		} catch(Exception ex) {
			internal_error(e,ex);
			return null; // dead-code
		}
	}
	
	protected List<JilClass> doBlock(Stmt.Block block) {
		ArrayList<JilClass> classes = new ArrayList<JilClass>();
		if(block != null) {			
			// now process every statement in this block.
			for(Stmt s : block.statements()) {
				classes.addAll(doStatement(s));
			}			
		}		
		return classes;
	}
	
	protected List<JilClass> doSynchronisedBlock(Stmt.SynchronisedBlock block) {
		return doBlock(block);		
	}
	
	protected List<JilClass> doTryCatchBlock(Stmt.TryCatchBlock block) throws ClassNotFoundException {		
		List<JilClass> classes = doBlock(block);		
		classes.addAll(doBlock(block.finaly()));		
		
		for(Stmt.CatchBlock cb : block.handlers()) {			
			classes.addAll(doBlock(cb));			
		}
		return classes;
	}
			
	protected List<JilClass> doLabel(Stmt.Label lab) {						
		return doStatement(lab.statement());
	}
	
	protected List<JilClass> doIf(Stmt.If stmt) {
		List<JilClass> classes = doStatement(stmt.trueStatement());
		classes.addAll(doStatement(stmt.falseStatement()));
		return classes;
	}
	
	protected List<JilClass> doWhile(Stmt.While stmt) {
		return doStatement(stmt.body());		
	}
	
	protected List<JilClass> doDoWhile(Stmt.DoWhile stmt) {
		return doStatement(stmt.body());
	}
	
	protected List<JilClass> doFor(Stmt.For stmt) {
		return doStatement(stmt.body());	
	}
	
	protected List<JilClass> doForEach(Stmt.ForEach stmt) throws ClassNotFoundException {
		return doStatement(stmt.body());
	}
	
	protected List<JilClass> doSwitch(Stmt.Switch sw) {
		ArrayList<JilClass> classes = new ArrayList<JilClass>();
		for(Case c : sw.cases()) {
			for(Stmt s : c.statements()) {
				classes.addAll(doStatement(s));
			}
		}
		return classes;
	}
	
	public ClassScope getEnclosingClassScope() {
		if (scopes.isEmpty()) {
			return null;
		} else {
			for (int i = scopes.size() - 1; i >= 0; --i) {
				Scope s = scopes.get(i);
				if (s instanceof ClassScope) {
					return (ClassScope) s;
				}
			}
		}
		return null;
	}
	
	public MethodScope getEnclosingMethodScope() {
		if (scopes.isEmpty()) {
			return null;
		} else {
			for (int i = scopes.size() - 1; i >= 0; --i) {
				Scope s = scopes.get(i);
				if (s instanceof ClassScope) {
					return null;
				} else if(s instanceof MethodScope) {
					return (MethodScope) s;
				}
			}
		}
		return null;
	}
}
