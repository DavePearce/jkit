// This file is part of the Simple Lisp Interpreter.
//
// The Simple Lisp Interpreter is free software; you can 
// redistribute it and/or modify it under the terms of the 
// GNU General Public License as published by the Free Software 
// Foundation; either version 2 of the License, or (at your 
// option) any later version.
//
// The Simpular Program Interpreter is distributed in the hope
// that it will be useful, but WITHOUT ANY WARRANTY; without 
// even the implied warranty of MERCHANTABILITY or FITNESS FOR 
// A PARTICULAR PURPOSE.  See the GNU General Public License 
// for more details.
//
// You should have received a copy of the GNU General Public 
// License along with the Simpular Program Interpreter; if not, 
// write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA
//
// (C) David James Pearce, 2005. 

package org.simplelisp.interpreter;

import java.util.*;
import java.io.*;

public class Interpreter {
	private HashMap<String,LispExpr> globals = new HashMap<String,LispExpr>();    	
	
	public Interpreter() { InternalFunctions.setup_internals(this); }
	
	public LispExpr evaluate(LispExpr e) {
		return e.evaluate(new HashMap<String,LispExpr>(),globals);
	}        
	
	public void type_check(String fn, LispExpr[] es, Class ... cs) {	
		for(int i=0;i!=cs.length;++i) {
			if(es.length <= i) { error(es[0],"insufficient parameters!"); }
			if(!cs[i].isInstance(es[i])) { error(es[i],"type error in \"" + fn + "\""); }
		}
	}
	
	public void error(LispExpr e, String msg) { 
		throw new Error(msg); 
	} 
	
	public void setGlobalExpr(String name, LispExpr e) {
		globals.put(name, e);
	}
	
	public LispExpr getGlobalExpr(String name) {
		LispExpr r = globals.get(name);
		if(r == null) { return new LispNil(); }
		else return r;
	}
	
	public void load(String filename) throws FileNotFoundException {
		// attempt to load file.
		File file = new File(filename); // will need to be changes to URLReader thing
		try {
			BufferedReader input = new BufferedReader(
					new InputStreamReader(
							new FileInputStream(file)));
			
			StringBuffer text = new StringBuffer();
			while (input.ready()) {
				text.append(input.readLine());
				text.append("\n");
			}
			// Ok, successful load;  run the library code!
			try {	       
				LispExpr root = Parser.parse(text.toString());
				evaluate(root);
			} catch(Error e) {
				// unrecoverable syntax error.
				System.err.println(e.getMessage());
				System.exit(1);
			}
		} catch(FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			// This happens if e.g. file already exists and
			// we do not have write permissions
			System.err.println("Unable to load file " + file.getName() + ": " + 
					e.getMessage());
			System.exit(1);				
		}
	}
	
	// main method, used to run interpreter without GUI
	public static void main(String[] args) {
		Interpreter interpreter = new Interpreter();
		
		// now either run files on command-line or enter interactive mode...
		if(args.length == 0) {
			// interactive mode
			System.out.println("Simple Lisp Interpreter v1.0");
			System.out.println("Written by David J. Pearce, March 2006");
			System.out.println("");
			BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
			
			try {
				while(true) {
					System.out.print("> ");
					String text = input.readLine();
					try {
						LispExpr root = Parser.parse(text);
						System.out.println(interpreter.evaluate(root));
					} catch(Error e) { 
						System.err.println(e.getMessage());
					}
				}
			} catch(IOException e) {
				System.err.println("I/O Error - " + e.getMessage());
			}
		} else { 
			try {
				interpreter.load(args[0]); 
			} catch(FileNotFoundException e) {
				System.err.println("Unable to load \"" + args[0] + "\" " + e.getMessage());
			}
		}
	}
}
