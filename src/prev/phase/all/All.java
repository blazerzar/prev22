package prev.phase.all;

import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.io.PrintWriter;
import java.io.IOException;

import prev.data.asm.*;
import prev.data.lin.LinDataChunk;
import prev.phase.asmgen.AsmGen;
import prev.phase.regall.RegAll;
import prev.phase.imclin.ImcLin;
import prev.Compiler;
import prev.common.report.Report;

/**
 * Putting it all together.
 */
public class All {

	public static List<Fun> funs = new LinkedList<>();

    private long dataSize = 0L;


    /* Create assembly file with the compiled program */
    public void createFile(String filename) {
        // First create static data to get HP location
        List<String> staticData = createStaticData();

        // Create start code, standard library
        List<String> startCode = createStartCode();
        List<String> standardLibrary = createStandardLibrary();

        // Write output file
        try (PrintWriter writer = new PrintWriter(filename, "UTF-8")) {
            // Static data
            for (String instr : staticData) {
                writer.println(instr);
            }
            writer.println();

            writer.println("\t\tLOC #100\n");

            // Start code
            for (String instr : startCode) {
                writer.println(instr);
            }
            writer.println();

            // Functions
            for (Fun fun : funs) {
                for (String instr : fun.prologue) {
                    writer.println(instr);
                }

                for (int i = 0; i < fun.body.instrs.size(); ++i) {
                    AsmInstr instr = fun.body.instrs.get(i);
                    if (instr instanceof AsmLABEL) {
                        if (i < fun.body.instrs.size() + 1 &&
                                fun.body.instrs.get(i + 1) instanceof AsmLABEL) {
                            writer.print(instr.toString(RegAll.tempToReg));
                            writer.println("\t\tSWYM");
                        } else {
                            writer.print(instr.toString(RegAll.tempToReg));
                        }
                    } else {
                        writer.println(instr.toString(RegAll.tempToReg));
                    }
                }

                for (String instr : fun.epilogue) {
                    writer.println(instr);
                }

                writer.println();
            }

            // Stdlib
            for (String instr : standardLibrary) {
                writer.println(instr);
            }

        } catch (IOException e) {
            throw new Report.Error("Cannot write to file.");
        }
   }

    private List<String> createStartCode() {
        List<String> instrs = new LinkedList<>();

        // Set global registers
        instrs.add("Main\t\tSET\t$0,252");
        instrs.add("\t\tPUT\trG,$0");
        instrs.add("\t\tGREG\t0");
        instrs.add("\t\tGREG\t0");
        instrs.add("\t\tGREG\t0");
        instrs.add("\t\tGREG\t0");

        // Create stack and heap
        loadConst(0x7FFFFFFFFFFFFFF8L, instrs, "$0");
        instrs.add("\t\tADD\t$254,$0,0");
        loadConst(0x2000000000000000L + dataSize, instrs, "$0");
        instrs.add("\t\tADD\t$252,$0,0");

        // Call main
        int K = Integer.decode(Compiler.cmdLineArgValue("--nregs"));
        instrs.add(String.format("\t\tPUSHJ\t$%d,_main", K));

        instrs.add("\t\tTRAP\t0,Halt,0");

        return instrs;
    }

    private List<String> createStandardLibrary() {
        List<String> instrs = new LinkedList<>();

        // getChar
        instrs.add("_getChar\tLDA\t$255,Arg");
        instrs.add("\t\tTRAP\t0,Fgets,StdIn");
        instrs.add("\t\tLDB\t$0,$255,0");
        instrs.add("\t\tSTB\t$0,$254,8");
        instrs.add("\t\tPOP\t0,0");

        // putChar
        instrs.add("_putChar\tLDO\t$0,$254,8");
        instrs.add("\t\tLDA\t$255,OutBuf");
        instrs.add("\t\tSTB\t$0,$255,0");
        instrs.add("\t\tTRAP\t0,Fputs,StdOut");
        instrs.add("\t\tPOP\t0,0");

        // new
        instrs.add("_new\t\tSTO\t$252,$254,0");
        instrs.add("\t\tLDO\t$0,$254,8");
        instrs.add("\t\tADD\t$252,$252,$0");
        instrs.add("\t\tPOP\t0,0");

        // del
        instrs.add("_del\t\tPOP\t0,0");

        // exit
        instrs.add("_exit\t\tTRAP\t0,Halt,0");

        return instrs;
    }

    private List<String> createStaticData() {
        List<String> staticData = new LinkedList<>();

        staticData.add("\t\tLOC\tData_Segment\n");

        // Save memory for reading and printing a character
        staticData.add("OutBuf\t\tBYTE\t0,0");
        staticData.add("InBuf\t\tBYTE\t0");
        staticData.add("Arg\t\tOCTA\tInBuf,1");
        dataSize += 24;

        for (LinDataChunk data : ImcLin.dataChunks()) {
            if (data.init != null) {
                // String
                String fixedString = Arrays.toString(data.init.getBytes())
                                        .replaceAll("[\\[\\] ]", "");
                staticData.add(String.format("%s\t\tOCTA\t%s,0",
                                            data.label.name, fixedString));
            } else if (data.size > 8) {
                // Non primitive
                staticData.add(data.label.name + "\t\tOCTA");
                dataSize += data.size;
                staticData.add(String.format("\t\tLOC\t%s+%d",
                                            data.label.name, data.size));
            } else {
                // Primitive
                staticData.add(data.label.name + "\t\tOCTA");
                dataSize += data.size;
            }
        }

        return staticData;
    }

    /* Add prologue and epilogue to every compiled function */
    public void finishFuns() {
        for (Code fun : AsmGen.codes) {
            funs.add(new Fun(fun, createPrologue(fun), createEpilogue(fun)));
        }
    }

    private List<String> createPrologue(Code fun) {
        List<String> prologue = new LinkedList<>();

        // Function label
        prologue.add(fun.frame.label.name + "\t\tSWYM");

        // Save FP and return address
        loadConst(16 + fun.frame.locsSize, prologue, "$0");
        prologue.add("\t\tNEG\t$0,$0");
        prologue.add("\t\tSTO\t$253,$254,$0");
        prologue.add("\t\tADD\t$0,$0,8");
        prologue.add("\t\tGET\t$1,rJ");
        prologue.add("\t\tSTO\t$1,$254,$0");

        // Move FP
        prologue.add("\t\tADD\t$253,$254,0");

        // Move SP
        loadConst(fun.frame.size + fun.tempSize, prologue, "$0");
        prologue.add("\t\tSUB\t$254,$254,$0");

        // Jump to function entry label
        prologue.add("\t\tJMP\t" + fun.entryLabel.name);

        return prologue;
    }

    private List<String> createEpilogue(Code fun) {
        List<String> epilogue = new LinkedList<>();

        // Function exit label
        epilogue.add(fun.exitLabel.name + "\t\tSWYM");

        // Save return value to FP
        int RV = RegAll.tempToReg.get(fun.frame.RV);
        epilogue.add(String.format("\t\tSTO\t$%d,$253,0", RV));

        // Restore SP, FP and return address
        epilogue.add("\t\tADD\t$254,$253,0");
        loadConst(16 + fun.frame.locsSize, epilogue, "$0");
        epilogue.add("\t\tNEG\t$0,$0");
        epilogue.add("\t\tLDO\t$253,$254,$0");
        epilogue.add("\t\tADD\t$0,$0,8");
        epilogue.add("\t\tLDO\t$0,$254,$0");
        epilogue.add("\t\tPUT\trJ,$0");

        // Pop out of a function call
        epilogue.add("\t\tPOP\t0,0");

        return epilogue;
    }

    private void loadConst(long value, List<String> instrs, String register) {
        // Save long into a register 2 bytes at a time
        int[] offsets = { 0, 16, 32, 48 };
        String[] instructions = { "SETL", "INCML", "INCMH", "INCH" };

        for (int i = 0; i < offsets.length; ++i) {
            int val = (int) (value >> offsets[i] & (1L << 16) - 1);

            // Only save wyde if it is the first one or bigger than 0
            if (offsets[i] == 0 || val > 0) {
                instrs.add(String.format("\t\t%s\t%s,%d", instructions[i], register, val));
            }
        }
    }

}