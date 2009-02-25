package jkit.java.stages;

import java.util.ArrayList;
import java.util.List;

import jkit.compiler.ClassLoader;
import jkit.compiler.SyntaxError;
import jkit.java.io.JavaFile;
import jkit.java.tree.Decl;
import jkit.java.tree.Decl.Field;
import jkit.jil.Clazz;
import jkit.jil.SourceLocation;
import jkit.jil.SyntacticElement;
import jkit.jil.Type;
import jkit.util.Pair;

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
	public List<Clazz> apply(JavaFile file, ClassLoader loader) {
		List<Clazz> skeletons = new ArrayList<Clazz>();
		// Now, traverse the declarations
		for(Decl d : file.declarations()) {
			skeletons.addAll(doDeclaration(d,file.pkg(),null));
		}
		loader.register(skeletons);
		return skeletons;
	}
	
	protected List<Clazz> doDeclaration(Decl d, String pkg, Type.Clazz parent) {		
		if(d instanceof Decl.Interface) {
			return doInterface((Decl.Interface)d,pkg,parent);
		} else if(d instanceof Decl.Enum) {
			return doEnum((Decl.Enum)d,pkg,parent);
		} else if(d instanceof Decl.Clazz) {
			return doClass((Decl.Clazz)d,pkg,parent);
		} else if(d instanceof Decl.Method) {
			return doMethod((Decl.Method)d,pkg,parent);
		} else if(d instanceof Decl.Field) {
			return doField((Field)d,pkg,parent);
		} else if(d instanceof Decl.InitialiserBlock) {
			return doInitialiserBlock((Decl.InitialiserBlock)d,pkg,parent);
		} else if(d instanceof Decl.StaticInitialiserBlock) {
			return doStaticInitialiserBlock((Decl.StaticInitialiserBlock)d,pkg,parent);
		} else {
			syntax_error("internal failure (unknown declaration \"" + d
					+ "\" encountered)",d);
			return null; // dead code.
		}
	}
	
	protected List<Clazz> doEnum(Decl.Enum d, String pkg, Type.Clazz parent) {
		return doClass(d,pkg,parent);
	}
	
	protected List<Clazz> doInterface(Decl.Interface d, String pkg,
			Type.Clazz parent) {
		List<Clazz> cs = doClass(d, pkg, parent);
		cs.get(cs.size() - 1).modifiers()
				.add(
						new jkit.jil.Modifier.Base(
								java.lang.reflect.Modifier.INTERFACE));
		return cs;
	}
	
	protected List<Clazz> doClass(Decl.Clazz c, String pkg, Type.Clazz parent) {
		ArrayList<Clazz> skeletons = new ArrayList<Clazz>();
		// At this stage, type resolution has not already occurred and,
		// hence, we have only basic (i.e. non-generic) type information
		// available.
		List<Pair<String,List<Type.Reference>>> components = new ArrayList();
		if(parent != null) {
			for (Pair<String, List<Type.Reference>> i : parent.components()) {
				components.add(i);
			}
		}
		components.add(new Pair(c.name(), new ArrayList()));
		Type.Clazz type = new Type.Clazz(pkg,components);
		
		for(Decl d : c.declarations()) {			
			skeletons.addAll(doDeclaration(d,pkg,type));
		}				
				
		/**
		 * Now, construct the skeleton for this class! 
		 */
		skeletons.add(new Clazz(type, c.modifiers(), null, new ArrayList(),
				new ArrayList(), new ArrayList()));
						
		return skeletons;
	}

	protected List<Clazz> doMethod(Decl.Method d, String pkg, Type.Clazz parent) {
		return new ArrayList();
	}

	protected List<Clazz> doField(Decl.Field d, String pkg, Type.Clazz parent) {
		return new ArrayList();
	}
	
	protected List<Clazz> doInitialiserBlock(Decl.InitialiserBlock d,
			String pkg, Type.Clazz parent) {		
		return new ArrayList<Clazz>();
	}
	
	protected List<Clazz> doStaticInitialiserBlock(
			Decl.StaticInitialiserBlock d, String pkg, Type.Clazz parent) {		
		return new ArrayList<Clazz>();
	}
	
	
	/**
     * This method is just to factor out the code for looking up the source
     * location and throwing an exception based on that.
     * 
     * @param msg --- the error message
     * @param e --- the syntactic element causing the error
     */
	protected void syntax_error(String msg, SyntacticElement e) {
		SourceLocation loc = (SourceLocation) e.attribute(SourceLocation.class);
		throw new SyntaxError(msg,loc.line(),loc.column());
	}
}
