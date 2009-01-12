package jkit.java.stages;

import java.util.*;

import jkit.*;
import jkit.compiler.ClassLoader;
import jkit.compiler.SyntaxError;
import jkit.java.*;
import jkit.java.Decl.Clazz;
import jkit.java.Decl.Field;
import jkit.java.Decl.Interface;
import jkit.java.Decl.Method;
import jkit.jil.Modifier;
import jkit.jil.SourceLocation;
import jkit.jil.SyntacticElement;
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
 *     ... 
 *    }
 * }
 * </pre>
 * 
 * After type resolution, we will have resolved the following types:
 * 
 * <pre>
 *  Vector -&gt; java.util.Vector
 *  String -&gt; java.lang.String
 * </pre>
 * 
 * Thus, in principle, we could use this information to eliminate any import
 * statements (although type resolution by itself does not do this).
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
		// the following may cause problems with static imports.
		ArrayList<String> imports = new ArrayList<String>();
		for(Pair<Boolean,String> i : file.imports()) {
			imports.add(i.second());
		}	
		
		imports.add(0,"java.lang.*");
		
		for(Decl d : file.declarations()) {
			doDeclaration(d, imports);
		}
	}
	
	protected void doDeclaration(Decl d, List<String> imports) {
		if(d instanceof Interface) {
			doClass((Interface)d, imports);
		} else if(d instanceof Clazz) {
			doClass((Clazz)d, imports);
		} else if(d instanceof Method) {
			doMethod((Method)d, imports);
		} else if(d instanceof Field) {
			doField((Field)d, imports);
		}
	}	
	
	protected void doClass(Clazz c, List<String> imports) {		
		if(c.superclass() != null) {
			c.superclass().attributes().add(resolve(c.superclass(), imports));
		}
		
		for(Type.Variable v : c.typeParameters()) {
			v.attributes().add(resolve(v, imports));
		}
		
		for(Type.Clazz i : c.interfaces()) {
			i.attributes().add(resolve(i, imports));
		}
		
		for(Decl d : c.declarations()) {
			doDeclaration(d, imports);
		}
	}

	protected void doMethod(Method d, List<String> imports) {
		// First, resolve parameter types.		
		for(Triple<String,List<Modifier>,Type> p : d.parameters()) {
			p.third().attributes().add(resolve(p.third(), imports));			
		}
		// Second, resolve return type
		d.returnType().attributes().add(resolve(d.returnType(), imports));
		
		// Third, resolve exceptions
		for(Type.Clazz e : d.exceptions()) {
			e.attributes().add(resolve(e,imports));
		}
		
		// Finally, resolve any types in the method body
		// doStatement(d.body(),environment);
	}
	
	protected void doField(Field d, List<String> imports) {
		d.type().attributes().add(resolve(d.type(),imports));
						
		// doExpression(d.initialiser(), new HashMap<String,Type>());
	}
	
	/**
     * The purpose of the resovle method is to examine the type in question, and
     * determine the fully qualified it represents, based on the current import
     * list. 
     * 
     * @param t
     * @param file
     * @return
     */
	protected jkit.jil.Type resolve(Type t, List<String> imports) {
		if(t instanceof Type.Primitive) {
			return resolve((Type.Primitive)t, imports);
		} else if(t instanceof jkit.java.Type.Clazz) {
			return resolve((Type.Clazz)t, imports);			
		} else if(t instanceof Type.Array) {
			return resolve((Type.Array)t, imports);
		} 
		
		return null;
	}
	
	protected jkit.jil.Type.Primitive resolve(Type.Primitive pt, List<String> imports) {
		if(pt instanceof Type.Void) {
			return new jkit.jil.Type.Void();
		} else if(pt instanceof Type.Bool) {
			return new jkit.jil.Type.Bool();
		} else if(pt instanceof Type.Byte) {
			return new jkit.jil.Type.Byte();
		} else if(pt instanceof Type.Char) {
			return new jkit.jil.Type.Char();
		} else if(pt instanceof Type.Short) {
			return new jkit.jil.Type.Short();
		} else if(pt instanceof Type.Int) {
			return new jkit.jil.Type.Int();
		} else if(pt instanceof Type.Long) {
			return new jkit.jil.Type.Long();
		} else if(pt instanceof Type.Float) {
			return new jkit.jil.Type.Float();
		} else {
			return new jkit.jil.Type.Double();
		}
	}
	
	protected jkit.jil.Type.Array resolve(Type.Array t, List<String> imports) {
		return new jkit.jil.Type.Array(resolve(t.element(), imports));
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
	protected jkit.jil.Type.Reference resolve(Type.Clazz ct, List<String> imports) {
		ArrayList<Pair<String,List<jkit.jil.Type.Reference>>> ncomponents = new ArrayList();
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
				
				// now, rebuild the component list
				Pair<String,List<Type.Reference>> component = ct.components().get(i);
				ArrayList<jkit.jil.Type.Reference> nvars = new ArrayList();
				
				for(Type.Reference r : component.second()) {
					nvars.add((jkit.jil.Type.Reference) resolve(r, imports));
				}
				
				ncomponents.add(new Pair<String,List<jkit.jil.Type.Reference>>(component.first(),nvars));
			}
		}
		
		// now, some sanity checking.
		if(className.equals("")) {
			throw new SyntaxError("unable to find class " + pkg,0,0);
		} else if(pkg.length() > 0) {
			// could add "containsClass" check here. Need to modify
			// classLoader though.
			return new jkit.jil.Type.Clazz(pkg,ncomponents);			
		}
		
		// So, at this point, it seems there was no package information in the
		// source code and, hence, we need to determine this fromt he CLASSPATH
		// and the import list.
									
		try {			
			System.out.println("LOADING: " + className);
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
