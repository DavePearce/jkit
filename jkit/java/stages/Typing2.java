package jkit.java.stages;

import jkit.compiler.ClassLoader;
import jkit.java.JavaFile;
import jkit.java.JavaFile.*;


/**
 * This class goes through a JavaFile and propagate type attributes
 * appropriately. In doing this, the stage will add implicit cast conversion
 * where appropriate.
 * 
 * @author djp
 * 
 */
public class Typing2 {
	private ClassLoader loader;
	
	public Typing2(ClassLoader loader) {
		this.loader = loader; 
	}
	
	public void apply(JavaFile file) {
		for(Declaration d : file.declarations()) {
			apply(d);
		}
	}
	
	protected void apply(Declaration d) {
		if(d instanceof Interface) {
			apply((Interface)d);
		} else if(d instanceof Clazz) {
			apply((Clazz)d);
		} else if(d instanceof Method) {
			apply((Method)d);
		} else if(d instanceof Field) {
			apply((Field)d);
		}
	}
	
	protected void apply(Interface d) {
		
	}
	
	protected void apply(Clazz d) {

	}

	protected void apply(Method d) {

	}

	protected void apply(Field d) {

	}
}
