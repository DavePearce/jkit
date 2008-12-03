package jkit.java;

import java.util.*;

import jkit.jil.*;
import jkit.util.*;

public class JavaFile {
	private String pkg;
	private List<Pair<Boolean,String> > imports;
	private List<Declaration> declarations; 
	
	public JavaFile(String pkg, List<Pair<Boolean, String> > imports, List<Declaration> declarations) {
		this.pkg = pkg;
		this.imports = imports;
		this.declarations = declarations;
	}
	
	/**
	 * Get the package declared at the beginning of this file (if there is one)
	 */			
	public String pkg() { 
		return pkg;
	}
	
	/**
	 * Get the list of import declarations at the beginning of this file.
	 * 
	 * @return
	 */
	public List<Pair<Boolean,String> > imports() { 
		return imports;
	}
	
	/**
	 * Get the list of class declarations in this file.
	 * 
	 * @return
	 */
	public List<Declaration> declarations() { 
		return declarations;
	}	
		
	// ====================================================
	// MODIFIERS
	// ====================================================
	
	// ====================================================
	// DECLARATIONS
	// ====================================================
	
	public static interface Declaration extends SyntacticElement {
		
	}
	
	public static class Clazz extends SyntacticElementImpl implements Declaration, Stmt {
		private List<Modifier> modifiers;
		private String name;
		private List<Type.Variable> typeParameters;
		private Type.Clazz superclass;
		private List<Type.Clazz> interfaces;
		private List<Declaration> declarations;		
				
		public Clazz(List<Modifier> modifiers, String name,
				List<Type.Variable> typeParameters, Type.Clazz superclass,
				List<Type.Clazz> interfaces, List<Declaration> declarations,
				Attribute... attributes) {
			super(attributes);
			this.modifiers = modifiers;
			this.name = name;
			this.typeParameters = typeParameters;
			this.superclass = superclass;
			this.interfaces = interfaces;
			this.declarations = declarations;			
		}
		
		public List<Modifier> modifiers() {
			return modifiers;
		}
		
		public String name() {
			return name;
		}

		public List<Type.Variable> typeParameters() {
			return typeParameters;
		}
		
		public Type.Clazz superclass() {
			return superclass;
		}
		
		public List<Type.Clazz> interfaces() {
			return interfaces;
		}
		
		public List<Declaration> declarations() { 
			return declarations;
		}		
	}
	
	public static class Interface extends Clazz {
		public Interface(List<Modifier> modifiers, String name,
				List<Type.Variable> typeParameters, Type.Clazz superclass,
				List<Type.Clazz> interfaces, List<Declaration> declarations,
				Attribute... attributes) {
			super(modifiers, name, typeParameters, superclass, interfaces,
					declarations, attributes);
		}
	}
	
	public static class Enum extends Clazz {
		private List<EnumConstant> constants;

		public Enum(List<Modifier> modifiers, String name,
				List<Type.Clazz> interfaces, List<EnumConstant> constants,
				List<Declaration> declarations, Attribute... attributes) {
			super(modifiers, name, new ArrayList<Type.Variable>(), null,
					interfaces, declarations, attributes);
			this.constants = constants;
		}

		public List<EnumConstant> constants() {
			return constants;
		}
	}
	
	public static class EnumConstant extends SyntacticElementImpl{
		private String name;
		private List<Expr> arguments;
		private List<Declaration> declarations;
		
		public EnumConstant(String name, List<Expr> arguments,
				List<Declaration> declarations, Attribute... attributes) {
			super(attributes);
			this.name = name;
			this.arguments = arguments;
			this.declarations = declarations;
		}
		
		public String name() {
			return name;
		}
		
		public List<Expr> arguments() {
			return arguments;
		}
		
		public List<Declaration> declarations() {
			return declarations;
		}
	}
	
	public static class AnnotationInterface extends SyntacticElementImpl  implements Declaration {
		private List<Modifier> modifiers;
		private String name;
		private List<Triple<Type, String, JavaFile.Value>> methods; 
						
		public AnnotationInterface(List<Modifier> modifiers, String name,
				List<Triple<Type, String, JavaFile.Value>> methods,
				Attribute... attributes) {
			super(attributes);
			this.modifiers = modifiers;
			this.name = name;
			this.methods = methods;
		}
		
		public List<Modifier> modifiers() {
			return modifiers;
		}
		public String name() {
			return name;
		}
		public List<Triple<Type, String, JavaFile.Value>> methods() {
			return methods;
		}
	}
	
	/**
	 * This class stores all known information about a method, including it's
	 * full (possibly generic) type, its name, its modifiers (e.g. public/private
	 * etc), as well as the methods code.
	 * 
	 * @author djp
	 * 
	 */
	public static class Method extends SyntacticElementImpl  implements Declaration {
		private List<Modifier> modifiers;
		private String name;
		private Type returnType;
		private List<Triple<String,List<Modifier>,Type>> parameters;
		private boolean varargs;
		private List<Type.Variable> typeParameters;
		private List<Type.Clazz> exceptions;
		private Stmt.Block block;

		public Method(List<Modifier> modifiers, String name, Type returnType,
				List<Triple<String, List<Modifier>, Type>> parameters,
				boolean varargs, List<Type.Variable> typeParameters,
				List<Type.Clazz> exceptions, Stmt.Block block,
				Attribute... attributes) {
			super(attributes);
			this.modifiers = modifiers;
			this.returnType = returnType;
			this.name = name;
			this.parameters = parameters;
			this.varargs = varargs;
			this.typeParameters = typeParameters;
			this.exceptions = exceptions;
			this.block = block;
		}
		
		public List<Modifier> modifiers() {
			return modifiers;
		}
		
		public String name() {
			return name;
		}

		public Type returnType() {
			return returnType;
		}
		
		/**
         * List of triples (n,m,t), where n is the parameter name, m are the
         * modifiers and t is the type.
         * 
         * @return
         */
		public List<Triple<String,List<Modifier>,Type>> parameters() {
			return parameters;
		}
		
		/**
         * Indicates whether or not this method accept a variable-length
         * argument list.
         * 
         * @return
         */
		public boolean varargs() {
			return varargs;
		}
		
		public List<Type.Variable> typeParameters() {
			return typeParameters;
		}
		
		public List<Type.Clazz> exceptions() {
			return exceptions;
		}
		
		public Stmt.Block block() {
			return block;
		}
	}
	
	/**
	 * A constructor is a special kind of method.
	 * 
	 * @author djp
	 * 
	 */
	public static class Constructor extends Method {
		public Constructor(List<Modifier> modifiers, String name,
				List<Triple<String, List<Modifier>, Type>> parameters, boolean varargs,
				List<Type.Variable> typeParameters,
				List<Type.Clazz> exceptions,
				Stmt.Block block, Attribute... attributes) {			
			super(modifiers, name, null, parameters, varargs, typeParameters,
					exceptions, block,attributes);
		}
	}
	
	public static class Field extends SyntacticElementImpl implements Declaration {
		private List<Modifier> modifiers;
		private String name;
		private Type type;
		private Expr initialiser;
		
		public Field(List<Modifier> modifiers, String name, Type type,
				Expr initialiser, Attribute... attributes) {
			super(attributes);
			this.modifiers = modifiers;
			this.name = name;
			this.type = type;
			this.initialiser = initialiser;
		}
		
		public List<Modifier> modifiers() {
			return modifiers;
		}
		
		public String name() {
			return name;
		}

		public Type type() {
			return type;
		}
		
		public Expr initialiser() {
			return initialiser;
		}		
	}
	
	public static class InitialiserBlock extends Stmt.Block implements Declaration {
		public InitialiserBlock(List<Stmt> statements, Attribute... attributes) {
			super(statements,attributes);
		}
	}
	public static class StaticInitialiserBlock extends Stmt.Block implements Declaration {
		public StaticInitialiserBlock(List<Stmt> statements, Attribute... attributes) {
			super(statements,attributes);
		}
	}
	
	// ====================================================
	// STATEMENTS
	// ====================================================


	
	// ====================================================
	// Values
	// ====================================================
	
	public static interface Value extends Expr {
		
	}
		
	/**
	 * Represents a numerical constant
	 * 
	 * @author djp
	 *
	 */
	public static class Number extends SyntacticElementImpl implements Value {
		protected int value;
		
		public Number(int value, Attribute... attributes) {
			super(attributes);
			this.value = value;
		}
	}
	
	/**
	 * A boolean constant.
	 * 
	 * @author djp
	 *
	 */
	public static class BoolVal extends Number {
		public BoolVal(boolean value, Attribute... attributes) {
			super(value?1:0);
		}
		
		public boolean value() {
			return value==1;
		}
	}
	
	/**
	 * Represents a character constant.
	 * 
	 * @author djp
	 *
	 */
	public static class CharVal extends Number {
		public CharVal(char value, Attribute... attributes) {
			super(value,attributes);
		}
		
		public char value() {
			return (char)value;
		}
	}
	
	/**
	 * Represents a byte constant.
	 * 
	 * @author djp
	 *
	 */
	public static class ByteVal extends Number {
		public ByteVal(byte value, Attribute... attributes) {
			super(value,attributes);
		}
		
		public byte value() {
			return (byte)value;
		}
	}
	
	/**
	 * Represents a short constant.
	 * @author djp
	 *
	 */
	public static class ShortVal extends Number {
		public ShortVal(short value, Attribute... attributes) {
			super(value,attributes);
		}
		
		public short value() {
			return (short)value;
		}
	}

	/**
     * Represents an int constant.
     * 
     * @author djp
     * 
     */	
	public static class IntVal extends Number {
		public IntVal(int value, Attribute... attributes) {
			super(value,attributes);
		}
		
		public int value() {
			return value;
		}
	}

	/**
     * Represents a long Constant.
     * 
     * @author djp
     * 
     */
	public static class LongVal extends SyntacticElementImpl implements Value {
		private long value;
		
		public LongVal(long value, Attribute... attributes) {
			this.value=value;
		}
		
		public long value() {
			return value;
		}
	}
	
	/**
     * A Float Constant.
     * 
     * @author djp
     * 
     */
	public static class FloatVal extends SyntacticElementImpl implements Value {
		private float value;
		
		public FloatVal(float value, Attribute... attributes) {
			super(attributes);
			this.value=value;
		}
		
		public float value() {
			return value;
		}
	}

	/**
     * A Double Constant.
     * 
     * @author djp
     * 
     */
	public static class DoubleVal extends SyntacticElementImpl implements Value {
		private double value;
		
		public DoubleVal(double value, Attribute... attributes) {
			super(attributes);
			this.value=value;
		}
		
		public double value() {
			return value;
		}
	}
	
	/**
     * A String Constant.
     * 
     * @author djp
     * 
     */
	public static class StringVal extends SyntacticElementImpl implements Value {
		private String value;
		
		public StringVal(String value, Attribute... attributes) {
			super(attributes);
			this.value=value;
		}
		
		public String value() {
			return value;
		}
	}		
	
	/**
     * The null Constant.
     * 
     * @author djp
     * 
     */
	public static class NullVal extends SyntacticElementImpl implements Value {
		public NullVal(Attribute... attributes) {
			super(attributes);
		}
	}
			
	/**
     * An array constant (used for array initialisers only).
     * 
     * @author djp
     * 
     */
	public static class ArrayVal extends SyntacticElementImpl implements Value {
		private List<Expr> values;
		
		public ArrayVal(List<Expr> values, Attribute... attributes) {
			super(attributes);
			this.values = values;
		}
		
		public List<Expr> values() {
			return values;
		}
	}
	
	/**
	 * A typed array constant (used for array initialisers only). This is
	 * similar to a normal array constant, except that the target type is also
	 * specified. For example:
	 * 
	 * <pre>
     * Object[] test = new Object[]{&quot;abc&quot;, new Integer(2)};
     * </pre>
     * 
	 * @author djp
	 */
	public static class TypedArrayVal extends ArrayVal {
		private Type type;

		public TypedArrayVal(Type type, List<Expr> values,
				Attribute... attributes) {
			super(values,attributes);
			this.type = type;
		}

		public Type type() {
			return type;
		}
	}	
	
	/**
	 * Represents a Class Constant
	 * 
	 */
	public static class ClassVal extends SyntacticElementImpl implements Value {
		private Type.Clazz type;

		public ClassVal(Type.Clazz type, Attribute... attributes) {
			super(attributes);
			this.type = type;
		}

		public Type.Clazz value() {
			return type;
		}
	}
}
