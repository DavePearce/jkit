package jkit.java;

import java.util.*;
import jkit.util.*;

public class JavaFile {
	private String pkg;
	private List<Pair<Boolean,String> > imports;
	private List<Decl> declarations; 
	
	public JavaFile(String pkg, List<Pair<Boolean, String> > imports, List<Decl> declarations) {
		this.pkg = pkg;
		this.imports = imports;
		this.declarations = declarations;
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
	public List<Pair<Boolean,String> > imports() { 
		return imports;
	}
	
	/**
	 * Get the list of class declarations in this file.
	 * 
	 * @return
	 */
	public List<Decl> declarations() { 
		return declarations;
	}	
		
	// ====================================================
	// MODIFIERS
	// ====================================================
	
	// ====================================================
	// DECLARATIONS
	// ====================================================
	
	
}
