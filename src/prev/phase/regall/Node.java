package prev.phase.regall;

import java.util.Set;
import java.util.HashSet;

import prev.data.mem.MemTemp;
import prev.Compiler;

public class Node {

    public enum NodeSet {
        INITIAL, SIMPLIFY, SPILL, COLORED, SELECT
    }

    // Temp that is represented by this node
    public MemTemp temp;

    // Value telling which set this temp is in
    public NodeSet nodeSet;

    // Current degree of the node
    public int degree;

    // Original edges and the ones not yet removed
    public Set<Node> neighbours;
    public Set<Node> adjacent;

    // Color chosen by the algorithm
    public int color;

    public Node(MemTemp temp) {
        this.temp = temp;
        this.nodeSet = NodeSet.INITIAL;
        this.degree = 0;
        this.neighbours = new HashSet<>();
        this.adjacent = new HashSet<>();
        this.color = -1;
    }

    /* Add new interference edge between two nodes */
    public void addEdge(Node node) {
        if (neighbours.add(node)) {
            adjacent.add(node);
            ++degree;
        }
    }

    /* Remove edge because of graph simplification */
    public boolean removeEdge(Node node) {
        if (adjacent.remove(node)) {
            --degree;

            // Check if node became low-degree
            int K = Integer.decode(Compiler.cmdLineArgValue("--nregs"));
            if (degree == K - 1 && nodeSet == NodeSet.SPILL) {
                nodeSet = NodeSet.SIMPLIFY;

                // Return true to indicate this change
                return true;
            }
        }

        return false;
    }

}
