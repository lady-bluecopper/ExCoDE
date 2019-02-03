package eu.unitn.disi.db.excode.graph;

import java.util.stream.IntStream;

/**
 *
 * @author bluecopper
 */
public class WeightedDynamicEdge extends DynamicEdge {
    
    public WeightedDynamicEdge(int edgeId, int src, int dst, int edgeLabel, double[] edgeSeries) {
        super(edgeId, src, dst, edgeLabel, edgeSeries);
    }
    
    public WeightedDynamicEdge(int edgeId, int src, int dst, int edgeLabel, int size) {
        super(edgeId, src, dst, edgeLabel, size);
    }
    
    public double getEdgeWeight(int t) {
        return edgeSeries[t];
    }
    
    public double computeSupport() {
        double sum = 0;
        for (double d : edgeSeries) {
            sum += d;
        }
        return sum;
    }
    
    public void zetaNormalization() {
        final double mean = support / edgeSeries.length;
        double sum = 0;
        for (double d : edgeSeries) {
            sum += Math.pow(d - mean, 2);
        }
        double var = Math.sqrt(sum / edgeSeries.length);
        IntStream.range(0, edgeSeries.length).forEach(i -> edgeSeries[i] = (edgeSeries[i] - mean) / var);
    }
}
