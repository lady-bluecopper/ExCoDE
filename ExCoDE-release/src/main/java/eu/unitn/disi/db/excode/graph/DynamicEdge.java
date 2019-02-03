package eu.unitn.disi.db.excode.graph;

/**
 *
 * @author bluecopper
 */
public abstract class DynamicEdge extends Edge {
    
    protected double[] edgeSeries;
    protected double support;
    
    public DynamicEdge(int edgeId, int src, int dst, int edgeLabel, int size) {
        super(edgeId, src, dst, edgeLabel);
        this.edgeSeries = new double[size];
    }
    
    public DynamicEdge(int edgeId, int src, int dst, int edgeLabel, double[] edgeSeries) {
        super(edgeId, src, dst, edgeLabel);
        this.edgeSeries = edgeSeries;
        this.support = computeSupport();
    }
    
    public double[] getEdgeSeries() {
        return edgeSeries;
    }
    
    public void setEdgeSeries(double[] edgeSeries) {
        this.edgeSeries = edgeSeries;
        this.support = computeSupport();
    }
    
    public boolean existsInT(int t) {
        return edgeSeries[t] > 0;
    }
    
    public abstract double computeSupport();
    
    public double getSupport() {
        return support;
    }
    
}
