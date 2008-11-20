package jkit.compiler;

import java.io.IOException;

import jkit.jkil.Clazz;

public interface ClassWriter {
	
	/**
     * This method writes a class out to a file in a particular format (e.g.
     * classfile).
     * 
     * @param clazz
     * @throws IOException
     */
	void writeClass(Clazz clazz) throws IOException;
}