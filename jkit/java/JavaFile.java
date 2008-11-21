package jkit.java;

import java.util.*;

public class JavaFile {
	private String pkg;
	private List<String> imports;
	private List<ClassDecl> classes; 
	
	public JavaFile(String pkg, List<String> imports, List<ClassDecl> classes) {
		this.pkg = pkg;
		this.imports = imports;
		this.classes = classes;
	}
		
	/**
     * Represents the class of imperative statements allowed.
     * 
     * @author djp
     * 
     */		
	public static interface Statement {}
		
	/**
     * Represents all expressions in the code
     * 
     * @author djp
     * 
     */	
	public static interface Expression {}

	// ====================================================
	// DECLARATIONS
	// ====================================================
	
	public static class ClassDecl {
		public final String name;
				
		public ClassDecl(String name) {
			this.name = name;
		}
	}
	
	// ====================================================
	// STATEMENTS
	// ====================================================

	public static class Block implements Statement {
		public final List<Statement> statements;
		public Block(List<Statement> statements) { this.statements = statements; }
	}
	
	public static class Assignment implements Statement {}
	
	public static class WhileLoop implements Statement {
		public final Expression condition;
		public final Statement body;
		public WhileLoop(Expression condition, Statement body) { 
			this.body = body; 
			this.condition = condition;
		}		
	}
	
	public static class ForLoop implements Statement {
		public final Statement initialiser;
		public final Expression condition;
		public final Statement increment;
		public final Statement body;
		
		
		public ForLoop(Statement initialiser, Expression condition,
				Statement increment, Statement body) { 
			this.initialiser = initialiser;
			this.condition = condition;
			this.increment = increment;
			this.body = body; 						
		}
	}
		
	public static class ForEachLoop implements Statement {}
	
	
	// ====================================================
	// EXPRESSIONS
	// ====================================================

}
