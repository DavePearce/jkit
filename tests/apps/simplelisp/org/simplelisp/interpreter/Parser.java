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

// ==============================================
// COMP205: YOU DO NOT NEED TO MODIFY THIS CLASS!
// ==============================================

package org.simplelisp.interpreter;

import java.util.*;

public class Parser {
	private List<Lexer.Token> _tokens; // tokens being parsed
	private int _pos;  // current position in token list		

	Parser(String text) {
		_pos = 0;
		_tokens = Lexer.tokenise(text,false);		
	}

	public static LispExpr parse(String s) {
		Parser p = new Parser(s);
		return p.parseProgram();
	}

	private LispExpr parseProgram() {
		LispExpr t;
		LispList r = new LispList();
		r.add(new LispSymbol("progn"));

		while ((t = parseExpr()) != null) {
			r.add(t);
		}

		return r;
	}

	private LispExpr parseExpr() {
		// Expr ::= list | integer | boolean | string | identifier

		if (_pos >= _tokens.size()) { return null; }

		Lexer.Token lookahead = _tokens.get(_pos);
		if (lookahead instanceof Lexer.Quote) {
			++_pos;
			LispExpr l = new LispQuote(parseExpr());
			return l;
		} else if (lookahead instanceof Lexer.LeftBrace) {
			return parseList();
		} else if (lookahead instanceof Lexer.Strung) {
			return parseString();
		} else if (lookahead instanceof Lexer.Integer) {
			++_pos;
			return new LispInteger(Integer.parseInt(lookahead.toString()));
		} else if (lookahead instanceof Lexer.Char) {
			++_pos;
			String c = lookahead.toString();
			if(c.length() > 3) {
				// special character form
				if(c.equals("#\\Newline")) {
					return new LispChar('\n');
				} else if(c.equals("#\\Tab")) {
					return new LispChar('\t');
				} else {
					throw new SyntaxError(lookahead.getLine(),lookahead.getColumn(),"unrecognised character \"" + lookahead.toString() + "\"");
				}
			} else {
				return new LispChar(c.charAt(2));
			}
		} else if (lookahead instanceof Lexer.Identifier) {
			// catch the special forms
			++_pos;
			String sym=lookahead.toString();
			if(sym.equals("nil")) {
				return new LispNil();
			} else {
				return new LispSymbol(sym);
			}
		} else {
			throw new SyntaxError(lookahead.getLine(),lookahead.getColumn(),"unrecognised token \"" + lookahead.toString() + "\"");
		}
	}

	private LispList parseList() {
		// list ::= '(' Expr* ')' | nil	
		Lexer.Token tok = _tokens.get(_pos++);

		if (_pos >= _tokens.size()) {
			throw new SyntaxError(tok.getLine(),tok.getColumn(),
					" end of file after '('");
		}
		
		LispList l = new LispList();

		while (_pos < _tokens.size() && 
				!(_tokens.get(_pos) instanceof Lexer.RightBrace)) {
			l.add(parseExpr());
		}

		if (_pos >= _tokens.size()) {
			throw new SyntaxError(tok.getLine(),tok.getColumn(),
			" missing ')'");			
		}

		++_pos;
		
		return l;
	}
/*
	private LispChar parseCharacter() {
		match("#\\");
		String t = getToken();
		if (t.length() == 1) {
			_pos++;
			LispChar r = new LispChar(t.charAt(0));
			return r;
		} else {
			// this could definitely be improved
			if (t.equals("Newline")) {
				match("Newline");
				LispChar r = new LispChar('\n');
				return r;
			} else if (t.equals("Tab")) {
				match("Tab");
				LispChar r = new LispChar('\t');
				return r;
			}
		}

		throw new Error("unrecognised special character \"" + t + "\"");
	}
*/

	private LispString parseString() {
		// a string consists of any sequence of characters
		// terminated by a '"'.				
		StringBuffer buf = new StringBuffer();
		boolean escaped = false;	
		Lexer.Token tok = _tokens.get(_pos++);
		String str = tok.toString();				
		int p=1;
		while (p < (str.length()-1) || escaped) {
			// deal with escape codes here
			char c = str.charAt(p);
			if (escaped) {
				if (c == 'n') {
					// new line 
					buf.append('\n');
				} else if (c == 't') {
					// tab
					buf.append('\t');
				} else if (c == '"') {
					// inverted comma
					buf.append('\"');
				} else if (c == '\\') {
					// back slash
					buf.append('\\');
				} else if (c == 'r') {
					// carriage return
					buf.append('\r');
				} else {
					// don't recognise this escape character
					throw new SyntaxError(tok.getLine(),tok.getColumn(),": invalid escape sequence \\" + c);
				}
				escaped = false;
				++p;
			} else {
				if (c == '\\') {
					escaped = true;
				} else {
					buf.append(c);
				}
				++p;
			}
		}		
		return new LispString(buf.toString());
	}
}

