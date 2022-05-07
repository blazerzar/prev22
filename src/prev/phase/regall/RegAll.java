package prev.phase.regall;

import java.util.*;

import prev.data.mem.*;
import prev.data.asm.*;
import prev.data.imc.code.expr.ImcCONST;
import prev.phase.*;
import prev.phase.asmgen.*;
import prev.phase.regall.Graph;
import prev.phase.livean.LiveAn;

/**
 * Register allocation.
 */
public class RegAll extends Phase {

	/** Mapping of temporary variables to registers. */
	public final HashMap<MemTemp, Integer> tempToReg = new HashMap<MemTemp, Integer>();

	private final int nregs;

	public RegAll(int nregs) {
		super("regall");
		this.nregs = nregs;
	}

	public void allocate() {
		for (Code fun : AsmGen.codes) {
			tempToReg.put(fun.frame.FP, 253);

			while (!allocateFun(fun));
		}
	}

	private boolean allocateFun(Code fun) {
		Graph g = new Graph(fun);

		boolean emptyGraph = g.simplifyWorklist.isEmpty() && g.spillWorklist.isEmpty();
		while (!emptyGraph) {
			if (!g.simplifyWorklist.isEmpty()) {
				g.simplify();
			} else if (!g.spillWorklist.isEmpty()) {
				g.spill();
			} else {
				emptyGraph = true;
			}
		}

		// Try to color the graph
		g.assignColors();

		// Change asm instructions if any temp has spilled
		if (!g.spilled.isEmpty()) {
			for (MemTemp t : g.spilled) {
				spillTemp(fun, t);
			}

			// Redo liveness analysis for this function
			LiveAn livean = new LiveAn();
			livean.compLifetimesFun(fun);
			return false;
		}

		// Save register mapping
		for (Node n : g.nodes.values()) {
			tempToReg.put(n.temp, n.color);
		}

		return true;
	}

	private void spillTemp(Code fun, MemTemp temp) {
		// Allocate space for temp in the frame
		fun.tempSize += 8;
		MemTemp FP = fun.frame.FP;
		ImcCONST offset = new ImcCONST(-fun.frame.locsSize - 16 - fun.tempSize);

		for (int i = 0; i < fun.instrs.size(); ++i) {
			AsmInstr instr = fun.instrs.get(i);
			boolean inUse = instr.uses().contains(temp);
			boolean inDef = instr.defs().contains(temp);

			if (inUse || inDef) {
				Vector<AsmInstr> instrs = new Vector<>();
				MemTemp newTemp = new MemTemp();
				MemTemp offsetTemp = offset.accept(new ExprGenerator(), instrs);
				fun.instrs.set(i, replaceTemp(instr, temp, newTemp));

				if (inUse) {
					// Insert a fetch before use
					instrs.add(new AsmOPER(
						"LDO `d0,`s0,`s1",
						new Vector<>(Arrays.asList(new MemTemp[]{ FP, offsetTemp })),
						new Vector<>(Arrays.asList(new MemTemp[]{ newTemp })),
						null
					));
					fun.instrs.addAll(i, instrs);
				} else if (inDef) {
					// Insert a store after definition
					instrs.add(new AsmOPER(
						"STO `s0,`s1,`s2",
						new Vector<>(Arrays.asList(new MemTemp[]{
							newTemp, FP, offsetTemp
						})),
						null,
						null
					));
					fun.instrs.addAll(i + 1, instrs);
				}

				i += instrs.size();
			}
		}
	}

	/* Return new instruction with replaced temp */
	private AsmOPER replaceTemp(AsmInstr asmInstr, MemTemp old, MemTemp newTemp) {
		AsmOPER instr = (AsmOPER) asmInstr;
		Vector<MemTemp> uses = instr.uses();
		Vector<MemTemp> defs = instr.defs();

		for (int i = 0; i < uses.size(); ++i) {
			if (uses.get(i) == old) {
				uses.set(i, newTemp);
			}
		}

		for (int i = 0; i < defs.size(); ++i) {
			if (defs.get(i) == old) {
				defs.set(i, newTemp);
			}
		}

		return new AsmOPER(instr.instr(), uses, defs, instr.jumps());
	}

	public void log() {
		if (logger == null)
			return;
		for (Code code : AsmGen.codes) {
			logger.begElement("code");
			logger.addAttribute("entrylabel", code.entryLabel.name);
			logger.addAttribute("exitlabel", code.exitLabel.name);
			logger.addAttribute("tempsize", Long.toString(code.tempSize));
			code.frame.log(logger);
			logger.begElement("instructions");
			for (AsmInstr instr : code.instrs) {
				logger.begElement("instruction");
				logger.addAttribute("code", instr.toString(tempToReg));
				logger.begElement("temps");
				logger.addAttribute("name", "use");
				for (MemTemp temp : instr.uses()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.begElement("temps");
				logger.addAttribute("name", "def");
				for (MemTemp temp : instr.defs()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.begElement("temps");
				logger.addAttribute("name", "in");
				for (MemTemp temp : instr.in()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.begElement("temps");
				logger.addAttribute("name", "out");
				for (MemTemp temp : instr.out()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.endElement();
			}
			logger.endElement();
			logger.endElement();
		}
	}

}
