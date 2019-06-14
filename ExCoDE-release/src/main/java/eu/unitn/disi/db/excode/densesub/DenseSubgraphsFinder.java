package eu.unitn.disi.db.excode.densesub;

import com.google.common.collect.Lists;
import com.koloboke.collect.IntCursor;
import com.koloboke.collect.map.hash.HashObjIntMap;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.collect.set.hash.HashObjSets;
import eu.unitn.disi.db.excode.graph.DynamicGraph;
import eu.unitn.disi.db.excode.graph.SummaryGraph;
import eu.unitn.disi.db.excode.utils.Pair;
import eu.unitn.disi.db.excode.utils.Settings;
import eu.unitn.disi.db.excode.utils.Triplet;
import eu.unitn.disi.db.excode.utils.Utilities;
import eu.unitn.disi.db.excode.webserver.utils.Configuration;
import java.util.ArrayList;
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
    private Double minDen;
    private Double maxJac;
    private Integer maxCCSize;
    private boolean isMA;
    private Integer minEdgesInSnap;

    public DenseSubgraphsFinder(DynamicGraph graph) {
        this.graph = graph;
        this.summaryGraph = new SummaryGraph(graph.getEdges(), minEdgesInSnap);
        this.denseCorrelatedEdges = Lists.newArrayList();
        this.minDen = Settings.minDen;
        this.maxJac = Settings.maxJac;
        this.maxCCSize = Settings.maxCCSize;
        this.isMA = Settings.isMA;
        this.minEdgesInSnap = Settings.minEdgesInSnap;
    }
    
    public DenseSubgraphsFinder(DynamicGraph graph, Configuration conf) {
        this.graph = graph;
        this.summaryGraph = new SummaryGraph(graph.getEdges(), conf.Task.EdgesPerSnapshot);
        this.denseCorrelatedEdges = Lists.newArrayList();
        this.minDen = conf.Task.Density;
        this.maxJac = conf.Task.Epsilon;
        this.maxCCSize = conf.Task.MaxSize;
        this.isMA = conf.Task.DensityFunction.equals("Minimum");
        this.minEdgesInSnap = conf.Task.EdgesPerSnapshot;
    }

    public List<HashIntSet> findDiverseDenseSubgraphs(List<HashIntSet> cliques) {
        List<HashIntSet> candidates = extractAllCCs(cliques);
        long start = System.currentTimeMillis();
        exploreComponents(candidates);
        System.out.println(".......................Found " + denseCorrelatedEdges.size() + " Dense Subgraphs in (s) " + ((System.currentTimeMillis() - start) / 1000));
        return denseCorrelatedEdges;
    }
    
    public List<Pair<HashIntSet, Double>> findDiverseDenseSubgraphsWithScore(List<HashIntSet> cliques) {
        List<HashIntSet> candidates = extractAllCCs(cliques);
        return exploreComponentsWithScore(candidates);
    }

    private Pair<Integer, HashObjSet<HashIntSet>> iskMINDenseSubgraph(HashIntSet comp) {
        Pair<HashIntSet, Integer> p = graph.getKEdgeSnaps(comp, minEdgesInSnap);
        if (summaryGraph.isAVGDenseSubgraph(comp, p.getB(), p.getA().size(), minDen)) {
            if (graph.isMINDense(comp, p.getA(), minDen)) {
                return new Pair<Integer, HashObjSet<HashIntSet>>(1, null); 
            }
        }
        if (!graph.containsDenseSubgraph(comp, p.getA(), minDen / 2)) {
            return new Pair<Integer, HashObjSet<HashIntSet>>(-1, null);
        }
        HashObjSet<HashIntSet> denseSubs = graph.extractDenseSubgraphs(comp, p.getA(), minDen);
        return (!denseSubs.isEmpty()) ? new Pair<Integer, HashObjSet<HashIntSet>>(0, denseSubs) 
                : new Pair<Integer, HashObjSet<HashIntSet>>(-1, null);
    }
    
    private Triplet<Integer, HashObjSet<Pair<HashIntSet, Double>>, Double> iskMINDenseSubgraphWithScore(HashIntSet comp) {
        Pair<HashIntSet, Integer> p = graph.getKEdgeSnaps(comp, minEdgesInSnap);
        if (summaryGraph.isAVGDenseSubgraph(comp, p.getB(), p.getA().size(), minDen)) {
            if (graph.isMINDense(comp, p.getA(), minDen)) {
                return new Triplet<Integer, HashObjSet<Pair<HashIntSet, Double>>, Double>(1, null, graph.getMINDensity(comp, p.getA())); 
            }
        }
        if (!graph.containsDenseSubgraph(comp, p.getA(), minDen / 2)) {
            return new Triplet<Integer, HashObjSet<Pair<HashIntSet, Double>>, Double>(-1, null, -1.);
        }
        HashObjSet<Pair<HashIntSet, Double>> denseSubs = graph.extractDenseSubgraphsWithScore(comp, p.getA(), minDen);
        return (!denseSubs.isEmpty()) ? new Triplet<Integer, HashObjSet<Pair<HashIntSet, Double>>, Double>(0, denseSubs, -1.) 
                : new Triplet<Integer, HashObjSet<Pair<HashIntSet, Double>>, Double>(-1, null, -1.);
    }
    
    private Pair<Integer, HashObjSet<HashIntSet>> iskAVGDenseSubgraph(HashIntSet comp) {
        Pair<HashIntSet, Integer> p = graph.getKEdgeSnaps(comp, minEdgesInSnap);
        if (summaryGraph.isAVGDenseSubgraph(comp, p.getB(), p.getA().size(), minDen)) {
            return new Pair<Integer, HashObjSet<HashIntSet>>(1, null);
        }
        HashObjSet<HashIntSet> denseSubs = HashObjSets.newMutableSet();
        if (summaryGraph.containsDenseSubgraph(comp, p.getB(), p.getA().size(), minDen / 2)) {
            denseSubs = summaryGraph.extractDenseSubgraphs(comp, p.getB(), p.getA().size(), minDen);
        }
        return (!denseSubs.isEmpty()) ? new Pair<Integer, HashObjSet<HashIntSet>>(0, denseSubs)
                : new Pair<Integer, HashObjSet<HashIntSet>>(-1, null);
    }
    
    private Triplet<Integer, HashObjSet<Pair<HashIntSet, Double>>, Double> iskAVGDenseSubgraphWithScore(HashIntSet comp) {
        Pair<HashIntSet, Integer> p = graph.getKEdgeSnaps(comp, minEdgesInSnap);
        if (summaryGraph.isAVGDenseSubgraph(comp, p.getB(), p.getA().size(), minDen)) {
            return new Triplet<Integer, HashObjSet<Pair<HashIntSet, Double>>, Double>(1, null, summaryGraph.getAVGDensity(comp, p.getB(), p.getA().size()));
        }
        HashObjSet<Pair<HashIntSet, Double>> denseSubs = HashObjSets.newMutableSet();
        if (summaryGraph.containsDenseSubgraph(comp, p.getB(), p.getA().size(), minDen / 2)) {
            denseSubs = summaryGraph.extractDenseSubgraphsWithScore(comp, p.getB(), p.getA().size(), minDen);
        }
        return (!denseSubs.isEmpty()) ? new Triplet<Integer, HashObjSet<Pair<HashIntSet, Double>>, Double>(0, denseSubs, -1.)
                : new Triplet<Integer, HashObjSet<Pair<HashIntSet, Double>>, Double>(-1, null, -1.);
    }

    public void exploreComponents(Collection<HashIntSet> candidates) {
        HashObjSet<HashIntSet> denseSubs = HashObjSets.newMutableSet();
        candidates.stream().forEachOrdered(cand -> {
            if (isValidCandidate(cand, denseSubs)) {
                Pair<Integer, HashObjSet<HashIntSet>> result = (isMA) ? iskMINDenseSubgraph(cand) : iskAVGDenseSubgraph(cand);
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
    
    public List<Pair<HashIntSet, Double>> exploreComponentsWithScore(Collection<HashIntSet> candidates) {
        List<Pair<HashIntSet, Double>> denCorEdgesWithScore = new ArrayList<Pair<HashIntSet, Double>>();
        HashObjSet<Pair<HashIntSet, Double>> denseSubs = HashObjSets.newMutableSet();
        candidates.stream().forEachOrdered(cand -> {
            if (isValidCandidate(cand, denseSubs, denCorEdgesWithScore)) {
                Triplet<Integer, HashObjSet<Pair<HashIntSet, Double>>, Double> result = (isMA) ? iskMINDenseSubgraphWithScore(cand) : iskAVGDenseSubgraphWithScore(cand);
                if (result.getA() == 1) {
                    denCorEdgesWithScore.add(new Pair<HashIntSet, Double>(cand, result.getC()));
                } else if (result.getA() == 0) {
                    denseSubs.addAll(result.getB());
                }
            }
        });
        if (denseSubs.size() > 0) {
            List<Pair<HashIntSet, Double>> denseSubsList = Lists.newArrayList(denseSubs);
            Collections.sort(denseSubsList, (Pair<HashIntSet, Double> s1, Pair<HashIntSet, Double> s2) -> -Integer.compare(s1.getA().size(), s2.getA().size()));
            denseSubsList.stream().forEachOrdered(subgraph -> {
                if (isValidCandidate(subgraph.getA())) {
                    denCorEdgesWithScore.add(subgraph);
                }
            });
        }
        return denCorEdgesWithScore;
    }

    private boolean isValidCandidate(HashIntSet cand, HashObjSet<HashIntSet> denseSubs) {
        if (cand.size() >= maxCCSize) {
            return false;
        }
        if (!isMaximalInCollection(cand, denseSubs) || !isMaximal(cand)) {
            return false;
        }
        return denseCorrelatedEdges.parallelStream().noneMatch(res -> Utilities.jaccardSimilarity(res, cand) >= maxJac);
    }
    
    private boolean isValidCandidate(HashIntSet cand, HashObjSet<Pair<HashIntSet, Double>> denseSubs, List<Pair<HashIntSet, Double>> denseGroups) {
        if (cand.size() >= maxCCSize) {
            return false;
        }
        if (denseSubs.parallelStream().anyMatch(dense -> (dense.getA().containsAll(cand))) || !isMaximal(cand)) {
            return false;
        }
        return denseGroups.parallelStream().noneMatch(res -> Utilities.jaccardSimilarity(res.getA(), cand) >= maxJac);
    }
    
    private boolean isValidCandidate(HashIntSet cand) {
        if (!isMaximal(cand)) {
            return false;
        }
        return denseCorrelatedEdges.parallelStream().noneMatch(res -> Utilities.jaccardSimilarity(res, cand) >= maxJac);
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
                .filter(cc -> cc.size() >= minDen)
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
