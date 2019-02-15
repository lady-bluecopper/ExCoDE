package eu.unitn.disi.db.excode.main;

import com.koloboke.collect.map.hash.HashIntIntMap;
import com.koloboke.collect.map.hash.HashIntIntMaps;
import com.koloboke.collect.set.hash.HashIntSet;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
    
    public static String runTask(Configuration conf) throws JSONException {
        System.out.println("Starting Task...");
        try {
            DynamicGraph graph = loadEdges(conf.Dataset.Path);
            Collection<Pair<Integer, Integer>> correlatedPairs = graph.findCorrelatedPairs(conf.Task.Correlation);
            GPCorrelationGraph gpgraph = new GPCorrelationGraph(graph.getNumEdges(), correlatedPairs);
            GPCliqueFinder gpfinder = new GPCliqueFinder(gpgraph);
            List<HashIntSet> gpcliques = gpfinder.findMaxCliquesWithCCs();
            DenseSubgraphsFinder DSFinder = new DenseSubgraphsFinder(graph, conf);
            List<HashIntSet> result = DSFinder.findDiverseDenseSubgraphs(gpcliques);
            System.out.println("Done");
            return stringify(graph, result);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return "";
        }
    }
    
    public static String stringify(DynamicGraph graph, Collection<HashIntSet> result) throws JSONException {
        if (result.isEmpty()) {
            return "";
        }
        HashIntIntMap nodes = HashIntIntMaps.newMutableMap();
        HashObjSet<Pair<Integer, Integer>> edges = HashObjSets.newMutableSet();
        int nodeCount = 0;
        for (Set<Integer> denseSub : result) {
            for (int edge : denseSub) {
                int src = graph.getSrc(edge);
                int dst = graph.getDst(edge);
                int srcId;
                int dstId;
                if (nodes.containsKey(src)) {
                    srcId = nodes.get(src);
                } else {
                    srcId = nodeCount;
                    nodes.put(src, nodeCount);
                    nodeCount ++;
                    
                }
                if (nodes.containsKey(dst)) {
                    dstId = nodes.get(dst);
                } else {
                    dstId = nodeCount;
                    nodes.put(dst, nodeCount);
                    nodeCount ++;
                }
                edges.add(new Pair<>(srcId, dstId));
            }
        }
        JSONObject obj = new JSONObject();
        JSONArray JSONnodes = new JSONArray();
        JSONArray JSONedges = new JSONArray();
        List<Entry<Integer, Integer>> nodeList = new ArrayList(nodes.entrySet());
        Collections.sort(nodeList, (Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) -> Integer.compare(o1.getValue(), o2.getValue()));
        for (Entry<Integer, Integer> entry : nodeList) {
            JSONObject n = new JSONObject();
            n.put("name", entry.getKey());
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
        return obj.toString();
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
                node_counter ++;
            }
            if (nodes.putIfAbsent(dstId, node_counter) == null) {
                node_counter ++;
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
            String fName = "ExCoDE_" + Settings.edgeFile + 
                    "_C" + Settings.minCor + 
                    "D" + Settings.minDen + 
                    ((Settings.isMA) ? "M" : "A") + 
                    "K" + Settings.minEdgesInSnap + 
                    "S" + Settings.maxCCSize +
                    "E" + Settings.maxJac +
                    ((!Settings.isExact) ? "AX" + Settings.numHashFuncs + "+" + Settings.numHashRuns : "");
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
