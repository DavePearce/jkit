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

package jkit.jil.io;

import java.io.*;
import java.util.List;
import jkit.util.Pair;
import jkit.java.tree.Expr;
import jkit.java.tree.Stmt;
import jkit.jil.tree.*;

/**
 * This class is used to output Jil code to file.
 * 
 * @author djp
 * 
 */

public class JilFileWriter {
	private PrintWriter output;
	
	public JilFileWriter(Writer out) {
		output = new PrintWriter(out);
	}
	
	public JilFileWriter(OutputStream out) {
		output = new PrintWriter(out);
	}
	
	public void write(JilClass jc) {
		output.print("class " + jc.type() + " ");
		if(jc.superClass() != null) {			
			output.print(" extends " + jc.superClass());
		}
		if(jc.interfaces().size() > 0) {			
			output.print(" implements ");
			boolean firstTime=true;
			for(Type.Clazz i : jc.interfaces()) {
				if(!firstTime) {
					output.print(", ");
				}
				firstTime=false;
				output.print(i);
			}			
		}				
		
		output.println(" {");
		
		for(JilField f : jc.fields()) {
			write(f);
		}
		
		if(!jc.fields().isEmpty()) {
			output.println("");
		}
		
		for(JilMethod m : jc.methods()) {
			write(m);
		}
		
		output.println("}");
		
		output.flush();
	}
	
	public void write(JilField f) {
		output.print("  ");
		writeModifiers(f.modifiers());
		output.print(f.type());
		output.println(" " + f.name() + ";");		
	}
	
	public void write(JilMethod m) {
		output.print("  ");
		writeModifiers(m.modifiers());
		Type.Function type = m.type(); 
		output.print(type.returnType() + " " + m.name());
		output.print("(");
		boolean firstTime=true;	
		
		List<Type> paramTypes = type.parameterTypes();
		List<JilMethod.Parameter> params = m.parameters();
		
		for(int i = 0; i != params.size();++i) {
			if(!firstTime) {
				output.print(", ");
			}
			firstTime=false;
			writeModifiers(params.get(i).modifiers());			
			output.print(paramTypes.get(i));
			output.print(" " + params.get(i).name());
		}
		
		output.println(") {");
		
		for(JilStmt s : m.body()) {
			write(s);
		}
		
		output.println("  }\n");	
	}
	
	protected void write(JilStmt s) {		
		if(s instanceof JilStmt.Assign) {
			write((JilStmt.Assign)s);
		} else if(s instanceof JilStmt.Return) {
			write((JilStmt.Return)s);
		} else if(s instanceof JilStmt.Throw) {
			write((JilStmt.Throw)s);
		} else if(s instanceof JilStmt.IfGoto) {
			write((JilStmt.IfGoto)s);
		} else if(s instanceof JilStmt.Goto) {
			write((JilStmt.Goto)s);
		} else if(s instanceof JilStmt.Label) {
			write((JilStmt.Label)s);
		} else if(s instanceof JilExpr.New) {
			output.print("    ");
			write((JilExpr.New)s);
			write(s.exceptions());
			output.println(";");
		} else if(s instanceof JilExpr.Invoke) {
			output.print("    ");
			write((JilExpr.Invoke)s);
			write(s.exceptions());
			output.println(";");
		} else if(s instanceof JilStmt.Switch) {
			write((JilStmt.Switch)s);
		} else {
			throw new RuntimeException("Invalid statement encountered: "
					+ s.getClass());
		}
	}
	
	protected void write(JilStmt.Assign s) {
		output.print("    ");
		
		if(s.lhs() instanceof JilExpr.Variable) {
			output.print(s.rhs().type() + " ");
		}
		
		write(s.lhs());
		output.print(" = ");
		write(s.rhs());
		write(s.exceptions());
		output.println(";");
	}
	
	protected void write(JilStmt.Return s) {
		output.print("    return");
		if(s.expr() != null) {
			output.print(" ");
			write(s.expr());
		}
		write(s.exceptions());
		output.println(";");
	}
	
	protected void write(JilStmt.Throw s) {
		output.print("    throw");
		if(s.expr() != null) {
			output.print(" ");
			write(s.expr());
		}
		write(s.exceptions());
		output.println(";");
	}
	
	protected void write(JilStmt.Goto s) {
		output.print("    goto " + s.label());
		write(s.exceptions());
		output.println(";");
	}
	
	protected void write(JilStmt.IfGoto s) {
		output.print("    if(");
		write(s.condition());
		output.print(") goto " + s.label());
		write(s.exceptions());
		output.println(";");
	}
	
	protected void write(JilStmt.Label s) {
		output.print("  " + s.label() + ":");
		write(s.exceptions()); // should be nothing	
		output.println();
	}
	
	protected void write(JilStmt.Switch s) {
		output.print("    switch("); 
		write(s.condition());
		output.print(") ");
		write(s.exceptions());
		output.println(" {");
		for(Pair<JilExpr.Number,String> c : s.cases()) {
			output.println("    case " + c.first() + ": goto " + c.second() + ";");			
		}
		output.println("    default: goto " + s.defaultLabel() + ";");		
		output.println("    }");
	}
	
	protected void write(List<? extends Pair<Type.Clazz,String>> exceptions) {		
		for(Pair<Type.Clazz,String> p : exceptions) {			
			output.print(", ");						
			output.print(p.first() + " goto " + p.second());
		}
	}
	
	protected void write(JilExpr e) {
		if(e instanceof JilExpr.Bool) {
			write((JilExpr.Bool)e);
		} else if(e instanceof JilExpr.Char) {
			write((JilExpr.Char)e);
		} else if(e instanceof JilExpr.Byte) {
			write((JilExpr.Byte)e);
		} else if(e instanceof JilExpr.Short) {
			write((JilExpr.Short)e);
		} else if(e instanceof JilExpr.Int) {
			write((JilExpr.Int)e);
		} else if(e instanceof JilExpr.Long) {
			write((JilExpr.Long)e);
		} else if(e instanceof JilExpr.Float) {
			write((JilExpr.Float)e);
		} else if(e instanceof JilExpr.Double) {
			write((JilExpr.Double)e);
		} else if(e instanceof JilExpr.Null) {
			write((JilExpr.Null)e);
		} else if(e instanceof JilExpr.StringVal) {
			write((JilExpr.StringVal)e);
		} else if(e instanceof JilExpr.Array) {
			write((JilExpr.Array)e);
		} else if(e instanceof JilExpr.Variable) {		
			write((JilExpr.Variable)e);
		} else if(e instanceof JilExpr.UnOp) {
			write((JilExpr.UnOp)e);
		} else if(e instanceof JilExpr.BinOp) {
			write((JilExpr.BinOp)e);
		} else if(e instanceof JilExpr.InstanceOf) {
			write((JilExpr.InstanceOf)e);
		} else if(e instanceof JilExpr.Cast) {
			write((JilExpr.Cast)e);
		} else if(e instanceof JilExpr.Convert) {
			write((JilExpr.Convert)e);
		} else if(e instanceof JilExpr.ArrayIndex) {
			write((JilExpr.ArrayIndex)e);
		} else if(e instanceof JilExpr.Deref) {
			write((JilExpr.Deref)e);
		} else if(e instanceof JilExpr.Invoke) {
			write((JilExpr.Invoke)e);
		} else if(e instanceof JilExpr.New) {
			write((JilExpr.New)e);
		} else if(e instanceof JilExpr.Class) {
			write((JilExpr.Class)e);
		} else if(e instanceof JilExpr.ClassVariable) {
			write((JilExpr.ClassVariable)e);
		} else {
			throw new RuntimeException("Invalid expression encountered: "
					+ e.getClass());
		}
	}
	
	protected void write(JilExpr.Bool e) {
		if(e.value()) {
			output.print("true");
		} else {
			output.print("false");
		}
	}
	
	protected void write(JilExpr.Char e) {
		output.write("'");
		writeWithEscapes(Character.toString(e.value()));
		output.write("'");		
	}
	
	protected void write(JilExpr.Byte b) {
		output.print(b.value());
	}
	
	protected void write(JilExpr.Short s) {
		output.print(s.value() + "S");
	}
	
	protected void write(JilExpr.Int s) {
		output.print(s.value());
	}
	
	protected void write(JilExpr.Long s) {
		output.print(s.value() + "L");
	}
	
	protected void write(JilExpr.Float s) {
		output.print(s.value() + "F");
	}
	
	protected void write(JilExpr.Double s) {
		output.print(s.value() + "D");
	}
	
	protected void write(JilExpr.Null e) {
		output.print("null");
	}
	
	protected void write(JilExpr.StringVal e) {
		output.print("\"");
		writeWithEscapes(e.value());
		output.print("\"");
	}
	
	protected void write(JilExpr.Array ae) {
		output.print("new " + ae.type() + "{");
		boolean firstTime=true;
		for(JilExpr e : ae.values()) {
			if(!firstTime) {
				output.print(", ");
			}
			firstTime=false;
			write(e);
		}
		output.print("}");
	}
	
	protected void write(JilExpr.Variable v) {
		output.write(v.value());
	}
	
	protected void write(JilExpr.Class c) {
		output.write(c.classType() + ".class");
	}
	
	protected void write(JilExpr.ClassVariable c) {
		output.write(c.type().toString());
	}
	
	public static final String[] unopstr={"!","~","-","++","--","++","--"};	
	
	protected void write(JilExpr.UnOp e) {
		output.write(unopstr[e.op()]);
		writeExpressionWithBracketsIfNecessary(e.expr());					
	}
	
	protected static final String[] binopstr = {"+", "-", "*", "/", "%", "<<",
		">>", ">>>", "&", "|", "^", "<", "<=", ">", ">=", "==", "!=", "&&",
		"||", "+"};

	
	protected void write(JilExpr.BinOp e) {
		writeExpressionWithBracketsIfNecessary(e.lhs());					
		output.write(" ");
		output.write(binopstr[e.op()]);
		output.write(" ");
		writeExpressionWithBracketsIfNecessary(e.rhs());	
	}
	
	protected void write(JilExpr.InstanceOf e) {
		writeExpressionWithBracketsIfNecessary(e.lhs());
		output.write(" instanceof ");
		output.print(e.rhs());
	}
	
	protected void write(JilExpr.Cast e) {
		output.print("(" + e.type() + ") ");
		writeExpressionWithBracketsIfNecessary(e.expr());				
	}
	
	protected void write(JilExpr.Convert e) {
		output.print("[" + e.type() + "]");
		writeExpressionWithBracketsIfNecessary(e.expr());				
	}
	
	protected void write(JilExpr.ArrayIndex e) {
		write(e.target());
		output.print("[");
		write(e.index());
		output.print("]");					
	}
	
	protected void write(JilExpr.Deref e) {
		write(e.target());		
		output.print(".");
		// output.print("[" + e.type() + "]");
		output.print(e.name());						
	}
	
	protected void write(JilExpr.Invoke e) {
		write(e.target());				
		output.print(".");
		// output.print("[" + e.funType() + "]");
		output.print(e.name());
		if (e instanceof JilExpr.SpecialInvoke) {
			output.print("!");
		}
		output.print("(");
		boolean firstTime=true;
		for(JilExpr p : e.parameters()) {
			if(!firstTime) {
				output.print(", ");
			}
			firstTime=false;
			write(p);
		}
		output.print(")");
	}
	
	protected void write(JilExpr.New e) {
		output.print("new ");
		output.print(e.type());
		output.print("(");
		boolean firstTime=true;
		for(JilExpr p : e.parameters()) {
			if(!firstTime) {
				output.print(", ");
			}
			firstTime=false;
			write(p);
		}
		output.print(")");
	}
	
	protected void writeModifiers(List<Modifier> modifiers) {
		for(Modifier x : modifiers) {						
			if(x instanceof Modifier.Private) { output.write("private "); }
			else if(x instanceof Modifier.Protected) { output.write("protected "); }
			else if(x instanceof Modifier.Public) { output.write("public "); }
			else if(x instanceof Modifier.Static) { output.write("static "); }
			else if(x instanceof Modifier.Abstract) { output.write("abstract "); }
			else if(x instanceof Modifier.Final) { output.write("final "); }
			else if(x instanceof Modifier.Native) { output.write("native "); }				
			else if(x instanceof Modifier.StrictFP) { output.write("strictfp "); }
			else if(x instanceof Modifier.Synchronized) { output.write("synchronized "); }
			else if(x instanceof Modifier.Transient) { output.write("transient "); }
			else if(x instanceof Modifier.Volatile) { output.write("volatile "); }
			else if (x instanceof Modifier.Annotation) {
				Modifier.Annotation a = (Modifier.Annotation) x;
				output.write("@");
				output.write(a.name());
			} else {				
				output.write("unknown ");				
			}
		}					
	}
	
	protected void writeExpressionWithBracketsIfNecessary(JilExpr e) {
		if (e instanceof JilExpr.BinOp || e instanceof JilExpr.InstanceOf
				|| e instanceof JilExpr.Cast || e instanceof JilExpr.Convert) {
			output.write("(");
			write(e);
			output.write(")");
		} else {
			write(e);
		}
	}
	
	protected void writeWithEscapes(String s) {
		for(int i=0;i!=s.length();++i) {
			char c = s.charAt(i);
			switch (c) {
			case '\b':
				output.write("\\b");
				break;
			case '\t':
				output.write("\\t");
				break;
			case '\f':
				output.write("\\f");
				break;
			case '\n':
				output.write("\\n");
				break;
			case '\r':
				output.write("\\r");
				break;
			case '\"':
				output.write("\\\"");
				break;
			case '\\':
				output.write("\\\\");
				break;
			case '\'':		
				output.write("\\'");
				break;
			default:
				if(c >= 32 && c < 128) {
					output.write(Character.toString(c));
				} else {
					String str = Integer.toString(c,16);
					int padding = 4 - str.length();
					for(int k=0;k!=padding;++k) {
						str = "0" + str;
					}
					output.write("\\u" + str);
				}
			}
		}
	}
}
