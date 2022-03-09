package prev.data.ast.tree.type;

import prev.common.report.*;
import prev.data.ast.tree.*;
import prev.data.ast.visitor.*;

/**
 * Pointer type.
 */
public class AstPtrType extends AstNode implements AstType {

	/** Base type. */
	public final AstType baseType;

	/**
	 * Constructs a pointer type.
	 * 
	 * @param location The location.
	 * @param baseType The base type.
	 */
	public AstPtrType(Location location, AstType baseType) {
		super(location);
		this.baseType = baseType;
	}

	@Override
	public <Result, Arg> Result accept(AstVisitor<Result, Arg> visitor, Arg arg) {
		return visitor.visit(this, arg);
	}

}
