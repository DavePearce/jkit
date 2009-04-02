package jkit.bytecode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jkit.jil.tree.*;
import jkit.util.Pair;

public class Signature implements Attribute {
	protected Type.Clazz type;
	protected Type.Clazz superClazz;
	protected List<Type.Clazz> interfaces;
	
	public Signature(Type.Clazz type, Type.Clazz superClazz, List<Type.Clazz> interfaces) {
		this.type = type;
		this.superClazz = superClazz;
		this.interfaces = interfaces;
	}
	
	public String name() {
		return "Signature";
	}
	
	public void write(BinaryWriter writer,
			Map<Constant.Info, Integer> constantPool) throws IOException {
		writer.write_u2(constantPool.get(new Constant.Utf8("Signature")));
		writer.write_u4(2);
		writer.write_u2(constantPool.get(new Constant.Utf8(ClassFile
				.descriptor(type, true))));
	}
	
	public void addPoolItems(Set<Constant.Info> constantPool) {
		Constant.addPoolItem(
				new Constant.Utf8(classSignature()),constantPool);
	}
	
	/**
	 * This method constructors a class signature string from a clazz.
	 * 
	 * @param clazz
	 * @return
	 */
	public String classSignature() {
		String desc = "";
		
		List<Pair<String,List<Type.Reference>>> classes = type.components();
		if(classes.get(classes.size()-1).second().size() > 0) { 
			desc += "<"; 
			for(Type t : classes.get(classes.size()-1).second()) {
				if(t instanceof Type.Variable) {
					Type.Variable tv = (Type.Variable) t;
					desc += tv.variable() + ":";
					// NOTE: lowerBounds() should *never* be null.
					if(tv.lowerBound() == null) {
						desc += "Ljava/lang/Object;";
					} else {
						Type lb = tv.lowerBound();
						// The following check is needed to deal with the case
						// where the type bounds are only interfaces. In this
						// case, there must be an extra colon indicating the
						// absence of super class. It's actually really annoying
						// since it couples this code with ClassTable ... grrr.
						
						/*
						 * This code is temporarily bypassed ... it needs to be fixed! 
						try {
							Clazz tmp = ClassTable.findClass((Type.Reference) lb);
							if(tmp.isInterface()) { desc += ":"; }
							desc += Types.descriptor(lb, true);
						} catch(ClassNotFoundException ce) {
							throw new RuntimeException("Type bound " + lb + " not found");
						}
						*/						
					}
				} else {
					throw new RuntimeException("Type Variable required in Class Signature!");
				}
			}
			desc += ">";
		}
		if (superClazz != null) {
			desc += ClassFile.descriptor(superClazz, true);
		}
		for (Type t : interfaces) {
			desc += ClassFile.descriptor(t, true);
		}
		return desc;
	}
}
