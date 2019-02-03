package eu.unitn.disi.db.excode.correlation;

import eu.unitn.disi.db.excode.graph.DynamicEdge;
import org.apache.commons.math3.ml.distance.EuclideanDistance;

/**
 *
 * @author bluecopper
 */
public class EuclideanCorrelation implements CorrelationMeasure {
    
    EuclideanDistance measure;
    
    public EuclideanCorrelation() {
        this.measure = new EuclideanDistance();
    }

    @Override
    public double computeCorrelation(DynamicEdge e1, DynamicEdge e2) {
        return 1 / (1 + measure.compute(e1.getEdgeSeries(), e2.getEdgeSeries()));
    }

}