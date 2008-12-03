package jkit.java;

import java.util.List;

import jkit.jkil.Attribute;
import jkit.jkil.SyntacticElement;
import jkit.jkil.SyntacticElementImpl;
import jkit.util.Pair;

/**
 * Represents types used in Java programs (e.g. int, String, Object[], etc).
 * <p>
 * JKit provides classes and methods for representing and manipulating Java
 * types (such <code>int</code>, <code>String[]</code> etc). The majority
 * of these can be found here. For example, the <code>Type.Int</code> class is
 * used to represent <code>int</code> types, whilst
 * <code>Type.Reference</code> represents general reference types, such as
 * <code>java.lang.String</code>.
 */

public interface Type {
	
	public interface Reference extends Type {}
	public interface Primitive extends Type {}
	
	public static class BoolType extends SyntacticElementImpl implements Primitive {
		public BoolType(Attribute... attributes) {
			super(attributes);			
		}		
	}
	
	public static class ByteType extends SyntacticElementImpl implements Primitive {
		public ByteType(Attribute... attributes) {
			super(attributes);			
		}		
	}
	
	public static class CharType extends SyntacticElementImpl implements Primitive {
		public CharType(Attribute... attributes) {
			super(attributes);			
		}		
	}
	
	public static class ShortType extends SyntacticElementImpl implements Primitive {
		public ShortType(Attribute... attributes) {
			super(attributes);			
		}		
	}
	
	public static class IntType extends SyntacticElementImpl implements Primitive {
		public IntType(Attribute... attributes) {
			super(attributes);			
		}		
	}
	
	public static class LongType extends SyntacticElementImpl implements Primitive {
		public LongType(Attribute... attributes) {
			super(attributes);			
		}		
	}
	
	public static class FloatType extends SyntacticElementImpl implements Primitive {
		public FloatType(Attribute... attributes) {
			super(attributes);			
		}		
	}
	
	public static class DoubleType extends SyntacticElementImpl implements Primitive {
		public DoubleType(Attribute... attributes) {
			super(attributes);			
		}		
	}
	
	public static class ArrayType extends SyntacticElementImpl implements Reference {		
		private Type element;
		
		public ArrayType(Type element, Attribute... attributes) {
			super(attributes);
			this.element = element;			
		}
		
		public Type element() {
			return element;
		}		
	}
	
	public static class ClassType extends SyntacticElementImpl implements Reference {		
		private List<Pair<String, List<Type>>> components;
		public ClassType(List<Pair<String, List<Type>>> components,
				Attribute... attributes) {
			super(attributes);
			this.components = components;
		}		
		public List<Pair<String, List<Type>>> components() {
			return components;
		}
		public void setComponents(
				List<Pair<String, List<Type>>> components) {
			this.components = components;
		}
	}
	
	public static class WildcardType extends SyntacticElementImpl implements Reference {
		private Type lowerBound;
		private Type upperBound;

		public WildcardType(Type lowerBound, Type upperBound,
				Attribute... attributes) {
			super(attributes);
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}

		public Type upperBound() {
			return upperBound;
		}

		public Type lowerBound() {
			return lowerBound;
		}
	}
	
	public static class VariableType extends SyntacticElementImpl implements Reference {
		private String variable;
		private List<Type> lowerBounds;

		public VariableType(String variable, List<Type> lowerBounds,
				Attribute... attributes) {
			super(attributes);
			this.variable = variable;
			this.lowerBounds = lowerBounds;
		}

		public String variable() {
			return variable;
		}

		public List<Type> lowerBounds() {
			return lowerBounds;
		}		
	}
	
	public static class FunctionType extends SyntacticElementImpl implements Type {
		private final List<Type> parameters;
		private final Type returnType;
		private final List<Type> typeArgs;
	}
	
}
