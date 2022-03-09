package prev.data.ast.tree.type;

import prev.common.report.*;
import prev.data.ast.tree.*;
import prev.data.ast.tree.expr.*;
import prev.data.ast.visitor.*;

/**
 * Array type.
 */
public class AstArrType extends AstNode implements AstType {

	/** Element type. */
	public final AstType elemType;

	/** Number of elements. */
	public final AstExpr numElems;

	/**
	 * Constructs an array type.
	 * 
	 * @param location The location.
	 * @param elemType The element type.
	 * @param numElems The number of elements.
	 */
	public AstArrType(Location location, AstType elemType, AstExpr numElems) {
		super(location);
		this.elemType = elemType;
		this.numElems = numElems;
	}

	@Override
	public <Result, Arg> Result accept(AstVisitor<Result, Arg> visitor, Arg arg) {
		return visitor.visit(this, arg);
	}

}
