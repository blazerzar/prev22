package prev.data.ast.tree.expr;

import prev.common.report.*;
import prev.data.ast.tree.*;
import prev.data.ast.tree.decl.*;
import prev.data.ast.visitor.*;

/**
 * Where expression.
 */
public class AstWhereExpr extends AstNode implements AstExpr {

	/** The expression. */
	public final AstExpr expr;

	/** The declarations. */
	public final AstTrees<AstDecl> decls;

	/**
	 * Constructs a where expression.
	 * 
	 * @param location The location.
	 * @param expr     The expression.
	 * @param decls    The declarations.
	 */
	public AstWhereExpr(Location location, AstExpr expr, AstTrees<AstDecl> decls) {
		super(location);
		this.expr = expr;
		this.decls = decls;
	}

	@Override
	public <Result, Arg> Result accept(AstVisitor<Result, Arg> visitor, Arg arg) {
		return visitor.visit(this, arg);
	}

}
