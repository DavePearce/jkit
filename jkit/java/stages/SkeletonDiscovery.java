package jkit.java.stages;

import java.util.ArrayList;
import java.util.List;

import jkit.compiler.ClassLoader;
import static jkit.compiler.SyntaxError.*;
import jkit.java.io.JavaFile;
import jkit.java.tree.Decl;
import jkit.java.tree.Decl.JavaField;
import jkit.jil.tree.JilClass;
import jkit.jil.tree.SourceLocation;
import jkit.jil.tree.SyntacticElement;
import jkit.jil.tree.Type;
import jkit.jil.tree.Modifier;
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
	public List<JilClass> apply(JavaFile file, ClassLoader loader) {
		List<JilClass> skeletons = new ArrayList<JilClass>();
		// Now, traverse the declarations
		for(Decl d : file.declarations()) {
			skeletons.addAll(doDeclaration(d,file.pkg(),null));
		}
		loader.register(skeletons);
		return skeletons;
	}
	
	protected List<JilClass> doDeclaration(Decl d, String pkg, Type.Clazz parent) {				
		if(d instanceof Decl.JavaInterface) {
			return doInterface((Decl.JavaInterface)d,pkg,parent);
		} else if(d instanceof Decl.JavaEnum) {
			return doEnum((Decl.JavaEnum)d,pkg,parent);
		} else if(d instanceof Decl.JavaClass) {
			return doClass((Decl.JavaClass)d,pkg,parent);
		} else if(d instanceof Decl.JavaMethod) {
			return doMethod((Decl.JavaMethod)d,pkg,parent);
		} else if(d instanceof Decl.JavaField) {
			return doField((JavaField)d,pkg,parent);
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
	
	protected List<JilClass> doEnum(Decl.JavaEnum d, String pkg, Type.Clazz parent) {
		List<JilClass> cs = doClass(d, pkg, parent);
		cs.get(cs.size() - 1).modifiers()
				.add(Modifier.ACC_STATIC);
		return cs;		
	}
	
	protected List<JilClass> doInterface(Decl.JavaInterface d, String pkg,
			Type.Clazz parent) {
		List<JilClass> cs = doClass(d, pkg, parent);
		cs.get(cs.size() - 1).modifiers()
				.add(Modifier.ACC_INTERFACE);
		return cs;
	}
	
	protected List<JilClass> doClass(Decl.JavaClass c, String pkg, Type.Clazz parent) {
		ArrayList<JilClass> skeletons = new ArrayList<JilClass>();
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
		 * Construct inner classes list.
		 */
		ArrayList<Type.Clazz> inners = new ArrayList<Type.Clazz>();
		for(JilClass jc : skeletons) {
			inners.add(jc.type());
		}
		
		/**
		 * Now, construct the skeleton for this class!
		 */
		skeletons.add(new JilClass(type, c.modifiers(), null, new ArrayList(),
				inners, new ArrayList(), new ArrayList()));
						
		return skeletons;
	}

	protected List<JilClass> doMethod(Decl.JavaMethod d, String pkg, Type.Clazz parent) {
		return new ArrayList();
	}

	protected List<JilClass> doField(Decl.JavaField d, String pkg, Type.Clazz parent) {
		return new ArrayList();
	}
	
	protected List<JilClass> doInitialiserBlock(Decl.InitialiserBlock d,
			String pkg, Type.Clazz parent) {		
		return new ArrayList<JilClass>();
	}
	
	protected List<JilClass> doStaticInitialiserBlock(
			Decl.StaticInitialiserBlock d, String pkg, Type.Clazz parent) {		
		return new ArrayList<JilClass>();
	}	
}
