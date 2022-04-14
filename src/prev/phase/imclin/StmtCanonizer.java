package prev.phase.imclin;

import java.util.*;

import prev.common.report.*;
import prev.data.mem.*;
import prev.data.imc.code.expr.*;
import prev.data.imc.code.stmt.*;
import prev.data.imc.visitor.*;

/**
 * Statement canonizer.
 */
public class StmtCanonizer implements ImcVisitor<Vector<ImcStmt>, Object> {

    public Vector<ImcStmt> visit(ImcCJUMP cjump, Object arg) {
        Vector<ImcStmt> stmts = new Vector<>();

        // Canonize expression and add it to stmts
        stmts.add(new ImcCJUMP(
            cjump.cond.accept(new ExprCanonizer(), stmts),
            cjump.posLabel, cjump.negLabel
        ));

        return stmts;
    }

    public Vector<ImcStmt> visit(ImcESTMT eStmt, Object arg) {
        Vector<ImcStmt> stmts = new Vector<>();

        // Canonize expression and add it to stmts
        stmts.add(new ImcESTMT(eStmt.expr.accept(new ExprCanonizer(), stmts)));
        return stmts;
    }

    public Vector<ImcStmt> visit(ImcJUMP jump, Object arg) {
        return new Vector<>(Arrays.asList(new ImcStmt[]{
            new ImcJUMP(jump.label)
        }));
    }

    public Vector<ImcStmt> visit(ImcLABEL label, Object arg) {
        return new Vector<>(Arrays.asList(new ImcStmt[]{
            new ImcLABEL(label.label)
        }));
    }

    public Vector<ImcStmt> visit(ImcMOVE move, Object arg) {
        Vector<ImcStmt> stmts = new Vector<>();

        // Canonize destination and source and save them into temps
        MemTemp dstTemp = new MemTemp();

        if (move.dst instanceof ImcMEM) {
            // Save addr to temp
            stmts.add(new ImcMOVE(
                new ImcTEMP(dstTemp),
                ((ImcMEM) move.dst.accept(new ExprCanonizer(), stmts)).addr
            ));

            // Store to memory address
            stmts.add(new ImcMOVE(
                new ImcMEM(new ImcTEMP(dstTemp)),
                move.src.accept(new ExprCanonizer(), stmts)
            ));
        } else if (move.dst instanceof ImcTEMP) {
            // Store in temporary variable
            stmts.add(new ImcMOVE(
                move.dst.accept(new ExprCanonizer(), stmts),
                move.src.accept(new ExprCanonizer(), stmts)
            ));
        } else {
            // Error, this should not happen
            throw new Report.InternalError();
        }

        return stmts;
    }

    public Vector<ImcStmt> visit(ImcSTMTS stmts, Object arg) {
        Vector<ImcStmt> statements = new Vector<>();

        // Canonize every statement
        for (ImcStmt stmt : stmts.stmts) {
            statements.addAll(stmt.accept(this, null));
        }

        return statements;
    }

}
