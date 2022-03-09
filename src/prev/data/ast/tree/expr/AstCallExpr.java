package prev.data.ast.tree.expr;

import prev.common.report.*;
import prev.data.ast.tree.*;
import prev.data.ast.visitor.*;

/**
 * Subprogram call.
 */
public class AstCallExpr extends AstNameExpr {

	/** The arguments. */
	public final AstTrees<AstExpr> args;

	/**
	 * Constructs a function call.
	 * 
	 * @param location The location.
	 * @param name     The name.
	 * @param args     The arguments.
	 */
	public AstCallExpr(Location location, String name, AstTrees<AstExpr> args) {
		super(location, name);
		this.args = args;
	}

	@Override
	public <Result, Arg> Result accept(AstVisitor<Result, Arg> visitor, Arg arg) {
		return visitor.visit(this, arg);
	}

}
