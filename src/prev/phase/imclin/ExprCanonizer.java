package prev.phase.imclin;

import java.util.*;
import prev.data.mem.*;
import prev.data.imc.code.expr.*;
import prev.data.imc.code.stmt.*;
import prev.data.imc.visitor.*;

/**
 * Expression canonizer.
 */
public class ExprCanonizer implements ImcVisitor<ImcExpr, Vector<ImcStmt>> {

    public ImcExpr visit(ImcBINOP binOp, Vector<ImcStmt> stmts) {
        // Save left expression into temp
        MemTemp lhs = new MemTemp();
        stmts.add(new ImcMOVE(
            new ImcTEMP(lhs), binOp.fstExpr.accept(this, stmts)));

        // Save right expression into temp
        MemTemp rhs = new MemTemp();
        stmts.add(new ImcMOVE(
            new ImcTEMP(rhs), binOp.sndExpr.accept(this, stmts)));

        // Save result into temp
        MemTemp result = new MemTemp();
        stmts.add(new ImcMOVE(
            new ImcTEMP(result), new ImcBINOP(
                binOp.oper, new ImcTEMP(lhs), new ImcTEMP(rhs)
            )
        ));

        // Return result value
        return new ImcTEMP(result);
    }

    public ImcExpr visit(ImcCALL call, Vector<ImcStmt> stmts) {
        // Save arguments into temporary variables
        Vector<ImcExpr> args = new Vector<>();
        for (ImcExpr arg : call.args) {
            MemTemp argTemp = new MemTemp();
            stmts.add(new ImcMOVE(
                new ImcTEMP(argTemp), arg.accept(this, stmts)
            ));
            args.add(new ImcTEMP(argTemp));
        }

        // Save result into temp and return it
        MemTemp result = new MemTemp();
        stmts.add(new ImcMOVE(
            new ImcTEMP(result), new ImcCALL(
                call.label, call.offs, args
            )
        ));
        return new ImcTEMP(result);
    }

    public ImcExpr visit(ImcCONST constant, Vector<ImcStmt> stmts) {
        return new ImcCONST(constant.value);
    }

    public ImcExpr visit(ImcMEM mem, Vector<ImcStmt> stmts) {
        // Make sure addr is in canonical form
        return new ImcMEM(mem.addr.accept(this, stmts));
    }

    public ImcExpr visit(ImcNAME name, Vector<ImcStmt> stmts) {
        return new ImcNAME(name.label);
    }

    public ImcExpr visit(ImcSEXPR sExpr, Vector<ImcStmt> stmts) {
        // Canonize all stmts
        stmts.addAll(sExpr.stmt.accept(new StmtCanonizer(), null));
        return sExpr.expr.accept(this, stmts);
    }

    public ImcExpr visit(ImcTEMP temp, Vector<ImcStmt> stmts) {
        return new ImcTEMP(temp.temp);
    }

    public ImcExpr visit(ImcUNOP unOp, Vector<ImcStmt> stmts) {
        return new ImcUNOP(unOp.oper, unOp.subExpr.accept(this, stmts));
    }

}
