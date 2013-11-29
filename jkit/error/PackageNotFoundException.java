package jkit.error;

import java.util.List;

import jkit.compiler.Clazz;

/**
 * Exception for the case where a package isn't found during compilation.
 *
 * @author Daniel Campbell
 *
 */
public class PackageNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	private final List<String> classpath;	//The current classpath
	private final List<String> sourcepath;	//The current sourcepath
	private final Clazz jilClass;			//The class with the bad package declaration

	public PackageNotFoundException(Clazz jc, List<String> cp, List<String> sp) {
		jilClass = jc;
		classpath = cp;
		sourcepath = sp;
	}

	public Clazz jilClass() {
		return jilClass;
	}

	public List<String> classpath() {
		return classpath;
	}

	public List<String> sourcepath() {
		return sourcepath;
	}
}