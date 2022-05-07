package prev.phase.regall;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;

import prev.phase.regall.Node;
import prev.phase.asmgen.AsmGen;
import prev.phase.livean.LiveAn;
import prev.data.asm.Code;
import prev.data.mem.MemTemp;
import prev.data.asm.AsmInstr;
import prev.Compiler;

public class Graph {
    // Not yet processed temps
    private List<Node> initial;

    // Low-degree nodes
    public LinkedList<Node> simplifyWorklist;

    // High-degree nodes
    public LinkedList<Node> spillWorklist;

    // Stack for removed temps
    private LinkedList<Node> selectStack;

    // Spilled temps
    public List<MemTemp> spilled;

    public Map<MemTemp, Node> nodes;

    /* Crate a graph for function with instructions [fun] */
    public Graph(Code fun) {
        initial = new LinkedList<>();
        simplifyWorklist = new LinkedList<>();
        spillWorklist = new LinkedList<>();
        selectStack = new LinkedList<>();
        spilled = new LinkedList<>();
        nodes = new HashMap<>();

        build(fun);
        makeWorklists();
    }

    /* Build the initial graph */
    private void build(Code fun) {
        // Add all nodes (except FP)
        MemTemp FP = fun.frame.FP;
        for (AsmInstr instr : fun.instrs) {
            for (MemTemp t : instr.uses()) {
                if (t != FP && !nodes.containsKey(t)) {
                    Node n = new Node(t);
                    initial.add(n);
                    nodes.put(t, n);
                }
            }

            for (MemTemp t : instr.defs()) {
                if (t != FP && !nodes.containsKey(t)) {
                    Node n = new Node(t);
                    initial.add(n);
                    nodes.put(t, n);
                }
            }
        }

        // Add edges
        for (AsmInstr instr : fun.instrs) {
            for (MemTemp u : instr.out()) {
                for (MemTemp v : instr.out()) {
                    if (u != FP && v != FP && u != v) {
                        addEdge(u, v);
                    }
                }
            }
        }
    }

    /* Arrange initial nodes into simplify and spill worklists */
    private void makeWorklists() {
        int K = Integer.decode(Compiler.cmdLineArgValue("--nregs"));

        for (Node n : initial) {
            if (n.degree >= K) {
                spillWorklist.add(n);
            } else {
                simplifyWorklist.add(n);
            }
        }

        initial.clear();
    }

    /* Add edge to the initial graph */
    private void addEdge(MemTemp u, MemTemp v) {
        Node n = nodes.get(u);
        Node m = nodes.get(v);
        n.addEdge(m);
        m.addEdge(n);
    }

    public void simplify() {
        // Select node from simplify worklist
        Node n = simplifyWorklist.remove();
        removeNode(n);
    }

    public void spill() {
        // Select node from spill worklist
        Node n = spillWorklist.remove();
        removeNode(n);
    }

    /* Place node into stack and remove its edges */
    private void removeNode(Node n) {
        n.nodeSet = Node.NodeSet.SELECT;
        selectStack.push(n);

        // Remove edges
        for (Node m : n.adjacent) {
            if (m.removeEdge(n)) {
                spillWorklist.remove(m);
                simplifyWorklist.add(m);
            }
        }
    }

    /* Try to assign colors and save actual spills */
    public void assignColors() {
        int K = Integer.decode(Compiler.cmdLineArgValue("--nregs"));

        while (!selectStack.isEmpty()) {
            Node n = selectStack.pop();

            // Get colors of all colored neighbours
            Set<Integer> colors = new HashSet<>();
            for (Node m : n.neighbours) {
                if (m.color != -1) {
                    colors.add(m.color);
                }
            }

            // Find first available register
            for (int i = 0; i < K && n.color == -1; ++i) {
                if (!colors.contains(i)) {
                    n.color = i;
                }
            }

            // If color was not assigned, spill
            if (n.color == -1) {
                spilled.add(n.temp);
            }
        }
    }

}