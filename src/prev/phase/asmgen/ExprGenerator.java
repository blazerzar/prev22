package prev.phase.asmgen;

import java.util.*;
import prev.data.mem.*;
import prev.data.imc.code.expr.*;
import prev.data.imc.visitor.*;
import prev.data.asm.*;
import prev.common.report.*;
import prev.Compiler;

/**
 * Machine code generator for expressions.
 */
public class ExprGenerator implements ImcVisitor<MemTemp, Vector<AsmInstr>> {

    private final int nregs;

    public ExprGenerator() {
        this.nregs = Integer.decode(Compiler.cmdLineArgValue("--nregs"));
    }

    public MemTemp visit(ImcBINOP binOp, Vector<AsmInstr> instrs) {
        // Create instructions for evaluating operands expressions
        MemTemp lhs = binOp.fstExpr.accept(this, instrs);
        MemTemp rhs = binOp.sndExpr.accept(this, instrs);

        MemTemp dst = new MemTemp();
        Vector<MemTemp> uses = new Vector<>(
            Arrays.asList(new MemTemp[]{ lhs, rhs }));
        Vector<MemTemp> defs = new Vector<>(
            Arrays.asList(new MemTemp[]{ dst }));

        switch (binOp.oper) {
            case OR, AND, ADD, SUB, MUL, DIV -> {
                instrs.add(new AsmOPER(
                    String.format("\t\t%s\t`d0,`s0,`s1", binOp.oper.name()),
                    uses, defs, null
                ));
            }
            case MOD -> {
                // After division the remainder is in register rR
                instrs.add(new AsmOPER("\t\tDIV\t`d0,`s0,`s1", uses, defs, null));
                instrs.add(new AsmOPER("\t\tGET\t`d0,rR", null, defs, null));
            }
            case default -> {
                String instruction = switch (binOp.oper) {
                    case EQU -> "ZSZ";   // Check if CMP returned 0
                    case NEQ -> "ZSNZ";  // Check if CMP did not return 0
                    case LTH -> "ZSN";   // Check if CMP returned -1 (negative)
                    case GTH -> "ZSP";   // Check if CMP returned +1 (positive)
                    case LEQ -> "ZSNP";  // Check if CMP returned -1 or 0 (nonpositive)
                    case GEQ -> "ZSNN";  // Check if CMP returned +1 or 0 (nonnegative)
                    default  -> throw new Report.InternalError();
                };

                instrs.add(new AsmOPER("\t\tCMP\t`d0,`s0,`s1", uses, defs, null));
                instrs.add(new AsmOPER(
                    String.format("\t\t%s\t`d0,`s0,1", instruction),
                    defs, defs, null));
            }
        }

        return dst;
    }

    public MemTemp visit(ImcCALL call, Vector<AsmInstr> instrs) {
        // Compute arguments and place them into registers
        List<MemTemp> args = new LinkedList<>();
        for (ImcExpr arg : call.args) {
            args.add(arg.accept(this, instrs));
        }

        // Put arguments at the end of the frame
        for (int i = 0; i < args.size(); ++i) {
            long offsetVal = call.offs.get(i);
            Vector<MemTemp> uses = new Vector<>(
                Arrays.asList(new MemTemp[]{ args.get(i) }));

            // Check if offset is small enough to fit as an immediate value
            if (offsetVal <= 255) {
                // M[$254 + offsetVal] = $arg
                instrs.add(new AsmOPER(
                    String.format("\t\tSTO\t`s0,$254,%d", offsetVal),
                    uses, null, null
                ));
            } else {
                // Put offset in a register
                uses.add((new ImcCONST(offsetVal)).accept(this, instrs));

                // M[$254 + $offset] = $arg
                instrs.add(new AsmOPER("\t\tSTO\t`s0,$254,`s1", uses, null, null));
            }
        }

        // Call the function
        Vector<MemLabel> jumps = new Vector<>(
            Arrays.asList(new MemLabel[]{ call.label }));
        instrs.add(new AsmOPER(
            String.format("\t\tPUSHJ\t$%s,%s", nregs, call.label.name),
            null, null, jumps));

        // Return value from SP
        Vector<MemTemp> defs = new Vector<>(
            Arrays.asList(new MemTemp[]{ new MemTemp() }));
        instrs.add(new AsmOPER("\t\tLDO\t`d0,$254,0", null, defs, null));

        return defs.get(0);
    }

    public MemTemp visit(ImcCONST constant, Vector<AsmInstr> instrs) {
        Vector<MemTemp> defs = new Vector<>(
            Arrays.asList(new MemTemp[]{ new MemTemp() }));

        // Save long into a register 2 bytes at a time
        int[] offsets = { 0, 16, 32, 48 };
        String[] instructions = { "SETL", "INCML", "INCMH", "INCH" };

        for (int i = 0; i < offsets.length; ++i) {
            int val = (int) (constant.value >> offsets[i] & (1L << 16) - 1);

            // Only save wyde if it is the first one or bigger than 0
            if (offsets[i] == 0 || val > 0) {
                instrs.add(new AsmOPER(
                    String.format("\t\t%s\t`d0,%d", instructions[i], val),
                    null, defs, null
                ));
            }
        }

        // Return temp (register) that includes the constant
        return defs.get(0);
    }

    public MemTemp visit(ImcMEM mem, Vector<AsmInstr> instrs) {
        // Create instructions for evaluation addr expression
        MemTemp src = mem.addr.accept(this, instrs);

        // Load octabyte from [addr] address in memory
        MemTemp dst = new MemTemp();
        Vector<MemTemp> uses = new Vector<>(
            Arrays.asList(new MemTemp[]{ src }));
        Vector<MemTemp> defs = new Vector<>(
            Arrays.asList(new MemTemp[]{ dst }));

        // TEMP(dst) <- M[TEMP(src) + 0]
        instrs.add(new AsmOPER("\t\tLDO\t`d0,`s0,0", uses, defs, null));

        return dst;
    }

    public MemTemp visit(ImcNAME name, Vector<AsmInstr> instrs) {
        // Save address represented by label in [dst]
        MemTemp dst = new MemTemp();
        Vector<MemTemp> defs = new Vector<>(
            Arrays.asList(new MemTemp[]{ dst }));
        instrs.add(new AsmOPER(
            String.format("\t\tLDA\t`d0,%s", name.label.name),
            null, defs, null
        ));
        return dst;
    }

    public MemTemp visit(ImcSEXPR sExpr, Vector<AsmInstr> instrs) {
        // These nodes have been removed by the canonizer
        return null;
    }

    public MemTemp visit(ImcTEMP temp, Vector<AsmInstr> instrs) {
        return temp.temp;
    }

    public MemTemp visit(ImcUNOP unOp, Vector<AsmInstr> instrs) {
        // Create instructions for evaluating operand expression
        MemTemp operand = unOp.subExpr.accept(this, instrs);

        // If operand is ImcCONST, use the same register for result
        Vector<MemTemp> defs = new Vector<>();
        if (unOp.subExpr instanceof ImcCONST) {
            defs.add(operand);
        } else {
            MemTemp dst = new MemTemp();
            defs.add(dst);
        }

        Vector<MemTemp> uses = new Vector<>(
            Arrays.asList(new MemTemp[]{ operand }));

        switch (unOp.oper) {
            case NEG -> {
                instrs.add(new AsmOPER("\t\tNEG\t`d0,`s0", uses, defs, null));
            }
            case NOT -> {
                // First negate number to prevent overflow
                instrs.add(new AsmOPER("\t\tNEG\t`d0,`s0", uses, defs, null));
                instrs.add(new AsmOPER("\t\tSUB\t`d0,`s0,1", defs, defs, null));
            }
        };

        return defs.get(0);
    }

}
