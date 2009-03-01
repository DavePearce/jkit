package jkit.jil.io;

import java.io.*;
import java.util.List;
import jkit.util.Pair;
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
	
	public void write(Clazz jc) {
		output.print("class " + jc.type() + " ");
		if(jc.superClass() != null) {
			output.println("");
			output.print("\t extends " + jc.superClass());
		}
		if(jc.interfaces().size() > 0) {
			output.println("");
			output.print("\t implements ");
			boolean firstTime=true;
			for(Type.Clazz i : jc.interfaces()) {
				if(!firstTime) {
					output.print(", ");
				}
				firstTime=false;
				output.print(i);
			}			
		}				
		
		output.println(" {\n");
		
		for(Field f : jc.fields()) {
			write(f);
		}
		
		output.println("");
		
		for(Method m : jc.methods()) {
			write(m);
		}
		
		output.println("}");
		
		output.flush();
	}
	
	public void write(Field f) {
		output.print("\t");
		writeModifiers(f.modifiers());
		output.print(f.type());
		output.println(" " + f.name() + ";");		
	}
	
	public void write(Method m) {
		output.print("\t");
		writeModifiers(m.modifiers());
		Type.Function type = m.type(); 
		output.print(type.returnType() + " " + m.name());
		output.print("(");
		boolean firstTime=true;
		
		List<Type> paramTypes = type.parameterTypes();
		List<Pair<String,List<Modifier>>> params = m.parameters();
		
		for(int i = 0; i != params.size();++i) {
			if(!firstTime) {
				output.print(", ");
			}
			firstTime=false;
			writeModifiers(params.get(i).second());			
			output.print(paramTypes.get(i));
			output.print(" " + params.get(i).first());
		}
		
		output.println(") {");
		
		for(Stmt s : m.body()) {
			write(s);
		}
		
		output.println("\t}");
		
	}
	
	protected void write(Stmt s) {
		if(s instanceof Stmt.Assign) {
			write((Stmt.Assign)s);
		} else if(s instanceof Stmt.Return) {
			write((Stmt.Return)s);
		} else if(s instanceof Stmt.Throw) {
			write((Stmt.Return)s);
		} else if(s instanceof Stmt.IfGoto) {
			write((Stmt.IfGoto)s);
		} else if(s instanceof Stmt.Goto) {
			write((Stmt.Goto)s);
		} else if(s instanceof Stmt.Label) {
			write((Stmt.Label)s);
		} else {
			throw new RuntimeException("Invalid statement encountered: "
					+ s.getClass());
		}
	}
	
	protected void write(Stmt.Assign s) {
		output.print("\t\t");
		write(s.lhs());
		output.print(" = ");
		write(s.rhs());
		output.println(";");
	}
	
	protected void write(Stmt.Return s) {
		output.print("\t\treturn");
		if(s.expr() != null) {
			output.print(" ");
			write(s.expr());
		}
		output.println(";");
	}
	
	protected void write(Stmt.Throw s) {
		output.println("\t\tthrow");
		if(s.expr() != null) {
			output.print(" ");
			write(s.expr());
		}
		output.println(";");
	}
	
	protected void write(Stmt.Goto s) {
		output.println("\t\tgoto " + s.label() + ";");
	}
	
	protected void write(Stmt.IfGoto s) {
		output.print("\t\tif(");
		write(s.condition());
		output.println(") goto " + s.label() + ";");
	}
	
	protected void write(Stmt.Label s) {
		output.println("\t" + s.label() + ":");
	}
	
	protected void write(Expr e) {
		if(e instanceof Expr.Bool) {
			write((Expr.Bool)e);
		} else if(e instanceof Expr.Char) {
			write((Expr.Char)e);
		} else if(e instanceof Expr.Byte) {
			write((Expr.Byte)e);
		} else if(e instanceof Expr.Short) {
			write((Expr.Short)e);
		} else if(e instanceof Expr.Int) {
			write((Expr.Int)e);
		} else if(e instanceof Expr.Long) {
			write((Expr.Long)e);
		} else if(e instanceof Expr.Float) {
			write((Expr.Float)e);
		} else if(e instanceof Expr.Double) {
			write((Expr.Double)e);
		} else if(e instanceof Expr.Null) {
			write((Expr.Null)e);
		} else if(e instanceof Expr.Variable) {		
			write((Expr.Variable)e);
		} else if(e instanceof Expr.UnOp) {
			write((Expr.UnOp)e);
		} else if(e instanceof Expr.BinOp) {
			write((Expr.BinOp)e);
		} else if(e instanceof Expr.InstanceOf) {
			write((Expr.InstanceOf)e);
		} else if(e instanceof Expr.Cast) {
			write((Expr.Cast)e);
		} else if(e instanceof Expr.Convert) {
			write((Expr.Convert)e);
		} else if(e instanceof Expr.ArrayIndex) {
			write((Expr.ArrayIndex)e);
		} else if(e instanceof Expr.Deref) {
			write((Expr.Deref)e);
		} else {
			throw new RuntimeException("Invalid expression encountered: "
					+ e.getClass());
		}
	}
	
	protected void write(Expr.Bool e) {
		if(e.value()) {
			output.print("true");
		} else {
			output.print("false");
		}
	}
	
	protected void write(Expr.Char e) {
		output.write("'");
		writeWithEscapes(Character.toString(e.value()));
		output.write("'");		
	}
	
	protected void write(Expr.Short s) {
		output.print(s.value() + "S");
	}
	
	protected void write(Expr.Int s) {
		output.print(s.value());
	}
	
	protected void write(Expr.Long s) {
		output.print(s.value() + "L");
	}
	
	protected void write(Expr.Float s) {
		output.print(s.value() + "F");
	}
	
	protected void write(Expr.Double s) {
		output.print(s.value() + "D");
	}
	
	protected void write(Expr.Null e) {
		output.print("null");
	}
	
	protected void write(Expr.Variable v) {
		output.write(v.value());
	}
	
	public static final String[] unopstr={"!","~","-","++","--","++","--"};	
	
	protected void write(Expr.UnOp e) {
		output.write(unopstr[e.op()]);
		writeExpressionWithBracketsIfNecessary(e.expr());					
	}
	
	protected static final String[] binopstr = {"+", "-", "*", "/", "%", "<<",
		">>", ">>>", "&", "|", "^", "<", "<=", ">", ">=", "==", "!=", "&&",
		"||", "+"};

	
	protected void write(Expr.BinOp e) {
		writeExpressionWithBracketsIfNecessary(e.lhs());					
		output.write(" ");
		output.write(binopstr[e.op()]);
		output.write(" ");
		writeExpressionWithBracketsIfNecessary(e.rhs());	
	}
	
	protected void write(Expr.InstanceOf e) {
		writeExpressionWithBracketsIfNecessary(e.lhs());
		output.write(" instanceof ");
		output.print(e.rhs());
	}
	
	protected void write(Expr.Cast e) {
		output.print("(" + e.type() + ") ");
		writeExpressionWithBracketsIfNecessary(e.expr());				
	}
	
	protected void write(Expr.Convert e) {
		output.print("[" + e.type() + "]");
		writeExpressionWithBracketsIfNecessary(e.expr());				
	}
	
	protected void write(Expr.ArrayIndex e) {
		write(e.target());
		output.print("[");
		write(e.index());
		output.print("]");					
	}
	
	protected void write(Expr.Deref e) {
		write(e.target());
		output.print(".");
		output.print(e.name());						
	}
	
	protected void writeModifiers(List<Modifier> modifiers) {
		for (Modifier x : modifiers) {
			if (x instanceof Modifier.Base) {
				int mod = ((Modifier.Base) x).modifier();
				if ((mod & java.lang.reflect.Modifier.PRIVATE) != 0) {
					output.write("private ");
				}
				if ((mod & java.lang.reflect.Modifier.PROTECTED) != 0) {
					output.write("protected ");
				}
				if ((mod & java.lang.reflect.Modifier.PUBLIC) != 0) {
					output.write("public ");
				}
				if ((mod & java.lang.reflect.Modifier.STATIC) != 0) {
					output.write("static ");
				}
				if ((mod & java.lang.reflect.Modifier.ABSTRACT) != 0) {
					output.write("abstract ");
				}
				if ((mod & java.lang.reflect.Modifier.FINAL) != 0) {
					output.write("final ");
				}
				if ((mod & java.lang.reflect.Modifier.NATIVE) != 0) {
					output.write("native ");
				}
				if ((mod & java.lang.reflect.Modifier.STRICT) != 0) {
					output.write("strictfp ");
				}
				if ((mod & java.lang.reflect.Modifier.SYNCHRONIZED) != 0) {
					output.write("synchronized ");
				}
				if ((mod & java.lang.reflect.Modifier.TRANSIENT) != 0) {
					output.write("transient ");
				}
				if ((mod & java.lang.reflect.Modifier.VOLATILE) != 0) {
					output.write("volatile ");
				}
			} else if (x instanceof Modifier.Annotation) {
				Modifier.Annotation a = (Modifier.Annotation) x;
				output.write("@");
				output.write(a.name());
			} else {
				// do nothing
			}
		}
	}
	
	protected void writeExpressionWithBracketsIfNecessary(Expr e) {
		if (e instanceof Expr.BinOp || e instanceof Expr.InstanceOf
				|| e instanceof Expr.Cast || e instanceof Expr.Convert) {
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
