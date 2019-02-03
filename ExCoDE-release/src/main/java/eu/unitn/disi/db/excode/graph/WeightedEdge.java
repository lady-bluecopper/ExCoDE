package eu.unitn.disi.db.excode.graph;

/**
 *
 * @author bluecopper
 */
public class WeightedEdge extends Edge {
    
    private double weight;
    
    public WeightedEdge(int edgeId, int src, int dst, int edgeLabel) {
        super(edgeId, src, dst, edgeLabel);
        this.weight = 0;
    }
    
    public WeightedEdge(int edgeId, int src, int dst, int edgeLabel, double weight) {
        super(edgeId, src, dst, edgeLabel);
        this.weight = weight;
    }
    
    public WeightedEdge(Edge edge, double weight) {
        super(edge.getEdgeID(), edge.getSrc(), edge.getDst(), edge.getEdgeLabel());
        this.weight = weight;
    }
    
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    public double getWeight() {
        return weight;
    }
    
    public void incrementWeight() {
        this.weight += 1;
    }
    
    public void incrementWeight(int c) {
        this.weight += c;
    }
    
}
