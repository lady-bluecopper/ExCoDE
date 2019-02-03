package eu.unitn.disi.db.excode.correlation;

import eu.unitn.disi.db.excode.graph.DynamicEdge;

/**
 *
 * @author bluecopper
 */
public interface CorrelationMeasure {
    
    public double computeCorrelation(DynamicEdge e1, DynamicEdge e2);
    
}
