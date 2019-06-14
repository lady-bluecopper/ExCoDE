package eu.unitn.disi.db.excode.main;

import com.koloboke.collect.map.hash.HashIntDoubleMap;
import com.koloboke.collect.map.hash.HashIntDoubleMaps;
import com.koloboke.collect.map.hash.HashIntIntMap;
import com.koloboke.collect.map.hash.HashIntIntMaps;
import com.koloboke.collect.map.hash.HashIntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import com.koloboke.collect.map.hash.HashObjIntMap;
import com.koloboke.collect.map.hash.HashObjObjMap;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.collect.set.hash.HashObjSets;
import eu.unitn.disi.db.excode.bucketization.MinHashBucketization;
import eu.unitn.disi.db.excode.densesub.DenseSubgraphsFinder;
import eu.unitn.disi.db.excode.graph.BinaryDynamicEdge;
import eu.unitn.disi.db.excode.graph.DynamicGraph;
import eu.unitn.disi.db.excode.graph.BinaryDynamicGraph;
import eu.unitn.disi.db.excode.graph.DynamicEdge;
import eu.unitn.disi.db.excode.graph.WeightedDynamicEdge;
import eu.unitn.disi.db.excode.graph.WeightedDynamicGraph;
import eu.unitn.disi.db.excode.utils.CommandLineParser;
import eu.unitn.disi.db.excode.graph.GPCorrelationGraph;
import eu.unitn.disi.db.excode.maxclique.GPCliqueFinder;
import eu.unitn.disi.db.excode.utils.Pair;
import eu.unitn.disi.db.excode.utils.Settings;
import eu.unitn.disi.db.excode.utils.StopWatch;
import eu.unitn.disi.db.excode.utils.Utilities;
import eu.unitn.disi.db.excode.webserver.utils.Configuration;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import jersey.repackaged.com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author bluecopper
 */
public class Main {

    static StopWatch watch;

    public static void main(String[] args) throws Exception {
        //parse the command line arguments
        CommandLineParser.parse(args);

        watch = new StopWatch();
        StopWatch watch2 = new StopWatch();
        //load dynamic graph
        System.out.println(new Date(System.currentTimeMillis()).toGMTString());
        System.out.println("Loading graph.");
        watch.start();
        watch2.start();
        DynamicGraph graph = loadEdges(Settings.dataFolder + Settings.edgeFile);
        System.out.println(".......................Graph loaded in (ms) " + watch2.getElapsedTime());
        Collection<Pair<Integer, Integer>> correlatedPairs;
        System.out.println(new Date(System.currentTimeMillis()).toGMTString());
        System.out.println("Searching correlated pairs.");
        watch2.start();
        if (Settings.isExact) {
            correlatedPairs = graph.findCorrelatedPairs(Settings.minCor);
        } else {
            MinHashBucketization buck = new MinHashBucketization(Settings.numHashFuncs, Settings.numHashRuns, graph.getNumSnaps());
            Collection<Pair<Integer, Integer>> candPairs = buck.getCandidatePairs(graph.getEdges());
            correlatedPairs = graph.findCorrelatedPairs(candPairs, Settings.minCor);
        }
        System.out.println(".......................Found " + correlatedPairs.size() + " Correlated Pairs in (s) " + watch2.getElapsedTimeInSec());
        //find maximal cliques
        GPCorrelationGraph gpgraph = new GPCorrelationGraph(graph.getNumEdges(), correlatedPairs);
        GPCliqueFinder gpfinder = new GPCliqueFinder(gpgraph);
        System.out.println(new Date(System.currentTimeMillis()).toGMTString());
        System.out.println("Searching cliques.");
        watch2.start();
        List<HashIntSet> gpcliques = gpfinder.findMaxCliquesWithCCs();
        System.out.println(".......................Found " + gpcliques.size() + " Cliques in (s) " + watch2.getElapsedTimeInSec());
        watch2.start();
        //find dense subgraphs
        DenseSubgraphsFinder DSFinder = new DenseSubgraphsFinder(graph);
        System.out.println(new Date(System.currentTimeMillis()).toGMTString());
        System.out.println("Finding dense subgraphs.");
        List<HashIntSet> result = DSFinder.findDiverseDenseSubgraphs(gpcliques);
        watch.stop();
        System.out.println("TOTAL TIME (min): " + watch.getElapsedTimeInMin());
        //store results
        writeResults(graph, result);
    }

    public static String runTask(Configuration conf) {
        System.out.println("Starting Task...");
        try {
            DynamicGraph graph = loadEdges(conf.Dataset.Path);
            Collection<Pair<Integer, Integer>> correlatedPairs = graph.findCorrelatedPairs(conf.Task.Correlation);
            GPCorrelationGraph gpgraph = new GPCorrelationGraph(graph.getNumEdges(), correlatedPairs);
            GPCliqueFinder gpfinder = new GPCliqueFinder(gpgraph);
            List<HashIntSet> gpcliques = gpfinder.findMaxCliquesWithCCs();
            DenseSubgraphsFinder DSFinder = new DenseSubgraphsFinder(graph, conf);
            List<Pair<HashIntSet, Double>> result = DSFinder.findDiverseDenseSubgraphsWithScore(gpcliques);
            System.out.println("Done");
            return subs2JSON(graph, result);
        } catch (IOException | JSONException e) {
            System.out.println(e.getMessage());
            return "";
        }
    }

    public static String runExplorationTask(Configuration conf) {
        System.out.println("Starting Exploration Task...");
        try {
            DynamicGraph graph = loadEdges(conf.Dataset.Path);
            HashIntObjMap<HashIntSet> views = graph.generateExplodeView(conf.Subgraph, conf.Task.EdgesPerSnapshot);
            System.out.println("Done");
            return views2JSON(graph, views);
        } catch (IOException | JSONException e) {
            System.out.println(e.getMessage());
            return "";
        }
    }

    public static String subs2JSON(DynamicGraph graph, List<Pair<HashIntSet, Double>> result) throws JSONException {
        if (result.isEmpty()) {
            return "";
        }
        Collections.sort(result, (Pair<HashIntSet, Double> p1, Pair<HashIntSet, Double> p2) -> - Double.compare(p1.getB(), p2.getB()));
        HashIntObjMap<Pair<Integer, HashIntSet>> nodes = HashIntObjMaps.newMutableMap();
        HashObjObjMap<Pair<Integer, Integer>, HashIntSet> edges = HashObjObjMaps.newMutableMap();
        HashIntDoubleMap densities = HashIntDoubleMaps.newMutableMap();
        DecimalFormat df = new DecimalFormat("#.###");
        int nodeCount = 0;
        double previousDensity = Double.parseDouble(df.format(result.get(0).getB()));
        int index = 1;
        densities.put(index, previousDensity);
        for (int s = 0; s < result.size(); s++) {
            for (int edge : result.get(s).getA()) {
                int src = graph.getSrc(edge);
                int dst = graph.getDst(edge);
                int srcId;
                int dstId;
                double thisDensity = Double.parseDouble(df.format(result.get(s).getB()));
                if (thisDensity < previousDensity && index < 6) {
                    index ++;
                    previousDensity = thisDensity;
                    densities.put(index, previousDensity);
                }
                if (nodes.containsKey(src)) {
                    Pair<Integer, HashIntSet> srcP = nodes.get(src);
                    srcId = srcP.getA();
                    HashIntSet srcA = srcP.getB();
                    srcA.add(Integer.parseInt(String.format("%d%d", index, s)));
                    nodes.put(src, new Pair<Integer, HashIntSet>(srcId, srcA));
                } else {
                    srcId = nodeCount;
                    HashIntSet srcA = HashIntSets.newMutableSet();
                    srcA.add(Integer.parseInt(String.format("%d%d", index, s)));
                    nodes.put(src, new Pair<Integer, HashIntSet>(nodeCount, srcA));
                    nodeCount++;
                }
                if (nodes.containsKey(dst)) {
                    Pair<Integer, HashIntSet> dstP = nodes.get(dst);
                    dstId = dstP.getA();
                    HashIntSet dstA = dstP.getB();
                    dstA.add(Integer.parseInt(String.format("%d%d", index, s)));
                    nodes.put(dst, new Pair<Integer, HashIntSet>(dstId, dstA));
                } else {
                    dstId = nodeCount;
                    HashIntSet dstA = HashIntSets.newMutableSet();
                    dstA.add(Integer.parseInt(String.format("%d%d", index, s)));
                    nodes.put(dst, new Pair<Integer, HashIntSet>(nodeCount, dstA));
                    nodeCount++;
                }
                Pair<Integer, Integer> thisEdge = new Pair<>(srcId, dstId);
                HashIntSet apps = edges.getOrDefault(thisEdge, HashIntSets.newMutableSet());
                apps.add(Integer.parseInt(String.format("%d%d", index, s)));
                edges.put(thisEdge, apps);
            }
        }
        JSONObject obj = new JSONObject();
        JSONArray JSONnodes = new JSONArray();
        JSONArray JSONedges = new JSONArray();
        JSONArray JSONdensities = new JSONArray();
        List<Entry<Integer, Pair<Integer, HashIntSet>>> nodeList = new ArrayList(nodes.entrySet());
        Collections.sort(nodeList, (Entry<Integer, Pair<Integer, HashIntSet>> o1, Entry<Integer, Pair<Integer, HashIntSet>> o2) -> Integer.compare(o1.getValue().getA(), o2.getValue().getA()));
        for (Entry<Integer, Pair<Integer, HashIntSet>> entry : nodeList) {
            JSONObject n = new JSONObject();
            n.put("name", entry.getKey());
            StringBuilder builder = new StringBuilder();
            entry.getValue().getB().stream().forEach(sub -> builder.append(",").append(sub));
            n.put("sids", builder.substring(1));
            JSONnodes.put(n);
        }
        obj.put("nodes", JSONnodes);
        for (Entry<Pair<Integer, Integer>, HashIntSet> entry : edges.entrySet()) {
            JSONObject e = new JSONObject();
            e.put("source", entry.getKey().getA());
            e.put("target", entry.getKey().getB());
            StringBuilder builder = new StringBuilder();
            entry.getValue().stream().forEach(sub -> builder.append(",").append(sub));
            e.put("sids", builder.substring(1));
            JSONedges.put(e);
        }
        obj.put("edges", JSONedges);
        for (int id = index; id > 0; id --) {
            JSONObject d = new JSONObject();
            d.put("id", id);
            d.put("value", densities.get(id));
            JSONdensities.put(d);
        }
        obj.put("densities", JSONdensities);
        return obj.toString();
    }

    public static String views2JSON(DynamicGraph graph, HashIntObjMap<HashIntSet> views) throws JSONException {
        if (views.isEmpty()) {
            return "";
        }
        JSONArray snapshots = new JSONArray();
        List<Entry<Integer, HashIntSet>> entries = new ArrayList(views.entrySet());
        Collections.sort(entries, (Entry<Integer, HashIntSet> e1, Entry<Integer, HashIntSet> e2) -> Integer.compare(e1.getKey(), e2.getKey()));
        for (Entry<Integer, HashIntSet> entry : entries) {
            HashIntIntMap nodes = HashIntIntMaps.newMutableMap();
            HashObjSet<Pair<Integer, Integer>> edges = HashObjSets.newMutableSet();
            int nodeCount = 0;
            for (int edge : entry.getValue()) {
                int src = graph.getSrc(edge);
                int dst = graph.getDst(edge);
                int srcId;
                int dstId;
                if (nodes.containsKey(src)) {
                    srcId = nodes.get(src);
                } else {
                    srcId = nodeCount;
                    nodes.put(src, nodeCount);
                    nodeCount++;

                }
                if (nodes.containsKey(dst)) {
                    dstId = nodes.get(dst);
                } else {
                    dstId = nodeCount;
                    nodes.put(dst, nodeCount);
                    nodeCount++;
                }
                edges.add(new Pair<>(srcId, dstId));
            }
            JSONObject obj = new JSONObject();
            JSONArray JSONnodes = new JSONArray();
            JSONArray JSONedges = new JSONArray();
            List<Entry<Integer, Integer>> nodeList = new ArrayList(nodes.entrySet());
            Collections.sort(nodeList, (Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) -> Integer.compare(o1.getValue(), o2.getValue()));
            for (Entry<Integer, Integer> node : nodeList) {
                JSONObject n = new JSONObject();
                n.put("name", node.getKey());
                JSONnodes.put(n);
            }
            obj.put("nodes", JSONnodes);

            for (Pair<Integer, Integer> edge : edges) {
                JSONObject e = new JSONObject();
                e.put("source", edge.getA());
                e.put("target", edge.getB());
                JSONedges.put(e);
            }
            obj.put("edges", JSONedges);
            JSONObject view = new JSONObject();
            view.put("number", entry.getKey());
            view.put("subgraph", obj);
            snapshots.put(view);
        }
        return snapshots.toString();
    }

    public static DynamicGraph loadEdges(String edgePath) throws IOException {
        final BufferedReader rows = new BufferedReader(new FileReader(edgePath));
        Map<Integer, Integer> nodes = new HashMap<Integer, Integer>();
        List<DynamicEdge> edges = Lists.newArrayList();
        String line = rows.readLine();
        int node_counter = 0;
        int edge_counter = 0;

        while (line != null) {
            String[] parts = line.split(" ");
            int srcId = Integer.parseInt(parts[0]);
            int dstId = Integer.parseInt(parts[1]);
            if (nodes.putIfAbsent(srcId, node_counter) == null) {
                node_counter++;
            }
            if (nodes.putIfAbsent(dstId, node_counter) == null) {
                node_counter++;
            }
            int label;
            String[] series;
            if (Settings.isLabeled) {
                label = Integer.parseInt(parts[2]);
                series = parts[3].split(",");
            } else {
                label = 1;
                series = parts[2].split(",");
            }
            DynamicEdge edge;
            if (Settings.isWeighted) {
                edge = new WeightedDynamicEdge(edge_counter, nodes.get(srcId), nodes.get(dstId), label, Utilities.convertToDouble(series));
            } else {
                edge = new BinaryDynamicEdge(edge_counter, nodes.get(srcId), nodes.get(dstId), label, Utilities.convertToDouble(series));
            }
            edges.add(edge);
            edge_counter++;
            line = rows.readLine();
        }
        rows.close();

        DynamicGraph g;
        if (Settings.isWeighted) {
            g = new WeightedDynamicGraph(nodes.size(), edges.size());
        } else {
            g = new BinaryDynamicGraph(nodes.size(), edges.size());
        }
        List<Entry<Integer, Integer>> node_list = Lists.newArrayList(nodes.entrySet());
        Collections.sort(node_list, (Entry<Integer, Integer> e1, Entry<Integer, Integer> e2)
                -> Integer.compare(e1.getValue(), e2.getValue()));
        node_list.stream().forEach(e -> g.addNode(e.getValue(), e.getKey()));
        edges.stream().forEachOrdered(edge -> g.addEdge(edge));
        System.out.println("Nodes: " + g.getNumNodes() + " Edges: " + g.getNumEdges());
        return g;
    }

    public static void writeResults(DynamicGraph graph, Collection<HashIntSet> densCorEdges) throws IOException {
        try {
            FileWriter fw = new FileWriter(Settings.outputFolder + "statistics.csv", true);
            fw.write(String.format("%s\t%s\t%f\t%d\t%s\t%f\t%f\t%d\t%d\t%s\t%s\n",
                    Settings.edgeFile,
                    new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()),
                    watch.getElapsedTime() / 1000.0D,
                    densCorEdges.size(),
                    Settings.isMA,
                    Settings.minCor,
                    Settings.minDen,
                    Settings.maxCCSize,
                    Settings.minEdgesInSnap,
                    Settings.maxJac,
                    (!Settings.isExact) ? Settings.numHashFuncs + "+" + Settings.numHashRuns : ""));
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            String fName = "ExCoDE_" + Settings.edgeFile
                    + "_C" + Settings.minCor
                    + "D" + Settings.minDen
                    + ((Settings.isMA) ? "M" : "A")
                    + "K" + Settings.minEdgesInSnap
                    + "S" + Settings.maxCCSize
                    + "E" + Settings.maxJac
                    + ((!Settings.isExact) ? "AX" + Settings.numHashFuncs + "+" + Settings.numHashRuns : "");
            FileWriter fwP = new FileWriter(Settings.outputFolder + fName);
            for (Set<Integer> denseSub : densCorEdges) {
                StringBuilder builder = new StringBuilder();
                denseSub.stream().forEach(edge -> builder.append(edge).append("\t"));
                fwP.write(builder.toString() + "\n");
            }
            fwP.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
