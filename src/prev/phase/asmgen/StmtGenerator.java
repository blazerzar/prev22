package prev.phase.asmgen;

import java.util.*;
import prev.data.imc.code.expr.*;
import prev.data.imc.code.stmt.*;
import prev.data.imc.visitor.*;
import prev.data.mem.*;
import prev.data.asm.*;
import prev.common.report.*;

/**
 * Machine code generator for statements.
 */
public class StmtGenerator implements ImcVisitor<Vector<AsmInstr>, Object> {

    public Vector<AsmInstr> visit(ImcCJUMP cjump, Object arg) {
        Vector<AsmInstr> instrs = new Vector<>();
        // Generate instruction to evaluate the condition
        MemTemp cond = cjump.cond.accept(new ExprGenerator(), instrs);
        Vector<MemTemp> uses = new Vector<>(
            Arrays.asList(new MemTemp[]{ cond }));

        // Branch to positive label if condition is nonzero
        instrs.add(new AsmOPER("\t\tBNZ\t`s0," + cjump.posLabel.name, uses, null,
            // We can jump to both labels from here
            new Vector<>(Arrays.asList(new MemLabel[]{
                cjump.posLabel, cjump.negLabel
            }))
        ));

        return instrs;
    }

    public Vector<AsmInstr> visit(ImcESTMT eStmt, Object arg) {
        // Generate instructions to evaluate the expression
        Vector<AsmInstr> instrs = new Vector<>();
        eStmt.expr.accept(new ExprGenerator(), instrs);
        return instrs;
    }

    public Vector<AsmInstr> visit(ImcJUMP jump, Object arg) {
        Vector<AsmInstr> instrs = new Vector<>();
        instrs.add(new AsmOPER("\t\tJMP\t" + jump.label.name, null, null,
            new Vector<>(Arrays.asList(new MemLabel[]{ jump.label }))
        ));
        return instrs;
    }

    public Vector<AsmInstr> visit(ImcLABEL label, Object arg) {
        return new Vector<>(Arrays.asList(new AsmInstr[]{
            new AsmLABEL(label.label)
        }));
    }

    public Vector<AsmInstr> visit(ImcMOVE move, Object arg) {
        Vector<AsmInstr> instrs = new Vector<>();

        MemTemp dst;
        if (move.dst instanceof ImcMEM) {
            // Get address in a register
            dst = ((ImcMEM) move.dst).addr.accept(new ExprGenerator(), instrs);
        } else if (move.dst instanceof ImcTEMP) {
            dst = move.dst.accept(new ExprGenerator(), instrs);
        } else {
            throw new Report.InternalError();
        }

        // Generate instructions for right hand side
        MemTemp src = move.src.accept(new ExprGenerator(), instrs);

        if (move.dst instanceof ImcMEM) {
            Vector<MemTemp> uses = new Vector<>(
                Arrays.asList(new MemTemp[]{ src, dst }));
            instrs.add(new AsmOPER("\t\tSTO\t`s0,`s1,0", uses, null, null));
        } else {
            Vector<MemTemp> uses = new Vector<>(
                Arrays.asList(new MemTemp[]{ src }));
            Vector<MemTemp> defs = new Vector<>(
                Arrays.asList(new MemTemp[]{ dst }));
            instrs.add(new AsmMOVE("\t\tSET\t`d0,`s0", uses, defs));
        }

        return instrs;
    }

    public Vector<AsmInstr> visit(ImcSTMTS stmts, Object arg) {
        Vector<AsmInstr> instrs = new Vector<>();

        // Generate instructions for every statement
        for (ImcStmt stmt : stmts.stmts) {
            instrs.addAll(stmt.accept(this, arg));
        }

        return instrs;
    }

}
