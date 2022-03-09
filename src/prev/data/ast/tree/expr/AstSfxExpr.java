package prev.data.ast.tree.expr;

import prev.common.report.*;
import prev.data.ast.tree.*;
import prev.data.ast.visitor.*;

/**
 * Suffix expression.
 */
public class AstSfxExpr extends AstNode implements AstExpr {

	/** Operators. */
	public enum Oper {
		PTR
	};

	/** The operator. */
	public final Oper oper;

	/** The subexpression. */
	public final AstExpr expr;

	/**
	 * Constructs a suffix expression.
	 * 
	 * @param location The location.
	 * @param oper     The operator.
	 * @param expr     The subexpression.
	 */
	public AstSfxExpr(Location location, Oper oper, AstExpr expr) {
		super(location);
		this.oper = oper;
		this.expr = expr;
	}

	@Override
	public <Result, Arg> Result accept(AstVisitor<Result, Arg> visitor, Arg arg) {
		return visitor.visit(this, arg);
	}

}
