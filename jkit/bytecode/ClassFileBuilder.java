package jkit.bytecode;

import java.util.*;

import jkit.bytecode.ClassFileWriter.ExceptionHandler;
import jkit.compiler.ClassLoader;
import jkit.jil.tree.*;
import jkit.util.Pair;

public class ClassFileBuilder {
	protected final ClassLoader loader;
	protected final int version;
	
	public ClassFileBuilder(ClassLoader loader, int version) {
		this.loader = loader;
		this.version = version;
	}
	
	public ClassFile build(jkit.jil.tree.Clazz clazz) {
		ClassFile cfile = null;
		
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
		
	}
	
	
	
	protected void buildMethodPool(ClassFile.Method m, ClassFile cfile) {								
		
	}
}
