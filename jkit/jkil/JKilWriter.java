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
 * The InternalCodeWriter dumps out JKits intermediate language 
 * in a raw format which is halfway between Java source code and 
 * Java bytecode.  The purpose of this class is purely for 
 * debugging purposes,  so that we can really see what's going 
 * on inside the compiler.
 * 
 */

package jkit.jkil;

import java.io.*;
import java.util.*;
import java.lang.reflect.Modifier;

import jkit.bytecode.ClassFileReader;
import jkit.compiler.ClassWriter;
import jkit.jkil.FlowGraph.Expr;
import jkit.jkil.FlowGraph.Point;
import jkit.util.*;
import jkit.util.graph.Graph;

public class JKilWriter implements ClassWriter {
	private PrintWriter output;

	public JKilWriter() {
		output = new PrintWriter(System.out);
	}
	
	public JKilWriter(Writer out) {
		output = new PrintWriter(out);
	}
	
	public JKilWriter(OutputStream out) {
		output = new PrintWriter(out);
	}
	
	public void writeClass(Clazz clazz) {		
		String pkg = clazz.type().pkg();
		
		if(!pkg.equals("")) {
			output.println("package " + pkg + ";");
		}		
		
		writeImports(clazz);
		
		writeClass(clazz,0);
		
		output.flush();
	}
	
	/**
     * The purpose of this method is to print out appropriate import statements.
     * To do this, we simply scan all types and collect their packages.
     * 
     * @param clazz
     */
	private void writeImports(Clazz clazz) {
		HashSet<String> packages = new HashSet<String>();
		addPackages(clazz.superClass(),packages);
		
		for(Type i : clazz.interfaces()) {
			addPackages(i,packages);
		}
		
		for(Field f : clazz.fields()) {
			addPackages(f.type(),packages);
		}
		
		for(Method m : clazz.methods()) {
			addPackages(m.type(),packages);
			// will need to recurse method statements
			// here
		}
		
		for(String p : packages) {
			if(!p.equals("") && !p.equals("java.lang") && !p.equals(clazz.type().pkg())) {
				output.println("import " + p + ".*;");
			}
		}
	}
	
	/**
	 * The purpose of this method is to scan the type given and
	 * extract all packages that it, or types used by it, refer to.
	 * 
	 * @param t
	 * @param packages
	 */
	private void addPackages(Type t, Set<String> packages) {		
		if(t instanceof Type.Reference) {
			Type.Reference tr = (Type.Reference) t;  		
			packages.add(tr.pkg());
		} else if(t instanceof Type.Function) {
			Type.Function ft = (Type.Function) t;
			addPackages(ft.returnType(),packages);
			for(Type p : ft.parameterTypes()) {
				addPackages(p,packages);
			}
			for(Type ta : ft.typeArgs()) {
				addPackages(ta,packages);
			}
		}
	}
	
	private void writeClass(Clazz clazz, int indent) {
		
		// ==============================
		// ======== DECLARATION =========
		// ==============================
		
		output.println("");
		indent(indent);
		writeModifiers(clazz.modifiers());
		Type.Reference type = clazz.type();
		Type.Reference superClass = clazz.superClass();
		Pair<String,Type[]> classDecl = type.classes()[type.classes().length-1];
		if(clazz.isInterface()) {
			output.print("interface " + classDecl.first());
		} else {
			output.print("class " + classDecl.first());
		}
		Type[] typeParams = classDecl.second();
		if(typeParams.length > 0) {
			output.print("<");
			for(int i=0;i!=typeParams.length;++i) {
				if(i!=0) { output.print(","); }
				output.print(typeParams[i].toShortString());
			}
			output.print(">");
		}
		if (!jkit.bytecode.Types.descriptor(superClass, false).equals(
				"Ljava/lang/Object;")) {
			output.print(" extends " + superClass.toShortString());
		}
		List<Type.Reference> interfaces = clazz.interfaces();
		if(interfaces.size() > 0) {
			if(clazz.isInterface()) {
				output.print(" extends ");
			} else {
				output.print(" implements ");
			}
			boolean firstTime=true;
			for(Type t : interfaces) {
				if(!firstTime) { output.print(", "); }
				firstTime=false;
				output.print(t.toShortString());
			}
		}
		output.println(" { ");
		
		// ==============================
		// ========== FIELDS ============
		// ==============================
								
		for(Field f : clazz.fields()) {
			writeField(f,indent+1);
		}
			
		// ==============================
		// ========== METHODS ===========
		// ==============================
		
		if(clazz.fields().size() > 0) {
			output.println("");
		}		
		
		for(Method m : clazz.methods()) {			
			writeMethod(m,clazz,indent+1);
		}
		
		// ==============================
		// ======= INNER CLASSES ========
		// ==============================		
		
		if(clazz.methods().size() > 0) {
			output.println("");
		}
			
		indent(indent);
		output.println("} ");			
	}
	
	private void writeField(Field field, int indent) {
		indent(indent);
		writeModifiers(field.modifiers());
		output.println(field.type().toShortString() + " " + field.name() + ";");
	}	
	
	private void writeMethod(Method method, Clazz clazz, int indent) {
		indent(indent);		
		writeModifiers(method.modifiers());
		Type.Function ft = method.type();
		
		if(ft.typeArgs().length > 0) {
			output.print("<");
			boolean firstTime=true;
			for(Type.Variable tv : ft.typeArgs()) {
				if(!firstTime) { output.print(","); }
				firstTime=false;
				output.print(tv.toShortString());
			}
			output.print("> ");
		}
		
		// === print return type, name and parameter types ===		
		if(!method.name().equals(clazz.name())) {
			output.print(ft.returnType().toShortString() + " ");
		}
		
		ArrayList<String> parameterNames = new ArrayList<String>();
		FlowGraph code = method.code();
		
		if(code != null) {
			for(FlowGraph.LocalVarDef v : code.localVariables()) {
				parameterNames.add(v.name());
			}
		} else {
			for(int i=0;i!=ft.parameterTypes().length;++i) {
				parameterNames.add("v" + Integer.toString(i));
			}
		}
		 
		output.print(method.name() + "(");
		boolean firstTime=true;
		Type[] parameterTypes = ft.parameterTypes();
		for(int i=0;i!=parameterTypes.length;++i) {
			if(!firstTime) { output.print(", "); }
			firstTime=false;
			output.print(parameterTypes[i].toShortString());
			output.print(" " + parameterNames.get(i));
		}		
		output.print(")");
		
		// === print throws clause ===
		List<Type.Reference> exceptions = method.exceptions();
		if(exceptions.size() > 0) {
			output.print(" throws ");
			firstTime=true;
			for(Type e : exceptions) {
				if(!firstTime) { output.print(", "); }
				firstTime=false;
				output.print(e.toShortString());
			}
		}
		
		if(method.isAbstract() | clazz.isInterface()) { output.println(";"); }
		else {
			assert code != null;
			output.println(" {");
			writeCode(code,indent+1);					
			indent(indent);output.println("}");
		}
	}	
	
	private void writeCode(FlowGraph cfg, int indent) {
		// System.out.println("CFG = " + cfg);
		// === Variable Declarations ===
		
		for(FlowGraph.LocalVarDef v : cfg.localVariables()) {
			if(v.isParameter()) { continue; } // ignore parameters
			indent(indent);writeModifiers(v.modifiers());
			output.println(v.type().toShortString() + " " + v.name() + ";");
		}
		
		// === Statements ===  
		
		ArrayList<FlowGraph.Point> ord = cfgOrder(cfg.entry(),cfg);
		HashMap<FlowGraph.Point,Integer> labels = new HashMap<FlowGraph.Point,Integer>();		
						
		for(int i=0;i!=ord.size();++i) {			
			FlowGraph.Point s = ord.get(i);					
			
			// ==============================================================
			// ============ First, print label (only if needed) =============
			// ==============================================================
			
			boolean containsLast = false;
			if(i > 0) {
				// the following loop is needed, but really wouldn't be
				// if this were a half-decent programming language.				
				FlowGraph.Point last = ord.get(i-1);
				for(Triple<FlowGraph.Point,FlowGraph.Point,FlowGraph.Expr> t : cfg.to(s)) {
					if(t.first() == last && !(t.third() instanceof FlowGraph.Exception)) { 
						containsLast=true;
						break;
					}
				}
			}
			if (containsLast && cfg.from(ord.get(i-1)).size() > 2) {
				containsLast = false;
			}
			
			if(cfg.to(s).size() > 1 || !containsLast) {
				indent(indent-1);
				Integer label;
				if((label=labels.get(s)) == null) {
					label = new Integer(labels.size());
					labels.put(s,label);
				}
				output.println(" L" + label + ":");
			}
			
			// ==============================================================
			// === Second, split conditional edges from exceptional edges ===
			// ==============================================================
			
			HashSet<Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr>> conditionals;
			HashSet<Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr>> exceptionals;

			conditionals = new HashSet<Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr>>();
			exceptionals = new HashSet<Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr>>();
			
			for (Triple<FlowGraph.Point, FlowGraph.Point, FlowGraph.Expr> edge : cfg
					.from(s)) {
				if (!(edge.third() instanceof FlowGraph.Exception)) {
					conditionals.add(edge);
				} else {
					exceptionals.add(edge);
				}
			}
			
			// ==============================================================
			// ================ Third, write statement body =================
			// ==============================================================
						
			if(s.statement() != null) {
				indent(indent);output.print(s.statement());
				// now, print out any exceptions thrown by this statement
				if(!exceptionals.isEmpty()) {
					output.print("\t[");
					boolean firstTime=true;
					for(Triple<FlowGraph.Point,FlowGraph.Point,FlowGraph.Expr> e : exceptionals) {
						FlowGraph.Point target = e.second();
						Type.Reference exception = ((FlowGraph.Exception) e.third()).type;
						// pretty printing stuff
						if(!firstTime) { output.print(", "); }
						firstTime=false;
						// work out destination label
						Integer label;
						if((label=labels.get(target)) == null) {
							label = new Integer(labels.size());
							labels.put(target,label);
						}
						// print it!
						output.print(exception + ":L" + label);
					}			
					output.print("]");
				}
				output.println("");
			}
			
			// ==============================================================
			// ============= Fourth, deal with conditional edges ============
			// ==============================================================
			
			if (conditionals.size() > 2) {
				Expr[] conditions = new Expr[conditionals.size()];
				Point[] targets = new Point[conditionals.size()];
				String[] lbls = new String[conditionals.size()];
				
				int id = 0;
				for(Triple<Point, Point, Expr> f: conditionals) {
					conditions[id] = f.third();
					targets[id] = f.second();
					Integer label;
					if ((label = labels.get(targets[id])) == null) {
						label = new Integer(labels.size());
						labels.put(targets[id], label);
					}
					lbls[id] = "L"+label;
					id++;
				}
				FlowGraph.Expr target = null;
				for (FlowGraph.Expr c: conditions) {
					FlowGraph.BinOp cond = (FlowGraph.BinOp)c;
					if (cond.op == FlowGraph.BinOp.EQ) {
						target = cond.lhs;
						break;
					}
				}
				indent(indent);
				output.println("switch ("+target+") {");
				for (int j = 0; j < conditions.length; j++) {
					Expr ex = ((FlowGraph.BinOp)conditions[j]).rhs;
					Integer label;
					if((label=labels.get(targets[j])) == null) {
						label = new Integer(labels.size());
						labels.put(targets[j],label);
					}
					indent(indent+1);
					if (ex instanceof FlowGraph.IntVal) {
						output.println(((FlowGraph.IntVal)ex).value+":\tL"+label);
					}
					else {
						output.println("default:\tL"+label);
					}
				}
				indent(indent);
				output.println("}");
			}
			else if(conditionals.size() == 2) {
				FlowGraph.Expr condition = null;
				FlowGraph.Point target = null;
				for(Triple<FlowGraph.Point,FlowGraph.Point,FlowGraph.Expr> f : conditionals) {
					if(f.second() != ord.get(i+1)) {
						condition = f.third();
						target = f.second();
					}
				}
				indent(indent);
				Integer label;
				if((label=labels.get(target)) == null) {
					label = new Integer(labels.size());
					labels.put(target,label);
				}
				output.println("if " + condition + " goto L" + label);
			} else if(conditionals.size() == 1) {
				Triple<FlowGraph.Point,FlowGraph.Point,FlowGraph.Expr> target = conditionals.iterator().next();				
				if(ord.size() == (i+1) || target.second() != ord.get(i+1)) {
					assert target.third() == null;
					indent(indent);
					Integer label;
					if((label=labels.get(target.second())) == null) {
						label = new Integer(labels.size());
						labels.put(target.second(),label);
					}	
					output.println("goto L" + label);
				}
			}			
		}
	}
	
	/**
	 * This method flattens the control-flow graph into a sequence of
	 * statements. This is done using a depth-first traversal of the CFG, whilst
	 * ignoring exception edges.
	 * 
	 * @param entry entry point of method in control-flow graph.
	 * @param cfg control-flow graph of method.
	 * @return List of statements in their flattened order
	 */
	private ArrayList<FlowGraph.Point> cfgOrder(FlowGraph.Point entry, 
				Graph<FlowGraph.Point,Triple<FlowGraph.Point,FlowGraph.Point,FlowGraph.Expr>> cfg) {		
		// first,  perform the depth-first search.
		ArrayList<FlowGraph.Point> ord = new ArrayList<FlowGraph.Point>();
		cfgVisit(entry,new HashSet<FlowGraph.Point>(),ord,cfg);
		// we need to reverse the ordering here, since cfg_visit
		// will have added the statements in reverse topological order!		
		Collections.reverse(ord);
		return ord;
	}
	
	/**
	 * This method performs a standard depth-first search. 
	 * 
	 * @param cfg the control-flow graph.
	 */
	private void cfgVisit(FlowGraph.Point v, 
			Set<FlowGraph.Point> visited, 
			List<FlowGraph.Point> ord, 
			Graph<FlowGraph.Point,Triple<FlowGraph.Point,FlowGraph.Point,FlowGraph.Expr>> cfg) {		
		
		visited.add(v);
		
		// Sort out-edges according to their target position in the program.
		// Doing this helps ensure blocks which are located close together in
		// the source remain close together. Otherwise, for example, you end up
		// with for-loops where the code after the for loop comes before the
		// for-loop body!!!
		ArrayList<Pair<FlowGraph.Point,FlowGraph.Point>> outs;
		outs = new ArrayList<Pair<FlowGraph.Point,FlowGraph.Point>>(cfg.from(v));
		
		Collections.sort(outs,new Comparator<Pair<FlowGraph.Point,FlowGraph.Point>>() {
			public int compare(Pair<FlowGraph.Point,FlowGraph.Point> p1, Pair<FlowGraph.Point,FlowGraph.Point> p2) {
				FlowGraph.Point e1 = p1.second();
				FlowGraph.Point e2 = p2.second();
				if(e1.line() < e2.line()) {
					return -1;
				} else if(e1.line() == e2.line()) {
					if(e1.column() < e2.column()) {
						return -1;
					} else if(e1.column() == e2.column()) {
						return 0;
					}
				}
				return 1;
			}
		});
		
		// Now, visit the edges in their sorted order 
		for(Pair<FlowGraph.Point,FlowGraph.Point> e : outs) {
			if(!visited.contains(e.second())) {
				cfgVisit(e.second(),visited,ord,cfg);
			}
		}
		ord.add(v);
	}
	
	private void writeModifiers(int modifiers) {
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
		if((modifiers & ClassFileReader.ACC_VARARGS)!=0) { output.print("varargs "); }
	}
	
	private void indent(int level) {
		for(int i=0;i!=level;++i) {
			output.print("  ");
		}
	}
}
