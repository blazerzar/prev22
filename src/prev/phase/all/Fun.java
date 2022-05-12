package prev.phase.all;

import java.util.List;

import prev.data.asm.Code;

/**
 * Function with included prolog and epilogue.
 */
public class Fun {
    public Code body;
    public List<String> prologue;
    public List<String> epilogue;

    public Fun(Code body, List<String> prologue, List<String> epilogue) {
        this.body = body;
        this.prologue = prologue;
        this.epilogue = epilogue;
    }
}