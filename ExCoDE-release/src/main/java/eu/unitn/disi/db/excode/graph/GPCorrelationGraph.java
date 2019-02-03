package eu.unitn.disi.db.excode.graph;

import com.google.common.collect.Lists;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import eu.unitn.disi.db.excode.utils.Pair;
import eu.unitn.disi.db.excode.utils.Utilities;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.stream.IntStream;

/**
 *
 * @author bluecopper
 */
public class GPCorrelationGraph {
    
    HashIntSet nodes;
    HashIntSet[] adj;
    
    public GPCorrelationGraph(int numNodes, Collection<Pair<Integer, Integer>> correlatedEdges) {
        nodes = HashIntSets.newMutableSet();
        adj = new HashIntSet[numNodes];
        IntStream.range(0, numNodes).forEach(i -> adj[i] = HashIntSets.newMutableSet());
        correlatedEdges.stream().forEach(e -> {
            nodes.add(e.getA());
            nodes.add(e.getB());
            adj[e.getA()].add(e.getB());
            adj[e.getB()].add(e.getA());
        });
    }
    
    public HashIntSet getNeighbors(int index) {
        return adj[index];
    }
    
    public HashIntSet getNodes() {
        return nodes;
    }
    
    public int getDegreeInSet(int n, HashIntSet s) {
        return Utilities.intersectionSize(adj[n], s);
    }
    
    public double getLargeness(int n, HashIntSet s) {
        return ((double) getDegreeInSet(n, s)) / s.size();
    }
    
    public int getLargestNodeInSet(HashIntSet s) {
        int node = -1;
        double largeness = -1;
        for (int n : s) {
            double currLarg = getLargeness(n, s);
            if (currLarg > largeness) {
                largeness = currLarg;
                node = n;
            }
        }
        return node;
    }
    
    public int getDegree(int n) {
        return adj[n].size();
    }
    
    public HashIntSet getNeighborsInSet(int n, HashIntSet s) {
        if (s.isEmpty() || adj[n].isEmpty()) {
            return HashIntSets.newMutableSet();
        }
        HashIntSet neighbors = HashIntSets.newMutableSet(adj[n]);
        neighbors.retainAll(s);
        return neighbors;
    }
    
    public HashIntSet getNotNeighborsInSet(int n, HashIntSet s) {
        if (s.isEmpty()) {
            return HashIntSets.newMutableSet(adj[n]);
        }
        if (adj[n].isEmpty()) {
            return HashIntSets.newMutableSet();
        }
        HashIntSet neighbors = HashIntSets.newMutableSet(s);
        neighbors.removeAll(adj[n]);
        return neighbors;
    }
    
    public List<HashIntSet> findCCs() {
        boolean[] visited = new boolean[adj.length];
        List<HashIntSet> ccs = Lists.newArrayList();
        nodes.stream().forEach(n -> {
            if (!visited[n]) {
                ccs.add(bfs(n, visited));
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
            cc.add(v);
            adj[v].stream().forEach(neigh -> {
                if (!visited[neigh]) {
                    queue.add(neigh);
                    visited[neigh] = true;
                }
            });
        }
        return cc;
    }
    
    public boolean checkResult(HashIntSet clique) {
        return clique.parallelStream().noneMatch((node) -> (getDegreeInSet(node, clique) != clique.size() - 1));
    }
    
}
