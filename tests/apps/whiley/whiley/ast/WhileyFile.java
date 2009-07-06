package whiley.ast;

import java.util.*;

public class WhileyFile {
	private String filename;
	private ArrayList<Function> decls;

	public WhileyFile(String filename, ArrayList<Function> declarations) {
		this.filename = filename;
		this.decls = declarations;
	}

	public String filename() {
		return filename;
	}

	public List<Function> declarations() {
		return decls;
	}
}
