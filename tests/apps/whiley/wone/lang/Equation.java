package wone.lang;

public interface Equation extends Literal {
	public Polynomial lhs();
	public Polynomial rhs();
	
	/**
	 * The purpose of this method is to convert the method into a normal form
	 * (so-called standard form). In this form, the left-hand side is always
	 * zero. For example:
	 * 
	 * <pre>
	 * x == 2y  =====&gt;  0 == 2y - x
	 * x + y < 4 ======>   0 < 4 - x - y
	 * </pre>
	 * 
	 * @return
	 * 
	 */
	public Equation normalise();
	
	/**
	 * The purpose of this method is to rearrange the equation to collect terms
	 * involving the given variable on the lhs.
	 * 
	 * <pre>
	 * x &lt;= 2xy + 2 =====&gt; x-2xy &lt;= 2
	 * y &lt;= 3 + x   =====&gt; -x &lt;= 3 - y
	 * </pre>
	 * 
	 * @param var
	 * @return
	 */
	public Equation rearrange(String var);
}
