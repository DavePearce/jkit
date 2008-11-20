package mocha.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.antlr.runtime.tree.Tree;

import jkit.java.JavaFileReader;
import jkit.jkil.Type;

public class MochaFileReader extends JavaFileReader {
	
	/**
	 * Create a JavaFileReader from a file.
	 * @param file the filename to read from.
	 * 
	 * @throws IOException
	 */
	public MochaFileReader(String file) throws IOException {
		super(file);
	}

	/**
	 * Create a JavaFileReader from a general Reader.
	 * @param r the reader to read from
	 * 
	 * @throws IOException
	 */
	public MochaFileReader(Reader r) throws IOException {
		super(r);
	}

	/**
	 * Create a JavaFileReader from a general InputStream
	 * @param in the input stream to read from
	 * 
	 * @throws IOException
	 */
	public MochaFileReader(InputStream in) throws IOException {
		super(in);
	}
	
	protected Type parseType(Tree type) {
		Tree c = type.getChild(0);
		String ct = c.getText();
		if(ct.equals("var")) {
			return Type.voidType();
		}
		else {
			return super.parseType(type);
		}
	}
	
}
