package org.simplelisp.interpreter;

public class SyntaxError extends Error {
	private int _line;
	private int _column;
	public SyntaxError(int line, int col, String s) {
		super(s);
		_line=line;
		_column=col;
	}
	public int getLine() { return _line; }
	public int getColumn() { return _column; }
}
