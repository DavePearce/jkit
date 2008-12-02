package jkit.jkil;

/**
 * A Source Location identifies a position within an originating source file.
 * This is useful for producing good error messages.
 * 
 * @author djp
 * 
 */
public class SourceLocation implements Attribute {
	private String file;
	private int line;
	private int column;	

	public SourceLocation(String file, int line, int column) {
		this.file = file;
		this.line = line;
		this.column = column;		
	}

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
		return file + ":" + Integer.toString(line) + ":"
				+ Integer.toString(column);					
	}				
}
