package prev.data.ast.tree.decl;

import prev.common.report.*;
import prev.data.ast.tree.*;
import prev.data.ast.tree.expr.*;
import prev.data.ast.tree.type.*;
import prev.data.ast.visitor.AstVisitor;

/**
 * Any function declaration.
 */
public class AstFunDecl extends AstNameDecl {

	/** The parameters of this function. */
	public final AstTrees<AstParDecl> pars;

	/** The type of this function. */
	public final AstType type;

	/** The expression of this function. */
	public final AstExpr expr;

	/**
	 * Constructs a function declaration.
	 * 
	 * @param location The location.
	 * @param name     The name of this function.
	 * @param pars     The parameters of this function.
	 * @param type     The type of this function.
	 * @param expr     The expression of this function.
	 */
	public AstFunDecl(Location location, String name, AstTrees<AstParDecl> pars, AstType type, AstExpr expr) {
		super(location, name);
		this.pars = pars;
		this.type = type;
		this.expr = expr;
	}
	
	@Override
	public <Result, Arg> Result accept(AstVisitor<Result, Arg> visitor, Arg arg) {
		return visitor.visit(this, arg);
	}

}
