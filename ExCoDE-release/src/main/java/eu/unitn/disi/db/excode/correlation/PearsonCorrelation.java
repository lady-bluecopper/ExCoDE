package eu.unitn.disi.db.excode.correlation;

import eu.unitn.disi.db.excode.graph.DynamicEdge;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/**
 *
 * @author bluecopper
 */
public class PearsonCorrelation implements CorrelationMeasure {
    
    PearsonsCorrelation measure;
    
    public PearsonCorrelation() {
        measure = new PearsonsCorrelation();
    }

    public double computeCorrelation(DynamicEdge e1, DynamicEdge e2) {
        return measure.correlation(e1.getEdgeSeries(), e2.getEdgeSeries());
    }
    
}
