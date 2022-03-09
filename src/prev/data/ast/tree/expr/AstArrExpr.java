package prev.data.ast.tree.expr;

import prev.common.report.*;
import prev.data.ast.tree.*;
import prev.data.ast.visitor.*;

/**
 * Array access expression.
 */
public class AstArrExpr extends AstNode implements AstExpr {

	/** The array. */
	public final AstExpr arr;

	/** The index. */
	public final AstExpr idx;

	/**
	 * Constructs an array access expression.
	 * 
	 * @param location The location.
	 * @param arr      The array.
	 * @param index The index.
	 */
	public AstArrExpr(Location location, AstExpr arr, AstExpr idx) {
		super(location);
		this.arr = arr;
		this.idx = idx;
	}

	@Override
	public <Result, Arg> Result accept(AstVisitor<Result, Arg> visitor, Arg arg) {
		return visitor.visit(this, arg);
	}

}
