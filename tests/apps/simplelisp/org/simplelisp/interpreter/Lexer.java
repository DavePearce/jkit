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

public class Lexer {
	
	// This method is a good example of why you should use comments!
	public static List<Token> tokenise(String text, boolean whitespace) {
		int pos=0;
		int linestart=0;
		int line=0;
		
		ArrayList<Token> r = new ArrayList<Token>();
		
		while(pos < text.length()) {			
			if (text.charAt(pos) == '(') {
				r.add(new LeftBrace(text.substring(pos,pos+1),line,pos-linestart));
				++pos;
			} else if (text.charAt(pos) == ')') {
				r.add(new RightBrace(text.substring(pos,pos+1),line,pos-linestart));
				++pos;
			} else if (text.charAt(pos) == '\'') {
				r.add(new Quote(text.substring(pos,pos+1),line,pos-linestart));
				++pos;
			} else if(text.charAt(pos) == '\n') {
				int start=pos;
				++pos;				
				if(pos < text.length() && text.charAt(pos) == '\r') {
					++pos;
				}
				if(whitespace) {
					r.add(new NewLine(text.substring(start,start+1),line,pos-linestart));
				}
				++line;
				linestart=pos;
			} else if(text.charAt(pos) == '\r') {
				int start=pos;
				++pos;				
				if(pos < text.length() && text.charAt(pos) == '\n') {
					++pos;
				}
				if(whitespace) {
					r.add(new NewLine(text.substring(start,start+1),line,pos-linestart));
				}
				++line;
				linestart=pos;
			} else if(text.charAt(pos) == '"') {
				// strings are a bit tricky!			
				char c;
				StringBuffer buf = new StringBuffer("\"");
				boolean escaped = false;
				pos++;
				// escaped is needed so we don't terminate the string
				// prematurely
				while (pos < text.length()
						&& ((c = text.charAt(pos)) != '"' || escaped)) {
					escaped=false;
					if (c == '\\') { escaped = true; }
					buf.append(c);					
					++pos;					
				}
				buf.append('"');
				++pos;
				r.add(new Strung(buf.toString(),line,pos-linestart));
			} else if(text.charAt(pos) == ' ' || text.charAt(pos) == '\t') {
				int start=pos;
				do {
					++pos;
				} while(pos < text.length() && 
						(text.charAt(pos) == ' ' || text.charAt(pos) == '\t'));
				
				if(whitespace) {
					r.add(new Gap(text.substring(start,pos),line,start-linestart));
				}
			} else if(Character.isDigit(text.charAt(pos))) {
				int start=pos;
				do {
					++pos;
				} while (pos < text.length()
						&& !Character.isWhitespace(text.charAt(pos))
						&& Character.isDigit(text.charAt(pos)));
				
				r.add(new Integer(text.substring(start,pos),line,start-linestart));
			} else if(isIdentifierChar(text.charAt(pos))){
				// must be identifier				
				int start=pos;
				do {
					++pos;
				} while (pos < text.length()
						&& !Character.isWhitespace(text.charAt(pos))
						&& isIdentifierChar(text.charAt(pos)));
				
				r.add(new Identifier(text.substring(start,pos),line,start-linestart));
			} else if(text.charAt(pos) == ';') {
				// must be comment				
				int start=pos;
				do {
					++pos;
				} while (pos < text.length()
						&& text.charAt(pos) != '\n' && text.charAt(pos) != '\r');
				if(whitespace) {
					r.add(new Comment(text.substring(start,pos),line,start-linestart));
				}
			} else if(text.charAt(pos) == '#' && (pos+1) < text.length() && text.charAt(pos+1) == '\\') {
				int start=pos;
				pos=pos+2;
				do {
					++pos;
				} while (pos < text.length()
						&& !Character.isWhitespace(text.charAt(pos))
						&& Character.isLetter(text.charAt(pos)));
				r.add(new Char(text.substring(start,pos),line,start-linestart));
			} else {		
				r.add(new Unknown(text.substring(pos,pos+1),line,pos-linestart));
				pos++;
			}
		}
		return r;							 
	}
	
	public static boolean isIdentifierChar(char c) {
		return Character.isLetterOrDigit(c) 
			|| c == '_' || c == '%'
			|| c == '<' || c == '=' || c == '>'
			|| c == '+' || c == '-' || c == '*' || c == '/';
	}
	
	public static abstract class Token {		
		private String _str;
		private int _line;
		private int _column;
		
		public Token(String str, int line, int column) {
			_str = str;
			_line=line;
			_column=column;
		}
		
		public String toString() { return _str; }
		public int getLine() { return _line; }
		public int getColumn() { return _column; }
	}
	
	public static abstract class WhiteSpace extends Token {
		public WhiteSpace(String str, int line, int column) { super(str,line,column); }
	}
	public static class NewLine extends WhiteSpace {
		public NewLine(String str, int line, int column) { super(str,line,column); }
	}
	public static class Comment extends WhiteSpace {
		public Comment(String str, int line, int column) { super(str,line,column); }
	}
	public static class Gap extends WhiteSpace {
		public Gap(String str, int line, int column) { super(str,line,column); }
	}	
	public static class LeftBrace extends Token {
		public LeftBrace(String str, int line, int column) { super(str,line,column); }
	}
	public static class RightBrace extends Token {
		public RightBrace(String str, int line, int column) { super(str,line,column); }
	}
	public static class Quote extends Token {
		public Quote(String str, int line, int column) { super(str,line,column); }
	}
	public static class Comma extends Token {
		public Comma(String str, int line, int column) { super(str,line,column); }
	}
	public static class Strung extends Token {
		public Strung(String str, int line, int column) { super(str,line,column); }
	}
	public static class Identifier extends Token {
		public Identifier(String str, int line, int column) { super(str,line,column); }
	}
	public static class Integer extends Token {
		public Integer(String str, int line, int column) { super(str,line,column); }
	}
	public static class Char extends Token {
		public Char(String str, int line, int column) { super(str,line,column); }
	}
	public static class Unknown extends Token {
		public Unknown(String str, int line, int column) { super(str,line,column); }
	}
}
