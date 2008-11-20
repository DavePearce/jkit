package jkit.jkil;

/**
 * A Source Location identifies a position within an originating source file.
 * This is useful for producing good error messages.
 * 
 * @author djp
 * 
 */
public class SourceLocation {
	private String file;
	private int line;
	private int column;
	private FlowGraph.Stmt statement;

	public SourceLocation(FlowGraph.Stmt stmt, String file, int line, int column) {
		this.file = file;
		this.line = line;
		this.column = column;
		this.statement = stmt;
	}

	public SourceLocation(FlowGraph.Stmt stmt, String file, int line) {
		this.file=file;
		this.line = line;
		this.column = -1;
		this.statement = stmt;
	}

	public SourceLocation(FlowGraph.Stmt stmt, String file) {
		this.file=file;
		this.line = -1;
		this.column = -1;
		this.statement = stmt;
	}		

	public SourceLocation(FlowGraph.Stmt stmt) {
		this.file="unknown";
		this.line = -1;
		this.column = -1;
		this.statement = stmt;
	}

	public void updateStmt(FlowGraph.Stmt s) {
		statement = s;
	}

	/**
	 * Get statement at this source location
	 * 
	 * @return statement (or null, if none)
	 */
	public FlowGraph.Stmt statement() { return statement; }

	/**
	 * Set statement at this source location
	 * 
	 * @param stmt statement (may be null)
	 */
	public void setStatement(FlowGraph.Stmt stmt) { statement = stmt; }

	/**
	 * Get source file where this source location originates from.
	 * 
	 * @return 
	 */
	public String source() { return file; }

	/**
	 * Set originating source file.
	 * 
	 * @param src
	 */
	public void setSource(String src) { file = src; }

	/**
	 * Get line number in originating source file for this source location.
	 * 
	 * @return 
	 */		
	public int line() { return line; }

	/**
	 * Set line number in originating source file for this source location.
	 * 
	 * @return 
	 */
	public void setLine(int line) { this.line = line; }

	/**
	 * Get column number in originating source file for this source location.
	 * 
	 * @return 
	 */
	public int column() { return column; }

	/**
	 * Set column number in originating source file for this source location.
	 * 
	 * @return 
	 */
	public void setColumn(int column) { this.column = column; }

	public String toString() {
		if(statement == null) {
			return file + ":" + 
			Integer.toString(line) + ":" +
			Integer.toString(column);			
		} else {
			return "\"" + statement + "\":" + file + ":" + 
			Integer.toString(line) + ":" +
			Integer.toString(column);
		}
	}

	public boolean equals(Object o) {
		return this == o;
	}				
}
