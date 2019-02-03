package eu.unitn.disi.db.excode.graph;

/**
 *
 * @author bluecopper
 */
public class BinaryDynamicEdge extends DynamicEdge {

    public BinaryDynamicEdge(int edgeId, int src, int dst, int edgeLabel, double[] edgeSeries) {
        super(edgeId, src, dst, edgeLabel, edgeSeries);
    }
    
    public BinaryDynamicEdge(int edgeId, int src, int dst, int edgeLabel,  int size) {
        super(edgeId, src, dst, edgeLabel, size);
    }
    
    public double computeSupport() {
        double sum = 0;
        for (double d : edgeSeries) {
            if (d > 0) {
                sum += 1;
            }
        }
        return sum;
    }
    
}
