package jkit.jil.stages;

import java.util.*;

import jkit.bytecode.Bytecode;
import jkit.bytecode.ClassFile;
import jkit.bytecode.ClassSignature;
import jkit.bytecode.Code;
import jkit.bytecode.Exceptions;
import jkit.bytecode.FieldSignature;
import jkit.bytecode.InnerClasses;
import jkit.bytecode.Bytecode.*;
import jkit.bytecode.ClassFile.Field;
import jkit.bytecode.ClassFile.Method;
import jkit.bytecode.Code.Handler;
import jkit.compiler.Clazz;
import jkit.compiler.ClassLoader;
import jkit.compiler.SyntaxError;
import jkit.compiler.FieldNotFoundException;
import jkit.compiler.MethodNotFoundException;
import jkit.jil.util.Types;
import jkit.jil.tree.*;
import jkit.jil.util.Exprs;
import jkit.util.Pair;
import jkit.util.Triple;
import static jkit.compiler.SyntaxError.*;

public class ClassFileBuilder {
	protected final ClassLoader loader;
	protected final int version;
	
	public ClassFileBuilder(ClassLoader loader, int version) {
		this.loader = loader;
		this.version = version;
	}
	
	public ClassFile build(jkit.jil.tree.JilClass clazz) {
		ClassFile cfile = new ClassFile(version, clazz.type(), clazz
				.superClass(), clazz.interfaces(), clazz.modifiers());
		
		if (needClassSignature(clazz)) {
			cfile.attributes().add(
					new ClassSignature(clazz.type(), clazz.superClass(), clazz
							.interfaces()));
		}
		
		buildInnerClasses(clazz,cfile);
		buildFields(clazz,cfile);
		buildMethods(clazz,cfile);					
		
		return cfile;
	}
	
	protected void buildInnerClasses(JilClass clazz, ClassFile cfile) {
		if(clazz.isInnerClass() || !clazz.inners().isEmpty()) {			
			// this is basically about building the inner classes attribute
			ArrayList<Pair<Type.Clazz,List<Modifier>>> inners = new ArrayList();
			ArrayList<Pair<Type.Clazz,List<Modifier>>> outers = new ArrayList();

			Type.Clazz inner = clazz.type();
			List<Pair<String,List<Type.Reference>>> components = inner.components();
			
			for(int i=components.size()-1;i>=0;--i) {
				List<Pair<String,List<Type.Reference>>> ncomponents = components.subList(0,i);
				Type.Clazz ref = new Type.Clazz(inner.pkg(),ncomponents);
				try {
					Clazz ic = loader.loadClass(ref);
					outers.add(new Pair(ic.type(),ic.modifiers()));
				} catch(ClassNotFoundException e) {
					// this is a problem, but for now we'll just ignore it
				}
			}			
			
			for(Type.Clazz tc : clazz.inners()) {
				try {
					Clazz ic = loader.loadClass(tc);
					inners.add(new Pair(ic.type(),ic.modifiers()));
				} catch(ClassNotFoundException e) {
					// this is a problem, but for now we'll just ignore it
				}
			}
			
			cfile.attributes().add(new InnerClasses(clazz.type(),inners,outers));
		}
	}
	
	protected void buildFields(JilClass clazz, ClassFile cfile) {
		for (JilField f : clazz.fields()) {
			ClassFile.Field cf = new ClassFile.Field(f.name(), f.type(), f.modifiers()); 
			cfile.fields().add(cf);
			if(isGeneric(f.type())) {
				cf.attributes().add(new FieldSignature(f.type()));
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
			ClassFile.Method cfm = new ClassFile.Method(m_name, m.type(), m
					.modifiers());
			
			if(!m.exceptions().isEmpty()) {
				cfm.attributes().add(new Exceptions(m.exceptions()));
			}
			
			if(!m.isAbstract() && !clazz.isInterface()) {
				ArrayList<Bytecode> bytecodes = new ArrayList<Bytecode>();
				ArrayList<Code.Handler> handlers = new ArrayList<Code.Handler>();

				translateCode(clazz, m, bytecodes, handlers);
				
				Code codeAttr = new Code(bytecodes,handlers,cfm);
				cfm.attributes().add(codeAttr);
			}
			
			if (isGeneric(m.type())) {
				cfm.attributes().add(new FieldSignature(m.type()));
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
			ArrayList<Bytecode> bytecodes, ArrayList<Code.Handler> handlers) {
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
		for (Pair<String,List<Modifier>> pp : method.parameters()) {
			localVarMap.put(pp.first(), maxLocals);
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
			int start = bytecodes.size();
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
	}


	protected void translateIfGoto(JilStmt.IfGoto stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {
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
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {
		
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
			boolean needReturnValue) {
		
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
			ArrayList<Bytecode> bytecodes, boolean needReturnValue) {

		if (stmt.name().equals("super")) {
			// catch explicit super constructor call.
			translateExpression(stmt.target(), varmap, bytecodes);
			bytecodes.add(new Bytecode.Invoke(targetT, "<init>",
					stmt.funType(), Bytecode.SPECIAL));
			return;
		}

		try {			
			Pair<Clazz,Clazz.Method> cm = determineMethod(targetT, stmt.name(), stmt
					.funType());
			Clazz c = cm.first();
			Clazz.Method m = cm.second();
			
			if (!m.isStatic()) {
				// must be non-static invocation
				translateExpression(stmt.target(), varmap, bytecodes);
			}
			// translate parameters
			for (JilExpr p : stmt.parameters()) {
				translateExpression(p, varmap, bytecodes);
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
				} else if ((retT instanceof Type.Variable || isGenericArray(retT))
						&& !(stmt.type() instanceof Type.Variable)
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
					bytecodes.add(new Bytecode.CheckCast(stmt.type()));
				}
			}
		} catch (ClassNotFoundException cnfe) {
			syntax_error(cnfe.getMessage(),stmt,cnfe);
		} catch (MethodNotFoundException mnfe) {
			syntax_error(mnfe.getMessage(),stmt,mnfe);			
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
			translateExpression(der.target(), varmap, bytecodes);
			translateExpression(stmt.rhs(), varmap, bytecodes);
			// figure out the type of the field involved
			Type.Clazz lhs_t = (Type.Clazz) der.target().type();
			
			if (der.isStatic()) {
				bytecodes.add(new Bytecode.PutField(lhs_t, der.name(), der
						.type(), Bytecode.STATIC));
			} else {
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
				throw new RuntimeException(
						"internal failure (looking for variable " + lv.value()
								+ ") " + expr);
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
			ArrayList<Bytecode> bytecodes) {
		Type tmp_t = def.target().type();

		if (tmp_t instanceof Type.Clazz) {
			Type.Clazz lhs_t = (Type.Clazz) tmp_t;

			try {
				Type actualFieldType = determineFieldType(lhs_t,def.name());
				Type bytecodeType = actualFieldType;
				
				if(actualFieldType instanceof Type.Variable) {
					bytecodeType = new Type.Clazz("java.lang","Object");
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
			} catch(ClassNotFoundException e) {
				syntax_error(e.getMessage(),def,e);
			} catch(FieldNotFoundException e) {
				syntax_error(e.getMessage(),def,e);
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
			ArrayList<Bytecode> bytecodes) {
		
		List<JilExpr> params = new ArrayList<JilExpr>();
		params.add(new JilExpr.Int(av.values().size()));
		translateNew(new JilExpr.New(av.type(), params, null), varmap, bytecodes,
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
			ArrayList<Bytecode> bytecodes, boolean needReturnValue) {

		if (news.type() instanceof Type.Clazz) {
			Type.Clazz type = (Type.Clazz) news.type();

			bytecodes.add(new Bytecode.New(news.type()));
			bytecodes.add(new Bytecode.Dup(news.type()));

			ArrayList<Type> paramTypes = new ArrayList<Type>();
			for (JilExpr p : news.parameters()) {
				translateExpression(p, varmap, bytecodes);
				paramTypes.add(p.type());
			}

			// call the appropriate constructor
			bytecodes.add(new Bytecode.Invoke(type, "<init>", news.funType(),
					Bytecode.SPECIAL));
		} else if (news.type() instanceof Type.Array) {
			int usedStack = 0;

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

	protected void translateBinaryOp(JilExpr.BinOp bop, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {

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
			ArrayList<Bytecode> bytecodes) {

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

		Type srcType = cast.expr().type();
		// Now, do implicit conversions
		if (cast.type() instanceof Type.Primitive
				&& srcType instanceof Type.Primitive) {
			bytecodes.add(new Bytecode.Conversion((Type.Primitive) srcType,
					(Type.Primitive) cast.type()));
		} else {
			bytecodes.add(new Bytecode.CheckCast(cast.type()));
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
		while (receiver != null) {
			Clazz c = loader.loadClass(receiver);
			Clazz.Field f = c.field(name);
			if (f != null) {
				return f.type();
			}
			receiver = c.superClass();
		}
		throw new FieldNotFoundException(name,receiver.toString());
	}
	
	protected static boolean isGeneric(Type t) {
		if (t instanceof Type.Variable) {
			return true;
		} else if (!(t instanceof Type.Clazz)) {
			return false;
		}
		Type.Clazz ref = (Type.Clazz) t;
		for(Pair<String, List<Type.Reference>> p : ref.components()) {
			if(p.second().size() > 0) {
				return true;
			}
		}
		return false;
	}
	
	protected static boolean isGenericArray(Type t) {
		if(t instanceof Type.Array) {
			Type et = ((Type.Array)t).element();
			if(et instanceof Type.Variable) {
				return true;
			} else {
				return isGenericArray(et);
			}
		} 
		
		return false;	
	}
	
	
	protected boolean needClassSignature(JilClass c) {
		if (isGeneric(c.type())
				|| (c.superClass() != null && isGeneric(c.superClass()))) {
			return true;
		}
		for (Type.Reference t : c.interfaces()) {
			if (isGeneric(t)) {
				return true;
			}
		}
		return false;
	}		
}
