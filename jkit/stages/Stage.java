package jkit.stages;

import jkit.jkil.Clazz;

public interface Stage {
	public String description();
	
	/**
     * Apply this pass to a class.
     * 
     * @param owner
     *            class to manipulate    
     */
	public void apply(Clazz owner);	
}
