package eu.unitn.disi.db.excode.bucketization;

import com.google.common.collect.Lists;
import com.koloboke.collect.map.hash.HashObjObjMap;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import eu.unitn.disi.db.excode.graph.DynamicEdge;
import eu.unitn.disi.db.excode.utils.Pair;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.collections4.collection.SynchronizedCollection;

/**
 *
 * @author bluecopper
 */
public class MinHashBucketization {
    
    int[][] indices;
    final int numHashFuncs;
    final int numHashRuns;
    
    public MinHashBucketization(int numHashFuncs, int numHashRuns, int vectorSize) {
        this.numHashFuncs = numHashFuncs;
        this.numHashRuns = numHashRuns;
        this.indices = new int[numHashFuncs * numHashRuns][vectorSize];
        initialize();
    }
    
    private void initialize() {
        IntStream.range(0, indices.length).parallel().forEach(h -> 
                indices[h] = PRNG.randIntVec((numHashFuncs * h) + (h % numHashFuncs) + 1, indices[h].length));
    }
    
    // create (e1, e2) only if e1 and e2 agree on every hash value
    public Collection<Pair<Integer, Integer>> getCandidatePairs(List<DynamicEdge> edges) {
        HashObjObjMap<String, List<Integer>> buckets = HashObjObjMaps.newMutableMap();
        List<Pair<Integer, Set<String>>> edgeCodes = edges.parallelStream().map(edge -> new Pair<Integer, Set<String>>(edge.getEdgeID(), minHashes(edge.getEdgeSeries())))
                .collect(Collectors.toList());
        edgeCodes.stream().forEach(p -> {
            p.getB().stream().forEach(code -> {
                List<Integer> current = buckets.getOrDefault(code, Lists.newArrayList());
                current.add(p.getA());
                buckets.put(code, current);
            });
        });
        Collection<Pair<Integer, Integer>> candPairs = SynchronizedCollection.synchronizedCollection(Lists.newArrayList());
        buckets.values().stream().forEach(cList -> 
                cList.stream().forEach(e1 -> 
                        cList.stream().filter(e2 -> e1 < e2).forEach(e2 -> candPairs.add(new Pair(e1, e2)))));
        return candPairs;
    }
    
    public Set<String> minHashes(double[] vector) {
        int[] mins = new int[numHashFuncs * numHashRuns];
        Set<String> hashCodes = new HashSet<String>();
        Arrays.fill(mins, Integer.MAX_VALUE);
        IntStream.range(0, vector.length).forEach(t -> {
            if (vector[t] > 0) {
                IntStream.range(0, mins.length).forEach(h -> mins[h] = Math.min(mins[h], indices[h][t]));
            }
        });
        IntStream.range(0, numHashRuns).forEach(r -> {
            StringBuilder builder = new StringBuilder();
            IntStream.range(0, numHashFuncs).forEach(h -> {
                builder.append("-").append(mins[r * h + h]);
            });
            hashCodes.add(builder.deleteCharAt(0).toString());
        });
        return hashCodes;
    }
    
}
