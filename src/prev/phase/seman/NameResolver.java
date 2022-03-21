package prev.phase.seman;

import prev.common.report.*;
import prev.data.ast.tree.*;
import prev.data.ast.tree.decl.*;
import prev.data.ast.tree.expr.*;
import prev.data.ast.tree.stmt.*;
import prev.data.ast.tree.type.*;
import prev.data.ast.visitor.*;

/**
 * Name resolver.
 * 
 * Name resolver connects each node of a abstract syntax tree where a name is
 * used with the node where it is declared. The only exceptions are a record
 * field names which are connected with its declarations by type resolver. The
 * results of the name resolver are stored in
 * {@link prev.phase.seman.SemAn#declaredAt}.
 */
public class NameResolver extends AstFullVisitor<Object, NameResolver.Mode> {

	public enum Mode {
		HEAD, BODY
	}

	private SymbTable symbTable = new SymbTable();

	// GENERAL PURPOSE

	@Override
	public Object visit(AstTrees<? extends AstTree> trees, Mode mode) {
		for (Mode m : new Mode[]{ Mode.HEAD, Mode.BODY }) {
			for (AstTree t : trees) {
				if (t != null && t instanceof AstDecl) {
					t.accept(this, m);
				} else if (t != null && m == Mode.BODY) {
					t.accept(this, null);
				}
			}
		}
		return null;
	}

	// DECLARATIONS

	@Override
	public Object visit(AstFunDecl funDecl, Mode mode) {
		if (mode == Mode.HEAD) {
			try {
				// Add function declaration name to dictionary
				symbTable.ins(funDecl.name, funDecl);
			} catch (SymbTable.CannotInsNameException e) {
				throw new Report.Error(funDecl, funDecl.name + " : Function name already declared");
			}
		} else {
			// Increase scope
			symbTable.newScope();

			// Name resolve parameters, return type and body expression
			if (funDecl.pars != null) {
				funDecl.pars.accept(this, null);
			}
			if (funDecl.type != null) {
				funDecl.type.accept(this, null);
			}
			if (funDecl.expr != null) {
				funDecl.expr.accept(this, null);
			}

			// Decrease scope
			symbTable.oldScope();
		}
		return null;
	}

	@Override
	public Object visit(AstParDecl parDecl, Mode mode) {
		if (parDecl.type != null) {
			if (mode == Mode.HEAD) {
				try {
					// Add parameter declaration name to dictionary
					symbTable.ins(parDecl.name, parDecl);
				} catch (SymbTable.CannotInsNameException e) {
					throw new Report.Error(parDecl, parDecl.name + " : Parameter name already used");
				}
			} else if (mode == Mode.BODY) {
				// Name resolve parameter type
				parDecl.type.accept(this, null);
			}
		}
		return null;
	}

	@Override
	public Object visit(AstTypeDecl typeDecl, Mode mode) {
		if (typeDecl.type != null) {
			if (mode == Mode.HEAD) {
				try {
					// Add type declaration name to dictionary
					symbTable.ins(typeDecl.name, typeDecl);
				} catch (SymbTable.CannotInsNameException e) {
					throw new Report.Error(typeDecl, typeDecl.name + " : Type already declared");
				}
			} else {
				// Name check type definition
				typeDecl.type.accept(this, null);
			}
		}
		return null;
	}

	@Override
	public Object visit(AstVarDecl varDecl, Mode mode) {
		if (varDecl.type != null) {
			if (mode == Mode.HEAD) {
				try {
					// Add var declaration name to dictionary
					symbTable.ins(varDecl.name, varDecl);
				} catch (SymbTable.CannotInsNameException e) {
					throw new Report.Error(varDecl, varDecl.name + " : Variable already declared");
				}
			} else {
				// Name check variable type
				varDecl.type.accept(this, null);
			}
		}
		return null;
	}

	// EXPRESSIONS

	@Override
	public Object visit(AstCallExpr callExpr, Mode mode) {
		if (callExpr.args != null) {
			try {
				// Find function declaration in dictionary
				SemAn.declaredAt.put(callExpr, symbTable.fnd(callExpr.name));
			} catch (SymbTable.CannotFndNameException e) {
				throw new Report.Error(callExpr, callExpr.name + " : Cannot resolve name");
			}

			// Name resolve function arguments
			callExpr.args.accept(this, mode);
		}
		return null;
	}

	@Override
	public Object visit(AstNameExpr nameExpr, Mode mode) {
		try {
			// Find expression name declaraction in dictionary
			SemAn.declaredAt.put(nameExpr, symbTable.fnd(nameExpr.name));
		} catch (SymbTable.CannotFndNameException e) {
			throw new Report.Error(nameExpr, nameExpr.name + " : Cannot resolve name");
		}
		return null;
	}

	@Override
	public Object visit(AstRecExpr recExpr, Mode mode) {
		// Only resolve record name
		if (recExpr.rec != null) {
			recExpr.rec.accept(this, mode);
		}
		return null;
	}

	@Override
	public Mode visit(AstWhereExpr whereExpr, Mode mode) {
		// Increase scope
		symbTable.newScope();

		// Name resolve declarations first and then expression
		if (whereExpr.decls != null) {
			whereExpr.decls.accept(this, null);
		}
		if (whereExpr.expr != null) {
			whereExpr.expr.accept(this, mode);
		}

		// Decrease scope
		symbTable.oldScope();

		return null;
	}

	// TYPES

	@Override
	public Object visit(AstNameType nameType, Mode mode) {
		try {
			// Find type name declaraction in dictionary
			SemAn.declaredAt.put(nameType, symbTable.fnd(nameType.name));
		} catch (SymbTable.CannotFndNameException e) {
			throw new Report.Error(nameType, nameType.name + " : Cannot resolve name");
		}
		return null;
	}

}

