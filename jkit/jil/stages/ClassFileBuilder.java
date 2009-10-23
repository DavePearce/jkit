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

package jkit.jil.stages;

import java.util.*;

import jkit.bytecode.*;
import jkit.bytecode.Bytecode.*;
import jkit.bytecode.attributes.*;
import jkit.compiler.Clazz;
import jkit.compiler.ClassLoader;
import jkit.compiler.FieldNotFoundException;
import jkit.compiler.MethodNotFoundException;
import jkit.jil.util.Types;
import jkit.jil.tree.*;
import jkit.jil.util.Exprs;
import jkit.util.Pair;
import jkit.util.Triple;
import static jkit.compiler.SyntaxError.*;

public final class ClassFileBuilder {
	protected final ClassLoader loader;
	protected final int version;
	
	public ClassFileBuilder(ClassLoader loader, int version) {
		this.loader = loader;
		this.version = version;
	}
	
	public ClassFile build(jkit.jil.tree.JilClass clazz) {				
		ClassFile cfile = new ClassFile(version, clazz.type(), clazz
				.superClass(), clazz.interfaces(), filterModifiers(clazz.modifiers()), clazz
				.attributes(BytecodeAttribute.class));				
		
		if (needClassSignature(clazz)) {
			cfile.attributes().add(
					new ClassSignature(clazz.type(), clazz.superClass(), clazz
							.interfaces()));
		}
		
		List<Modifier.Annotation> annotations = filterAnnotations(clazz.modifiers());
		if(annotations.size() > 0) {				
			cfile.attributes().add(new RuntimeVisibleAnnotations(annotations));
		}
		
		buildInnerClasses(clazz,cfile);
		buildFields(clazz,cfile);
		buildMethods(clazz,cfile);		
		
		return cfile;
	}
	
	protected void buildInnerClasses(JilClass clazz, ClassFile cfile) {
		if(clazz.isInnerClass() || !clazz.inners().isEmpty()) {			
			// this is basically about building the inner classes attribute
			ArrayList<Triple<Type.Clazz,Type.Clazz,List<Modifier>>> inners = new ArrayList();			

			Type.Clazz inner = clazz.type();
			List<Pair<String,List<Type.Reference>>> components = inner.components();
			
			for(int i=components.size()-1;i>=0;--i) {
				List<Pair<String,List<Type.Reference>>> ncomponents = components.subList(0,i);
				Type.Clazz ref = new Type.Clazz(inner.pkg(),ncomponents);
				try {
					Clazz ic = loader.loadClass(ref);
					inners.add(new Triple(ic.type(),inner,filterModifiers(clazz.modifiers())));
				} catch(ClassNotFoundException e) {
					// this is a problem, but for now we'll just ignore it
				}
			}			
			
			for(Type.Clazz tc : clazz.inners()) {
				try {
					Clazz ic = loader.loadClass(tc);
					inners.add(new Triple(inner,ic.type(),filterModifiers(ic.modifiers())));
				} catch(ClassNotFoundException e) {
					// this is a problem, but for now we'll just ignore it
				}
			}
			
			cfile.attributes().add(new InnerClasses(clazz.type(),inners));
		}
	}		
	
	protected void buildFields(JilClass clazz, ClassFile cfile) {
		for (JilField f : clazz.fields()) {
			ClassFile.Field cf = new ClassFile.Field(f.name(), f.type(), filterModifiers(f.modifiers())); 
			cfile.fields().add(cf);
			if(Types.isGeneric(f.type())) {
				cf.attributes().add(new FieldSignature(f.type()));
			} 
			if(f instanceof JilConstant) {
				JilConstant c = (JilConstant) f;
				cf.attributes().add(new ConstantValue(c.constant()));
			}
			List<Modifier.Annotation> annotations = filterAnnotations(f.modifiers());
			if(annotations.size() > 0) {				
				cf.attributes().add(new RuntimeVisibleAnnotations(annotations));
			}
		}
	}
	
	protected void buildMethods(JilClass clazz, ClassFile cfile) {
		for (JilMethod m : clazz.methods()) {
			String m_name = m.name();
			if(m_name.equals(clazz.name())) {
				// this is a constructor call, so we need to use a different
				// name.
				m_name = "<init>";
			}
			ClassFile.Method cfm = new ClassFile.Method(m_name, m.type(), filterModifiers(m
					.modifiers()));
			
			if (clazz.isInterface() && !cfm.isPublic()) {
				// interfaces cannot have non-public methods in the bytecode.
				cfm.modifiers().add(
						Modifier.ACC_PUBLIC);				
			}
			
			if (clazz.isInterface() && !cfm.isAbstract()) {
				// interfaces cannot have non-abstract methods in the bytecode.
				cfm.modifiers().add(
						Modifier.ACC_ABSTRACT);				
			}
			
			if(!m.exceptions().isEmpty()) {
				cfm.attributes().add(new Exceptions(m.exceptions()));
			}
			
			if(!m.isAbstract() && !clazz.isInterface()) {
				ArrayList<Bytecode> bytecodes = new ArrayList<Bytecode>();
				ArrayList<Code.Handler> handlers = new ArrayList<Code.Handler>();				
				ArrayList<LineNumberTable.Entry> lines = new ArrayList<LineNumberTable.Entry>();
				
				translateCode(clazz, m, bytecodes, handlers, lines);
				Code code = new Code(bytecodes,handlers,cfm);
				if(!lines.isEmpty()) {
					code.attributes().add(new LineNumberTable(lines));
				}
				cfm.attributes().add(code);									
			}
						
			if (Types.isGeneric(m.type())) {				
				cfm.attributes().add(new MethodSignature(m.type()));
			}			
			
			List<Modifier.Annotation> annotations = filterAnnotations(m.modifiers());
			if(annotations.size() > 0) {				
				cfm.attributes().add(new RuntimeVisibleAnnotations(annotations));
			}
			
			cfile.methods().add(cfm);
		}
	}
	
	/**
	 * This method translates a method's control-flow graph into a sequence of
	 * bytecodes. Any missing constant pool items are added to the constant
	 * pool.
	 * 
	 * @param clazz
	 *            Enclosing class
	 * @param method
	 *            Method whose code block is to be translated
	 * @param constantPool
	 *            Required constant Pool items are placed into this
	 * @param bytecodes
	 *            Bytecodes generated for this method will be appended to this
	 * @return maximum size of stack required
	 * @throws ClassNotFoundException,
	 *             MethodNotFoundException, FieldNotFoundException If it needs
	 *             to access a Class which cannot be found.
	 */
	protected void translateCode(JilClass clazz, JilMethod method,
			ArrayList<Bytecode> bytecodes, ArrayList<Code.Handler> handlers,
			ArrayList<LineNumberTable.Entry> lines) {
		// === CREATE TYPE ENVIRONMENT ===

		// create the local variable slot mapping
		HashMap<String, Integer> localVarMap = new HashMap<String, Integer>();		

		int maxLocals = 0;

		if (!method.isStatic()) {			
			// observe that "super" and "this" are actually aliases from a
			// bytecode generation point of view.
			localVarMap.put("this", maxLocals);
			localVarMap.put("super", maxLocals++);			
		}

		// determine slot allocations for parameters
		List<Type> paramTypes = method.type().parameterTypes();
		int idx = 0;
		for (JilMethod.Parameter pp : method.parameters()) {
			localVarMap.put(pp.name(), maxLocals);
			maxLocals += ClassFile.slotSize(paramTypes.get(idx++));
		}
		
		// determine slot allocations for local variables. 		
		for (Pair<String,Boolean> p : method.localVariables()) {
			localVarMap.put(p.first(), maxLocals);
			maxLocals ++;
			// The following represents a conservative assumption regarding
			// slotSize which will not be optimal is some cases. For example,
			// when a variable with the same name is used as a variable of type
			// long, and elsewhere used as a variable of type e.g. int.
			if(p.second()) {
				maxLocals ++;
			}
		}

		// === TRANSLATE BYTECODES ===		
		for(JilStmt s : method.body()) {
			SourceLocation loc = s.attribute(SourceLocation.class);
			int start = bytecodes.size();
			if(loc != null) {				
				lines.add(new LineNumberTable.Entry(start,loc.line()));
			}
			translateStatement(s,localVarMap,bytecodes);
			// add exception handlers (if present)
			for(Pair<Type.Clazz,String> c : s.exceptions()) {
				Code.Handler handler = new Code.Handler(start, bytecodes.size(), c
						.second(), c.first());
				handlers.add(handler);
			}
		}
		
		// At this point, add a return statement (if there is none, and we're
		// returning void)
		if (method.type().returnType() instanceof Type.Void
				&& (bytecodes.isEmpty() || !(bytecodes
						.get(bytecodes.size() - 1) instanceof Bytecode.Return))) {
			bytecodes.add(new Bytecode.Return(null));
		}
		
		// Now make sure the exception handlers are compacted and
		// also arranged in the correct order.
		// sortAndCompactExceptionHandlers(handlers);
	}
	
	/**
	 * This method aims to sort compact handlers that are next to each other
	 * together. For example, if we have the following handlers:
	 * 
	 * <p>
	 * <table border=1>
	 * <tr>
	 * <td>from</td>
	 * <td>to</td>
	 * <td>handler</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td>3</td>
	 * <td>RuntimeException</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td>3</td>
	 * <td>ArrayIndexOutOfBoundsException</td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td>6</td>
	 * <td>RuntimeException</td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td>6</td>
	 * <td>ArrayIndexOutOfBoundsException</td>
	 * </tr>
	 * </table>
	 * </p>
	 * <br>
	 * 
	 * Then we need to compact the four handlers into two and reorder them as
	 * follows:
	 * 
	 * <p>
	 * <table border=1>
	 * <tr>
	 * <td>from</td>
	 * <td>to</td>
	 * <td>handler</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td>6</td>
	 * <td>ArrayIndexOutOfBoundsException</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td>6</td>
	 * <td>RuntimeException</td>
	 * </tr>
	 * </table>
	 * </p>
	 * <br>
	 * 
	 * Observe here that the order of exceptions matters in the classfile. If
	 * the RuntimeException were to come first, then it would prevent the
	 * ArrayIndexOutOfBoundsException from ever being called. Note that, had the
	 * user actually specified this to happen in the source code then we
	 * wouldn't even see the ArrayIndexOutOfBoundsException handler here.
	 * 
	 * @param handlers.
	 */
	protected void sortAndCompactExceptionHandlers(
			ArrayList<Code.Handler> handlers) {

		// NOTE: this code does not work correctly because the sorting function
		// disturbs the correct order of exceptions.
		
		// firstly, sort them into the correct order
		Collections.sort(handlers, new Comparator<Code.Handler>() {
			public int compare(Code.Handler e1, Code.Handler e2) {
				int ct = e1.exception.compareTo(e2.exception); 
				if (ct == 0) {
					if (e1.start < e2.start) {
						return -1;
					} else if (e1.start > e2.start) {
						return 1;
					} else {
						return 0;
					}
				} else {
					return ct;
				}
			}
		});

		// now, we compact them together.
		ArrayList<Code.Handler> oldhandlers = new ArrayList<Code.Handler>(
				handlers);
		handlers.clear();

		for (int i = 0; i != oldhandlers.size();) {
			Code.Handler handler = oldhandlers.get(i);
			int end = handler.end;
			Code.Handler tmp;
			i = i + 1;
			while (i < oldhandlers.size()
					&& (tmp = oldhandlers.get(i)).start == (end + 1)
					&& tmp.label == handler.label
					&& tmp.exception.equals(handler.exception)) {
				end = tmp.end;
				i = i + 1;
			}
			tmp = new Code.Handler(handler.start, end, handler.label,
					handler.exception);
			handlers.add(tmp);
		}
	}
	
	/**
	 * This method is responsible for translating a statement from the
	 * intermediate language into Java Bytecode(s).
	 * 
	 * @param stmt
	 *            the statement to be translated
	 * @param bytecodes
	 *            Java bytecodes representing statement appended onto this
	 * @param varmap
	 *            maps local variable names to their slot numbers.
	 */
	protected void translateStatement(JilStmt stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {
		try {
			if (stmt instanceof JilStmt.Return) {
				translateReturn((JilStmt.Return) stmt, varmap,
						bytecodes);
			} else if (stmt instanceof JilStmt.Assign) {
				translateAssign((JilStmt.Assign) stmt, varmap,
						bytecodes);
			} else if (stmt instanceof JilExpr.Invoke) {
				translateInvoke((JilExpr.Invoke) stmt, varmap,
						bytecodes, false);
			} else if (stmt instanceof JilExpr.New) {
				translateNew((JilExpr.New) stmt, varmap, bytecodes,
						false);
			} else if (stmt instanceof JilStmt.Nop) {
				bytecodes.add(new Bytecode.Nop());
			} else if (stmt instanceof JilStmt.Throw) {
				translateThrow((JilStmt.Throw) stmt, varmap, bytecodes);
			} else if (stmt instanceof JilStmt.Lock) {
				translateLock((JilStmt.Lock) stmt, varmap, bytecodes);
			} else if (stmt instanceof JilStmt.Unlock) {
				translateUnlock((JilStmt.Unlock) stmt, varmap,
						bytecodes);
			} else if(stmt instanceof JilStmt.Label) {
				translateLabel((JilStmt.Label)stmt,varmap,bytecodes);
			} else if(stmt instanceof JilStmt.IfGoto) {
				translateIfGoto((JilStmt.IfGoto)stmt,varmap,bytecodes);
			} else if(stmt instanceof JilStmt.Goto) {
				translateGoto((JilStmt.Goto)stmt,varmap,bytecodes);
			} else if(stmt instanceof JilStmt.Switch) {
				translateSwitch((JilStmt.Switch)stmt,varmap,bytecodes);
			} else {
				throw new RuntimeException("Unknown statement encountered: " + stmt);
			}
		} catch(Exception ex) {
			internal_error(stmt,ex);
		}
	}


	protected void translateIfGoto(JilStmt.IfGoto stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) throws ClassNotFoundException, MethodNotFoundException {
		translateConditionalBranch(stmt.condition(), stmt.label(), varmap,
				bytecodes);
	}

	/**
	 * This method is responsible for translating a conditional expression, such
	 * as if(x < 0) etc.
	 * 
	 * @param condition
	 *            the condition being tested
	 * @param trueLabel
	 *            the destination if the condition is true
	 * @param varmap
	 *            Maps local variables to their slot number
	 * @param bytecodes
	 *            bytecodes representing this statement are appended onto this
	 */
	protected static int condLabelCount = 0;
	protected void translateConditionalBranch(JilExpr condition, String trueLabel,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) throws ClassNotFoundException, MethodNotFoundException {
		
		if (condition instanceof JilExpr.BinOp) {
			JilExpr.BinOp bop = (JilExpr.BinOp) condition;

			switch (bop.op()) {
			case JilExpr.BinOp.LAND: {
				String exitLabel = "CL" + condLabelCount++;
				translateConditionalBranch(Exprs.invertBoolean(bop
						.lhs()), exitLabel, varmap, bytecodes);
				translateConditionalBranch(Exprs.invertBoolean(bop
						.rhs()), exitLabel, varmap, bytecodes);
				bytecodes.add(new Bytecode.Goto(trueLabel));
				bytecodes.add(new Bytecode.Label(exitLabel));
				return;
			}
			case JilExpr.BinOp.LOR: {
				translateConditionalBranch(bop.lhs(), trueLabel,
						varmap, bytecodes);
				translateConditionalBranch(bop.rhs(), trueLabel,
						varmap, bytecodes);
				return;
			}
			}

			// There are a number of optimisations which could be applied
			// here. For example, using ifnull and ifnotnull bytecodes. Also,
			// using ifeq when comparing directly against zero.

			translateExpression(bop.lhs(), varmap, 
					bytecodes);

			translateExpression(bop.rhs(), varmap, 
					bytecodes);

			Type cmpT = bop.lhs().type();
			int code = -1;
			switch (bop.op()) {
			case JilExpr.BinOp.EQ:
				code = Bytecode.IfCmp.EQ;
				break;
			case JilExpr.BinOp.NEQ:
				code = Bytecode.IfCmp.NE;
				break;
			case JilExpr.BinOp.LT:
				code = Bytecode.IfCmp.LT;
				break;
			case JilExpr.BinOp.GT:
				code = Bytecode.IfCmp.GT;
				break;
			case JilExpr.BinOp.LTEQ:
				code = Bytecode.IfCmp.LE;
				break;
			case JilExpr.BinOp.GTEQ:
				code = Bytecode.IfCmp.GE;
				break;
			}
			if (cmpT instanceof Type.Double || cmpT instanceof Type.Float
					|| cmpT instanceof Type.Long) {
				bytecodes.add(new Bytecode.Cmp(cmpT, Bytecode.Cmp.LT));
				bytecodes.add(new Bytecode.If(code, trueLabel));
			} else {
				bytecodes.add(new Bytecode.IfCmp(code, cmpT, trueLabel));
			}
		} else if (condition instanceof JilExpr.UnOp) {
			JilExpr.UnOp uop = (JilExpr.UnOp) condition;
			if (uop.op() == JilExpr.UnOp.NOT) {
				// First attempt to eliminate the NOT altogether!
				JilExpr e1 = Exprs.eliminateNot(uop);

				if (e1 instanceof JilExpr.UnOp) {
					JilExpr.UnOp e2 = (JilExpr.UnOp) e1;
					if (e2.op() == JilExpr.UnOp.NOT) {
						// not elimination was unsuccessful
						translateExpression(uop.expr(), varmap,
								bytecodes);
						bytecodes
								.add(new Bytecode.If(Bytecode.If.EQ, trueLabel));						
					}
				} else {
					// not elimination was successful ...
					translateConditionalBranch(e1, trueLabel, varmap, bytecodes);
				}
			} else {
				// anything else doesn't make sense
				throw new RuntimeException(
						"Invalid use of unary operator in conditional ("
						+ condition + ")");
			}
		} else if (condition instanceof JilExpr.Invoke) {
			translateInvoke((JilExpr.Invoke) condition, varmap, 
					bytecodes, true);
			bytecodes.add(new Bytecode.If(Bytecode.If.NE, trueLabel));			
		} else if (condition instanceof JilExpr.InstanceOf) {
			translateExpression(condition, varmap, 
					bytecodes);
			bytecodes.add(new Bytecode.If(Bytecode.If.NE, trueLabel));			
		} else if (condition instanceof JilExpr.Bool 
					|| condition instanceof JilExpr.ArrayIndex
					|| condition instanceof JilExpr.Variable
					|| condition instanceof JilExpr.Deref) {
			translateExpression(condition, varmap,
					bytecodes);
			bytecodes.add(new Bytecode.If(Bytecode.If.NE, trueLabel));			
		} else {
			throw new RuntimeException("Unknown conditional expression ("
				+ condition + ")");
		}
	}

	/**
	 * This method is responsible for translating a multi-conditional branching
	 * instruction (i.e. a switch).
	 * 
	 * @param conditions
	 *            The list of conditional expressions to translate
	 * @param labels
	 *            The corresponding destination of each conditional expression
	 * @param varmap
	 *            Maps local variables to their slot number
	 * @param bytecodes
	 *            bytecodes representing this statement are appended onto this
	 */
	protected void translateSwitch(JilExpr[] conditions, String[] labels,
			HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {

		JilExpr expr = null;
		for (JilExpr c : conditions) {
			JilExpr.BinOp cond = (JilExpr.BinOp) c;
			if (cond.op() == JilExpr.BinOp.EQ) {
				expr = cond.lhs();
				break;
			}
		}

		translateExpression(expr, varmap, bytecodes);
		
		String def = null;
		List<Pair<Integer, String>> cases = new ArrayList<Pair<Integer, String>>();
		for (int i = 0; i < conditions.length; i++) {
			JilExpr e = ((JilExpr.BinOp) conditions[i]).rhs();
			if (e instanceof JilExpr.Int) {
				int c = ((JilExpr.Int) e).value();
				String label = labels[i];
				cases.add(new Pair<Integer, String>(c, label));
			} else {
				def = labels[i];
			}
		}

		Collections.sort(cases, new Comparator<Pair<Integer, String>>() {
			public int compare(Pair<Integer, String> p1,
					Pair<Integer, String> p2) {
				return p1.first() - p2.first();
			}
		});

		bytecodes.add(new Bytecode.Switch(def, cases));
	}

	protected void translateInvoke(JilExpr.Invoke stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes,
			boolean needReturnValue) throws ClassNotFoundException, MethodNotFoundException {
		
		Type.Reference _targetT = (Type.Reference) stmt.target().type();
		
		if(_targetT instanceof Type.Array) {			
			translateInvokeHelper(new Type.Clazz("java.lang", "Object"), stmt,
					varmap, bytecodes, needReturnValue);
		} else if(_targetT instanceof Type.Variable) {			
			Type.Variable targetT = (Type.Variable) _targetT;
			
			if (targetT.lowerBound() instanceof Type.Clazz) {
				translateInvokeHelper((Type.Clazz) targetT.lowerBound(), stmt,
						varmap, bytecodes, needReturnValue);
			} else if(targetT.lowerBound() instanceof Type.Intersection) {
				// there is a somewhat awkward problem here. not sure how to
				// deal with it exactly.
				translateInvokeHelper(new Type.Clazz("java.lang", "Object"),
						stmt, varmap, bytecodes, needReturnValue);
			} else {
				translateInvokeHelper(new Type.Clazz("java.lang", "Object"),
						stmt, varmap, bytecodes, needReturnValue);
			}
		} else if(_targetT instanceof Type.Clazz) {
			translateInvokeHelper((Type.Clazz) _targetT, stmt, varmap,
					bytecodes, needReturnValue);
		}
	}
	
	protected void translateInvokeHelper(Type.Clazz targetT,
			JilExpr.Invoke stmt, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes, boolean needReturnValue) throws ClassNotFoundException, MethodNotFoundException {

		if (stmt.name().equals("super") || stmt.name().equals("this")) {
			// catch explicit super constructor call.
			translateExpression(stmt.target(), varmap, bytecodes);
			for (JilExpr p : stmt.parameters()) {
				translateExpression(p, varmap, bytecodes);
			}
			bytecodes.add(new Bytecode.Invoke(targetT, "<init>",
					stmt.funType(), Bytecode.SPECIAL));
			return;
		} 
					
		Pair<Clazz,Clazz.Method> cm = determineMethod(targetT, stmt.name(), stmt
				.funType());
		Clazz c = cm.first();
		Clazz.Method m = cm.second();

		if (!m.isStatic()) {
			// must be non-static invocation
			translateExpression(stmt.target(), varmap, bytecodes);
		}

		if(!m.isVariableArity()) {
			for(JilExpr e : stmt.parameters()) {
				translateExpression(e, varmap, bytecodes);
			}
		} else {
			// now, this is a variable-arity method --- so we need to
			// package up some arguments into an array.
			List<? extends JilExpr> arguments = stmt.parameters();
			List<Type> paramTypes = m.type().parameterTypes();

			int vargcount = stmt.parameters().size() - paramTypes.size() + 1;
			int arg = 0;
			for(;arg!=arguments.size()-vargcount;++arg) {
				JilExpr e = arguments.get(arg);
				translateExpression(e, varmap, bytecodes);
			}

			Type.Array arrType = (Type.Array) paramTypes.get(paramTypes
					.size() - 1);								

			// At this point, we need to deal with the case where the
			// element type of the array is actually a generic type.
			if (arrType.element() instanceof Type.Variable
					|| arrType.element() instanceof Type.Wildcard) {
				if ((arg + 1) == arguments.size()
						&& arguments.get(arg).type() instanceof Type.Array) {
					arrType = (Type.Array) arguments.get(arg).type();
				} else {
					arrType = new Type.Array(Types.JAVA_LANG_OBJECT);
				}
			}

			if ((arg + 1) == arguments.size()
					&& arguments.get(arg).type().equals(arrType)) {				
				// this is the special case when an appropriate array is
				// supplied directly to the variable argument list.
				translateExpression(arguments.get(arg), varmap, bytecodes);
			} else {
				bytecodes.add(new LoadConst(vargcount));
				bytecodes.add(new Bytecode.New(arrType,1));
				for(int i=0;arg!=arguments.size();++arg,++i) {
					bytecodes.add(new Bytecode.Dup(arrType));
					bytecodes.add(new LoadConst(i));
					translateExpression(arguments.get(arg), varmap, bytecodes);
					bytecodes.add(new Bytecode.ArrayStore(arrType));
				}	
			}
		}

		if (stmt instanceof JilExpr.SpecialInvoke) {
			bytecodes.add(new Bytecode.Invoke(targetT, stmt.name(), stmt
					.funType(), Bytecode.SPECIAL));
		} else if (m.isStatic()) {
			// STATIC
			bytecodes.add(new Bytecode.Invoke(targetT, stmt.name(), stmt
					.funType(), Bytecode.STATIC));
		} else if (c.isInterface()) {
			bytecodes.add(new Bytecode.Invoke(targetT, stmt.name(), stmt
					.funType(), Bytecode.INTERFACE));
		} else {
			bytecodes.add(new Bytecode.Invoke(targetT, stmt.name(), stmt
					.funType(), Bytecode.VIRTUAL));
		}

		Type retT = m.type().returnType();

		if (!(retT instanceof Type.Void)) {
			// Need to account for space occupied by return type!
			if (!needReturnValue) {
				// the return value is not required, so we need to pop
				// it from
				// the stack
				bytecodes.add(new Bytecode.Pop(retT));
			} else if ((retT instanceof Type.Variable 
					|| retT instanceof Type.Wildcard
					|| Types.isGenericArray(retT))
					&& !stmt.type().equals(
							new Type.Clazz("java.lang", "Object"))) {
				// Here, the actual return type is a (generic) type
				// variable (e.g. T or T[]), and we're expecting it to
				// return a real value (e.g. String, substituted for T).
				// This issue is
				// that, because of erasure, the returned type will be
				// Object and we need to cast it to whatever it needs to be
				// (e.g. String). Note, if the value substituted for T is
				// actually Object, then we just do nothing!
				
				retT = stmt.type();
				while(retT instanceof Type.Variable || retT instanceof Type.Wildcard) {
					if(retT instanceof Type.Variable) {
						Type.Variable vt = (Type.Variable) retT;
						
						if(vt.lowerBound() == null) {
							return; // no return value cast is required.
						} else {
							retT = vt.lowerBound(); // keep search for a concrete type!
						}
						
					} else if(retT instanceof Type.Wildcard) {
						Type.Wildcard wt = (Type.Wildcard) retT;
						if(wt.lowerBound() == null) {
							return; // no return value cast is required.
						} else {							
							retT = wt.lowerBound(); // keep search for a concrete type!
						}
					} 			
				}
				bytecodes.add(new Bytecode.CheckCast(retT));
			}
		}	
	}
		
	/**
	 * Translate a Return statement.
	 * 
	 * @param ret
	 * @param varmap
	 * @param bytecodes
	 * @return
	 */
	protected void translateReturn(JilStmt.Return ret,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {

		if (ret.expr() != null) {
			translateExpression(ret.expr(), varmap, bytecodes);
			bytecodes.add(new Bytecode.Return(ret.expr().type()));
		} else {
			bytecodes.add(new Bytecode.Return(null));
		}
	}

	protected void translateAssign(JilStmt.Assign stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {

		if (stmt.lhs() instanceof JilExpr.Variable) {
			JilExpr.Variable var = (JilExpr.Variable) stmt.lhs();
			assert varmap.keySet().contains(var.value());
			int slot = varmap.get(var.value());
			translateExpression(stmt.rhs(), varmap, bytecodes);
			bytecodes.add(new Bytecode.Store(slot, stmt.lhs().type()));
		} else if (stmt.lhs() instanceof JilExpr.Deref) {
			JilExpr.Deref der = (JilExpr.Deref) stmt.lhs();			
			// figure out the type of the field involved
			Type.Clazz lhs_t = (Type.Clazz) der.target().type();
			
			if (der.isStatic()) {
				translateExpression(stmt.rhs(), varmap, bytecodes);
				bytecodes.add(new Bytecode.PutField(lhs_t, der.name(), der
						.type(), Bytecode.STATIC));
			} else {
				translateExpression(der.target(), varmap, bytecodes);
				translateExpression(stmt.rhs(), varmap, bytecodes);
				bytecodes.add(new Bytecode.PutField(lhs_t, der.name(), der
						.type(), Bytecode.NONSTATIC));
			}
		} else if (stmt.lhs() instanceof JilExpr.ArrayIndex) {
			JilExpr.ArrayIndex ai = (JilExpr.ArrayIndex) stmt.lhs();
			translateExpression(ai.target(), varmap, bytecodes);
			translateExpression(ai.index(), varmap, bytecodes);
			translateExpression(stmt.rhs(), varmap, bytecodes);
			bytecodes.add(new Bytecode.ArrayStore((Type.Array) ai.target()
					.type()));			
		} else {
			throw new RuntimeException("Unknown lval encountered");
		}
	}

	protected void translateThrow(JilStmt.Throw stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {
		translateExpression(stmt.expr(), varmap, bytecodes);
		bytecodes.add(new Bytecode.Throw());
	}

	protected void translateLock(JilStmt.Lock stmt, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		translateExpression(stmt.expr(), varmap, bytecodes);
		bytecodes.add(new Bytecode.MonitorEnter());
	}

	protected void translateUnlock(JilStmt.Unlock stmt, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		translateExpression(stmt.expr(), varmap, bytecodes);
		bytecodes.add(new Bytecode.MonitorExit());
	}

	protected void translateLabel(JilStmt.Label label, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		bytecodes.add(new Bytecode.Label(label.label()));
	}
	
	protected void translateGoto(JilStmt.Goto stmt, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		bytecodes.add(new Bytecode.Goto(stmt.label()));
	}
	
	protected void translateSwitch(JilStmt.Switch stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {
		translateExpression(stmt.condition(), varmap, bytecodes);
		List<Pair<Integer,String>> cases = new ArrayList();
		
		for(Pair<JilExpr.Number,String> c : stmt.cases()) {
			cases.add(new Pair<Integer,String>(c.first().intValue(), c.second()));
		}
		
		bytecodes.add(new Bytecode.Switch(stmt.defaultLabel(),cases));
	}
	
	/**
	 * This method flatterns an expression into bytecodes, such that the result
	 * is left on top of the stack.
	 * 
	 * @param expr
	 * @param varmap
	 *            Map from local variables to local array slot numbers
	 * @param bytecodes
	 *            translated bytecodes are appended to this
	 * @return the maximum stack size required for this expression
	 */
	protected void translateExpression(JilExpr expr,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {

		try {
			if (expr instanceof JilExpr.Bool) {			
				bytecodes.add(new Bytecode.LoadConst(((JilExpr.Bool) expr).value()));
			} else if (expr instanceof JilExpr.Byte) {
				bytecodes.add(new Bytecode.LoadConst(((JilExpr.Byte) expr).value()));
			} else if (expr instanceof JilExpr.Char) {
				bytecodes.add(new Bytecode.LoadConst(((JilExpr.Char) expr).value()));
			} else if (expr instanceof JilExpr.Short) {
				bytecodes.add(new Bytecode.LoadConst(((JilExpr.Short) expr).value()));
			} else if (expr instanceof JilExpr.Int) {
				bytecodes.add(new Bytecode.LoadConst(((JilExpr.Int) expr).value()));
			} else if (expr instanceof JilExpr.Long) {
				bytecodes.add(new Bytecode.LoadConst(((JilExpr.Long) expr).value()));
			} else if (expr instanceof JilExpr.Float) {
				bytecodes.add(new Bytecode.LoadConst(((JilExpr.Float) expr).value()));
			} else if (expr instanceof JilExpr.Double) {
				bytecodes.add(new Bytecode.LoadConst(((JilExpr.Double) expr).value()));
			} else if (expr instanceof JilExpr.Null) {
				bytecodes.add(new Bytecode.LoadConst(null));
			} else if (expr instanceof JilExpr.StringVal) {
				bytecodes.add(new Bytecode.LoadConst(((JilExpr.StringVal) expr).value()));
			} else if (expr instanceof JilExpr.Array) {
				translateArrayVal((JilExpr.Array) expr, varmap, bytecodes);
			} else if (expr instanceof JilExpr.Class) {
				translateClassVal((JilExpr.Class) expr, varmap, bytecodes);
			} else if (expr instanceof JilExpr.Variable) {
				JilExpr.Variable lv = (JilExpr.Variable) expr;

				if (varmap.containsKey(lv.value())) {				
					bytecodes.add(new Bytecode.Load(varmap.get(lv.value()), lv.type()));				
				} else if(lv.value().equals("$")) {
					// this is the special variable used to get an Exception object
					// off the stack in an exception handler.
					//
					// In this case, we don't actually have to do anything since
					// it's already on the stack!
				} else {
					syntax_error("unknown variable \"" + lv.value() + "\"", expr);
				}
			} else if (expr instanceof JilExpr.New) {
				translateNew((JilExpr.New) expr, varmap, bytecodes,true);
			} else if (expr instanceof JilExpr.Deref) {
				translateDeref((JilExpr.Deref) expr, varmap, bytecodes);
			} else if (expr instanceof JilExpr.ArrayIndex) {
				JilExpr.ArrayIndex ai = (JilExpr.ArrayIndex) expr;
				translateExpression(ai.target(), varmap, bytecodes);
				translateExpression(ai.index(), varmap, bytecodes);
				Type arr_t = ai.target().type();
				bytecodes.add(new Bytecode.ArrayLoad((Type.Array) arr_t));			
			} else if (expr instanceof JilExpr.Invoke) {
				translateInvoke((JilExpr.Invoke) expr, varmap, bytecodes, true);
			} else if (expr instanceof JilExpr.UnOp) {
				translateUnaryOp((JilExpr.UnOp) expr, varmap,bytecodes);
			} else if (expr instanceof JilExpr.BinOp) {
				translateBinaryOp((JilExpr.BinOp) expr, varmap,bytecodes);
			} else if (expr instanceof JilExpr.InstanceOf) {
				JilExpr.InstanceOf iof = (JilExpr.InstanceOf) expr;
				translateExpression(iof.lhs(), varmap, bytecodes);
				bytecodes.add(new Bytecode.InstanceOf(iof.rhs()));
			} else if (expr instanceof JilExpr.Cast) {
				translateCast((JilExpr.Cast) expr, varmap,bytecodes);
			} else if (expr instanceof JilExpr.Convert) {
				translateConvert((JilExpr.Convert) expr, varmap,bytecodes);
			} else if (expr instanceof JilExpr.ClassVariable) {
				translateClassVariable((JilExpr.ClassVariable)expr,varmap,bytecodes);
			} else {
				throw new RuntimeException("Unknown expression encountered ("
						+ expr + ")");
			}
		} catch(Exception ex) {
			internal_error(expr,ex);
		}
	}

	public void translateClassVariable(JilExpr.ClassVariable cvar,  HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		// do nothing here.
	}
	
	public void translateClassVal(JilExpr.Class cval,  HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		if (cval.classType() instanceof Type.Primitive) {
			bytecodes.add(new Bytecode.GetField(Types
					.boxedType((Type.Primitive) cval.classType()), "TYPE",
					new Type.Clazz("java.lang", "Class"), Bytecode.STATIC));
		} else {
			bytecodes.add(new Bytecode.LoadConst(cval.classType()));
		}
	}
	
	public void translateDeref(JilExpr.Deref def, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) throws ClassNotFoundException, FieldNotFoundException {
		Type tmp_t = def.target().type();

		if (tmp_t instanceof Type.Clazz) {
			Type.Clazz lhs_t = (Type.Clazz) tmp_t;


			Type actualFieldType = determineFieldType(lhs_t,def.name());
			Type bytecodeType = actualFieldType;				

			if(actualFieldType instanceof Type.Variable) {
				Type.Variable tv = (Type.Variable) actualFieldType;
				if(tv.lowerBound() == null) {
					bytecodeType = new Type.Clazz("java.lang","Object");
				} else {
					bytecodeType = tv.lowerBound();
				}
			}

			if (def.isStatic()) {					
				// This is a static field load					
				bytecodes.add(new Bytecode.GetField(lhs_t, def.name(),
						bytecodeType, Bytecode.STATIC));				
			} else {
				// Non-static field load
				translateExpression(def.target(), varmap, bytecodes);

				bytecodes.add(new Bytecode.GetField(lhs_t, def.name(),
						bytecodeType, Bytecode.NONSTATIC));		

				if (actualFieldType instanceof Type.Variable
						&& !(def.type() instanceof Type.Variable)
						&& !def.type().equals(new Type.Clazz("java.lang",
						"Object"))) {
					// Ok, actual type is a (generic) type variable. Need to
					// cast to the desired type!
					bytecodes.add(new Bytecode.CheckCast(def.type()));					
				}		
			}					
		} else if (tmp_t instanceof Type.Array && def.name().equals("length")) {
			translateExpression(def.target(), varmap, bytecodes);
			bytecodes.add(new Bytecode.ArrayLength());
		} else {
			throw new RuntimeException(
					"Attempt to dereference variable with type "
							+ tmp_t.toString());
		}
	}

	public void translateArrayVal(JilExpr.Array av, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) throws ClassNotFoundException, MethodNotFoundException {						
		List<JilExpr> params = new ArrayList<JilExpr>();
		params.add(new JilExpr.Int(av.values().size()));
		translateNew(new JilExpr.New(av.type(), params, new Type.Function(Types.T_VOID)), varmap, bytecodes,
				true);

		int index = 0;
		
		for (JilExpr e : av.values()) {
			bytecodes.add(new Bytecode.Dup(av.type()));
			bytecodes.add(new Bytecode.LoadConst(index++));
			translateExpression(e, varmap, bytecodes);
			bytecodes.add(new Bytecode.ArrayStore((Type.Array) av.type()));			
		}
	}

	public void translateNew(JilExpr.New news, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes, boolean needReturnValue)
			throws ClassNotFoundException, MethodNotFoundException {

		if (news.type() instanceof Type.Clazz) {
			Type.Clazz type = (Type.Clazz) news.type();								
			
			bytecodes.add(new Bytecode.New(news.type()));
			bytecodes.add(new Bytecode.Dup(news.type()));

			// Now, translate the parameters.
			translateNewHelper(type,news,varmap,bytecodes);

			// call the appropriate constructor
			bytecodes.add(new Bytecode.Invoke(type, "<init>", news.funType(),
					Bytecode.SPECIAL));
		} else if (news.type() instanceof Type.Array) {
			for (JilExpr p : news.parameters()) {
				translateExpression(p, varmap, bytecodes);				
			}

			bytecodes.add(new Bytecode.New(news.type(), news.parameters()
					.size()));
		}

		if (!needReturnValue) {
			// the return value is not required, so we need to pop it from
			// the stack
			bytecodes.add(new Bytecode.Pop(news.type()));
		}
	}

	protected void translateNewHelper(Type.Clazz targetT, JilExpr.New stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes)
			throws ClassNotFoundException, MethodNotFoundException {
		// Ideally, this method should be combined with translateInvokeHelper.
		// To do this, will probably need to combine JilExpr.New and
		// JilExpr.Invoke via
		// inheritance somehow.
		
		String name = targetT.lastComponent().first();
						
		Pair<Clazz, Clazz.Method> cm = determineMethod(targetT, name, stmt
				.funType());			
		Clazz.Method m = cm.second();		

		if(!m.isVariableArity()) {
			for(JilExpr e : stmt.parameters()) {
				translateExpression(e, varmap, bytecodes);
			}
		} else {
			// now, this is a variable-arity method --- so we need to
			// package up some arguments into an array.
			List<? extends JilExpr> arguments = stmt.parameters();
			List<Type> paramTypes = m.type().parameterTypes();

			int vargcount = stmt.parameters().size() - paramTypes.size() + 1;
			int arg = 0;
			for(;arg!=arguments.size()-vargcount;++arg) {
				JilExpr e = arguments.get(arg);
				translateExpression(e, varmap, bytecodes);
			}

			Type.Array arrType = (Type.Array) paramTypes.get(paramTypes
					.size() - 1);								

			// At this point, we need to deal with the case where the
			// element type of the array is actually a generic type.
			if (arrType.element() instanceof Type.Variable
					|| arrType.element() instanceof Type.Wildcard) {
				if ((arg + 1) == arguments.size()
						&& arguments.get(arg).type() instanceof Type.Array) {
					arrType = (Type.Array) arguments.get(arg).type();
				} else {
					arrType = new Type.Array(Types.JAVA_LANG_OBJECT);
				}
			}

			if ((arg + 1) == arguments.size()
					&& arguments.get(arg).type().equals(arrType)) {				
				// this is the special case when an appropriate array is
				// supplied directly to the variable argument list.
				translateExpression(arguments.get(arg), varmap, bytecodes);
			} else {
				bytecodes.add(new LoadConst(vargcount));
				bytecodes.add(new Bytecode.New(arrType,1));
				for(int i=0;arg!=arguments.size();++arg,++i) {
					bytecodes.add(new Bytecode.Dup(arrType));
					bytecodes.add(new LoadConst(i));
					translateExpression(arguments.get(arg), varmap, bytecodes);
					bytecodes.add(new Bytecode.ArrayStore(arrType));
				}	
			}
		}
	}
	
	protected void translateBinaryOp(JilExpr.BinOp bop, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) throws ClassNotFoundException, MethodNotFoundException {

		// second, translate the binary operator.
		switch (bop.op()) {
		case JilExpr.BinOp.LT:
		case JilExpr.BinOp.LTEQ:
		case JilExpr.BinOp.GT:
		case JilExpr.BinOp.GTEQ:
		case JilExpr.BinOp.EQ:
		case JilExpr.BinOp.NEQ:
		case JilExpr.BinOp.LAND:
		case JilExpr.BinOp.LOR: {
			String trueLabel = "CL" + condLabelCount++;
			String exitLabel = "CL" + condLabelCount++;
			translateConditionalBranch(bop, trueLabel, varmap, bytecodes);
			bytecodes.add(new Bytecode.LoadConst(0));
			bytecodes.add(new Bytecode.Goto(exitLabel));
			bytecodes.add(new Bytecode.Label(trueLabel));
			bytecodes.add(new Bytecode.LoadConst(1));
			bytecodes.add(new Bytecode.Label(exitLabel));
			return;
		}
		}

		// must be a standard arithmetic operation.
		translateExpression(bop.lhs(), varmap, bytecodes);
		translateExpression(bop.rhs(), varmap, bytecodes);
		bytecodes.add(new Bytecode.BinOp(bop.op(), bop.type()));		
	}

	protected void translateUnaryOp(JilExpr.UnOp uop, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes)
			throws ClassNotFoundException, MethodNotFoundException {

		switch (uop.op()) {
		case JilExpr.UnOp.NOT:
			String trueLabel = "CL" + condLabelCount++;
			String exitLabel = "CL" + condLabelCount++;
			translateConditionalBranch(uop, trueLabel, varmap, bytecodes);
			bytecodes.add(new Bytecode.LoadConst(0));
			bytecodes.add(new Bytecode.Goto(exitLabel));
			bytecodes.add(new Bytecode.Label(trueLabel));
			bytecodes.add(new Bytecode.LoadConst(1));
			bytecodes.add(new Bytecode.Label(exitLabel));	
			return;
		}

		// first, translate the expression.
		translateExpression(uop.expr(), varmap, bytecodes);

		switch (uop.op()) {
		case JilExpr.UnOp.INV:
			bytecodes.add(new Bytecode.LoadConst(new Integer(-1)));
			bytecodes
					.add(new Bytecode.BinOp(Bytecode.BinOp.XOR, new Type.Int()));			
			break;
		case JilExpr.UnOp.NEG:
			bytecodes.add(new Bytecode.Neg(uop.type()));
			break;
		default:
			throw new RuntimeException("Unknown unary expression encountered ("
					+ uop + ", " + uop.op() + ")");
		}
	}

	protected void translateCast(JilExpr.Cast cast, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		
		translateExpression(cast.expr(), varmap, bytecodes);

		Type type = cast.type();
		Type srcType = cast.expr().type();
		// Now, do implicit conversions
		if (type instanceof Type.Primitive
				&& srcType instanceof Type.Primitive) {			
			if(!type.equals(srcType)) {
				bytecodes.add(new Bytecode.Conversion((Type.Primitive) srcType,
					(Type.Primitive) cast.type()));
			}			
		} else if (type instanceof Type.Array || type instanceof Type.Clazz) {
			bytecodes.add(new Bytecode.CheckCast(cast.type()));
		} else if (type instanceof Type.Variable) {
			Type.Variable tv = (Type.Variable) type;
			Type.Reference lb = tv.lowerBound();
			if (lb instanceof Type.Clazz) {
				// i'm not sure if this is general enough.
				bytecodes.add(new Bytecode.CheckCast(lb));
			}
		}
	}

	protected void translateConvert(JilExpr.Convert cast, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		translateExpression(cast.expr(), varmap, bytecodes);

		Type to = cast.type();
		Type from = cast.expr().type();
		// Now, do implicit conversions
		if((from instanceof Type.Int || from instanceof Type.Short
				|| from instanceof Type.Byte || from instanceof Type.Char) && 
				(to instanceof Type.Long 
						|| to instanceof Type.Float 
						|| to instanceof Type.Double 
						|| (to instanceof Type.Char && !(from instanceof Type.Char)) 
						|| (to instanceof Type.Short && !(from instanceof Type.Short))
						|| (to instanceof Type.Byte && !(from instanceof Type.Byte)))) {
			
			bytecodes.add(new Bytecode.Conversion((Type.Primitive) from,
					(Type.Primitive) to));	
		} else if(from instanceof Type.Long && 
				(to instanceof Type.Char 
						|| to instanceof Type.Byte 
						|| to instanceof Type.Short 
						|| to instanceof Type.Int 
						|| to instanceof Type.Float 
						|| to instanceof Type.Double)) {
			bytecodes.add(new Bytecode.Conversion((Type.Primitive) from,
					(Type.Primitive) to));	
		} else if (from instanceof Type.Float
				&& (to instanceof Type.Char || to instanceof Type.Byte
						|| to instanceof Type.Short || to instanceof Type.Int
						|| to instanceof Type.Long || to instanceof Type.Double)) {
			bytecodes.add(new Bytecode.Conversion((Type.Primitive) from,
					(Type.Primitive) to));	
		} else if (from instanceof Type.Double
				&& (to instanceof Type.Char || to instanceof Type.Byte
						|| to instanceof Type.Short || to instanceof Type.Int
						|| to instanceof Type.Long || to instanceof Type.Float)) {
			bytecodes.add(new Bytecode.Conversion((Type.Primitive) from,
					(Type.Primitive) to));	
		} 
	}		
	
	/**
	 * The purpose of this method is to determine the dispatch mode required for
	 * this particular method call.
	 * 
	 * @param receiver
	 * @param funType
	 * @return 0 for virtual, 1 for interface, 2 for static
	 */	
	protected Pair<Clazz, Clazz.Method> determineMethod(Type.Clazz receiver,
			String name, Type.Function funType) throws ClassNotFoundException,
			MethodNotFoundException {						
		
		String fdesc = ClassFile.descriptor(funType, false);				
		
		Stack<Type.Clazz> worklist = new Stack<Type.Clazz>();
		Stack<Type.Clazz> interfaceWorklist = new Stack<Type.Clazz>();
		worklist.push(receiver);
		
		// Need to save the class of the static receiver type, since this
		// determines whether to use an invokevirtual or invokeinterface. Could
		// probably optimise this to avoid two identical calls to load class.
		Clazz outer = loader.loadClass(worklist.peek());
		
		while (!worklist.isEmpty()) {
			Clazz c = loader.loadClass(worklist.pop());						
			for (Clazz.Method m : c.methods(name)) {								
				String mdesc = ClassFile.descriptor(m.type(), false);
				if (fdesc.equals(mdesc)) {					
					return new Pair(outer,m);
				}
			}
			if(c.superClass() != null) {
				worklist.push(c.superClass());
			}
			for(Type.Clazz i : c.interfaces()) {
				interfaceWorklist.push(i);
			}
		}

		while (!interfaceWorklist.isEmpty()) {
			Clazz c = loader.loadClass(interfaceWorklist.pop());						
			for (Clazz.Method m : c.methods(name)) {				
				String mdesc = ClassFile.descriptor(m.type(), false);						
				if (fdesc.equals(mdesc)) {
					return new Pair(outer,m);
				}
			}
			for(Type.Clazz i : c.interfaces()) {
				interfaceWorklist.push(i);
			}			
		}
		
		throw new MethodNotFoundException(name,receiver.toString());
	}
	
	/**
	 * This method determines the actual type of a field. This is important,
	 * since the actual type and the bytecode type can differ in the case of
	 * generics. Thus, if we're loading a field of generic type, then we need a
	 * cast in the bytecode accordinly.
	 * 
	 * @param t
	 * @return
	 */
	protected Type determineFieldType(Type.Clazz receiver, String name)
			throws ClassNotFoundException, FieldNotFoundException {
		
		Stack<Type.Clazz> worklist = new Stack<Type.Clazz>();
		Stack<Type.Clazz> interfaceWorklist = new Stack<Type.Clazz>();
		worklist.push(receiver);
		
		// Need to save the class of the static receiver type, since this
		// determines whether to use an invokevirtual or invokeinterface. Could
		// probably optimise this to avoid two identical calls to load class.
		Clazz outer = loader.loadClass(worklist.peek());
		
		while (!worklist.isEmpty()) {
			Clazz c = loader.loadClass(worklist.pop());									
			Clazz.Field f = c.field(name);
			if (f != null) {
				return f.type();
			}
			if(c.superClass() != null) {
				worklist.push(c.superClass());
			}
			for(Type.Clazz i : c.interfaces()) {
				interfaceWorklist.push(i);
			}
		}

		while (!interfaceWorklist.isEmpty()) {
			Clazz c = loader.loadClass(interfaceWorklist.pop());			
			Clazz.Field f = c.field(name);
			if (f != null) {
				return f.type();
			}			
			for(Type.Clazz i : c.interfaces()) {
				interfaceWorklist.push(i);
			}		
		}
				
		throw new FieldNotFoundException(name,receiver.toString());
	}			
	
	protected boolean needClassSignature(JilClass c) {
		if (Types.isGeneric(c.type())
				|| (c.superClass() != null && Types.isGeneric(c.superClass()))) {
			return true;
		}
		for (Type.Reference t : c.interfaces()) {
			if (Types.isGeneric(t)) {
				return true;
			}
		}
		return false;
	}		
	
	protected ArrayList<Modifier> filterModifiers(List<Modifier> modifiers) { 
		ArrayList<Modifier> r = new ArrayList();
		for(Modifier m : modifiers) {
			if(!(m instanceof Modifier.Annotation)) {
				r.add(m);
			}
		}
		return r;
	}
	
	protected ArrayList<Modifier.Annotation> filterAnnotations(List<Modifier> modifiers) { 
		ArrayList<Modifier.Annotation> r = new ArrayList();
		for(Modifier m : modifiers) {
			if(m instanceof Modifier.Annotation) {
				r.add((Modifier.Annotation) m);
			}
		}
		return r;
	}
}
