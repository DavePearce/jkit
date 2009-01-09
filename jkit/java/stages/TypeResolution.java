package jkit.java.stages;

import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.compiler.SyntaxError;
import jkit.java.Decl;
import jkit.java.JavaFile;
import jkit.java.Decl.Clazz;
import jkit.java.Decl.Field;
import jkit.java.Decl.Interface;
import jkit.java.Decl.Method;
import jkit.jil.Modifier;
import jkit.jil.SourceLocation;
import jkit.jil.SyntacticElement;
import jkit.jil.Type;
import jkit.util.*;

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
 *    ... 
 *   }
 * }
 * </pre>
 * 
 * After type resolution, this code will look like the following:
 * 
 * <pre>
 * public class Test extends java.util.Vector {
 * 	public static void main(java.lang.String[] args) {
 *    ... 
 *   }
 * }
 * </pre>
 * 
 * Thus, we can see that the import statements are no longer required since the
 * full package for everytype has been provided.
 * 
 * @author djp
 * 
 */
public class TypeResolution {
	private ClassLoader loader;
	
	public TypeResolution(ClassLoader loader) {
		this.loader = loader; 
	}
	
	public void apply(JavaFile file) {
		for(Decl d : file.declarations()) {
			doDeclaration(d, file);
		}
	}
	
	protected void doDeclaration(Decl d, JavaFile file) {
		if(d instanceof Interface) {
			doInterface((Interface)d, file);
		} else if(d instanceof Clazz) {
			doClass((Clazz)d, file);
		} else if(d instanceof Method) {
			doMethod((Method)d, file);
		} else if(d instanceof Field) {
			doField((Field)d, file);
		}
	}
	
	protected void doInterface(Interface d, JavaFile file) {
		
	}
	
	protected void doClass(Clazz c, JavaFile file) {		
		resolve(c.superclass(),file);
		
		for(Decl d : c.declarations()) {
			doDeclaration(d,file);
		}
	}

	protected void doMethod(Method d, JavaFile file) {
		// First, we need to construct a typing environment for local variables.
		HashMap<String,Type> environment = new HashMap<String,Type>();
		for(Triple<String,List<Modifier>,Type> p : d.parameters()) {
			environment.put(p.first(), p.third());
		}
						
		// doStatement(d.body(),environment);
	}
	
	protected void doField(Field d, JavaFile file) {
		// doExpression(d.initialiser(), new HashMap<String,Type>());
	}
	
	protected Type.Reference resolve(Type.Reference t, JavaFile file) {
		if(t instanceof Type.Clazz) {
			resolveClass((Type.Clazz)t,file);			
		} else if(t instanceof Type.Array) {
			
		}
		
		return t;
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
	protected Type.Reference resolveClass(Type.Clazz ct, JavaFile file) {
		ArrayList<Pair<String,List<Type.Reference>>> ncomponents = new ArrayList();
		String className = "";
		String pkg = "";
				
		boolean firstTime = true;
		for(int i=0;i!=ct.components().size();++i) {
			String tmp = ct.components().get(i).first();
			String tmppkg = pkg.equals("") ? tmp : pkg + "." + tmp;
			if(firstTime && loader.isPackage(tmppkg))  {
				pkg = tmppkg;
			} else {
				if(!firstTime) {
					className += "$";
				}
				firstTime = false;
				className += ct.components().get(i).first();
				ncomponents.add(ct.components().get(i));
			}
		}
		
		// now, some sanity checking.
		if(className.equals("")) {
			throw new SyntaxError("unable to find class " + pkg,0,0);
		} else if(pkg.length() > 0) {
			// could add "containsClass" check here. Need to modify
			// classLoader though.
			return new Type.Clazz(pkg,ncomponents);			
		}
		
		// So, at this point, it seems there was no package information in the
		// source code and, hence, we need to determine this fromt he CLASSPATH
		// and the import list.
		
		// the following may cause problems with static imports.
		ArrayList<String> imports = new ArrayList<String>();
		for(Pair<Boolean,String> i : file.imports()) {
			imports.add(i.second());
		}		
				
		try {			
			return loader.resolve(className, imports);			
		} catch(ClassNotFoundException e) {}

		throw new SyntaxError("unable to find class " + className,0,0);
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
