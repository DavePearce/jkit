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
//
// Notes
// =====
// This class is used to implement the input statement.  The details 
// of how this works are well beyond the scope of COMP205.  So, you 
// do not need to concern yourself with changing anything in this 
// file --- no marks will be awarded for any refactoring here.
//
// As an aside, if anyone happens to know of a better way to do what 
// this class does I would be very grateful to hear it! -Dave


// ==============================================
// COMP205: YOU DO NOT NEED TO MODIFY THIS CLASS!
// ==============================================


package org.simplelisp.editor;

import java.io.*;
import java.awt.*;
import java.awt.event.*;

public class ComponentInputStream extends InputStream implements KeyListener {				
	private ByteArrayOutputStream _in = new ByteArrayOutputStream();
	private PrintStream _out = new PrintStream(_in);
	private byte _packet[] = null;	
	private int _pos = 0;
	
	public ComponentInputStream(Component c) {		
		if(c == null) {
			throw new IllegalArgumentException("Component cannot be null");
		}
		c.addKeyListener(this);			
	}
	
	public synchronized int read() throws IOException { 
		try {
			while(_packet == null) { wait(); }
			int b = _packet[_pos++];
			if(_pos == _packet.length) { 
				_packet = null;				
			}			
			return b;
		} catch(InterruptedException e) {
			return -1;
		}
	}
	
	public synchronized int available() throws IOException {			
		if(_packet != null) {
			return (_packet.length - _pos) + _in.size();
		} else { return _in.size(); }
	}
	
    // -------------------
	// KeyListener Methods
	// -------------------
	
	public void keyPressed(KeyEvent e) {}	
	public void keyReleased(KeyEvent e) {}
	
	public synchronized void keyTyped(KeyEvent e) {      						
		char c = e.getKeyChar();
		_out.print(c);
			
		if(c == '\n' && _packet == null) {
			// I plain don't understand why
			// the -1 is needed here!?
			_out.write(-1);
			_packet = _in.toByteArray();
			_pos = 0;
			_in.reset();
			// tell reader data arrived
			notify();
		}
	}
}
