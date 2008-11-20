package jkit.java;

import java.io.IOException;

import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;

public class JavaFileReader2 {
	
	private JavaFile ast;
	
	/**
     * Create a JavaFileReader from a file.
     * 
     * @param file
     *            the filename to read from.
     * 
     * @param loader
     *            the class loader to use for resolving types
     * 
     * @throws IOException
     */
	public JavaFileReader2(String file)
			throws IOException {
		CommonTokenStream tokenStream = new CommonTokenStream(new JavaLexer(
				new ANTLRFileStream(file)));		
		
		JavaParser parser = new JavaParser(tokenStream);
		
		try {
			ast = (JavaFile) parser.compilationUnit().getTree();
		} catch (RecognitionException e) {
		}
	}
}
