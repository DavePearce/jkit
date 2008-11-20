package jkit.core;

/**
 * This method is thrown when some kind of error happens. This will typically
 * reflect a syntax error or other similar problem with the source file being
 * compiled.
 * 
 * @author djp
 * 
 */
public class InternalException extends RuntimeException {
	private Clazz owner;
	private Method method;
	private FlowGraph.Point point;	
	
	public InternalException(FlowGraph.Point point, Method method, Clazz owner) {
		this.owner = owner;
		this.method = method;
		this.point = point;
	}
	
	public InternalException(String msg, FlowGraph.Point point, Method method, Clazz owner) {
		super(msg);
		this.owner = owner;
		this.method = method;
		this.point = point;
	}
	
	public InternalException(Throwable cause, FlowGraph.Point point, Method method, Clazz owner) {
		super(cause);
		this.owner = owner;
		this.method = method;
		this.point = point;
	}
		
	public Clazz owner() { return owner; }
	public Method method() { return method; }
	public FlowGraph.Point point() { return point; }
	
	public static final long serialVersionUID = 1l;
}
