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

package jkit.java.io;

import java.util.*;

import jkit.java.tree.Decl;
import jkit.util.*;

public class JavaFile {
	private String filename;
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
	
	public String filename() {
		return filename; 
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	// ====================================================
	// MODIFIERS
	// ====================================================
	
	// ====================================================
	// DECLARATIONS
	// ====================================================
	
	
}
