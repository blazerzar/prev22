package prev.phase.memory;

import prev.data.ast.tree.*;
import prev.data.ast.tree.decl.*;
import prev.data.ast.tree.expr.*;
import prev.data.ast.tree.type.*;
import prev.data.ast.visitor.*;
import prev.data.typ.*;
import prev.data.mem.*;
import prev.phase.seman.*;

/**
 * Computing memory layout: frames and accesses.
 */
public class MemEvaluator extends AstFullVisitor<Object, MemEvaluator.Context> {

	/**
	 * The context {@link MemEvaluator} uses while computing function frames and
	 * variable accesses.
	 */
	protected abstract class Context {
	}

	public class FunctionContext extends Context {
		int depth = 0;
		int localVarsSize = 0;
		int paramsSize = 0;
		int argsSize = 0; 			// Function call arguments + SL
	}

	public class RecordContext extends Context {
		int compsSize = 0;
	}

	// FUNCTION DECLARATIONS

	@Override
	public Object visit(AstFunDecl funDecl, Context context) {
		FunctionContext subContext = new FunctionContext();
		MemLabel funLabel;
		if (context != null) {
			// Local function
			subContext.depth = ((FunctionContext) context).depth + 1;
			funLabel = new MemLabel();
		} else {
			// Global function
			subContext.depth = 1;
			funLabel = new MemLabel(funDecl.name);
		}

		// Visit parameters, return type and expression
		if (funDecl.pars != null) {
			funDecl.pars.accept(this, subContext);
		}
		funDecl.type.accept(this, subContext);
		if (funDecl.expr != null) {
			funDecl.expr.accept(this, subContext);
		}

		// Create frame
		Memory.frames.put(funDecl, new MemFrame(
			funLabel,
			subContext.depth - 1,
			subContext.localVarsSize,
			subContext.argsSize
		));

		return null;
	}

	// FUNCTION CALLS

	@Override
	public Object visit(AstCallExpr callExpr, Context context) {
		FunctionContext funContext = (FunctionContext) context;

		// Visit arguments to also take calls in arguments into account
		callExpr.args.accept(this, context);

		// Find out if this function call has highest number of arguments
		int argsSize = 8;
		for (AstExpr arg : callExpr.args) {
			arg.accept(this, context);
			argsSize += SemAn.ofType.get(arg).size();
		}
		funContext.argsSize = argsSize > funContext.argsSize ?
											argsSize : funContext.argsSize;

		return null;
	}

	@Override
	public Object visit(AstNameExpr nameExpr, Context context) {
		AstDecl decl = SemAn.declaredAt.get(nameExpr);
		if (decl instanceof AstFunDecl) {
			// We only need SL in the frame for this function call
			FunctionContext funContext = (FunctionContext) context;
			funContext.argsSize = 8 > funContext.argsSize ?
											8 : funContext.argsSize;
		}

		return null;
	}

	// VARIABLE DECLARATIONS

	@Override
	public Object visit(AstVarDecl varDecl, Context context) {
		// Visit type
		varDecl.type.accept(this, context);
		SemType type = SemAn.isType.get(varDecl.type);

		if (context != null) {
			// Local variable
			FunctionContext funContext = (FunctionContext) context;
			long typeSize = type.size();

			// We increase offset from FP
			funContext.localVarsSize += typeSize;

			Memory.accesses.put(varDecl, new MemRelAccess(
				typeSize,
				-funContext.localVarsSize,
				funContext.depth
			));
		} else {
			// Global variable
			Memory.accesses.put(varDecl, new MemAbsAccess(
				type.size(),
				new MemLabel(varDecl.name)
			));
		}

		return null;
	}

	// PARAMETER DECLARATION

	@Override
	public Object visit(AstParDecl parDecl, Context context) {
		// Visit type
		parDecl.type.accept(this, context);
		SemType type = SemAn.isType.get(parDecl.type);

		FunctionContext funContext = (FunctionContext) context;
		long typeSize = type.size();
		funContext.paramsSize += typeSize;

		Memory.accesses.put(parDecl, new MemRelAccess(
			typeSize,
			funContext.paramsSize,
			funContext.depth
		));

		return null;
	}

	// RECORD COMPONENT OFFSETS

	@Override
	public Object visit(AstRecType recType, Context context) {
		// Create new record context to keep track of current component offset
		recType.comps.accept(this, new RecordContext());
		return null;
	}

	@Override
	public Object visit(AstCompDecl compDecl, Context context) {
		// Visit type
		compDecl.type.accept(this, context);
		SemType type = SemAn.isType.get(compDecl.type);

		RecordContext recContext = (RecordContext) context;
		Memory.accesses.put(compDecl, new MemRelAccess(
			type.size(),
			recContext.compsSize,
			0
		));

		// Move offset to the next component
		recContext.compsSize += type.size();

		return null;
	}

	// STRING CONSTANTS

	@Override
	public Object visit(AstAtomExpr atomExpr, Context context) {
		// Map string constants to accesses
		if (atomExpr.type == AstAtomExpr.Type.STRING) {
			// Get string size (length * charSize)
			long strSize = atomExpr.value.length() * 8;

			Memory.strings.put(atomExpr, new MemAbsAccess(
				strSize, new MemLabel(), atomExpr.value
			));
		}

		return null;
	}

}
