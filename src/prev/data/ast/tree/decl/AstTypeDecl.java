package prev.data.ast.tree.decl;

import prev.common.report.*;
import prev.data.ast.tree.type.*;
import prev.data.ast.visitor.*;

/**
 * Type declaration.
 */
public class AstTypeDecl extends AstNameDecl {

	/** The representation of this type. */
	public final AstType type;

	/**
	 * Constructs a type declaration.
	 * 
	 * @param location The location.
	 * @param name     The name of this type.
	 * @param type     The representation of this type.
	 */
	public AstTypeDecl(Location location, String name, AstType type) {
		super(location, name);
		this.type = type;
	}

	@Override
	public <Result, Arg> Result accept(AstVisitor<Result, Arg> visitor, Arg arg) {
		return visitor.visit(this, arg);
	}

}
