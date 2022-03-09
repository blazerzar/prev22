package prev.data.ast.tree.type;

import prev.common.report.*;
import prev.data.ast.tree.*;
import prev.data.ast.visitor.*;

/**
 * Atom type.
 */
public class AstAtomType extends AstNode implements AstType {

	/** Types. */
	public enum Type {
		VOID, CHAR, INT, BOOL
	};

	/** The type of a constant. */
	public final Type type;

	/**
	 * Constructs an atom type.
	 * 
	 * @param location The location.
	 * @param type     The type.
	 */
	public AstAtomType(Location location, Type type) {
		super(location);
		this.type = type;
	}

	@Override
	public <Result, Arg> Result accept(AstVisitor<Result, Arg> visitor, Arg arg) {
		return visitor.visit(this, arg);
	}

}
