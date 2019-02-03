package eu.unitn.disi.db.excode.graph;

import com.google.common.collect.Lists;
import com.koloboke.collect.map.hash.HashIntIntMap;
import com.koloboke.collect.map.hash.HashIntIntMaps;
import com.koloboke.collect.map.hash.HashIntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import com.koloboke.collect.map.hash.HashObjObjMap;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.collect.set.hash.HashObjSets;
import eu.unitn.disi.db.excode.densesub.MetaData;
import eu.unitn.disi.db.excode.utils.Pair;
import eu.unitn.disi.db.excode.utils.Settings;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author bluecopper
 */
public class SummaryGraph {

    private final int numSnaps;
    private List<DynamicEdge> seeds;
    private HashIntObjMap<WeightedEdge> edges;
   

    public SummaryGraph(List<DynamicEdge> seeds) {
        this.numSnaps = seeds.get(0).getEdgeSeries().length;
        this.seeds = seeds;
        initializeEdgeMap(seeds);
    }
    
    public SummaryGraph(int numSnaps, List<WeightedEdge> seeds) {
        this.numSnaps = numSnaps;
        this.edges = HashIntObjMaps.newMutableMap();
        seeds.stream().forEach(e -> this.edges.put(e.getEdgeID(), e));
    }

    private void initializeEdgeMap(List<DynamicEdge> seeds) {
        this.edges = HashIntObjMaps.newMutableMap();
        seeds.stream().forEach(e -> edges.put(e.getEdgeID(), new WeightedEdge(e, e.getSupport())));
    }
    
    public void addEdge(WeightedEdge e) {
        edges.put(e.getEdgeID(), e);
    }
    
    public double getEdgeWeight(int edge) {
        return edges.get(edge).getWeight();
    }

    private double getWeightSumOf(HashIntSet comp) {
        return comp.stream().mapToDouble(e -> edges.get(e).getWeight()).sum();
    }

    public int getNumNodesOf(HashIntSet comp) {
        return comp.stream()
                .flatMap(e -> Arrays.asList(edges.get(e).getSrc(), edges.get(e).getDst()).stream())
                .collect(Collectors.toSet())
                .size();
    }

    public boolean isAVGDenseSubgraph(HashIntSet comp, double minDen) {
        int numNodes = getNumNodesOf(comp);
        if (numNodes < 2 && minDen > 0) {
            return false;
        }
        return getWeightSumOf(comp) >= minDen * numSnaps * numNodes / 2;
    }
    
    public boolean isAVGDenseSubgraph(HashIntSet comp, int skipped, int numkSnaps, double minDen) {
        if (numkSnaps == 0) {
            return false;
        }
        int numNodes = getNumNodesOf(comp);
        if (numNodes < 2 && minDen > 0) {
            return false;
        }
        return getWeightSumOf(comp) - skipped >= minDen * numkSnaps * numNodes / 2;
    }

    public boolean isAVGDenseSubgraph(HashIntSet comp, int numNodes, double minDen) {
        if (numNodes < 2 && minDen > 0) {
            return false;
        }
        return getWeightSumOf(comp) >= minDen * numSnaps * numNodes / 2;
    }
    
    public boolean isAVGDenseSubgraph(HashIntSet comp, int numNodes, int skipped, int numkSnaps, double minDen) {
        if ((numNodes < 2 && minDen > 0) || numkSnaps == 0) {
            return false;
        }
        return getWeightSumOf(comp) - skipped >= minDen * numkSnaps * numNodes / 2;
    }
    
    public boolean containsDenseSubgraph(HashIntSet comp, double minDen) {
        HashIntSet temp = HashIntSets.newMutableSet(comp);
        HashIntObjMap<HashIntSet> nodeEdges = HashIntObjMaps.newMutableMap();
        int min;
        int max;
        int minIndex;
        double weightSum;
        temp.stream().forEach(e -> {
            WeightedEdge edge = edges.get(e);
            HashIntSet srcList = nodeEdges.getOrDefault(edge.getSrc(), HashIntSets.newMutableSet());
            srcList.add(e);
            HashIntSet dstList = nodeEdges.getOrDefault(edge.getDst(), HashIntSets.newMutableSet());
            dstList.add(e);
            nodeEdges.put(edge.getSrc(), srcList);
            nodeEdges.put(edge.getDst(), dstList);
        });
        int numNodes = nodeEdges.size();
        while (!isAVGDenseSubgraph(temp, numNodes, minDen)) {
            if (numNodes < 2 && minDen > 0) {
                return false;
            }
            min = Integer.MAX_VALUE;
            max = 0;
            minIndex = -1;
            weightSum = Double.MAX_VALUE;
            for (Entry<Integer, HashIntSet> e : nodeEdges.entrySet()) {
                int curr = e.getValue().size();
                if (curr < min) {
                    double thisSum = getWeightSumOf(e.getValue());
                    if (thisSum < weightSum) {
                        minIndex = e.getKey();
                        min = curr;
                        weightSum = thisSum;
                    }
                }
                max = Math.max(curr, max);
            }
            if (max < minDen) {
                return false;
            }
            HashIntSet toRemove = HashIntSets.newMutableSet(nodeEdges.getOrDefault(minIndex, HashIntSets.newMutableSet()));
            temp.removeAll(toRemove);
            nodeEdges.values().stream().forEach(edgeList -> edgeList.removeAll(toRemove));
            nodeEdges.remove(minIndex);
            numNodes--;
        }
        return true;
    }
    
    public boolean containsDenseSubgraph(HashIntSet comp, int skipped, int numSnaps, double minDen) {
        HashIntSet temp = HashIntSets.newMutableSet(comp);
        HashIntObjMap<HashIntSet> nodeEdges = HashIntObjMaps.newMutableMap();
        int min;
        int max;
        int minIndex;
        double weightSum;
        temp.stream().forEach(e -> {
            WeightedEdge edge = edges.get(e);
            HashIntSet srcList = nodeEdges.getOrDefault(edge.getSrc(), HashIntSets.newMutableSet());
            srcList.add(e);
            HashIntSet dstList = nodeEdges.getOrDefault(edge.getDst(), HashIntSets.newMutableSet());
            dstList.add(e);
            nodeEdges.put(edge.getSrc(), srcList);
            nodeEdges.put(edge.getDst(), dstList);
        });
        int numNodes = nodeEdges.size();
        while (!isAVGDenseSubgraph(temp, numNodes, skipped, numSnaps, minDen)) {
            if (numNodes < 2 && minDen > 0) {
                return false;
            }
            if (numSnaps == 0) {
                return false;
            }
            min = Integer.MAX_VALUE;
            max = 0;
            minIndex = -1;
            weightSum = Double.MAX_VALUE;
            for (Entry<Integer, HashIntSet> e : nodeEdges.entrySet()) {
                int curr = e.getValue().size();
                if (curr < min) {
                    double thisSum = getWeightSumOf(e.getValue());
                    if (thisSum < weightSum) {
                        minIndex = e.getKey();
                        min = curr;
                        weightSum = thisSum;
                    }
                }
                max = Math.max(curr, max);
            }
            if (max < minDen) {
                return false;
            }
            HashIntSet toRemove = HashIntSets.newMutableSet(nodeEdges.getOrDefault(minIndex, HashIntSets.newMutableSet()));
            temp.removeAll(toRemove);
            nodeEdges.values().stream().forEach(edgeList -> edgeList.removeAll(toRemove));
            nodeEdges.remove(minIndex);
            numNodes--;
            Pair<Integer, Integer> p = getKEdgeSnaps(temp);
            skipped = p.getA();
            numSnaps = p.getB();
        }
        return true;
    }
    
    public HashObjSet<HashIntSet> extractDenseSubgraphs(HashIntSet comp, double minDen) {
        HashObjSet<HashIntSet> denseSubs = HashObjSets.newMutableSet();
        HashIntObjMap<HashObjSet<HashIntSet>> queue = HashIntObjMaps.newMutableMap();
        HashObjObjMap<HashIntSet, HashIntObjMap<HashIntSet>> nodeEdgeMap = HashObjObjMaps.newMutableMap();
        int min;
        int max;
        HashIntObjMap<HashIntSet> nodeEdges = HashIntObjMaps.newMutableMap();
        comp.stream().forEach(e -> {
            WeightedEdge edge = edges.get(e);
            HashIntSet srcList = nodeEdges.getOrDefault(edge.getSrc(), HashIntSets.newMutableSet());
            srcList.add(e);
            HashIntSet dstList = nodeEdges.getOrDefault(edge.getDst(), HashIntSets.newMutableSet());
            dstList.add(e);
            nodeEdges.put(edge.getSrc(), srcList);
            nodeEdges.put(edge.getDst(), dstList);
        });
        int size = nodeEdges.size();
        HashObjSet<HashIntSet> firstSet = HashObjSets.newMutableSet();
        firstSet.add(comp);
        queue.put(size, firstSet);
        nodeEdgeMap.put(comp, nodeEdges);
        while (size > 1 && !queue.isEmpty()) {
            HashObjSet<HashIntSet> candidates = queue.getOrDefault(size, HashObjSets.newMutableSet());
            for (HashIntSet candidate : candidates) {
                HashIntObjMap<HashIntSet> nodeEdgeCand = nodeEdgeMap.get(candidate);
                if (isAVGDenseSubgraph(candidate, size, minDen)) {
                    denseSubs.add(candidate);
                    continue;
                } 
                min = Integer.MAX_VALUE;
                max = 0;
                List<Integer> minIndices = Lists.newArrayList();
                for (Entry<Integer, HashIntSet> e : nodeEdgeCand.entrySet()) {
                    int curr = e.getValue().size();
                    min = Math.min(curr, min);
                    max = Math.max(curr, max);
                }
                if (max < minDen) {
                    continue;
                }
                final int minDeg = min;
                nodeEdgeCand.entrySet().stream().filter(e -> e.getValue().size() == minDeg).forEach(e -> minIndices.add(e.getKey()));
                if (minDeg == 0) {
                    minIndices.stream().forEach(id -> nodeEdgeCand.remove(id));
                    int newSize = size - minIndices.size();
                    HashObjSet<HashIntSet> currSet = queue.getOrDefault(newSize, HashObjSets.newMutableSet());
                    currSet.add(candidate);
                    queue.put(newSize, currSet);
                    nodeEdgeMap.replace(candidate, nodeEdgeCand);
                    continue;
                } 
                if (minDeg == 1) {
                    HashIntSet tempCand = HashIntSets.newMutableSet(candidate);
                    HashIntSet toRemove = HashIntSets.newMutableSet();
                    nodeEdgeCand.entrySet().stream()
                            .filter(e -> minIndices.contains(e.getKey()))
                            .forEach(e -> toRemove.addAll(e.getValue()));
                    tempCand.removeAll(toRemove);
                    if (tempCand.size() > 0 && !isAVGDenseSubgraph(tempCand, size - minIndices.size(), minDen) && !nodeEdgeMap.containsKey(tempCand)) {
                        candidate.removeAll(toRemove);
                        nodeEdgeCand.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                        minIndices.stream().forEach(id -> nodeEdgeCand.remove(id));
                        int newSize = size - minIndices.size();
                        HashObjSet<HashIntSet> currSet = queue.getOrDefault(newSize, HashObjSets.newMutableSet());
                        currSet.add(candidate);
                        queue.put(newSize, currSet);
                        nodeEdgeMap.put(candidate, nodeEdgeCand);
                        continue;
                    } 
                } 
                for (int node : minIndices) {
                    HashIntObjMap<HashIntSet> newMap = HashIntObjMaps.newMutableMap();
                    nodeEdgeCand.entrySet().forEach(entry -> newMap.put(entry.getKey(), HashIntSets.newMutableSet(entry.getValue())));
                    HashIntSet newCand = HashIntSets.newMutableSet(candidate);
                    HashIntSet toRemove = HashIntSets.newMutableSet(newMap.getOrDefault(node, HashIntSets.newMutableSet()));
                    newCand.removeAll(toRemove);
                    newMap.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                    newMap.remove(node);
                    if (newCand.size() > 0 && !nodeEdgeMap.containsKey(newCand)) {
                        HashObjSet<HashIntSet> currSet = queue.getOrDefault(size - 1, HashObjSets.newMutableSet());
                        currSet.add(newCand);
                        queue.put(size - 1, currSet);
                        nodeEdgeMap.put(newCand, newMap);
                    }
                }
            }
            queue.remove(size);
            size--;
        }
        return denseSubs;
    }
    
    public HashObjSet<HashIntSet> extractDenseSubgraphs(HashIntSet comp, int skipped, int numSnaps, double minDen) {
        HashObjSet<HashIntSet> denseSubs = HashObjSets.newMutableSet();
        HashIntObjMap<HashObjSet<HashIntSet>> queue = HashIntObjMaps.newMutableMap();
        HashObjObjMap<HashIntSet, MetaData> candsData = HashObjObjMaps.newMutableMap();
        int min;
        int max;
        HashIntObjMap<HashIntSet> nodeEdges = HashIntObjMaps.newMutableMap();
        comp.stream().forEach(e -> {
            WeightedEdge edge = edges.get(e);
            HashIntSet srcList = nodeEdges.getOrDefault(edge.getSrc(), HashIntSets.newMutableSet());
            srcList.add(e);
            HashIntSet dstList = nodeEdges.getOrDefault(edge.getDst(), HashIntSets.newMutableSet());
            dstList.add(e);
            nodeEdges.put(edge.getSrc(), srcList);
            nodeEdges.put(edge.getDst(), dstList);
        });
        int size = nodeEdges.size();
        HashObjSet<HashIntSet> firstSet = HashObjSets.newMutableSet();
        firstSet.add(comp);
        queue.put(size, firstSet);
        candsData.put(comp, new MetaData<Pair<Integer, Integer>>(nodeEdges, new Pair<Integer, Integer>(skipped, numSnaps)));
        while (size > 1 && !queue.isEmpty()) {
            HashObjSet<HashIntSet> candidates = queue.getOrDefault(size, HashObjSets.newMutableSet());
            for (HashIntSet candidate : candidates) {
                MetaData<Pair<Integer, Integer>> candData = candsData.get(candidate);
                HashIntObjMap<HashIntSet> nodeEdgeCand = candData.nodeEdgeMap;
                int candSkipped = candData.snapInfo.getA();
                int candNumSnaps = candData.snapInfo.getB();
                if (isAVGDenseSubgraph(candidate, size, candSkipped, candNumSnaps, minDen)) {
                    denseSubs.add(candidate);
                    continue;
                }
                if (candNumSnaps == 0) {
                    continue;
                }
                List<Integer> minIndices = Lists.newArrayList();
                min = Integer.MAX_VALUE;
                max = 0;
                for (Entry<Integer, HashIntSet> e : nodeEdgeCand.entrySet()) {
                    int curr = e.getValue().size();
                    min = Math.min(curr, min);
                    max = Math.max(curr, max);
                }
                if (max < minDen) {
                    continue;
                }
                final int minDeg = min;
                nodeEdgeCand.entrySet().stream().filter(e -> e.getValue().size() == minDeg).forEach(e -> minIndices.add(e.getKey()));
                if (minDeg == 0) {
                    minIndices.stream().forEach(id -> nodeEdgeCand.remove(id));
                    int newSize = size - minIndices.size();
                    HashObjSet<HashIntSet> currSet = queue.getOrDefault(newSize, HashObjSets.newMutableSet());
                    currSet.add(candidate);
                    queue.put(newSize, currSet);
                    candsData.replace(candidate, new MetaData<Pair<Integer, Integer>>(nodeEdgeCand, candData.snapInfo));
                    continue;
                } 
                if (minDeg == 1 && minIndices.size() > 1) {
                    HashIntSet tempCand = HashIntSets.newMutableSet(candidate);
                    HashIntSet toRemove = HashIntSets.newMutableSet();
                    nodeEdgeCand.entrySet().stream()
                            .filter(e -> minIndices.contains(e.getKey()))
                            .forEach(e -> toRemove.addAll(e.getValue()));
                    tempCand.removeAll(toRemove);
                    int newSize = size - minIndices.size();
                    Pair<Integer, Integer> p = getKEdgeSnaps(tempCand);
                    if (tempCand.size() > 0 && !isAVGDenseSubgraph(tempCand, newSize, p.getA(), p.getB(), minDen) && !candsData.containsKey(tempCand)) {
                        candidate.removeAll(toRemove);
                        nodeEdgeCand.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                        minIndices.stream().forEach(id -> nodeEdgeCand.remove(id));
                        HashObjSet<HashIntSet> currSet = queue.getOrDefault(newSize, HashObjSets.newMutableSet());
                        currSet.add(candidate);
                        queue.put(newSize, currSet);
                        candsData.put(candidate, new MetaData<Pair<Integer, Integer>>(nodeEdgeCand, p));
                        continue;
                    } 
                }
                for (int node : minIndices) {
                    HashIntObjMap<HashIntSet> newMap = HashIntObjMaps.newMutableMap();
                    nodeEdgeCand.entrySet().forEach(entry -> newMap.put(entry.getKey(), HashIntSets.newMutableSet(entry.getValue())));
                    HashIntSet newCand = HashIntSets.newMutableSet(candidate);
                    HashIntSet toRemove = HashIntSets.newMutableSet(newMap.getOrDefault(node, HashIntSets.newMutableSet()));
                    newCand.removeAll(toRemove);
                    newMap.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                    newMap.remove(node);
                    if (newCand.size() > 0 && !candsData.containsKey(newCand)) {
                        HashObjSet<HashIntSet> currSet = queue.getOrDefault(size - 1, HashObjSets.newMutableSet());
                        currSet.add(newCand);
                        queue.put(size - 1, currSet);
                        Pair<Integer, Integer> p = getKEdgeSnaps(newCand);
                        candsData.put(newCand, new MetaData<Pair<Integer, Integer>>(newMap, p));
                    }
                }
            }
            queue.remove(size);
            size--;
        }
        return denseSubs;
    }
    
    public HashObjSet<HashIntSet> extractDenseSubgraph(HashIntSet comp, int skipped, int numSnaps, double minDen) {
        HashIntObjMap<HashObjSet<HashIntSet>> queue = HashIntObjMaps.newMutableMap();
        HashObjObjMap<HashIntSet, MetaData> candsData = HashObjObjMaps.newMutableMap();
        int min;
        int max;
        HashIntObjMap<HashIntSet> nodeEdges = HashIntObjMaps.newMutableMap();
        comp.stream().forEach(e -> {
            WeightedEdge edge = edges.get(e);
            HashIntSet srcList = nodeEdges.getOrDefault(edge.getSrc(), HashIntSets.newMutableSet());
            srcList.add(e);
            HashIntSet dstList = nodeEdges.getOrDefault(edge.getDst(), HashIntSets.newMutableSet());
            dstList.add(e);
            nodeEdges.put(edge.getSrc(), srcList);
            nodeEdges.put(edge.getDst(), dstList);
        });
        int size = nodeEdges.size();
        HashObjSet<HashIntSet> firstSet = HashObjSets.newMutableSet();
        firstSet.add(comp);
        queue.put(size, firstSet);
        candsData.put(comp, new MetaData<Pair<Integer, Integer>>(nodeEdges, new Pair<Integer, Integer>(skipped, numSnaps)));
        while (size > 1 && !queue.isEmpty()) {
            HashObjSet<HashIntSet> candidates = queue.getOrDefault(size, HashObjSets.newMutableSet());
            for (HashIntSet candidate : candidates) {
                MetaData<Pair<Integer, Integer>> candData = candsData.get(candidate);
                HashIntObjMap<HashIntSet> nodeEdgeCand = candData.nodeEdgeMap;
                int candSkipped = candData.snapInfo.getA();
                int candNumSnaps = candData.snapInfo.getB();
                if (isAVGDenseSubgraph(candidate, size, candSkipped, candNumSnaps, minDen)) {
                    HashObjSet<HashIntSet> output = HashObjSets.newMutableSet();
                    output.add(candidate);
                    return output;
                }
                if (candNumSnaps == 0) {
                    continue;
                }
                List<Integer> minIndices = Lists.newArrayList();
                min = Integer.MAX_VALUE;
                max = 0;
                for (Entry<Integer, HashIntSet> e : nodeEdgeCand.entrySet()) {
                    int curr = e.getValue().size();
                    min = Math.min(curr, min);
                    max = Math.max(curr, max);
                }
                if (max < minDen) {
                    continue;
                }
                final int minDeg = min;
                nodeEdgeCand.entrySet().stream().filter(e -> e.getValue().size() == minDeg).forEach(e -> minIndices.add(e.getKey()));
                if (minDeg == 0) {
                    minIndices.stream().forEach(id -> nodeEdgeCand.remove(id));
                    int newSize = size - minIndices.size();
                    HashObjSet<HashIntSet> currSet = queue.getOrDefault(newSize, HashObjSets.newMutableSet());
                    currSet.add(candidate);
                    queue.put(newSize, currSet);
                    candsData.replace(candidate, new MetaData<Pair<Integer, Integer>>(nodeEdgeCand, candData.snapInfo));
                    continue;
                } 
                if (minDeg == 1 && minIndices.size() > 1) {
                    HashIntSet tempCand = HashIntSets.newMutableSet(candidate);
                    HashIntSet toRemove = HashIntSets.newMutableSet();
                    nodeEdgeCand.entrySet().stream()
                            .filter(e -> minIndices.contains(e.getKey()))
                            .forEach(e -> toRemove.addAll(e.getValue()));
                    tempCand.removeAll(toRemove);
                    int newSize = size - minIndices.size();
                    Pair<Integer, Integer> p = getKEdgeSnaps(tempCand);
                    if (tempCand.size() > 0 && !isAVGDenseSubgraph(tempCand, newSize, p.getA(), p.getB(), minDen) && !candsData.containsKey(tempCand)) {
                        candidate.removeAll(toRemove);
                        nodeEdgeCand.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                        minIndices.stream().forEach(id -> nodeEdgeCand.remove(id));
                        HashObjSet<HashIntSet> currSet = queue.getOrDefault(newSize, HashObjSets.newMutableSet());
                        currSet.add(candidate);
                        queue.put(newSize, currSet);
                        candsData.put(candidate, new MetaData<Pair<Integer, Integer>>(nodeEdgeCand, p));
                        continue;
                    }
                }
                for (int node : minIndices) {
                    HashIntObjMap<HashIntSet> newMap = HashIntObjMaps.newMutableMap();
                    nodeEdgeCand.entrySet().forEach(entry -> newMap.put(entry.getKey(), HashIntSets.newMutableSet(entry.getValue())));
                    HashIntSet newCand = HashIntSets.newMutableSet(candidate);
                    HashIntSet toRemove = HashIntSets.newMutableSet(newMap.getOrDefault(node, HashIntSets.newMutableSet()));
                    newCand.removeAll(toRemove);
                    newMap.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                    newMap.remove(node);
                    if (newCand.size() > 0 && !candsData.containsKey(newCand)) {
                        HashObjSet<HashIntSet> currSet = queue.getOrDefault(size - 1, HashObjSets.newMutableSet());
                        currSet.add(newCand);
                        queue.put(size - 1, currSet);
                        Pair<Integer, Integer> p = getKEdgeSnaps(newCand);
                        candsData.put(newCand, new MetaData<Pair<Integer, Integer>>(newMap, p));
                    }
                }
            }
            queue.remove(size);
            size--;
        }
        return HashObjSets.newMutableSet();
    }
    
    private Pair<Integer, Integer> getKEdgeSnaps(HashIntSet comp) {
        HashIntIntMap existCount = HashIntIntMaps.newMutableMap();
        comp.stream().forEach(id -> {
            IntStream.range(0, numSnaps).forEach(t -> {
                if (seeds.get(id).existsInT(t)) {
                    existCount.put(t, existCount.getOrDefault(t, 0) + 1);
                }
            });
        });
        HashIntSet existing = HashIntSets.newMutableSet();
        existCount.entrySet().stream()
                .filter(e -> e.getValue() >= Settings.minEdgesInSnap)
                .forEach(e -> existing.add(e.getKey()));
        int skipped = 0;
        if (Settings.minEdgesInSnap > 1 && existing.size() < numSnaps) {
            skipped = existCount.entrySet().stream()
                .filter(e -> e.getValue() < Settings.minEdgesInSnap)
                .mapToInt(e -> e.getValue())
                .sum();
        }
        return new Pair<Integer, Integer>(skipped, existing.size());
    }
    
    public HashObjSet<HashIntSet> denseSubgraphsInComp(HashIntObjMap<HashIntSet> nodeEdges, HashIntSet comp, int numNodes, double minDen) {
        HashObjSet<HashIntSet> denseSubs = HashObjSets.newMutableSet();
        while (!isAVGDenseSubgraph(comp, numNodes, minDen)) {
            List<Integer> minIndices = Lists.newArrayList();
            int min = Integer.MAX_VALUE;
            if (numNodes < 2 && minDen > 0) {
                return denseSubs;
            }
            for (Entry<Integer, HashIntSet> e : nodeEdges.entrySet()) {
                min = Math.min(e.getValue().size(), min);
            }
            final int minDeg = min;
            nodeEdges.entrySet().stream().filter(e -> e.getValue().size() == minDeg).forEach(e -> minIndices.add(e.getKey()));
            if (minDeg == 0) {
                minIndices.stream().forEach(id -> nodeEdges.remove(id));
                numNodes = numNodes - minIndices.size();
                continue;
            } 
            for (int node : minIndices) {
                HashIntObjMap<HashIntSet> newMap = HashIntObjMaps.newMutableMap();
                nodeEdges.entrySet().forEach(entry -> newMap.put(entry.getKey(), HashIntSets.newMutableSet(entry.getValue())));
                HashIntSet newComp = HashIntSets.newMutableSet(comp);
                HashIntSet toRemove = HashIntSets.newMutableSet(newMap.getOrDefault(node, HashIntSets.newMutableSet()));
                newComp.removeAll(toRemove);
                newMap.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                newMap.remove(node);
                denseSubs.addAll(denseSubgraphsInComp(newMap, newComp, numNodes - 1, minDen));
            }
            return denseSubs;
        }
        denseSubs.add(comp);
        return denseSubs;
    }
    
    public HashObjSet<HashIntSet> extractDenseSubgraphsRecursive(HashIntSet comp, double minDen) {
        HashIntObjMap<HashIntSet> nodeEdges = HashIntObjMaps.newMutableMap();
        comp.stream().forEach(e -> {
            WeightedEdge edge = edges.get(e);
            HashIntSet srcList = nodeEdges.getOrDefault(edge.getSrc(), HashIntSets.newMutableSet());
            srcList.add(e);
            HashIntSet dstList = nodeEdges.getOrDefault(edge.getDst(), HashIntSets.newMutableSet());
            dstList.add(e);
            nodeEdges.put(edge.getSrc(), srcList);
            nodeEdges.put(edge.getDst(), dstList);
        });
        return denseSubgraphsInComp(nodeEdges, comp, nodeEdges.size(), minDen);
    }

}
