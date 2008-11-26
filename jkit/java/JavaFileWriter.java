package jkit.java;

import java.io.*;
import java.lang.reflect.Modifier;

import jkit.util.*; 

public class JavaFileWriter {
	private PrintWriter output;
	
	public JavaFileWriter(Writer out) {
		output = new PrintWriter(out);
	}
	
	public JavaFileWriter(OutputStream out) {
		output = new PrintWriter(out);
	}
	
	public void write(JavaFile jf) {
		if(jf.pkg() != null) {
			output.println("package " + jf.pkg() + ";");
		}
		
		output.println("");
		
		for(String imp : jf.imports()) {
			output.println("import " + imp + ";");
		}
		
		output.println("");
		
		for(JavaFile.Clazz decl : jf.classes()) {
			writeClass(decl,0);
		}
		
		output.flush();
	}
	
	public void writeClass(JavaFile.Clazz decl, int depth) {
		indent(depth);
		writeModifiers(decl.modifiers());
		
		if(decl.isInterface()) {
			output.print("interface ");
		} else {
			output.print("class ");
		}
		
		output.print(decl.name());
		
		if(decl.isInterface()) {
			if(decl.interfaces().size() > 0) {
				output.print(" extends ");
				boolean firstTime = true;
				for(JavaFile.Type i : decl.interfaces()) {
					if(!firstTime) {
						output.print(", ");
					} else { firstTime = false; }
					writeType(i);
					output.write(" ");
				}
			}
		} else {
			if(decl.superclass() != null) {
				output.print(" extends ");
				writeType(decl.superclass());
				output.write(" ");
			}
			if(decl.interfaces().size() > 0) {
				output.print(" implements ");
				boolean firstTime = true;
				for(JavaFile.Type i : decl.interfaces()) {
					if(!firstTime) {
						output.print(", ");
					} else { firstTime = false; }
					writeType(i);
					output.write(" ");
				}
			}
		}
		
		output.println(" {");
		
		for(JavaFile.Declaration d : decl.declarations()) {
			if(d instanceof JavaFile.Clazz) {
				writeClass((JavaFile.Clazz) d, depth + 1);
			} else if(d instanceof JavaFile.Field) {
				writeField((JavaFile.Field) d, depth + 1);
			} else if(d instanceof JavaFile.Method) {
				writeMethod((JavaFile.Method) d, depth + 1);
			}
		}
		
		indent(depth);output.println("}");
	}
	
	protected void writeMethod(JavaFile.Method m, int depth) {
		indent(depth);
		writeModifiers(m.modifiers());
		writeType(m.returnType());
		output.write(" ");
		output.write(m.name());
		output.write("(");
		boolean firstTime=true;
		for(Pair<String,JavaFile.Type> p : m.parameters()) {
			if(!firstTime) {
				output.write(", ");				
			}
			firstTime=false;
			writeType(p.second());
			output.write(" ");
			output.write(p.first());
		}
		output.write(")");
		
		if(m.exceptions().size() > 0) {
			output.write("throws ");
			firstTime=true;
			for(JavaFile.Type t : m.exceptions()) {
				if(!firstTime) {
					output.write(", ");				
				}
				firstTime=false;
				writeType(t);
			}
		}
				
		if(m.block() != null) { writeBlock(m.block(),depth); }
	}
	
	protected void writeField(JavaFile.Field f, int depth) {
		indent(depth);
		writeModifiers(f.modifiers());		
		writeType(f.type());
		output.write(" ");
		output.print(f.name());
		if(f.initialiser() != null) {
			output.print(" = ");
			writeExpression(f.initialiser());
		}
		output.println(";\n");
	}
	
	protected void writeStatement(JavaFile.Statement e, int depth) {
		if(e instanceof JavaFile.SynchronisedBlock) {
			writeSynchronisedBlock((JavaFile.SynchronisedBlock)e, depth);
		} else if(e instanceof JavaFile.Block) {
			writeBlock((JavaFile.Block)e, depth);
		} else if(e instanceof JavaFile.VarDef) {
			writeVarDef((JavaFile.VarDef) e, depth);
		} else if(e instanceof JavaFile.Assignment) {
			writeAssignment((JavaFile.Assignment) e, depth);
		} else if(e instanceof JavaFile.Return) {
			writeReturn((JavaFile.Return) e, depth);
		} else if(e instanceof JavaFile.Throw) {
			writeThrow((JavaFile.Throw) e, depth);
		} else if(e instanceof JavaFile.Break) {
			writeBreak((JavaFile.Break) e, depth);
		} else if(e instanceof JavaFile.Continue) {
			writeContinue((JavaFile.Continue) e, depth);
		} else if(e instanceof JavaFile.Label) {
			writeLabel((JavaFile.Label) e, depth);
		} else {
			throw new RuntimeException("Invalid statement encountered: "
					+ e.getClass());
		}
	}
	
	protected void writeBlock(JavaFile.Block block, int depth) {
		indent(depth);
		output.println("{");
		for(JavaFile.Statement s : block.statements()) {
			writeStatement(s,depth+1);
		}
		indent(depth);output.println("}");
	}
	
	protected void writeSynchronisedBlock(JavaFile.SynchronisedBlock block, int depth) {
		indent(depth);
		output.write("synchronized(");
		writeExpression(block.expr());
		output.println(") {");
		for(JavaFile.Statement s : block.statements()) {
			writeStatement(s,depth+1);
		}
		indent(depth);output.println("}");
	}
	
	protected void writeVarDef(JavaFile.VarDef def, int depth) {				
		for(Triple<String,JavaFile.Type,JavaFile.Expression> d : def.definitions()) {			
			indent(depth);
			writeModifiers(def.modifiers());
			writeType(d.second());
			output.write(" ");
			output.write(d.first());
			if(d.third() != null) {
				output.write(" = ");
				writeExpression(d.third());
			}
			output.println(";");
		}
	}
	
	protected void writeAssignment(JavaFile.Assignment def, int depth) {
		indent(depth);
		writeExpression(def.lhs());
		output.write(" = ");
		writeExpression(def.rhs());
		output.println(";");
	}
	
	protected void writeReturn(JavaFile.Return ret, int depth) {
		indent(depth);		
		output.write("return");
		if(ret.expr() != null) {
			output.write(" ");
			writeExpression(ret.expr());
		}
		output.println(";");
	}
	
	protected void writeThrow(JavaFile.Throw ret, int depth) {
		indent(depth);		
		output.write("throw ");				
		writeExpression(ret.expr());		
		output.println(";");
	}
	
	protected void writeBreak(JavaFile.Break brk, int depth) {
		indent(depth);		
		output.write("break");
		if(brk.label() != null) {
			output.write(" ");
			output.write(brk.label());
		}
		output.println(";");
	}
	
	protected void writeContinue(JavaFile.Continue brk, int depth) {
		indent(depth);		
		output.write("continue");
		if(brk.label() != null) {
			output.write(" ");
			output.write(brk.label());
		}
		output.println(";");
	}
	
	protected void writeLabel(JavaFile.Label lab, int depth) {
		indent(depth);		
		output.write(lab.label());
		output.write(": ");
		writeStatement(lab.statement(),0);
	}
	
	protected void writeExpression(JavaFile.Expression e) {
		
		if(e instanceof JavaFile.BoolVal) {
			writeBoolVal((JavaFile.BoolVal)e);
		} else if(e instanceof JavaFile.CharVal) {
			writeCharVal((JavaFile.CharVal)e);
		} else if(e instanceof JavaFile.IntVal) {
			writeIntVal((JavaFile.IntVal)e);
		} else if(e instanceof JavaFile.LongVal) {
			writeLongVal((JavaFile.LongVal)e);
		} else if(e instanceof JavaFile.FloatVal) {
			writeFloatVal((JavaFile.FloatVal)e);
		} else if(e instanceof JavaFile.DoubleVal) {
			writeDoubleVal((JavaFile.DoubleVal)e);
		} else if(e instanceof JavaFile.StringVal) {
			writeStringVal((JavaFile.StringVal)e);
		} else if(e instanceof JavaFile.NullVal) {
			writeNullVal((JavaFile.NullVal)e);
		} else if(e instanceof JavaFile.ArrayVal) {
			writeArrayVal((JavaFile.ArrayVal)e);
		} else if(e instanceof JavaFile.ClassVal) {
			writeClassVal((JavaFile.ClassVal) e);
		} else if(e instanceof JavaFile.Variable) {
			writeVariable((JavaFile.Variable)e);
		} else if(e instanceof JavaFile.UnOp) {
			writeUnOp((JavaFile.UnOp)e);
		} else if(e instanceof JavaFile.BinOp) {
			writeBinOp((JavaFile.BinOp)e);
		} else if(e instanceof JavaFile.TernOp) {
			writeTernOp((JavaFile.TernOp)e);
		} else if(e instanceof JavaFile.Cast) {
			writeCast((JavaFile.Cast)e);
		} else if(e instanceof JavaFile.InstanceOf) {
			writeInstanceOf((JavaFile.InstanceOf)e);
		} else if(e instanceof JavaFile.Invoke) {
			writeInvoke((JavaFile.Invoke) e);
		} else if(e instanceof JavaFile.New) {
			writeNew((JavaFile.New) e);
		} else if(e instanceof JavaFile.ArrayIndex) {
			writeArrayIndex((JavaFile.ArrayIndex) e);
		} else if(e instanceof JavaFile.Deref) {
			writeDeref((JavaFile.Deref) e);
		} else {
			throw new RuntimeException("Invalid expression encountered: "
					+ e.getClass());
		}
	}
	
	protected void writeDeref(JavaFile.Deref e) {
		writeExpression(e.target());
		output.write(".");
		output.write(e.name());		
	}
	
	protected void writeArrayIndex(JavaFile.ArrayIndex e) {
		writeExpression(e.target());
		output.write("[");
		writeExpression(e.index());
		output.write("]");
	}
	
	protected void writeNew(JavaFile.New e) {
		output.write("new ");
		writeType(e.type());		
		output.write("(");
		boolean firstTime=true;
		for(JavaFile.Expression i : e.parameters()) {
			if(!firstTime) {
				output.write(", ");
			} else {
				firstTime = false;
			}
			writeExpression(i);
		}
		output.write(")");
		
		if(e.declarations().size() > 0) {
			output.write(" { ");
			for(JavaFile.Declaration d : e.declarations()) {				
				if(d instanceof JavaFile.Clazz) {
					writeClass((JavaFile.Clazz) d, 0);
				} else if(d instanceof JavaFile.Field) {
					writeField((JavaFile.Field) d, 0);
				} else if(d instanceof JavaFile.Method) {
					writeMethod((JavaFile.Method) d, 0);
				} else {
					throw new RuntimeException(
							"Support required for methods in anonymous inner classes");
				}
			}
			output.write(" } ");
		}
	}
	
	protected void writeInvoke(JavaFile.Invoke e) {
		if(e.target() != null) {
			writeExpression(e.target());
			output.write(".");
		}
		output.write(e.name());
		if(e.typeParameters().size() > 0) {
			output.write("<");
			boolean firstTime=true;
			for(JavaFile.Type i : e.typeParameters()) {
				if(!firstTime) {
					output.write(",");
				} else {
					firstTime = false;
				}
				writeType(i);
			}	
			output.write(">");
		}
		output.write("(");
		boolean firstTime=true;
		for(JavaFile.Expression i : e.parameters()) {
			if(!firstTime) {
				output.write(", ");
			} else {
				firstTime = false;
			}
			writeExpression(i);
		}
		output.write(")");
	}
	
	protected void writeInstanceOf(JavaFile.InstanceOf e) {		
		writeExpression(e.lhs());
		output.write(" instanceof ");
		writeType(e.rhs());		
	}
	
	protected void writeCast(JavaFile.Cast e) {
		output.write("(");
		writeType(e.type());
		output.write(") ");
		writeExpression(e.expr());
	}
	
	protected void writeBoolVal(JavaFile.BoolVal e) {
		if(e.value()) {
			output.write("true");
		} else {
			output.write("false");
		}
	}
	
	protected void writeCharVal(JavaFile.CharVal e) {
		output.write("'");
		output.write(e.value()); // this will fail for non-ASCII chars
		output.write("'");
	}
	
	protected void writeIntVal(JavaFile.IntVal e) {		
		output.write(Integer.toString(e.value()));
	}
	
	protected void writeLongVal(JavaFile.LongVal e) {		
		output.write(Long.toString(e.value()) + "L");
	}
	
	protected void writeFloatVal(JavaFile.FloatVal e) {		
		output.write(Float.toString(e.value()) + "F");
	}
	
	protected void writeDoubleVal(JavaFile.DoubleVal e) {		
		output.write(Double.toString(e.value()));
	}
	
	protected void writeStringVal(JavaFile.StringVal e) {		
		output.write("\"");
		output.write(e.value());
		output.write("\"");
	}
	
	protected void writeNullVal(JavaFile.NullVal e) {		
		output.write("null");
	}
	
	protected void writeArrayVal(JavaFile.ArrayVal e) {		
		boolean firstTime = true;
		output.write("{");
		for(JavaFile.Expression i : e.values()) {
			if(!firstTime) {
				output.write(", ");
			} else {
				firstTime = false;
			}
			writeExpression(i);
		}
		output.write("}");
	}
	
	protected void writeClassVal(JavaFile.ClassVal e) {
		writeType(e.value());
		output.write(".class");
	}
	
	protected void writeVariable(JavaFile.Variable e) {			
		output.write(e.value());		
	}
	
	public static final String[] unopstr={"!","~","-","++","--","++","--"};
	
	protected void writeUnOp(JavaFile.UnOp e) {		
		
		if(e.op() != JavaFile.UnOp.POSTDEC && e.op() != JavaFile.UnOp.POSTINC) {
			output.write(unopstr[e.op()]);
		}
				
		writeExpressionWithBracketsIfNecessary(e.expr());					
		
		if (e.op() == JavaFile.UnOp.POSTDEC || e.op() == JavaFile.UnOp.POSTINC) {
			output.write(unopstr[e.op()]);
		}
	}
	

	protected static final String[] binopstr = {"+", "-", "*", "/", "%", "<<",
			">>", ">>>", "&", "|", "^", "<", "<=", ">", ">=", "==", "!=", "&&",
			"||", "++"};
	
	
	protected void writeBinOp(JavaFile.BinOp e) {				
		writeExpressionWithBracketsIfNecessary(e.lhs());					
		output.write(binopstr[e.op()]);
		writeExpressionWithBracketsIfNecessary(e.rhs());						
	}
	
	protected void writeTernOp(JavaFile.TernOp e) {		
		writeExpressionWithBracketsIfNecessary(e.condition());		
		output.write(" ? ");
		writeExpressionWithBracketsIfNecessary(e.trueBranch());
		output.write(" : ");
		writeExpressionWithBracketsIfNecessary(e.falseBranch());
	}
	
	protected void writeExpressionWithBracketsIfNecessary(JavaFile.Expression e) {		
		if(e instanceof JavaFile.BinOp) {
			output.print("(");
			writeExpression(e);
			output.print(")");
		} else {
			writeExpression(e);			
		}
	}
	
	protected void writeType(JavaFile.Type t) {
		boolean firstTime=true;
		for(String c : t.components()) {
			if(!firstTime) {
				output.write(".");
			} else {
				firstTime=false;
			}
			output.write(c);			
		}
		for(int i=0;i!=t.dims();++i) {
			output.write("[]");
		}		
	}
	
	protected void writeModifiers(int modifiers) {
		if((modifiers & Modifier.STATIC)!=0) { output.print("static "); }
		if((modifiers & Modifier.ABSTRACT)!=0) { output.print("abstract "); }
		if((modifiers & Modifier.FINAL)!=0) { output.print("final "); }
		if((modifiers & Modifier.NATIVE)!=0) { output.print("native "); }
		if((modifiers & Modifier.PRIVATE)!=0) { output.print("private "); }
		if((modifiers & Modifier.PROTECTED)!=0) { output.print("protected "); }
		if((modifiers & Modifier.PUBLIC)!=0) { output.print("public "); }
		if((modifiers & Modifier.STRICT)!=0) { output.print("strict "); }
		if((modifiers & Modifier.SYNCHRONIZED)!=0) { output.print("synchronized "); }
		if((modifiers & Modifier.TRANSIENT)!=0) { output.print("transient "); }
		if((modifiers & Modifier.VOLATILE)!=0) { output.print("volatile "); }
	}
	
	protected void indent(int level) {
		for(int i=0;i!=level;++i) {
			output.print("  ");
		}
	}
}
