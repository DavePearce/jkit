package jkit.io;

import java.util.List;

import jkit.core.Clazz;

public interface ClassReader {

	/**
     * This method returns the set of class skeletons defined in the source file.
     * A class skeleton includes all information about a class, except for the
     * method bodies defined in that class. Observe that multiple classes may be
     * defined in the same source file (due to inner classes). The first class
     * in the returned list is the outermost.
     * 
     * The purpose of this method is to allow information about the class
     * hierarchy to be loaded into ClassTable before attempting to parse method
     * bodies. This is necessary since we require full information about the
     * class hierarchy of all classes being compiled before we can complete
     * parsing any single class.
     * 
     * @return
     */
	public List<Clazz> readSkeletons();
	
	/**
     * This method returns the classes defined in the source file. Observe that
     * multiple classes may be defined in the same source file (due to inner
     * classes).  The first class in the returned list is the outermost.
     * 
     * @return
     */
	public List<Clazz> readClasses();
}
