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
// (C) David James Pearce, 2007. 

/**
 * This class represents the code of a Java method.
 */

package jkit.jkil;

import java.util.*;

import jkit.compiler.ClassTable;
import jkit.compiler.FieldNotFoundException;
import jkit.compiler.MethodNotFoundException;
import jkit.util.*;
import jkit.util.graph.DirectedAdjacencyList;

/**
 * <p>
 * This class provides a control-flow graph representation of method bodies. At
 * an abstract level, a flow-graph is simply a weighted directed graph, whose
 * edges connect statements in the method together. Thus, flow graphs can be
 * thought of simply as sets of edges, where each edge is a triple <i>(x,y,c)</i>
 * that goes from <i>x</i> to <i>y</i> if condition <i>c</i> holds (which
 * maybe <code>null</code>, if there is no condition).
 * </p>
 * <p>
 * For example, we can iterate the edges of a flow graph quite easily as
 * follows:
 * </p>
 * 
 * <pre>
 *  FlowGraph cfg = ...;
 * 
 *  for(Triple&lt;FlowGraph.Point,FlowGraph.Point,FlowGraph.Expr&gt; edge : cfg) {
 *   ...
 *  }
 * </pre>
 * 
 * <p>
 * See the <a
 * href="http://www.mcs.vuw.ac.nz/~djp/jkit/manual.html#jkil.flowgraphs">JKit
 * Reference Manual</a> for more information.
 * </p>
 * 
 * @author djp
 * 
 */
public class FlowGraph extends 
	DirectedAdjacencyList<FlowGraph.Point,Triple<FlowGraph.Point,FlowGraph.Point,FlowGraph.Expr>> {

	/**
	 * The entry point to the CFG
	 */
	private Point entry; 
	
	/**
	 * This contains the complete set of local variable definitions. Since each
	 * local variable in a method must have a unique name, there is no chance of
	 * ambiguity. The variables are added to the list in the order they are
	 * seen.
	 */	
	private final ArrayList<LocalVarDef> localVars = new ArrayList<LocalVarDef>();	
	
	/**
	 * The list of active regions for this control-flow graph.
	 */
	private final LinkedList<Region> regions = new LinkedList<Region>();
	
	/**
     * Construct a Code object representing the body of a method. In this case,
     * no structured information is provided about the method. Without this
     * information, this method cannot be printed using, for example,
     * JavaFileWriter. However, other classes, such as ClassFileWriter, which do
     * not require structured control-flow information are OK.
     * 
     * @param localVariables -
     *            list of local variables in the order they are declared, with
     *            parameters coming first.
     */
	public FlowGraph(List<LocalVarDef> localVariables) {
		this.localVars.addAll(localVariables);
		this.entry = null;
	}
	
	/**
     * Construct a Code object representing the body of a method. In this case,
     * no structured information is provided about the method. Without this
     * information, this method cannot be printed using, for example,
     * JavaFileWriter. However, other classes, such as ClassFileWriter, which do
     * not require structured control-flow information are OK.
     * 
     * @param localVariables -
     *            list of local variables in the order they are declared, with
     *            parameters coming first.
     * @param entry
     *            entry point of method in control-flow graph 
     */
	public FlowGraph(List<LocalVarDef> localVariables, 
				FlowGraph.Point entry) {
		this.localVars.addAll(localVariables);
		this.entry = entry;
	}
	
	/**
     * A LocalVarDef is a symbol table entry for a local variable. It can be
     * thought of as a declaration for that variable, including its type,
     * modifiers, name and whether or not it is a parameter to the method.
     * 
     * @author djp
     */
	public static class LocalVarDef {
		private Type type;
		private String name;
		private int modifiers;
		private boolean parameter;
		
		public LocalVarDef(String name, Type type, int modifiers, boolean parameter) {
			this.name = name;
			this.type = type;
			this.modifiers = modifiers;
			this.parameter=parameter;
		}
		
		/**
		 * Set the type of this local variable.
		 * 
		 * @param type
		 */
		public void setType(Type type) { this.type = type; }
		
		/**
		 * Get the type of this local variable
		 * 
		 * @return
		 */
		public Type type() { return type; }
		
		/**
		 * Set the name of this local variable.
		 * 
		 * @param type
		 */
		public void setName(String name) { this.name = name; }
		
		/**
		 * Get the name of this local variable
		 * 
		 * @return
		 */
		public String name() { return name; }
		
		/**
         * Set the modifiers of this local variable. Use
         * java.lang.reflect.Modifier for this.
         * 
         * @param type
         */
		public void setModifiers(int modifiers) { this.modifiers = modifiers; }
		
		/**
		 * Get modifiers of this local variable
		 * 
		 * @return
		 */
		public int modifiers() { return modifiers; }
		
		/**
         * Set the parameter status of this local variable.
         * 
         * @param type
         */
		public void setParameter(boolean parameter) { this.parameter = parameter; }
		
		/**
		 * Check with this local variable is a parameter or not.
		 * 
		 * @return
		 */
		public boolean isParameter() { return parameter; }	
	}
			
	/**
	 * Return the list of nodes currently used in this flow graph. Observe you
	 * may iterate this list, whilst modifying the flow graph without risk of
	 * concurrency modification exceptions.
	 */
	public Set<Point> domain() {
		HashSet<Point> domain = new HashSet<Point>();
		domain.add(entry); // ensure it's in there!
		for(Triple<Point,Point,Expr> edge : this) {
			domain.add(edge.first());
			domain.add(edge.second());
		}
		return domain; }
	
	/**
     * Return the list of regions currently active. Note that innermost regions
     * have the lowest indices, whilst outermost regions have higher indices.
     * This reflects the fact that we generally want to iterate from inner
     * regions to outer regions, rather than the other way around.
     */
	public List<Region> regions() { return regions; }
			
	/**
     * Return the list of local variables used in this code body. Note, the
     * first n variables are always the parameters to the method; furthermore,
     * the variables are ordered in the order they are seen in the source file.
     * It may also occur that there are multiple variables with the same name,
     * which represents the (erroneous) declaration of two variables with the
     * same name.
     * 
     * @return
     */
	public List<LocalVarDef> localVariables() { return localVars; }
		
	/**
	 * Return the local var definition for a given variable.
	 * 
	 * @return the corresponding LocalVarDef, or null if there is no match.
	 */
	public LocalVarDef localVariable(String name) { 
		for(LocalVarDef vd : localVars) {
			if(vd.name.equals(name)) {
				return vd;
			}
		}
		return null;
	}
	/**
     * Add a new local varaible to the flow graph. The local variable must not
     * already be defined within the list of local variables for this method.
     * 
     * @param x
     */
	public void add(LocalVarDef x) {
		localVars.add(x);
	}
			
	/**
	 * Returns the entry point of the CFG.
	 * @return
	 */
	public FlowGraph.Point entry() { return entry; }
	
	/**
	 * Set the entry point of the CFG.
	 * @return
	 */
	public void setEntry(Point p) {		
		entry = p; 
	}
			
	/**
     * A FlowGraph.Point identifies a position within the flow graph. It also
     * identifies a position within the originating source file --- which is
     * useful for producing good error messages.
     * 
     * @author djp
     * 
     */
	public static class Point implements Comparable<Point> { 
		private String file;
		private int line;
		private int column;
		private FlowGraph.Stmt statement;
		
		public Point(FlowGraph.Stmt stmt, String file, int line, int column) {
			this.file = file;
			this.line = line;
			this.column = column;
			this.statement = stmt;
		}
		
		public Point(FlowGraph.Stmt stmt, String file, int line) {
			this.file=file;
			this.line = line;
			this.column = -1;
			this.statement = stmt;
		}
		
		public Point(FlowGraph.Stmt stmt, String file) {
			this.file=file;
			this.line = -1;
			this.column = -1;
			this.statement = stmt;
		}		
		
		public Point(FlowGraph.Stmt stmt) {
			this.file="unknown";
			this.line = -1;
			this.column = -1;
			this.statement = stmt;
		}
		
		public void updateStmt(Stmt s) {
			statement = s;
		}
		
		/**
         * Get statement at this point
         * 
         * @return statement (or null, if none)
         */
		public Stmt statement() { return statement; }
		
		/**
		 * Set statement at this point
		 * 
		 * @param stmt statement (may be null)
		 */
		public void setStatement(Stmt stmt) { statement = stmt; }
		
		/**
		 * Get source file where this point originates from.
		 * 
		 * @return 
		 */
		public String source() { return file; }
		
		/**
		 * Set originating source file.
		 * 
		 * @param src
		 */
		public void setSource(String src) { file = src; }
		
		/**
		 * Get line number in originating source file for this point.
		 * 
		 * @return 
		 */		
		public int line() { return line; }
		
		/**
		 * Set line number in originating source file for this point.
		 * 
		 * @return 
		 */
		public void setLine(int line) { this.line = line; }
		
		/**
		 * Get column number in originating source file for this point.
		 * 
		 * @return 
		 */
		public int column() { return column; }
		
		/**
		 * Set column number in originating source file for this point.
		 * 
		 * @return 
		 */
		public void setColumn(int column) { this.column = column; }
						
		public String toString() {
			if(statement == null) {
				return file + ":" + 
				Integer.toString(line) + ":" +
				Integer.toString(column);			
			} else {
				return "\"" + statement + "\":" + file + ":" + 
					Integer.toString(line) + ":" +
					Integer.toString(column);
			}
		}
		
		public boolean equals(Object o) {
			return this == o;
		}
		
		public int compareTo(Point p) {
			int ret = p.file.compareTo(this.file);
			if (ret != 0) return ret;
			ret = this.line - p.line;
			if (ret != 0) return ret;
			ret = this.column - p.column;
			return ret;
		}
	}
	
	/**
     * A region identifies a set of points within the flow graph, and associates
     * meta-information with them. The intention is that the flow graph makes
     * sense without any region information, but this information can help in
     * various ways. For example, by allowing exceptional edges to be introduced
     * during an intermediate pass; or, by identifying the live regions of a
     * particular variable so that register allocation can also be a pass; or,
     * by identifying blocks of points that correspond to a high-level language
     * construct to aid with error reporting and in reconstructing the original
     * source version.
     */
	public static class Region extends HashSet<Point> {
		public Region() {}
		
		public Region(Collection<Point> points) {
			super(points);
		}
		
		public static final long serialVersionUID = 1l;
	}
		
	/**
     * A catch indicates a region in which a particular exception handler is
     * active. This is useful, for example, since it allows us to delay
     * introducing exceptional edges to a separate pass, which can then utilise
     * this information to add the appropriate edges.
     * 
     * @author djp
     * 
     */
	public static class CatchBlock extends Region {		
		/**
         * Indicates the entry point of the exception handler. That is, the code
         * point where exceptions covered by this block are directed.
         */
		public Point entry;
				
		/**
		 * The type of exception being caught by this block.
		 */
		public Type.Reference type;
		
		/**
         * Construct CatchBlock, for representing a region of nodes which a
         * catch block operates over.
         * 
         * @param entry
         *            entry point of catch block
         * @param type
         *            of exception being caught by this block.
         * @param points
         *            set of points in which exceptions by this block are caught
         */		
		public CatchBlock(Point entry, Type.Reference type, Collection<Point> points) {
			super(points);
			this.entry = entry;
			this.type = type;
		}
		
		public static final long serialVersionUID = 1l;
	}

	/**
	 * Region representing the body of a loop. This includes all nodes in the body, but not e.g. initialisers.
	 * @author andrew
	 */
	public static class LoopBody extends Region {
		private static final long serialVersionUID = 1L;

		/**
		 * Head node of the loop.
		 */
		public final Point head;

		/**
		 * Construct region to represent the body of a loop.
		 *
		 * @param head The head node of the loop.
		 * @param points All nodes in the body of the loop (including head).
		 */
		public LoopBody(Point head, Collection<Point> points) {
			super(points);
			this.head = head;
		}

		public String toString() {
			return "Loop head: " + head + "\n" + super.toString() + "\n----------";
		}
	}

	/**
	 * Split point p1 into two points, p1 and p2, where p2 becomes the only
	 * successor of p1 and the original successors of p1 becomes successors of
	 * p2.  Any regions that p1 is in, p2 is also in.
	 * 
	 * @param p1
	 * @param p2
	 */
	public Point split(Point p1) {
		Point p2 = new Point(null,p1.source(),p1.line(),p1.column());
		// calculate successors of p1
		ArrayList<Triple<Point,Point,Expr>> succs = new ArrayList<Triple<Point,Point,Expr>>();
		for(Triple<Point,Point,Expr> e : this) {
			if(e.first() == p1) {
				succs.add(e);
			}
		}
		removeAll(succs);
		// add edge from p1->p2
		add(new Triple<Point,Point,Expr>(p1,p2,null));
		// add edges from p2 -> successors of p1
		for(Triple<Point,Point,Expr> e : succs) {
			add(new Triple<Point,Point,Expr>(p2,e.second(),e.third()));
		}
		// finally, update any region containing p1 to also contain p2
		for(Region r : regions) {
			if(r.contains(p1)) { r.add(p2); }
		}
		return p2;
	}
	
	// ====================================================
	// STATEMENTS
	// ====================================================
	
	/**
     * Represents the class of imperative statements allowed.
     * 
     * @author djp
     * 
     */		
	public static interface Stmt { }
	
	/**
     * This statement is a no-operation. It's useful sometimes as a place holder
     * for the head of loops/ifs and switches
     * 
     * @author djp
     * 
     */
	public static final class Nop implements Stmt {
		public Nop() { }
		public String toString() { return "nop"; }
		public boolean equals(Object o) {
			return o instanceof Nop;
		}
		public int hashCode() { return "nop".hashCode(); }
	}
	
	/**
     * Represents assignments. 
     * 
     * @author djp
     * 
     */
	public static final class Assign implements Stmt {
		public final LVal lhs;
		public final Expr rhs;
		
		public Assign(LVal lhs, Expr rhs) {
			this.lhs=lhs;
			this.rhs=rhs;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Assign) {
				Assign a = (Assign) o;
				return lhs.equals(a.lhs) && rhs.equals(a.rhs); 
			}
			return false;
		}
		public int hashCode() { return lhs.hashCode() ^ rhs.hashCode(); }
		
		public String toString() { return lhs + " = " + rhs; }				
	}
	
	/**
	 * Represents return statements.
	 */	
	public static final class Return implements Stmt {
		public final Expr expr;
		public Return(Expr rhs) {
			this.expr = rhs;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Return) {
				Return t = (Return) o;				
				return expr == t.expr || (expr != null && expr.equals(t.expr)); 
			}
			return false;
		}
		
		public int hashCode() {
			if(expr == null) { return 0; }
			else return expr.hashCode(); 
		}
		
		public String toString() { 
			if(expr == null) { return "return"; }
			else { return "return " + expr; }
		}				
	}
	
	/**
	 * Represents throw statements.
	 * 
	 * @author djp
	 *
	 */
	public static class Throw implements Stmt {		
		public final Expr expr;
		
		public Throw(Expr expr) {			
			this.expr = expr;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Throw) {
				Throw t = (Throw) o;				
				return expr.equals(t.expr); 
			}
			return false;
		}
		
		public int hashCode() { return expr.hashCode(); }
		
		public String toString() {
			return "throw " + expr; 
		}
	}
		
	
	/**
     * Used for synchronised blocks. Corresponds to the monitoenter Java
     * bytecode.
     */
	public static class Lock implements Stmt {		
		public final LocalVar var;
		
		public Lock(LocalVar var) {			
			this.var = var;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Lock) {
				Lock t = (Lock) o;				
				return var.equals(t.var); 
			}
			return false;
		}
		
		public int hashCode() { return var.hashCode(); }
		
		public String toString() {
			return "lock " + var; 
		}
	}
	
	/**
     * Used for synchronised blocks. Corresponds to the monitorexit Java
     * bytecode.
     */
	public static class Unlock implements Stmt {		
		public final LocalVar var;
				
		public Unlock(LocalVar var) {			
			this.var = var;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Unlock) {
				Unlock t = (Unlock) o;				
				return var.equals(t.var); 
			}
			return false;
		}
		
		public int hashCode() { return var.hashCode(); }
		
		public String toString() {
			return "unlock " + var; 
		}
	}
	
	// ====================================================
	// EXPRESSIONS
	// ====================================================
	
	/**
     * Represents all expressions in the code
     * 
     * @author djp
     * 
     */	
	public static abstract class Expr { 
		public final Type type;
		public Expr(Type type) { this.type = type; }
	}
	
	/**
     * Represents Unary Arithmetic Operators
     * 
     * @author djp
     * 
     */
	public static final class UnOp extends Expr {
		public static final int NOT = 0;
		public static final int INV = 1;
		public static final int NEG = 2;
		public static final int PREINC = 3;
		public static final int PREDEC = 4;
		public static final int POSTINC = 5;
		public static final int POSTDEC = 6;
		
		public static final String[] opstr={"!","~","-","++","--","++","--"};
		
		public final Expr expr;		
		public final int op;
		
		public UnOp(int op, Expr expr) {
			this(op,expr,Type.anyType());			
		}
		
		public UnOp(int op, Expr expr, Type type) {
			super(type);
			this.expr = expr;
			this.op=op;
		}

		public boolean equals(Object o) {
			if(o instanceof UnOp) {
				UnOp u = (UnOp) o;				
				return op == u.op && expr.equals(u.expr); 
			}
			return false;
		}
		
		public int hashCode() { return op ^ expr.hashCode(); }
		
		public String toString() {						
			if(op >= POSTINC) {
				return "(" + expr.toString() + opstr[op] + ")[" + type + "]";
			} else {
				return "(" + opstr[op] + expr.toString() + ")[" + type + "]";
			}
		}		
	}	
	
	/**
     * Represents an InstanceOf binary operation.
     * 
     * @author djp
     * 
     */
	public static final class InstanceOf extends Expr {
		public final Expr lhs;		
		public final Type rhs;		
		
		public InstanceOf(Expr lhs, Type rhs) {
			super(Type.booleanType());
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public boolean equals(Object o) {
			if(o instanceof InstanceOf) {
				InstanceOf u = (InstanceOf) o;				
				return rhs.equals(u.rhs) && lhs.equals(u.lhs); 
			}
			return false;
		}
		
		public int hashCode() { return lhs.hashCode() ^ rhs.hashCode(); }
		
		public String toString() {
			return "(" + lhs.toString() + " instanceof " + rhs + ")[" + type + "]";
		}
	}
	
	/**
     * A Binary Operator.  E.g. +.-,*,/,<,<=,>,?=,==,!=, etc.
     * 
     * @author djp
     * 
     */
	public static final class BinOp extends Expr {
		// BinOp Constants
		public static final int ADD = 0;
		public static final int SUB = 1;
		public static final int MUL = 2;
		public static final int DIV = 3;
		public static final int MOD = 4;
		public static final int SHL = 5;
		public static final int SHR = 6;
		public static final int USHR = 7;
		public static final int AND = 8;
		public static final int OR = 9;
		public static final int XOR = 10;
		
		public static final int LT = 11;
		public static final int LTEQ = 12;
		public static final int GT = 13;
		public static final int GTEQ = 14;
		public static final int EQ = 15;
		public static final int NEQ = 16;
		public static final int LAND = 17;
		public static final int LOR = 18;
		
		public static final int CONCAT = 19; // string concatenation
				
		public static final String[] opstr = {"+", "-", "*", "/", "%", 
			"<<", ">>", ">>>", "&", "|", "^", "<", "<=", ">", ">=", "==", "!=", "&&", "||", "++"};
		
		public final Expr lhs;
		public final Expr rhs;
		public final int op;				
		
		public BinOp(int op, Expr lhs, Expr rhs) {
			this(op,lhs,rhs,Type.anyType());
		}
		
		public BinOp(int op, Expr lhs, Expr rhs, Type type) {
			super(type);
			this.lhs = lhs;
			this.rhs = rhs;
			this.op=op;
		}
		
		public boolean equals(Object o) {
			if(o instanceof BinOp) {
				BinOp b = (BinOp) o;				
				return op == b.op && lhs.equals(b.lhs) && rhs.equals(b.rhs); 
			}
			return false;
		}
		
		public int hashCode() { return op ^ lhs.hashCode() ^ rhs.hashCode(); }
		
		public String toString() {						
			return "(" + lhs.toString() + " " + opstr[op] + " " + rhs.toString() + ")[" + type +"]";
		}				
	}
	
	public static final class TernOp extends Expr implements Stmt {
		
		public final Expr cond;
		public final Expr toption;
		public final Expr foption;
		
		public TernOp(Expr con, Expr tOption, Expr fOption) {
			this(con, tOption, fOption, Type.anyType());
		}
		
		public TernOp(Expr con, Expr tOption, Expr fOption, Type type) {
			super(type);
			cond = con;
			toption = tOption;
			foption = fOption;
		}
		
		public boolean equals(Object o) {
			if(o instanceof TernOp) {
				TernOp t = (TernOp) o;
				return t.cond.equals(cond) && t.toption.equals(toption) && t.foption.equals(foption);
			}
			return false;
		}
		
		public int hashCode() {
			return cond.hashCode() ^ toption.hashCode() ^ foption.hashCode();
		}
		
		public String toString() {
			return "(" + cond + ") ? " + toption + " : " + foption + "[" + type + "]";
		}
		
	}
	/**
     * A value represents a constant, such as "hello" or 1 or 1.0.
     * 
     * @author djp
     * 
     */
	public static abstract class Value extends Expr { 
		public Value(Type type) {
			super(type);
		}
	}

	/**
	 * Represents a numerical constant
	 * 
	 * @author djp
	 *
	 */
	public static class Number extends Value {
		public final int value;
		
		public Number(int value, Type type) {
			super(type);
			this.value=value;
		}
		
		public boolean equals(Object o) {
			if(o.getClass() == this.getClass()) {
				Number v = (Number) o;				
				return value == v.value; 
			}
			return false;
		}
		
		public int hashCode() { return value; }
		
		public String toString() { return Integer.toString(value); }
	}
	
	/**
	 * A boolean constant.
	 * 
	 * @author djp
	 *
	 */
	public static final class BoolVal extends Number {
		public BoolVal(boolean value) {
			super(value?1:0, Type.booleanType());
		}
		
		public boolean get() {
			return value==1;
		}
		
		public String toString() { return Boolean.toString(value==1) + "[" + type + "]"; }				
		
	}
	
	/**
	 * Represents a character constant.
	 * 
	 * @author djp
	 *
	 */
	public static final class CharVal extends Number {
		public CharVal(char value) {
			super(value, Type.charType());
		}
		
		public char get() {
			return (char)value;
		}
		
		public String toString() { return Character.toString((char)value) + "[" + type + "]"; }				
	}
	
	/**
	 * Represents a byte constant.
	 * 
	 * @author djp
	 *
	 */
	public static final class ByteVal extends Number {
		public ByteVal(byte value) {
			super(value, Type.byteType());
		}
		
		public byte get() {
			return (byte)value;
		}
		
		public String toString() { return Byte.toString((byte)value) + "[" + type + "]"; }				
	}
	
	/**
	 * Represents a short constant.
	 * @author djp
	 *
	 */
	public static final class ShortVal extends Number {
		public ShortVal(short value) {
			super(value, Type.shortType());
		}
		
		public short get() {
			return (short)value;
		}
		
		public String toString() { return Short.toString((short)value) + "[" + type + "]"; }				
	}

	/**
     * Represents an int constant.
     * 
     * @author djp
     * 
     */	
	public static final class IntVal extends Number {
		public IntVal(int value) {
			super(value, Type.intType());
		}
		
		public int get() {
			return value;
		}
		
		public String toString() { return Integer.toString(value) + "[" + type + "]"; }				
	}

	/**
     * Represents a long Constant.
     * 
     * @author djp
     * 
     */
	public static final class LongVal extends Value {
		public final long value;
		
		public LongVal(long value) {
			super(Type.longType());
			this.value=value;
		}
		
		public boolean equals(Object o) {
			if(o instanceof LongVal) {
				LongVal v = (LongVal) o;				
				return value == v.value; 
			}
			return false;
		}
		
		public int hashCode() { return (int) value; }
		
		public String toString() { return Long.toString(value) + "[" + type + "]"; }				
	}
	
	/**
     * A Float Constant.
     * 
     * @author djp
     * 
     */
	public static final class FloatVal extends Value {
		public final float value;
		
		public FloatVal(float value) {
			super(Type.floatType());
			this.value=value;
		}
		
		public boolean equals(Object o) {
			if(o instanceof FloatVal) {
				FloatVal v = (FloatVal) o;				
				return value == v.value; 
			}
			return false;
		}
		
		public int hashCode() { return Float.toString(value).hashCode(); }
		
		public String toString() { return java.lang.Float.toString(value)  + "[" + type + "]"; }
	}

	/**
     * A Double Constant.
     * 
     * @author djp
     * 
     */
	public static final class DoubleVal extends Value {
		public final double value;
		
		public DoubleVal(double value) {
			super(Type.doubleType());
			this.value=value;
		}
		
		public boolean equals(Object o) {
			if(o instanceof FloatVal) {
				FloatVal v = (FloatVal) o;				
				return value == v.value; 
			}
			return false;
		}
		
		public int hashCode() { return Double.toString(value).hashCode(); }
		
		public String toString() { return java.lang.Double.toString(value)  + "[" + type + "]"; }
	}
	
	/**
     * A String Constant.
     * 
     * @author djp
     * 
     */
	public static final class StringVal extends Value {
		public final String value;
		
		public StringVal(String value) {
			super(Type.referenceType("java.lang","String"));
			this.value=value;
		}
		
		public boolean equals(Object o) {
			if(o instanceof StringVal) {
				StringVal v = (StringVal) o;				
				return value.equals(v.value); 
			}
			return false;
		}
		
		public int hashCode() { return value.hashCode(); }
			
		public String toString() { return "\"" + value + "\"[" + type + "]"; }
	}		
	
	/**
     * The null Constant.
     * 
     * @author djp
     * 
     */
	public static final class NullVal extends Value {
		public NullVal() { super(Type.nullType()); }
		
		public boolean equals(Object o) {
			return o instanceof NullVal;
		}
		
		public int hashCode() { return "null".hashCode(); }
		
		public String toString() { return "null[" + type + "]"; }
	}		
	
	/**
	 * Represents a Class Constant
	 * 
	 */
	public static final class ClassVal extends Value {
		public final Type classType;

		public ClassVal(Type type) {
			// FIXME: bug regarding type parameters for Class
			super(Type.referenceType("java.lang", "Class"));
			this.classType = type;
		}

		public boolean equals(Object o) {
			if (o instanceof ClassVal) {
				ClassVal v = (ClassVal) o;
				return type.equals(v.type);
			}
			return false;
		}

		public int hashCode() {
			return type.hashCode();
		}

		public String toString() {
			return classType.toString() + ".class" + "[" + type + "]";
		}
	}
	
	/**
     * An array constant (used for array initialisers only).
     * 
     * @author djp
     * 
     */
	public static final class ArrayVal extends Value {
		public final List<Expr> values;
		
		public ArrayVal(List<Expr> values) {
			super(Type.anyType());
			this.values = values;
		}
		
		public ArrayVal(List<Expr> values, Type type) {
			super(type);
			this.values = values;
		}
		
		public boolean equals(Object o) {
			if(o instanceof ArrayVal) {
				ArrayVal av = (ArrayVal) o;
				if(av.values.size() != values.size()) { return false; }
				for(int i=0;i!=values.size();++i) {
					if(!av.values.get(i).equals(values.get(i))) {
						return false;
					}
				}
				return av.type.equals(type);
			}
			return false;
		}
		
		public int hashCode() { 
			int hc = 0;
			for(Expr e : values) {
				hc ^= e.hashCode();
			}
			return type.hashCode() ^ hc; 
		}
			
		public String toString() {
			String r = "{ ";
			boolean firstTime=true;
			for(Expr e : values) {
				if(!firstTime) { r += ", "; }
				firstTime=false;
				r += e.toString();
			}
			return r + "}[" + type +"]";		
		}
	}
	
	/**
     * Represents the new operator. The parameters provided are either passed to
     * that object's constructor, or are used to determine the necessary array
     * dimensions (e.g. in new array[x+1]).
     * 
     * @author djp
     * 
     */
	public static final class New extends Value implements Stmt {		
		public final ArrayList<Expr> parameters;
		
		public New(Type type, List<Expr> parameters) {
			super(type);			
			this.parameters = new ArrayList<Expr>(parameters);			
		}
		
		public New(Type type, Expr... parameters) {
			super(type);			
			this.parameters = new ArrayList<Expr>();
			Collections.addAll(this.parameters,parameters);
		}
		
		public boolean equals(Object o) {
			if(o instanceof New) {
				New v = (New) o;	
				if(parameters.size() != v.parameters.size()) {
					return false;
				}
				for(int i=0;i!=parameters.size();++i) {
					if(!parameters.get(i).equals(v.parameters.get(i))) {
						return false;
					}
				}
				return type.equals(v.type); 
			}
			return false;
		}
		
		public int hashCode() { return type.hashCode(); }
		
		public String toString() {			
			if(type instanceof Type.Array) {
				// First, identity the true element type
				Type.Array t = (Type.Array) type;
				Type elemType = t.elementType();
				int count = 0;
				while(elemType instanceof Type.Array) {
					t = (Type.Array) elemType;
					elemType = t.elementType();
					count++;
				}
				String rest="";
				for(Expr e : parameters) {
					rest += "[" + e.toString() + "]";
					count--;
				}
				for(;count >= 0;count=count-1) { rest += "[]"; }
				return "new " + elemType.toString() + rest;
			} else {
				String rest = "(";			
				boolean firstTime=true;
				for(Expr e : parameters) {
					if(!firstTime) { rest += ", "; }
					firstTime=false;
					rest += e.toString();
				}
				return "new " + type.toString() + rest + ")" + "[" + type + "]";
			}
		}
	}
	
	/**
	 * Represents an explicit cast.
	 * 
	 * @author djp
	 *
	 */
	public static class Cast extends Expr {		
		public final Expr expr;
		
		public Cast(Type type,  Expr expr) {
			super(type);
			this.expr = expr;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Cast) {
				Cast v = (Cast) o;				
				return type.equals(v.type) && expr.equals(v.expr); 
			}
			return false;
		}
		
		public int hashCode() { return type.hashCode() ^ expr.hashCode(); }
		
		public String toString() {			
			return "((" + type.toString() + ") " + expr.toString() + ")"; 
		}
	}
	
	/**
	 * Represents a method call. The method call be either "polymorphic", or
	 * "non-polymorphic". The former means the method will be called on the
	 * dynamic type of the received, whilst the latter means that the method
	 * will be called directly on the static type of the receiver.
	 * 
	 * @author djp
	 * 
	 */
	public static class Invoke extends Expr implements Stmt {
		public final Expr target;
		public final String name;		
		public final ArrayList<Expr> parameters;
		
		// The need for the typeParameters here is annoying, since they
        // eventually get folded into the method type.
		public final ArrayList<Type.Variable> typeParameters;
		public final boolean polymorphic;
		
		/**
		 * Construct a polymorphic method, with unknown return type.
		 * 
		 * @param target
		 *            The expression from which the receiver is determined
		 * @param name
		 *            The name of the method
		 * @param parameters
		 *            The parameters of the method
		 */
		public Invoke(Expr target, String name, List<FlowGraph.Expr> parameters) {
			this(target,name,parameters,Type.anyType());			
		}
		
		/**
		 * Construct a method with unknown return type which may, or may not be
		 * polymorphic.
		 * 
		 * @param target
		 *            The expression from which the receiver is determined
		 * @param name
		 *            The name of the method
		 * @param parameters
		 *            The parameters of the method
		 * @param polymorphic
		 *            Determines whether this is a polymorphic method or not.
		 */
		public Invoke(Expr target, String name, List<FlowGraph.Expr> parameters, boolean polymorphic) {
			this(target,name,parameters,Type.anyType(),polymorphic);			
		}
		
		/**
		 * Construct a polymorphic method.
		 * 
		 * @param target
		 *            The expression from which the receiver is determined
		 * @param name
		 *            The name of the method
		 * @param parameters
		 *            The parameters of the method
		 * @param type
		 *            The return type of the method.
		 */
		public Invoke(Expr target, String name, List<FlowGraph.Expr> parameters, Type type) {
			super(type);
			this.target = target;
			this.name = name;
			this.parameters = new ArrayList<FlowGraph.Expr>(parameters);
			this.typeParameters = new ArrayList<Type.Variable>();
			this.polymorphic = true;			
		}
		
		/**
		 * Construct a method which may, or may not be polymorphic.
		 * 
		 * @param target
		 *            The expression from which the receiver is determined
		 * @param name
		 *            The name of the method
		 * @param parameters
		 *            The parameters of the method
		 * @param type
		 *            The return type of the method.
		 * @param polymorphic
		 *            Determines whether this is a polymorphic method or not.
		 */
		public Invoke(Expr target, String name,
				List<FlowGraph.Expr> parameters, Type type, boolean polymorphic) {
			super(type);
			this.target = target;
			this.name = name;
			this.parameters = new ArrayList<FlowGraph.Expr>(parameters);
			this.typeParameters = new ArrayList<Type.Variable>();
			this.polymorphic = polymorphic;											
		}
		
		public boolean equals(Object o) {
			if(o instanceof Invoke) {
				Invoke v = (Invoke) o;	
				if(parameters.size() != v.parameters.size()) {
					return false;
				}
				for(int i=0;i!=parameters.size();++i) {
					if(!parameters.get(i).equals(v.parameters.get(i))) {
						return false;
					}
				}
				return name.equals(v.name) && v.polymorphic == polymorphic; 
			}
			return false;
		}
		
		public int hashCode() { return name.hashCode(); }
		
		public String toString() {
			String r="";
			if(target != null) {
				r += target.toString() + ".";
			}  else {
				r += "?.";
			}
			r += name;
			if(!polymorphic) { r += "#"; }
			r += "(";
			boolean firstTime=true;
			for(FlowGraph.Expr e : parameters) {
				if(!firstTime) { r += ","; }
				firstTime=false;
				r += e.toString();
			}			
			return r + ")"  + "[" + type + "]";			
		}
	}
		
	/**
     * Represents an expression which may appear on the left-hand side
     * of an assignment.
     * 
     * @author djp
     * 
     */
	public static abstract class LVal extends Expr { 
		public LVal(Type type) { super(type); }
	}
	
	/**
     * Represents a local variable access.
     * 
     * @author djp
     * 
     */
	public static final class LocalVar extends LVal {
		public final String name;
		
		public LocalVar(String name) {
			this(name,Type.anyType());
		}
		
		public LocalVar(String name, Type type) {
			super(type);
			this.name = name;
		}
		
		public boolean equals(Object o) {
			if(o instanceof LocalVar) {		
				LocalVar v = (LocalVar) o;
				return name.equals(v.name); 
			}
			return false;
		}
		
		public int hashCode() { return name.hashCode(); }
		
		public String toString() { return name + "[" + type + "]"; }
	}
	
	/**
	 * Represents a static reference to a class; for example, in
	 * "System.out" we have a static reference to class "System".
	 * 
	 * @author djp
	 * 
	 */
	public static final class ClassAccess extends Expr {
		public final Type.Reference clazz;
		
		public ClassAccess(Type.Reference clazz) {
			super(clazz);
			this.clazz = clazz;
		}
		
		public boolean equals(Object o) {
			if(o instanceof ClassAccess) {		
				ClassAccess v = (ClassAccess) o;
				return clazz.equals(v.clazz); 
			}
			return false;
		}
		
		public int hashCode() { return clazz.hashCode(); }
		
		public String toString() { return clazz.toString(); }
	}
	
	/**
     * Represents the act of derefencing a field.
     * 
     * @author djp
     * 
     */
	public static final class Deref extends LVal {
		public final Expr target;
		public final String name;
		
		public Deref(Expr lhs, String rhs) {
			this(lhs,rhs,Type.anyType());
		}
		public Deref(Expr lhs, String rhs, Type type) {
			super(type);
			this.target = lhs;
			this.name = rhs;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Deref) {
				Deref d = (Deref) o;				
				return target.equals(d.target) && name.equals(d.name); 
			}
			return false;
		}
		
		public int hashCode() { return target.hashCode() ^ name.hashCode(); }
		
		public String toString() {
			String l = target == null ? "?" : target.toString();
			if(target instanceof Cast) {
				return "(" + l + ")." + name;
			} else {
				return l + "." + name + "[" + type + "]";
			}
		}
	}
	
	/**
     * Represents an index into an array. E.g. A[i] is an index into array A.
     * 
     * @author djp
     * 
     */
	public static final class ArrayIndex extends LVal {
		public final Expr array;
		public final Expr idx;
		
		public ArrayIndex(Expr array, Expr idx) {
			this(array,idx,Type.anyType());
		}
		public ArrayIndex(Expr array, Expr idx, Type type) {
			super(type);
			this.array = array;
			this.idx = idx;
		}
		
		public boolean equals(Object o) {
			if(o instanceof ArrayIndex) {
				ArrayIndex a = (ArrayIndex) o;				
				return array.equals(a.array) && idx.equals(a.idx); 
			}
			return false;
		}
		
		public int hashCode() { return idx.hashCode() ^ array.hashCode(); }
		
		
		public String toString() {
			if(array instanceof Cast) {
				return "(" + array + ")[" + idx + "]";
			} else {
				return array + "[" + idx + "]";
			}
		}
	}
	
	/**
     * This class is used to represent the conditional given to exceptional
     * edges.
     * 
     * @author djp
     * 
     */
	public static class Exception extends Expr {		
		public final Type.Reference type;
		
		public Exception(Type.Reference type) {			
			super(Type.voidType());
			this.type = type;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Exception) {
				Exception e = (Exception) o;				
				return type.equals(e.type); 
			}
			return false;
		}
		
		public int hashCode() { return type.hashCode(); }
		
		public String toString() {
			return type.toString(); 
		}
	}
			
	/* ==================================================== */
	/* =============== Helper Methods ===================== */
	/* ==================================================== */
	
	/**
	 * This method constructs a typing environment for a given method.
	 * 
     * @param method
     *            The method whose typing environment to build
     * @param owner
     *            The enclosing class
     */
	public static HashMap<String,Type> buildEnvironment(Method method, Clazz owner) {
		assert method.code()!=null;
		FlowGraph cfg = method.code();		
		
		HashMap<String, Type> environment = new HashMap<String, Type>();
		
		if(!method.isStatic()) {			
			environment.put("super", owner.superClass());
		}
		
		// this variable is used in resolve static method calls as well.
		environment.put("this", owner.type());
		
		for (FlowGraph.LocalVarDef vd : cfg.localVariables()) {					
			environment.put(vd.name, vd.type); 
		}
		
		return environment;
	}
	
	/**
	 * This method constructs a typing environment for a given field.
	 * 
     * @param field
     *            The field whose typing environment to build
     * @param owner
     *            The enclosing class
     */
	public static HashMap<String,Type> buildEnvironment(Field field, Clazz owner) {
		assert field.initialiser()!=null;
		//FlowGraph.Expr expr = field.initialiser();		
		
		HashMap<String, Type> environment = new HashMap<String, Type>();
		
		if(!field.isStatic()) {			
			environment.put("super", owner.superClass());
		}
		
		// this variable is used in resolve static method calls as well.
		environment.put("this", owner.type());
		
		return environment;
	}
	
	/**
     * This method determines the actual method referred to by an invoke
     * expression.
     * 
     * @param call
     *            The invoke call being resolved
     * @param environment
     *            The typing environment
     * @return A triple (C,M,T), where M is the method being invoked, C it's
     *         enclosing class, and T is the actual type of the method. Note
     *         that T can vary from M.type, since it may contain appropriate
     *         substitutions for any generic type variables.
     * @throws ClassNotFoundException
     *         If it needs to access a class which cannot be found.
     * @throws MethodNotFoundException
     *         If it cannot find a suitable method.
     */
	public static Triple<Clazz, Method, Type.Function> resolveMethod(Invoke call)
			throws ClassNotFoundException, MethodNotFoundException, FieldNotFoundException {
		
		// first, determine the parameter types
		ArrayList<Type> parameterTypes = new ArrayList<Type>();
		for(FlowGraph.Expr p : call.parameters) {						
			parameterTypes.add(p.type);
		}
		
		Type.Reference receiver;
		//System.out.println(call.target.type.getClass());
		if(call.target.type instanceof Type.Reference) {
			receiver = (Type.Reference) call.target.type;
		} else {
			assert call.target.type instanceof Type.Variable;
			// In this case, a method has been invoked upon a variable with
			// a generic type. This can basically only happen if that method
			// is part of java.lang.Object (or we have a lower bound).
			receiver = Type.referenceType("java.lang","Object");
		}			
					
		return ClassTable.resolveMethod(receiver,call.name,parameterTypes);		
	}
	
	/**
	 * This method simply inverts a boolean comparison.
	 */
	public static FlowGraph.Expr invertBoolean(FlowGraph.Expr e) {
		if(e instanceof FlowGraph.BinOp) {
			FlowGraph.BinOp be = (FlowGraph.BinOp) e;
			switch(be.op) {
			case FlowGraph.BinOp.EQ:
				return new FlowGraph.BinOp(FlowGraph.BinOp.NEQ,be.lhs,be.rhs,Type.booleanType());
			case FlowGraph.BinOp.NEQ:
				return new FlowGraph.BinOp(FlowGraph.BinOp.EQ,be.lhs,be.rhs,Type.booleanType());
			case FlowGraph.BinOp.LT:
				return new FlowGraph.BinOp(FlowGraph.BinOp.GTEQ,be.lhs,be.rhs,Type.booleanType());
			case FlowGraph.BinOp.LTEQ:
				return new FlowGraph.BinOp(FlowGraph.BinOp.GT,be.lhs,be.rhs,Type.booleanType());
			case FlowGraph.BinOp.GT:
				return new FlowGraph.BinOp(FlowGraph.BinOp.LTEQ,be.lhs,be.rhs,Type.booleanType());
			case FlowGraph.BinOp.GTEQ:
				return new FlowGraph.BinOp(FlowGraph.BinOp.LT,be.lhs,be.rhs,Type.booleanType());
			}
		} else if(e instanceof FlowGraph.UnOp) {
			FlowGraph.UnOp uop = (FlowGraph.UnOp) e;
			if(uop.op == FlowGraph.UnOp.NOT) {
				return uop.expr;
			}
		}
		return new FlowGraph.UnOp(FlowGraph.UnOp.NOT,e,Type.booleanType());		
	}
	
	/**
     * This method attempts to eliminate an expressio of the form !e, by
     * applying demorgans theorem and various well-known equivalences.
     * Specifically, the rules are:
     * 
     * !!X       ===> X
     * !(X == Y) ===> X != Y
     * !(X != Y) ===> X == Y
     * !(X < Y)  ===> X >= Y
     * !(X <= Y) ===> X > Y
     * !(X > Y)  ===> X <= Y
     * !(X >= Y) ===> X <  Y
     * 
     * !(X && Y) ===> !X || !Y
     * !(X || Y) ===> !X && !Y
     * 
     * Note that, in the case of !(X instanceof Y), or !f(...), no rewrite 
     * is possible, so the original expression is simply returned.
     */
	public static FlowGraph.Expr eliminateNot(FlowGraph.UnOp e) {
		assert e.op == FlowGraph.UnOp.NOT;
		
		if(e.expr instanceof FlowGraph.UnOp) {
			FlowGraph.UnOp e2 = (FlowGraph.UnOp) e.expr;
			if(e2.op == UnOp.NOT) {
				// now, check for another not!
				if(e2.expr instanceof FlowGraph.UnOp) {
					FlowGraph.UnOp e3 = (FlowGraph.UnOp) e.expr;
					if(e3.op == UnOp.NOT) {
						// expression originally had form !!!e
						return eliminateNot(e3);
					}
				}
				return e2.expr;
			} else { return e; } // must be a type error					
		} else if(e.expr instanceof FlowGraph.BinOp) {
			FlowGraph.BinOp e2 = (FlowGraph.BinOp) e.expr;
			switch(e2.op) {
				case BinOp.EQ:
					return new BinOp(BinOp.NEQ,e2.lhs,e2.rhs,Type.booleanType());
				case BinOp.NEQ:
					return new BinOp(BinOp.EQ,e2.lhs,e2.rhs,Type.booleanType());
				case BinOp.LT:
					return new BinOp(BinOp.GTEQ,e2.lhs,e2.rhs,Type.booleanType());
				case BinOp.LTEQ:
					return new BinOp(BinOp.GT,e2.lhs,e2.rhs,Type.booleanType());
				case BinOp.GT:
					return new BinOp(BinOp.LTEQ,e2.lhs,e2.rhs,Type.booleanType());
				case BinOp.GTEQ:
					return new BinOp(BinOp.LT,e2.lhs,e2.rhs,Type.booleanType());
				case BinOp.LAND:
				{
					FlowGraph.Expr lhs = eliminateNot(new UnOp(UnOp.NOT,e2.lhs,Type.booleanType()));
					FlowGraph.Expr rhs = eliminateNot(new UnOp(UnOp.NOT,e2.rhs,Type.booleanType()));
					return new BinOp(BinOp.LOR,lhs,rhs,Type.booleanType());
				}
				case BinOp.LOR:
				{
					FlowGraph.Expr lhs = eliminateNot(new UnOp(UnOp.NOT,e2.lhs,Type.booleanType()));
					FlowGraph.Expr rhs = eliminateNot(new UnOp(UnOp.NOT,e2.rhs,Type.booleanType()));
					return new BinOp(BinOp.LAND,lhs,rhs,Type.booleanType());
				}
			}
		}

		// no rewrite rules apply here, so do nothing!
		return e;
	}
	
	public static final long serialVersionUID = 1l;
}
