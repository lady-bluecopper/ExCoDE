package eu.unitn.disi.db.excode.graph;

/**
 *
 * @author bluecopper
 */
public class Edge {
    
    private final int edgeId;
    private final int src;
    private final int dst;
    private int edgeLabel;
    
    public Edge(int edgeId, int src, int dst, int edgeLabel) {
        this.edgeId = edgeId;
        this.src = src;
        this.dst = dst;
        this.edgeLabel = edgeLabel;
    }
    
    public int getEdgeID() {
        return edgeId;
    }
    
    public int getSrc() {
        return src;
    }
    
    public int getDst() {
        return dst;
    }
    
    public int getEdgeLabel() {
        return edgeLabel;
    }
    
    public void setEdgeLabel(int label) {
        this.edgeLabel = label;
    }
    
    @Override
    public boolean equals(Object other) {
        //check for self-comparison
        if (this == other) {
            return true;
        }
        //actual comparison
        if (other instanceof Edge) {
            Edge o = (Edge) other;
            return (this.edgeId == o.edgeId);
        } else {
            throw new IllegalArgumentException("Objects not comparable");
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + this.edgeId;
        return hash;
    }
    
    @Override
    public String toString() {
        return "(" + this.src + "," + this.dst + ") - " + this.edgeLabel;
    }
    
}