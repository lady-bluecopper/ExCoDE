package eu.unitn.disi.db.excode.densesub;

import com.google.common.collect.Lists;
import com.koloboke.collect.IntCursor;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.collect.set.hash.HashObjSets;
import eu.unitn.disi.db.excode.graph.DynamicGraph;
import eu.unitn.disi.db.excode.graph.SummaryGraph;
import eu.unitn.disi.db.excode.utils.Pair;
import eu.unitn.disi.db.excode.utils.Settings;
import eu.unitn.disi.db.excode.utils.Utilities;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author bluecopper
 */
public class DenseSubgraphsFinder {

    private final DynamicGraph graph;
    private final SummaryGraph summaryGraph;
    private final List<HashIntSet> denseCorrelatedEdges;

    public DenseSubgraphsFinder(DynamicGraph graph) {
        this.graph = graph;
        this.summaryGraph = new SummaryGraph(graph.getEdges());
        this.denseCorrelatedEdges = Lists.newArrayList();
    }

    public List<HashIntSet> findDiverseDenseSubgraphs(List<HashIntSet> cliques) {
        List<HashIntSet> candidates = extractAllCCs(cliques);
        long start = System.currentTimeMillis();
        exploreComponents(candidates);
        System.out.println(".......................Found " + denseCorrelatedEdges.size() + " Dense Subgraphs in (s) " + ((System.currentTimeMillis() - start) / 1000));
        return denseCorrelatedEdges;
    }

    private Pair<Integer, HashObjSet<HashIntSet>> iskMINDenseSubgraph(HashIntSet comp) {
        Pair<HashIntSet, Integer> p = graph.getKEdgeSnaps(comp);
        if (summaryGraph.isAVGDenseSubgraph(comp, p.getB(), p.getA().size(), Settings.minDen)) {
            if (graph.isMINDense(comp, p.getA(), Settings.minDen)) {
                return new Pair<Integer, HashObjSet<HashIntSet>>(1, null); 
            }
        }
        if (!graph.containsDenseSubgraph(comp, p.getA(), Settings.minDen / 2)) {
            return new Pair<Integer, HashObjSet<HashIntSet>>(-1, null);
        }
        HashObjSet<HashIntSet> denseSubs = graph.extractDenseSubgraphs(comp, p.getA(), Settings.minDen);
        return (!denseSubs.isEmpty()) ? new Pair<Integer, HashObjSet<HashIntSet>>(0, denseSubs) 
                : new Pair<Integer, HashObjSet<HashIntSet>>(-1, null);
    }
    
    private Pair<Integer, HashObjSet<HashIntSet>> iskAVGDenseSubgraph(HashIntSet comp) {
        Pair<HashIntSet, Integer> p = graph.getKEdgeSnaps(comp);
        if (summaryGraph.isAVGDenseSubgraph(comp, p.getB(), p.getA().size(), Settings.minDen)) {
            return new Pair<Integer, HashObjSet<HashIntSet>>(1, null);
        }
        HashObjSet<HashIntSet> denseSubs = HashObjSets.newMutableSet();
        if (summaryGraph.containsDenseSubgraph(comp, p.getB(), p.getA().size(), Settings.minDen / 2)) {
            denseSubs = summaryGraph.extractDenseSubgraphs(comp, p.getB(), p.getA().size(), Settings.minDen);
        }
        return (!denseSubs.isEmpty()) ? new Pair<Integer, HashObjSet<HashIntSet>>(0, denseSubs)
                : new Pair<Integer, HashObjSet<HashIntSet>>(-1, null);
    }

    public void exploreComponents(Collection<HashIntSet> candidates) {
        HashObjSet<HashIntSet> denseSubs = HashObjSets.newMutableSet();
        candidates.stream().forEachOrdered(cand -> {
            if (isValidCandidate(cand, denseSubs)) {
                Pair<Integer, HashObjSet<HashIntSet>> result = (Settings.isMA) ? iskMINDenseSubgraph(cand) : iskAVGDenseSubgraph(cand);
                if (result.getA() == 1) {
                    denseCorrelatedEdges.add(cand);
                } else if (result.getA() == 0) {
                    denseSubs.addAll(result.getB());
                }
            }
        });
        if (denseSubs.size() > 0) {
            List<HashIntSet> denseSubsList = Lists.newArrayList(denseSubs);
            Collections.sort(denseSubsList, (HashIntSet s1, HashIntSet s2) -> -Integer.compare(s1.size(), s2.size()));
            denseSubsList.stream().forEachOrdered(subgraph -> {
                if (isValidCandidate(subgraph)) {
                    denseCorrelatedEdges.add(subgraph);
                }
            });
        }
    }

    private boolean isValidCandidate(HashIntSet cand, HashObjSet<HashIntSet> denseSubs) {
        if (cand.size() >= Settings.maxCCSize) {
            return false;
        }
        if (!isMaximalInCollection(cand, denseSubs) || !isMaximal(cand)) {
            return false;
        }
        return denseCorrelatedEdges.parallelStream().noneMatch(res -> Utilities.jaccardSimilarity(res, cand) >= Settings.maxJac);
    }
    
    private boolean isValidCandidate(HashIntSet cand) {
        if (!isMaximal(cand)) {
            return false;
        }
        return denseCorrelatedEdges.parallelStream().noneMatch(res -> Utilities.jaccardSimilarity(res, cand) >= Settings.maxJac);
    }

    private boolean isMaximal(HashIntSet cand) {
        return denseCorrelatedEdges.stream().filter(dense -> cand.size() <= dense.size()).noneMatch(dense -> dense.containsAll(cand));
    }

    private List<HashIntSet> extractAllCCs(List<HashIntSet> cliques) {
        long start = System.currentTimeMillis();
        List<HashIntSet> allCCs = cliques
                .parallelStream()
                .flatMap(clique -> graph.findCCsER(clique).stream())
                .distinct()
                .filter(cc -> cc.size() >= Settings.minDen)
                .collect(Collectors.toList());
        Collections.sort(allCCs, (HashIntSet s1, HashIntSet s2) -> -Integer.compare(s1.size(), s2.size()));
        System.out.println(".......................Found " + allCCs.size() + " CCs in (s) " + ((System.currentTimeMillis() - start) / 1000));
        return allCCs;
    }

    private boolean isMaximalInCollection(HashIntSet cand, Collection<HashIntSet> set) {
        return set.parallelStream().noneMatch((dense) -> (dense.containsAll(cand)));
    }

    private boolean isContainedIn(int[] cand, int[] dense) {
        if (cand[0] < dense[0] || cand[cand.length - 1] > dense[dense.length - 1]) {
            return false;
        }
        int id1 = 0;
        int id2 = 0;
        while (id1 < cand.length && id2 < dense.length) {
            int cmp = cand[id1] - dense[id2];
            if (cmp < 0) {
                return false;
            }
            if (cmp == 0) {
                id1++;
            }
            id2++;
        }
        return id1 == cand.length;
    }
    
    private boolean isMaximalInCollection(int[] cand, Collection<int[]> set) {
        return set.parallelStream().noneMatch(dense -> isContainedIn(cand, dense));
    }
    
    private List<int[]> extractMaximal(List<HashIntSet> sets) {
        List<int[]> arrays = Lists.newArrayList();
        List<int[]> maximal = Lists.newArrayList();
        long start = System.currentTimeMillis();
        sets.stream().forEach(set -> {
            int[] array = new int[set.size()];
            int index = 0;
            IntCursor cur = set.cursor();
            while (cur.moveNext()) {
                array[index] = cur.elem();
                index++;
            }
            Arrays.sort(array);
            arrays.add(array);
        });
        Collections.sort(arrays, (int[] s1, int[] s2) -> -Integer.compare(s1.length, s2.length));
        arrays.stream().forEachOrdered(can -> {
            if (can.length > 1 && isMaximalInCollection(can, maximal)) {
                maximal.add(can);
            }
        });
        System.out.println(".......................Found " + maximal.size() + " Maximal CCs in (s) " + ((System.currentTimeMillis() - start) / 1000));
        return maximal;
    }

}
