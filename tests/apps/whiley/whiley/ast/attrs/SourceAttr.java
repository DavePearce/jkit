package whiley.ast.attrs;


/**
 * A Source location Attribute identifies a position within an originating
 * source file. This is useful for producing good error messages.
 * 
 * @author djp
 * 
 */
public class SourceAttr implements Attribute {
	private String filename;
	private int start;	
	private int end;	

	public SourceAttr(String filename, int start, int end) {
		this.filename = filename;
		this.start = start;
		this.end = end;		
	}

	/**
	 * Get the source filename that this line corresponds to.
	 * @return
	 */
	public String filename() { return filename; }
	
	/**
	 * Get the index of the first character in this element.
	 * 
	 * @return 
	 */		
	public int start() { return start; }

	/**
	 * Get the index of the last character in this element.
	 * 
	 * @return 
	 */
	public int end() { return end; }
}
