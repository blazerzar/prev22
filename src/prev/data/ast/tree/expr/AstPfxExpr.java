package prev.data.ast.tree.expr;

import prev.common.report.*;
import prev.data.ast.tree.*;
import prev.data.ast.visitor.*;

/**
 * Prefix expression.
 */
public class AstPfxExpr extends AstNode implements AstExpr {

	/** Operators. */
	public enum Oper {
		ADD, SUB, NOT, PTR, NEW, DEL
	};

	/** The operator. */
	public final Oper oper;

	/** The subexpression. */
	public final AstExpr expr;

	/**
	 * Constructs a prefix expression.
	 * 
	 * @param location The location.
	 * @param oper     The operator.
	 * @param expr     The subexpression.
	 */
	public AstPfxExpr(Location location, Oper oper, AstExpr expr) {
		super(location);
		this.oper = oper;
		this.expr = expr;
	}

	@Override
	public <Result, Arg> Result accept(AstVisitor<Result, Arg> visitor, Arg arg) {
		return visitor.visit(this, arg);
	}

}
