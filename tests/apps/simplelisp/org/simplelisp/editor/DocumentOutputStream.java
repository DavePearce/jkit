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

package org.simplelisp.editor;

import java.io.*;
import javax.swing.text.*;

public class DocumentOutputStream extends OutputStream {	
	private Document _doc;
	private StringBuffer _buf = new StringBuffer(" ");
	
	DocumentOutputStream(Document d) {
		if(d == null) {
			throw new IllegalArgumentException("Document cannot be null");
		}
		_doc = d; 
	}
	
	public void write(int b) {
		try {
			_buf.setCharAt(0,(char) b);
			_doc.insertString(_doc.getLength (),_buf.toString(),null);
		} catch(BadLocationException e) {
			// do nothing
		}		
	}
}
