package eu.unitn.disi.db.excode.graph;

import com.google.common.collect.Lists;

/**
 *
 * @author bluecopper
 */
public class BinaryDynamicGraph extends DynamicGraph {
    
    public BinaryDynamicGraph(int numNodes, int numEdges) {
        super(numNodes, numEdges);
        this.edges = Lists.newArrayList();
    }
    
    @Override
    public void addEdge(int id, int src, int dst, int label, double[] series) {
        edges.add(new BinaryDynamicEdge(id, src, dst, label, series));
        nodes[src].addReachableNode(dst);
        nodes[dst].addReachableNode(src);
        nodeEdges[src][dst] = id;
        nodeEdges[dst][src] = id;
    }
    
}
