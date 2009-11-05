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
public interface Tag {
	
	public static class Method extends Triple<Type.Reference, String, Type.Function> implements Tag {
		public Method(Type.Reference owner, String name, Type.Function type) {
			super((Type.Reference) Types.stripGenerics(owner), name, Types.stripGenerics(type));
		}

		public Type.Reference owner() {
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
	
	public static class Field extends Pair<Type.Reference, String> implements Tag {
		public Field(Type.Reference owner, String name) {
			super((Type.Reference) Types.stripGenerics(owner), name);
		}

		public Type.Reference owner() {
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
