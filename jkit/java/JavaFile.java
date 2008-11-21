package jkit.java;

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
	 * Get the package declared at the beginning of this class (if there is one)
	 */			
	public String pkg() { 
		return pkg;
	}
	
	/**
	 * Get the list of import declarations at the beginning of this class.
	 * 
	 * @return
	 */
	public List<String> imports() { 
		return imports;
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
	// DECLARATIONS
	// ====================================================
	
	public static class Clazz {
		public final String name;
				
		public Clazz(String name) {
			this.name = name;
		}
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
