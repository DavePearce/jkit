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
import javax.swing.*;
import javax.swing.event.*;

public class JConsoleArea extends JTextArea implements DocumentListener {
	// A JConsoleArea is a text area which provides several 
	// pieces of functionality:
	//
	// 1) Input from the text area can be directed into 
	//    an input stream.
	// 2) Output from an output stream can be directed
	//    into the text area.
	// 3) the cursor (a.k.a. the caret) is fixed to be
	//    at the bottom of the text area after any text
	//    insertion.
	
    // needed for serialization
	private static final long serialVersionUID = 202L;
	
	// create the input and output streams 
	private PrintStream _output = new PrintStream(
    		new DocumentOutputStream(getDocument()));
	private InputStream _input = new ComponentInputStream(this);
	
	public JConsoleArea() {		
		super(); 
		getDocument().addDocumentListener(this);
	}
	public JConsoleArea(int width, int height) { 
		super(width,height); 
		getDocument().addDocumentListener(this);
	}
	
	public void setEditable(boolean flag) {
		super.setEditable(flag);
		if(flag && _input == null) {			
			_input = new ComponentInputStream(this);
		} else if(!flag && _input != null){
			try {
				_input.close();
			} catch(IOException e) {				
			}
			_input = null;
		}
	}
	public PrintStream getOutputStream() { return _output; }
	public InputStream getInputStream() { return _input; }
	
    // ------------------------
	// DocumentListener Methods
	// ------------------------

	public void changedUpdate(DocumentEvent e) {}

	public void insertUpdate(DocumentEvent e) {		
		// text inserted to automatically move
		// cursor to end
		setCaretPosition(getDocument().getLength());
	}

	public void removeUpdate(DocumentEvent e) {}
}
