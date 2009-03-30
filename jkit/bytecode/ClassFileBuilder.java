package jkit.bytecode;

import java.util.*;

import jkit.bytecode.ClassFileWriter.ExceptionHandler;
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
				.superClass(), clazz.interfaces());
		
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
				int maxLocals = 0;// method.code().localVariables().size();
				
				// FIXME: support for local variables
//				for (LocalVarDef lvd : method.code().localVariables()) {
//					maxLocals += ClassFile.slotSize(lvd.type());
//				}
				
				if (!m.isStatic()) {
					maxLocals++;
				}

				ArrayList<Bytecode> bytecodes = new ArrayList<Bytecode>();
				ArrayList<ExceptionHandler> handlers = new ArrayList<ExceptionHandler>();

				int maxStack = translateCode(clazz, m, bytecodes, handlers);
				
				ClassFile.Code codeAttr = new ClassFile.Code(maxLocals,maxStack,bytecodes);
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
	protected int translateCode(Clazz clazz, Method method, ArrayList<Bytecode> bytecodes,
			ArrayList<ExceptionHandler> handlers) {
		// === CREATE TYPE ENVIRONMENT ===

		// create the local variable slot mapping
		HashMap<String, Integer> localVarMap = new HashMap<String, Integer>();		

		int maxLocals = 0;
		int maxStack = 0;

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
		
		// Now make sure the exception handlers are compacted and
		// also arranged in the correct order.
		sortAndCompactExceptionHandlers(handlers);

		return maxStack;
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
			ArrayList<ExceptionHandler> handlers) {

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
	 * @return the maximum stack size required by this statement.
	 */
	protected int translateStatement(Stmt stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {
		if (stmt instanceof Stmt.Return) {
			return translateReturn((Stmt.Return) stmt, varmap,
					bytecodes);
		} else if (stmt instanceof Stmt.Assign) {
			return translateAssign((Stmt.Assign) stmt, varmap,
					bytecodes);
		} else if (stmt instanceof Expr.Invoke) {
			return translateInvoke((Expr.Invoke) stmt, varmap,
					bytecodes, false);
		} else if (stmt instanceof Expr.New) {
			return translateNew((Expr.New) stmt, varmap, bytecodes,
					false);
		} else if (stmt instanceof Stmt.Nop) {
			bytecodes.add(new Bytecode.Nop());
			return 0;
		} else if (stmt instanceof Stmt.Throw) {
			return translateThrow((Stmt.Throw) stmt, varmap, bytecodes);
		} else if (stmt instanceof Stmt.Lock) {
			return translateLock((Stmt.Lock) stmt, varmap, bytecodes);
		} else if (stmt instanceof Stmt.Unlock) {
			return translateUnlock((Stmt.Unlock) stmt, varmap,
					bytecodes);
		} else if(stmt instanceof Stmt.Label) {
			return translateLabel((Stmt.Label)stmt,varmap,bytecodes);
		} else if(stmt instanceof Stmt.IfGoto) {
			return translateIfGoto((Stmt.IfGoto)stmt,varmap,bytecodes);
		} else if(stmt instanceof Stmt.Goto) {
			return translateGoto((Stmt.Goto)stmt,varmap,bytecodes);
		} else {
			throw new RuntimeException("Unknown statement encountered: " + stmt);
		}
	}


	protected int translateIfGoto(Stmt.IfGoto stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {
		return translateConditionalBranch(stmt.condition(), stmt.label(),
				varmap, bytecodes);
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
	 * @return maximum size of stack required for this statement
	 */
	protected static int condLabelCount = 0;
	protected int translateConditionalBranch(Expr condition, String trueLabel,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {

		if (condition instanceof Expr.BinOp) {
			Expr.BinOp bop = (Expr.BinOp) condition;

			switch (bop.op()) {
			case Expr.BinOp.LAND: {
				String exitLabel = "CL" + condLabelCount++;
				int ms_lhs = translateConditionalBranch(Exprs.invertBoolean(bop
						.lhs()), exitLabel, varmap, bytecodes);
				int ms_rhs = translateConditionalBranch(Exprs.invertBoolean(bop
						.rhs()), exitLabel, varmap, bytecodes);
				bytecodes.add(new Bytecode.Goto(trueLabel));
				bytecodes.add(new Bytecode.Label(exitLabel));
				return Math.max(ms_lhs, ms_rhs);
			}
			case Expr.BinOp.LOR: {
				int ms_lhs = translateConditionalBranch(bop.lhs(), trueLabel,
						varmap, bytecodes);
				int ms_rhs = translateConditionalBranch(bop.rhs(), trueLabel,
						varmap, bytecodes);
				return Math.max(ms_lhs, ms_rhs);
			}
			}

			// There are a number of optimisations which could be applied
			// here. For example, using ifnull and ifnotnull bytecodes. Also,
			// using ifeq when comparing directly against zero.

			int ms_lhs = translateExpression(bop.lhs(), varmap, 
					bytecodes);

			int ms_rhs = translateExpression(bop.rhs(), varmap, 
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
			return Math.max(ms_lhs, ms_rhs + ClassFile.slotSize(bop.lhs().type()));

		} else if (condition instanceof Expr.UnOp) {
			Expr.UnOp uop = (Expr.UnOp) condition;
			if (uop.op() == Expr.UnOp.NOT) {
				// First attempt to eliminate the NOT altogether!
				Expr e1 = Exprs.eliminateNot(uop);

				if (e1 instanceof Expr.UnOp) {
					Expr.UnOp e2 = (Expr.UnOp) e1;
					if (e2.op() == Expr.UnOp.NOT) {
						// not elimination was unsuccessful
						int ms = translateExpression(uop.expr(), varmap,
								bytecodes);
						bytecodes
								.add(new Bytecode.If(Bytecode.If.EQ, trueLabel));
						return ms;
					}
				}
				// not elimination was successful ...
				return translateConditionalBranch(e1, trueLabel, varmap,
						bytecodes);

			}
			// anything else doesn't make sense
			throw new RuntimeException(
					"Invalid use of unary operator in conditional ("
							+ condition + ")");
		} else if (condition instanceof Expr.Invoke) {
			int ms = translateInvoke((Expr.Invoke) condition, varmap, 
					bytecodes, true);
			bytecodes.add(new Bytecode.If(Bytecode.If.NE, trueLabel));
			return ms;
		} else if (condition instanceof Expr.InstanceOf) {
			int ms = translateExpression(condition, varmap, 
					bytecodes);
			bytecodes.add(new Bytecode.If(Bytecode.If.NE, trueLabel));
			return ms;
		} else if (condition instanceof Expr.Bool 
					|| condition instanceof Expr.ArrayIndex
					|| condition instanceof Expr.Variable
					|| condition instanceof Expr.Deref) {
			int ms = translateExpression(condition, varmap,
					bytecodes);
			bytecodes.add(new Bytecode.If(Bytecode.If.NE, trueLabel));
			return ms;
		} 

		throw new RuntimeException("Unknown conditional expression ("
				+ condition + ")");
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
	 * @return maximum size of stack required for this statement
	 */
	protected int translateSwitch(Expr[] conditions, String[] labels,
			HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {

		int maxStack = 0; // max stack required for this statement

		Expr expr = null;
		for (Expr c : conditions) {
			Expr.BinOp cond = (Expr.BinOp) c;
			if (cond.op() == Expr.BinOp.EQ) {
				expr = cond.lhs();
				break;
			}
		}

		int ms = translateExpression(expr, varmap, bytecodes);
		maxStack = Math.max(ms, maxStack);

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

		return maxStack;
	}

	protected int translateInvoke(Expr.Invoke stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes,
			boolean needReturnValue) {

		int stackUsed = 0; // stack slots used by store results
		int maxStack = 0; // max stack required for this statement

		if (!stmt.isStatic()) {
			// must be non-static invocation
			maxStack = translateExpression(stmt.target(), varmap, bytecodes);
			stackUsed++; // it's a reference, so don't worry about slot size
		}
		// translate parameters
		for (Expr p : stmt.parameters()) {
			int ms = translateExpression(p, varmap, bytecodes);
			maxStack = Math.max(ms + stackUsed, maxStack);
			stackUsed += ClassFile.slotSize(p.type()); // this is correct here,
												   // since don't account for
												   // our slot
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
			maxStack = Math.max(ClassFile.slotSize(retT) + stackUsed, maxStack);
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

		return maxStack;
	}

	/**
	 * Translate a Return statement.
	 * 
	 * @param ret
	 * @param varmap
	 * @param bytecodes
	 * @return
	 */
	protected int translateReturn(Stmt.Return ret,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {

		if (ret.expr() != null) {
			int ms = translateExpression(ret.expr(), varmap,
					bytecodes);
			bytecodes.add(new Bytecode.Return(ret.expr().type()));
			return ms;
		} else {
			bytecodes.add(new Bytecode.Return(null));
			return 0;
		}
	}

	protected int translateAssign(Stmt.Assign stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {

		int maxStack = 0;

		if (stmt.lhs() instanceof Expr.Variable) {
			Expr.Variable var = (Expr.Variable) stmt.lhs();
			assert varmap.keySet().contains(var.value());
			int slot = varmap.get(var.value());
			maxStack = translateExpression(stmt.rhs(), varmap, bytecodes);
			bytecodes.add(new Bytecode.Store(slot, stmt.lhs().type()));
		} else if (stmt.lhs() instanceof Expr.Deref) {
			Expr.Deref der = (Expr.Deref) stmt.lhs();
			int ms_lhs = translateExpression(der.target(), varmap,
					bytecodes);
			int ms_rhs = translateExpression(stmt.rhs(), varmap,
					bytecodes);
			// figure out the type of the field involved
			Type.Reference lhs_t = (Type.Reference) der.target().type();

			if (der.isStatic()) {
				bytecodes.add(new Bytecode.PutField(lhs_t, der.name(), der
						.type(), Bytecode.STATIC));
			} else {
				bytecodes.add(new Bytecode.PutField(lhs_t, der.name(), der
						.type(), Bytecode.NONSTATIC));
			}
			maxStack = Math.max(ms_lhs, ms_rhs + 1);

		} else if (stmt.lhs() instanceof Expr.ArrayIndex) {
			Expr.ArrayIndex ai = (Expr.ArrayIndex) stmt.lhs();
			int ms_arr = translateExpression(ai.target(), varmap,
					bytecodes);
			int ms_idx = translateExpression(ai.index(), varmap,
					bytecodes);
			int ms_rhs = translateExpression(stmt.rhs(), varmap,
					bytecodes);
			bytecodes.add(new Bytecode.ArrayStore((Type.Array) ai.target()
					.type()));
			maxStack = Math.max(ms_arr, Math.max(ms_idx + 1, ms_rhs + 2));
		} else {
			throw new RuntimeException("Unknown lval encountered");
		}
		return maxStack;
	}

	protected int translateThrow(Stmt.Throw stmt,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {
		int maxStack = translateExpression(stmt.expr(), varmap, 
				bytecodes);
		bytecodes.add(new Bytecode.Throw());
		return maxStack;
	}

	protected int translateLock(Stmt.Lock stmt, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		int maxStack = translateExpression(stmt.expr(), varmap, 
				bytecodes);
		
		bytecodes.add(new Bytecode.MonitorEnter());
		return maxStack;
	}

	protected int translateUnlock(Stmt.Unlock stmt, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		int maxStack = translateExpression(stmt.expr(), varmap, 
				bytecodes);
		
		bytecodes.add(new Bytecode.MonitorExit());
		return maxStack;
	}

	protected int translateLabel(Stmt.Label label, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		bytecodes.add(new Bytecode.Label(label.label()));
		return 0;
	}
	
	protected int translateGoto(Stmt.Goto stmt, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		bytecodes.add(new Bytecode.Goto(stmt.label()));
		return 0;
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
	protected int translateExpression(Expr expr,
			HashMap<String, Integer> varmap, ArrayList<Bytecode> bytecodes) {

		int maxStack = 0;

		if (expr instanceof Expr.Bool) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Bool) expr).value()));
			maxStack = 1;
		} else if (expr instanceof Expr.Byte) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Byte) expr).value()));
			maxStack = 1;
		} else if (expr instanceof Expr.Char) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Char) expr).value()));
			maxStack = 1;
		} else if (expr instanceof Expr.Short) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Short) expr).value()));
			maxStack = 1;
		} else if (expr instanceof Expr.Int) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Int) expr).value()));
			maxStack = 1;
		} else if (expr instanceof Expr.Long) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Long) expr).value()));
			maxStack = 2;
		} else if (expr instanceof Expr.Float) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Float) expr).value()));
			maxStack = 1;
		} else if (expr instanceof Expr.Double) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.Double) expr).value()));
			maxStack = 2;
		} else if (expr instanceof Expr.Null) {
			bytecodes.add(new Bytecode.LoadConst(null));
			maxStack = 1;
		} else if (expr instanceof Expr.StringVal) {
			bytecodes.add(new Bytecode.LoadConst(((Expr.StringVal) expr).value()));
			maxStack = 1;
		} else if (expr instanceof Expr.Array) {
			return translateArrayVal((Expr.Array) expr, varmap, 
					bytecodes);
		} else if (expr instanceof Expr.Class) {
			return translateClassVal((Expr.Class) expr, varmap,
					bytecodes);			
		} else if (expr instanceof Expr.Variable) {
			Expr.Variable lv = (Expr.Variable) expr;
			if (varmap.containsKey(lv.value())) {
				bytecodes.add(new Bytecode.Load(varmap.get(lv.value()), lv.type()));
				maxStack = ClassFile.slotSize(lv.type());
			} else {
				throw new RuntimeException(
						"internal failure (looking for variable " + lv.value()
								+ ") " + expr);
			}
		} else if (expr instanceof Expr.New) {
			maxStack = translateNew((Expr.New) expr, varmap, bytecodes,
					true);
		} else if (expr instanceof Expr.Deref) {
			maxStack = translateDeref((Expr.Deref) expr, varmap, 
					bytecodes);
		} else if (expr instanceof Expr.ArrayIndex) {
			Expr.ArrayIndex ai = (Expr.ArrayIndex) expr;
			int ms_arr = translateExpression(ai.target(), varmap, 
					bytecodes);
			int ms_idx = translateExpression(ai.index(), varmap, 
					bytecodes);
			Type arr_t = ai.target().type();
			bytecodes.add(new Bytecode.ArrayLoad((Type.Array) arr_t));
			maxStack = Math.max(ms_arr, ms_idx + ClassFile.slotSize(arr_t));
		} else if (expr instanceof Expr.Invoke) {
			maxStack = translateInvoke((Expr.Invoke) expr, varmap, 
					bytecodes, true);
		} else if (expr instanceof Expr.UnOp) {
			maxStack = translateUnaryOp((Expr.UnOp) expr, varmap,
					bytecodes);
		} else if (expr instanceof Expr.BinOp) {
			maxStack = translateBinaryOp((Expr.BinOp) expr, varmap,
					bytecodes);
		} else if (expr instanceof Expr.InstanceOf) {
			Expr.InstanceOf iof = (Expr.InstanceOf) expr;
			int ms = translateExpression(iof.lhs(), varmap, 
					bytecodes);
			bytecodes.add(new Bytecode.InstanceOf(iof.rhs()));
			maxStack = ms;
		} else if (expr instanceof Expr.Cast) {
			maxStack = translateCast((Expr.Cast) expr, varmap,
					bytecodes);
		} else {
			throw new RuntimeException("Unknown expression encountered ("
					+ expr + ")");
		}
		return maxStack;
	}

	public int translateClassVal(Expr.Class cval,  HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		if(cval.type() instanceof Type.Primitive) {
			// FIXME: fix class access to primitive types
//			bytecodes.add(new Bytecode.GetField(Typing
//					.boxedType((Type.Primitive) cval.classType), "TYPE", Type
//					.referenceType("java.lang", "Class"), Bytecode.STATIC));
		} else {
			bytecodes.add(new Bytecode.LoadConst(cval.type()));
		}
		return 1;
	}
	
	public int translateDeref(Expr.Deref def, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {

		int maxStack = 0;
		Type tmp_t = def.target().type();

		if (tmp_t instanceof Type.Clazz) {
			Type.Clazz lhs_t = (Type.Clazz) tmp_t;

			if (def.isStatic()) {
				// This is a static field load					
				bytecodes.add(new Bytecode.GetField(lhs_t, def.name(),
						def.type(), Bytecode.STATIC));
				maxStack = ClassFile.slotSize(def.type());		
			} else {
				// Non-static field load
				maxStack = translateExpression(def.target(), varmap,
						bytecodes);

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
				
				maxStack = Math.max(maxStack,ClassFile.slotSize(def.type()));	
			}
		} else if (tmp_t instanceof Type.Array && def.name().equals("length")) {
			maxStack = translateExpression(def.target(), varmap,
					bytecodes);
			bytecodes.add(new Bytecode.ArrayLength());
		} else {
			throw new RuntimeException(
					"Attempt to dereference variable with type "
							+ tmp_t.toString());
		}

		return maxStack;
	}

	public int translateArrayVal(Expr.Array av, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		
		List<Expr> params = new ArrayList<Expr>();
		params.add(new Expr.Int(av.values().size()));
		int maxStack = translateNew(new Expr.New(av.type(), params, null),
				varmap, bytecodes, true);

		int index = 0;
		for (Expr e : av.values()) {
			bytecodes.add(new Bytecode.Dup(av.type()));
			bytecodes.add(new Bytecode.LoadConst(index++));
			int ms = translateExpression(e, varmap, bytecodes);
			bytecodes.add(new Bytecode.ArrayStore((Type.Array) av.type()));
			maxStack = Math.max(maxStack, 3 + ms);
		}

		return maxStack;
	}

	public int translateNew(Expr.New news, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes, boolean needReturnValue) {

		int maxStack = 0;

		if (news.type() instanceof Type.Clazz) {
			Type.Clazz type = (Type.Clazz) news.type();

			bytecodes.add(new Bytecode.New(news.type()));
			bytecodes.add(new Bytecode.Dup(news.type()));

			maxStack = 3;
			int usedStack = 3;

			ArrayList<Type> paramTypes = new ArrayList<Type>();
			for (Expr p : news.parameters()) {
				int ms = translateExpression(p, varmap, bytecodes);
				maxStack = Math.max(ms + usedStack, maxStack);
				usedStack += ClassFile.slotSize(p.type());
				paramTypes.add(p.type());
			}

						// call the appropriate constructor
			bytecodes.add(new Bytecode.Invoke(type, "<init>", news.funType(),
					Bytecode.SPECIAL));
		} else if (news.type() instanceof Type.Array) {
			int usedStack = 0;

			for (Expr p : news.parameters()) {
				int ms = translateExpression(p, varmap, bytecodes);
				maxStack = Math.max(ms + usedStack, maxStack);
				usedStack += ClassFile.slotSize(p.type());
			}

			bytecodes.add(new Bytecode.New(news.type(), news.parameters()
					.size()));
		}

		if (!needReturnValue) {
			// the return value is not required, so we need to pop it from
			// the stack
			bytecodes.add(new Bytecode.Pop(news.type()));
		}
		return maxStack;
	}

	protected int translateBinaryOp(Expr.BinOp bop, HashMap<String, Integer> varmap,
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
			int maxStack = translateConditionalBranch(bop, trueLabel, varmap,
					bytecodes);
			bytecodes.add(new Bytecode.LoadConst(0));
			bytecodes.add(new Bytecode.Goto(exitLabel));
			bytecodes.add(new Bytecode.Label(trueLabel));
			bytecodes.add(new Bytecode.LoadConst(1));
			bytecodes.add(new Bytecode.Label(exitLabel));
			return maxStack;
		}
		}

		// must be a standard arithmetic operation.
		int ms_lhs = translateExpression(bop.lhs(), varmap,
				bytecodes);
		int ms_rhs = translateExpression(bop.rhs(), varmap,
				bytecodes);

		bytecodes.add(new Bytecode.BinOp(bop.op(), bop.type()));
		return Math.max(ms_lhs, ms_rhs + ClassFile.slotSize(bop.type()));
	}

	protected int translateUnaryOp(Expr.UnOp uop, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		// second, translate the operation.
		// FIXME: resolve operator type

		switch (uop.op()) {
		case Expr.UnOp.NOT:
			String trueLabel = "CL" + condLabelCount++;
			String exitLabel = "CL" + condLabelCount++;
			int ms = translateConditionalBranch(uop, trueLabel, varmap,
					bytecodes);
			bytecodes.add(new Bytecode.LoadConst(0));
			bytecodes.add(new Bytecode.Goto(exitLabel));
			bytecodes.add(new Bytecode.Label(trueLabel));
			bytecodes.add(new Bytecode.LoadConst(1));
			bytecodes.add(new Bytecode.Label(exitLabel));
			return ms;
		}

		// first, translate the expression.
		int maxStack = translateExpression(uop.expr(), varmap,
				bytecodes);

		switch (uop.op()) {
		case Expr.UnOp.INV:
			bytecodes.add(new Bytecode.LoadConst(new Integer(-1)));
			bytecodes
					.add(new Bytecode.BinOp(Bytecode.BinOp.XOR, new Type.Int()));
			maxStack = Math.max(maxStack, 2);
			break;
		case Expr.UnOp.NEG:
			bytecodes.add(new Bytecode.Neg(uop.type()));
			break;
		default:
			throw new RuntimeException("Unknown unary expression encountered ("
					+ uop + ")");
		}

		return maxStack;
	}

	protected int translateCast(Expr.Cast cast, HashMap<String, Integer> varmap,
			ArrayList<Bytecode> bytecodes) {
		
		int maxStack = translateExpression(cast.expr(), varmap, bytecodes);

		Type srcType = cast.expr().type();
		// Now, do implicit conversions
		if (cast.type() instanceof Type.Primitive
				&& srcType instanceof Type.Primitive) {
			bytecodes.add(new Bytecode.Conversion((Type.Primitive) srcType,
					(Type.Primitive) cast.type()));
		} else {
			bytecodes.add(new Bytecode.CheckCast(cast.type()));
		} 
		
		return Math.max(maxStack, ClassFile.slotSize(cast.type()));
	}

}
