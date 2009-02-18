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

package org.simplelisp.editor;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.HashMap;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.simplelisp.interpreter.*;

public class InternalFunctions {
	public static void setup_internals(final Interpreter interpreter, final InterpreterFrame frame) {
		// this methods adds a number of additional
		// functions to the Lisp environment which allow
		// customization of the GUI from within Lisp code!

	    	interpreter.setGlobalExpr("set-window-width", new LispFunction(1) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			if(!(es[0] instanceof LispInteger)) {
	    				throw new Error("set-window-width needs an integer!");
	    			}
	    			int width = ((LispInteger) es[0]).value();		    
	    			int height = frame.getHeight();
	    			frame.changeSize(width,height);
	    			return new LispNil();
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("set-window-height", new LispFunction(1) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			if(!(es[0] instanceof LispInteger)) {
	    				throw new Error("set-window-height needs an integer!");
	    			}
	    			int height = ((LispInteger) es[0]).value();		    
	    			int width = frame.getWidth();
	    			frame.changeSize(width,height);
	    			return new LispNil();
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("window-width", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			return new LispInteger(frame.getWidth());
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("window-height", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			return new LispInteger(frame.getHeight());
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("screen-width", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	    			return new LispInteger((int) dim.getWidth());
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("screen-height", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	    			return new LispInteger((int) dim.getHeight());
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("set-bottom-pane-proportion", new LispFunction(1) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			if(!(es[0] instanceof LispInteger)) {
	    				throw new Error("set-bottom-proportion needs an integer!");
	    			}
	    			int bottomProportion = ((LispInteger) es[0]).value();
	    			frame.setTopProportion(100-bottomProportion);		    	    			
	    			int height = frame.getHeight();
	    			int width = frame.getWidth();
	    			frame.changeSize(width,height);
	    			return new LispNil();
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("set-top-pane-proportion", new LispFunction(1) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			if(!(es[0] instanceof LispInteger)) {
	    				throw new Error("set-top-proportion needs an integer!");
	    			}
	    			int topProportion = ((LispInteger) es[0]).value();	
	    			frame.setTopProportion(topProportion);	
	    			int height = frame.getHeight();
	    			int width = frame.getWidth();
	    			frame.changeSize(width,height);
	    			return new LispNil();
	    		}
	    	});	    	    		    
	    	
	    	interpreter.setGlobalExpr("toolbar-mode", new LispFunction(1) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			if(!(es[0] instanceof LispInteger)) {
	    				throw new Error("toolbar-mode needs an integer!");
	    			}
	    			LispInteger i = (LispInteger) es[0];
	    			frame.setToolBarMode(i.value() == 1);	    			
	    			return new LispNil();
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("menubar-mode", new LispFunction(1) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			if(!(es[0] instanceof LispInteger)) {
	    				throw new Error("toolbar-mode needs an integer!");
	    			}	    			
	    			LispInteger i = (LispInteger) es[0];
	    			frame.setMenuBarMode(i.value() == 1);	   	    			
	    			return new LispNil();
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("statusbar-mode", new LispFunction(1) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			if(!(es[0] instanceof LispInteger)) {
	    				throw new Error("toolbar-mode needs an integer!");
	    			}
	    			LispInteger i = (LispInteger) es[0];
	    			frame.setStatusBarMode(i.value() == 1);		    			
	    			return new LispNil();
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("set-key", new LispFunction(2) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			if(!(es[0] instanceof LispString)) {
	    				throw new Error("set-key takes a string and a Lisp expression");
	    			}
	    			String keySequence = ((LispString)es[0]).toString();		   
	    			frame.bindKeyToCommand(keySequence,es[1]);
	    			return new LispNil();
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("unset-key", new LispFunction(1) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			if(!(es[0] instanceof LispString)) {
	    				throw new Error("unset-key takes a string");
	    			}
	    			String keySequence = ((LispString)es[0]).toString();		   	    			
	    			frame.unbindKey(keySequence);
	    			return new LispNil();
	    		}
	    	});
	    	
	    	
	    	interpreter.setGlobalExpr("buffer-copy", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			frame.copy();
	    			return new LispNil();
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("buffer-cut", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			frame.cut();
	    			return new LispNil();
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("buffer-paste", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			frame.paste();
	    			return new LispNil();
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("buffer-read-string", new LispFunction(2) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			if(!(es[0] instanceof LispInteger) || !(es[1] instanceof LispInteger)) {
	    				throw new Error("buffer-read-string takes two integers!");
	    			}
	    			LispInteger pos = (LispInteger) es[0];		    
	    			LispInteger len = (LispInteger) es[1];		    
	    			Document doc = frame.getDocument();
	    			try {
	    				String str = doc.getText(pos.value(),len.value());
	    				return new LispString(str);
	    			} catch(BadLocationException e) {
	    				return new LispNil();
	    			}
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("buffer-insert-string", new LispFunction(2) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			if(!(es[0] instanceof LispInteger) || !(es[1] instanceof LispString)) {
	    				throw new Error("buffer-insert-string takes an integer and a string!");
	    			}
	    			LispInteger pos = (LispInteger) es[0];		    
	    			LispString str = (LispString) es[1];		    
	    			Document doc = frame.getDocument();
	    			try {
	    				doc.insertString(pos.value(),str.toString(),null);		    
	    				return new LispTrue();
	    			} catch(BadLocationException e) {
	    				return new LispNil();
	    			}
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("buffer-length", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			Document doc = frame.getDocument();
	    			return new LispInteger(doc.getLength());
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("buffer-remove-string", new LispFunction(2) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			if(!(es[0] instanceof LispInteger) || !(es[1] instanceof LispString)) {
	    				throw new Error("buffer-remove--string takes two integers!");
	    			}
	    			LispInteger pos = (LispInteger) es[0];		    
	    			LispInteger len = (LispInteger) es[1];		    
	    			Document doc = frame.getDocument();
	    			try {
	    				doc.remove(pos.value(),len.value());
	    				return new LispTrue();
	    			} catch(BadLocationException e) {
	    				return new LispNil();
	    			}
	    		}
	    	});
	    	
	    	
	    	interpreter.setGlobalExpr("caret-position", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			return new LispInteger(frame.getCaretPosition());
	    		}
	    	});
	    	
	    	interpreter.setGlobalExpr("set-caret-position", new LispFunction(1) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {
	    			if(!(es[0] instanceof LispInteger)) {
	    				throw new Error("set-caret-position takes an integer!");
	    			}
	    			LispInteger pos = (LispInteger) es[0];
	    			frame.setCaretPosition(pos.value());	    			
	    			return new LispNil();
	    		}
	    	});	

	    	interpreter.setGlobalExpr("buffer-eval", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {		    
	    			frame.evaluate();
	    			return new LispNil();
	    		}
	    	});	
	    	
	    	interpreter.setGlobalExpr("eval-stop", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {		    
	    			frame.stopEvaluate();
	    			return new LispNil();
	    		}
	    	});	
	    	
	    	interpreter.setGlobalExpr("exit", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {		    
	    			frame.exit();
	    			return new LispNil();
	    		}
	    	});	
	    	
	    	interpreter.setGlobalExpr("file-open", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {		    
	    			frame.openFile();
	    			return new LispNil();
	    		}
	    	});	
	    	
	    	interpreter.setGlobalExpr("file-new", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {		    
	    			frame.newFile();
	    			return new LispNil();
	    		}
	    	});	
	    	
	    	interpreter.setGlobalExpr("file-save", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {		    
	    			frame.saveFile();
	    			return new LispNil();
	    		}
	    	});		
	    	
	    	interpreter.setGlobalExpr("file-save-as", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {		    
	    			frame.saveFileAs();
	    			return new LispNil();
	    		}
	    	});		
	    	
	    	interpreter.setGlobalExpr("set-selected-tab", new LispFunction(1) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {		    
	    			if(!(es[0] instanceof LispInteger)) {
	    				throw new Error("set-selected-tab takes an integer!");
	    			}
	    			LispInteger pos = (LispInteger) es[0];
	    			frame.setSelectedTab(pos.value());	    					   
	    			return new LispNil();
	    		}
	    	});		
	    	
	    	interpreter.setGlobalExpr("selected-tab", new LispFunction(0) {
	    		public LispExpr internal_invoke(LispExpr[] es, 
	    				HashMap<String,LispExpr> locals, 
	    				HashMap<String,LispExpr> globals) {		    
	    			return new LispInteger(frame.getSelectedTab());
	    		}
	    	});		
	    }
}
