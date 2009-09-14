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

package jkit.compiler;

import jkit.jil.tree.SourceLocation;

/**
 * This exception is thrown when a syntax error occurs in the parser. 
 * 
 * @author djp
 * 
 */
public final class SyntaxError extends RuntimeException {
	private final String msg;
	private final String filename;
	private final int line;
	private final int column;	
	private final int width;
	
	/**
	 * Identify a syntax error at a particular point in a file.
	 * 
	 * @param msg
	 *            Message detailing the problem.
	 * @param line
	 *            Line number within file containing problem.
	 * @param column
	 *            Column within line of file containing problem.
	 */
	public SyntaxError(String msg, int line, int column) {
		this.msg = msg;
		this.filename = null;
		this.line=line;		
		this.column=column;
		this.width=1;
	}
	
	/**
	 * Identify a syntax error at a particular point in a file.
	 * 
	 * @param msg
	 *            Message detailing the problem.
	 * @param line
	 *            Line number within file containing problem.
	 * @param column
	 *            Column within line of file containing problem.
	 */
	public SyntaxError(String msg, int line, int column, Throwable ex) {
		super(ex);
		this.msg = msg;
		this.filename = null;
		this.line=line;		
		this.column=column;
		this.width=1;
	}
	/**
	 * Identify a syntax error at a particular point in a file.
	 * 
	 * @param msg
	 *            Message detailing the problem.
	 * @param line
	 *            Line number within file containing problem.
	 * @param column
	 *            Column within line of file containing problem.
	 * @param width
	 *            Width of token within line of file containing problem.
	 */
	public SyntaxError(String msg, int line, int column, int width) {
		this.msg = msg;
		this.filename = null;
		this.line=line;		
		this.column=column;
		this.width=width;
	}
	
	/**
	 * Identify a syntax error at a particular point in a file.
	 * 
	 * @param msg
	 *            Message detailing the problem.
	 * @param filename
	 *            Filename of file containing the problem.
	 * @param line
	 *            Line number within file containing problem.
	 * @param column
	 *            Column within line of file containing problem.
	 */
	public SyntaxError(String msg, String filename, int line, int column) {
		this.msg = msg;
		this.filename = filename;
		this.line=line;		
		this.column=column;
		this.width=1;
	}
	
	/**
	 * Identify a syntax error at a particular point in a file.
	 * 
	 * @param msg
	 *            Message detailing the problem.
	 * @param filename
	 *            Filename of file containing the problem.
	 * @param line
	 *            Line number within file containing problem.
	 * @param column
	 *            Column within line of file containing problem.
	 * @param width
	 *            Width of token within line of file containing problem.
	 * @param e
	 *            Exception which generated this exception (in the case of a rethrow).
	 */
	public SyntaxError(String msg, String filename, int line, int column, int width, Throwable e) {
		super(e);
		this.msg = msg;
		this.filename = filename;
		this.line=line;		
		this.column=column;
		this.width=width;
	}	
	
	/**
	 * Identify a syntax error at a particular point in a file.
	 * 
	 * @param msg
	 *            Message detailing the problem.
	 * @param filename
	 *            Filename of file containing the problem.
	 * @param line
	 *            Line number within file containing problem.
	 * @param column
	 *            Column within line of file containing problem.
	 * @param width
	 *            Width of token within line of file containing problem.
	 */
	public SyntaxError(String msg, String filename, int line, int column, int width) {
		this.msg = msg;
		this.filename = filename;
		this.line=line;		
		this.column=column;
		this.width=width;
	}	
	
	public String getMessage() {
		if(msg != null) {
			return msg;
		} else {
			return "";
		}
	}
	
	/**
	 * Error message
	 * @return
	 */
	public String msg() { return msg; }
	
	/**
	 * Get name of file containing syntax error.
	 * @return
	 */
	public String fileName() { return filename; }
	
	/**
	 * Get line number of syntax error
	 * @return
	 */
	public int line() { return line; }	
	
	/**
	 * Get the column that the syntax error starts on
	 * @return
	 */
	public int column() { return column; }
	
	/**
	 * Get width of token causing syntax error
	 * @return
	 */
	public int width() { return width; }
	
	public static final long serialVersionUID = 1l;
	
	/**
     * This method is just to factor out the code for looking up the source
     * location and throwing an exception based on that.
     * 
     * @param msg --- the error message
     * @param e --- the syntactic element causing the error
     */
	public static void syntax_error(String msg, SyntacticElement e) {
		SourceLocation loc = (SourceLocation) e.attribute(SourceLocation.class);
		if(loc != null) {
			throw new SyntaxError(msg,loc.line(),loc.column());
		} else {
			throw new SyntaxError(msg,-1,-1);
		}
	}
	
	/**
	 * This method is just to factor out the code for looking up the source
	 * location and throwing an exception based on that. In this case, we also
	 * have an internal exception which has given rise to this particular
	 * problem.
	 *
	 * @param e
	 *            --- the syntactic element causing the error
	 * @parem ex --- an internal exception, the details of which we want to
	 *        keep.
	 */
	public static void internal_error(SyntacticElement e, Throwable ex) {
		
		SourceLocation loc = (SourceLocation) e.attribute(SourceLocation.class);
		int line = -1;
		int column = -1;
		
		if(loc != null) {
			line = loc.line();
			column = loc.column();
		}
		
		if (ex instanceof SyntaxError) {
			// in the special case that the cause of this exception was already
			// a syntax error, then we simply rethrow it; otherwise, we'll
			// attribute the wrong error message and line number.
			SyntaxError se = (SyntaxError) ex;
			throw se;
		} else if (ex instanceof ClassNotFoundException) {
			throw new SyntaxError("class not found (" + ex.getMessage() + ")",
					line, column, ex);
		} else if (ex instanceof MethodNotFoundException) {
			throw new SyntaxError("method not found (" + ex.getMessage() + ")",
					line, column, ex);
		} else if (ex instanceof FieldNotFoundException) {
			throw new SyntaxError("field not found (" + ex.getMessage() + ")",
					line, column, ex);
		} 			
				
		throw new SyntaxError("internal failure (" + ex.getMessage() + ")",line,column,ex);
				
	}		
	
	/**
	 * This method is just to factor out the code for looking up the source
	 * location .
	 * 
	 * @param msg
	 *            --- the error message
	 * @param e
	 *            --- the syntactic element causing the error
	 */
	public static void internal_error(String msg, SyntacticElement e) {
		
		SourceLocation loc = (SourceLocation) e.attribute(SourceLocation.class);
		int line = -1;
		int column = -1;
		
		if(loc != null) {
			line = loc.line();
			column = loc.column();
		}		
				
		throw new SyntaxError("internal failure (" + msg + ")",line,column);
				
	}		
}
