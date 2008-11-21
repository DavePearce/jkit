package jkit.java;

import java.lang.reflect.Modifier;
import java.util.*;
import jkit.jkil.SourceLocation;

public class JavaFile {
	private String pkg;
	private List<String> imports;
	private List<Clazz> classes; 
	
	public JavaFile(String pkg, List<String> imports, List<Clazz> classes) {
		this.pkg = pkg;
		this.imports = imports;
		this.classes = classes;
	}
	
	/**
	 * Get the package declared at the beginning of this file (if there is one)
	 */			
	public String pkg() { 
		return pkg;
	}
	
	/**
	 * Get the list of import declarations at the beginning of this file.
	 * 
	 * @return
	 */
	public List<String> imports() { 
		return imports;
	}
	
	/**
	 * Get the list of class declarations in this file.
	 * 
	 * @return
	 */
	public List<Clazz> classes() { 
		return classes;
	}
	
	
	/**
     * Represents the class of imperative statements allowed.
     * 
     * @author djp
     * 
     */		
	public static abstract class Statement {
		public final SourceLocation location;
		public Statement(SourceLocation location) {
			this.location = location;
		}
	}
		
	/**
     * Represents all expressions in the code
     * 
     * @author djp
     * 
     */	
	public static abstract class Expression {
		public final SourceLocation location;
		public Expression(SourceLocation location) {
			this.location = location;
		}
	}

	// ====================================================
	// Type
	// ====================================================

	public static class Type {
		private int dims;
		private List<String> components;
		public Type(List<String> components, int dims) {
			this.components = components;
			this.dims = dims;
		}
		public int dims() {
			return dims;
		}
		public void setDims(int dims) {
			this.dims = dims;
		}
		public List<String> components() {
			return components;
		}
		public void setComponents(List<String> components) {
			this.components = components;
		}
	}
	
	// ====================================================
	// DECLARATIONS
	// ====================================================
	
	public static abstract class Declaration {
		
	}
	
	public static class Clazz extends Declaration {
		private int modifiers;
		private String name;
		private Type superclass;
		private List<Type> interfaces;
		private List<Declaration> declarations;
				
		public Clazz(int modifiers, String name, Type superclass,
				List<Type> interfaces, List<Declaration> declarations) {
			this.modifiers = modifiers;
			this.name = name;
			this.superclass = superclass;
			this.interfaces = interfaces;
			this.declarations = declarations;
		}
		
		public int modifiers() {
			return modifiers;
		}
		
		public String name() {
			return name;
		}

		public Type superclass() {
			return superclass;
		}
		
		public List<Type> interfaces() {
			return interfaces;
		}
		
		public List<Declaration> declarations() { 
			return declarations;
		}
		
		
		/**
		 * Check whether this is an interface
		 */
		public boolean isInterface() { return (modifiers&Modifier.INTERFACE)!=0; } 

		/**
		 * Check whether this class or interface is abstract
		 */
		public boolean isAbstract() { return (modifiers&Modifier.ABSTRACT)!=0; }
		
		/**
		 * Check whether this class or interface is final
		 */
		public boolean isFinal() { return (modifiers&Modifier.FINAL)!=0; }
		
		/**
		 * Check whether this class or interface is static
		 */
		public boolean isStatic() { return (modifiers&Modifier.STATIC)!=0; }
		
		/**
		 * Check whether this class or interface is static
		 */
		public boolean isPublic() { return (modifiers&Modifier.PUBLIC)!=0; }
	}
	
	// ====================================================
	// STATEMENTS
	// ====================================================

	public static class Block extends Statement {
		public final List<Statement> statements;
		public Block(List<Statement> statements, SourceLocation location) {
			super(location);
			this.statements = statements; 
		}
	}
	
	public static class Assignment extends Statement {
		public Assignment(SourceLocation location) {
			super(location);
		}
	}
	
	public static class WhileLoop extends Statement {
		public final Expression condition;
		public final Statement body;
		public WhileLoop(Expression condition, Statement body,
				SourceLocation location) {
			super(location);
			this.body = body; 
			this.condition = condition;
		}		
	}
	
	public static class ForLoop extends Statement {
		public final Statement initialiser;
		public final Expression condition;
		public final Statement increment;
		public final Statement body;
		
		
		public ForLoop(Statement initialiser, Expression condition,
				Statement increment, Statement body, SourceLocation location) {
			super(location);
			this.initialiser = initialiser;
			this.condition = condition;
			this.increment = increment;
			this.body = body; 						
		}
	}
		
	public static class ForEachLoop extends Statement {
		public ForEachLoop(SourceLocation location) {
			super(location);
		}
	}
	
	
	// ====================================================
	// EXPRESSIONS
	// ====================================================

}
