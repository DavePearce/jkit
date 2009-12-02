package jkit.jil.util;

import jkit.compiler.ClassLoader;
import jkit.compiler.MethodNotFoundException;
import jkit.compiler.FieldNotFoundException;
import jkit.compiler.Clazz;
import jkit.jil.tree.Type;
import jkit.util.Pair;

/**
 * The purpose of tags is to uniquely identify methods and fields in a given
 * Java program. Primarily, this is useful for the interprocedural analysis
 * phases.
 * 
 * @author djp
 * 
 */
public abstract class Tag {
	
	public static class Method extends Tag {
		private Type.Reference owner;
		private String name;
		private Type.Function type;
		
		protected Method(Type.Reference owner, String name, Type.Function type) {
			this.owner = (Type.Reference) Types.stripGenerics(owner);
			this.name = name;
			this.type = Types.stripGenerics(type);
		}

		public Type.Reference owner() {
			return owner;
		}

		public String name() {
			return name;
		}

		public Type.Function type() {
			return type;
		}

		public int hashCode() {		
			int fhc = owner == null ? 0 : owner.hashCode();
			int shc = name == null ? 0 : name.hashCode();			
			int thc = type == null ? 0 : type.hashCode();
			return fhc ^ shc ^ thc; 
		}
		
		public boolean equals(Object o) {
			if(o instanceof Tag.Method) {				
				Tag.Method p = (Tag.Method) o;
				boolean r=false;
				if(owner != null) { r = owner.equals(p.owner); }
				else { r = p.owner == owner; }
				if(name != null) { r &= name.equals(p.name); }
				else { r &= p.name == name; }
				if(type != null) { r &= type.equals(p.type()); }
				else { r &= p.type() == type; }		
				return r;				
			}
			return false;
		}
				
		
		public String toString() {
			return owner + "." + name + ":" + type;
		}
	}

	public static class Field extends Tag {
		private Type.Reference owner;
		private String name;
		
		protected Field(Type.Reference owner, String name) {
			this.owner = owner;
			this.name = name;
		}

		public Type.Reference owner() {
			return owner;
		}

		public String name() {
			return name;
		}

		public String toString() {
			return owner + "." + name;
		}
		
		public int hashCode() {		
			int fhc = owner == null ? 0 : owner.hashCode();
			int shc = name == null ? 0 : name.hashCode();						
			return fhc ^ shc; 
		}
		
		public boolean equals(Object o) {
			if(o instanceof Tag.Field) {				
				Tag.Field p = (Tag.Field) o;
				boolean r=false;
				if(owner != null) { r = owner.equals(p.owner); }
				else { r = p.owner == owner; }
				if(name != null) { r &= name.equals(p.name); }
				else { r &= p.name == name; }					
				return r;				
			}
			return false;
		}
	}
	
	public static Tag.Method create(Clazz owner, String name, Type.Function funtype) {
		funtype = Types.stripGenerics(funtype);
		
		if(name.equals("super") || name.equals("this")) {
			name = owner.type().lastComponent().first();			
		} 
		
		for (Clazz.Method m : owner.methods(name)) {
			Type.Function mtype = Types.stripGenerics(m.type());
			if (mtype.equals(funtype)) {
				return new Tag.Method(owner.type(), name, mtype);
			}
		}
		throw new IllegalArgumentException("cannot create tag --- method \""
				+ name + ":" + funtype + "\" does not exist in class \""
				+ owner.type() + "\"");		
	}
	
	public static Tag.Method create(Type.Reference owner, String name,
			Type.Function funtype, ClassLoader loader)
			throws ClassNotFoundException, MethodNotFoundException {		

		if (!(owner instanceof Type.Clazz)) {
			return new Tag.Method(owner, name, funtype);
		} else {
			Type.Clazz cowner = (Type.Clazz) owner;
			
			if(name.equals("super") || name.equals("this")) {
				name = cowner.lastComponent().first();			
			} 
						
			Pair<Clazz, Clazz.Method> m = loader.determineMethod(
					cowner, name, funtype);

			return new Tag.Method(m.first().type(), name, m.second().type());
		}
	}
	
	public static Tag.Field create(Clazz owner, String name) {
		if(owner.field(name) != null) {
			return new Tag.Field(owner.type(), name);	
		}
		
		throw new IllegalArgumentException("cannot create tag --- field \""
				+ name + "\" does not exist in class \"" + owner.type() + "\"");
	}
	
	public static Tag.Field create(Type.Reference owner, String name,
			ClassLoader loader)
			throws ClassNotFoundException, FieldNotFoundException {

		if (!(owner instanceof Type.Clazz)) {
			
			return new Tag.Field(owner, name);
			
		} else {

			Pair<Clazz, Clazz.Field> m = loader.determineField(
					(Type.Clazz) owner, name);

			return new Tag.Field(m.first().type(), name);
		}
	}
}
