package jkit.bytecode;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jkit.jil.tree.JilClass;
import jkit.jil.tree.Modifier;
import jkit.jil.tree.Type;
import jkit.util.Pair;

public class InnerClasses implements Attribute {
	protected List<Pair<Type.Clazz,List<Modifier>>> inners;
	protected List<Pair<Type.Clazz,List<Modifier>>> outers;
	protected Type.Clazz type;
	
	public String name() {
		return "InnerClasses";
	}
	
	/**
	 * Create an InnerClasses attribute (see JLS Section 4.7.5).
	 * 
	 * @param type - the type of the class containing this attribute.
	 * @param inners - the types and modifiers for all classes contained in this class.
	 * @param outers-  the types and modifiers for all classes containing this class.
	 */
	public InnerClasses(Type.Clazz type,
			List<Pair<Type.Clazz, List<Modifier>>> inners,
			List<Pair<Type.Clazz, List<Modifier>>> outers) {
		this.type = type;
		this.inners = inners;
		this.outers = outers;
	}
	
	protected List<Pair<Type.Clazz,List<Modifier>>> inners() {
		return inners;
	}
	protected List<Pair<Type.Clazz,List<Modifier>>> outers() {
		return outers;
	}
	protected Type.Clazz type() {
		return type;
	}
	
	/**
	 * When this method is called, the attribute must add all items that it
	 * needs to the constant pool.
	 * 
	 * @param constantPool
	 */
	public void addPoolItems(Set<Constant.Info> constantPool) {
		Constant.addPoolItem(new Constant.Utf8("InnerClasses"), constantPool);
		for(Pair<Type.Clazz,List<Modifier>> i : outers) {
			Constant.addPoolItem(Constant.buildClass(type),constantPool);
			Constant.addPoolItem(Constant.buildClass(i.first()),constantPool);
			String name = type.lastComponent().first();
			Constant.addPoolItem(new Constant.Utf8(name),constantPool);		
		}
		
		for(Pair<Type.Clazz,List<Modifier>> i : inners) {
			Constant.addPoolItem(Constant.buildClass(type),constantPool);
			Constant.addPoolItem(Constant.buildClass(i.first()),constantPool);
			String name = i.first().lastComponent().first();
			Constant.addPoolItem(new Constant.Utf8(name),constantPool);										
		}		
	}
	
	public void print(PrintWriter output, Map<Constant.Info, Integer> constantPool) {
		output.println("  InnerClasses:");
		for(Pair<Type.Clazz,List<Modifier>> i : outers) {
			String name = type.lastComponent().first();
			int nameIndex = constantPool.get(new Constant.Utf8(name));			
			int outerIndex = constantPool.get(Constant.buildClass(i.first()));
			int innerIndex = constantPool.get(Constant.buildClass(type));
			output.print("   ");			
			output.print(nameIndex + " (");
			BytecodeFileWriter.writeModifiers(i.second(),output);					
			output.println(") = " + innerIndex + " of " + outerIndex);
		}	
		
		for(Pair<Type.Clazz,List<Modifier>> i : inners) {
			String name = i.first().lastComponent().first();
			int nameIndex = constantPool.get(new Constant.Utf8(name));
			int outerIndex = constantPool.get(Constant.buildClass(type));
			int innerIndex = constantPool.get(Constant.buildClass(i.first()));	
			output.print("   ");			
			output.print(nameIndex + " (");
			BytecodeFileWriter.writeModifiers(i.second(),output);					
			output.println(") = " + innerIndex + " of " + outerIndex);
		}			
	}
	
	/**
     * Write attribute detailing what direct inner classes there are for this
     * class, or what inner class this class is in.
     * 
     * @param clazz
     * @param constantPool
     */
	public void write(BinaryOutputStream output,
			Map<Constant.Info, Integer> constantPool) throws IOException {
		output.write_u2(constantPool.get(new Constant.Utf8("InnerClasses")));
		
		int ninners = inners.size() + outers.size();
		
		output.write_u4(2 + (8 * ninners));
		output.write_u2(ninners);
		
		for(Pair<Type.Clazz,List<Modifier>> i : outers) {
			output.write_u2(constantPool.get(Constant.buildClass(type)));
			output.write_u2(constantPool.get(Constant.buildClass(i.first())));
			String name = type.lastComponent().first();
			output.write_u2(constantPool.get(new Constant.Utf8(name)));
			ClassFileWriter.writeModifiers(i.second(),output);								
		}		
		
		for(Pair<Type.Clazz,List<Modifier>> i : inners) {
			output.write_u2(constantPool.get(Constant.buildClass(i.first())));
			output.write_u2(constantPool.get(Constant.buildClass(type)));
			String name = i.first().lastComponent().first();
			output.write_u2(constantPool.get(new Constant.Utf8(name)));
			ClassFileWriter.writeModifiers(i.second(),output);			
		}		
	}
	
}
