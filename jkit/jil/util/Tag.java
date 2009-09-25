package jkit.jil.util;

import jkit.jil.tree.Type;
import jkit.util.Pair;
import jkit.util.Triple;

/**
 * The purpose of tags is to uniquely identify methods and fields in a given
 * Java program. Primarily, this is useful for the interprocedural analysis
 * phases.
 * 
 * @author djp
 * 
 */
public class Tag {
	
	public static class Method extends Triple<Type.Clazz, String, Type.Function> {
		public Method(Type.Clazz owner, String name, Type.Function type) {
			super(owner, name, type);
		}

		public Type.Clazz owner() {
			return first();
		}

		public String name() {
			return second();
		}

		public Type.Function type() {
			return third();
		}
		
		public String toString() {
			return first() + "." + second() + ":" + type();
		}		
	}
	
	public static class Field extends Pair<Type.Clazz, String> {
		public Field(Type.Clazz owner, String name) {
			super(owner, name);
		}

		public Type.Clazz owner() {
			return first();
		}

		public String name() {
			return second();
		}
		
		public String toString() {
			return first() + "." + second();
		}		
	}
}
