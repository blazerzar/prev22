package prev.phase.seman;

import prev.data.ast.tree.expr.*;
import prev.data.ast.tree.decl.*;
import prev.data.ast.tree.stmt.AstAssignStmt;
import prev.data.typ.*;
import prev.data.ast.visitor.*;
import prev.common.report.*;

/**
 * Address resolver.
 *
 * The address resolver finds out which expressions denote lvalues and leaves
 * the information in {@link SemAn#isAddr}.
 */
public class AddrResolver extends AstFullVisitor<Object, Object> {

	// EXPRESSIONS

	@Override
	public Object visit(AstArrExpr arrExpr, Object arg) {
        boolean res = (Boolean) arrExpr.arr.accept(this, arg);
        if (res) {
            SemAn.isAddr.put(arrExpr, true);
            return true;
        } else {
            SemAn.isAddr.put(arrExpr, false);
            return false;
        }
	}

	@Override
	public Object visit(AstAtomExpr atomExpr, Object arg) {
        SemAn.isAddr.put(atomExpr, false);
        return false;
	}

	@Override
	public Object visit(AstBinExpr binExpr, Object arg) {
        Object fstRes = binExpr.fstExpr.accept(this, arg);
        Object sndRes = binExpr.sndExpr.accept(this, arg);
        SemAn.isAddr.put(binExpr, false);
        return false;
	}

	@Override
	public Object visit(AstCallExpr callExpr, Object arg) {
        Object res = callExpr.args.accept(this, arg);
        SemAn.isAddr.put(callExpr, false);
		return false;
	}

	@Override
	public Object visit(AstCastExpr castExpr, Object arg) {
        Object res = castExpr.expr.accept(this, arg);
        SemAn.isAddr.put(castExpr, false);
		return false;
	}

	@Override
	public Object visit(AstNameExpr nameExpr, Object arg) {
        AstDecl decl = SemAn.declaredAt.get(nameExpr);

        // Variables and parameters are l-values
        if (decl instanceof AstVarDecl || decl instanceof AstParDecl) {
            SemAn.isAddr.put(nameExpr, true);
            return true;
        } else {
            SemAn.isAddr.put(nameExpr, false);
            return false;
        }
	}

	@Override
	public Object visit(AstPfxExpr pfxExpr, Object arg) {
        Object res = pfxExpr.expr.accept(this, arg);
        SemAn.isAddr.put(pfxExpr, false);
		return false;
	}

	@Override
	public Object visit(AstRecExpr recExpr, Object arg) {
        boolean res = (Boolean) recExpr.rec.accept(this, arg);
        if (res) {
            SemAn.isAddr.put(recExpr, true);
            return true;
        } else {
            SemAn.isAddr.put(recExpr, false);
            return false;
        }
	}

	@Override
	public Object visit(AstSfxExpr sfxExpr, Object arg) {
        Object res = sfxExpr.expr.accept(this, arg);

        // Dereferenced pointers are l-values
        if (SemAn.ofType.get(sfxExpr.expr).actualType() instanceof SemPtr) {
            SemAn.isAddr.put(sfxExpr, true);
            return true;
        } else {
            SemAn.isAddr.put(sfxExpr, false);
            return false;
        }
	}

	@Override
	public Object visit(AstStmtExpr stmtExpr, Object arg) {
        Object res = stmtExpr.stmts.accept(this, arg);
        SemAn.isAddr.put(stmtExpr, false);
		return false;
	}

	@Override
	public Object visit(AstWhereExpr whereExpr, Object arg) {
        Object resDecls = whereExpr.decls.accept(this, arg);
        Object resExpr = whereExpr.expr.accept(this, arg);
        SemAn.isAddr.put(whereExpr, false);
		return false;
	}

	// STATEMENTS

	@Override
	public Object visit(AstAssignStmt assignStmt, Object arg) {
		boolean resDst = (Boolean) assignStmt.dst.accept(this, arg);
		Object resSrc = assignStmt.src.accept(this, arg);

        if (!resDst) {
            throw new Report.Error(assignStmt,
				"Left hand side of assignment needs to be an l-value");
        }

        return null;
	}
}
