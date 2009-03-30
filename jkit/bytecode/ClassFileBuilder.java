package jkit.bytecode;

import java.util.*;

import jkit.compiler.ClassLoader;
import jkit.jil.tree.*;
import jkit.jil.util.Exprs;
import jkit.util.Pair;

public class ClassFileBuilder {
	protected final ClassLoader loader;
	protected final int version;
	
	public ClassFileBuilder(ClassLoader loader, int version) {
		this.loader = loader;
		this.version = version;
	}
	
	public ClassFile build(jkit.jil.tree.Clazz clazz) {
		ClassFile cfile = new ClassFile(version, clazz.type(), clazz
				.superClass(), clazz.interfaces(), clazz.modifiers());
		
		buildFields(clazz,cfile);
		buildMethods(clazz,cfile);					
		
		return cfile;
	}
	
	protected void buildFields(Clazz clazz, ClassFile cfile) {
		for (Field f : clazz.fields()) {
			cfile.fields().add(
					new ClassFile.Field(f.name(), f.type(), f.modifiers()));						
		}
	}
	
	protected void buildMethods(Clazz clazz, ClassFile cfile) {
		for (Method m : clazz.methods()) {
			ClassFile.Method cfm = new ClassFile.Method(m.name(), m.type(), m
					.modifiers(), m.exceptions());
			
			if(m.body() != null) {
				ArrayList<Bytecode> bytecodes = new ArrayList<Bytecode>();
				ArrayList<ClassFile.Handler> handlers = new ArrayList<ClassFile.Handler>();

				translateCode(clazz, m, bytecodes, handlers);
				
				ClassFile.Code codeAttr = new ClassFile.Code(bytecodes,handlers,cfm);
				cfm.attributes().add(codeAttr);
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
	protected void translateCode(Clazz clazz, Method method,
			ArrayList<Bytecode> bytecodes, ArrayList<ClassFile.Handler> handlers) {
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
		for (String var : method.localVariables()) {
			localVarMap.put(var, maxLocals);
			// there's a bug here, which i'm not sure how to resolve as yet.
			maxLocals ++;
		}
		
		// === TRANSLATE BYTECODES ===
		for(Stmt s : method.body()) {
			translateStatement(s,localVarMap,bytecodes);
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
		sortAndCompactExceptionHandlers(handlers);
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
			ArrayList<ClassFile.Handler> handlers) {

		// FIXME: support for sorting exception handlers
		
//		// firstly, sort them into the correct order
//		Collections.sort(handlers, new Comparator<ExceptionHandler>() {
//			public int compare(ExceptionHandler e1, ExceptionHandler e2) {
//				if (e1.exception.supsetEqOf(e2.exception)
//						&& e2.exception.supsetEqOf(e1.exception)) {
//					if (e1.start < e2.start) {
//						return -1;
//					} else if (e1.start > e2.start) {
//						return 1;
//					} else {
//						return 0;
//					}
//				} else if (e1.exception.supsetEqOf(e2.exception)) {
//					return 1;
//				} else {
//					return -1;
//				}
//			}
//		});
//
//		// now, we compact them together.
//		ArrayList<ExceptionHandler> oldhandlers = new ArrayList<ExceptionHandler>(
//				handlers);
//		handlers.clear();
//
//		for (int i = 0; i != oldhandlers.size();) {
//			ExceptionHandler handler = oldhandlers.get(i);
//			int end = handler.end;
//			ExceptionHandler tmp;
//			i = i + 1;
//			while (i < oldhandlers.size()
//					&& (tmp = oldhandlers.get(i)).start == (end + 1)
//					&& tmp.label == handler.label
//					&& tmp.exception.equals(handler.exception)) {
//				end = tmp.end;
//				i = i + 1;
//			}
//			tmp = new ExceptionHandler(handler.start, end, handler.label,
//					handler.exception);
//			handlers.add(tmp);
//		}
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
	protected void translateStatement(Stmt stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {
		if (stmt instanceof Stmt.Return) {
			translateReturn((Stmt.Return) stmt, varmap,
					bytecodes);
		} else if (stmt instanceof Stmt.Assign) {
			translateAssign((Stmt.Assign) stmt, varmap,
					bytecodes);
		} else if (stmt instanceof Expr.Invoke) {
			translateInvoke((Expr.Invoke) stmt, varmap,
					bytecodes, false);
		} else if (stmt instanceof Expr.New) {
			translateNew((Expr.New) stmt, varmap, bytecodes,
					false);
		} else if (stmt instanceof Stmt.Nop) {
			bytecodes.add(new Bytecode.Nop());
		} else if (stmt instanceof Stmt.Throw) {
			translateThrow((Stmt.Throw) stmt, varmap, bytecodes);
		} else if (stmt instanceof Stmt.Lock) {
			translateLock((Stmt.Lock) stmt, varmap, bytecodes);
		} else if (stmt instanceof Stmt.Unlock) {
			translateUnlock((Stmt.Unlock) stmt, varmap,
					bytecodes);
		} else if(stmt instanceof Stmt.Label) {
			translateLabel((Stmt.Label)stmt,varmap,bytecodes);
		} else if(stmt instanceof Stmt.IfGoto) {
			translateIfGoto((Stmt.IfGoto)stmt,varmap,bytecodes);
		} else if(stmt instanceof Stmt.Goto) {
			translateGoto((Stmt.Goto)stmt,varmap,bytecodes);
		} else {
			throw new RuntimeException("Unknown statement encountered: " + stmt);
		}
	}


	protected void translateIfGoto(Stmt.IfGoto stmt,
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
	protected void translateConditionalBranch(Expr condition, String trueLabel,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {
		
		if (condition instanceof Expr.BinOp) {
			Expr.BinOp bop = (Expr.BinOp) condition;

			switch (bop.op()) {
			case Expr.BinOp.LAND: {
				String exitLabel = "CL" + condLabelCount++;
				translateConditionalBranch(Exprs.invertBoolean(bop
						.lhs()), exitLabel, varmap, bytecodes);
				translateConditionalBranch(Exprs.invertBoolean(bop
						.rhs()), exitLabel, varmap, bytecodes);
				bytecodes.add(new Bytecode.Goto(trueLabel));
				bytecodes.add(new Bytecode.Label(exitLabel));
				return;
			}
			case Expr.BinOp.LOR: {
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
			case Expr.BinOp.EQ:
				code = Bytecode.IfCmp.EQ;
				break;
			case Expr.BinOp.NEQ:
				code = Bytecode.IfCmp.NE;
				break;
			case Expr.BinOp.LT:
				code = Bytecode.IfCmp.LT;
				break;
			case Expr.BinOp.GT:
				code = Bytecode.IfCmp.GT;
				break;
			case Expr.BinOp.LTEQ:
				code = Bytecode.IfCmp.LE;
				break;
			case Expr.BinOp.GTEQ:
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
		} else if (condition instanceof Expr.UnOp) {
			Expr.UnOp uop = (Expr.UnOp) condition;
			if (uop.op() == Expr.UnOp.NOT) {
				// First attempt to eliminate the NOT altogether!
				Expr e1 = Exprs.eliminateNot(uop);

				if (e1 instanceof Expr.UnOp) {
					Expr.UnOp e2 = (Expr.UnOp) e1;
					if (e2.op() == Expr.UnOp.NOT) {
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
		} else if (condition instanceof Expr.Invoke) {
			translateInvoke((Expr.Invoke) condition, varmap, 
					bytecodes, true);
			bytecodes.add(new Bytecode.If(Bytecode.If.NE, trueLabel));			
		} else if (condition instanceof Expr.InstanceOf) {
			translateExpression(condition, varmap, 
					bytecodes);
			bytecodes.add(new Bytecode.If(Bytecode.If.NE, trueLabel));			
		} else if (condition instanceof Expr.Bool 
					|| condition instanceof Expr.ArrayIndex
					|| condition instanceof Expr.Variable
					|| condition instanceof Expr.Deref) {
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
	protected void translateSwitch(Expr[] conditions, String[] labels,
			HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {

		Expr expr = null;
		for (Expr c : conditions) {
			Expr.BinOp cond = (Expr.BinOp) c;
			if (cond.op() == Expr.BinOp.EQ) {
				expr = cond.lhs();
				break;
			}
		}

		translateExpression(expr, varmap, bytecodes);
		
		String def = null;
		List<Pair<Integer, String>> cases = new ArrayList<Pair<Integer, String>>();
		for (int i = 0; i < conditions.length; i++) {
			Expr e = ((Expr.BinOp) conditions[i]).rhs();
			if (e instanceof Expr.Int) {
				int c = ((Expr.Int) e).value();
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

	protected void translateInvoke(Expr.Invoke stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes,
			boolean needReturnValue) {

		if (!stmt.isStatic()) {
			// must be non-static invocation
			translateExpression(stmt.target(), varmap, bytecodes);
		}
		// translate parameters
		for (Expr p : stmt.parameters()) {
			translateExpression(p, varmap, bytecodes);
		}
		
		Type.Clazz targetT = (Type.Clazz) stmt.target().type(); 
		String targetName = targetT.components().get(targetT.components().size()-1).first();
		
		if (stmt.isStatic()) {
			// STATIC
			bytecodes.add(new Bytecode.Invoke(
					targetT, stmt.name(), stmt.funType(), Bytecode.STATIC));
		} else if (stmt.target().type() instanceof Type.Clazz
				&& stmt.name().equals(targetName)) {
			// this is a constructor call
			bytecodes.add(new Bytecode.Invoke(targetT, "<init>",
					stmt.funType(), Bytecode.SPECIAL));
		} else {
			// check whether this is an interface or a class call.
			if (stmt.isInterface()) {
				bytecodes.add(new Bytecode.Invoke(targetT, stmt.name(), stmt
						.funType(), Bytecode.INTERFACE));
			} else {
				bytecodes.add(new Bytecode.Invoke(targetT, stmt
						.name(), stmt.funType(),
						stmt.isPolymorphic() ? Bytecode.VIRTUAL
								: Bytecode.SPECIAL));
			}
		}		

		Type retT = stmt.funType().returnType();
				
		if (!(retT instanceof Type.Void)) {
			// Need to account for space occupied by return type!			
			if (!needReturnValue) {
				// the return value is not required, so we need to pop it from
				// the stack
				bytecodes.add(new Bytecode.Pop(retT));
			} else if ((retT instanceof Type.Variable ||
						ClassFile.isGenericArray(retT))
					&& !(stmt.type() instanceof Type.Variable)
					&& !stmt.type().equals(
							new Type.Clazz("java.lang", "Object"))) {				
				// Here, the actual return type is a (generic) type variable
				// (e.g. T or T[]), and we're expecting it to return a real
                // value (e.g. String, substituted for T). This issue is
				// that, because of erasure, the returned type will be Object
				// and we need to cast it to whatever it needs to be (e.g.
				// String). Note, if the value substituted for T is actually
				// Object, then we just do nothing!
				bytecodes.add(new Bytecode.CheckCast(stmt.type()));								
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
	protected void translateReturn(Stmt.Return ret,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {

		if (ret.expr() != null) {
			translateExpression(ret.expr(), varmap, bytecodes);
			bytecodes.add(new Bytecode.Return(ret.expr().type()));
		} else {
			bytecodes.add(new Bytecode.Return(null));
		}
	}

	protected void translateAssign(Stmt.Assign stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {

		if (stmt.lhs() instanceof Expr.Variable) {
			Expr.Variable var = (Expr.Variable) stmt.lhs();
			assert varmap.keySet().contains(var.value());
			int slot = varmap.get(var.value());
			translateExpression(stmt.rhs(), varmap, bytecodes);
			bytecodes.add(new Bytecode.Store(slot, stmt.lhs().type()));
		} else if (stmt.lhs() instanceof Expr.Deref) {
			Expr.Deref der = (Expr.Deref) stmt.lhs();
			translateExpression(der.target(), varmap, bytecodes);
			translateExpression(stmt.rhs(), varmap, bytecodes);
			// figure out the type of the field involved
			Type.Reference lhs_t = (Type.Reference) der.target().type();

			if (der.isStatic()) {
				bytecodes.add(new Bytecode.PutField(lhs_t, der.name(), der
						.type(), Bytecode.STATIC));
			} else {
				bytecodes.add(new Bytecode.PutField(lhs_t, der.name(), der
						.type(), Bytecode.NONSTATIC));
			}
		} else if (stmt.lhs() instanceof Expr.ArrayIndex) {
			Expr.ArrayIndex ai = (Expr.ArrayIndex) stmt.lhs();
			translateExpression(ai.target(), varmap, bytecodes);
			translateExpression(ai.index(), varmap, bytecodes);
			translateExpression(stmt.rhs(), varmap, bytecodes);
			bytecodes.add(new Bytecode.ArrayStore((Type.Array) ai.target()
					.type()));			
		} else {
			throw new RuntimeException("Unknown lval encountered");
		}
	}

	protected void translateThrow(Stmt.Throw stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {
		translateExpression(stmt.expr(), varmap, bytecodes);
		bytecodes.add(new Bytecode.Throw());
	}

	protected void translateLock(Stmt.Lock stmt, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		translateExpression(stmt.expr(), varmap, bytecodes);
		bytecodes.add(new Bytecode.MonitorEnter());
	}

	protected void translateUnlock(Stmt.Unlock stmt, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		translateExpression(stmt.expr(), varmap, bytecodes);
		bytecodes.add(new Bytecode.MonitorExit());
	}

	protected void translateLabel(Stmt.Label label, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		bytecodes.add(new Bytecode.Label(label.label()));
	}
	
	protected void translateGoto(Stmt.Goto stmt, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		bytecodes.add(new Bytecode.Goto(stmt.label()));
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
	protected void translateExpression(Expr expr,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {

		if (expr instanceof Expr.Bool) {			
			bytecodes.add(new Bytecode.LoadConst(((Expr.Bool) expr).value()));
		} else if (expr instanceof Expr.Byte) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Byte) expr).value()));
		} else if (expr instanceof Expr.Char) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Char) expr).value()));
		} else if (expr instanceof Expr.Short) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Short) expr).value()));
		} else if (expr instanceof Expr.Int) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Int) expr).value()));
		} else if (expr instanceof Expr.Long) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Long) expr).value()));
		} else if (expr instanceof Expr.Float) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Float) expr).value()));
		} else if (expr instanceof Expr.Double) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Double) expr).value()));
		} else if (expr instanceof Expr.Null) {
			bytecodes.add(new Bytecode.LoadConst(null));
		} else if (expr instanceof Expr.StringVal) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.StringVal) expr).value()));
		} else if (expr instanceof Expr.Array) {
			translateArrayVal((Expr.Array) expr, varmap, bytecodes);
		} else if (expr instanceof Expr.Class) {
			translateClassVal((Expr.Class) expr, varmap, bytecodes);
		} else if (expr instanceof Expr.Variable) {
			Expr.Variable lv = (Expr.Variable) expr;
			if (varmap.containsKey(lv.value())) {
				bytecodes.add(new Bytecode.Load(varmap.get(lv.value()), lv.type()));				
			} else {
				throw new RuntimeException(
						"internal failure (looking for variable " + lv.value()
								+ ") " + expr);
			}
		} else if (expr instanceof Expr.New) {
			translateNew((Expr.New) expr, varmap, bytecodes,true);
		} else if (expr instanceof Expr.Deref) {
			translateDeref((Expr.Deref) expr, varmap, bytecodes);
		} else if (expr instanceof Expr.ArrayIndex) {
			Expr.ArrayIndex ai = (Expr.ArrayIndex) expr;
			translateExpression(ai.target(), varmap, bytecodes);
			translateExpression(ai.index(), varmap, bytecodes);
			Type arr_t = ai.target().type();
			bytecodes.add(new Bytecode.ArrayLoad((Type.Array) arr_t));			
		} else if (expr instanceof Expr.Invoke) {
			translateInvoke((Expr.Invoke) expr, varmap, bytecodes, true);
		} else if (expr instanceof Expr.UnOp) {
			translateUnaryOp((Expr.UnOp) expr, varmap,bytecodes);
		} else if (expr instanceof Expr.BinOp) {
			translateBinaryOp((Expr.BinOp) expr, varmap,bytecodes);
		} else if (expr instanceof Expr.InstanceOf) {
			Expr.InstanceOf iof = (Expr.InstanceOf) expr;
			translateExpression(iof.lhs(), varmap, bytecodes);
			bytecodes.add(new Bytecode.InstanceOf(iof.rhs()));
		} else if (expr instanceof Expr.Cast) {
			translateCast((Expr.Cast) expr, varmap,bytecodes);
		} else if (expr instanceof Expr.Convert) {
			translateConvert((Expr.Convert) expr, varmap,bytecodes);
		} else {
			throw new RuntimeException("Unknown expression encountered ("
					+ expr + ")");
		}
	}

	public void translateClassVal(Expr.Class cval,  HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		if(cval.type() instanceof Type.Primitive) {
			// FIXME: fix class access to primitive types
//			bytecodes.add(new Bytecode.GetField(Typing
//					.boxedType((Type.Primitive) cval.classType), "TYPE", Type
//					.referenceType("java.lang", "Class"), Bytecode.STATIC));
		} else {
			bytecodes.add(new Bytecode.LoadConst(cval.type()));
		}
	}
	
	public void translateDeref(Expr.Deref def, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		Type tmp_t = def.target().type();

		if (tmp_t instanceof Type.Clazz) {
			Type.Clazz lhs_t = (Type.Clazz) tmp_t;

			if (def.isStatic()) {
				// This is a static field load					
				bytecodes.add(new Bytecode.GetField(lhs_t, def.name(),
						def.type(), Bytecode.STATIC));				
			} else {
				// Non-static field load
				translateExpression(def.target(), varmap, bytecodes);

				bytecodes.add(new Bytecode.GetField(lhs_t, def.name(),
						def.type(), Bytecode.NONSTATIC));
				
				// FIXME: generic type conversions
//				if (actualFieldType instanceof Type.Variable
//						&& !(def.type() instanceof Type.Variable)
//						&& !def.type().equals(new Type.Clazz("java.lang",
//								"Object"))) {
//					// Ok, actual type is a (generic) type variable. Need to
//					// cast to the desired type!
//					bytecodes.add(new Bytecode.CheckCast(def.type()));					
//				}				
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

	public void translateArrayVal(Expr.Array av, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		
		List<Expr> params = new ArrayList<Expr>();
		params.add(new Expr.Int(av.values().size()));
		translateNew(new Expr.New(av.type(), params, null), varmap, bytecodes,
				true);

		int index = 0;
		for (Expr e : av.values()) {
			bytecodes.add(new Bytecode.Dup(av.type()));
			bytecodes.add(new Bytecode.LoadConst(index++));
			translateExpression(e, varmap, bytecodes);
			bytecodes.add(new Bytecode.ArrayStore((Type.Array) av.type()));			
		}
	}

	public void translateNew(Expr.New news, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes, boolean needReturnValue) {

		if (news.type() instanceof Type.Clazz) {
			Type.Clazz type = (Type.Clazz) news.type();

			bytecodes.add(new Bytecode.New(news.type()));
			bytecodes.add(new Bytecode.Dup(news.type()));

			ArrayList<Type> paramTypes = new ArrayList<Type>();
			for (Expr p : news.parameters()) {
				translateExpression(p, varmap, bytecodes);
				paramTypes.add(p.type());
			}

						// call the appropriate constructor
			bytecodes.add(new Bytecode.Invoke(type, "<init>", news.funType(),
					Bytecode.SPECIAL));
		} else if (news.type() instanceof Type.Array) {
			int usedStack = 0;

			for (Expr p : news.parameters()) {
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

	protected void translateBinaryOp(Expr.BinOp bop, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {

		// second, translate the binary operator.
		switch (bop.op()) {
		case Expr.BinOp.LT:
		case Expr.BinOp.LTEQ:
		case Expr.BinOp.GT:
		case Expr.BinOp.GTEQ:
		case Expr.BinOp.EQ:
		case Expr.BinOp.NEQ:
		case Expr.BinOp.LAND:
		case Expr.BinOp.LOR: {
			String trueLabel = "CL" + condLabelCount++;
			String exitLabel = "CL" + condLabelCount++;
			translateConditionalBranch(bop, trueLabel, varmap, bytecodes);
			bytecodes.add(new Bytecode.LoadConst(0));
			bytecodes.add(new Bytecode.Goto(exitLabel));
			bytecodes.add(new Bytecode.Label(trueLabel));
			bytecodes.add(new Bytecode.LoadConst(1));
			bytecodes.add(new Bytecode.Label(exitLabel));
		}
		}

		// must be a standard arithmetic operation.
		translateExpression(bop.lhs(), varmap, bytecodes);
		translateExpression(bop.rhs(), varmap, bytecodes);
		bytecodes.add(new Bytecode.BinOp(bop.op(), bop.type()));		
	}

	protected void translateUnaryOp(Expr.UnOp uop, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		// second, translate the operation.
		// FIXME: resolve operator type

		switch (uop.op()) {
		case Expr.UnOp.NOT:
			String trueLabel = "CL" + condLabelCount++;
			String exitLabel = "CL" + condLabelCount++;
			translateConditionalBranch(uop, trueLabel, varmap, bytecodes);
			bytecodes.add(new Bytecode.LoadConst(0));
			bytecodes.add(new Bytecode.Goto(exitLabel));
			bytecodes.add(new Bytecode.Label(trueLabel));
			bytecodes.add(new Bytecode.LoadConst(1));
			bytecodes.add(new Bytecode.Label(exitLabel));			
		}

		// first, translate the expression.
		translateExpression(uop.expr(), varmap, bytecodes);

		switch (uop.op()) {
		case Expr.UnOp.INV:
			bytecodes.add(new Bytecode.LoadConst(new Integer(-1)));
			bytecodes
					.add(new Bytecode.BinOp(Bytecode.BinOp.XOR, new Type.Int()));			
			break;
		case Expr.UnOp.NEG:
			bytecodes.add(new Bytecode.Neg(uop.type()));
			break;
		default:
			throw new RuntimeException("Unknown unary expression encountered ("
					+ uop + ")");
		}
	}

	protected void translateCast(Expr.Cast cast, HashMap<String, Integer> varmap,
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

	protected void translateConvert(Expr.Convert cast, HashMap<String, Integer> varmap,
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
}
