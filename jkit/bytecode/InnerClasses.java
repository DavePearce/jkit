package jkit.bytecode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jkit.jil.tree.Clazz;
import jkit.jil.tree.Modifier;
import jkit.jil.tree.Type;
import jkit.util.Pair;

public class InnerClasses implements Attribute {
	protected Clazz clazz;
	
	public InnerClasses(Clazz clazz) {
		this.clazz = clazz;
	}
	
	/**
     * Write attribute detailing what direct inner classes there are for this
     * class, or what inner class this class is in.
     * 
     * @param clazz
     * @param pmap
     */
	public void write(BinaryOutputStream output,
			Map<Constant.Info, Integer> pmap) throws IOException {
		output.write_u2(pmap.get(new Constant.Utf8("InnerClasses")));
		
		int ninners = clazz.inners().size() + clazz.type().components().size()
				- 1;
		
		output.write_u4(2 + (8 * ninners));
		output.write_u2(ninners);
		
		if(clazz.isInnerClass()) {
			Type.Clazz inner = clazz.type();
			List<Pair<String,List<Type.Reference>>> classes = clazz.type().components();
			for(int i=classes.size()-1;i>0;--i) {		
				// First, we need to construct the outer reference type.
				List<Pair<String,List<Type.Reference>>> nclasses = new ArrayList();
				for(Pair<String,List<Type.Reference>> p : classes) {
					nclasses.add(p);
				}							
				Type.Clazz outer = new Type.Clazz(inner.pkg(),nclasses);
				// Now, we can actually write the information.
				output.write_u2(pmap.get(Constant.buildClass(inner)));
				output.write_u2(pmap.get(Constant.buildClass(outer)));
				output.write_u2(pmap.get(new Constant.Utf8(inner.components().get(
						inner.components().size() - 1).first())));
				try {
					// This dependence on ClassTable here is annoying really.
					Clazz innerC = loader.loadClass(inner);
					ClassFileWriter.writeModifiers(innerC.modifiers(),output);
				} catch(ClassNotFoundException e) {
					output.write_u2(0); // this is a problem!!!!
				 }
				inner = outer;				
			}
		}		
		
		for(Pair<Type.Clazz,List<Modifier>> i : clazz.inners()) {
			output.write_u2(pmap.get(Constant.buildClass(i.first())));
			output.write_u2(pmap.get(Constant.buildClass(clazz.type())));
			String name = i.first().lastComponent().first();
			output.write_u2(pmap.get(new Constant.Utf8(name)));
			ClassFileWriter.writeModifiers(i.second(),output);			
		}		
	}
	
}
