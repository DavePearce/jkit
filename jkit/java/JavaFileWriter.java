package jkit.java;

import java.io.*;
import java.util.*;

import jkit.jil.*;
import jkit.util.*; 

public class JavaFileWriter {
	private PrintWriter output;
	private int depth = 0;
	
	public JavaFileWriter(Writer out) {
		output = new PrintWriter(out);
	}
	
	public JavaFileWriter(OutputStream out) {
		output = new PrintWriter(out);
	}
	
	public void write(JavaFile jf) {
		if(jf.pkg() != null) {
			output.println("package " + jf.pkg() + ";\n");
		}				
		
		for(Pair<Boolean,String> imp : jf.imports()) {
			if(imp.first()) {
				output.println("import static " + imp.second() + ";");
			} else {
				output.println("import " + imp.second() + ";");
			}
		}				
		if(jf.imports().size() > 0) {
			output.println("");
		}
		for(JavaFile.Declaration decl : jf.declarations()) {
			writeDeclaration(decl);			
		}
		
		output.flush();
	}
	
	public void writeDeclaration(JavaFile.Declaration d) {
		if(d instanceof JavaFile.Enum) {
			writeEnum((JavaFile.Enum) d);
		} else if(d instanceof JavaFile.Clazz) {
			writeClass((JavaFile.Clazz) d);
		} else if(d instanceof JavaFile.Field) {
			writeField((JavaFile.Field) d);
		} else if(d instanceof JavaFile.Method) {
			writeMethod((JavaFile.Method) d);
		} else if(d instanceof JavaFile.StaticInitialiserBlock) {
			writeStaticInitialiserBlock((JavaFile.StaticInitialiserBlock)d);
		} else if(d instanceof JavaFile.InitialiserBlock) {
			writeInitialiserBlock((JavaFile.InitialiserBlock)d);
		} else if(d instanceof JavaFile.AnnotationInterface) {
			writeAnnotationInterface((JavaFile.AnnotationInterface)d);
		}
	}
	
	public void writeClass(JavaFile.Clazz decl) {
		
		writeModifiers(decl.modifiers());
		
		if(decl instanceof JavaFile.Interface) {
			write("interface ");
		} else {
			write("class ");
		}
		
		write(decl.name());				
		
		if(decl.typeParameters().size() > 0) {
			write("<");
			boolean firstTime = true;
			for(Type.Variable vt : decl.typeParameters()) {
				if(!firstTime) {
					write(",");
				}
				firstTime=false;
				writeType(vt);
			}
			write(">");
		}
		
		if(decl instanceof JavaFile.Interface) {
			if(decl.interfaces().size() > 0) {
				write(" extends ");
				boolean firstTime = true;
				for(Type.Clazz i : decl.interfaces()) {
					if(!firstTime) {
						write(", ");
					} else { firstTime = false; }
					writeType(i);
					write(" ");
				}
			}
		} else {
			if(decl.superclass() != null) {
				write(" extends ");
				writeType(decl.superclass());
				write(" ");
			}
			if(decl.interfaces().size() > 0) {
				write(" implements ");
				boolean firstTime = true;
				for(Type.Clazz i : decl.interfaces()) {
					if(!firstTime) {
						write(", ");
					} else { firstTime = false; }
					writeType(i);
					write(" ");
				}
			}
		}
		
		write("{");
		
		for(JavaFile.Declaration d : decl.declarations()) {
			writeDeclaration(d);			
		}
		
		write("}");
	}
	
	protected void writeEnum(JavaFile.Enum decl) {
		writeModifiers(decl.modifiers());
		write("enum ");
		write(decl.name());
		
		if(decl.interfaces().size() > 0) {
			write(" implements ");
			boolean firstTime = true;
			for(Type.Clazz i : decl.interfaces()) {
				if(!firstTime) {
					write(", ");
				} else { firstTime = false; }
				writeType(i);
				write(" ");
			}
		}
		
		write("{");
		
		boolean firstTime = true;
		for(JavaFile.EnumConstant c : decl.constants()) {
			if(!firstTime) {
				write(",");
				write("\n");
			}
			firstTime=false;
			writeEnumConstant(c);
		}
		
		if(decl.declarations().size() > 0) {
			write(";");			
			for(JavaFile.Declaration d : decl.declarations()) {				
				writeDeclaration(d);
			}
		}
		
		write("\n");
		write("}");
	}
	
	protected void writeEnumConstant(JavaFile.EnumConstant c) {
		write(c.name());
		if(c.arguments().size() > 0) {
			write("(");
			boolean firstTime = true;
			for(Expr e : c.arguments()) {
				if(!firstTime) {
					write(",");
				}
				firstTime=false;
				writeExpression(e);
			}
			write(")");
		}
		
		if(c.declarations().size() > 0) {
			write("{");
			boolean firstTime = true;
			for(JavaFile.Declaration d : c.declarations()) {
				if(!firstTime) {
					write(",");
				}
				firstTime=false;
				writeDeclaration(d);
			}
			write("}");			
		}
	}
	
	protected void writeAnnotationInterface(JavaFile.AnnotationInterface e) {
		writeModifiers(e.modifiers());
		write("@interface ");
		write(e.name());
		write("{");
		for(Triple<Type, String, Value> m : e.methods()) {
			writeType(m.first());
			write(" ");
			write(m.second());
			write("()");
			if(m.third() != null) {
				write(" default ");
				writeExpression(m.third());
			}
			write(";");			
		}
		write("}");
	}
	
	protected void writeMethod(JavaFile.Method m) {
		write("\n");
		writeModifiers(m.modifiers());
		
		if(m.typeParameters().size() > 0) {
			write("<");
			boolean firstTime=true;
			for(Type t : m.typeParameters()) {
				if(!firstTime) {
					write(",");
				}
				firstTime=false;
				writeType(t);
			}
			write("> ");
		}
		
		if(m.returnType() != null) {
			// can be null if this method is actually a constructor.
			writeType(m.returnType());
			write(" ");
		}		
		write(m.name());
		write("(");
		boolean firstTime=true;
		int va_count = 1; // to detect varargs 
		for(Triple<String,List<Modifier>,Type> p : m.parameters()) {
			if(!firstTime) {
				write(", ");				
			}
			firstTime=false;
			writeModifiers(p.second());
			writeType(p.third());
			
			if(m.varargs() && va_count == m.parameters().size()) {
				write("...");
			}
			
			write(" ");
			write(p.first());
			va_count++;
		}
		write(")");
		
		if(m.exceptions().size() > 0) {
			write(" throws ");
			firstTime=true;
			for(Type.Clazz t : m.exceptions()) {
				if(!firstTime) {
					write(", ");				
				}
				firstTime=false;
				writeType(t);
			}
		}
				
		if(m.block() != null) { 
			writeBlock(m.block()); 
		} else {
			write(";");
		}
	}
	
	protected void writeField(JavaFile.Field f) {
		
		writeModifiers(f.modifiers());		
		writeType(f.type());
		write(" ");
		write(f.name());
		if(f.initialiser() != null) {
			write(" = ");
			writeExpression(f.initialiser());
		}
		write(";");
	}
	
	protected void writeStaticInitialiserBlock(JavaFile.StaticInitialiserBlock e) {
		write("static ");
		writeBlock(e);
	}
	
	protected void writeInitialiserBlock(JavaFile.InitialiserBlock e) {		
		writeBlock(e);
	}
	
	protected void writeSubStatement(Stmt e) {
		if(e instanceof Stmt.Block) {
			writeStatement(e);
		} else {
			depth = depth + 1;
			write("\n");
			writeStatement(e);
			depth = depth - 1;
			if(e instanceof Stmt.Simple) {
				write(";");
			}			
		}
	}
	
	protected void writeStatement(Stmt e) {
		if(e instanceof Stmt.SynchronisedBlock) {
			writeSynchronisedBlock((Stmt.SynchronisedBlock)e);
		} else if(e instanceof Stmt.TryCatchBlock) {
			writeTryCatchBlock((Stmt.TryCatchBlock)e);
		} else if(e instanceof Stmt.Block) {
			writeBlock((Stmt.Block)e);
		} else if(e instanceof Stmt.VarDef) {
			writeVarDef((Stmt.VarDef) e);
		} else if(e instanceof Stmt.Assignment) {
			writeAssignment((Stmt.Assignment) e);
		} else if(e instanceof Stmt.Return) {
			writeReturn((Stmt.Return) e);
		} else if(e instanceof Stmt.Throw) {
			writeThrow((Stmt.Throw) e);
		} else if(e instanceof Stmt.Assert) {
			writeAssert((Stmt.Assert) e);
		} else if(e instanceof Stmt.Break) {
			writeBreak((Stmt.Break) e);
		} else if(e instanceof Stmt.Continue) {
			writeContinue((Stmt.Continue) e);
		} else if(e instanceof Stmt.Label) {
			writeLabel((Stmt.Label) e);
		} else if(e instanceof Stmt.If) {
			writeIf((Stmt.If) e);
		} else if(e instanceof Stmt.For) {
			writeFor((Stmt.For) e);
		} else if(e instanceof Stmt.ForEach) {
			writeForEach((Stmt.ForEach) e);
		} else if(e instanceof Stmt.While) {
			writeWhile((Stmt.While) e);
		} else if(e instanceof Stmt.DoWhile) {
			writeDoWhile((Stmt.DoWhile) e);
		} else if(e instanceof Stmt.Switch) {
			writeSwitch((Stmt.Switch) e);
		} else if(e instanceof Expr.Invoke) {
			writeInvoke((Expr.Invoke) e);
		} else if(e instanceof Expr.New) {
			writeNew((Expr.New) e);
		} else if(e instanceof JavaFile.Clazz) {
			writeClass((JavaFile.Clazz)e);
		} else {
			throw new RuntimeException("Invalid statement encountered: "
					+ e.getClass());
		}
	}
	
	protected void writeBlock(Stmt.Block block) {		
		write("{");
		for(Stmt s : block.statements()) {
			writeStatement(s);		
			if(s instanceof Stmt.Simple) {
				write(";");
			}
		}
		write("}");
	}
	
	protected void writeSynchronisedBlock(Stmt.SynchronisedBlock block) {
		
		write("synchronized(");
		writeExpression(block.expr());
		write(")");
		write("{");
		for(Stmt s : block.statements()) {
			writeStatement(s);
			if(s instanceof Stmt.Simple) {
				write(";");
			}
		}
		write("}");
	}
	
	protected void writeTryCatchBlock(Stmt.TryCatchBlock block) {
		
		write("try ");		
		write("{");
		for(Stmt s : block.statements()) {
			writeStatement(s);
			if(s instanceof Stmt.Simple) {
				write(";");
			}
		}
		write("}");
		
		for(Stmt.CatchBlock c : block.handlers()) {
			write(" catch(");
			writeType(c.type());
			write(" ");
			write(c.variable());
			write(")");
			write("{");
			for(Stmt s : c.statements()) {
				writeStatement(s);
				if(s instanceof Stmt.Simple) {
					write(";");
				}
			}	
			 write("}");
		}
		
		if(block.finaly() != null) {
			write(" finally");
			writeBlock(block.finaly());
		}
	}
	
	protected void writeVarDef(Stmt.VarDef def) {
		writeModifiers(def.modifiers());
		writeType(def.type());
		write(" ");
		boolean firstTime=true;
		for(Triple<String,Integer,Expr> d : def.definitions()) {
			if(!firstTime) {
				write(", ");				
			}
			firstTime=false;
			write(d.first());
			for(int i=0;i!=d.second();++i) {
				write("[]");
			}
			if(d.third() != null) {
				write(" = ");
				writeExpression(d.third());
			}
		}
	}
	
	protected void writeAssignment(Stmt.Assignment def) {
		
		writeExpression(def.lhs());
		write(" = ");
		writeExpression(def.rhs());		
	}
	
	protected void writeReturn(Stmt.Return ret) {
				
		write("return");
		if(ret.expr() != null) {
			write(" ");
			writeExpression(ret.expr());
		}
	}
	
	protected void writeThrow(Stmt.Throw ret) {
				
		write("throw ");				
		writeExpression(ret.expr());		
	}
	
	protected void writeAssert(Stmt.Assert ret) {
				
		write("assert ");				
		writeExpression(ret.expr());				
	}
	
	protected void writeBreak(Stmt.Break brk) {
				
		write("break");
		if(brk.label() != null) {
			write(" ");
			write(brk.label());
		}		
	}
	
	protected void writeContinue(Stmt.Continue brk) {
				
		write("continue");
		if(brk.label() != null) {
			write(" ");
			write(brk.label());
		}		
	}
	
	protected void writeLabel(Stmt.Label lab) {				
		write(lab.label());
		write(": ");
		writeStatement(lab.statement());
	}
	
	protected void writeIf(Stmt.If stmt) {
		
		write("if(");
		writeExpression(stmt.condition());
		write(") ");
		writeSubStatement(stmt.trueStatement());		
		
		if (stmt.falseStatement() != null) {
			if(!(stmt.trueStatement() instanceof Stmt.Block)) {
				write("\n");
			}
			write("else");	
			writeSubStatement(stmt.falseStatement());			
		}
	}
	
	protected void writeWhile(Stmt.While stmt) {
		write("while(");
		writeExpression(stmt.condition());
		write(") ");
		if (stmt.body() != null) {
			writeSubStatement(stmt.body());			
		} else {
			write(";");
		}		
	}
	
	protected void writeDoWhile(Stmt.DoWhile stmt) {
		
		write("do ");
		writeSubStatement(stmt.body());
		write("while(");
		writeExpression(stmt.condition());
		write(");");
	}
	
	protected void writeFor(Stmt.For stmt) {
		
		write("for(");
		if(stmt.initialiser() != null) {
			writeStatement(stmt.initialiser());
		} 
		
		write("; ");
				
		if(stmt.condition() != null) {
			writeExpression(stmt.condition());
		} 
		
		write("; ");
		
		if(stmt.increment() != null) {
			writeStatement(stmt.increment());
		}
		write(")");
		
		if(stmt.body() != null) {
			writeSubStatement(stmt.body());					
		} else {
			write(";");
		}
	}
	
	protected void writeForEach(Stmt.ForEach stmt) {
		write("for(");
		writeModifiers(stmt.modifiers());
		write(" ");
		writeType(stmt.type());
		write(" ");
		write(stmt.var());
		write(" : ");
		writeExpression(stmt.source());
		write(")");
		
		if (stmt.body() != null) {
			writeSubStatement(stmt.body());			
		} else {
			write(";");
		}
	}
	
	protected void writeSwitch(Stmt.Switch s) {
		write("switch(");
		writeExpression(s.condition());
		write(")");
		write("{");
		for(Stmt.Case c : s.cases()) {
			if(c instanceof Stmt.DefaultCase) {
				write("default:");
				write("\n");
			} else {
				write("case ");
				writeExpression(c.condition());
				write(":");
				write("\n");
			}			
			for(Stmt x : c.statements()) {
				writeStatement(x);
				if(x instanceof Stmt.Simple) {
					write(";");
				}
			}
		}
		write("}");
	}
	
	protected void writeExpression(Expr e) {
		
		if(e instanceof Value.BoolVal) {
			writeBoolVal((Value.BoolVal)e);
		} else if(e instanceof Value.CharVal) {
			writeCharVal((Value.CharVal)e);
		} else if(e instanceof Value.IntVal) {
			writeIntVal((Value.IntVal)e);
		} else if(e instanceof Value.LongVal) {
			writeLongVal((Value.LongVal)e);
		} else if(e instanceof Value.FloatVal) {
			writeFloatVal((Value.FloatVal)e);
		} else if(e instanceof Value.DoubleVal) {
			writeDoubleVal((Value.DoubleVal)e);
		} else if(e instanceof Value.StringVal) {
			writeStringVal((Value.StringVal)e);
		} else if(e instanceof Value.NullVal) {
			writeNullVal((Value.NullVal)e);
		} else if(e instanceof Value.TypedArrayVal) {
			writeTypedArrayVal((Value.TypedArrayVal)e);
		} else if(e instanceof Value.ArrayVal) {
			writeArrayVal((Value.ArrayVal)e);
		} else if(e instanceof Value.ClassVal) {
			writeClassVal((Value.ClassVal) e);
		} else if(e instanceof Expr.Variable) {
			writeVariable((Expr.Variable)e);
		} else if(e instanceof Expr.UnOp) {
			writeUnOp((Expr.UnOp)e);
		} else if(e instanceof Expr.BinOp) {
			writeBinOp((Expr.BinOp)e);
		} else if(e instanceof Expr.TernOp) {
			writeTernOp((Expr.TernOp)e);
		} else if(e instanceof Expr.Cast) {
			writeCast((Expr.Cast)e);
		} else if(e instanceof Expr.InstanceOf) {
			writeInstanceOf((Expr.InstanceOf)e);
		} else if(e instanceof Expr.Invoke) {
			writeInvoke((Expr.Invoke) e);
		} else if(e instanceof Expr.New) {
			writeNew((Expr.New) e);
		} else if(e instanceof Expr.ArrayIndex) {
			writeArrayIndex((Expr.ArrayIndex) e);
		} else if(e instanceof Expr.Deref) {
			writeDeref((Expr.Deref) e);
		} else if(e instanceof Stmt.Assignment) {
			// force brackets
			write("(");
			writeAssignment((Stmt.Assignment) e);
			write(")");
		} else {
			throw new RuntimeException("Invalid expression encountered: "
					+ e.getClass());
		}
	}
	
	protected void writeDeref(Expr.Deref e) {
		writeExpressionWithBracketsIfNecessary(e.target());
		write(".");
		write(e.name());		
	}
	
	protected void writeArrayIndex(Expr.ArrayIndex e) {
		writeExpression(e.target());
		write("[");
		writeExpression(e.index());
		write("]");
	}
	
	protected void writeNew(Expr.New e) {
		if(e.context() != null) {
			writeExpressionWithBracketsIfNecessary(e.context());
			write(".");
		}
		write("new ");
		if(e.type() instanceof Type.Array) {
			// array initialiser
			List<Expr> ps = e.parameters();
			Type type =  e.type();
			int dims = 0;
			
			while(type instanceof Type.Array) {
				type = ((Type.Array)type).element();
				dims++;
			}
			
			writeType(type);
								
			for(int i=0;i!=dims;++i) {
				if(i < ps.size()) {
					write("[");
					writeExpression(ps.get(i));
					write("]");
				} else {
					write("[]");
				}
			}
		} else {
			writeType(e.type());
			write("(");
			boolean firstTime=true;
			for(Expr i : e.parameters()) {
				if(!firstTime) {
					write(", ");
				} else {
					firstTime = false;
				}
				writeExpression(i);
			}
			write(")");

			if(e.declarations().size() > 0) {
				write(" { ");
				for(JavaFile.Declaration d : e.declarations()) {				
					if(d instanceof JavaFile.Clazz) {
						writeClass((JavaFile.Clazz) d);
					} else if(d instanceof JavaFile.Field) {
						writeField((JavaFile.Field) d);
					} else if(d instanceof JavaFile.Method) {
						writeMethod((JavaFile.Method) d);
					} else {					
						throw new RuntimeException(
						"Support required for methods in anonymous inner classes");
					}
				}
				write(" } ");
			}
		}
	}
	
	protected void writeInvoke(Expr.Invoke e) {
		if(e.target() != null) {
			writeExpressionWithBracketsIfNecessary(e.target());
			write(".");
		}		
		if(e.typeParameters().size() > 0) {
			write("<");
			boolean firstTime=true;
			for(Type i : e.typeParameters()) {
				if(!firstTime) {
					write(",");
				} else {
					firstTime = false;
				}
				writeType(i);
			}	
			write(">");
		}
		write(e.name());
		write("(");
		boolean firstTime=true;
		for(Expr i : e.parameters()) {
			if(!firstTime) {
				write(", ");
			} else {
				firstTime = false;
			}
			writeExpression(i);
		}
		write(")");
	}
	
	protected void writeInstanceOf(Expr.InstanceOf e) {		
		writeExpressionWithBracketsIfNecessary(e.lhs());
		write(" instanceof ");
		writeType(e.rhs());		
	}
	
	protected void writeCast(Expr.Cast e) {
		write("(");
		writeType(e.type());
		write(") ");
		writeExpressionWithBracketsIfNecessary(e.expr());
	}
	
	protected void writeBoolVal(Value.BoolVal e) {
		if(e.value()) {
			write("true");
		} else {
			write("false");
		}
	}
	
	protected void writeCharVal(Value.CharVal e) {
		write("'");
		writeWithEscapes(Character.toString(e.value()));		
		write("'");
	}
	
	protected void writeIntVal(Value.IntVal e) {		
		write(Integer.toString(e.value()));
	}
	
	protected void writeLongVal(Value.LongVal e) {		
		write(Long.toString(e.value()) + "L");
	}
	
	protected void writeFloatVal(Value.FloatVal e) {		
		write(Float.toString(e.value()) + "F");
	}
	
	protected void writeDoubleVal(Value.DoubleVal e) {		
		write(Double.toString(e.value()));
	}
	
	protected void writeStringVal(Value.StringVal e) {		
		write("\"");
		writeWithEscapes(e.value());
		write("\"");
	}
	
	protected void writeNullVal(Value.NullVal e) {		
		write("null");
	}
	
	protected void writeTypedArrayVal(Value.TypedArrayVal e) {		
		boolean firstTime = true;
		write("new ");
		writeType(e.type());
		output.write("{");		
		for(Expr i : e.values()) {
			if(!firstTime) {
				write(", ");
			} else {
				firstTime = false;
			}
			writeExpression(i);
		}
		output.write("}");
	}
	
	protected void writeArrayVal(Value.ArrayVal e) {		
		boolean firstTime = true;
		write("{");
		for(Expr i : e.values()) {
			if(!firstTime) {
				write(", ");
			} else {
				firstTime = false;
			}
			writeExpression(i);
		}
		write("}");
	}
	
	protected void writeClassVal(Value.ClassVal e) {
		writeType(e.value());
		write(".class");
	}
	
	protected void writeVariable(Expr.Variable e) {			
		write(e.value());		
	}
	
	public static final String[] unopstr={"!","~","-","++","--","++","--"};
	
	protected void writeUnOp(Expr.UnOp e) {		
		
		if(e.op() != Expr.UnOp.POSTDEC && e.op() != Expr.UnOp.POSTINC) {
			write(unopstr[e.op()]);
		}
				
		writeExpressionWithBracketsIfNecessary(e.expr());					
		
		if (e.op() == Expr.UnOp.POSTDEC || e.op() == Expr.UnOp.POSTINC) {
			write(unopstr[e.op()]);
		}
	}
	

	protected static final String[] binopstr = {"+", "-", "*", "/", "%", "<<",
			">>", ">>>", "&", "|", "^", "<", "<=", ">", ">=", "==", "!=", "&&",
			"||", "++"};
	
	
	protected void writeBinOp(Expr.BinOp e) {				
		writeExpressionWithBracketsIfNecessary(e.lhs());					
		write(" ");
		write(binopstr[e.op()]);
		write(" ");
		writeExpressionWithBracketsIfNecessary(e.rhs());						
	}
	
	protected void writeTernOp(Expr.TernOp e) {		
		writeExpressionWithBracketsIfNecessary(e.condition());		
		write(" ? ");
		writeExpressionWithBracketsIfNecessary(e.trueBranch());
		write(" : ");
		writeExpressionWithBracketsIfNecessary(e.falseBranch());
	}
	
	protected void writeExpressionWithBracketsIfNecessary(Expr e) {
		if (e instanceof Expr.BinOp || e instanceof Expr.InstanceOf
				|| e instanceof Expr.TernOp || e instanceof Expr.Cast) {
			write("(");
			writeExpression(e);
			write(")");
		} else {
			writeExpression(e);
		}
	}
	
	protected void writeType(Type t) {
		if(t instanceof Type.Clazz) {
			writeClassType((Type.Clazz)t);
		} else if(t instanceof Type.Array) {
			writeArrayType((Type.Array)t);
		} else if(t instanceof Type.Wildcard) {
			writeWildcardType((Type.Wildcard)t);
		} else if(t instanceof Type.Variable) {
			writeVariableType((Type.Variable)t);
		}
	}
	
	protected void writeArrayType(Type.Array at) {
		writeType(at.element());
		write("[]");
	}
	
	protected void writeWildcardType(Type.Wildcard wt) {
		write("?");
		
		if(wt.lowerBound() != null) {
			write(" extends ");
			writeType(wt.lowerBound());
		} else if(wt.upperBound() != null) {
			write(" super ");
			writeType(wt.upperBound());
		}		
	}
	
	protected void writeVariableType(Type.Variable vt) {
		write(vt.variable());
		
		if(vt.lowerBounds().size() > 0) {
			write(" extends ");
			boolean firstTime = true;
			for(Type lb : vt.lowerBounds()) {
				if(!firstTime) {
					write(" & ");
				}
				firstTime=false;
				writeType(lb);	
			}			
		} 	
	}
	
	protected void writeClassType(Type.Clazz t) {		
		boolean firstTime=true;
		for(Pair<String,List<Type>> c : t.components()) {
			if(!firstTime) {
				write(".");
			} else {
				firstTime=false;
			}
			write(c.first());			
			if (!c.second().isEmpty()) {
				// yes, there are generic parameters as well.
				write("<");
				firstTime = true;
				for (Type d : c.second()) {
					if (!firstTime) {
						write(",");
					} else {
						firstTime = false;
					}
					writeType(d);
				}
				write(">");
			}
		}			
	}
	
	protected void writeModifiers(List<Modifier> modifiers) {
		for(Modifier x : modifiers) {
			if(x instanceof Modifier.Base) {
				int mod = ((Modifier.Base)x).modifier();
				if((mod & java.lang.reflect.Modifier.PRIVATE)!=0) { write("private "); }
				if((mod & java.lang.reflect.Modifier.PROTECTED)!=0) { write("protected "); }
				if((mod & java.lang.reflect.Modifier.PUBLIC)!=0) { write("public "); }
				if((mod & java.lang.reflect.Modifier.STATIC)!=0) { write("static "); }
				if((mod & java.lang.reflect.Modifier.ABSTRACT)!=0) { write("abstract "); }
				if((mod & java.lang.reflect.Modifier.FINAL)!=0) { write("final "); }
				if((mod & java.lang.reflect.Modifier.NATIVE)!=0) { write("native "); }				
				if((mod & java.lang.reflect.Modifier.STRICT)!=0) { write("strictfp "); }
				if((mod & java.lang.reflect.Modifier.SYNCHRONIZED)!=0) { write("synchronized "); }
				if((mod & java.lang.reflect.Modifier.TRANSIENT)!=0) { write("transient "); }
				if((mod & java.lang.reflect.Modifier.VOLATILE)!=0) { write("volatile "); }
			} else if(x instanceof Modifier.Annotation){
				Modifier.Annotation a = (Modifier.Annotation) x;
				write("@");
				write(a.name());
				
				if(a.arguments().size() > 0) {
					write("(");
					boolean firstTime=true;
					for(Expr e : a.arguments()) {
						if(!firstTime) {
							write(",");
						}
						firstTime=false;
						if(e instanceof Stmt.Assignment) {
							writeStatement((Stmt.Assignment)e);
						} else {
							writeExpression(e);
						}
					}
					write(")");
				}
				
				write(" ");
			} else {
				// do nothing
			}
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
	
	protected void write(String s) {		
		if(s.equals(";")) {
			output.println(";");
			indent(depth);
		} else if(s.equals("{")) {
			output.println(" {");
			depth = depth + 1;
			indent(depth);
		} else if(s.equals("}")) {
			output.println("}");
			depth = depth - 1;
			indent(depth);
		} else if(s.equals("\n")) {
			output.print("\n");
			indent(depth);
		} else {
			output.write(s);
		}
	}
	
	protected void indent(int level) {
		for(int i=0;i<level;++i) {
			output.print("  ");
		}
	}
}
