package eu.unitn.disi.db.excode.maxclique;

import com.google.common.collect.Lists;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import eu.unitn.disi.db.excode.graph.GPCorrelationGraph;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author bluecopper
 */
public class GPCliqueFinder {
    
    GPCorrelationGraph graph;
    
    public GPCliqueFinder(GPCorrelationGraph graph) {
        this.graph = graph;
    }
    
    private List<HashIntSet> enumerateClique(HashIntSet anchor, HashIntSet cand, HashIntSet not, DegreeMap degreeMap) {
        List<HashIntSet> output = Lists.newArrayList();
        if (isAClique(cand, degreeMap)) {
            HashIntSet clique = HashIntSets.newMutableSet(anchor);
            clique.addAll(cand);
            output.add(clique);
            return output;
        }
        while (!isAClique(cand, degreeMap)) {
            int next = degreeMap.removeLowDegNode();
            HashIntSet nextCand = HashIntSets.newMutableSet(cand);
            nextCand.retainAll(graph.getNeighbors(next));
            HashIntSet nextNot = HashIntSets.newMutableSet(not);
            nextNot.retainAll(graph.getNeighbors(next));
            boolean doRecursion = true;
            for (int u : nextNot) {
                if (graph.getDegreeInSet(u, nextCand) == nextCand.size()) {
                    doRecursion = false;
                    break;
                }
            }
            if (doRecursion) {
                HashIntSet nextAnchor = HashIntSets.newMutableSet(anchor);
                nextAnchor.add(next);
                DegreeMap nextDegreeMap = new DegreeMap(graph, nextCand);
                output.addAll(enumerateClique(nextAnchor, nextCand, nextNot, nextDegreeMap));
            }
            cand.remove(next);
            not.add(next);
            HashIntSet neighInCand = graph.getNeighborsInSet(next, cand);
            degreeMap.updateMap(neighInCand);
        }
        if (not.stream().noneMatch(u -> graph.getDegreeInSet(u, cand) == cand.size())) {
            HashIntSet clique = HashIntSets.newMutableSet(anchor);
            clique.addAll(cand);
            output.add(clique);
        }
        return output;
    }
    
    private List<HashIntSet> enumerateCliqueIterative(HashIntSet anchor, HashIntSet cand, HashIntSet not, DegreeMap degreeMap) {
        List<HashIntSet> cliques = Lists.newArrayList();
        LinkedList<CandidateClique> queue = Lists.newLinkedList();
        
        queue.add(new CandidateClique(anchor, cand, not, degreeMap));
        
        while(!queue.isEmpty()) {
            CandidateClique candidate = queue.poll();
            HashIntSet candAnchor = candidate.getAnchor();
            HashIntSet candCand = candidate.getCand();
            HashIntSet candNot = candidate.getNot();
            DegreeMap candMap = candidate.getDegreeMap();
            if (isAClique(candCand, candMap)) {
                if (candNot.stream().noneMatch(u -> graph.getDegreeInSet(u, candCand) == candCand.size())) {
                    HashIntSet clique = HashIntSets.newMutableSet(candAnchor);
                    clique.addAll(candCand);
                    cliques.add(clique);
                }
            } else {
                int next = candMap.removeLowDegNode();
                HashIntSet nextCand = HashIntSets.newMutableSet(candCand);
                nextCand.retainAll(graph.getNeighbors(next));
                HashIntSet nextNot = HashIntSets.newMutableSet(candNot);
                nextNot.retainAll(graph.getNeighbors(next));
                boolean insert = true;
                for (int u : nextNot) {
                    if (graph.getDegreeInSet(u, nextCand) == nextCand.size()) {
                        insert = false;
                        break;
                    }
                }
                if (insert) {
                    HashIntSet nextAnchor = HashIntSets.newMutableSet(candAnchor);
                    nextAnchor.add(next);
                    DegreeMap nextDegreeMap = new DegreeMap(graph, nextCand);
                    queue.add(new CandidateClique(nextAnchor, nextCand, nextNot, nextDegreeMap));
                }
                candCand.remove(next);
                candNot.add(next);
                HashIntSet neighInCand = graph.getNeighborsInSet(next, candCand);
                candMap.updateMap(neighInCand);
                queue.add(candidate);
            }
        }
        return cliques;
    }
    
    protected class CandidateClique {
        
        HashIntSet anchor;
        HashIntSet cand;
        HashIntSet not;
        DegreeMap degreeMap;
        
        protected CandidateClique(HashIntSet anchor, HashIntSet cand, HashIntSet not, DegreeMap degreeMap) {
            this.anchor = anchor;
            this.cand = cand;
            this.not = not;
            this.degreeMap = degreeMap;
        }
        
        protected HashIntSet getAnchor() {
            return anchor;
        }
        
        protected HashIntSet getCand() {
            return cand;
        }
        
        protected HashIntSet getNot() {
            return not;
        }
        
        protected DegreeMap getDegreeMap() {
            return degreeMap;
        }
        
    }
    
    public List<HashIntSet> findMaxCliques() {
        // Initialize data structures
        HashIntSet anchor = HashIntSets.newMutableSet();
        HashIntSet not = HashIntSets.newMutableSet();
        HashIntSet cand = HashIntSets.newMutableSet(graph.getNodes());
        DegreeMap degreeMap = new DegreeMap(graph, cand);
        // Start iteration
        return enumerateClique(anchor, cand, not, degreeMap);
    }
    
    public List<HashIntSet> findMaxCliquesWithCCs() {
        List<HashIntSet> ccs = graph.findCCs();
        Optional<List<HashIntSet>> op = ccs.parallelStream()
                .map(cc -> {
                    HashIntSet anchor = HashIntSets.newMutableSet();
                    HashIntSet not = HashIntSets.newMutableSet();
                    HashIntSet cand = HashIntSets.newMutableSet(cc);
                    DegreeMap degreeMap = new DegreeMap(graph, cc);
                    // Start iteration
                    return enumerateClique(anchor, cand, not, degreeMap);
                })
                .reduce((List<HashIntSet> l1, List<HashIntSet> l2) -> {l1.addAll(l2); return l1;});
        return op.isPresent() ? op.get() : Lists.newArrayList();
    }
    
    public List<HashIntSet> findMaxCliquesIterative() {
        List<HashIntSet> ccs = graph.findCCs();
        Optional<List<HashIntSet>> op = ccs.parallelStream()
                .map(cc -> {
                    HashIntSet anchor = HashIntSets.newMutableSet();
                    HashIntSet not = HashIntSets.newMutableSet();
                    HashIntSet cand = HashIntSets.newMutableSet(cc);
                    DegreeMap degreeMap = new DegreeMap(graph, cc);
                    // Start iteration
                    return enumerateCliqueIterative(anchor, cand, not, degreeMap);
                })
                .reduce((List<HashIntSet> l1, List<HashIntSet> l2) -> {l1.addAll(l2); return l1;});
        return op.isPresent() ? op.get() : Lists.newArrayList();
    }

    private boolean isAClique(HashIntSet nodes, DegreeMap degreeMap) {
        Collection<Integer> cands = degreeMap.getNodesWithDeg(nodes.size() - 1);
        if (cands.size() < nodes.size()) {
            return false;
        }
        return nodes.stream().noneMatch((n) -> (!cands.contains(n)));
    }
    
}
