package prev.phase.livean;

import prev.data.mem.*;
import prev.data.asm.*;
import prev.phase.*;
import prev.phase.asmgen.*;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;

/**
 * Liveness analysis.
 */
public class LiveAn extends Phase {

	public LiveAn() {
		super("livean");
	}

	public void compLifetimes() {
		// Do liveness analysis for each function
		for (Code code : AsmGen.codes) {
			Vector<AsmInstr> instrs = code.instrs;

			// Previous in and out sizes
			int[] sizesIn = new int[instrs.size()];
			int[] sizesOut = new int[instrs.size()];

			Map<String, AsmInstr> asmLabels = new HashMap<>();

			// Add use to in of every instruction
			for (int i = 0; i < instrs.size(); ++i) {
				AsmInstr instr = instrs.get(i);
				instr.addInTemps(new HashSet<>(instr.uses()));
				sizesIn[i] = instr.in().size();

				// Save instruction if it is a label
				if (instr instanceof AsmLABEL) {
					asmLabels.put(instr.toString(), instr);
				}
			}

			boolean converged = false;

			do {
				converged = true;

				// Update ins and outs
				for (int i = 0; i < instrs.size(); ++i) {
					AsmInstr instr = instrs.get(i);

					// Add out \ def
					HashSet<MemTemp> outWithoutDef = new HashSet<>(instr.out());
					outWithoutDef.removeAll(instr.defs());
					instr.addInTemps(outWithoutDef);

					if (i + 1 < instrs.size()) {
						// If not the last instr, add in of the next instr
						instr.addOutTemp(instrs.get(i + 1).in());

						// Add in of every jump successor (not for function calls)
						if (!instr.toString().contains("PUSHJ")) {
							for (MemLabel label : instr.jumps()) {
								instr.addOutTemp(
									asmLabels.get(label.name).in()
								);
							}
						}
					}

					converged &= instr.in().size() == sizesIn[i] &&
								 instr.out().size() == sizesOut[i];
				}

				if (!converged) {
					// Save new sizes
					for (int i = 0; i < instrs.size(); ++i) {
						sizesIn[i] = instrs.get(i).in().size();
						sizesOut[i] = instrs.get(i).out().size();
					}
				}
			} while (!converged);
		}
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
				logger.addAttribute("code", instr.toString());
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
