package eu.unitn.disi.db.excode.graph;

import com.google.common.collect.Lists;
import com.koloboke.collect.IntCursor;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 *
 * @author bluecopper
 * @param <E>
 */
public abstract class Graph<E extends Edge> { 
    
    protected Node[] nodes;
    protected List<E> edges;
    protected int[][] nodeEdges;
   
    public Graph(int numNodes, int numEdges) {
        this.nodes = new Node[numNodes];
        this.nodeEdges = new int[numNodes][numNodes];
        for(int i = 0; i < numNodes; i++) {
            Arrays.fill(nodeEdges[i], -1);
        }
    }

    public abstract void addEdge(int id, int src, int dst, int label);

    public int getEdgeLabel(int src, int dst) {
        int edgeIdx = getEdgeId(src, dst);
        return (edgeIdx < 0) ? null : getEdgeLabel(edgeIdx);
    }
    
    public int getEdgeLabel(int edgeId) {
        return edges.get(edgeId).getEdgeLabel();
    }

    public void addNode(int id, int label) {
        Node n = new Node(id, label);
        if (nodes[id] != null) {
            throw new RuntimeException("Node " + id + " already present.");
        }
        nodes[id] = n;
    }
    
    public int getNodeLabel(int nodeId) {
        return nodes[nodeId].getNodeLabel();
    }

    public int getDegree(int nodeId) {
        return nodes[nodeId].getDegree();
    }

    public int getEdgeId(int src, int dst) {
        return nodeEdges[src][dst];
    }
    
    public E getEdge(int src, int dst) {
        if (nodeEdges[src][dst] != -1) {
            return edges.get(nodeEdges[src][dst]);
        }
        return null;
    }
    
    public E getEdge(int edgeId) {
        return edges.get(edgeId);
    }
    
    public List<E> getEdges() {
        return edges;
    }
    
    public Node getNode(int nodeId) {
        return nodes[nodeId];
    }
    
    public Node[] getNodes() {
        return nodes;
    }

    public int getNumNodes() {
        return nodes.length;
    }
    
    public int getNumEdges() {
        return edges.size();
    }
    
    public int getSrc(int edgeId) {
        return edges.get(edgeId).getSrc();
    }
    
    public int getSrcLabel(int edgeId) {
        return nodes[edges.get(edgeId).getSrc()].getNodeLabel();
    }

    public int getDst(int edgeId) {
        return edges.get(edgeId).getDst();
    }
    
    public int getDstLabel(int edgeId) {
        return nodes[edges.get(edgeId).getDst()].getNodeLabel();
    }
    
    public HashIntSet getReachableNodes(int nodeId) {
        return nodes[nodeId].getReachableNodes();
    }
    
    public E getEdge(Node v, Node v1) {
        int index = nodeEdges[v.getNodeId()][v1.getNodeId()];
        if (index != -1) {
            return edges.get(index);
        }
        return null;
    }
    
    public List<E> getEdgeList(HashIntSet edgeIds) {
        return edgeIds.stream().map(id -> edges.get(id)).collect(Collectors.toList());
    }
        
    public HashIntSet getNodesOf(HashIntSet comp) {
        HashIntSet s = HashIntSets.newMutableSet(comp.size() * 2);
        comp.stream().forEach(e -> {s.add(edges.get(e).getSrc()); s.add(edges.get(e).getDst());});
        return s;
    }
    
    public List<HashIntSet> findCCsER(HashIntSet allowedEdges) {
        HashIntSet visited = HashIntSets.newMutableSet(allowedEdges.size() * 2);
        HashIntSet inCC = HashIntSets.newMutableSet(allowedEdges.size());
        List<HashIntSet> ccs = Lists.newArrayList();
        allowedEdges.stream().forEach(edge -> {
            if (!inCC.contains(edge)) {
                ccs.add(bfs(getSrc(edge), visited, inCC, allowedEdges));
            }
        });
        return ccs;
    }
    
    private HashIntSet bfs(int n, HashIntSet visited, HashIntSet inCC, HashIntSet allowedEdges) {
        Queue<Integer> queue = Lists.newLinkedList();
        HashIntSet cc = HashIntSets.newMutableSet();
        
        queue.add(n);
        visited.add(n);
        while(!queue.isEmpty()) {
            int v = queue.poll();
            getReachableNodes(v).stream().forEach(d -> {
                int edgeId = getEdgeId(v, d);
                if (allowedEdges.contains(edgeId)) {
                    if (inCC.add(edgeId)) {
                        cc.add(edgeId);
                    }
                    if (!visited.contains(d)) {
                        queue.add(d);
                        visited.add(d);
                    }
                }
            });
        }
        return cc;
    }

    public List<HashIntSet> findCCs() {
        boolean[] visited = new boolean[getNumNodes()];
        HashIntSet inCC = HashIntSets.newMutableSet(getNumEdges());
        List<HashIntSet> ccs = Lists.newArrayList();
        getEdges().stream().forEach(edge -> {
            if (!inCC.contains(edge.getEdgeID())) {
                ccs.add(bfs(edge.getSrc(), visited));
            }
        });
        return ccs;
    }
    
    private HashIntSet bfs(int n, boolean[] visited) {
        Queue<Integer> queue = Lists.newLinkedList();
        HashIntSet cc = HashIntSets.newMutableSet();
        
        queue.add(n);
        visited[n] = true;
        while(!queue.isEmpty()) {
            int v = queue.poll();
            getReachableNodes(v).stream().forEach(d -> {
                int edgeId = getEdgeId(v, d);
                    cc.add(edgeId);
                    if (!visited[d]) {
                        queue.add(d);
                        visited[d] = true;
                    }
            });
        }
        return cc;
    }

    public boolean isConnected(HashIntSet subgraph) {
        HashIntSet visitedNodes = HashIntSets.newMutableSet(getNumNodes());
        HashIntSet visitedEdges = HashIntSets.newMutableSet(getNumEdges());
        IntCursor cur = subgraph.cursor();
        
        while(cur.moveNext()) {
            int edge = cur.elem();
            if (!visitedEdges.contains(edge)) {
                visitedEdges.addAll(bfsER(getSrc(edge), visitedNodes, subgraph));
            }
        }
        return (subgraph.size() == visitedEdges.size());
    }
        
    private HashIntSet bfsER(int s, HashIntSet visitedNodes, HashIntSet subgraph) {
        Queue<Integer> queue = Lists.newLinkedList();
        HashIntSet cc = HashIntSets.newMutableSet();
        queue.add(s);
        visitedNodes.add(s);
        while(!queue.isEmpty()) {
            int v = queue.poll();
            getReachableNodes(v).stream().forEach(d -> {
                int edgeId = getEdgeId(v, d);
                if (subgraph.contains(edgeId)) {
                    cc.add(edgeId);
                    if (visitedNodes.add(d)) {
                        queue.add(d);
                    }
                }
            });
            
        }
        return cc;    
    }

}
