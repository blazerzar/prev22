package prev.phase.imcgen;

import java.util.*;

import prev.data.ast.tree.*;
import prev.data.ast.tree.decl.*;
import prev.data.ast.tree.expr.*;
import prev.data.ast.tree.stmt.*;
import prev.data.ast.visitor.*;
import prev.data.mem.*;
import prev.data.imc.code.expr.*;
import prev.data.imc.code.stmt.*;
import prev.data.typ.*;
import prev.phase.memory.*;
import prev.phase.seman.SemAn;

public class CodeGenerator extends AstNullVisitor<Object, Stack<MemFrame>> {

	// GENERAL PURPOSE

	@Override
	public Object visit(AstTrees<? extends AstTree> trees, Stack<MemFrame> frames) {
		// If there is no stack, we need to initialize it
        if (frames == null) {
            frames = new Stack<>();
        }

        // Only visit function declarations
        for (AstTree tree : trees) {
            if (tree instanceof AstFunDecl) {
                tree.accept(this, frames);
            }
        }

        return null;
	}

    // FUNCTION DECLARATIONS

	@Override
	public Object visit(AstFunDecl funDecl, Stack<MemFrame> frames) {
        // Add function's frame onto the stack
        frames.push(Memory.frames.get(funDecl));

        // Generate intermediate code for function body
        if (funDecl.expr != null) {
            funDecl.expr.accept(this, frames);
        }

        // Remove function's frame from the stack at the end
        frames.pop();

		return null;
	}

    // EXPRESSIONS

    // EX1, EX2, EX3, A1
	@Override
	public Object visit(AstAtomExpr atomExpr, Stack<MemFrame> frames) {
		ImcExpr imc = switch (atomExpr.type) {
            case VOID    -> new ImcCONST(0);
            case POINTER -> new ImcCONST(0);
            case BOOL    -> new ImcCONST(atomExpr.value.equals("true") ? 1 : 0);
            case CHAR    -> {
                // Get character code
                long charVal = (long) atomExpr.value.charAt(atomExpr.value.length() - 2);
                yield new ImcCONST(charVal);
            }
            case INT     -> new ImcCONST(Long.parseLong(atomExpr.value));
            case STRING  -> {
                // Get label of the string ...
                MemLabel label = Memory.strings.get(atomExpr).label;
                // ... and map it to an andress
                yield new ImcNAME(label);
            }
        };

        ImcGen.exprImc.put(atomExpr, imc);
        return imc;
	}

    // EX4, EX6.1, EX7, EX8
	@Override
	public Object visit(AstPfxExpr pfxExpr, Stack<MemFrame> frames) {
        ImcExpr expr = (ImcExpr) pfxExpr.expr.accept(this, frames);
        ImcExpr imc = switch (pfxExpr.oper) {
            case ADD -> expr;
            case SUB -> new ImcUNOP(ImcUNOP.Oper.NEG, expr);
            case NOT -> new ImcUNOP(ImcUNOP.Oper.NOT, expr);
            case PTR -> ((ImcMEM) expr).addr;
            case NEW -> {
                Vector<Long> offsets = new Vector<>(Arrays.asList(new Long[]{ 0L }));
                Vector<ImcExpr> args = new Vector<>(Arrays.asList(new ImcExpr[]{ expr }));
                yield new ImcCALL(new MemLabel("new"), offsets, args);
            }
            case DEL -> {
                Vector<Long> offsets = new Vector<>(Arrays.asList(new Long[]{ 0L }));
                Vector<ImcExpr> args = new Vector<>(Arrays.asList(new ImcExpr[]{ expr }));
                yield new ImcCALL(new MemLabel("del"), offsets, args);
            }
        };
        ImcGen.exprImc.put(pfxExpr, imc);
		return imc;
	}

    // EX5
	@Override
	public Object visit(AstBinExpr binExpr, Stack<MemFrame> frames) {
        // Generate code for operands
        ImcExpr lhs = (ImcExpr) binExpr.fstExpr.accept(this, frames);
        ImcExpr rhs = (ImcExpr) binExpr.sndExpr.accept(this, frames);

        // Get operation
        ImcBINOP.Oper oper = ImcBINOP.Oper.values()[binExpr.oper.ordinal()];

        ImcBINOP imc = new ImcBINOP(oper, lhs, rhs);
        ImcGen.exprImc.put(binExpr, imc);
		return imc;
	}

    // EX6.2
	@Override
	public Object visit(AstSfxExpr sfxExpr, Stack<MemFrame> frames) {
        ImcExpr expr = (ImcExpr) sfxExpr.expr.accept(this, frames);
        ImcMEM imc = new ImcMEM(expr);
        ImcGen.exprImc.put(sfxExpr, imc);
		return imc;
	}

    // EX9
	@Override
	public Object visit(AstNameExpr nameExpr, Stack<MemFrame> frames) {
        AstDecl decl = SemAn.declaredAt.get(nameExpr);

        // Parameterless function
        if (decl instanceof AstFunDecl) {
            MemFrame frame = Memory.frames.get((AstFunDecl) decl);

            // Get SL
            ImcExpr SL = new ImcTEMP(frames.peek().FP);
            for (int i = 0; i < frames.peek().depth - frame.depth; ++i) {
                SL = new ImcMEM(SL);
            }

            // Create offsets and arguments list for SL
            Vector<Long> offsets = new Vector<>(Arrays.asList(new Long[]{ 0L }));
            Vector<ImcExpr> args = new Vector<>(Arrays.asList(new ImcExpr[]{ SL }));
            ImcCALL imc = new ImcCALL(frame.label, offsets, args);
            ImcGen.exprImc.put(nameExpr, imc);
            return imc;
        }

        if (!(decl instanceof AstVarDecl || decl instanceof AstParDecl)) {
            return null;
        }

        // Generate access for variables and parameters
        AstMemDecl memDecl = (AstMemDecl) decl;
        MemAccess access = Memory.accesses.get(memDecl);

        ImcMEM imc = null;
        if (access instanceof MemAbsAccess) {
            // Global variable
            imc = new ImcMEM(new ImcNAME(((MemAbsAccess) access).label));
        } else if (access instanceof MemRelAccess) {
            // Local variable of parameter
            MemRelAccess relAccess = (MemRelAccess) access;
            ImcExpr fpExpr = new ImcTEMP(frames.peek().FP);

            // Find the correct parent function
            for (int i = 0; i < frames.peek().depth - relAccess.depth + 1; ++i) {
                fpExpr = new ImcMEM(fpExpr);
            }

            imc = new ImcMEM(
                new ImcBINOP(
                    ImcBINOP.Oper.ADD,
                    fpExpr,
                    new ImcCONST(relAccess.offset)
                )
            );
        }
        ImcGen.exprImc.put(nameExpr, imc);
		return imc;
	}

    // EX10
	@Override
	public Object visit(AstArrExpr arrExpr, Stack<MemFrame> frames) {
        // Get base address, index and element size
        ImcMEM base = (ImcMEM) arrExpr.arr.accept(this, frames);
        ImcExpr idx = (ImcExpr) arrExpr.idx.accept(this, frames);
        long size = SemAn.ofType.get(arrExpr).size();

        // base + idx * size
        ImcMEM imc = new ImcMEM(
            new ImcBINOP(
                ImcBINOP.Oper.ADD,
                base.addr,
                new ImcBINOP(ImcBINOP.Oper.MUL, idx, new ImcCONST(size))
            )
        );
        ImcGen.exprImc.put(arrExpr, imc);
		return imc;
	}

    // EX11
	@Override
	public Object visit(AstRecExpr recExpr, Stack<MemFrame> frames) {
        // Get memory address of the record
        ImcMEM record = (ImcMEM) recExpr.rec.accept(this, frames);

        // Get declaration of component and its access
        AstCompDecl compDecl = (AstCompDecl) SemAn.declaredAt.get(recExpr.comp);
        MemRelAccess access = (MemRelAccess) Memory.accesses.get(compDecl);

        // record + comp offset
        ImcMEM imc = new ImcMEM(
            new ImcBINOP(
                ImcBINOP.Oper.ADD,
                record.addr,
                new ImcCONST(access.offset)
            )
        );
        ImcGen.exprImc.put(recExpr, imc);
		return imc;
	}

    // EX12
	@Override
	public Object visit(AstCallExpr callExpr, Stack<MemFrame> frames) {
        // Get function declaration for offsets
        AstFunDecl funDecl = (AstFunDecl) SemAn.declaredAt.get(callExpr);

        // Called function
        MemFrame frame = Memory.frames.get(funDecl);

        // Get SL
        ImcExpr SL = new ImcTEMP(frames.peek().FP);
        for (int i = 0; i < frames.peek().depth - frame.depth; ++i) {
            SL = new ImcMEM(SL);
        }

        // Create offsets and arguments list for SL
        Vector<Long> offsets = new Vector<>(Arrays.asList(new Long[]{ 0L }));
        Vector<ImcExpr> args = new Vector<>(Arrays.asList(new ImcExpr[]{ SL }));

        // Generate code for arguments and save offsets
        for (int i = 0; i < callExpr.args.size(); ++i) {
            AstParDecl parDecl = funDecl.pars.get(i);
            MemRelAccess access = (MemRelAccess) Memory.accesses.get(parDecl);
            offsets.add(access.offset);
            args.add((ImcExpr) callExpr.args.get(i).accept(this, frames));
        }

        ImcCALL imc = new ImcCALL(frame.label, offsets, args);
        ImcGen.exprImc.put(callExpr, imc);
		return imc;
	}

    // EX13
	@Override
	public Object visit(AstStmtExpr stmtExpr, Stack<MemFrame> frames) {
        Vector<ImcStmt> stmts = new Vector<>();
        int stmtsNum = stmtExpr.stmts.size();

        // Generate code for statements except the last one
        for (int i = 0; i < stmtsNum - 1; ++i) {
            stmts.add((ImcStmt) stmtExpr.stmts.get(i).accept(this, frames));
        }

        // Generate code for last statement and check if it is an expression
        ImcStmt stmt = (ImcStmt) stmtExpr.stmts.get(stmtsNum - 1).accept(this, frames);

        ImcExpr imc;
        if (stmt instanceof ImcESTMT) {
            // Last statement is an expression
            imc = new ImcSEXPR(new ImcSTMTS(stmts), ((ImcESTMT) stmt).expr);
        } else {
            // Last statement is not an expression
            stmts.add(stmt);
            imc = new ImcSEXPR(new ImcSTMTS(stmts), new ImcCONST(0));
        }

        ImcGen.exprImc.put(stmtExpr, imc);
		return imc;
	}

    // EX15, EX16
	@Override
	public Object visit(AstCastExpr castExpr, Stack<MemFrame> frames) {
        // Generate code for expression
        ImcExpr expr = (ImcExpr) castExpr.expr.accept(this, frames);

        // Check if cast type is bool
        SemType type = SemAn.isType.get(castExpr.type);
        boolean charCast = type.actualType() instanceof SemChar;

        ImcExpr imc;
        if (charCast) {
            imc = new ImcBINOP(
                ImcBINOP.Oper.MOD,
                expr,
                new ImcCONST(256)
            );
        } else {
            imc = expr;
        }
        ImcGen.exprImc.put(castExpr, imc);
		return imc;
	}

    // EX17
	@Override
	public Object visit(AstWhereExpr whereExpr, Stack<MemFrame> frames) {
        // Generate code for declarations
        whereExpr.decls.accept(this, frames);

        // Generate expression
        ImcExpr imc = (ImcExpr) whereExpr.expr.accept(this, frames);
        ImcGen.exprImc.put(whereExpr, imc);
		return imc;
	}

    // STATEMENTS

    // ST1
	@Override
	public Object visit(AstExprStmt exprStmt, Stack<MemFrame> frames) {
        ImcExpr expr = (ImcExpr) exprStmt.expr.accept(this, frames);
        ImcESTMT imc = new ImcESTMT(expr);
        ImcGen.stmtImc.put(exprStmt, imc);
		return imc;
	}

    // ST2
	@Override
	public Object visit(AstAssignStmt assignStmt, Stack<MemFrame> frames) {
        // Generate destination address and right hand side value
        ImcExpr dst = (ImcExpr) assignStmt.dst.accept(this, frames);
        ImcExpr src = (ImcExpr) assignStmt.src.accept(this, frames);

        // Save [src] value into [dst] address
        ImcMOVE imc = new ImcMOVE(dst, src);

        ImcGen.stmtImc.put(assignStmt, imc);
		return imc;
	}

    // ST3, ST4
	@Override
	public Object visit(AstIfStmt ifStmt, Stack<MemFrame> frames) {
        // Generate condition expression and both statements
        ImcExpr cond = (ImcExpr) ifStmt.cond.accept(this, frames);
        ImcStmt thenStmt = (ImcStmt) ifStmt.thenStmt.accept(this, frames);
        ImcStmt elseStmt = (ImcStmt) ifStmt.elseStmt.accept(this, frames);

        MemLabel positiveLabel = new MemLabel();
        MemLabel negativeLabel = new MemLabel();
        MemLabel endLabel = new MemLabel();

        Vector<ImcStmt> stmts = new Vector<>(Arrays.asList(new ImcStmt[]{
            new ImcCJUMP(cond, positiveLabel, negativeLabel),
            // If [cond] is nonzero, jump to then stmt
            new ImcLABEL(positiveLabel), thenStmt,
            // Aften then stmt jump to the end
            new ImcJUMP(endLabel),
            // If [cond] is zero, jump to else stmt
            new ImcLABEL(negativeLabel), elseStmt,
            new ImcLABEL(endLabel)
        }));
        ImcSTMTS imc = new ImcSTMTS(stmts);
        ImcGen.stmtImc.put(ifStmt, imc);
		return imc;
	}

    // ST5, ST6
	@Override
	public Object visit(AstWhileStmt whileStmt, Stack<MemFrame> frames) {
        // Generate condition and body statement
        ImcExpr cond = (ImcExpr) whileStmt.cond.accept(this, frames);
        ImcStmt body = (ImcStmt) whileStmt.bodyStmt.accept(this, frames);

        MemLabel condCheckLabel = new MemLabel();
        MemLabel bodyLabel = new MemLabel();
        MemLabel endLabel = new MemLabel();

        Vector<ImcStmt> stmts = new Vector<>(Arrays.asList(new ImcStmt[]{
            new ImcLABEL(condCheckLabel),
            new ImcCJUMP(cond, bodyLabel, endLabel),
            // If [cond] is nonzero, jump to body stmt
            new ImcLABEL(bodyLabel), body,
            // After body stmt jump to condition check
            new ImcJUMP(condCheckLabel),
            // If [cond] is zero, jump to the end
            new ImcLABEL(endLabel)
        }));
        ImcSTMTS imc = new ImcSTMTS(stmts);
        ImcGen.stmtImc.put(whileStmt, imc);
		return imc;
	}

}
