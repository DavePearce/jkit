package whiley.ast.exprs;

public interface BinOp extends Expression {
	public Expression getLeftExpr();
	public Expression getRightExpr();
}
