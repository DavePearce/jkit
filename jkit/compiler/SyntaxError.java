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
// (C) David James Pearce, 2007. 

package jkit.compiler;

/**
 * This exception is thrown when a syntax error occurs in the parser. 
 * 
 * @author djp
 * 
 */
public class SyntaxError extends RuntimeException {
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
		this.filename = "unknown";
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
		this.filename = "unknown";
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
}
